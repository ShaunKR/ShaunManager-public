package knr.shaunmanager.calendar;

import knr.shaunmanager.amaranth.AmaranthAttendanceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class AttendanceScheduler {
    private static final Logger LOG=LoggerFactory.getLogger(AttendanceScheduler.class);
    private static final ZoneId KST=ZoneId.of("Asia/Seoul");
    private knr.shaunmanager.shaunrise.ShaunriseService mission;
    @org.springframework.beans.factory.annotation.Autowired
    public void setMission(knr.shaunmanager.shaunrise.ShaunriseService mission){this.mission=mission;}

    private final AmaranthAttendanceService attendance;
    private final LeaveService leaves;
    private final AttendancePlanRepository plans;
    private final WorkCalendar calendar;
    private final boolean enabled;
    public AttendanceScheduler(AmaranthAttendanceService attendance,LeaveService leaves,
            AttendancePlanRepository plans,WorkCalendar calendar,
            @Value("${shaun.scheduler.enabled:false}") boolean enabled) {
        this.attendance=attendance;this.leaves=leaves;this.plans=plans;this.calendar=calendar;this.enabled=enabled;
    }
    @javax.annotation.PostConstruct
    public void logConfiguration() {
        LOG.info("Attendance scheduler enabled={} zone=Asia/Seoul cadence=every-minute", enabled);
    }
    @Scheduled(cron="0 0 * * * *",zone="Asia/Seoul")
    public void heartbeat() {
        LOG.info("Attendance scheduler heartbeat enabled={} time={}",enabled,LocalDateTime.now(KST));
    }
    public boolean isEnabled(){return enabled;}
    @Scheduled(cron="0 * * * * *",zone="Asia/Seoul")
    public void tick(){if(enabled) runAt(LocalDateTime.now(KST));}
    synchronized void runAt(LocalDateTime now) {
        if(!calendar.isWorkday(now.toLocalDate())){LOG.debug("Attendance skipped date={} reason=NON_WORKDAY",now.toLocalDate());return;}
        String date=now.toLocalDate().format(DateTimeFormatter.BASIC_ISO_DATE);
        Leave setting=leaves.getLeave(date);
        WorkType type=WorkType.from(setting==null?"work":setting.getLeaveType());
        if(type==WorkType.ANNUAL){LOG.debug("Attendance skipped date={} reason=ANNUAL",date);return;}
        String location=setting==null||setting.getLocationCd()==null?"외부현장":setting.getLocationCd();
        WorkShift shift=WorkShift.from(setting==null?null:setting.getWorkShift());
        shift.validate(type);
        LocalTime checkIn=shift.checkIn(type), checkOut=shift.checkOut(type);
        AttendancePlan plan=plans.findById(date).orElse(new AttendancePlan(date));
        String oldTargets=plan.getInTarget()+"/"+plan.getOutTarget();
        WorkType previous=plan.getWorkType()==null?null:WorkType.from(plan.getWorkType());
        WorkShift previousShift=WorkShift.from(plan.getWorkShift());
        // Only unattempted targets whose base time changed are resampled.
        if("PENDING".equals(plan.getInState()) && (plan.getInTarget()==null||previous==null||!checkIn.equals(previousShift.checkIn(previous))))
            plan.setInTarget(randomTime(checkIn,shift.earliestOffset(true)).toString());
        if("PENDING".equals(plan.getOutState()) && (plan.getOutTarget()==null||previous==null||!checkOut.equals(previousShift.checkOut(previous)) || shift.earliestOffset(false)!=previousShift.earliestOffset(false)))
            plan.setOutTarget(randomTime(checkOut,shift.earliestOffset(false)).toString());
        plan.setWorkShift(shift.code);plan.setWorkType(type.code);plan.setLocationCd(location);plans.saveAndFlush(plan);
        if(!oldTargets.equals(plan.getInTarget()+"/"+plan.getOutTarget()))
            LOG.info("Attendance plan date={} type={} inTarget={} outTarget={} inState={} outState={}",date,type,plan.getInTarget(),plan.getOutTarget(),plan.getInState(),plan.getOutState());
        if(setting==null || !setting.isSkipCheckIn()) execute(plan,true,now.toLocalTime(),checkIn);
        if(setting==null || !setting.isSkipCheckOut()) execute(plan,false,now.toLocalTime(),checkOut);
    }
    private LocalTime randomTime(LocalTime base,int earliestOffset){return base.plusMinutes(ThreadLocalRandom.current().nextInt(earliestOffset,6));}
    private void execute(AttendancePlan plan,boolean in,LocalTime now,LocalTime base) {
        String state=in?plan.getInState():plan.getOutState();
        if(!"PENDING".equals(state))return;
        LocalTime target=LocalTime.parse(in?plan.getInTarget():plan.getOutTarget());
        if(now.isBefore(target))return;
        if(!now.isBefore(base.plusMinutes(6))) {state(plan,in,"MISSED");return;}
        LOG.info("Attendance attempt date={} action={} target={} actual={}",plan.getWorkDt(),in?"IN":"OUT",target,now);
        // Persist claim BEFORE any ERP operation. Restart/timeout must not replay a write.
        state(plan,in,"RUNNING");
        String stage="TODAY_READ";
        AttendanceLog record=leaves.getOrCreateLog(plan.getWorkDt(),plan.getLocationCd());
        try {
            Map<String,Object> today=attendance.today();
            String field=in?"comeTm":"leaveTm";
            String existing=Objects.toString(today.get(field),"");
            String terminal;
            String actual;
            if(!existing.isEmpty()) {actual=existing;terminal="SKIPPED";}
            else {
                stage="REGISTER";
                Map<String,Object> result=in?attendance.checkIn(plan.getLocationCd()):attendance.checkOut(plan.getLocationCd());
                stage="VERIFY_RESULT";
                if(!Boolean.TRUE.equals(result.get("verificationSucceeded"))) {
                    throw new IllegalStateException("VERIFICATION_REQUIRED");
                }
                Object after=result.get("after");
                actual=after instanceof Map?Objects.toString(((Map<?,?>)after).get(field),""):"";
                if(actual.isEmpty())throw new IllegalStateException("VERIFICATION_REQUIRED");
                terminal="DONE";
            }
            stage="LOCAL_SAVE";
            record=leaves.getOrCreateLog(plan.getWorkDt(),plan.getLocationCd());
            String time=actual.startsWith(plan.getWorkDt())&&actual.length()>=12?actual.substring(8,12)+"00":null;
            if(in){record.setCheckInTm(time);record.setCheckInSuccess(true);}
            else{record.setCheckOutTm(time);record.setCheckOutSuccess(true);}
            record.setLocationCd(plan.getLocationCd());record.setErrorMsg(null);
            leaves.saveLog(record);state(plan,in,terminal);
            if(mission!=null && "DONE".equals(terminal))mission.record(LocalDate.parse(plan.getWorkDt(),DateTimeFormatter.BASIC_ISO_DATE),in,true,actual);
            LOG.info("Attendance schedule {} {} {}",plan.getWorkDt(),in?"IN":"OUT",terminal);
        } catch(Exception ex) {
            LOG.warn("Attendance failed date={} action={} stage={} code={} exceptionType={}",plan.getWorkDt(),in?"IN":"OUT",stage,knr.shaunmanager.amaranth.AttendanceDiagnostics.code(ex),ex.getClass().getSimpleName());
            // Outcome may be unknown; never retry automatically.
            stage="LOCAL_SAVE";
            record=leaves.getOrCreateLog(plan.getWorkDt(),plan.getLocationCd());
            if(in)record.setCheckInSuccess(false);else record.setCheckOutSuccess(false);
            record.setErrorMsg("VERIFY_ERP_RECORD_BEFORE_RETRY");
            leaves.saveLog(record);state(plan,in,"REVIEW_REQUIRED");
            if(mission!=null)mission.record(LocalDate.parse(plan.getWorkDt(),DateTimeFormatter.BASIC_ISO_DATE),in,false,"");
            LOG.warn("Attendance schedule requires review: {} {}",plan.getWorkDt(),in?"IN":"OUT");
        }
    }
    private void state(AttendancePlan plan,boolean in,String state) {
        if(in)plan.setInState(state);else plan.setOutState(state);plans.saveAndFlush(plan);
        LOG.info("Attendance state date={} action={} state={}",plan.getWorkDt(),in?"IN":"OUT",state);
    }
}

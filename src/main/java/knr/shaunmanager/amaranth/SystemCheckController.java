package knr.shaunmanager.amaranth;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import knr.shaunmanager.calendar.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system")
public class SystemCheckController {
    private final AmaranthAttendanceService attendance;
    private final AttendanceScheduler scheduler;
    private final AttendancePlanRepository plans;
    private final LeaveRepository leaves;
    private final AttendanceLogRepository logs;
    public SystemCheckController(AmaranthAttendanceService attendance,AttendanceScheduler scheduler,
            AttendancePlanRepository plans,LeaveRepository leaves,AttendanceLogRepository logs) {
        this.attendance=attendance;this.scheduler=scheduler;this.plans=plans;this.leaves=leaves;this.logs=logs;
    }
    @PostMapping("/check")
    public ResponseEntity<SystemCheckReport> check() {
        SystemCheckReport report=new SystemCheckReport();
        String date=LocalDate.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.BASIC_ISO_DATE);
        report.add("SCHEDULER",scheduler.isEnabled()?"PASS":"WARN",scheduler.isEnabled()?null:"SCHEDULER_DISABLED",0);
        report.check("DATABASE_READ",()->{
            leaves.findById(date);logs.findById(date);
            Optional<AttendancePlan> found=plans.findById(date);
            if(found.isPresent()) {
                AttendancePlan p=found.get();Map<String,Object> plan=new LinkedHashMap<>();
                plan.put("date",date);plan.put("workShift",p.getWorkShift());plan.put("checkInTarget",p.getInTarget());plan.put("checkOutTarget",p.getOutTarget());
                plan.put("checkInState",p.getInState());plan.put("checkOutState",p.getOutState());report.todayPlan=plan;
                if(needsAttention(p.getInState())||needsAttention(p.getOutState()))
                    report.add("TODAY_PLAN","WARN","PLAN_REQUIRES_ATTENTION",0);
                else report.add("TODAY_PLAN","PASS",null,0);
            } else report.add("TODAY_PLAN","WARN","NO_PLAN_FOR_TODAY",0);
        });
        attendance.checkReadiness(report);
        return ResponseEntity.status("FAIL".equals(report.status)?503:200)
            .header("Cache-Control","no-store").body(report);
    }
    private boolean needsAttention(String state) {
        return "REVIEW_REQUIRED".equals(state)||"MISSED".equals(state)||"RUNNING".equals(state);
    }
}

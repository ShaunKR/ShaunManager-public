package knr.shaunmanager.calendar;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LeaveService {

    private final LeaveRepository leaveRepo;
    private final AttendanceLogRepository logRepo;

    public LeaveService(LeaveRepository leaveRepo, AttendanceLogRepository logRepo) {
        this.leaveRepo = leaveRepo;
        this.logRepo   = logRepo;
    }

    // 전체 연차 조회 → { "20260921": { leaveType, locationCd } }
    @Transactional(readOnly = true)
    public Map<String, Map<String, String>> findAllLeaves() {
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        leaveRepo.findAll().forEach(l -> {
            Map<String, String> info = new LinkedHashMap<>();
            info.put("leaveType",  l.getLeaveType());
            info.put("locationCd", l.getLocationCd());
            info.put("workShift", l.getWorkShift());
            info.put("skipCheckIn", Boolean.toString(l.isSkipCheckIn()));
            info.put("skipCheckOut", Boolean.toString(l.isSkipCheckOut()));
            result.put(l.getWorkDt(), info);
        });
        return result;
    }

    // 전체 출퇴근 로그 조회 → { "20260921": { checkInTm, checkOutTm, ... } }
    @Transactional(readOnly = true)
    public Map<String, Map<String, Object>> findAllLogs() {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        logRepo.findAll().forEach(l -> {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("checkInTm",       l.getCheckInTm());
            info.put("checkOutTm",      l.getCheckOutTm());
            info.put("locationCd",      l.getLocationCd());
            info.put("checkInSuccess",  l.getCheckInSuccess());
            info.put("checkOutSuccess", l.getCheckOutSuccess());
            info.put("errorMsg",        l.getErrorMsg());
            result.put(l.getWorkDt(), info);
        });
        return result;
    }

    // 연차/반차 저장 or 수정
    @Transactional
    public void saveLeave(String workDt, String leaveType, String locationCd) {
        saveLeave(workDt, leaveType, locationCd, null);
    }

    @Transactional
    public void saveLeave(String workDt, String leaveType, String locationCd, String workShift) {
        saveLeave(workDt, leaveType, locationCd, workShift, null, null);
    }

    @Transactional
    public void saveLeave(String workDt, String leaveType, String locationCd, String workShift,
                          Boolean skipCheckIn, Boolean skipCheckOut) {
        validateDate(workDt); WorkType.from(leaveType);
        WorkShift shift = WorkShift.from(workShift);
        shift.validate(WorkType.from(leaveType));
        String loc = (locationCd == null || locationCd.isEmpty()) ? "외부현장" : locationCd;
        if (!"외부현장".equals(loc) && !"본사".equals(loc)) throw new IllegalArgumentException("INVALID_LOCATION");
        Leave leave = leaveRepo.findById(workDt).orElse(new Leave(workDt, leaveType, loc));
        leave.setLeaveType(leaveType);
        leave.setLocationCd(loc);
        leave.setWorkShift(shift.code);
        // Older clients omitting these fields must not erase a user's skip settings.
        if (skipCheckIn != null) leave.setSkipCheckIn(skipCheckIn);
        if (skipCheckOut != null) leave.setSkipCheckOut(skipCheckOut);
        leaveRepo.save(leave);
    }

    // 출근으로 되돌리기
    @Transactional
    public void deleteLeave(String workDt) {
        validateDate(workDt); leaveRepo.findById(workDt).ifPresent(leaveRepo::delete);
    }

    // 특정 날짜 연차 정보 조회 (스케줄러용)
    @Transactional(readOnly = true)
    public Leave getLeave(String workDt) {
        return leaveRepo.findById(workDt).orElse(null);
    }

    // 출퇴근 로그 저장 (스케줄러용)
    @Transactional
    public AttendanceLog getOrCreateLog(String workDt, String locationCd) {
        return logRepo.findById(workDt).orElse(new AttendanceLog(workDt, locationCd));
    }

    @Transactional
    public void saveLog(AttendanceLog log) {
        logRepo.save(log);
    }
    static void validateDate(String value) {
        try {
            if (value == null || !value.matches("[0-9]{8}")) throw new IllegalArgumentException();
            java.time.LocalDate.parse(value, java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        } catch (java.time.DateTimeException ex) { throw new IllegalArgumentException("INVALID_DATE"); }
    }
    // Merge only valid ERP response times. An empty field must not erase an earlier record.
    @Transactional
    public void saveRegistrationTimes(String date, String location, Map<String, Object> times) {
        validateDate(date);
        String in = responseTime(date, times.get("comeTm"));
        String out = responseTime(date, times.get("leaveTm"));
        if (in == null && out == null) return;
        AttendanceLog record = logRepo.findById(date).orElse(new AttendanceLog(date, location));
        if (in != null) { record.setCheckInTm(in); record.setCheckInSuccess(true); }
        if (out != null) { record.setCheckOutTm(out); record.setCheckOutSuccess(true); }
        record.setLocationCd(location);
        logRepo.save(record);
    }
    private static String responseTime(String date, Object value) {
        if (!(value instanceof String)) return null;
        String text = (String) value;
        if (!text.startsWith(date) || !text.matches("[0-9]{12}([0-9]{2})?")) return null;
        String time = text.substring(8);
        if (time.length() == 4) time += "00";
        try { java.time.LocalTime.parse(time, java.time.format.DateTimeFormatter.ofPattern("HHmmss").withResolverStyle(java.time.format.ResolverStyle.STRICT)); }
        catch (java.time.DateTimeException ex) { return null; }
        return time;
    }
}

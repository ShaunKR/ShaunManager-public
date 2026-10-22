package knr.shaunmanager.calendar;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/calendar")
public class LeaveController {
    private final LeaveService service;
    private final WorkCalendar calendar;
    private final AttendanceScheduler scheduler;
    public LeaveController(LeaveService service, WorkCalendar calendar, AttendanceScheduler scheduler) {
        this.service=service; this.calendar=calendar; this.scheduler=scheduler;
    }
    @GetMapping("/leaves")
    public Map<String, Map<String, String>> getAll() { return service.findAllLeaves(); }
    @GetMapping("/holidays")
    public Map<String,String> holidays() { return calendar.holidays(); }
    @GetMapping("/logs")
    public Map<String, Map<String, Object>> logs() { return service.findAllLogs(); }
    @GetMapping("/scheduler")
    public Map<String,Object> scheduler() { return Collections.singletonMap("enabled",scheduler.isEnabled()); }
    @PostMapping("/leaves")
    public ResponseEntity<Void> save(@RequestBody LeaveRequest req) {
        service.saveLeave(req.getWorkDt(),req.getLeaveType(),req.getLocationCd(),req.getWorkShift(),req.getSkipCheckIn(),req.getSkipCheckOut());
        return ResponseEntity.ok().build();
    }
    @DeleteMapping("/leaves/{workDt}")
    public ResponseEntity<Void> delete(@PathVariable String workDt) {
        service.deleteLeave(workDt); return ResponseEntity.ok().build();
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,String>> invalid(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Collections.singletonMap("code","INVALID_DAY_SETTING"));
    }
    public static class LeaveRequest {
        private String workDt,leaveType,locationCd,workShift;
        private Boolean skipCheckIn, skipCheckOut;
        public Boolean getSkipCheckIn(){return skipCheckIn;} public void setSkipCheckIn(Boolean v){skipCheckIn=v;}
        public Boolean getSkipCheckOut(){return skipCheckOut;} public void setSkipCheckOut(Boolean v){skipCheckOut=v;}
        public String getWorkShift(){return workShift;} public void setWorkShift(String v){workShift=v;}
        public String getWorkDt(){return workDt;} public void setWorkDt(String v){workDt=v;}
        public String getLeaveType(){return leaveType;} public void setLeaveType(String v){leaveType=v;}
        public String getLocationCd(){return locationCd;} public void setLocationCd(String v){locationCd=v;}
    }
}

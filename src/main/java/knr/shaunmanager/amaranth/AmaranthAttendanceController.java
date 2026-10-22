package knr.shaunmanager.amaranth;

import java.util.Collections;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/amaranth/attendance")
public class AmaranthAttendanceController {
    private knr.shaunmanager.shaunrise.ShaunriseService mission;
    @org.springframework.beans.factory.annotation.Autowired
    public void setMission(knr.shaunmanager.shaunrise.ShaunriseService mission){this.mission=mission;}

    private final AmaranthAttendanceService service;
    public AmaranthAttendanceController(AmaranthAttendanceService service) { this.service = service; }
    @GetMapping("/today")
    public Map<String, Object> today() { return service.today(); }
    @PostMapping("/check-in")
    public Map<String, Object> checkIn(@Valid @RequestBody Request request) {
        return register(true,request.getLocationCd());
    }
    @PostMapping("/check-out")
    public Map<String, Object> checkOut(@Valid @RequestBody Request request) {
        return register(false,request.getLocationCd());
    }
    private Map<String,Object> register(boolean in,String location){
        Map<String,Object> result;
        try {result=in?service.checkIn(location):service.checkOut(location);}
        catch(RuntimeException ex){if(mission!=null)mission.record(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul")),in,false,"");throw ex;}
        if(mission!=null){
            Object after=result.get("after");
            String actual=after instanceof Map?java.util.Objects.toString(((Map<?,?>)after).get(in?"comeTm":"leaveTm"),""):"";
            mission.record(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul")),in,Boolean.TRUE.equals(result.get("verificationSucceeded")),actual);
        }
        return result;
    }
    public static class Request {
        @NotBlank private String locationCd;
        public String getLocationCd() { return locationCd; }
        public void setLocationCd(String value) { locationCd = value; }
    }
    @ExceptionHandler(AmaranthLoginService.LoginFailure.class)
    public ResponseEntity<Map<String, String>> failure(AmaranthLoginService.LoginFailure ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Collections.singletonMap("code", ex.getMessage()));
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalid(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Collections.singletonMap("code", "INVALID_ATTENDANCE_REQUEST"));
    }
}

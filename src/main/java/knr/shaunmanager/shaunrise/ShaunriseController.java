package knr.shaunmanager.shaunrise;
import org.springframework.web.bind.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.time.*;
import java.util.*;
@RestController @RequestMapping("/api/shaunrise")
@ConditionalOnProperty(prefix="shaun.shaunrise",name="test-enabled",havingValue="true")
public class ShaunriseController {
 private final ShaunriseService service;
 private final ShaunriseProperties props;
 public ShaunriseController(ShaunriseService service,ShaunriseProperties props){this.service=service;this.props=props;}
 @GetMapping("/preview/{action}") public Map<String,Object> preview(@PathVariable String action,@RequestParam String date){return service.preview(action,LocalDate.parse(date));}
 @PostMapping("/test/{action}") public Map<String,Object> test(@PathVariable String action,@RequestParam String date){service.test(action,LocalDate.parse(date));return Collections.singletonMap("queued",true);}
 @PostMapping("/launch") public void launch(){if(LocalDate.now(ZoneId.of("Asia/Seoul")).isBefore(props.getLaunchDate()))throw new IllegalArgumentException("LAUNCH_NOT_DUE");service.launch();}
 @ExceptionHandler(IllegalArgumentException.class) @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
 public Map<String,String> invalid(){return Collections.singletonMap("code","INVALID_MISSION_REQUEST");}
}

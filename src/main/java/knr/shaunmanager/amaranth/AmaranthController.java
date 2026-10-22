package knr.shaunmanager.amaranth;

import java.util.Collections;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/amaranth")
public class AmaranthController {
    private final AmaranthLoginService service;
    public AmaranthController(AmaranthLoginService service) { this.service = service; }
    @PostMapping("/login")
    public Map<String, Object> login() { return service.login(); }
    @GetMapping("/status")
    public Map<String, Object> status() { return service.status(); }
    @ExceptionHandler(AmaranthLoginService.LoginFailure.class)
    public ResponseEntity<Map<String, String>> failure(AmaranthLoginService.LoginFailure ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Collections.singletonMap("code", ex.getMessage()));
    }
}

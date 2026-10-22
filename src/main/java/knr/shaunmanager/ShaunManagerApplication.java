package knr.shaunmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.scheduling.annotation.EnableScheduling
@SpringBootApplication
public class ShaunManagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShaunManagerApplication.class, args);
    }
}

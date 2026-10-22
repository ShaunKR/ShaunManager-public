package knr.shaunmanager.shaunrise;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
@Data @Component @ConfigurationProperties(prefix="shaun.shaunrise")
public class ShaunriseProperties {
 private boolean enabled=false, testEnabled=false, animationEnabled=true;
 private String configPath="config/telegram.enc";
 @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
 private java.time.LocalDate startDate=java.time.LocalDate.of(2026,10,20);
 @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
 private java.time.LocalDate launchDate=java.time.LocalDate.of(2026,12,2);
 public void validateDates(){if(!launchDate.isAfter(startDate))throw new IllegalArgumentException("INVALID_MISSION_DATES");}
}

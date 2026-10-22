package knr.shaunmanager.common;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "shaun")
public class ShaunProperties {
    private String accessToken = "";

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String value) { accessToken = value; }
}

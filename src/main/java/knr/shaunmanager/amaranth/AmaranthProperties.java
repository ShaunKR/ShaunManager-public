package knr.shaunmanager.amaranth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "amaranth")
public class AmaranthProperties {
    private String discoveryUrl = "https://discovery.example.invalid";
    private String gatewayUrl = "https://erp.example.invalid";
    private String mobileId = "demo-company";
    private String loginTemplate = "config/amaranth-login.enc";
    private String configKey = System.getenv("SHAUN_CONFIG_KEY");
    public String getConfigKey() { return configKey; }
    public void setConfigKey(String value) { configKey = value; }
    public String getDiscoveryUrl() { return discoveryUrl; }
    public void setDiscoveryUrl(String value) { discoveryUrl = value; }
    public String getGatewayUrl() { return gatewayUrl; }
    public void setGatewayUrl(String value) { gatewayUrl = value; }
    public String getMobileId() { return mobileId; }
    public void setMobileId(String value) { mobileId = value; }
    public String getLoginTemplate() { return loginTemplate; }
    public void setLoginTemplate(String value) { loginTemplate = value; }
}

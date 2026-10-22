package knr.shaunmanager.amaranth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class AmaranthLoginServiceTest {
    @TempDir Path temp;
    final ObjectMapper mapper = new ObjectMapper();
    MockRestServiceServer server;
    AmaranthLoginService service;
    @BeforeEach void setup() throws Exception {
        Path template = temp.resolve("login.json");
        Files.write(template, ("{\"body\":{\"loginId\":\"encrypted-id\",\"passwd\":\"encrypted-secret\",\"kNum\":\"1\",\"mobileId\":\"demo-company\",\"token\":\"stale\"},"
            + "\"header\":{\"mobileId\":\"demo-company\",\"loginId\":\"tester\",\"tId\":\"old\"}}").getBytes(StandardCharsets.UTF_8));
        AmaranthProperties properties = new AmaranthProperties();
        String key = java.util.Base64.getEncoder().encodeToString(new byte[32]);
        Files.write(template, LoginConfigCrypto.encrypt(Files.readAllBytes(template), key));
        properties.setConfigKey(key);
        properties.setLoginTemplate(template.toString());
        RestTemplate rest = new RestTemplate();
        server = MockRestServiceServer.bindTo(rest).build();
        RestTemplateBuilder builder = new RestTemplateBuilder() {
            @Override public RestTemplate build() { return rest; }
            @Override public RestTemplateBuilder setConnectTimeout(java.time.Duration timeout) { return this; }
            @Override public RestTemplateBuilder setReadTimeout(java.time.Duration timeout) { return this; }
        };
        service = new AmaranthLoginService(properties, mapper, builder);
    }
    void discovery(String gateway) {
        server.expect(requestTo("https://discovery.example.invalid/proxymgw/proxymgw00A01?mobileId=demo-company"))
            .andRespond(withSuccess("{\"resultCode\":0,\"resultData\":\"" + gateway + "\"}", MediaType.APPLICATION_JSON));
    }
    void seed() {
        server.expect(requestTo("https://erp.example.invalid/get_token/?url=/proxymgw/proxymgw17A08"))
            .andRespond(withSuccess("{\"token\":\"new-seed\",\"cur_date\":\"20260919\"}", MediaType.APPLICATION_JSON));
    }
    @Test void authenticatesUsingFreshSeedAndKeepsSecretsOutOfStatus() throws Exception {
        discovery("https://erp.example.invalid"); seed();
        server.expect(requestTo("https://erp.example.invalid/proxymgw/proxymgw17A08"))
            .andExpect(method(HttpMethod.POST)).andExpect(request -> {
                JsonNode body = mapper.readTree(((MockClientHttpRequest) request).getBodyAsString());
                assertEquals("new-seed", body.path("body").path("token").asText());
                assertEquals("encrypted-secret", body.path("body").path("passwd").asText());
                assertNotEquals("old", body.path("header").path("tId").asText());
                String tid = request.getHeaders().getFirst("transaction-id");
                assertEquals(AmaranthSignature.login("new-seed", "20260919", tid, "/proxymgw/proxymgw17A08"),
                        request.getHeaders().getFirst("signature"));
            }).andRespond(withSuccess("{\"resultCode\":0,\"resultData\":{\"token\":\"session-secret\",\"hashKey\":\"key-secret\",\"groupSeq\":\"g\",\"compSeq\":\"c\",\"empSeq\":\"e\"}}", MediaType.APPLICATION_JSON));
        Map<String, Object> status = service.login();
        assertEquals(true, status.get("sessionPresent"));
        assertEquals("e", status.get("empSeq"));
        assertFalse(mapper.writeValueAsString(status).contains("secret"));
        server.verify();
    }
    @Test void rejectsUnexpectedDiscoveryDestinationBeforeSendingCredentials() {
        discovery("https://unexpected.example");
        assertEquals("DISCOVERY_MISMATCH", assertThrows(AmaranthLoginService.LoginFailure.class, () -> service.login()).getMessage());
        assertEquals(false, service.status().get("sessionPresent"));
        server.verify();
    }
    @Test void rejectsApplicationFailureEvenWithHttp200() {
        discovery("https://erp.example.invalid"); seed();
        server.expect(requestTo("https://erp.example.invalid/proxymgw/proxymgw17A08"))
            .andRespond(withSuccess("{\"resultCode\":99,\"resultMsg\":\"sensitive upstream detail\"}", MediaType.APPLICATION_JSON));
        assertEquals("LOGIN_REJECTED", assertThrows(AmaranthLoginService.LoginFailure.class, () -> service.login()).getMessage());
        assertEquals(false, service.status().get("sessionPresent"));
        server.verify();
    }
}

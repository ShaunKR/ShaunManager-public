package knr.shaunmanager.amaranth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class AmaranthLoginService {
    private static final String LOGIN_PATH = "/proxymgw/proxymgw17A08";
    private final AmaranthProperties properties;
    private final ObjectMapper mapper;
    private final RestTemplate http;
    private Session session;

    public AmaranthLoginService(AmaranthProperties properties, ObjectMapper mapper, RestTemplateBuilder builder) {
        this.properties = properties;
        this.mapper = mapper;
        this.http = builder.setConnectTimeout(java.time.Duration.ofSeconds(10))
                .setReadTimeout(java.time.Duration.ofSeconds(20)).build();
    }

    public synchronized Map<String, Object> login() {
        session = null;
        ObjectNode request = AttendanceDiagnostics.step("LOGIN_CONFIG", () -> template());
        try {
            URI discovery = UriComponentsBuilder.fromHttpUrl(properties.getDiscoveryUrl())
                    .path("/proxymgw/proxymgw00A01").queryParam("mobileId", properties.getMobileId()).build().encode().toUri();
            JsonNode discovered = http.getForObject(discovery, JsonNode.class);
            requireSuccess(discovered);
            String gateway = text(discovered, "resultData").replaceAll("/+$", "");
            if (!gateway.equals(properties.getGatewayUrl().replaceAll("/+$", "")) || !gateway.startsWith("https://")) {
                throw new LoginFailure("DISCOVERY_MISMATCH");
            }
            String tid = UUID.randomUUID().toString().replace("-", "");
            HttpHeaders seedHeaders = new HttpHeaders();
            seedHeaders.set("transaction-id", tid);
            URI seedUri = UriComponentsBuilder.fromHttpUrl(gateway).path("/get_token/")
                    .queryParam("url", LOGIN_PATH).build().encode().toUri();
            JsonNode seed = http.exchange(seedUri, HttpMethod.GET, new HttpEntity<Void>(seedHeaders), JsonNode.class).getBody();
            String token = text(seed, "token");
            String date = text(seed, "cur_date");
            ((ObjectNode) request.get("body")).put("token", token);
            ((ObjectNode) request.get("header")).put("tId", UUID.randomUUID().toString().replace("-", ""));
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("User-Agent", "mobile");
            headers.set("transaction-id", tid);
            headers.set("timestamp", Long.toString(Instant.now().getEpochSecond()));
            headers.set("token", token);
            headers.set("signature", AmaranthSignature.login(token, date, tid, LOGIN_PATH));
            JsonNode response = http.postForObject(gateway + LOGIN_PATH, new HttpEntity<JsonNode>(request, headers), JsonNode.class);
            requireSuccess(response);
            JsonNode data = response.path("resultData");
            session = new Session(text(data, "token"), text(data, "hashKey"),
                    text(data, "groupSeq"), text(data, "compSeq"), text(data, "empSeq"), gateway, (ObjectNode) request.get("header"));
            return status();
        } catch (RestClientException ex) {
            // Never expose upstream bodies, request credentials, or transport exception details.
            logTransportFailure("LOGIN", ex);
            throw new LoginFailure("UPSTREAM_REQUEST_FAILED");
        }
    }

    public synchronized Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        // A retained session is not a guarantee that the upstream token has not expired.
        result.put("sessionPresent", session != null);
        if (session != null) {
            result.put("loggedInAt", session.loggedInAt);
            result.put("groupSeq", session.groupSeq);
            result.put("compSeq", session.compSeq);
            result.put("empSeq", session.empSeq);
        }
        return result;
    }

    synchronized JsonNode authenticatedPost(String path, String protocolId, ObjectNode body) {
        if (session == null) throw new LoginFailure("LOGIN_REQUIRED");
        String tid = UUID.randomUUID().toString().replace("-", "");
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        ObjectNode header = session.header.deepCopy();
        header.put("pId", protocolId).put("tId", UUID.randomUUID().toString().replace("-", ""))
                .put("groupSeq", session.groupSeq).put("token", session.token);
        ObjectNode payload = body.deepCopy();
        payload.put("langCode", "kr").put("empSeq", session.empSeq).put("groupSeq", session.groupSeq);
        ObjectNode request = mapper.createObjectNode();
        request.set("body", payload); request.set("header", header);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", "mobile");
        headers.set("Authorization", "Bearer " + session.token);
        headers.set("transaction-id", tid); headers.set("timestamp", timestamp);
        headers.set("wehago-sign", AmaranthSignature.authenticated(session.token, tid, timestamp, path, session.signKey));
        try {
            return http.postForObject(session.gateway + path, new HttpEntity<JsonNode>(request, headers), JsonNode.class);
        } catch (RestClientException ex) {
            // The server may already have committed a write; never replay automatically.
            logTransportFailure(protocolId, ex);
            throw new LoginFailure("UPSTREAM_REQUEST_OUTCOME_UNKNOWN");
        }
    }

    private static void logTransportFailure(String stage, RestClientException ex) {
        int status = ex instanceof org.springframework.web.client.RestClientResponseException
            ? ((org.springframework.web.client.RestClientResponseException) ex).getRawStatusCode() : 0;
        Throwable cause = ex.getCause();
        org.slf4j.LoggerFactory.getLogger(AmaranthLoginService.class).warn(
            "ERP transport failed stage={} httpStatus={} exceptionType={} causeType={}",
            stage, status, ex.getClass().getSimpleName(), cause == null ? "none" : cause.getClass().getSimpleName());
    }
    void validateConfig() { template(); }
    private ObjectNode template() {
        try {
            byte[] plaintext = LoginConfigCrypto.decrypt(Files.readAllBytes(Paths.get(properties.getLoginTemplate())), properties.getConfigKey());
            JsonNode node;
            try { node = mapper.readTree(plaintext); }
            finally { java.util.Arrays.fill(plaintext, (byte) 0); }
            if (!node.isObject() || !node.path("body").isObject() || !node.path("header").isObject())
                throw new LoginFailure("INVALID_LOCAL_CONFIG");
            for (String name : new String[]{"loginId", "passwd", "kNum", "mobileId"}) text(node.path("body"), name);
            if (!properties.getMobileId().equals(node.path("body").path("mobileId").asText())
                    || !properties.getMobileId().equals(node.path("header").path("mobileId").asText()))
                throw new LoginFailure("INVALID_LOCAL_CONFIG");
            return (ObjectNode) node;
        } catch (java.io.IOException ex) {
            throw new LoginFailure("LOCAL_CONFIG_UNAVAILABLE");
        }
    }

    private static String text(JsonNode node, String name) {
        if (node == null || !node.hasNonNull(name) || node.path(name).asText().isEmpty())
            throw new LoginFailure("REQUIRED_FIELD_MISSING");
        return node.path(name).asText();
    }
    private static void requireSuccess(JsonNode response) {
        if (response == null || !response.path("resultCode").isIntegralNumber()
                || response.path("resultCode").asInt() != 0) throw new LoginFailure("LOGIN_REJECTED");
    }
    private static final class Session {
        final String token, signKey, groupSeq, compSeq, empSeq, gateway; final ObjectNode header;
        final String loggedInAt = Instant.now().toString();
        Session(String token, String signKey, String groupSeq, String compSeq, String empSeq, String gateway, ObjectNode header) {
            this.token = token; this.signKey = signKey; this.groupSeq = groupSeq;
            this.compSeq = compSeq; this.empSeq = empSeq; this.gateway = gateway; this.header = header.deepCopy();
        }
    }
    public static final class LoginFailure extends RuntimeException {
        public LoginFailure(String code) { super(code); }
    }
}

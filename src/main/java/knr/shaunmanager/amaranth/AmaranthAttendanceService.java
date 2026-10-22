package knr.shaunmanager.amaranth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AmaranthAttendanceService {
    private final AmaranthLoginService login;
    private final ObjectMapper mapper;
    private final knr.shaunmanager.calendar.LeaveService records;
    public AmaranthAttendanceService(AmaranthLoginService login, ObjectMapper mapper, knr.shaunmanager.calendar.LeaveService records) {
        this.login = login; this.mapper = mapper; this.records = records;
    }
    public synchronized Map<String, Object> today() {
        AttendanceDiagnostics.step("LOGIN", () -> login.login());
        JsonNode employee = AttendanceDiagnostics.step("EMPLOYEE_LOOKUP", () -> employee());
        return AttendanceDiagnostics.step("TODAY_READ", () -> read(employee, todayDate()));
    }
    /** Diagnostic reads only: deliberately no registration or local attendance writes. */
    public synchronized void checkReadiness(SystemCheckReport report) {
        boolean ok=report.check("CONFIG_DECRYPT",()->login.validateConfig());
        if(ok)ok=report.check("LOGIN",()->login.login());else report.skip("LOGIN");
        final JsonNode[] selected=new JsonNode[1];
        if(ok)ok=report.check("EMPLOYEE_LOOKUP",()->{
            selected[0]=employee();
            required(selected[0],"coCd");required(selected[0],"empCd");required(selected[0],"deptCd");
        });else report.skip("EMPLOYEE_LOOKUP");
        if(ok)report.check("TODAY_READ",()->read(selected[0],todayDate()));else report.skip("TODAY_READ");
    }
    public synchronized Map<String, Object> checkIn(String location) {
        return register("check-in", location);
    }
    public synchronized Map<String, Object> checkOut(String location) {
        return register("check-out", location);
    }
    private Map<String, Object> register(String action, String location) {
        if (!"check-in".equals(action) && !"check-out".equals(action))
            throw new IllegalArgumentException("INVALID_ACTION");
        if (!"외부현장".equals(location) && !"본사".equals(location))
            throw new IllegalArgumentException("INVALID_LOCATION");
        // Refresh authentication before each operation; never retry a registration.
        AttendanceDiagnostics.step("LOGIN", () -> login.login());
        JsonNode employee = AttendanceDiagnostics.step("EMPLOYEE_LOOKUP", () -> employee());
        String date = todayDate();
        Map<String, Object> before = AttendanceDiagnostics.step("BEFORE_READ", () -> read(employee, date));
        ObjectNode judge = mapper.createObjectNode();
        judge.put("coCd", required(employee, "coCd")).put("empCd", required(employee, "empCd"))
            .put("deptCd", required(employee, "deptCd")).put("attendFg", "check-in".equals(action) ? "1" : "4");
        ObjectNode body = mapper.createObjectNode();
        body.put("type", "MOBILE_FORME").put("locationCd", location); body.set("judgeData", judge);
        // Do not fabricate GPS confirmation or reuse captured IP addresses.
        JsonNode data = AttendanceDiagnostics.step("REGISTER", () -> {
            JsonNode accepted = success(login.authenticatedPost("/proxymgw/proxymgw44A112", "P1271", body), true);
            if (accepted.path("failCount").asInt(0) > 0 || accepted.path("failList").size() > 0)
                throw new AmaranthLoginService.LoginFailure("ATTENDANCE_REJECTED");
            return accepted;
        });
        if (data.path("failCount").asInt(0) > 0 || data.path("failList").size() > 0)
            throw new AmaranthLoginService.LoginFailure("ATTENDANCE_REJECTED");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", action); result.put("locationCd", location); result.put("before", before);
        result.put("registration", times(data, date)); result.put("registrationAccepted", true);
        try {
            Map<String, Object> after = AttendanceDiagnostics.step("VERIFY_READ", () -> read(employee, date));
            String field = "check-in".equals(action) ? "comeTm" : "leaveTm";
            result.put("after", after);
            result.put("timeChanged", !java.util.Objects.equals(before.get(field), after.get(field)));
            result.put("verificationSucceeded", !String.valueOf(after.get(field)).isEmpty()
                    && java.util.Objects.equals(data.path(field).asText(""), after.get(field)));
        } catch (AmaranthLoginService.LoginFailure ex) {
            result.put("verificationSucceeded", false); result.put("verificationError", ex.getMessage());
        }
        try {
            AttendanceDiagnostics.step("LOCAL_SAVE", () -> {
                records.saveRegistrationTimes(date, location, times(data, date)); return true;
            });
            result.put("localLogSaved", true);
        } catch (RuntimeException ex) {
            // ERP has accepted the write: report local persistence failure without retrying it.
            result.put("localLogSaved", false);
            result.put("localLogError", "LOCAL_RECORD_SAVE_FAILED");
        }
        org.slf4j.LoggerFactory.getLogger(getClass()).info(
            "Attendance action={} date={} accepted=true verified={} localLogSaved={}",
            action, date, result.get("verificationSucceeded"), result.get("localLogSaved"));
        return result;
    }
    private JsonNode employee() {
        Map<String, Object> session = login.status();
        if (!Boolean.TRUE.equals(session.get("sessionPresent"))) throw new AmaranthLoginService.LoginFailure("LOGIN_REQUIRED");
        ObjectNode body = mapper.createObjectNode();
        body.put("compSeq", String.valueOf(session.get("compSeq")))
            .put("mCompSeq", String.valueOf(session.get("compSeq")))
            .put("mEmpSeq", String.valueOf(session.get("empSeq"))).put("searchType", 1);
        JsonNode data = success(login.authenticatedPost("/proxymgw/proxymgw44A147", "P1315", body), false);
        if (!data.isArray()) throw new AmaranthLoginService.LoginFailure("EMPLOYEE_NOT_FOUND");
        JsonNode found = null;
        for (JsonNode item : data) {
            if (String.valueOf(session.get("empSeq")).equals(item.path("empSeq").asText())
                    && String.valueOf(session.get("compSeq")).equals(item.path("compSeq").asText())) {
                if (found != null) throw new AmaranthLoginService.LoginFailure("EMPLOYEE_AMBIGUOUS");
                found = item;
            }
        }
        if (found == null) throw new AmaranthLoginService.LoginFailure("EMPLOYEE_NOT_FOUND");
        return found;
    }
    private Map<String, Object> read(JsonNode employee, String date) {
        ObjectNode body = mapper.createObjectNode();
        body.put("coCd", required(employee, "coCd")).put("empCd", required(employee, "empCd")).put("workDt", date);
        return times(success(login.authenticatedPost("/proxymgw/proxymgw44A111", "P1270", body), true), date);
    }
    private static JsonNode success(JsonNode response, boolean nested) {
        if (response == null || !response.path("resultCode").isIntegralNumber()
                || response.path("resultCode").asInt() != 0 || !response.hasNonNull("resultData"))
            throw new AmaranthLoginService.LoginFailure("ATTENDANCE_REJECTED");
        JsonNode data = response.get("resultData");
        if (nested && !"SUCCESS".equals(data.path("resultCode").asText()))
            throw new AmaranthLoginService.LoginFailure("ATTENDANCE_REJECTED");
        return data;
    }
    private static Map<String, Object> times(JsonNode data, String date) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workDt", date); result.put("comeTm", data.path("comeTm").asText(""));
        result.put("leaveTm", data.path("leaveTm").asText("")); return result;
    }
    private static String required(JsonNode node, String field) {
        if (!node.hasNonNull(field) || node.path(field).asText().isEmpty())
            throw new AmaranthLoginService.LoginFailure("EMPLOYEE_FIELD_MISSING");
        return node.path(field).asText();
    }
    private static String todayDate() {
        return LocalDate.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.BASIC_ISO_DATE);
    }
}

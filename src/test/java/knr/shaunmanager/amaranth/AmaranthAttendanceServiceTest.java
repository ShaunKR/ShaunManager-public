package knr.shaunmanager.amaranth;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AmaranthAttendanceServiceTest {
    @Test void readinessOnlyReadsAndNeverRegistersOrSaves() throws Exception {
        when(login.authenticatedPost(eq("/proxymgw/proxymgw44A111"),eq("P1270"),any())).thenReturn(times("",""));
        SystemCheckReport report=new SystemCheckReport();service.checkReadiness(report);
        assertEquals("PASS",report.status);assertEquals(4,report.checks.size());
        verify(login).validateConfig();verify(login).login();
        verify(login,never()).authenticatedPost(eq("/proxymgw/proxymgw44A112"),anyString(),any());
        verifyNoInteractions(records);
    }
    @Test void missingKeySkipsAllErpRequests() {
        doThrow(new AmaranthLoginService.LoginFailure("CONFIG_KEY_MISSING")).when(login).validateConfig();
        SystemCheckReport report=new SystemCheckReport();service.checkReadiness(report);
        assertEquals("FAIL",report.status);assertEquals("CONFIG_KEY_MISSING",report.checks.get(0).get("code"));
        assertEquals("SKIPPED",report.checks.get(3).get("status"));
        verify(login,never()).login();verify(login,never()).authenticatedPost(anyString(),anyString(),any());
        verifyNoInteractions(records);
    }
    ObjectMapper mapper = new ObjectMapper();
    AmaranthLoginService login;
    AmaranthAttendanceService service;
    knr.shaunmanager.calendar.LeaveService records;
    JsonNode json(String text) throws Exception { return mapper.readTree(text); }
    @BeforeEach void setup() throws Exception {
        login = mock(AmaranthLoginService.class);
        Map<String,Object> status = new HashMap<>();
        status.put("sessionPresent",true);status.put("empSeq","e");status.put("compSeq","c");
        when(login.status()).thenReturn(status);
        when(login.authenticatedPost(eq("/proxymgw/proxymgw44A147"),eq("P1315"),any()))
            .thenReturn(json("{\"resultCode\":0,\"resultData\":[{\"empSeq\":\"e\",\"compSeq\":\"c\",\"empCd\":\"employee\",\"coCd\":\"company\",\"deptCd\":\"dept\"}]}"));
        records = mock(knr.shaunmanager.calendar.LeaveService.class);
        service = new AmaranthAttendanceService(login, mapper, records);
    }
    JsonNode times(String come,String leave) throws Exception {
        return json("{\"resultCode\":0,\"resultData\":{\"resultCode\":\"SUCCESS\",\"comeTm\":\""+come+"\",\"leaveTm\":\""+leave+"\"}}");
    }
    @ParameterizedTest @CsvSource({"check-in,1,new,old","check-out,4,old,new"})
    void registersCorrectActionAndVerifiesPersistedTime(String action,String flag,String come,String leave) throws Exception {
        when(login.authenticatedPost(eq("/proxymgw/proxymgw44A111"),eq("P1270"),any()))
            .thenReturn(times("old","old"),times(come,leave));
        when(login.authenticatedPost(eq("/proxymgw/proxymgw44A112"),eq("P1271"),any())).thenReturn(times(come,leave));
        Map<String,Object> result=("check-in".equals(action) ? service.checkIn("외부현장") : service.checkOut("외부현장"));
        assertEquals(true,result.get("verificationSucceeded"));assertEquals(true,result.get("timeChanged"));
        verify(records).saveRegistrationTimes(anyString(),eq("외부현장"),anyMap());
        org.mockito.InOrder order = inOrder(login);
        order.verify(login).login();
        order.verify(login).status();
        order.verify(login).authenticatedPost(eq("/proxymgw/proxymgw44A147"),eq("P1315"),any());
        ArgumentCaptor<ObjectNode> request=ArgumentCaptor.forClass(ObjectNode.class);
        verify(login,org.mockito.Mockito.times(1)).authenticatedPost(eq("/proxymgw/proxymgw44A112"),eq("P1271"),request.capture());
        assertEquals(flag,request.getValue().path("judgeData").path("attendFg").asText());
        assertEquals("employee",request.getValue().path("judgeData").path("empCd").asText());
        assertFalse(request.getValue().has("authType"));assertFalse(request.getValue().has("clientIp"));
    }
    @Test void rejectsNestedFailure() throws Exception {
        when(login.authenticatedPost(eq("/proxymgw/proxymgw44A111"),eq("P1270"),any())).thenReturn(times("old","old"));
        when(login.authenticatedPost(eq("/proxymgw/proxymgw44A112"),eq("P1271"),any()))
            .thenReturn(json("{\"resultCode\":0,\"resultData\":{\"resultCode\":\"FAIL\"}}"));
        assertThrows(AmaranthLoginService.LoginFailure.class,()->service.checkIn("외부현장"));
        verify(login,org.mockito.Mockito.times(1)).authenticatedPost(eq("/proxymgw/proxymgw44A112"),eq("P1271"),any());
    }
    @Test void reportsAcceptedWriteWhenVerificationFailsWithoutReplaying() throws Exception {
        when(login.authenticatedPost(eq("/proxymgw/proxymgw44A111"),eq("P1270"),any()))
            .thenReturn(times("old","old")).thenThrow(new AmaranthLoginService.LoginFailure("UPSTREAM_REQUEST_OUTCOME_UNKNOWN"));
        when(login.authenticatedPost(eq("/proxymgw/proxymgw44A112"),eq("P1271"),any())).thenReturn(times("old","new"));
        Map<String,Object> result=service.checkOut("외부현장");
        assertEquals(true,result.get("registrationAccepted"));assertEquals(false,result.get("verificationSucceeded"));
        verify(login,org.mockito.Mockito.times(1)).authenticatedPost(eq("/proxymgw/proxymgw44A112"),eq("P1271"),any());
    }
    @Test void rejectsInvalidLocationBeforeRequestingUpstream() {
        assertThrows(IllegalArgumentException.class,()->service.checkIn("unknown"));
        verify(login,never()).authenticatedPost(anyString(),anyString(),any());
    }
    @Test void loginFailurePreventsAllAttendanceRequests() {
        when(login.login()).thenThrow(new AmaranthLoginService.LoginFailure("LOGIN_REJECTED"));
        assertThrows(AmaranthLoginService.LoginFailure.class, () -> service.checkIn("외부현장"));
        assertThrows(AmaranthLoginService.LoginFailure.class, () -> service.checkOut("외부현장"));
        verify(login, never()).authenticatedPost(anyString(), anyString(), any());
    }}

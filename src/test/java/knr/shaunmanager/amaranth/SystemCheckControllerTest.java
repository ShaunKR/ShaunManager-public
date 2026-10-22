package knr.shaunmanager.amaranth;
import knr.shaunmanager.calendar.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class SystemCheckControllerTest {
 @Test void databaseFailureIsSanitizedAndErpCheckStillRuns() {
  AmaranthAttendanceService attendance=mock(AmaranthAttendanceService.class);
  AttendanceScheduler scheduler=mock(AttendanceScheduler.class);
  AttendancePlanRepository plans=mock(AttendancePlanRepository.class);
  LeaveRepository leaves=mock(LeaveRepository.class);AttendanceLogRepository logs=mock(AttendanceLogRepository.class);
  when(leaves.findById(anyString())).thenThrow(new IllegalStateException("password=secret"));
  org.springframework.http.ResponseEntity<SystemCheckReport> result=new SystemCheckController(attendance,scheduler,plans,leaves,logs).check();
  assertEquals(503,result.getStatusCodeValue());assertEquals("FAIL",result.getBody().status);
  assertFalse(result.getBody().checks.toString().contains("secret"));
  verify(attendance).checkReadiness(any());verifyNoMoreInteractions(attendance);
  assertEquals("no-store",result.getHeaders().getFirst("Cache-Control"));
 }
}

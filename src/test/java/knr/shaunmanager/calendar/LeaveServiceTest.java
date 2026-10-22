package knr.shaunmanager.calendar;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
class LeaveServiceTest {
 @Test void normalWorkLocationAndValidation() {
  LeaveRepository repo=mock(LeaveRepository.class);when(repo.findById("20260921")).thenReturn(Optional.empty());
  LeaveService service=new LeaveService(repo,mock(AttendanceLogRepository.class));service.saveLeave("20260921","work","본사");
  org.mockito.ArgumentCaptor<Leave> cap=org.mockito.ArgumentCaptor.forClass(Leave.class);verify(repo).save(cap.capture());assertEquals("본사",cap.getValue().getLocationCd());assertEquals("work",cap.getValue().getLeaveType());
  assertThrows(IllegalArgumentException.class,()->service.saveLeave("20260230","am","본사"));
  assertThrows(IllegalArgumentException.class,()->service.saveLeave("20260921","other","본사"));
  assertThrows(IllegalArgumentException.class,()->service.saveLeave("20260921","am","unknown"));
 }
 @Test void savesBothResponseTimesAndPreservesExistingWhenBlank() {
  AttendanceLogRepository repo=mock(AttendanceLogRepository.class);
  AttendanceLog record=new AttendanceLog("20260921","외부현장");
  when(repo.findById("20260921")).thenReturn(Optional.of(record));
  LeaveService service=new LeaveService(mock(LeaveRepository.class),repo);
  Map<String,Object> times=new HashMap<>();
  times.put("comeTm","202609210849"); times.put("leaveTm","20260921183012");
  service.saveRegistrationTimes("20260921","외부현장",times);
  assertEquals("084900",record.getCheckInTm()); assertEquals("183012",record.getCheckOutTm());
  times.put("comeTm",""); times.put("leaveTm","202609211834");
  service.saveRegistrationTimes("20260921","외부현장",times);
  assertEquals("084900",record.getCheckInTm()); assertEquals("183400",record.getCheckOutTm());
  when(repo.findAll()).thenReturn(Collections.singletonList(record));
  assertEquals("084900",service.findAllLogs().get("20260921").get("checkInTm"));
 }
 @Test void ignoresInvalidOrDifferentDayTimes() {
  AttendanceLogRepository repo=mock(AttendanceLogRepository.class);
  LeaveService service=new LeaveService(mock(LeaveRepository.class),repo);
  Map<String,Object> times=new HashMap<>();
  times.put("comeTm","202609200849"); times.put("leaveTm","20260921240000");
  service.saveRegistrationTimes("20260921","외부현장",times);
  verifyNoInteractions(repo);
 }
 @Test void shiftPersistenceDefaultsAndValidation() {
  LeaveRepository repo=mock(LeaveRepository.class);
  Leave record=new Leave("20260921","work","본사");
  assertEquals("day",record.getWorkShift());
  assertEquals("day",new AttendancePlan("20260921").getWorkShift());
  when(repo.findById("20260921")).thenReturn(Optional.of(record));
  when(repo.findAll()).thenReturn(Collections.singletonList(record));
  LeaveService service=new LeaveService(repo,mock(AttendanceLogRepository.class));
  service.saveLeave("20260921","work","본사","night");
  assertEquals("night",service.findAllLeaves().get("20260921").get("workShift"));
  assertThrows(IllegalArgumentException.class,()->service.saveLeave("20260921","am","본사","night"));
  assertThrows(IllegalArgumentException.class,()->service.saveLeave("20260921","pm","본사","night"));
  assertThrows(IllegalArgumentException.class,()->service.saveLeave("20260921","work","본사","invalid"));
  service.saveLeave("20260921","annual","본사","night");assertEquals("annual",record.getLeaveType());
  service.saveLeave("20260921","work","본사");assertEquals("day",record.getWorkShift());
 }

 @Test void skipSettingsRoundTripAndOlderClientsPreserveChoices() {
  LeaveRepository repo=mock(LeaveRepository.class);Leave record=new Leave("20260921","work","본사");
  assertFalse(record.isSkipCheckIn());assertFalse(record.isSkipCheckOut());
  when(repo.findById("20260921")).thenReturn(Optional.of(record));when(repo.findAll()).thenReturn(Collections.singletonList(record));
  LeaveService service=new LeaveService(repo,mock(AttendanceLogRepository.class));
  service.saveLeave("20260921","work","본사","day",true,false);
  assertEquals("true",service.findAllLeaves().get("20260921").get("skipCheckIn"));assertEquals("false",service.findAllLeaves().get("20260921").get("skipCheckOut"));
  service.saveLeave("20260921","work","본사","day");assertTrue(record.isSkipCheckIn());
  service.saveLeave("20260921","work","본사","day",false,true);assertFalse(record.isSkipCheckIn());assertTrue(record.isSkipCheckOut());
 }
}

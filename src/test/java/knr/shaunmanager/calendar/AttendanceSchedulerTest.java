package knr.shaunmanager.calendar;
import knr.shaunmanager.amaranth.AmaranthAttendanceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class AttendanceSchedulerTest {
 AmaranthAttendanceService api; LeaveService leaves; AttendancePlanRepository repo; AttendanceScheduler scheduler; Map<String,AttendancePlan> stored;
 @BeforeEach void setup() throws Exception {
  api=mock(AmaranthAttendanceService.class);leaves=mock(LeaveService.class);repo=mock(AttendancePlanRepository.class);stored=new HashMap<>();
  when(repo.findById(anyString())).thenAnswer(x->Optional.ofNullable(stored.get(x.getArgument(0))));
  when(repo.saveAndFlush(any())).thenAnswer(x->{AttendancePlan p=x.getArgument(0);stored.put(p.getWorkDt(),p);return p;});
  when(leaves.getOrCreateLog(anyString(),anyString())).thenAnswer(x->new AttendanceLog(x.getArgument(0),x.getArgument(1)));
  when(api.today()).thenReturn(new HashMap<>());
  Map<String,Object> result=new HashMap<>(),after=new HashMap<>();result.put("verificationSucceeded",true);
  after.put("comeTm","202609210820");after.put("leaveTm","202609211830");result.put("after",after);
  when(api.checkIn(anyString())).thenReturn(result);when(api.checkOut(anyString())).thenReturn(result);
  scheduler=new AttendanceScheduler(api,leaves,repo,new WorkCalendar(new ObjectMapper()),true);
 }
 @ParameterizedTest @CsvSource({"work,08:15,08:25,18:25,18:35","am,13:45,13:55,18:25,18:35","pm,08:15,08:25,14:15,14:25"})
 void persistsSingleTarget(String type,String inMin,String inMax,String outMin,String outMax) {
  when(leaves.getLeave("20260921")).thenReturn(new Leave("20260921",type,"본사"));
  scheduler.runAt(LocalDateTime.of(2026,9,21,0,0));AttendancePlan p=stored.get("20260921");String in=p.getInTarget(),out=p.getOutTarget();
  assertFalse(LocalTime.parse(in).isBefore(LocalTime.parse(inMin)));assertFalse(LocalTime.parse(in).isAfter(LocalTime.parse(inMax)));
  assertFalse(LocalTime.parse(out).isBefore(LocalTime.parse(outMin)));assertFalse(LocalTime.parse(out).isAfter(LocalTime.parse(outMax)));
  scheduler.runAt(LocalDateTime.of(2026,9,21,0,1));assertEquals(in,p.getInTarget());assertEquals(out,p.getOutTarget());assertEquals("본사",p.getLocationCd());verifyNoInteractions(api);
 }
 @Test void noReplayAfterRestart() throws Exception {
  scheduler.runAt(LocalDateTime.of(2026,9,21,0,0));AttendancePlan p=stored.get("20260921");LocalDateTime due=LocalDate.of(2026,9,21).atTime(LocalTime.parse(p.getInTarget()));
  scheduler.runAt(due);scheduler.runAt(due.plusSeconds(10));new AttendanceScheduler(api,leaves,repo,new WorkCalendar(new ObjectMapper()),true).runAt(due.plusSeconds(20));
  verify(api,times(1)).checkIn("외부현장");assertEquals("DONE",p.getInState());
 }
 @Test void failureNotRetried() {
  when(api.checkIn(anyString())).thenThrow(new IllegalStateException("timeout"));scheduler.runAt(LocalDateTime.of(2026,9,21,0,0));AttendancePlan p=stored.get("20260921");
  LocalDateTime due=LocalDate.of(2026,9,21).atTime(LocalTime.parse(p.getInTarget()));scheduler.runAt(due);scheduler.runAt(due.plusMinutes(1));
  verify(api,times(1)).checkIn(anyString());assertEquals("REVIEW_REQUIRED",p.getInState());
 }
 @Test void skipExistingRecord() {
  when(api.today()).thenReturn(Collections.singletonMap("leaveTm","202609211700"));scheduler.runAt(LocalDateTime.of(2026,9,21,0,0));AttendancePlan p=stored.get("20260921");
  scheduler.runAt(LocalDate.of(2026,9,21).atTime(LocalTime.parse(p.getOutTarget())));verify(api,never()).checkOut(anyString());assertEquals("SKIPPED",p.getOutState());
 }
 @Test void annualWeekendsAndHolidaysExcluded() {
  when(leaves.getLeave("20260921")).thenReturn(new Leave("20260921","annual","외부현장"));
  scheduler.runAt(LocalDateTime.of(2026,9,21,8,20));scheduler.runAt(LocalDateTime.of(2026,9,26,8,20));scheduler.runAt(LocalDateTime.of(2026,9,24,8,20));verifyNoInteractions(api);assertTrue(stored.isEmpty());
 }
 @Test void lateStartupDoesNotRegister() {
  scheduler.runAt(LocalDateTime.of(2026,9,21,20,0));assertEquals("MISSED",stored.get("20260921").getInState());assertEquals("MISSED",stored.get("20260921").getOutState());verifyNoInteractions(api);
 }
 @Test void nightTargetsAndCutoff() {
  Leave night=new Leave("20260921","work","본사");night.setWorkShift("night");
  when(leaves.getLeave("20260921")).thenReturn(night);
  scheduler.runAt(LocalDateTime.of(2026,9,21,0,0));AttendancePlan p=stored.get("20260921");
  assertEquals("night",p.getWorkShift());
  assertTrue(LocalTime.parse(p.getInTarget()).compareTo(LocalTime.of(13,45))>=0);
  assertTrue(LocalTime.parse(p.getInTarget()).compareTo(LocalTime.of(13,55))<=0);
  assertTrue(LocalTime.parse(p.getOutTarget()).compareTo(LocalTime.of(23,0))>=0);
  assertTrue(LocalTime.parse(p.getOutTarget()).compareTo(LocalTime.of(23,5))<=0);
  p.setInState("DONE");
  scheduler.runAt(LocalDateTime.of(2026,9,21,22,59));verifyNoInteractions(api);
  scheduler.runAt(LocalDateTime.of(2026,9,21,23,5));verify(api).checkOut("본사");
 }
 @Test void nightLateStartupDoesNotRegister() {
  Leave night=new Leave("20260921","work","본사");night.setWorkShift("night");
  when(leaves.getLeave("20260921")).thenReturn(night);
  scheduler.runAt(LocalDateTime.of(2026,9,21,23,6));
  assertEquals("MISSED",stored.get("20260921").getOutState());verifyNoInteractions(api);
 }
 @Test void shiftChangeReschedulesOnlyPendingTargets() {
  scheduler.runAt(LocalDateTime.of(2026,9,21,0,0));AttendancePlan p=stored.get("20260921");
  String previousIn=p.getInTarget();p.setInState("REVIEW_REQUIRED");
  Leave night=new Leave("20260921","work","본사");night.setWorkShift("night");
  when(leaves.getLeave("20260921")).thenReturn(night);
  scheduler.runAt(LocalDateTime.of(2026,9,21,0,1));
  assertEquals(previousIn,p.getInTarget());assertEquals("REVIEW_REQUIRED",p.getInState());
  assertTrue(LocalTime.parse(p.getOutTarget()).getHour()==23);
  String out=p.getOutTarget();night.setLocationCd("외부현장");
  scheduler.runAt(LocalDateTime.of(2026,9,21,0,2));assertEquals(out,p.getOutTarget());verifyNoInteractions(api);
 }

 @ParameterizedTest @CsvSource({"true,false","false,true","true,true"})
 void skipActionsIndependentlyAndNeverNotify(boolean skipIn,boolean skipOut) {
  Leave setting=new Leave("20260921","work","본사");setting.setSkipCheckIn(skipIn);setting.setSkipCheckOut(skipOut);
  when(leaves.getLeave("20260921")).thenReturn(setting);
  knr.shaunmanager.shaunrise.ShaunriseService mission=mock(knr.shaunmanager.shaunrise.ShaunriseService.class);scheduler.setMission(mission);
  scheduler.runAt(LocalDateTime.of(2026,9,21,0,0));AttendancePlan plan=stored.get("20260921");
  scheduler.runAt(LocalDate.of(2026,9,21).atTime(LocalTime.parse(plan.getInTarget())));
  scheduler.runAt(LocalDate.of(2026,9,21).atTime(LocalTime.parse(plan.getOutTarget())));
  verify(api,times(skipIn?0:1)).checkIn("본사");verify(api,times(skipOut?0:1)).checkOut("본사");
  if(skipIn){assertEquals("PENDING",plan.getInState());verify(mission,never()).record(any(),eq(true),anyBoolean(),anyString());}
  if(skipOut){assertEquals("PENDING",plan.getOutState());verify(mission,never()).record(any(),eq(false),anyBoolean(),anyString());}
 }
 @Test void reenableBeforeCutoffExecutesOnce() {
  Leave setting=new Leave("20260921","work","본사");setting.setSkipCheckIn(true);
  when(leaves.getLeave("20260921")).thenReturn(setting);
  scheduler.runAt(LocalDateTime.of(2026,9,21,0,0));AttendancePlan plan=stored.get("20260921");
  LocalDateTime due=LocalDate.of(2026,9,21).atTime(LocalTime.parse(plan.getInTarget()));scheduler.runAt(due);verifyNoInteractions(api);
  setting.setSkipCheckIn(false);scheduler.runAt(due);scheduler.runAt(due);verify(api,times(1)).checkIn("본사");
 }
 @Test void reenableAfterCutoffDoesNotCatchUp() {
  Leave setting=new Leave("20260921","work","본사");setting.setSkipCheckIn(true);when(leaves.getLeave("20260921")).thenReturn(setting);
  scheduler.runAt(LocalDateTime.of(2026,9,21,8,26));setting.setSkipCheckIn(false);scheduler.runAt(LocalDateTime.of(2026,9,21,8,27));
  assertEquals("MISSED",stored.get("20260921").getInState());verifyNoInteractions(api);
 }
}

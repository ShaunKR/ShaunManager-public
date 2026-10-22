package knr.shaunmanager.shaunrise;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
class MissionJournalTest {
 @Test void everyDayHasDistinctDeterministicEntriesAndFitsTelegram(){
  ShaunriseProperties props=new ShaunriseProperties();RocketRenderer renderer=new RocketRenderer(props);
  Set<String> incoming=new HashSet<>(),outgoing=new HashSet<>();int assembled=0;
  for(LocalDate d=props.getStartDate();!d.isAfter(props.getLaunchDate());d=d.plusDays(1)){
   incoming.add(MissionJournal.entry(d,props.getStartDate(),props.getLaunchDate(),true));
   outgoing.add(MissionJournal.entry(d,props.getStartDate(),props.getLaunchDate(),false));
   for(boolean in:new boolean[]{true,false}){
    String message=renderer.message(d,in,true,"082000",assembled);
    assertTrue(message.length()<4096);assertTrue(message.contains(RocketRenderer.SLOGAN));
    assertEquals(message,new RocketRenderer(props).message(d,in,true,"082000",assembled));
    assertEquals(!in,message.contains("[조립 현황]"));
   }
   assembled=renderer.target(d);
  }
  assertEquals(44,incoming.size());assertEquals(44,outgoing.size());
 }
 @Test void catchupReportsOnlyActualAssemblyAndNextPart(){
  ShaunriseProperties props=new ShaunriseProperties();RocketRenderer renderer=new RocketRenderer(props);
  String in=renderer.message(props.getLaunchDate(),true,true,"082000",2);
  assertTrue(in.contains("10개 부품 중 2개"));assertTrue(in.contains("NEXT: 보조 엔진 장착"));
  String out=renderer.message(props.getLaunchDate(),false,true,"183000",2);
  assertTrue(out.contains("10개 부품 중 10개"));assertFalse(out.contains("[ ]"));
  assertTrue(out.contains("[x] 최종 점검 장치 · 오늘 장착"));
 }
 @Test void beforeAndAfterMissionAndLaunch(){
  ShaunriseProperties props=new ShaunriseProperties();RocketRenderer renderer=new RocketRenderer(props);
  assertTrue(renderer.message(props.getStartDate().minusDays(1),true,true,"082000",0).contains("작전 준비"));
  assertTrue(renderer.message(props.getLaunchDate().plusDays(1),false,true,"183000",10).contains("작전 이후"));
  assertTrue(renderer.launch(3).contains(RocketRenderer.SLOGAN));assertTrue(renderer.launch(3).contains("MISSION COMPLETE"));
 }
}

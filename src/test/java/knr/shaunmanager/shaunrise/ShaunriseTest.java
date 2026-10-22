package knr.shaunmanager.shaunrise;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
class ShaunriseTest {
 ShaunriseProperties props;RocketRenderer renderer;MissionEventRepository repo;Map<String,MissionEvent> db;ShaunriseService service;
 @BeforeEach void setup(){props=new ShaunriseProperties();props.setEnabled(true);renderer=new RocketRenderer(props);repo=mock(MissionEventRepository.class);db=new LinkedHashMap<>();
 when(repo.existsById(anyString())).thenAnswer(i->db.containsKey(i.getArgument(0)));
 when(repo.saveAndFlush(any())).thenAnswer(i->{MissionEvent e=i.getArgument(0);db.put(e.getId(),e);return e;});
 when(repo.findByAction(anyString())).thenAnswer(i->{List<MissionEvent> out=new ArrayList<>();db.values().stream().filter(e->e.getAction().equals(i.getArgument(0))).forEach(out::add);return out;});
 service=new ShaunriseService(repo,renderer,props);}
 @Test void dateBoundariesAndMobileArt(){assertEquals(0,renderer.target(LocalDate.of(2026,10,19)));assertEquals(1,renderer.target(props.getStartDate()));assertEquals(10,renderer.target(props.getLaunchDate()));assertEquals(10,renderer.target(props.getLaunchDate().plusDays(1)));String art=renderer.art(10);assertTrue(art.split("\n").length>=30);assertTrue(art.split("\n").length<=40);for(String line:art.split("\n"))assertTrue(line.length()<=29);assertFalse(renderer.art(0).contains("FUEL"));assertTrue(renderer.launch(3).length()<4096);}
 @Test void duplicateRestartAndCatchup(){LocalDate date=LocalDate.of(2026,11,20);service.record(date,false,true,"183000");new ShaunriseService(repo,renderer,props).record(date,false,true,"183000");assertEquals(1,db.size());assertEquals(renderer.target(date),service.assembled(date));}
 @Test void errorsDoNotAssembleOrLaunch(){service.record(props.getLaunchDate(),false,false,"");assertEquals(0,service.assembled(props.getLaunchDate()));assertFalse(db.containsKey("SHAUN-01-LAUNCH"));service.record(props.getLaunchDate(),false,true,"230000");service.record(props.getLaunchDate(),false,true,"230000");assertEquals(3,db.size());assertTrue(db.containsKey("SHAUN-01-LAUNCH"));}
 @Test void storageFailureDoesNotEscape(){when(repo.existsById(anyString())).thenThrow(new RuntimeException());assertDoesNotThrow(()->service.record(props.getStartDate(),true,true,"082000"));}
 @Test void disabledDoesNothing(){props.setEnabled(false);service.record(props.getStartDate(),true,true,"");assertTrue(db.isEmpty());}
 @Test void failedSendNeverAutomaticallyRetries(){MissionEvent event=new MissionEvent("a","2026-10-20","IN",0,"test");when(repo.findByStateOrderByIdAsc("PENDING")).thenAnswer(i->"PENDING".equals(event.getState())?Collections.singletonList(event):Collections.emptyList());TelegramClient client=mock(TelegramClient.class);when(client.send(anyString(),any())).thenThrow(new IllegalStateException());NotificationDispatcher dispatcher=new NotificationDispatcher(repo,client,renderer,props);dispatcher.dispatch();dispatcher.dispatch();assertEquals("REVIEW_REQUIRED",event.getState());verify(client,times(1)).send(anyString(),any());dispatcher.stop();}
 @Test void animationUsesSameMessage(){MissionEvent event=new MissionEvent("launch","2026-12-02","LAUNCH",10,"test");when(repo.findByStateOrderByIdAsc("PENDING")).thenAnswer(i->"PENDING".equals(event.getState())?Collections.singletonList(event):Collections.emptyList());TelegramClient client=mock(TelegramClient.class);when(client.send(anyString(),any())).thenReturn(42L);NotificationDispatcher dispatcher=new NotificationDispatcher(repo,client,renderer,props);for(int i=0;i<4;i++)dispatcher.dispatch();assertEquals("SENT",event.getState());verify(client,times(1)).send(anyString(),isNull());verify(client,times(3)).send(anyString(),eq(42L));dispatcher.stop();}
}

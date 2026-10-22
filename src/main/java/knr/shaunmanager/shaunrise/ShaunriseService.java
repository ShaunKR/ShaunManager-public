package knr.shaunmanager.shaunrise;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;
@Service
public class ShaunriseService {
 private final MissionEventRepository repo;private final RocketRenderer renderer;private final ShaunriseProperties props;
 public ShaunriseService(MissionEventRepository repo,RocketRenderer renderer,ShaunriseProperties props){this.repo=repo;this.renderer=renderer;this.props=props;}
 public synchronized void record(LocalDate date,boolean in,boolean success,String time){
  if(!props.isEnabled())return;
  try {
   String action=in?"IN":"OUT",key=date+"-"+action+(success?"-OK":"-ERROR");
   if(!repo.existsById(key))repo.saveAndFlush(new MissionEvent(key,date.toString(),success?action:"ERROR",success&&!in?renderer.target(date):0,renderer.message(date,in,success,time,assembled(date))));
   if(success&&!in&&date.equals(props.getLaunchDate()))launch();
  }catch(Exception ex){org.slf4j.LoggerFactory.getLogger(getClass()).warn("SHAUNRISE event persistence failed; attendance result retained");}
 }
 int assembled(LocalDate date){return repo.findByAction("OUT").stream().filter(e->!LocalDate.parse(e.getWorkDate()).isAfter(date)).mapToInt(MissionEvent::getStage).max().orElse(0);}
 public synchronized void launch(){if(!repo.existsById("SHAUN-01-LAUNCH"))repo.saveAndFlush(new MissionEvent("SHAUN-01-LAUNCH",props.getLaunchDate().toString(),"LAUNCH",10,renderer.launch(0)));}
 public Map<String,Object> preview(String action,LocalDate date){
  if(!Arrays.asList("in","out","error","launch").contains(action))throw new IllegalArgumentException("INVALID_ACTION");
  String text="[TEST]\n"+(action.equals("launch")?renderer.launch(3):renderer.message(date,action.equals("in"),!action.equals("error"),"TEST 08:20",Math.max(0,renderer.target(date)-1)));
  return Collections.singletonMap("message",text);
 }
 public void test(String action,LocalDate date){String id="TEST-"+UUID.randomUUID();repo.saveAndFlush(new MissionEvent(id,date.toString(),"TEST",0,(String)preview(action,date).get("message")));}
}

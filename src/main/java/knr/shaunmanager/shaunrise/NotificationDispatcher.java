package knr.shaunmanager.shaunrise;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
@Component
public class NotificationDispatcher {
 private final MissionEventRepository repo;private final TelegramClient client;private final RocketRenderer renderer;private final ShaunriseProperties props;
 public NotificationDispatcher(MissionEventRepository repo,TelegramClient client,RocketRenderer renderer,ShaunriseProperties props){this.repo=repo;this.client=client;this.renderer=renderer;this.props=props;}
 private final java.util.concurrent.ScheduledExecutorService worker=java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"shaunrise-telegram");t.setDaemon(true);return t;});
 @javax.annotation.PostConstruct public void start(){worker.scheduleWithFixedDelay(()->{try{dispatch();}catch(Exception ex){org.slf4j.LoggerFactory.getLogger(getClass()).warn("SHAUNRISE dispatch storage failure");}},3,3,java.util.concurrent.TimeUnit.SECONDS);}
 @javax.annotation.PreDestroy public void stop(){worker.shutdownNow();}
 public synchronized void dispatch(){
  if(!props.isEnabled())return;
  for(MissionEvent event:repo.findByStateOrderByIdAsc("PENDING")) {
   // Persist claim before HTTP. Unknown delivery is never resent automatically after restart.
   event.setState("SENDING");repo.saveAndFlush(event);
   try {
    boolean launch="LAUNCH".equals(event.getAction());
    String text=launch?renderer.launch(props.isAnimationEnabled()?event.getFrame():3):event.getMessage();
    event.setMessageId(client.send(text,event.getMessageId()));
    if(launch&&props.isAnimationEnabled()&&event.getFrame()<3){event.setFrame(event.getFrame()+1);event.setState("PENDING");}
    else event.setState("SENT");
   }catch(Exception ex){event.setState("REVIEW_REQUIRED");}
   repo.saveAndFlush(event);
   break;
  }
 }
}

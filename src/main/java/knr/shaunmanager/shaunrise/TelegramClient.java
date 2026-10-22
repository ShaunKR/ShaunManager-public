package knr.shaunmanager.shaunrise;
import com.fasterxml.jackson.databind.*;
import knr.shaunmanager.amaranth.LoginConfigCrypto;
import org.springframework.stereotype.Component;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;
import java.nio.file.*;
import java.util.*;
@Component
public class TelegramClient {
 private final ShaunriseProperties props;private final ObjectMapper mapper;private final RestTemplate http;
 public TelegramClient(ShaunriseProperties props,ObjectMapper mapper,RestTemplateBuilder builder){this.props=props;this.mapper=mapper;this.http=builder.setConnectTimeout(java.time.Duration.ofSeconds(3)).setReadTimeout(java.time.Duration.ofSeconds(5)).build();}
 public long send(String text,Long messageId){
  byte[] plain=null;
  try {
   plain=LoginConfigCrypto.decrypt(Files.readAllBytes(Paths.get(props.getConfigPath())),System.getenv("SHAUN_CONFIG_KEY"));
   JsonNode cfg=mapper.readTree(plain);String token=cfg.path("botToken").asText(),chat=cfg.path("chatId").asText();
   if(!token.matches("[0-9]+:[A-Za-z0-9_-]+")||!chat.matches("-?[0-9]+"))throw new IllegalArgumentException();
   Map<String,Object> body=new LinkedHashMap<>();body.put("chat_id",chat);body.put("text",text);body.put("parse_mode","HTML");
   if(messageId!=null)body.put("message_id",messageId);
   JsonNode response=http.postForObject("https://api.telegram.org/bot"+token+"/"+(messageId==null?"sendMessage":"editMessageText"),body,JsonNode.class);
   if(response==null||!response.path("ok").asBoolean())throw new IllegalStateException();
   long id=response.path("result").path("message_id").asLong(0);
   if(id==0)throw new IllegalStateException();return id;
  }catch(Exception ex){throw new IllegalStateException("TELEGRAM_DELIVERY_UNCONFIRMED");}
  finally{if(plain!=null)Arrays.fill(plain,(byte)0);}
 }
}

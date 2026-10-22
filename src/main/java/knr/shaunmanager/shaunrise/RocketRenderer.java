package knr.shaunmanager.shaunrise;
import org.springframework.stereotype.Component;
import java.time.*;
import java.time.temporal.ChronoUnit;
@Component
public class RocketRenderer {
 public static final String SLOGAN="Every day, one step closer to freedom.\n매일, 자유에 한 걸음 더 가까이.";
 public static final String[] PARTS={"발사대","메인 엔진","보조 엔진","연료탱크","로켓 하부 동체","로켓 상부 동체","좌우 날개","조종석","노즈콘","최종 점검 장치"};
 private final ShaunriseProperties props;
 public RocketRenderer(ShaunriseProperties props){this.props=props;}
 public int target(LocalDate date){props.validateDates();long total=ChronoUnit.DAYS.between(props.getStartDate(),props.getLaunchDate());long day=ChronoUnit.DAYS.between(props.getStartDate(),date);if(day<0)return 0;return (int)Math.max(0,Math.min(10,1+day*9/total));}
 public String art(int stage){
  StringBuilder b=new StringBuilder();
  if(stage>=10)b.append("        [ READY ]\n");
  if(stage>=9)b.append("           /\\\n          /  \\\n         /    \\\n        /______\\\n");
  if(stage>=8)b.append("        | .--. |\n        | |  | |\n        | '--' |\n");
  if(stage>=6)b.append("        |------|\n        |  S   |\n        |  H   |\n        |  A   |\n        |  U   |\n        |  N   |\n        |  01  |\n");
  if(stage>=5)b.append("        |------|\n        |      |\n        |      |\n        |      |\n");
  if(stage>=4)b.append("        | FUEL |\n        | ==== |\n        | ==== |\n        |______|\n");
  if(stage>=7)b.append("       /|      |\\\n      / |      | \\\n     /__|______|__\\\n");
  if(stage>=3)b.append("       [|      |]\n       /|      |\\\n");
  if(stage>=2)b.append("        |______|\n         / || \\\n        /__||__\\\n");
  if(stage>=1)b.append("    =================\n    |   LAUNCH PAD  |\n    =================\n");
  return b.length()==0?"    [ EMPTY HANGAR ]\n":b.toString();
 }
 public String message(LocalDate date,boolean in,boolean success,String time,int assembled){
  long d=ChronoUnit.DAYS.between(date,props.getLaunchDate());String day=d==0?"D-DAY":d>0?"D-"+d:"D+"+(-d);
  String text="OPERATION SHAUNRISE\nPILOT: SHAUN / SHAUN-01\nESCAPE FROM HELL\n"+date+" · "+day+"\n"+(in?"출근":"퇴근")+(success?" 성공 · "+escape(displayTime(time)):" 실패 / ERP 기록 확인 필요")+"\n";
  if(!success)return text;
  assembled=Math.max(0,Math.min(10,assembled));
  int target=target(date);String task=assembled<target?String.join(", ",java.util.Arrays.copyOfRange(PARTS,assembled,target)):"배선·체결·점검";
  if(in)text+="오늘 운송: "+task+"\n<pre>  _______   [PARTS]\n | SHAUN |__|____|\n '--O---------O--'\n</pre>";
  else text+="오늘 조립 완료: "+task+"\n";
  int progress=in?assembled:Math.max(assembled,target);
  long missionDay=ChronoUnit.DAYS.between(props.getStartDate(),date)+1;
  text+="MISSION DAY "+Math.max(0,missionDay)+"\n";
  text+="[현재 공정] "+MissionJournal.phase(progress)+"\n전체 10개 부품 중 "+progress+"개 장착 완료\n";
  if(!in){
   text+="\n[조립 현황]\n";
   for(int i=0;i<PARTS.length;i++)text+=(i<progress?"[x] ":"[ ] ")+PARTS[i]+(i>=assembled&&i<progress?" · 오늘 장착":"")+"\n";
  }
  text+="NEXT: "+(progress<10?PARTS[progress]+" 장착":"발사 시퀀스")+"\n";
  text+="\n[오늘의 작전일지 · 가상 임무]\n"+MissionJournal.entry(date,props.getStartDate(),props.getLaunchDate(),in)+"\n\n";
  return text+"조립 진행률: "+progress*10+"%\n<pre>"+escape(art(progress))+"</pre>"+SLOGAN;
 }
 private String displayTime(String time){if(time!=null&&time.matches("[0-9]{12}([0-9]{2})?"))return time.substring(8,10)+":"+time.substring(10,12);if(time!=null&&time.matches("[0-9]{6}"))return time.substring(0,2)+":"+time.substring(2,4);return time==null?"":time;}
 public String launch(int frame){return "OPERATION SHAUNRISE\nMISSION COMPLETE\nSHAUN-01 LIFTOFF\n"+SLOGAN+"\n<pre>"+escape((frame<3?"          T-"+(3-frame)+"\n":"          LIFTOFF!\n")+art(10)+(frame>=3?"         * || *\n        ** || **\n       *** || ***\n":""))+"</pre>";}
 public static String escape(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");}
}

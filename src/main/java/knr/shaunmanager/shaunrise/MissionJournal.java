package knr.shaunmanager.shaunrise;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Fictional mission entries, selected deterministically by calendar day. */
public final class MissionJournal {
 private static final List<String[]> ENTRIES = load();
 private MissionJournal() {}
 private static List<String[]> load() {
  List<String[]> entries=new ArrayList<>();
  java.io.InputStream stream=MissionJournal.class.getResourceAsStream("/shaunrise/journal.txt");
  if(stream==null)throw new IllegalStateException("MISSION_JOURNAL_MISSING");
  try(Scanner scanner=new Scanner(stream,StandardCharsets.UTF_8.name())) {
   while(scanner.hasNextLine()) {
    String[] fields=scanner.nextLine().split("\\|",-1);
    if(fields.length!=3)throw new IllegalStateException("INVALID_MISSION_JOURNAL");
    entries.add(fields);
   }
  }
  if(entries.size()!=44)throw new IllegalStateException("INVALID_MISSION_JOURNAL_LENGTH");
  return Collections.unmodifiableList(entries);
 }
 public static String entry(LocalDate date,LocalDate start,LocalDate launch,boolean in) {
  if(date.isBefore(start))return "작전 준비 · 격납고가 첫 임무를 기다립니다.";
  if(date.isAfter(launch))return "작전 이후 · 오늘도 자유를 향한 여정은 이어집니다.";
  long duration=ChronoUnit.DAYS.between(start,launch);
  int index=(int)(ChronoUnit.DAYS.between(start,date)*43/duration);
  String[] entry=ENTRIES.get(index);
  return entry[0]+"\n"+entry[in?1:2];
 }
 public static String phase(int stage) {
  if(stage==0)return "작전 준비";
  if(stage==1)return "기반 구축";
  if(stage<=3)return "추진부 구축";
  if(stage==4)return "연료 계통 조립";
  if(stage<=6)return "동체 구축";
  if(stage==7)return "비행 구조 조립";
  if(stage==8)return "파일럿 공간 구축";
  if(stage==9)return "외형 완성 · 최종 점검 대기";
  return "최종 점검 완료 · 발사 준비";
 }
}

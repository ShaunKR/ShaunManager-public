package knr.shaunmanager.amaranth;

import java.util.*;

public class SystemCheckReport {
    public final String checkedAt = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Seoul")).toString();
    public final List<Map<String,Object>> checks = new ArrayList<>();
    public final boolean registrationTested = false;
    public final boolean databaseWriteTested = false;
    public String status = "PASS";
    public Map<String,Object> todayPlan;
    public boolean check(String stage, Runnable action) {
        long start=System.nanoTime();
        try {
            AttendanceDiagnostics.step("CHECK_"+stage,()->{action.run();return true;});
            add(stage,"PASS",null,(System.nanoTime()-start)/1000000);return true;
        } catch(RuntimeException ex) {
            add(stage,"FAIL",AttendanceDiagnostics.code(ex),(System.nanoTime()-start)/1000000);return false;
        }
    }
    public void add(String stage,String result,String code,long elapsed) {
        Map<String,Object> item=new LinkedHashMap<>();
        item.put("stage",stage);item.put("status",result);item.put("code",code);item.put("elapsedMs",elapsed);
        checks.add(item);
        if("FAIL".equals(result))status="FAIL";
        else if("WARN".equals(result)&&!"FAIL".equals(status))status="WARN";
    }
    public void skip(String stage) {add(stage,"SKIPPED","DEPENDENCY_FAILED",0);}
}

package knr.shaunmanager.shaunrise;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
@SpringBootTest(properties={"shaun.access-token=integration-test-token","spring.datasource.url=jdbc:h2:mem:shaunrise;DB_CLOSE_DELAY=-1","shaun.scheduler.enabled=false","shaun.shaunrise.enabled=false","shaun.shaunrise.test-enabled=true"})
@AutoConfigureMockMvc
class ShaunriseIntegrationTest {
 @Autowired MockMvc mvc;@Autowired MissionEventRepository repo;@Autowired ShaunriseProperties props;@Autowired RocketRenderer renderer;
 @Test void previewRequiresTokenAndDoesNotChangeAssembly() throws Exception {
  mvc.perform(get("/api/shaunrise/preview/in").param("date","2026-10-20")).andExpect(status().isForbidden());
  mvc.perform(get("/api/shaunrise/preview/in").param("date","2026-10-20").header("X-Access-Token","integration-test-token")).andExpect(status().isOk()).andExpect(jsonPath("$.message").exists());
  assertEquals(0,repo.count());
 }
 @Test void databaseStateSurvivesServiceRecreation(){
  ShaunriseProperties local=new ShaunriseProperties();local.setEnabled(true);
  ShaunriseService first=new ShaunriseService(repo,renderer,local);LocalDate date=LocalDate.of(2026,11,20);
  first.record(date,false,true,"20261120183000");ShaunriseService restarted=new ShaunriseService(repo,renderer,local);restarted.record(date,false,true,"20261120183000");
  assertEquals(1,repo.count());assertEquals(renderer.target(date),restarted.assembled(date));repo.deleteAll();
 }

 @Autowired knr.shaunmanager.calendar.LeaveService leaves;
 @Test void skipSettingsPersistThroughApiAndReset() throws Exception {
  mvc.perform(post("/api/calendar/leaves").header("X-Access-Token","integration-test-token").contentType("application/json")
    .content("{\"workDt\":\"20261021\",\"leaveType\":\"work\",\"locationCd\":\"본사\",\"skipCheckIn\":true,\"skipCheckOut\":false}"))
    .andExpect(status().isOk());
  assertTrue(leaves.getLeave("20261021").isSkipCheckIn());assertFalse(leaves.getLeave("20261021").isSkipCheckOut());
  mvc.perform(get("/api/calendar/leaves").header("X-Access-Token","integration-test-token")).andExpect(jsonPath("$['20261021'].skipCheckIn").value("true"));
  mvc.perform(delete("/api/calendar/leaves/20261021").header("X-Access-Token","integration-test-token")).andExpect(status().isOk());assertNull(leaves.getLeave("20261021"));
 }
}

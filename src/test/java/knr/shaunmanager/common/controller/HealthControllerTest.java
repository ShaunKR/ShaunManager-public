package knr.shaunmanager.common.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HealthController.class)
@org.springframework.context.annotation.Import({knr.shaunmanager.common.SecurityConfig.class, knr.shaunmanager.common.ShaunProperties.class})
@org.springframework.test.context.TestPropertySource(properties = "shaun.access-token=test-token")
class HealthControllerTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void returnsApplicationHealth() throws Exception {
        mvc.perform(get("/api/health").header("X-Access-Token", "test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application").value("ShaunManager"))
                .andExpect(jsonPath("$.status").value("UP"));
    }
}

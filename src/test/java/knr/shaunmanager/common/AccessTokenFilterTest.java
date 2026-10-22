package knr.shaunmanager.common;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;

class AccessTokenFilterTest {
    @Test void emptyConfiguredTokenNeverAuthenticates() throws Exception {
        SecurityContextHolder.clearContext();
        try {
            ShaunProperties props = new ShaunProperties();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Access-Token", "");
            new AccessTokenFilter(props).doFilter(request, new MockHttpServletResponse(), (req, res) -> {});
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        } finally { SecurityContextHolder.clearContext(); }
    }
}

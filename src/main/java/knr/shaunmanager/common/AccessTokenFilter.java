package knr.shaunmanager.common;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

public class AccessTokenFilter extends OncePerRequestFilter {

    private final ShaunProperties props;

    public AccessTokenFilter(ShaunProperties props) {
        this.props = props;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String token = request.getHeader("X-Access-Token");
        if (token == null) token = request.getParameter("token");

        if (props.getAccessToken() != null && !props.getAccessToken().trim().isEmpty()
                && props.getAccessToken().equals(token)) {
            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                    "shaun", null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        chain.doFilter(request, response);
    }
}

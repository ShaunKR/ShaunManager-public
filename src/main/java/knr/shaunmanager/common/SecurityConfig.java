package knr.shaunmanager.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ShaunProperties props;

    public SecurityConfig(ShaunProperties props) {
        this.props = props;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .addFilterBefore(new AccessTokenFilter(props),
                             UsernamePasswordAuthenticationFilter.class)
            .authorizeRequests()
                .antMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
            .and()
            .exceptionHandling()
                .authenticationEntryPoint((request, response, ex) -> {
                    response.setContentType("text/html;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write(FORBIDDEN_HTML);
                });

        return http.build();
    }

    private static final String FORBIDDEN_HTML =
        "<!DOCTYPE html>" +
        "<html lang=\"ko\"><head><meta charset=\"UTF-8\"/><title>접근 거부</title>" +
        "<style>" +
        "body{font-family:'Segoe UI',sans-serif;background:#f5f6fa;" +
        "display:flex;align-items:center;justify-content:center;height:100vh;margin:0;}" +
        ".box{background:#fff;border-radius:12px;padding:40px 48px;" +
        "text-align:center;box-shadow:0 2px 12px rgba(0,0,0,.08);}" +
        "h2{color:#E24B4A;margin-bottom:8px;}p{color:#888;font-size:14px;}" +
        "</style></head><body>" +
        "<div class=\"box\"><h2>\uD83D\uDD12 접근 거부</h2><p>올바른 토큰이 필요합니다.</p></div>" +
        "</body></html>";
}

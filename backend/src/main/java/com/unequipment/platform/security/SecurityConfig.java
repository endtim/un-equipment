package com.unequipment.platform.security;

import com.unequipment.platform.modules.system.entity.SysUser;
import com.unequipment.platform.modules.system.repository.SysUserRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthTokenFilter authTokenFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .exceptionHandling()
            .authenticationEntryPoint(unauthorizedEntryPoint())
            .accessDeniedHandler((request, response, accessDeniedException) ->
                writeError(response, HttpStatus.FORBIDDEN.value(), 403, "forbidden"))
            .and()
            .authorizeHttpRequests()
            .antMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
            .antMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register").permitAll()
            .antMatchers(HttpMethod.GET, "/api/instruments/**", "/api/notices/**", "/api/help-docs/**", "/api/stats/**").permitAll()
            .antMatchers("/api/admin/system/users/**")
            .hasAnyRole("ADMIN", "DEPT_MANAGER")
            .antMatchers(HttpMethod.GET, "/api/admin/system/departments")
            .hasAnyRole("ADMIN", "DEPT_MANAGER")
            .antMatchers(HttpMethod.GET, "/api/admin/system/roles")
            .hasAnyRole("ADMIN", "DEPT_MANAGER")
            .antMatchers("/api/admin/system/**", "/api/admin/logs/**", "/api/admin/content/**")
            .hasRole("ADMIN")
            .antMatchers("/api/admin/finance/**")
            .hasAnyRole("ADMIN", "DEPT_MANAGER")
            .antMatchers("/api/admin/settlements/**")
            .hasAnyRole("ADMIN", "DEPT_MANAGER")
            .antMatchers("/api/admin/instruments/**", "/api/admin/orders/**")
            .hasAnyRole("ADMIN", "INSTRUMENT_OWNER", "DEPT_MANAGER")
            .antMatchers("/api/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated()
            .and()
            .addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) ->
            writeError(response, HttpStatus.UNAUTHORIZED.value(), 401, "unauthorized");
    }

    private static void writeError(HttpServletResponse response, int httpStatus, int code, String msg) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(httpStatus);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format("{\"code\":%d,\"msg\":\"%s\",\"data\":null}", code, msg));
    }

    @Component
    @RequiredArgsConstructor
    public static class AuthTokenFilter extends OncePerRequestFilter {

        private final TokenService tokenService;
        private final SysUserRepository userRepository;

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
            String auth = request.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                String token = auth.substring(7);
                try {
                    Long userId = tokenService.parseUserId(token);
                    SysUser user = userRepository.findById(userId);
                    if (user == null || !"ENABLED".equalsIgnoreCase(user.getStatus())) {
                        SecurityContextHolder.clearContext();
                        writeError(response, HttpStatus.UNAUTHORIZED.value(), 401, "unauthorized");
                        return;
                    }
                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                            user.getUsername(),
                            null,
                            AuthorityUtils.createAuthorityList("ROLE_" + (user.getPrimaryRoleCode() == null ? "INTERNAL_USER" : user.getPrimaryRoleCode())));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    request.setAttribute("currentUser", user);
                } catch (Exception ignored) {
                    SecurityContextHolder.clearContext();
                    writeError(response, HttpStatus.UNAUTHORIZED.value(), 401, "invalid or expired token");
                    return;
                }
            }
            filterChain.doFilter(request, response);
        }
    }
}

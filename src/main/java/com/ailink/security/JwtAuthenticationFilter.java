package com.ailink.security;

import com.ailink.config.JwtProperties;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String LOGIN_TOKEN_KEY_PREFIX = "login:token:";

    private final JwtProperties jwtProperties;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate stringRedisTemplate;

    public JwtAuthenticationFilter(JwtProperties jwtProperties,
                                   JwtTokenProvider jwtTokenProvider,
                                   StringRedisTemplate stringRedisTemplate) {
        this.jwtProperties = jwtProperties;
        this.jwtTokenProvider = jwtTokenProvider;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(jwtProperties.getHeaderName());
        if (header != null && header.startsWith(jwtProperties.getTokenPrefix() + " ")) {
            String token = header.substring((jwtProperties.getTokenPrefix() + " ").length());
            try {
                String tokenAlive = stringRedisTemplate.opsForValue().get(LOGIN_TOKEN_KEY_PREFIX + token);
                if (tokenAlive == null) {
                    filterChain.doFilter(request, response);
                    return;
                }
                Claims claims = jwtTokenProvider.parseClaims(token);
                String userId = claims.getSubject();
                String role = String.valueOf(claims.get("role"));
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}

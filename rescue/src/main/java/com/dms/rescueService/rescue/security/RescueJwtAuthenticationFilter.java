package com.dms.rescueService.rescue.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class RescueJwtAuthenticationFilter extends OncePerRequestFilter {
    @Value("${application.security.jwt.secret-key}") private String secret;
    @Override protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                               @NonNull FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = Jwts.parser().verifyWith(key()).build().parseSignedClaims(header.substring(7)).getPayload();
                String id = claims.get("userId", String.class), role = claims.get("role", String.class);
                if (id != null && role != null) {
                    var auth = new UsernamePasswordAuthenticationToken(UUID.fromString(id), null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role.replace("ROLE_", ""))));
                    Map<String, Object> details = new HashMap<>(claims); auth.setDetails(details);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception ignored) { SecurityContextHolder.clearContext(); }
        }
        chain.doFilter(request, response);
    }
    private SecretKey key() { return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)); }
}

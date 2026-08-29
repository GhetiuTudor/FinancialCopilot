package com.fincopilot.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

//extrage jwt din fiecare request, ruleaza inainte de controller 

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // scoate 'Bearer '
            String token = authHeader.substring(7);
            Claims claims = jwtService.validateAndParse(token);

            if (claims != null) {
                AuthPrincipal principal = jwtService.toPrincipal(claims);

                // credentials null pt ca tokenul  fost deja validat 
                //creaza o instanta de Authentication, interfata folosita de spring pt a reprezenta useri logati 
                var auth = new UsernamePasswordAuthenticationToken(principal, null,List.of(new SimpleGrantedAuthority(principal.role())));

                //@PreAuthorize("hasAuthority('OWNER')") - pentru role check in controller daca e nevoie 

                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        // ca sa nu opreasca requestul - > continua cu controller
        filterChain.doFilter(request, response);
    }
}

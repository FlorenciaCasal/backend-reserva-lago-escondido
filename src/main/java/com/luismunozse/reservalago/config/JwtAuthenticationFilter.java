package com.luismunozse.reservalago.config;

import com.luismunozse.reservalago.service.JwtService;
import com.luismunozse.reservalago.service.UploadGrantService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UploadGrantService uploadGrantService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            @Lazy UserDetailsService userDetailsService,
            UploadGrantService uploadGrantService
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.uploadGrantService = uploadGrantService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // Si no hay header Authorization o no empieza con Bearer, continuar
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extraer el token
        jwt = authHeader.substring(7);

        if (jwt.startsWith("up_")) {
            uploadGrantService.consume(jwt, uploadKind(request))
                    .ifPresent(grant -> {
                        var authorities = grant.authorities().stream()
                                .map(SimpleGrantedAuthority::new)
                                .toList();
                        var authentication = new UsernamePasswordAuthenticationToken(
                                grant.username(), null, authorities);
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            userEmail = jwtService.extractUsername(jwt);

            // Si hay un username y no hay autenticación previa en el contexto
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                // Validar token
                if (jwtService.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Si hay error al procesar el token, simplemente continuamos sin autenticar
            logger.error("Error procesando JWT: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private com.luismunozse.reservalago.model.MediaAssetKind uploadKind(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.endsWith("/images")) return com.luismunozse.reservalago.model.MediaAssetKind.IMAGE;
        if (path.endsWith("/documents")) return com.luismunozse.reservalago.model.MediaAssetKind.DOCUMENT;
        if (path.endsWith("/videos")) return com.luismunozse.reservalago.model.MediaAssetKind.VIDEO;
        return null;
    }
}

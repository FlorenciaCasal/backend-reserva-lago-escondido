package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.model.MediaAssetKind;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UploadGrantService {

    private static final Duration GRANT_LIFETIME = Duration.ofMinutes(2);
    private static final String TOKEN_PREFIX = "up_";

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, UploadGrant> grants = new ConcurrentHashMap<>();

    public UploadGrant issue(Authentication authentication, MediaAssetKind kind) {
        if (kind == null || authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("Tipo de upload o autenticacion invalida");
        }

        purgeExpired();
        Instant expiresAt = Instant.now().plus(GRANT_LIFETIME);
        String token = TOKEN_PREFIX + randomToken();
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        UploadGrant grant = new UploadGrant(token, authentication.getName(), authorities, kind, expiresAt);
        grants.put(token, grant);
        return grant;
    }

    public Optional<UploadGrant> consume(String token, MediaAssetKind kind) {
        if (token == null || kind == null || !token.startsWith(TOKEN_PREFIX)) {
            return Optional.empty();
        }

        UploadGrant grant = grants.get(token);
        if (grant == null || grant.expiresAt().isBefore(Instant.now()) || grant.kind() != kind) {
            return Optional.empty();
        }
        if (!grants.remove(token, grant)) {
            return Optional.empty();
        }
        return Optional.of(grant);
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        grants.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    public record UploadGrant(
            String token,
            String username,
            List<String> authorities,
            MediaAssetKind kind,
            Instant expiresAt
    ) {
    }
}

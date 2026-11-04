package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.model.MediaAssetKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadGrantServiceTest {

    @Mock
    private org.springframework.security.core.Authentication authentication;

    @Test
    void grantIsScopedToKindAndCanOnlyBeConsumedOnce() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("admin@example.com");
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();

        UploadGrantService service = new UploadGrantService();
        UploadGrantService.UploadGrant grant = service.issue(authentication, MediaAssetKind.IMAGE);

        assertThat(service.consume(grant.token(), MediaAssetKind.DOCUMENT)).isEmpty();
        assertThat(service.consume(grant.token(), MediaAssetKind.IMAGE)).isPresent();
        assertThat(service.consume(grant.token(), MediaAssetKind.IMAGE)).isEmpty();
    }

    @Test
    void validGrantReturnsTheOriginalPrincipalAndScope() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("manager@example.com");
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))).when(authentication).getAuthorities();

        UploadGrantService service = new UploadGrantService();
        UploadGrantService.UploadGrant grant = service.issue(authentication, MediaAssetKind.VIDEO);

        var consumed = service.consume(grant.token(), MediaAssetKind.VIDEO);

        assertThat(consumed).isPresent();
        assertThat(consumed.orElseThrow().username()).isEqualTo("manager@example.com");
        assertThat(consumed.orElseThrow().authorities()).containsExactly("ROLE_MANAGER");
        assertThat(consumed.orElseThrow().kind()).isEqualTo(MediaAssetKind.VIDEO);
    }
}

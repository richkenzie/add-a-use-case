package team.projectpulse.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import team.projectpulse.user.UserRepository;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * FR-SEC-active-account at the unit level: which tokens the converter lets through. The integration test
 * {@link ActiveAccountIntegrationTest} covers how a refusal reaches the caller.
 */
@ExtendWith(MockitoExtension.class)
class ActiveAccountJwtAuthenticationConverterTest {

    @Mock
    UserRepository userRepository;

    ActiveAccountJwtAuthenticationConverter converter;


    @BeforeEach
    void setUp() {
        // The same delegate SecurityConfiguration builds: the "authorities" claim, no prefix
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("authorities");
        authoritiesConverter.setAuthorityPrefix("");
        JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();
        delegate.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

        this.converter = new ActiveAccountJwtAuthenticationConverter(delegate, this.userRepository);
    }

    private static Jwt.Builder token() {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("j.smith@abc.edu")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("authorities", "ROLE_student");
    }

    @Test
    void testActiveAccountIsAuthenticatedWithTheTokensAuthorities() {
        // Given: JwtProvider writes userId as an Integer; Nimbus decodes it as a Long
        Jwt jwt = token().claim("userId", 3L).build();
        given(this.userRepository.existsByIdAndEnabledTrue(3)).willReturn(true);

        // When
        AbstractAuthenticationToken authentication = this.converter.convert(jwt);

        // Then
        assertThat(authentication.getName()).isEqualTo("j.smith@abc.edu");
        // contains, not containsExactly: Spring Security 7 also adds its own FACTOR_BEARER authority
        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority).contains("ROLE_student");
    }

    @Test
    void testDeactivatedOrDeletedAccountIsRefused() {
        // Given: no active account has this id, whether it was deactivated or deleted
        Jwt jwt = token().claim("userId", 3L).build();
        given(this.userRepository.existsByIdAndEnabledTrue(3)).willReturn(false);

        // When
        Throwable throwable = catchThrowable(() -> this.converter.convert(jwt));

        // Then
        assertThat(throwable).isInstanceOf(DisabledException.class);
    }

    @Test
    void testTokenWithoutUserIdIsRefused() {
        // Given
        Jwt jwt = token().build();

        // When
        Throwable throwable = catchThrowable(() -> this.converter.convert(jwt));

        // Then
        assertThat(throwable).isInstanceOf(DisabledException.class);
        verify(this.userRepository, never()).existsByIdAndEnabledTrue(anyInt());
    }

    @Test
    void testTokenWhoseUserIdIsNotANumberIsRefused() {
        // Given
        Jwt jwt = token().claim("userId", "3").build();

        // When
        Throwable throwable = catchThrowable(() -> this.converter.convert(jwt));

        // Then
        assertThat(throwable).isInstanceOf(DisabledException.class);
        verify(this.userRepository, never()).existsByIdAndEnabledTrue(anyInt());
    }

}

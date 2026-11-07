package team.projectpulse.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import team.projectpulse.user.UserRepository;

/**
 * The resource server's JWT converter: refuses a token whose account is deactivated or no longer exists
 * (FR-SEC-active-account), then builds the authentication exactly as the wrapped converter does.
 *
 * <p><strong>Why every request reads the account.</strong> A JWT stays valid for two hours after it is issued,
 * and its signature cannot tell that the account was deactivated in the meantime. Checking only at login would
 * let a deactivated student keep submitting weekly activity reports and peer evaluations until her token
 * expires. So the status is re-read on every authenticated request: one indexed read by primary key.
 *
 * <p><strong>How the refusal reaches the caller.</strong> {@link DisabledException} is an
 * {@code AuthenticationException}, so the bearer-token filter hands it to
 * {@link CustomBearerTokenAuthenticationEntryPoint}, and {@code ExceptionHandlerAdvice}'s
 * {@code AccountStatusException} handler answers {@code 401} "User account is abnormal.", the same response a
 * deactivated login gets.
 *
 * <p>It fails closed: a token with no {@code userId} claim, or one that is not a number, is refused too.
 * {@link JwtProvider} always writes the claim, so only a token this application did not issue lacks it.
 */
public class ActiveAccountJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    static final String USER_ID_CLAIM = "userId";

    private final JwtAuthenticationConverter delegate;

    private final UserRepository userRepository;


    public ActiveAccountJwtAuthenticationConverter(JwtAuthenticationConverter delegate, UserRepository userRepository) {
        this.delegate = delegate;
        this.userRepository = userRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        if (!(jwt.getClaims().get(USER_ID_CLAIM) instanceof Number userId)) {
            throw new DisabledException("The access token names no user account.");
        }
        if (!this.userRepository.existsByIdAndEnabledTrue(userId.intValue())) {
            throw new DisabledException("The user account is deactivated or no longer exists.");
        }
        return this.delegate.convert(jwt);
    }

}

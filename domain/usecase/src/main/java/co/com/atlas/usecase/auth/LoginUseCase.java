package co.com.atlas.usecase.auth;

import co.com.atlas.model.auth.AuthCredentials;
import co.com.atlas.model.auth.AuthToken;
import co.com.atlas.model.auth.AuthUser;
import co.com.atlas.model.auth.gateways.AuthUserRepository;
import co.com.atlas.model.auth.gateways.JwtTokenGateway;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Caso de uso para el login de usuarios.
 */
@RequiredArgsConstructor
public class LoginUseCase {
    
    private final AuthUserRepository authUserRepository;
    private final JwtTokenGateway jwtTokenGateway;

    public Mono<AuthToken> execute(AuthCredentials credentials) {
        return authUserRepository.findByEmail(credentials.getEmail())
                .switchIfEmpty(Mono.error(new AuthenticationException("Usuario no encontrado")))
                .flatMap(user -> validateUserAndPassword(user, credentials.getPassword()))
                .flatMap(user -> authUserRepository.updateLastLogin(user.getId())
                        .then(jwtTokenGateway.generateTokenPair(user)));
    }

    private Mono<AuthUser> validateUserAndPassword(AuthUser user, String password) {
        if (!user.isActive()) {
            return Mono.error(new AuthenticationException("Usuario inactivo"));
        }
        return authUserRepository.validatePassword(password, user.getPasswordHash())
                .flatMap(isValid -> {
                    if (Boolean.TRUE.equals(isValid)) {
                        return Mono.just(user);
                    }
                    return Mono.error(new AuthenticationException("Credenciales inválidas"));
                });
    }
}

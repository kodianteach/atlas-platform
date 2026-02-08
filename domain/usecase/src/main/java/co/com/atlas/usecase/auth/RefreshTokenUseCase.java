package co.com.atlas.usecase.auth;

import co.com.atlas.model.auth.AuthToken;
import co.com.atlas.model.auth.gateways.JwtTokenGateway;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Caso de uso para refrescar el token de acceso.
 */
@RequiredArgsConstructor
public class RefreshTokenUseCase {
    
    private final JwtTokenGateway jwtTokenGateway;

    public Mono<AuthToken> execute(String refreshToken) {
        return jwtTokenGateway.validateToken(refreshToken)
                .flatMap(isValid -> {
                    if (Boolean.TRUE.equals(isValid)) {
                        return jwtTokenGateway.refreshToken(refreshToken);
                    }
                    return Mono.error(new AuthenticationException("Token de refresco inválido"));
                });
    }
}

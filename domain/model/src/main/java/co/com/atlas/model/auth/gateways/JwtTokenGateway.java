package co.com.atlas.model.auth.gateways;

import co.com.atlas.model.auth.AuthToken;
import co.com.atlas.model.auth.AuthUser;
import reactor.core.publisher.Mono;

public interface JwtTokenGateway {
    
    Mono<String> generateAccessToken(AuthUser user);
    
    Mono<String> generateRefreshToken(AuthUser user);
    
    Mono<AuthToken> generateTokenPair(AuthUser user);
    
    Mono<Boolean> validateToken(String token);
    
    Mono<String> extractUserId(String token);
    
    Mono<String> extractRole(String token);
    
    Mono<AuthToken> refreshToken(String refreshToken);
}

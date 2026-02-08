package co.com.atlas.usecase.auth;

import co.com.atlas.model.auth.AuthUser;
import co.com.atlas.model.auth.gateways.AuthUserRepository;
import co.com.atlas.model.common.DuplicateException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Caso de uso para el registro de nuevos usuarios.
 */
@RequiredArgsConstructor
public class RegisterUserUseCase {
    
    private final AuthUserRepository authUserRepository;
    
    /**
     * Registra un nuevo usuario en el sistema.
     *
     * @param user Datos del usuario a registrar
     * @return Usuario registrado
     */
    public Mono<AuthUser> execute(AuthUser user) {
        return validateEmailUnique(user.getEmail())
                .then(Mono.defer(() -> {
                    AuthUser newUser = user.toBuilder()
                            .active(true)
                            .build();
                    return authUserRepository.save(newUser);
                }));
    }
    
    private Mono<Void> validateEmailUnique(String email) {
        return authUserRepository.findByEmail(email)
                .flatMap(existing -> Mono.<Void>error(
                        new DuplicateException("Usuario", "email", email)))
                .switchIfEmpty(Mono.empty())
                .then();
    }
}

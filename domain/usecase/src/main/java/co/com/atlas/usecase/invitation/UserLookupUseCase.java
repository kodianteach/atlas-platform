package co.com.atlas.usecase.invitation;

import co.com.atlas.model.auth.AuthUser;
import co.com.atlas.model.auth.gateways.AuthUserRepository;
import co.com.atlas.model.unit.Unit;
import co.com.atlas.model.unit.gateways.UnitRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Use case for user lookup and unit search operations.
 * Supports autocompletion in registration forms.
 */
@RequiredArgsConstructor
public class UserLookupUseCase {

    private final AuthUserRepository authUserRepository;
    private final UnitRepository unitRepository;

    /**
     * Looks up a user by document type and number.
     * @return Mono with the user if found, empty otherwise
     */
    public Mono<AuthUser> lookupByDocument(String documentType, String documentNumber) {
        return authUserRepository.findByDocumentTypeAndNumber(documentType, documentNumber);
    }

    /**
     * Looks up a user by email.
     * @return Mono with the user if found, empty otherwise
     */
    public Mono<AuthUser> lookupByEmail(String email) {
        return authUserRepository.findByEmail(email);
    }

    /**
     * Searches units by code prefix for autocomplete.
     * @return Flux of matching units (max 20)
     */
    public Flux<Unit> searchUnits(Long organizationId, String prefix) {
        return unitRepository.searchByOrganizationIdAndCodePrefix(organizationId, prefix);
    }
}

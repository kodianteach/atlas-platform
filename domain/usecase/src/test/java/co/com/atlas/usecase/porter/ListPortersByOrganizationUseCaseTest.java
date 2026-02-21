package co.com.atlas.usecase.porter;

import co.com.atlas.model.porter.Porter;
import co.com.atlas.model.porter.PorterType;
import co.com.atlas.model.porter.gateways.PorterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListPortersByOrganizationUseCaseTest {

    @Mock
    private PorterRepository porterRepository;

    private ListPortersByOrganizationUseCase useCase;

    private static final Long ORG_ID = 100L;

    @BeforeEach
    void setUp() {
        useCase = new ListPortersByOrganizationUseCase(porterRepository);
    }

    @Test
    void shouldReturnPortersOfCurrentOrganization() {
        Porter porter1 = Porter.builder().id(1L).names("Portería A")
                .organizationId(ORG_ID).porterType(PorterType.PORTERO_GENERAL).build();
        Porter porter2 = Porter.builder().id(2L).names("Portería B")
                .organizationId(ORG_ID).porterType(PorterType.PORTERO_DELIVERY).build();

        when(porterRepository.findByOrganizationId(ORG_ID)).thenReturn(Flux.just(porter1, porter2));

        StepVerifier.create(useCase.execute(ORG_ID))
                .assertNext(p -> assertThat(p.getNames()).isEqualTo("Portería A"))
                .assertNext(p -> assertThat(p.getNames()).isEqualTo("Portería B"))
                .verifyComplete();

        verify(porterRepository).findByOrganizationId(ORG_ID);
    }

    @Test
    void shouldReturnEmptyFluxWhenNoPorters() {
        when(porterRepository.findByOrganizationId(ORG_ID)).thenReturn(Flux.empty());

        StepVerifier.create(useCase.execute(ORG_ID))
                .verifyComplete();
    }
}

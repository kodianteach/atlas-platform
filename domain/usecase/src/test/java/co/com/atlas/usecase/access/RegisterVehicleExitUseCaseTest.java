package co.com.atlas.usecase.access;

import co.com.atlas.model.access.AccessAction;
import co.com.atlas.model.access.AccessEvent;
import co.com.atlas.model.access.ScanResult;
import co.com.atlas.model.access.gateways.AccessEventRepository;
import co.com.atlas.model.common.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterVehicleExitUseCaseTest {

    @Mock private AccessEventRepository accessEventRepository;

    private RegisterVehicleExitUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegisterVehicleExitUseCase(accessEventRepository);
    }

    @Test
    void shouldRegisterVehicleExitSuccessfully() {
        when(accessEventRepository.save(any(AccessEvent.class)))
                .thenAnswer(inv -> Mono.just(((AccessEvent) inv.getArgument(0)).toBuilder().id(1L).build()));

        StepVerifier.create(useCase.execute("ABC123", "Juan Perez", 10L, 1L, "device-001"))
                .assertNext(event -> {
                    assertThat(event.getId()).isEqualTo(1L);
                    assertThat(event.getAction()).isEqualTo(AccessAction.EXIT);
                    assertThat(event.getScanResult()).isEqualTo(ScanResult.VALID);
                    assertThat(event.getVehiclePlate()).isEqualTo("ABC123");
                    assertThat(event.getPersonName()).isEqualTo("Juan Perez");
                })
                .verifyComplete();
    }

    @Test
    void shouldRejectEmptyPlate() {
        StepVerifier.create(useCase.execute("", "Juan Perez", 10L, 1L, "device-001"))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && e.getMessage().contains("placa"))
                .verify();
    }

    @Test
    void shouldRejectNullPlate() {
        StepVerifier.create(useCase.execute(null, "Juan Perez", 10L, 1L, "device-001"))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && e.getMessage().contains("placa"))
                .verify();
    }

    @Test
    void shouldRejectEmptyPersonName() {
        StepVerifier.create(useCase.execute("ABC123", "", 10L, 1L, "device-001"))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && e.getMessage().contains("nombre"))
                .verify();
    }
}

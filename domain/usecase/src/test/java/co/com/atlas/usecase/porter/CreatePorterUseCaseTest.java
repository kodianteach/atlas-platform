package co.com.atlas.usecase.porter;

import co.com.atlas.model.auth.AuthUser;
import co.com.atlas.model.auth.UserStatus;
import co.com.atlas.model.auth.gateways.AuthUserRepository;
import co.com.atlas.model.organization.Organization;
import co.com.atlas.model.organization.gateways.OrganizationRepository;
import co.com.atlas.model.porter.PorterEnrollmentAuditLog;
import co.com.atlas.model.porter.PorterEnrollmentToken;
import co.com.atlas.model.porter.PorterEnrollmentTokenStatus;
import co.com.atlas.model.porter.PorterType;
import co.com.atlas.model.porter.gateways.PorterEnrollmentAuditRepository;
import co.com.atlas.model.porter.gateways.PorterEnrollmentTokenRepository;
import co.com.atlas.model.role.Role;
import co.com.atlas.model.role.gateways.RoleRepository;
import co.com.atlas.model.userrolemulti.UserRoleMulti;
import co.com.atlas.model.userrolemulti.gateways.UserRoleMultiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreatePorterUseCaseTest {

    @Mock private PorterEnrollmentTokenRepository tokenRepository;
    @Mock private PorterEnrollmentAuditRepository auditRepository;
    @Mock private AuthUserRepository authUserRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRoleMultiRepository userRoleMultiRepository;
    @Mock private OrganizationRepository organizationRepository;

    private CreatePorterUseCase useCase;

    private static final Long ORG_ID = 100L;
    private static final Long ADMIN_USER_ID = 5L;

    @BeforeEach
    void setUp() {
        useCase = new CreatePorterUseCase(
                tokenRepository, auditRepository,
                authUserRepository, roleRepository, userRoleMultiRepository,
                organizationRepository);
    }

    @Test
    void shouldCreatePorterGeneralSuccessfully() {
        // Arrange
        Organization org = Organization.builder().id(ORG_ID).slug("mi-conjunto").build();
        AuthUser savedUser = AuthUser.builder().id(10L).names("Portería Principal")
                .email("porter-xxx@mi-conjunto.atlas.internal")
                .status(UserStatus.PRE_REGISTERED).active(false).build();
        Role porterRole = Role.builder().id(50L).name("Portero General").code("PORTERO_GENERAL").build();
        PorterEnrollmentToken savedToken = PorterEnrollmentToken.builder().id(1L)
                .userId(10L).organizationId(ORG_ID).tokenHash("hash")
                .status(PorterEnrollmentTokenStatus.PENDING).build();

        when(organizationRepository.findById(ORG_ID)).thenReturn(Mono.just(org));
        when(authUserRepository.save(any(AuthUser.class))).thenReturn(Mono.just(savedUser));
        when(roleRepository.findByCode("PORTERO_GENERAL")).thenReturn(Mono.just(porterRole));
        when(userRoleMultiRepository.save(any(UserRoleMulti.class)))
                .thenReturn(Mono.just(UserRoleMulti.builder().id(1L).build()));
        when(tokenRepository.save(any(PorterEnrollmentToken.class))).thenReturn(Mono.just(savedToken));
        when(auditRepository.save(any(PorterEnrollmentAuditLog.class)))
                .thenReturn(Mono.just(PorterEnrollmentAuditLog.builder().id(1L).build()));

        var command = new CreatePorterUseCase.CreatePorterCommand("Portería Principal", PorterType.PORTERO_GENERAL);

        // Act & Assert
        StepVerifier.create(useCase.execute(command, ORG_ID, ADMIN_USER_ID))
                .assertNext(result -> {
                    assertThat(result.porter().getNames()).isEqualTo("Portería Principal");
                    assertThat(result.porter().getPorterType()).isEqualTo(PorterType.PORTERO_GENERAL);
                    assertThat(result.porter().getId()).isEqualTo(10L);
                    assertThat(result.enrollmentUrl()).startsWith("/porter-enroll?token=");
                })
                .verifyComplete();
    }

    @Test
    void shouldCreatePorterDeliverySuccessfully() {
        // Arrange
        Organization org = Organization.builder().id(ORG_ID).slug("mi-conjunto").build();
        AuthUser savedUser = AuthUser.builder().id(11L).names("Portería Delivery")
                .status(UserStatus.PRE_REGISTERED).active(false).build();
        Role porterRole = Role.builder().id(51L).name("Portero Delivery").code("PORTERO_DELIVERY").build();
        PorterEnrollmentToken savedToken = PorterEnrollmentToken.builder().id(2L)
                .userId(11L).organizationId(ORG_ID).tokenHash("hash")
                .status(PorterEnrollmentTokenStatus.PENDING).build();

        when(organizationRepository.findById(ORG_ID)).thenReturn(Mono.just(org));
        when(authUserRepository.save(any(AuthUser.class))).thenReturn(Mono.just(savedUser));
        when(roleRepository.findByCode("PORTERO_DELIVERY")).thenReturn(Mono.just(porterRole));
        when(userRoleMultiRepository.save(any(UserRoleMulti.class)))
                .thenReturn(Mono.just(UserRoleMulti.builder().id(1L).build()));
        when(tokenRepository.save(any(PorterEnrollmentToken.class))).thenReturn(Mono.just(savedToken));
        when(auditRepository.save(any(PorterEnrollmentAuditLog.class)))
                .thenReturn(Mono.just(PorterEnrollmentAuditLog.builder().id(1L).build()));

        var command = new CreatePorterUseCase.CreatePorterCommand("Portería Delivery", PorterType.PORTERO_DELIVERY);

        // Act & Assert
        StepVerifier.create(useCase.execute(command, ORG_ID, ADMIN_USER_ID))
                .assertNext(result -> {
                    assertThat(result.porter().getPorterType()).isEqualTo(PorterType.PORTERO_DELIVERY);
                })
                .verifyComplete();
    }

    @Test
    void shouldGenerateSyntheticEmailCorrectly() {
        // Arrange
        Organization org = Organization.builder().id(ORG_ID).slug("mi-conjunto").build();
        AuthUser savedUser = AuthUser.builder().id(10L).names("Test")
                .status(UserStatus.PRE_REGISTERED).active(false).build();
        Role porterRole = Role.builder().id(50L).code("PORTERO_GENERAL").build();
        PorterEnrollmentToken savedToken = PorterEnrollmentToken.builder().id(1L)
                .userId(10L).organizationId(ORG_ID).tokenHash("hash")
                .status(PorterEnrollmentTokenStatus.PENDING).build();

        ArgumentCaptor<AuthUser> userCaptor = ArgumentCaptor.forClass(AuthUser.class);
        when(organizationRepository.findById(ORG_ID)).thenReturn(Mono.just(org));
        when(authUserRepository.save(userCaptor.capture())).thenReturn(Mono.just(savedUser));
        when(roleRepository.findByCode(anyString())).thenReturn(Mono.just(porterRole));
        when(userRoleMultiRepository.save(any())).thenReturn(Mono.just(UserRoleMulti.builder().id(1L).build()));
        when(tokenRepository.save(any())).thenReturn(Mono.just(savedToken));
        when(auditRepository.save(any())).thenReturn(Mono.just(PorterEnrollmentAuditLog.builder().id(1L).build()));

        var command = new CreatePorterUseCase.CreatePorterCommand("Test", PorterType.PORTERO_GENERAL);

        // Act
        StepVerifier.create(useCase.execute(command, ORG_ID, ADMIN_USER_ID)).expectNextCount(1).verifyComplete();

        // Assert - Verify synthetic email pattern
        AuthUser capturedUser = userCaptor.getValue();
        assertThat(capturedUser.getEmail()).matches("porter-[a-f0-9\\-]+@mi-conjunto\\.atlas\\.internal");
        assertThat(capturedUser.getStatus()).isEqualTo(UserStatus.PRE_REGISTERED);
        assertThat(capturedUser.isActive()).isFalse();
        assertThat(capturedUser.getPasswordHash()).isNull();
    }

    @Test
    void shouldAssignRoleWithCorrectOrganizationId() {
        // Arrange
        Organization org = Organization.builder().id(ORG_ID).slug("mi-conjunto").build();
        AuthUser savedUser = AuthUser.builder().id(10L).build();
        Role porterRole = Role.builder().id(50L).code("PORTERO_GENERAL").build();
        PorterEnrollmentToken savedToken = PorterEnrollmentToken.builder().id(1L)
                .userId(10L).organizationId(ORG_ID).tokenHash("hash")
                .status(PorterEnrollmentTokenStatus.PENDING).build();

        ArgumentCaptor<UserRoleMulti> roleCaptor = ArgumentCaptor.forClass(UserRoleMulti.class);
        when(organizationRepository.findById(ORG_ID)).thenReturn(Mono.just(org));
        when(authUserRepository.save(any())).thenReturn(Mono.just(savedUser));
        when(roleRepository.findByCode("PORTERO_GENERAL")).thenReturn(Mono.just(porterRole));
        when(userRoleMultiRepository.save(roleCaptor.capture()))
                .thenReturn(Mono.just(UserRoleMulti.builder().id(1L).build()));
        when(tokenRepository.save(any())).thenReturn(Mono.just(savedToken));
        when(auditRepository.save(any())).thenReturn(Mono.just(PorterEnrollmentAuditLog.builder().id(1L).build()));

        var command = new CreatePorterUseCase.CreatePorterCommand("Test", PorterType.PORTERO_GENERAL);

        // Act
        StepVerifier.create(useCase.execute(command, ORG_ID, ADMIN_USER_ID)).expectNextCount(1).verifyComplete();

        // Assert - organizationId must be the admin's org (NOT null)
        UserRoleMulti capturedRole = roleCaptor.getValue();
        assertThat(capturedRole.getOrganizationId()).isEqualTo(ORG_ID);
        assertThat(capturedRole.getRoleId()).isEqualTo(50L);
        assertThat(capturedRole.getIsPrimary()).isTrue();
    }

    @Test
    void shouldRegisterTwoAuditEvents() {
        // Arrange
        Organization org = Organization.builder().id(ORG_ID).slug("mi-conjunto").build();
        AuthUser savedUser = AuthUser.builder().id(10L).build();
        Role porterRole = Role.builder().id(50L).code("PORTERO_GENERAL").build();
        PorterEnrollmentToken savedToken = PorterEnrollmentToken.builder().id(1L)
                .userId(10L).organizationId(ORG_ID).tokenHash("hash")
                .status(PorterEnrollmentTokenStatus.PENDING).build();

        when(organizationRepository.findById(ORG_ID)).thenReturn(Mono.just(org));
        when(authUserRepository.save(any())).thenReturn(Mono.just(savedUser));
        when(roleRepository.findByCode(anyString())).thenReturn(Mono.just(porterRole));
        when(userRoleMultiRepository.save(any())).thenReturn(Mono.just(UserRoleMulti.builder().id(1L).build()));
        when(tokenRepository.save(any())).thenReturn(Mono.just(savedToken));
        when(auditRepository.save(any())).thenReturn(Mono.just(PorterEnrollmentAuditLog.builder().id(1L).build()));

        var command = new CreatePorterUseCase.CreatePorterCommand("Test", PorterType.PORTERO_GENERAL);

        // Act
        StepVerifier.create(useCase.execute(command, ORG_ID, ADMIN_USER_ID)).expectNextCount(1).verifyComplete();

        // Assert - 2 audit events: CREATED + URL_GENERATED
        verify(auditRepository, times(2)).save(any(PorterEnrollmentAuditLog.class));
    }

    @Test
    void shouldFailWhenDisplayNameIsBlank() {
        var command = new CreatePorterUseCase.CreatePorterCommand("", PorterType.PORTERO_GENERAL);

        StepVerifier.create(useCase.execute(command, ORG_ID, ADMIN_USER_ID))
                .expectErrorMatches(e -> e instanceof co.com.atlas.model.common.BusinessException
                        && e.getMessage().contains("nombre descriptivo"))
                .verify();
    }

    @Test
    void shouldFailWhenPorterTypeIsNull() {
        var command = new CreatePorterUseCase.CreatePorterCommand("Test", null);

        StepVerifier.create(useCase.execute(command, ORG_ID, ADMIN_USER_ID))
                .expectErrorMatches(e -> e instanceof co.com.atlas.model.common.BusinessException
                        && e.getMessage().contains("tipo de portero"))
                .verify();
    }
}

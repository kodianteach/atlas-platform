package co.com.atlas.config;

import co.com.atlas.model.access.gateways.AccessCodeRepository;
import co.com.atlas.model.access.gateways.AccessEventRepository;
import co.com.atlas.model.access.gateways.AccessScanLogRepository;
import co.com.atlas.model.preregistration.gateways.AdminActivationTokenRepository;
import co.com.atlas.model.notification.gateways.NotificationGateway;
import co.com.atlas.model.preregistration.gateways.PreRegistrationAuditRepository;
import co.com.atlas.model.auth.gateways.AuthUserRepository;
import co.com.atlas.model.auth.gateways.JwtTokenGateway;
import co.com.atlas.model.comment.gateways.CommentRepository;
import co.com.atlas.model.company.gateways.CompanyRepository;
import co.com.atlas.model.invitation.gateways.InvitationRepository;
import co.com.atlas.model.organization.gateways.OrganizationRepository;
import co.com.atlas.model.permission.gateways.PermissionRepository;
import co.com.atlas.model.poll.gateways.PollOptionRepository;
import co.com.atlas.model.poll.gateways.PollRepository;
import co.com.atlas.model.poll.gateways.PollVoteRepository;
import co.com.atlas.model.post.gateways.PostRepository;
import co.com.atlas.model.role.gateways.RoleRepository;
import co.com.atlas.model.tower.gateways.TowerRepository;
import co.com.atlas.model.unit.gateways.UnitRepository;
import co.com.atlas.model.userorganization.gateways.UserOrganizationRepository;
import co.com.atlas.model.userrolemulti.gateways.UserRoleMultiRepository;
import co.com.atlas.model.userunit.gateways.UserUnitRepository;
import co.com.atlas.model.visit.gateways.VisitApprovalRepository;
import co.com.atlas.model.visit.gateways.VisitRequestRepository;
import co.com.atlas.model.zone.gateways.ZoneRepository;
import co.com.atlas.usecase.access.AccessCodeUseCase;
import co.com.atlas.usecase.access.GetRevocationListUseCase;
import co.com.atlas.usecase.access.RegisterVehicleExitUseCase;
import co.com.atlas.usecase.access.SyncAccessEventsUseCase;
import co.com.atlas.usecase.access.ValidateAuthorizationUseCase;
import co.com.atlas.usecase.access.ValidateByDocumentUseCase;
import co.com.atlas.usecase.preregistration.ActivateAdminUseCase;
import co.com.atlas.usecase.preregistration.CompleteOnboardingUseCase;
import co.com.atlas.usecase.preregistration.PreRegisterAdminUseCase;
import co.com.atlas.usecase.preregistration.ResendPreRegistrationUseCase;
import co.com.atlas.usecase.auth.LoginUseCase;
import co.com.atlas.usecase.auth.RefreshTokenUseCase;
import co.com.atlas.usecase.auth.RegisterUserUseCase;
import co.com.atlas.usecase.comment.CommentUseCase;
import co.com.atlas.usecase.company.CompanyUseCase;
import co.com.atlas.usecase.invitation.InvitationUseCase;
import co.com.atlas.usecase.organization.OrganizationUseCase;
import co.com.atlas.usecase.poll.PollUseCase;
import co.com.atlas.usecase.post.PostUseCase;
import co.com.atlas.usecase.tower.TowerUseCase;
import co.com.atlas.usecase.unit.UnitUseCase;
import co.com.atlas.usecase.visit.VisitRequestUseCase;
import co.com.atlas.usecase.vehicle.VehicleUseCase;
import co.com.atlas.usecase.zone.ZoneUseCase;
import co.com.atlas.usecase.activation.OwnerActivationUseCase;
import co.com.atlas.usecase.authorization.CreateAuthorizationUseCase;
import co.com.atlas.usecase.authorization.GetAuthorizationByIdUseCase;
import co.com.atlas.usecase.authorization.GetAuthorizationsUseCase;
import co.com.atlas.usecase.authorization.RevokeAuthorizationUseCase;
import co.com.atlas.usecase.unit.UnitDistributionUseCase;
import co.com.atlas.usecase.unit.UnitBulkUploadUseCase;
import co.com.atlas.usecase.organization.OrganizationSettingsUseCase;
import co.com.atlas.usecase.porter.CreatePorterUseCase;
import co.com.atlas.usecase.porter.EnrollPorterDeviceUseCase;
import co.com.atlas.usecase.porter.ListPortersByOrganizationUseCase;
import co.com.atlas.usecase.porter.RegeneratePorterEnrollmentUrlUseCase;
import co.com.atlas.usecase.porter.TogglePorterStatusUseCase;
import co.com.atlas.usecase.porter.ValidateEnrollmentTokenUseCase;
import co.com.atlas.model.authorization.gateways.FileStorageGateway;
import co.com.atlas.model.authorization.gateways.VisitorAuthorizationRepository;
import co.com.atlas.model.crypto.gateways.CryptoKeyGeneratorGateway;
import co.com.atlas.model.crypto.gateways.CryptoKeyRepository;
import co.com.atlas.model.organization.gateways.OrganizationConfigurationRepository;
import co.com.atlas.model.porter.gateways.PorterEnrollmentAuditRepository;
import co.com.atlas.model.porter.gateways.PorterEnrollmentTokenRepository;
import co.com.atlas.model.porter.gateways.PorterRepository;
import co.com.atlas.model.vehicle.gateways.VehicleRepository;
import co.com.atlas.model.invitation.gateways.InvitationAuditRepository;
import co.com.atlas.model.notification.gateways.NotificationRepository;
import co.com.atlas.model.comment.gateways.ContentModerationGateway;
import co.com.atlas.model.message.gateways.ChannelMessageRepository;
import co.com.atlas.usecase.message.ChannelMessageUseCase;
import co.com.atlas.usecase.message.MessageRetentionUseCase;
import co.com.atlas.usecase.notification.NotificationUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de beans para los casos de uso de Atlas.
 */
@Configuration
public class UseCasesConfig {

    // Auth Use Cases
    @Bean
    public LoginUseCase loginUseCase(
            AuthUserRepository authUserRepository,
            JwtTokenGateway jwtTokenGateway,
            UserOrganizationRepository userOrganizationRepository,
            UserRoleMultiRepository userRoleMultiRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository) {
        return new LoginUseCase(
                authUserRepository,
                jwtTokenGateway,
                userOrganizationRepository,
                userRoleMultiRepository,
                roleRepository,
                permissionRepository);
    }

    @Bean
    public RefreshTokenUseCase refreshTokenUseCase(JwtTokenGateway jwtTokenGateway) {
        return new RefreshTokenUseCase(jwtTokenGateway);
    }

    @Bean
    public RegisterUserUseCase registerUserUseCase(AuthUserRepository authUserRepository) {
        return new RegisterUserUseCase(authUserRepository);
    }

    // Company Use Cases
    @Bean
    public CompanyUseCase companyUseCase(CompanyRepository companyRepository) {
        return new CompanyUseCase(companyRepository);
    }

    // Organization Use Cases
    @Bean
    public OrganizationUseCase organizationUseCase(
            OrganizationRepository organizationRepository,
            CompanyRepository companyRepository) {
        return new OrganizationUseCase(organizationRepository, companyRepository);
    }

    // Zone Use Cases
    @Bean
    public ZoneUseCase zoneUseCase(
            ZoneRepository zoneRepository,
            OrganizationRepository organizationRepository) {
        return new ZoneUseCase(zoneRepository, organizationRepository);
    }

    // Tower Use Cases
    @Bean
    public TowerUseCase towerUseCase(
            TowerRepository towerRepository,
            ZoneRepository zoneRepository,
            OrganizationRepository organizationRepository) {
        return new TowerUseCase(towerRepository, zoneRepository, organizationRepository);
    }

    // Unit Use Cases
    @Bean
    public UnitUseCase unitUseCase(
            UnitRepository unitRepository,
            OrganizationRepository organizationRepository) {
        return new UnitUseCase(unitRepository, organizationRepository);
    }

    // Invitation Use Cases
    @Bean
    public InvitationUseCase invitationUseCase(
            InvitationRepository invitationRepository,
            OrganizationRepository organizationRepository,
            UnitRepository unitRepository,
            UserOrganizationRepository userOrganizationRepository,
            UserUnitRepository userUnitRepository,
            AuthUserRepository authUserRepository,
            NotificationGateway notificationGateway,
            RoleRepository roleRepository,
            UserRoleMultiRepository userRoleMultiRepository,
            org.springframework.security.crypto.password.PasswordEncoder springPasswordEncoder,
            InvitationAuditRepository invitationAuditRepository,
            OrganizationConfigurationRepository organizationConfigurationRepository,
            co.com.atlas.model.permission.gateways.PermissionRepository permissionRepository,
            co.com.atlas.model.userunitpermission.gateways.UserUnitPermissionRepository userUnitPermissionRepository,
            @org.springframework.beans.factory.annotation.Value("${app.frontend-url:http://localhost:4200}") String frontendUrl) {
        return new InvitationUseCase(
                invitationRepository,
                organizationRepository,
                unitRepository,
                userOrganizationRepository,
                userUnitRepository,
                authUserRepository,
                notificationGateway,
                roleRepository,
                userRoleMultiRepository,
                springPasswordEncoder::encode,
                invitationAuditRepository,
                organizationConfigurationRepository,
                permissionRepository,
                userUnitPermissionRepository,
                frontendUrl);
    }

    @Bean
    public co.com.atlas.usecase.invitation.UserLookupUseCase userLookupUseCase(
            AuthUserRepository authUserRepository,
            UnitRepository unitRepository) {
        return new co.com.atlas.usecase.invitation.UserLookupUseCase(authUserRepository, unitRepository);
    }

    // Visit Use Cases
    @Bean
    public VisitRequestUseCase visitRequestUseCase(
            VisitRequestRepository visitRequestRepository,
            VisitApprovalRepository visitApprovalRepository,
            AccessCodeRepository accessCodeRepository,
            UnitRepository unitRepository,
            UserUnitRepository userUnitRepository) {
        return new VisitRequestUseCase(
                visitRequestRepository,
                visitApprovalRepository,
                accessCodeRepository,
                unitRepository,
                userUnitRepository);
    }

    // Access Use Cases
    @Bean
    public AccessCodeUseCase accessCodeUseCase(
            AccessCodeRepository accessCodeRepository,
            AccessScanLogRepository accessScanLogRepository,
            VisitRequestRepository visitRequestRepository) {
        return new AccessCodeUseCase(accessCodeRepository, accessScanLogRepository, visitRequestRepository);
    }

    // Post Use Cases
    @Bean
    public PostUseCase postUseCase(PostRepository postRepository,
                                   NotificationRepository notificationRepository) {
        return new PostUseCase(postRepository, notificationRepository);
    }

    // Comment Use Cases
    @Bean
    public CommentUseCase commentUseCase(
            CommentRepository commentRepository,
            PostRepository postRepository,
            ContentModerationGateway contentModerationGateway) {
        return new CommentUseCase(commentRepository, postRepository, contentModerationGateway);
    }

    // Poll Use Cases
    @Bean
    public PollUseCase pollUseCase(
            PollRepository pollRepository,
            PollOptionRepository pollOptionRepository,
            PollVoteRepository pollVoteRepository,
            NotificationRepository notificationRepository) {
        return new PollUseCase(pollRepository, pollOptionRepository, pollVoteRepository, notificationRepository);
    }

    // Notification Use Cases
    @Bean
    public NotificationUseCase notificationUseCase(NotificationRepository notificationRepository) {
        return new NotificationUseCase(notificationRepository);
    }

    // Admin Pre-Registration Use Cases
    @Bean
    public PreRegisterAdminUseCase preRegisterAdminUseCase(
            AuthUserRepository authUserRepository,
            AdminActivationTokenRepository tokenRepository,
            PreRegistrationAuditRepository auditRepository,
            NotificationGateway notificationGateway,
            RoleRepository roleRepository,
            UserRoleMultiRepository userRoleMultiRepository) {
        return new PreRegisterAdminUseCase(
                authUserRepository,
                tokenRepository,
                auditRepository,
                notificationGateway,
                roleRepository,
                userRoleMultiRepository);
    }

    @Bean
    public ResendPreRegistrationUseCase resendPreRegistrationUseCase(
            AuthUserRepository authUserRepository,
            AdminActivationTokenRepository tokenRepository,
            PreRegistrationAuditRepository auditRepository,
            NotificationGateway notificationGateway) {
        return new ResendPreRegistrationUseCase(
                authUserRepository,
                tokenRepository,
                auditRepository,
                notificationGateway);
    }

    @Bean
    public ActivateAdminUseCase activateAdminUseCase(
            AuthUserRepository authUserRepository,
            AdminActivationTokenRepository tokenRepository,
            PreRegistrationAuditRepository auditRepository,
            NotificationGateway notificationGateway) {
        return new ActivateAdminUseCase(
                authUserRepository,
                tokenRepository,
                auditRepository,
                notificationGateway);
    }

    @Bean
    public CompleteOnboardingUseCase completeOnboardingUseCase(
            AuthUserRepository authUserRepository,
            CompanyRepository companyRepository,
            OrganizationRepository organizationRepository,
            UserOrganizationRepository userOrganizationRepository,
            RoleRepository roleRepository,
            UserRoleMultiRepository userRoleMultiRepository) {
        return new CompleteOnboardingUseCase(
                authUserRepository,
                companyRepository,
                organizationRepository,
                userOrganizationRepository,
                roleRepository,
                userRoleMultiRepository);
    }

    // Vehicle Use Cases
    @Bean
    public VehicleUseCase vehicleUseCase(
            VehicleRepository vehicleRepository,
            UnitRepository unitRepository) {
        return new VehicleUseCase(vehicleRepository, unitRepository);
    }

    // Owner Activation Use Cases
    @Bean
    public OwnerActivationUseCase ownerActivationUseCase(
            InvitationRepository invitationRepository,
            InvitationAuditRepository invitationAuditRepository,
            AuthUserRepository authUserRepository,
            UserOrganizationRepository userOrganizationRepository,
            UserUnitRepository userUnitRepository,
            UnitRepository unitRepository,
            UserRoleMultiRepository userRoleMultiRepository,
            NotificationGateway notificationGateway,
            PasswordEncoder passwordEncoder) {
        return new OwnerActivationUseCase(
                invitationRepository,
                invitationAuditRepository,
                authUserRepository,
                userOrganizationRepository,
                userUnitRepository,
                unitRepository,
                userRoleMultiRepository,
                notificationGateway,
                passwordEncoder::encode);
    }

    // Unit Distribution Use Cases
    @Bean
    public UnitDistributionUseCase unitDistributionUseCase(
            UnitRepository unitRepository,
            OrganizationRepository organizationRepository,
            OrganizationConfigurationRepository organizationConfigurationRepository,
            AuthUserRepository authUserRepository,
            InvitationRepository invitationRepository,
            InvitationAuditRepository invitationAuditRepository,
            UserOrganizationRepository userOrganizationRepository,
            UserUnitRepository userUnitRepository,
            RoleRepository roleRepository,
            NotificationGateway notificationGateway,
            @org.springframework.beans.factory.annotation.Value("${app.frontend-url:http://localhost:4200}") String frontendUrl) {
        return new UnitDistributionUseCase(
                unitRepository,
                organizationRepository,
                organizationConfigurationRepository,
                authUserRepository,
                invitationRepository,
                invitationAuditRepository,
                userOrganizationRepository,
                userUnitRepository,
                roleRepository,
                notificationGateway,
                frontendUrl);
    }

    // Unit Bulk Upload Use Cases
    @Bean
    public UnitBulkUploadUseCase unitBulkUploadUseCase(
            UnitRepository unitRepository,
            OrganizationRepository organizationRepository,
            AuthUserRepository authUserRepository,
            InvitationRepository invitationRepository,
            InvitationAuditRepository invitationAuditRepository,
            UserOrganizationRepository userOrganizationRepository,
            UserUnitRepository userUnitRepository,
            RoleRepository roleRepository,
            NotificationGateway notificationGateway,
            @org.springframework.beans.factory.annotation.Value("${app.frontend-url:http://localhost:4200}") String frontendUrl) {
        return new UnitBulkUploadUseCase(
                unitRepository,
                organizationRepository,
                authUserRepository,
                invitationRepository,
                invitationAuditRepository,
                userOrganizationRepository,
                userUnitRepository,
                roleRepository,
                notificationGateway,
                frontendUrl);
    }

    // Organization Settings Use Cases
    @Bean
    public OrganizationSettingsUseCase organizationSettingsUseCase(
            OrganizationRepository organizationRepository,
            OrganizationConfigurationRepository organizationConfigurationRepository,
            @Value("${atlas.branding.max-logo-size-bytes:2097152}") long maxLogoSizeBytes) {
        return new OrganizationSettingsUseCase(organizationRepository, organizationConfigurationRepository, maxLogoSizeBytes);
    }

    // Porter Use Cases
    @Bean
    public CreatePorterUseCase createPorterUseCase(
            PorterEnrollmentTokenRepository porterEnrollmentTokenRepository,
            PorterEnrollmentAuditRepository porterEnrollmentAuditRepository,
            AuthUserRepository authUserRepository,
            RoleRepository roleRepository,
            UserRoleMultiRepository userRoleMultiRepository,
            OrganizationRepository organizationRepository,
            UserOrganizationRepository userOrganizationRepository) {
        return new CreatePorterUseCase(
                porterEnrollmentTokenRepository,
                porterEnrollmentAuditRepository,
                authUserRepository,
                roleRepository,
                userRoleMultiRepository,
                organizationRepository,
                userOrganizationRepository);
    }

    @Bean
    public ListPortersByOrganizationUseCase listPortersByOrganizationUseCase(
            PorterRepository porterRepository) {
        return new ListPortersByOrganizationUseCase(porterRepository);
    }

    @Bean
    public TogglePorterStatusUseCase togglePorterStatusUseCase(
            PorterRepository porterRepository,
            AuthUserRepository authUserRepository) {
        return new TogglePorterStatusUseCase(porterRepository, authUserRepository);
    }

    @Bean
    public RegeneratePorterEnrollmentUrlUseCase regeneratePorterEnrollmentUrlUseCase(
            PorterRepository porterRepository,
            PorterEnrollmentTokenRepository porterEnrollmentTokenRepository,
            PorterEnrollmentAuditRepository porterEnrollmentAuditRepository) {
        return new RegeneratePorterEnrollmentUrlUseCase(
                porterRepository,
                porterEnrollmentTokenRepository,
                porterEnrollmentAuditRepository);
    }

    @Bean
    public ValidateEnrollmentTokenUseCase validateEnrollmentTokenUseCase(
            PorterEnrollmentTokenRepository porterEnrollmentTokenRepository,
            PorterRepository porterRepository,
            OrganizationRepository organizationRepository) {
        return new ValidateEnrollmentTokenUseCase(
                porterEnrollmentTokenRepository,
                porterRepository,
                organizationRepository);
    }

    @Bean
    public EnrollPorterDeviceUseCase enrollPorterDeviceUseCase(
            PorterEnrollmentTokenRepository porterEnrollmentTokenRepository,
            PorterEnrollmentAuditRepository porterEnrollmentAuditRepository,
            AuthUserRepository authUserRepository,
            OrganizationRepository organizationRepository,
            CryptoKeyRepository cryptoKeyRepository,
            CryptoKeyGeneratorGateway cryptoKeyGeneratorGateway,
            JwtTokenGateway jwtTokenGateway,
            UserRoleMultiRepository userRoleMultiRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            UserOrganizationRepository userOrganizationRepository) {
        return new EnrollPorterDeviceUseCase(
                porterEnrollmentTokenRepository,
                porterEnrollmentAuditRepository,
                authUserRepository,
                organizationRepository,
                cryptoKeyRepository,
                cryptoKeyGeneratorGateway,
                jwtTokenGateway,
                userRoleMultiRepository,
                roleRepository,
                permissionRepository,
                userOrganizationRepository);
    }

    // Authorization Use Cases
    @Bean
    public CreateAuthorizationUseCase createAuthorizationUseCase(
            VisitorAuthorizationRepository authorizationRepository,
            FileStorageGateway fileStorageGateway,
            CryptoKeyRepository cryptoKeyRepository,
            CryptoKeyGeneratorGateway cryptoKeyGeneratorGateway,
            UserUnitRepository userUnitRepository,
            UnitRepository unitRepository) {
        return new CreateAuthorizationUseCase(
                authorizationRepository,
                fileStorageGateway,
                cryptoKeyRepository,
                cryptoKeyGeneratorGateway,
                userUnitRepository,
                unitRepository);
    }

    @Bean
    public GetAuthorizationsUseCase getAuthorizationsUseCase(
            VisitorAuthorizationRepository authorizationRepository,
            UserUnitRepository userUnitRepository) {
        return new GetAuthorizationsUseCase(authorizationRepository, userUnitRepository);
    }

    @Bean
    public GetAuthorizationByIdUseCase getAuthorizationByIdUseCase(
            VisitorAuthorizationRepository authorizationRepository) {
        return new GetAuthorizationByIdUseCase(authorizationRepository);
    }

    @Bean
    public RevokeAuthorizationUseCase revokeAuthorizationUseCase(
            VisitorAuthorizationRepository authorizationRepository) {
        return new RevokeAuthorizationUseCase(authorizationRepository);
    }

    // Access Porter Use Cases (HU #7)
    @Bean
    public ValidateAuthorizationUseCase validateAuthorizationUseCase(
            CryptoKeyRepository cryptoKeyRepository,
            VisitorAuthorizationRepository visitorAuthorizationRepository,
            AccessEventRepository accessEventRepository) {
        return new ValidateAuthorizationUseCase(
                cryptoKeyRepository,
                visitorAuthorizationRepository,
                accessEventRepository);
    }

    @Bean
    public ValidateByDocumentUseCase validateByDocumentUseCase(
            VisitorAuthorizationRepository visitorAuthorizationRepository,
            AccessEventRepository accessEventRepository) {
        return new ValidateByDocumentUseCase(
                visitorAuthorizationRepository,
                accessEventRepository);
    }

    @Bean
    public SyncAccessEventsUseCase syncAccessEventsUseCase(
            AccessEventRepository accessEventRepository) {
        return new SyncAccessEventsUseCase(accessEventRepository);
    }

    @Bean
    public GetRevocationListUseCase getRevocationListUseCase(
            VisitorAuthorizationRepository visitorAuthorizationRepository) {
        return new GetRevocationListUseCase(visitorAuthorizationRepository);
    }

    @Bean
    public RegisterVehicleExitUseCase registerVehicleExitUseCase(
            AccessEventRepository accessEventRepository) {
        return new RegisterVehicleExitUseCase(accessEventRepository);
    }

    // Message Use Cases (HU #13)
    @Bean
    public ChannelMessageUseCase channelMessageUseCase(
            ChannelMessageRepository channelMessageRepository) {
        return new ChannelMessageUseCase(channelMessageRepository);
    }

    @Bean
    public MessageRetentionUseCase messageRetentionUseCase(
            ChannelMessageRepository channelMessageRepository) {
        return new MessageRetentionUseCase(channelMessageRepository);
    }
}

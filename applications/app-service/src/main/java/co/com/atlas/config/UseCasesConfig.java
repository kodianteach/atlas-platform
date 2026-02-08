package co.com.atlas.config;

import co.com.atlas.model.access.gateways.AccessCodeRepository;
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
import co.com.atlas.model.poll.gateways.PollOptionRepository;
import co.com.atlas.model.poll.gateways.PollRepository;
import co.com.atlas.model.poll.gateways.PollVoteRepository;
import co.com.atlas.model.post.gateways.PostRepository;
import co.com.atlas.model.tower.gateways.TowerRepository;
import co.com.atlas.model.unit.gateways.UnitRepository;
import co.com.atlas.model.userorganization.gateways.UserOrganizationRepository;
import co.com.atlas.model.userunit.gateways.UserUnitRepository;
import co.com.atlas.model.visit.gateways.VisitApprovalRepository;
import co.com.atlas.model.visit.gateways.VisitRequestRepository;
import co.com.atlas.model.zone.gateways.ZoneRepository;
import co.com.atlas.usecase.access.AccessCodeUseCase;
import co.com.atlas.usecase.preregistration.ActivateAdminUseCase;
import co.com.atlas.usecase.preregistration.CompleteOnboardingUseCase;
import co.com.atlas.usecase.preregistration.PreRegisterAdminUseCase;
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
import co.com.atlas.usecase.zone.ZoneUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de beans para los casos de uso de Atlas.
 */
@Configuration
public class UseCasesConfig {

    // Auth Use Cases
    @Bean
    public LoginUseCase loginUseCase(AuthUserRepository authUserRepository, JwtTokenGateway jwtTokenGateway) {
        return new LoginUseCase(authUserRepository, jwtTokenGateway);
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
            AuthUserRepository authUserRepository) {
        return new InvitationUseCase(
                invitationRepository,
                organizationRepository,
                unitRepository,
                userOrganizationRepository,
                userUnitRepository,
                authUserRepository);
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
    public PostUseCase postUseCase(PostRepository postRepository) {
        return new PostUseCase(postRepository);
    }

    // Comment Use Cases
    @Bean
    public CommentUseCase commentUseCase(
            CommentRepository commentRepository,
            PostRepository postRepository) {
        return new CommentUseCase(commentRepository, postRepository);
    }

    // Poll Use Cases
    @Bean
    public PollUseCase pollUseCase(
            PollRepository pollRepository,
            PollOptionRepository pollOptionRepository,
            PollVoteRepository pollVoteRepository) {
        return new PollUseCase(pollRepository, pollOptionRepository, pollVoteRepository);
    }

    // Admin Pre-Registration Use Cases
    @Bean
    public PreRegisterAdminUseCase preRegisterAdminUseCase(
            AuthUserRepository authUserRepository,
            AdminActivationTokenRepository tokenRepository,
            PreRegistrationAuditRepository auditRepository,
            NotificationGateway notificationGateway) {
        return new PreRegisterAdminUseCase(
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
            UserOrganizationRepository userOrganizationRepository) {
        return new CompleteOnboardingUseCase(
                authUserRepository,
                companyRepository,
                organizationRepository,
                userOrganizationRepository);
    }
}

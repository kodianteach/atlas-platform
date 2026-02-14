package co.com.atlas.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuración de OpenAPI/Swagger UI usando SpringDoc para la plataforma Atlas.
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:Atlas Platform API}")
    private String applicationName;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${openapi.server.url:}")
    private String serverUrl;

    @Bean
    public OpenAPI openAPI() {
        final String schemeName = "bearerAuth";
        
        List<Server> servers = new ArrayList<>();
        
        // Si hay una URL de servidor configurada (producción), usarla primero
        if (serverUrl != null && !serverUrl.isEmpty()) {
            servers.add(new Server()
                    .url(serverUrl)
                    .description("Servidor de producción"));
        }
        
        // Siempre añadir servidor relativo (usa la URL actual del navegador)
        servers.add(new Server()
                .url("")
                .description("Servidor actual"));
        
        return new OpenAPI()
                .info(new Info()
                        .title(applicationName)
                        .description("""
                                API REST de la plataforma Atlas - Sistema de Gestión Residencial.
                                
                                ## Autenticación
                                
                                La API utiliza JWT (JSON Web Tokens) para la autenticación.
                                Para acceder a los endpoints protegidos:
                                
                                1. Realizar login en `/api/auth/login` con email y contraseña
                                2. Obtener el `accessToken` de la respuesta
                                3. Incluir el token en el header `Authorization: Bearer {token}`
                                
                                ## Roles y Permisos
                                
                                El token JWT contiene información sobre:
                                - **roles**: Lista de roles del usuario (OWNER, TENANT, ADMIN_ATLAS, etc.)
                                - **permissions**: Lista de permisos específicos del usuario
                                - **modules**: Módulos accesibles según los roles asignados
                                
                                ## Multi-Tenancy
                                
                                Atlas es una plataforma multi-tenant donde:
                                - Cada organización (ciudadela/conjunto/condominio) es un tenant aislado
                                - Los usuarios pueden pertenecer a múltiples organizaciones
                                - El token JWT incluye el `organizationId` del tenant activo
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Atlas Platform Team")
                                .email("soporte@atlas-platform.com")
                                .url("https://atlas-platform.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(servers)
                .components(new Components()
                        .addSecuritySchemes(schemeName,
                                new SecurityScheme()
                                        .name(schemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token de autenticación. Obtener mediante /api/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(schemeName));
    }
}

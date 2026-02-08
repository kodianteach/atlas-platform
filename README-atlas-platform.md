# Atlas Platform - Backend

Sistema de gestión residencial multi-tenant para ciudadelas y conjuntos cerrados.

## Tecnologías

- **Spring Boot 4.0.1** con WebFlux (reactivo)
- **Java 21**
- **MySQL 8.0** via R2DBC (reactivo)
- **Flyway** para migraciones de base de datos
- **JWT** (jjwt 0.12.6) para autenticación
- **SpringDoc OpenAPI 3.0.0** para documentación Swagger
- **Gradle** con Clean Architecture Plugin 4.0.5

## Arquitectura

El proyecto sigue la arquitectura Clean Architecture de Bancolombia:

```
atlas/
├── applications/
│   └── app-service/           # Punto de entrada principal
├── domain/
│   ├── model/                 # Entidades y gateways del dominio
│   └── usecase/               # Casos de uso de negocio
├── infrastructure/
│   ├── driven-adapters/
│   │   └── r2dbc-postgresql/  # Adaptador MySQL R2DBC + Flyway
│   ├── entry-points/
│   │   └── reactive-web/      # API REST WebFlux
│   └── helpers/
│       ├── jwt-helper/        # Gestión de tokens JWT
│       └── tenant-context/    # Contexto multi-tenant
```

## Configuración

### Base de datos

Configurar las siguientes variables de entorno:

```properties
DB_HOST=localhost
DB_PORT=3306
DB_NAME=atlas_platform
DB_USERNAME=root
DB_PASSWORD=password
```

### JWT

```properties
JWT_SECRET=MiClaveSecretaMuyLargaDeAlMenos256BitsParaHMACSHA256
JWT_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=86400000
```

## Ejecución

### Desarrollo

```bash
./gradlew bootRun
```

### Producción

```bash
./gradlew build
java -jar applications/app-service/build/libs/AtlasPlatform.jar
```

## API Endpoints

### Autenticación

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/auth/login` | Iniciar sesión |
| POST | `/api/auth/refresh` | Renovar token |
| POST | `/api/auth/verify-token` | Verificar token |

### Documentación

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

### Actuator

- **Health**: http://localhost:8080/actuator/health
- **Info**: http://localhost:8080/actuator/info

## Modelo de Datos

### Tablas Principales

| Tabla | Descripción |
|-------|-------------|
| users | Usuarios del sistema |
| role | Roles del sistema (SUPER_ADMIN, ADMIN_ATLAS, OWNER, TENANT, SECURITY) |
| company | Empresas administradoras |
| organization | Ciudadelas/Conjuntos (tenants) |
| zone | Zonas dentro de una organización |
| tower | Torres dentro de una zona |
| unit | Unidades (apartamentos/casas) |
| modules | Módulos del sistema |
| permissions | Permisos granulares |

### Multi-Tenancy

El sistema soporta múltiples organizaciones:

1. **Usuario se autentica** → Recibe token JWT con `organizationId`
2. **TenantFilter** extrae `organizationId` del JWT
3. **TenantContext** proporciona el contexto a las operaciones de BD
4. **Datos filtrados** por `organization_id` automáticamente

### Roles y Permisos

| Rol | Descripción | Módulo |
|-----|-------------|--------|
| SUPER_ADMIN | Acceso total al sistema | Global |
| ADMIN_ATLAS | Administrador de organización | ATLAS_CORE |
| OWNER | Propietario de unidad | ATLAS_CORE |
| TENANT | Arrendatario | ATLAS_CORE |
| FAMILY | Familiar/Residente | ATLAS_CORE |
| GUEST | Invitado temporal | ATLAS_CORE |
| SECURITY | Personal de seguridad | ACCESS_CONTROL |

## Usuario de Prueba

```
Email: admin@atlas.com
Password: Admin123!
```

(La contraseña está hasheada con BCrypt en la migración V3)

## Migraciones de Flyway

| Versión | Descripción |
|---------|-------------|
| V1 | Esquema inicial (users, roles, organizations, units) |
| V2 | Esquema de unidades y visitas |
| V3 | Datos semilla (roles, permisos, usuario admin) |

## Estructura de Archivos Creados

### Domain Model
- `co.com.atlas.model.auth.AuthUser`
- `co.com.atlas.model.auth.AuthCredentials`
- `co.com.atlas.model.auth.AuthToken`
- `co.com.atlas.model.auth.gateways.JwtTokenGateway`
- `co.com.atlas.model.auth.gateways.AuthUserRepository`
- `co.com.atlas.model.role.Role`
- `co.com.atlas.model.permission.Permission`
- `co.com.atlas.model.permission.ModulePermission`
- `co.com.atlas.model.module.Module`

### Domain Use Cases
- `co.com.atlas.usecase.auth.LoginUseCase`
- `co.com.atlas.usecase.auth.RefreshTokenUseCase`
- `co.com.atlas.usecase.auth.AuthenticationException`

### Infrastructure - JWT Helper
- `co.com.atlas.jwt.config.JwtProperties`
- `co.com.atlas.jwt.JwtTokenAdapter`

### Infrastructure - Tenant Context
- `co.com.atlas.tenant.TenantContext`

### Infrastructure - R2DBC
- `co.com.atlas.r2dbc.config.MysqlConnectionProperties`
- `co.com.atlas.r2dbc.config.R2dbcConfig`
- `co.com.atlas.r2dbc.config.FlywayConfig`
- `co.com.atlas.r2dbc.authuser.AuthUserEntity`
- `co.com.atlas.r2dbc.authuser.AuthUserReactiveRepository`
- `co.com.atlas.r2dbc.authuser.AuthUserRepositoryAdapter`

### Infrastructure - Reactive Web
- `co.com.atlas.api.config.SecurityConfig`
- `co.com.atlas.api.config.JwtAuthenticationFilter`
- `co.com.atlas.api.config.TenantFilter`
- `co.com.atlas.api.config.OpenApiConfig`
- `co.com.atlas.api.auth.AuthHandler`
- `co.com.atlas.api.auth.AuthRouterRest`
- `co.com.atlas.api.auth.dto.*` (LoginRequest, LoginResponse, etc.)
- `co.com.atlas.api.common.dto.ApiResponse`
- `co.com.atlas.api.common.dto.ErrorResponse`

### Application Config
- `co.com.atlas.config.UseCasesConfig`

## Próximos Pasos Sugeridos

1. **Implementar CRUD de Organizaciones**
2. **Implementar CRUD de Unidades**
3. **Implementar flujo de Visitas**
4. **Implementar generación de códigos QR de acceso**
5. **Añadir notificaciones (WebSockets/Push)**
6. **Implementar cambio de organización activa**

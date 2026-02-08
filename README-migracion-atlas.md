# README - Diseño de Migración para Atlas

## 1. Resumen

**Atlas** es un backend para administrar organizaciones residenciales (condominios) con dos tipos principales:
- **Ciudadela**: Puede tener zonas → torres → apartamentos
- **Conjunto**: Puede tener zonas (opcional) → casas

Este documento detalla el diseño de migración de base de datos, modelo de datos, integración con Clean Architecture y R2DBC, y cómo replica/extiende el modelo de roles/permisos/vistas desde el backend referencial CCP (kodianteach-ssp-backend).

**Objetivos funcionales:**
- Creación de organizaciones y definición de distribución física
- Vinculación de usuarios a unidades (apartamentos/casas)
- Sistema de invitaciones por token
- Roles y permisos por unidad/organización
- Solicitudes de ingreso de visitantes
- Aprobación y generación de QR/código
- Validación en portería (escaneo)

---

## 2. Fuentes Analizadas (con rutas)

### 2.1 CCP Backend (kodianteach-ssp-backend) ✅ ANALIZADO

#### Migraciones Flyway
| Archivo | Propósito |
|---------|-----------|
| `infrastructure/driven-adapters/r2dbc-postgresql/src/main/resources/db/migration/V1__create_initial_schema.sql` | Esquema inicial: users, roles, modules, views, companies, organizations, projects, tickets |
| `infrastructure/driven-adapters/r2dbc-postgresql/src/main/resources/db/migration/V5__create_project_invitations.sql` | Sistema de invitaciones por token |
| `infrastructure/driven-adapters/r2dbc-postgresql/src/main/resources/db/migration/V6__create_psychology_module_schema.sql` | Patrón de módulo específico (psicología) con organization_id |
| `infrastructure/driven-adapters/r2dbc-postgresql/src/main/resources/db/migration/V8__multi_tenant_rbac_schema.sql` | Sistema RBAC multi-tenant: modules, permissions, user_organizations, user_roles_multi |
| `infrastructure/driven-adapters/r2dbc-postgresql/src/main/resources/db/migration/V9__seed_multi_tenant_data.sql` | Datos semilla para permisos granulares |

#### Modelos de Dominio
| Archivo | Propósito |
|---------|-----------|
| `domain/model/src/main/java/co/com/ssp/platform/model/role/Role.java` | Modelo Role: id, name, code, description, moduleCode, isSystem |
| `domain/model/src/main/java/co/com/ssp/platform/model/permission/Permission.java` | Modelo Permission: id, code, name, description, moduleCode, resource, action |
| `domain/model/src/main/java/co/com/ssp/platform/model/organization/Organization.java` | Modelo Organization: id, companyId, code, name, slug, status, enabledModules, userRoles |

#### Adaptadores R2DBC
| Archivo | Propósito |
|---------|-----------|
| `infrastructure/driven-adapters/r2dbc-postgresql/src/main/java/co/com/ssp/platform/r2dbc/organization/OrganizationEntity.java` | Entidad con patrón auditoría: created_at, updated_at, deleted_at |
| `infrastructure/driven-adapters/r2dbc-postgresql/src/main/java/co/com/ssp/platform/r2dbc/organization/OrganizationRepositoryAdapter.java` | Patrón de soft delete y enrichment |
| `infrastructure/driven-adapters/r2dbc-postgresql/README.md` | Estándares de codificación R2DBC |

#### Documentación
| Archivo | Propósito |
|---------|-----------|
| `GRANULAR_PERMISSIONS_GUIDE.md` | Sistema de permisos granulares en JWT |
| `ARQUITECTURA_PROJECT_CLIENT_PROFILE.md` | Patrón de vinculación usuarios-proyectos |

### 2.2 Odyssey - **NO ENCONTRADO EN CÓDIGO**

> ⚠️ **NO ENCONTRADO**: No existe proyecto Odyssey en el workspace analizado. Las propuestas relacionadas se marcan como **[PROPUESTA]**.

### 2.3 SCP - **NO ENCONTRADO EN CÓDIGO**

> ⚠️ **NO ENCONTRADO**: No existe proyecto SCP en el workspace analizado. Las propuestas relacionadas se marcan como **[PROPUESTA]**.

---

## 3. Supuestos y Hallazgos

### 3.1 Hallazgos (encontrados en código)

| Hallazgo | Fuente | Patrón a replicar |
|----------|--------|-------------------|
| Auditoría con `created_at`, `updated_at`, `deleted_at` | V1, V6, V8 | Todas las tablas de Atlas tendrán estos campos |
| Soft delete con `deleted_at IS NULL` | OrganizationRepositoryAdapter.java | Queries filtran por deleted_at IS NULL |
| Multi-tenant con `organization_id` | V6, V8 | Todas las tablas de negocio tendrán organization_id |
| Permisos granulares: resource + action | V9 permissions seed | Permisos tipo: RESOURCE_ACTION (ej: UNITS_CREATE) |
| Roles por organización y usuario | user_roles_multi (V8) | user_id + organization_id + role_id |
| Invitaciones con token | V5 project_invitations | token, expires_at, status, invited_by |
| ENUM para tipos cerrados | V6 document_type, status | ENUMs para tipos de organización y estados |
| Flyway en r2dbc-postgresql | FlywayConfig.java | Migraciones en `db/migration/V{n}__{desc}.sql` |
| Motor MySQL/MariaDB | build.gradle r2dbc-mysql | Engine=InnoDB, CHARSET=utf8mb4 |

### 3.2 NO Encontrados (propuestas)

| Elemento | Estado | Propuesta |
|----------|--------|-----------|
| QR/código de acceso | **[PROPUESTA]** | Tabla `access_codes` con token hash, expiración, estado |
| Log de escaneo/portería | **[PROPUESTA]** | Tabla `access_scan_log` para auditoría de validaciones |
| Zonas/Torres/Unidades | **[PROPUESTA]** | Jerarquía: zones → towers → units (con abstracción) |
| Vinculación usuario-unidad | **[PROPUESTA]** | Tabla `user_units` tipo puente |
| Permisos extra por unidad | **[PROPUESTA]** | Tabla `user_unit_permissions` para permisos adicionales |
| Solicitudes de ingreso | **[PROPUESTA]** | Tabla `visit_requests` con flujo de aprobación |

---

## 4. Modelo Conceptual (MER)

### 4.1 Diagrama de Relaciones

```
                    ┌──────────────────┐
                    │   organization   │
                    │  (organización)  │
                    └────────┬─────────┘
                             │
           ┌─────────────────┼─────────────────┐
           │                 │                 │
           ▼                 ▼                 ▼
  ┌────────────────┐  ┌──────────────┐  ┌──────────────────┐
  │     zone       │  │ org_settings │  │ user_organizations│
  │    (zona)      │  │ (config org) │  │  (membresía)     │
  └───────┬────────┘  └──────────────┘  └──────────────────┘
          │
    ┌─────┴─────┐
    │           │
    ▼           ▼
┌────────┐  ┌────────┐
│ tower  │  │  unit  │ (si conjunto sin zonas,
│ (torre)│  │ (casa) │  unit va directo a org)
└───┬────┘  └────────┘
    │
    ▼
┌────────────┐
│    unit    │
│(apartament)│
└────────────┘

                    ┌──────────────┐
                    │    users     │
                    └──────┬───────┘
                           │
          ┌────────────────┼────────────────┐
          │                │                │
          ▼                ▼                ▼
  ┌────────────────┐ ┌────────────────┐ ┌──────────────────┐
  │  user_units    │ │ invitations    │ │ user_roles_org   │
  │ (vinculación)  │ │ (tokens)       │ │ (roles por org)  │
  └───────┬────────┘ └────────────────┘ └──────────────────┘
          │
          ▼
  ┌────────────────┐
  │user_unit_perms │
  │(permisos extra)│
  └────────────────┘

                    ┌──────────────────┐
                    │  visit_requests  │
                    │ (solicitudes)    │
                    └────────┬─────────┘
                             │
          ┌──────────────────┼──────────────────┐
          │                  │                  │
          ▼                  ▼                  ▼
  ┌────────────────┐ ┌────────────────┐ ┌──────────────────┐
  │ visit_approval │ │  access_codes  │ │  access_scan_log │
  │ (aprobación)   │ │  (QR/código)   │ │ (auditoría scan) │
  └────────────────┘ └────────────────┘ └──────────────────┘
```

### 4.2 Tabla de Relaciones

| Tabla Origen | Relación | Tabla Destino | Cardinalidad |
|--------------|----------|---------------|--------------|
| organization | 1:N | zone | Una org tiene muchas zonas |
| organization | 1:N | unit | Una org tiene muchas unidades (si no usa zonas) |
| zone | 1:N | tower | Una zona tiene muchas torres (ciudadela) |
| zone | 1:N | unit | Una zona tiene muchas casas (conjunto) |
| tower | 1:N | unit | Una torre tiene muchos apartamentos |
| users | N:M | unit | Via user_units |
| users | N:M | organization | Via user_organizations (replicado de CCP) |
| user_units | 1:N | user_unit_permissions | Permisos extra por unidad |
| unit | 1:N | visit_requests | Solicitudes de ingreso |
| visit_requests | 1:1 | access_codes | QR generado al aprobar |
| access_codes | 1:N | access_scan_log | Historial de escaneos |

---

## 5. Diseño de Tablas Propuesto para Atlas

### 5.1 Núcleo Organizacional

#### Tabla: `organization`

**Propósito**: Representa una organización residencial (ciudadela o conjunto).

> **Replica patrón de**: CCP `organization` (V1, V8 multi-tenant)

| Columna | Tipo | Nullable | Descripción |
|---------|------|----------|-------------|
| id | INT | NO | PK AUTO_INCREMENT |
| company_id | INT | NO | FK a company (si aplica, para holding) |
| code | VARCHAR(50) | NO | Código único de organización |
| name | VARCHAR(160) | NO | Nombre de la organización |
| slug | VARCHAR(100) | YES | Slug para URLs |
| type | ENUM('CIUDADELA','CONJUNTO') | NO | **[PROPUESTA]** Tipo de organización |
| uses_zones | BOOLEAN | NO | **[PROPUESTA]** Si usa zonas |
| description | TEXT | YES | Descripción |
| settings | JSON | YES | Configuraciones específicas |
| status | VARCHAR(50) | NO | DEFAULT 'ACTIVE' |
| is_active | BOOLEAN | NO | DEFAULT TRUE |
| created_at | TIMESTAMP | NO | Auditoría |
| updated_at | TIMESTAMP | NO | Auditoría |
| deleted_at | TIMESTAMP | YES | Soft delete |

**Índices**:
- `idx_organization_code` UNIQUE (code)
- `idx_organization_slug` UNIQUE (slug)
- `idx_organization_type` (type)
- `idx_organization_status` (status)

**Constraints**:
- `fk_organization_company` FK (company_id) → company(id)

---

#### Tabla: `zone`

**Propósito**: Zonas dentro de una organización (ej: Zona A, Zona Norte).

> **[PROPUESTA]** - No existe patrón directo en CCP

| Columna | Tipo | Nullable | Descripción |
|---------|------|----------|-------------|
| id | INT | NO | PK AUTO_INCREMENT |
| organization_id | INT | NO | FK a organization |
| code | VARCHAR(50) | NO | Código único dentro de la org |
| name | VARCHAR(100) | NO | Nombre de la zona |
| description | TEXT | YES | Descripción |
| sort_order | INT | NO | DEFAULT 0, orden de visualización |
| is_active | BOOLEAN | NO | DEFAULT TRUE |
| created_at | TIMESTAMP | NO | Auditoría |
| updated_at | TIMESTAMP | NO | Auditoría |
| deleted_at | TIMESTAMP | YES | Soft delete |

**Índices**:
- `idx_zone_org_code` UNIQUE (organization_id, code)
- `idx_zone_organization_id` (organization_id)

**Constraints**:
- `fk_zone_organization` FK (organization_id) → organization(id) ON DELETE CASCADE

---

#### Tabla: `tower`

**Propósito**: Torres dentro de una zona (solo para CIUDADELA).

> **[PROPUESTA]** - No existe patrón directo en CCP

| Columna | Tipo | Nullable | Descripción |
|---------|------|----------|-------------|
| id | INT | NO | PK AUTO_INCREMENT |
| zone_id | INT | NO | FK a zone |
| code | VARCHAR(50) | NO | Código único dentro de la zona |
| name | VARCHAR(100) | NO | Nombre de la torre |
| floors_count | INT | YES | Número de pisos |
| description | TEXT | YES | Descripción |
| sort_order | INT | NO | DEFAULT 0 |
| is_active | BOOLEAN | NO | DEFAULT TRUE |
| created_at | TIMESTAMP | NO | Auditoría |
| updated_at | TIMESTAMP | NO | Auditoría |
| deleted_at | TIMESTAMP | YES | Soft delete |

**Índices**:
- `idx_tower_zone_code` UNIQUE (zone_id, code)
- `idx_tower_zone_id` (zone_id)

**Constraints**:
- `fk_tower_zone` FK (zone_id) → zone(id) ON DELETE CASCADE

---

#### Tabla: `unit`

**Propósito**: Unidad habitacional (apartamento o casa). Abstracción que funciona para ambos tipos de organización.

> **[PROPUESTA]** - Inspirado en patrón multi-tenant de CCP

| Columna | Tipo | Nullable | Descripción |
|---------|------|----------|-------------|
| id | INT | NO | PK AUTO_INCREMENT |
| organization_id | INT | NO | FK a organization |
| zone_id | INT | YES | FK a zone (NULL si org no usa zonas) |
| tower_id | INT | YES | FK a tower (NULL si es conjunto) |
| code | VARCHAR(50) | NO | Código de unidad (ej: "101", "A-15") |
| type | ENUM('APARTMENT','HOUSE') | NO | Tipo de unidad |
| floor | INT | YES | Piso (solo apartamentos) |
| area_sqm | DECIMAL(10,2) | YES | Área en m² |
| bedrooms | INT | YES | Número de habitaciones |
| bathrooms | INT | YES | Número de baños |
| parking_spots | INT | YES | Puestos de parqueo |
| status | ENUM('AVAILABLE','OCCUPIED','MAINTENANCE') | NO | DEFAULT 'AVAILABLE' |
| is_active | BOOLEAN | NO | DEFAULT TRUE |
| created_at | TIMESTAMP | NO | Auditoría |
| updated_at | TIMESTAMP | NO | Auditoría |
| deleted_at | TIMESTAMP | YES | Soft delete |

**Índices**:
- `idx_unit_org_code` UNIQUE (organization_id, code)
- `idx_unit_organization_id` (organization_id)
- `idx_unit_zone_id` (zone_id)
- `idx_unit_tower_id` (tower_id)
- `idx_unit_type` (type)
- `idx_unit_status` (status)

**Constraints**:
- `fk_unit_organization` FK (organization_id) → organization(id)
- `fk_unit_zone` FK (zone_id) → zone(id) ON DELETE SET NULL
- `fk_unit_tower` FK (tower_id) → tower(id) ON DELETE SET NULL

---

### 5.2 Usuarios Vinculados a Unidades

#### Tabla: `user_units`

**Propósito**: Tabla puente para vincular usuarios a unidades. Un usuario puede pertenecer a varias unidades.

> **Replica patrón de**: CCP `user_organizations` (V8) + extensión para unidades

| Columna | Tipo | Nullable | Descripción |
|---------|------|----------|-------------|
| id | INT | NO | PK AUTO_INCREMENT |
| user_id | INT | NO | FK a users |
| unit_id | INT | NO | FK a unit |
| role_id | INT | NO | FK a role (rol dentro de la unidad) |
| ownership_type | ENUM('OWNER','TENANT','FAMILY','GUEST') | NO | Tipo de vinculación |
| is_primary | BOOLEAN | NO | DEFAULT FALSE, si es propietario principal |
| move_in_date | DATE | YES | Fecha de ingreso |
| status | VARCHAR(50) | NO | DEFAULT 'ACTIVE' |
| invited_by | INT | YES | FK a users (quién invitó) |
| joined_at | TIMESTAMP | NO | Fecha de vinculación |
| created_at | TIMESTAMP | NO | Auditoría |
| updated_at | TIMESTAMP | NO | Auditoría |
| deleted_at | TIMESTAMP | YES | Soft delete |

**Índices**:
- `idx_user_units_user_unit` UNIQUE (user_id, unit_id)
- `idx_user_units_user_id` (user_id)
- `idx_user_units_unit_id` (unit_id)
- `idx_user_units_role_id` (role_id)
- `idx_user_units_status` (status)

**Constraints**:
- `fk_user_units_user` FK (user_id) → users(id) ON DELETE CASCADE
- `fk_user_units_unit` FK (unit_id) → unit(id) ON DELETE CASCADE
- `fk_user_units_role` FK (role_id) → role(id)
- `fk_user_units_invited_by` FK (invited_by) → users(id) ON DELETE SET NULL

---

#### Tabla: `user_unit_permissions`

**Propósito**: Permisos adicionales por unidad (además del rol base). Permite permisos extra específicos.

> **Replica patrón de**: CCP `role_permissions` (V8) + scoping por unidad

| Columna | Tipo | Nullable | Descripción |
|---------|------|----------|-------------|
| id | INT | NO | PK AUTO_INCREMENT |
| user_unit_id | INT | NO | FK a user_units |
| permission_id | INT | NO | FK a permissions |
| granted_by | INT | YES | FK a users (quién otorgó) |
| granted_at | TIMESTAMP | NO | Fecha de otorgamiento |
| expires_at | TIMESTAMP | YES | Expiración del permiso (NULL=permanente) |
| created_at | TIMESTAMP | NO | Auditoría |

**Índices**:
- `idx_user_unit_perms_unique` UNIQUE (user_unit_id, permission_id)
- `idx_user_unit_perms_user_unit_id` (user_unit_id)
- `idx_user_unit_perms_permission_id` (permission_id)

**Constraints**:
- `fk_user_unit_perms_user_unit` FK (user_unit_id) → user_units(id) ON DELETE CASCADE
- `fk_user_unit_perms_permission` FK (permission_id) → permissions(id)
- `fk_user_unit_perms_granted_by` FK (granted_by) → users(id) ON DELETE SET NULL

---

#### Tabla: `invitations`

**Propósito**: Invitaciones por token para vincular usuarios a organizaciones/unidades.

> **Replica patrón de**: CCP `project_invitations` (V5)

| Columna | Tipo | Nullable | Descripción |
|---------|------|----------|-------------|
| id | INT | NO | PK AUTO_INCREMENT |
| organization_id | INT | NO | FK a organization |
| unit_id | INT | YES | FK a unit (NULL si invitación a nivel org) |
| email | VARCHAR(255) | NO | Email del invitado |
| invitation_token | VARCHAR(100) | NO | Token único |
| type | ENUM('ORG_MEMBER','UNIT_OWNER','UNIT_TENANT','UNIT_FAMILY') | NO | Tipo de invitación |
| role_id | INT | YES | FK a role (rol inicial) |
| initial_permissions | JSON | YES | IDs de permisos adicionales iniciales |
| status | ENUM('PENDING','ACCEPTED','EXPIRED','CANCELLED') | NO | DEFAULT 'PENDING' |
| invited_by_user_id | INT | NO | FK a users |
| expires_at | TIMESTAMP | NO | Fecha de expiración |
| accepted_at | TIMESTAMP | YES | Fecha de aceptación |
| created_at | TIMESTAMP | NO | Auditoría |
| updated_at | TIMESTAMP | NO | Auditoría |

**Índices**:
- `idx_invitations_token` UNIQUE (invitation_token)
- `idx_invitations_organization_id` (organization_id)
- `idx_invitations_unit_id` (unit_id)
- `idx_invitations_email` (email)
- `idx_invitations_status` (status)
- `idx_invitations_expires_at` (expires_at)

**Constraints**:
- `fk_invitations_organization` FK (organization_id) → organization(id) ON DELETE CASCADE
- `fk_invitations_unit` FK (unit_id) → unit(id) ON DELETE CASCADE
- `fk_invitations_role` FK (role_id) → role(id)
- `fk_invitations_invited_by` FK (invited_by_user_id) → users(id)

---

### 5.3 Solicitudes de Ingreso y Validación

#### Tabla: `visit_requests`

**Propósito**: Solicitudes de ingreso de visitantes a una unidad.

> **[PROPUESTA]** - No existe patrón directo en CCP

| Columna | Tipo | Nullable | Descripción |
|---------|------|----------|-------------|
| id | INT | NO | PK AUTO_INCREMENT |
| organization_id | INT | NO | FK a organization |
| unit_id | INT | NO | FK a unit |
| requested_by | INT | NO | FK a users (quien solicita) |
| visitor_name | VARCHAR(200) | NO | Nombre del visitante |
| visitor_document | VARCHAR(50) | YES | Documento del visitante |
| visitor_phone | VARCHAR(40) | YES | Teléfono del visitante |
| visitor_email | VARCHAR(160) | YES | Email del visitante |
| vehicle_plate | VARCHAR(20) | YES | Placa del vehículo |
| purpose | VARCHAR(255) | YES | Motivo de la visita |
| valid_from | TIMESTAMP | NO | Inicio de validez |
| valid_until | TIMESTAMP | NO | Fin de validez |
| recurrence_type | ENUM('ONCE','DAILY','WEEKLY','MONTHLY') | NO | DEFAULT 'ONCE' |
| recurrence_days | JSON | YES | Días de recurrencia (ej: [1,3,5] = L,M,V) |
| max_entries | INT | YES | Máximo de entradas (NULL=ilimitado) |
| status | ENUM('PENDING','APPROVED','REJECTED','EXPIRED','CANCELLED') | NO | DEFAULT 'PENDING' |
| notes | TEXT | YES | Notas adicionales |
| created_at | TIMESTAMP | NO | Auditoría |
| updated_at | TIMESTAMP | NO | Auditoría |
| deleted_at | TIMESTAMP | YES | Soft delete |

**Índices**:
- `idx_visit_requests_organization_id` (organization_id)
- `idx_visit_requests_unit_id` (unit_id)
- `idx_visit_requests_requested_by` (requested_by)
- `idx_visit_requests_status` (status)
- `idx_visit_requests_valid_from` (valid_from)
- `idx_visit_requests_valid_until` (valid_until)

**Constraints**:
- `fk_visit_requests_organization` FK (organization_id) → organization(id)
- `fk_visit_requests_unit` FK (unit_id) → unit(id)
- `fk_visit_requests_requested_by` FK (requested_by) → users(id)

---

#### Tabla: `visit_approvals`

**Propósito**: Registro de aprobaciones/rechazos de solicitudes.

> **[PROPUESTA]** - Inspirado en patrón de auditoría de CCP

| Columna | Tipo | Nullable | Descripción |
|---------|------|----------|-------------|
| id | INT | NO | PK AUTO_INCREMENT |
| visit_request_id | INT | NO | FK a visit_requests |
| approved_by | INT | NO | FK a users |
| action | ENUM('APPROVED','REJECTED') | NO | Acción tomada |
| comments | TEXT | YES | Comentarios de aprobación/rechazo |
| created_at | TIMESTAMP | NO | Auditoría |

**Índices**:
- `idx_visit_approvals_visit_request_id` (visit_request_id)
- `idx_visit_approvals_approved_by` (approved_by)

**Constraints**:
- `fk_visit_approvals_visit_request` FK (visit_request_id) → visit_requests(id) ON DELETE CASCADE
- `fk_visit_approvals_approved_by` FK (approved_by) → users(id)

---

#### Tabla: `access_codes`

**Propósito**: Códigos QR/tokens generados al aprobar una solicitud.

> **[PROPUESTA]** - No existe patrón directo en CCP

| Columna | Tipo | Nullable | Descripción |
|---------|------|----------|-------------|
| id | INT | NO | PK AUTO_INCREMENT |
| visit_request_id | INT | NO | FK a visit_requests |
| code_hash | VARCHAR(128) | NO | Hash del código (SHA-256) |
| code_short | VARCHAR(10) | YES | Código corto para entrada manual |
| qr_data | TEXT | NO | Datos para generar QR |
| valid_from | TIMESTAMP | NO | Inicio de validez |
| valid_until | TIMESTAMP | NO | Fin de validez |
| max_uses | INT | YES | Máximo de usos (NULL=ilimitado) |
| current_uses | INT | NO | DEFAULT 0 |
| status | ENUM('ACTIVE','EXPIRED','REVOKED','EXHAUSTED') | NO | DEFAULT 'ACTIVE' |
| generated_by | INT | NO | FK a users |
| generated_at | TIMESTAMP | NO | Fecha de generación |
| revoked_at | TIMESTAMP | YES | Fecha de revocación |
| revoked_by | INT | YES | FK a users |
| created_at | TIMESTAMP | NO | Auditoría |
| updated_at | TIMESTAMP | NO | Auditoría |

**Índices**:
- `idx_access_codes_code_hash` UNIQUE (code_hash)
- `idx_access_codes_code_short` (code_short)
- `idx_access_codes_visit_request_id` (visit_request_id)
- `idx_access_codes_status` (status)
- `idx_access_codes_valid_until` (valid_until)

**Constraints**:
- `fk_access_codes_visit_request` FK (visit_request_id) → visit_requests(id) ON DELETE CASCADE
- `fk_access_codes_generated_by` FK (generated_by) → users(id)
- `fk_access_codes_revoked_by` FK (revoked_by) → users(id)

---

#### Tabla: `access_scan_log`

**Propósito**: Log de escaneos/validaciones en portería.

> **[PROPUESTA]** - Inspirado en ticket_history de CCP (V1)

| Columna | Tipo | Nullable | Descripción |
|---------|------|----------|-------------|
| id | INT | NO | PK AUTO_INCREMENT |
| access_code_id | INT | NO | FK a access_codes |
| organization_id | INT | NO | FK a organization |
| scanned_by | INT | NO | FK a users (guardia/portero) |
| scan_result | ENUM('VALID','INVALID','EXPIRED','ALREADY_USED','REVOKED') | NO | Resultado |
| scan_location | VARCHAR(100) | YES | Punto de acceso (ej: "Portería Principal") |
| visitor_name_verified | VARCHAR(200) | YES | Nombre verificado en portería |
| visitor_document_verified | VARCHAR(50) | YES | Documento verificado |
| notes | TEXT | YES | Notas del guardia |
| created_at | TIMESTAMP | NO | Fecha/hora del escaneo |

**Índices**:
- `idx_access_scan_log_access_code_id` (access_code_id)
- `idx_access_scan_log_organization_id` (organization_id)
- `idx_access_scan_log_scanned_by` (scanned_by)
- `idx_access_scan_log_scan_result` (scan_result)
- `idx_access_scan_log_created_at` (created_at)

**Constraints**:
- `fk_access_scan_log_access_code` FK (access_code_id) → access_codes(id)
- `fk_access_scan_log_organization` FK (organization_id) → organization(id)
- `fk_access_scan_log_scanned_by` FK (scanned_by) → users(id)

---

## 6. Replicación del Modelo Roles/Permisos/Vistas desde CCP

### 6.1 Tablas Existentes en CCP (a reutilizar)

| Tabla | Ubicación | Propósito en Atlas |
|-------|-----------|-------------------|
| `users` | V1 | Usuarios del sistema |
| `role` | V1, V8 | Roles base (ADMIN, OWNER, TENANT, SECURITY, etc.) |
| `permissions` | V8 | Permisos granulares (RESOURCE_ACTION) |
| `role_permissions` | V8 | Asignación de permisos a roles |
| `modules` | V8 | Módulos del sistema (ATLAS) |
| `organization_modules` | V8 | Módulos habilitados por organización |
| `user_organizations` | V8 | Membresía usuarios-organizaciones |
| `user_roles_multi` | V8 | Roles por usuario y organización |

### 6.2 Tablas que Replica Atlas Igual

| Tabla CCP | Tabla Atlas | Cambios |
|-----------|-------------|---------|
| `users` | `users` | Sin cambios |
| `role` | `role` | Nuevos roles específicos de Atlas |
| `permissions` | `permissions` | Nuevos permisos específicos de Atlas |
| `role_permissions` | `role_permissions` | Sin cambios |
| `user_organizations` | `user_organizations` | Sin cambios |
| `user_roles_multi` | `user_roles_multi` | Sin cambios |

### 6.3 Extensiones que Agrega Atlas

| Elemento | Extensión | Propósito |
|----------|-----------|-----------|
| Scope por unidad | `user_units` + `user_unit_permissions` | Roles y permisos a nivel de unidad, no solo organización |
| Tipo organización | `organization.type` ENUM | Diferenciar CIUDADELA vs CONJUNTO |
| Jerarquía física | `zone`, `tower`, `unit` | Estructura de la organización |
| Invitaciones | `invitations` con scope a unidad | Extender project_invitations |
| Accesos | `visit_requests`, `access_codes`, `access_scan_log` | Flujo de visitas |

### 6.4 Permisos Propuestos para Atlas (basados en patrón CCP)

```sql
-- Permisos del Módulo ATLAS (siguiendo patrón de V9__seed_multi_tenant_data.sql)
INSERT INTO permissions (code, name, description, module_code, resource, action) VALUES
-- Gestión de organizaciones (extendido)
('ATLAS_ORGANIZATIONS_CREATE', 'Crear organizaciones residenciales', 'Permite crear condominios', 'ATLAS', 'organizations', 'CREATE'),
('ATLAS_ORGANIZATIONS_READ', 'Ver organizaciones', 'Permite ver organizaciones', 'ATLAS', 'organizations', 'READ'),
('ATLAS_ORGANIZATIONS_UPDATE', 'Actualizar organizaciones', 'Permite editar organizaciones', 'ATLAS', 'organizations', 'UPDATE'),
('ATLAS_ORGANIZATIONS_DELETE', 'Eliminar organizaciones', 'Permite eliminar organizaciones', 'ATLAS', 'organizations', 'DELETE'),
('ATLAS_ORGANIZATIONS_MANAGE', 'Gestionar organizaciones', 'Permisos completos', 'ATLAS', 'organizations', 'MANAGE'),

-- Gestión de zonas
('ATLAS_ZONES_CREATE', 'Crear zonas', 'Permite crear zonas', 'ATLAS', 'zones', 'CREATE'),
('ATLAS_ZONES_READ', 'Ver zonas', 'Permite ver zonas', 'ATLAS', 'zones', 'READ'),
('ATLAS_ZONES_UPDATE', 'Actualizar zonas', 'Permite editar zonas', 'ATLAS', 'zones', 'UPDATE'),
('ATLAS_ZONES_DELETE', 'Eliminar zonas', 'Permite eliminar zonas', 'ATLAS', 'zones', 'DELETE'),

-- Gestión de torres
('ATLAS_TOWERS_CREATE', 'Crear torres', 'Permite crear torres', 'ATLAS', 'towers', 'CREATE'),
('ATLAS_TOWERS_READ', 'Ver torres', 'Permite ver torres', 'ATLAS', 'towers', 'READ'),
('ATLAS_TOWERS_UPDATE', 'Actualizar torres', 'Permite editar torres', 'ATLAS', 'towers', 'UPDATE'),
('ATLAS_TOWERS_DELETE', 'Eliminar torres', 'Permite eliminar torres', 'ATLAS', 'towers', 'DELETE'),

-- Gestión de unidades
('ATLAS_UNITS_CREATE', 'Crear unidades', 'Permite registrar apartamentos/casas', 'ATLAS', 'units', 'CREATE'),
('ATLAS_UNITS_READ', 'Ver unidades', 'Permite ver unidades', 'ATLAS', 'units', 'READ'),
('ATLAS_UNITS_UPDATE', 'Actualizar unidades', 'Permite editar unidades', 'ATLAS', 'units', 'UPDATE'),
('ATLAS_UNITS_DELETE', 'Eliminar unidades', 'Permite eliminar unidades', 'ATLAS', 'units', 'DELETE'),
('ATLAS_UNITS_MANAGE', 'Gestionar unidades', 'Permisos completos', 'ATLAS', 'units', 'MANAGE'),

-- Invitaciones
('ATLAS_INVITATIONS_CREATE', 'Crear invitaciones', 'Permite invitar usuarios', 'ATLAS', 'invitations', 'CREATE'),
('ATLAS_INVITATIONS_READ', 'Ver invitaciones', 'Permite ver invitaciones', 'ATLAS', 'invitations', 'READ'),
('ATLAS_INVITATIONS_CANCEL', 'Cancelar invitaciones', 'Permite cancelar invitaciones', 'ATLAS', 'invitations', 'CANCEL'),

-- Solicitudes de ingreso
('ATLAS_VISITS_CREATE', 'Crear solicitudes de ingreso', 'Permite solicitar ingreso de visitantes', 'ATLAS', 'visits', 'CREATE'),
('ATLAS_VISITS_READ', 'Ver solicitudes', 'Permite ver solicitudes propias', 'ATLAS', 'visits', 'READ'),
('ATLAS_VISITS_READ_ALL', 'Ver todas las solicitudes', 'Permite ver todas las solicitudes de la org', 'ATLAS', 'visits', 'READ_ALL'),
('ATLAS_VISITS_APPROVE', 'Aprobar solicitudes', 'Permite aprobar/rechazar solicitudes', 'ATLAS', 'visits', 'APPROVE'),
('ATLAS_VISITS_CANCEL', 'Cancelar solicitudes', 'Permite cancelar solicitudes', 'ATLAS', 'visits', 'CANCEL'),

-- Control de acceso
('ATLAS_ACCESS_SCAN', 'Escanear códigos', 'Permite validar códigos en portería', 'ATLAS', 'access', 'SCAN'),
('ATLAS_ACCESS_REVOKE', 'Revocar códigos', 'Permite revocar códigos de acceso', 'ATLAS', 'access', 'REVOKE'),
('ATLAS_ACCESS_VIEW_LOG', 'Ver log de accesos', 'Permite ver historial de escaneos', 'ATLAS', 'access', 'VIEW_LOG');
```

### 6.5 Roles Propuestos para Atlas

```sql
-- Roles multi-tenant Atlas (siguiendo patrón de V9)
INSERT INTO role (code, name, description, module_code, is_system) VALUES
('ATLAS_SUPER_ADMIN', 'Super Administrador Atlas', 'Control total del sistema', 'ATLAS', TRUE),
('ATLAS_ADMIN', 'Administrador de Organización', 'Administrador de un condominio', 'ATLAS', TRUE),
('ATLAS_OWNER', 'Propietario', 'Propietario de unidad', 'ATLAS', TRUE),
('ATLAS_TENANT', 'Arrendatario', 'Arrendatario de unidad', 'ATLAS', TRUE),
('ATLAS_FAMILY', 'Familiar/Allegado', 'Miembro de familia de residente', 'ATLAS', TRUE),
('ATLAS_SECURITY', 'Seguridad/Portería', 'Personal de seguridad', 'ATLAS', TRUE),
('ATLAS_GUEST', 'Invitado', 'Usuario invitado temporal', 'ATLAS', TRUE);
```

---

## 7. Flujos End-to-End Documentados

### 7.1 Creación de Organización por Admin

```
1. POST /api/atlas/organizations
   Body: {
     "name": "Ciudadela Los Pinos",
     "code": "CIUDADELA_LOS_PINOS",
     "type": "CIUDADELA",
     "usesZones": true
   }

2. Backend:
   a) Valida permisos: ATLAS_ORGANIZATIONS_CREATE
   b) Genera slug: "ciudadela-los-pinos"
   c) Inserta en organization con type='CIUDADELA', uses_zones=true
   d) Crea registro en organization_modules (module_code='ATLAS')
   e) Asigna rol ATLAS_ADMIN al creador via user_organizations + user_roles_multi

3. Response: Organization creada con ID y slug
```

**Tablas involucradas**: `organization`, `organization_modules`, `user_organizations`, `user_roles_multi`

---

### 7.2 Definición de Distribución (ciudadela/conjunto, zonas sí/no)

```
1. PUT /api/atlas/organizations/{id}/distribution
   Body: {
     "zones": [
       {
         "code": "ZONA_A",
         "name": "Zona A",
         "towers": [
           {
             "code": "TORRE_1",
             "name": "Torre 1",
             "floorsCount": 10
           }
         ]
       }
     ]
   }

2. Backend:
   a) Valida permisos: ATLAS_ORGANIZATIONS_MANAGE
   b) Valida coherencia (ciudadela requiere torres, conjunto no)
   c) Crea zonas en tabla zone
   d) Crea torres en tabla tower (si ciudadela)
   e) NO crea unidades (se crean después o por autorregistro)

3. Response: Distribución creada
```

**Tablas involucradas**: `zone`, `tower`

---

### 7.3 Registro Propietario por Link/Token

```
1. GET /api/atlas/invitations/accept?token=abc123

2. Backend:
   a) Busca invitación por token en tabla invitations
   b) Valida que no esté expirada (expires_at > NOW())
   c) Valida status='PENDING'
   d) Obtiene organization_id, unit_id, role_id, initial_permissions

3. Frontend muestra formulario:
   - Si unit_id existe: solo confirmar datos
   - Si unit_id es NULL: mostrar selector de zona/torre/unidad

4. POST /api/atlas/invitations/complete
   Body: {
     "token": "abc123",
     "unitCode": "101",  // si no venía en invitación
     "zoneId": 1,        // si aplica
     "towerId": 2        // si aplica (ciudadela)
   }

5. Backend:
   a) Crea/actualiza usuario en users
   b) Crea registro en user_organizations
   c) Crea registro en user_units con role_id
   d) Crea registros en user_unit_permissions (si initial_permissions)
   e) Actualiza invitación: status='ACCEPTED', accepted_at=NOW()

6. Response: Vinculación exitosa + token JWT
```

**Tablas involucradas**: `invitations`, `users`, `user_organizations`, `user_units`, `user_unit_permissions`

---

### 7.4 Vinculación a Unidad (zona/torre/código unidad)

```
1. POST /api/atlas/units/{unitId}/link
   Body: {
     "userId": 123,
     "roleCode": "ATLAS_OWNER",
     "ownershipType": "OWNER",
     "isPrimary": true
   }

2. Backend:
   a) Valida permisos: ATLAS_UNITS_MANAGE o ser admin de org
   b) Valida que unidad exista y esté activa
   c) Valida que usuario no esté ya vinculado
   d) Obtiene role_id por code
   e) Inserta en user_units

3. Response: Vinculación creada
```

**Tablas involucradas**: `unit`, `user_units`, `role`

---

### 7.5 Invitación de Usuarios a Unidad

```
1. POST /api/atlas/units/{unitId}/invite
   Body: {
     "email": "familiar@email.com",
     "type": "UNIT_FAMILY",
     "roleCode": "ATLAS_FAMILY",
     "additionalPermissions": ["ATLAS_VISITS_CREATE"]
   }

2. Backend:
   a) Valida permisos: ATLAS_INVITATIONS_CREATE + estar vinculado a la unidad
   b) Genera token único (UUID)
   c) Calcula expires_at (ej: 7 días)
   d) Inserta en invitations con unit_id, role_id, initial_permissions JSON
   e) Envía email con link de invitación

3. Response: Invitación creada + token (para compartir)
```

**Tablas involucradas**: `invitations`, `permissions`

---

### 7.6 Solicitud de Ingreso

```
1. POST /api/atlas/visits
   Body: {
     "unitId": 45,
     "visitorName": "Juan Pérez",
     "visitorDocument": "12345678",
     "visitorPhone": "+57 300 1234567",
     "purpose": "Visita familiar",
     "validFrom": "2026-02-15T08:00:00",
     "validUntil": "2026-02-15T18:00:00",
     "recurrenceType": "ONCE"
   }

2. Backend:
   a) Valida permisos: ATLAS_VISITS_CREATE
   b) Valida que usuario esté vinculado a la unidad
   c) Valida rangos de fecha coherentes
   d) Inserta en visit_requests con status='PENDING'
   e) Notifica a usuarios con permiso ATLAS_VISITS_APPROVE de la unidad

3. Response: Solicitud creada en estado PENDING
```

**Tablas involucradas**: `visit_requests`, `user_units`

---

### 7.7 Aprobación y Generación de QR/Código

```
1. POST /api/atlas/visits/{visitId}/approve
   Body: {
     "comments": "Aprobado, es familiar conocido"
   }

2. Backend:
   a) Valida permisos: ATLAS_VISITS_APPROVE
   b) Valida que solicitud esté en PENDING
   c) Actualiza visit_requests: status='APPROVED'
   d) Inserta en visit_approvals
   e) Genera código único:
      - code_hash = SHA256(UUID + timestamp + salt)
      - code_short = primeros 6 caracteres alfanuméricos
      - qr_data = JSON con info para escaneo
   f) Inserta en access_codes
   g) Genera imagen QR (servicio externo o biblioteca)
   h) Envía QR por WhatsApp/Email al solicitante

3. Response: {
     "visitId": 123,
     "accessCode": {
       "codeShort": "ABC123",
       "qrImageUrl": "https://...",
       "validFrom": "2026-02-15T08:00:00",
       "validUntil": "2026-02-15T18:00:00"
     }
   }
```

**Tablas involucradas**: `visit_requests`, `visit_approvals`, `access_codes`

---

### 7.8 Validación en Portería (Escaneo)

```
1. POST /api/atlas/access/validate
   Body: {
     "codeHash": "abc...xyz", // o codeShort
     "scanLocation": "Portería Principal"
   }

2. Backend:
   a) Valida permisos: ATLAS_ACCESS_SCAN (usuario portería)
   b) Busca en access_codes por code_hash o code_short
   c) Valida:
      - status = 'ACTIVE'
      - valid_from <= NOW() <= valid_until
      - current_uses < max_uses (si max_uses no es NULL)
   d) Si válido:
      - Incrementa current_uses
      - Si current_uses >= max_uses → status='EXHAUSTED'
   e) Inserta en access_scan_log con resultado

3. Response: {
     "valid": true,
     "visitorName": "Juan Pérez",
     "unitCode": "101",
     "purpose": "Visita familiar",
     "message": "Acceso autorizado"
   }
   
   O si inválido:
   {
     "valid": false,
     "reason": "EXPIRED",
     "message": "Código expirado"
   }
```

**Tablas involucradas**: `access_codes`, `access_scan_log`, `visit_requests`

---

## 8. Convención de Migraciones

### 8.1 Versionado Flyway

> **Basado en**: CCP `FlywayConfig.java` y estructura de `db/migration/`

| Patrón | Ejemplo | Descripción |
|--------|---------|-------------|
| Versión | `V{n}__` | Número secuencial |
| Nombre | `{descripción}.sql` | snake_case descriptivo |
| Completo | `V1__create_atlas_core_schema.sql` | - |

### 8.2 Naming Estándar de Scripts

```
V1__create_atlas_core_schema.sql       # Tablas base: extension organization, zone, tower, unit
V2__create_user_units_schema.sql       # Vinculación usuarios-unidades
V3__create_invitations_schema.sql      # Sistema de invitaciones
V4__create_visits_schema.sql           # Solicitudes de ingreso
V5__create_access_codes_schema.sql     # QR/códigos y log de escaneo
V6__seed_atlas_permissions.sql         # Permisos del módulo ATLAS
V7__seed_atlas_roles.sql               # Roles del módulo ATLAS
V8__seed_role_permissions.sql          # Asignación permisos-roles
```

### 8.3 Orden Recomendado de Scripts

1. **Extensión/modificación** de tablas existentes (organization)
2. **Tablas de estructura** (zone, tower, unit)
3. **Tablas de vinculación** (user_units, user_unit_permissions)
4. **Tablas de invitaciones** (invitations)
5. **Tablas de solicitudes** (visit_requests, visit_approvals)
6. **Tablas de acceso** (access_codes, access_scan_log)
7. **Seeds de permisos** (permissions)
8. **Seeds de roles** (role)
9. **Seeds de asignaciones** (role_permissions)

### 8.4 Seeds Mínimos

> **Basado en patrón de**: CCP V9__seed_multi_tenant_data.sql

```sql
-- Módulo ATLAS
INSERT INTO modules (code, name, description, is_active) VALUES
('ATLAS', 'Módulo Atlas', 'Gestión de organizaciones residenciales (condominios)', TRUE);

-- Permisos (ver sección 6.4)
-- Roles (ver sección 6.5)
-- Asignación role_permissions (basado en matriz de roles-permisos)
```

---

## 9. Ubicación en la Arquitectura (Infra / R2DBC)

### 9.1 Dónde Van las Migraciones

```
atlas/
└── infrastructure/
    └── driven-adapters/
        └── r2dbc-postgresql/              # O r2dbc-mysql según motor
            └── src/
                └── main/
                    └── resources/
                        └── db/
                            └── migration/
                                ├── V1__create_atlas_core_schema.sql
                                ├── V2__create_user_units_schema.sql
                                └── ...
```

> **Replica patrón de**: CCP `infrastructure/driven-adapters/r2dbc-postgresql/src/main/resources/db/migration/`

### 9.2 Dónde Van los Repos/Adapters R2DBC

```
atlas/
└── infrastructure/
    └── driven-adapters/
        └── r2dbc-postgresql/
            └── src/
                └── main/
                    └── java/
                        └── co/
                            └── com/
                                └── atlas/
                                    └── r2dbc/
                                        ├── config/
                                        │   ├── FlywayConfig.java
                                        │   ├── R2dbcConfig.java
                                        │   └── ConnectionPool.java
                                        ├── organization/
                                        │   ├── OrganizationEntity.java
                                        │   ├── OrganizationReactiveRepository.java
                                        │   └── OrganizationRepositoryAdapter.java
                                        ├── zone/
                                        │   ├── ZoneEntity.java
                                        │   ├── ZoneReactiveRepository.java
                                        │   └── ZoneRepositoryAdapter.java
                                        ├── tower/
                                        │   └── ...
                                        ├── unit/
                                        │   └── ...
                                        ├── userunit/
                                        │   └── ...
                                        ├── invitation/
                                        │   └── ...
                                        ├── visitrequest/
                                        │   └── ...
                                        ├── accesscode/
                                        │   └── ...
                                        ├── accessscanlog/
                                        │   └── ...
                                        ├── mapper/
                                        │   ├── OrganizationMapper.java
                                        │   ├── ZoneMapper.java
                                        │   └── ...
                                        └── helper/
                                            └── ReactiveAdapterOperations.java
```

> **Replica patrón de**: CCP `infrastructure/driven-adapters/r2dbc-postgresql/src/main/java/co/com/ssp/platform/r2dbc/`

### 9.3 Qué NO Debe Ir en Dominio

| NO en Dominio | Razón | Dónde Sí |
|---------------|-------|----------|
| Anotaciones `@Table`, `@Column`, `@Id` | Son detalles de infraestructura | Entity en r2dbc/ |
| `ReactiveCrudRepository` | Dependencia de Spring Data | r2dbc/ |
| Nombres de columnas DB (snake_case) | Detalle de persistencia | Entity en r2dbc/ |
| Queries SQL | Infraestructura | RepositoryAdapter en r2dbc/ |
| Configuración Flyway | Infraestructura | config/ en r2dbc/ |
| Soft delete logic (`deleted_at`) | Puede ser infra o dominio | Preferir en Adapter |

### 9.4 Estructura de Dominio (Solo Conceptual)

```
atlas/
└── domain/
    └── model/
        └── src/
            └── main/
                └── java/
                    └── co/
                        └── com/
                            └── atlas/
                                └── model/
                                    ├── organization/
                                    │   ├── Organization.java        # @Builder, @Getter
                                    │   ├── OrganizationType.java    # Enum CIUDADELA, CONJUNTO
                                    │   └── gateways/
                                    │       └── OrganizationRepository.java  # Interface
                                    ├── zone/
                                    │   ├── Zone.java
                                    │   └── gateways/
                                    │       └── ZoneRepository.java
                                    ├── tower/
                                    ├── unit/
                                    │   ├── Unit.java
                                    │   ├── UnitType.java            # Enum APARTMENT, HOUSE
                                    │   └── gateways/
                                    ├── userunit/
                                    │   ├── UserUnit.java
                                    │   ├── OwnershipType.java       # Enum OWNER, TENANT, ...
                                    │   └── gateways/
                                    ├── invitation/
                                    ├── visit/
                                    │   ├── VisitRequest.java
                                    │   ├── VisitStatus.java         # Enum
                                    │   └── gateways/
                                    └── access/
                                        ├── AccessCode.java
                                        ├── AccessScanLog.java
                                        └── gateways/
```

> **Replica patrón de**: CCP `domain/model/src/main/java/co/com/ssp/platform/model/`

---

## 10. Extensión: Posts / Anuncios / Encuestas / Notificaciones

### 10.1 Objetivo Funcional

Agregar un módulo donde **solo los administradores** de la organización puedan publicar contenido:

- **Anuncios / Comunicados** (ej: "Mantenimiento el 10/02/2026")
- **Publicidad interna** (solo admins crean)
- **Encuestas** (pregunta + opciones + votos)

**Propietarios/usuarios pueden:**
- Recibir notificaciones cuando se publica o actualiza un post
- Comentar únicamente si el admin habilita comentarios en el post
- Votar en encuestas (una vez por encuesta, según configuración)

> **[PROPUESTA]** - No existe patrón directo de posts/encuestas en CCP. Se replica auditoría y multi-tenant existentes.

---

### 10.2 Diseño de Tablas

#### Tabla: `organization_posts`

**Propósito**: Almacenar publicaciones creadas por admins dentro de una organización.

> **[PROPUESTA]** - Replica patrones de auditoría y soft-delete de CCP

| Columna | Tipo | Nullable | Descripción |
|---------|------|----------|-------------|
| id | INT | NO | PK AUTO_INCREMENT |
| organization_id | INT | NO | FK a organization (multi-tenant) |
| created_by_user_id | INT | NO | FK a users (admin autor) |
| type | ENUM('ANNOUNCEMENT','POLL','ADVERTISEMENT') | NO | Tipo de publicación |
| title | VARCHAR(255) | NO | Título del post |
| content | TEXT | YES | Contenido (markdown o texto plano) |
| status | ENUM('DRAFT','PUBLISHED','ARCHIVED') | NO | DEFAULT 'DRAFT' |
| published_at | TIMESTAMP | YES | NULL si es draft |
| allow_comments | BOOLEAN | NO | DEFAULT FALSE |
| notify_on_publish | BOOLEAN | NO | DEFAULT TRUE |
| target_audience | ENUM('ALL_MEMBERS','OWNERS_ONLY','ADMINS_ONLY') | NO | DEFAULT 'ALL_MEMBERS' **[PROPUESTA]** |
| is_pinned | BOOLEAN | NO | DEFAULT FALSE, fijado en el tablero |
| created_at | TIMESTAMP | NO | Auditoría |
| updated_at | TIMESTAMP | NO | Auditoría |
| deleted_at | TIMESTAMP | YES | Soft delete |

**Índices**:
- `idx_org_posts_organization_id` (organization_id)
- `idx_org_posts_status_published` (status, published_at)
- `idx_org_posts_type` (type)
- `idx_org_posts_created_by` (created_by_user_id)
- `idx_org_posts_is_pinned` (is_pinned, published_at DESC)

**Constraints**:
- `fk_org_posts_organization` FK (organization_id) → organization(id) ON DELETE CASCADE
- `fk_org_posts_created_by` FK (created_by_user_id) → users(id)

---

#### Tabla: `organization_post_attachments`

**Propósito**: Adjuntos (imágenes/PDF/links) asociados a posts.

> **[PROPUESTA]** - No existe patrón de attachments en CCP analizado

| Columna | Tipo | Nullable | Descripción |
|---------|------|----------|-------------|
| id | INT | NO | PK AUTO_INCREMENT |
| post_id | INT | NO | FK a organization_posts |
| file_name | VARCHAR(255) | NO | Nombre original del archivo |
| file_type | VARCHAR(100) | YES | MIME type (image/png, application/pdf) |
| storage_key | VARCHAR(500) | NO | Clave en storage (S3, etc.) |
| file_url | VARCHAR(1000) | YES | URL pública (si aplica) |
| file_size_bytes | BIGINT | YES | Tamaño en bytes |
| metadata | JSON | YES | Metadata adicional |
| sort_order | INT | NO | DEFAULT 0 |
| created_at | TIMESTAMP | NO | Auditoría |
| deleted_at | TIMESTAMP | YES | Soft delete |

**Índices**:
- `idx_post_attachments_post_id` (post_id)
- `idx_post_attachments_file_type` (file_type)

**Constraints**:
- `fk_post_attachments_post` FK (post_id) → organization_posts(id) ON DELETE CASCADE

---

#### Tabla: `organization_post_comments`

**Propósito**: Comentarios de propietarios/usuarios en posts con `allow_comments = true`.

> **[PROPUESTA]** - Inspirado en ticket_comments_multi de CCP (V8)

| Columna | Tipo | Nullable | Descripción |
|---------|------|----------|-------------|
| id | INT | NO | PK AUTO_INCREMENT |
| post_id | INT | NO | FK a organization_posts |
| organization_id | INT | NO | FK a organization (multi-tenant filter) |
| author_user_id | INT | NO | FK a users |
| content | TEXT | NO | Contenido del comentario |
| status | ENUM('VISIBLE','HIDDEN','DELETED') | NO | DEFAULT 'VISIBLE' **[PROPUESTA]** |
| parent_comment_id | INT | YES | FK a self (para hilos/respuestas) |
| created_at | TIMESTAMP | NO | Auditoría |
| updated_at | TIMESTAMP | NO | Auditoría |
| deleted_at | TIMESTAMP | YES | Soft delete |

**Índices**:
- `idx_post_comments_post_id` (post_id)
- `idx_post_comments_organization_id` (organization_id)
- `idx_post_comments_author` (author_user_id)
- `idx_post_comments_parent` (parent_comment_id)
- `idx_post_comments_status` (status)

**Constraints**:
- `fk_post_comments_post` FK (post_id) → organization_posts(id) ON DELETE CASCADE
- `fk_post_comments_organization` FK (organization_id) → organization(id)
- `fk_post_comments_author` FK (author_user_id) → users(id)
- `fk_post_comments_parent` FK (parent_comment_id) → organization_post_comments(id) ON DELETE CASCADE

**Reglas de negocio**:
- Solo permitir INSERT si `organization_posts.allow_comments = true`
- Admins pueden moderar (cambiar status a HIDDEN)

---

#### Tabla: `organization_post_polls`

**Propósito**: Configuración de encuesta asociada a un post con `type = 'POLL'`.

> **[PROPUESTA]** - No existe patrón de encuestas en CCP

| Columna | Tipo | Nullable | Descripción |
|---------|------|----------|-------------|
| id | INT | NO | PK AUTO_INCREMENT |
| post_id | INT | NO | FK a organization_posts (UNIQUE, 1:1) |
| question | VARCHAR(500) | YES | Pregunta (si no usa title/content del post) |
| allow_multiple_answers | BOOLEAN | NO | DEFAULT FALSE |
| starts_at | TIMESTAMP | YES | Inicio de votación (NULL = inmediato) |
| ends_at | TIMESTAMP | YES | Fin de votación (NULL = sin límite) |
| is_anonymous | BOOLEAN | NO | DEFAULT FALSE **[PROPUESTA]** |
| show_results_before_end | BOOLEAN | NO | DEFAULT FALSE |
| created_at | TIMESTAMP | NO | Auditoría |
| updated_at | TIMESTAMP | NO | Auditoría |

**Índices**:
- `idx_post_polls_post_id` UNIQUE (post_id)
- `idx_post_polls_ends_at` (ends_at)

**Constraints**:
- `fk_post_polls_post` FK (post_id) → organization_posts(id) ON DELETE CASCADE
- `uq_post_polls_post_id` UNIQUE (post_id)

---

#### Tabla: `organization_post_poll_options`

**Propósito**: Opciones disponibles en una encuesta.

> **[PROPUESTA]**

| Columna | Tipo | Nullable | Descripción |
|---------|------|----------|-------------|
| id | INT | NO | PK AUTO_INCREMENT |
| poll_id | INT | NO | FK a organization_post_polls |
| label | VARCHAR(255) | NO | Texto de la opción |
| sort_order | INT | NO | DEFAULT 0 |
| created_at | TIMESTAMP | NO | Auditoría |
| updated_at | TIMESTAMP | NO | Auditoría |

**Índices**:
- `idx_poll_options_poll_id` (poll_id)
- `idx_poll_options_sort` (poll_id, sort_order)

**Constraints**:
- `fk_poll_options_poll` FK (poll_id) → organization_post_polls(id) ON DELETE CASCADE

---

#### Tabla: `organization_post_poll_votes`

**Propósito**: Registrar votos de usuarios en encuestas.

> **[PROPUESTA]**

| Columna | Tipo | Nullable | Descripción |
|---------|------|----------|-------------|
| id | INT | NO | PK AUTO_INCREMENT |
| poll_id | INT | NO | FK a organization_post_polls |
| option_id | INT | NO | FK a organization_post_poll_options |
| voter_user_id | INT | NO | FK a users |
| organization_id | INT | NO | FK a organization (multi-tenant filter) |
| voted_at | TIMESTAMP | NO | Fecha/hora del voto |
| created_at | TIMESTAMP | NO | Auditoría |

**Índices**:
- `idx_poll_votes_poll_id` (poll_id)
- `idx_poll_votes_option_id` (option_id)
- `idx_poll_votes_voter` (voter_user_id)
- `idx_poll_votes_organization_id` (organization_id)

**Constraints**:
- `fk_poll_votes_poll` FK (poll_id) → organization_post_polls(id) ON DELETE CASCADE
- `fk_poll_votes_option` FK (option_id) → organization_post_poll_options(id) ON DELETE CASCADE
- `fk_poll_votes_voter` FK (voter_user_id) → users(id)
- `fk_poll_votes_organization` FK (organization_id) → organization(id)

**Constraints de unicidad (condicionales)**:

```sql
-- Si allow_multiple_answers = FALSE (un voto total por usuario):
ALTER TABLE organization_post_poll_votes
ADD CONSTRAINT uq_poll_votes_single UNIQUE (poll_id, voter_user_id);

-- Si allow_multiple_answers = TRUE (un voto por opción por usuario):
ALTER TABLE organization_post_poll_votes
ADD CONSTRAINT uq_poll_votes_multi UNIQUE (poll_id, option_id, voter_user_id);
```

> **Nota de implementación**: Manejar en lógica de negocio según configuración de la encuesta.

---

#### Tabla: `notifications`

**Propósito**: Notificaciones generadas para usuarios (posts publicados, encuestas, etc.).

> **[PROPUESTA]** - No existe tabla de notificaciones en CCP analizado

| Columna | Tipo | Nullable | Descripción |
|---------|------|----------|-------------|
| id | INT | NO | PK AUTO_INCREMENT |
| organization_id | INT | NO | FK a organization |
| user_id | INT | NO | FK a users (destinatario) |
| type | ENUM('POST_PUBLISHED','POST_UPDATED','POLL_PUBLISHED','POLL_ENDED','COMMENT_ADDED','VISIT_APPROVED','ACCESS_USED') | NO | Tipo de notificación |
| title | VARCHAR(255) | NO | Título corto |
| message | TEXT | YES | Mensaje descriptivo |
| resource_type | VARCHAR(50) | YES | Tipo de recurso (POST, POLL, VISIT) |
| resource_id | INT | YES | ID del recurso relacionado |
| status | ENUM('UNREAD','READ','DISMISSED') | NO | DEFAULT 'UNREAD' |
| action_url | VARCHAR(500) | YES | URL de acción (deep link) |
| read_at | TIMESTAMP | YES | Fecha de lectura |
| created_at | TIMESTAMP | NO | Auditoría |

**Índices**:
- `idx_notifications_user_status` (user_id, status)
- `idx_notifications_organization_id` (organization_id)
- `idx_notifications_resource` (resource_type, resource_id)
- `idx_notifications_type` (type)
- `idx_notifications_created_at` (created_at DESC)

**Constraints**:
- `fk_notifications_organization` FK (organization_id) → organization(id) ON DELETE CASCADE
- `fk_notifications_user` FK (user_id) → users(id) ON DELETE CASCADE

**Nota de diseño**: Para evitar fan-out masivo (miles de filas por post), considerar alternativa con `notification_events` + `notification_recipients` en implementación futura.

---

### 10.3 Permisos del Módulo Posts/Notificaciones

> **Basado en patrón de**: CCP V9__seed_multi_tenant_data.sql (permissions con RESOURCE_ACTION)

```sql
-- Permisos del Módulo ATLAS - Posts y Notificaciones
INSERT INTO permissions (code, name, description, module_code, resource, action) VALUES
-- Posts (solo admins)
('ATLAS_POSTS_CREATE', 'Crear posts', 'Permite crear anuncios/encuestas', 'ATLAS', 'posts', 'CREATE'),
('ATLAS_POSTS_READ', 'Ver posts', 'Permite ver posts publicados', 'ATLAS', 'posts', 'READ'),
('ATLAS_POSTS_UPDATE', 'Editar posts', 'Permite editar posts propios', 'ATLAS', 'posts', 'UPDATE'),
('ATLAS_POSTS_DELETE', 'Eliminar posts', 'Permite eliminar/archivar posts', 'ATLAS', 'posts', 'DELETE'),
('ATLAS_POSTS_PUBLISH', 'Publicar posts', 'Permite cambiar de draft a published', 'ATLAS', 'posts', 'PUBLISH'),
('ATLAS_POSTS_MANAGE', 'Gestionar posts', 'Permisos completos sobre posts', 'ATLAS', 'posts', 'MANAGE'),

-- Comentarios
('ATLAS_COMMENTS_CREATE', 'Crear comentarios', 'Permite comentar en posts (si allow_comments)', 'ATLAS', 'comments', 'CREATE'),
('ATLAS_COMMENTS_READ', 'Ver comentarios', 'Permite ver comentarios', 'ATLAS', 'comments', 'READ'),
('ATLAS_COMMENTS_MODERATE', 'Moderar comentarios', 'Permite ocultar/eliminar comentarios', 'ATLAS', 'comments', 'MODERATE'),

-- Encuestas
('ATLAS_POLLS_VOTE', 'Votar en encuestas', 'Permite votar en encuestas activas', 'ATLAS', 'polls', 'VOTE'),
('ATLAS_POLLS_VIEW_RESULTS', 'Ver resultados', 'Permite ver resultados de encuestas', 'ATLAS', 'polls', 'VIEW_RESULTS'),

-- Notificaciones
('ATLAS_NOTIFICATIONS_READ', 'Ver notificaciones', 'Permite ver notificaciones propias', 'ATLAS', 'notifications', 'READ'),
('ATLAS_NOTIFICATIONS_MANAGE', 'Gestionar notificaciones', 'Permite marcar como leídas/descartadas', 'ATLAS', 'notifications', 'MANAGE');
```

---

### 10.4 Matriz de Roles y Permisos (Posts/Notificaciones)

| Permiso | SUPER_ADMIN | ADMIN | OWNER | TENANT | FAMILY | SECURITY | GUEST |
|---------|:-----------:|:-----:|:-----:|:------:|:------:|:--------:|:-----:|
| ATLAS_POSTS_CREATE | ✅ | ✅ | - | - | - | - | - |
| ATLAS_POSTS_READ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | - |
| ATLAS_POSTS_UPDATE | ✅ | ✅ | - | - | - | - | - |
| ATLAS_POSTS_DELETE | ✅ | ✅ | - | - | - | - | - |
| ATLAS_POSTS_PUBLISH | ✅ | ✅ | - | - | - | - | - |
| ATLAS_POSTS_MANAGE | ✅ | ✅ | - | - | - | - | - |
| ATLAS_COMMENTS_CREATE | ✅ | ✅ | ✅ | ✅ | ✅ | - | - |
| ATLAS_COMMENTS_READ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | - |
| ATLAS_COMMENTS_MODERATE | ✅ | ✅ | - | - | - | - | - |
| ATLAS_POLLS_VOTE | ✅ | ✅ | ✅ | ✅ | ✅ | - | - |
| ATLAS_POLLS_VIEW_RESULTS | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | - |
| ATLAS_NOTIFICATIONS_READ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | - |
| ATLAS_NOTIFICATIONS_MANAGE | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | - |

**Reglas adicionales**:
- `ATLAS_COMMENTS_CREATE` solo aplica si `organization_posts.allow_comments = true`
- `ATLAS_POLLS_VOTE` solo aplica si la encuesta está activa (dentro de starts_at/ends_at)

---

### 10.5 Flujos End-to-End

#### 10.5.1 Crear y Publicar Anuncio

```
1. POST /api/atlas/posts
   Headers: Authorization: Bearer {admin_token}
   Body: {
     "type": "ANNOUNCEMENT",
     "title": "Mantenimiento programado",
     "content": "El día 10/02/2026 se realizará mantenimiento en la piscina...",
     "allowComments": true,
     "notifyOnPublish": true,
     "targetAudience": "ALL_MEMBERS"
   }

2. Backend:
   a) Valida permisos: ATLAS_POSTS_CREATE
   b) Valida que usuario sea admin de la organización
   c) Inserta en organization_posts con status='DRAFT'
   d) Response: Post creado en borrador

3. POST /api/atlas/posts/{postId}/publish
   
4. Backend:
   a) Valida permisos: ATLAS_POSTS_PUBLISH
   b) Actualiza status='PUBLISHED', published_at=NOW()
   c) Si notify_on_publish=true:
      - Obtiene usuarios según target_audience
      - Inserta notificaciones para cada usuario
   d) Response: Post publicado
```

**Tablas involucradas**: `organization_posts`, `notifications`, `users`, `user_organizations`

---

#### 10.5.2 Crear Encuesta con Opciones

```
1. POST /api/atlas/posts
   Body: {
     "type": "POLL",
     "title": "¿Qué día prefieren para la asamblea?",
     "content": "Por favor voten por su día preferido",
     "allowComments": false,
     "notifyOnPublish": true,
     "poll": {
       "question": "Seleccione el día",
       "allowMultipleAnswers": false,
       "startsAt": null,
       "endsAt": "2026-02-20T23:59:59",
       "isAnonymous": false,
       "options": [
         {"label": "Sábado 22 de febrero", "sortOrder": 1},
         {"label": "Domingo 23 de febrero", "sortOrder": 2},
         {"label": "Sábado 1 de marzo", "sortOrder": 3}
       ]
     }
   }

2. Backend:
   a) Valida permisos: ATLAS_POSTS_CREATE
   b) Inserta en organization_posts con type='POLL', status='DRAFT'
   c) Inserta en organization_post_polls
   d) Inserta en organization_post_poll_options por cada opción
   e) Response: Encuesta creada

3. POST /api/atlas/posts/{postId}/publish

4. Backend:
   a) Publica post y genera notificaciones tipo POLL_PUBLISHED
```

**Tablas involucradas**: `organization_posts`, `organization_post_polls`, `organization_post_poll_options`, `notifications`

---

#### 10.5.3 Votar en Encuesta

```
1. POST /api/atlas/polls/{pollId}/vote
   Body: {
     "optionId": 42
   }

2. Backend:
   a) Valida permisos: ATLAS_POLLS_VOTE
   b) Valida que usuario esté vinculado a la organización
   c) Valida que encuesta esté activa:
      - Post status = 'PUBLISHED'
      - NOW() >= starts_at (o starts_at NULL)
      - NOW() <= ends_at (o ends_at NULL)
   d) Valida constraint de unicidad:
      - Si allow_multiple_answers=false: no existe voto previo del usuario
      - Si allow_multiple_answers=true: no existe voto previo en esa opción
   e) Inserta en organization_post_poll_votes
   f) Response: Voto registrado

3. GET /api/atlas/polls/{pollId}/results

4. Backend:
   a) Valida permisos: ATLAS_POLLS_VIEW_RESULTS
   b) Si show_results_before_end=false y NOW() < ends_at:
      - Retorna solo si usuario votó, no resultados completos
   c) Response: Resultados agregados por opción
```

**Tablas involucradas**: `organization_post_polls`, `organization_post_poll_options`, `organization_post_poll_votes`

---

#### 10.5.4 Comentar en Post

```
1. POST /api/atlas/posts/{postId}/comments
   Body: {
     "content": "Excelente iniciativa, gracias por informar.",
     "parentCommentId": null
   }

2. Backend:
   a) Valida permisos: ATLAS_COMMENTS_CREATE
   b) Valida que organization_posts.allow_comments = true
   c) Valida que usuario esté vinculado a la organización
   d) Inserta en organization_post_comments con status='VISIBLE'
   e) (Opcional) Notifica al autor del post
   f) Response: Comentario creado
```

**Tablas involucradas**: `organization_posts`, `organization_post_comments`

---

#### 10.5.5 Leer y Gestionar Notificaciones

```
1. GET /api/atlas/notifications?status=UNREAD

2. Backend:
   a) Valida permisos: ATLAS_NOTIFICATIONS_READ
   b) Filtra por user_id del token y organization_id actual
   c) Response: Lista de notificaciones no leídas

3. PATCH /api/atlas/notifications/{notificationId}
   Body: {
     "status": "READ"
   }

4. Backend:
   a) Valida permisos: ATLAS_NOTIFICATIONS_MANAGE
   b) Valida que notification.user_id = token.user_id
   c) Actualiza status='READ', read_at=NOW()
   d) Response: Notificación actualizada

5. POST /api/atlas/notifications/mark-all-read

6. Backend:
   a) Actualiza todas las notificaciones UNREAD del usuario a READ
```

**Tablas involucradas**: `notifications`

---

### 10.6 Eventos que Generan Notificación

| Evento | Tipo Notificación | Audiencia | Condición |
|--------|-------------------|-----------|-----------|
| Post pasa a PUBLISHED | `POST_PUBLISHED` | Según target_audience | notify_on_publish = true |
| Post editado después de publicar | `POST_UPDATED` | Misma audiencia | **[PROPUESTA]** opcional |
| Encuesta publicada | `POLL_PUBLISHED` | Según target_audience | notify_on_publish = true |
| Encuesta finalizada (ends_at) | `POLL_ENDED` | Votantes + autor | Job automático **[PROPUESTA]** |
| Nuevo comentario en post propio | `COMMENT_ADDED` | Autor del post | **[PROPUESTA]** opcional |
| Solicitud de visita aprobada | `VISIT_APPROVED` | Solicitante | Siempre |
| Código de acceso usado | `ACCESS_USED` | Creador de solicitud | **[PROPUESTA]** opcional |

---

### 10.7 Convención de Migraciones (Extensión)

```
V9__create_posts_schema.sql             # organization_posts, attachments
V10__create_comments_schema.sql         # organization_post_comments
V11__create_polls_schema.sql            # polls, options, votes
V12__create_notifications_schema.sql    # notifications
V13__seed_posts_permissions.sql         # Permisos ATLAS_POSTS_*, ATLAS_COMMENTS_*, etc.
V14__seed_posts_role_permissions.sql    # Asignación permisos a roles
```

---

### 10.8 Estados y Transiciones

#### Post (organization_posts.status)

```
DRAFT → PUBLISHED     (admin publica)
PUBLISHED → ARCHIVED  (admin archiva)
DRAFT → ARCHIVED      (admin descarta borrador)
```

#### Comentario (organization_post_comments.status)

```
VISIBLE → HIDDEN      (admin modera)
VISIBLE → DELETED     (autor elimina o admin)
HIDDEN → VISIBLE      (admin restaura)
```

#### Notificación (notifications.status)

```
UNREAD → READ         (usuario lee)
UNREAD → DISMISSED    (usuario descarta sin leer)
READ → DISMISSED      (usuario descarta)
```

---

### 10.9 Checklist de Aceptación (Posts/Notificaciones)

| # | Criterio | Estado |
|---|----------|--------|
| 1 | Tablas incluyen organization_id (multi-tenant) | ✅ |
| 2 | Posts soportan tipo ANNOUNCEMENT, POLL, ADVERTISEMENT | ✅ |
| 3 | Posts soportan allow_comments configurable | ✅ |
| 4 | Posts soportan notify_on_publish | ✅ |
| 5 | Posts soportan target_audience | ✅ |
| 6 | Encuestas soportan múltiples opciones | ✅ |
| 7 | Encuestas soportan allow_multiple_answers | ✅ |
| 8 | Encuestas soportan ventana temporal (starts_at/ends_at) | ✅ |
| 9 | Votos tienen constraints contra duplicados | ✅ |
| 10 | Comentarios soportan hilos (parent_comment_id) | ✅ |
| 11 | Comentarios soportan moderación (status HIDDEN) | ✅ |
| 12 | Notificaciones soportan UNREAD/READ/DISMISSED | ✅ |
| 13 | Notificaciones tienen link al recurso (resource_type, resource_id) | ✅ |
| 14 | Permisos alineados con patrón CCP (RESOURCE_ACTION) | ✅ |
| 15 | Matriz de roles-permisos definida | ✅ |
| 16 | Flujos end-to-end documentados | ✅ |
| 17 | Auditoría (created_at, updated_at, deleted_at) en todas las tablas | ✅ |
| 18 | Soft delete donde aplica | ✅ |

---

## 11. Checklist de Aceptación (General)

### 11.1 Validación de Alineación con Backends Referenciales

| # | Criterio | Estado | Evidencia |
|---|----------|--------|-----------|
| 1 | README referencia rutas reales de CCP | ✅ | Sección 2.1 |
| 2 | No hay tablas inventadas sin marca [PROPUESTA] | ✅ | Todas las nuevas tablas marcadas |
| 3 | Soporte ciudadela vs conjunto | ✅ | `organization.type` ENUM |
| 4 | Soporte zonas opcionales | ✅ | `organization.uses_zones`, `unit.zone_id` nullable |
| 5 | Soporte torres/apartamentos | ✅ | `tower` + `unit.tower_id` |
| 6 | Soporte casas | ✅ | `unit.type` = 'HOUSE', tower_id=NULL |
| 7 | Multi-unidad por usuario | ✅ | Tabla `user_units` N:M |
| 8 | Invitaciones por token | ✅ | Tabla `invitations` (replica V5) |
| 9 | Roles + permisos extra por unidad | ✅ | `user_units.role_id` + `user_unit_permissions` |
| 10 | Solicitud de ingreso | ✅ | Tabla `visit_requests` |
| 11 | Aprobación | ✅ | Tabla `visit_approvals` |
| 12 | QR/código | ✅ | Tabla `access_codes` |
| 13 | Escaneo/validación | ✅ | Tabla `access_scan_log` |
| 14 | Integración R2DBC documentada | ✅ | Sección 9 |
| 15 | No hay código ni scripts SQL implementados | ✅ | Solo documentación |

### 11.2 Patrones Replicados Correctamente

| Patrón CCP | Replicado en Atlas | Verificación |
|------------|-------------------|--------------|
| Auditoría (created_at, updated_at, deleted_at) | Todas las tablas | ✅ |
| Soft delete (deleted_at IS NULL) | Tablas principales | ✅ |
| Multi-tenant (organization_id) | Todas las tablas de negocio | ✅ |
| Permisos granulares (RESOURCE_ACTION) | permissions Atlas | ✅ |
| Invitaciones con token + expiración | invitations | ✅ |
| ENUM para tipos cerrados | organization.type, unit.type, etc. | ✅ |
| Flyway migrations en db/migration | Sección 8 | ✅ |
| Entity/Adapter/Mapper separation | Sección 9.2 | ✅ |
| Clean Architecture layers | Sección 9.3, 9.4 | ✅ |

### 11.3 Elementos NO Encontrados (Transparencia)

| Elemento | Acción Tomada |
|----------|---------------|
| Proyecto Odyssey | Documentado como NO ENCONTRADO |
| Proyecto SCP | Documentado como NO ENCONTRADO |
| QR generation pattern | Propuesta basada en mejores prácticas |
| Access scan log pattern | Propuesta basada en ticket_history de CCP |

---

## Anexo A: Matriz de Roles y Permisos

| Permiso | SUPER_ADMIN | ADMIN | OWNER | TENANT | FAMILY | SECURITY | GUEST |
|---------|:-----------:|:-----:|:-----:|:------:|:------:|:--------:|:-----:|
| ATLAS_ORGANIZATIONS_* | ✅ | - | - | - | - | - | - |
| ATLAS_ZONES_* | ✅ | ✅ | - | - | - | - | - |
| ATLAS_TOWERS_* | ✅ | ✅ | - | - | - | - | - |
| ATLAS_UNITS_* | ✅ | ✅ | R | R | R | R | - |
| ATLAS_INVITATIONS_CREATE | ✅ | ✅ | ✅ | - | - | - | - |
| ATLAS_VISITS_CREATE | ✅ | ✅ | ✅ | ✅ | ✅* | - | - |
| ATLAS_VISITS_READ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | - |
| ATLAS_VISITS_APPROVE | ✅ | ✅ | ✅ | - | - | - | - |
| ATLAS_ACCESS_SCAN | ✅ | - | - | - | - | ✅ | - |
| ATLAS_ACCESS_VIEW_LOG | ✅ | ✅ | - | - | - | ✅ | - |

*: Requiere permiso adicional en `user_unit_permissions`

---

## Anexo B: Estados y Transiciones

### B.1 Invitación (invitations.status)

```
PENDING → ACCEPTED    (usuario acepta)
PENDING → EXPIRED     (job automático)
PENDING → CANCELLED   (invitador cancela)
```

### B.2 Solicitud de Visita (visit_requests.status)

```
PENDING → APPROVED    (aprobador aprueba)
PENDING → REJECTED    (aprobador rechaza)
PENDING → EXPIRED     (job automático, valid_from pasó)
PENDING → CANCELLED   (solicitante cancela)
APPROVED → CANCELLED  (cualquiera con permiso cancela)
```

### B.3 Código de Acceso (access_codes.status)

```
ACTIVE → EXPIRED      (job automático o al validar)
ACTIVE → REVOKED      (administrador revoca)
ACTIVE → EXHAUSTED    (current_uses >= max_uses)
```

---

*Documento generado: 2026-02-07*
*Basado en análisis de: kodianteach-ssp-backend (CCP)*
*Proyectos NO encontrados: Odyssey, SCP*

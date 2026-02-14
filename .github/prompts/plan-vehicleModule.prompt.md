# Plan: Módulo Vehículos — Atlas Platform

## Diagnóstico de Arquitectura y Migraciones Actuales

### Hallazgos positivos

- Arquitectura Clean Architecture bien implementada (Bancolombia scaffold v4.0.5), separación estricta domain/usecase/infrastructure
- Convenciones consistentes: snake_case en DB, camelCase en Java, `@Table`/`@Column`, soft-delete con `deleted_at`, audit con `created_at`/`updated_at`
- RBAC granular maduro: modules → permissions → role_permissions → user_roles_multi
- 5 migraciones Flyway bien versionadas, integridad referencial con FK nombrados (`fk_prefix`)
- Índices en columnas de búsqueda frecuente

### Hallazgos / mejoras necesarias

- **No existe paginación** en Atlas — todos los `findBy*` retornan `Flux<T>` sin límite. Esto es un riesgo con datos crecientes y contradice el estándar OBLIGATORIO de CODING_STANDARDS.md
- **`parking_spots` vs `max_vehicles`**: el campo `parking_spots` en `unit` describe infraestructura física, no cupo de registro. Se necesita columna explícita `max_vehicles`
- **No hay índice en `vehicle_plate`** de `visit_requests` — búsqueda lenta para validaciones
- **Ausencia de módulo VEHICLE_CONTROL** en la tabla `modules`

---

## Modelo Entidad–Relación (MER) — Asociación Vehículos ↔ Viviendas

### Jerarquía residencial existente

```
Company
  └── Organization (CIUDADELA | CONJUNTO)
        ├── Zone (opcional, si usesZones = true)
        │     └── Tower (solo CIUDADELA)
        │           └── Unit (APARTMENT)  ← vivienda
        └── Unit (HOUSE)                  ← vivienda
              └── UserUnit (OWNER | TENANT | FAMILY | GUEST)
```

### Nueva relación: Vehicle → Unit (vivienda)

```
┌──────────────────────┐         ┌──────────────────────────────┐
│    organization       │         │           unit               │
│ ──────────────────── │         │ ──────────────────────────── │
│ id (PK)              │◄────┐   │ id (PK)                     │
│ name                 │     │   │ organization_id (FK)         │
│ type (CIUDADELA|     │     │   │ zone_id (FK, nullable)       │
│       CONJUNTO)      │     │   │ tower_id (FK, nullable)      │
│ ...                  │     │   │ code (ej: "101", "CASA-5")   │
└──────────────────────┘     │   │ type (APARTMENT | HOUSE)  ◄──── ESTO es la vivienda
                             │   │ parking_spots (físicos)      │
                             │   │ max_vehicles (NEW! cupo reg) │
                             │   │ ...                          │
                             │   └──────────┬───────────────────┘
                             │              │ 1
                             │              │
                             │              │ N
                             │   ┌──────────▼───────────────────┐
                             │   │        vehicles (NEW!)       │
                             │   │ ──────────────────────────── │
                             │   │ id (PK)                     │
                             └───┤ organization_id (FK) ◄─┐    │
                                 │ unit_id (FK) ───────────┘    │
                                 │ plate (VARCHAR 20)           │
                                 │ vehicle_type (CAR|MOTO|...)  │
                                 │ brand, model, color          │
                                 │ owner_name                   │
                                 │ is_active (BOOL)             │
                                 │ registered_by (FK → users)   │
                                 │ notes                        │
                                 │ created_at, updated_at       │
                                 │ deleted_at (soft delete)     │
                                 └──────────────────────────────┘
```

### Relación clave explicada

| Concepto | Tabla | Campo | Ejemplo |
|----------|-------|-------|---------|
| **La vivienda** (casa o apartamento) | `unit` | `id`, `type` | Unit id=5, code="101", type=APARTMENT |
| **El vehículo** pertenece a una vivienda | `vehicles` | `unit_id` (FK → `unit.id`) | Vehicle plate="ABC123", unit_id=5 |
| **Cupo máximo** de vehículos de la vivienda | `unit` | `max_vehicles` (NEW) | Unit id=5, max_vehicles=2 |
| **Regla**: no registrar más activos que el cupo | `VehicleUseCase` | `countActive < unit.maxVehicles` | Si ya hay 2 activos y max=2 → RECHAZADO |
| **org_id desnormalizado** en vehicles | `vehicles` | `organization_id` | Para query rápida del guarda sin JOIN a `unit` |

### Restricciones de integridad

- `UNIQUE (organization_id, plate)` → una placa no puede existir dos veces en la misma organización
- `FK unit_id → unit(id)` → todo vehículo DEBE pertenecer a una vivienda (casa o apartamento)
- `FK organization_id → organization(id)` → desnormalizado para performance en validación de guarda
- `INDEX ON plate` → búsqueda rápida por placa (O(log n))
- `max_vehicles >= count(vehicles WHERE is_active = true AND unit_id = X)` → regla de negocio en UseCase

---

## Paso 0: Prerequisito — Paginación genérica para Atlas

1. Crear `PageResponse<T>` en `co.com.atlas.model.common` con campos: `content`, `page`, `size`, `totalElements`, `totalPages` — modelado idéntico al `PageResponse` de SSP
2. Crear métodos `countBy*` en los gateways que lo necesiten (empezando por `VehicleRepository`)
3. Este `PageResponse` será reutilizable por cualquier módulo futuro de Atlas

---

## Paso 1: Migración V6 — Schema de Vehículos

Crear `V6__create_vehicles_schema.sql`:

### 1a. ALTER TABLE `unit` — agregar cupo de vehículos

```sql
ALTER TABLE unit ADD COLUMN max_vehicles INT NOT NULL DEFAULT 2 AFTER parking_spots;
```

Justificación: se independiza del concepto físico `parking_spots`. Un usuario puede tener 1 parqueadero pero 2 vehículos registrados (ej. rota carros). Valor default 2 es razonable para residencias.

### 1b. CREATE TABLE `vehicles`

```sql
CREATE TABLE IF NOT EXISTS vehicles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    unit_id INT NOT NULL COMMENT 'Vivienda (casa o apartamento) a la que pertenece el vehículo',
    organization_id INT NOT NULL COMMENT 'Desnormalizado para queries rápidas de validación',
    plate VARCHAR(20) NOT NULL COMMENT 'Placa del vehículo',
    vehicle_type ENUM('CAR','MOTORCYCLE','BICYCLE','OTHER') NOT NULL DEFAULT 'CAR',
    brand VARCHAR(80) NULL,
    model VARCHAR(80) NULL,
    color VARCHAR(40) NULL,
    owner_name VARCHAR(160) NULL COMMENT 'Nombre del responsable del vehículo',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Placa activa (permitida) o inactiva (bloqueada)',
    registered_by INT NULL COMMENT 'Usuario que registró el vehículo',
    notes TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_vehicles_unit FOREIGN KEY (unit_id) REFERENCES unit(id) ON DELETE CASCADE,
    CONSTRAINT fk_vehicles_organization FOREIGN KEY (organization_id) REFERENCES organization(id),
    CONSTRAINT fk_vehicles_registered_by FOREIGN KEY (registered_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### Índices

```sql
CREATE UNIQUE INDEX idx_vehicles_org_plate ON vehicles(organization_id, plate);
CREATE INDEX idx_vehicles_unit_id ON vehicles(unit_id);
CREATE INDEX idx_vehicles_organization_id ON vehicles(organization_id);
CREATE INDEX idx_vehicles_plate ON vehicles(plate);
CREATE INDEX idx_vehicles_is_active ON vehicles(is_active);
CREATE INDEX idx_vehicles_vehicle_type ON vehicles(vehicle_type);
```

Justificación del UNIQUE en `(organization_id, plate)`: una misma placa no puede estar registrada dos veces en la misma organización, pero sí podría existir en organizaciones distintas (escenario multi-tenant).

### 1c. INSERT módulo VEHICLE_CONTROL

```sql
INSERT INTO modules (code, name, description, is_active) VALUES
('VEHICLE_CONTROL', 'Control de Vehículos', 'Módulo de gestión y validación de vehículos por vivienda', TRUE);
```

### 1d. INSERT permisos granulares (resource `vehicles`)

```sql
INSERT INTO permissions (code, name, description, module_code, resource, action) VALUES
('VEHICLES_CREATE', 'Registrar Vehículos', 'Permite registrar vehículos a una vivienda', 'VEHICLE_CONTROL', 'vehicles', 'CREATE'),
('VEHICLES_READ', 'Ver Vehículos', 'Permite ver vehículos registrados', 'VEHICLE_CONTROL', 'vehicles', 'READ'),
('VEHICLES_UPDATE', 'Editar Vehículos', 'Permite modificar datos de vehículos', 'VEHICLE_CONTROL', 'vehicles', 'UPDATE'),
('VEHICLES_DELETE', 'Eliminar Vehículos', 'Permite eliminar vehículos', 'VEHICLE_CONTROL', 'vehicles', 'DELETE'),
('VEHICLES_MANAGE', 'Gestionar Vehículos', 'Control total de vehículos (bulk, sync)', 'VEHICLE_CONTROL', 'vehicles', 'MANAGE'),
('VEHICLES_VALIDATE', 'Validar Placas', 'Permite validar ingreso por placa (guardas)', 'VEHICLE_CONTROL', 'vehicles', 'VALIDATE');
```

### 1e. INSERT role_permissions

```sql
-- SUPER_ADMIN: todos los permisos nuevos (ya tiene cross-join implícito, pero por consistencia)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p 
WHERE r.code = 'SUPER_ADMIN' AND p.module_code = 'VEHICLE_CONTROL';

-- ADMIN_ATLAS: gestión completa de vehículos en su organización
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p 
WHERE r.code = 'ADMIN_ATLAS' 
AND p.code IN ('VEHICLES_CREATE', 'VEHICLES_READ', 'VEHICLES_UPDATE', 'VEHICLES_DELETE', 'VEHICLES_MANAGE', 'VEHICLES_VALIDATE');

-- OWNER: CRUD de vehículos de su propia vivienda
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p 
WHERE r.code = 'OWNER' 
AND p.code IN ('VEHICLES_CREATE', 'VEHICLES_READ', 'VEHICLES_UPDATE', 'VEHICLES_DELETE');

-- TENANT: CRUD de vehículos de su propia vivienda
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p 
WHERE r.code = 'TENANT' 
AND p.code IN ('VEHICLES_CREATE', 'VEHICLES_READ', 'VEHICLES_UPDATE', 'VEHICLES_DELETE');

-- FAMILY: solo lectura
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p 
WHERE r.code = 'FAMILY' AND p.code = 'VEHICLES_READ';

-- SECURITY: lectura + validación de placas (API de guarda)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p 
WHERE r.code = 'SECURITY' 
AND p.code IN ('VEHICLES_READ', 'VEHICLES_VALIDATE');
```

### 1f. Habilitar módulo para organización demo

```sql
INSERT INTO organization_modules (organization_id, module_id, is_enabled)
SELECT o.id, m.id, TRUE FROM organization o, modules m 
WHERE o.code = 'CIUDADELA-001' AND m.code = 'VEHICLE_CONTROL';
```

### 1g. Datos de prueba (vehículos asociados a unidades existentes)

```sql
-- Vehículo para la unidad 101 de Torre 1 (apartamento)
INSERT INTO vehicles (unit_id, organization_id, plate, vehicle_type, brand, color, owner_name, is_active, notes)
SELECT u.id, u.organization_id, 'ABC123', 'CAR', 'Renault', 'Blanco', 'Admin Atlas', TRUE, 'Vehículo de prueba'
FROM unit u WHERE u.code = '101';

-- Vehículo inactivo para la misma unidad
INSERT INTO vehicles (unit_id, organization_id, plate, vehicle_type, brand, color, owner_name, is_active, notes)
SELECT u.id, u.organization_id, 'XYZ789', 'MOTORCYCLE', 'Honda', 'Negro', 'Admin Atlas', FALSE, 'Moto inactiva de prueba'
FROM unit u WHERE u.code = '101';
```

---

## Paso 2: Domain Model — Capa de dominio

### 2a. Crear enum `VehicleType`

Paquete: `co.com.atlas.model.vehicle`

```java
public enum VehicleType {
    CAR,
    MOTORCYCLE,
    BICYCLE,
    OTHER
}
```

### 2b. Crear modelo `Vehicle`

Paquete: `co.com.atlas.model.vehicle`

Campos: `id`, `unitId`, `organizationId`, `plate`, `vehicleType` (`VehicleType`), `brand`, `model`, `color`, `ownerName`, `isActive`, `registeredBy`, `notes`, `createdAt`, `updatedAt`, `deletedAt`

Patrón Lombok: `@Getter @Setter @AllArgsConstructor @Builder(toBuilder = true)` — idéntico al patrón de `Unit`, `VisitRequest`, etc.

### 2c. Crear modelo `PlateValidationResult`

Para respuesta rápida al guarda. Campos:

- `plate` (String)
- `found` (boolean) — ¿La placa existe en el sistema?
- `active` (boolean) — ¿Está activa?
- `allowed` (boolean) — ¿Puede ingresar?
- `unitCode` (String) — Código de la vivienda (ej. "101", "CASA-5")
- `unitType` (String) — "APARTMENT" o "HOUSE"
- `organizationName` (String)
- `ownerName` (String)
- `vehicleType` (String)
- `reason` (String) — Razón si no está permitido

### 2d. Crear modelo `BulkInactivateResult`

Campos: `successful` (List<String>), `notFound` (List<String>), `alreadyInactive` (List<String>), `errors` (List<String>)

### 2e. Crear modelo `BulkSyncResult`

Campos: `created` (int), `kept` (int), `inactivated` (int), `errors` (List<String>)

### 2f. Crear gateway `VehicleRepository`

Paquete: `co.com.atlas.model.vehicle.gateways`

Métodos:

```java
Mono<Vehicle> findById(Long id);
Mono<Vehicle> findByOrganizationIdAndPlate(Long orgId, String plate);
Flux<Vehicle> findByUnitId(Long unitId);
Flux<Vehicle> findByOrganizationId(Long orgId);
Mono<Long> countByUnitIdAndIsActive(Long unitId, Boolean isActive);
Mono<Long> countByOrganizationId(Long orgId);
Flux<Vehicle> findByOrganizationIdPaginated(Long orgId, int page, int size);
Flux<Vehicle> findByUnitIdAndIsActive(Long unitId, Boolean isActive);
Mono<Vehicle> save(Vehicle vehicle);
Mono<Void> softDelete(Long id);
Mono<Boolean> existsByOrganizationIdAndPlate(Long orgId, String plate);
```

### 2g. Actualizar modelo `Unit`

Agregar campo `maxVehicles` (Integer) al modelo existente.

---

## Paso 3: Domain UseCase — Lógica de negocio

Crear `VehicleUseCase` en `co.com.atlas.usecase.vehicle`

### Métodos principales

| Método | Lógica |
|--------|--------|
| `create(Vehicle)` | Normalizar placa (uppercase, trim) → verificar unit existe → check placa no duplicada en org → contar activos por unit → validar `count < maxVehicles` → guardar con `isActive=true` |
| `findById(Long)` | Con `NotFoundException` si no existe |
| `findByUnitId(Long)` | Lista completa por vivienda (casa o apartamento) |
| `findByOrganizationIdPaginated(Long, int, int)` | Retorna `Mono<PageResponse<Vehicle>>` |
| `update(Long, Vehicle)` | Solo campos editables (brand, model, color, ownerName, notes, vehicleType) |
| `inactivate(Long)` | Setea `isActive=false`, **idempotente** (no falla si ya inactivo) |
| `activate(Long)` | Valida cupo antes de activar: `count < maxVehicles` |
| `validatePlate(Long orgId, String plate)` | Retorna `Mono<PlateValidationResult>` — consulta rápida para guarda |
| `bulkInactivate(Long orgId, List<String> plates)` | Recorre placas, inactiva, retorna `BulkInactivateResult` |
| `syncVehiclesByUnit(Long unitId, List<Vehicle>)` | Reemplaza lista: lo que viene se crea/mantiene, lo que no viene se inactiva. Valida `maxVehicles`. Retorna `BulkSyncResult` |

### Reglas de negocio

- **Validación de cupo**: `countActive < unit.maxVehicles` — se aplica en `create`, `activate` y `syncVehiclesByUnit`
- **Placa única por organización**: validada en `create` y `syncVehiclesByUnit`
- **Idempotencia**: inactivar placa repetida no falla
- **Normalización de placa**: uppercase + trim siempre

---

## Paso 4: Infrastructure — Driven Adapter (R2DBC)

### 4a. Crear `VehicleEntity`

Paquete: `co.com.atlas.r2dbc.vehicle` — `@Table("vehicles")` con `@Data @Builder @NoArgsConstructor @AllArgsConstructor`

### 4b. Crear `VehicleReactiveRepository`

Extends `ReactiveCrudRepository<VehicleEntity, Long>`:

```java
Mono<VehicleEntity> findByOrganizationIdAndPlateAndDeletedAtIsNull(Long orgId, String plate);
Flux<VehicleEntity> findByUnitIdAndDeletedAtIsNull(Long unitId);
Flux<VehicleEntity> findByUnitIdAndIsActiveAndDeletedAtIsNull(Long unitId, Boolean isActive);
Mono<Boolean> existsByOrganizationIdAndPlateAndDeletedAtIsNull(Long orgId, String plate);
Mono<Long> countByUnitIdAndIsActiveAndDeletedAtIsNull(Long unitId, Boolean isActive);
```

### 4c. Crear `VehicleRepositoryAdapter`

Implements `VehicleRepository`:

- Inline `toDomain()` / `toEntity()` mappers (patrón existente)
- Queries paginadas con `DatabaseClient` raw SQL (`LIMIT/OFFSET` + `COUNT(*)`)
- Soft delete con `UPDATE vehicles SET deleted_at = :now, is_active = false WHERE id = :id`

### 4d. Actualizar `UnitEntity` y `UnitRepositoryAdapter`

Mapear el nuevo campo `maxVehicles` ↔ `max_vehicles`.

---

## Paso 5: Infrastructure — Entry Point (Reactive Web)

Paquete: `co.com.atlas.api.vehicle`

### 5a. DTOs

| DTO | Campos |
|-----|--------|
| `VehicleRequest` | `unitId`, `plate`, `vehicleType`, `brand`, `model`, `color`, `ownerName`, `notes` |
| `VehicleResponse` | Todos los campos del modelo + `unitCode` |
| `PlateValidationResponse` | `plate`, `found`, `active`, `unitCode`, `unitType`, `orgName`, `allowed`, `reason`, `vehicleType` |
| `BulkInactivateRequest` | `plates` (List<String>) |
| `BulkInactivateResponse` | `successful`, `notFound`, `alreadyInactive`, `errors`, `summary` |
| `BulkSyncRequest` | `vehicles` (List<VehicleRequest>) |
| `BulkSyncResponse` | `created`, `kept`, `inactivated`, `errors`, `summary` |

### 5b. `VehicleHandler`

| Método | Ruta | Descripción |
|--------|------|-------------|
| `create` | POST `/api/vehicles` | Registrar vehículo a vivienda (casa/apto) |
| `getById` | GET `/api/vehicles/{id}` | Obtener por ID |
| `getByUnit` | GET `/api/vehicles/unit/{unitId}` | Listar vehículos de una vivienda |
| `getByOrganization` | GET `/api/vehicles/organization/{orgId}` | Listar global paginado (`?page=0&size=10`) |
| `update` | PUT `/api/vehicles/{id}` | Actualizar datos del vehículo |
| `inactivate` | PUT `/api/vehicles/{id}/inactivate` | Inactivar placa individual |
| `activate` | PUT `/api/vehicles/{id}/activate` | Reactivar placa |
| `validatePlate` | GET `/api/vehicles/validate/{plate}` | **API guarda** — respuesta rápida |
| `bulkInactivate` | POST `/api/vehicles/bulk-inactivate` | Inactivación masiva por JSON |
| `syncByUnit` | PUT `/api/vehicles/unit/{unitId}/sync` | Actualización masiva por vivienda |

### 5c. `VehicleRouterRest`

`@Configuration` con `@Bean RouterFunction<ServerResponse>` + `@RouterOperations` para Swagger. Seguir patrón idéntico a `UnitRouterRest.java`.

### 5d. Actualizar `SecurityConfig.java`

```java
.pathMatchers("/api/vehicles/validate/**").authenticated()
.pathMatchers("/api/vehicles/**").authenticated()
```

---

## Especificación de Endpoints

| # | Método | Ruta | Roles | Request | Response |
|---|--------|------|-------|---------|----------|
| 1 | POST | `/api/vehicles` | ADMIN_ATLAS, OWNER, TENANT | `VehicleRequest` | `ApiResponse<VehicleResponse>` |
| 2 | GET | `/api/vehicles/{id}` | Autenticado | — | `ApiResponse<VehicleResponse>` |
| 3 | GET | `/api/vehicles/unit/{unitId}` | Autenticado | — | `ApiResponse<List<VehicleResponse>>` |
| 4 | GET | `/api/vehicles/organization/{orgId}?page=0&size=10&plate=&type=&active=` | ADMIN_ATLAS | — | `ApiResponse<PageResponse<VehicleResponse>>` |
| 5 | PUT | `/api/vehicles/{id}` | ADMIN_ATLAS, OWNER, TENANT | `VehicleRequest` | `ApiResponse<VehicleResponse>` |
| 6 | PUT | `/api/vehicles/{id}/inactivate` | ADMIN_ATLAS, OWNER, TENANT | — | `ApiResponse<VehicleResponse>` |
| 7 | PUT | `/api/vehicles/{id}/activate` | ADMIN_ATLAS, OWNER, TENANT | — | `ApiResponse<VehicleResponse>` |
| 8 | **GET** | **`/api/vehicles/validate/{plate}`** | **SECURITY** | — | `ApiResponse<PlateValidationResponse>` |
| 9 | POST | `/api/vehicles/bulk-inactivate` | ADMIN_ATLAS | `BulkInactivateRequest` | `ApiResponse<BulkInactivateResponse>` |
| 10 | PUT | `/api/vehicles/unit/{unitId}/sync` | ADMIN_ATLAS, OWNER | `BulkSyncRequest` | `ApiResponse<BulkSyncResponse>` |

### Errores estándar

| Situación | Exception | HTTP | Código |
|-----------|-----------|------|--------|
| Vehículo no encontrado | `NotFoundException` | 404 | `NOT_FOUND` |
| Placa duplicada en org | `DuplicateException` | 409 | `DUPLICATE` |
| Cupo excedido (max_vehicles) | `BusinessException` | 400 | `VEHICLE_QUOTA_EXCEEDED` |
| Unit no encontrada | `NotFoundException` | 404 | `NOT_FOUND` |
| Placa inválida (formato) | `BusinessException` | 400 | `INVALID_PLATE` |

---

## Flujos Principales

### Flujo 1 — Registrar vehículo a una vivienda

```
POST /api/vehicles { unitId: 5, plate: "ABC123", vehicleType: "CAR", ... }
  → Handler parsea VehicleRequest
  → UseCase: normalize plate → uppercase("abc123") = "ABC123"
  → Verificar que unit_id=5 existe en tabla `unit` (casa o apartamento)
  → Verificar plate "ABC123" no existe en org (UNIQUE org+plate)
  → Contar vehículos activos de unit_id=5: SELECT COUNT(*) FROM vehicles WHERE unit_id=5 AND is_active=true
  → Comparar count vs unit.max_vehicles (ej. max=2, count=1 → OK)
  → INSERT INTO vehicles (unit_id, organization_id, plate, ...) VALUES (5, org_id, 'ABC123', ...)
  → Retornar VehicleResponse con datos completos
```

### Flujo 2 — Validar placa (guarda)

```
GET /api/vehicles/validate/ABC123
  → Handler extrae plate="ABC123" del path
  → UseCase: buscar en vehicles WHERE organization_id=:orgId AND plate='ABC123' AND deleted_at IS NULL
  → Si no existe → { found: false, active: false, allowed: false, reason: "Placa no registrada" }
  → Si existe pero is_active=false → { found: true, active: false, allowed: false, reason: "Placa inactiva/bloqueada" }
  → Si existe y is_active=true → { found: true, active: true, allowed: true, unitCode: "101", unitType: "APARTMENT", ownerName: "Juan Pérez" }
```

### Flujo 3 — Inactivación masiva

```
POST /api/vehicles/bulk-inactivate { plates: ["ABC123", "DEF456", "GHI789"] }
  → UseCase: por cada placa en la lista:
    → Buscar en org: vehicles WHERE org_id=:orgId AND plate=:plate
    → "ABC123" encontrada y activa → UPDATE is_active=false → lista "successful"
    → "DEF456" no encontrada → lista "notFound"
    → "GHI789" encontrada pero ya inactiva → lista "alreadyInactive"
  → Retornar BulkInactivateResponse:
    { successful: ["ABC123"], notFound: ["DEF456"], alreadyInactive: ["GHI789"], errors: [] }
```

### Flujo 4 — Sync vehículos por vivienda

```
PUT /api/vehicles/unit/5/sync { vehicles: [{ plate: "ABC123", ... }, { plate: "NEW001", ... }] }
  → UseCase: 
    → Obtener lista actual activa de unit_id=5: ["ABC123", "XYZ789"]
    → Incoming: ["ABC123", "NEW001"]
    → ABC123 está en ambos → mantener (actualizar datos si cambiaron) → count "kept"
    → NEW001 no existía → crear nuevo → count "created"
    → XYZ789 estaba activo pero NO vino en incoming → inactivar (is_active=false) → count "inactivated"
    → Validar que total activos resultantes ≤ max_vehicles ANTES de aplicar
  → Retornar: { created: 1, kept: 1, inactivated: 1, errors: [] }
```

---

## Decisiones de Diseño

| Decisión | Justificación |
|----------|---------------|
| `max_vehicles` sobre `parking_spots` | Concepto independiente: un usuario puede tener 1 parqueadero pero 2 vehículos registrados (rota carros) |
| UNIQUE `(organization_id, plate)` sobre UNIQUE `(plate)` | Multi-tenant: misma placa puede existir en organizaciones distintas |
| `organization_id` desnormalizado en `vehicles` | Evita JOIN con `unit` en la query de validación del guarda (performance) |
| `is_active` flag sobre status ENUM | El mundo vehicular solo necesita activo/inactivo, no estados intermedios complejos |
| PageResponse genérica | Inversión que paga dividendos para los otros módulos de Atlas que aún no paginan |
| GET sobre POST para validación del guarda | Semántica correcta (es una consulta), cacheable, más simple para frontend móvil |

---

## Lista de Tareas Técnicas (orden de implementación)

| # | Tarea | Capa | Dependencia |
|---|-------|------|-------------|
| 1 | Crear `PageResponse<T>` genérico | domain/model | — |
| 2 | Crear migración `V6__create_vehicles_schema.sql` | infra/r2dbc | — |
| 3 | Agregar `maxVehicles` a modelo `Unit` + entity + mapper | domain + infra | T2 |
| 4 | Crear enum `VehicleType` | domain/model | — |
| 5 | Crear modelos `Vehicle`, `PlateValidationResult`, `BulkInactivateResult`, `BulkSyncResult` | domain/model | T4 |
| 6 | Crear gateway `VehicleRepository` | domain/model | T5 |
| 7 | Crear `VehicleEntity` + `VehicleReactiveRepository` | infra/r2dbc | T2, T5 |
| 8 | Crear `VehicleRepositoryAdapter` | infra/r2dbc | T6, T7 |
| 9 | Crear `VehicleUseCase` | domain/usecase | T6, T1 |
| 10 | Crear DTOs (request/response) | infra/reactive-web | T5 |
| 11 | Crear `VehicleHandler` | infra/reactive-web | T9, T10 |
| 12 | Crear `VehicleRouterRest` | infra/reactive-web | T11 |
| 13 | Actualizar `SecurityConfig` | infra/reactive-web | T12 |
| 14 | Actualizar `UnitHandler` DTOs para `maxVehicles` | infra/reactive-web | T3 |
| 15 | Escribir tests unitarios `VehicleUseCaseTest` | domain/usecase | T9 |
| 16 | Escribir tests de integración | infra/r2dbc | T8 |

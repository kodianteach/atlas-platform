# Análisis del Módulo de Visitas / Invitaciones - Atlas Platform

**Fecha:** 2026-02-14  
**Versión:** 1.0  
**Estado:** Completo

---

## 1. INVENTARIO DE APIs ACTUALES

### 1.1 APIs de Invitaciones (`/api/invitations`)

| Método | Path | Rol(es) | Descripción |
|--------|------|---------|-------------|
| POST | `/api/invitations` | OWNER, ADMIN | Crear invitación a organización/unidad |
| GET | `/api/invitations/{id}` | Autenticado | Obtener invitación por ID |
| GET | `/api/invitations/organization/{organizationId}` | ADMIN, OWNER | Listar invitaciones de organización |
| GET | `/api/invitations/unit/{unitId}` | OWNER | Listar invitaciones de unidad |
| POST | `/api/invitations/accept` | Público | Aceptar invitación (requiere token) |
| POST | `/api/invitations/{id}/cancel` | OWNER, ADMIN | Cancelar invitación pendiente |
| POST | `/api/invitations/{id}/resend` | OWNER, ADMIN | Reenviar invitación |

#### DTO Request - `InvitationRequest`
```java
{
  "organizationId": Long,      // Requerido
  "unitId": Long,              // Opcional - null si es solo a org
  "email": String,             // Requerido
  "type": String,              // ENUM: ORG_MEMBER, UNIT_OWNER, UNIT_TENANT, UNIT_FAMILY, OWNER_INVITATION
  "roleId": Long               // Opcional - ID del rol a asignar
}
```

#### DTO Request - `AcceptInvitationRequest`
```java
{
  "token": String,            // Requerido - Token de invitación
  "names": String,            // Requerido - Nombre completo
  "phone": String,            // Opcional
  "documentType": String,     // Requerido - CC, CE, TI, PASSPORT, NIT
  "documentNumber": String,   // Requerido
  "password": String,         // Requerido
  "confirmPassword": String   // Requerido - Debe coincidir con password
}
```

#### DTO Response - `InvitationResponse`
```java
{
  "id": Long,
  "organizationId": Long,
  "unitId": Long,
  "email": String,
  "invitationToken": String,
  "type": String,
  "roleId": Long,
  "status": String,           // PENDING, ACCEPTED, EXPIRED, CANCELLED
  "invitedBy": Long,
  "expiresAt": Instant,
  "acceptedAt": Instant,
  "createdAt": Instant
}
```

### 1.2 APIs de Visitas (`/api/visits`)

| Método | Path | Rol(es) | Descripción |
|--------|------|---------|-------------|
| POST | `/api/visits` | OWNER, TENANT, FAMILY | Crear solicitud de visita |
| GET | `/api/visits/{id}` | Autenticado | Obtener visita por ID |
| GET | `/api/visits/organization/{organizationId}` | ADMIN | Listar visitas de organización |
| GET | `/api/visits/unit/{unitId}` | OWNER, TENANT | Listar visitas de unidad |
| POST | `/api/visits/{id}/approve` | OWNER (primario) | Aprobar solicitud |
| POST | `/api/visits/{id}/reject` | OWNER (primario) | Rechazar solicitud |
| GET | `/api/visits/me` | Autenticado | Listar visitas del usuario actual |

#### DTO Request - `VisitRequestDto`
```java
{
  "organizationId": Long,
  "unitId": Long,              // Requerido
  "visitorName": String,       // Requerido
  "visitorDocument": String,
  "visitorPhone": String,
  "visitorEmail": String,
  "reason": String,
  "validFrom": Instant,        // Requerido
  "validUntil": Instant,       // Requerido
  "recurrenceType": String,    // ENUM: ONCE, DAILY, WEEKLY, MONTHLY
  "maxEntries": Integer
}
```

### 1.3 APIs de Unidades Relacionadas (`/api/units`)

| Método | Path | Rol(es) | Descripción |
|--------|------|---------|-------------|
| GET | `/api/units/organization/{organizationId}` | ADMIN, OWNER | Listar unidades de organización |
| GET | `/api/units/tower/{towerId}` | ADMIN | Listar unidades de torre |
| GET | `/api/units/{id}` | Autenticado | Obtener unidad por ID |

---

## 2. FLUJO ACTUAL END-TO-END

### 2.1 Diagrama de Flujo: Propietario Invita → Usuario Acepta

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         FLUJO DE INVITACIÓN COMPLETO                            │
└─────────────────────────────────────────────────────────────────────────────────┘

    ┌──────────────┐         ┌──────────────┐         ┌──────────────┐
    │  PROPIETARIO │         │   SISTEMA    │         │   INVITADO   │
    └──────────────┘         └──────────────┘         └──────────────┘
           │                        │                        │
           │  1. POST /api/invitations                       │
           │  {email, unitId, type}  │                       │
           │ ───────────────────────>│                       │
           │                        │                        │
           │                        │ 2. Validar:            │
           │                        │    - Org existe        │
           │                        │    - Unit existe       │
           │                        │    - No duplicados     │
           │                        │                        │
           │                        │ 3. ensureUserExists()  │
           │                        │    Crea user PRE_REGISTERED
           │                        │                        │
           │                        │ 4. Guardar invitación  │
           │                        │    status=PENDING      │
           │                        │    token=UUID          │
           │                        │                        │
           │                        │ 5. Enviar email ───────────────────>│
           │                        │    con link de aceptación           │
           │                        │                        │             │
           │<───────────────────────│                        │             │
           │  Response: invitación  │                        │             │
           │                        │                        │             │
           │                        │                        │             │
           │                        │         6. Click en link (frontend) │
           │                        │                        │<────────────│
           │                        │                        │
           │                        │  7. POST /api/invitations/accept
           │                        │     {token, names, documentType,
           │                        │      documentNumber, password}
           │                        │<───────────────────────│
           │                        │                        │
           │                        │ 8. Validar token:      │
           │                        │    - Existe            │
           │                        │    - PENDING           │
           │                        │    - No expirado       │
           │                        │                        │
           │                        │ 9. createOrUpdateOwner()
           │                        │    - Actualiza user    │
           │                        │    - status=ACTIVE     │
           │                        │    - Hash password     │
           │                        │                        │
           │                        │ 10. createMembership() │
           │                        │    ┌─────────────────────────────────┐
           │                        │    │ a) user_organizations          │
           │                        │    │    (userId, orgId, ACTIVE)     │
           │                        │    │                                │
           │                        │    │ b) user_units (si unitId != null)
           │                        │    │    (userId, unitId, roleId,    │
           │                        │    │     ownershipType, isPrimary)  │
           │                        │    └─────────────────────────────────┘
           │                        │                        │
           │                        │ 11. assignOwnerRole()  │
           │                        │     user_roles_multi   │
           │                        │                        │
           │                        │ 12. invitation.status  │
           │                        │     = ACCEPTED         │
           │                        │                        │
           │                        │ ───────────────────────>│
           │                        │    Response: success   │
           │                        │                        │
```

### 2.2 Mapeo InvitationType → OwnershipType

| InvitationType | OwnershipType | isPrimary |
|----------------|---------------|-----------|
| `UNIT_OWNER` | `OWNER` | `true` |
| `UNIT_TENANT` | `TENANT` | `false` |
| `UNIT_FAMILY` | `FAMILY` | `false` |
| `ORG_MEMBER` | `GUEST` | `false` |
| `OWNER_INVITATION` | `GUEST` | `false` |

### 2.3 Tablas Involucradas

| Tabla | Descripción |
|-------|-------------|
| `users` | Usuarios del sistema (AuthUser) |
| `organization` | Organizaciones/Conjuntos |
| `unit` | Unidades habitacionales |
| `invitations` | Invitaciones pendientes/aceptadas |
| `user_organizations` | Vinculación usuario ↔ organización |
| `user_units` | Vinculación usuario ↔ unidad (con rol y tipo) |
| `user_roles_multi` | Roles del usuario por organización |
| `role` | Catálogo de roles |
| `visit_requests` | Solicitudes de visita |

---

## 3. VALIDACIÓN DE PERMISOS PARA CREAR VISITAS

### 3.1 Flujo Actual en `VisitRequestUseCase.create()`

```java
public Mono<VisitRequest> create(VisitRequest request, Long requestedByUserId) {
    return unitRepository.findById(request.getUnitId())
            .switchIfEmpty(Mono.error(new NotFoundException("Unit", request.getUnitId())))
            .flatMap(unit -> {
                // ⚠️ VALIDACIÓN CRÍTICA: Usuario debe tener relación con la unidad
                return userUnitRepository.existsByUserIdAndUnitId(requestedByUserId, request.getUnitId())
                        .flatMap(exists -> {
                            if (Boolean.FALSE.equals(exists)) {
                                return Mono.error(new BusinessException(
                                        "No tienes permisos para crear visitas en esta unidad",
                                        "NO_PERMISSION", 403));
                            }
                            // ... continúa creación
                        });
            });
}
```

### 3.2 Requisito para Crear Visitas

Un usuario puede crear visitas **SOLO SI**:
1. Existe un registro en `user_units` con:
   - `user_id = userId del JWT`
   - `unit_id = unitId de la solicitud`
   - `status = 'ACTIVE'` (implícito en la query actual)

**Nota:** Actualmente NO se valida el `ownershipType` ni `roleId`, solo la existencia del vínculo.

---

## 4. GAP ANALYSIS

### 4.1 ✅ Funcionalidades IMPLEMENTADAS Correctamente

| Funcionalidad | Estado | Ubicación |
|---------------|--------|-----------|
| Crear invitación con validaciones | ✅ Completo | `InvitationUseCase.create()` |
| Validar org/unit existe | ✅ Completo | `InvitationUseCase.create()` |
| Evitar invitaciones duplicadas | ✅ Completo | `existsPendingByEmailAndOrganizationId()` |
| Crear usuario PRE_REGISTERED | ✅ Completo | `ensureUserExists()` |
| Enviar email de invitación | ✅ Completo | `sendInvitationEmail()` |
| Aceptar invitación con datos | ✅ Completo | `acceptWithOwnerData()` |
| Validar token/estado/expiración | ✅ Completo | `acceptWithOwnerData()` |
| Crear/actualizar usuario | ✅ Completo | `createOrUpdateOwner()` |
| Crear `user_organizations` | ✅ Completo | `createMembership()` |
| Crear `user_units` con tipo | ✅ Completo | `createMembership()` |
| Asignar rol OWNER | ✅ Completo | `assignOwnerRole()` |
| Marcar invitación aceptada | ✅ Completo | `acceptWithOwnerData()` |
| Validar permisos para visitas | ✅ Completo | `VisitRequestUseCase.create()` |
| Cancelar invitación | ✅ Completo | `InvitationUseCase.cancel()` |
| Reenviar invitación | ✅ Completo | `InvitationUseCase.resend()` |

### 4.2 ⚠️ Funcionalidades PARCIALES o con Observaciones

| Funcionalidad | Estado | Observación |
|---------------|--------|-------------|
| Extracción de userId | ⚠️ Parcial | Usa `X-User-Id` header en lugar de `TenantContext` (funciona pero inconsistente) |
| Validación de tipo de ownership | ⚠️ No validado | Cualquier tipo de vínculo puede crear visitas (OWNER, TENANT, FAMILY, GUEST) |
| Rol VISITANTE específico | ⚠️ No existe | Se usa `GUEST` como ownership pero no hay rol específico "VISITANTE" |
| API de validación standalone | ❌ No existe | No hay endpoint para validar `userId + unitId` sin crear visita |

### 4.3 ❌ Funcionalidades NO Implementadas

| Funcionalidad | Impacto | Prioridad |
|---------------|---------|-----------|
| `GET /api/users/{userId}/units` | Frontend necesita listar unidades del usuario | Alta |
| `GET /api/units/{unitId}/access/validate` | Guardia necesita validar acceso rápidamente | Alta |
| Rol VISITANTE en catálogo | Semántica correcta vs GUEST/OWNER | Media |
| Notificación al propietario | No se notifica cuando alguien solicita visita | Media |

---

## 5. CAUSA DEL ERROR ACTUAL

### 5.1 Error Reportado
```json
{
  "message": "No tienes permisos para crear visitas en esta unidad",
  "status": 400,
  "errorCode": "NO_PERMISSION"
}
```

### 5.2 Análisis de la Causa

**Contexto del Request:**
- JWT Subject: `"sub": "4"` → userId = 4
- JWT organizationId: 2
- Request unitId: 1

**El error ocurre porque:**
1. El usuario con `id=4` NO tiene registro en `user_units` para `unit_id=1`
2. Esto significa que el usuario 4 NO fue invitado a la unidad 1, o:
   - Fue invitado pero no aceptó la invitación
   - Fue invitado a una unidad diferente

### 5.3 Verificación Recomendada

Ejecutar en MySQL:
```sql
-- Verificar si existe vínculo usuario-unidad
SELECT * FROM user_units WHERE user_id = 4;

-- Verificar invitaciones del usuario
SELECT * FROM invitations WHERE email = (SELECT email FROM users WHERE id = 4);

-- Verificar a qué unidades tiene acceso el usuario 4
SELECT u.id, u.name, uu.ownership_type, uu.status
FROM user_units uu
JOIN unit u ON uu.unit_id = u.id
WHERE uu.user_id = 4 AND uu.status = 'ACTIVE';
```

---

## 6. RECOMENDACIONES DE IMPLEMENTACIÓN

### 6.1 API Nueva: Validación de Acceso Usuario-Unidad

**Necesidad:** El frontend/guardia necesita validar rápidamente si un usuario puede crear visitas en una unidad, sin hacer el POST completo.

**Endpoint propuesto:**

```yaml
GET /api/units/{unitId}/access/validate?userId={userId}&operation={CREAR_VISITA}

Response 200:
{
  "allowed": true,
  "effectiveRole": "OWNER",
  "ownershipType": "OWNER",
  "unitBindingStatus": "ACTIVE",
  "permissions": ["VISITS_CREATE", "VISITS_READ", "VISITS_APPROVE"]
}

Response 200 (no permitido):
{
  "allowed": false,
  "reasonCode": "NO_UNIT_BINDING",
  "message": "Usuario no tiene vínculo con esta unidad"
}
```

### 6.2 API Nueva: Listar Unidades del Usuario

**Necesidad:** El frontend necesita mostrar al usuario sus unidades disponibles para crear visitas.

**Endpoint propuesto:**

```yaml
GET /api/users/me/units

Response 200:
{
  "success": true,
  "data": [
    {
      "unitId": 1,
      "unitName": "Apto 101",
      "organizationId": 2,
      "organizationName": "Conjunto Residencial X",
      "ownershipType": "OWNER",
      "isPrimary": true,
      "canCreateVisits": true
    }
  ]
}
```

### 6.3 Mejora: Usar TenantContext en lugar de X-User-Id Header

**Cambio recomendado en handlers:**

```java
// ANTES (actual)
private Long extractUserIdFromRequest(ServerRequest request) {
    return request.headers().firstHeader("X-User-Id") != null 
            ? Long.parseLong(request.headers().firstHeader("X-User-Id")) 
            : 1L;  // ⚠️ Fallback peligroso
}

// DESPUÉS (recomendado)
private Long extractUserIdFromRequest(ServerRequest request) {
    Long userId = TenantContext.getUserId();
    if (userId == null) {
        throw new BusinessException("Usuario no autenticado", "UNAUTHORIZED", 401);
    }
    return userId;
}
```

---

## 7. CLASES POR CAPA (ARQUITECTURA ACTUAL)

### 7.1 Domain Layer

```
domain/
├── model/
│   ├── invitation/
│   │   ├── Invitation.java
│   │   ├── InvitationType.java (ENUM)
│   │   ├── InvitationStatus.java (ENUM)
│   │   └── gateways/
│   │       └── InvitationRepository.java
│   ├── visit/
│   │   ├── VisitRequest.java
│   │   ├── VisitApproval.java
│   │   ├── VisitStatus.java (ENUM)
│   │   ├── RecurrenceType.java (ENUM)
│   │   └── gateways/
│   │       ├── VisitRequestRepository.java
│   │       └── VisitApprovalRepository.java
│   ├── userunit/
│   │   ├── UserUnit.java
│   │   ├── OwnershipType.java (ENUM)
│   │   └── gateways/
│   │       └── UserUnitRepository.java
│   └── auth/
│       ├── AuthUser.java
│       ├── UserStatus.java (ENUM)
│       ├── DocumentType.java (ENUM)
│       └── gateways/
│           └── AuthUserRepository.java
├── usecase/
│   ├── invitation/
│   │   ├── InvitationUseCase.java
│   │   └── OwnerRegistrationData.java
│   └── visit/
│       └── VisitRequestUseCase.java
```

### 7.2 Infrastructure Layer

```
infrastructure/
├── entry-points/reactive-web/
│   └── src/main/java/co/com/atlas/api/
│       ├── invitation/
│       │   ├── InvitationRouterRest.java
│       │   ├── InvitationHandler.java
│       │   └── dto/
│       │       ├── InvitationRequest.java
│       │       ├── AcceptInvitationRequest.java
│       │       └── InvitationResponse.java
│       ├── visit/
│       │   ├── VisitRouterRest.java
│       │   ├── VisitHandler.java
│       │   └── dto/
│       │       ├── VisitRequestDto.java
│       │       └── VisitRequestResponse.java
│       └── config/
│           ├── TenantFilter.java
│           └── JwtAuthenticationFilter.java
├── driven-adapters/r2dbc-postgresql/
│   └── src/main/java/co/com/atlas/r2dbc/
│       ├── invitation/
│       │   ├── InvitationEntity.java
│       │   ├── InvitationReactiveRepository.java
│       │   └── InvitationRepositoryAdapter.java
│       ├── visit/
│       │   ├── VisitRequestEntity.java
│       │   ├── VisitRequestReactiveRepository.java
│       │   └── VisitRequestRepositoryAdapter.java
│       └── userunit/
│           ├── UserUnitEntity.java
│           ├── UserUnitReactiveRepository.java
│           └── UserUnitRepositoryAdapter.java
```

---

## 8. CONCLUSIÓN

### 8.1 Estado del Flujo Principal

| Aspecto | Estado |
|---------|--------|
| Flujo Propietario → Invitación → Aceptación | ✅ **IMPLEMENTADO** |
| Creación de usuario al aceptar | ✅ **IMPLEMENTADO** |
| Vinculación Usuario ↔ Unidad | ✅ **IMPLEMENTADO** |
| Asignación de rol | ✅ **IMPLEMENTADO** |
| Validación para crear visitas | ✅ **IMPLEMENTADO** |

### 8.2 El Error Específico

El error `"No tienes permisos para crear visitas en esta unidad"` es **un comportamiento esperado** cuando:
- El usuario ID=4 intenta crear una visita para unit_id=1
- Pero NO existe registro en `user_units` para esa combinación

**Solución:** El usuario debe ser invitado a la unidad 1 y aceptar la invitación para poder crear visitas en esa unidad.

### 8.3 Mejoras Recomendadas (Opcionales)

1. **API de validación standalone** - Para que el frontend pueda verificar permisos antes de mostrar el formulario
2. **Listar unidades del usuario** - Para que el usuario sepa en qué unidades puede crear visitas
3. **Usar TenantContext** - En lugar de X-User-Id header (consistencia)
4. **Validar ownership type** - Si se requiere que solo OWNER/TENANT puedan crear visitas (no GUEST)

---

## 9. APÉNDICE: Scripts de Verificación

### 9.1 Verificar Estado de Usuario en Unidad

```sql
-- Ver todas las unidades de un usuario
SELECT 
    u.id AS user_id,
    u.email,
    u.names,
    uu.unit_id,
    un.name AS unit_name,
    uu.ownership_type,
    uu.is_primary,
    uu.status,
    uu.joined_at
FROM users u
JOIN user_units uu ON u.id = uu.user_id
JOIN unit un ON uu.unit_id = un.id
WHERE u.id = 4;

-- Ver invitaciones pendientes  
SELECT 
    i.*,
    u.names AS invited_by_name
FROM invitations i
LEFT JOIN users u ON i.invited_by_user_id = u.id
WHERE i.email = 'developerpractice2@gmail.com'
ORDER BY i.created_at DESC;
```

### 9.2 Crear Vínculo Manualmente (Solo para pruebas)

```sql
-- SOLO PARA AMBIENTE DE DESARROLLO
-- Vincular usuario 4 a unidad 1 como OWNER
INSERT INTO user_units (
    user_id, unit_id, role_id, ownership_type, 
    is_primary, status, joined_at
) VALUES (
    4, 1, 3, 'OWNER', 
    TRUE, 'ACTIVE', NOW()
);
```

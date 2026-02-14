# Gestión de Registro, Unidades, Vehículos e Invitaciones

## Resumen

Este documento describe los flujos implementados para la gestión de unidades, vehículos e invitaciones en ATLAS PLATFORM.

---

## APIs Implementadas

### Resumen de Endpoints

| Método | Endpoint | Descripción | Autenticación |
|--------|----------|-------------|---------------|
| `POST` | `/api/units/distribute` | Distribuir unidades por rango | ✅ Requerida |
| `POST` | `/api/units/bulk-upload/validate` | Validar archivo Excel/CSV | ✅ Requerida |
| `POST` | `/api/units/bulk-upload/process` | Procesar carga masiva | ✅ Requerida |
| `GET` | `/api/activation/owner/validate/{token}` | Validar token de invitación | ❌ Pública |
| `POST` | `/api/activation/owner/complete` | Completar activación de propietario | ❌ Pública |

---

### API 1: Distribuir Unidades por Rango

**Endpoint:** `POST /api/units/distribute`

**Headers:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**Request Body:**
```json
{
  "organizationId": 1,
  "rangeStart": 101,
  "rangeEnd": 110,
  "codePrefix": "APTO-",
  "unitType": "APARTMENT",
  "vehiclesEnabled": true,
  "vehicleLimit": 2,
  "towerId": 1,
  "zoneId": null,
  "floor": "1",
  "sendInvitationImmediately": true,
  "owner": {
    "email": "propietario@email.com",
    "names": "Juan Pérez",
    "phone": "+573001234567",
    "documentType": "CC",
    "documentNumber": "12345678"
  }
}
```

**Campos:**
| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| organizationId | Long | ✅ | ID de la organización |
| rangeStart | Integer | ✅ | Número inicial del rango |
| rangeEnd | Integer | ✅ | Número final del rango |
| codePrefix | String | ❌ | Prefijo para códigos (ej: "APTO-") |
| unitType | String | ✅ | APARTMENT, HOUSE, LOCAL, OFFICE, WAREHOUSE, PARKING, OTHER |
| vehiclesEnabled | Boolean | ❌ | Habilitar vehículos (default: false) |
| vehicleLimit | Integer | ❌ | Límite de vehículos (default: 0) |
| towerId | Long | ❌ | ID de la torre |
| zoneId | Long | ❌ | ID de la zona |
| floor | String | ❌ | Piso |
| sendInvitationImmediately | Boolean | ❌ | Enviar invitación al crear (default: false) |
| owner | Object | ❌ | Información del propietario |

**Owner Object:**
| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| email | String | ✅ | Email del propietario |
| names | String | ❌ | Nombres completos |
| phone | String | ❌ | Teléfono con código país |
| documentType | String | ✅ | CC, NIT, CE, TI, PA, PEP |
| documentNumber | String | ✅ | Número de documento |

**Response 200 OK:**
```json
{
  "success": true,
  "message": "Distribución completada",
  "data": {
    "unitsCreated": 10,
    "unitIds": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10],
    "unitCodes": ["APTO-101", "APTO-102", "APTO-103", "APTO-104", "APTO-105", "APTO-106", "APTO-107", "APTO-108", "APTO-109", "APTO-110"],
    "invitationsSent": 1,
    "message": "10 unidades creadas exitosamente",
    "errors": []
  }
}
```

**Response 400 Bad Request:**
```json
{
  "success": false,
  "message": "Error de validación",
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "El rango excede el máximo permitido de 500 unidades"
  }
}
```

---

### API 2: Validar Carga Masiva (Excel/CSV)

**Endpoint:** `POST /api/units/bulk-upload/validate?organizationId={id}`

**Headers:**
```
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

**Query Parameters:**
| Parámetro | Tipo | Requerido | Descripción |
|-----------|------|-----------|-------------|
| organizationId | Long | ✅ | ID de la organización |

**Form Data:**
| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| file | File | ✅ | Archivo .xlsx, .xls o .csv |

**Formato del Archivo Excel/CSV:**
| Columna | Requerido | Descripción | Ejemplo |
|---------|-----------|-------------|---------|
| code | ✅ | Código único de la unidad | APTO-101 |
| unitType | ✅ | Tipo de unidad | APARTMENT |
| vehiclesEnabled | ❌ | Habilitar vehículos | TRUE |
| vehicleLimit | ❌ | Límite de vehículos | 2 |
| floor | ❌ | Piso | 1 |
| ownerEmail | ❌ | Email del propietario | juan@email.com |
| ownerNames | ❌ | Nombres del propietario | Juan Pérez |
| ownerPhone | ❌ | Teléfono del propietario | +573001234567 |
| ownerDocumentType | ❌ | Tipo de documento | CC |
| ownerDocumentNumber | ❌ | Número de documento | 12345678 |

**Response 200 OK (válido):**
```json
{
  "success": true,
  "message": "Validación completada",
  "data": {
    "valid": true,
    "totalRows": 50,
    "validRows": 50,
    "invalidRows": 0,
    "errors": [],
    "duplicateCodes": [],
    "existingCodes": []
  }
}
```

**Response 200 OK (con errores):**
```json
{
  "success": true,
  "message": "Validación completada con errores",
  "data": {
    "valid": false,
    "totalRows": 50,
    "validRows": 47,
    "invalidRows": 3,
    "errors": [
      {
        "row": 5,
        "field": "ownerDocumentNumber",
        "message": "El formato del documento no es válido para tipo CC"
      },
      {
        "row": 12,
        "field": "code",
        "message": "El código es requerido"
      },
      {
        "row": 23,
        "field": "unitType",
        "message": "Tipo de unidad no válido: INVALID"
      }
    ],
    "duplicateCodes": ["APTO-101"],
    "existingCodes": ["APTO-102", "APTO-103"]
  }
}
```

---

### API 3: Procesar Carga Masiva

**Endpoint:** `POST /api/units/bulk-upload/process`

**Headers:**
```
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

**Query Parameters:**
| Parámetro | Tipo | Requerido | Descripción |
|-----------|------|-----------|-------------|
| organizationId | Long | ✅ | ID de la organización |
| createdById | Long | ✅ | ID del usuario que crea |
| sendInvitations | Boolean | ❌ | Enviar invitaciones (default: false) |

**Form Data:**
| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| file | File | ✅ | Archivo .xlsx, .xls o .csv (previamente validado) |

**Response 200 OK:**
```json
{
  "success": true,
  "message": "Procesamiento completado",
  "data": {
    "success": true,
    "unitsCreated": 50,
    "usersCreated": 30,
    "invitationsSent": 30,
    "errors": [],
    "message": "50 unidades creadas, 30 invitaciones enviadas"
  }
}
```

**Response 200 OK (parcial):**
```json
{
  "success": true,
  "message": "Procesamiento completado con errores",
  "data": {
    "success": true,
    "unitsCreated": 45,
    "usersCreated": 25,
    "invitationsSent": 25,
    "errors": [
      {
        "row": 3,
        "message": "Error al crear unidad: código duplicado"
      }
    ],
    "message": "45 unidades creadas, 5 errores"
  }
}
```

---

### API 4: Validar Token de Propietario (Pública)

**Endpoint:** `GET /api/activation/owner/validate/{token}`

**⚠️ Este endpoint es PÚBLICO (no requiere autenticación)**

**Path Parameters:**
| Parámetro | Tipo | Requerido | Descripción |
|-----------|------|-----------|-------------|
| token | String (UUID) | ✅ | Token de invitación recibido por email |

**Response 200 OK (válido):**
```json
{
  "success": true,
  "message": "Token válido",
  "data": {
    "valid": true,
    "email": "propietario@email.com",
    "names": "Juan Pérez",
    "organizationName": "Torres del Parque",
    "unitCode": "APTO-101",
    "invitationId": "uuid-de-la-invitacion",
    "userExists": true,
    "message": null,
    "errorCode": null
  }
}
```

**Response 200 OK (expirado):**
```json
{
  "success": true,
  "message": "Token expirado",
  "data": {
    "valid": false,
    "email": null,
    "names": null,
    "organizationName": null,
    "unitCode": null,
    "invitationId": null,
    "userExists": false,
    "message": "El token ha expirado",
    "errorCode": "TOKEN_EXPIRED"
  }
}
```

**Response 404 Not Found:**
```json
{
  "success": false,
  "message": "Token no encontrado",
  "error": {
    "code": "NOT_FOUND",
    "message": "El token no existe o ya fue utilizado"
  }
}
```

---

### API 5: Completar Activación de Propietario (Pública)

**Endpoint:** `POST /api/activation/owner/complete`

**⚠️ Este endpoint es PÚBLICO (no requiere autenticación)**

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "password": "SecurePass123!",
  "confirmPassword": "SecurePass123!"
}
```

**Campos:**
| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| token | String (UUID) | ✅ | Token de invitación |
| password | String | ✅ | Nueva contraseña (min. 8 caracteres) |
| confirmPassword | String | ✅ | Confirmación de contraseña |

**Response 200 OK:**
```json
{
  "success": true,
  "message": "Activación completada exitosamente",
  "data": {
    "userId": 123,
    "email": "propietario@email.com",
    "names": "Juan Pérez",
    "status": "ACTIVE",
    "role": "OWNER",
    "unitCode": "APTO-101",
    "organizationName": "Torres del Parque",
    "message": "Su cuenta ha sido activada. Ya puede iniciar sesión."
  }
}
```

**Response 400 Bad Request:**
```json
{
  "success": false,
  "message": "Error de validación",
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Las contraseñas no coinciden"
  }
}
```

**Response 400 Bad Request (contraseña débil):**
```json
{
  "success": false,
  "message": "Error de validación",
  "error": {
    "code": "WEAK_PASSWORD",
    "message": "La contraseña debe tener mínimo 8 caracteres, incluir mayúsculas, minúsculas y números"
  }
}
```

---

## Códigos de Error

| Código | HTTP Status | Descripción |
|--------|-------------|-------------|
| VALIDATION_ERROR | 400 | Error de validación de datos |
| WEAK_PASSWORD | 400 | Contraseña no cumple requisitos |
| TOKEN_EXPIRED | 400 | Token de invitación expirado |
| TOKEN_INVALID | 400 | Token no válido o ya usado |
| PASSWORDS_MISMATCH | 400 | Las contraseñas no coinciden |
| NOT_FOUND | 404 | Recurso no encontrado |
| DUPLICATE_CODE | 400 | Código de unidad duplicado |
| DUPLICATE_DOCUMENT | 400 | Documento de identidad duplicado |
| MAX_RANGE_EXCEEDED | 400 | Rango excede máximo (500) |
| INVALID_FILE_FORMAT | 400 | Formato de archivo no soportado |

---

## 1. Registro de Usuario con Identificación

### Descripción
Todo usuario debe registrarse con tipo y número de documento obligatorio.

### Tipos de Documento Soportados (Colombia)
| Código | Nombre | Regex | Aplica a |
|--------|--------|-------|----------|
| CC | Cédula de Ciudadanía | `^\d{6,10}$` | NATURAL |
| NIT | NIT | `^\d{9}-\d$` | JURIDICA |
| CE | Cédula de Extranjería | `^[A-Z0-9]{6,12}$` | NATURAL |
| TI | Tarjeta de Identidad | `^\d{10,11}$` | NATURAL |
| PA | Pasaporte | `^[A-Z0-9]{5,20}$` | AMBOS |
| PEP | Permiso Especial Permanencia | `^\d{15}$` | NATURAL |

### Validaciones
- `documentType` y `documentNumber` son **obligatorios**
- El número debe cumplir con el regex del tipo seleccionado
- La combinación `documentType + documentNumber` debe ser **única** en el sistema

### Flujo
```
┌─────────────────┐
│  Usuario llena  │
│   formulario    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Validar tipo   │
│   de documento  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Validar formato │
│  según regex    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Verificar que   │
│ no exista       │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Crear usuario   │
└─────────────────┘
```

---

## 2. Distribución de Unidades (Manual por Rango)

### Descripción
Permite crear múltiples unidades especificando un rango numérico.

### Endpoint
```
POST /api/units/distribute
```

### Request Body
```json
{
  "organizationId": "uuid",
  "codePrefix": "APTO",
  "startNumber": 101,
  "endNumber": 110,
  "unitType": "APARTMENT",
  "vehiclesEnabled": true,
  "vehicleLimit": 2,
  "createOwner": true,
  "ownerInfo": {
    "email": "propietario@email.com",
    "firstName": "Juan",
    "lastName": "Pérez",
    "documentType": "CC",
    "documentNumber": "12345678"
  }
}
```

### Flujo
```
┌──────────────────────┐
│  Admin solicita      │
│  distribución        │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│  Validar rango       │
│  (start <= end)      │
│  (max 500 unidades)  │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│  Verificar códigos   │
│  no existentes       │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│  Crear unidades      │
│  APTO-101...APTO-110 │
└──────────┬───────────┘
           │
           ▼
    ┌──────┴──────┐
    │createOwner? │
    └──────┬──────┘
           │
     ┌─────┴─────┐
     │ SÍ       │ NO
     ▼           ▼
┌──────────┐  ┌──────────┐
│ Crear    │  │   FIN    │
│ usuario  │  └──────────┘
│ owner    │
└────┬─────┘
     │
     ▼
┌──────────────────────┐
│  Crear invitación    │
│  OWNER_INVITATION    │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│  Enviar email con    │
│  token de activación │
└──────────────────────┘
```

### Response
```json
{
  "success": true,
  "unitsCreated": 10,
  "unitCodes": ["APTO-101", "APTO-102", ...],
  "ownerInvitationSent": true,
  "message": "10 unidades creadas exitosamente"
}
```

---

## 3. Carga Masiva de Unidades (Excel/CSV)

### Descripción
Permite cargar múltiples unidades desde un archivo Excel o CSV con validación previa.

### Endpoints

#### Validación (sin procesar)
```
POST /api/units/bulk-upload/validate
Content-Type: multipart/form-data

file: archivo.xlsx
organizationId: uuid
```

#### Procesamiento
```
POST /api/units/bulk-upload/process
Content-Type: multipart/form-data

file: archivo.xlsx
organizationId: uuid
```

### Formato del Archivo
| Columna | Descripción | Obligatorio |
|---------|-------------|-------------|
| code | Código único de la unidad | ✅ |
| unitType | APARTMENT, HOUSE, LOCAL, OFFICE, WAREHOUSE, PARKING, OTHER | ✅ |
| vehiclesEnabled | true/false | ❌ (default: false) |
| vehicleLimit | Número máximo de vehículos | ❌ (default: 0) |
| ownerEmail | Email del propietario | ❌ |
| ownerFirstName | Nombre del propietario | ❌ |
| ownerLastName | Apellido del propietario | ❌ |
| ownerDocumentType | CC, NIT, CE, TI, PA, PEP | ❌ |
| ownerDocumentNumber | Número de documento | ❌ |

### Flujo de Validación
```
┌──────────────────────┐
│  Subir archivo       │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│  Parsear filas       │
│  (Excel con POI)     │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│  Validar cada fila:  │
│  - Campos requeridos │
│  - Formato documento │
│  - Duplicados        │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│  Retornar resumen    │
│  de validación       │
└──────────────────────┘
```

### Response de Validación
```json
{
  "valid": false,
  "totalRows": 100,
  "validRows": 95,
  "invalidRows": 5,
  "errors": [
    {
      "row": 15,
      "field": "ownerDocumentNumber",
      "message": "El formato del documento no es válido para tipo CC"
    }
  ],
  "duplicateCodes": ["APTO-101"],
  "existingCodes": ["APTO-102"]
}
```

### Flujo de Procesamiento
```
┌──────────────────────┐
│  Validar archivo     │
│  (mismo proceso)     │
└──────────┬───────────┘
           │
           ▼
    ┌──────┴──────┐
    │  ¿Válido?   │
    └──────┬──────┘
           │
     ┌─────┴─────┐
     │ SÍ       │ NO
     ▼           ▼
┌──────────┐  ┌──────────┐
│ Por cada │  │  Error   │
│   fila:  │  └──────────┘
└────┬─────┘
     │
     ▼
┌──────────────────────┐
│  1. Crear unidad     │
│  2. Si hay owner:    │
│     - Crear usuario  │
│     - Crear invit.   │
│     - Enviar email   │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│  Retornar resumen    │
│  de procesamiento    │
└──────────────────────┘
```

---

## 4. Configuración de Vehículos por Unidad

### Descripción
Cada unidad puede tener habilitada la gestión de vehículos con un límite configurable.

### Campos
| Campo | Tipo | Descripción |
|-------|------|-------------|
| vehiclesEnabled | Boolean | Habilita/deshabilita gestión de vehículos |
| maxVehicles | Integer | Límite máximo de vehículos (existente de V6) |

### Validaciones
- Si `vehiclesEnabled = false`, no se pueden registrar vehículos
- Si `vehiclesEnabled = true`, se respeta el límite `maxVehicles`
- Por defecto: `vehiclesEnabled = false`, `maxVehicles = 0`

---

## 5. Flujo de Invitación de Propietario (OWNER)

### Descripción
Cuando se crea un propietario, se genera una invitación especial que permite al usuario activar su cuenta y recibir automáticamente el rol OWNER.

### Estados de Invitación
| Estado | Descripción |
|--------|-------------|
| PENDING | Invitación creada, no enviada |
| SENT | Email enviado correctamente |
| FAILED | Falló el envío del email |
| BOUNCED | Email rebotado |

### Flujo Completo
```
┌──────────────────────────────────────────────────────────────────┐
│                     ADMINISTRADOR                                 │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  Crear unidad    │
                    │  con propietario │
                    └────────┬─────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  Crear usuario   │
                    │  (status=PENDING)│
                    └────────┬─────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │ Crear invitación │
                    │ OWNER_INVITATION │
                    │ token = UUID     │
                    │ expiresAt = +48h │
                    └────────┬─────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  Enviar email    │
                    │  con link:       │
                    │  /activate?      │
                    │  token=xxx       │
                    └────────┬─────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │ Registrar en     │
                    │ invitation_audit │
                    └──────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                     PROPIETARIO                                   │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  Recibe email    │
                    │  Click en link   │
                    └────────┬─────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│  GET /api/activation/owner/validate/{token}                       │
└────────────────────────────────┬─────────────────────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  Validar token:  │
                    │  - Existe        │
                    │  - No expirado   │
                    │  - Status PENDING│
                    └────────┬─────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  Retornar datos  │
                    │  del usuario     │
                    │  (email, nombre) │
                    └────────┬─────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│  POST /api/activation/owner/complete                              │
│  {                                                                │
│    "token": "xxx",                                                │
│    "password": "SecurePass123!",                                  │
│    "confirmPassword": "SecurePass123!"                            │
│  }                                                                │
└────────────────────────────────┬─────────────────────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  1. Validar      │
                    │     passwords    │
                    │  2. Validar      │
                    │     token        │
                    └────────┬─────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  Actualizar      │
                    │  usuario:        │
                    │  - password=hash │
                    │  - status=ACTIVE │
                    └────────┬─────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  Asignar rol     │
                    │  OWNER a unidad  │
                    └────────┬─────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  Marcar invit.   │
                    │  como ACCEPTED   │
                    └────────┬─────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  Enviar email    │
                    │  confirmación    │
                    └────────┬─────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  Usuario puede   │
                    │  hacer login     │
                    └──────────────────┘
```

---

## 6. Reenvío de Invitaciones

### Descripción
El administrador puede reenviar invitaciones que no fueron recibidas o expiraron.

### Restricciones
| Configuración | Valor Default | Descripción |
|---------------|---------------|-------------|
| max_invitation_retries | 3 | Máximo reintentos por invitación |
| invitation_expiration_hours | 48 | Horas hasta expiración |
| min_retry_interval_hours | 1 | Horas mínimas entre reintentos |

### Flujo
```
┌──────────────────────┐
│  Admin solicita      │
│  reenvío             │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│  Validar:            │
│  - retry_count < max │
│  - intervalo > min   │
└──────────┬───────────┘
           │
           ▼
    ┌──────┴──────┐
    │  ¿Válido?   │
    └──────┬──────┘
           │
     ┌─────┴─────┐
     │ SÍ       │ NO
     ▼           ▼
┌──────────┐  ┌─────────────┐
│ Generar  │  │ Error:      │
│ nuevo    │  │ "Límite     │
│ token    │  │ alcanzado"  │
└────┬─────┘  └─────────────┘
     │
     ▼
┌──────────────────────┐
│  Actualizar:         │
│  - token = nuevo     │
│  - expiresAt = +48h  │
│  - retry_count++     │
│  - last_retry_at=now │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│  Enviar email        │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│  Registrar en        │
│  invitation_audit    │
│  (action=RESEND)     │
└──────────────────────┘
```

---

## 7. Auditoría de Invitaciones

### Tabla `invitation_audit_log`
Registra todas las acciones sobre invitaciones.

| Acción | Descripción |
|--------|-------------|
| INVITATION_CREATED | Invitación creada |
| EMAIL_SENT | Email enviado |
| EMAIL_FAILED | Fallo en envío |
| INVITATION_RESENT | Invitación reenviada |
| INVITATION_ACCEPTED | Usuario activó cuenta |
| INVITATION_EXPIRED | Invitación expiró |
| INVITATION_CANCELLED | Invitación cancelada |

### Ejemplo de Registro
```json
{
  "id": "uuid",
  "invitationId": "uuid",
  "action": "EMAIL_SENT",
  "performedBy": "admin-uuid",
  "details": "{\"emailProvider\": \"SES\", \"messageId\": \"xxx\"}",
  "createdAt": "2026-02-14T10:30:00Z"
}
```

---

## 8. Configuración del Sistema

### Tabla `system_configuration`
Almacena parámetros configurables.

| Clave | Tipo | Default | Descripción |
|-------|------|---------|-------------|
| max_invitation_retries | INTEGER | 3 | Máximo reintentos |
| invitation_expiration_hours | INTEGER | 48 | Horas de expiración |
| min_retry_interval_hours | INTEGER | 1 | Intervalo mínimo |

---

## 9. Diagrama de Arquitectura

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              ENTRY POINTS                                    │
│  ┌─────────────────────────┐  ┌──────────────────────────────────────────┐  │
│  │ OwnerActivationRouter   │  │ UnitDistributionRouter                   │  │
│  │ /api/activation/owner/* │  │ /api/units/distribute                    │  │
│  │                         │  │ /api/units/bulk-upload/*                 │  │
│  └────────────┬────────────┘  └────────────────────┬─────────────────────┘  │
└───────────────┼─────────────────────────────────────┼────────────────────────┘
                │                                     │
                ▼                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                               USE CASES                                      │
│  ┌─────────────────────────┐  ┌─────────────────────────────────────────┐   │
│  │ OwnerActivationUseCase  │  │ UnitDistributionUseCase                 │   │
│  │ - validateToken()       │  │ - distribute()                          │   │
│  │ - completeActivation()  │  │                                         │   │
│  └────────────┬────────────┘  └────────────────────┬────────────────────┘   │
│               │                ┌────────────────────┘                        │
│               │                │  ┌─────────────────────────────────────┐   │
│               │                │  │ UnitBulkUploadUseCase               │   │
│               │                │  │ - validate()                        │   │
│               │                │  │ - processBulk()                     │   │
│               │                │  └─────────────────────────────────────┘   │
└───────────────┼────────────────┼────────────────────────────────────────────┘
                │                │
                ▼                ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                               DOMAIN                                         │
│  ┌──────────────────────┐  ┌────────────────────┐  ┌─────────────────────┐  │
│  │ Models               │  │ Validators         │  │ Gateways            │  │
│  │ - AuthUser           │  │ - UserIdentif.     │  │ - AuthUserRepo      │  │
│  │ - Unit               │  │ - UnitDistrib.     │  │ - UnitRepo          │  │
│  │ - Invitation         │  │ - BulkUpload       │  │ - InvitationRepo    │  │
│  │ - DocumentType       │  │                    │  │ - NotificationGw    │  │
│  │ - UnitDistribution   │  │                    │  │ - SystemConfigRepo  │  │
│  │ - BulkUploadResult   │  │                    │  │ - InvitationAudit   │  │
│  └──────────────────────┘  └────────────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         DRIVEN ADAPTERS                                      │
│  ┌──────────────────────┐  ┌────────────────────┐  ┌─────────────────────┐  │
│  │ R2DBC Repositories   │  │ Email Adapter      │  │ System Config       │  │
│  │ - AuthUserAdapter    │  │ - AWS SES          │  │ Adapter             │  │
│  │ - UnitAdapter        │  │ - sendOwnerInvit.  │  │                     │  │
│  │ - InvitationAdapter  │  │                    │  │                     │  │
│  └──────────────────────┘  └────────────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            DATABASE                                          │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │ MySQL                                                                 │   │
│  │ - users (+ document_type, document_number)                           │   │
│  │ - unit (+ vehicles_enabled)                                          │   │
│  │ - invitations (+ mail_status, retry_count)                           │   │
│  │ - document_types                                                      │   │
│  │ - invitation_audit_log                                                │   │
│  │ - system_configuration                                                │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 10. Permisos Requeridos

| Permiso | Descripción |
|---------|-------------|
| UNITS_DISTRIBUTE | Permite distribuir unidades por rango |
| UNITS_BULK_UPLOAD | Permite carga masiva de unidades |
| INVITATIONS_BULK_RESEND | Permite reenviar invitaciones masivamente |

---

## 11. Archivos Creados/Modificados

### Migración
- `V10__user_identification_and_units_enhancement.sql`

### Domain Models
- `DocumentType.java` (nuevo)
- `InvitationMailStatus.java` (nuevo)
- `UnitDistribution.java` (nuevo)
- `OwnerInfo.java` (nuevo)
- `BulkUploadResult.java` (nuevo)
- `BulkUnitRow.java` (nuevo)
- `AuthUser.java` (modificado)
- `Unit.java` (modificado)
- `Invitation.java` (modificado)
- `InvitationType.java` (modificado)

### Validators
- `UserIdentificationValidator.java` (nuevo)
- `UnitDistributionValidator.java` (nuevo)
- `BulkUploadValidator.java` (nuevo)

### Gateways
- `AuthUserRepository.java` (modificado)
- `UnitRepository.java` (modificado)
- `InvitationRepository.java` (modificado)
- `SystemConfigurationRepository.java` (nuevo)
- `InvitationAuditRepository.java` (nuevo)
- `NotificationGateway.java` (modificado)

### Use Cases
- `UnitDistributionUseCase.java` (nuevo)
- `UnitBulkUploadUseCase.java` (nuevo)
- `OwnerActivationUseCase.java` (nuevo)

### Driven Adapters
- `AuthUserEntity.java` (modificado)
- `AuthUserRepositoryAdapter.java` (modificado)
- `UnitEntity.java` (modificado)
- `UnitRepositoryAdapter.java` (modificado)
- `InvitationEntity.java` (modificado)
- `InvitationRepositoryAdapter.java` (modificado)
- `SystemConfigurationEntity.java` (nuevo)
- `SystemConfigurationRepositoryAdapter.java` (nuevo)
- `InvitationAuditEntity.java` (nuevo)
- `InvitationAuditRepositoryAdapter.java` (nuevo)
- `EmailNotificationAdapter.java` (modificado)

### Entry Points
- `OwnerActivationHandler.java` (nuevo)
- `OwnerActivationRouterRest.java` (nuevo)
- `UnitDistributionHandler.java` (nuevo)
- `UnitDistributionRouterRest.java` (nuevo)
- DTOs varios (nuevos)

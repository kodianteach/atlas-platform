# Atlas Platform Backend - Guía de Configuración y Ejecución

## Tabla de Contenidos

- [Requisitos Previos](#requisitos-previos)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [Configuración de la Base de Datos](#configuración-de-la-base-de-datos)
  - [Opción 1: MySQL con Docker](#opción-1-mysql-con-docker-recomendado)
  - [Opción 2: MySQL Local (sin Docker)](#opción-2-mysql-local-sin-docker)
- [Variables de Entorno](#variables-de-entorno)
- [Inicializar y Ejecutar el Backend](#inicializar-y-ejecutar-el-backend)
  - [Compilar el proyecto](#1-compilar-el-proyecto)
  - [Ejecutar en modo desarrollo](#2-ejecutar-en-modo-desarrollo)
  - [Ejecutar con Docker (producción)](#3-ejecutar-con-docker-producción)
- [Migraciones de Base de Datos](#migraciones-de-base-de-datos)
- [Endpoints Útiles](#endpoints-útiles)
- [Usuarios y Roles de Prueba](#usuarios-y-roles-de-prueba)
- [Troubleshooting](#troubleshooting)
- [Stack Tecnológico](#stack-tecnológico)

---

## Requisitos Previos

| Herramienta        | Versión mínima | Notas                                  |
|--------------------|----------------|----------------------------------------|
| **Java JDK**       | 21             | Se recomienda Eclipse Temurin          |
| **Gradle**         | 8.x            | Incluido vía `gradlew` (Gradle Wrapper)|
| **Docker**         | 20.x           | Para levantar MySQL con Docker         |
| **MySQL**          | 8.0            | Solo si NO se usa Docker               |
| **Git**            | 2.x            | Control de versiones                   |

---

## Estructura del Proyecto

El proyecto sigue **Clean Architecture** (scaffold de Bancolombia):

```
atlas/
├── applications/
│   └── app-service/                 # Punto de entrada (MainApplication.java)
├── domain/
│   ├── model/                       # Entidades y modelos de dominio
│   └── usecase/                     # Casos de uso / lógica de negocio
├── infrastructure/
│   ├── driven-adapters/
│   │   ├── r2dbc-postgresql/        # Adaptador R2DBC para MySQL + Flyway migrations
│   │   └── notification/            # Adaptador de notificaciones por email
│   ├── entry-points/
│   │   └── reactive-web/            # API REST reactiva (WebFlux + Swagger)
│   └── helpers/
│       ├── jwt-helper/              # Utilidades JWT (autenticación)
│       └── tenant-context/          # Contexto multitenant
├── deployment/
│   └── Dockerfile                   # Imagen Docker para producción
├── docs/                            # Documentación adicional
├── build.gradle                     # Configuración principal de Gradle
├── main.gradle                      # Configuración compartida de subproyectos
├── settings.gradle                  # Módulos del proyecto
└── gradle.properties                # Propiedades del proyecto
```

> **Nota:** La carpeta `r2dbc-postgresql` es un nombre heredado del scaffold. El proyecto usa **MySQL**, no PostgreSQL.

---

## Configuración de la Base de Datos

### Opción 1: MySQL con Docker (Recomendado)

El proyecto no incluye un `docker-compose.yml`, pero puedes levantar MySQL rápidamente con Docker:

**1. Crear y ejecutar el contenedor:**

```bash
docker run -d \
  --name atlas-mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=12345 \
  -e MYSQL_DATABASE=atlas_platform \
  -v atlas_mysql_data:/var/lib/mysql \
  --restart unless-stopped \
  mysql:8.0 --default-authentication-plugin=mysql_native_password --ssl=0
```

**O crear un archivo `docker-compose.yml` en la raíz del proyecto:**

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: atlas-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: 12345
      MYSQL_DATABASE: atlas_platform
    ports:
      - "3306:3306"
    volumes:
      - atlas_mysql_data:/var/lib/mysql
    command: --default-authentication-plugin=mysql_native_password --ssl=0
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p12345"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  atlas_mysql_data:
    driver: local
```

Luego ejecutar:

```bash
docker-compose up -d
```

**2. Verificar que MySQL está corriendo:**

```bash
docker ps
```

**3. Conectarse al contenedor (opcional):**

```bash
docker exec -it atlas-mysql mysql -u root -p12345
```

**4. Detener el contenedor:**

```bash
docker stop atlas-mysql

# O con docker-compose
docker-compose down
```

**5. Eliminar datos persistentes (reiniciar todo):**

```bash
docker rm atlas-mysql
docker volume rm atlas_mysql_data

# O con docker-compose
docker-compose down -v
```

---

### Opción 2: MySQL Local (sin Docker)

**1. Instalar MySQL 8.0:**

- **Windows:** Descargar desde [MySQL Installer](https://dev.mysql.com/downloads/installer/)
- **macOS:** `brew install mysql@8.0`
- **Linux (Ubuntu/Debian):** `sudo apt-get install mysql-server`

**2. Iniciar el servicio:**

```bash
# Windows (si se instaló como servicio)
net start MySQL80

# macOS
brew services start mysql@8.0

# Linux
sudo systemctl start mysql
```

**3. Crear la base de datos:**

```sql
-- Conectarse como root
mysql -u root -p

-- Crear la base de datos
CREATE DATABASE atlas_platform CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Verificar
SHOW DATABASES;
```

> **Nota:** Las tablas se crean automáticamente al iniciar el backend gracias a Flyway. No necesitas crear tablas manualmente.

---

## Variables de Entorno

La aplicación usa variables de entorno con valores por defecto para desarrollo local. Solo necesitas configurarlas si tu entorno difiere:

| Variable                | Valor por defecto                              | Descripción                          |
|-------------------------|------------------------------------------------|--------------------------------------|
| `DB_HOST`               | `localhost`                                    | Host de MySQL                        |
| `DB_PORT`               | `3306`                                         | Puerto de MySQL                      |
| `DB_NAME`               | `atlas_platform`                               | Nombre de la base de datos           |
| `DB_USERNAME`           | `root`                                         | Usuario de MySQL                     |
| `DB_PASSWORD`           | `12345`                                        | Contraseña de MySQL                  |
| `JWT_SECRET`            | (clave por defecto incluida)                   | Clave secreta para firmar JWT        |
| `JWT_ACCESS_EXPIRATION` | `3600000` (1 hora)                             | Expiración del access token (ms)     |
| `JWT_REFRESH_EXPIRATION`| `86400000` (24 horas)                          | Expiración del refresh token (ms)    |
| `JWT_ISSUER`            | `atlas-platform`                               | Emisor del JWT                       |
| `MAIL_HOST`             | `smtp.gmail.com`                               | Servidor SMTP                        |
| `MAIL_PORT`             | `587`                                          | Puerto SMTP                          |
| `MAIL_USERNAME`         | `kodianteach@gmail.com`                        | Email para envío de correos          |
| `MAIL_PASSWORD`         | (app password configurado)                     | Contraseña de aplicación Gmail       |
| `NOTIFICATION_ENABLED`  | `true`                                         | Habilitar/deshabilitar notificaciones|

**Configurar variables de entorno (opcional):**

```powershell
# Windows (PowerShell)
$env:DB_HOST="localhost"
$env:DB_PORT="3306"
$env:DB_NAME="atlas_platform"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="12345"
```

```bash
# Linux / macOS
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=atlas_platform
export DB_USERNAME=root
export DB_PASSWORD=12345
```

> Con los valores por defecto y MySQL corriendo en `localhost:3306` con root/12345, no necesitas configurar nada.

---

## Inicializar y Ejecutar el Backend

### 1. Compilar el proyecto

```bash
# Windows
.\gradlew.bat clean build

# Linux / macOS
./gradlew clean build
```

Para compilar **sin ejecutar tests** (más rápido):

```bash
# Windows
.\gradlew.bat clean build -x test

# Linux / macOS
./gradlew clean build -x test
```

### 2. Ejecutar en modo desarrollo

**Opción A: Con Gradle (recomendado para desarrollo)**

```bash
# Windows
.\gradlew.bat :app-service:bootRun

# Linux / macOS
./gradlew :app-service:bootRun
```

**Opción B: Ejecutar el JAR directamente**

```bash
# Primero compilar
.\gradlew.bat clean build -x test

# Luego ejecutar
java -jar applications/app-service/build/libs/AtlasPlatform.jar
```

La aplicación estará disponible en: **http://localhost:8080**

### 3. Ejecutar con Docker (Producción)

**Paso 1: Compilar el JAR**

```bash
.\gradlew.bat clean build -x test
```

**Paso 2: Copiar el JAR al directorio de deployment**

```powershell
# Windows (PowerShell)
Copy-Item applications\app-service\build\libs\AtlasPlatform.jar deployment\
```

```bash
# Linux / macOS
cp applications/app-service/build/libs/AtlasPlatform.jar deployment/
```

**Paso 3: Construir la imagen Docker**

```bash
cd deployment
docker build -t atlas-platform-backend:latest .
```

**Paso 4: Ejecutar el contenedor**

```bash
docker run -d \
  --name atlas-backend \
  -p 8080:8080 \
  -e DB_HOST=host.docker.internal \
  -e DB_PORT=3306 \
  -e DB_NAME=atlas_platform \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=12345 \
  atlas-platform-backend:latest
```

> En Windows/macOS, `host.docker.internal` apunta al host de Docker. En Linux, usa `--network=host` o la IP del host.

---

## Migraciones de Base de Datos

El proyecto utiliza **Flyway** para gestionar las migraciones automáticamente al iniciar la aplicación.

### Ubicación de los scripts

```
infrastructure/driven-adapters/r2dbc-postgresql/src/main/resources/db/migration/
```

### Scripts disponibles

| Script | Descripción |
|--------|-------------|
| `V1__create_initial_schema.sql` | Esquema inicial: users, roles, companies, organizations, zones, towers, units, modules, permissions, user_organizations, user_roles_multi |
| `V2__create_units_visits_schema.sql` | Esquema de relación unidades-visitas |
| `V3__seed_initial_data.sql` | Datos semilla: módulos (ATLAS_CORE, VISIT_CONTROL, ACCESS_CONTROL), roles, permisos y asignaciones |
| `V4__create_posts_polls_schema.sql` | Esquema de publicaciones y encuestas |
| `V5__create_admin_preregistration_schema.sql` | Esquema de pre-registro de administradores |

### Entities principales

El esquema incluye las siguientes tablas clave:

- **`users`** — Cuentas de usuario
- **`role`** — Definición de roles del sistema
- **`company`** — Empresas/holdings (tenant principal)
- **`organization`** — Ciudadela o Conjunto Residencial (CIUDADELA/CONJUNTO)
- **`zone`** — Zonas dentro de una organización
- **`tower`** — Torres dentro de zonas (solo CIUDADELA)
- **`unit`** — Unidades habitacionales (APARTMENT/HOUSE)
- **`modules`** — Módulos del sistema multitenant
- **`permissions`** — Permisos granulares por recurso y acción
- **`user_organizations`** — Membresía multitenant
- **`user_roles_multi`** — Roles por usuario por organización

---

## Endpoints Útiles

Una vez levantado el backend en `http://localhost:8080`:

| Endpoint | Descripción |
|----------|-------------|
| `http://localhost:8080/swagger-ui.html` | Documentación Swagger UI |
| `http://localhost:8080/v3/api-docs` | OpenAPI JSON spec |
| `http://localhost:8080/actuator/health` | Health check |
| `http://localhost:8080/actuator/prometheus` | Métricas Prometheus |

### CORS

Los orígenes permitidos por defecto son:
- `http://localhost:4200` (Angular frontend)
- `http://localhost:8080`

---

## Usuarios y Roles de Prueba

Después de que Flyway ejecute las migraciones (V3), estarán disponibles los siguientes roles del sistema:

### Módulos

| Código | Nombre | Descripción |
|--------|--------|-------------|
| `ATLAS_CORE` | Atlas Core | Gestión de organizaciones residenciales |
| `VISIT_CONTROL` | Control de Visitas | Solicitudes y aprobación de visitas |
| `ACCESS_CONTROL` | Control de Acceso | Códigos QR y validación de acceso |

### Roles

| Código | Nombre | Módulo |
|--------|--------|--------|
| `SUPER_ADMIN` | Super Administrador | Global |
| `ADMIN_ATLAS` | Administrador Atlas | ATLAS_CORE |
| `OWNER` | Propietario | ATLAS_CORE |
| `TENANT` | Arrendatario | ATLAS_CORE |
| `FAMILY` | Familiar | ATLAS_CORE |
| `GUEST` | Invitado | ATLAS_CORE |
| `SECURITY` | Seguridad | ACCESS_CONTROL |

> Los usuarios deben ser creados a través de la API de registro. Los roles y permisos se asignan automáticamente.

---

## Troubleshooting

### Error: "Connection refused" al conectar con MySQL

1. Verifica que MySQL está corriendo:
   ```bash
   docker ps                        # si usas Docker
   mysql --version                   # si es local
   ```
2. Verifica que el puerto 3306 no esté ocupado:
   ```powershell
   netstat -an | findstr 3306       # Windows
   ```
   ```bash
   lsof -i :3306                    # Linux/macOS
   ```

### Error: "Access denied for user"

Verifica las credenciales. Por defecto el backend usa `root` / `12345`. Si creaste un usuario diferente, configura las variables `DB_USERNAME` y `DB_PASSWORD`.

### Error de compilación con Java

El proyecto requiere **JDK 21**:
```bash
java -version
```
Si no lo tienes, descárgalo desde [Eclipse Temurin](https://adoptium.net/).

### Flyway: "Migration checksum mismatch"

Si modificaste un script de migración ya aplicado:
```sql
-- Conectarse a MySQL
mysql -u root -p12345 atlas_platform

-- Eliminar la entrada problemática
DELETE FROM flyway_schema_history WHERE version = 'X';
```
Luego reinicia la aplicación.

### El puerto 8080 está ocupado

Cambia el puerto con una variable de entorno:

```powershell
# Windows (PowerShell)
$env:SERVER_PORT="9090"
.\gradlew.bat :app-service:bootRun
```

```bash
# O directamente con argumento
./gradlew :app-service:bootRun --args='--server.port=9090'
```

### Docker: contenedor no conecta a MySQL del host

Usa `host.docker.internal` como `DB_HOST`:
```bash
docker run -e DB_HOST=host.docker.internal ...
```

En Linux, agrega `--add-host=host.docker.internal:host-gateway` al comando docker run.

---

## Stack Tecnológico

| Tecnología | Versión | Uso |
|------------|---------|-----|
| Java | 21 | Lenguaje principal |
| Spring Boot | 4.0.1 | Framework backend |
| Spring WebFlux | - | API REST reactiva |
| Spring Security | - | Autenticación y autorización |
| R2DBC (MySQL) | 1.1.3 | Conexión reactiva a MySQL |
| Flyway | - | Migraciones de base de datos |
| JWT (jjwt) | 0.12.3 | Autenticación por tokens |
| Lombok | 1.18.42 | Reducción de boilerplate |
| Swagger/OpenAPI | 3.0.0 | Documentación de API |
| MySQL | 8.0 | Base de datos |
| Docker | - | Containerización |
| Gradle | 8.x | Build system |
| JaCoCo | 0.8.14 | Cobertura de código |
| Pitest | 1.19.0-rc.2 | Mutation testing |
| Micrometer | - | Métricas (Prometheus) |

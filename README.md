# RazonaPro Backend

API REST de la plataforma de preparación académica RazonaPro.

**Stack** · Spring Boot 4.0.6 · Java 21 · PostgreSQL 18 · JWT (jjwt 0.12) · Lombok · springdoc-openapi 2.8 · Bucket4j (rate limit) · Spring Cache

---

## Arquitectura

Domain-Driven package structure con hexagonal en el módulo `aitried`.

```
com.razonapro.razonaprobackend/
├── RazonaProBackendApplication
├── infrastructure/
│   ├── config/        AppProperties, AiModelProperties, CorsConfig,
│   │                  SecurityConfig, OpenApiConfig, DataInitializer,
│   │                  ApplicationStartupLogger
│   ├── security/      JwtService, JwtAuthenticationFilter,
│   │                  AuthRateLimitFilter, UserPrincipal
│   ├── email/         EmailService
│   ├── util/          BooleanToYNConverter, IdGenerator, TokenHashUtil
│   ├── jobs/          TokenCleanupJob (@Scheduled)
│   └── health/        HealthController
├── shared/
│   ├── dto/           ApiResponse, PagedResponse
│   ├── exception/     ErrorCode, ApiException, ResourceNotFoundException,
│   │                  GlobalExceptionHandler
│   ├── ids/           StudentId, QuestionId, OptionId, TestPK, TriedId,
│   │                  AiTriedId, AiTriedResponseId
│   ├── jpa/           Normalizable, NormalizingEntityListener
│   └── util/          StringNormalizer
└── domain/
    ├── auth/          login / register / forgot / reset (unificados)
    ├── admin/         CRUD admins
    ├── student/       CRUD estudiantes + /me
    ├── program/       CRUD programas
    ├── competence/    CRUD competencias
    ├── question/      preguntas + opciones
    ├── test/          tests + asignación de preguntas
    ├── tried/         intentos de tests
    ├── ranking/       rankings + leaderboards
    ├── stats/         /stats/home (público) para métricas del landing
    └── aitried/
        ├── port/      AiQuestionGenerator (puerto hexagonal)
        └── adapter/   NoOp · HuggingFace · Local
```

---

## Variables de entorno (`.env`)

```env
SERVER_PORT=8080
DB_URL=jdbc:postgresql://localhost:5432/postgres
DB_USERNAME=postgres
DB_PASSWORD=<tu_pass>
DB_SCHEMA=razonapro

JWT_SECRET=<64+ chars random>
JWT_EXPIRATION_MS=86400000
JWT_EMAIL_VERIFY_EXPIRATION_MS=86400000
JWT_PASSWORD_RESET_EXPIRATION_MS=900000

MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=<tu_email>
MAIL_PASSWORD=<app_password>

CORS_ALLOWED_ORIGINS=http://localhost:3000
CORS_ALLOWED_METHODS=GET,POST,PUT,DELETE,PATCH,OPTIONS
CORS_ALLOWED_HEADERS=*
CORS_EXPOSED_HEADERS=Authorization
CORS_ALLOW_CREDENTIALS=true
CORS_MAX_AGE=3600

FRONTEND_URL=http://localhost:3000

ADMIN_INITIALIZER_ENABLED=true
ADMIN_INITIAL_EMAIL=admin@razonapro.com
ADMIN_INITIAL_PASSWORD=Admin123!
ADMIN_INITIAL_FIRST_NAME=Super
ADMIN_INITIAL_LAST_NAME=Admin
ADMIN_INITIAL_PHONE=+573001234567

# Transfer Learning (deshabilitado por default)
AI_MODEL_ENABLED=false
AI_MODEL_PROVIDER=NONE         # NONE | HUGGINGFACE | LOCAL
HF_API_TOKEN=
HF_MODEL_ID=
AI_MODEL_LOCAL_PATH=
AI_MODEL_MIN_CONFIDENCE=0.7
```

---

## Levantar local

```bash
./mvnw spring-boot:run
```

Al arrancar verás un resumen con el frontend configurado, profile, schema y estado del módulo AI.

---

## Endpoints principales

### Auth (público)
| Método | Ruta                              | Descripción                                   |
|--------|-----------------------------------|-----------------------------------------------|
| POST   | `/api/auth/login`                 | Login unificado admin/estudiante              |
| POST   | `/api/auth/register`              | Registro de estudiante                        |
| GET    | `/api/auth/verify-email?token=`   | Verificación de email                         |
| POST   | `/api/auth/forgot-password`       | Recuperación unificada (email + code + phone) |
| POST   | `/api/auth/reset-password`        | Aplicar nueva contraseña                      |

### Stats (público)
| Método | Ruta                | Descripción                            |
|--------|---------------------|----------------------------------------|
| GET    | `/api/stats/home`   | Métricas agregadas para el landing     |

### Admin (rol ADMIN)
- `/api/admins` — CRUD de administradores (un admin puede crear otros)
- `/api/students` — gestión de estudiantes
- `/api/programs` — programas
- `/api/competences` — competencias
- `/api/competences/{id}/questions` — banco de preguntas
- `/api/tests` — tests y asignación de preguntas
- `/api/rankings` — rankings

### Student (rol STUDENT)
- `/api/students/me` — perfil propio
- `/api/trieds/my`, `/start`, `/{id}/answer`, `/{id}/finish` — intentos de tests
- `/api/ai-trieds/*` — sesiones con IA

---

## Códigos de error

Toda respuesta de error: `{ success: false, code: "XXX-NNN", message: "..." }`.

| Prefijo | Familia                |
|---------|------------------------|
| AUTH-   | Autenticación          |
| VAL-    | Validación             |
| RES-    | Recurso no encontrado  |
| CFL-    | Conflicto / duplicado  |
| BIZ-    | Regla de negocio       |
| SRV-    | Error servidor         |

Listado completo: `shared/exception/ErrorCode.java`.

---

## Transfer Learning

`AiQuestionGenerator` es un **puerto** (hexagonal) con tres implementaciones intercambiables vía `AI_MODEL_PROVIDER`:

| Provider     | Adapter                          | Estado                  |
|--------------|----------------------------------|-------------------------|
| `NONE`       | `NoOpAiQuestionGenerator`        | Default, deshabilitado  |
| `HUGGINGFACE`| `HuggingFaceQuestionGenerator`   | Esqueleto listo         |
| `LOCAL`      | `LocalModelQuestionGenerator`    | Esqueleto listo         |

Para activar: setear `AI_MODEL_ENABLED=true` + `AI_MODEL_PROVIDER=...` + credenciales del proveedor. La inyección se resuelve por `@ConditionalOnProperty` sin tocar código del service.

---

## Convenciones

- **Datos en mayúscula** en DB; normalización automática en `@PrePersist`/`@PreUpdate` vía `NormalizingEntityListener`. Los services no normalizan manualmente.
- **IDs string con prefijo**: `AMN001` admin, `CPE001` competence, `QTN0001` question, `OTN001` option, `TET00001` test, `RKG001` ranking, `TRD<7random>` tried.
- **Boolean → CHAR(1) Y/N** vía `BooleanToYNConverter(autoApply=true)`.
- **JWT** solo para acceso (admin/student). Verificación de email y reset de contraseña usan **tokens opacos** UUID + SHA-256 persistidos en `student_tokens` / `admin_tokens`.
- **Schema parametrizable** vía `DB_SCHEMA`. Ninguna entidad hardcodea schema en `@Table`.
- **Rate limit** en `/api/auth/**` (10 req/min por IP, Bucket4j).
- **Limpieza automática** de tokens expirados (`TokenCleanupJob`, cron diario 3 AM).

---

## Testing

```bash
./mvnw test
```

---

## URLs útiles (desarrollo)

- Swagger: `${FRONTEND_URL}` no aplica — Swagger corre en el backend en `http://<host>:${SERVER_PORT}/swagger-ui.html`
- Health: `/api/health`
- Home stats: `/api/stats/home`
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
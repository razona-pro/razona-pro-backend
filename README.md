# RazonaPro Backend

API REST de la plataforma de preparación Saber Pro - UFPSO.

**Stack** · Spring Boot 4.0.6 · Java 21 · PostgreSQL 18 · JWT (jjwt 0.12) · Lombok · springdoc-openapi 2.8 · Bucket4j · Spring Cache

---

## Arquitectura

DDD por paquetes; arquitectura hexagonal en el módulo `aitried` (puertos y adaptadores).

```text
com.razonapro.razonaprobackend/
├── infrastructure/
│   ├── ai/            # ChatClient (puerto), CloudChatClient, OllamaChatClient,
│   │                  # PromptFactory, AiUnavailableException
│   ├── config/        # AppProperties, AiModelProperties, Cors, Security,
│   │                  # OpenApi, Jackson, DataInitializer, StartupLogger
│   ├── security/      # JwtService, JwtAuthenticationFilter, RateLimit, UserPrincipal
│   ├── email/         # EmailService
│   ├── util/          # IdGenerator, TokenHashUtil, BooleanToYNConverter
│   └── jobs/          # TokenCleanupJob
│
├── shared/            # dto · exception · ids · jpa · util
│
└── domain/
    ├── auth/
    ├── admin/
    ├── student/
    ├── program/
    ├── competence/
    ├── question/
    ├── test/
    ├── tried/
    ├── ranking/
    ├── stats/
    │
    ├── aitried/       # IA generativa (práctica en tanda + pistas)
    │   ├── port/      # AiQuestionGenerator · AiTutor · dto
    │   ├── adapter/   # AiBatchGenerator · DefaultTutor
    │   ├── model/     # AiTried · AiTriedResponse · AiQuestion
    │   └── service/   # AiTriedService
    │
    ├── notification/  # Notificaciones in-app + email masivo
    └── doubt/         # Reportes de duda sobre preguntas
```

---

## Módulo de IA

`ChatClient` es un puerto con dos implementaciones intercambiables por `AI_MODEL_PROVIDER`:

| Provider | Cliente            | Uso                                 |
|----------|--------------------|-------------------------------------|
| `CLOUD`  | `CloudChatClient`  | Groq / OpenAI / OpenRouter (OpenAI) |
| `OLLAMA` | `OllamaChatClient` | Modelo local                        |
| `NONE`   | -                  | IA deshabilitada                    |

**Flujo de práctica (batch):**
1. `POST /api/ai-trieds/start` - genera **todas** las preguntas en una sola llamada, las persiste en `ai_questions` y devuelve la primera.
2. `GET /api/ai-trieds/{id}/questions` - lista todas (sin revelar la correcta).
3. `POST /api/ai-trieds/{id}/answer` - evalúa en servidor; escribe en `ai_tried_responses` (dispara triggers de `correct_answers`/score).
4. `POST /api/ai-trieds/{id}/hint` - pista nivel 1-3, progresiva, tope 3 (`hints_used` persistido).
5. `GET /api/ai-trieds/{id}/review` - historial con respuestas y explicaciones.
6. `PUT /api/ai-trieds/{id}/finish` - cierra y calcula puntaje.

La corrección **nunca** la decide el cliente: solo envía `selectedIndex`.

---

## Endpoints

### Auth (público)
`POST /api/auth/login` · `/register` · `GET /verify-email` · `POST /forgot-password` · `/reset-password`

### Stats (público)
`GET /api/stats/home`

### Admin (ADMIN)
`/api/admins` · `/api/students` · `/api/programs` · `/api/competences` · `/api/competences/{id}/questions` · `/api/tests` · `/api/rankings` · `/api/doubts`

### Student (STUDENT)
`/api/students/me` · `/api/trieds/*` · `/api/ai-trieds/*` · `/api/notifications/*` · `POST /api/doubts`

### IA
| Método | Ruta | Descripción |
|--------|------|-------------|
| GET  | `/api/ai-trieds/status`            | Estado del proveedor IA (público) |
| POST | `/api/ai-trieds/start`             | Inicia práctica (genera batch) |
| GET  | `/api/ai-trieds/my`                | Historial de prácticas |
| GET  | `/api/ai-trieds/{id}/questions`    | Preguntas del intento |
| GET  | `/api/ai-trieds/{id}/review`       | Review con explicaciones |
| POST | `/api/ai-trieds/{id}/answer`       | Responder (evalúa servidor) |
| POST | `/api/ai-trieds/{id}/hint`         | Pista (1-3) |
| PUT  | `/api/ai-trieds/{id}/finish`       | Finalizar |
| GET  | `/api/ai-hint`                     | Pista para banco estático |

### Notificaciones (STUDENT/ADMIN)
`GET /api/notifications` · `GET /unread-count` · `PUT /read-all` · `PUT /{id}/read`

### Dudas
`POST /api/doubts` (STUDENT) · `GET /api/doubts?status=` (ADMIN) · `PUT /{id}/status` (ADMIN)

---

## Notificaciones

- **Nuevo test** → al crear un test, broadcast `@Async` a todos los estudiantes activos (in-app + email).
- **Duda reportada** → notifica in-app a todos los admins activos.
- In-app persistidas en `notifications`; correos vía `EmailService`.

---

## Convenciones

- Datos en MAYÚSCULA en BD; normalización en `@PrePersist`/`@PreUpdate`.
- IDs con prefijo: `AMN` admin, `CPE` competence, `QTN` question, `TET` test, `ATD` ai_tried, `ATE` ai_tried_response, `AQN` ai_question, `NOT` notificación, `DBT` duda.
- Boolean → `CHAR(1)` Y/N (`BooleanToYNConverter`).
- `ddl-auto=none`: el esquema lo gestiona el repo de BD. Aplicar `migration.sql` antes de levantar con el módulo IA.
- Errores: `{ success:false, code:"XXX-NNN", message }`. Familias en `ErrorCode.java`.

---

## Puesta en marcha

```bash
# 1. Base de datos (esquema completo)
psql -U postgres -d postgres -f setup.sql
# 1b. Migración módulo IA (si la BD ya existía)
psql -U postgres -d postgres -f migration.sql

# 2. Variables: copia y completa .env (define AI_CLOUD_API_KEY)

# 3. Arrancar
./mvnw spring-boot:run
```

- Swagger: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/api/health`
- Estado IA: `http://localhost:8080/api/ai-trieds/status`

---

## Testing

```bash
./mvnw test
```
# RazonaPro Backend

API REST de la plataforma de preparación Saber Pro - UFPSO.

**Stack** · Spring Boot 4.0.6 · Java 21 · PostgreSQL 18 · JWT (jjwt 0.12) · Lombok · springdoc-openapi 2.8 · Bucket4j · Spring Cache · Spring Mail

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
│   ├── security/      # JwtService, JwtAuthenticationFilter, AuthRateLimitFilter,
│   │                  # RestAuthExceptionHandler, UserPrincipal
│   ├── email/         # EmailService (plantillas HTML por evento)
│   ├── health/        # HealthController
│   ├── util/          # IdGenerator, TokenHashUtil, BooleanToYNConverter
│   └── jobs/          # TokenCleanupJob (limpieza diaria de tokens expirados)
│
├── shared/            # dto · exception (ErrorCode/ApiException) · ids · jpa · util
│
└── domain/
    ├── auth/           # Login unificado admin/estudiante, registro, verificación de
    │                   # email, forgot/reset password, cambio de contraseña con código
    ├── admin/
    ├── student/
    ├── program/
    ├── competence/
    ├── question/       # Banco estático de preguntas (CRUD + import desde IA)
    ├── test/            # Pruebas multicompetencia (PRACTICE/EXAM/TIMED)
    ├── tried/           # Intentos de pruebas estáticas: respuestas, fraude, review
    ├── ranking/         # Leaderboards configurables por período y fuente
    ├── stats/           # Métricas públicas y de administración
    │
    ├── aitried/         # IA generativa adaptativa (práctica pregunta a pregunta + pistas)
    │   ├── port/        # AiQuestionGenerator · AiTutor · dto
    │   ├── adapter/      # AiBatchGenerator · DefaultTutor
    │   ├── model/        # AiTried · AiTriedResponse · AiQuestion · AiUserCompetence
    │   └── service/      # AiTriedService
    │
    ├── notification/    # Notificaciones in-app + email masivo
    ├── doubt/           # Reportes de duda sobre preguntas (banco estático o IA)
    └── appeal/          # Apelaciones de cuentas desactivadas (plagio o manual)
```

---

## Módulo de IA (práctica adaptativa)

`ChatClient` es un puerto con dos implementaciones intercambiables por `AI_MODEL_PROVIDER`:

| Provider | Cliente            | Uso                                 |
|----------|---------------------|--------------------------------------|
| `CLOUD`  | `CloudChatClient`   | Groq / OpenAI / OpenRouter (OpenAI)  |
| `OLLAMA` | `OllamaChatClient`  | Modelo local                         |
| `NONE`   | -                   | IA deshabilitada                     |

La práctica IA es **adaptativa pregunta por pregunta**, no por lotes: cada pregunta se genera con la dificultad que corresponde al desempeño del estudiante en ese momento.

**Sistema de niveles (IRT simplificado, estilo "ranked"):**
- Cada sesión y cada competencia del estudiante tienen un nivel de 1 a 10, derivado de un `theta` acumulado (`ai_user_competence`) que persiste **entre** sesiones.
- 3 aciertos consecutivos suben un nivel; 2 fallos consecutivos bajan un nivel.
- Subir de nivel en racha otorga un bono creciente (1, 2, 3...) que se reinicia al fallar.
- El puntaje final son puntos ponderados por la dificultad de cada pregunta (no normalizado a 100) más los bonos de racha.
- Multi-competencia: una sesión puede combinar varias competencias, rotando entre ellas pregunta a pregunta; el theta inicial es el promedio de las competencias elegidas.

**Flujo:**
1. `POST /api/ai-trieds/start` - crea la sesión y genera **solo la primera** pregunta según el theta acumulado del estudiante.
2. `POST /api/ai-trieds/{id}/next-question` - genera la siguiente pregunta con la dificultad recalculada por la máquina de niveles.
3. `GET /api/ai-trieds/{id}/questions` - lista las preguntas generadas hasta ahora (sin revelar la correcta si no fue respondida).
4. `POST /api/ai-trieds/{id}/answer` - evalúa en servidor; persiste en `ai_tried_responses` y actualiza el nivel/score.
5. `POST /api/ai-trieds/{id}/hint` - pista nivel 1-3, progresiva, tope 3 (`hints_used` persistido); nunca revela la opción correcta.
6. `GET /api/ai-trieds/{id}/review` - historial con respuestas y explicaciones (solo administradores).
7. `PUT /api/ai-trieds/{id}/finish` - cierra manualmente y calcula el puntaje final, persistiendo el theta alcanzado por competencia.

La corrección **nunca** la decide el cliente: solo envía `selectedIndex`.

---

## Integridad académica: fraude y apelaciones

**Detección de fraude (pruebas estáticas, modo EXAM/TIMED):**
- El frontend reporta eventos sospechosos (cambio de pestaña, salir de la ventana, etc.) vía `POST /api/trieds/{triedId}/fraud`.
- Al acumular 2 eventos el intento pasa a estado `PLAGIO` (puntaje anulado a 0) y la cuenta del estudiante se **desactiva automáticamente** (`deactivation_reason = FRAUD`).
- Se notifica in-app y por correo a todos los administradores activos, y por correo al estudiante.
- El modo `PRACTICE` nunca penaliza.

**Apelaciones (`/api/appeals`):**
- Un estudiante con la cuenta desactivada (por fraude o manualmente por un admin) puede consultar su estado (`POST /account-status`, re-valida credenciales) y enviar una apelación (`POST /submit`), sin necesitar sesión.
- Solo puede existir una apelación `PENDING` a la vez por estudiante.
- Los administradores listan y resuelven apelaciones (`GET /api/appeals`, `PUT /{appealId}/resolve`); aprobar reactiva la cuenta y limpia el motivo de desactivación. Se notifica al estudiante por correo e in-app en ambos casos.

---

## Endpoints

### Auth (público salvo lo indicado)
`POST /api/auth/login` · `/register` · `GET /verify-email` · `POST /resend-verification` · `/forgot-password` · `/reset-password`
`POST /api/auth/change-password/request-code` · `POST /api/auth/change-password` (autenticado: envía código al correo y luego valida código + contraseña actual)

### Appeals
`POST /api/appeals/account-status` (público) · `POST /api/appeals/submit` (público)
`GET /api/appeals` (ADMIN, filtro `status`) · `PUT /api/appeals/{appealId}/resolve` (ADMIN)

### Stats (público salvo lo indicado)
`GET /api/stats/home` (público) · `GET /api/stats/admin/overview` (ADMIN) · `GET /api/stats/admin/student-performance` (ADMIN) · `GET /api/stats/admin/question-trends` (ADMIN)

### Admin (ADMIN)
`/api/admins` (CRUD + `/me`) · `/api/students` · `/api/programs` · `/api/competences` · `/api/competences/{id}/questions` · `/api/questions` · `/api/tests` · `/api/rankings` · `/api/doubts`

### Student (STUDENT)
`/api/students/me` (GET/PUT) · `/api/trieds/*` · `/api/ai-trieds/*` · `/api/notifications/*` · `POST /api/doubts`

### Programs
`GET /api/programs/active` (público) · `GET /api/programs` (ADMIN) · `GET /api/programs/{id}` (ADMIN/STUDENT) · `POST/PUT/DELETE` y `/activate` (ADMIN)

### Competences
`GET /api/competences` (público, `?active=`) · `GET /{id}` · `POST/PUT/DELETE` y `/activate` (ADMIN)

### Questions (banco estático)
| Método | Ruta | Descripción |
|--------|------|-------------|
| GET    | `/api/questions`                                          | Listado global con filtros (ADMIN) |
| GET    | `/api/competences/{competenceId}/questions`               | Por competencia |
| GET    | `/api/competences/{competenceId}/questions/{questionId}`  | Detalle |
| POST   | `/api/competences/{competenceId}/questions`               | Crear (ADMIN) |
| POST   | `/api/questions/from-ai/{aiQuestionId}`                   | Importar una pregunta generada por IA al banco (ADMIN) |
| PUT    | `/api/competences/{competenceId}/questions/{questionId}`  | Editar (ADMIN) |
| PUT    | `.../activate` · DELETE `...`                              | Activar/desactivar (ADMIN) |

### Tests (pruebas multicompetencia)
| Método | Ruta | Descripción |
|--------|------|-------------|
| GET    | `/api/tests`                                                | Listado (ADMIN ve todas, STUDENT solo activas) |
| GET    | `/api/tests/{testId}`                                       | Detalle con desglose de dificultad y competencias |
| GET    | `/api/tests/{testId}/questions`                             | Preguntas presentadas (selección determinista por `triedId`) |
| POST   | `/api/tests`                                                | Crear (ADMIN); notifica a estudiantes si `notifyStudents` |
| PUT    | `/api/tests/{testId}` · `/activate`                          | Editar / reactivar (ADMIN) |
| POST/DELETE | `/api/tests/{testId}/questions/{competenceId}/{questionId}` | Asignar/quitar pregunta (ADMIN) |
| DELETE | `/api/tests/{testId}`                                       | Desactivar (ADMIN) |

### Trieds (intentos de pruebas estáticas)
| Método | Ruta | Descripción |
|--------|------|-------------|
| GET    | `/api/trieds`                       | Historial admin (filtros `studentId`, `status`) |
| GET    | `/api/trieds/my`                    | Historial propio (STUDENT) |
| GET    | `/api/trieds/eligibility`           | Si el estudiante puede entrar/reanudar una prueba |
| POST   | `/api/trieds/start`                 | Inicia intento (selección determinista de preguntas) |
| GET    | `/api/trieds/{triedId}/resume`      | Reanuda; tiempo autoritativo del servidor |
| POST   | `/api/trieds/{triedId}/answer`      | Responder (evalúa servidor) |
| POST   | `/api/trieds/{triedId}/fraud`       | Reporta evento sospechoso |
| PUT    | `/api/trieds/{triedId}/finish`      | Finaliza manualmente |
| GET    | `/api/trieds/{triedId}/review`      | Retroalimentación completa (estudiante: un solo uso; admin: siempre) |
| POST   | `/api/trieds/{triedId}/forfeit-review` | Renuncia a la retroalimentación sin verla |
| GET    | `/api/trieds/{triedId}/breakdown`   | Aciertos por competencia, sin revelar respuestas |

### IA adaptativa
| Método | Ruta | Descripción |
|--------|------|-------------|
| GET    | `/api/ai-trieds/status`             | Estado del proveedor IA (público) |
| POST   | `/api/ai-trieds/start`              | Inicia práctica (genera la primera pregunta) |
| POST   | `/api/ai-trieds/{id}/next-question` | Genera la siguiente pregunta adaptativa |
| GET    | `/api/ai-trieds/my`                 | Historial de prácticas |
| GET    | `/api/ai-trieds/{id}/questions`     | Preguntas del intento |
| GET    | `/api/ai-trieds/{id}/review`        | Review con explicaciones (ADMIN) |
| POST   | `/api/ai-trieds/{id}/answer`        | Responder (evalúa servidor) |
| POST   | `/api/ai-trieds/{id}/hint`          | Pista (1-3) |
| PUT    | `/api/ai-trieds/{id}/finish`        | Finalizar |
| GET    | `/api/ai-trieds/admin/all` · `/student/{programId}/{studentId}` · `/{id}/admin-questions` | Consultas administrativas |
| GET    | `/api/ai-hint`                      | Pista IA para una pregunta del banco estático |

### Rankings
`GET /api/rankings` (`?active=`) · `GET /{rankingId}/leaderboard` · `POST/PUT/DELETE` y `/activate` (ADMIN)
Configurables por `periodType` (DAILY/WEEKLY/MONTHLY/GENERAL), `sourceFilter` (ALL/TRIEDS/AI_TRIEDS) y una lista opcional de competencias (vacío = general).

### Notificaciones (STUDENT/ADMIN)
`GET /api/notifications` · `GET /unread-count` · `PUT /read-all` · `PUT /{id}/read`

### Dudas
`POST /api/doubts` (STUDENT) · `GET /api/doubts?status=` (ADMIN) · `PUT /{id}/status` (ADMIN)

---

## Notificaciones

- **Nuevo test** → al crear un test, broadcast `@Async` a todos los estudiantes activos (in-app + email).
- **Duda reportada** → notifica in-app a todos los admins activos.
- **Plagio detectado** → notifica in-app y por correo a admins; correo al estudiante desactivado.
- **Apelación** → notifica al enviarla (admins) y al resolverla (estudiante).
- In-app persistidas en `notifications`; correos vía `EmailService` (plantillas HTML con la paleta institucional).

---

## Seguridad

- JWT (jjwt) con claims de rol y tipo de usuario; estudiantes desactivados pierden acceso inmediato en rutas protegidas (`JwtAuthenticationFilter`).
- Rate limiting con Bucket4j (`AuthRateLimitFilter`) sobre endpoints sensibles (`/api/auth/*`, apelaciones públicas): 10 solicitudes/minuto por IP.
- Errores de autenticación/autorización de Spring Security se devuelven en el mismo formato `ApiResponse` que el resto de la API (`RestAuthExceptionHandler`).
- `TokenCleanupJob` purga diariamente (3 a.m.) tokens de admin/estudiante expirados hace más de 7 días.

---

## Convenciones

- Datos en MAYÚSCULA en BD; normalización en `@PrePersist`/`@PreUpdate`.
- IDs con prefijo: `AMN` admin, `CPE` competence, `QTN` question, `TET` test, `TRD` tried, `SRE` student_response, `ATD` ai_tried, `ATE` ai_tried_response, `AQN` ai_question, `NOT` notificación, `DBT` duda, `APL` apelación, `RKG` ranking.
- Boolean → `CHAR(1)` Y/N (`BooleanToYNConverter`).
- `ddl-auto=none`: el esquema lo gestiona el repo de BD. Aplicar `migration.sql` antes de levantar con el módulo IA.
- Errores: `{ success:false, code:"NOMBRE_ENUM", message }`. Catálogo completo en `ErrorCode.java`.

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
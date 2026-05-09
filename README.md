# razona-pro-backend

Backend del proyecto Razona Pro. Spring Boot 4, Java 21, PostgreSQL.

## Cómo levantar

Necesitás las siguientes variables de entorno definidas:

```
DB_URL='x'
DB_USERNAME='x'
DB_PASSWORD='x'
```

El servidor queda en `http://localhost:8080`. Podés verificar con `GET /api/health`.

## Estructura de paquetes

```
com.razonapro.razonaprobackend/
├── controllers/   HTTP
├── services/      lógica de negocio
├── repositories/  JPA
├── models/        entidades
├── dtos/          request y response bodies
└── config/        security, beans, etc.
```

## Dependencias principales

- `spring-boot-starter-web`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-security`
- `postgresql`
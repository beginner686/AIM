# AI-Link Backend (SpringBoot 3)

## 1. Tech Stack

- Spring Boot 3
- Spring Security + JWT
- MyBatis-Plus
- MySQL 8
- Redis
- Knife4j / OpenAPI

## 2. Project Structure

```text
src/main/java/com/ailink
├── config
├── common
├── security
└── module
    ├── ai
    ├── demand
    ├── escrow
    ├── match
    ├── order
    ├── stat
    ├── user
    └── worker
```

## 3. Database Setup

1. Create schema and tables:

```sql
SOURCE sql/01_schema.sql;
```

2. Initialize fee config and admin account:

```sql
SOURCE sql/02_seed.sql;
```

## 4. Configuration

Edit `src/main/resources/application.yml`:

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`
- `spring.data.redis.host`
- `spring.data.redis.port`
- `jwt.secret`

## 5. Run

```bash
mvn -DskipTests package
mvn spring-boot:run
```

API doc:

- `http://localhost:8080/doc.html`

## 6. Core APIs

- Auth: `/api/auth/register`, `/api/auth/login`
- User: `/api/user/me`
- Demand: `/api/demand`
- Worker: `/api/worker/profile`, `/api/worker/list`
- Match: `/api/match/demand/{demandId}`
- Order: `/api/order`
- Escrow: `/api/escrow/order/{orderId}`
- Admin fee config: `/api/admin/config/fees`
- Admin risk control: `/api/admin/order/{orderId}/force-close`
- Admin stats: `/api/admin/stat/*`

## 7. Trading Flow

1. Employer publishes demand.
2. Worker submits and gets verified profile.
3. Employer creates order from demand + worker.
4. Employer pays escrow (`/api/order/{id}/escrow`).
5. Worker starts execution (`/api/order/{id}/start`).
6. Employer confirms completion (`/api/order/{id}/complete`).
7. Platform settles by stored `platform_fee` and `escrow_fee`.

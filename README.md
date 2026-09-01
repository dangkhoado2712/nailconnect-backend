# NailConnect Backend

Spring Boot API for the NailConnect V1 hiring marketplace. It provides a secure foundation for salon owners to publish nail technician jobs and for technicians to search and apply.

## Included in V1

- Email/password registration and login with BCrypt and JWT
- Technician, salon owner, and administrator roles
- PostgreSQL schema managed with Flyway
- PostGIS locations and radius-based job search (1–100 miles)
- Salon ownership checks for creating or updating listings
- Payment classifications: `W2`, `CONTRACT_1099`, and `CASH_W2`
- Draft, active, paused, closed, and expired job states
- Duplicate-safe job applications and application status management
- Input validation, stateless authorization, restricted CORS, and health checks
- Docker configuration and GitHub Actions CI

## Architecture

```text
NailConnect website/mobile app
          │ HTTPS + JWT
          ▼
Spring Boot REST API
          │ JDBC + Flyway
          ▼
PostgreSQL 16 + PostGIS
```

## Local setup

Requirements: Java 21, Maven 3.9+, and Docker.

```bash
cp .env.example .env
docker compose up -d
mvn spring-boot:run
```

The API runs at `http://localhost:8080`. Health check: `GET /actuator/health`.

## Main endpoints

| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/v1/auth/register` | Public |
| POST | `/api/v1/auth/login` | Public |
| GET | `/api/v1/jobs?lat=38.88&lng=-77.10&radiusMiles=25` | Public |
| GET | `/api/v1/jobs/{id}` | Public |
| POST | `/api/v1/jobs` | Salon owner |
| PATCH | `/api/v1/jobs/{id}/status` | Owning salon |
| POST | `/api/v1/applications` | Technician |
| GET | `/api/v1/applications/mine` | Technician |
| GET | `/api/v1/applications/job/{jobId}` | Owning salon |
| PATCH | `/api/v1/applications/{id}/status` | Owning salon |

Authenticated requests use `Authorization: Bearer <accessToken>`.

## Example job listing

```json
{
  "salonId": "22222222-2222-2222-2222-222222222222",
  "title": "Senior Nail Technician",
  "description": "Appointment-first studio with steady clientele.",
  "employmentType": "Full-time",
  "paymentType": "W2",
  "compensationMin": 24,
  "compensationMax": 32,
  "compensationUnit": "hour",
  "schedule": "Tue-Sat, 10 AM-7 PM",
  "minimumExperienceYears": 2,
  "licenseRequired": true,
  "skills": ["Gel X", "Builder Gel", "Nail Art"],
  "publish": true
}
```

## Production checklist

- Replace the development JWT secret with a generated secret of at least 32 bytes.
- Use managed PostgreSQL/PostGIS with TLS and private networking.
- Set `FRONTEND_ORIGIN` to the deployed NailConnect website.
- Remove or replace the development seed migration.
- Add email verification, password reset, rate limiting, file storage, and audit logging before public launch.
- Keep precise technician home locations private; store/display only the level of location required for matching.

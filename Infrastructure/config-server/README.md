# Spring Cloud Config Server

[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-Config%20Server-blue.svg?style=flat&logo=spring)](https://spring.io/projects/spring-cloud-config)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg?style=flat&logo=openjdk)](https://openjdk.org/)
[![Port](https://img.shields.io/badge/Port-8888-brightgreen.svg)]()

Centralized configuration microservice for the Enterprise E-Commerce Platform. It connects to the Git configuration repository on startup and serves environment-specific, hierarchical configuration properties to all platform microservices over HTTP.

---

## Key Features

- **Git-Backed Configuration:** Pulls configurations from [`Enterprise-E-Commerce-Platform-Config-Repo`](https://github.com/husseineelsaka/Enterprise-E-Commerce-Platform-Config-Repo) with clone-on-start enabled.
- **Hierarchical Inheritance:** Automatically merges common properties from `application.yml` into service-specific profiles (`product-service.yml`, `order-service.yml`, etc.).
- **Container Health Monitored:** Fully instrumented with `spring-boot-starter-actuator` responding at `/actuator/health` to orchestrate Docker Compose dependency boot sequences.
- **Configurable Repository URI:** Supports runtime override via `SPRING_CLOUD_CONFIG_SERVER_GIT_URI` and `SPRING_CLOUD_CONFIG_SERVER_GIT_DEFAULT_LABEL`.

---

## Quick Reference

| Attribute | Value |
| :--- | :--- |
| **Port** | `8888` |
| **Container Name** | `config-server` |
| **Health Endpoint** | `http://localhost:8888/actuator/health` |
| **Default Git Repo** | `https://github.com/husseineelsaka/Enterprise-E-Commerce-Platform-Config-Repo` |
| **Default Branch** | `main` |

---

## Running Locally

### Via Maven Wrapper
```powershell
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

### Via Docker Compose
```bash
docker compose up -d config-server
```

---

## Verifying Configurations

Query configuration endpoints directly via cURL or browser:

```bash
# Health check
curl http://localhost:8888/actuator/health

# Query product-service config
curl http://localhost:8888/product-service/default

# Query API gateway config
curl http://localhost:8888/api-gateway/default
```

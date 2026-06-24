# Enterprise E-Commerce Microservices Platform

Enterprise E-Commerce Microservices Platform


======================

A Spring Boot / Spring Cloud microservices architecture consisting of a centralizedconfiguration server, a service discovery server, and business services.
Architecture
------------

    .
    ├── platform/
    │   ├── config-server/        # Spring Cloud Config Server
    │   └── discovery-server/      # Eureka Discovery Server
    └── services/
        └── product-service/       # Product business service

### Components

| Component            | Location                    | Purpose                                                                              |
| -------------------- | --------------------------- | ------------------------------------------------------------------------------------ |
| **Config Server**    | `platform/config-server`    | Centralized externalized configuration, backed by a Git config repository            |
| **Discovery Server** | `platform/discovery-server` | Eureka service registry for service discovery                                        |
| **Product Service**  | `services/product-service`  | Business microservice; registers with Eureka and pulls config from the Config Server |

Startup Order
-------------

Services must start in this order, as each depends on the previous one being available:

1. **Config Server** — must be up first; other services fetch their configuration from it on boot

2. **Discovery Server** — registers itself once the Config Server is reachable; provides the service registry

3. **Product Service** (and any other business service) — registers with Eureka and resolves its config from the Config Server

4. Config Server

----------------

Spring Cloud Config Server that serves configuration to all other services from a remote Git repository.

**Location:** `platform/config-server`

### Configuration

Set the Git config repo URI in `application.yml` (or `application.properties`):
    spring:
      cloud:
        config:
          server:
            git:
              uri: <your-config-repo-url>
              default-label: main

### Run

    cd platform/config-server
    ./mvnw spring-boot:run

Runs on port: `8888`

### Verify

    curl http://localhost:8888/<application-name>/<profile>

2. Discovery Server (Eureka)

----------------------------

Netflix Eureka server used by all other services to register and discover each other.

**Location:** `platform/discovery-server`

### Configuration

`application.yml` should disable self-registration/fetching (since this _is_ the registry):
    eureka:
      client:
        register-with-eureka: false
        fetch-registry: false

### Run

    cd platform/discovery-server
    ./mvnw spring-boot:run

Runs on port: `8761`

### Verify

Open the Eureka dashboard: `http://localhost:8761`

3. Product Service

------------------

Business microservice that fetches its configuration from the Config Server and registers itself with Eureka.

**Location:** `services/product-service`

### Configuration

`bootstrap.yml` / `application.yml` points to the Config Server:
    spring:
      application:
        name: product-service
      cloud:
        config:
          uri: http://localhost:8888

    eureka:
      client:
        service-url:
          defaultZone: http://localhost:8761/eureka/

### Run

    cd services/product-service
    ./mvnw spring-boot:run

Runs on port: `8081`

### Verify

* Confirm registration in the Eureka dashboard (`http://localhost:8761`)
* Hit a sample endpoint, e.g. `http://localhost:8081/api/products`

Local Development — Quick Start
-------------------------------

    # 1. Config Server
    cd platform/config-server && ./mvnw spring-boot:run &
    
    # 2. Discovery Server
    cd platform/discovery-server && ./mvnw spring-boot:run &
    
    # 3. Product Service
    cd services/product-service && ./mvnw spring-boot:run &

Wait for each service to fully start before starting the next (or add health-check retriesin your startup script).
Requirements
------------

* Java 17+
* Maven (or the included `mvnw` wrapper)
* Access to the Git config repository used by the Config Server

Tech Stack
----------

* Spring Boot
* Spring Cloud Config (Server)
* Spring Cloud Netflix Eureka (Discovery Server / Client)

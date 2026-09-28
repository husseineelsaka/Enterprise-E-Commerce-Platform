# Phase 3 Reflections

### 1. One architectural decision from Sessions 1-23 you would defend confidently under questioning
**Using API Gateway for JWT Header Enrichment (Session 20):** Stripping the raw JWT at the perimeter and passing `X-User-Id` and `X-User-Roles` to downstream services keeps the internal network decoupled from the Identity Provider. The microservices do not need to fetch Keycloak's JWKS endpoint or parse tokens, drastically simplifying their testing and operational overhead.

### 2. One decision you are genuinely UNSURE was correct, and why
**Service Mesh (Istio) introduction (Session 21):** While mTLS and canary deployments are valuable, Istio introduces massive operational complexity (control plane, sidecars). Given the scale of this e-commerce platform, simpler Kubernetes NetworkPolicies and an Ingress controller like NGINX/Traefik could likely achieve 90% of the benefits with a fraction of the memory overhead and learning curve.

### 3. One thing you would do differently if starting the platform over today
**Implement Database Migrations (Flyway) from Session 1:** Manually managing schema structures in a distributed microservice system caused friction anytime a model changed. Bootstrapping Flyway or Liquibase from the start would have established a culture of version-controlled, reproducible schemas alongside code.

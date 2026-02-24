# Microservice Spring Learn

A hands-on project exploring microservices architecture using Spring Boot and Spring Cloud. The system was evolved from a monolithic quiz application into a distributed microservices architecture to demonstrate service decomposition, inter-service communication, and resilience patterns.

## Architecture Overview

```
Client
  │
  ▼
API Gateway (port 8765)          ← Single entry point, routing
  │
  ├──▶ Question Service (port 8080)   ← Manages questions, own DB
  │
  └──▶ Quiz Service (port 8090)       ← Manages quizzes, own DB
              │
              └──▶ Question Service   ← via OpenFeign (inter-service call)

Service Registry / Eureka (port 8761)  ← Service discovery for all services
```

## Services

| Service | Port | Responsibility |
|---|---|---|
| `service-registry` | 8761 | Eureka Server – service registration & discovery |
| `api-gateway` | 8765 | Spring Cloud Gateway – routing & single entry point |
| `question-service` | 8080 | Manages question bank, exposes question APIs |
| `quiz-service` | 8090 | Manages quizzes, fetches questions via OpenFeign |

## Tech Stack

- **Java** + **Spring Boot**
- **Spring Cloud Gateway** – API gateway and request routing
- **Netflix Eureka** – service registration and discovery
- **OpenFeign** – declarative inter-service HTTP communication
- **Resilience4j** – circuit breaker and retry for fault tolerance
- **Spring Data JPA** – data access layer
- **PostgreSQL** – each service has its own independent database
- **Gradle** – build tool

## Key Concepts Demonstrated

- **Service decomposition** – monolithic quiz app broken down into independent services with clear boundaries
- **Database per service** – `question-service` and `quiz-service` each own their own database, ensuring loose coupling
- **Service discovery** – services register with Eureka and communicate by service name, not hardcoded URLs
- **Declarative inter-service communication** – `quiz-service` calls `question-service` using a Feign client
- **Fault tolerance** – Resilience4j circuit breaker with fallback protects against cascading failures
- **API Gateway** – all client traffic goes through a single entry point with centralized routing

## Project Structure

```
microservice-spring-learn/
├── monolithic-app/
│   └── quiz-app/               # Original monolithic version
└── microservice-app/
    ├── service-registry/       # Eureka Server
    ├── api-gateway/            # Spring Cloud Gateway
    ├── question-service/       # Question management service
    └── quiz-service/           # Quiz management service
```

## Getting Started

### Prerequisites
- Java 25
- Gradle
- PostgreSQL

### Running locally

Start services **in this order**:

**1. Service Registry**
```bash
cd microservice-app/service-registry
./gradlew bootRun
# Eureka dashboard: http://localhost:8761
```

**2. Question Service**
```bash
cd microservice-app/question-service
./gradlew bootRun
# Runs on: http://localhost:8080
```

**3. Quiz Service**
```bash
cd microservice-app/quiz-service
./gradlew bootRun
# Runs on: http://localhost:8090
```

**4. API Gateway**
```bash
cd microservice-app/api-gateway
./gradlew bootRun
# All requests go through: http://localhost:8765
```

### Database Setup

Each service requires its own PostgreSQL database. Update the connection details in each service's `application.properties` before running:

```
microservice-app/question-service/src/main/resources/application.properties
microservice-app/quiz-service/src/main/resources/application.properties
```

## Evolution: Monolithic → Microservices

The `monolithic-app/quiz-app` contains the original single-application version of the system. Comparing the two versions illustrates the trade-offs and structural changes involved in migrating to microservices, including service boundary definition, data separation, and the introduction of inter-service communication.

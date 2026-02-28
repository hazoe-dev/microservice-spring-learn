# Microservice Spring Learn

A hands-on project exploring microservices architecture using Spring Boot and Spring Cloud.

The system was evolved from a monolithic quiz application into a distributed microservices architecture to demonstrate service decomposition, inter-service communication, and resilience patterns.

---

## 🧠 Overview

This project demonstrates a typical microservices stack using:

- **Netflix Eureka** – Service discovery and registry
- **Spring Cloud Gateway** – API gateway for routing
- **OpenFeign** – Declarative inter-service HTTP calls
- **Resilience4j** – Fault tolerance (retry, circuit breaker)
- **PostgreSQL** – Each service has its own database
- **Gradle** – Build tool
- **Docker & Docker Compose** – Containerized local deployment

---

## 🧩 Architecture

```text
Client ──▶ API Gateway  (8765)  ──▶ quiz-service  (8090)
                                   └──▶ question-service  (8080)
                  Eureka Server  (8761) (Registry)

Databases:
- quiz-service → quiz-db
- question-service → question-db
````

---

## 📦 Services

| Service          | Port | Description                                |
| ---------------- | ---- | ------------------------------------------ |
| service-registry | 8761 | Eureka Server – registry & discovery       |
| api-gateway      | 8765 | Spring Cloud Gateway – centralized routing |
| question-service | 8080 | Manages question data                      |
| quiz-service     | 8090 | Manages quizzes + calls question service   |

---

## 🚀 Getting Started

### Requirements

* Docker (v20+)
* Docker Compose (v2+)
* Java (only required for local non-Docker runs)

---

## 🐳 Run with Docker Compose

This will start all services and required PostgreSQL databases.

From the project root:

```bash
docker compose up --build
```

Once all containers start:

| Feature     | URL                                            |
| ----------- | ---------------------------------------------- |
| Eureka UI   | [http://localhost:8761](http://localhost:8761) |
| API Gateway | [http://localhost:8765](http://localhost:8765) |

🔹 All services will register themselves with Eureka automatically.

---

## 🛠 Run Locally Without Docker

If you want to run individual services locally:

1️⃣ Start PostgreSQL locally  
2️⃣ Update `application.properties` for each service with correct DB settings  
3️⃣ Start services in this order:  

```bash
# 1. Registry
cd microservice-app/service-registry
./gradlew bootRun

# 2. Question service
cd ../question-service
./gradlew bootRun

# 3. Quiz service
cd ../quiz-service
./gradlew bootRun

# 4. API Gateway
cd ../api-gateway
./gradlew bootRun
```

Access:

* [http://localhost:8761](http://localhost:8761) (Eureka)
* [http://localhost:8765](http://localhost:8765) (Gateway)

---

## 📁 Project Structure

```
microservice-spring-learn/
├── monolithic-app/             // Original monolithic quiz app
└── microservice-app/
    ├── service-registry/       // Eureka Server
    ├── api-gateway/            // Spring Cloud Gateway
    ├── question-service/       // Question microservice
    └── quiz-service/           // Quiz microservice
```

---

## 🧠 Key Concepts Demonstrated

* **Service Decomposition** – Monolith split into independent services
* **Database per Service** – Loose coupling between services/data
* **Service Discovery** – Using Eureka to register and locate services
* **Resilience** – Resilience4j retry + circuit breaker
* **API Gateway** – Central entry point for all client traffic

# System Architecture

## Component Diagram

```mermaid
flowchart TB
    Client(["Client / Browser"])

    subgraph NET["micro-net · Docker Network"]
        subgraph GW["API Gateway  ‹component›  :8765"]
            GW_Core["Spring Cloud Gateway"]
        end

        subgraph REG["Service Registry  ‹component›  :8761"]
            Eureka["Eureka Server"]
        end

        subgraph QUIZ["Quiz Service  ‹component›  :8090"]
            QuizCtrl["«component»<br/>QuizController"]
            QuizSvc["«component»<br/>QuizService"]
            QClient["«component»<br/>QuestionClient<br/>OpenFeign + Resilience4j<br/>──────────────────<br/>Circuit Breaker 50% / 20-window<br/>Retry  max 2 · 500 ms exp. backoff<br/>Time Limiter  2 s<br/>Bulkhead  20 concurrent"]
            QuizRepo["«component»<br/>QuizRepository  JPA"]
        end

        subgraph QSVC["Question Service  ‹component›  :8080"]
            QCtrl["«component»<br/>QuestionController"]
            QSvc["«component»<br/>QuestionService"]
            QRepo["«component»<br/>QuestionRepository  JPA"]
        end

        QuizDB[("quiz-db<br/>PostgreSQL :5432")]
        QuestionDB[("question-db<br/>PostgreSQL :5432")]
    end

    %% Client → Gateway
    Client -->|HTTP| GW_Core

    %% Gateway → Services  (load-balanced via Eureka)
    GW_Core -->|"lb://QUIZ-SERVICE"| QuizCtrl
    GW_Core -->|"lb://QUESTION-SERVICE"| QCtrl

    %% Service registration
    GW_Core -.->|register / heartbeat| Eureka
    QuizSvc -.->|register / heartbeat| Eureka
    QSvc    -.->|register / heartbeat| Eureka

    %% Quiz Service internals
    QuizCtrl --> QuizSvc
    QuizSvc  --> QClient
    QuizSvc  --> QuizRepo

    %% Quiz → Question  inter-service call
    QClient -->|"HTTP/REST"| QCtrl

    %% Question Service internals
    QCtrl --> QSvc
    QSvc  --> QRepo

    %% DB connections
    QuizRepo -->|JDBC| QuizDB
    QRepo    -->|JDBC| QuestionDB
```

---

## Services

| Service | Port | Technology | Description |
|---|---|---|---|
| Service Registry | 8761 | Spring Cloud Netflix Eureka | Service discovery and registration |
| API Gateway | 8765 | Spring Cloud Gateway | Unified entry point, routing, load balancing |
| Question Service | 8080 | Spring Boot + JPA | Manages questions CRUD, random selection, answer validation |
| Quiz Service | 8090 | Spring Boot + JPA + OpenFeign | Manages quizzes, orchestrates question fetching |

## Infrastructure

| Component | Technology | Details |
|---|---|---|
| Service Discovery | Eureka Server | All services self-register and resolve each other by name |
| Load Balancing | Spring Cloud LoadBalancer | Client-side load balancing via Eureka registry |
| Inter-service HTTP | OpenFeign + RestTemplate | Quiz Service calls Question Service |
| Fault Tolerance | Resilience4j | Circuit Breaker, Retry, Time Limiter, Bulkhead |
| Databases | PostgreSQL 16 | One dedicated database instance per service |
| Containerization | Docker + Docker Compose | All services run in `micro-net` bridge network |
| CI/CD | GitHub Actions | CI on PR to main; CD deploys to AWS ECS Fargate on version tag |

---

## Class Diagrams

### Question Service

```mermaid
classDiagram
    class Question {
        +Long id
        +String title
        +List~String~ options
        +Integer correctOptionIndex
        +QuestionLevel level
        +String category
        +of(title, answerIndex, level, category, options) Question$
    }

    class QuestionLevel {
        <<enumeration>>
        EASY
        MEDIUM
        HARD
        +from(value: String) QuestionLevel$
    }

    class CreatedQuestionRequest {
        <<record>>
        +String title
        +List~String~ options
        +Integer selectedOption
        +QuestionLevel level
        +String category
    }

    class RandomQuestionRequest {
        <<record>>
        +String category
        +int size
    }

    class ValidateAnswersRequest {
        <<record>>
        +List~AnswerRequest~ answers
    }

    class AnswerRequest {
        <<record>>
        +Long questionId
        +Integer selectedOptionIndex
    }

    class QuestionSummaryResponse {
        <<record>>
        +Long id
        +String title
        +List~String~ options
        +String level
    }

    class ValidateAnswersResponse {
        <<record>>
        +int total
        +int correct
        +Map~Long, Boolean~ detail
    }

    class QuestionRepo {
        <<interface>>
        +findByCategoryIgnoreCase(category, pageable) Page~Question~
        +deleteByIdReturningCount(id) int
        +deleteAllByTitleIgnoreCase(title) int
        +countByCategoryIgnoreCase(category) long
    }

    class QuestionService {
        -QuestionRepo questionRepo
        +getAllQuestions(pageable) Page~Question~
        +getQuestionsByCategory(category, pageable) Page~Question~
        +addQuestion(request) Question
        +getQuestionById(id) Question
        +deleteQuestionById(id) void
        +deleteQuestionsByTitle(title) int
        +getRandomQuestions(category, size) List~QuestionSummaryResponse~
        +getQuestionsByIds(ids) List~QuestionSummaryResponse~
        +validateAnswers(request) ValidateAnswersResponse
    }

    class QuestionController {
        -QuestionService questionService
        +getAll(category, pageable) ResponseEntity
        +create(request) ResponseEntity
        +getById(id) ResponseEntity
        +deleteById(id) ResponseEntity
        +deleteByTitle(title) ResponseEntity
        +getRandom(request) ResponseEntity
        +getByIds(ids) ResponseEntity
        +validate(request) ResponseEntity
    }

    Question --> QuestionLevel
    ValidateAnswersRequest --> AnswerRequest
    QuestionRepo --> Question : manages
    QuestionService --> QuestionRepo
    QuestionService --> QuestionSummaryResponse : returns
    QuestionService --> ValidateAnswersResponse : returns
    QuestionController --> QuestionService
    QuestionController ..> CreatedQuestionRequest : receives
    QuestionController ..> RandomQuestionRequest : receives
    QuestionController ..> ValidateAnswersRequest : receives
```

### Quiz Service

```mermaid
classDiagram
    class Quiz {
        +Long id
        +String title
        +List~Long~ questionIds
    }

    class CreatedQuizRequest {
        <<record>>
        +String title
        +int numOfQuestion
        +String category
    }

    class ValidateAnswersRequest {
        <<record>>
        +List~AnswerRequest~ answers
    }

    class AnswerRequest {
        <<record>>
        +Long questionId
        +Integer selectedOptionIndex
    }

    class QuestionSummaryResponse {
        <<record>>
        +Long id
        +String title
        +List~String~ options
        +String level
    }

    class QuizQuestionResponse {
        <<record>>
        +Long id
        +String title
        +List~String~ options
        +String level
    }

    class QuizResultResponse {
        <<record>>
        +Long quizId
        +int totalQuestions
        +int correctAnswers
        +double score
    }

    class ValidateAnswersResponse {
        <<record>>
        +int total
        +int correct
        +Map~Long, Boolean~ detail
    }

    class QuizRepo {
        <<interface>>
    }

    class QuestionClient {
        <<interface>>
        +getRandomQuestions(request) List~QuestionSummaryResponse~
        +getQuestionsByIds(ids) List~QuestionSummaryResponse~
        +validateAnswers(request) ValidateAnswersResponse
    }

    class QuestionAsyncService {
        -QuestionClient questionClient
        -Executor quizExecutor
        +getQuestionsAsync(questionIds) CompletableFuture~List~
        +getQuestionsFallback(questionIds, ex) CompletableFuture~List~
    }

    class QuizService {
        -QuizRepo quizRepo
        -QuestionClient questionClient
        -RestTemplate restTemplate
        -QuestionAsyncService questionAsyncService
        +createQuiz(request) Quiz
        +getQuizById(id) Quiz
        +getQuestionsByQuizId(quizId) CompletableFuture~List~
        +submitQuiz(quizId, request) QuizResultResponse
    }

    class QuizController {
        -QuizService quizService
        +getById(id) ResponseEntity
        +create(request) ResponseEntity
        +getQuestions(id) CompletableFuture~ResponseEntity~
        +submit(id, request) ResponseEntity
    }

    ValidateAnswersRequest --> AnswerRequest
    QuizRepo --> Quiz : manages
    QuestionAsyncService --> QuestionClient
    QuizService --> QuizRepo
    QuizService --> QuestionClient
    QuizService --> QuestionAsyncService
    QuizService --> QuizResultResponse : returns
    QuizController --> QuizService
    QuizController ..> CreatedQuizRequest : receives
    QuizController ..> ValidateAnswersRequest : receives
    QuestionClient ..> QuestionSummaryResponse : returns
    QuestionClient ..> ValidateAnswersResponse : returns
```

---

## Sequence Diagrams

### POST /api/quizzes — Create Quiz

```mermaid
sequenceDiagram
    actor Client
    participant GW as API Gateway
    participant QZ as QuizController
    participant QZS as QuizService
    participant QC as QuestionClient
    participant QSC as QuestionController
    participant QSVC as QuestionService
    participant QuizDB as quiz-db
    participant QuestionDB as question-db

    Client->>GW: POST /api/quizzes<br/>{title, numOfQuestion, category}
    GW->>QZ: route lb://QUIZ-SERVICE
    QZ->>QZS: createQuiz(CreatedQuizRequest)
    QZS->>QC: getRandomQuestions({category, size})
    QC->>QSC: POST /api/questions/random
    QSC->>QSVC: getRandomQuestions(category, size)
    QSVC->>QuestionDB: SELECT random N questions by category
    QuestionDB-->>QSVC: List~Question~
    QSVC-->>QSC: List~QuestionSummaryResponse~
    QSC-->>QC: 200 List~QuestionSummaryResponse~
    QC-->>QZS: List~QuestionSummaryResponse~
    QZS->>QuizDB: INSERT quiz + questionIds
    QuizDB-->>QZS: saved Quiz
    QZS-->>QZ: Quiz
    QZ-->>GW: 201 Created (Location: /api/quizzes/{id})
    GW-->>Client: 201 Created
```

### GET /api/quizzes/{id}/questions — Get Quiz Questions

```mermaid
sequenceDiagram
    actor Client
    participant GW as API Gateway
    participant QZ as QuizController
    participant QZS as QuizService
    participant QZAS as QuestionAsyncService
    participant QC as QuestionClient
    participant QSC as QuestionController
    participant QuizDB as quiz-db
    participant QuestionDB as question-db

    Client->>GW: GET /api/quizzes/{id}/questions
    GW->>QZ: route lb://QUIZ-SERVICE
    QZ->>QZS: getQuestionsByQuizId(id)
    QZS->>QuizDB: SELECT questionIds WHERE quiz.id = {id}
    QuizDB-->>QZS: List~Long~ questionIds
    QZS->>QZAS: getQuestionsAsync(questionIds)
    Note over QZAS: @Retry + @CircuitBreaker<br/>@TimeLimiter (2s)
    QZAS->>QC: getQuestionsByIds(ids)
    QC->>QSC: GET /api/questions/by-ids?ids=...
    QSC->>QuestionDB: SELECT questions by ids
    QuestionDB-->>QSC: List~Question~
    QSC-->>QC: 200 List~QuestionSummaryResponse~
    QC-->>QZAS: List~QuestionSummaryResponse~
    QZAS-->>QZS: CompletableFuture resolved
    QZS-->>QZ: List~QuizQuestionResponse~
    QZ-->>GW: 200 OK
    GW-->>Client: 200 List~QuizQuestionResponse~
```

### POST /api/quizzes/{id}/submit — Submit Quiz

```mermaid
sequenceDiagram
    actor Client
    participant GW as API Gateway
    participant QZ as QuizController
    participant QZS as QuizService
    participant QC as QuestionClient
    participant QSC as QuestionController
    participant QSVC as QuestionService
    participant QuizDB as quiz-db
    participant QuestionDB as question-db

    Client->>GW: POST /api/quizzes/{id}/submit<br/>{answers: [{questionId, selectedOptionIndex}]}
    GW->>QZ: route lb://QUIZ-SERVICE
    QZ->>QZS: submitQuiz(id, ValidateAnswersRequest)
    QZS->>QuizDB: SELECT quiz WHERE id = {id}
    QuizDB-->>QZS: Quiz
    QZS->>QC: validateAnswers(ValidateAnswersRequest)
    QC->>QSC: POST /api/questions/validate
    QSC->>QSVC: validateAnswers(request)
    QSVC->>QuestionDB: SELECT correctOptionIndex WHERE id IN (...)
    QuestionDB-->>QSVC: List~Question~
    QSVC-->>QSC: ValidateAnswersResponse {total, correct, detail}
    QSC-->>QC: 200 ValidateAnswersResponse
    QC-->>QZS: ValidateAnswersResponse
    QZS-->>QZ: QuizResultResponse {quizId, totalQuestions, correctAnswers, score}
    QZ-->>GW: 200 OK
    GW-->>Client: 200 QuizResultResponse
```

### POST /api/questions — Create Question

```mermaid
sequenceDiagram
    actor Client
    participant GW as API Gateway
    participant QC as QuestionController
    participant QSVC as QuestionService
    participant QuestionDB as question-db

    Client->>GW: POST /api/questions<br/>{title, options, selectedOption, level, category}
    GW->>QC: route lb://QUESTION-SERVICE
    QC->>QSVC: addQuestion(CreatedQuestionRequest)
    QSVC->>QuestionDB: INSERT question
    QuestionDB-->>QSVC: saved Question
    QSVC-->>QC: Question
    QC-->>GW: 201 Created
    GW-->>Client: 201 Created
```

### GET /api/quizzes/{id} — Get Quiz by ID (with Resilience4j fallback)

```mermaid
sequenceDiagram
    actor Client
    participant GW as API Gateway
    participant QZ as QuizController
    participant QZS as QuizService
    participant QuizDB as quiz-db

    Client->>GW: GET /api/quizzes/{id}
    GW->>QZ: route lb://QUIZ-SERVICE
    QZ->>QZS: getQuizById(id)
    QZS->>QuizDB: SELECT quiz WHERE id = {id}
    alt found
        QuizDB-->>QZS: Quiz
        QZS-->>QZ: Quiz
        QZ-->>GW: 200 OK
        GW-->>Client: 200 Quiz {id, title, questionIds}
    else not found
        QuizDB-->>QZS: empty
        QZS-->>QZ: throws NotFoundException
        QZ-->>GW: 404 Not Found
        GW-->>Client: 404 Not Found
    end
```

---

## Deployment Diagrams

### Local — Docker Compose

```mermaid
flowchart TB
    subgraph DEV["«device» Developer Machine"]
        subgraph C1["«container» eureka-server"]
            A1["«artifact» hazoe-dev/service-registry :8761"]
        end
        subgraph C2["«container» api-gateway"]
            A2["«artifact» hazoe-dev/api-gateway :8765"]
        end
        subgraph C3["«container» question-service"]
            A3["«artifact» hazoe-dev/question-service :8080"]
        end
        subgraph C4["«container» quiz-service"]
            A4["«artifact» hazoe-dev/quiz-service :8090"]
        end
        subgraph C5["«container» postgres-question"]
            A5["«artifact» postgres:16 · :5432 · volume: pgdata-question"]
        end
        subgraph C6["«container» postgres-quiz"]
            A6["«artifact» postgres:16 · :5432 · volume: pgdata-quiz"]
        end
    end

    A2 -->|"register :8761"| A1
    A3 -->|"register :8761"| A1
    A4 -->|"register :8761"| A1
    A2 -->|"route :8080"| A3
    A2 -->|"route :8090"| A4
    A4 -->|"HTTP/REST :8080"| A3
    A3 -->|"JDBC :5432"| A5
    A4 -->|"JDBC :5432"| A6
```

### Production — AWS ECS Fargate

```mermaid
flowchart TB
    Browser(["«actor»<br/>Client / Browser"])

    subgraph AWS["«execution environment»<br/>AWS Cloud"]
        subgraph ECS["«execution environment»<br/>AWS ECS Cluster · Fargate"]
            subgraph T1["«artifact»<br/>Task: service-registry"]
                S1["hazoe-dev/service-registry<br/>:8761"]
            end
            subgraph T2["«artifact»<br/>Task: api-gateway"]
                S2["hazoe-dev/api-gateway<br/>:8765"]
            end
            subgraph T3["«artifact»<br/>Task: question-service"]
                S3["hazoe-dev/question-service<br/>:8080"]
            end
            subgraph T4["«artifact»<br/>Task: quiz-service"]
                S4["hazoe-dev/quiz-service<br/>:8090"]
            end
        end

        subgraph RDS1["«device»<br/>Amazon RDS"]
            DB1[("question-db<br/>PostgreSQL :5432")]
        end
        subgraph RDS2["«device»<br/>Amazon RDS"]
            DB2[("quiz-db<br/>PostgreSQL :5432")]
        end

        subgraph CICD["«execution environment»<br/>GitHub Actions"]
            W1["CI — test on PR to main"]
            W2["CD — build & push on tag v*.*.*<br/>then deploy to ECS"]
        end

        subgraph REG["«device»<br/>Docker Hub"]
            IMG["hazoe-dev/*<br/>container images"]
        end
    end

    Browser -->|HTTPS| S2
    S2 -->|"register :8761"| S1
    S3 -->|"register :8761"| S1
    S4 -->|"register :8761"| S1
    S2 -->|"route lb://QUESTION-SERVICE"| S3
    S2 -->|"route lb://QUIZ-SERVICE"| S4
    S4 -->|"HTTP/REST"| S3
    S3 -->|JDBC| DB1
    S4 -->|JDBC| DB2
    W2 -->|"docker push"| IMG
    W2 -->|"ecs update-service"| ECS
    IMG -.->|"docker pull"| ECS
```

---

## Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.0.2 |
| Build | Gradle |
| Service Discovery | Spring Cloud Netflix Eureka |
| API Gateway | Spring Cloud Gateway (WebMVC) |
| Service Client | OpenFeign, RestTemplate (LoadBalanced) |
| Fault Tolerance | Resilience4j |
| ORM | JPA / Hibernate |
| Connection Pool | HikariCP (max 30, min 10 idle) |
| Database | PostgreSQL 16 |
| Async | CompletableFuture, custom 30-thread Executor |
| Container | Docker |
| Orchestration (local) | Docker Compose |
| Orchestration (prod) | AWS ECS Fargate |
| CI/CD | GitHub Actions |

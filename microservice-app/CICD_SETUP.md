# CI/CD Setup Guide — Microservice Spring Learn

**Stack:** GitHub Actions → Docker Hub → AWS ECS Fargate  
**Trigger:** CI on PR to `main` · CD on git tag `v*.*.*`

---

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Phase 1 — AWS Infrastructure Setup](#phase-1--aws-infrastructure-setup)
4. [Phase 2 — Configure GitHub Secrets](#phase-2--configure-github-secrets)
5. [Phase 3 — Update Task Definition Placeholders](#phase-3--update-task-definition-placeholders)
6. [Phase 4 — First Deployment](#phase-4--first-deployment)
7. [Day-to-Day Workflow](#day-to-day-workflow)
8. [Verification Checklist](#verification-checklist)
9. [Test API After Deployment](#test-api-after-deployment)
10. [Troubleshooting](#troubleshooting)
11. [Teardown — Remove All Resources](#teardown--remove-all-resources)
12. [Architecture Diagram](#architecture-diagram)

---

## Overview

```
Developer pushes code
        │
        ├─── Opens PR to main ──────────► CI Workflow runs
        │                                  └─ Tests all 4 services (parallel)
        │
        └─── Pushes tag v1.0.0 ─────────► CD Workflow runs
                                           ├─ Build Docker images (x4, parallel)
                                           ├─ Push to Docker Hub
                                           └─ Deploy to AWS ECS Fargate
                                                ├─ service-registry (first)
                                                └─ api-gateway, question-service,
                                                   quiz-service (parallel after)
```

**Services and ports:**

| Service | Port | Docker Hub Image |
|---------|------|-----------------|
| service-registry (Eureka) | 8761 | `hazoe-dev/service-registry` |
| api-gateway | 8765 | `hazoe-dev/api-gateway` |
| question-service | 8080 | `hazoe-dev/question-service` |
| quiz-service | 8090 | `hazoe-dev/quiz-service` |

---

## Prerequisites

Before starting, make sure you have:

- [ ] An [AWS account](https://aws.amazon.com)
- [ ] AWS CLI installed and configured (`aws configure`)
- [ ] A [Docker Hub account](https://hub.docker.com) — create 4 public repositories:
  - `hazoe-dev/service-registry`
  - `hazoe-dev/api-gateway`
  - `hazoe-dev/question-service`
  - `hazoe-dev/quiz-service`
- [ ] A Docker Hub **Access Token** (not your password):
  - Docker Hub → Account Settings → Security → New Access Token
- [ ] The GitHub repo: `hazoe-dev/microservice-spring-learn`

---

## Phase 1 — AWS Infrastructure Setup

> Do this once. These resources stay running; you pay for them.

### 1.1 Choose a Region

Pick a region close to you. All resources below must be in the **same region**.

```bash
export AWS_REGION=ap-southeast-1   # example: Singapore
export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
echo "Account: $AWS_ACCOUNT_ID  Region: $AWS_REGION"
```

---

### 1.2 Create a VPC and Subnets

> Skip this step if you want to use the default VPC (easier for learning).

Using the **default VPC** is fine for a learning project. Note its ID:

```bash
export VPC_ID=$(aws ec2 describe-vpcs \
  --filters "Name=isDefault,Values=true" \
  --query "Vpcs[0].VpcId" --output text)

export SUBNET_IDS=$(aws ec2 describe-subnets \
  --filters "Name=vpc-id,Values=$VPC_ID" \
  --query "Subnets[*].SubnetId" --output text | tr '\t' ',')

echo "VPC: $VPC_ID"
echo "Subnets: $SUBNET_IDS"
```

Save these values — you'll need them when creating ECS services.

---

### 1.3 Create Security Groups

**Security group for ECS tasks** (allows inbound traffic between services):

```bash
export SG_ECS=$(aws ec2 create-security-group \
  --group-name microservice-ecs-sg \
  --description "Security group for ECS tasks" \
  --vpc-id $VPC_ID \
  --query GroupId --output text)

# Allow all traffic within the same security group (inter-service)
aws ec2 authorize-security-group-ingress \
  --group-id $SG_ECS \
  --protocol all \
  --source-group $SG_ECS

echo "ECS Security Group: $SG_ECS"
```

**Security group for the public ALB** (api-gateway exposed to internet):

```bash
export SG_ALB=$(aws ec2 create-security-group \
  --group-name microservice-alb-sg \
  --description "Security group for public ALB" \
  --vpc-id $VPC_ID \
  --query GroupId --output text)

# Allow HTTPS from internet
aws ec2 authorize-security-group-ingress \
  --group-id $SG_ALB --protocol tcp --port 443 --cidr 0.0.0.0/0

# Allow HTTP (for testing; remove in production)
aws ec2 authorize-security-group-ingress \
  --group-id $SG_ALB --protocol tcp --port 80 --cidr 0.0.0.0/0

# Allow ECS tasks to receive traffic from the ALB
aws ec2 authorize-security-group-ingress \
  --group-id $SG_ECS --protocol tcp --port 8765 --source-group $SG_ALB

echo "ALB Security Group: $SG_ALB"
```

---

### 1.4 Create RDS PostgreSQL Instances

**Security group for RDS** (only ECS tasks can connect):

```bash
export SG_RDS=$(aws ec2 create-security-group \
  --group-name microservice-rds-sg \
  --description "Security group for RDS" \
  --vpc-id $VPC_ID \
  --query GroupId --output text)

aws ec2 authorize-security-group-ingress \
  --group-id $SG_RDS --protocol tcp --port 5432 --source-group $SG_ECS

echo "RDS Security Group: $SG_RDS"
```

**Create question-db:**

```bash
aws rds create-db-instance \
  --db-instance-identifier question-db \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --engine-version 16 \
  --master-username question_user \
  --master-user-password "CHANGE_ME_QUESTION_DB_PASS" \
  --db-name questiondb \
  --allocated-storage 20 \
  --vpc-security-group-ids $SG_RDS \
  --no-multi-az \
  --no-publicly-accessible
```

**Create quiz-db:**

```bash
aws rds create-db-instance \
  --db-instance-identifier quiz-db \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --engine-version 16 \
  --master-username quiz_user \
  --master-user-password "CHANGE_ME_QUIZ_DB_PASS" \
  --db-name quizdb \
  --allocated-storage 20 \
  --vpc-security-group-ids $SG_RDS \
  --no-multi-az \
  --no-publicly-accessible
```

> RDS creation takes ~5 minutes. Check status:
> ```bash
> aws rds describe-db-instances --db-instance-identifier question-db \
>   --query "DBInstances[0].DBInstanceStatus"
> aws rds describe-db-instances --db-instance-identifier quiz-db \
>   --query "DBInstances[0].DBInstanceStatus"
> ```

Once status is `available`, get the endpoints:

```bash
export QUESTION_DB_HOST=$(aws rds describe-db-instances \
  --db-instance-identifier question-db \
  --query "DBInstances[0].Endpoint.Address" --output text)

export QUIZ_DB_HOST=$(aws rds describe-db-instances \
  --db-instance-identifier quiz-db \
  --query "DBInstances[0].Endpoint.Address" --output text)

echo "question-db: $QUESTION_DB_HOST"
echo "quiz-db:     $QUIZ_DB_HOST"
```

---

### 1.5 Store Secrets in AWS Secrets Manager

**Docker Hub credentials** (needed so ECS can pull your private images):

```bash
aws secretsmanager create-secret \
  --name dockerhub-creds \
  --description "Docker Hub pull credentials for ECS" \
  --secret-string '{"username":"YOUR_DOCKERHUB_USERNAME","password":"YOUR_DOCKERHUB_TOKEN"}'
```

**Database passwords:**

```bash
aws secretsmanager create-secret \
  --name question-db-password \
  --secret-string "CHANGE_ME_QUESTION_DB_PASS"

aws secretsmanager create-secret \
  --name quiz-db-password \
  --secret-string "CHANGE_ME_QUIZ_DB_PASS"
```

---

### 1.6 Create IAM Role for ECS Task Execution

```bash
# Create the role
aws iam create-role \
  --role-name ecsTaskExecutionRole \
  --assume-role-policy-document '{
    "Version": "2012-10-17",
    "Statement": [{
      "Effect": "Allow",
      "Principal": { "Service": "ecs-tasks.amazonaws.com" },
      "Action": "sts:AssumeRole"
    }]
  }'

# Attach managed ECS policy
aws iam attach-role-policy \
  --role-name ecsTaskExecutionRole \
  --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy

# Allow reading secrets (for DB passwords and Docker Hub creds)
aws iam attach-role-policy \
  --role-name ecsTaskExecutionRole \
  --policy-arn arn:aws:iam::aws:policy/SecretsManagerReadWrite
```

---

### 1.7 Create CloudWatch Log Groups

```bash
for service in service-registry api-gateway question-service quiz-service; do
  aws logs create-log-group --log-group-name /ecs/$service
  echo "Created log group: /ecs/$service"
done
```
> In case you meet parsing path:
> ```bash
> export MSYS_NO_PATHCONV=1
> ```
---

### 1.8 Create the ECS Cluster

```bash
aws ecs create-cluster \
  --cluster-name microservice-cluster \
  --capacity-providers FARGATE \
  --default-capacity-provider-strategy capacityProvider=FARGATE,weight=1

echo "ECS cluster created: microservice-cluster"
```

---

### 1.9 Create an Internal ALB for Eureka

Eureka (service-registry) needs a fixed hostname so api-gateway, quiz-service, and question-service can register with it.

```bash
# Check again or create enviroment variables for ALBs
export MSYS_NO_PATHCONV=1
export VPC_ID=$(aws ec2 describe-vpcs --filters "Name=isDefault,Values=true" --query "Vpcs[0].VpcId" --output text)
export SUBNET_IDS=$(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$VPC_ID" --query "Subnets[*].SubnetId" --output text | tr '\t' ',')
export SG_ECS=$(aws ec2 describe-security-groups --filters "Name=group-name,Values=microservice-ecs-sg" --query "SecurityGroups[0].GroupId" --output text)
export SG_ALB=$(aws ec2 describe-security-groups --filters "Name=group-name,Values=microservice-alb-sg" --query "SecurityGroups[0].GroupId" --output text)
```

```bash
# Create internal ALB
export EUREKA_ALB_ARN=$(aws elbv2 create-load-balancer \
  --name eureka-internal-alb \
  --scheme internal \
  --type application \
  --subnets $(echo $SUBNET_IDS | tr ',' ' ') \
  --security-groups $SG_ECS \
  --query "LoadBalancers[0].LoadBalancerArn" --output text)

export EUREKA_ALB_DNS=$(aws elbv2 describe-load-balancers \
  --load-balancer-arns $EUREKA_ALB_ARN \
  --query "LoadBalancers[0].DNSName" --output text)

echo "Eureka ALB DNS: $EUREKA_ALB_DNS"

# Create target group for Eureka
export EUREKA_TG_ARN=$(aws elbv2 create-target-group \
  --name eureka-tg \
  --protocol HTTP \
  --port 8761 \
  --vpc-id $VPC_ID \
  --target-type ip \
  --health-check-path / \
  --query "TargetGroups[0].TargetGroupArn" --output text)

# Get old Eureka TG ARN
export EUREKA_TG_ARN=$(aws elbv2 describe-target-groups \
  --names eureka-tg \
  --query "TargetGroups[0].TargetGroupArn" --output text)
echo "Eureka TG: $EUREKA_TG_ARN"

# Add listener
aws elbv2 create-listener \
  --load-balancer-arn $EUREKA_ALB_ARN \
  --protocol HTTP \
  --port 8761 \
  --default-actions Type=forward,TargetGroupArn=$EUREKA_TG_ARN
```

---

### 1.10 Create a Public ALB for the API Gateway

```bash
# Create public ALB
export GW_ALB_ARN=$(aws elbv2 create-load-balancer \
  --name api-gateway-alb \
  --scheme internet-facing \
  --type application \
  --subnets $(echo $SUBNET_IDS | tr ',' ' ') \
  --security-groups $SG_ALB \
  --query "LoadBalancers[0].LoadBalancerArn" --output text)

export GW_ALB_DNS=$(aws elbv2 describe-load-balancers \
  --load-balancer-arns $GW_ALB_ARN \
  --query "LoadBalancers[0].DNSName" --output text)

echo "API Gateway public URL: http://$GW_ALB_DNS"

# Create target group
export GW_TG_ARN=$(aws elbv2 create-target-group \
  --name api-gateway-tg \
  --protocol HTTP \
  --port 8765 \
  --vpc-id $VPC_ID \
  --target-type ip \
  --health-check-path /actuator \
  --query "TargetGroups[0].TargetGroupArn" --output text)

# Get existed API Gateway TG ARN  
export GW_TG_ARN=$(aws elbv2 describe-target-groups \
  --names api-gateway-tg \
  --query "TargetGroups[0].TargetGroupArn" --output text)

echo "GW TG: $GW_TG_ARN"

# Add listener
aws elbv2 create-listener \
  --load-balancer-arn $GW_ALB_ARN \
  --protocol HTTP \
  --port 80 \
  --default-actions Type=forward,TargetGroupArn=$GW_TG_ARN
```

---

### 1.11 Register Initial ECS Task Definitions

Before ECS services can be created, the task definitions need to exist. First, update the placeholder values in your JSON files (see [Phase 3](#phase-3--update-task-definition-placeholders)), then register them:

```bash
REPO_ROOT="path/to/microservice-learn"

aws ecs register-task-definition \
  --cli-input-json "file://$REPO_ROOT/.github/ecs/service-registry-task-def.json"

aws ecs register-task-definition \
  --cli-input-json "file://$REPO_ROOT/.github/ecs/api-gateway-task-def.json"

aws ecs register-task-definition \
  --cli-input-json "file://$REPO_ROOT/.github/ecs/question-service-task-def.json"

aws ecs register-task-definition \
  --cli-input-json "file://$REPO_ROOT/.github/ecs/quiz-service-task-def.json"
```

---

### 1.12 Create ECS Services

**service-registry** (attached to its internal ALB):

```bash
aws ecs create-service \
  --cluster microservice-cluster \
  --service-name service-registry \
  --task-definition service-registry \
  --desired-count 1 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[$SUBNET_IDS],securityGroups=[$SG_ECS],assignPublicIp=ENABLED}" \
  --load-balancers "targetGroupArn=$EUREKA_TG_ARN,containerName=service-registry,containerPort=8761"
```

**api-gateway** (attached to the public ALB):

```bash
aws ecs create-service \
  --cluster microservice-cluster \
  --service-name api-gateway \
  --task-definition api-gateway \
  --desired-count 1 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[$SUBNET_IDS],securityGroups=[$SG_ECS],assignPublicIp=ENABLED}" \
  --load-balancers "targetGroupArn=$GW_TG_ARN,containerName=api-gateway,containerPort=8765"
```

**question-service** and **quiz-service** (no public ALB, only internal):

```bash
aws ecs create-service \
  --cluster microservice-cluster \
  --service-name question-service \
  --task-definition question-service \
  --desired-count 1 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[$SUBNET_IDS],securityGroups=[$SG_ECS],assignPublicIp=ENABLED}"

aws ecs create-service \
  --cluster microservice-cluster \
  --service-name quiz-service \
  --task-definition quiz-service \
  --desired-count 1 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[$SUBNET_IDS],securityGroups=[$SG_ECS],assignPublicIp=ENABLED}"
```

---

## Phase 2 — Configure GitHub Secrets

Go to your GitHub repo:  
**Settings → Secrets and variables → Actions → New repository secret**

Add all 6 secrets:

| Secret name | Where to get the value |
|-------------|----------------------|
| `DOCKERHUB_USERNAME` | Your Docker Hub username |
| `DOCKERHUB_TOKEN` | Docker Hub → Account Settings → Security → Access Token |
| `AWS_ACCESS_KEY_ID` | AWS Console → IAM → Users → your user → Security credentials |
| `AWS_SECRET_ACCESS_KEY` | Same place (only shown once when created) |
| `AWS_REGION` | The region you chose, e.g. `ap-southeast-1` |
| `ECS_CLUSTER` | `microservice-cluster` |

> **Tip:** Create a dedicated IAM user for GitHub Actions with only the permissions it needs:
> - `AmazonECSFullAccess`
> - `AmazonEC2ContainerRegistryReadOnly`
> - `SecretsManagerReadWrite` (for task definition registration only)

---

## Phase 3 — Update Task Definition Placeholders

Open each file in `.github/ecs/` and replace every placeholder:

| Placeholder | Replace with |
|-------------|-------------|
| `ACCOUNT_ID` | Your 12-digit AWS account ID (from step 1.1) |
| `REGION` | Your AWS region, e.g. `ap-southeast-1` |
| `EUREKA_ALB_DNS` | The `$EUREKA_ALB_DNS` value from step 1.9 |
| `QUESTION_RDS_ENDPOINT` | The `$QUESTION_DB_HOST` value from step 1.4 |
| `QUIZ_RDS_ENDPOINT` | The `$QUIZ_DB_HOST` value from step 1.4 |

**Files to update:**
- [.github/ecs/service-registry-task-def.json](.github/ecs/service-registry-task-def.json)
- [.github/ecs/api-gateway-task-def.json](.github/ecs/api-gateway-task-def.json)
- [.github/ecs/question-service-task-def.json](.github/ecs/question-service-task-def.json)
- [.github/ecs/quiz-service-task-def.json](.github/ecs/quiz-service-task-def.json)

Example — what `api-gateway-task-def.json` looks like after replacing:

```json
"executionRoleArn": "arn:aws:iam::123456789012:role/ecsTaskExecutionRole",
"EUREKA_CLIENT_SERVICEURL_DEFAULTZONE": "http://internal-eureka-internal-alb-xxx.ap-southeast-1.elb.amazonaws.com:8761/eureka/"
```

---

## Phase 4 — First Deployment

### 4.1 Commit and push all CI/CD files

```bash
git checkout -b cicd-setup    # or stay on your current branch
git add .github/ question-service/Dockerfile
git commit -m "chore: add GitHub Actions CI/CD pipeline"
git push origin cicd-setup
```

### 4.2 Open a Pull Request → watch CI run

1. Open a PR from `cicd-setup` → `main` on GitHub
2. Go to **Actions** tab — you should see the `CI — Test on Pull Request` workflow start
3. It runs `./gradlew test` for all 4 services in parallel
4. All jobs should pass (green checkmarks)

### 4.3 Merge and tag to trigger CD

```bash
# After PR is merged to main:
git checkout main
git pull origin main

# Create and push a version tag
git tag v1.0.0
git push origin v1.0.0
```

### 4.4 Watch the CD workflow

1. Go to GitHub → **Actions** tab
2. You should see `CD — Build, Push & Deploy on Tag` running
3. Watch the 3 stages:
   - **build-and-push** — 4 parallel jobs building Docker images
   - **deploy-service-registry** — waits for Eureka to be stable
   - **deploy-services** — deploys api-gateway, question-service, quiz-service in parallel

---

## Day-to-Day Workflow

### For a regular feature

```bash
git checkout -b feature/my-feature
# ... make changes ...
git add .
git commit -m "feat: my feature"
git push origin feature/my-feature
# Open PR → CI tests run automatically
# Merge PR → nothing deployed yet
```

### To deploy a new version

```bash
git checkout main && git pull
git tag v1.1.0         # increment the version
git push origin v1.1.0  # triggers full CD pipeline
```

### Versioning convention

| Tag | When to use |
|-----|------------|
| `v1.0.0` | Major breaking change |
| `v1.1.0` | New feature, backward compatible |
| `v1.0.1` | Bug fix |

---

## Verification Checklist

After a successful CD run, verify end-to-end:

- [ ] **GitHub Actions** — all workflow jobs show green
- [ ] **Docker Hub** — images `hazoe-dev/{service}:1.0.0` and `hazoe-dev/{service}:latest` exist
- [ ] **ECS Console** → cluster `microservice-cluster` → Services → all 4 services show `RUNNING`
- [ ] **ECS Tasks** — each service has 1 running task, Last status: `RUNNING`
- [ ] **CloudWatch Logs** → `/ecs/service-registry` → verify Spring Boot startup log: `Started ServiceRegistryApplication`
- [ ] **Eureka Dashboard** — open `http://$EUREKA_ALB_DNS:8761` → confirm api-gateway, question-service, quiz-service are all registered
- [ ] **API Gateway test:**
  ```bash
  curl http://$GW_ALB_DNS/api/questions
  curl http://$GW_ALB_DNS/api/questions/by-ids?ids=1,2,3
  curl -X POST http://$GW_ALB_DNS/api/quizzes \
  -H "Content-Type: application/json" \
  -d '{"title": "My Quiz", "numOfQuestion": 5, "category": "Science"}'

  curl http://$GW_ALB_DNS/api/quizzes/all
  ```

- ```
MSYS_NO_PATHCONV=1 aws logs tail /ecs/quiz-service --since 5m

```
---

## Test API After Deployment

After CD pipeline completes and all ECS tasks are `RUNNING`:

### Get the public URL
```bash
echo $GW_ALB_DNS
# Or if the variable is lost:
aws elbv2 describe-load-balancers \
  --query "LoadBalancers[?LoadBalancerName=='api-gateway-alb'].DNSName" \
  --output text
```

### Call the APIs
```bash
# Health check
curl http://$GW_ALB_DNS/actuator/health

# Question service
curl http://$GW_ALB_DNS/api/questions/all

# Quiz service
curl http://$GW_ALB_DNS/api/quizzes/all
```

> Getting `[]` with no data is normal — as long as there is no `502 Bad Gateway` or `Connection refused`.

### If you get 502
The ECS task is not healthy yet — check the logs:
```bash
aws logs tail /ecs/api-gateway --follow
aws logs tail /ecs/question-service --follow
aws logs tail /ecs/quiz-service --follow
```

---

## Troubleshooting

### Workflow fails at "Build and push"
- Check `DOCKERHUB_USERNAME` and `DOCKERHUB_TOKEN` secrets are set correctly
- Make sure the Docker Hub repositories exist (create them manually first)

### ECS task fails to start (stopped immediately)
- Check CloudWatch Logs for the service — the Spring Boot stack trace will be there
- Common causes: wrong DB endpoint, wrong Eureka URL, wrong secret ARN in task def

### ECS task can't pull image from Docker Hub
- Confirm `dockerhub-creds` secret exists in Secrets Manager with the correct JSON format
- Confirm `ecsTaskExecutionRole` has Secrets Manager read access
- Confirm the `repositoryCredentials.credentialsParameter` ARN in task def is exact (including the suffix `-xxxxxx`)

### Services register with Eureka but can't communicate
- Verify the ECS security group (`$SG_ECS`) allows all inbound traffic from itself
- Check that all services are in the same VPC and subnets

### `wait-for-service-stability` times out in CD
- Default timeout is 10 minutes. Spring Boot 4 with Java 25 may need up to 90s to start
- Increase the `startPeriod` in the task definition `healthCheck` to `120`
- Check the container's CloudWatch logs for startup errors

### Eureka ALB health check fails (service-registry crash-loops)
- Symptom: `service-registry` shows `running: 0, pending: 2` and api-gateway logs show `503` from Eureka
- Cause: Eureka's `/actuator/health` returns 503 in self-preservation mode (AWS ALB only accepts 200-499)
- Fix: change the Eureka target group health check path to `/` (the Eureka dashboard always returns 200):
  ```bash
  MSYS_NO_PATHCONV=1 aws elbv2 modify-target-group \
    --target-group-arn $(aws elbv2 describe-target-groups \
      --names eureka-tg \
      --query "TargetGroups[0].TargetGroupArn" --output text) \
    --health-check-path /
  ```

### RDS connection refused
- Confirm the RDS security group allows port 5432 from the ECS security group
- Confirm the RDS instance is in `available` state
- Double-check the `SPRING_DATASOURCE_URL` endpoint hostname in the task definition

### Temporarily suspended resources:
- ECS services → set desired count = 0 (4 services)
- RDS → Stop temporarily (both 2 instances)
- ALB → Delete (both public + internal)
- NAT Gateway → Delete

```bash
aws ecs update-service --cluster microservice-cluster --service service-registry --desired-count 0
aws ecs update-service --cluster microservice-cluster --service api-gateway --desired-count 0
aws ecs update-service --cluster microservice-cluster --service question-service --desired-count 0
aws ecs update-service --cluster microservice-cluster --service quiz-service --desired-count 0
```
---

## Teardown — Remove All Resources

Run these in order to avoid deleting something that another resource still depends on.

### 1. ECS Services
```bash
for svc in api-gateway question-service quiz-service service-registry; do
  aws ecs update-service --cluster microservice-cluster --service $svc --desired-count 0
  aws ecs delete-service --cluster microservice-cluster --service $svc
done
```

### 2. ECS Cluster
```bash
aws ecs delete-cluster --cluster microservice-cluster
```

### 3. Load Balancers & Target Groups
```bash
# List ARNs if you no longer have them in your shell
aws elbv2 describe-load-balancers --query "LoadBalancers[*].[LoadBalancerArn,LoadBalancerName]" --output table

aws elbv2 delete-load-balancer --load-balancer-arn $EUREKA_ALB_ARN
aws elbv2 delete-load-balancer --load-balancer-arn $GW_ALB_ARN

aws elbv2 delete-target-group --target-group-arn $EUREKA_TG_ARN
aws elbv2 delete-target-group --target-group-arn $GW_TG_ARN
```

### 4. RDS Instances
> Delete these first if you are in a hurry — RDS charges ~$0.02/hr each even when idle.

```bash
aws rds delete-db-instance --db-instance-identifier question-db --skip-final-snapshot
aws rds delete-db-instance --db-instance-identifier quiz-db --skip-final-snapshot
```

Check deletion progress:
```bash
aws rds describe-db-instances \
  --query "DBInstances[*].[DBInstanceIdentifier,DBInstanceStatus]" --output table
```

### 5. Secrets Manager
```bash
aws secretsmanager delete-secret --secret-id dockerhub-creds --force-delete-without-recovery
aws secretsmanager delete-secret --secret-id question-db-password --force-delete-without-recovery
aws secretsmanager delete-secret --secret-id quiz-db-password --force-delete-without-recovery
```

### 6. CloudWatch Log Groups
```bash
for svc in service-registry api-gateway question-service quiz-service; do
  aws logs delete-log-group --log-group-name /ecs/$svc
done
```

### 7. IAM Role
```bash
aws iam detach-role-policy --role-name ecsTaskExecutionRole \
  --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy
aws iam detach-role-policy --role-name ecsTaskExecutionRole \
  --policy-arn arn:aws:iam::aws:policy/SecretsManagerReadWrite
aws iam delete-role --role-name ecsTaskExecutionRole
```

### 8. Security Groups
> Must run after ALBs and RDS are fully deleted.

```bash
aws ec2 delete-security-group --group-id $SG_ECS
aws ec2 delete-security-group --group-id $SG_ALB
aws ec2 delete-security-group --group-id $SG_RDS
```

### Verify nothing billable remains
```bash
aws rds describe-db-instances --query "DBInstances[*].DBInstanceIdentifier"
aws ecs list-clusters
aws elbv2 describe-load-balancers --query "LoadBalancers[*].LoadBalancerName"
```

---

## Architecture Diagram

```
Internet
    │
    ▼
[ Public ALB ] ──────────────────────────────────────────────────────
    │  port 80                                                         │
    │                                                         AWS VPC  │
    ▼                                                                   │
[ api-gateway :8765 ] ──► Eureka lookup ──► [ Internal ALB :8761 ]    │
    │                                              │                   │
    │                               [ service-registry :8761 ]        │
    │                                                                   │
    ├──► /api/questions/** ──► [ question-service :8080 ]             │
    │                                    │                             │
    │                          [ RDS question-db :5432 ]              │
    │                                                                   │
    └──► /api/quizzes/**   ──► [ quiz-service :8090 ]                 │
                                         │                             │
                               [ RDS quiz-db :5432 ]                  │
                                                                        │
────────────────────────────────────────────────────────────────────────
```

All services run as **AWS ECS Fargate tasks** (serverless containers).  
All inter-service traffic stays within the VPC (never leaves AWS).

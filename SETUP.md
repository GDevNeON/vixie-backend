# VixieAI Backend Setup Guide

This guide provides detailed instructions for setting up and running the VixieAI backend services, which consist of two main Spring Boot applications: User Authentication Service and AI Companion Service.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Architecture Overview](#architecture-overview)
- [Environment Setup](#environment-setup)
- [Database Setup](#database-setup)
- [User Authentication Service](#user-authentication-service)
- [AI Companion Service](#ai-companion-service)
- [Docker Setup](#docker-setup)
- [Running the Services](#running-the-services)
- [API Documentation](#api-documentation)
- [Testing](#testing)
- [Tunneling with ngrok](#tunneling-with-ngrok)
- [Project Structure](#project-structure)
- [Key Dependencies](#key-dependencies)
- [Troubleshooting](#troubleshooting)

## Prerequisites

### Required Software

1. **Java Development Kit (JDK) 21**
   - Download from [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://adoptium.net/)

2. **Apache Maven 3.8+**
   - Download from [Maven](https://maven.apache.org/download.cgi)
   - Or use the included Maven wrapper (`mvnw`)

3. **PostgreSQL 15+**
   - Download from [PostgreSQL](https://www.postgresql.org/download/)
   - Or use Docker (recommended)

4. **Redis 7+** (for AI Companion Service)
   - Download from [Redis](https://redis.io/download)
   - Or use Docker (recommended)

5. **Git**
   - For version control

6. **IDE/Editor** (recommended)
   - IntelliJ IDEA with Spring Boot plugin
   - VS Code with Java Extension Pack
   - Eclipse with Spring Tools

### Verify Installation

```bash
java -version
mvn -version
psql --version
redis-cli --version
```

## Architecture Overview

The VixieAI backend consists of two microservices:

1. **User Authentication Service** (`user-auth/`)
   - Port: 8080
   - Handles user registration, login, OAuth integration
   - JWT token management
   - Email verification

2. **AI Companion Service** (`ai-companion/`)
   - Port: 8081
   - AI chat functionality with streaming responses
   - WebSocket support for real-time communication
   - Integration with OpenAI and ElevenLabs APIs

Both services share the same PostgreSQL database and JWT secret for authentication.

## Environment Setup

### 1. Clone the Repository

```bash
git clone <repository-url>
cd vixie-backend
```

### 2. Environment Variables

#### User Authentication Service

```bash
cd user-auth
cp .env.example .env
```

Edit the `.env` file:

```env
# Database Configuration
POSTGRES_DB=vixie_user_auth
POSTGRES_USER=postgres
POSTGRES_PASSWORD=root
POSTGRES_HOST=localhost
POSTGRES_PORT=5432

# Spring Boot Application Configuration
SPRING_PROFILES_ACTIVE=docker
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/vixie_user_auth
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=root

# SMTP Configuration (for email verification)
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=YOUR_GMAIL_ADDRESS
SMTP_PASSWORD=YOUR_APP_PASSWORD

# JWT Configuration
JWT_SECRET=change-me-please-very-very-secret-jwt-key-change-me

# OAuth Configuration
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
FACEBOOK_CLIENT_ID=your_facebook_client_id
FACEBOOK_CLIENT_SECRET=your_facebook_client_secret

BASE_URL=http://localhost:8080
```

#### AI Companion Service

```bash
cd ../ai-companion
cp .env.example .env
```

Edit the `.env` file:

```env
# Database Configuration
POSTGRES_DB=vixie_ai
POSTGRES_USER=postgres
POSTGRES_PASSWORD=root
POSTGRES_HOST=localhost
POSTGRES_PORT=5432

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# Spring Configuration
SPRING_PROFILES_ACTIVE=default
ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8080

# JWT (MUST match user-auth service)
JWT_SECRET=change-me-please-very-very-secret-jwt-key-change-me

# AI Provider Keys
OPENAI_API_KEY=your_openai_api_key
OPENAI_MODEL=gpt-4o
ELEVENLABS_API_KEY=your_elevenlabs_api_key
```

## Database Setup

### Option 1: Using Docker (Recommended)

1. **Start PostgreSQL for User Authentication Service**

```bash
cd user-auth
docker-compose up -d
```

2. **Create AI Companion Database**

```bash
docker exec -it vixie-user-auth-postgres psql -U postgres -c "CREATE DATABASE vixie_ai;"
```

3. **Start Redis for AI Companion Service**

```bash
cd ../ai-companion
docker-compose up -d
```

### Option 2: Local Installation

1. **Install PostgreSQL locally**
2. **Create databases:**

```sql
CREATE DATABASE vixie_user_auth;
CREATE DATABASE vixie_ai;
```

3. **Install and start Redis locally**

```bash
redis-server
```

## User Authentication Service

### Installation

```bash
cd user-auth
./mvnw clean install
```

### Running the Service

#### Development Mode

```bash
./mvnw spring-boot:run
```

The service will start on `http://localhost:8080`

#### Production Mode

```bash
java -jar target/user-auth-0.0.1-SNAPSHOT.jar
```

### Key Features

- JWT-based authentication
- OAuth2 integration (Google, Facebook)
- Email verification
- Password reset
- User profile management
- Firebase integration for push notifications

### API Endpoints

- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login
- `POST /api/auth/refresh` - Refresh JWT token
- `GET /api/auth/oauth2/google` - Google OAuth
- `GET /api/auth/oauth2/facebook` - Facebook OAuth
- `POST /api/auth/forgot-password` - Password reset
- `POST /api/auth/verify-email` - Email verification

## AI Companion Service

### Installation

```bash
cd ai-companion
./mvnw clean install
```

### Running the Service

#### Development Mode

```bash
./mvnw spring-boot:run
```

The service will start on `http://localhost:8081`

#### Production Mode

```bash
java -jar target/ai-companion-0.0.1-SNAPSHOT.jar
```

### Key Features

- Real-time AI chat via WebSocket
- Streaming responses from OpenAI
- Voice synthesis with ElevenLabs
- Conversation history management
- User session management with Redis

### API Endpoints

- `POST /api/chat/completions` - AI chat completion
- `GET /api/chat/history/{userId}` - Chat history
- `DELETE /api/chat/history/{userId}` - Clear chat history
- WebSocket endpoint: `/ws/chat` - Real-time chat

### WebSocket Configuration

Connect to: `ws://localhost:8081/ws/chat`

Message format:
```json
{
  "type": "CHAT",
  "content": "Hello, AI!",
  "userId": "user123",
  "timestamp": "2024-01-01T00:00:00Z"
}
```

## Docker Setup

### Complete Docker Environment

1. **Start User Authentication Service with Database**

```bash
cd user-auth
docker-compose up -d postgres
```

2. **Create AI Database**

```bash
docker exec -it vixie-user-auth-postgres psql -U postgres -c "CREATE DATABASE vixie_ai;"
```

3. **Start AI Companion Service with Redis**

```bash
cd ../ai-companion
docker-compose up -d redis
```

4. **Build and Run Services**

```bash
# User Authentication Service
cd ../user-auth
./mvnw spring-boot:run

# AI Companion Service (in separate terminal)
cd ../ai-companion
./mvnw spring-boot:run
```

### Docker Compose Services

#### User Authentication Service
- **PostgreSQL**: Port 5433 (mapped to container 5432)
- **Database**: `vixie_user_auth`
- **Network**: `vixie-network`

#### AI Companion Service
- **Redis**: Port 6379
- **Network**: Uses external network from user-auth service

## Running the Services

### Development Workflow

1. **Start Infrastructure**

```bash
# Terminal 1: Start databases
cd user-auth
docker-compose up -d

cd ../ai-companion
docker-compose up -d
```

2. **Create AI Database** (one-time setup)

```bash
docker exec -it vixie-user-auth-postgres psql -U postgres -c "CREATE DATABASE vixie_ai;"
```

3. **Start Services** (in separate terminals)

```bash
# Terminal 2: User Authentication Service
cd user-auth
./mvnw spring-boot:run

# Terminal 3: AI Companion Service
cd ai-companion
./mvnw spring-boot:run
```

### Health Checks

- User Auth Service: `http://localhost:8080/actuator/health`
- AI Companion Service: `http://localhost:8081/actuator/health`
- PostgreSQL: `docker exec vixie-user-auth-postgres pg_isready`
- Redis: `docker exec vixie-ai-redis redis-cli ping`

## API Documentation

### Swagger/OpenAPI

- User Auth Service: `http://localhost:8080/swagger-ui.html`
- AI Companion Service: `http://localhost:8081/swagger-ui.html`

### Authentication

All protected endpoints require a valid JWT token in the Authorization header:

```
Authorization: Bearer <jwt_token>
```

## Testing

### Unit Tests

```bash
# User Authentication Service
cd user-auth
./mvnw test

# AI Companion Service
cd ai-companion
./mvnw test
```

### Integration Tests

```bash
# User Authentication Service
cd user-auth
./mvnw test -Dspring.profiles.active=test

# AI Companion Service
cd ai-companion
./mvnw test -Dspring.profiles.active=test
```

### Test Coverage

```bash
./mvnw jacoco:report
```

Coverage reports will be generated in `target/site/jacoco/index.html`

## Tunneling with ngrok

During development, especially when testing on physical Android devices, you need to expose your local backend services to the internet using **ngrok**.

### 1. Install ngrok
Download and install ngrok from [ngrok.com](https://ngrok.com/download).

### 2. Start Tunnels
Since VixieAI uses two services, you need to tunnel both.

**Tunnel User Auth Service (8080):**
```bash
ngrok http 8080
```

**Tunnel AI Companion Service (8081):**
```bash
ngrok http 8081
```

### 3. Update Environment Variables
Copy the forwarding URLs provided by ngrok (e.g., `https://renee-arumlike-azariah.ngrok-free.dev`) and update your `.env` files:

**user-auth/.env:**
```env
BASE_URL=https://renee-arumlike-azariah.ngrok-free.dev
```

> [!IMPORTANT]
> The backend URL changes every time you restart the ngrok tunnel (on the free tier). You must update the `API_BASE_URL` and `AI_BASE_URL` in the frontend `.env` file accordingly.

## Project Structure

```
vixie-backend/
├── user-auth/                    # User Authentication Service
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/neong/
│   │   │   │   ├── config/      # Security, JWT, OAuth config
│   │   │   │   ├── controller/   # REST controllers
│   │   │   │   ├── dto/         # Data transfer objects
│   │   │   │   ├── entity/      # JPA entities
│   │   │   │   ├── repository/  # JPA repositories
│   │   │   │   ├── service/     # Business logic
│   │   │   │   └── UserAuthApplication.java
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/migration/ # Flyway migrations
│   │   └── test/
│   ├── .env                     # Environment variables
│   ├── .env.example             # Environment template
│   ├── docker-compose.yml       # PostgreSQL setup
│   └── pom.xml                  # Maven configuration
├── ai-companion/                # AI Companion Service
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/neong/
│   │   │   │   ├── config/      # WebSocket, OpenAI config
│   │   │   │   ├── controller/  # REST and WebSocket controllers
│   │   │   │   ├── dto/         # Data transfer objects
│   │   │   │   ├── entity/      # JPA entities
│   │   │   │   ├── repository/  # JPA repositories
│   │   │   │   ├── service/     # Business logic
│   │   │   │   └── AiCompanionApplication.java
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/migration/ # Flyway migrations
│   │   └── test/
│   ├── .env                     # Environment variables
│   ├── .env.example             # Environment template
│   ├── docker-compose.yml       # Redis setup
│   └── pom.xml                  # Maven configuration
└── SETUP.md                     # This setup guide
```

## Key Dependencies

### User Authentication Service

- **Spring Boot 4.0.1**: Main framework
- **Spring Security**: Authentication and authorization
- **Spring Data JPA**: Database access
- **Spring Boot Mail**: Email functionality
- **PostgreSQL**: Primary database
- **Flyway**: Database migrations
- **JWT (JJWT)**: Token-based authentication
- **Spring Boot OAuth2**: Social login integration
- **Firebase Admin**: Push notifications
- **Lombok**: Code generation

### AI Companion Service

- **Spring Boot 4.0.1**: Main framework
- **Spring WebSocket**: Real-time communication
- **Spring Data Redis**: Session management
- **Spring WebFlux**: Reactive programming for AI streaming
- **OpenAI API**: AI chat integration
- **ElevenLabs API**: Voice synthesis
- **PostgreSQL**: Conversation storage
- **Redis**: Session caching
- **SpringDoc OpenAPI**: API documentation
- **Lombok**: Code generation

## Troubleshooting

### Common Issues

#### Database Connection Issues

1. **Check PostgreSQL status:**

```bash
docker ps | grep postgres
docker logs vixie-user-auth-postgres
```

2. **Verify database exists:**

```bash
docker exec -it vixie-user-auth-postgres psql -U postgres -l
```

3. **Check connection string in .env files**

#### Redis Connection Issues

1. **Check Redis status:**

```bash
docker ps | grep redis
docker logs vixie-ai-redis
```

2. **Test Redis connection:**

```bash
docker exec -it vixie-ai-redis redis-cli ping
```

#### Port Conflicts

- User Auth Service: Default port 8080
- AI Companion Service: Default port 8081
- PostgreSQL: Port 5433 (Docker) or 5432 (local)
- Redis: Port 6379

Change ports in application.yml if conflicts occur.

#### JWT Token Issues

1. **Ensure JWT_SECRET matches in both services**
2. **Check token expiration**
3. **Verify token format in Authorization header**

#### Maven Build Issues

1. **Clean and rebuild:**

```bash
./mvnw clean install
```

2. **Check Java version (requires JDK 21):**

```bash
java -version
```

3. **Clear Maven cache:**

```bash
./mvnw dependency:purge-local-repository
```

#### SSL/TLS Issues with External APIs

1. **Import certificates if using corporate proxy:**

```bash
keytool -import -alias cert -file certificate.cer -keystore $JAVA_HOME/lib/security/cacerts
```

2. **Configure proxy in application.yml if needed**

### Getting Help

1. Check application logs for detailed error messages
2. Verify all environment variables are set correctly
3. Ensure all required services (PostgreSQL, Redis) are running
4. Check network connectivity to external APIs (OpenAI, ElevenLabs)
5. Review Spring Boot Actuator endpoints for health status

### Performance Monitoring

- Use Spring Boot Actuator endpoints: `/actuator/metrics`, `/actuator/health`
- Monitor database performance with PostgreSQL logs
- Check Redis memory usage and connection counts
- Use application performance monitoring (APM) tools in production

---

**Note**: This setup guide assumes you're working with the latest versions of all dependencies. Always check the official documentation for each service for the most up-to-date requirements and best practices.

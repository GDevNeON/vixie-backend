# Docker Setup for User Auth Service

## Quick Start

1. **Start PostgreSQL database:**
   ```bash
   docker-compose up -d
   ```

2. **Check database status:**
   ```bash
   docker-compose ps
   ```

3. **View logs:**
   ```bash
   docker-compose logs -f postgres
   ```

4. **Stop services:**
   ```bash
   docker-compose down
   ```

## Configuration

### Database Connection
- **Host:** localhost
- **Port:** 5432
- **Database:** vixie_user_auth
- **Username:** postgres
- **Password:** postgres123

### Environment Variables
Create a `.env` file to customize configuration:
```bash
POSTGRES_PASSWORD=your_secure_password
POSTGRES_DB=your_database_name
```

### Application Properties
Update your `application.properties` to use Docker configuration:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/vixie_user_auth
spring.datasource.username=postgres
spring.datasource.password=postgres123
```

## Development Workflow

1. **Start database:** `docker-compose up -d`
2. **Run Spring Boot application** from IDE or: `mvn spring-boot:run`
3. **Stop database when done:** `docker-compose down`

## Database Management

### Connect to database
```bash
docker-compose exec postgres psql -U postgres -d vixie_user_auth
```

### Backup database
```bash
docker-compose exec postgres pg_dump -U postgres vixie_user_auth > backup.sql
```

### Restore database
```bash
docker-compose exec -T postgres psql -U postgres vixie_user_auth < backup.sql
```

## Troubleshooting

### Port conflicts
If port 5432 is already in use, modify the port mapping in `docker-compose.yml`:
```yaml
ports:
  - "5433:5432"  # Use 5433 on host
```

### Data persistence
Database data is persisted in Docker volume `postgres_data`. To reset:
```bash
docker-compose down -v
```

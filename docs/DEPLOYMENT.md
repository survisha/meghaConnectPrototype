# MeghaConnect - Deployment Guide

## Overview
MeghaConnect is packaged as a single executable JAR file containing both the Spring Boot backend and Angular frontend. This makes deployment simple - just one file to copy and run.

---

## Prerequisites

### On Build Machine (Development)
- **Node.js**: 18+ (for Angular build)
- **Angular CLI**: `npm install -g @angular/cli`
- **Java**: JDK 8 or higher
- **Maven**: 3.6+ 
- **Git**: For version control

### On Target Machine (Production/Testing)
- **Java Runtime**: JRE 8 or higher (`java -version` to check)
- **MySQL**: 8.0+ (or compatible database)
- **Redis**: 6.0+ (for caching)
- **Network Access**: Port 8080 (default, configurable)

---

## Build Process

### Option 1: Automated Build (Recommended)

Run the PowerShell build script:

```powershell
# Full build (frontend + backend)
.\build-deploy.ps1

# With clean (removes previous builds)
.\build-deploy.ps1 -Clean

# Skip frontend rebuild (use existing dist)
.\build-deploy.ps1 -SkipFrontend

# Skip backend rebuild (use existing JAR)
.\build-deploy.ps1 -SkipBackend
```

### Option 2: Manual Build

**Step 1: Build Angular Frontend**
```powershell
cd frontend
ng build --configuration production
# Output: frontend/dist/frontend/
```

**Step 2: Copy Frontend to Backend Static Folder**
```powershell
# Clean static folder first
Remove-Item backend/src/main/resources/static/* -Recurse -Force

# Copy frontend build
Copy-Item frontend/dist/frontend/* backend/src/main/resources/static/ -Recurse -Force
```

**Step 3: Build Spring Boot JAR**
```powershell
cd backend
mvn clean package -DskipTests
# Output: backend/target/meghaconnect-1.0.0-SNAPSHOT.jar
```

---

## Deployment

### 1. Database Setup

On the target machine, create the MySQL database:

```sql
CREATE DATABASE meghaconnect CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create dedicated user (recommended)
CREATE USER 'meghaconnect_user'@'localhost' IDENTIFIED BY 'SecurePassword123!';
GRANT ALL PRIVILEGES ON meghaconnect.* TO 'meghaconnect_user'@'localhost';
FLUSH PRIVILEGES;
```

### 2. Redis Setup

MeghaConnect uses Redis for caching reference data and improving performance. Redis must be running before starting the application.

#### Local Development Setup

**Option A: Docker (Recommended)**
```bash
# Pull and run Redis container
docker run -d --name meghaconnect-redis -p 6379:6379 redis:7.2-alpine

# Verify it's running
docker ps | grep meghaconnect-redis

# Test connection
docker exec -it meghaconnect-redis redis-cli ping
# Expected response: PONG

# Stop Redis
docker stop meghaconnect-redis

# Start Redis again
docker start meghaconnect-redis

# View logs
docker logs meghaconnect-redis
```

**Option B: Windows (Native Installation)**
1. Download Redis for Windows: https://redis.io/download
2. Extract to `C:\Redis`
3. Open Command Prompt as Administrator
```cmd
cd C:\Redis
redis-server.exe --service-install
redis-server.exe --service-start
```

**Option C: WSL Ubuntu (on Windows)**
```bash
# Install Redis
sudo apt update
sudo apt install redis-server

# Start Redis service
sudo systemctl enable redis-server
sudo systemctl start redis-server

# Check status
sudo systemctl status redis-server

# Test connection
redis-cli ping
```

#### Production Setup

**Option A: Docker (Recommended for Production)**
```bash
# Create Redis data directory
sudo mkdir -p /opt/redis/data
sudo chown redis:redis /opt/redis/data

# Run Redis with persistence and security
docker run -d \
  --name meghaconnect-redis \
  -p 6379:6379 \
  -v /opt/redis/data:/data \
  -e REDIS_PASSWORD=your_secure_password_here \
  --restart unless-stopped \
  redis:7.2-alpine \
  redis-server --appendonly yes --requirepass your_secure_password_here

# Verify
docker logs meghaconnect-redis
```

**Option B: Ubuntu/Debian Server**
```bash
# Install Redis
sudo apt update
sudo apt install redis-server

# Configure Redis
sudo nano /etc/redis/redis.conf

# Key settings to modify:
# bind 127.0.0.1 ::1  # Listen on localhost only
# port 6379
# requirepass your_secure_password_here
# maxmemory 256mb
# maxmemory-policy allkeys-lru
# appendonly yes
# appendfilename "appendonly.aof"

# Restart Redis
sudo systemctl restart redis-server
sudo systemctl enable redis-server

# Test connection
redis-cli -a your_secure_password_here ping
```

**Option C: Red Hat/CentOS**
```bash
# Install Redis
sudo yum install redis

# Configure (similar to Ubuntu)
sudo nano /etc/redis.conf

# Start service
sudo systemctl start redis
sudo systemctl enable redis
```

**Option D: AWS ElastiCache (Managed Redis)**
- Create ElastiCache cluster in AWS Console
- Choose Redis engine
- Configure security groups to allow access from your EC2 instance
- Use the endpoint in your `application-prod.properties`

**Option E: Azure Cache for Redis**
- Create Azure Cache for Redis in Azure Portal
- Choose Basic/Standard/Premium tier
- Get connection string and configure in environment variables

#### Redis Configuration in Application

Add to your `application-prod.properties`:

```properties
# Redis Configuration
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.password=your_secure_password_here
spring.redis.timeout=2000ms
spring.redis.database=0

# Cache Configuration
spring.cache.type=redis
spring.cache.redis.time-to-live=3600000
```

Or use environment variables:
```bash
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=your_secure_password_here
```

### 3. Copy JAR to Target Machine

Transfer the JAR file:
```
backend/target/meghaconnect-1.0.0-SNAPSHOT.jar
```

You can use:
- SCP: `scp meghaconnect-1.0.0-SNAPSHOT.jar user@server:/opt/meghaconnect/`
- USB drive
- Network share
- FTP/SFTP

### 3. Create Configuration File (Recommended)

Create `application-prod.properties` next to the JAR:

```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/meghaconnect?useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=meghaconnect_user
spring.datasource.password=SecurePassword123!
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# Flyway Migration
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration

# JWT Configuration
app.jwt.secret=MeghaConnectSecureJWTSecretKey2026!ChangeThisInProduction
app.jwt.expiration-ms=86400000

# Server Configuration
server.port=8080
server.compression.enabled=true
server.compression.mime-types=text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json

# Logging
logging.level.root=INFO
logging.level.com.survisha.meghaconnect=DEBUG
logging.file.name=logs/meghaconnect.log
logging.file.max-size=10MB
logging.file.max-history=30
```

### 4. Run the Application

**Using Configuration File:**
```bash
java -jar meghaconnect-1.0.0-SNAPSHOT.jar --spring.config.location=application-prod.properties
```

**Using Command-Line Arguments:**
```bash
java -jar meghaconnect-1.0.0-SNAPSHOT.jar \
  --spring.datasource.url=jdbc:mysql://localhost:3306/meghaconnect \
  --spring.datasource.username=meghaconnect_user \
  --spring.datasource.password=SecurePassword123! \
  --app.jwt.secret=MeghaConnectSecureJWTSecretKey2026 \
  --server.port=8080
```

**Increase Memory (for production):**
```bash
java -Xms512m -Xmx2048m -jar meghaconnect-1.0.0-SNAPSHOT.jar --spring.config.location=application-prod.properties
```

### 5. Access the Application

- **URL**: http://localhost:8080 (or http://server-ip:8080)
- **Frontend**: Root path `/` serves Angular app
- **API**: Backend REST APIs at `/api/v1/*`

---

## Default Login Credentials

Do not use seeded/demo credentials in production. Create production users
through the approved admin process and share temporary reviewer/UAT
credentials out of band only. Migration `V54__disable_known_seed_credentials`
disables old seeded accounts if they still have known default hashes.

---

## Running as a Service

### Windows Service (using NSSM)

1. Download NSSM: https://nssm.cc/download
2. Install service:
```powershell
nssm install MeghaConnect "C:\Program Files\Java\jdk1.8.0_351\bin\java.exe"
nssm set MeghaConnect AppParameters "-jar C:\apps\meghaconnect\meghaconnect-1.0.0-SNAPSHOT.jar --spring.config.location=C:\apps\meghaconnect\application-prod.properties"
nssm set MeghaConnect AppDirectory "C:\apps\meghaconnect"
nssm set MeghaConnect DisplayName "MeghaConnect CM Office Portal"
nssm set MeghaConnect Description "Chief Minister Office Scheduling & Scheme Management System"
nssm set MeghaConnect Start SERVICE_AUTO_START
nssm start MeghaConnect
```

### Linux Systemd Service

Create `/etc/systemd/system/meghaconnect.service`:

```ini
[Unit]
Description=MeghaConnect CM Office Portal
After=syslog.target network.target mysql.service

[Service]
Type=simple
User=meghaconnect
WorkingDirectory=/opt/meghaconnect
ExecStart=/usr/bin/java -Xms512m -Xmx2048m -jar /opt/meghaconnect/meghaconnect-1.0.0-SNAPSHOT.jar --spring.config.location=/opt/meghaconnect/application-prod.properties
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

Enable and start:
```bash
sudo systemctl daemon-reload
sudo systemctl enable meghaconnect
sudo systemctl start meghaconnect
sudo systemctl status meghaconnect
```

---

## Troubleshooting

### Application Doesn't Start

**Check Java Version:**
```bash
java -version
# Should be 1.8 or higher
```

**Check Port Availability:**
```bash
# Windows
netstat -ano | findstr :8080

# Linux
netstat -tulpn | grep :8080
```

**Check Database Connection:**
```bash
# From target machine, test MySQL connection
mysql -h localhost -u meghaconnect_user -p meghaconnect
```

### Flyway Migration Errors

If database schema already exists:
```bash
# Run with repair flag
java -jar meghaconnect-1.0.0-SNAPSHOT.jar --spring.flyway.repair=true
```

Or connect to MySQL and:
```sql
USE meghaconnect;
DELETE FROM flyway_schema_history WHERE success = 0;
```

### Redis Connection Issues

**Check if Redis is running:**
```bash
# Local Docker
docker ps | grep redis

# Native installation
redis-cli ping

# With password
redis-cli -a your_password ping
```

**Check Redis logs:**
```bash
# Docker
docker logs meghaconnect-redis

# Systemd
sudo journalctl -u redis-server -f
```

**Common Redis errors:**
- `Redis is required but not available. Please start Redis server.`
  - Solution: Start Redis server as described above

- `WRONGPASS invalid username-password pair`
  - Solution: Check `REDIS_PASSWORD` environment variable or `spring.redis.password`

- `Connection refused`
  - Solution: Verify Redis host/port, firewall rules, Docker networking

**Reset Redis data (development only):**
```bash
redis-cli FLUSHALL
```

### Frontend Not Loading

1. Verify static files are in JAR:
```bash
jar -tf meghaconnect-1.0.0-SNAPSHOT.jar | grep "BOOT-INF/classes/static"
```

Should show files like:
- `BOOT-INF/classes/static/index.html`
- `BOOT-INF/classes/static/main.*.js`
- `BOOT-INF/classes/static/styles.*.css`

2. Check browser console for errors
3. Access backend API directly: http://localhost:8080/api/v1/auth/login

### Check Logs

**Console Output:**
Application logs print to stdout/stderr

**Log File (if configured):**
```bash
tail -f logs/meghaconnect.log
```

**Increase Log Level:**
```bash
java -jar meghaconnect-1.0.0-SNAPSHOT.jar --logging.level.root=DEBUG
```

---

## Environment-Specific Configuration

### Development
- Port: 8080
- CORS: Enabled for localhost:4200
- JWT Secret: Development key
- Flyway: Auto-migrate
- Log Level: DEBUG

### Testing/Staging
- Port: 8080
- CORS: Restricted to staging domain
- JWT Secret: Staging-specific key
- Flyway: Auto-migrate
- Log Level: INFO

### Production
- Port: 80/443 (behind reverse proxy)
- CORS: Restricted to production domain
- JWT Secret: Strong random key (rotate regularly)
- Flyway: Manual migrations (disable auto)
- Log Level: WARN
- HTTPS: Required
- Database: Remote MySQL with replication

---

## Reverse Proxy Setup (Production)

### Nginx Configuration

```nginx
server {
    listen 80;
    server_name meghaconnect.meghalaya.gov.in;
    
    # Redirect HTTP to HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name meghaconnect.meghalaya.gov.in;
    
    ssl_certificate /etc/ssl/certs/meghaconnect.crt;
    ssl_certificate_key /etc/ssl/private/meghaconnect.key;
    
    # SSL Configuration
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    
    # Proxy to Spring Boot
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket support (if needed)
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
    
    # Cache static assets
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        proxy_pass http://localhost:8080;
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

---

## Security Checklist

- [ ] Change default user passwords
- [ ] Use strong JWT secret (min 256 bits)
- [ ] **Configure Redis password** (`requirepass` in redis.conf)
- [ ] **Restrict Redis access** (bind to localhost only in production)
- [ ] Enable HTTPS in production
- [ ] Restrict CORS origins
- [ ] Use firewall to limit port access (8080, 6379)
- [ ] Regular security updates (Java, MySQL, Redis, OS)
- [ ] Database user has minimum required privileges
- [ ] Enable SQL injection protection (parameterized queries - already done via JPA)
- [ ] Implement rate limiting for login endpoints
- [ ] Regular backups of MySQL database and Redis data
- [ ] Monitor application logs for suspicious activity

---

## Backup & Recovery

### Database Backup (Daily)
```bash
mysqldump -u meghaconnect_user -p meghaconnect > backup_$(date +%Y%m%d).sql
```

### Restore Database
```bash
mysql -u meghaconnect_user -p meghaconnect < backup_20260305.sql
```

### Application Logs Backup
```bash
tar -czf logs_backup_$(date +%Y%m%d).tar.gz logs/
```

---

## Performance Tuning

### JVM Options
```bash
java -Xms1g -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/log/meghaconnect/heapdump.hprof \
  -jar meghaconnect-1.0.0-SNAPSHOT.jar
```

### Database Connection Pool
Add to `application-prod.properties`:
```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

---

## Monitoring

### Health Check Endpoint
```bash
curl http://localhost:8080/actuator/health
```

### Application Metrics
Enable Spring Boot Actuator endpoints in `application-prod.properties`:
```properties
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=always
```

---

## Support

For issues or questions:
- **Email**: support@survisha.com
- **Documentation**: `docs/` folder
- **SRS**: `docs/SRS.md`

---

**Last Updated**: April 14, 2026  
**Version**: 1.0.0-SNAPSHOT

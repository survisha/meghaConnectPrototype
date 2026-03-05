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

### 2. Copy JAR to Target Machine

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

After first startup, Flyway migrations will seed demo users:

| Username | Password | Role |
|----------|----------|------|
| `hcm` | `hcm123` | HCM (Chief Minister) |
| `admin` | `admin123` | ADMIN |
| `saidul` | `osd123` | SAIDUL_OSD (Officer on Special Duty) |
| `jtsecy` | `jts123` | APPROVER_JT_SECY (Joint Secretary) |
| `cmo` | `cmo123` | CMO_OFFICER |
| `deo1` | `deo123` | DATA_ENTRY_OPERATOR |
| `public1` | `public123` | PUBLIC (Citizen) |

**⚠️ IMPORTANT**: Change these passwords in production!

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
- [ ] Enable HTTPS in production
- [ ] Restrict CORS origins
- [ ] Use firewall to limit port access
- [ ] Regular security updates (Java, MySQL, OS)
- [ ] Database user has minimum required privileges
- [ ] Enable SQL injection protection (parameterized queries - already done via JPA)
- [ ] Implement rate limiting for login endpoints
- [ ] Regular backups of MySQL database
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

**Last Updated**: March 5, 2026  
**Version**: 1.0.0-SNAPSHOT

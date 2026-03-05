# MeghaConnect - Quick Deployment Guide

## 📦 Deployment Package Created

| Item | Value |
|------|-------|
| **Artifact** | `meghaconnect-1.0.0-SNAPSHOT.jar` |
| **Location** | `backend/target/meghaconnect-1.0.0-SNAPSHOT.jar` |
| **Size** | 86.05 MB |
| **Includes** | Spring Boot Backend + Angular Frontend (embedded) |
| **Build Date** | March 5, 2026 |

---

## 🚀 Quick Start (Testing on Another Machine)

### Prerequisites
- **Java**: JDK/JRE 8 or higher ([Download](https://www.oracle.com/java/technologies/downloads/))
- **MySQL**: 8.0 or higher ([Download](https://dev.mysql.com/downloads/mysql/))

### Step 1: Setup Database

```sql
-- Connect to MySQL as root
mysql -u root -p

-- Create database
CREATE DATABASE meghaconnect CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create dedicated user (recommended)
CREATE USER 'megha_user'@'localhost' IDENTIFIED BY 'MeghaPass2026!';
GRANT ALL PRIVILEGES ON meghaconnect.* TO 'megha_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### Step 2: Copy JAR File

Copy `backend/target/meghaconnect-1.0.0-SNAPSHOT.jar` to your deployment folder:

**Windows:**
```powershell
# Example: Copy to C:\meghaconnect\
New-Item -Path "C:\meghaconnect" -ItemType Directory -Force
Copy-Item "backend\target\meghaconnect-1.0.0-SNAPSHOT.jar" "C:\meghaconnect\"
```

**Linux/Mac:**
```bash
# Example: Copy to /opt/meghaconnect/
sudo mkdir -p /opt/meghaconnect
sudo cp backend/target/meghaconnect-1.0.0-SNAPSHOT.jar /opt/meghaconnect/
```

### Step 3: Create Configuration File (Optional but Recommended)

Create `application.properties` next to the JAR:

```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/meghaconnect
spring.datasource.username=megha_user
spring.datasource.password=MeghaPass2026!

# JWT Secret (CHANGE THIS!)
app.jwt.secret=MeghaConnectSecureJWTKey2026ChangeThisInProduction

# Server
server.port=8080

# Logging
logging.level.root=INFO
logging.level.com.survisha.meghaconnect=DEBUG
```

### Step 4: Run the Application

**Option A: With Configuration File (Recommended)**
```bash
java -jar meghaconnect-1.0.0-SNAPSHOT.jar --spring.config.location=application.properties
```

**Option B: With Command-Line Parameters**
```bash
java -jar meghaconnect-1.0.0-SNAPSHOT.jar \
  --spring.datasource.url=jdbc:mysql://localhost:3306/meghaconnect \
  --spring.datasource.username=megha_user \
  --spring.datasource.password=MeghaPass2026! \
  --app.jwt.secret=MeghaConnect2026SecureJwtSecretKeyForProductionUseOnly12345
```

**⚠️ IMPORTANT:** JWT secret MUST be at least 32 characters (256 bits) for security compliance!

**Option C: With Increased Memory (for production)**
```bash
java -Xms512m -Xmx2048m -jar meghaconnect-1.0.0-SNAPSHOT.jar --spring.config.location=application.properties
```

### Step 5: Access the Application

Open your browser and navigate to:

```
http://localhost:8080
```

You should see the MeghaConnect landing page with login options.

---

## 🔐 Default Login Credentials

| Role | Username | Password |
|------|----------|----------|
| HCM (Chief Minister) | `hcm` | `hcm123` |
| Admin | `admin` | `admin123` |
| OSD (Officer on Special Duty) | `saidul` | `osd123` |
| Joint Secretary (Approver) | `jtsecy` | `jts123` |
| CMO Officer | `cmo` | `cmo123` |
| Data Entry Operator | `deo1` | `deo123` |
| Public/Citizen | `public1` | `public123` |

**⚠️ SECURITY WARNING**: Change these default passwords immediately in production!

---

## 📱 Testing Workflows

### 1. Staff Login (e.g., DEO)
1. Navigate to http://localhost:8080/login
2. Username: `deo1` | Password: `deo123`
3. Access:
   - Appointments management
   - Public identification
   - New appointment form
   - Grievances

### 2. Visitor/Citizen Login
1. Navigate to http://localhost:8080/public-login
2. **First Time**: Click "Register as New Visitor"
   - Complete KYC registration (ID verification, OTP, photo capture)
   - Provides EPIC/Aadhaar, mobile number
3. **Returning**: Enter registered mobile → OTP login
4. Access:
   - Book appointments
   - Apply for schemes
   - Submit grievances
   - Track status

### 3. HCM (Chief Minister) Login
1. Navigate to http://localhost:8080/login
2. Username: `hcm` | Password: `hcm123`
3. Access:
   - Dashboard with AI insights
   - Review appointments (Accept/Snooze/Reject)
   - Schedule calendar
   - Audit trail
   - Reports

---

## ✅ Verification Checklist

After startup, verify:

- [ ] Application starts without errors
- [ ] Can access http://localhost:8080
- [ ] Frontend loads (Angular app)
- [ ] Can login with staff credentials
- [ ] Can login with citizen OTP flow
- [ ] Database tables created automatically (Flyway migrations)
- [ ] Demo users seeded
- [ ] API endpoints working (check Network tab in browser)

---

## 🛠️ Troubleshooting

### "Port 8080 is already in use"
```bash
# Windows - Find and kill process
netstat -ano | findstr :8080
taskkill /F /PID <process_id>

# Linux/Mac
lsof -i :8080
kill -9 <process_id>

# Or run on different port
java -jar meghaconnect-1.0.0-SNAPSHOT.jar --server.port=9090
```

### "Unable to connect to database"
1. Verify MySQL is running:
   ```bash
   # Windows
   sc query MySQL80
   
   # Linux
   sudo systemctl status mysql
   ```

2. Test database connection:
   ```bash
   mysql -h localhost -u megha_user -p meghaconnect
   ```

3. Check connection string in application.properties

### "Flyway migration failed"
1. Check if database exists:
   ```sql
   SHOW DATABASES LIKE 'meghaconnect';
   ```

2. If schema conflicts exist, reset:
   ```sql
   DROP DATABASE meghaconnect;
   CREATE DATABASE meghaconnect;
   ```

3. If migration checksum errors:
   ```bash
   java -jar meghaconnect-1.0.0-SNAPSHOT.jar --spring.flyway.repair=true
   ```

### Frontend not loading (404 errors)
1. Verify frontend files are in JAR:
   ```bash
   jar -tf meghaconnect-1.0.0-SNAPSHOT.jar | grep "BOOT-INF/classes/static"
   ```

2. Should show files like:
   - `BOOT-INF/classes/static/index.html`
   - `BOOT-INF/classes/static/main-*.js`
   - `BOOT-INF/classes/static/styles-*.css`

3. If missing, rebuild with:
   ```bash
   .\build-deploy.ps1 -Clean
   ```

---

## 📚 Additional Documentation

- **Full Deployment Guide**: `docs/DEPLOYMENT.md`
- **SRS Document**: `docs/SRS.md`
- **Build Script**: `build-deploy.ps1` (PowerShell) or `build-deploy.sh` (Bash)
- **Configuration Template**: `application-prod.properties.template`

---

## 🆘 Support

For issues or questions:
- **Email**: support@survisha.com
- **Project Structure**: See `README.md`
- **Architecture**: See `.github/copilot-instructions.md`

---

**Built**: March 5, 2026  
**Version**: 1.0.0-SNAPSHOT  
**Technology Stack**: Spring Boot 2.7.18 + Angular 19 + MySQL 8

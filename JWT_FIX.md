# JWT Secret Key Fix - URGENT

## Problem
Getting error: "The specified key byte array is 72 bits which is not secure enough for any JWT HMAC-SHA algorithm"

## Root Cause
The JWT secret key used in deployment was **TOO SHORT**:
- ❌ `SecureKey123` = 12 characters (96 bits) → **REJECTED by JWT library**
- ✅ Required: **At least 32 characters (256 bits)**

## Solution

### On the "Other Machine" - STOP the JAR and restart with secure key:

**1. Stop the current JAR:**
```bash
# Find Java process
tasklist | findstr java
# Kill it (replace PID with actual process ID)
taskkill /F /PID <processID>
```

**2. Restart with SECURE JWT secret (at least 32 characters):**

```bash
java -jar meghaconnect-1.0.0-SNAPSHOT.jar \
  --spring.datasource.url=jdbc:mysql://localhost:3306/meghaconnect_db \
  --spring.datasource.username=root \
  --spring.datasource.password=yourpassword \
  --app.jwt.secret=MeghaConnect2026SecureJwtSecretKeyForProductionUseOnly12345
```

**IMPORTANT:** The above key is **61 characters** - meets the 256-bit requirement! ✅

### Alternative Option - Remove JWT Override (Use Default from JAR)

The JAR already has a **secure 256-bit default key** built-in. You can simply remove the `--app.jwt.secret` parameter:

```bash
java -jar meghaconnect-1.0.0-SNAPSHOT.jar \
  --spring.datasource.url=jdbc:mysql://localhost:3306/meghaconnect_db \
  --spring.datasource.username=root \
  --spring.datasource.password=yourpassword
```

The default key in `application.properties` is:
```
app.jwt.secret=dGhpc0lzT05MWUZPUkxPQ0FMREVWRUxPUE1FTlRQTEVBU0VDSEFOR0VJTlBST0Q=
```
This is a **Base64-encoded 64-character string** - very secure! ✅

## For Production Deployment

**Best Practice:** Use environment variable instead of command-line parameter:

**Linux/Mac:**
```bash
export JWT_SECRET="MeghaConnect2026SecureJwtSecretKeyForProductionUseOnly12345"
java -jar meghaconnect-1.0.0-SNAPSHOT.jar \
  --spring.datasource.url=jdbc:mysql://localhost:3306/meghaconnect_db \
  --spring.datasource.username=root \
  --spring.datasource.password=yourpassword
```

**Windows:**
```powershell
$env:JWT_SECRET="MeghaConnect2026SecureJwtSecretKeyForProductionUseOnly12345"
java -jar meghaconnect-1.0.0-SNAPSHOT.jar `
  --spring.datasource.url=jdbc:mysql://localhost:3306/meghaconnect_db `
  --spring.datasource.username=root `
  --spring.datasource.password=yourpassword
```

## Security Notes

### ✅ SECURE Keys (256 bits minimum = 32 characters):
- `MeghaConnect2026SecureJwtSecretKeyForProductionUseOnly12345` (61 chars)
- `MeghaConnectSecureJWTSecretKey2026!ChangeThisInProduction` (57 chars)
- `dGhpc0lzT05MWUZPUkxPQ0FMREVWRUxPUE1FTlRQTEVBU0VDSEFOR0VJTlBST0Q=` (64 chars)
- Any random string 32+ characters long

### ❌ INSECURE Keys (TOO SHORT - will cause 500 error):
- `SecureKey123` (12 chars) - **REJECTED**
- `yourSecretKey123` (16 chars) - **REJECTED**
- `YourSecureSecretKey123` (22 chars) - **STILL TOO SHORT**
- `password` (8 chars) - **REJECTED**

### Generate Your Own Secure Key

**PowerShell (Windows):**
```powershell
-join ((48..57) + (65..90) + (97..122) | Get-Random -Count 64 | % {[char]$_})
```

**Bash (Linux/Mac):**
```bash
openssl rand -base64 48
```

**Python:**
```python
import secrets
print(secrets.token_urlsafe(48))
```

## Testing After Fix

1. **Restart with secure key** (see commands above)
2. **Access application:** http://localhost:8080
3. **Test OTP login:**
   - Go to: http://localhost:8080/public-login
   - Enter registered mobile: `9876543210`
   - Get OTP: `123456` (demo mode)
   - Verify OTP → Should redirect to `/visitor` dashboard **WITHOUT 500 error**
4. **Check browser console:** No JWT errors

## Why This Happened

- **Local machine:** Uses default secure key from `application.properties` ✅
- **Other machine:** You overrode with `--app.jwt.secret=SecureKey123` ❌
- **JWT library checks:** Key must be ≥ 256 bits (≥ 32 characters) per RFC 7518 standard

## Updated Documentation

The following files have been updated with secure keys:
- ✅ `QUICKSTART.md` - Updated with 61-character key
- ✅ `build-deploy.ps1` - Updated with 61-character key
- ✅ `build-deploy.sh` - Updated with 61-character key
- ✅ `docs/DEPLOYMENT.md` - Already had secure keys (57+ characters)

---

**TL;DR:** Change `--app.jwt.secret=SecureKey123` to `--app.jwt.secret=MeghaConnect2026SecureJwtSecretKeyForProductionUseOnly12345` or remove the parameter entirely to use the secure default.

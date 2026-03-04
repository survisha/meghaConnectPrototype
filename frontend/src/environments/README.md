# Environment Configuration

This folder contains environment-specific configuration files for the MeghaConnect frontend application.

## Available Environments

### 1. **environment.development.ts** (Default for `ng serve`)
- **Purpose**: Local development
- **API URL**: `http://localhost:8080/api/v1`
- **Usage**: Automatically used when running `ng serve` or `npm start`
- **Proxy**: Works with `proxy.conf.json` to route `/api` calls to backend on port 8080

### 2. **environment.ts** (Production)
- **Purpose**: Production deployment
- **API URL**: `http://your-production-domain.com/api/v1`
- **Usage**: Used when building with `ng build` or `ng build --configuration production`
- **Action Required**: Update `apiUrl` before production deployment

### 3. **environment.dev.ts** (Custom Dev Server)
- **Purpose**: Development server deployment (staging/QA)
- **API URL**: `http://your-dev-server.com/api/v1` (placeholder)
- **Usage**: Create custom configuration in `angular.json` to use this file
- **Action Required**: Configure your dev server URL

## How to Use

### For Local Development (Default)
```bash
npm start
# or
ng serve
```
This automatically uses `environment.development.ts` and proxies API calls to `localhost:8080`.

### For Production Build
```bash
ng build
# or
ng build --configuration production
```
This uses `environment.ts`. **Remember to update the production API URL first!**

### For Custom Dev Server
1. Update `environment.dev.ts` with your dev server URL
2. Add a new configuration in `angular.json`:
```json
"configurations": {
  "dev": {
    "fileReplacements": [
      {
        "replace": "src/environments/environment.development.ts",
        "with": "src/environments/environment.dev.ts"
      }
    ]
  }
}
```
3. Build or serve: `ng build --configuration dev`

## Environment Variables

Each environment file exports an object with:
- `production` (boolean): Production mode flag
- `apiUrl` (string): Base URL for API endpoints
- `appName` (string): Application display name
- `version` (string): Application version

## Using in Services

Import the environment in your services:
```typescript
import { environment } from '../environments/environment.development';

@Injectable({ providedIn: 'root' })
export class MyService {
  private apiUrl = environment.apiUrl;
  
  getData() {
    return this.http.get(`${this.apiUrl}/endpoint`);
  }
}
```

## Current Setup

### Proxy Configuration (Local Development)
The `proxy.conf.json` file routes all `/api/*` requests to `http://localhost:8080`:
- Frontend: `http://localhost:4200` (Angular dev server)
- Backend: `http://localhost:8080` (Spring Boot)
- API calls: Use relative paths like `/api/v1/...` - automatically proxied

### Backend Required
Make sure the Spring Boot backend is running on port 8080:
```bash
cd backend
mvn spring-boot:run
```

## Notes
- The `environment.development.ts` file is the default and should not be committed with sensitive credentials
- Always update production URLs before deployment
- Use environment variables for sensitive configuration in CI/CD pipelines

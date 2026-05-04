# Prebuilt UAT Deployment

Use this flow when you want to build Angular and the Spring Boot JAR on your local/build machine, then copy only deployment artifacts to the Ubuntu VPS.

## Build On Local/Build Machine

Build Angular with the UAT configuration:

```bash
cd frontend
npm ci
npx ng build --configuration uat
```

Build backend JAR:

```bash
cd ../backend
mvn clean package -DskipTests
```

## Create Release Bundle

Create this structure:

```text
release/
  frontend/
    index.html
    main-*.js
    styles-*.css
    assets or asserts folders
  backend/
    app.jar
  deploy.sh
```

For Angular 17+ style output, copy the contents of:

```text
frontend/dist/frontend/browser/
```

into:

```text
release/frontend/
```

Copy the backend JAR:

```text
backend/target/meghaconnect-1.0.0-SNAPSHOT.jar
```

to:

```text
release/backend/app.jar
```

Copy `deploy.sh` into the `release/` folder.

## Run On VPS

```bash
cd /path/to/release
chmod +x deploy.sh
DEPLOY_MODE=prebuilt CERTBOT_EMAIL=admin@meghaconnect.cloud ./deploy.sh
```

The script will install files into:

```text
/var/www/meghaconnect
/opt/meghaconnect/backend/app.jar
/opt/meghaconnect/scripts/deploy.sh
```

It will not delete uploads or logs.

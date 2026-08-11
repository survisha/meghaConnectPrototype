# EPIC Face environment configuration

Real provider credentials must be configured in the runtime environment or deployment secret manager. Never place them in `application.yml`, profile files, `.env` files committed to Git, container images, CI logs, or frontend/mobile configuration.

Required when enabling the integration:

```text
EPIC_FACE_ENABLED=true
EPIC_FACE_1N_API_KEY=<secret supplied by provider>
EPIC_FACE_11_API_KEY=<secret supplied by provider>
```

Optional overrides:

```text
EPIC_FACE_BASE_URL=https://prdev.onlineipv.com/MeghalayaEPICFaceMW
EPIC_FACE_1N_PATH=/FaceService1N
EPIC_FACE_11_PATH=/FaceService11
EPIC_FACE_CONNECT_TIMEOUT=10
EPIC_FACE_READ_TIMEOUT=60
EPIC_FACE_WRITE_TIMEOUT=30
EPIC_FACE_CALL_TIMEOUT=90
```

Example PowerShell session for local development (values remain process-local):

```powershell
$env:EPIC_FACE_ENABLED='true'
$env:EPIC_FACE_1N_API_KEY='<secret>'
$env:EPIC_FACE_11_API_KEY='<secret>'
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

For UAT/production, store both keys in the platform secret manager and inject them as environment variables. Rotate the credentials immediately if they are ever committed, pasted into logs, or included in a build artifact.

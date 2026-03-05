#!/bin/bash
# ============================================
# MeghaConnect - Quick Deploy Script (Linux/Mac)
# ============================================

set -e

echo "========================================"
echo "  MeghaConnect - Build & Deploy Script  "
echo "========================================"
echo ""

# Get project root directory
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND_DIR="$PROJECT_ROOT/frontend"
BACKEND_DIR="$PROJECT_ROOT/backend"
STATIC_DIR="$BACKEND_DIR/src/main/resources/static"
DIST_DIR="$FRONTEND_DIR/dist/frontend"

# Step 1: Build Angular Frontend
echo "[STEP 1] Building Angular Frontend (Production)..."
cd "$FRONTEND_DIR"
ng build --configuration production

if [ ! -d "$DIST_DIR" ]; then
    echo "  ✗ Angular build failed - dist folder not found!"
    exit 1
fi
echo "  ✓ Angular build completed"
echo ""

# Step 2: Copy Frontend to Backend Static Folder
echo "[STEP 2] Copying Frontend to Backend Static Folder..."
rm -rf "${STATIC_DIR:?}"/*

# Copy from browser subfolder (Angular 17+ esbuild output structure)
BROWSER_DIR="$DIST_DIR/browser"
if [ -d "$BROWSER_DIR" ]; then
    cp -r "$BROWSER_DIR"/* "$STATIC_DIR/"
    echo "  ✓ Frontend copied from browser subfolder (Angular 17+ structure)"
else
    # Fallback for older Angular versions
    cp -r "$DIST_DIR"/* "$STATIC_DIR/"
    echo "  ✓ Frontend copied from dist root (legacy Angular structure)"
fi
echo ""

# Step 3: Build Spring Boot JAR
echo "[STEP 3] Building Spring Boot JAR..."
cd "$BACKEND_DIR"
mvn clean package -DskipTests

JAR_FILE=$(find target -name "*.jar" ! -name "*-sources.jar" ! -name "*-javadoc.jar" | head -n 1)

if [ -z "$JAR_FILE" ]; then
    echo "  ✗ Maven build failed - JAR not found!"
    exit 1
fi

JAR_NAME=$(basename "$JAR_FILE")
JAR_SIZE=$(du -h "$JAR_FILE" | cut -f1)

echo "  ✓ Maven build completed"
echo "  ✓ JAR created: $JAR_NAME"
echo "  ✓ JAR size: $JAR_SIZE"
echo ""

# Step 4: Summary
echo "========================================"
echo "  BUILD COMPLETED SUCCESSFULLY! ✓"
echo "========================================"
echo ""
echo "📦 Deployment Package Ready:"
echo "   • JAR File: backend/target/$JAR_NAME"
echo "   • Includes: Backend API + Frontend UI (embedded)"
echo ""
echo "🚀 How to Run:"
echo ""
echo "   java -jar $JAR_NAME \\"
echo "     --spring.datasource.url=jdbc:mysql://localhost:3306/meghaconnect \\"
echo "     --spring.datasource.username=root \\"
echo "     --spring.datasource.password=yourpassword \\"
echo "     --app.jwt.secret=MeghaConnect2026SecureJwtSecretKeyForProductionUseOnly12345"
echo ""
echo "💡 See docs/DEPLOYMENT.md for detailed deployment instructions"
echo ""

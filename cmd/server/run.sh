#!/bin/bash
# One-command local run script for Payment Service
# Usage: ./run.sh

set -e

echo "======================================"
echo "Payment Service - One-Command Local Run"
echo "======================================"
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if Java 21 is installed
echo -n "Checking Java version... "
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 21 ]; then
        echo -e "${GREEN}✓ Java $JAVA_VERSION${NC}"
    else
        echo -e "${RED}✗ Java 21 or higher required (found Java $JAVA_VERSION)${NC}"
        exit 1
    fi
else
    echo -e "${RED}✗ Java not found${NC}"
    echo "Please install Java 21 or higher"
    exit 1
fi

# Navigate to project root
cd "$(dirname "$0")/../.."

# Check if env.properties exists
if [ ! -f "src/main/resources/env.properties" ]; then
    echo -e "${YELLOW}⚠ env.properties not found, creating from example...${NC}"
    if [ -f "src/main/resources/env.properties.example" ]; then
        cp src/main/resources/env.properties.example src/main/resources/env.properties
        echo -e "${GREEN}✓ Created env.properties${NC}"
    else
        echo "Creating default env.properties..."
        cat > src/main/resources/env.properties << EOF
db.url=jdbc:sqlite:payment.db
db.username=
db.password=
EOF
        echo -e "${GREEN}✓ Created default env.properties${NC}"
    fi
fi

# Clean and build
echo ""
echo "Building application..."
./mvnw clean package -DskipTests

if [ $? -ne 0 ]; then
    echo -e "${RED}✗ Build failed${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Build successful${NC}"
echo ""

# Run the application
echo "Starting Payment Service..."
echo "======================================"
echo "Ports:"
echo "  - gRPC Service: http://localhost:9090"
echo "  - Actuator (Health/Metrics): http://localhost:8081"
echo ""
echo "Health Check: http://localhost:8081/actuator/health"
echo "Metrics: http://localhost:8081/actuator/metrics"
echo ""
echo "Press Ctrl+C to stop"
echo "======================================"
echo ""

java -jar target/payment-service-*.jar


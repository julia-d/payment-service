# One-Command Local Run

Run the Payment Service locally with a single command.

## Usage

### Linux/Mac
```bash
chmod +x run.sh
./run.sh
```

### Windows (PowerShell)
```powershell
.\run.ps1
```

## What It Does

1. ✅ Checks Java 21+ is installed
2. ✅ Creates `env.properties` if missing (from example or default)
3. ✅ Builds the application (`mvn clean package`)
4. ✅ Starts the service

## Access

Once running:
- **gRPC Service:** `localhost:9090`
- **Health Check:** http://localhost:8081/actuator/health
- **Metrics:** http://localhost:8081/actuator/metrics

## Requirements

- Java 21 or higher
- Maven (uses bundled `mvnw`/`mvnw.cmd`)

## Stop

Press `Ctrl+C` to stop the service gracefully.


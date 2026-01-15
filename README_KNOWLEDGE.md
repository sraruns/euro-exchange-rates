# Exchange Rate Service - Development Guide

## Quick Start

### Maven
```bash
mvn spring-boot:run
```

### Docker
```bash
# Build and run
docker-compose up --build

# Or manually
docker build -t cm-coding-challenge .
docker run -p 8080:8080 -v ./data:/app/data cm-coding-challenge
```

## Swagger UI
- UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api-docs

## API Documentation

### Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/currencies` | GET | List all available currencies |
| `/api/exchange-rates/history` | GET | Get paginated exchange rate history |
| `/api/exchange-rates/{on_date}` | GET | Get all rates for specific date |
| `/api/convert-currency` | GET | Convert between currencies |

### Examples

```bash
# Get currencies
curl http://localhost:8080/api/currencies

# Get history (paginated)
curl "http://localhost:8080/api/exchange-rates/history?from_date=2024-01-01&to_date=2024-01-15&page=0&size=10"

# Get rates on date
curl http://localhost:8080/api/exchange-rates/2024-01-15

# Convert EUR to USD
curl "http://localhost:8080/api/convert-currency?from_currency=EUR&to_currency=USD&amount=100&on_date=2024-01-15"
```

### Running Tests
```bash
# All unit tests
mvn test -Dtest="BundesBankParserTest,ExchangeRateServiceTest,ExchangeRateControllerTest"

# Single test class
mvn test -Dtest=ExchangeRateServiceTest
```

## Tech Stack
- Java 11, Spring Boot 2.7.18
- H2 (file-based persistence)
- Caffeine cache (1hr TTL)
- WebClient for Bundesbank API
- springdoc-openapi for Swagger

## Prerequisites (per README.md requirements)
- **Java**: 11 (compatible with OpenSDK 15)
- **Maven**: 3.x
- **Spring Boot**: 2.7.18

## Docker Execution

### Multi-stage Build
The Dockerfile uses multi-stage build for smaller image:
1. **Build stage**: `maven:3.8-openjdk-11-slim` - compiles the app
2. **Runtime stage**: `eclipse-temurin:11-jre-alpine` - runs the JAR (~150MB)

### Commands
```bash
# Using docker-compose (recommended)
docker-compose up --build

# Manual build & run
docker build -t cm-coding-challenge .
docker run -p 8080:8080 -v ./data:/app/data cm-coding-challenge

```

### Volume Mount
- `./data:/app/data` - persists H2 database across container restarts

## Bundesbank SDMX API Flow

### Step 1: Dataflow - Get available data flows
```
GET /api/dataflow
```
Returns list of available dataflows. Focus: **BBEX3** (FX reference rates)

### Step 2: Datastructure - Understand series key format
```
GET /api/datastructure/BBK_ERX
```
Reveals the key dimensions for exchange rates:
```
{FREQ}.{CURRENCY}.{COUNTER_CURRENCY}.{RATE_TYPE}.{VARIANT}.{SERIES}
D.USD.EUR.BB.AC.000
```
- `D` = Daily frequency
- `USD` = Currency
- `EUR` = Counter currency (base)
- `BB` = Rate type
- `AC` = Variant
- `000` = Series

### Step 3: Codelist - Get available currencies
```
GET /api/codelist/{codelist_id}
```
Returns all valid currency codes for the dimensions.

### Step 4: Data - Fetch actual rates
```
GET /api/data/{flow}/{key}
GET /api/data/BBEX3/D.USD.EUR.BB.AC.000
```
Returns exchange rate data for specified series.

---

## Bundesbank Dataflow Reference

| Dataflow | Domain | Description | Example Key |
|----------|--------|-------------|-------------|
| BBEX3 | FX rates | ECB euro reference rates (daily/monthly/annual) | `D.USD.EUR.BB.AC.000` |
| BBK01 | Macro | Legacy macro DB (rates, money, prices) | `TTA032` |
| BBAF3 | Financial accounts | Sectoral financial balance sheets | `Q.DE.N.F.A.F2.S1.S11._Z._Z._Z.A` |
| BBSIS | Securities | Securities issues/holdings by sector | `D.I.ZAR.ZI.EUR.S1311.B.A604.R10XX.R.A.A._Z._Z.A` |
| BBMFK1 | Monetary | Money aggregates, base money | `M.AU1724` |
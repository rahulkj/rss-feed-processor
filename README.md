# RSS Feed Processing with Kafka and Ollama

This project contains two Spring Boot applications for processing RSS feeds using Kafka and Ollama (DeepSeek model).

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              RSS Feed Processing                                 │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│   ┌─────────────────────┐     ┌──────────────┐     ┌─────────────────────┐    │
│   │  RSS Feed Producer  │────▶│    Kafka     │────▶│  RSS Feed Consumer  │    │
│   │   (Port 8081)      │     │  (Port 9092) │     │   (Port 8082)      │    │
│   └─────────────────────┘     └──────────────┘     └─────────────────────┘    │
│            │                                                 │                   │
│            │                                                 │                   │
│            ▼                                                 ▼                   │
│   ┌─────────────────────────────────────────────────────────────────────────┐  │
│   │                     Optimized Ollama Service                           │  │
│   │  • WebClient with Connection Pooling (50 connections)                  │  │
│   │  • Parallel Processing (Reactive Flux/Mono)                           │  │
│   │  • MD5-based Response Caching (60 min TTL)                           │  │
│   │  • Conditional Enrichment (keyword-based)                             │  │
│   │  • Configurable Timeouts (connect: 10s, response: 60s)              │  │
│   └─────────────────────────────────────────────────────────────────────────┘  │
│            │                                                 │                   │
│            ▼                                                 ▼                   │
│   ┌───────────┐                                   ┌──────────────┐             │
│   │  Ollama    │◀─────────────────────────────────│  PostgreSQL   │             │
│   │(DeepSeek)  │                                   │(Port 5432)   │             │
│   └───────────┘                                   └──────────────┘             │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## Project Structure

```
.
├── rss-feed-producer/          # Polls RSS feeds, publishes to Kafka with Ollama enrichment
├── rss-feed-consumer/          # Consumes Kafka events, stores in DB, provides Ollama search
├── docker-compose.yml          # Infrastructure (Kafka, PostgreSQL)
├── run.sh                      # Build and run script
├── kpack/                     # KPack image build configurations
├── kubernetes/                 # Kubernetes deployment YAMLs
└── README.md
```

## Prerequisites

- Java 17+
- Maven 3.8+
- Docker/Podman
- Kubernetes cluster (for deployment)
- KPack (for OCI image building)

## Quick Start (Local Development)

### 1. Start Infrastructure

```bash
./run.sh up
```

This starts:
- Kafka (port 9092)
- Zookeeper (port 2181)
- PostgreSQL (port 5432)

### 2. Build Applications

```bash
./run.sh build
```

### 3. Run Applications

```bash
./run.sh start
```

## Run Script Commands

```bash
./run.sh build      # Build both applications
./run.sh up        # Start infrastructure only
./run.sh start     # Start applications (requires infrastructure)
./run.sh run       # Start everything (up + start)
./run.sh down      # Stop infrastructure
./run.sh stop     # Stop applications
./run.sh status   # Check services status
./run.sh restart  # Restart everything
```

## API Endpoints

### Producer (Port 8081)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/feeds` | List all RSS feeds |
| GET | `/api/feeds/{id}` | Get feed by ID |
| GET | `/api/feeds/name/{name}` | Get feed by name |
| POST | `/api/feeds` | Add new RSS feed |
| PUT | `/api/feeds/{id}` | Update feed |
| DELETE | `/api/feeds/{id}` | Delete feed |
| POST | `/api/feeds/{id}/poll` | Manually poll a feed |
| POST | `/api/feeds/{id}/toggle` | Toggle feed active status |

### Consumer (Port 8082)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/news` | Get all stored news (100 most recent) |
| GET | `/api/news/feed/{feedKey}` | Get news by feed key |
| POST | `/api/news/search` | Search news using Ollama |

## Example Requests

### Add a new RSS feed

```bash
curl -X POST http://localhost:8081/api/feeds \
  -H "Content-Type: application/json" \
  -d '{
    "name": "bbc-news",
    "url": "http://feeds.bbci.co.uk/news/rss.xml",
    "description": "BBC News RSS Feed"
  }'
```

### Search news using Ollama

```bash
curl -X POST http://localhost:8082/api/news/search \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Find news about artificial intelligence and machine learning"
  }'
```

### Search within specific feed

```bash
curl -X POST http://localhost:8082/api/news/search \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Find technology news",
    "feedKey": "bbc-news"
  }'
```

## Configuration

### Producer (application.yml)

```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/rssdb
    username: postgres
    password: postgres
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

rss:
  polling:
    interval: 300000  # 5 minutes
  feeds:
    - name: bbc-news
      url: http://feeds.bbci.co.uk/news/rss.xml
    - name: techcrunch
      url: https://techcrunch.com/feed/

ollama:
  base-url: http://localhost:11434
  model: deepseek-r1
  enabled: true
  connection-pool-size: 50
  connect-timeout: 10000
  response-timeout: 60000
  batch-size: 10
  cache-enabled: true
  cache-ttl-minutes: 60
  prompt-template: "Analyze and provide a brief summary: {title} - {description}"
```

### Consumer (application.yml)

```yaml
server:
  port: 8082

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/rssconsumerdb
    username: postgres
    password: postgres
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: rss-consumer-group

ollama:
  base-url: http://localhost:11434
  model: deepseek-r1
  enabled: true
  connection-pool-size: 50
  connect-timeout: 10000
  response-timeout: 60000
  batch-size: 10
  cache-enabled: true
  cache-ttl-minutes: 60
  search-prompt-template: "Find relevant articles: {prompt}. Articles: {articles}"
```

## Ollama Optimizations

### 1. WebClient with Connection Pooling
- Replaced blocking `RestTemplate` with non-blocking `WebClient`
- Configurable connection pool (default: 50 connections)
- Connection timeout: 10 seconds
- Response timeout: 60 seconds

### 2. Parallel Processing
- Uses Spring WebFlux (Reactor) for async processing
- `Flux` and `Mono` for reactive streams
- `Schedulers.boundedElastic()` for parallel execution
- Concurrency matches connection pool size

### 3. Response Caching
- MD5 hash-based cache keys from content
- TTL: configurable (default: 60 minutes)
- Caches both enrichment and search responses
- Reduces redundant Ollama calls

### 4. Conditional Enrichment
Only enriches items matching criteria:
- Keywords: "breaking", "exclusive", "urgent", "alert", "news", "update", "announcement", "release"
- OR title length > 30 characters

### 5. Batch Processing
- Multiple entries processed in parallel
- Configurable batch size (default: 10)

## Kafka Topic

- **Topic Name**: `rss-events`
- **Key**: RSS feed name (e.g., "bbc-news")
- **Partitions**: 3
- **Value**: JSON with title, description, link, pubDate, content, enrichedContent

## Kubernetes Deployment

### Prerequisites
- Kubernetes cluster
- KPack installed for OCI image building
- ConfigMap and Secret management

### Build OCI Images with KPack

```bash
# Apply KPack configurations
kubectl apply -f kpack/
```

### Deploy to Kubernetes

```bash
# Apply configurations
kubectl apply -f kubernetes/configmap.yaml
kubectl apply -f kubernetes/secrets.yaml
kubectl apply -f kubernetes/producer-deployment.yaml
kubectl apply -f kubernetes/consumer-deployment.yaml
```

## Troubleshooting

### Ollama not available
If Ollama is not running, the applications will work but without AI enrichment/search capabilities. Set `ollama.enabled: false` in application.yml to disable.

### Kafka connection issues
Ensure Kafka is running and accessible. Check the `spring.kafka.bootstrap-servers` configuration matches your setup.

### Database connection issues
Ensure PostgreSQL is running and accessible. Check datasource configuration in application.yml.

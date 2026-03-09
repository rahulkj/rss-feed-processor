# Build OCI Images using pack CLI

## Prerequisites

1. Install [pack CLI](https://buildpacks.io/docs/tools/pack/)
   ```bash
   brew install pack
   # Or download from: https://github.com/buildpacks/pack/releases
   ```

2. Ensure Docker is running
   ```bash
   docker ps
   ```

## Build Images

### Build Producer Image

```bash
cd rss-feed-producer
pack build registry.example.com/rss-feed-producer:latest \
  --builder paketobuildpacks/builder-jammy-tiny \
  --path . \
  --env BP_JVM_VERSION=17 \
  --env BP_MAVEN_BUILD_ARGUMENTS="-DskipTests"
```

### Build Consumer Image

```bash
cd rss-feed-consumer
pack build registry.example.com/rss-feed-consumer:latest \
  --builder paketobuildpacks/builder-jammy-tiny \
  --path . \
  --env BP_JVM_VERSION=17 \
  --env BP_MAVEN_BUILD_ARGUMENTS="-DskipTests"
```

## Run Locally

### Producer

```bash
docker run -p 8081:8081 \
  --env SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/rssdb \
  --env SPRING_DATASOURCE_USERNAME=postgres \
  --env SPRING_DATASOURCE_PASSWORD=postgres \
  --env SPRING_KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9092 \
  registry.example.com/rss-feed-producer:latest
```

### Consumer

```bash
docker run -p 8082:8082 \
  --env SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/rssconsumerdb \
  --env SPRING_DATASOURCE_USERNAME=postgres \
  --env SPRING_DATASOURCE_PASSWORD=postgres \
  --env SPRING_KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9092 \
  registry.example.com/rss-feed-consumer:latest
```

## Deploy to Kubernetes

### Push Images to Registry

```bash
docker push registry.example.com/rss-feed-producer:latest
docker push registry.example.com/rss-feed-consumer:latest
```

### Update Deployment YAMLs

Update the `image` fields in `kubernetes/producer-deployment.yaml` and `kubernetes/consumer-deployment.yaml`:

```yaml
image: registry.example.com/rss-feed-producer:latest
# and
image: registry.example.com/rss-feed-consumer:latest
```

### Apply Deployments

```bash
kubectl apply -f kubernetes/configmap.yaml
kubectl apply -f kubernetes/secrets.yaml
kubectl apply -f kubernetes/producer-deployment.yaml
kubectl apply -f kubernetes/consumer-deployment.yaml
```

## Notes

- `paketobuildpacks/builder-jammy-tiny` is a minimal builder that includes only the necessary buildpacks for Java apps
- The builder automatically detects the JAR file and creates an optimized container image
- No Dockerfile needed - buildpacks handle everything
- The `BP_MAVEN_BUILD_ARGUMENTS` environment variable passes Maven build arguments to skip tests during build

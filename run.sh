#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PRODUCER_DIR="$SCRIPT_DIR/rss-feed-producer"
CONSUMER_DIR="$SCRIPT_DIR/rss-feed-consumer"

build() {
    echo "=========================================="
    echo "Building RSS Feed Producer..."
    echo "=========================================="
    cd "$PRODUCER_DIR"
    mvn clean package -DskipTests
    echo "Producer built successfully!"

    echo "=========================================="
    echo "Building RSS Feed Consumer..."
    echo "=========================================="
    cd "$CONSUMER_DIR"
    mvn clean package -DskipTests
    echo "Consumer built successfully!"

    echo "=========================================="
    echo "Build complete!"
    echo "=========================================="
}

up() {
    echo "=========================================="
    echo "Starting Infrastructure (Podman Compose)..."
    echo "=========================================="
    cd "$SCRIPT_DIR"
    podman compose up -d

    echo "Waiting for PostgreSQL to be ready..."
    sleep 10
    
    for i in {1..30}; do
        if podman exec postgres pg_isready -U postgres -q 2>/dev/null; then
            echo "PostgreSQL is ready!"
            break
        fi
        echo "Waiting for PostgreSQL... ($i/30)"
        sleep 2
    done

    echo "Creating databases..."
    podman exec postgres psql -U postgres -c "CREATE DATABASE rssdb;" 2>/dev/null || true
    podman exec postgres psql -U postgres -c "CREATE DATABASE rssconsumerdb;" 2>/dev/null || true
    echo "Databases created!"

    echo "Waiting for Kafka..."
    sleep 10

    echo "=========================================="
    echo "Infrastructure started!"
    echo "  - PostgreSQL: localhost:5432"
    echo "  - Kafka: localhost:9092"
    echo "  - Zookeeper: localhost:2181"
    echo "=========================================="
}

start() {
    echo "=========================================="
    echo "Starting RSS Feed Producer on port 8081..."
    echo "=========================================="
    cd "$PRODUCER_DIR"
    java -jar target/rss-feed-producer-1.0.0.jar &

    PRODUCER_PID=$!
    echo "Producer started with PID: $PRODUCER_PID"

    echo "=========================================="
    echo "Starting RSS Feed Consumer on port 8082..."
    echo "=========================================="
    cd "$CONSUMER_DIR"
    java -jar target/rss-feed-consumer-1.0.0.jar &

    CONSUMER_PID=$!
    echo "Consumer started with PID: $CONSUMER_PID"

    echo "=========================================="
    echo "All services started!"
    echo "Producer: http://localhost:8081"
    echo "Consumer: http://localhost:8082"
    echo "Producer PID: $PRODUCER_PID"
    echo "Consumer PID: $CONSUMER_PID"
    echo "=========================================="
}

run() {
    up
    start
}

down() {
    echo "=========================================="
    echo "Stopping Podman Compose..."
    echo "=========================================="
    cd "$SCRIPT_DIR"
    podman compose down -v
    echo "Podman Compose stopped and volumes removed."
}

stop() {
    echo "=========================================="
    echo "Stopping applications..."
    echo "=========================================="
    pkill -f "rss-feed-producer" || true
    pkill -f "rss-feed-consumer" || true
    echo "Applications stopped."
}

status() {
    echo "=========================================="
    echo "Checking services status..."
    echo "=========================================="
    cd "$SCRIPT_DIR"
    podman compose ps
    echo ""
    echo "Java processes:"
    pgrep -fl "rss-feed" || echo "No running applications"
}

case "$1" in
    build)
        build
        ;;
    up)
        up
        ;;
    start)
        start
        ;;
    run)
        run
        ;;
    down)
        down
        ;;
    stop)
        stop
        ;;
    status)
        status
        ;;
    restart)
        stop
        down
        run
        ;;
    *)
        echo "Usage: $0 {build|up|start|run|down|stop|status|restart}"
        echo "  build   - Build both applications"
        echo "  up      - Start Podman Compose infrastructure only"
        echo "  start   - Start applications (requires infrastructure running)"
        echo "  run     - Start infrastructure and applications"
        echo "  down    - Stop Podman Compose and remove volumes"
        echo "  stop    - Stop running applications"
        echo "  status  - Check services status"
        echo "  restart - Restart everything"
        exit 1
        ;;
esac

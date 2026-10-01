#!/bin/zsh

ROOT="$(cd "$(dirname "$0")" && pwd)"
SERVICE="$1"
ENV_FILE="$ROOT/.env/$SERVICE.env"

if [[ -z "$SERVICE" ]]; then
    echo "Usage: ./run-service.sh <service-name>"
    echo
    echo "Available services:"
    find "$ROOT/services" -mindepth 1 -maxdepth 1 -type d -printf "  %f\n"
    exit 1
fi

SERVICE_PATH="$ROOT/services/$SERVICE"

if [[ ! -d "$SERVICE_PATH" ]]; then
    echo "Error: Service '$SERVICE' does not exist."
    exit 1
fi

if [[ ! -f "$ENV_FILE" ]]; then
    echo "Error: Environment file not found:"
    echo "  $ENV_FILE"
    exit 1
fi

echo "Starting $SERVICE..."
echo "Loading: $ENV_FILE"

kgx -- zsh -c "
    cd '$ROOT'

    set -a
    source '$ENV_FILE'
    set +a

    ./gradlew :services:$SERVICE:bootRun

    echo
    echo '========================================'
    echo '  $SERVICE has stopped'
    echo '========================================'
    echo

    exec zsh
"
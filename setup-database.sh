#!/bin/bash

# Smart Product Recommendation Engine - Database Setup Script
# This script sets up PostgreSQL with PGVector extension for the application

set -e

echo "🚀 Setting up Smart Product Recommendation Engine Database..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker and try again."
    exit 1
fi

# Check if Docker Compose is available
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null 2>&1; then
    echo "❌ Docker Compose is not available. Please install Docker Compose."
    exit 1
fi

# Use docker-compose or docker compose based on availability
DOCKER_COMPOSE_CMD="docker-compose"
if ! command -v docker-compose &> /dev/null; then
    DOCKER_COMPOSE_CMD="docker compose"
fi

echo "📦 Starting PostgreSQL with PGVector extension..."

# Start PostgreSQL container
$DOCKER_COMPOSE_CMD up -d postgres

echo "⏳ Waiting for PostgreSQL to be ready..."

# Wait for PostgreSQL to be healthy
max_attempts=30
attempt=0
while [ $attempt -lt $max_attempts ]; do
    if $DOCKER_COMPOSE_CMD exec -T postgres pg_isready -U postgres > /dev/null 2>&1; then
        echo "✅ PostgreSQL is ready!"
        break
    fi
    attempt=$((attempt + 1))
    echo "   Attempt $attempt/$max_attempts - waiting for PostgreSQL..."
    sleep 2
done

if [ $attempt -eq $max_attempts ]; then
    echo "❌ PostgreSQL failed to start within expected time."
    echo "📋 Checking container logs:"
    $DOCKER_COMPOSE_CMD logs postgres
    exit 1
fi

echo "🔧 Verifying PGVector extension..."

# Verify PGVector extension is available
if $DOCKER_COMPOSE_CMD exec -T postgres psql -U postgres -d product_recommender -c "SELECT * FROM pg_extension WHERE extname='vector';" | grep -q "vector"; then
    echo "✅ PGVector extension is installed and ready!"
else
    echo "❌ PGVector extension not found. This might indicate a setup issue."
    exit 1
fi

echo "📊 Database connection details:"
echo "   Host: localhost"
echo "   Port: 5432"
echo "   Database: product_recommender"
echo "   Username: postgres"
echo "   Password: postgres"

echo ""
echo "🎯 Optional: Start pgAdmin for database management"
echo "   Run: $DOCKER_COMPOSE_CMD --profile dev up -d"
echo "   Access: http://localhost:5050"
echo "   Email: admin@admin.com"
echo "   Password: admin"

echo ""
echo "✅ Database setup complete! You can now start the application."
echo "   Run: ./mvnw spring-boot:run"

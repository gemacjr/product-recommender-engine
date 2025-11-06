# 🚀 Smart Product Recommendation Engine - Setup Guide

This guide will help you set up and run the Smart Product Recommendation Engine with all its dependencies.

## 📋 Prerequisites

Before starting, ensure you have the following installed:

- **Java 17 or higher** - [Download here](https://adoptium.net/)
- **Docker & Docker Compose** - [Download here](https://www.docker.com/products/docker-desktop/)
- **Maven 3.6+** (or use the included Maven wrapper `./mvnw`)
- **OpenAI API Key** - [Get one here](https://platform.openai.com/api-keys)

## 🔧 Quick Setup (Automated)

### 1. Clone and Navigate
```bash
git clone <repository-url>
cd product-recommender-engine
```

### 2. Set Up Environment Variables
Create a `.env` file or set environment variable:
```bash
export OPENAI_API_KEY=your-openai-api-key-here
```

Or copy the example file:
```bash
cp .env.example .env
# Edit .env and add your OpenAI API key
```

### 3. Run Database Setup Script
```bash
./setup-database.sh
```

This script will:
- Start PostgreSQL with PGVector extension
- Create the required database
- Verify the setup
- Provide connection details

### 4. Build and Run the Application
```bash
./mvnw clean compile
./mvnw spring-boot:run
```

### 5. Verify Setup
```bash
curl http://localhost:8080/api/queries/health
```

## 🛠️ Manual Setup (Step by Step)

### Step 1: Database Setup

#### Start PostgreSQL with PGVector
```bash
docker-compose up -d postgres
```

#### Verify Database is Running
```bash
docker-compose ps
```

#### Check PGVector Extension
```bash
docker-compose exec postgres psql -U postgres -d product_recommender -c "SELECT * FROM pg_extension WHERE extname='vector';"
```

### Step 2: Application Configuration

#### Environment Variables
Set your OpenAI API key:
```bash
export OPENAI_API_KEY=your-openai-api-key-here
```

#### Application Properties
The application is pre-configured with the following settings in `application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/product_recommender
    username: postgres
    password: postgres
  
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o
          temperature: 0.7
      embedding:
        options:
          model: text-embedding-3-small
    
    vectorstore:
      pgvector:
        index-type: HNSW
        distance-type: COSINE_DISTANCE
        dimensions: 1536
```

### Step 3: Build and Run

#### Compile the Application
```bash
./mvnw clean compile
```

#### Run the Application
```bash
./mvnw spring-boot:run
```

Or with inline environment variable:
```bash
OPENAI_API_KEY=your-key-here ./mvnw spring-boot:run
```

## 🧪 Testing the Setup

### Health Check
```bash
curl http://localhost:8080/api/queries/health
```

### Get All Products
```bash
curl http://localhost:8080/api/products
```

### Test Semantic Search
```bash
curl -X POST http://localhost:8080/api/queries/ask \
  -H "Content-Type: application/json" \
  -d '{"query": "What are the best wireless headphones?"}'
```

### Test Recommendations
```bash
curl http://localhost:8080/api/recommendations/search?query=noise%20canceling%20headphones
```

## 🔍 Optional: Database Management

### Start pgAdmin (Optional)
```bash
docker-compose --profile dev up -d
```

Access pgAdmin at: http://localhost:5050
- Email: admin@admin.com
- Password: admin

### Connect to Database in pgAdmin
- Host: postgres (or localhost if connecting from outside Docker)
- Port: 5432
- Database: product_recommender
- Username: postgres
- Password: postgres

## 📊 Sample Data

The application automatically loads sample data on first startup, including:
- 12 products across various categories
- Electronics, Clothing, Home & Garden, Sports, Books, Beauty, Health
- Automatic vector embeddings generation

## 🚨 Troubleshooting

### Common Issues

#### 1. Database Connection Failed
```bash
# Check if PostgreSQL is running
docker-compose ps

# Restart PostgreSQL
docker-compose restart postgres

# Check logs
docker-compose logs postgres
```

#### 2. PGVector Extension Missing
```bash
# Verify extension
docker-compose exec postgres psql -U postgres -d product_recommender -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

#### 3. OpenAI API Errors
```bash
# Verify API key is set
echo $OPENAI_API_KEY

# Check application logs
./mvnw spring-boot:run | grep "OpenAI"
```

#### 4. Port Already in Use
```bash
# Check what's using port 8080
lsof -i :8080

# Or change the port in application.yaml
server:
  port: 8081
```

#### 5. Empty Recommendations
```bash
# Manually trigger vector store ingestion
curl -X POST http://localhost:8080/api/products/ingest
```

### Logs and Debugging

#### View Application Logs
```bash
./mvnw spring-boot:run
```

#### View Database Logs
```bash
docker-compose logs postgres
```

#### Enable Debug Logging
Add to `application.yaml`:
```yaml
logging:
  level:
    com.swiftbeard.product_recommender_engine: DEBUG
    org.springframework.ai: DEBUG
```

## 🏗️ Development Setup

### Running Tests
```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=ProductServiceTest

# Skip tests during build
./mvnw clean package -DskipTests
```

### Hot Reload Development
```bash
# Run with dev profile for hot reload
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Database Reset
```bash
# Stop and remove containers
docker-compose down

# Remove volumes (WARNING: This deletes all data)
docker-compose down -v

# Start fresh
./setup-database.sh
```

## 🌐 API Documentation

Once the application is running, you can explore the API:

### Core Endpoints

#### Product Management
- `GET /api/products` - List all products
- `GET /api/products/{id}` - Get product by ID
- `POST /api/products` - Create new product
- `PUT /api/products/{id}` - Update product
- `DELETE /api/products/{id}` - Delete product

#### Recommendations
- `GET /api/recommendations/similar/{productId}` - Similar products
- `GET /api/recommendations/search?query=...` - Search-based recommendations
- `POST /api/recommendations/personalized` - Personalized recommendations

#### AI-Powered Queries
- `POST /api/queries/ask` - Ask questions about products
- `POST /api/queries/recommend-with-explanation` - Get recommendations with explanations
- `POST /api/queries/compare-products` - Compare multiple products

### Example API Calls

See the main README.md for comprehensive API documentation with examples.

## 🚀 Production Deployment

### Environment Variables for Production
```bash
export OPENAI_API_KEY=your-production-api-key
export SPRING_DATASOURCE_URL=jdbc:postgresql://your-db-host:5432/product_recommender
export SPRING_DATASOURCE_USERNAME=your-db-user
export SPRING_DATASOURCE_PASSWORD=your-db-password
```

### Build Production JAR
```bash
./mvnw clean package -DskipTests
java -jar target/product-recommender-engine-0.0.1-SNAPSHOT.jar
```

### Docker Production Build
```bash
./mvnw spring-boot:build-image
docker run -p 8080:8080 -e OPENAI_API_KEY=your-key product-recommender-engine:0.0.1-SNAPSHOT
```

## 📞 Support

If you encounter any issues:

1. Check this troubleshooting guide
2. Review the application logs
3. Verify all prerequisites are installed
4. Ensure your OpenAI API key is valid and has sufficient credits
5. Check that Docker containers are running properly

## 🎉 Success!

Once everything is set up, you'll have a fully functional AI-powered product recommendation engine with:

- ✅ Semantic product search using OpenAI embeddings
- ✅ Vector similarity search with PGVector
- ✅ RAG-based question answering
- ✅ Multiple recommendation algorithms
- ✅ RESTful API for all operations
- ✅ Sample data for immediate testing

Happy coding! 🚀
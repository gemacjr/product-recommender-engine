# Smart Product Recommendation Engine

A comprehensive Spring Boot application powered by Spring AI and OpenAI that provides intelligent product recommendations using semantic search, vector embeddings, and RAG (Retrieval Augmented Generation) patterns.

## Features

- **Vector-Based Semantic Search**: Find products using natural language queries with PGVector
- **Product Embeddings**: Automatic generation of product embeddings using OpenAI's embedding models
- **RAG-Powered Q&A**: Answer customer questions using contextual product information
- **Smart Recommendations**: Multiple recommendation strategies including:
  - Similar products based on embeddings
  - Personalized recommendations
  - Complementary products
  - Budget and premium alternatives
  - Trending products
  - Diverse recommendations across categories
- **AI-Generated Content**: Create personalized product descriptions
- **RESTful API**: Comprehensive REST endpoints for all operations
- **Sample Data**: Pre-loaded product catalog for testing

## Technology Stack

- **Spring Boot 3.5.7**: Modern Java framework
- **Spring AI 1.0.3**: AI integration framework
- **OpenAI GPT-4**: Language model for generation tasks
- **OpenAI Embeddings**: text-embedding-3-small for vector representations
- **PostgreSQL + PGVector**: Vector database for semantic search
- **Spring Data JPA**: Data persistence
- **Lombok**: Reduce boilerplate code
- **Maven**: Dependency management

## Architecture

```
┌─────────────────┐
│  REST API       │
│  Controllers    │
└────────┬────────┘
         │
┌────────▼────────────────────────────────┐
│  Service Layer                          │
│  ├─ ProductService                      │
│  ├─ VectorStoreService                  │
│  ├─ RecommendationService               │
│  └─ RagService                          │
└────────┬────────────────────────────────┘
         │
┌────────▼────────┐       ┌──────────────┐
│  PostgreSQL     │       │  OpenAI API  │
│  + PGVector     │       │  - GPT-4     │
│                 │       │  - Embeddings│
└─────────────────┘       └──────────────┘
```

## Prerequisites

- Java 17 or higher
- Docker and Docker Compose
- OpenAI API key
- Maven 3.6+

## Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
cd product-recommender-engine
```

### 2. Set Up Environment Variables

Create a `.env` file or set the environment variable:

```bash
export OPENAI_API_KEY=your-openai-api-key-here
```

Or copy the example file:

```bash
cp .env.example .env
# Edit .env and add your OpenAI API key
```

### 3. Start PostgreSQL with PGVector

```bash
docker-compose up -d postgres
```

This will start:
- PostgreSQL 16 with PGVector extension on port 5432

Optional: Start pgAdmin for database management:
```bash
docker-compose --profile dev up -d
# Access pgAdmin at http://localhost:5050
# Email: admin@admin.com, Password: admin
```

### 4. Build the Application

```bash
./mvnw clean install
```

### 5. Run the Application

```bash
./mvnw spring-boot:run
```

Or with environment variable inline:

```bash
OPENAI_API_KEY=your-key-here ./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`

### 6. Verify Setup

Check application health:
```bash
curl http://localhost:8080/api/queries/health
```

## API Documentation

### Product Management

#### Get All Products
```bash
GET /api/products?page=0&size=10
```

#### Get Product by ID
```bash
GET /api/products/{id}
```

#### Search Products
```bash
GET /api/products/search?keyword=wireless&page=0&size=10
```

#### Get Products by Category
```bash
GET /api/products/category/ELECTRONICS?page=0&size=10
```

Categories: `ELECTRONICS`, `CLOTHING`, `HOME_GARDEN`, `SPORTS_OUTDOORS`, `BOOKS`, `TOYS_GAMES`, `BEAUTY_PERSONAL_CARE`, `FOOD_BEVERAGES`, `AUTOMOTIVE`, `HEALTH_WELLNESS`

#### Get Products by Price Range
```bash
GET /api/products/price-range?minPrice=50&maxPrice=500
```

#### Get Top-Rated Products
```bash
GET /api/products/top-rated?page=0&size=10
```

#### Create Product
```bash
POST /api/products
Content-Type: application/json

{
  "name": "Premium Wireless Earbuds",
  "description": "High-quality wireless earbuds with active noise cancellation",
  "category": "ELECTRONICS",
  "price": 199.99,
  "brand": "TechBrand",
  "sku": "TB-EARBUDS-001",
  "tags": ["wireless", "audio", "premium"],
  "features": ["ANC", "30-hour battery", "Water resistant"],
  "stockQuantity": 100,
  "rating": 4.5,
  "reviewCount": 250
}
```

#### Update Product
```bash
PUT /api/products/{id}
Content-Type: application/json

{
  "name": "Updated Product Name",
  "description": "Updated description",
  ...
}
```

#### Delete Product
```bash
DELETE /api/products/{id}
```

#### Generate Personalized Description
```bash
POST /api/products/{id}/personalized-description?userPreferences=fitness enthusiast
```

#### Ingest Products to Vector Store
```bash
POST /api/products/ingest
```

### Recommendations

#### Get Similar Products
```bash
GET /api/recommendations/similar/{productId}?limit=10
```

#### Search-Based Recommendations
```bash
GET /api/recommendations/search?query=noise canceling headphones for travel&limit=10
```

#### Personalized Recommendations
```bash
POST /api/recommendations/personalized
Content-Type: application/json

{
  "userPreferences": "tech enthusiast, loves premium audio, budgets $200-500",
  "limit": 10
}
```

#### Recommendations from Browsing History
```bash
POST /api/recommendations/from-history
Content-Type: application/json

{
  "viewedProductIds": [1, 3, 5, 7],
  "limit": 10
}
```

#### Complementary Products
```bash
GET /api/recommendations/complementary/{productId}?limit=5
```

#### Trending Products
```bash
GET /api/recommendations/trending?limit=10
```

#### Diverse Recommendations
```bash
GET /api/recommendations/diverse?userInterests=outdoor activities and fitness&limit=10
```

#### Budget Alternatives
```bash
GET /api/recommendations/budget-alternatives/{productId}?limit=5
```

#### Premium Alternatives
```bash
GET /api/recommendations/premium-alternatives/{productId}?limit=5
```

#### Category Recommendations
```bash
GET /api/recommendations/category/ELECTRONICS?userContext=gaming setup&limit=10
```

### RAG-Based Query Answering

#### Ask a Question
```bash
POST /api/queries/ask
Content-Type: application/json

{
  "query": "What are the best noise-canceling headphones for travel under $500?",
  "userPreferences": "long battery life, comfortable for long flights"
}
```

#### Get Recommendation with Explanation
```bash
POST /api/queries/recommend-with-explanation
Content-Type: application/json

{
  "query": "I need headphones for my daily commute",
  "userPreferences": "budget-friendly, good battery life, comfortable"
}
```

#### Compare Products
```bash
POST /api/queries/compare-products
Content-Type: application/json

[1, 2, 3]
```

#### Product FAQ
```bash
POST /api/queries/product-faq/{productId}
Content-Type: application/json

{
  "question": "Is this suitable for running?"
}
```

#### Personalized Shopping Suggestions
```bash
POST /api/queries/personalized-suggestions
Content-Type: application/json

{
  "userProfile": "fitness enthusiast, early 30s, values quality",
  "occasion": "marathon training"
}
```

#### Health Check
```bash
GET /api/queries/health
```

## Configuration

### Application Properties

Key configurations in `application.yaml`:

```yaml
spring:
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

application:
  product:
    default-limit: 10
    max-limit: 100
  recommendation:
    similarity-threshold: 0.7
    max-results: 20
  rag:
    context-window-size: 5
    temperature: 0.3
```

### Customization

- **Embedding Model**: Change `text-embedding-3-small` to `text-embedding-3-large` for higher accuracy (3072 dimensions)
- **Chat Model**: Use `gpt-3.5-turbo` for faster/cheaper responses
- **Similarity Threshold**: Adjust `similarity-threshold` (0-1) to control recommendation relevance
- **Context Window**: Increase `context-window-size` for more comprehensive RAG responses

## Sample Data

The application includes 12 pre-loaded sample products across various categories:

- Electronics (headphones, laptop, TV)
- Clothing (jeans, shoes)
- Home & Garden (vacuum, mixer)
- Sports & Outdoors (water bottle, exercise bike)
- Books, Beauty, Health & Wellness

Sample data is automatically loaded on first startup and ingested into the vector store.

## Development

### Project Structure

```
src/main/java/com/swiftbeard/product_recommender_engine/
├── config/              # Configuration classes
├── controller/          # REST controllers
├── dto/                 # Data Transfer Objects
├── model/              # JPA entities
├── repository/         # JPA repositories
└── service/            # Business logic
    ├── ProductService
    ├── VectorStoreService
    ├── RecommendationService
    └── RagService
```

### Key Components

- **VectorStoreService**: Manages product embeddings and semantic search
- **ProductService**: CRUD operations and AI-powered description generation
- **RecommendationService**: Multiple recommendation algorithms
- **RagService**: Retrieval Augmented Generation for Q&A

## Best Practices Implemented

1. **Vector Store Management**
   - Automatic embedding generation on product creation
   - Batch ingestion support
   - Update and delete synchronization

2. **Semantic Search**
   - HNSW indexing for fast approximate nearest neighbor search
   - Cosine similarity for relevance scoring
   - Configurable similarity thresholds

3. **RAG Pattern**
   - Context retrieval from vector store
   - Prompt engineering for accurate responses
   - Metadata enrichment for better context

4. **Error Handling**
   - Comprehensive exception handling
   - Graceful degradation if vector store fails
   - Detailed logging

5. **Performance**
   - Connection pooling
   - Pagination for large result sets
   - Lazy loading for JPA entities

## Troubleshooting

### Common Issues

**Issue**: Application fails to start with database connection error
```bash
# Solution: Ensure PostgreSQL is running
docker-compose ps
docker-compose up -d postgres
```

**Issue**: Vector store initialization fails
```bash
# Solution: Check if PGVector extension is installed
docker-compose exec postgres psql -U postgres -d product_recommender -c "SELECT * FROM pg_extension WHERE extname='vector';"
```

**Issue**: OpenAI API errors
```bash
# Solution: Verify API key is set correctly
echo $OPENAI_API_KEY

# Check application logs for specific error
./mvnw spring-boot:run | grep "OpenAI"
```

**Issue**: Empty recommendations
```bash
# Solution: Manually trigger vector store ingestion
curl -X POST http://localhost:8080/api/products/ingest
```

## Testing

Run the full test suite:

```bash
./mvnw test
```

## Production Deployment

### Environment Variables

Required for production:
- `OPENAI_API_KEY`: Your OpenAI API key
- `SPRING_DATASOURCE_URL`: Production database URL
- `SPRING_DATASOURCE_USERNAME`: Database username
- `SPRING_DATASOURCE_PASSWORD`: Database password

### Docker Deployment

Build Docker image:
```bash
./mvnw spring-boot:build-image
```

### Performance Tuning

1. Adjust JVM heap size: `-Xmx2g -Xms1g`
2. Configure connection pool size
3. Enable database query caching
4. Consider read replicas for scaling

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License.

## Support

For issues, questions, or contributions, please open an issue on GitHub.

## Acknowledgments

- Spring AI team for the excellent AI integration framework
- OpenAI for powerful language models and embeddings
- PGVector for enabling vector similarity search in PostgreSQL

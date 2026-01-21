# 🍔 Real-Time Food Delivery & Ghost Kitchen Management Platform

[![Build Status](https://github.com/anasshatim1997/food-delivery-platform/workflows/CI/badge.svg)](https://github.com/anasshatim1997/food-delivery-platform/actions)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java Version](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)

A comprehensive, production-ready food delivery platform built with microservices architecture, featuring real-time order tracking, ghost kitchen management, and multi-platform support (Web & Mobile). Developed by Anass Hatim, a Full Stack Java Developer with expertise in Spring Boot, React.js, and Next.js.

## 📋 Table of Contents

- [Features](#-features)
- [Architecture](#-architecture)
- [Technology Stack](#-technology-stack)
- [Prerequisites](#-prerequisites)
- [Quick Start](#-quick-start)
- [Project Structure](#-project-structure)
- [Services Overview](#-services-overview)
- [API Documentation](#-api-documentation)
- [Development](#-development)
- [Testing](#-testing)
- [Deployment](#-deployment)
- [Contributing](#-contributing)
- [License](#-license)

## ✨ Features

### For Customers
- 🔐 User registration and authentication with JWT
- 🗺️ Location-based restaurant search
- 🍕 Browse restaurant menus with categories, variants, and add-ons
- 🛒 Shopping cart with customization options
- 💳 Multiple payment methods (Cash, Card, Wallet)
- 📍 Real-time order tracking with driver location
- ⭐ Rate and review restaurants and drivers
- 💰 Wallet system for easy payments
- 🎟️ Apply promotional codes and discounts

### For Drivers
- 📱 Driver registration with document verification
- 📍 Real-time location tracking
- 📦 Order assignment based on proximity
- 💵 Earnings tracking and payouts
- 🚗 Vehicle and license management
- ⭐ Rating system

### For Restaurant Owners
- 🏪 Restaurant registration and verification
- 📝 Complete menu management (categories, items, variants, add-ons)
- 📊 Real-time order queue with WebSocket updates
- 📈 Analytics dashboard (orders, revenue, popular items)
- ⏰ Opening hours management
- 🔔 Instant notifications for new orders

### For Administrators
- 👥 User management (customers, drivers, restaurant owners)
- ✅ Restaurant and driver verification
- 📊 System-wide analytics and reporting
- 🛡️ Platform monitoring and management

## 🏗️ Architecture

This project follows a **microservices architecture** with the following design patterns:

- **Domain-Driven Design (DDD)**: Clear bounded contexts for each service
- **Event-Driven Architecture**: Asynchronous communication via RabbitMQ/Kafka
- **CQRS**: Command Query Responsibility Segregation where applicable
- **API Gateway Pattern**: Centralized routing and authentication
- **Database per Service**: Each microservice has its own PostgreSQL database
- **Circuit Breaker**: Resilience4j for fault tolerance

### System Architecture Diagram

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Mobile    │     │     Web     │     │    Admin    │
│    Apps     │     │  Dashboard  │     │    Panel    │
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                    │
       └───────────────────┴────────────────────┘
                           │
                    ┌──────▼──────┐
                    │             │
                    │ API Gateway │
                    │             │
                    └──────┬──────┘
                           │
        ┏━━━━━━━━━━━━━━━━━━┻━━━━━━━━━━━━━━━━━━┓
        ┃                                      ┃
┌───────▼────────┐  ┌────────────┐  ┌─────────▼────────┐
│  User Service  │  │  Restaurant│  │  Order Service   │
│                │  │  Service   │  │                  │
└───────┬────────┘  └─────┬──────┘  └─────────┬────────┘
        │                 │                    │
┌───────▼────────┐  ┌────▼──────┐  ┌──────────▼───────┐
│ Payment Service│  │ Delivery  │  │ Notification     │
│                │  │ Service   │  │ Service          │
└────────────────┘  └───────────┘  └──────────────────┘
        │                 │                    │
        └─────────────────┴────────────────────┘
                          │
                ┌─────────▼─────────┐
                │  Message Queue    │
                │  (RabbitMQ/Kafka) │
                └───────────────────┘
```

## 🛠️ Technology Stack

### Backend
- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Database**: PostgreSQL 15.5
- **Cache**: Redis 7.2
- **Message Queue**: RabbitMQ 3.12 / Apache Kafka 7.5
- **API Gateway**: Spring Cloud Gateway 4.1.0
- **Authentication**: JWT (JSON Web Tokens)
- **Database Migration**: Flyway 10.4.1
- **ORM**: Hibernate / Spring Data JPA
- **Testing**: JUnit 5, Mockito, Testcontainers

### Frontend Web
- **Framework**: React 18.2.0
- **Language**: TypeScript 5.3.3
- **Styling**: Tailwind CSS 3.4.0
- **State Management**: Redux Toolkit 2.0.1
- **Build Tool**: Vite 5.0.8
- **Charts**: Recharts 2.10.3

### Mobile
- **Framework**: React Native 0.73.1
- **Language**: TypeScript
- **State Management**: Redux Toolkit
- **Maps**: React Native Maps
- **Push Notifications**: Firebase Cloud Messaging

### DevOps & Infrastructure
- **Containerization**: Docker 24.0.7
- **Orchestration**: Kubernetes 1.28
- **CI/CD**: GitHub Actions
- **Monitoring**: Prometheus, Grafana
- **Tracing**: Zipkin
- **Logging**: ELK Stack (Elasticsearch, Logstash, Kibana)

## 📦 Prerequisites

Before you begin, ensure you have the following installed:

- **Java 17** or higher ([Download](https://adoptium.net/))
- **Maven 3.9+** ([Download](https://maven.apache.org/download.cgi))
- **Node.js 20.10+** and npm ([Download](https://nodejs.org/))
- **Docker 24+** and Docker Compose ([Download](https://www.docker.com/products/docker-desktop))
- **Git** ([Download](https://git-scm.com/downloads))
- **PostgreSQL 15+** (optional, can use Docker)
- **Redis 7+** (optional, can use Docker)

### For Mobile Development (Optional)
- **React Native CLI**: `npm install -g react-native-cli`
- **Android Studio** (for Android development)
- **Xcode** (for iOS development, macOS only)

## 🚀 Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/anasshatim1997/food-delivery-platform.git
cd food-delivery-platform
```

### 2. Set Up Environment Variables

Create a `.env` file in the root directory:

```bash
cp .env.example .env
```

Edit the `.env` file with your configuration:

```env
# Database
DB_HOST=localhost
DB_PORT=5432
DB_USER=postgres
DB_PASSWORD=your_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT
JWT_SECRET=your_super_secret_jwt_key_here_change_in_production
JWT_EXPIRATION=86400000

# Firebase (for push notifications)
FIREBASE_API_KEY=your_firebase_api_key
FIREBASE_PROJECT_ID=your_firebase_project_id

# Payment Gateway (Stripe)
STRIPE_SECRET_KEY=sk_test_your_stripe_key
STRIPE_WEBHOOK_SECRET=whsec_your_webhook_secret

# Email
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your_email@gmail.com
SMTP_PASSWORD=your_app_password
```

### 3. Start Infrastructure Services with Docker

```bash
cd infrastructure/docker
docker-compose up -d
```

This will start:
- PostgreSQL (port 5432)
- Redis (port 6379)
- RabbitMQ (port 5672, Management UI: 15672)
- Zipkin (port 9411)

### 4. Build All Services

```bash
# Build parent project
mvn clean install

# Or build individual services
cd services/user-service
mvn clean package
```

### 5. Run Services

You can run services individually:

```bash
# Terminal 1 - API Gateway
cd services/api-gateway
mvn spring-boot:run

# Terminal 2 - User Service
cd services/user-service
mvn spring-boot:run

# Terminal 3 - Restaurant Service
cd services/restaurant-service
mvn spring-boot:run

# Terminal 4 - Order Service
cd services/order-service
mvn spring-boot:run

# Terminal 5 - Payment Service
cd services/payment-service
mvn spring-boot:run

# Terminal 6 - Delivery Service
cd services/delivery-service
mvn spring-boot:run

# Terminal 7 - Notification Service
cd services/notification-service
mvn spring-boot:run
```

Or use Docker Compose to run all services:

```bash
docker-compose -f infrastructure/docker/docker-compose.dev.yml up
```

### 6. Run Web Dashboard

```bash
cd web/restaurant-dashboard
npm install
npm run dev
```

Access the dashboard at: `http://localhost:5173`

### 7. Run Mobile App (Optional)

```bash
cd mobile/customer-app
npm install

# For iOS
npx react-native run-ios

# For Android
npx react-native run-android
```

## 📂 Project Structure

See [STRUCTURE.md](docs/STRUCTURE.md) for detailed folder structure.

```
food-delivery-platform/
├── services/           # Backend microservices
├── mobile/            # Mobile applications
├── web/               # Web applications
├── infrastructure/    # Docker, Kubernetes configs
├── docs/              # Documentation
└── scripts/           # Utility scripts
```

## 🔧 Services Overview

### API Gateway (Port: 8080)
- Routes requests to microservices
- Handles authentication and authorization
- Rate limiting and request logging
- CORS configuration

### User Service (Port: 8081)
- User registration and authentication
- Customer, driver, and restaurant owner management
- Address management
- JWT token generation and validation

### Restaurant Service (Port: 8082)
- Restaurant registration and verification
- Menu management (categories, items, variants, add-ons)
- Inventory tracking
- Geospatial restaurant search

### Order Service (Port: 8083)
- Order creation and management
- Order state machine
- Order history and details
- Review and rating system

### Payment Service (Port: 8084)
- Payment processing (Cash, Card, Wallet)
- Wallet management
- Refund processing
- Transaction history

### Delivery Service (Port: 8085)
- Driver assignment algorithm
- Real-time location tracking
- Driver earnings calculation
- Delivery status management

### Notification Service (Port: 8086)
- Push notifications (Firebase)
- Email notifications
- SMS notifications (optional)
- Notification templates

### Analytics Service (Port: 8087)
- Data aggregation
- Restaurant analytics
- Driver analytics
- System-wide metrics

## 📚 API Documentation

### Swagger UI
Access Swagger documentation for each service:

- **API Gateway**: http://localhost:8080/swagger-ui.html
- **User Service**: http://localhost:8081/swagger-ui.html
- **Restaurant Service**: http://localhost:8082/swagger-ui.html
- **Order Service**: http://localhost:8083/swagger-ui.html
- **Payment Service**: http://localhost:8084/swagger-ui.html
- **Delivery Service**: http://localhost:8085/swagger-ui.html

### Postman Collection
Import the Postman collection from `docs/api/Food-Delivery-Platform.postman_collection.json`

### Example API Calls

#### 1. Register a Customer
```bash
POST http://localhost:8080/api/users/register/customer
Content-Type: application/json

{
  "email": "customer@example.com",
  "password": "SecurePass123!",
  "phone": "+1234567890",
  "firstName": "John",
  "lastName": "Doe"
}
```

#### 2. Login
```bash
POST http://localhost:8080/api/users/login
Content-Type: application/json

{
  "email": "customer@example.com",
  "password": "SecurePass123!"
}
```

#### 3. Search Restaurants
```bash
GET http://localhost:8080/api/restaurants/search?lat=33.5731&lng=-7.5898&radius=5&isOpen=true
Authorization: Bearer <your_jwt_token>
```

#### 4. Create Order
```bash
POST http://localhost:8080/api/orders
Authorization: Bearer <your_jwt_token>
Content-Type: application/json

{
  "restaurantId": "uuid-here",
  "deliveryAddressId": "uuid-here",
  "items": [
    {
      "menuItemId": "uuid-here",
      "variantId": "uuid-here",
      "quantity": 2,
      "addons": [
        {
          "addonId": "uuid-here",
          "quantity": 1
        }
      ]
    }
  ],
  "specialInstructions": "No onions please"
}
```

## 💻 Development

### Branch Naming Convention
```
feature/TICKET-ID-short-description
bugfix/TICKET-ID-short-description
hotfix/TICKET-ID-short-description
```

Examples:
- `feature/USER-1-customer-registration`
- `bugfix/ORDER-15-payment-calculation`
- `hotfix/SECURITY-3-jwt-validation`

### Commit Message Format
```
[TICKET-ID] Your commit message

Example:
[USER-1] Add customer registration endpoint
[ORDER-2] Implement order state machine validation
```

### Code Style
- Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- Use **Lombok** to reduce boilerplate
- Write **JavaDoc** for public methods
- Maximum line length: 120 characters

### Running Tests

#### Unit Tests
```bash
mvn test
```

#### Integration Tests
```bash
mvn verify -P integration-tests
```

#### Generate Coverage Report
```bash
mvn clean verify
# Report available at: target/site/jacoco/index.html
```

### Database Migrations

Create a new migration:
```bash
# Create migration file in src/main/resources/db/migration/
# Format: V{version}__{description}.sql
# Example: V5__Add_Rating_Column_To_Driver.sql
```

Run migrations:
```bash
mvn flyway:migrate
```

Rollback (if needed):
```bash
mvn flyway:undo
```

## 🧪 Testing

### Test Coverage Requirements
- Unit Tests: **> 80%** coverage
- Integration Tests: All REST endpoints
- E2E Tests: Critical user flows

### Testing Tools
- **JUnit 5**: Unit testing framework
- **Mockito**: Mocking framework
- **Testcontainers**: Integration testing with real databases
- **REST Assured**: API testing
- **WireMock**: HTTP mocking

### Running Different Test Suites

```bash
# Unit tests only
mvn test

# Integration tests only
mvn test -P integration-tests

# E2E tests
mvn test -P e2e-tests

# Load tests (using Gatling)
mvn gatling:test
```

## 🚀 Deployment

### Docker Deployment

#### Build Docker Images
```bash
# Build all service images
docker-compose -f infrastructure/docker/docker-compose.prod.yml build
```

#### Run in Production Mode
```bash
docker-compose -f infrastructure/docker/docker-compose.prod.yml up -d
```

### Kubernetes Deployment

#### Prerequisites
- Kubernetes cluster (GKE, EKS, AKS, or local with Minikube)
- `kubectl` configured
- `helm` installed

#### Deploy to Kubernetes

```bash
# Create namespace
kubectl create namespace food-delivery

# Apply configurations
kubectl apply -f infrastructure/k8s/base/

# Deploy services
kubectl apply -f infrastructure/k8s/services/

# Deploy databases
kubectl apply -f infrastructure/k8s/databases/

# Check deployment status
kubectl get pods -n food-delivery
```

#### Using Helm (Recommended)
```bash
# Install with Helm
helm install food-delivery ./helm/food-delivery-chart -n food-delivery

# Upgrade
helm upgrade food-delivery ./helm/food-delivery-chart -n food-delivery

# Rollback
helm rollback food-delivery -n food-delivery
```

### CI/CD Pipeline

The project uses **GitHub Actions** for CI/CD:

1. **On Pull Request**: Run tests, code quality checks
2. **On Merge to Main**: Build, test, and deploy to staging
3. **On Release Tag**: Deploy to production (manual approval required)

Pipeline stages:
- ✅ Lint and code quality (SonarQube)
- ✅ Unit tests
- ✅ Integration tests
- ✅ Build Docker images
- ✅ Push to container registry
- ✅ Deploy to Kubernetes
- ✅ Run smoke tests

## 📊 Monitoring & Observability

### Prometheus Metrics
Access Prometheus: `http://localhost:9090`

Key metrics:
- Request rate (requests/second)
- Error rate (errors/total requests)
- Response time (p50, p95, p99)
- Database connection pool usage

### Grafana Dashboards
Access Grafana: `http://localhost:3000` (admin/admin)

Pre-configured dashboards:
- System Overview
- Service Health
- Database Performance
- API Performance

### Distributed Tracing
Access Zipkin: `http://localhost:9411`

### Logging
Access Kibana: `http://localhost:5601`

All services use structured logging (JSON format) for easy parsing.

## 🤝 Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details.

### Development Workflow

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m '[TICKET-ID] Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request to [anasshatim1997/food-delivery-platform](https://github.com/anasshatim1997/food-delivery-platform)

### Code Review Process

All PRs require:
- ✅ All tests passing
- ✅ Code coverage > 80%
- ✅ No SonarQube critical issues
- ✅ Approval from maintainer
- ✅ Documentation updated

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👥 Team

- **Project Lead & Full Stack Developer**: Anass Hatim
    - GitHub: [@anasshatim1997](https://github.com/anasshatim1997)
    - LinkedIn: [Anass Hatim](https://www.linkedin.com/in/anass-hatim-376710175/)
    - Email: anass1997hatim@gmail.com
    - Location: Casablanca, Morocco

## 📞 Support

- **Documentation**: [docs/](docs/)
- **Issues**: [GitHub Issues](https://github.com/anasshatim1997/food-delivery-platform/issues)
- **Discussions**: [GitHub Discussions](https://github.com/anasshatim1997/food-delivery-platform/discussions)
- **Email**: anass1997hatim@gmail.com

## 🗺️ Roadmap

### Phase 1 (Completed) ✅
- Core microservices
- Basic order flow
- Mobile apps
- Web dashboards

### Phase 2 (In Progress) 🚧
- Advanced analytics
- Machine learning for recommendations
- Multi-language support
- Dark mode

### Phase 3 (Planned) 📋
- Subscription model
- Loyalty program
- Advanced inventory management
- Restaurant chain management

## 🙏 Acknowledgments

- Spring Boot and Spring Cloud teams
- React and React Native communities
- All open-source contributors

---

**Built with ❤️ by Anass Hatim**

**Ingénieur Logiciel Full Stack Java | Spring Boot • React.js • Next.js • PostgreSQL • Docker**

**Star ⭐ this repository if you find it helpful!**

**Connect with me:**
- GitHub: [@anasshatim1997](https://github.com/anasshatim1997)
- LinkedIn: [Anass Hatim](https://www.linkedin.com/in/anass-hatim-376710175/)
- Email: anass1997hatim@gmail.com
- Location: Casablanca, Morocco
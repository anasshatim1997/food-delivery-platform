# Food Delivery Platform Microservices

A comprehensive food delivery platform built with Spring Boot microservices architecture, handling users, restaurants, orders, payments, promotions, and notifications.

---

## 👨‍💻 Author

**Anass Hatim**
- GitHub: [@anasshatim1997](https://github.com/anasshatim1997)
- Email: anass1997hatim@gmail.com

---

## 📋 Project Overview

This project is a **Food Delivery Platform** built using **Spring Boot 4.0.0** microservices architecture. The platform is designed to handle various aspects of a food delivery system, including users, restaurants, orders, payments, promotions, and notifications.

---

## 🏗️ Microservices Architecture

| Service | Responsibilities |
|---------|-----------------|
| **User Service** | Manages user registration, authentication, and profile data |
| **Restaurant Service** | Handles restaurant information, menu items, and caching frequently accessed data |
| **Order Service** | Processes orders, interacts with restaurants and payments |
| **Payment Service** | Handles Stripe/PayPal payments and transactions |
| **Promotion Service** | Manages discounts, coupons, and promotions |
| **Notification Service** | Sends notifications via email, SMS (Twilio), and Firebase push notifications |
| **Analytics Service** | Tracks metrics, reports, and analytics for the platform |

---

## 🔧 Key Dependencies

- **Spring Boot Starters:** webmvc, data-jpa, validation, actuator, cache, flyway, kafka, mail, devtools, security
- **Database:** PostgreSQL, Hibernate spatial for geospatial operations
- **Messaging & Communication:** Spring Cloud OpenFeign, Kafka
- **Notifications:** Firebase Admin SDK, Twilio SDK
- **Object Mapping:** MapStruct
- **Testing:** Spring Boot Starter Test, JUnit 5

---

## 📁 Project Structure

```
├── User-Service
├── Restaurant-Service
├── Order-Service
├── Payment-Service
├── Promotion-Service
├── Notification-Service
├── Analytics-Service
└── README.md
```

Each service follows a standard **Spring Boot layered architecture**:

```
src/main/java
└── com.[service_name]
    ├── controller
    ├── service
    ├── repository
    ├── dto
    ├── model
    ├── config
    └── exception
```

---

## 🚀 Setup Instructions

### Prerequisites

- Java 17
- Maven 3.8+
- PostgreSQL database
- Kafka (optional if testing messaging)
- Stripe/PayPal accounts for payment integration (optional)
- Firebase account (optional for notifications)
- Twilio account (optional for SMS)

### Installation Steps

1. **Clone the repository:**
   ```bash
   git clone https://github.com/anasshatim1997/food-delivery-platform.git
   cd food-delivery-platform
   ```

2. **Configure PostgreSQL** for each service in `application.properties` or `application.yml`

3. **Run Flyway migrations** (handled automatically at startup)

4. **Build and run each microservice:**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

5. **Test endpoints** via Postman, Swagger, or frontend integration

---

## 🌿 Git Branch Strategy

- **`main`**: Stable production-ready code
- **`develop`**: Integration branch for all features
- **`feature/[feature-name]`**: Feature-specific branches
- **`bugfix/[bug-name]`**: Bugfix branches
- **`release/[version]`**: Release candidate branches

### Example Workflow

```bash
git checkout -b feature/user-authentication
# Implement your feature
git add .
git commit -m "Implemented user authentication"
git push origin feature/user-authentication
```

Merge to `develop`, then later to `main` when stable.

---

## 💾 Caching Strategy

- Use `@Cacheable` for data that rarely changes (e.g., restaurant menus, promotions)
- Avoid caching frequently updated data like order status or live notifications

---

## 💳 Payment Integration

- Stripe or PayPal SDK used for learning purposes
- Local testing is possible without actual charges

---

## 📧 Notification System

- **Email**: Spring Boot Mail
- **SMS**: Twilio SDK
- **Push Notifications**: Firebase Admin SDK

---

## 🔄 Microservices Communication

- **Feign Clients** for internal service-to-service calls
- **Kafka** for event-driven asynchronous communication (e.g., order updates, notifications)

---

## 🧪 Testing

- Unit and integration tests included using Spring Boot Test
- Each service has separate test packages

---

## 📄 License

This project is open source and available for learning and demonstration purposes.

---

## 📞 Contact

**Anass Hatim**
- Email: anass1997hatim@gmail.com
- GitHub: [github.com/anasshatim1997](https://github.com/anasshatim1997)
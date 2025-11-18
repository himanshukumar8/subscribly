## 🚀 Features

### 🔐 Authentication & Security
- JWT Authentication (Login, Register)
- BCrypt password hashing
- Role-based access control (USER / ADMIN)
- Secured private endpoints
- Token validation via custom JwtAuthFilter

### 👤 User Module
- Fetch logged-in user profile (`/api/user/me`)
- Role management using `ROLE_USER` and `ROLE_ADMIN`
- Admin can view all users (optional)

### 📦 Subscription Management
Users can:
- Create new subscription
- View all own subscriptions
- Cancel subscription
- Change billing cycle (MONTHLY / YEARLY)

Admin can:
- View all subscriptions
- Update subscription status (ACTIVE / EXPIRED / CANCELED)

### 💳 Payment System
- Manual payment processing
- Payment linked to subscription
- Auto-renew support
- Retry logic for failed payments

Payment statuses:
- `PENDING`
- `SUCCESS`
- `FAILED`
- `RETRYING`
- `CANCELLED`
- `AUTO_RENEW`

### ⏱️ Auto-Renew Scheduler (CRON)
A daily scheduled job:
- Checks expired subscriptions
- Automatically creates renewal payments
- Retries failed payments with backoff logic
- Updates subscription next billing date

### 🗄️ Clean Architecture
- Modular package structure
- DTO layer for requests
- Service layer for business logic
- Repository layer using Spring Data JPA
- Enum-based models for subscription plans, billing cycles, and payment status

### 🛡️ Error Handling
- Centralized exceptions
- Descriptive error messages
- Authorization & validation checks






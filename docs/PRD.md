# NexCart — Product Requirements Document

## 1. Product Overview

NexCart is a full-stack electronics e-commerce application. Customers can browse products, manage shopping data, place orders, and complete Razorpay test-mode payments. Administrators manage catalog and operational data through a protected workspace.

The application also includes RecoverAI, a bounded payment-recovery workflow for failed payments. It records recovery decisions, actions, outcomes, and audit events while preventing duplicate or unsafe recovery actions.

This document describes only features implemented in the repository.

## 2. Users and Roles

| Role | Capabilities |
| --- | --- |
| Visitor | Browse products, categories, and brands; register or log in. |
| Customer | Manage profile, addresses, cart, wishlist, orders, payments, coupons, and reviews. |
| Administrator | Manage platform data and inspect/run RecoverAI workflows. |

## 3. Functional Requirements

### Authentication and authorization

- Users can register and log in.
- The backend issues JWT-based authentication for protected requests.
- Passwords are encrypted using BCrypt.
- Customer APIs require the `CUSTOMER` role.
- Administrative APIs under `/api/admin/**` require the `ADMIN` role.

### Catalog

- Customers can view products, brands, and categories.
- Administrators can create, update, and delete products, brands, and categories.
- Product pages display product information and reviews.

### Shopping

- Customers can add, update, and remove cart items.
- Customers can add and remove wishlist items.
- Customers can maintain delivery addresses.
- Customers can apply coupons during ordering.

### Orders and payments

- Customers can place an order from their cart.
- NexCart creates Razorpay test-mode payment orders for checkout.
- The backend verifies payment completion before confirming an order.
- Customers can view their orders and order details.
- Customers can cancel eligible orders.
- Razorpay webhook requests are signature-verified before processing payment events.

### Administration

- Administrators can access the admin workspace.
- The workspace supports management of products, categories, brands, users, addresses, coupons, reviews, payments, and orders.
- Administrators can inspect recovery metrics and recovery-case detail.

### RecoverAI

- A payment failure creates a persistent recovery case.
- A deterministic fallback decision service selects `RETRY_PAYMENT`, `CREATE_PAYMENT_LINK`, `SEND_RECOVERY_MESSAGE`, or `NO_ACTION`.
- The service records decision source, reason, confidence, risk level, probability, expected recovery amount, actions, and audit events.
- The backend blocks recovery actions for terminal cases: `RECOVERED`, `CUSTOMER_CANCELLED`, `EXHAUSTED`, `FAILED`, and `NO_ACTION`.
- Equivalent completed or pending retry/payment-link actions are skipped to prevent duplicates.
- Customer cancellation stops recovery and attempts to cancel an existing Razorpay payment link.
- REAL recovery cases use Razorpay test-mode payment links and resolve through signed webhook events.
- SIMULATED cases use isolated demo links and outcomes; they never call Razorpay and do not contribute to real recovered revenue.

## 4. Primary User Flows

### Customer purchase

```text
Browse products → Add to cart → Place order → Razorpay checkout
→ Verify payment → Order confirmed → View order history
```

### Failed-payment recovery

```text
Payment failure → Recovery case → Decision → Guardrail
→ Bounded action → Payment outcome → Recovered or terminal state
```

### Admin operations

```text
Admin login → Admin workspace → Manage platform data
or inspect RecoverAI cases, actions, and audit trail
```

## 5. Non-Functional Requirements

- REST APIs are implemented with Spring Boot.
- Data is persisted in MySQL through Spring Data JPA and Hibernate.
- Protected API access is enforced by Spring Security and JWT authentication.
- Frontend routes use protected and admin-only route guards.
- The system provides OpenAPI/Swagger documentation.
- Recovery operations are transactional and auditable.

## 6. System Boundaries

NexCart currently integrates with:

- **MySQL** for application and recovery data.
- **Razorpay Test Mode** for payment checkout, payment links, and webhooks.

RecoverAI does not independently change order values, payment amounts, user permissions, or product inventory. Its available actions are restricted and verified in the backend.

## 7. Success Criteria

The current implementation is considered operational when:

- Customers can complete the catalog-to-order payment journey.
- Authentication and role-based authorization protect relevant APIs and routes.
- Administrators can manage application data.
- Payment failures create visible recovery cases.
- RecoverAI guards terminal states, prevents duplicate equivalent actions, and preserves a clear audit trail.
- Simulated recovery outcomes remain separate from real Razorpay activity and real revenue metrics.

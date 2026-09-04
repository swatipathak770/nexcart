NexCart + RecoverAI

NexCart is a full-stack, role-based e-commerce platform built with
React, Spring Boot, MySQL, JWT/Spring Security, and Razorpay Test
Mode.

RecoverAI extends NexCart with an AI Revenue Recovery layer for
Track 3 of the Razorpay Buildathon. It detects failed-payment revenue at
risk, analyzes the situation, recommends a bounded recovery action,
executes it safely, reaches the customer, observes the payment result,
and measures recovered revenue.

Tagline: Recover revenue before it's lost.

Key Features

NexCart E-commerce

Customer registration/login with JWT authentication

BCrypt password hashing and role-based access

Product browsing, search, filter and sort

Categories and brands

Cart and wishlist

Saved addresses and customer profile

Coupons

Order management

Razorpay Test Mode checkout

Admin product/category/brand/coupon management

Responsive React UI connected to Spring Boot REST APIs

RecoverAI

Failed-payment detection and recovery cases

Revenue-at-risk and recovered-revenue metrics

Recovery probability, confidence, risk level and expected recovery

Bounded actions:

RETRY_PAYMENT

CREATE_PAYMENT_LINK

SEND_RECOVERY_MESSAGE (if enabled)

NO_ACTION

Customer in-app notification when a recovery payment link is
available

Secure Razorpay Test Mode Payment Links for real recovery

Customer-side Complete Payment Securely flow

Signed Razorpay webhook processing

Recovery audit trail

Real vs simulated recovery separation

Simulation mode for repeatable demonstrations

Backend guardrails and idempotency

Cancellation and terminal-state protection

Architecture

flowchart TD
    U[Customer] --> FE[React / NexCart UI]
    A[Admin] --> FE

    FE --> API[Spring Boot REST API]
    API --> SEC[Spring Security + JWT]
    API --> DB[(MySQL)]

    API --> REC[RecoverAI Engine]
    REC --> DEC[AI Decision / Deterministic Fallback]
    DEC --> G[Guardrails + Idempotency]
    G --> ACT[Bounded Recovery Action]

    ACT -->|Retry| CHK[NexCart Secure Checkout]
    ACT -->|Create Payment Link| RP[Razorpay Test Mode]
    ACT -->|Notification| N[Customer Notification]

    U --> N
    U --> CHK
    U --> RP

    RP --> WH[Signed Razorpay Webhook]
    WH --> REC

    REC --> AUD[Recovery Audit Trail]
    REC --> MET[Recovery Metrics]

    SIM[Simulation Engine] --> REC
    SIM -.->|No real gateway transaction| MET

Recovery lifecycle

PAYMENT FAILED
      ↓
RECOVERY CASE CREATED
      ↓
ANALYZE
      ↓
DECIDE
      ↓
GUARDRAIL CHECK
      ↓
BOUNDED ACTION
      ↓
CUSTOMER NOTIFIED
      ↓
PAYMENT / RESULT OBSERVED
      ↓
RECOVERED / FAILED / EXHAUSTED / NO_ACTION
      ↓
METRICS + AUDIT TRAIL

REAL Recovery Flow

sequenceDiagram
    participant Customer
    participant NexCart
    participant RecoverAI
    participant Razorpay
    participant Webhook

    Customer->>NexCart: Checkout
    NexCart->>Razorpay: Test payment
    Razorpay-->>NexCart: Payment failed
    NexCart->>RecoverAI: Create recovery case
    RecoverAI->>RecoverAI: Analyze + decide
    RecoverAI->>RecoverAI: Apply guardrails
    RecoverAI->>Razorpay: Create test-mode Payment Link
    RecoverAI-->>Customer: Recovery notification
    Customer->>Razorpay: Complete payment
    Razorpay->>Webhook: payment_link.paid
    Webhook->>RecoverAI: Verify signature + resolve case
    RecoverAI->>NexCart: Update order/payment state
    RecoverAI->>RecoverAI: RECOVERED + audit + metrics

A real recovered amount is counted only in REAL recovery metrics.

SIMULATED Recovery Flow

Simulation is isolated from Razorpay and real financial metrics.

Admin creates or runs a simulated scenario.

RecoverAI analyzes the synthetic case using the same decision and
guardrail logic.

Executing CREATE_PAYMENT_LINK creates a simulated:// demo link
only.

No Razorpay transaction, webhook, customer payment, or real gateway
request is created.

The simulation records events such as:

SIMULATED_ACTION_PENDING

SIMULATED_PAYMENT_LINK_CREATED

SIMULATED_PAYMENT_SUCCESSFUL

SIMULATED_RECOVERY_COMPLETED

Simulated recovered amounts are clearly labeled and never
contribute to REAL recovered revenue.

Dashboard modes: - REAL --- actual NexCart/Razorpay Test Mode
recovery cases - SIMULATED --- synthetic demo cases - ALL ---
both, with real and simulated metrics clearly separated

AI Decision and Guardrails

RecoverAI considers payment context such as: - Payment amount - Failure
reason - Earlier recovery attempts - Previous action outcomes - Recovery
limits and order state

The decision returns: - Recommended action - Recovery probability -
Expected recovery amount - Confidence - Risk level - Decision source -
Explanation/reason

Expected recovery:

Expected Recovery = Payment Amount × Recovery Probability

If the AI provider is unavailable, the system uses a clearly labeled
deterministic fallback to produce a safe bounded recommendation.

Guardrails

Enforced on the backend, not only in the UI:

RECOVERED, CUSTOMER_CANCELLED, EXHAUSTED, FAILED, and
NO_ACTION cases cannot execute another action.

Recovery attempts are capped by RECOVERY_MAX_ATTEMPTS (default:
3).

Equivalent pending/completed retry or payment-link actions are not
duplicated.

An active real Payment Link prevents unnecessary duplicate links.

Cancelled orders become CUSTOMER_CANCELLED and recovery stops.

Associated Razorpay Payment Links are cancelled when supported.

Customer ownership is checked before exposing recovery payment
information.

Material recovery events are stored in the audit trail.

Razorpay webhook signatures are verified before accepting events.

Recovery Endpoints

All /api/admin/recovery/* endpoints require the ADMIN role.

Method                  Endpoint                                   Purpose

GET                     /api/admin/recovery/metrics              Recovery metrics
(ALL, REAL,
SIMULATED)

GET                     /api/admin/recovery/cases                List recovery cases

GET                     /api/admin/recovery/cases/{id}           Case detail, actions
and audit trail

POST                    /api/admin/recovery/cases/{id}/analyze   Analyze/re-analyze a
non-terminal case

POST                    /api/admin/recovery/cases/{id}/execute   Execute approved
bounded action

POST                    /api/admin/recovery/simulate             Create an isolated demo
case

The Razorpay webhook is authenticated through Razorpay signature
verification, not customer/admin JWT authentication.

Tech Stack

Layer      Technology

Frontend   React 19, Vite, Tailwind CSS
Backend    Spring Boot 3, Java 21
Security   Spring Security, JWT, BCrypt
Database   MySQL, Spring Data JPA
Payments   Razorpay Java SDK, Test Mode
API Docs   Springdoc / OpenAPI
HTTP       REST APIs
Mapping    MapStruct
Caching    Redis

Project Structure

nexcart/
├── backend/
│   ├── src/
│   ├── pom.xml
│   └── .env.example
├── frontend/
│   ├── src/
│   ├── package.json
│   └── .env.example
└── docs/
    ├── PRD.md
    └── RECOVERAI.md

Local Setup

Prerequisites

Java 21+

Maven 3.9+

Node.js + npm

MySQL 8+

Razorpay Test Mode credentials for REAL recovery testing

Backend

Create a MySQL database named nexcart and configure local secrets:

spring.datasource.url=jdbc:mysql://localhost:3306/nexcart
spring.datasource.username=YOUR_DB_USER
spring.datasource.password=YOUR_DB_PASSWORD

JWT_SECRET=YOUR_LONG_RANDOM_SECRET
RAZORPAY_KEY_ID=rzp_test_...
RAZORPAY_KEY_SECRET=...
RAZORPAY_WEBHOOK_SECRET=...
RECOVERY_MAX_ATTEMPTS=3

Never commit real credentials.

Start the backend:

cd backend
mvn spring-boot:run

Default API:

http://localhost:8080

Frontend

cd frontend
npm install
npm run dev

Default frontend:

http://localhost:5173

Build and Test

# Backend
cd backend
mvn test

# Frontend
cd ../frontend
npm run build
npm run lint

Security

Stateless JWT authentication

BCrypt password hashing

Role-based admin authorization

Customer ownership checks

Backend-enforced recovery guardrails

Idempotent recovery actions

Razorpay webhook signature verification

Secrets kept outside version control

Simulated transactions isolated from real gateway activity

No Razorpay secret exposed to the frontend

Demo Flow

For the Track 3 demonstration:

1. Customer places a NexCart order
2. Razorpay Test Mode payment fails
3. RecoverAI creates a recovery case
4. RecoverAI analyzes the failure
5. Recovery action is recommended
6. Backend guardrail approves the bounded action
7. Razorpay Payment Link is generated
8. Customer receives an in-app recovery notification
9. Customer completes payment securely
10. Razorpay sends signed payment webhook
11. Recovery case becomes RECOVERED
12. Order/payment state is updated
13. Recovered amount and recovery rate are updated
14. Audit trail shows the complete journey

Example judge-facing story

RecoverAI does not just identify lost revenue. It decides a safe
recovery intervention, executes it within financial guardrails, brings
the customer back to payment, observes the outcome, and measures the
revenue actually recovered.

Buildathon Track

Razorpay Buildathon --- Track 03: AI Revenue Recovery

Problem

Failed payments and checkout issues create immediate revenue at risk.
Merchants need a system that can identify that risk and take the right
recovery action without uncontrolled financial automation.

Solution

NexCart RecoverAI closes that loop:

Detect → Diagnose → Decide → Act → Measure

It combines AI/fallback decisioning, bounded financial actions, Razorpay
Test Mode payments, customer recovery notifications, webhook-driven
resolution, guardrails, idempotency, simulation, metrics, and a complete
audit trail.

Documentation

docs/PRD.md --- product requirements and roadmap

docs/RECOVERAI.md --- RecoverAI implementation details

Important Note

This project uses Razorpay Test Mode for payment demonstrations.
Simulated cases are synthetic and are always separated from real
recovery metrics.

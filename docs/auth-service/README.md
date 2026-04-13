# Auth Service Documentation

Complete documentation for the Travelo Auth Service - a production-ready authentication microservice.

## 📚 Documentation Index

### [API Reference](./api-reference.md)
Complete API documentation with all endpoints, request/response examples, and error codes.

### [cURL Commands](./curl-commands.md)
Quick reference for testing all endpoints with cURL commands and PowerShell examples.

### [Production Features](./production-features.md)
Detailed documentation of all production-grade features implemented in the service.

### [Production Checklist](./production-checklist.md)
Step-by-step checklist for deploying the auth-service to production.

### [Database Setup](./database-setup.md)
Database schema documentation and setup instructions.

---

## 🚀 Quick Start

### 1. Create Database

```bash
# Create database
psql -U postgres -c "CREATE DATABASE travelo_auth;"

# Run schema script
psql -U travelo -d travelo_auth -f scripts/create-all-auth-tables.sql
```

### 2. Start Services

```bash
# Start PostgreSQL (if not running)
# Start Redis (if not running)
# Start auth-service
mvn spring-boot:run -pl services/auth-service
```

### 3. Test Endpoints

Access Swagger UI: `http://localhost:8081/swagger-ui.html`

Or use cURL commands from [cURL Commands](./curl-commands.md)

---

## 🔑 Key Features

- ✅ User Registration & Login
- ✅ Email Verification (OTP)
- ✅ Password Reset
- ✅ Change Password (requires email verification)
- ✅ Account Lockout Protection
- ✅ Rate Limiting
- ✅ Token Blacklist
- ✅ Device Tracking
- ✅ Account Management (requires email verification)
- ✅ JWT Authentication
- ✅ Comprehensive Security
- ✅ Email Verification Enforcement on Critical Operations

---

## 📊 Service Overview

**Port**: 8081  
**Base Path**: `/api/v1/auth`  
**Swagger UI**: `http://localhost:8081/swagger-ui.html`  
**Health Check**: `http://localhost:8081/health`

---

## 🔐 Security

- BCrypt password hashing (strength 12)
- JWT token authentication
- Account lockout (5 attempts → 15 min)
- Rate limiting (IP & email based)
- Token blacklisting
- Device fingerprinting
- Email enumeration protection

---

## 📝 Configuration

All configuration is in `application.yml`. Key settings:

- JWT secret (must be set in production)
- Database connection
- Redis connection
- Email service (SMTP)
- Rate limits
- Token expiration

---

## 🗄️ Database

Required tables:
- `users` - User accounts
- `otps` - OTP verification
- `devices` - Device tracking
- `password_reset_tokens` - Password reset

See [Database Setup](./database-setup.md) for details.

---

## 📈 Monitoring

- Health: `GET /health`
- Metrics: `GET /actuator/metrics`
- Prometheus: `GET /actuator/prometheus`

---

## 🆘 Support

For issues or questions:
1. Check [Production Checklist](./production-checklist.md) for common issues
2. Review [API Reference](./api-reference.md) for endpoint details
3. Check application logs for detailed error messages

---

## 📚 Related Documentation

- [Local Setup Guide](../LOCAL_SETUP.md) - Setting up services locally
- [Redis Setup](../redis-setup.md) - Redis configuration
- [Memurai Setup](../memurai-redis-setup.md) - Memurai Redis setup

---

## ✅ Production Readiness

The auth-service is **100% production-ready** with all critical features implemented:

- ✅ Authentication & Authorization
- ✅ Password Management
- ✅ Account Security
- ✅ Rate Limiting
- ✅ Token Management
- ✅ Device Tracking
- ✅ Error Handling
- ✅ Monitoring
- ✅ Documentation

---

**Last Updated**: December 2025  
**Version**: 1.0.0


# Auth Service - Production Deployment Checklist

## 🚀 Pre-Deployment

### 1. Security Configuration

- [ ] **JWT Secret**: Set strong, random secret (minimum 32 characters)
  ```bash
  # Generate secure secret
  openssl rand -base64 32
  export JWT_SECRET=<generated-secret>
  ```

- [ ] **Database Credentials**: Use strong passwords
  ```bash
  export POSTGRES_PASSWORD=<strong-password>
  export POSTGRES_USERNAME=travelo
  ```

- [ ] **Redis Security**: Enable password if exposed
  ```bash
  export REDIS_PASSWORD=<redis-password>
  ```

- [ ] **Email Configuration**: Configure production SMTP
  ```bash
  export MAIL_HOST=smtp.gmail.com
  export MAIL_PORT=587
  export MAIL_USERNAME=<production-email>
  export MAIL_PASSWORD=<app-password>
  ```

### 2. Database Setup

- [ ] **Create Database**: Ensure `travelo_auth` database exists
  ```sql
  CREATE DATABASE travelo_auth;
  ```

- [ ] **Run Migrations**: Execute table creation scripts
  ```bash
  psql -U travelo -d travelo_auth -f scripts/create-all-auth-tables.sql
  ```

- [ ] **Verify Tables**: Check all tables are created
  ```sql
  \dt
  -- Should show: users, otps, devices, password_reset_tokens
  ```

### 3. Infrastructure

- [ ] **PostgreSQL**: Running and accessible
- [ ] **Redis**: Running and accessible (required for OTP, rate limiting, token blacklist)
- [ ] **Email Service**: SMTP server configured and tested
- [ ] **Network**: Firewall rules configured
- [ ] **SSL/TLS**: HTTPS enabled (if exposing publicly)

### 4. Application Configuration

- [ ] **Environment Variables**: All required variables set
- [ ] **Logging**: Configured for production (log levels, rotation)
- [ ] **Monitoring**: Prometheus/Grafana or similar configured
- [ ] **Health Checks**: Configured for load balancer
- [ ] **Graceful Shutdown**: Tested (30s timeout configured)

### 5. Testing

- [ ] **Unit Tests**: All tests passing
- [ ] **Integration Tests**: End-to-end flow tested
- [ ] **Security Tests**: Penetration testing (recommended)
- [ ] **Load Testing**: Verify rate limiting works under load
- [ ] **Failover Testing**: Test Redis/DB connection failures

---

## 🔐 Security Hardening

### Required in Production

1. **JWT Secret**: Must be cryptographically random, minimum 32 characters
2. **HTTPS Only**: All endpoints should use TLS
3. **CORS Configuration**: Restrict to known origins (if needed)
4. **Rate Limiting**: Verify limits are appropriate for your traffic
5. **Account Lockout**: Verify lockout duration is appropriate
6. **Token Expiration**: Review token expiration times
7. **Password Policy**: Ensure strong password requirements
8. **Email Security**: Use TLS for SMTP connections

### Recommended

- **WAF (Web Application Firewall)**: Protect against common attacks
- **DDoS Protection**: CloudFlare or similar
- **Intrusion Detection**: Monitor for suspicious activity
- **Regular Security Audits**: Periodic penetration testing
- **Secret Management**: Use Vault or similar for secrets

---

## 📊 Monitoring & Alerting

### Key Metrics to Monitor

1. **Authentication Metrics**
   - Login success/failure rate
   - Account lockouts
   - Failed login attempts
   - Token refresh rate

2. **System Metrics**
   - Response times
   - Error rates
   - Database connection pool usage
   - Redis connection status

3. **Security Metrics**
   - Rate limit hits
   - Suspicious IP addresses
   - Unusual login patterns
   - Token blacklist size

### Alerts to Configure

- High error rate (> 5%)
- Account lockout spike
- Database connection failures
- Redis connection failures
- High response time (> 1s)
- Unusual login patterns

---

## 🗄️ Database Maintenance

### Regular Tasks

1. **Cleanup Expired Tokens**: Automated daily at 2 AM
2. **Database Backups**: Daily automated backups
3. **Index Maintenance**: Monitor and optimize indexes
4. **Connection Pool**: Monitor pool usage

### Manual Cleanup (if needed)

```sql
-- Clean expired password reset tokens
DELETE FROM password_reset_tokens 
WHERE expires_at < NOW() OR is_used = TRUE;

-- Clean old OTPs (optional, if not auto-cleaned)
DELETE FROM otps 
WHERE expires_at < NOW() OR is_used = TRUE;
```

---

## 🔄 Deployment Steps

### 1. Pre-Deployment

```bash
# Build application
mvn clean package -DskipTests

# Run tests
mvn test

# Check for vulnerabilities
mvn dependency-check:check
```

### 2. Database Migration

```bash
# Backup existing database
pg_dump -U travelo travelo_auth > backup_$(date +%Y%m%d).sql

# Run migrations
psql -U travelo -d travelo_auth -f scripts/create-all-auth-tables.sql
```

### 3. Deploy Application

```bash
# Set environment variables
export JWT_SECRET=<production-secret>
export POSTGRES_URL=jdbc:postgresql://<db-host>:5432/travelo_auth
export REDIS_HOST=<redis-host>
# ... other variables

# Start application
java -jar auth-service.jar
```

### 4. Post-Deployment Verification

```bash
# Health check
curl http://localhost:8081/health

# Test registration
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","username":"testuser","email":"test@example.com","password":"Test123!@#"}'

# Test login
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"emailOrUsername":"test@example.com","password":"Test123!@#"}'
```

---

## 🐛 Troubleshooting

### Common Issues

1. **Database Connection Errors**
   - Check PostgreSQL is running
   - Verify connection string
   - Check firewall rules
   - Verify credentials

2. **Redis Connection Errors**
   - Check Redis is running
   - Verify host/port
   - Check password if configured
   - Test connection: `redis-cli ping`

3. **Email Sending Failures**
   - Verify SMTP credentials
   - Check email service logs
   - Test SMTP connection
   - Verify app password (for Gmail)

4. **Token Issues**
   - Verify JWT secret is set
   - Check token expiration
   - Verify Redis for blacklist

5. **Rate Limiting Issues**
   - Check Redis connection
   - Verify rate limit configuration
   - Check IP extraction logic

---

## 📈 Performance Tuning

### Database
- Connection pool size: Adjust based on load
- Query optimization: Monitor slow queries
- Index optimization: Add indexes for frequently queried fields

### Redis
- Connection pool: Configure appropriately
- Memory limits: Set based on usage
- Persistence: Configure RDB/AOF if needed

### Application
- JVM tuning: Heap size, GC settings
- Thread pool: Async email executor
- Connection timeouts: Adjust based on network

---

## ✅ Production Readiness Verification

Run these checks before going live:

```bash
# 1. Health check
curl http://localhost:8081/health

# 2. Database connectivity
psql -U travelo -d travelo_auth -c "SELECT COUNT(*) FROM users;"

# 3. Redis connectivity
redis-cli -h localhost -p 6379 ping

# 4. Email service (if configured)
# Test email sending via registration

# 5. All endpoints accessible
# Test via Swagger UI: http://localhost:8081/swagger-ui.html
```

---

## 📞 Support & Maintenance

### Regular Maintenance Tasks

- **Weekly**: Review error logs
- **Monthly**: Security audit
- **Quarterly**: Performance review
- **Annually**: Full security assessment

---

## 🎯 Success Criteria

Your auth-service is production-ready when:

- ✅ All endpoints tested and working
- ✅ Security features enabled and tested
- ✅ Monitoring and alerting configured
- ✅ Database backups automated
- ✅ Documentation complete
- ✅ Load testing passed
- ✅ Security audit completed
- ✅ Incident response plan ready

---

## 📚 Additional Resources

- [API Reference](./api-reference.md)
- [cURL Commands](./curl-commands.md)
- [Production Features](./production-features.md)


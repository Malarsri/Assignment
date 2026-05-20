# Database Best Practices for Warehouse Assignment

## Overview
This document outlines best practices for the warehouse management system codebase, with emphasis on database design, Quarkus/Hibernate ORM, and architectural patterns.

---

## 1. **Entity Design & JPA Annotations**

### Current Issues:
- **DbWarehouse** entity lacks column constraints and metadata
- **Product** entity is better but missing some constraints
- No `@Column` annotations for data validation at DB level

### Recommendations:

#### ✅ DO:
```java
@Entity
@Table(name = "warehouse", indexes = {
    @Index(name = "idx_business_unit_code", columnList = "businessUnitCode", unique = true),
    @Index(name = "idx_location", columnList = "location")
})
public class DbWarehouse {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(length = 50, nullable = false, unique = true)
    private String businessUnitCode;
    
    @Column(length = 100, nullable = false)
    private String location;
    
    @Column(nullable = false)
    @Min(1)
    private Integer capacity;
    
    @Column(nullable = false)
    @Min(0)
    private Integer stock;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = true)
    private LocalDateTime archivedAt;
    
    @Version  // For optimistic locking
    private Long version;
}
```

#### ❌ DON'T:
```java
// Missing constraints, annotations, and indexes
public class DbWarehouse {
    public String businessUnitCode;  // No constraints!
    public Integer capacity;
}
```

---

## 2. **Database Configuration (application.properties)**

### Current Status:
- Good dev setup with auto-provisioning
- Production config is commented out
- `drop-and-create` is unsafe for production

### Recommended Configuration:

```ini
# ========== Development Profile ==========
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=quarkus_dev
quarkus.datasource.password=quarkus_dev

# Schema generation - ONLY for development
quarkus.hibernate-orm.database.generation=drop-and-create
quarkus.hibernate-orm.log.sql=true

# ========== Production Profile ==========
%prod.quarkus.datasource.db-kind=postgresql
%prod.quarkus.datasource.username=${DATABASE_USER}
%prod.quarkus.datasource.password=${DATABASE_PASSWORD}
%prod.quarkus.datasource.jdbc.url=jdbc:postgresql://${DATABASE_HOST}:${DATABASE_PORT}/${DATABASE_NAME}
%prod.quarkus.datasource.jdbc.max-size=20
%prod.quarkus.datasource.jdbc.min-size=5

# CRITICAL: Use 'validate' in production, NOT 'drop-and-create'
%prod.quarkus.hibernate-orm.database.generation=validate
%prod.quarkus.hibernate-orm.log.sql=false

# ========== Connection Pooling ==========
# Default values are usually fine, but adjust for your load
quarkus.datasource.jdbc.max-idle-time=15m
quarkus.datasource.jdbc.validation-interval=2m
quarkus.datasource.jdbc.background-validation=true

# ========== Performance & Caching ==========
quarkus.hibernate-orm.second-level-cache.enabled=true
quarkus.hibernate-orm.persistence.xml.ignored=true
```

---

## 3. **Repository Pattern & Query Optimization**

### Current Pattern:
Using Quarkus Panache - excellent for CRUD operations!

### Recommendations:

#### ✅ Better Query Methods:
```java
@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {
    
    // Use named queries for complex operations
    public Warehouse findByBusinessUnitCode(String buCode) {
        return Database
            .find("businessUnitCode = ?1", buCode)
            .firstResultOptional()
            .map(DbWarehouse::toWarehouse)
            .orElse(null);
    }
    
    // HQL for better performance with large datasets
    public List<Warehouse> findByLocation(String location) {
        return this.find(
            "FROM DbWarehouse d WHERE d.location = ?1 AND d.archivedAt IS NULL",
            location
        ).list().stream()
         .map(DbWarehouse::toWarehouse)
         .toList();
    }
    
    // Count operations for pagination
    public long countActive() {
        return this.count("archivedAt IS NULL");
    }
    
    // Batch operations for performance
    @Transactional
    public void bulkArchive(List<String> businessUnitCodes) {
        this.update(
            "archivedAt = ?1 WHERE businessUnitCode IN ?2",
            LocalDateTime.now(),
            businessUnitCodes
        );
    }
}
```

#### ❌ Avoid:
```java
// N+1 query problem - avoid in loops
for (String buCode : codes) {
    find("businessUnitCode", buCode).firstResult();  // Multiple DB calls!
}

// Inefficient fetching
listAll().stream().filter(...).toList();  // Fetches ALL then filters in memory
```

---

## 4. **Data Validation & Constraints**

### Current Issues:
- No `@NotNull`, `@NotBlank` annotations
- No minimum/maximum constraints
- Business logic validation scattered

### Recommendations:

```java
// In Entity
@Entity
public class DbWarehouse {
    @Column(nullable = false)
    @NotBlank(message = "Business unit code cannot be blank")
    @Size(min = 3, max = 50, message = "Business unit code must be 3-50 characters")
    private String businessUnitCode;
    
    @Column(nullable = false)
    @Positive(message = "Capacity must be greater than 0")
    @Max(value = 10000, message = "Capacity cannot exceed 10000")
    private Integer capacity;
    
    @PositiveOrZero(message = "Stock cannot be negative")
    @Column(nullable = false)
    private Integer stock;
}

// In Resource (API validation)
@Override
public Warehouse createANewWarehouseUnit(@NotNull @Valid Warehouse data) {
    // @Valid triggers bean validation
    // ...
}
```

---

## 5. **Transaction Management**

### Best Practices:

```java
// ✅ DO: Mark only methods that modify data as @Transactional
@Transactional
public void create(Warehouse warehouse) {
    // Auto-commit on success
}

// ✅ DO: Use specific transaction propagation
@Transactional(Transactional.TxType.REQUIRES_NEW)
public void archiveWarehouse(String id) {
    // Runs in new transaction, independent of caller
}

// ❌ DON'T: Mark read-only methods as @Transactional
// Unnecessary overhead
public List<Warehouse> getAll() {
    // This is read-only, doesn't need @Transactional
}

// ❌ DON'T: Open transactions in loops
for (String id : ids) {
    archiveWarehouse(id);  // Creates new transaction each time!
}
// Instead: Create a batch method
```

---

## 6. **Caching Strategy**

### Current Implementation:
```java
@Entity
@Cacheable  // First-level cache (entity cache)
public class DbWarehouse { ... }
```

### Recommendations:

```java
// ✅ Cache configuration in application.properties
quarkus.hibernate-orm.second-level-cache.enabled=true

// ✅ Only cache stable, read-many entities
@Entity
@Cacheable
public class DbWarehouse { }  // Good - changes infrequently per entity

// ✅ Use cache expiration
%prod.quarkus.cache.type=redis
%prod.quarkus.cache.redis.host=redis-server
%prod.quarkus.cache.redis.port=6379
%prod.quarkus.cache.default-min-max-idle-time=10m

// ❌ DON'T: Cache frequently changing entities
@Entity
@Cacheable
public class DbOrder { }  // Bad - orders change constantly
```

---

## 7. **Query Patterns & N+1 Prevention**

### Problem: N+1 Queries
```java
// ❌ BAD: Causes N+1 queries
var warehouses = warehouseRepository.getAll();
for (Warehouse w : warehouses) {
    var location = locationGateway.resolveByIdentifier(w.location);
}
```

### Solution: Eager Loading / Batch Queries
```java
// ✅ GOOD: Single query with JOIN
public List<WarehouseDTO> getAllWithLocations() {
    return this.find(
        "SELECT new com.example.WarehouseDTO(w.id, w.businessUnitCode, l.name) " +
        "FROM DbWarehouse w " +
        "JOIN Location l ON w.location = l.code " +
        "WHERE w.archivedAt IS NULL"
    ).list();
}

// ✅ GOOD: Fetch related data in single query
@Entity
public class DbWarehouse {
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "location_code")
    private Location location;
}
```

---

## 8. **Database Schema Design**

### Current Schema (inferred):
```sql
CREATE TABLE warehouse (
    id BIGSERIAL PRIMARY KEY,
    businessUnitCode VARCHAR(50) NOT NULL UNIQUE,
    location VARCHAR(100) NOT NULL,
    capacity INTEGER NOT NULL,
    stock INTEGER NOT NULL,
    createdAt TIMESTAMP NOT NULL,
    archivedAt TIMESTAMP
);
```

### Recommended Schema:
```sql
CREATE TABLE warehouse (
    id BIGSERIAL PRIMARY KEY,
    businessUnitCode VARCHAR(50) NOT NULL UNIQUE,
    location VARCHAR(100) NOT NULL,
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    stock INTEGER NOT NULL CHECK (stock >= 0),
    createdAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    archivedAt TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,  -- For optimistic locking
    updatedAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for common queries
CREATE INDEX idx_warehouse_location ON warehouse(location);
CREATE INDEX idx_warehouse_archivedAt ON warehouse(archivedAt);
CREATE INDEX idx_warehouse_createdAt ON warehouse(createdAt);
```

---

## 9. **Error Handling & Null Safety**

### Current Issues:
```java
// Returns null - callers must check
public Warehouse findByBusinessUnitCode(String buCode) {
    DbWarehouse dbWarehouse = this.find("businessUnitCode", buCode).firstResult();
    if (dbWarehouse != null) {
        return dbWarehouse.toWarehouse();
    }
    return null;  // ⚠️ Dangerous!
}
```

### Better Approach:
```java
public Optional<Warehouse> findByBusinessUnitCode(String buCode) {
    return this.find("businessUnitCode", buCode)
        .firstResultOptional()
        .map(DbWarehouse::toWarehouse);
}

// Usage
warehouseRepository
    .findByBusinessUnitCode(code)
    .orElseThrow(() -> new WarehouseNotFoundException("Code: " + code));
```

---

## 10. **Database Migrations (Liquibase/Flyway)**

### Recommendation:
For production, use migrations instead of Hibernate auto-generation.

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-flyway</artifactId>
</dependency>
```

```ini
# application.properties
quarkus.flyway.migrate-at-start=true
quarkus.flyway.baseline-on-migrate=true
```

```sql
-- src/main/resources/db/migration/V1__init.sql
CREATE TABLE warehouse (
    id BIGSERIAL PRIMARY KEY,
    businessUnitCode VARCHAR(50) NOT NULL UNIQUE,
    ...
);
```

---

## 11. **Monitoring & Performance**

### Enable Query Statistics:
```ini
quarkus.hibernate-orm.log.sql=true
quarkus.log.category."org.hibernate.SQL".level=DEBUG
quarkus.log.category."org.hibernate.type.descriptor.sql.BasicBinder".level=TRACE
```

### Connection Pool Monitoring:
```java
@Health
@Applicationscoped
public class DbHealthCheck implements HealthCheck {
    @Inject
    AgroalDataSource dataSource;
    
    @Override
    public HealthCheckResponse call() {
        int available = dataSource.getConfiguration()
            .poolConfiguration()
            .maxSize();
        return HealthCheckResponse.up("database")
            .withData("connections", available)
            .build();
    }
}
```

---

## 12. **Testing Best Practices**

### Use Testcontainers (already in pom.xml):
```java
@QuarkusTest
public class WarehouseRepositoryTest {
    
    @Inject
    WarehouseRepository repository;
    
    @Test
    public void testCreateWarehouse() {
        Warehouse w = new Warehouse();
        w.businessUnitCode = "TEST.001";
        
        repository.create(w);
        
        Optional<Warehouse> found = repository.findByBusinessUnitCode("TEST.001");
        assertThat(found).isPresent();
    }
}
```

### Use @Transactional(NOT_SUPPORTED) in Tests:
```java
@Transactional(Transactional.TxType.NOT_SUPPORTED)
public void testConcurrency() {
    // Each operation has separate transaction
}
```

---

## 13. **Security Considerations**

### SQL Injection Prevention:
```java
// ✅ SAFE: Parameterized queries
this.find("businessUnitCode = ?1", userInput).firstResult();

// ❌ DANGEROUS: String concatenation
this.find("businessUnitCode = '" + userInput + "'");  // SQL Injection!
```

### Connection Security:
```ini
# Use SSL/TLS for production
%prod.quarkus.datasource.jdbc.url=jdbc:postgresql://host:5432/db?sslmode=require
```

---

## 14. **Performance Tuning Checklist**

- [ ] Enable second-level cache for stable entities
- [ ] Use pagination for large result sets
- [ ] Create appropriate database indexes
- [ ] Use batch operations for bulk updates
- [ ] Profile slow queries with `log.sql=true`
- [ ] Use connection pooling with proper min/max sizes
- [ ] Implement caching at application level (Redis)
- [ ] Use read replicas for read-heavy operations
- [ ] Monitor query execution plans
- [ ] Avoid N+1 query patterns

---

## 15. **Summary of Immediate Actions**

1. **Add Column Constraints** to `DbWarehouse` entity
2. **Configure Migration Tool** (Flyway/Liquibase)
3. **Update application.properties** with production settings
4. **Use Optional<T>** instead of null returns
5. **Add Batch Operations** for bulk updates
6. **Enable Query Logging** in dev environment
7. **Create Indexes** for frequently queried columns
8. **Add Optimistic Locking** (@Version)
9. **Document Query Performance** expectations
10. **Set up Connection Pool Monitoring**

---

## References
- [Quarkus Hibernate ORM Guide](https://quarkus.io/guides/hibernate-orm)
- [Panache Repository Pattern](https://quarkus.io/guides/hibernate-orm-panache)
- [Jakarta Persistence Best Practices](https://jakarta.ee/specifications/persistence/)
- [PostgreSQL Performance Tips](https://www.postgresql.org/docs/current/performance-tips.html)


# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
1. Entity Constraints (HIGH PRIORITY)
Add proper JPA annotations to DbWarehouse.java:
@Column constraints (length, nullable, unique)
@Min, @Max, @Positive for numeric validation
@Index annotations for query optimization
@Version for optimistic locking
2. Database Configuration (HIGH PRIORITY)
Update application.properties:
Use validate mode in production (NOT drop-and-create)
Configure connection pooling (max-size: 20, min-size: 5)
Use environment variables for credentials
Enable proper logging in dev, disable in prod
3. Repository Pattern (MEDIUM PRIORITY)
Change null returns to Optional<T> for null-safety
Use batch operations for bulk updates
Add location-based queries with proper indexing
Implement query methods with HQL for better performance
4. Query Optimization (MEDIUM PRIORITY)
Add indexes on: businessUnitCode, location, createdAt, archivedAt
Prevent N+1 queries when fetching related locations
Use batch operations for archiving multiple warehouses
5. Schema Improvements  
We can use liquibase or flyway for better schema management and versioning, instead of relying on JPA auto-generation. This allows for more controlled migrations and better handling of production databases.

6. Error Handling (LOW PRIORITY)
Return Optional<Warehouse> instead of null
Add custom exceptions like WarehouseNotFoundException
Validate inputs at API level with @Valid
7. Testing (LOW PRIORITY)
Use Testcontainers for database tests (already in pom.xml)
Test with real PostgreSQL instance
The guide includes 15 detailed sections with code examples, configuration snippets, and a complete checklist for production readiness. Review the DATABASE_BEST_PRACTICES.md file for comprehensive details and implementation examples!
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
Best Practice is to use OPEN API. 
If we have to decide I will usually choose based on the API type:
Use OpenAPI-first when, API is shared across teams (Multiple consumers exist),  public/external
Examples: Warehouse API

Use code-first ,when Internal microservice, Small team, Rapid iteration, Prototype/MVP
Examples: Internal Product services, Experimental services


```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
I would prioritize testing using a testing pyramid approach: focus mainly on unit/service tests for core business logic, then add API contract/integration tests for critical endpoints and database interactions, with only a few end-to-end tests for key user flows. This provides strong coverage with lower maintenance cost.
In bottom covering all the core logic and edge cases, then medium layer for infrastructure and contracts, and small top for critical workflows.
all Unit/service tests - Fast, many, highly isolated.
Medium layer - Integration/API contract tests and Validate infrastructure and contracts.
Small top - E2E, Only critical workflows.
To keep coverage effective over time, I’d:

add regression tests for every bug fix,
run automated tests in CI/CD,
monitor meaningful coverage (especially service layer and critical paths),
and regularly refactor flaky or duplicated tests.
```
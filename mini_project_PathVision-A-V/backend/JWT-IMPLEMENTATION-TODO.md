# JWT Implementation TODO

This file lists the implementation steps to add JWT authentication to the backend.

1) Add JJWT dependencies to `pom.xml`
   - Add the following dependencies (recommended JJWT 0.11.5):

```xml
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-api</artifactId>
  <version>0.11.5</version>
</dependency>
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-impl</artifactId>
  <version>0.11.5</version>
  <scope>runtime</scope>
</dependency>
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-jackson</artifactId>
  <version>0.11.5</version>
  <scope>runtime</scope>
</dependency>
```

2) Add JWT properties to `application.properties`
   - Add entries to `src/main/resources/application.properties`:

```
# JWT
jwt.secret=change-this-to-a-very-long-random-secret
jwt.expiration-ms=3600000
jwt.issuer=pathvision
```

3) Add `JwtService` class
   - Suggested location: `com.pathvision.util.JwtService`.
   - Responsibilities:
     - Generate token for a user/principal
     - Validate token
     - Extract username / claims
   - Minimal method signatures:

```java
public String generateToken(UserDetails userDetails);
public boolean isTokenValid(String token, UserDetails userDetails);
public String extractUsername(String token);
```

4) Add `JwtAuthenticationFilter`
   - Suggested location: `com.pathvision.config.JwtAuthenticationFilter` or `util`.
   - Extend `OncePerRequestFilter`.
   - Read `Authorization` header, validate token, set `UsernamePasswordAuthenticationToken` in `SecurityContextHolder`.

5) Add `JwtAuthenticationEntryPoint`
   - Suggested location: `com.pathvision.config.JwtAuthenticationEntryPoint`.
   - Implement `AuthenticationEntryPoint` and send HTTP 401 on authentication failures.

6) Update `SecurityConfig`
   - Register `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`.
   - Configure `authenticationEntryPoint` to `JwtAuthenticationEntryPoint`.
   - Set session management to stateless.

7) Update `AuthService` to generate tokens
   - After successful authentication or registration, call `JwtService.generateToken(...)` and return it in `AuthResponse`.
   - Ensure `AuthResponse` includes the token (e.g., `accessToken` or `jwt`).

8) Run / verify instructions
   - Backend (run from project root):

```powershell
cd backend
mvn clean package
mvn spring-boot:run
```

   - Frontend (if using Vite):

```powershell
cd frontend
npm install
npm run dev
```

   - Test endpoints:
     - Use `POST /api/auth/login` to get JWT (AuthService should return token).
     - Call protected endpoints with header `Authorization: Bearer <token>`.

Notes / Tips
- Keep `jwt.secret` long (>= 256 bits) and store it securely in environment or secrets manager for production.
- Use `Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8))` when creating the signing key (JJWT 0.11.x).
- Add unit tests for `JwtService` and integration tests for `JwtAuthenticationFilter`.

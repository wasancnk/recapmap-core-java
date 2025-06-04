# Authentication Architecture for Separated Frontend/Backend

**Date**: June 1, 2025  
**Project**: RecapMap Java Backend  
**Focus**: Secure React ↔ Java Authentication

## Authentication Challenge

### The Problem
```yaml
setup: "React frontend (port 5173) + Java backend (port 8080)"
challenges:
  - "Cross-origin requests (CORS)"
  - "Different domains/ports"
  - "Stateless backend preferred"
  - "WebSocket authentication"
  - "Token storage and refresh"
```

## Recommended Solution: JWT + HTTP-Only Cookies

### Why This Approach?
```yaml
security_benefits:
  - "HTTP-only cookies prevent XSS attacks"
  - "SameSite cookies prevent CSRF"
  - "JWT for stateless backend"
  - "Refresh token rotation for security"

developer_experience:
  - "Automatic cookie handling by browser"
  - "No manual token management in frontend"
  - "Works seamlessly with WebSockets"
```

## Implementation Strategy

### 1. Backend JWT Service
```java
@Service
public class AuthenticationService {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.access-token-expiry:900}") // 15 minutes
    private int accessTokenExpiry;
    
    @Value("${jwt.refresh-token-expiry:604800}") // 7 days
    private int refreshTokenExpiry;
    
    public AuthTokens authenticate(String email, String password) {
        // Validate credentials
        User user = userService.validateCredentials(email, password);
        
        // Generate tokens
        String accessToken = generateAccessToken(user);
        String refreshToken = generateRefreshToken(user);
        
        return new AuthTokens(accessToken, refreshToken);
    }
    
    private String generateAccessToken(User user) {
        return Jwts.builder()
            .setSubject(user.getId())
            .claim("email", user.getEmail())
            .claim("role", user.getRole())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiry * 1000))
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }
}
```

### 2. Cookie Configuration
```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request,
            HttpServletResponse response) {
        
        AuthTokens tokens = authService.authenticate(
            request.getEmail(), 
            request.getPassword()
        );
        
        // Set HTTP-only cookies
        setAuthCookies(response, tokens);
        
        return ResponseEntity.ok(new LoginResponse("Login successful"));
    }
    
    private void setAuthCookies(HttpServletResponse response, AuthTokens tokens) {
        // Access token cookie (short-lived)
        Cookie accessCookie = new Cookie("access_token", tokens.getAccessToken());
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(true); // HTTPS only in production
        accessCookie.setSameSite("Strict");
        accessCookie.setPath("/");
        accessCookie.setMaxAge(900); // 15 minutes
        response.addCookie(accessCookie);
        
        // Refresh token cookie (longer-lived)
        Cookie refreshCookie = new Cookie("refresh_token", tokens.getRefreshToken());
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setSameSite("Strict");
        refreshCookie.setPath("/api/auth/refresh");
        refreshCookie.setMaxAge(604800); // 7 days
        response.addCookie(refreshCookie);
    }
}
```

### 3. CORS Configuration
```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173") // React dev server
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true); // Important: allows cookies
    }
}
```

### 4. JWT Authentication Filter
```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        // Extract JWT from cookie
        String jwt = extractJwtFromCookie(request, "access_token");
        
        if (jwt != null && jwtUtil.validateToken(jwt)) {
            // Set Spring Security context
            UsernamePasswordAuthenticationToken auth = 
                new UsernamePasswordAuthenticationToken(
                    jwtUtil.getUserId(jwt),
                    null,
                    jwtUtil.getAuthorities(jwt)
                );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String extractJwtFromCookie(HttpServletRequest request, String cookieName) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
```

## Frontend Integration

### 1. Login Request
```typescript
// No need to handle tokens manually!
const login = async (email: string, password: string) => {
  const response = await fetch('http://localhost:8080/api/auth/login', {
    method: 'POST',
    credentials: 'include', // Important: includes cookies
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ email, password }),
  });
  
  if (response.ok) {
    // Cookies are automatically set by browser
    // No manual token storage needed!
    router.push('/dashboard');
  }
};
```

### 2. API Requests
```typescript
// All API calls automatically include cookies
const fetchProjects = async () => {
  const response = await fetch('http://localhost:8080/api/projects', {
    credentials: 'include', // Always include this
  });
  
  if (response.status === 401) {
    // Token expired, try refresh
    await refreshToken();
    // Retry original request
  }
  
  return response.json();
};
```

### 3. Automatic Token Refresh
```typescript
const refreshToken = async () => {
  const response = await fetch('http://localhost:8080/api/auth/refresh', {
    method: 'POST',
    credentials: 'include',
  });
  
  if (!response.ok) {
    // Refresh failed, redirect to login
    router.push('/login');
  }
  
  // New tokens automatically set in cookies
};
```

## WebSocket Authentication

### Backend WebSocket Security
```java
@Configuration
@EnableWebSocketSecurity
public class WebSocketSecurityConfig {
    
    @Bean
    public AuthorizationManager<Message<?>> messageAuthorizationManager() {
        return (authentication, context) -> {
            // Extract JWT from WebSocket handshake
            String jwt = extractJwtFromHandshake(context);
            return jwtUtil.validateToken(jwt) ? 
                AuthorizationDecision.grant() : 
                AuthorizationDecision.deny();
        };
    }
}

@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {
    
    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {
        
        // Extract JWT from cookie in handshake
        String jwt = extractJwtFromCookies(request);
        
        if (jwt != null && jwtUtil.validateToken(jwt)) {
            attributes.put("userId", jwtUtil.getUserId(jwt));
            return true;
        }
        
        return false; // Reject connection
    }
}
```

### Frontend WebSocket Connection
```typescript
// Cookies automatically included in WebSocket handshake
const connectWebSocket = () => {
  const ws = new WebSocket('ws://localhost:8080/ws/mindmap');
  
  ws.onopen = () => {
    console.log('WebSocket connected');
    // Authentication already handled via cookies
  };
  
  ws.onerror = (error) => {
    console.log('WebSocket error:', error);
    // Might be auth failure, try refresh
  };
};
```

## Security Best Practices

### Cookie Security
```yaml
production_settings:
  secure: true              # HTTPS only
  sameSite: "Strict"       # CSRF protection
  httpOnly: true           # XSS protection
  domain: ".recapmap.com"  # Subdomain sharing if needed

development_settings:
  secure: false            # Allow HTTP for local dev
  sameSite: "Lax"         # More permissive for dev
  httpOnly: true          # Still prevent XSS
```

### Token Rotation
```yaml
strategy: "Refresh token rotation"
benefits:
  - "Compromised refresh tokens become invalid"
  - "Automatic cleanup of old tokens"
  - "Enhanced security for long-lived sessions"

implementation:
  - "Each refresh generates new refresh token"
  - "Old refresh token immediately invalidated"
  - "Database cleanup of expired tokens"
```

## Alternative Approaches (Not Recommended)

### 1. Local Storage JWT
```yaml
problems:
  - "Vulnerable to XSS attacks"
  - "Manual token management complexity"
  - "WebSocket auth complications"
```

### 2. Session-Based Auth
```yaml
problems:
  - "Stateful backend (harder to scale)"
  - "Session storage requirements"
  - "Sticky session complexity"
```

### 3. Authorization Header
```yaml
problems:
  - "Manual token management in frontend"
  - "WebSocket header limitations"
  - "More complex refresh logic"
```

## Implementation Timeline

### Phase 1: Basic Auth
1. **JWT service** with cookie setting
2. **Login/logout endpoints** 
3. **Basic CORS configuration**
4. **Frontend auth integration**

### Phase 2: Advanced Features
1. **Refresh token rotation**
2. **WebSocket authentication**
3. **Role-based authorization**
4. **Session management**

### Phase 3: Production Hardening
1. **Rate limiting**
2. **Advanced security headers**
3. **Audit logging**
4. **Multi-factor authentication**

This approach gives you enterprise-grade security while keeping the implementation straightforward and maintainable.

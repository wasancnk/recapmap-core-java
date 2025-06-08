# RecapMap Core API Endpoints Reference

## Overview

This document provides comprehensive documentation for all API endpoints in the RecapMap Core Java backend. All endpoints require proper authentication unless explicitly marked as public.

## Authentication

The API uses session-based authentication with the following credentials:

- **Admin User**: `admin` / `admin123`
- **Regular User**: `user` / `user123`

### Authentication Flow
1. POST to `/api/auth/login` with credentials
2. Receive session cookie for subsequent requests
3. Include session cookie in all authenticated requests

---

## API Endpoints

### 🔐 Authentication Endpoints

#### POST `/api/auth/login`
**Description**: Authenticate user and create session  
**Access**: Public  
**Content-Type**: `application/json`

**Request Body**:
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Response (Success - 200)**:
```json
{
  "success": true,
  "message": "Login successful",
  "user": "admin",
  "role": "ADMIN"
}
```

**Response (Error - 401)**:
```json
{
  "success": false,
  "message": "Invalid credentials"
}
```

#### POST `/api/auth/logout`
**Description**: Logout user and invalidate session  
**Access**: Authenticated users  

**Response (200)**:
```json
{
  "success": true,
  "message": "Logout successful"
}
```

#### GET `/api/auth/status`
**Description**: Check current authentication status  
**Access**: Public  

**Response (Authenticated - 200)**:
```json
{
  "authenticated": true,
  "user": "admin",
  "role": "ADMIN"
}
```

**Response (Not Authenticated - 200)**:
```json
{
  "authenticated": false
}
```

---

### 📚 Documentation Endpoints

#### GET `/api/docs/endpoints`
**Description**: Get live discovery of all API endpoints  
**Access**: Admin only  

**Response (200)**:
```json
[
  {
    "method": "GET",
    "path": "/api/docs/endpoints",
    "description": "Get all API endpoints",
    "parameters": [],
    "controller": "ApiDocumentationController",
    "action": "getApiEndpoints"
  },
  {
    "method": "POST",
    "path": "/api/auth/login",
    "description": "User authentication",
    "parameters": [
      {
        "name": "username",
        "type": "String",
        "required": true
      },
      {
        "name": "password", 
        "type": "String",
        "required": true
      }
    ],
    "controller": "ApiAuthController",
    "action": "login"
  }
]
```

#### GET `/api/docs/markdown/{filename}`
**Description**: Get markdown documentation content  
**Access**: Admin only  
**Path Parameters**:
- `filename`: Name of markdown file (without .md extension)

**Example**: `/api/docs/markdown/API_ENDPOINTS_REFERENCE`

**Response (200)**:
```json
{
  "filename": "API_ENDPOINTS_REFERENCE",
  "markdown": "# RecapMap Core API Endpoints Reference...",
  "html": "<h1>RecapMap Core API Endpoints Reference</h1>...",
  "lastModified": 1703123456789
}
```

**Response (404)**:
```json
{
  "error": "File not found"
}
```

**Response (400)**:
```json
{
  "error": "Invalid filename"
}
```

---

### 🧪 Test Endpoints

#### GET `/api/test/hello`
**Description**: Simple test endpoint  
**Access**: Public  

**Response (200)**:
```json
{
  "message": "Hello from RecapMap Core!",
  "timestamp": "2024-01-15T10:30:00Z",
  "version": "1.0.0"
}
```

#### GET `/api/test/authenticated`
**Description**: Test endpoint requiring authentication  
**Access**: Authenticated users  

**Response (200)**:
```json
{
  "message": "Hello authenticated user!",
  "user": "admin",
  "role": "ADMIN",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

**Response (401)**:
```json
{
  "error": "Authentication required"
}
```

#### GET `/api/test/admin`
**Description**: Test endpoint requiring admin role  
**Access**: Admin only  

**Response (200)**:
```json
{
  "message": "Hello admin!",
  "user": "admin", 
  "privileges": ["READ", "WRITE", "ADMIN"],
  "timestamp": "2024-01-15T10:30:00Z"
}
```

**Response (403)**:
```json
{
  "error": "Admin access required"
}
```

---

## HTTP Status Codes

### Success Codes
- **200 OK**: Request successful
- **201 Created**: Resource created successfully

### Client Error Codes  
- **400 Bad Request**: Invalid request format or parameters
- **401 Unauthorized**: Authentication required
- **403 Forbidden**: Insufficient permissions
- **404 Not Found**: Resource not found

### Server Error Codes
- **500 Internal Server Error**: Unexpected server error

---

## Security Features

### XSS Protection
- All markdown content is sanitized before rendering
- HTML output is cleaned of potentially harmful content

### Path Traversal Prevention  
- Filename validation prevents directory traversal attacks
- Only allows alphanumeric characters and safe symbols

### Session Security
- HTTP-only session cookies
- Secure cookie settings in production
- Session timeout and invalidation

### CORS Configuration
- Configured for frontend origin: `http://localhost:3000`
- Credentials allowed for authenticated requests

---

## Usage Examples

### Login Flow
```bash
# 1. Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  -c cookies.txt

# 2. Access protected endpoint  
curl -X GET http://localhost:8080/api/docs/endpoints \
  -b cookies.txt

# 3. Logout
curl -X POST http://localhost:8080/api/auth/logout \
  -b cookies.txt
```

### Testing Endpoints
```bash
# Test public endpoint
curl http://localhost:8080/api/test/hello

# Test authenticated endpoint (requires login first)
curl http://localhost:8080/api/test/authenticated -b cookies.txt

# Test admin endpoint (requires admin role)
curl http://localhost:8080/api/test/admin -b cookies.txt
```

### Getting Documentation
```bash
# Get all endpoints
curl http://localhost:8080/api/docs/endpoints -b cookies.txt

# Get this markdown file as HTML
curl http://localhost:8080/api/docs/markdown/API_ENDPOINTS_REFERENCE -b cookies.txt
```

---

## Error Handling

All endpoints follow consistent error response formats:

### Authentication Errors
```json
{
  "error": "Authentication required",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/docs/endpoints"
}
```

### Validation Errors
```json
{
  "error": "Invalid filename",
  "details": "Filename cannot contain path separators",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### Server Errors
```json
{
  "error": "Internal server error",
  "message": "An unexpected error occurred",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

---

## Development Notes

### Adding New Endpoints
1. Create controller method with proper annotations
2. Add security configuration if needed
3. Update this documentation
4. Test with provided examples

### Security Considerations
- Always validate input parameters
- Use proper authentication checks
- Sanitize output data
- Follow principle of least privilege

### Testing
- Use the admin interface at `/admin/api-docs` for interactive testing
- All endpoints are automatically discovered and documented
- Live testing available through the web interface

---

## Changelog

### Version 1.0.0 (Current)
- Initial API implementation
- Session-based authentication
- Live endpoint discovery
- Markdown documentation system
- Admin interface integration
- Security hardening

---

*This documentation is automatically loaded in the RecapMap Admin interface. For the latest endpoint information, visit `/admin/api-docs` after logging in.*

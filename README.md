# RecapMap Core

A Java Spring Boot application for document processing, AI-powered content extraction, and mind mapping capabilities.

## Project Overview

RecapMap Core is a comprehensive document processing platform that leverages AI vision capabilities to extract and analyze content from various document formats. The application provides a web-based interface for document upload, processing, and visualization.

## Technology Stack

- **Java**: 17
- **Spring Boot**: 3.4.5
- **MongoDB**: Atlas (Cloud Database)
- **Frontend**: HTML5, CSS3, JavaScript, jQuery
- **AI Integration**: OpenAI Vision API
- **Build Tool**: Maven 3.x
- **Logging**: Logback with timestamp-based log files

## Project Structure

```
recapmap-core-java/
├── src/main/java/com/recapmap/core/
│   ├── CoreApplication.java              # Main Spring Boot application
│   ├── config/                          # Configuration classes
│   │   ├── AppConfig.java               # General application configuration
│   │   ├── RestClientConfig.java        # REST client configuration
│   │   └── SecurityConfig.java          # Security configuration
│   ├── controller/                      # REST API controllers
│   │   ├── CopilotController.java       # Main AI copilot functionality
│   │   ├── FileProcessingController.java # File upload and processing
│   │   ├── LoginController.java         # Authentication endpoints
│   │   ├── OpenAiVisionTestController.java # AI vision testing
│   │   └── UserInfoController.java      # User management
│   ├── data/                           # Data models and entities
│   │   ├── ExtractedDocument.java       # Document extraction model
│   │   ├── ExtractionConfig.java        # Extraction configuration
│   │   ├── ImageExtractionJob.java      # Image processing job model
│   │   └── PdfSourceDataSource.java     # PDF data source model
│   └── service/                        # Business logic services
│       ├── CopilotService.java          # AI copilot business logic
│       ├── ExtractionPipelineService.java # Document extraction pipeline
│       ├── MongoDbService.java          # Database operations
│       ├── OpenAiVisionService.java     # AI vision integration
│       └── PdfService.java              # PDF processing
├── src/main/resources/
│   ├── application.properties           # Application configuration
│   ├── logback-spring.xml              # Logging configuration
│   └── static/                         # Frontend assets
│       ├── app.css                     # Application styles
│       ├── app.js                      # Frontend JavaScript
│       ├── index.html                  # Main application page
│       └── jquery-3.7.1.min.js        # jQuery library
├── src/test/java/                      # Unit tests
├── logs/                               # Application log files
├── pom.xml                             # Maven configuration
└── README.md                           # This file
```

## Prerequisites

- **Java 17** or higher
- **Maven 3.6+**
- **MongoDB Atlas** account (or local MongoDB instance)
- **OpenAI API Key** (for AI vision features)

## Setup Instructions

### 1. Clone and Navigate
```bash
cd d:\workspace_recapmap\recapmap-core-java
```

### 2. Configure Database and API Keys
**Create your local configuration file:**
```bash
copy src\main\resources\application-local.properties.example src\main\resources\application-local.properties
```

**Edit `src/main/resources/application-local.properties` with your actual values:**
```properties
# MongoDB Atlas connection string
spring.data.mongodb.uri=mongodb+srv://YOUR_USERNAME:YOUR_PASSWORD@YOUR_CLUSTER.mongodb.net/YOUR_DATABASE?retryWrites=true&w=majority

# OpenAI API Key for AI vision features
openai.api.key=sk-your-openai-api-key-here
```

## 🔒 **Security Note**

This project uses Spring profiles for secure configuration management:
- **Sensitive data** (API keys, database credentials) are stored in `application-local.properties`
- **This file is git-ignored** and never committed to version control
- **Each developer** maintains their own local configuration file
- **No API keys or secrets** appear anywhere in the source code

### 3. Build and Run (One Command)
Use the provided combined batch script for both compilation and running:
```bash
mvn-compile-and-run-for-POWERSTATION.bat
```

### 4. Alternative: Build and Run Separately

**Build the Project:**
```bash
mvn-compile-for-POWERSTATION.bat
```

**Run the Application:**
```bash
mvn-springboot-run.bat
```

**Or use standard Maven commands:**
```bash
mvn clean compile
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## Quick Start Guide

**🚀 Fastest way to get started:**
```bash
mvn-compile-and-run-for-POWERSTATION.bat
```
This single command will compile and run the application in one step, perfect for development!

## Features

### 🚀 Core Functionality
- **Document Processing**: Upload and process PDF documents
- **AI Vision Integration**: Extract content using OpenAI Vision API
- **Real-time Processing**: Live document analysis and extraction
- **Web Interface**: User-friendly frontend for document management

### 🔧 Developer Features
- **Hot Reload**: Spring Boot DevTools for automatic restart during development
- **Comprehensive Logging**: Timestamp-based log files with rolling policies
- **RESTful APIs**: Well-structured REST endpoints for all functionality
- **MongoDB Integration**: Flexible document storage and retrieval

### 🛡️ Security & Configuration
- **Spring Security**: Authentication and authorization
- **CORS Configuration**: Cross-origin request handling
- **Environment-based Configuration**: Flexible deployment options

## Development

### Hot Reload
The application is configured with Spring Boot DevTools for hot reload functionality:
- Java file changes trigger automatic restart
- Static resources reload without restart
- LiveReload browser extension support on port 35729

### Logging
Application logs are stored in the `logs/` directory with timestamps:
- Format: `core-YYYY-MM-DD-HHMMSS-YYYY-MM-DD-N.log`
- Automatic log rotation when files exceed 100MB
- Keeps last 30 days of logs

### Configuration Files

#### application.properties
```properties
spring.application.name=core
spring.data.mongodb.uri=<your-mongodb-connection-string>
spring.devtools.restart.enabled=true
server.port=8080
```

#### logback-spring.xml
Comprehensive logging configuration with:
- Console and file appenders
- Timestamp-based file naming
- Log rotation policies
- Different log levels for different packages

## API Endpoints

### Authentication
- `POST /api/login` - User authentication
- `GET /api/user-info` - Get user information

### Document Processing
- `POST /api/upload` - Upload documents for processing
- `GET /api/documents` - List processed documents
- `POST /api/extract` - Extract content from documents

### AI Integration
- `POST /api/copilot/chat` - AI chat functionality
- `POST /api/vision/test` - Test AI vision capabilities
- `GET /api/copilot/conversations` - Get chat history

## Database Schema

### ExtractedDocument
```json
{
  "_id": "ObjectId",
  "filename": "string",
  "content": "string",
  "extractedText": "string",
  "processingDate": "Date",
  "metadata": {}
}
```

### ExtractionConfig
```json
{
  "_id": "ObjectId",
  "configName": "string",
  "parameters": {},
  "isActive": "boolean"
}
```

## Troubleshooting

### Common Issues

1. **Port 8080 already in use**
   - Change port in `application.properties`: `server.port=8081`

2. **MongoDB connection issues**
   - Verify connection string in `application.properties`
   - Check network connectivity to MongoDB Atlas

3. **Java version issues**
   - Ensure Java 17 is installed and `JAVA_HOME` is set correctly

4. **Hot reload not working**
   - Ensure DevTools dependency is in `pom.xml`
   - Check that your IDE is set to auto-compile

### Log File Locations
- Application logs: `logs/core-*.log`
- Spring Boot logs: Console output
- Error logs: `error.log` (if configured)

## Contributing

1. Follow the existing code structure and naming conventions
2. Use the `com.recapmap.core` package structure
3. Add unit tests for new functionality
4. Update this README when adding new features
5. Use the provided Maven batch scripts for consistency:
   - `mvn-compile-and-run-for-POWERSTATION.bat` - One-step compile and run (recommended)
   - `mvn-compile-for-POWERSTATION.bat` - Compile only
   - `mvn-springboot-run.bat` - Run only (after compilation)

## Version History

- **Current**: Spring Boot 3.4.5, Java 17, MongoDB Atlas integration
- **Previous**: Migrated from `ipcopilot` to `core` package structure

## License

[Add your license information here]

## Support

For technical support or questions, refer to the knowledge base in the `kb/` directory or check the conversation history in `MothershipBot-Conversation-History.md`.

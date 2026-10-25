# TrustVault — Secure Document Sharing with a Real Audit Trail

TrustVault is a web application built with Java Spring Boot that enables users to upload, manage, and securely share documents with permission-based access and audit logging.

## Features

- User registration and login
- Secure document upload, viewing, and downloading
- Permission-based document sharing (VIEW and DOWNLOAD)
- Audit logs for document-related activities
- Admin management
- Password recovery
- Private document storage using generated storage identifiers

## Technology Stack

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA / Hibernate
- MySQL 8
- Maven
- HTML, CSS, and JavaScript

## Prerequisites

Install the following before running the application:

- JDK 21
- MySQL 8
- Maven, or use the included Maven Wrapper

## Database Setup

1. Start your MySQL server.
2. Create a database:

```sql
CREATE DATABASE trustvault;
```

3. Configure your database username and password using your local environment configuration.

Do not commit database passwords, API keys, or other secrets to GitHub.

## Configuration

The application uses Spring Boot configuration in `src/main/resources/application.properties`.

Set the `DB_PASSWORD` environment variable to your local MySQL password before starting the application. Ensure that the configured database username matches your MySQL account.

## Run the Application

From the project root directory, run:

**Windows:**

```bat
mvnw.cmd spring-boot:run
```

Or, in Eclipse, run `TrustvaultApplication.java` as a Java Application.

The application is configured to use port `8082`.

Open:

http://localhost:8082

## File Upload Restrictions

The application is configured for a maximum upload size of 10 MB. Supported document types include PDF, TXT, PNG, and JPEG, subject to application validation.

## Security Notes

- Spring Security protects application routes.
- Passwords are handled using password hashing.
- Document access is controlled by permissions.
- Private uploaded files should not be committed to the repository.
- Review password recovery protections, access controls, and production configuration before deployment.

## Testing

Run the test suite using:

```bat
mvnw.cmd test
```

## Project Status

This project is being developed as a secure document-sharing application with an auditable access history.

## License

No license has been specified yet.

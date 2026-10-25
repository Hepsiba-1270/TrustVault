# 🔐 TrustVault
### Secure Document Sharing with a Real Audit Trail

**TrustVault** is a secure document-sharing web application built with Java and Spring Boot. It is designed to help users manage private documents, share access through permissions, and maintain an audit trail of document-related activities.

The goal is simple: make document sharing more controlled, transparent, and accountable.

---

## 📌 Table of Contents

- [Overview](#-overview)
- [Key Features](#-key-features)
- [Technology Stack](#-technology-stack)
- [Application Architecture](#-application-architecture)
- [Security Approach](#-security-approach)
- [Getting Started](#-getting-started)
- [Running Tests](#-running-tests)
- [Project Structure](#-project-structure)
- [Future Enhancements](#-future-enhancements)

---

## 🚀 Overview

Sharing sensitive documents requires more than simply uploading a file. Users need control over who can access their documents and a way to review document-related activity.

TrustVault addresses this need through a web application that combines authentication, permission-based sharing, private file storage, and audit logging.

### Why TrustVault?

- **Controlled access:** Support for different document-sharing permissions.
- **Privacy-focused storage:** Uploaded files are stored separately from the public web resources.
- **Account security:** Authentication and password hashing mechanisms.
- **Accountability:** Audit logging for document-related activities.
- **Administration:** Dedicated administrative functionality.

## ✨ Key Features

### 👤 User Authentication
- User registration and login.
- Password hashing using Spring Security's password-encoding mechanism.
- Password recovery functionality.

### 📁 Document Management
- Upload and manage documents.
- View and download documents subject to access permissions.
- Configured maximum upload size of 10 MB.
- Support for PDF, TXT, PNG, and JPEG files, subject to application validation.

### 🔑 Permission-Based Sharing
- `VIEW` permission for viewing shared documents.
- `DOWNLOAD` permission for downloading shared documents.
- Access checks intended to keep document operations within the permissions granted to users.

### 📋 Audit Trail
- Audit logging for document-related activities.
- A dedicated interface for reviewing audit information.
- A foundation for improving traceability and accountability in document access.

### 🛡️ Administrative Features
- Dedicated administrative interface.
- Protected administrative routes.
- Management functionality for authorized administrators.

---

## 🧰 Technology Stack

| Technology | Purpose |
|---|---|
| Java 21 | Application development |
| Spring Boot | Backend application framework |
| Spring Security | Authentication and authorization |
| Spring Data JPA / Hibernate | Database persistence and ORM |
| MySQL 8 | Relational database |
| Maven | Dependency management and build automation |
| HTML, CSS, JavaScript | Web interface |

---

## 🏗️ Application Architecture

TrustVault follows a layered Spring Boot application structure.

```text
User / Browser
      |
      v
Web Interface
(HTML, CSS, JavaScript)
      |
      v
Spring Boot Controllers
      |
      v
Security and Service Layers
      |
      +-------------------+
      |                   |
      v                   v
MySQL Database      Private File Storage
      |
      v
User, Document,
Permission and Audit Data
```

**Architecture overview:**

- **Controllers:** Handle incoming web requests.
- **Services:** Implement application and business logic.
- **Repositories:** Manage database access.
- **Entities:** Represent persisted application data.
- **Security components:** Enforce authentication and access restrictions.
- **Private file storage:** Keeps uploaded files outside the public static-resource directory.

---

## 🔒 Security Approach

Security is an important design consideration in TrustVault.

- **Authentication:** Spring Security protects authenticated application functionality.
- **Password protection:** Passwords are handled through password hashing rather than being stored as plain text.
- **Authorization:** Protected routes and document permissions help restrict access.
- **Private storage:** Uploaded documents are stored separately from public static resources.
- **CSRF protection:** Spring Security's CSRF protection is retained in the application configuration.
- **Configuration hygiene:** Database credentials should be supplied through local environment configuration and must not be committed to version control.

> **Security note:** These controls are part of the application's current design. Production readiness requires additional verification of authorization checks, audit-log integrity, password-recovery protections, file validation, and deployment configuration.

---

## ⚙️ Getting Started

### Prerequisites

Install the following:

- JDK 21
- MySQL 8
- Git
- Eclipse IDE or another Java IDE (optional)

The included Maven Wrapper can be used to run Maven commands without installing Maven separately.

### 1. Clone the repository

```bash
git clone https://github.com/Hepsiba-1270/TrustVault.git
cd TrustVault
```

### 2. Create the database

Start MySQL and execute:

```sql
CREATE DATABASE trustvault;
```

### 3. Configure database credentials

Open:

`src/main/resources/application.properties`

The application is configured to use the `trustvault` database on the local MySQL server and port `8082`.

Set the `DB_PASSWORD` environment variable to your local MySQL password. For Windows Command Prompt, you can set it for the current terminal session using:

```bat
set DB_PASSWORD=your_local_password
```

Replace the example value with your actual local password. Never commit the real password to GitHub.

Check that the configured database username matches your local MySQL account.

### 4. Run the application

From the project root, execute:

```bat
mvnw.cmd spring-boot:run
```

Alternatively, import the project into Eclipse as a Maven project and run the main Spring Boot application class.

### 5. Open the application

Visit:

**http://localhost:8082**

The application requires a running MySQL server and valid database configuration.

---

## 🧪 Running Tests

Run the automated test suite from the project root:

```bat
mvnw.cmd test
```

A successful Maven test run should finish with `BUILD SUCCESS`.

---

## 📂 Project Structure

```text
TrustVault/
├── src/
│   ├── main/
│   │   ├── java/com/trustvault/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── entity/
│   │   │   ├── repository/
│   │   │   ├── security/
│   │   │   └── service/
│   │   └── resources/
│   │       ├── static/
│   │       ├── templates/
│   │       └── application.properties
│   └── test/
├── .gitignore
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```

---

## 🖼️ Screenshots and Demo

Screenshots and a demonstration video can be added here after they are captured from the running application.

Suggested screenshots:

- Login and registration
- User dashboard
- Document management
- Permission management
- Audit log interface
- Administrative interface

---

## 🛣️ Future Enhancements

Potential improvements include:

- Expiring and revocable document-sharing links.
- Stronger password-recovery rate limiting and abuse prevention.
- More comprehensive security and authorization tests.
- Tamper-resistant audit logging.
- Production deployment with HTTPS and managed secrets.
- Automated CI checks and deployment workflows.
- Improved monitoring and operational documentation.

These are proposed enhancements and should not be interpreted as already implemented features.

---

## 🤝 Contributing

Contributions and suggestions are welcome. Before proposing changes:

1. Review the existing implementation.
2. Test the affected functionality.
3. Keep secrets and private documents out of version control.
4. Document any changes to setup or security behavior.

---

## 📄 License

No open-source license has been specified yet. Please add an appropriate license before permitting reuse or redistribution.

---

**TrustVault — Secure sharing. Controlled access. Accountable activity.**
# Expert File Download Service with Form‑Based Portal Login

This Spring Boot (Java 21 + Gradle) project demonstrates a **complete, expert approach of using actual login/password** to authenticate against a third‑party web portal (`resourceUrl`), fetch and parse protected content, and download files to disk. It leverages:

- **Form‑based authentication** (username/password) to the portal login endpoint.
- **Session cookie** management for subsequent authenticated requests.
- **Jsoup** for HTML parsing and link extraction.
- **Spring Data JPA** and **Flyway** for database migrations and duplicate‑download checks.
- **Externalized configuration** via `application.yml` and `.env`.

> "Please implement another complete, expert aproach of using actual login/password to that resourceUrl portal"

---

## Table of Contents

1. [Features](#features)
2. [Prerequisites](#prerequisites)
3. [Configuration](#configuration)
    - [.env file](#env-file)
    - [`application.yml`](#applicationyml)
4. [Running the Application](#running-the-application)
5. [Project Structure](#project-structure)
6. [Key Components](#key-components)
7. [Endpoints](#endpoints)
8. [Customization](#customization)

---

## Features

- **PortalAuthService**: Logs in via HTTP POST (form‑data) to `loginUrl` and retrieves session cookies.
- **FileDownloadService**: Uses the session cookie to fetch `resourceUrl`, scrapes file links, filters by extension, and downloads only new files.
- **DownloadedFileRepository**: Records each download in PostgreSQL, preventing duplicates.
- **ConfigurationProperties**: Binds portal and download settings from `application.yml`.
- **.env support**: Loads sensitive credentials and custom flags at startup.
- **Flyway**: Manages schema migrations (`downloaded_files` table).

## Prerequisites

- Java 21 SDK
- PostgreSQL database
- Gradle (wrapper included)

## Configuration

### .env file

Place a `.env` in the project root with:

```ini
# Portal credentials and URLs
PORTAL_LOGIN_URL=https://third-party-portal.com/login
PORTAL_RESOURCE_URL=https://third-party-portal.com/protected-page
PORTAL_USERNAME=yourUsername
PORTAL_PASSWORD=yourPassword

# Download directory
DOWNLOAD_DIR=./downloads

# Database
DB_NAME=mydatabase
DB_USERNAME=myuser
DB_PASSWORD=mypassword
```

### application.yml

In `src/main/resources/application.yml`:

```yaml
spring:
  config:
    import:
      - "optional:dotenv:./.env"

  datasource:
    url: jdbc:postgresql://localhost:5432/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: update
  flyway:
    baseline-on-migrate: true

server:
  port: 8080

app:
  portal:
    loginUrl:    ${PORTAL_LOGIN_URL}
    resourceUrl: ${PORTAL_RESOURCE_URL}
    username:    ${PORTAL_USERNAME}
    password:    ${PORTAL_PASSWORD}

download:
  dir: ${DOWNLOAD_DIR:./downloads}
```

---

## Running the Application

1. **Ensure** PostgreSQL is running and the database exists.
2. **Populate** `.env` with your portal credentials and URLs.
3. **Run** with Gradle:
   ```bash
   ./gradlew bootRun
   ```
4. **Observe** Flyway migrations and log messages confirming login, scraping, and downloads.

---

## Project Structure

```
file-download-project/
├── build.gradle
├── .env
├── src
│   └── main
│       ├── java/com/files/downloader
│       │   ├── FileDownloadApplication.java
│       │   ├── config
│       │   │   ├── PortalProperties.java
│       │   │   └── DownloadProperties.java
│       │   ├── controller
│       │   │   └── DownloadController.java
│       │   ├── model
│       │   │   └── DownloadedFile.java
│       │   ├── repository
│       │   │   └── DownloadedFileRepository.java
│       │   ├── service
│       │   │   ├── PortalAuthService.java
│       │   │   └── FileDownloadService.java
│       │   └── util
│       │       └── HtmlContentParsingService.java
│       └── resources
│           ├── application.yml
│           └── db/migration
│               └── V1__create_downloaded_files_table.sql
└── README.md
```

---

## Key Components

- **PortalAuthService**: Handles form‑based login, collects `Set-Cookie` headers.
- **FileDownloadService**: Fetches pages with authenticated cookies, downloads files, and records them.
- **Config Properties**: `@ConfigurationProperties(prefix="app.portal")` and `download.dir` for strong typing.
- **HtmlContentParsingService**: Parses HTML, extracts and filters JSON (e.g. by `tab_url` containing `-my-pro`).

---

## Endpoints

- **POST** `/api/download-files` &rarr; initiates login, scraping, and download.
- **GET** `/api/downloaded-files`  &rarr; lists all downloaded files in JSON.

---

## Customization

- Change login form field names in `PortalAuthService` if your portal uses different names.
- Adjust `isDownloadable(...)` in `FileDownloadService` for additional file types.
- Extend `HtmlContentParsingService` to handle more complex JSON/HTML parsing.

---

MIT © Your Name or Organization




## License

This project is licensed under the [MIT License](LICENSE).

---

## Contact

**Dzmitry Ivaniuta** — [diafter@gmail.com](mailto:diafter@gmail.com) — [GitHub](https://github.com/DimitryIvaniuta)
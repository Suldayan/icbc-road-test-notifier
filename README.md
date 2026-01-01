# ICBC Road Test Notifier (Legacy)

**Note on Project Status:** This tool was developed to automate appointment searches on the ICBC legacy portal. As of recent updates to ICBC's frontend architecture, the automation script is currently deprecated. This repository is maintained to demonstrate Event-Driven Architecture, Spring Modulith, and Web Automation implementation.

This is a Spring Boot application designed to monitor ICBC road test availability and send real-time email notifications when slots open up. It leverages Playwright for browser automation and Spring Modulith for internal event handling.

## 🛠 Tech Stack

* **Java 21 / Spring Boot 3**
* **Spring Modulith:** For domain-driven event handling
* **Playwright:** Used for robust browser automation and navigation
* **JUnit 5 / Mockito:** Comprehensive unit testing for notification logic
* **Docker:** Containerized for easy deployment on Render
* **H2 Database:** For persistent event logging and tracking

## 🚀 Deployment & Usage

The application is designed to be deployed as a Dockerized web service, triggered externally by a Cron job.

### 1. Build and Containerization

The project includes a specific Docker configuration for Render's environment.
```bash
# Build the image
docker build -t icbc-notifier .
```

### 2. Environment Variables

Configure the following variables in your hosting environment (e.g., Render Dashboard):

| Variable | Description | Example |
|----------|-------------|---------|
| `ICBC_LAST_NAME` | Your legal last name | `Doe` |
| `ICBC_LICENSE_NUMBER` | Your driver's license number (7 digits) | `1234567` |
| `ICBC_KEYWORD` | Your ICBC login keyword | `********` |
| `ICBC_PREFERRED_LOCATION` | Desired testing center | `Surrey, BC` |
| `ICBC_PREFERRED_DAYS` | Days of the week (comma-separated) | `MONDAY,TUESDAY,FRIDAY` |
| `ICBC_TIME_PREFERENCE` | Time preference (ANY, MORNING, AFTERNOON, EVENING) | `ANY` |
| `ICBC_DATE_RANGE_PREFERENCE_START_DATE` | Start date for appointment search | `2025-01-01` |
| `ICBC_DATE_RANGE_PREFERENCE_END_DATE` | End date for appointment search | `2025-12-31` |
| `MAIL_USERNAME` | SMTP Login (Gmail) | `your-bot@gmail.com` |
| `MAIL_PASSWORD` | Google App Password | `xxxx-xxxx-xxxx-xxxx` |

### 3. Triggering the Search

Since the app is stateless, it exposes a REST endpoint. Use a Cron service (like Cron-job.org) to ping the endpoint at your desired interval (e.g., every 15 minutes).

**Endpoint:** `POST /api/v1/appointments/check`
```bash
curl -X POST https://your-app-name.onrender.com/api/v1/appointments/check
```

## 🏗 Key Architectural Features

### Event-Driven Notifications

Instead of tightly coupling the search logic to the email service, the application uses Spring Modulith Events.

1. The `AppointmentService` publishes an `AppointmentFound` event.
2. The `EmailService` listens for this event asynchronously via `@ApplicationModuleListener`.
3. This allows for adding new notification channels (like SMS or Telegram) without touching the core search logic.

### Browser Automation

The search service utilizes Playwright in headless mode to authenticate, navigate the ICBC portal, and parse dynamic HTML content, handling complex session management and timeouts.

## 🧪 Testing

The project includes a suite of unit tests. To run the tests:
```bash
./mvnw test
```

Tests are configured via `src/test/resources/application.properties` to ensure no real emails are sent during the build process.
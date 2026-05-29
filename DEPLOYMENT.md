# Deployment

## Configuration model

This application now uses three sources:

1. One external `performance-management.properties` file for infrastructure secrets and deployment-level values.
2. `System Settings` page for operational values managed by admins.
3. Environment variables for optional external integrations.

The application will fail to start if datasource settings are not provided through the external properties file.

## External secrets file

Create:

```text
/data/secrets/performance-management/performance-management.properties
```

Copy the starter template from this repository root:

```bash
cp performance-management.properties /data/secrets/performance-management/performance-management.properties
```

Populate it with values for:

- `server.port`
- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`
- `spring.mail.host`
- `spring.mail.port`
- `spring.mail.username`
- `spring.mail.password`
- `host.url`
- `email.hr`
- `email.admin`
- `spring.servlet.multipart.location`

Optional:

- `TWILIO_ACCOUNT_SID`
- `TWILIO_AUTH_TOKEN`
- `TWILIO_WHATSAPP_FROM`
- `OPENAI_API_KEY`
- `OPENAI_API_URL`
- `BOOTSTRAP_ADMIN_EMAIL`
- `BOOTSTRAP_ADMIN_PASSWORD`

## Startup command

```bash
java \
  -Dspring.config.additional-location=file:/data/secrets/performance-management/performance-management.properties \
  -jar /data/apps/services/performance-management-0.0.1-SNAPSHOT.jar
```

## Profiles

Environment-specific `application-*.properties` profiles were removed from source.
Deployment now relies on:

- source `application.properties` (safe defaults only)
- one external `performance-management.properties` file (all deployment values)

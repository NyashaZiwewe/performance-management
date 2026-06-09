# Deployment

Build the application jar, then copy it to the target server using deployment credentials managed outside this repository.

Important:

- source configuration no longer contains datasource or mail credentials
- use one external secrets file only: `performance-management.properties`
- this repo includes a starter copy at project root: [performance-management.properties](performance-management.properties)
- run with `-Dspring.config.additional-location=file:/data/secrets/performance-management/performance-management.properties`
- see `DEPLOYMENT.md` for required properties and startup steps


Wanyengerai00
scp -rv target/performance-management-0.0.1-SNAPSHOT.jar root@207.180.195.48:/data/apps/services/performance-management-0.0.1-SNAPSHOT.jar
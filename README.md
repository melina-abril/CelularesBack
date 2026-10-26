# Venta de celulares — backend

Requiere Java 21 y Maven. Desde la raíz del repositorio, ejecutá `./mvnw spring-boot:run` para iniciar la API en el puerto 8080. Si el script no tiene permiso de ejecución, usá `sh mvnw spring-boot:run`. Para compilar y ejecutar los tests: `./mvnw verify`.

El perfil `dev` está activo por defecto. Usa HTTP Basic con los usuarios de prueba `cliente`, `vendedor`, `supervisor`, `backoffice` y `gerencia`; configurá su contraseña común mediante `DEV_USER_PASSWORD` antes de usar el entorno. El valor predeterminado `change-me-dev` es únicamente un ejemplo local. Vendedor y supervisor pertenecen a Palermo.

Swagger UI: `http://localhost:8080/swagger-ui.html`. Health: `http://localhost:8080/actuator/health`.

Los endpoints se agrupan bajo `/api/v1/{modulo}`. Cada módulo contiene `domain` (sin Spring), `application` (casos de uso y DTO), `infrastructure` (adaptadores en memoria) y `api` (controladores). Los módulos no importan clases de otros módulos; los tipos transversales viven en `shared`. Las respuestas y los datos de ejemplo de los adaptadores son dummy hasta conectar la persistencia y las integraciones. El perfil de desarrollo y sus usuarios no deben utilizarse en producción.

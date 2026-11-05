# Reserva Natural Lago Escondido - Backend

Backend REST y servicios administrativos de RNLE. Gestiona reservas, usuarios, contenido editorial, multimedia e integraciones externas.

## Stack

- Java 21
- Spring Boot 3.5.5
- Spring Web, Spring Data JPA, Spring Security, Validation y Actuator
- PostgreSQL 16
- Flyway integrado en el arranque de la aplicación
- JJWT 0.12.3 para autenticación mediante JWT
- Springdoc OpenAPI 2.8.11
- Apache POI 5.2.5 para exportaciones XLSX
- Twilio SDK 10.1.0 para WhatsApp
- Bucket4j 8.10.1 para rate limiting
- Testcontainers para pruebas de integración con PostgreSQL

## Funcionalidades

- Reservas de visitas particulares y educativas.
- Anticipación mínima de siete días calendario y validaciones de disponibilidad.
- Capacidad, calendario, estados y administración de reservas.
- Usuarios administrativos y autorización por roles.
- Proyectos, avances, documentos y galerías multimedia.
- Novedades con fecha editorial, contenido social y galerías.
- Contenido administrable del Home y de Conservar.
- Imágenes, documentos, videos legacy y videos externos de YouTube.
- Generación asistida de proyectos, avances y novedades mediante OpenAI.
- Notificaciones opcionales de WhatsApp mediante Twilio.

## Arquitectura y seguridad

La aplicación expone una API REST con Spring Boot. Las rutas administrativas requieren autenticación y autorización mediante Spring Security y JWT. CORS se restringe mediante `ALLOWED_ORIGINS` y el almacenamiento de credenciales debe realizarse fuera de Git.

El rate limiting protege los endpoints configurados. Las pruebas de autenticación y autorización deben conservarse al modificar controladores o filtros.

## Multimedia y almacenamiento

Las subidas administrativas admiten imágenes, documentos y videos, con autorizaciones temporales para las cargas directas. Los límites configurados por defecto son:

- Imágenes: 20 MiB.
- Documentos: 10 MiB.
- Videos: 50 MiB.
- Multipart general: 50 MB por archivo y 55 MB por solicitud.

Las imágenes se procesan con ImageMagick. El pipeline corrige la orientación, limita la resolución máxima a 2560 px, convierte a WebP y utiliza `-strip` para eliminar metadatos EXIF, GPS, XMP e IPTC. El procesamiento del backend es la etapa final de saneamiento; la optimización previa del navegador no lo reemplaza.

Los archivos se almacenan en `APP_UPLOAD_DIR`. En Compose se declara el volumen `app_uploads`; en producción, Coolify debe montar almacenamiento persistente en la ruta efectiva del contenedor y garantizar permisos de escritura para el usuario de la aplicación UID/GID 1001. La persistencia y los backups reales del servidor deben verificarse fuera del repositorio.

## Base de datos y migraciones

La aplicación utiliza PostgreSQL y Flyway con migraciones en `src/main/resources/db/migration`. La secuencia presente en este repositorio va de `V4__init.sql` a `V27__add_news_editorial_date.sql` e incluye reservas, usuarios, configuración, proyectos, avances, documentos, MediaAssets, Novedades y galerías multimedia.

Flyway está habilitado en el perfil de producción y valida el esquema mediante JPA. Antes de actualizar un entorno existente, revisar `flyway_schema_history`, realizar un backup verificable y comprobar el procedimiento de restauración. No reemplazar una base de producción con una copia de desarrollo: el contenido editorial debe migrarse selectivamente cuando corresponda.

## Variables de entorno

Los valores deben configurarse mediante Coolify, Docker Compose local u otro gestor seguro. No incluir valores reales en el repositorio.

| Variable | Uso |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | Perfil Spring activo |
| `DATABASE_URL` | URL JDBC de PostgreSQL |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | Acceso a PostgreSQL |
| `JWT_SECRET` / `JWT_EXPIRATION` | Firma y duración de JWT |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | Cuenta administrativa inicial |
| `FRONTEND_URL` | URL utilizada en enlaces del frontend y notificaciones |
| `ALLOWED_ORIGINS` | Orígenes CORS permitidos |
| `APP_UPLOAD_DIR` | Directorio persistente de archivos |
| `APP_UPLOAD_MAX_FILE_SIZE` / `APP_UPLOAD_MAX_REQUEST_SIZE` | Límites multipart generales |
| `APP_UPLOAD_MAX_IMAGE_SIZE_BYTES` | Límite de imágenes |
| `APP_UPLOAD_MAX_DOCUMENT_SIZE_BYTES` | Límite de documentos |
| `APP_UPLOAD_MAX_VIDEO_SIZE_BYTES` | Límite de videos |
| `APP_IMAGE_PROCESSING_COMMAND` | Ejecutable de ImageMagick |
| `APP_IMAGE_PROCESSING_TIMEOUT_SECONDS` | Timeout del procesamiento |
| `APP_IMAGE_PROCESSING_TEMP_DIR` | Directorio temporal de procesamiento |
| `OPENAI_API_KEY` / `OPENAI_MODEL` | Generación de contenido |
| `WHATSAPP_ENABLED` | Activa WhatsApp |
| `TWILIO_ACCOUNT_SID` / `TWILIO_AUTH_TOKEN` / `TWILIO_WHATSAPP_FROM` | Integración Twilio |
| `WHATSAPP_ADMIN_PHONES` | Destinatarios administrativos |
| `DEFAULT_CAPACITY` | Capacidad predeterminada |
| `PORT` / `CONTEXT_PATH` | Servidor HTTP |
| `LOG_FILE` / `JAVA_OPTS` | Logs y opciones de JVM |

`OPENAI_API_KEY`, `JWT_SECRET`, contraseñas, tokens y credenciales de Twilio son secretos. No registrarlos ni incluirlos en logs, ejemplos o commits.

## Desarrollo local

Requisitos: Java 21, Docker Desktop, Docker Compose y Git Bash o un shell compatible con `mvnw`.

Para levantar PostgreSQL y la aplicación con la configuración del repositorio:

```bash
docker compose -f docker-compose.dev.yml up -d
docker compose -f docker-compose.dev.yml logs -f app
```

La API local utiliza `http://localhost:8080` y PostgreSQL se publica en el puerto definido por `docker-compose.dev.yml`. El perfil `dev` habilita la configuración de desarrollo y Swagger.

Para ejecutar la aplicación fuera de Docker:

```bash
./mvnw spring-boot:run
```

El repositorio contiene `mvnw`; no asumir la existencia de `mvnw.cmd`.

## Pruebas

```bash
./mvnw test
```

Las pruebas de integración con `ReservalagoApplicationTests` utilizan Testcontainers y requieren que Maven pueda acceder a Docker Desktop. Ejecutarlas de forma aislada:

```bash
./mvnw -q -Dtest=ReservalagoApplicationTests test
```

El test utiliza una base PostgreSQL temporal; no debe configurarse contra una base existente o de producción.

## Despliegue y operación

El `Dockerfile` compila con Maven y Java 21, y genera una imagen runtime con Java 21, ImageMagick, soporte JPEG, PNG, WebP y lectura HEIC/HEIF, además de un usuario no root UID/GID 1001.

El backend se despliega mediante Coolify. El Compose de producción es una referencia del repositorio; la configuración efectiva de Coolify, los mounts, permisos, variables y backups deben comprobarse en el servidor.

Antes de actualizar:

1. Respaldar PostgreSQL y comprobar la restauración.
2. Respaldar el directorio persistente de uploads.
3. Verificar variables, CORS, espacio y permisos del usuario de la aplicación.
4. Revisar el estado de Flyway.
5. Desplegar el backend y comprobar Actuator, autenticación, reservas, contenido y multimedia.

Ante un incidente, conservar logs, identificar si hubo migraciones aplicadas y restaurar código, base y archivos según el procedimiento aprobado. Revertir el código no revierte automáticamente una migración de base de datos.

## Mantenimiento

Consultar logs de la aplicación, Actuator y PostgreSQL. No ejecutar comandos destructivos ni migraciones manuales sobre bases reales. No versionar `.env`, credenciales, tokens, dumps, exports, datos personales, archivos de staging ni entornos locales de Postman.

## Observaciones sobre pom.xml y configuración heredada

- Confirmado por código y configuración: las dependencias de Spring Boot, PostgreSQL, Flyway, JWT, Springdoc, Apache POI, Twilio, Bucket4j y Testcontainers están declaradas y sus APIs aparecen utilizadas en el código o en las pruebas.
- Confirmado por `Dockerfile`: la compilación usa Maven con Java 21 y `mvn clean package -DskipTests`; la imagen final instala ImageMagick y ejecuta el JAR con un usuario no root.
- Confirmado por `application-prod.yml`: Flyway se ejecuta al iniciar Spring Boot con `classpath:db/migration`; la configuración de producción no depende del plugin Flyway de Maven para el arranque normal.
- El `flyway-maven-plugin` está declarado en `pom.xml` con una URL, usuario y versión propias. No forma parte del comando del Dockerfile ni del procedimiento normal de despliegue; cualquier uso manual requiere revisar compatibilidad y destino antes de ejecutarlo.
- `docker-compose.prod.yml`, Coolify y los documentos históricos contienen procedimientos adicionales. La configuración efectiva de Coolify, sus volúmenes, permisos, variables y estrategia de despliegue requieren confirmación en el servidor.
- `docker-compose.simple.yml.bak`, `PLAN_DESPLIEGUE.md`, `README-DOCKER.md`, `ARQUITECTURA.md` y `SSL-SETUP.md` requieren una revisión documental independiente para separar instrucciones históricas de procedimientos vigentes.

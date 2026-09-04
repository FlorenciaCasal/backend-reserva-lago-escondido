# Checklist de producción — Etapa 1

## 1. Código y ramas

- [x] Backend productivo configurado en Coolify con `FlorenciaCasal/backend-reserva-lago-escondido`
- [x] Rama productiva: `main`
- [x] Webhook GitHub → Coolify funcionando
- [ ] Terminar cambios funcionales/UX de Etapa 1
- [ ] Ejecutar auditoría final de frontend
- [ ] Ejecutar auditoría final de backend
- [ ] Commit de todos los cambios pendientes
- [ ] Push de ramas de Etapa 1
- [ ] Verificar working tree limpio en ambos repos
- [ ] NO mergear a `main` hasta terminar todas las validaciones

## 2. Contenido inicial

- [ ] Reunión final con empresa para aprobar contenido
- [ ] Definir texto definitivo de Conservar
- [ ] Comparar contenido aprobado contra V24/V25
- [ ] Crear V26 solo si el contenido aprobado difiere
- [ ] No modificar V24/V25
- [ ] Guardar aparte contenido final de los Proyectos que se cargarán en producción
- [ ] Guardar imágenes/videos finales de esos Proyectos
- [ ] Guardar aparte contenido final de las Novedades que se cargarán en producción
- [ ] Guardar imágenes/videos finales de esas Novedades
- [ ] Recrear manualmente Proyectos/Novedades desde el admin productivo después del deploy

## 3. PostgreSQL / Flyway

- [x] Desarrollo y producción usan bases diferentes
- [x] Producción tiene PostgreSQL persistente
- [ ] Auditar todas las migraciones nuevas de Etapa 1
- [ ] Confirmar checksums y orden de migraciones
- [ ] Confirmar que ninguna migración sea destructiva
- [ ] Confirmar V24/V25 sin modificaciones posteriores
- [ ] Confirmar V26 si corresponde
- [ ] Verificar backup de PostgreSQL antes del deploy
- [ ] Confirmar que no se eliminen/recreen volúmenes productivos
- [ ] No ejecutar `down -v`

## 4. Uploads / almacenamiento persistente

- [x] Ruta usada por backend identificada: `/var/lib/lago-escondido/uploads`
- [x] Coolify actualmente no tiene Persistent Storage para la aplicación
- [ ] Definir con Álvaro almacenamiento para Etapa 1
- [ ] Crear/montar almacenamiento persistente
- [ ] Confirmar permisos de escritura del usuario del contenedor
- [ ] Confirmar capacidad inicial acordada (referencia evaluada: 20 GB)
- [ ] Confirmar estrategia de backup de uploads
- [ ] Probar upload de imagen
- [ ] Probar upload de documento
- [ ] Probar upload de MP4
- [ ] Hacer redeploy
- [ ] Confirmar que los archivos siguen existiendo después del redeploy

## 5. OpenAI

- [ ] Obtener `OPENAI_API_KEY` productiva de la empresa
- [ ] No usar la API key personal de desarrollo
- [ ] Agregar `OPENAI_API_KEY` en Coolify
- [ ] Definir/agregar `OPENAI_MODEL`
- [ ] Verificar generación IA de Proyecto en producción
- [ ] Verificar generación IA de Novedad en producción
- [ ] Verificar generación Instagram
- [ ] Verificar generación Facebook

## 6. Límites de archivos

- [ ] Verificar `SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE=50MB`
- [ ] Verificar `SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE=55MB`
- [ ] Verificar `APP_UPLOAD_MAX_IMAGE_SIZE=5242880`
- [ ] Verificar `APP_UPLOAD_MAX_DOCUMENT_SIZE=10485760`
- [ ] Verificar `APP_UPLOAD_MAX_VIDEO_SIZE=52428800`

## 7. Variables backend / Coolify

- [ ] Confirmar `SPRING_PROFILES_ACTIVE=prod`
- [ ] Confirmar `DATABASE_URL`
- [ ] Confirmar `DATABASE_USERNAME`
- [ ] Confirmar `DATABASE_PASSWORD`
- [ ] Confirmar `JWT_SECRET`
- [ ] Confirmar `FRONTEND_URL`
- [ ] Confirmar `ALLOWED_ORIGINS`
- [ ] Confirmar variables Twilio existentes
- [ ] Agregar/verificar `APP_UPLOAD_DIR=/var/lib/lago-escondido/uploads`

No incluir valores reales de secretos en este archivo.

## 8. Frontend / Vercel

- [ ] Confirmar `NEXT_PUBLIC_API_URL` productiva
- [ ] Confirmar `APP_ORIGIN=https://www.reservalagoescondido.com.ar`
- [ ] Revisar variables obsoletas y eliminarlas si corresponde
- [ ] Ejecutar `npm run lint`
- [ ] Ejecutar `npm run build`
- [ ] Verificar build productivo antes del merge

## 9. SEO

- [ ] Verificar canonical en producción
- [ ] Verificar metadata de Home
- [ ] Verificar metadata de Proyecto
- [ ] Verificar metadata de Novedad
- [ ] Verificar Open Graph
- [ ] Verificar `/robots.txt`
- [ ] Verificar `/sitemap.xml`
- [ ] Confirmar que sitemap contiene solo contenido `PUBLISHED`
- [ ] Confirmar que no aparezcan localhost/URLs preview
- [ ] Verificar JSON-LD Organization/WebSite/NewsArticle

## 10. Pruebas funcionales finales

- [ ] Login ADMIN
- [ ] Login MANAGER
- [ ] Crear Proyecto con IA
- [ ] Editar Proyecto manualmente
- [ ] Imagen/video principal de Proyecto
- [ ] Galería
- [ ] Avances
- [ ] Documentos
- [ ] Publicar/archivar/eliminar Proyecto
- [ ] Crear Novedad con IA
- [ ] Editar Novedad manualmente
- [ ] Imagen/video principal de Novedad
- [ ] Galería
- [ ] Instagram/Facebook
- [ ] Publicar/archivar/eliminar Novedad
- [ ] Conservar desde admin
- [ ] Responsive admin
- [ ] Responsive público

## 11. Deploy final

- [ ] Confirmar backup antes del deploy
- [ ] Confirmar storage persistente listo
- [ ] Confirmar variables nuevas listas
- [ ] Confirmar migraciones revisadas
- [ ] Merge controlado de Etapa 1 a `main`
- [ ] Confirmar webhook recibido
- [ ] Confirmar deploy Coolify exitoso
- [ ] Confirmar `/actuator/health`
- [ ] Confirmar frontend Vercel
- [ ] Ejecutar smoke test productivo
- [ ] Cargar contenido inicial aprobado desde admin
- [ ] Verificación final con la empresa

## Regla de seguridad

Nunca:

- borrar volúmenes productivos;
- usar `docker compose down -v`;
- copiar `.env` o secretos al repo;
- modificar migraciones Flyway ya aplicadas;
- copiar IDs/rutas de `media_assets` desde desarrollo a producción;
- mergear a `main` sin backup y checklist de pre-deploy completado.

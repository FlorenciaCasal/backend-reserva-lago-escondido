package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.dto.MediaAssetResponse;
import com.luismunozse.reservalago.model.MediaAsset;
import com.luismunozse.reservalago.model.MediaAssetKind;
import com.luismunozse.reservalago.model.NewsStatus;
import com.luismunozse.reservalago.model.ProjectStatus;
import com.luismunozse.reservalago.repo.MediaAssetRepository;
import com.luismunozse.reservalago.repo.NewsImageRepository;
import com.luismunozse.reservalago.repo.NewsGalleryItemRepository;
import com.luismunozse.reservalago.repo.NewsRepository;
import com.luismunozse.reservalago.repo.ProjectAdvanceRepository;
import com.luismunozse.reservalago.repo.ProjectAdvanceGalleryItemRepository;
import com.luismunozse.reservalago.repo.ProjectDocumentRepository;
import com.luismunozse.reservalago.repo.ProjectImageRepository;
import com.luismunozse.reservalago.repo.ProjectGalleryItemRepository;
import com.luismunozse.reservalago.repo.ProjectRepository;
import com.luismunozse.reservalago.repo.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class MediaAssetService {

    private static final Set<String> PUBLIC_SYSTEM_CONFIG_MEDIA_KEYS = Set.of(
            "home_hero_image_url",
            "home_visits_image_url"
    );

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "heic", "heif");

    private static final Set<String> ALLOWED_DOCUMENT_CONTENT_TYPES = Set.of(
            MediaType.APPLICATION_PDF_VALUE,
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
    private static final Set<String> ALLOWED_DOCUMENT_EXTENSIONS = Set.of("pdf", "doc", "docx");

    private static final Set<String> ALLOWED_VIDEO_CONTENT_TYPES = Set.of("video/mp4");
    private static final Set<String> ALLOWED_VIDEO_EXTENSIONS = Set.of("mp4");

    private final MediaAssetRepository mediaAssetRepository;
    private final ProjectRepository projectRepository;
    private final ProjectImageRepository projectImageRepository;
    private final ProjectGalleryItemRepository projectGalleryItemRepository;
    private final ProjectDocumentRepository projectDocumentRepository;
    private final ProjectAdvanceRepository projectAdvanceRepository;
    private final ProjectAdvanceGalleryItemRepository projectAdvanceGalleryItemRepository;
    private final NewsRepository newsRepository;
    private final NewsImageRepository newsImageRepository;
    private final NewsGalleryItemRepository newsGalleryItemRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final ImageProcessingService imageProcessingService;

    @Value("${app.upload.dir:/var/lib/lago-escondido/uploads}")
    private String uploadDir;

    @Value("${app.upload.max-image-size:20971520}")
    private long maxImageSize;

    @Value("${app.upload.max-document-size:10485760}")
    private long maxDocumentSize;

    @Value("${app.upload.max-video-size:52428800}")
    private long maxVideoSize;

    public MediaAssetResponse uploadImage(MultipartFile file) {
        validateImageFile(file);
        return storeProcessedImage(file);
    }

    public MediaAssetResponse uploadDocument(MultipartFile file) {
        validateFile(file, ALLOWED_DOCUMENT_CONTENT_TYPES, ALLOWED_DOCUMENT_EXTENSIONS, maxDocumentSize, "documento");
        return storeFile(file, MediaAssetKind.DOCUMENT, "documents");
    }

    public MediaAssetResponse uploadVideo(MultipartFile file) {
        validateFile(file, ALLOWED_VIDEO_CONTENT_TYPES, ALLOWED_VIDEO_EXTENSIONS, maxVideoSize, "video");
        return storeFile(file, MediaAssetKind.VIDEO, "videos");
    }

    public MediaAsset findImageAsset(UUID id) {
        MediaAsset asset = findAsset(id);
        if (asset.getKind() != MediaAssetKind.IMAGE) {
            throw new ResponseStatusException(BAD_REQUEST, "El archivo no es una imagen");
        }
        return asset;
    }

    public MediaAsset findDocumentAsset(UUID id) {
        MediaAsset asset = findAsset(id);
        if (asset.getKind() != MediaAssetKind.DOCUMENT) {
            throw new ResponseStatusException(BAD_REQUEST, "El archivo no es un documento");
        }
        return asset;
    }

    public MediaAsset findVideoAsset(UUID id) {
        MediaAsset asset = findAsset(id);
        if (asset.getKind() != MediaAssetKind.VIDEO) {
            throw new ResponseStatusException(BAD_REQUEST, "El archivo no es un video");
        }
        return asset;
    }

    public ServedMedia loadForRequest(UUID id) {
        MediaAsset asset = findAsset(id);

        if (!canRead(asset.getId())) {
            throw new ResponseStatusException(FORBIDDEN, "No se puede acceder a este archivo");
        }

        Path path = resolveStorageKey(asset.getStorageKey());
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new ResponseStatusException(NOT_FOUND, "Archivo no encontrado");
        }

        try {
            return new ServedMedia(new UrlResource(path.toUri()), asset.getContentType(), asset.getSizeBytes(), asset.getOriginalFilename());
        } catch (MalformedURLException ex) {
            throw new ResponseStatusException(NOT_FOUND, "Archivo no encontrado");
        }
    }

    public MediaAssetResponse toResponse(MediaAsset asset) {
        return new MediaAssetResponse(
                asset.getId(),
                asset.getKind(),
                "/api/media/" + asset.getId(),
                asset.getOriginalFilename(),
                asset.getContentType(),
                asset.getSizeBytes(),
                asset.getChecksum(),
                asset.getCreatedAt()
        );
    }

    private MediaAssetResponse storeProcessedImage(MultipartFile file) {
        UUID fileId = UUID.randomUUID();
        String storageKey = "images/" + fileId + ".webp";
        Path target = resolveStorageKey(storageKey);

        try {
            imageProcessingService.process(file, target);

            MediaAsset asset = new MediaAsset();
            asset.setKind(MediaAssetKind.IMAGE);
            asset.setStorageProvider("local");
            asset.setStorageKey(storageKey);
            asset.setOriginalFilename(safeOriginalFilename(file.getOriginalFilename()));
            asset.setContentType("image/webp");
            asset.setSizeBytes(Files.size(target));
            asset.setChecksum(sha256(target));

            return toResponse(mediaAssetRepository.save(asset));
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo guardar la imagen", ex);
        }
    }

    private MediaAssetResponse storeFile(MultipartFile file, MediaAssetKind kind, String folder) {
        UUID fileId = UUID.randomUUID();
        String extension = extensionOf(file.getOriginalFilename());
        String storageKey = folder + "/" + fileId + "." + extension;
        Path target = resolveStorageKey(storageKey);

        try {
            Files.createDirectories(target.getParent());
            byte[] bytes = file.getBytes();
            Files.write(target, bytes);

            MediaAsset asset = new MediaAsset();
            asset.setKind(kind);
            asset.setStorageProvider("local");
            asset.setStorageKey(storageKey);
            asset.setOriginalFilename(safeOriginalFilename(file.getOriginalFilename()));
            asset.setContentType(file.getContentType());
            asset.setSizeBytes(file.getSize());
            asset.setChecksum(sha256(bytes));

            return toResponse(mediaAssetRepository.save(asset));
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo guardar el archivo", ex);
        }
    }

    private MediaAsset findAsset(UUID id) {
        return mediaAssetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Archivo no encontrado"));
    }

    private boolean canRead(UUID mediaAssetId) {
        if (isAdminOrManager()) {
            return true;
        }

        return projectRepository.existsByImageAssetIdAndStatus(mediaAssetId, ProjectStatus.PUBLISHED)
                || projectRepository.existsByVideoAssetIdAndStatus(mediaAssetId, ProjectStatus.PUBLISHED)
                || projectImageRepository.existsPublishedAssociation(mediaAssetId, ProjectStatus.PUBLISHED)
                || projectGalleryItemRepository.existsPublishedAssociation(mediaAssetId, ProjectStatus.PUBLISHED)
                || projectDocumentRepository.existsPublishedAssociation(mediaAssetId, ProjectStatus.PUBLISHED)
                || projectAdvanceRepository.existsPublishedImageAssociation(mediaAssetId, ProjectStatus.PUBLISHED)
                || projectAdvanceRepository.existsPublishedVideoAssociation(mediaAssetId, ProjectStatus.PUBLISHED)
                || projectAdvanceGalleryItemRepository.existsPublishedAssociation(mediaAssetId, ProjectStatus.PUBLISHED)
                || newsRepository.existsByImageAssetIdAndStatus(mediaAssetId, NewsStatus.PUBLISHED)
                || newsRepository.existsByVideoAssetIdAndStatus(mediaAssetId, NewsStatus.PUBLISHED)
                || newsImageRepository.existsPublishedAssociation(mediaAssetId, NewsStatus.PUBLISHED)
                || newsGalleryItemRepository.existsPublishedAssociation(mediaAssetId, NewsStatus.PUBLISHED)
                || isReferencedByPublicSystemConfig(mediaAssetId);
    }

    private boolean isReferencedByPublicSystemConfig(UUID mediaAssetId) {
        String mediaUrl = "/api/media/" + mediaAssetId;
        return PUBLIC_SYSTEM_CONFIG_MEDIA_KEYS.stream()
                .anyMatch(key -> systemConfigRepository.existsByConfigKeyAndConfigValue(key, mediaUrl));
    }

    private boolean isAdminOrManager() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) return false;

        return authentication.getAuthorities().stream().anyMatch(authority ->
                "ROLE_ADMIN".equals(authority.getAuthority()) || "ROLE_MANAGER".equals(authority.getAuthority())
        );
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "El archivo es obligatorio");
        }
        if (file.getSize() > maxImageSize) {
            throw new ResponseStatusException(BAD_REQUEST, "El archivo supera el tamano maximo permitido");
        }

        String extension = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(BAD_REQUEST, "Extension de imagen no permitida");
        }
    }

    private void validateFile(
            MultipartFile file,
            Set<String> allowedContentTypes,
            Set<String> allowedExtensions,
            long maxSize,
            String label
    ) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "El archivo es obligatorio");
        }
        if (file.getSize() > maxSize) {
            throw new ResponseStatusException(BAD_REQUEST, "El archivo supera el tamano maximo permitido");
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!allowedContentTypes.contains(contentType)) {
            throw new ResponseStatusException(BAD_REQUEST, "Tipo de " + label + " no permitido");
        }

        String extension = extensionOf(file.getOriginalFilename());
        if (!allowedExtensions.contains(extension)) {
            throw new ResponseStatusException(BAD_REQUEST, "Extension de " + label + " no permitida");
        }
    }

    private Path resolveStorageKey(String storageKey) {
        Path root = Path.of(uploadDir).toAbsolutePath().normalize();
        Path resolved = root.resolve(storageKey).normalize();
        if (!resolved.startsWith(root)) {
            throw new ResponseStatusException(BAD_REQUEST, "Ruta de archivo invalida");
        }
        return resolved;
    }

    private String extensionOf(String filename) {
        String safeName = safeOriginalFilename(filename);
        int dot = safeName.lastIndexOf('.');
        if (dot < 0 || dot == safeName.length() - 1) return "";
        return safeName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String safeOriginalFilename(String filename) {
        String value = filename == null ? "archivo" : filename;
        value = value.replace('\\', '/');
        int slash = value.lastIndexOf('/');
        if (slash >= 0) value = value.substring(slash + 1);
        value = value.replaceAll("[^A-Za-z0-9._-]", "_");
        return value.isBlank() ? "archivo" : value;
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    private String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    public record ServedMedia(Resource resource, String contentType, long sizeBytes, String originalFilename) {
    }
}

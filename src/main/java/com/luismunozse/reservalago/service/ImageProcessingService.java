package com.luismunozse.reservalago.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Semaphore;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Service
@RequiredArgsConstructor
public class ImageProcessingService {

    private static final Set<String> ALLOWED_FORMATS = Set.of("JPEG", "PNG", "WEBP", "HEIC", "HEIF");
    private static final long MAX_PIXELS = 50_000_000L;
    private static final int MAX_DIMENSION = 9_000;
    private static final int MAX_OUTPUT_DIMENSION = 2_560;
    private static final int WEBP_QUALITY = 82;
    private static final int WEBP_ALPHA_QUALITY = 85;

    private final ImageMagickProcessRunner processRunner;
    private final Semaphore processingSemaphore = new Semaphore(1);

    @Value("${app.image-processing.command:magick}")
    private String magickCommand;

    @Value("${app.image-processing.timeout-seconds:30}")
    private long timeoutSeconds;

    @Value("${app.image-processing.temp-dir:}")
    private String configuredTempDir;

    public ProcessedImage process(MultipartFile file, Path target) {
        acquireSlot();
        Path tempDir = null;

        try {
            Files.createDirectories(target.getParent());
            tempDir = createTempDirectory();
            Path original = tempDir.resolve("original" + temporarySuffix(file.getOriginalFilename()));
            Path processed = tempDir.resolve("processed.webp");

            try (InputStream input = file.getInputStream()) {
                Files.copy(input, original, StandardCopyOption.REPLACE_EXISTING);
            }

            ensureSupportedMagicBytes(original);
            ImageInfo sourceInfo = identify(original);
            validateImageInfo(sourceInfo);
            convertToWebp(original, processed);

            ImageInfo outputInfo = identify(processed);
            if (!"WEBP".equals(outputInfo.format())) {
                throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "No se pudo procesar la imagen subida");
            }

            Files.move(processed, target, StandardCopyOption.REPLACE_EXISTING);
            return new ProcessedImage(outputInfo.width(), outputInfo.height());
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo procesar la imagen subida", ex);
        } finally {
            deleteRecursively(tempDir);
            processingSemaphore.release();
        }
    }

    private void acquireSlot() {
        try {
            processingSemaphore.acquire();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "No se pudo procesar la imagen subida");
        }
    }

    private Path createTempDirectory() throws IOException {
        if (configuredTempDir == null || configuredTempDir.isBlank()) {
            return Files.createTempDirectory("lago-image-");
        }
        Path root = Path.of(configuredTempDir).toAbsolutePath().normalize();
        Files.createDirectories(root);
        return Files.createTempDirectory(root, "lago-image-");
    }

    private void ensureSupportedMagicBytes(Path path) throws IOException {
        byte[] header = new byte[32];
        int read;
        try (InputStream input = Files.newInputStream(path)) {
            read = input.read(header);
        }

        Optional<String> format = detectMagicFormat(header, read);
        if (format.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "El archivo no es una imagen compatible");
        }
    }

    private Optional<String> detectMagicFormat(byte[] header, int length) {
        if (length >= 3 && unsigned(header[0]) == 0xFF && unsigned(header[1]) == 0xD8 && unsigned(header[2]) == 0xFF) {
            return Optional.of("JPEG");
        }
        if (length >= 8
                && unsigned(header[0]) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47
                && header[4] == 0x0D && header[5] == 0x0A && header[6] == 0x1A && header[7] == 0x0A) {
            return Optional.of("PNG");
        }
        if (length >= 12
                && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
            return Optional.of("WEBP");
        }
        if (length >= 12 && header[4] == 'f' && header[5] == 't' && header[6] == 'y' && header[7] == 'p') {
            String brand = new String(header, 8, Math.min(16, length - 8), StandardCharsets.US_ASCII).toLowerCase(Locale.ROOT);
            if (brand.contains("heic") || brand.contains("heix") || brand.contains("hevc") || brand.contains("hevx")) {
                return Optional.of("HEIC");
            }
            if (brand.contains("mif1") || brand.contains("msf1")) {
                return Optional.of("HEIF");
            }
        }
        return Optional.empty();
    }

    private int unsigned(byte value) {
        return value & 0xFF;
    }

    private ImageInfo identify(Path image) {
        ImageMagickProcessRunner.CommandResult result = processRunner.run(
                List.of(
                        magickCommand,
                        "identify",
                        "-quiet",
                        "-format",
                        "%m\n%w\n%h\n%n\n",
                        image.toString()
                ),
                Duration.ofSeconds(timeoutSeconds)
        );

        if (result.timedOut()) {
            throw new ResponseStatusException(BAD_REQUEST, "La imagen tardo demasiado en procesarse");
        }
        if (result.exitCode() != 0) {
            throw new ResponseStatusException(BAD_REQUEST, "No se pudo leer la imagen subida");
        }

        String[] lines = result.stdout().lines().toArray(String[]::new);
        if (lines.length < 4) {
            throw new ResponseStatusException(BAD_REQUEST, "No se pudo leer la imagen subida");
        }

        try {
            String format = lines[0].trim().toUpperCase(Locale.ROOT);
            int width = Integer.parseInt(lines[1].trim());
            int height = Integer.parseInt(lines[2].trim());
            int frames = Integer.parseInt(lines[3].trim());
            return new ImageInfo(format, width, height, frames);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(BAD_REQUEST, "No se pudo leer la imagen subida");
        }
    }

    private void validateImageInfo(ImageInfo info) {
        if (!ALLOWED_FORMATS.contains(info.format())) {
            throw new ResponseStatusException(BAD_REQUEST, "Formato de imagen no permitido");
        }
        if (info.frames() != 1) {
            throw new ResponseStatusException(BAD_REQUEST, "La imagen debe tener un unico frame");
        }
        if (info.width() <= 0 || info.height() <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "No se pudo leer la dimension de la imagen");
        }
        if (info.width() > MAX_DIMENSION || info.height() > MAX_DIMENSION) {
            throw new ResponseStatusException(BAD_REQUEST, "La imagen supera la dimension maxima permitida");
        }
        long pixels = (long) info.width() * (long) info.height();
        if (pixels > MAX_PIXELS) {
            throw new ResponseStatusException(BAD_REQUEST, "La imagen supera la resolucion maxima permitida");
        }
    }

    private void convertToWebp(Path original, Path processed) {
        ImageMagickProcessRunner.CommandResult result = processRunner.run(
                List.of(
                        magickCommand,
                        original.toString() + "[0]",
                        "-auto-orient",
                        "-resize",
                        MAX_OUTPUT_DIMENSION + "x" + MAX_OUTPUT_DIMENSION + ">",
                        "-strip",
                        "-quality",
                        String.valueOf(WEBP_QUALITY),
                        "-define",
                        "webp:alpha-quality=" + WEBP_ALPHA_QUALITY,
                        processed.toString()
                ),
                Duration.ofSeconds(timeoutSeconds)
        );

        if (result.timedOut()) {
            throw new ResponseStatusException(BAD_REQUEST, "La imagen tardo demasiado en procesarse");
        }
        if (result.exitCode() != 0) {
            throw new ResponseStatusException(BAD_REQUEST, "No se pudo optimizar la imagen subida");
        }
        try {
            if (!Files.exists(processed) || Files.size(processed) == 0) {
                throw new ResponseStatusException(BAD_REQUEST, "No se pudo optimizar la imagen subida");
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo procesar la imagen subida", ex);
        }
    }

    private String temporarySuffix(String filename) {
        if (filename == null) return ".upload";
        String safe = filename.replace('\\', '/');
        int slash = safe.lastIndexOf('/');
        if (slash >= 0) safe = safe.substring(slash + 1);
        int dot = safe.lastIndexOf('.');
        if (dot < 0 || dot == safe.length() - 1) return ".upload";
        String extension = safe.substring(dot + 1).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return extension.isBlank() ? ".upload" : "." + extension;
    }

    private void deleteRecursively(Path path) {
        if (path == null || !Files.exists(path)) return;
        try (var stream = Files.walk(path)) {
            stream.sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(item -> {
                try {
                    Files.deleteIfExists(item);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    private record ImageInfo(String format, int width, int height, int frames) {
    }

    public record ProcessedImage(int width, int height) {
    }
}
package com.luismunozse.reservalago.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageProcessingServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldProcessValidJpegAndCleanTemporaryFiles() throws IOException {
        FakeImageMagickRunner runner = new FakeImageMagickRunner();
        runner.identify("JPEG", 5712, 4284, 1);
        runner.convertSucceeds();
        runner.identify("WEBP", 2560, 1920, 1);
        ImageProcessingService service = service(runner);
        Path target = tempDir.resolve("uploads/images/foto.webp");

        service.process(file("foto.jpg", "image/jpeg", jpegBytes()), target);

        assertThat(Files.readString(target)).isEqualTo("optimized-webp");
        assertThat(runner.convertCommand).contains("-auto-orient", "-strip", "-quality", "82", "-resize", "2560x2560>");
        assertThat(Files.list(tempDir).filter(path -> path.getFileName().toString().startsWith("lago-image-")).toList()).isEmpty();
    }

    @Test
    void shouldProcessHeicWithEmptyMimeWhenMagicBytesAreCompatible() throws IOException {
        FakeImageMagickRunner runner = new FakeImageMagickRunner();
        runner.identify("HEIC", 6048, 8064, 1);
        runner.convertSucceeds();
        runner.identify("WEBP", 1920, 2560, 1);
        ImageProcessingService service = service(runner);
        Path target = tempDir.resolve("uploads/images/foto.webp");

        service.process(file("foto.heic", "", heicBytes()), target);

        assertThat(Files.exists(target)).isTrue();
        assertThat(runner.commands).hasSize(3);
    }

    @Test
    void shouldProcessValidPng() throws IOException {
        FakeImageMagickRunner runner = new FakeImageMagickRunner();
        runner.identify("PNG", 1600, 900, 1);
        runner.convertSucceeds();
        runner.identify("WEBP", 1600, 900, 1);
        ImageProcessingService service = service(runner);
        Path target = tempDir.resolve("uploads/images/foto.webp");

        service.process(file("foto.png", "image/png", pngBytes()), target);

        assertThat(Files.exists(target)).isTrue();
    }

    @Test
    void shouldProcessValidWebp() throws IOException {
        FakeImageMagickRunner runner = new FakeImageMagickRunner();
        runner.identify("WEBP", 1200, 800, 1);
        runner.convertSucceeds();
        runner.identify("WEBP", 1200, 800, 1);
        ImageProcessingService service = service(runner);
        Path target = tempDir.resolve("uploads/images/foto.webp");

        service.process(file("foto.webp", "image/webp", webpBytes()), target);

        assertThat(Files.exists(target)).isTrue();
    }

    @Test
    void shouldRejectFakeHeicBeforeRunningImageMagick() {
        FakeImageMagickRunner runner = new FakeImageMagickRunner();
        ImageProcessingService service = service(runner);
        Path target = tempDir.resolve("uploads/images/foto.webp");

        assertThatThrownBy(() -> service.process(file("foto.heic", "application/octet-stream", "%PDF".getBytes()), target))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("El archivo no es una imagen compatible");

        assertThat(runner.commands).isEmpty();
        assertThat(Files.exists(target)).isFalse();
    }

    @Test
    void shouldRejectFakeJpgBeforeRunningImageMagick() {
        FakeImageMagickRunner runner = new FakeImageMagickRunner();
        ImageProcessingService service = service(runner);
        Path target = tempDir.resolve("uploads/images/foto.webp");

        assertThatThrownBy(() -> service.process(file("foto.jpg", "image/jpeg", "%PDF".getBytes()), target))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("El archivo no es una imagen compatible");

        assertThat(runner.commands).isEmpty();
        assertThat(Files.exists(target)).isFalse();
    }

    @Test
    void shouldRejectImagesWithExcessivePixels() {
        FakeImageMagickRunner runner = new FakeImageMagickRunner();
        runner.identify("JPEG", 9000, 6000, 1);
        ImageProcessingService service = service(runner);
        Path target = tempDir.resolve("uploads/images/foto.webp");

        assertThatThrownBy(() -> service.process(file("foto.jpg", "image/jpeg", jpegBytes()), target))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("La imagen supera la resolucion maxima permitida");

        assertThat(Files.exists(target)).isFalse();
    }

    @Test
    void shouldRejectImagesWithExcessiveDimensions() {
        FakeImageMagickRunner runner = new FakeImageMagickRunner();
        runner.identify("JPEG", 9001, 1000, 1);
        ImageProcessingService service = service(runner);
        Path target = tempDir.resolve("uploads/images/foto.webp");

        assertThatThrownBy(() -> service.process(file("foto.jpg", "image/jpeg", jpegBytes()), target))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("La imagen supera la dimension maxima permitida");

        assertThat(Files.exists(target)).isFalse();
    }

    @Test
    void shouldRejectMultiFrameImages() {
        FakeImageMagickRunner runner = new FakeImageMagickRunner();
        runner.identify("WEBP", 1200, 800, 2);
        ImageProcessingService service = service(runner);
        Path target = tempDir.resolve("uploads/images/foto.webp");

        assertThatThrownBy(() -> service.process(file("foto.webp", "image/webp", webpBytes()), target))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("La imagen debe tener un unico frame");

        assertThat(Files.exists(target)).isFalse();
    }

    @Test
    void shouldHideImageMagickFailureAndCleanTemporaryFiles() throws IOException {
        FakeImageMagickRunner runner = new FakeImageMagickRunner();
        runner.identify("JPEG", 2000, 1000, 1);
        runner.convertFails();
        ImageProcessingService service = service(runner);
        Path target = tempDir.resolve("uploads/images/foto.webp");

        assertThatThrownBy(() -> service.process(file("foto.jpg", "image/jpeg", jpegBytes()), target))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No se pudo optimizar la imagen subida")
                .hasMessageNotContaining("delegate failed");

        assertThat(Files.exists(target)).isFalse();
        assertThat(Files.list(tempDir).filter(path -> path.getFileName().toString().startsWith("lago-image-")).toList()).isEmpty();
    }

    private ImageProcessingService service(FakeImageMagickRunner runner) {
        ImageProcessingService service = new ImageProcessingService(runner);
        ReflectionTestUtils.setField(service, "magickCommand", "magick");
        ReflectionTestUtils.setField(service, "timeoutSeconds", 30L);
        ReflectionTestUtils.setField(service, "configuredTempDir", tempDir.toString());
        return service;
    }

    private MockMultipartFile file(String filename, String contentType, byte[] bytes) {
        return new MockMultipartFile("file", filename, contentType, bytes);
    }

    private byte[] jpegBytes() {
        return new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01};
    }

    private byte[] pngBytes() {
        return new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
    }

    private byte[] webpBytes() {
        return new byte[]{'R', 'I', 'F', 'F', 0x00, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P', 0x00};
    }

    private byte[] heicBytes() {
        return new byte[]{0x00, 0x00, 0x00, 0x18, 'f', 't', 'y', 'p', 'h', 'e', 'i', 'c', 0x00, 0x00, 0x00, 0x00};
    }

    private static class FakeImageMagickRunner extends ImageMagickProcessRunner {
        private final Queue<CommandResult> identifyResults = new ArrayDeque<>();
        private CommandResult convertResult = new CommandResult(0, "", "", false);
        private final List<List<String>> commands = new java.util.ArrayList<>();
        private List<String> convertCommand = List.of();

        void identify(String format, int width, int height, int frames) {
            identifyResults.add(new CommandResult(0, format + "\n" + width + "\n" + height + "\n" + frames + "\n", "", false));
        }

        void convertSucceeds() {
            convertResult = new CommandResult(0, "", "", false);
        }

        void convertFails() {
            convertResult = new CommandResult(1, "", "delegate failed", false);
        }

        @Override
        public CommandResult run(List<String> command, Duration timeout) {
            commands.add(command);
            if (command.contains("identify")) {
                return identifyResults.remove();
            }
            convertCommand = command;
            if (convertResult.exitCode() == 0) {
                try {
                    Files.writeString(Path.of(command.get(command.size() - 1)), "optimized-webp");
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
            return convertResult;
        }
    }
}
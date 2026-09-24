package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.dto.MediaAssetResponse;
import com.luismunozse.reservalago.model.MediaAsset;
import com.luismunozse.reservalago.model.MediaAssetKind;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaAssetServiceTest {

    @TempDir
    Path uploadDir;

    @Mock
    private MediaAssetRepository mediaAssetRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectImageRepository projectImageRepository;

    @Mock
    private ProjectGalleryItemRepository projectGalleryItemRepository;

    @Mock
    private ProjectDocumentRepository projectDocumentRepository;

    @Mock
    private ProjectAdvanceRepository projectAdvanceRepository;

    @Mock
    private ProjectAdvanceGalleryItemRepository projectAdvanceGalleryItemRepository;

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private NewsImageRepository newsImageRepository;

    @Mock
    private NewsGalleryItemRepository newsGalleryItemRepository;

    @Mock
    private SystemConfigRepository systemConfigRepository;

    @Mock
    private ImageProcessingService imageProcessingService;

    private MediaAssetService service;

    @BeforeEach
    void setUp() {
        service = new MediaAssetService(
                mediaAssetRepository,
                projectRepository,
                projectImageRepository,
                projectGalleryItemRepository,
                projectDocumentRepository,
                projectAdvanceRepository,
                projectAdvanceGalleryItemRepository,
                newsRepository,
                newsImageRepository,
                newsGalleryItemRepository,
                systemConfigRepository,
                imageProcessingService
        );
        ReflectionTestUtils.setField(service, "uploadDir", uploadDir.toString());
        ReflectionTestUtils.setField(service, "maxImageSize", 20L * 1024L * 1024L);
        ReflectionTestUtils.setField(service, "maxDocumentSize", 10L * 1024L * 1024L);
        ReflectionTestUtils.setField(service, "maxVideoSize", 50L * 1024L * 1024L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @CsvSource({
            "foto.jpg,image/jpeg",
            "foto.jpeg,image/jpeg",
            "foto.png,image/png",
            "foto.webp,image/webp",
            "foto.heic,image/heic",
            "foto.heif,image/heif",
            "foto.heic,",
            "foto.heic,application/octet-stream",
            "foto.jpg,"
    })
    void shouldStoreProcessedImageAsWebp(String filename, String contentType) throws Exception {
        when(mediaAssetRepository.save(any(MediaAsset.class))).thenAnswer(invocation -> {
            MediaAsset asset = invocation.getArgument(0);
            asset.setId(UUID.randomUUID());
            return asset;
        });
        when(imageProcessingService.process(any(MockMultipartFile.class), any(Path.class)))
                .thenAnswer(invocation -> {
                    Path target = invocation.getArgument(1);
                    Files.createDirectories(target.getParent());
                    Files.writeString(target, "optimized-webp");
                    return new ImageProcessingService.ProcessedImage(1200, 800);
                });

        MediaAssetResponse response = service.uploadImage(new MockMultipartFile("file", filename, contentType, new byte[]{1, 2, 3}));

        ArgumentCaptor<MediaAsset> captor = ArgumentCaptor.forClass(MediaAsset.class);
        verify(mediaAssetRepository).save(captor.capture());
        MediaAsset saved = captor.getValue();
        assertThat(saved.getStorageKey()).startsWith("images/").endsWith(".webp");
        assertThat(saved.getOriginalFilename()).isEqualTo(filename);
        assertThat(saved.getContentType()).isEqualTo("image/webp");
        assertThat(saved.getSizeBytes()).isEqualTo("optimized-webp".length());
        assertThat(saved.getChecksum()).isNotBlank();
        assertThat(response.contentType()).isEqualTo("image/webp");
    }

    @Test
    void shouldRejectImageAboveTwentyMib() {
        byte[] bytes = new byte[(20 * 1024 * 1024) + 1];
        MockMultipartFile file = new MockMultipartFile("file", "foto.jpg", "", bytes);

        assertThatThrownBy(() -> service.uploadImage(file))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("El archivo supera el tamano maximo permitido");

        verify(imageProcessingService, never()).process(any(), any());
        verify(mediaAssetRepository, never()).save(any());
    }

    @Test
    void shouldRejectFakeHeicWhenImageProcessingRejectsRealContent() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "foto.heic", "application/octet-stream", "%PDF".getBytes());
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "El archivo no es una imagen compatible"))
                .when(imageProcessingService).process(any(MockMultipartFile.class), any(Path.class));

        assertThatThrownBy(() -> service.uploadImage(file))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("El archivo no es una imagen compatible");

        verify(imageProcessingService).process(any(MockMultipartFile.class), any(Path.class));
        verify(mediaAssetRepository, never()).save(any());
    }

    @Test
    void shouldRejectInvalidImageExtensionBeforeProcessing() {
        MockMultipartFile file = new MockMultipartFile("file", "foto.raw", "image/jpeg", new byte[]{1});

        assertThatThrownBy(() -> service.uploadImage(file))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Extension de imagen no permitida");

        verify(imageProcessingService, never()).process(any(), any());
    }

    @Test
    void shouldAllowAnonymousAccessToHomeHeroImage() throws Exception {
        UUID id = UUID.randomUUID();
        mockStoredAsset(id);
        mockPublicHomeReference("home_hero_image_url", id);

        MediaAssetService.ServedMedia media = service.loadForRequest(id);

        assertThat(media.contentType()).isEqualTo("image/webp");
        assertThat(media.sizeBytes()).isEqualTo(12L);
    }

    @Test
    void shouldAllowAnonymousAccessToHomeVisitsImage() throws Exception {
        UUID id = UUID.randomUUID();
        mockStoredAsset(id);
        mockPublicHomeReference("home_visits_image_url", id);

        MediaAssetService.ServedMedia media = service.loadForRequest(id);

        assertThat(media.contentType()).isEqualTo("image/webp");
        assertThat(media.sizeBytes()).isEqualTo(12L);
    }

    @Test
    void shouldRejectAnonymousAccessToUnreferencedAsset() throws Exception {
        UUID id = UUID.randomUUID();
        mockStoredAsset(id);

        assertThatThrownBy(() -> service.loadForRequest(id))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @ParameterizedTest
    @CsvSource({"ROLE_ADMIN", "ROLE_MANAGER"})
    void shouldAllowAdminAndManagerAccessToAsset(String role) throws Exception {
        UUID id = UUID.randomUUID();
        mockStoredAsset(id);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "password", role));

        MediaAssetService.ServedMedia media = service.loadForRequest(id);

        assertThat(media.contentType()).isEqualTo("image/webp");
        assertThat(media.sizeBytes()).isEqualTo(12L);
    }

    @Test
    void shouldStopServingOldHomeAssetWhenReferenceIsReplaced() throws Exception {
        UUID oldId = UUID.randomUUID();
        UUID newId = UUID.randomUUID();
        mockStoredAsset(oldId);
        mockStoredAsset(newId);
        mockPublicHomeReference("home_hero_image_url", newId);

        assertThatThrownBy(() -> service.loadForRequest(oldId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(service.loadForRequest(newId).contentType()).isEqualTo("image/webp");
    }


    @Test
    void shouldAllowAnonymousAccessToPublishedProjectGalleryAsset() throws Exception {
        UUID id = UUID.randomUUID();
        mockStoredAsset(id);
        when(projectGalleryItemRepository.existsPublishedAssociation(id, com.luismunozse.reservalago.model.ProjectStatus.PUBLISHED)).thenReturn(true);

        MediaAssetService.ServedMedia media = service.loadForRequest(id);

        assertThat(media.contentType()).isEqualTo("image/webp");
    }

    @Test
    void shouldAllowAnonymousAccessToPublishedAdvanceGalleryAsset() throws Exception {
        UUID id = UUID.randomUUID();
        mockStoredAsset(id);
        when(projectAdvanceGalleryItemRepository.existsPublishedAssociation(id, com.luismunozse.reservalago.model.ProjectStatus.PUBLISHED)).thenReturn(true);

        MediaAssetService.ServedMedia media = service.loadForRequest(id);

        assertThat(media.contentType()).isEqualTo("image/webp");
    }

    @Test
    void shouldAllowAnonymousAccessToPublishedNewsGalleryAsset() throws Exception {
        UUID id = UUID.randomUUID();
        mockStoredAsset(id);
        when(newsGalleryItemRepository.existsPublishedAssociation(id, com.luismunozse.reservalago.model.NewsStatus.PUBLISHED)).thenReturn(true);

        MediaAssetService.ServedMedia media = service.loadForRequest(id);

        assertThat(media.contentType()).isEqualTo("image/webp");
    }
    private void mockPublicHomeReference(String key, UUID id) {
        String mediaUrl = "/api/media/" + id;
        when(systemConfigRepository.existsByConfigKeyAndConfigValue(anyString(), anyString()))
                .thenAnswer(invocation ->
                        key.equals(invocation.getArgument(0)) && mediaUrl.equals(invocation.getArgument(1))
                );
    }

    private void mockStoredAsset(UUID id) throws Exception {
        Path file = uploadDir.resolve("images").resolve(id + ".webp");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "webp-content");

        MediaAsset asset = new MediaAsset();
        asset.setId(id);
        asset.setKind(MediaAssetKind.IMAGE);
        asset.setStorageProvider("local");
        asset.setStorageKey("images/" + id + ".webp");
        asset.setOriginalFilename("home.webp");
        asset.setContentType("image/webp");
        asset.setSizeBytes(Files.size(file));

        when(mediaAssetRepository.findById(id)).thenReturn(java.util.Optional.of(asset));
    }
}

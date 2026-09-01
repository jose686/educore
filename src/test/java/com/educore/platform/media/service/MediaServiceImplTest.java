package com.educore.platform.media.service;

import com.educore.platform.media.model.MediaFile;
import com.educore.platform.media.model.MediaType;
import com.educore.platform.media.model.CategoriaMedia;
import com.educore.platform.media.repository.MediaFileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para MediaServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class MediaServiceImplTest {

    @Mock
    private MediaFileRepository mediaFileRepository;

    @Mock
    private VideoConversionService videoConversionService;

    private MediaServiceImpl mediaService;
    private Path tempUploadsDir;

    @BeforeEach
    void setUp() throws IOException {
        tempUploadsDir = Files.createTempDirectory("media-uploads-test");
        mediaService = new MediaServiceImpl(tempUploadsDir.toString(), mediaFileRepository, videoConversionService);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempUploadsDir != null && Files.exists(tempUploadsDir)) {
            try (var walk = Files.walk(tempUploadsDir)) {
                walk.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                // Ignore
                            }
                        });
            }
        }
    }

    @Test
    void uploadFile_ShouldThrowException_WhenFilenameContainsPathTraversal() {
        MockMultipartFile file = new MockMultipartFile("file", "../traversal.png", "image/png", "data".getBytes());
        assertThrows(IllegalArgumentException.class, () -> mediaService.uploadFile(file, "alias", CategoriaMedia.GENERAL));
    }

    @Test
    void uploadFile_ShouldSaveImageSuccessfully() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "test-image.png", "image/png", "image_data".getBytes());
        when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String url = mediaService.uploadFile(file, "my-image-alias", CategoriaMedia.GENERAL);

        assertNotNull(url);
        assertTrue(url.startsWith("/media/"));
        verify(mediaFileRepository, times(1)).save(any(MediaFile.class));
    }

    @Test
    void uploadFile_ShouldSaveHtmlInteractivoSuccessfully() {
        MockMultipartFile file = new MockMultipartFile("file", "index.html", "text/html", "<html></html>".getBytes());
        when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String url = mediaService.uploadFile(file, "game-alias", CategoriaMedia.MINIJUEGO);

        assertNotNull(url);
        ArgumentCaptor<MediaFile> captor = ArgumentCaptor.forClass(MediaFile.class);
        verify(mediaFileRepository, times(1)).save(captor.capture());
        assertEquals(MediaType.HTML_INTERACTIVO, captor.getValue().getTipo());
        assertEquals(CategoriaMedia.MINIJUEGO, captor.getValue().getCategoriaMedia());
    }

    @Test
    void uploadFile_ShouldSavePdfSuccessfully() {
        MockMultipartFile file = new MockMultipartFile("file", "document.pdf", "application/pdf", "pdf_content".getBytes());
        when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mediaService.uploadFile(file, "pdf-alias", CategoriaMedia.RECURSO_CURSO);

        ArgumentCaptor<MediaFile> captor = ArgumentCaptor.forClass(MediaFile.class);
        verify(mediaFileRepository, times(1)).save(captor.capture());
        assertEquals(MediaType.DOCUMENTO_PDF, captor.getValue().getTipo());
        assertEquals(CategoriaMedia.GENERAL, captor.getValue().getCategoriaMedia());
    }

    @Test
    void uploadFile_ShouldSaveVideoAndConvertHls() {
        MockMultipartFile file = new MockMultipartFile("file", "video.mp4", "video/mp4", "video_data".getBytes());
        when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(videoConversionService.convertMp4ToHls(any(Path.class), any(Path.class))).thenReturn(true);

        mediaService.uploadFile(file, "video-alias", CategoriaMedia.GENERAL);

        verify(videoConversionService, times(1)).convertMp4ToHls(any(Path.class), any(Path.class));
        ArgumentCaptor<MediaFile> captor = ArgumentCaptor.forClass(MediaFile.class);
        verify(mediaFileRepository, times(1)).save(captor.capture());
        assertEquals(MediaType.VIDEO, captor.getValue().getTipo());
        assertTrue(captor.getValue().getFilename().endsWith(".m3u8"));
    }

    @Test
    void uploadFile_ShouldSaveVideoAndFallbackToMp4_WhenHlsConversionFails() {
        MockMultipartFile file = new MockMultipartFile("file", "video.mp4", "video/mp4", "video_data".getBytes());
        when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(videoConversionService.convertMp4ToHls(any(Path.class), any(Path.class))).thenReturn(false);

        mediaService.uploadFile(file, "video-fallback-alias", CategoriaMedia.GENERAL);

        verify(videoConversionService, times(1)).convertMp4ToHls(any(Path.class), any(Path.class));
        ArgumentCaptor<MediaFile> captor = ArgumentCaptor.forClass(MediaFile.class);
        verify(mediaFileRepository, times(1)).save(captor.capture());
        assertEquals(MediaType.VIDEO, captor.getValue().getTipo());
        assertTrue(captor.getValue().getFilename().endsWith(".mp4"));
    }

    @Test
    void uploadFile_ShouldThrowException_WhenAliasAlreadyExists() {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", "data".getBytes());
        when(mediaFileRepository.findByAlias("existing-alias")).thenReturn(Optional.of(new MediaFile()));

        assertThrows(IllegalArgumentException.class, () -> mediaService.uploadFile(file, "existing-alias", CategoriaMedia.GENERAL));
    }

    @Test
    void loadFileAsResource_ShouldReturnResource_WhenExists() throws IOException {
        String filename = "test-file.txt";
        Files.writeString(tempUploadsDir.resolve(filename), "Hello World");

        Resource resource = mediaService.loadFileAsResource(filename);

        assertNotNull(resource);
        assertTrue(resource.exists());
        assertTrue(resource.isReadable());
    }

    @Test
    void loadFileAsResource_ShouldThrowException_WhenFileDoesNotExist() {
        assertThrows(RuntimeException.class, () -> mediaService.loadFileAsResource("non-existent.txt"));
    }

    @Test
    void loadFileAsResource_ShouldThrowSecurityException_WhenPathTraversalAttempted() {
        assertThrows(SecurityException.class, () -> mediaService.loadFileAsResource("../outside.txt"));
    }

    @Test
    void listAllFiles_ShouldReturnRepositoryList() {
        when(mediaFileRepository.findAllByOrderByUploadedAtDesc()).thenReturn(Collections.emptyList());
        List<MediaFile> result = mediaService.listAllFiles();
        assertNotNull(result);
        verify(mediaFileRepository, times(1)).findAllByOrderByUploadedAtDesc();
    }

    @Test
    void listFilesByTipo_ShouldReturnRepositoryList() {
        when(mediaFileRepository.findByTipoOrderByUploadedAtDesc(MediaType.IMAGEN)).thenReturn(Collections.emptyList());
        List<MediaFile> result = mediaService.listFilesByTipo(MediaType.IMAGEN);
        assertNotNull(result);
        verify(mediaFileRepository, times(1)).findByTipoOrderByUploadedAtDesc(MediaType.IMAGEN);
    }

    @Test
    void listFilesByCategoria_ShouldReturnRepositoryList() {
        when(mediaFileRepository.findByCategoriaMediaOrderByUploadedAtDesc(CategoriaMedia.MINIJUEGO)).thenReturn(Collections.emptyList());
        List<MediaFile> result = mediaService.listFilesByCategoria(CategoriaMedia.MINIJUEGO);
        assertNotNull(result);
        verify(mediaFileRepository, times(1)).findByCategoriaMediaOrderByUploadedAtDesc(CategoriaMedia.MINIJUEGO);
    }

    @Test
    void searchFiles_ShouldReturnRepositorySearch() {
        when(mediaFileRepository.searchFiles(any(), any(), any())).thenReturn(Collections.emptyList());
        List<MediaFile> result = mediaService.searchFiles(MediaType.IMAGEN, CategoriaMedia.GENERAL, "search");
        assertNotNull(result);
        verify(mediaFileRepository, times(1)).searchFiles(MediaType.IMAGEN, CategoriaMedia.GENERAL, "search");
    }

    @Test
    void deleteFile_ShouldDeletePhysicallyAndFromRepository() throws IOException {
        String filename = "delete-me.txt";
        Files.writeString(tempUploadsDir.resolve(filename), "data");

        MediaFile mediaFile = MediaFile.builder()
                .filename(filename)
                .tipo(MediaType.IMAGEN)
                .build();

        when(mediaFileRepository.findByFilename(filename)).thenReturn(Optional.of(mediaFile));

        mediaService.deleteFile(filename);

        assertFalse(Files.exists(tempUploadsDir.resolve(filename)));
        verify(mediaFileRepository, times(1)).delete(mediaFile);
    }

    @Test
    void deleteFile_ShouldDeleteTempMp4_WhenVideoDeleted() throws IOException {
        String filename = "video.m3u8";
        String tempMp4 = "temp_video.mp4";
        Files.writeString(tempUploadsDir.resolve(filename), "data");
        Files.writeString(tempUploadsDir.resolve(tempMp4), "data");

        MediaFile mediaFile = MediaFile.builder()
                .filename(filename)
                .tipo(MediaType.VIDEO)
                .build();

        when(mediaFileRepository.findByFilename(filename)).thenReturn(Optional.of(mediaFile));

        mediaService.deleteFile(filename);

        assertFalse(Files.exists(tempUploadsDir.resolve(filename)));
        assertFalse(Files.exists(tempUploadsDir.resolve(tempMp4)));
        verify(mediaFileRepository, times(1)).delete(mediaFile);
    }

    @Test
    void deleteFile_ShouldThrowException_WhenFileNotFoundInDatabase() {
        when(mediaFileRepository.findByFilename("not-found.txt")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> mediaService.deleteFile("not-found.txt"));
    }

    @Test
    void syncDatabaseWithStorage_ShouldSyncMissingFiles() throws IOException {
        String newFile = "new-image.png";
        Files.writeString(tempUploadsDir.resolve(newFile), "data");
        Files.writeString(tempUploadsDir.resolve("temp_ignored.mp4"), "data"); // should be ignored

        when(mediaFileRepository.findByFilename(newFile)).thenReturn(Optional.empty());
        when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mediaService.syncDatabaseWithStorage();

        verify(mediaFileRepository, times(1)).save(any(MediaFile.class));
    }

    @Test
    void updateAlias_ShouldSaveNewAlias_WhenValid() {
        String filename = "test.png";
        MediaFile mediaFile = MediaFile.builder().id(java.util.UUID.randomUUID()).filename(filename).build();

        when(mediaFileRepository.findByFilename(filename)).thenReturn(Optional.of(mediaFile));
        when(mediaFileRepository.findByAlias("new-alias")).thenReturn(Optional.empty());

        mediaService.updateAlias(filename, "  new-alias  ");

        assertEquals("new-alias", mediaFile.getAlias());
        verify(mediaFileRepository, times(1)).save(mediaFile);
    }

    @Test
    void updateAlias_ShouldThrowException_WhenAliasTakenByOtherFile() {
        String filename = "test.png";
        java.util.UUID myId = java.util.UUID.randomUUID();
        MediaFile mediaFile = MediaFile.builder().id(myId).filename(filename).build();
        MediaFile otherFile = MediaFile.builder().id(java.util.UUID.randomUUID()).alias("taken").build();

        when(mediaFileRepository.findByFilename(filename)).thenReturn(Optional.of(mediaFile));
        when(mediaFileRepository.findByAlias("taken")).thenReturn(Optional.of(otherFile));

        assertThrows(IllegalArgumentException.class, () -> mediaService.updateAlias(filename, "taken"));
    }

    @Test
    void resolveUrlByAliasOrPath_ShouldReturnMappedUrl_WhenAliasRegistered() {
        MediaFile mediaFile = MediaFile.builder().url("/media/mapped.png").build();
        when(mediaFileRepository.findByAlias("alias-name")).thenReturn(Optional.of(mediaFile));

        String result = mediaService.resolveUrlByAliasOrPath("alias-name");

        assertEquals("/media/mapped.png", result);
    }

    @Test
    void resolveUrlByAliasOrPath_ShouldReturnOriginalInput_WhenNoAliasRegistered() {
        when(mediaFileRepository.findByAlias("path/to/img.png")).thenReturn(Optional.empty());

        String result = mediaService.resolveUrlByAliasOrPath("path/to/img.png");

        assertEquals("path/to/img.png", result);
    }
}

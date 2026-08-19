package com.educore.platform.media.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Pruebas unitarias para VideoConversionService.
 */
class VideoConversionServiceTest {

    private final VideoConversionService service = new VideoConversionService();

    @Test
    void convertMp4ToHls_ShouldDeleteInputFileEvenIfProcessFails(@TempDir Path tempDir) throws IOException {
        Path input = tempDir.resolve("temp_video.mp4");
        Path output = tempDir.resolve("output.m3u8");
        Files.writeString(input, "video content");

        service.convertMp4ToHls(input, output);

        // Verifica que la limpieza en el bloque finally borra el archivo temporal de entrada
        assertFalse(Files.exists(input));
    }
}

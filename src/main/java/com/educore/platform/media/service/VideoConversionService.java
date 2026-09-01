package com.educore.platform.media.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Servicio encargado de la conversión asíncrona de archivos de vídeo MP4
 * al estándar HLS (HTTP Live Streaming) con FFmpeg.
 */
@Service
public class VideoConversionService {

    public boolean convertMp4ToHls(Path inputPath, Path outputPath) {
        try {
            // Comando FFmpeg para transcodificar MP4 a HLS
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-y", // Sobrescribir archivos existentes sin preguntar
                    "-i", inputPath.toString(),
                    "-profile:v", "baseline",
                    "-level", "3.0",
                    "-start_number", "0",
                    "-hls_time", "10",
                    "-hls_list_size", "0",
                    "-f", "hls",
                    outputPath.toString()
            );

            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);

            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0 && Files.exists(outputPath)) {
                System.out.println("[VideoConversionService] Conversión HLS completada con éxito: " + outputPath.getFileName());
                return true;
            } else {
                System.err.println("[VideoConversionService] Error en la conversión FFmpeg. Código salida: " + exitCode);
                return false;
            }
        } catch (IOException | InterruptedException ex) {
            System.err.println("[VideoConversionService] Excepción durante transcodificación HLS: " + ex.getMessage());
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }
}

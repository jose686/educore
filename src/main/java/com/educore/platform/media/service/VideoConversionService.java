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

    @Async
    public void convertMp4ToHls(Path inputPath, Path outputPath) {
        try {
            // Comando FFmpeg para transcodificar MP4 a HLS
            // ffmpeg -i input.mp4 -profile:v baseline -level 3.0 -s 1280x720 -start_number 0 -hls_time 10 -hls_list_size 0 -f hls output.m3u8
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-y", // Sobrescribir archivos existentes sin preguntar
                    "-i", inputPath.toString(),
                    "-profile:v", "baseline",
                    "-level", "3.0",
                    "-s", "1280x720",
                    "-start_number", "0",
                    "-hls_time", "10",
                    "-hls_list_size", "0",
                    "-f", "hls",
                    outputPath.toString()
            );

            // Redirigir salidas del comando al output estándar de Spring para facilitar auditoría y depuración
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);

            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("[VideoConversionService] Conversión HLS completada con éxito: " + outputPath.getFileName());
            } else {
                System.err.println("[VideoConversionService] Error en la conversión FFmpeg. Código salida: " + exitCode);
            }
        } catch (IOException | InterruptedException ex) {
            System.err.println("[VideoConversionService] Excepción durante transcodificación HLS: " + ex.getMessage());
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        } finally {
            // Eliminar el archivo de vídeo temporal MP4 de entrada
            try {
                Files.deleteIfExists(inputPath);
            } catch (IOException e) {
                System.err.println("[VideoConversionService] No se pudo limpiar archivo temporal: " + e.getMessage());
            }
        }
    }
}

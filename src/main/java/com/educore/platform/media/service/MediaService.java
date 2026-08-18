package com.educore.platform.media.service;

import com.educore.platform.media.model.MediaFile;
import com.educore.platform.media.model.MediaType;
import com.educore.platform.media.model.CategoriaMedia;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/**
 * Servicio para gestionar la subida, lectura y alias de recursos estáticos en la Biblioteca de Medios.
 */
public interface MediaService {

    default String uploadFile(MultipartFile file) {
        return uploadFile(file, null, CategoriaMedia.GENERAL);
    }

    default String uploadFile(MultipartFile file, String alias) {
        return uploadFile(file, alias, CategoriaMedia.GENERAL);
    }

    /**
     * Guarda un archivo en el disco y lo registra en base de datos con un alias y categoría opcional.
     */
    String uploadFile(MultipartFile file, String alias, CategoriaMedia categoria);

    /**
     * Carga un archivo desde el almacenamiento local como un recurso de Spring.
     */
    Resource loadFileAsResource(String filename);

    /**
     * Recupera todos los archivos registrados.
     */
    List<MediaFile> listAllFiles();

    /**
     * Recupera archivos filtrados por tipo.
     */
    List<MediaFile> listFilesByTipo(MediaType tipo);

    /**
     * Recupera archivos filtrados por categoría.
     */
    List<MediaFile> listFilesByCategoria(CategoriaMedia categoria);

    /**
     * Filtra y busca archivos por su tipo, categoría y/o alias.
     */
    List<MediaFile> searchFiles(MediaType tipo, CategoriaMedia categoria, String search);

    /**
     * Elimina un archivo físicamente y de la base de datos.
     */
    void deleteFile(String filename);

    /**
     * Sincroniza los archivos presentes en el almacenamiento físico con la base de datos.
     */
    void syncDatabaseWithStorage();

    /**
     * Actualiza el alias único de un archivo registrado.
     */
    void updateAlias(String filename, String alias);

    /**
     * Resuelve el alias a la URL real de acceso del archivo, o devuelve la ruta cruda si no es un alias.
     */
    String resolveUrlByAliasOrPath(String aliasOrPath);
}

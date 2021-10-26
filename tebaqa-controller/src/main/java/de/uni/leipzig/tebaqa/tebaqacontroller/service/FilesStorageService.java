package de.uni.leipzig.tebaqa.tebaqacontroller.service;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface FilesStorageService {

    void init(String basePath);

    Path saveOntologyFile(long kbId, MultipartFile file);

    Path saveDataFile(long kbId, MultipartFile file);

    Path getRootPath();

//    public void deleteAll();

//    public Resource load(String filename);

//    public Stream<Path> loadAll();
}
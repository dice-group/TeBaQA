package de.uni.leipzig.tebaqa.tebaqacontroller.service;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FilesStorageServiceImpl implements FilesStorageService {

    private static final Logger LOGGER = Logger.getLogger(FilesStorageServiceImpl.class.getName());

    private String rootFolderLocation;
    private Path rootFolderPath;

    @Override
    public void init(String rootPath) {
        try {
            rootFolderPath = Paths.get(rootPath).toAbsolutePath();
            Files.createDirectories(rootFolderPath);
            rootFolderLocation = rootPath;
            LOGGER.info("File storage init: " + rootPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize folder for upload!");
        }
    }

    @Override
    public Path saveOntologyFile(long kbId, MultipartFile file) {
        try {
            Path ontologyFolder = Paths.get(rootFolderLocation + "/" + kbId + "/ontology/");
            Files.createDirectories(ontologyFolder);

            Path filePath = ontologyFolder.resolve(file.getOriginalFilename());
            Files.copy(file.getInputStream(), filePath);

            LOGGER.info("Saved ontology file: " + filePath);
            return filePath;
        } catch (Exception e) {
            throw new RuntimeException("Could not store ontology file. Error: " + e.getMessage());
        }
    }

    @Override
    public Path saveDataFile(long kbId, MultipartFile file) {
        try {
            Path dataFolder = Paths.get(rootFolderLocation + "/" + kbId + "/data/");
            Files.createDirectories(dataFolder);

            Path filePath = dataFolder.resolve(file.getOriginalFilename());
            Files.copy(file.getInputStream(), filePath);

            LOGGER.info("Saved data file: " + filePath);
            return filePath;
        } catch (Exception e) {
            throw new RuntimeException("Could not store data file. Error: " + e.getMessage());
        }
    }

    @Override
    public Path getRootPath() {
        return rootFolderPath;
    }

//    @Override
//    public void deleteAll() {
//        FileSystemUtils.deleteRecursively(Paths.g.toFile());
//    }

//    @Override
//    public Resource load(String filename) {
//        try {
//            Path file = root.resolve(filename);
//            Resource resource = new UrlResource(file.toUri());
//
//            if (resource.exists() || resource.isReadable()) {
//                return resource;
//            } else {
//                throw new RuntimeException("Could not read the file!");
//            }
//        } catch (MalformedURLException e) {
//            throw new RuntimeException("Error: " + e.getMessage());
//        }
//    }


//    @Override
//    public Stream<Path> loadAll() {
//        try {
//            return Files.walk(this.root, 1).filter(path -> !path.equals(this.root)).map(this.root::relativize);
//        } catch (IOException e) {
//            throw new RuntimeException("Could not load the files!");
//        }
//    }
}

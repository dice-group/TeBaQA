package de.uni.leipzig.tebaqa.tebaqacontroller;

import de.uni.leipzig.tebaqa.tebaqacontroller.service.FilesStorageService;
import org.apache.log4j.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.Resource;

@SpringBootApplication
public class TebaqaControllerApplication implements CommandLineRunner {

    public static Logger LOGGER = Logger.getRootLogger();

    @Resource
    private FilesStorageService storageService;

    public static void main(String[] args) {
        LOGGER.info("Starting TeBaQA controller ...");
        SpringApplication.run(TebaqaControllerApplication.class, args);

//        AblationProvider.init();

    }

    @Override
    public void run(String... arg) throws Exception {
        String fileUploadStorageFolder;
        if (arg.length > 0)
            fileUploadStorageFolder = arg[0];
        else
            fileUploadStorageFolder = "../../knowledge_base_storage/";

        storageService.init(fileUploadStorageFolder);
    }

}

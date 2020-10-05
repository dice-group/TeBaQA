package de.uni.leipzig.tebaqa.template.service;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TemplateClassificationService {

    private static final Logger LOGGER = Logger.getLogger(TemplateClassificationService.class);

    private static final String QUERY_PATTERNS_FILE = "src/main/resources/patterns.txt";
    private static List<String> ALL_PATTERNS = null;

    public static List<String> getAllTemplates() {

        if (ALL_PATTERNS == null) {
            try (Stream<String> stream = Files.lines(Paths.get(QUERY_PATTERNS_FILE))) {
                ALL_PATTERNS = stream.collect(Collectors.toList());
            } catch (IOException e) {
                LOGGER.error("Failed to read query pattens from file: " + e.getMessage());
                ALL_PATTERNS = new ArrayList<>();
            }
        }

        return ALL_PATTERNS;
    }

}

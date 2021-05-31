package de.uni.leipzig.tebaqa.template;

import de.uni.leipzig.tebaqa.template.nlp.ClassifierProvider;
import de.uni.leipzig.tebaqa.template.service.WekaClassifier;
import de.uni.leipzig.tebaqa.template.util.PropertyUtils;
import org.aksw.qa.commons.load.Dataset;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import weka.classifiers.Classifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

@SpringBootTest
class ClassificationApplicationTests {
    private List<String> loadGraphs(Dataset trainDataset) {
        List<String> graphsFromFile = new ArrayList<>();

        String graphsFilePath = PropertyUtils.getGraphsFileAbsolutePath(trainDataset.name());
        File graphsFile = new File(graphsFilePath);

        if (!graphsFile.exists()) {

            return null;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(graphsFile))) {
            String graphPattern = reader.readLine();
            while (graphPattern != null) {
                graphsFromFile.add(graphPattern);
                graphPattern = reader.readLine();
            }

        } catch (IOException e) {

            graphsFromFile = null;
        }

        return graphsFromFile;
    }

    @Test
    void contextLoads() {
    }

}

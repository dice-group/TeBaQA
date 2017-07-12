package de.uni.leipzig.tebaqa.controller;

import de.uni.leipzig.tebaqa.Analyzer.Analyzer;
import de.uni.leipzig.tebaqa.model.CustomQuestion;
import org.apache.log4j.Logger;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ArffGenerator {
    private static Logger log = Logger.getLogger(ArffGenerator.class);

    ArffGenerator(List<CustomQuestion> questions) {
        log.debug("Calculate the features per question and cluster");

        Set<String> graphs = new HashSet<>();
        for (CustomQuestion customQuestion : questions) {
            graphs.add(customQuestion.getGraph());
        }

        List<String> filter = new ArrayList<>();
        filter.add("Filter");
        filter.add("noFilter");

        List<String> optional = new ArrayList<>();
        optional.add("Optional");
        optional.add("noOptional");

        List<String> limit = new ArrayList<>();
        limit.add("Limit");
        limit.add("noLimit");

        List<String> orderBy = new ArrayList<>();
        orderBy.add("OrderBy");
        orderBy.add("noOrderBy");

        List<String> union = new ArrayList<>();
        union.add("Union");
        union.add("noUnion");

        List<Attribute> attributes = new ArrayList<>();
        Attribute filterAttribute = new Attribute("filter", filter);
        Attribute optionalAttribute = new Attribute("optional", optional);
        Attribute limitAttribute = new Attribute("limit", limit);
        Attribute orderByAttribute = new Attribute("orderBy", orderBy);
        Attribute unionAttribute = new Attribute("union", union);

        attributes.add(filterAttribute);
        attributes.add(optionalAttribute);
        attributes.add(limitAttribute);
        attributes.add(orderByAttribute);
        attributes.add(unionAttribute);

        // Add all occurring graphs(=class attribute) as possible attribute values
        List<String> graphsList = new ArrayList<>();
        graphsList.addAll(graphs);
        Attribute classAttribute = new Attribute("class", graphsList);
        attributes.add(classAttribute);
        Analyzer analyzer = new Analyzer(attributes);
        ArrayList<Attribute> fvfinal = analyzer.fvWekaAttributes;

        Instances trainingSet = new Instances("training_classifier: -C 4", fvfinal, questions.size());
        Instances testSet = new Instances("training_classifier: -C 4", fvfinal, questions.size());
        log.debug("Start collection of training data for each class");
        Map<Integer, CustomQuestion> idGraph = new HashMap<>();
        for (int i = 0; i < questions.size(); i++) {
            CustomQuestion question = questions.get(i);
            idGraph.put(i, question);

            Instance instance = analyzer.analyze(question.getQuestionText());
            List<String> modifiers = question.getModifiers();

            instance.setValue(filterAttribute, "noFilter");
            instance.setValue(optionalAttribute, "noOptional");
            instance.setValue(limitAttribute, "noLimit");
            instance.setValue(orderByAttribute, "noOrderBy");
            instance.setValue(unionAttribute, "noUnion");

            for (String modifier : modifiers) {
                if (modifier.contains("FILTER")) {
                    instance.setValue(filterAttribute, "Filter");
                } else if (modifier.contains("OPTIONAL")) {
                    instance.setValue(optionalAttribute, "Optional");
                } else if (modifier.contains("LIMIT")) {
                    instance.setValue(limitAttribute, "Limit");
                } else if (modifier.contains("ORDER BY")) {
                    instance.setValue(orderByAttribute, "OrderBy");
                } else if (modifier.contains("UNION")) {
                    instance.setValue(unionAttribute, "Union");
                }
            }

            //Create instance and set the class attribute missing for testing
            Instance testInstance = instance;
            testInstance.setMissing(classAttribute);
            testSet.add(testInstance);

            //Create instance with the class attribute for training
            Instance trainInstance = instance;
            trainInstance.setValue(classAttribute, question.getGraph());
            trainingSet.add(trainInstance);
        }

        writeSetToArffFile(trainingSet, "./src/main/resources/Train.arff");
        writeSetToArffFile(testSet, "./src/main/resources/Test.arff");

        WekaWrapper wekaWrapper = new WekaWrapper();
        wekaWrapper.classifyJ48();
        Instances classifiedResult = readClassifiedResult();
        int correctlyClassified = 0;
        int classified = 0;
        int numInstances = classifiedResult.numInstances();
        for (int i = 0; i < numInstances; i++) {
            classified++;
            Instance instance = classifiedResult.instance(i);
            String classifiedGraph = instance.stringValue(instance.numAttributes() - 1);
            String correctGraph = idGraph.get(i).getGraph();
            if (!classifiedGraph.equals(correctGraph)) {
                log.info("False classified: " + idGraph.get(i).getQuestionText());
            } else {
                correctlyClassified++;
            }
        }
        log.info("Correctly classified: " + correctlyClassified + " / " + classified);
    }

    private void writeSetToArffFile(Instances set, String path) {
        try (FileWriter file = new FileWriter(path)) {
            file.write(set.toString());
        } catch (IOException e) {
            log.error("Unable to write set to Arff file: " + path, e);
        }
    }

    private Instances readClassifiedResult() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("./src/main/resources/Test.arff"));
            ArffLoader.ArffReader arff = new ArffLoader.ArffReader(reader);
            Instances data = arff.getData();
            data.setClassIndex(data.numAttributes() - 1);
            return data;
        } catch (IOException e) {
            log.error("Unable to read Arff file Test.arff", e);
        }
        return null;
    }
}

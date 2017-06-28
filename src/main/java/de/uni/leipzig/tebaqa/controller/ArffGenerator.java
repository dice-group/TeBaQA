package de.uni.leipzig.tebaqa.controller;

import de.uni.leipzig.tebaqa.Analyzer.Analyzer;
import de.uni.leipzig.tebaqa.model.CustomQuestion;
import org.apache.log4j.Logger;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ArffGenerator {
    private static Logger log = Logger.getLogger(ArffGenerator.class);
    private Instances trainingSet;

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

        List<String> graphsList = new ArrayList<>();
        graphsList.addAll(graphs);

        List<Attribute> attributes = new ArrayList<>();
        Attribute filterAttribute = new Attribute("filter", filter);
        Attribute optionalAttribute = new Attribute("optional", optional);
        Attribute limitAttribute = new Attribute("limit", limit);
        Attribute orderByAttribute = new Attribute("orderBy", orderBy);
        Attribute unionAttribute = new Attribute("union", union);
        Attribute classAttribute = new Attribute("class", graphsList);
        attributes.add(filterAttribute);
        attributes.add(optionalAttribute);
        attributes.add(limitAttribute);
        attributes.add(orderByAttribute);
        attributes.add(unionAttribute);
        attributes.add(classAttribute);
        Analyzer analyzer = new Analyzer(attributes);
        ArrayList<Attribute> fvfinal = analyzer.fvWekaAttributes;

        trainingSet = new Instances("training_classifier: -C 4", fvfinal, questions.size());
        log.debug("Start collection of training data for each class");

        for (CustomQuestion question : questions) {
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
                } else {
                    log.warn("unknown modifier: " + modifier);
                }
            }

            instance.setValue(classAttribute, question.getGraph());
            trainingSet.add(instance);
        }

    }

    void writeArffFile() {
        try (FileWriter file = new FileWriter("./src/main/resources/Train.arff")) {
            file.write(trainingSet.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

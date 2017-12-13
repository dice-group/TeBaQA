package de.uni.leipzig.tebaqa.controller;

import com.google.common.collect.Lists;
import de.uni.leipzig.tebaqa.analyzer.Analyzer;
import de.uni.leipzig.tebaqa.model.CustomQuestion;
import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.bayes.NaiveBayesMultinomialText;
import weka.classifiers.bayes.NaiveBayesUpdateable;
import weka.classifiers.functions.Logistic;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.functions.SMO;
import weka.classifiers.functions.SimpleLogistic;
import weka.classifiers.lazy.IBk;
import weka.classifiers.lazy.KStar;
import weka.classifiers.lazy.LWL;
import weka.classifiers.meta.*;
import weka.classifiers.rules.DecisionTable;
import weka.classifiers.rules.JRip;
import weka.classifiers.rules.OneR;
import weka.classifiers.rules.PART;
import weka.classifiers.rules.ZeroR;
import weka.classifiers.trees.DecisionStump;
import weka.classifiers.trees.HoeffdingTree;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.LMT;
import weka.classifiers.trees.REPTree;
import weka.classifiers.trees.RandomTree;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ArffLoader;
import weka.core.converters.CSVSaver;

import java.io.BufferedReader;
import java.io.File;
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
        Set<String> graphs = new HashSet<>();
        for (CustomQuestion customQuestion : questions) {
            graphs.add(customQuestion.getGraph());
        }
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

        RandomCommittee randomCommittee = new RandomCommittee();
        try {
            trainingSet.setClassIndex(trainingSet.numAttributes() - 1);
            randomCommittee.buildClassifier(trainingSet);
            SerializationHelper.write(new ClassPathResource("randomCommittee.model").getFile().getPath(), randomCommittee);
        } catch (Exception e) {
            log.error("Unable to generate weka model!", e);
        }

        //TODO enable to evaluate result
        //evaluateResult(idGraph);
    }

    private void evaluateResult(Map<Integer, CustomQuestion> idGraph) {
        WekaWrapper wekaWrapper = new WekaWrapper();
        double f_measure;
        ArrayList<Attribute> attributes = new ArrayList<>();
        List<String> classifiers = new ArrayList<>();
        classifiers.addAll(Lists.asList("BayesNet", new String[]{"NaiveBayes", "NaiveBayesUpdateable",
                "NaiveBayesMultinominalText", "Logistic", "MultilayerPerception", "SimpleLogistic", "SMO", "IBk",
                "KStar", "LWL", "AdaBoostM1", "Bagging", "ClassificationViaRegression", "CVParameterSelection",
                "FilteredClassifier", "IterativeClassifierOptimizer", "LogitBoost", "MultiClassClassifier",
                "MultiClassClassifierUpdateable", "MultiScheme", "RandomCommittee", "RandomizableFilteredClassifier",
                "RandomSubSpace", "Stacking", "Vote", "DecisionTable", "JRip", "OneR", "PART",
                "ZeroR", "DecisionStump", "HoeffdingTree", "J48", "LMT", "RandomTree",
                "REPTree"}));
        Attribute name_attribute = new Attribute("name", classifiers);
        attributes.add(name_attribute);
        Attribute f_measure_attribute = new Attribute("f-measure");
        attributes.add(f_measure_attribute);
        Attribute correctly_classified_attribute = new Attribute("correctly classified");
        attributes.add(correctly_classified_attribute);
        Instances set = new Instances("classifier_evaluation", attributes, 36);

        Instance instance = new DenseInstance(3);

        f_measure = wekaWrapper.classify(new BayesNet(), new String[0]);
        instance.setValue(name_attribute, "BayesNet");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        instance = new DenseInstance(3);
        f_measure = wekaWrapper.classify(new NaiveBayes(), new String[0]);
        instance.setValue(name_attribute, "NaiveBayes");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        instance = new DenseInstance(3);
        f_measure = wekaWrapper.classify(new NaiveBayesUpdateable(), new String[0]);
        instance.setValue(name_attribute, "NaiveBayesUpdateable");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        instance = new DenseInstance(3);
        f_measure = wekaWrapper.classify(new NaiveBayesMultinomialText(), new String[0]);
        instance.setValue(name_attribute, "NaiveBayesMultinominalText");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        instance = new DenseInstance(3);
        f_measure = wekaWrapper.classify(new Logistic(), new String[0]);
        instance.setValue(name_attribute, "Logistic");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        instance = new DenseInstance(3);
        f_measure = wekaWrapper.classify(new MultilayerPerceptron(), new String[0]);
        instance.setValue(name_attribute, "MultilayerPerception");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        instance = new DenseInstance(3);
        f_measure = wekaWrapper.classify(new SimpleLogistic(), new String[0]);
        instance.setValue(name_attribute, "SimpleLogistic");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        instance = new DenseInstance(3);
        f_measure = wekaWrapper.classify(new SMO(), new String[0]);
        instance.setValue(name_attribute, "SMO");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        instance = new DenseInstance(3);
        f_measure = wekaWrapper.classify(new IBk(), new String[0]);
        instance.setValue(name_attribute, "IBk");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        instance = new DenseInstance(3);
        f_measure = wekaWrapper.classify(new KStar(), new String[0]);
        instance.setValue(name_attribute, "KStar");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        instance = new DenseInstance(3);
        f_measure = wekaWrapper.classify(new LWL(), new String[0]);
        instance.setValue(name_attribute, "LWL");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        instance = new DenseInstance(3);
        f_measure = wekaWrapper.classify(new AdaBoostM1(), new String[0]);
        instance.setValue(name_attribute, "AdaBoostM1");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        instance = new DenseInstance(3);
        f_measure = wekaWrapper.classify(new Bagging(), new String[0]);
        instance.setValue(name_attribute, "Bagging");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        instance = new DenseInstance(3);
        f_measure = wekaWrapper.classify(new ClassificationViaRegression(), new String[0]);
        instance.setValue(name_attribute, "ClassificationViaRegression");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        instance = new DenseInstance(3);
        f_measure = wekaWrapper.classify(new CVParameterSelection(), new String[0]);
        instance.setValue(name_attribute, "CVParameterSelection");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        instance = new DenseInstance(3);
        f_measure = wekaWrapper.classify(new FilteredClassifier(), new String[0]);
        instance.setValue(name_attribute, "FilteredClassifier");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        instance = new DenseInstance(3);
        f_measure = wekaWrapper.classify(new IterativeClassifierOptimizer(), new String[0]);
        instance.setValue(name_attribute, "IterativeClassifierOptimizer");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        instance = new DenseInstance(3);
        f_measure = wekaWrapper.classify(new LogitBoost(), new String[0]);
        instance.setValue(name_attribute, "LogitBoost");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        instance = new DenseInstance(3);
        f_measure = wekaWrapper.classify(new MultiClassClassifier(), new String[0]);
        instance.setValue(name_attribute, "MultiClassClassifier");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        instance = new DenseInstance(3);
        f_measure = wekaWrapper.classify(new MultiClassClassifierUpdateable(), new String[0]);
        instance.setValue(name_attribute, "MultiClassClassifierUpdateable");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        instance = new DenseInstance(3);
        f_measure = wekaWrapper.classify(new MultiScheme(), new String[0]);
        instance.setValue(name_attribute, "MultiScheme");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        instance = new DenseInstance(3);
        f_measure = wekaWrapper.classify(new RandomCommittee(), new String[0]);
        instance.setValue(name_attribute, "RandomCommittee");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        instance = new DenseInstance(3);
        f_measure = wekaWrapper.classify(new RandomizableFilteredClassifier(), new String[0]);
        instance.setValue(name_attribute, "RandomizableFilteredClassifier");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        instance = new DenseInstance(3);
        f_measure = wekaWrapper.classify(new RandomSubSpace(), new String[0]);
        instance.setValue(name_attribute, "RandomSubSpace");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        instance = new DenseInstance(3);
        f_measure = wekaWrapper.classify(new Stacking(), new String[0]);
        instance.setValue(name_attribute, "Stacking");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        instance = new DenseInstance(3);
        f_measure = wekaWrapper.classify(new Vote(), new String[0]);
        instance.setValue(name_attribute, "Vote");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        instance = new DenseInstance(3);
        f_measure = wekaWrapper.classify(new DecisionTable(), new String[0]);
        instance.setValue(name_attribute, "DecisionTable");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        instance = new DenseInstance(3);
        f_measure = wekaWrapper.classify(new JRip(), new String[0]);
        instance.setValue(name_attribute, "JRip");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        instance = new DenseInstance(3);
        f_measure = wekaWrapper.classify(new OneR(), new String[0]);
        instance.setValue(name_attribute, "OneR");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        instance = new DenseInstance(3);
        f_measure = wekaWrapper.classify(new PART(), new String[0]);
        instance.setValue(name_attribute, "PART");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        instance = new DenseInstance(3);
        f_measure = wekaWrapper.classify(new ZeroR(), new String[0]);
        instance.setValue(name_attribute, "ZeroR");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        instance = new DenseInstance(3);
        f_measure = wekaWrapper.classify(new DecisionStump(), new String[0]);
        instance.setValue(name_attribute, "DecisionStump");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        instance = new DenseInstance(3);
        f_measure = wekaWrapper.classify(new HoeffdingTree(), new String[0]);
        instance.setValue(name_attribute, "HoeffdingTree");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        String[] j48Options = new String[1];
        j48Options[0] = "-U";            // unpruned tree
        instance = new DenseInstance(3);
        f_measure = wekaWrapper.classify(new J48(), j48Options);
        instance.setValue(name_attribute, "J48");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        instance = new DenseInstance(3);
        f_measure = wekaWrapper.classify(new LMT(), new String[0]);
        instance.setValue(name_attribute, "LMT");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        instance = new DenseInstance(3);
        f_measure = wekaWrapper.classify(new RandomTree(), new String[0]);
        instance.setValue(name_attribute, "RandomTree");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        instance = new DenseInstance(3);
        f_measure = wekaWrapper.classify(new REPTree(), new String[0]);
        instance.setValue(name_attribute, "REPTree");
        instance.setValue(f_measure_attribute, f_measure);
        instance.setValue(correctly_classified_attribute, (double) getCorrectClassifiedCount(idGraph));
        set.add(instance);

        writeSetToArffFile(set, "./src/main/resources/Evaluation.arff");
        CSVSaver csvSaver = new CSVSaver();
        try {
            csvSaver.setInstances(set);
            csvSaver.setFile(new File("./src/main/resources/Evaluation.csv"));
            csvSaver.writeBatch();
        } catch (IOException e) {
            log.error("Unable to write classifier evaluation to CSV", e);
        }
    }

    private float getCorrectClassifiedCount(Map<Integer, CustomQuestion> idGraph) {
        Instances classifiedResult = readClassifiedResult();
        int correctlyClassified = 0;
        int classified = 0;
        int numInstances = classifiedResult.numInstances();
        for (int i = 0; i < numInstances; i++) {
            classified++;
            Instance instance = classifiedResult.instance(i);
            String classifiedGraph = instance.stringValue(instance.numAttributes() - 1);
            String correctGraph = idGraph.get(i).getGraph();
            if (classifiedGraph.equals(correctGraph)) {
                correctlyClassified++;
            } else {
                //log.info("False classified: " + idGraph.get(i).getQuestionText() + "\t" + classifiedGraph + "\t" + idGraph.get(i).getGraph());
            }
        }
        return (float) correctlyClassified / classified;
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

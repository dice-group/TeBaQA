package de.uni.leipzig.tebaqa.template.nlp;

import com.google.common.collect.Lists;
import de.uni.leipzig.tebaqa.template.model.CustomQuestion;
import de.uni.leipzig.tebaqa.template.nlp.analyzer.Analyzer;
import de.uni.leipzig.tebaqa.template.service.WekaWrapper;
import de.uni.leipzig.tebaqa.template.util.PropertyUtils;
import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;
import weka.classifiers.AbstractClassifier;
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
import weka.classifiers.rules.*;
import weka.classifiers.trees.*;
import weka.core.*;
import weka.core.converters.ArffLoader;
import weka.core.converters.CSVSaver;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ArffGenerator {
    private static Logger log = Logger.getLogger(ArffGenerator.class);

    public ArffGenerator(String datasetName, List<String> graphs, List<CustomQuestion> trainQuestions) {
        log.info("Calculate the features per question and cluster");
        List<Attribute> attributes = new ArrayList<>();

        // Add all occurring graphs(=class attribute) as possible attribute values
        //Set<String> graphs = trainQuestions.parallelStream().map(CustomQuestion::getGraph).collect(Collectors.toSet());
        Attribute classAttribute = new Attribute("class", Lists.newArrayList(graphs));
        attributes.add(classAttribute);
        Analyzer analyzer = new Analyzer(attributes);
        ArrayList<Attribute> fvfinal = analyzer.fvWekaAttributes;

        Instances trainingSet = new Instances("training_classifier: -C 4", fvfinal, trainQuestions.size());
//        Instances testSet = new Instances("test_classifier: -C 4", fvfinal, trainQuestions.size());
        log.debug("Start collection of training data for each class");
        //Create instance and set the class attribute missing for testing
        //Create instance with the class attribute for training
        trainQuestions.forEach(question -> {
            try {
                //Create instance with the class attribute for training
                Instance trainInstance = analyzer.analyze(question.getQuestionText());
                trainInstance.setValue(classAttribute, question.getGraph());
                trainingSet.add(trainInstance);
            } catch (Exception e) {
                log.warn("Training error: " + question.getQuestionText());
                log.warn("Training error: " + e.getMessage());
            }
        });

        String arffTrainFileAbsolutePath = PropertyUtils.getArffTrainFileAbsolutePath(datasetName);
        try {
            File file = new File(arffTrainFileAbsolutePath);
            file.createNewFile();
            writeSetToArffFile(trainingSet, file.getPath());
        } catch (IOException e) {
            log.error(String.format("Unable to write %s to file!", arffTrainFileAbsolutePath), e);
        }

        //AbstractClassifier classifier = new MultilayerPerceptron();
        AbstractClassifier classifier = new RandomizableFilteredClassifier();

        try {
            trainingSet.setClassIndex(trainingSet.numAttributes() - 1);
            classifier.buildClassifier(trainingSet);
            SerializationHelper.write(PropertyUtils.getClassifierFileAbsolutePath(datasetName), classifier);
        } catch (Exception e) {
            log.error("Unable to generate weka model!", e);
        }
    }

    public ArffGenerator(List<String> graphs, List<CustomQuestion> trainQuestions, List<CustomQuestion> testQuestions, Boolean evaluateWekaAlgorithms) {
        log.info("Calculate the features per question and cluster");
        List<Attribute> attributes = new ArrayList<>();

        // Add all occurring graphs(=class attribute) as possible attribute values
        //Set<String> graphs = trainQuestions.parallelStream().map(CustomQuestion::getGraph).collect(Collectors.toSet());
        Attribute classAttribute = new Attribute("class", Lists.newArrayList(graphs));
        attributes.add(classAttribute);
        Analyzer analyzer = new Analyzer(attributes);
        ArrayList<Attribute> fvfinal = analyzer.fvWekaAttributes;

        Instances trainingSet = new Instances("training_classifier: -C 4", fvfinal, trainQuestions.size());
//        Instances testSet = new Instances("test_classifier: -C 4", fvfinal, trainQuestions.size());
        log.debug("Start collection of training data for each class");
        //Create instance and set the class attribute missing for testing
        //Create instance with the class attribute for training
        trainQuestions.forEach(question -> {
            Instance instance = analyzer.analyze(question.getQuestionText());

            //Create instance and set the class attribute missing for testing
//            Instance testInstance = instance;
//            testInstance.setMissing(classAttribute);
//            testSet.add(testInstance);

            //Create instance with the class attribute for training
            Instance trainInstance = instance;
            trainInstance.setValue(classAttribute, question.getGraph());
            trainingSet.add(trainInstance);
        });
        /*testQuestions.forEach(question -> {
            Instance instance = analyzer.analyze(question.getQuestionText());

            //Create instance and set the class attribute missing for testing
            Instance testInstance = instance;
            testInstance.setValue(classAttribute, question.getGraph());
            testSet.add(testInstance);

        });*/
        try {
            File file = new File(new ClassPathResource("Train.arff").getFile().getAbsolutePath());
            file.createNewFile();
            writeSetToArffFile(trainingSet, file.getPath());
        } catch (IOException e) {
            log.error("Unable to write Train.arff to file!", e);
        }
//        try {
//            File file = new File(new ClassPathResource("Test.arff").getFile().getAbsolutePath());
//            file.createNewFile();
//            writeSetToArffFile(testSet, file.getPath());
//        } catch (IOException e) {
//            log.error("Unable to write Test.arff to file!", e);
//        }

        //AbstractClassifier classifier = new MultilayerPerceptron();
        AbstractClassifier classifier = new RandomizableFilteredClassifier();

        try {
            trainingSet.setClassIndex(trainingSet.numAttributes() - 1);
            classifier.buildClassifier(trainingSet);
            SerializationHelper.write(new ClassPathResource("question_classification.model").getFile().getAbsolutePath(), classifier);
        } catch (Exception e) {
            log.error("Unable to generate weka model!", e);
        }

        if (evaluateWekaAlgorithms) {
            evaluateResult(testQuestions);
        }
    }

    private void evaluateResult(List<CustomQuestion> questions) {
        ArrayList<Attribute> attributes = new ArrayList<>();
        List<String> classifiers = new ArrayList<>();
        classifiers.addAll(Lists.asList("BayesNet", new String[]{"NaiveBayes", "NaiveBayesUpdateable",
                "NaiveBayesMultinominalText", "Logistic", "MultilayerPerceptron", "SimpleLogistic", "SMO", "IBk",
                "KStar", "LWL", "AdaBoostM1", "Bagging", "ClassificationViaRegression", "CVParameterSelection",
                "FilteredClassifier", "IterativeClassifierOptimizer", "LogitBoost", "MultiClassClassifier",
                "MultiClassClassifierUpdateable", "MultiScheme", "RandomCommittee", "RandomizableFilteredClassifier",
                "RandomSubSpace", "Stacking", "Vote", "DecisionTable", "JRip", "OneR", "PART", "ZeroR", "DecisionStump",
                "HoeffdingTree", "J48", "LMT", "REPTree"}));
        Attribute name_attribute = new Attribute("name", classifiers);
        attributes.add(name_attribute);
        Attribute f_measure_attribute = new Attribute("f-measure");
        attributes.add(f_measure_attribute);
        Attribute correctly_classified_attribute = new Attribute("correctly classified");
        attributes.add(correctly_classified_attribute);
        Instances set = new Instances("classifier_evaluation", attributes, 36);

        set.add(createInstance(new BayesNet(), "BayesNet", new String[0], name_attribute, f_measure_attribute, correctly_classified_attribute, questions));
        set.add(createInstance(new NaiveBayes(), "NaiveBayes", new String[0], name_attribute, f_measure_attribute, correctly_classified_attribute, questions));
        set.add(createInstance(new NaiveBayesUpdateable(), "NaiveBayesUpdateable", new String[0], name_attribute, f_measure_attribute, correctly_classified_attribute, questions));
        set.add(createInstance(new NaiveBayesMultinomialText(), "NaiveBayesMultinominalText", new String[0], name_attribute, f_measure_attribute, correctly_classified_attribute, questions));
        set.add(createInstance(new Logistic(), "Logistic", new String[0], name_attribute, f_measure_attribute, correctly_classified_attribute, questions));
        set.add(createInstance(new MultilayerPerceptron(), "MultilayerPerceptron", new String[0], name_attribute, f_measure_attribute, correctly_classified_attribute, questions));
        set.add(createInstance(new SimpleLogistic(), "SimpleLogistic", new String[0], name_attribute, f_measure_attribute, correctly_classified_attribute, questions));
        set.add(createInstance(new SMO(), "SMO", new String[0], name_attribute, f_measure_attribute, correctly_classified_attribute, questions));
        set.add(createInstance(new IBk(), "IBk", new String[0], name_attribute, f_measure_attribute, correctly_classified_attribute, questions));
        set.add(createInstance(new KStar(), "KStar", new String[0], name_attribute, f_measure_attribute, correctly_classified_attribute, questions));
        set.add(createInstance(new LWL(), "LWL", new String[0], name_attribute, f_measure_attribute, correctly_classified_attribute, questions));
        set.add(createInstance(new AdaBoostM1(), "AdaBoostM1", new String[0], name_attribute, f_measure_attribute, correctly_classified_attribute, questions));
        set.add(createInstance(new Bagging(), "Bagging", new String[0], name_attribute, f_measure_attribute, correctly_classified_attribute, questions));
        set.add(createInstance(new ClassificationViaRegression(), "ClassificationViaRegression", new String[0], name_attribute, f_measure_attribute, correctly_classified_attribute, questions));
        set.add(createInstance(new CVParameterSelection(), "CVParameterSelection", new String[0], name_attribute, f_measure_attribute, correctly_classified_attribute, questions));
        set.add(createInstance(new FilteredClassifier(), "FilteredClassifier", new String[0], name_attribute, f_measure_attribute, correctly_classified_attribute, questions));
        set.add(createInstance(new IterativeClassifierOptimizer(), "IterativeClassifierOptimizer", new String[0], name_attribute, f_measure_attribute, correctly_classified_attribute, questions));
        set.add(createInstance(new LogitBoost(), "LogitBoost", new String[0], name_attribute, f_measure_attribute, correctly_classified_attribute, questions));
        set.add(createInstance(new MultiClassClassifier(), "MultiClassClassifier", new String[0], name_attribute, f_measure_attribute, correctly_classified_attribute, questions));
        set.add(createInstance(new MultiClassClassifierUpdateable(), "MultiClassClassifierUpdateable", new String[0], name_attribute, f_measure_attribute, correctly_classified_attribute, questions));
        set.add(createInstance(new MultiScheme(), "MultiScheme", new String[0], name_attribute, f_measure_attribute, correctly_classified_attribute, questions));
        set.add(createInstance(new RandomCommittee(), "RandomCommittee", new String[0], name_attribute, f_measure_attribute, correctly_classified_attribute, questions));
        set.add(createInstance(new RandomizableFilteredClassifier(), "RandomizableFilteredClassifier", new String[0], name_attribute, f_measure_attribute, correctly_classified_attribute, questions));
        set.add(createInstance(new RandomSubSpace(), "RandomSubSpace", new String[0], name_attribute, f_measure_attribute, correctly_classified_attribute, questions));
        set.add(createInstance(new Stacking(), "Stacking", new String[0], name_attribute, f_measure_attribute, correctly_classified_attribute, questions));
        set.add(createInstance(new J48(), "J48", new String[0], name_attribute, f_measure_attribute, correctly_classified_attribute, questions));
        set.add(createInstance(new Vote(), "Vote", new String[0], name_attribute, f_measure_attribute, correctly_classified_attribute, questions));
        set.add(createInstance(new DecisionTable(), "DecisionTable", new String[0], name_attribute, f_measure_attribute, correctly_classified_attribute, questions));
        set.add(createInstance(new JRip(), "JRip", new String[0], name_attribute, f_measure_attribute, correctly_classified_attribute, questions));
        set.add(createInstance(new OneR(), "OneR", new String[0], name_attribute, f_measure_attribute, correctly_classified_attribute, questions));
        set.add(createInstance(new PART(), "PART", new String[0], name_attribute, f_measure_attribute, correctly_classified_attribute, questions));
        set.add(createInstance(new ZeroR(), "ZeroR", new String[0], name_attribute, f_measure_attribute, correctly_classified_attribute, questions));
        set.add(createInstance(new DecisionStump(), "DecisionStump", new String[0], name_attribute, f_measure_attribute, correctly_classified_attribute, questions));
        set.add(createInstance(new HoeffdingTree(), "HoeffdingTree", new String[0], name_attribute, f_measure_attribute, correctly_classified_attribute, questions));
        set.add(createInstance(new LMT(), "LMT", new String[0], name_attribute, f_measure_attribute, correctly_classified_attribute, questions));
        set.add(createInstance(new REPTree(), "REPTree", new String[0], name_attribute, f_measure_attribute, correctly_classified_attribute, questions));

        writeSetToArffFile(set, "./src/main/resources/Evaluation.arff");
        CSVSaver csvSaver = new CSVSaver();
        try {
            csvSaver.setInstances(set);
            File csvFile = new File("./src/main/resources/Evaluation.csv");
            csvSaver.setFile(csvFile);
            csvSaver.writeBatch();
            log.info("File " + csvFile.getAbsolutePath() + " successfully written.");
        } catch (IOException e) {
            log.error("Unable to write classifier evaluation to CSV", e);
        }
    }

    private Instance createInstance(AbstractClassifier classifier, String name, String[] options,
                                    Attribute name_attribute, Attribute f_measure_attribute,
                                    Attribute correctly_classified_attribute, List<CustomQuestion> questions) {
        Instance instance = new DenseInstance(3);
        WekaWrapper wekaWrapper = new WekaWrapper();
        try {
            double f_measure = wekaWrapper.classify(classifier, options);
            instance.setValue(name_attribute, name);
            instance.setValue(f_measure_attribute, f_measure);
            instance.setValue(correctly_classified_attribute, getCorrectClassifiedCount(questions));
        } catch (IllegalArgumentException e) {
            log.error("Exception while classifying with WEKA algorithm: " + name, e);
        }

        return instance;
    }

    private float getCorrectClassifiedCount(List<CustomQuestion> questions) {
        Instances classifiedResult = readClassifiedResult();
        int correctlyClassified = 0;
        int classified = 0;
        int numInstances = classifiedResult.numInstances();
        for (int i = 0; i < numInstances; i++) {
            classified++;
            Instance instance = classifiedResult.instance(i);
            String classifiedGraph = instance.stringValue(instance.numAttributes() - 1);
            String correctGraph = questions.get(i).getGraph();
            if (classifiedGraph.equals(correctGraph)) {
                correctlyClassified++;
            }
        }
        return (float) correctlyClassified / classified;
    }

    private void writeSetToArffFile(Instances set, String path) {
        try (FileWriter file = new FileWriter(path, false)) {
            file.write(set.toString());
            log.info("File " + path + " successfully written.");
        } catch (IOException e) {
            log.error("Unable to write set to Arff file: " + path, e);
        }
    }

    private Instances readClassifiedResult() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new ClassPathResource("Test.arff").getFile()));
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

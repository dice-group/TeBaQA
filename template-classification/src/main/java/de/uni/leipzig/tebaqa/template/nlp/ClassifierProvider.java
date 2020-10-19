package de.uni.leipzig.tebaqa.template.nlp;

import de.uni.leipzig.tebaqa.template.nlp.analyzer.Analyzer;
import de.uni.leipzig.tebaqa.template.util.PropertyUtils;
import org.apache.log4j.Logger;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.SerializationHelper;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ClassifierProvider {
    private static Classifier cls;
    private static Analyzer analyzer;
    private static Instances dataset;
    private static Attribute classAttribute;
    private static Logger log = Logger.getLogger(ClassifierProvider.class);


    //do not instantiate
    private ClassifierProvider() {
    }


    /**
     * Provides a singleton instance of the Classifier.
     *
     * @return A shared instance of the Classifier.
     */
    public static Classifier init(String datasetName, List<String> graphs) {
        log.info("Init classifier...");
        ArrayList<Attribute> attributes = new ArrayList<>();
        classAttribute = new Attribute("class", new ArrayList<>(graphs));
        attributes.add(classAttribute);
        analyzer = new Analyzer(attributes);

        ArrayList<Attribute> filteredAttributes = analyzer.fvWekaAttributes.stream().filter(Objects::nonNull).collect(Collectors.toCollection(ArrayList::new));
        dataset = new Instances("testdata", filteredAttributes, 1);
        dataset.setClassIndex(dataset.numAttributes() - 1);

        try {
            cls = (Classifier) SerializationHelper.read(new FileInputStream(new File(PropertyUtils.getClassifierFileAbsolutePath(datasetName))));
        } catch (Exception e) {
            log.error("Unable to load weka model file: question_classification.model");
        }

        return cls;
    }

    public static Analyzer getAnalyzer() {
        return analyzer;
    }

    public static Classifier getSingletonClassifierInstance() {
        return cls;
    }

    public static Instances getDataset() {
        return dataset;
    }

    public static Attribute getClassAttribute() {
        return classAttribute;
    }
}

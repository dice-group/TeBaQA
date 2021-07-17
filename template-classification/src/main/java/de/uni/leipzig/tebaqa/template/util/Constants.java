package de.uni.leipzig.tebaqa.template.util;

public class Constants {

    public static final String TEMPLATE_CLASSIFICATION_PROP_FILE = "template-classification.properties";
    public static final String DEFAULT_SERIALIZED_CLASSIFIER_FILENAME = "question_classification_%s.model";
    public static final String DEFAULT_SERIALIZED_GRAPHS_FILENAME = "graphs_%s.txt";
    public static final String DEFAULT_SERIALIZED_MAPPINGS_FILENAME = "mappings_%s.json";
    public static final String DEFAULT_SERIALIZED_ARFF_TRAIN_FILENAME = "Train_%s.arff";
    public static final String DEFAULT_SERIALIZED_ARFF_TEST_FILENAME = "Test_%s.arff";

    // Keys in properties file
    public static final String DEFAULT_TRAINING_DATASET = "classifier.training.dataset";
    public static final String FILE_BASED_TRAINING_FLAG = "classifier.training.filebased";
    public static final String TRAINING_FILEPATH = "classifier.training.filepath";
    public static final String SERIALIZED_CLASSIFIER_FILE = "classifier.model.file";
    public static final String SERIALIZED_GRAPHS_FILE = "classifier.graphs.file";
    public static final String SERIALIZED_MAPPINGS_FILE = "classifier.mappings.file";
    public static final String SERIALIZED_ARFF_TRAIN_FILE = "classifier.arff.train.file";
    public static final String SERIALIZED_ARFF_TEST_FILE = "classifier.arff.test.file";
    public static final String FORCE_CLASSIFICATION_RESPONSE = "classifier.forceResponse";
}

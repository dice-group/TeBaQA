package de.uni.leipzig.tebaqa.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import static weka.core.Debug.Random;

class WekaWrapper {

    private static Logger log = LoggerFactory.getLogger(WekaWrapper.class);
    private Instances unlabeled;
    private Instances data;

    WekaWrapper() {
        try {
            DataSource source = new DataSource("./src/main/resources/Train.arff");
            data = source.getDataSet();
            // setting class attribute if the data format does not provide this information
            // For example, the XRFF format saves the class attribute information as well
            if (data.classIndex() == -1)
                data.setClassIndex(data.numAttributes() - 1);

            // load unlabeled data
            unlabeled = new Instances(
                    new BufferedReader(
                            new FileReader("./src/main/resources/Test.arff")));

            // set class attribute
            unlabeled.setClassIndex(unlabeled.numAttributes() - 1);
        } catch (Exception e) {
            log.error("Exception while loading Arff file", e);
        }
    }

    void classify(AbstractClassifier classifier, String[] options) {
        if (options.length > 0) {
            try {
                classifier.setOptions(options);     // set the options
            } catch (Exception e) {
                log.error("Option not supported:" + Arrays.toString(options), e);
            }
        }

        int seed = 1;          // the seed for randomizing the data
        int folds = 10;         // the number of folds to generate, >=2
        Random rand = new Random(seed);   // create seeded number generator
        Instances randData = new Instances(data);   // create copy of original data
        randData.randomize(rand);         // randomize data with number generator

        // perform cross-validation
        Evaluation eval = null;
        try {
            eval = new Evaluation(randData);
        } catch (Exception e) {
            log.error("Unable to evaluate data", e);
        }
        for (int n = 0; n < folds; n++) {
            Instances train = randData.trainCV(folds, n);
            Instances test = randData.testCV(folds, n);

            try {
                classifier.buildClassifier(train);
            } catch (Exception e) {
                log.error("Can't build classifier", e);
            }

            try {
                eval.evaluateModel(classifier, test);
            } catch (Exception e) {
                log.error("Unable to evaluate classifier model", e);
            }
        }

        //uncomment this line for some statistics from the cross validation run
        //log.info(eval.toSummaryString("=== " + folds + "-fold Cross-validation ===", false));

        // create copy
        Instances labeled = new Instances(unlabeled);

        // label instances
        for (int i = 0; i < unlabeled.numInstances(); i++) {
            double clsLabel;
            try {
                clsLabel = classifier.classifyInstance(unlabeled.instance(i));
            } catch (Exception e) {
                log.error("Unable to classify instances", e);
                break;
            }
            labeled.instance(i).setClassValue(clsLabel);
        }

        try {
            // save labeled data
            BufferedWriter writer = new BufferedWriter(
                    new FileWriter("./src/main/resources/Test.arff"));
            writer.write(labeled.toString());
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            log.error("IOException while writing Test.arff", e);
        }
    }
}

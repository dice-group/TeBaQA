package de.uni.leipzig.tebaqa.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weka.classifiers.trees.J48;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

class WekaWrapper {

    private static Logger log = LoggerFactory.getLogger(WekaWrapper.class);
    private J48 tree;
    private Instances unlabeled;

    WekaWrapper() {
        try {
            DataSource source = new DataSource("./src/main/resources/Train.arff");
            Instances data = source.getDataSet();
            // setting class attribute if the data format does not provide this information
            // For example, the XRFF format saves the class attribute information as well
            if (data.classIndex() == -1)
                data.setClassIndex(data.numAttributes() - 1);
            String[] options = new String[1];
            options[0] = "-U";            // unpruned tree

            tree = new J48();
            tree.setOptions(options);     // set the options
            tree.buildClassifier(data);   // build classifier

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

    void classifyJ48() {
        // create copy
        Instances labeled = new Instances(unlabeled);

        // label instances
        for (int i = 0; i < unlabeled.numInstances(); i++) {
            double clsLabel = 0;
            try {
                clsLabel = tree.classifyInstance(unlabeled.instance(i));
            } catch (Exception e) {
                log.error("Unable to classify instances", e);
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

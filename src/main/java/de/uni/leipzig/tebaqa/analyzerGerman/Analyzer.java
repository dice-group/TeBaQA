package de.uni.leipzig.tebaqa.analyzerGerman;

import org.aksw.mlqa.analyzer.IAnalyzer;
import org.aksw.mlqa.analyzer.numberoftoken.NumberOfToken;
import org.apache.log4j.Logger;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Analyzer {
    private static Logger log = Logger.getLogger(Analyzer.class);

    private ArrayList<IAnalyzer> analyzers;
    public ArrayList<Attribute> fvWekaAttributes = new ArrayList<>();

    public Analyzer(List<Attribute> attributes) {
        analyzers = new ArrayList<>();
        analyzers.add(new QuestionWord());
        analyzers.add(new NumberOfToken());
        //analyzers.add(new QueryResourceTypeAnalyzer());
        analyzers.add(new Noun());
        analyzers.add(new Number());
        analyzers.add(new Verb());
        analyzers.add(new Adjective());
        analyzers.add(new Comperative());
        //analyzers.add(new NamedEntities());
        try {
            analyzers.add(new TripleCandidates());
        } catch (IOException | ClassNotFoundException e) {
            log.error("Unable to load Classifier class", e);
        }

        // Declare the feature vector, register their attributes
        for (IAnalyzer analyzer : analyzers) {
            fvWekaAttributes.add(analyzer.getAttribute());
        }
        fvWekaAttributes.addAll(attributes);
    }

    /**
     * @param q Question string
     * @return feature vector leaving out a slot for the class variable
     */
    public Instance analyze(String q) {
        // +1 to later add class attribute
        Instance tmpInstance = new DenseInstance(fvWekaAttributes.size());
        // the feature adds itself to the instance
        for (IAnalyzer analyzer : analyzers) {
            Attribute attribute = analyzer.getAttribute();
            final Object classification = analyzer.analyze(q);
            if (attribute.isNumeric()) {
                double value = -1;
                try {
                    value = (double) classification;
                } catch (ClassCastException e) {
                    log.error(String.format("Invalid value: '%s' for numeric attribute: '%s' from question: '%s'", classification, attribute.toString(), q), e);
                }
                tmpInstance.setValue(attribute, value);
            } else if (attribute.isNominal() || attribute.isString()) {
                String analyze = (String) classification;
                try {
                    tmpInstance.setValue(attribute, analyze);
                } catch (IllegalArgumentException e) {
                    //log.warn(String.format("Unable to set: '%s' in '%s'", analyze, attribute), e);
                }
            }
        }
        return tmpInstance;
    }
}

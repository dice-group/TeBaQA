package de.uni.leipzig.tebaqa.template.nlp.analyzer;

import de.uni.leipzig.tebaqa.template.util.Utilities;
import org.aksw.mlqa.analyzer.IAnalyzer;
import org.aksw.qa.commons.nlp.nerd.Spotlight;
import weka.core.Attribute;

public class NamedEntities implements IAnalyzer {
    private Attribute attribute = null;

    public NamedEntities() {
        attribute = new Attribute("NumberOfDBPediaNamedEntities");
    }

    public Object analyze(String q) {
        Spotlight spotlight = Utilities.createCustomSpotlightInstance("http://model.dbpedia-spotlight.org/en/annotate");

        return (double) spotlight.getEntities(q).size();
    }

    @Override
    public Attribute getAttribute() {
        return attribute;
    }
}

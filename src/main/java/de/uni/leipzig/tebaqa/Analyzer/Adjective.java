package de.uni.leipzig.tebaqa.Analyzer;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import org.aksw.mlqa.analyzer.IAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weka.core.Attribute;

import java.util.List;
import java.util.Properties;

public class Adjective implements IAnalyzer {
    static Logger log = LoggerFactory.getLogger(Adjective.class);
    private Attribute attribute = null;
    private StanfordCoreNLP pipeline;

    public Adjective() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner");
        props.setProperty("ner.useSUTime", "false");
        pipeline = new StanfordCoreNLP(props);
        attribute = new Attribute("NumberOfAdjectives");
    }

    public Object analyze(String q) {
        Annotation annotation = new Annotation(q);
        pipeline.annotate(annotation);
        double adjectiveCnt = 0;
        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
            for (int i = 0; i < tokens.size(); i++) {
                CoreLabel token = tokens.get(i);
                String posTag = token.tag();
                if (posTag.toLowerCase().startsWith("jj")) {
                    if (i == 0 || !tokens.get(i - 1).tag().equalsIgnoreCase(posTag)) {
                        adjectiveCnt++;
                    }
                }
            }

        }

        return adjectiveCnt;
    }

    @Override
    public Attribute getAttribute() {
        return attribute;
    }
}

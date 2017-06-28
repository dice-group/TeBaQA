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

public class Verb implements IAnalyzer {
    private Attribute attribute = null;
    private StanfordCoreNLP pipeline;

    public Verb() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner");
        props.setProperty("ner.useSUTime", "false");
        pipeline = new StanfordCoreNLP(props);
        attribute = new Attribute("NumberOfVerbs");
    }

    public Object analyze(String q) {
        Annotation annotation = new Annotation(q);
        pipeline.annotate(annotation);
        double verbCnt = 0;
        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
            for (int i = 0; i < tokens.size(); i++) {
                CoreLabel token = tokens.get(i);
                String posTag = token.tag();
                if (posTag.toLowerCase().startsWith("vb")) {
                    if (i == 0 || !tokens.get(i - 1).tag().equalsIgnoreCase(posTag)) {
                        verbCnt++;
                    }
                }
            }

        }
        return verbCnt;
    }

    @Override
    public Attribute getAttribute() {
        return attribute;
    }
}

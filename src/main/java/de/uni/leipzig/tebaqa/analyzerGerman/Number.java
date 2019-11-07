package de.uni.leipzig.tebaqa.analyzerGerman;

import de.uni.leipzig.tebaqa.helper.StanfordPipelineProvider;
import de.uni.leipzig.tebaqa.helper.StanfordPipelineProviderGerman;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import org.aksw.mlqa.analyzer.IAnalyzer;
import weka.core.Attribute;

import java.util.List;

public class Number implements IAnalyzer {
    private Attribute attribute;
    private StanfordCoreNLP pipeline;

    public Number() {
        pipeline = StanfordPipelineProviderGerman.getSingletonPipelineInstance();
        attribute = new Attribute("NumberOfNumbers");
    }

    public Object analyze(String q) {
        Annotation annotation = new Annotation(q);
        pipeline.annotate(annotation);
        double numberCnt = 0;
        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
            for (CoreLabel token : tokens) {
                if (token.tag().startsWith("CARD")) {
                    numberCnt++;
                }
            }
        }
        return numberCnt;
    }

    @Override
    public Attribute getAttribute() {
        return attribute;
    }
}

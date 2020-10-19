package de.uni.leipzig.tebaqa.template.nlp.analyzer;

import de.uni.leipzig.tebaqa.tebaqacommons.nlp.StanfordPipelineProvider;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import org.aksw.mlqa.analyzer.IAnalyzer;
import weka.core.Attribute;

import java.util.List;

public class Verb implements IAnalyzer {
    private Attribute attribute = null;
    private StanfordCoreNLP pipeline;

    public Verb() {
        this.pipeline = StanfordPipelineProvider.getSingletonPipelineInstance(StanfordPipelineProvider.Lang.EN);
        attribute = new Attribute("NumberOfVerbs");
    }

    public Object analyze(String q) {
        Annotation annotation = new Annotation(q);
        pipeline.annotate(annotation);
        double verbCnt = 0;
        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
            for (CoreLabel token : tokens) {
                String posTag = token.tag();
                if (posTag.startsWith("VB")) {
                    verbCnt++;
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

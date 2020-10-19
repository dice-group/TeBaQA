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

public class Noun implements IAnalyzer {
    private Attribute attribute;
    private StanfordCoreNLP pipeline;

    public Noun() {
        pipeline = StanfordPipelineProvider.getSingletonPipelineInstance(StanfordPipelineProvider.Lang.EN);
        attribute = new Attribute("NumberOfNouns");
    }

    public Object analyze(String q) {
        Annotation annotation = new Annotation(q);
        if (!q.isEmpty()) {
            pipeline.annotate(annotation);
        }
        double nounCnt = 0;
        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
            for (int i = 0; i < tokens.size(); i++) {
                CoreLabel token = tokens.get(i);
                String posTag = token.tag();
                // don't count consecutive nouns
                if (posTag.toLowerCase().startsWith("nn")) {
                    if (i == 0 || !tokens.get(i - 1).tag().equalsIgnoreCase(posTag)) {
                        nounCnt++;
                    }
                }
            }
        }

        return nounCnt;
    }

    @Override
    public Attribute getAttribute() {
        return attribute;
    }
}

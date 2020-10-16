package de.uni.leipzig.tebaqa.template.nlp;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.util.HashMap;
import java.util.List;

public class SemanticAnalysisHelperEnglish extends SemanticAnalysisHelper {

    private final StanfordCoreNLP pipeline;


    public SemanticAnalysisHelperEnglish() {
        this.pipeline = StanfordPipelineProvider.getSingletonPipelineInstance(StanfordPipelineProvider.Lang.EN);
    }

//    public SemanticAnalysisHelperEnglish(StanfordCoreNLP pipeline) {
//        this.pipeline = pipeline;
//    }

    @Override
    public HashMap<String, String> getPosTags(String text) {
        Annotation annotation = new Annotation(text);
        pipeline.annotate(annotation);
        List<CoreLabel> tokens = annotation.get(CoreAnnotations.TokensAnnotation.class);
        HashMap<String, String> posTags = new HashMap<>();
        for (CoreLabel token : tokens) {
            String value = token.getString(CoreAnnotations.ValueAnnotation.class);
            String pos = token.getString(CoreAnnotations.PartOfSpeechAnnotation.class);
            posTags.put(value, pos);
        }

        return posTags;
    }
}

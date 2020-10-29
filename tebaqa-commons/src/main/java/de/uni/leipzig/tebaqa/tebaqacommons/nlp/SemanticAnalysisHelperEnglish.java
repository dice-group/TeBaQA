package de.uni.leipzig.tebaqa.tebaqacommons.nlp;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SemanticAnalysisHelperEnglish extends SemanticAnalysisHelper {

    private static final Logger LOGGER = Logger.getLogger(SemanticAnalysisHelperEnglish.class);

    private final StanfordCoreNLP pipeline;

    public SemanticAnalysisHelperEnglish() {
        this.pipeline = StanfordPipelineProvider.getSingletonPipelineInstance(Lang.EN);
    }

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

    @Override
    public String removeQuestionWords(String question) {
        String[] questionWords = "how many|how much|give me|list|give|show me|show|who|whom|when|were|what|why|whose|how|where|which|is|are|did|was|does".split("\\|");

        for (String questionWord : questionWords) {
            if (question.toLowerCase().startsWith(questionWord)) {
                return question.substring(questionWord.length()).trim();
            }
        }
        return question;
    }

    /**
     * Extracts the dependency graph out of a sentence. Note: Only the dependency graph of the first sentence is
     * recognized. Every following sentence will be ignored!
     *
     * @param text The string which contains the question.
     * @return The dependency graph.
     */
    @Override
    public SemanticGraph extractDependencyGraph(String text) {
        Annotation annotation = new Annotation(text);
        pipeline.annotate(annotation);

        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        if (sentences.size() > 1) {
            LOGGER.error("There is more than one sentence to analyze: " + text);
        }
        CoreMap sentence = sentences.get(0);
        SemanticGraph dependencyGraph = sentence.get(SemanticGraphCoreAnnotations.EnhancedDependenciesAnnotation.class);
        if (dependencyGraph == null) {
            return sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
        } else {
            return dependencyGraph;
        }
    }

    @Override
    public Map<String, String> getLemmas(String text) {
        return Collections.emptyMap(); // TODO ? this is not implemented in the old code
    }
}

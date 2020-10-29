package de.uni.leipzig.tebaqa.tebaqacommons.nlp;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

// TODO implement this helper for Deutsch
public class SemanticAnalysisHelperGerman extends SemanticAnalysisHelper {

    private static final Logger LOGGER = Logger.getLogger(SemanticAnalysisHelperGerman.class);

    private final StanfordCoreNLP pipeline;

    public SemanticAnalysisHelperGerman() {
//        this.pipeline = StanfordPipelineProvider.getSingletonPipelineInstance(Lang.DE);
        this.pipeline = null; // remove when implementation is done
    }

    @Override
    public HashMap<String, String> getPosTags(String text) {
        return null;
    }

    @Override
    public String removeQuestionWords(String question) {
        return null;
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
        return null;
    }

    @Override
    public Map<String, String> getLemmas(String text) {
        return Collections.emptyMap();
    }
}

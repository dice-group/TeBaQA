package de.uni.leipzig.tebaqa.nlp.core;

import de.uni.leipzig.tebaqa.tebaqacommons.model.QueryType;
import de.uni.leipzig.tebaqa.tebaqacommons.model.QuestionAnswerType;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.semgraph.SemanticGraph;

import java.util.HashMap;
import java.util.Map;

public interface NLPAnalyzer {
    Annotation annotate(String text);

    HashMap<String, String> getPosTags(String text);

    String removeQuestionWords(String question);

    SemanticGraph extractDependencyGraph(String text);

    Map<String, String> getLemmas(String text);

    QueryType mapQuestionToQueryType(String question);

    QuestionAnswerType detectQuestionAnswerType(String question);
}

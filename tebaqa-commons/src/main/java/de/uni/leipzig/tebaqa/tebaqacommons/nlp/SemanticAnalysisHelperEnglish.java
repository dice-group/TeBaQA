package de.uni.leipzig.tebaqa.tebaqacommons.nlp;

import de.uni.leipzig.tebaqa.tebaqacommons.model.QueryType;
import de.uni.leipzig.tebaqa.tebaqacommons.util.TextUtilities;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import org.apache.log4j.Logger;

import java.util.*;

public class SemanticAnalysisHelperEnglish extends SemanticAnalysisHelper {

    private static final Logger LOGGER = Logger.getLogger(SemanticAnalysisHelperEnglish.class);

    private static final String[] QUESTION_WORDS = "how many|how much|give me|list|give|show me|show|who|whom|when|were|what|why|whose|how|where|which|is|are|did|was|does".split("\\|");
    private static final List<String> SELECT_QUERY_INDICATORS = Arrays.asList("list|give|show|who|when|were|what|why|whose|how|where|which".split("\\|"));
    private static final List<String> ASK_QUERY_INDICATORS = Arrays.asList("is|are|did|was|does|can".split("\\|"));
    private static final String[] ASC_INDICATORS = new String[]{"first", "oldest", "smallest", "lowest", "shortest", "least"};
    private static final String[] DESC_INDICATORS = new String[]{"largest", "last", "highest", "most", "biggest", "youngest", "longest", "tallest"};


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

        for (String questionWord : QUESTION_WORDS) {
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

    @Override
    public QueryType mapQuestionToQueryType(String question) {
        List<String> firstThreeWords = getFirstThreeWords(question);
        String firstWord = firstThreeWords.get(0);

        if (hasAscAggregation(question)) {
            return QueryType.SELECT_SUPERLATIVE_ASC_QUERY;
        } else if (hasDescAggregation(question)) {
            return QueryType.SELECT_SUPERLATIVE_DESC_QUERY;
        } else if (hasCountAggregation(question)) {
            return QueryType.SELECT_COUNT_QUERY;
        } else if (firstThreeWords.stream().anyMatch(s -> SELECT_QUERY_INDICATORS.contains(s.toLowerCase()))) {
            return QueryType.SELECT_QUERY;
        } /*else if (firstThreeWords.parallelStream().anyMatch(s -> askIndicatorsList.contains(s.toLowerCase()))) {
            return SPARQLUtilities.ASK_QUERY;*/ else if (ASK_QUERY_INDICATORS.contains(firstWord)) {
            return QueryType.ASK_QUERY;
        } else {
            return QueryType.QUERY_TYPE_UNKNOWN;
        }

    }

    // TODO this can be improved by using POS tags?

    /**
     * Checks if a given sentence uses superlatives like first, least and so on which are indicators for aggregation queries.
     *
     * @param sentence A string which contains a sentence.
     * @return If the sentence contains keywords which are used in ascending aggregation queries.
     */
    private boolean hasAscAggregation(String sentence) {
        String[] words = sentence.toLowerCase().split(TextUtilities.NON_WORD_CHARACTERS_REGEX);
        return Arrays.stream(words).anyMatch(Arrays.asList(ASC_INDICATORS)::contains);
    }

    /**
     * Checks if a given sentence uses superlatives like largest, last, highest and so on which are indicators for aggregation queries.
     *
     * @param sentence A string which contains a sentence.
     * @return If the sentence contains keywords which are used in descending aggregation queries.
     */
    private boolean hasDescAggregation(String sentence) {
        String[] words = sentence.toLowerCase().split(TextUtilities.NON_WORD_CHARACTERS_REGEX);
        return Arrays.stream(words).anyMatch(Arrays.asList(DESC_INDICATORS)::contains);
    }

    private boolean hasCountAggregation(String sentence) {
        return sentence.toLowerCase().trim().startsWith("how many");
    }
}

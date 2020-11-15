package de.uni.leipzig.tebaqa.tebaqacommons.nlp;

import de.uni.leipzig.tebaqa.tebaqacommons.model.QueryType;
import de.uni.leipzig.tebaqa.tebaqacommons.util.TextUtilities;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import org.apache.log4j.Logger;

import java.util.*;

// TODO partial implementation. finish complete implementation helper for Deutsch.
public class SemanticAnalysisHelperGerman extends SemanticAnalysisHelper {

    private static final Logger LOGGER = Logger.getLogger(SemanticAnalysisHelperGerman.class);

    private static final List<String> QUESTION_WORDS = Arrays.asList("liste|gib|zeig mir|wer|wo|wann|was|warum|wessen|wie|welche|welches|welcher|ist|sind|hat|war".split("\\|"));
    private static final List<String> SELECT_INDICATORS = Arrays.asList("welche|liste|wie|wo|wann|warum|wessen|gib|in".split("\\|"));
    private static final List<String> ASK_INDICATORS = Arrays.asList("sind".split("\\|"));
    private static final String[] ASC_INDICATORS = new String[]{"erste", "älteste", "kleinste", "tiefste", "kürzeste", "wenigste"};
    private static final String[] DESC_INDICATORS = new String[]{"größte", "letzte", "höchste", "meiste", "jüngste", "längste", "schwerste"};

    private final StanfordCoreNLP pipeline;

    public SemanticAnalysisHelperGerman() {
//        this.pipeline = StanfordPipelineProvider.getSingletonPipelineInstance(Lang.DE);
        this.pipeline = null; // remove when implementation is done
        QUESTION_WORDS.sort(Comparator.reverseOrder()); // for e.g. welche and welcher, try to match welcher first.
    }

    @Override
    public HashMap<String, String> getPosTags(String text) {
        return null;
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
        return null;
    }

    @Override
    public Map<String, String> getLemmas(String text) {
        return Collections.emptyMap();
    }

    @Override
    public QueryType mapQuestionToQueryType(String question) {
        List<String> firstThreeWords = getFirstThreeWords(question);

        if (hasAscAggregation(question)) {
            return QueryType.SELECT_SUPERLATIVE_ASC_QUERY;
        } else if (hasDescAggregation(question)) {
            return QueryType.SELECT_SUPERLATIVE_DESC_QUERY;
        } else if (hasCountAggregation(question)) {
            return QueryType.SELECT_COUNT_QUERY;
        } else if (firstThreeWords.parallelStream().anyMatch(s -> SELECT_INDICATORS.contains(s.toLowerCase()))) {
            return QueryType.SELECT_QUERY;
        } else if (firstThreeWords.parallelStream().anyMatch(s -> ASK_INDICATORS.contains(s.toLowerCase()))) {
            return QueryType.ASK_QUERY;
        } else {
            return QueryType.QUERY_TYPE_UNKNOWN;
        }
    }

    /**
     * Checks if a given sentence uses superlatives like first, least and so on which are indicators for aggregation queries.
     *
     * @param sentence A string which contains a sentence.
     * @return If the sentence contains keywords which are used in ascending aggregation queries.
     */
    public boolean hasAscAggregation(String sentence) {
        String[] words = sentence.toLowerCase().split(TextUtilities.NON_WORD_CHARACTERS_REGEX);
        return Arrays.stream(words).anyMatch(Arrays.asList(ASC_INDICATORS)::contains);
    }

    /**
     * Checks if a given sentence uses superlatives like largest, last, highest and so on which are indicators for aggregation queries.
     *
     * @param sentence A string which contains a sentence.
     * @return If the sentence contains keywords which are used in descending aggregation queries.
     */
    public boolean hasDescAggregation(String sentence) {
        String[] words = sentence.toLowerCase().split(TextUtilities.NON_WORD_CHARACTERS_REGEX);
        return Arrays.stream(words).anyMatch(Arrays.asList(DESC_INDICATORS)::contains);
    }


    private boolean hasCountAggregation(String sentence) {
        return sentence.toLowerCase().trim().startsWith("wie viele");
    }
}

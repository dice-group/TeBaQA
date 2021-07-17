package de.uni.leipzig.tebaqa.nlp.core;

import de.uni.leipzig.tebaqa.tebaqacommons.model.QueryType;
import de.uni.leipzig.tebaqa.tebaqacommons.model.QuestionAnswerType;
import de.uni.leipzig.tebaqa.tebaqacommons.util.TextUtilities;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO partial implementation. finish complete implementation helper for Deutsch.
public class NLPAnalyzerGerman extends NLPAnalyzerBase {

    private static final Logger LOGGER = LogManager.getLogger(NLPAnalyzerGerman.class);

    private static final List<String> QUESTION_WORDS = Arrays.asList("liste|gib|zeig mir|wer|wo|wann|was|warum|wessen|wie|welche|welches|welcher|ist|sind|hat|war".split("\\|"));
    private static final List<String> SELECT_INDICATORS = Arrays.asList("welche|liste|wie|wo|wann|warum|wessen|gib|in".split("\\|"));
    private static final List<String> ASK_INDICATORS = Arrays.asList("sind".split("\\|"));
    private static final String[] ASC_INDICATORS = new String[]{"erste", "älteste", "kleinste", "tiefste", "kürzeste", "wenigste"};
    private static final String[] DESC_INDICATORS = new String[]{"größte", "letzte", "höchste", "meiste", "jüngste", "längste", "schwerste"};

    private final StanfordCoreNLP pipeline;

    public NLPAnalyzerGerman() {
//        this.pipeline = StanfordPipelineProvider.getSingletonPipelineInstance(Lang.DE);
        this.pipeline = null; // remove when implementation is done
        QUESTION_WORDS.sort(Comparator.reverseOrder()); // for e.g. welche and welcher, try to match welcher first.
    }

    @Override
    public Annotation annotate(String text) {
        Annotation annotation = new Annotation(text);
        pipeline.annotate(annotation);
        return annotation;
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

    private List<CoreLabel> getTokens(String text) {
        Annotation annotation = new Annotation(text);
        pipeline.annotate(annotation);
        return annotation.get(CoreAnnotations.TokensAnnotation.class);
    }

    @Override
    public QuestionAnswerType detectQuestionAnswerType(String question) {
        Pattern pattern = Pattern.compile("\\w+");
        List<CoreLabel> tokens = this.getTokens(question);
        Matcher m = pattern.matcher(question);
        if (m.find()) {
            String first = m.group();
            //if (first.isPresent()) {
            if (first.toLowerCase().matches("ist|hat|haben"))
                //if (first.get().toLowerCase().matches("be|do|have"))
                return QuestionAnswerType.BOOLEAN_ANSWER_TYPE;
        }
        //}
        if (question.toLowerCase().startsWith("wie lang") || question.toLowerCase().startsWith("wie viele") || question.toLowerCase().startsWith("wie hoch")
                || question.toLowerCase().startsWith("wie schwer") || question.toLowerCase().startsWith("wie breit") || question.toLowerCase().startsWith("wie ist")) {
            return QuestionAnswerType.NUMBER_ANSWER_TYPE;
        }
        if (question.toLowerCase().startsWith("wie") && tokens.size() >= 2) {
            CoreLabel token = tokens.get(1);
            //For cases like how big, how small, ...
            if (token.getString(CoreAnnotations.PartOfSpeechAnnotation.class).startsWith("AD")) {
                return QuestionAnswerType.NUMBER_ANSWER_TYPE;
            }

            if (question.toLowerCase().startsWith("wie lauten")) {
                return QuestionAnswerType.LIST_OF_RESOURCES_ANSWER_TYPE;
            }

            if (question.toLowerCase().startsWith("wie lautet")) {
                return QuestionAnswerType.SINGLE_ANSWER;
            }

        }
        if (question.toLowerCase().startsWith("in welchen"))
            return QuestionAnswerType.LIST_OF_RESOURCES_ANSWER_TYPE;
        if (question.toLowerCase().startsWith("in welcher"))
            return QuestionAnswerType.SINGLE_ANSWER;
        if (question.toLowerCase().startsWith("wann")) {
            return QuestionAnswerType.DATE_ANSWER_TYPE;
        }
        if (question.toLowerCase().startsWith("welche") && question.toLowerCase().contains("gibt es")) {
            return QuestionAnswerType.LIST_OF_RESOURCES_ANSWER_TYPE;
        }
        if (question.toLowerCase().startsWith("wann beginnen")) {
            return QuestionAnswerType.SINGLE_ANSWER;
        }

        if (question.toLowerCase().startsWith("welche") || question.toLowerCase().startsWith("welcher") ||
                question.toLowerCase().startsWith("gib")) {
            boolean hasPluralNoun = false;
            for (CoreLabel token : tokens) {
                String posTag = token.getString(CoreAnnotations.PartOfSpeechAnnotation.class);
                if (posTag.equalsIgnoreCase("NN")) {
                    String nounText = token.getString(CoreAnnotations.OriginalTextAnnotation.class);
                    if (nounText.endsWith("en")) {
                        hasPluralNoun = true;
                        break;
                    }
                }
            }

            if (hasPluralNoun) {
                return QuestionAnswerType.LIST_OF_RESOURCES_ANSWER_TYPE;
            } else {
                return QuestionAnswerType.SINGLE_ANSWER;
            }
        }

        return QuestionAnswerType.UNKNOWN_ANSWER_TYPE;
    }
}

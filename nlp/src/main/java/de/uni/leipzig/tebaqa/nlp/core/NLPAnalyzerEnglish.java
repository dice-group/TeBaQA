package de.uni.leipzig.tebaqa.nlp.core;

import de.uni.leipzig.tebaqa.tebaqacommons.model.QueryType;
import de.uni.leipzig.tebaqa.tebaqacommons.model.QuestionAnswerType;
import de.uni.leipzig.tebaqa.tebaqacommons.util.TextUtilities;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.util.CoreMap;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NLPAnalyzerEnglish extends NLPAnalyzerBase {

    private static final Logger LOGGER = Logger.getLogger(NLPAnalyzerEnglish.class);

    private static final String[] QUESTION_WORDS = "how many|how much|give me|list|give|show me|show|who|whom|when|were|what|why|whose|how|where|which|is|are|did|was|does".split("\\|");
    private static final List<String> SELECT_QUERY_INDICATORS = Arrays.asList("list|give|show|who|when|were|what|why|whose|how|where|which".split("\\|"));
    private static final List<String> ASK_QUERY_INDICATORS = Arrays.asList("is|are|did|was|does|can".split("\\|"));
    private static final String[] ASC_INDICATORS = new String[]{"first", "oldest", "smallest", "lowest", "shortest", "least"};
    private static final String[] DESC_INDICATORS = new String[]{"largest", "last", "highest", "most", "biggest", "youngest", "longest", "tallest"};

    private static final String LEMMA_EXCLUSION = "have|do|be|many|much|give|call|list";
    private static final String POS_EXCLUSION = "DT|IN|WDT|W.*|\\.";

    private final StanfordCoreNLP pipeline;

    public NLPAnalyzerEnglish() {
        this.pipeline = StanfordPipelineProvider.getSingletonPipelineInstance(NLPLang.EN);
    }

    @Override
    public Annotation annotate(String text) {
        Annotation annotation = new Annotation(text);
        pipeline.annotate(annotation);
        return annotation;
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

    public List<IndexedWord> getDependencySequence(SemanticGraph semanticGraph) {
        IndexedWord firstRoot = semanticGraph.getFirstRoot();
        return getDependenciesFromEdge(firstRoot, semanticGraph);
    }

    private List<IndexedWord> getDependenciesFromEdge(IndexedWord root, SemanticGraph semanticGraph) {

        List<IndexedWord> sequence = new ArrayList<>();
        String rootPos = root.get(CoreAnnotations.PartOfSpeechAnnotation.class);
        String rootLemma = root.get(CoreAnnotations.LemmaAnnotation.class);
        if (!rootPos.matches(POS_EXCLUSION) && !rootLemma.matches(LEMMA_EXCLUSION)) {
            sequence.add(root);
        }
        Set<IndexedWord> childrenFromRoot = semanticGraph.getChildren(root);

        for (IndexedWord word : childrenFromRoot) {
            String wordPos = word.get(CoreAnnotations.PartOfSpeechAnnotation.class);
            String wordLemma = word.get(CoreAnnotations.LemmaAnnotation.class);
            if (!wordPos.matches(POS_EXCLUSION) && !wordLemma.matches(LEMMA_EXCLUSION)) {
                sequence.add(word);
            }
            List<IndexedWord> children = semanticGraph.getChildList(word);
            //In some cases a leaf has itself as children which results in endless recursion.
            if (children.contains(root)) {
                children.remove(root);
            }
            for (IndexedWord child : children) {
                sequence.addAll(getDependenciesFromEdge(child, semanticGraph));
            }
        }
        return sequence;
    }

    @Override
    public Map<String, String> getLemmas(String text) {
        Sentence sentence = new Sentence(text);
        int wordCount = sentence.words().size();
        if (wordCount == sentence.lemmas().size()) {
            Map<String, String> lemmas = new LinkedHashMap<>(wordCount);
            for (int i = 0; i < wordCount; i++) {
                lemmas.put(sentence.word(i), sentence.lemma(i));
            }
            return lemmas;
        } else {
            LOGGER.error("Length of words and generated lemmas do not match: " + sentence.words() + " <-> " + sentence.lemmas());
            return Collections.emptyMap();
        }
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
            return SPARQLUtilities.ASK_QUERY;*/ else if (ASK_QUERY_INDICATORS.contains(firstWord.toLowerCase())) {
            return QueryType.ASK_QUERY;
        } else {
            return QueryType.QUERY_TYPE_UNKNOWN;
        }

    }

    @Override
    public QuestionAnswerType detectQuestionAnswerType(String question) {
        SemanticGraph semanticGraph = this.extractDependencyGraph(question);
        List<IndexedWord> sequence = this.getDependencySequence(semanticGraph);
        Pattern pattern = Pattern.compile("\\w+");
        Matcher m = pattern.matcher(question);
        if (m.find()) {
            Optional<String> first = getLemmas(m.group()).values().stream().findFirst();
            if (first.isPresent()) {
                if (first.get().toLowerCase().matches("be|do|have"))
                    return QuestionAnswerType.BOOLEAN_ANSWER_TYPE;
            }
        }
        if (question.toLowerCase().startsWith("how many") || question.toLowerCase().startsWith("how much")
                || question.toLowerCase().startsWith("how big") || question.toLowerCase().startsWith("how large")) {
            return QuestionAnswerType.NUMBER_ANSWER_TYPE;
        }
        if (question.toLowerCase().startsWith("how") && sequence.size() >= 2) {
            String posOfSecondWord = sequence.get(1).get(CoreAnnotations.PartOfSpeechAnnotation.class);
            //For cases like how big, how small, ...
            if (posOfSecondWord.startsWith("JJ")) {
                return QuestionAnswerType.NUMBER_ANSWER_TYPE;
            }
        }
        if (question.toLowerCase().startsWith("when")) {
            return QuestionAnswerType.DATE_ANSWER_TYPE;
        }
        for (IndexedWord word : sequence) {
            String posTag = word.get(CoreAnnotations.PartOfSpeechAnnotation.class);
            if (posTag.equalsIgnoreCase("NNS") || posTag.equalsIgnoreCase("NNPS")) {
                return QuestionAnswerType.LIST_OF_RESOURCES_ANSWER_TYPE;
            } else if (posTag.equalsIgnoreCase("NN") || posTag.equalsIgnoreCase("NNP")) {
                return QuestionAnswerType.SINGLE_ANSWER;
            }
        }
        return QuestionAnswerType.UNKNOWN_ANSWER_TYPE;
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
        String trim = sentence.toLowerCase().trim();
        return trim.contains("how many") || trim.startsWith("count ")
                || trim.contains("give me the number") || trim.contains("give me a number")
                || trim.contains("give me the total number") || trim.contains("give me a total number")
                || trim.contains("give me the count") || trim.contains("give me a count");
    }

    public static void main(String[] args) {
        Sentence sentence = new Sentence("Marie was born in Paris.");
        System.out.println(sentence.words());
        System.out.println(sentence.lemmas());

    }
}

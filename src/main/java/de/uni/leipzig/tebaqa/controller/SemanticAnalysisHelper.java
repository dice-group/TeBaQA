package de.uni.leipzig.tebaqa.controller;

import com.hp.hpl.jena.rdf.model.RDFNode;
import de.uni.leipzig.tebaqa.analyzer.Analyzer;
import de.uni.leipzig.tebaqa.helper.ClassifierProvider;
import de.uni.leipzig.tebaqa.helper.QueryMappingFactory;
import de.uni.leipzig.tebaqa.helper.SPARQLUtilities;
import de.uni.leipzig.tebaqa.helper.StanfordPipelineProvider;
import de.uni.leipzig.tebaqa.helper.TextUtilities;
import de.uni.leipzig.tebaqa.helper.Utilities;
import de.uni.leipzig.tebaqa.model.CustomQuestion;
import de.uni.leipzig.tebaqa.model.QueryTemplateMapping;
import de.uni.leipzig.tebaqa.model.ResultsetBinding;
import de.uni.leipzig.tebaqa.model.SPARQLResultSet;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.log4j.Logger;
import weka.classifiers.Classifier;
import weka.core.Instance;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static de.uni.leipzig.tebaqa.helper.HypernymMappingProvider.getHypernymMapping;
import static de.uni.leipzig.tebaqa.helper.TextUtilities.NON_WORD_CHARACTERS_REGEX;
import static de.uni.leipzig.tebaqa.helper.Utilities.ARGUMENTS_BETWEEN_SPACES;
import static de.uni.leipzig.tebaqa.helper.Utilities.getLevenshteinRatio;

public class SemanticAnalysisHelper {
    private static Logger log = Logger.getLogger(SemanticAnalysisHelper.class);
    private StanfordCoreNLP pipeline;
    private static DateTimeFormatter dateTimeFormatterLong = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static DateTimeFormatter dateTimeFormatterShortMonth = DateTimeFormatter.ofPattern("yyyy-M-dd");
    private static DateTimeFormatter dateTimeFormatterShortDay = DateTimeFormatter.ofPattern("yyyy-MM-d");


    public SemanticAnalysisHelper() {
        this.pipeline = StanfordPipelineProvider.getSingletonPipelineInstance();
    }

    public static int determineQueryType(String q) {
        List<String> selectIndicatorsList = Arrays.asList("list|give|show|who|when|were|what|why|whose|how|where|which".split("\\|"));
        List<String> askIndicatorsList = Arrays.asList("is|are|did|was|does|can".split("\\|"));
        //log.debug("String question: " + q);
        String[] split = q.split("\\s+");
        List<String> firstThreeWords = new ArrayList<>();
        if (split.length > 3) {
            firstThreeWords.addAll(Arrays.asList(split).subList(0, 3));
        } else {
            firstThreeWords.addAll(Arrays.asList(split));
        }
        if (SemanticAnalysisHelper.hasAscAggregation(q)) {
            return SPARQLUtilities.SELECT_SUPERLATIVE_ASC_QUERY;
        } else if (SemanticAnalysisHelper.hasDescAggregation(q)) {
            return SPARQLUtilities.SELECT_SUPERLATIVE_DESC_QUERY;
        } else if (SemanticAnalysisHelper.hasCountAggregation(q)) {
            return SPARQLUtilities.SELECT_COUNT_QUERY;
        } else if (firstThreeWords.parallelStream().anyMatch(s -> selectIndicatorsList.contains(s.toLowerCase()))) {
            return SPARQLUtilities.SELECT_QUERY;
        } else if (firstThreeWords.parallelStream().anyMatch(s -> askIndicatorsList.contains(s.toLowerCase()))) {
            return SPARQLUtilities.ASK_QUERY;
        } else {
            return SPARQLUtilities.QUERY_TYPE_UNKNOWN;
        }
    }

    public Annotation annotate(String text) {
        Annotation annotation = new Annotation(text);
        pipeline.annotate(annotation);
        return annotation;
    }

    /**
     * Extracts the dependency graph out of a sentence. Note: Only the dependency graph of the first sentence is
     * recognized. Every following sentence will be ignored!
     *
     * @param text The string which contains the question.
     * @return The dependency graph.
     */
    public SemanticGraph extractDependencyGraph(String text) {
        Annotation annotation = new Annotation(text);
        pipeline.annotate(annotation);

        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        if (sentences.size() > 1) {
            log.error("There is more than one sentence to analyze: " + text);
        }
        CoreMap sentence = sentences.get(0);
        SemanticGraph dependencyGraph = sentence.get(SemanticGraphCoreAnnotations.EnhancedDependenciesAnnotation.class);
        if (dependencyGraph == null) {
            return sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
        } else {
            return dependencyGraph;
        }
    }

    /**
     * Extracts a map of possible query templates and their graph patterns.
     *
     * @param questions The questions which contain a SPARQL query which will be used as template.
     * @param nodes     A list containing all entities from DBpedia's ontology.
     * @return A list which contains SPARQL query templates, divided by their number of entities and classes and by
     * their query type (ASK or SELECT).
     */
    public Map<String, QueryTemplateMapping> extractTemplates(List<CustomQuestion> questions, List<RDFNode> nodes, List<String> properties) {
        Map<String, QueryTemplateMapping> mappings = new HashMap<>();
        for (CustomQuestion question : questions) {
            String query = question.getQuery();
            QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question.getQuestionText(), query, nodes, properties);
            String queryPattern = queryMappingFactory.getQueryPattern();
            boolean isSuperlativeDesc = false;
            boolean isSuperlativeAsc = false;
            boolean isCountQuery = false;

            if (queryPattern.toLowerCase().contains("order by desc") && queryPattern.toLowerCase().contains("order by desc") && queryPattern.toLowerCase().contains("limit 1")) {
                isSuperlativeDesc = true;
            } else if (queryPattern.toLowerCase().contains("order by asc") && queryPattern.toLowerCase().contains("limit 1")) {
                isSuperlativeAsc = true;
            }
            if (queryPattern.toLowerCase().contains("count")) {
                isCountQuery = true;
            }

            if (!queryPattern.toLowerCase().contains("http://dbpedia.org/resource/")
                    && !queryPattern.toLowerCase().contains("sum") && !queryPattern.toLowerCase().contains("avg")
                    && !queryPattern.toLowerCase().contains("min") && !queryPattern.toLowerCase().contains("max")
                    && !queryPattern.toLowerCase().contains("filter") && !queryPattern.toLowerCase().contains("bound")) {
                int classCnt = 0;
                int propertyCnt = 0;

                List<String> triples = Utilities.extractTriples(queryPattern);
                for (String triple : triples) {
                    Matcher argumentMatcher = ARGUMENTS_BETWEEN_SPACES.matcher(triple);
                    int argumentCnt = 0;
                    while (argumentMatcher.find()) {
                        String argument = argumentMatcher.group();
                        if (argument.startsWith("<^") && (argumentCnt == 0 || argumentCnt == 2)) {
                            classCnt++;
                        } else if (argument.startsWith("<^") && argumentCnt == 1) {
                            propertyCnt++;
                        }
                        argumentCnt++;
                    }
                }

                int finalClassCnt = classCnt;
                int finalPropertyCnt = propertyCnt;
                String graph = question.getGraph();
                int queryType = SPARQLUtilities.getQueryType(query);
                if (mappings.containsKey(graph)) {
                    QueryTemplateMapping mapping = mappings.get(graph);
                    if (mapping.getNumberOfClasses() == finalClassCnt && mapping.getNumberOfClasses() == finalPropertyCnt) {
                        if (isSuperlativeDesc) {
                            mapping.addSelectSuperlativeDescTemplate(queryPattern, question.getQuery());
                        } else if (isSuperlativeAsc) {
                            mapping.addSelectSuperlativeAscTemplate(queryPattern, question.getQuery());
                        } else if (isCountQuery) {
                            mapping.addCountTemplate(queryPattern, question.getQuery());
                        } else if (queryType == SPARQLUtilities.SELECT_QUERY) {
                            mapping.addSelectTemplate(queryPattern, question.getQuery());
                        } else if (queryType == SPARQLUtilities.ASK_QUERY) {
                            mapping.addAskTemplate(queryPattern, question.getQuery());
                        } else if (queryType == SPARQLUtilities.QUERY_TYPE_UNKNOWN) {
                            log.error("Unknown query type: " + query);
                        }
                    }
                } else {
                    QueryTemplateMapping mapping = new QueryTemplateMapping(classCnt, propertyCnt);
                    if (isSuperlativeDesc) {
                        mapping.addSelectSuperlativeDescTemplate(queryPattern, question.getQuery());
                    } else if (isSuperlativeAsc) {
                        mapping.addSelectSuperlativeAscTemplate(queryPattern, question.getQuery());
                    } else if (isCountQuery) {
                        mapping.addCountTemplate(queryPattern, question.getQuery());
                    } else if (queryType == SPARQLUtilities.SELECT_QUERY) {
                        mapping.addSelectTemplate(queryPattern, question.getQuery());
                    } else if (queryType == SPARQLUtilities.ASK_QUERY) {
                        mapping.addAskTemplate(queryPattern, question.getQuery());
                    }
                    //create a new mapping class
                    mappings.put(graph, mapping);
                }
                //log.info(queryPattern);
            }
        }
        return mappings;
    }

    public static Map<String, String> getLemmas(String q) {
        if (q.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, String> lemmas = new HashMap<>();
        Annotation annotation = new Annotation(q);
        StanfordCoreNLP pipeline = StanfordPipelineProvider.getSingletonPipelineInstance();
        pipeline.annotate(annotation);
        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            List<CoreLabel> labels = sentence.get(CoreAnnotations.TokensAnnotation.class);
            for (CoreLabel token : labels) {
                String word = token.get(CoreAnnotations.TextAnnotation.class);
                String lemma = token.get(CoreAnnotations.LemmaAnnotation.class);
                lemmas.put(word, lemma);
            }
        }
        return lemmas;
    }

    public static Map<String, String> getPOS(String q) {
        if (q.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, String> pos = new HashMap<>();
        Annotation annotation = new Annotation(q);
        StanfordCoreNLP pipeline = StanfordPipelineProvider.getSingletonPipelineInstance();
        pipeline.annotate(annotation);
        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            List<CoreLabel> labels = sentence.get(CoreAnnotations.TokensAnnotation.class);
            for (CoreLabel token : labels) {
                String word = token.get(CoreAnnotations.TextAnnotation.class);
                String posAnnotation = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                pos.put(word, posAnnotation);
            }
        }
        return pos;
    }

    /**
     * Checks if a given sentence uses superlatives like first, least and so on which are indicators for aggregation queries.
     *
     * @param sentence A string which contains a sentence.
     * @return If the sentence contains keywords which are used in ascending aggregation queries.
     */
    public static boolean hasAscAggregation(String sentence) {
        String[] ascIndicators = new String[]{"first", "oldest", "smallest", "lowest", "shortest", "least"};
        String[] words = sentence.split(NON_WORD_CHARACTERS_REGEX);
        return Arrays.stream(words).anyMatch(Arrays.asList(ascIndicators)::contains);
    }

    /**
     * Checks if a given sentence uses superlatives like largest, last, highest and so on which are indicators for aggregation queries.
     *
     * @param sentence A string which contains a sentence.
     * @return If the sentence contains keywords which are used in descending aggregation queries.
     */
    public static boolean hasDescAggregation(String sentence) {
        String[] descIndicators = new String[]{"largest", "last", "highest", "most", "biggest", "youngest", "longest", "tallest"};
        String[] words = sentence.split(NON_WORD_CHARACTERS_REGEX);
        return Arrays.stream(words).anyMatch(Arrays.asList(descIndicators)::contains);
    }


    private static boolean hasCountAggregation(String sentence) {
        return sentence.toLowerCase().trim().startsWith("how many") || sentence.toLowerCase().trim().startsWith("how much");
    }

    /**
     * Classifies a question and tries to find the best matching graph pattern for it's SPARQL query.
     *
     * @param question  The question which shall be classified.
     * @return The predicted graph pattern.
     */
    String classifyInstance(String question) {
        Analyzer analyzer = ClassifierProvider.getAnalyzer();
        Instance instance = analyzer.analyze(question);
        instance.setDataset(ClassifierProvider.getDataset());
        instance.setMissing(ClassifierProvider.getClassAttribute());

        String predictedGraph = "";
        try {
            Classifier cls = ClassifierProvider.getSingletonClassifierInstance();
            double predictedClass = cls.classifyInstance(instance);
            predictedGraph = instance.classAttribute().value((int) predictedClass);
        } catch (Exception e) {
            log.error(String.format("Unable to classify question: '%s'!", question), e);
        }
        return predictedGraph;
    }

    /**
     * Checks, if a question is inside a Map.
     *
     * @param map  The map in which the question is not is not.
     * @param text The question text.
     * @return true if the text is inside, false otherwise.
     */
    boolean containsQuestionText(Map<String, String> map, String text) {
        boolean isInside = false;
        for (Entry<String, String> entry : map.entrySet()) {
            if (entry.getValue().equals(text)) {
                isInside = true;
                break;
            }
        }
        return isInside;
    }

    /**
     * Removes all variables, prefixes, newlines, standard keywords like ASK, SELECT, WHERE, DISTINCT.
     *
     * @param queryString The sparql query string.
     * @return A string which only contains sparql modifiers, a '?' as placeholder for a variable and '<>' as
     * placeholders for strings like this: { <> <> ? . ? <> ? FILTER regex( ? , ? ) }
     */
    String cleanQuery(String queryString) {
        Query query = QueryFactory.create(queryString);
        query.setPrefixMapping(null);
        return query.toString().trim()
                //replace newlines with space
                .replaceAll("\n", " ")
                //replace every variable with ?
                .replaceAll("\\?[a-zA-Z\\d]+", " ? ")
                //replace every number(e.g. 2 or 2.5) with a ?
                .replaceAll("\\s+\\d+\\.?\\d*", " ? ")
                //replace everything in quotes with ?
                .replaceAll("([\"'])(?:(?=(\\\\?))\\2.)*?\\1", " ? ")
                //remove everything between <>
                .replaceAll("<\\S*>", " <> ")
                //remove all SELECT, ASK, DISTINCT and WHERE keywords
                .replaceAll("(?i)(select|ask|where|distinct)", " ")
                //remove every consecutive spaces
                .replaceAll("\\s+", " ");
    }

    public List<IndexedWord> getDependencySequence(SemanticGraph semanticGraph) {
        IndexedWord firstRoot = semanticGraph.getFirstRoot();
        return getDependenciesFromEdge(firstRoot, semanticGraph);
    }

    private static List<IndexedWord> getDependenciesFromEdge(IndexedWord root, SemanticGraph semanticGraph) {
        final String posExclusion = "DT|IN|WDT|W.*|\\.";
        final String lemmaExclusion = "have|do|be|many|much|give|call|list";
        List<IndexedWord> sequence = new ArrayList<>();
        String rootPos = root.get(CoreAnnotations.PartOfSpeechAnnotation.class);
        String rootLemma = root.get(CoreAnnotations.LemmaAnnotation.class);
        if (!rootPos.matches(posExclusion) && !rootLemma.matches(lemmaExclusion)) {
            sequence.add(root);
        }
        Set<IndexedWord> childrenFromRoot = semanticGraph.getChildren(root);

        for (IndexedWord word : childrenFromRoot) {
            String wordPos = word.get(CoreAnnotations.PartOfSpeechAnnotation.class);
            String wordLemma = word.get(CoreAnnotations.LemmaAnnotation.class);
            if (!wordPos.matches(posExclusion) && !wordLemma.matches(lemmaExclusion)) {
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

    public static int detectQuestionAnswerType(String question) {
        SemanticAnalysisHelper semanticAnalysisHelper = new SemanticAnalysisHelper();
        SemanticGraph semanticGraph = semanticAnalysisHelper.extractDependencyGraph(question);
        List<IndexedWord> sequence = semanticAnalysisHelper.getDependencySequence(semanticGraph);
        Pattern pattern = Pattern.compile("\\w+");
        Matcher m = pattern.matcher(question);
        if (m.find()) {
            Optional<String> first = SemanticAnalysisHelper.getLemmas(m.group()).values().stream().findFirst();
            if (first.isPresent()) {
                if (first.get().toLowerCase().matches("be|do|have"))
                    return SPARQLResultSet.BOOLEAN_ANSWER_TYPE;
            }
        }
        if (question.toLowerCase().startsWith("how many") || question.toLowerCase().startsWith("how much")
                || question.toLowerCase().startsWith("how big") || question.toLowerCase().startsWith("how large")) {
            return SPARQLResultSet.NUMBER_ANSWER_TYPE;
        }
        if (question.toLowerCase().startsWith("how") && sequence.size() >= 2) {
            String posOfSecondWord = sequence.get(1).get(CoreAnnotations.PartOfSpeechAnnotation.class);
            //For cases like how big, how small, ...
            if (posOfSecondWord.startsWith("JJ")) {
                return SPARQLResultSet.NUMBER_ANSWER_TYPE;
            }
        }
        if (question.toLowerCase().startsWith("when")) {
            return SPARQLResultSet.DATE_ANSWER_TYPE;
        }
        for (IndexedWord word : sequence) {
            String posTag = word.get(CoreAnnotations.PartOfSpeechAnnotation.class);
            if (posTag.equalsIgnoreCase("NNS") || posTag.equalsIgnoreCase("NNPS")) {
                return SPARQLResultSet.LIST_OF_RESOURCES_ANSWER_TYPE;
            } else if (posTag.equalsIgnoreCase("NN") || posTag.equalsIgnoreCase("NNP")) {
                return SPARQLResultSet.SINGLE_ANSWER;
            }
        }
        return SPARQLResultSet.UNKNOWN_ANSWER_TYPE;
    }


    ResultsetBinding getBestAnswer(Set<ResultsetBinding> results, Map<String, String> entitiyToQuestionMapping, int expectedAnswerType, boolean forceResult) {
        results.parallelStream().forEach(resultsetBinding -> {
            Map<String, String> bindings = resultsetBinding.getBindings();
            Double rating = 0.0;
            if (!forceResult && resultsetBinding.getAnswerType() != expectedAnswerType) {
                resultsetBinding.setRating(0.0);
            } else {
                for (String variable : bindings.keySet()) {
                    if (variable.toLowerCase().startsWith("class_") || variable.toLowerCase().startsWith("property_")) {
                        String uri = bindings.get(variable);
                        String relatedPhrase = entitiyToQuestionMapping.getOrDefault(uri, "");
                        double relatednessFactor;
                        if (relatedPhrase.isEmpty()) {
                            relatednessFactor = 0;
                        } else {
                            relatednessFactor = TextUtilities.countWords(relatedPhrase) - getLevenshteinRatio(SPARQLUtilities.getBaseNameFromDBpediaEntitiy(uri).toLowerCase(), relatedPhrase.toLowerCase());
                        }
                        if (bindings.values().stream().filter(s -> s.equals(uri)).collect(Collectors.toList()).size() > 1) {
                            //If the binding is used more than once reduce its rating
                            //rating += relatednessFactor / 2;
                            rating = 0.0;
                        } else {
                            rating += relatednessFactor;
                        }
                    } else {
                        //Unbound variables mostly lead to worse results
                        rating = -5.0;
                    }
                }
                if (rating > 0) {
                    if (resultsetBinding.getResult().size() >= 50) {
                        rating *= (1 / 3);
                    }
                    if (resultsetBinding.getAnswerType() != expectedAnswerType) {
                        rating *= 2;
                    }
                    if (resultsetBinding.getAnswerType() == SPARQLResultSet.BOOLEAN_ANSWER_TYPE && resultsetBinding.getResult().size() == 1 && resultsetBinding.getResult().stream().findFirst().get().equalsIgnoreCase("false")) {
                        rating *= (1 / 2);
                    }
                } else {
                    if (resultsetBinding.getResult().size() >= 50) {
                        rating *= 3;
                    }
                    if (resultsetBinding.getAnswerType() != expectedAnswerType) {
                        rating *= (1 / 2);
                    }

                    if (resultsetBinding.getAnswerType() == SPARQLResultSet.BOOLEAN_ANSWER_TYPE && resultsetBinding.getResult().size() == 1 && resultsetBinding.getResult().stream().findFirst().get().equalsIgnoreCase("false")) {
                        rating *= 2;
                    }
                }

                resultsetBinding.setRating(rating);
            }
        });


        Optional<ResultsetBinding> bestAnswerWithMatchingResultType = results.stream().filter(resultsetBinding -> resultsetBinding.getAnswerType() == expectedAnswerType).max(Comparator.comparingDouble(ResultsetBinding::getRating));
        if (bestAnswerWithMatchingResultType.isPresent()) {
            return bestAnswerWithMatchingResultType.get();
        } else {
            if (forceResult) {
                return results.stream()
                        .max(Comparator.comparingDouble(ResultsetBinding::getRating)).orElseGet(ResultsetBinding::new);
            } else {
                return results.stream()
                        .filter(resultsetBinding -> resultsetBinding.getRating() > 0)
                        .max(Comparator.comparingDouble(ResultsetBinding::getRating)).orElseGet(ResultsetBinding::new);
            }
        }
    }

    public static List<String> getHypernymsFromWiktionary(String s) {
        Map<String, List<String>> hypernymMapping = getHypernymMapping();
        return hypernymMapping.getOrDefault(s, new ArrayList<>());
    }


    public static long countUpperCase(String s) {
        return s.chars().filter(Character::isUpperCase).count();
    }

    public static String removeQuestionWords(String question) {
        List<String> questionWords = Arrays.asList("list|give me|give|show me|show|who|when|were|what|why|whose|how|where|which|is|are|did|was|does".split("\\|"));

        for (String questionWord : questionWords) {
            if (question.toLowerCase().startsWith(questionWord)) {
                return question.substring(questionWord.length(), question.length()).trim();
            }
        }
        return question;
    }
}

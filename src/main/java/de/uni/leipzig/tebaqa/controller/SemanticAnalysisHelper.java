package de.uni.leipzig.tebaqa.controller;

import com.hp.hpl.jena.rdf.model.RDFNode;
import de.uni.leipzig.tebaqa.analyzer.Analyzer;
import de.uni.leipzig.tebaqa.helper.QueryMappingFactory;
import de.uni.leipzig.tebaqa.helper.SPARQLUtilities;
import de.uni.leipzig.tebaqa.helper.StanfordPipelineProvider;
import de.uni.leipzig.tebaqa.helper.Utilities;
import de.uni.leipzig.tebaqa.model.CustomQuestion;
import de.uni.leipzig.tebaqa.model.QueryTemplateMapping;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.log4j.Logger;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.uni.leipzig.tebaqa.helper.Utilities.ARGUMENTS_BETWEEN_SPACES;

public class SemanticAnalysisHelper {
    private static Logger log = Logger.getLogger(SemanticAnalysisHelper.class);
    StanfordCoreNLP pipeline;


    public SemanticAnalysisHelper() {
        this.pipeline = StanfordPipelineProvider.getSingletonPipelineInstance();
    }

    public static int determineQueryType(String q) {
        String selectIndicators = "Give|Show|Who|When|Were|What|Why|Whose|How|Where|Which";
        String askIndicators = "Is|Are|Did|Was|Does";
        log.debug("String question: " + q);
        String[] split = q.split("\\s+");
        List<String> firstThreeWords = new ArrayList<>();
        if (split.length > 3) {
            firstThreeWords.addAll(Arrays.asList(split));
        } else {
            firstThreeWords.addAll(Arrays.asList(split));
        }
        Pattern selectPattern = Pattern.compile(".*" + selectIndicators + ".*", Pattern.CASE_INSENSITIVE);
        Pattern askPattern = Pattern.compile(".*" + askIndicators + ".*", Pattern.CASE_INSENSITIVE);

        if (firstThreeWords.stream().anyMatch(s -> selectPattern.matcher(s).find())) {
            return SPARQLUtilities.SELECT_QUERY;
        } else if (firstThreeWords.stream().anyMatch(s -> askPattern.matcher(s).find())) {
            return SPARQLUtilities.ASK_QUERY;
        } else {
            return SPARQLUtilities.QUERY_TYPE_UNKNOWN;
        }
    }

    public Annotation annotate(String text) {
        Annotation annotation = new Annotation(text);
        pipeline.annotate(annotation);

        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            SemanticGraph dependencyGraph = sentence.get(SemanticGraphCoreAnnotations.EnhancedDependenciesAnnotation.class);
            if (dependencyGraph == null) {
                dependencyGraph = sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
            }
            //dependencyGraph.prettyPrint();
            //String compactGraph = dependencyGraph.toCompactString();

            //log.info(compactGraph);
        }


        //pipeline.prettyPrint(annotation, System.out);
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
    Map<String, List<QueryTemplateMapping>> extractTemplates(List<CustomQuestion> questions, List<RDFNode> nodes) {
        Map<String, List<QueryTemplateMapping>> mappings = new HashMap<>();
        for (CustomQuestion question : questions) {
            String query = question.getQuery();
            QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question.getQuestionText(), query, nodes);
            String queryPattern = queryMappingFactory.getQueryPattern();

            // if (!queryPattern.contains("http://")) {
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
                Optional<QueryTemplateMapping> match = mappings.get(graph).stream()
                        .filter(mapping -> mapping.getNumberOfClasses() == finalClassCnt && mapping.getNumberOfClasses() == finalPropertyCnt)
                        .findFirst();


                if (match.isPresent()) {
                    QueryTemplateMapping currentMapping = match.get();
                    if (queryType == SPARQLUtilities.SELECT_QUERY) {
                        currentMapping.addSelectTemplate(queryPattern);
                    } else if (queryType == SPARQLUtilities.ASK_QUERY) {
                        currentMapping.addAskTemplate(queryPattern);
                    } else if (queryType == SPARQLUtilities.QUERY_TYPE_UNKNOWN) {
                        log.error("Unknown query type: " + query);
                    }
                }
            } else {
                QueryTemplateMapping mapping = new QueryTemplateMapping(classCnt, propertyCnt);
                if (queryType == SPARQLUtilities.SELECT_QUERY) {
                    mapping.addSelectTemplate(queryPattern);
                } else if (queryType == SPARQLUtilities.ASK_QUERY) {
                    mapping.addAskTemplate(queryPattern);
                }
                //create a new mapping class
                List<QueryTemplateMapping> list = new ArrayList<>();
                list.add(mapping);
                mappings.put(graph, list);
            }
            //log.info(queryPattern);
            //}
        }
        return mappings;
    }

    /**
     * Classifies a question and tries to find the best matching graph pattern for it's SPARQL query.
     *
     * @param question  The question which shall be classified.
     * @param graphList A list containing every possible graph pattern.
     * @return The predicted graph pattern.
     */
    String classifyInstance(CustomQuestion question, HashSet<String> graphList) {
        ArrayList<Attribute> attributes = new ArrayList<>();

        List<String> filter = new ArrayList<>();
        filter.add("Filter");
        filter.add("noFilter");

        List<String> optional = new ArrayList<>();
        optional.add("Optional");
        optional.add("noOptional");

        List<String> limit = new ArrayList<>();
        limit.add("Limit");
        limit.add("noLimit");

        List<String> orderBy = new ArrayList<>();
        orderBy.add("OrderBy");
        orderBy.add("noOrderBy");

        List<String> union = new ArrayList<>();
        union.add("Union");
        union.add("noUnion");

        Attribute filterAttribute = new Attribute("filter", filter);
        Attribute optionalAttribute = new Attribute("optional", optional);
        Attribute limitAttribute = new Attribute("limit", limit);
        Attribute orderByAttribute = new Attribute("orderBy", orderBy);
        Attribute unionAttribute = new Attribute("union", union);

        attributes.add(filterAttribute);
        attributes.add(optionalAttribute);
        attributes.add(limitAttribute);
        attributes.add(orderByAttribute);
        attributes.add(unionAttribute);

        Attribute classAttribute = new Attribute("class", new ArrayList<>(graphList));
        attributes.add(classAttribute);

        Analyzer analyzer = new Analyzer(attributes);
        Instances dataset = new Instances("testdata", analyzer.fvWekaAttributes, 1);
        dataset.setClassIndex(dataset.numAttributes() - 1);
        Instance instance = analyzer.analyze(question.getQuestionText());
        instance.setDataset(dataset);
        instance.setMissing(classAttribute);
        String[] classes = new String[graphList.size()];
        int i = 0;
        for (Enumeration<Object> e = classAttribute.enumerateValues(); e.hasMoreElements(); ) {
            String graph = (String) e.nextElement();
            classes[i] = graph;
            i++;
        }

        //TODO The class detection ratio is lower than it should! (<50%)
        String predictedGraph = "";
        try {
            Classifier cls = (Classifier) SerializationHelper.read("./src/main/resources/multilayerPerceptron.model");
            double predictedClass = cls.classifyInstance(instance);

            predictedGraph = instance.classAttribute().value((int) predictedClass);
            //log.info(String.format("Question: '%s' \nPredicted class: %s", question.getQuestionText(), predictedGraph));
            //log.info("Classified instance: " + instance);

        } catch (Exception e) {
            log.error("Unable to load weka model file!", e);
        }
        if (predictedGraph.equals(question.getGraph())) {
            //log.info("Predicted class is correct.");
        } else {
            //log.info("Predicted class is incorrect! Predicted: " + predictedGraph + "; actual: " + question.getGraph());
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
        for (Map.Entry<String, String> entry : map.entrySet()) {
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
}
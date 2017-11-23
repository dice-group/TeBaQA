package de.uni.leipzig.tebaqa.helper;

import com.google.common.collect.Lists;
import com.hp.hpl.jena.rdf.model.RDFNode;
import de.uni.leipzig.tebaqa.controller.SemanticAnalysisHelper;
import de.uni.leipzig.tebaqa.model.QueryTemplateMapping;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.util.CoreMap;
import org.aksw.hawk.index.DBOIndex;
import org.aksw.qa.annotation.index.IndexDBO_classes;
import org.aksw.qa.annotation.index.IndexDBO_properties;
import org.aksw.qa.commons.datastructure.Entity;
import org.aksw.qa.commons.nlp.nerd.Spotlight;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Resource;
import org.apache.log4j.Logger;
import org.assertj.core.util.Sets;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static de.uni.leipzig.tebaqa.helper.Utilities.ARGUMENTS_BETWEEN_SPACES;
import static de.uni.leipzig.tebaqa.helper.Utilities.getLevenshteinRatio;
import static edu.stanford.nlp.ling.CoreAnnotations.*;
import static java.lang.String.join;
import static org.apache.commons.lang3.StringUtils.getLevenshteinDistance;

/**
 * Creates a Mapping between a part-of-speech tag sequence and a SPARQL query.
 * Algorithm:
 * <br>Input: Question, dependencySequencePos(mapping between the words of the question and their part-of-speech tag),
 * QueryPattern from <i>sparqlQuery</i></br>
 * <ol>
 * <li>Get Named Entities of the <i>question</i> from DBpedia Spotlight.</li>
 * <li>Replace every named entity from step 1 in the <i><QueryPattern</i> with its part-of-speech tag.
 * If there is no exactly same entity to replace in step go to step 3.</li>
 * <li>Find possible matches based on string similarities:
 * <ol>
 * <li type="1">Create a List with all possible neighbor co-occurrences from the words in <i>Question</i>. Calculate the
 * levenshtein distance between every neighbor co-occurrence permutation and the entity from Spotlight</li>
 * <li type="1">If the distance of the likeliest group of neighbor co-occurrences is lower than 0.5 and the ratio between
 * the 2 likeliest group of words is smaller than 0.7, replace the resource in the <i>QueryPattern</i> with the
 * part-of-speech tags of the word group</li>
 * </ol>
 * </li>
 * <li>For every resource in the <i>QueryPattern</i> which isn't detected in the steps above, search for a
 * similar(based on levenshtein distance, see step 3) string in the question.</li>
 * <p>
 * </ol>
 */
public class QueryMappingFactory {

    private int queryType = -1;

    private static Logger log = Logger.getLogger(QueryMappingFactory.class);
    private String queryPattern = "";
    private Map<String, List<String>> unresolvedEntities = new HashMap<>();
    private String question = "";
    private List<RDFNode> ontologyNodes = new ArrayList<>();
    private List<String> properties = new ArrayList<>();
    private Set<String> rdfEntities = new HashSet<>();

    /**
     * Default constructor. Tries to create a mapping between a word or group of words and a resource in it's SPARQL
     * query based on it's POS tags.
     *  @param dependencySequencePos A map which contains every relevant word as key and its part-of-speech tag as value.
     *                              Like this: "Airlines" -> "NNP"
     * @param sparqlQuery           The SPARQL query with a query pattern. The latter is used to replace resources with their
     * @param ontologyNodes                 A list with all RDF nodes from DBpedia's ontology.
     */
    public QueryMappingFactory(String question, Map<String, String> dependencySequencePos, String sparqlQuery, List<RDFNode> ontologyNodes) {
        this.ontologyNodes = ontologyNodes;
        this.queryType = SemanticAnalysisHelper.determineQueryType(question);

        this.question = question;
        String queryString = SPARQLUtilities.resolveNamespaces(sparqlQuery);
        List<String> permutations = getNeighborCoOccurrencePermutations(question.split(" "));
        //final String[] tmpQueryPatternString = replaceOntologyResources(question, dependencySequencePos, queryString);
        String tmpQueryPatternString = queryString;

        Map<String, List<Entity>> spotlightEntities = extractSpotlightEntities(question);
        tmpQueryPatternString = replaceSpotlightEntities(dependencySequencePos, permutations, tmpQueryPatternString, spotlightEntities);

        //log.info("unresolved: " + unresolvedEntities);
        queryString = tmpQueryPatternString;

        Pattern pattern = Pattern.compile("<(.*?)>");
        Matcher matcher = pattern.matcher(queryString);

        //Step 4: If there is a resource which isn't detected by Spotlight, search for a similar string in the question.
        //Find every resource between <>
        while (matcher.find()) {
            String resource = matcher.group(1);
            if (!resource.startsWith("<^") && !resource.startsWith("^")) {
                String[] split = resource.split("/");
                String entity = split[split.length - 1];
                //TODO in this case: Give me all launch pads operated by NASA. -> <http://dbpedia.org/ontology/LaunchPad> isn't recognized, because of the direct string matching!
                if (dependencySequencePos.containsKey(entity)) {
                    queryString = queryString.replace(resource,
                            "^" + dependencySequencePos.get(entity) + "^");
                } else {
                    //Calculate levenshtein distance
                    TreeMap<Double, String> distances = getLevenshteinDistances(permutations, entity);
                    queryString = conditionallyReplaceResourceWithPOSTag(dependencySequencePos, tmpQueryPatternString,
                            resource, distances);
                }
            }
        }

        this.queryPattern = queryString
                .replaceAll("\n", " ")
                .replaceAll("\\s+", " ");
    }

    public QueryMappingFactory(String question, String sparqlQuery, List<RDFNode> ontologyNodes, List<String> properties) {
        this.ontologyNodes = ontologyNodes;
        this.properties = properties;
        this.queryType = SemanticAnalysisHelper.determineQueryType(question);

        this.question = question;
        String queryString = SPARQLUtilities.resolveNamespaces(sparqlQuery);

        // queryString.replaceAll("<(.*?)>", )
        int i = 0;
        String regex = "<(.+?)>";
        Pattern pattern = Pattern.compile(regex);
        Matcher m = pattern.matcher(queryString);
        while (m.find()) {
            String group = m.group();
            if (!group.contains("^")) {
                queryString = queryString.replaceFirst(group, "<^VAR_" + i + "^>");
                i++;
            }
        }
        this.queryPattern = queryString
                .replaceAll("\n", " ")
                .replaceAll("\\s+", " ");
    }


    private String replaceSpotlightEntities(Map<String, String> dependencySequencePos, List<String> permutations,
                                            String queryPattern, Map<String, List<Entity>> spotlightEntities) {
        final String[] tmpQueryPattern = new String[1];
        tmpQueryPattern[0] = queryPattern;
        if (spotlightEntities.size() > 0) {
            spotlightEntities.get("en").forEach((Entity entity) -> {
                        String label = entity.getLabel();
                        List<Resource> uris = entity.getUris();
                        String[] words = label.split("\\s");
                        //Replace every named entity with its part-of-speech tag
                        //e.g.: <http://dbpedia.org/resource/Computer_science> => computer science => NN0 NN1 => ^NN0_NN1^
                        for (Resource uri : uris) {
                            String newQueryTemplate;

                            List<String> wordPos = new ArrayList<>();
                            for (String word : words) {
                                wordPos.add(dependencySequencePos.get(word));
                            }
                            String wordPosReplacement = "^" + join("_", wordPos) + "^";
                            if (tmpQueryPattern[0].toLowerCase().contains("<" + uri.toString().toLowerCase() + ">")) {
                                newQueryTemplate = tmpQueryPattern[0].replace(uri.toString(), wordPosReplacement);
                            } else {
                                //get the most similar word
                                TreeMap<Double, String> distances = getLevenshteinDistances(permutations, uri.getLocalName());
                                newQueryTemplate = conditionallyReplaceResourceWithPOSTag(dependencySequencePos, tmpQueryPattern[0],
                                        uri.toString(), distances);
                            }
                            tmpQueryPattern[0] = newQueryTemplate;
                        }
                    }
            );
        }
        return tmpQueryPattern[0];
    }

    private Map<String, List<Entity>> extractSpotlightEntities(String question) {
        //get named entities (consisting of one or multiple words) from DBpedia's Spotlight
        Spotlight spotlight = Utilities.createCustomSpotlightInstance("http://model.dbpedia-spotlight.org/en/annotate");
        spotlight.setConfidence(0.4);
        spotlight.setSupport("20");
        return spotlight.getEntities(question);
    }

    private String conditionallyReplaceResourceWithPOSTag(Map<String, String> dependencySequencePos,
                                                          String stringWithResources, String uriToReplace,
                                                          TreeMap<Double, String> distances) {
        String newString = stringWithResources;

        //Check if the difference between the two shortest distances is big enough
        if (distances.size() > 1) {
            Object[] keys = distances.keySet().toArray();
            //The thresholds are based on testing and might be suboptimal.
            if ((double) keys[0] < 0.5 && (double) keys[0] / (double) keys[1] < 0.7) {
                List<String> posList = new ArrayList<>();
                String[] split = distances.firstEntry().getValue().split(" ");
                for (String aSplit : split) {
                    posList.add(dependencySequencePos.get(aSplit));
                }
                if (newString.contains("<" + uriToReplace + ">")) {
                    newString = newString.replace(uriToReplace, "^" + join("_", posList) + "^");
                }
            } else {
                unresolvedEntities.put(uriToReplace, new ArrayList<>(dependencySequencePos.keySet()));
            }
        }
        return newString;
    }

    @NotNull
    private TreeMap<Double, String> getLevenshteinDistances(List<String> permutations, String string) {
        TreeMap<Double, String> distances = new TreeMap<>();
        permutations.forEach((word) -> {
            int lfd = getLevenshteinDistance(string, word);
            double ratio = ((double) lfd) / (Math.max(string.length(), word.length()));
            distances.put(ratio, word);
        });
        return distances;
    }

    private List<String> getNeighborCoOccurrencePermutations(String[] s) {
        List<String> permutations = new ArrayList<>();
        for (int i = 0; i <= s.length; i++) {
            for (int y = 1; y <= s.length - i; y++) {
                permutations.add(join(" ", Arrays.asList(s).subList(i, i + y)));
            }
        }
        return permutations;
    }

    private List<List<Integer>> createDownwardCountingPermutations(int a, int b) {
        List<List<Integer>> permutations = new ArrayList<>();
        for (int i = a; i >= 0; i--) {
            for (int y = b; y >= 0; y--) {
                List<Integer> newPermutation = new ArrayList<>();
                newPermutation.add(i);
                newPermutation.add(y);
                permutations.add(newPermutation);
            }
        }
        permutations.sort((List a1, List a2) -> ((int) a2.get(0) + (int) a2.get(1)) - ((int) a1.get(0) + (int) a1.get(1)));
        return permutations;
    }

    /**
     * Creates a SPARQL Query Pattern like this: SELECT DISTINCT ?uri WHERE { ^NNP_0 ^VBZ_0 ?uri . }
     * Every entity which is recognized with the DBPedia Spotlight API is replaced by it's part-of-speech Tag.
     *
     * @return A string with part-of-speech tag placeholders.
     */
    public String getQueryPattern() {
        return queryPattern;
    }

    public List<String> generateQueries(Map<String, QueryTemplateMapping> mappings, String graph, List<String> injectedVariables) {
        Set<String> rdfResources;
        if (!injectedVariables.isEmpty()) {
            rdfResources = Sets.newHashSet(injectedVariables);
        } else {
            rdfResources = extractResources(question);
        }
        //log.info("Found resources: " + Strings.join(rdfResources, "; "));

        List<String> classes = new ArrayList<>();
        List<String> properties = new ArrayList<>();
        for (String resource : rdfResources) {
            if (resourceStartsLowercase(resource)) {
                properties.add(resource);
            } else {
                classes.add(resource);
            }
        }

        int classCount = classes.size();
        int propertyCount = properties.size();
        //if there is no suitable mapping with exactly the amount of properties and classes, reduce both consecutively
        //and try again.
        //TODO Use eccentric method in case their are less RDF resources than slots
        List<String> suitableMappings = new ArrayList<>();
        List<List<Integer>> downwardCountingPermutations = createDownwardCountingPermutations(classCount, propertyCount);
        for (List<Integer> permutation : downwardCountingPermutations) {
            Integer classCount1 = permutation.get(0);
            Integer propertyCount1 = permutation.get(1);
            suitableMappings = getSuitableMappings(mappings, classCount1, propertyCount1, queryType, graph);
            if (!suitableMappings.isEmpty()) {
                break;
            }
        }

        return Lists.newArrayList(fillPatterns(rdfResources, suitableMappings));
    }

    Set<String> extractResources(String question) {
        Set<String> rdfResources = new HashSet<>();

        Map<String, List<Entity>> spotlightEntities = extractSpotlightEntities(question);
        if (spotlightEntities.size() > 0) {
            spotlightEntities.get("en").forEach(entity -> entity.getUris().forEach(resource -> rdfResources.add(resource.getURI())));
        }

        String[] wordsFromQuestion = question.split("\\W+");
        for (String word : wordsFromQuestion) {
            rdfResources.addAll(getProperties(word));
            rdfResources.addAll(getOntologies(word));
        }

        List<String> coOccurrences = getNeighborCoOccurrencePermutations(wordsFromQuestion);
        coOccurrences = coOccurrences.stream()
                .filter(s -> s.split("\\W+").length < 5)
                .collect(Collectors.toList());

        Set<String> ontologyURIs = new HashSet<>();
        ontologyNodes.forEach(rdfNode -> ontologyURIs.add(rdfNode.toString()));

        //TODO enable parallelization with coOccurrences.parallelStream().forEach
        coOccurrences.forEach(coOccurrence -> {
            Map<String, Double> currentResources = new HashMap<>();
            Map<String, Double> currentOntologies = new HashMap<>();

            String[] coOccurrenceSplitted = coOccurrence.split("\\W+");
            String lemmasJoined = joinCapitalizedLemmas(coOccurrenceSplitted, false, true);
            tryDBpediaResourceNamingCombinations(ontologyURIs, coOccurrenceSplitted, lemmasJoined).forEach(s -> {
                if (isResource(s)) {
                    currentResources.put(s, getLevenshteinRatio(s, coOccurrence));
                } else if (isOntology(s)) {
                    currentOntologies.put(s, getLevenshteinRatio(s, coOccurrence));
                } else {
                    log.error(String.format("WARNING: Entity '%s' neither starts with http://dbpedia.org/resource nor with http://dbpedia.org/ontology/ ", s));
                }
            });

            String lemmasJoinedCapitalized = joinCapitalizedLemmas(coOccurrenceSplitted, true, true);
            tryDBpediaResourceNamingCombinations(ontologyURIs, coOccurrenceSplitted, lemmasJoinedCapitalized).forEach(s -> {
                if (isResource(s)) {
                    currentResources.put(s, getLevenshteinRatio(s, coOccurrence));
                } else if (isOntology(s)) {
                    currentOntologies.put(s, getLevenshteinRatio(s, coOccurrence));
                } else {
                    log.error(String.format("WARNING: Entity '%s' neither starts with http://dbpedia.org/resource nor with http://dbpedia.org/ontology/ ", s));
                }
            });

            String wordsJoined = joinCapitalizedLemmas(coOccurrenceSplitted, false, false);
            tryDBpediaResourceNamingCombinations(ontologyURIs, coOccurrenceSplitted, wordsJoined).forEach(s -> {
                if (isResource(s)) {
                    currentResources.put(s, getLevenshteinRatio(s, coOccurrence));
                } else if (isOntology(s)) {
                    currentOntologies.put(s, getLevenshteinRatio(s, coOccurrence));
                } else {
                    log.error(String.format("WARNING: Entity '%s' neither starts with http://dbpedia.org/resource nor with http://dbpedia.org/ontology/ ", s));
                }
            });

            String wordsJoinedCapitalized = joinCapitalizedLemmas(coOccurrenceSplitted, true, false);
            tryDBpediaResourceNamingCombinations(ontologyURIs, coOccurrenceSplitted, wordsJoinedCapitalized).forEach(s -> {
                if (isResource(s)) {
                    currentResources.put(s, getLevenshteinRatio(s, coOccurrence));
                } else if (isOntology(s)) {
                    currentOntologies.put(s, getLevenshteinRatio(s, coOccurrence));
                } else {
                    log.error(String.format("WARNING: Entity '%s' neither starts with http://dbpedia.org/resource nor with http://dbpedia.org/ontology/ ", s));
                }
            });
            searchInDBOIndex(coOccurrence).forEach(s -> {
                if (isResource(s)) {
                    currentResources.put(s, getLevenshteinRatio(s, coOccurrence));
                } else if (isOntology(s)) {
                    currentOntologies.put(s, getLevenshteinRatio(s, coOccurrence));
                } else {
                    log.error(String.format("WARNING: Entity '%s' neither starts with http://dbpedia.org/resource nor with http://dbpedia.org/ontology/ ", s));
                }
            });
            List<String> bestOntologies = getKeyByLowestValue(currentOntologies);
            rdfResources.addAll(bestOntologies);
            List<String> bestResources = getKeyByLowestValue(currentResources);
            rdfResources.addAll(bestResources);
        });

        //Set<String> coOccurrenceEntities = new HashSet<>();
        //coOccurrences.parallelStream().forEach(string -> coOccurrenceEntities.addAll(SPARQLUtilities.findDBpediaEntities(string)));


        //log.info("Resources: " + Strings.join(rdfResources, "; "));
        this.rdfEntities = rdfResources;
        return rdfResources;
    }

    private boolean isResource(String s) {
        return s.startsWith("http://dbpedia.org/resource/");
    }

    private boolean isOntology(String s) {
        return s.startsWith("http://dbpedia.org/ontology/") || s.startsWith("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")|| s.startsWith("'http://dbpedia.org/datatype/");
    }

    private List<String> getKeyByLowestValue(Map<String, Double> map) {
        if (map.isEmpty()) {
            return Collections.emptyList();
        }
        Optional<Double> maxOptional = map.values().stream().min(Comparator.naturalOrder());
        double min = Double.MAX_VALUE;
        if (maxOptional.isPresent()) {
            min = maxOptional.get();
        }
        double finalMin = min;
        return map.entrySet().stream()
                .filter(e -> e.getValue() == finalMin)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private Set<String> searchInDBOIndex(String coOccurrence) {
        DBOIndex dboIndex = new DBOIndex();
        List<String> search = dboIndex.search(coOccurrence);
        Set<String> resultsInDBOIndex = search.stream()
                .filter(s -> {
                    String[] split = s.split("/");
                    String baseResourceName = split[split.length - 1];
                    double ratio = Utilities.getLevenshteinRatio(coOccurrence, baseResourceName);
                    return ratio <= 0.7;
                })
                .collect(Collectors.toSet());

        IndexDBO_classes indexDBO_classes = new IndexDBO_classes();
        List<String> indexDBO_classesSearch = indexDBO_classes.search(coOccurrence);
        Set<String> resultsInDBOIndexClass = indexDBO_classesSearch.stream()
                .filter(s -> {
                    String[] split = s.split("/");
                    String baseResourceName = split[split.length - 1];
                    double ratio = Utilities.getLevenshteinRatio(coOccurrence, baseResourceName);
                    //TODO instead of using string similarity use the shortest one (e.g. Television instead of TelevisionShow) if it exists
                    return ratio < 0.5;
                })
                .collect(Collectors.toSet());

        IndexDBO_properties indexDBO_properties = new IndexDBO_properties();
        List<String> indexDBO_propertySearch = indexDBO_properties.search(coOccurrence);
        Set<String> resultsInDBOIndexProperty = indexDBO_propertySearch.stream()
                .filter(s -> {
                    String[] split = s.split("/");
                    String baseResourceName = split[split.length - 1];
                    double ratio = Utilities.getLevenshteinRatio(coOccurrence, baseResourceName);
                    return ratio < 0.5;
                })
                .collect(Collectors.toSet());

        resultsInDBOIndex.addAll(resultsInDBOIndexClass);
        resultsInDBOIndex.addAll(resultsInDBOIndexProperty);
        return resultsInDBOIndex;
    }

    private List<String> tryDBpediaResourceNamingCombinations(Set<String> ontologyURIs, String[] words, String lemmasJoined) {
        List<String> addToResult = new ArrayList<>();
        if (words.length > 1 && SPARQLUtilities.isDBpediaEntity(String.format("http://dbpedia.org/resource/%s", lemmasJoined))) {
            addToResult.add(String.format("http://dbpedia.org/resource/%s", String.join("_", words)));
        }
        if (words.length <= 3 && ontologyURIs.contains(String.format("http://dbpedia.org/ontology/%s", lemmasJoined))) {
            addToResult.add(String.format("http://dbpedia.org/ontology/%s", lemmasJoined));
        }
        return addToResult;
    }

    public List<String> generateQueries(Map<String, QueryTemplateMapping> mappings) {
        return generateQueries(mappings, null, new ArrayList<>());
    }

    private String joinCapitalizedLemmas(String[] strings, boolean capitalizeFirstLetter, boolean useLemma) {
        final String[] result = {""};
        List<String> list = Arrays.asList(strings);
        if (useLemma) {
            list.forEach(s -> result[0] += StringUtils.capitalize(new Sentence(s).lemma(0)));
        } else {
            list.forEach(s -> result[0] += StringUtils.capitalize(s));
        }
        //The first letter is lowercase sometimes
        if (capitalizeFirstLetter) {
            return StringUtils.uncapitalize(result[0]);
        } else {
            return result[0];
        }
    }

    private Set<String> fillPatterns(Set<String> rdfResources, List<String> suitableMappings) {
        Set<String> sparqlQueries = new HashSet<>();
        List<Map<String, String>> replacements = new ArrayList<>();
        List<String> toReplaceProperties = new ArrayList<>();
        for (String pattern : suitableMappings) {
            List<String> triples = Utilities.extractTriples(pattern);
            for (String triple : triples) {
                int argumentCnt = 0;
                List<String> toReplaceClasses = new ArrayList<>();
                triple = triple.replace("{", "").replace("}", "");
                Matcher argumentMatcher = ARGUMENTS_BETWEEN_SPACES.matcher(triple);
                while (argumentMatcher.find()) {
                    String argument = argumentMatcher.group();
                    if (argument.startsWith("<^") && Utilities.isEven(argumentCnt)) {
                        toReplaceClasses.add(argument);
                    } else if (argument.startsWith("<^") && !Utilities.isEven(argumentCnt)) {
                        toReplaceProperties.add(argument);
                    }
                    argumentCnt++;
                }
                for (String placeholder : toReplaceClasses) {
                    for (String rdfResource : rdfResources) {
                        if (!resourceStartsLowercase(rdfResource)) {
                            Map<String, String> mapping = new HashMap<>();
                            mapping.put(placeholder, rdfResource);
                            replacements.add(mapping);
                        }
                    }
                }
                for (String placeholder : toReplaceProperties) {
                    for (String rdfResource : rdfResources) {
                        if (resourceStartsLowercase(rdfResource)) {
                            Map<String, String> mapping = new HashMap<>();
                            mapping.put(placeholder, rdfResource);
                            replacements.add(mapping);
                        }
                    }
                }
            }

            List<String> classResources = new ArrayList<>();
            List<String> propertyResources = new ArrayList<>();
            for (String resource : rdfResources) {
                if (!resourceStartsLowercase(resource)) {
                    classResources.add(resource);
                } else if (resourceStartsLowercase(resource)) {
                    propertyResources.add(resource);
                }
            }

            sparqlQueries.add(Utilities.fillPattern(pattern, classResources, propertyResources));
        }
        return sparqlQueries;
    }

    private List<String> getSuitableMappings(Map<String, QueryTemplateMapping> mappings, int classCount, int propertyCount, int queryType, String graph) {
        List<QueryTemplateMapping> templatesForGraph = new ArrayList<>();
        if (graph == null) {
            templatesForGraph = new ArrayList<>(mappings.values());
        } else {
            templatesForGraph.add(mappings.get(graph));
        }
        List<String> result = new ArrayList<>();
        if (queryType == SPARQLUtilities.ASK_QUERY) {

            result = templatesForGraph.stream()
                    //.filter(map -> map.getNumberOfClasses() <= classCount && map.getNumberOfProperties() <= propertyCount)
                    .map(QueryTemplateMapping::getAskTemplates)
                    .flatMap(Collection::stream).collect(Collectors.toList());

            //templatesForGraph.forEach(queryTemplateMapping -> result.addAll(queryTemplateMapping.getAskTemplates()));
        } else if (queryType == SPARQLUtilities.SELECT_QUERY) {

            result = templatesForGraph.stream()
                    // .filter(map -> map.getNumberOfClasses() <= classCount && map.getNumberOfProperties() <= propertyCount)
                    .map(QueryTemplateMapping::getSelectTemplates)
                    .flatMap(Collection::stream).collect(Collectors.toList());

            //templatesForGraph.forEach(queryTemplateMapping -> result.addAll(queryTemplateMapping.getSelectTemplates()));
        } else {
            result.addAll(templatesForGraph.stream()
                    //.filter(map -> map.getNumberOfClasses() <= classCount && map.getNumberOfProperties() <= propertyCount)
                    .map(QueryTemplateMapping::getAskTemplates)
                    .flatMap(Collection::stream).collect(Collectors.toList()));
            result.addAll(templatesForGraph.stream()
                    //.filter(map -> map.getNumberOfClasses() <= classCount && map.getNumberOfProperties() <= propertyCount)
                    .map(QueryTemplateMapping::getSelectTemplates)
                    .flatMap(Collection::stream).collect(Collectors.toList()));

            // templatesForGraph.forEach(queryTemplateMapping -> result.addAll(queryTemplateMapping.getAskTemplates()));
            // templatesForGraph.forEach(queryTemplateMapping -> result.addAll(queryTemplateMapping.getSelectTemplates()));
        }
        return result;
    }

    //TODO Don't just check for string equality, example: birth -> birthPlace
    //TODO Check for Neighbor Co-Occurrences? ethnic group -> http://dbpedia.org/ontology/ethnicGroup; same as in QueryMappingfactory.extractResources()
    private List<String> getProperties(String word) {
        List<String> result = new ArrayList<>();
        final String relevantPos = "JJ.*|NN.*|VB.*";
        Annotation document = new Annotation(word);
        StanfordCoreNLP pipeline = StanfordPipelineProvider.getSingletonPipelineInstance();
        pipeline.annotate(document);
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
                String pos = token.get(PartOfSpeechAnnotation.class);
                String lemma = token.get(LemmaAnnotation.class);
                if (pos.matches(relevantPos)) {
                    result.addAll(properties.stream()
                            .filter(property -> property.equalsIgnoreCase(String.format("http://dbpedia.org/property/%s", lemma)))
                            .collect(Collectors.toSet()));
                }
            }
        }
        return result;
    }

    private List<String> getOntologies(String word) {
        List<String> result = new ArrayList<>();
        final String relevantPos = "JJ.*|NN.*|VB.*";
        Annotation document = new Annotation(word);
        StanfordCoreNLP pipeline = StanfordPipelineProvider.getSingletonPipelineInstance();
        pipeline.annotate(document);
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
                String pos = token.get(PartOfSpeechAnnotation.class);
                String lemma = token.get(LemmaAnnotation.class);
                if (pos.matches(relevantPos)) {
                    result.addAll(ontologyNodes.stream()
                            .filter(rdfNode -> rdfNode.toString().equalsIgnoreCase(String.format("http://dbpedia.org/ontology/%s", lemma)))
                            .map(RDFNode::toString)
                            .collect(Collectors.toSet()));
                }
            }
        }
        return result;
    }

    public Map<String, List<String>> getUnresolvedEntities() {
        return unresolvedEntities;
    }

    private boolean resourceStartsLowercase(String rdfResource) {
        return rdfResource.startsWith("http://dbpedia.org/property/")
                || rdfResource.startsWith("http://www.w3.org/1999/02/22-rdf-syntax-ns#")
                || (rdfResource.contains("/") && rdfResource.charAt(rdfResource.length() - 1) != '/' && Character.isLowerCase(rdfResource.substring(rdfResource.lastIndexOf('/') + 1).charAt(0)));
    }

    public Set<String> getRdfEntities() {
        return rdfEntities;
    }
}

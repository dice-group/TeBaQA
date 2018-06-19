package de.uni.leipzig.tebaqa.helper;

import com.google.common.collect.Sets;
import com.hp.hpl.jena.rdf.model.RDFNode;
import de.uni.leipzig.tebaqa.controller.SemanticAnalysisHelper;
import de.uni.leipzig.tebaqa.model.QueryTemplateMapping;
import de.uni.leipzig.tebaqa.model.RatedEntity;
import de.uni.leipzig.tebaqa.model.SPARQLResultSet;
import de.uni.leipzig.tebaqa.model.WordNetWrapper;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.util.CoreMap;
import joptsimple.internal.Strings;
import org.aksw.hawk.index.DBOIndex;
import org.aksw.hawk.index.Patty_relations;
import org.aksw.qa.annotation.index.IndexDBO_classes;
import org.aksw.qa.annotation.index.IndexDBO_properties;
import org.aksw.qa.commons.datastructure.Entity;
import org.aksw.qa.commons.nlp.nerd.Spotlight;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Resource;
import org.apache.log4j.Logger;
import org.ehcache.Cache;
import org.ehcache.PersistentCacheManager;
import org.jetbrains.annotations.NotNull;
import weka.core.Stopwords;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static de.uni.leipzig.tebaqa.controller.SemanticAnalysisHelper.getLemmas;
import static de.uni.leipzig.tebaqa.helper.TextUtilities.NON_WORD_CHARACTERS_REGEX;
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

    private int queryType;
    private static Logger log = Logger.getLogger(QueryMappingFactory.class);
    private String queryPattern;
    private String question;
    private List<RDFNode> ontologyNodes;
    private List<String> properties;
    private Map<String, String> entitiyToQuestionMapping;
    private Map<String, String> entitiyToQuestionMappingWithSynonyms;
    private boolean entitiyToQuestionMappingWasSet;
    private boolean entitiyToQuestionMappingWithSynonymsWasSet;
    private Set<String> ontologyURIs;
    //private Configuration pattyPhrases;
    private PersistentCacheManager cacheManager;
    private Patty_relations patty_relations;

    public QueryMappingFactory(String question, String sparqlQuery, List<RDFNode> ontologyNodes, List<String> properties) {
        this.ontologyNodes = ontologyNodes;
        ontologyURIs = new HashSet<>();
        ontologyNodes.forEach(rdfNode -> ontologyURIs.add(rdfNode.toString()));
        this.properties = properties;
        this.queryType = SemanticAnalysisHelper.determineQueryType(question);
        this.entitiyToQuestionMapping = new HashMap<>();
        this.entitiyToQuestionMappingWasSet = false;
        this.entitiyToQuestionMappingWithSynonyms = new HashMap<>();
        this.entitiyToQuestionMappingWithSynonymsWasSet = false;
        //this.pattyPhrases = PattyPhrasesProvider.getPattyPhrases();
        this.patty_relations = new Patty_relations();

        this.question = question;
        String queryString = SPARQLUtilities.resolveNamespaces(sparqlQuery);

        this.cacheManager = CacheProvider.getSingletonCacheInstance();

        // queryString.replaceAll("<(.*?)>", )
        int i = 0;
        String regex = "<(.+?)>";
        Pattern pattern = Pattern.compile(regex);
        Matcher m = pattern.matcher(queryString);
        while (m.find()) {
            String group = m.group();
            if (!group.contains("^")) {
                queryString = queryString.replaceFirst(Pattern.quote(group), "<^VAR_" + i + "^>");
                i++;
            }
        }
        this.queryPattern = queryString
                .replaceAll("\n", " ")
                .replaceAll("\\s+", " ")
                .trim();
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

    static Map<String, List<Entity>> extractSpotlightEntities(String question) {
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

    private static List<String> getNeighborCoOccurrencePermutations(String[] s) {
        List<String> permutations = new ArrayList<>();
        for (int i = 0; i <= s.length; i++) {
            for (int y = 1; y <= s.length - i; y++) {
                if (y - i < 6) {
                    permutations.add(join(" ", Arrays.asList(s).subList(i, i + y)));
                }
            }
        }
        return permutations;
    }

    public static List<String> getNeighborCoOccurrencePermutations(List<String> s) {
        return getNeighborCoOccurrencePermutations(s.toArray(new String[0]));
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

    public Set<String> generateQueries(Map<String, QueryTemplateMapping> mappings, String graph, boolean useSynonyms) {
        if (!useSynonyms && !this.entitiyToQuestionMappingWasSet) {
            entitiyToQuestionMapping.putAll(extractEntities(question));
            this.entitiyToQuestionMappingWasSet = true;
        } else if (useSynonyms && !this.entitiyToQuestionMappingWithSynonymsWasSet) {
            entitiyToQuestionMappingWithSynonyms.putAll(extractEntitiesUsingSynonyms(question));
            this.entitiyToQuestionMappingWithSynonymsWasSet = true;
        }

        List<String> suitableMappings = getSuitableMappings(mappings, queryType, graph);
        Set<String> queries;
        if (!useSynonyms) {
            queries = fillPatterns(entitiyToQuestionMapping, suitableMappings);
        } else {
            Map<String, String> m = new HashMap<>();
            m.putAll(entitiyToQuestionMapping);
            m.putAll(entitiyToQuestionMappingWithSynonyms);
            queries = fillPatterns(m, suitableMappings);
        }
        return queries;
    }

    Map<String, String> extractEntities(String question) {
        Map<String, String> entitiyToQuestionMapping = new HashMap<>();
        question = SemanticAnalysisHelper.removeQuestionWords(question);

//        Map<String, List<Entity>> spotlightEntities = extractSpotlightEntities(question);
//        if (spotlightEntities.size() > 0) {
//            spotlightEntities.get("en").forEach(entity -> {
//                Set<String> uris = entity.getUris().stream().map(Resource::getURI).collect(Collectors.toSet());
//                uris.forEach(s -> entitiyToQuestionMapping.put(s, entity.getLabel()));
//            });
//        }

        List<String> wordsFromQuestion = Arrays.asList(question.split(NON_WORD_CHARACTERS_REGEX));
        for (String word : wordsFromQuestion) {
            Map<String, String> lemmas = getLemmas(word);
            Map<String, String> pos = SemanticAnalysisHelper.getPOS(word);
            if (!Stopwords.isStopword(word)
                    && !lemmas.getOrDefault(word, "").equalsIgnoreCase("be")
                    && !lemmas.getOrDefault(word, "").equalsIgnoreCase("the")
                    && !pos.getOrDefault(word, "").equalsIgnoreCase("WP")
                    && !pos.getOrDefault(word, "").equalsIgnoreCase("DT")
                    && !pos.getOrDefault(word, "").equalsIgnoreCase("IN")) {
                List<String> hypernyms = SemanticAnalysisHelper.getHypernymsFromWiktionary(word);
                Set<String> matchingHypernyms = hypernyms.stream().map(this::getProperties).flatMap(s -> s.keySet().stream()).collect(Collectors.toSet());
                matchingHypernyms.addAll(hypernyms.stream().map(this::getOntologyClass).flatMap(s -> s.keySet().stream()).collect(Collectors.toSet()));
                matchingHypernyms.forEach(s -> entitiyToQuestionMapping.put(s, word));
            }
        }

        Map<String, String> wordPosMap = SemanticAnalysisHelper.getPOS(question);
        List<String> coOccurrences = getNeighborCoOccurrencePermutations(wordsFromQuestion);
        coOccurrences = coOccurrences.parallelStream()
                .filter(s -> s.split(NON_WORD_CHARACTERS_REGEX).length <= 6)
                .collect(Collectors.toList());

        Cache<String, HashMap> cache = cacheManager.getCache("persistent-cache", String.class, HashMap.class);
        Consumer<String> coOccurrenceConsumer = coOccurrence -> {
            if (cache.containsKey(coOccurrence)) {
                entitiyToQuestionMapping.putAll(cache.get(coOccurrence));
            } else {
                HashMap<String, String> mapping = new HashMap<>();

                String[] coOccurrenceSplitted = coOccurrence.split(NON_WORD_CHARACTERS_REGEX);
                if (coOccurrenceSplitted.length > 0 && (coOccurrenceSplitted.length > 1 ||
                        (!wordPosMap.getOrDefault(coOccurrenceSplitted[0], "").equals("DT")
                                && !wordPosMap.getOrDefault(coOccurrenceSplitted[0], "").equals("WP")
                                && !wordPosMap.getOrDefault(coOccurrenceSplitted[0], "").equals("EX")
                                && !wordPosMap.getOrDefault(coOccurrenceSplitted[0], "").equals("IN")))) {
                    boolean containsOnlyStopwords = true;
                    for (String word : coOccurrenceSplitted) {
                        if (!Stopwords.isStopword(word)) {
                            containsOnlyStopwords = false;
                        }
                    }
                    if (!containsOnlyStopwords) {
                        List<RatedEntity> ratedResources = new ArrayList<>();
                        List<RatedEntity> ratedOntologies = new ArrayList<>();

                        mapping.putAll(getProperties(coOccurrence));
                        mapping.putAll(getOntologyClass(coOccurrence));

                        String lemmasJoined = joinCapitalizedLemmas(coOccurrenceSplitted, false, true);
                        Consumer<String> addResourceWithLevenshteinRatio = s -> {
                            if (isResource(s)) {
                                ratedResources.add(new RatedEntity(s, coOccurrence, getLevenshteinRatio(s, coOccurrence)));
                            } else {
                                ratedOntologies.add(new RatedEntity(s, coOccurrence, getLevenshteinRatio(s, coOccurrence)));
                            }
                        };
                        if (coOccurrenceSplitted.length > 1 || (!wordPosMap.getOrDefault(coOccurrenceSplitted[0], "").equals("DT") && !wordPosMap.getOrDefault(coOccurrenceSplitted[0], "").startsWith("W"))) {
                            boolean isImportantEntity = true;
                            if (coOccurrenceSplitted.length == 1) {
                                Map<String, String> lemmas = getLemmas(coOccurrenceSplitted[0]);
                                if (lemmas.getOrDefault(coOccurrenceSplitted[0], "").equals("be")
                                        || lemmas.getOrDefault(coOccurrenceSplitted[0], "").equals("the")) {
                                    isImportantEntity = false;
                                }
                            }
                            String coOccurrenceJoinedWithUnderScore = "http://dbpedia.org/resource/" + join("_", coOccurrenceSplitted);
                            if (isImportantEntity && existsAsEntity(coOccurrenceJoinedWithUnderScore)) {
                                mapping.put(coOccurrenceJoinedWithUnderScore, coOccurrence);
                            }
                        }
                        tryDBpediaResourceNamingCombinations(ontologyURIs, coOccurrenceSplitted, lemmasJoined).forEach(addResourceWithLevenshteinRatio);

                        String lemmasJoinedCapitalized = joinCapitalizedLemmas(coOccurrenceSplitted, true, true);
                        tryDBpediaResourceNamingCombinations(ontologyURIs, coOccurrenceSplitted, lemmasJoinedCapitalized).forEach(addResourceWithLevenshteinRatio);

                        String wordsJoined = joinCapitalizedLemmas(coOccurrenceSplitted, false, false);
                        tryDBpediaResourceNamingCombinations(ontologyURIs, coOccurrenceSplitted, wordsJoined).forEach(addResourceWithLevenshteinRatio);

                        String wordsJoinedCapitalized = joinCapitalizedLemmas(coOccurrenceSplitted, true, false);
                        tryDBpediaResourceNamingCombinations(ontologyURIs, coOccurrenceSplitted, wordsJoinedCapitalized).forEach(addResourceWithLevenshteinRatio);
                        searchInDBOIndex(coOccurrence).forEach(addResourceWithLevenshteinRatio);

                        ratedOntologies.sort(Comparator.comparing(RatedEntity::getRating));
                        ratedResources.sort(Comparator.comparing(RatedEntity::getRating));
                        Set<String> resourcesFoundInFullText = findResourcesInFullText(coOccurrence);
                        resourcesFoundInFullText.forEach(s -> mapping.put(s, coOccurrence));
                        if (ratedOntologies.size() > 1) {
                            mapping.put(ratedOntologies.get(0).getUri(), ratedOntologies.get(0).getOrigin());
                        }
                        if (ratedResources.size() > 1) {
                            mapping.put(ratedResources.get(0).getUri(), ratedResources.get(0).getOrigin());
                        }
                        cache.put(coOccurrence, mapping);
                        entitiyToQuestionMapping.putAll(mapping);
                    }
                }
            }
        };
        ForkJoinPool forkJoinPool = new ForkJoinPool(16);
        List<String> finalCoOccurrences = coOccurrences;
        try {
            forkJoinPool.submit(() -> finalCoOccurrences.parallelStream().filter(s -> !s.isEmpty()).forEach(coOccurrenceConsumer)).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Exception while extracting entities from wordgroups of the question!", e);
        }
        return entitiyToQuestionMapping;
    }

    Map<String, String> extractEntitiesUsingSynonyms(String question) {
        question = SemanticAnalysisHelper.removeQuestionWords(question);
        String[] wordsFromQuestion = question.split(NON_WORD_CHARACTERS_REGEX);

        for (String word : wordsFromQuestion) {
            Map<String, String> lemmas = getLemmas(word);
            Set<String> ontologiesFromMapping = new HashSet<>();
            Map<String, Set<String>> ontologyMapping = OntologyMappingProvider.getOntologyMapping();
            if (ontologyMapping != null) {
                ontologiesFromMapping = ontologyMapping.getOrDefault(lemmas.getOrDefault(word, "").toLowerCase(), new HashSet<>());
            }
            if (!ontologiesFromMapping.isEmpty()) {
                ontologiesFromMapping.forEach(s -> this.entitiyToQuestionMappingWithSynonyms.put(s, word));
            }
        }

        return findResourcesBySynonyms(question);
    }

    Set<String> findResourcesInFullText(String s) {
        List<String> questionWords = Arrays.asList("list|give|show|who|when|were|what|why|whose|how|where|which|is|are|did|was|does|a".split("\\|"));
        Set<String> result = new HashSet<>();
        //SPARQLResultSet sparqlResultSet = SPARQLUtilities.executeSPARQLQuery(String.format("select distinct ?s { ?s ?p ?o. ?s <http://www.w3.org/2000/01/rdf-schema#label> ?l. filter(langmatches(lang(?l), 'en')) ?l <bif:contains> \"'%s'\" }", s));
        List<SPARQLResultSet> sparqlResultSets = SPARQLUtilities.executeSPARQLQuery(String.format(SPARQLUtilities.FULLTEXT_SEARCH_SPARQL, s.replace("'", "\\\\'")));
        List<String> resultSet = new ArrayList<>();
        sparqlResultSets.forEach(sparqlResultSet -> resultSet.addAll(sparqlResultSet.getResultSet()));
        resultSet.parallelStream().filter(s1 -> s1.startsWith("http://")).forEach(uri -> {
            //uri = SPARQLUtilities.getRedirect(uri);
            String[] split;
            if (uri.startsWith("http://dbpedia.org/resource/")) {
                split = uri.split("http://dbpedia.org/resource/");
            } else {
                split = uri.split("/");
            }
            String resourceName = split[split.length - 1];
            if (!questionWords.contains(resourceName.toLowerCase())) {
                double levenshteinRatio = Utilities.getLevenshteinRatio(s.toLowerCase(), resourceName.replace("_", " ")
                        .replace("(", " ")
                        .replace(")", " ")
                        .replaceAll("\\s+", " ")
                        .trim()
                        .toLowerCase());
                if (levenshteinRatio < 0.2) {
                    result.add(uri);
                }
            }
        });
        return result;
    }

    private boolean existsAsEntity(String s) {
        List<SPARQLResultSet> sparqlResultSets = SPARQLUtilities.executeSPARQLQuery(String.format("ASK { VALUES (?r) {(<%s>)} {?r ?p ?o} UNION {?s ?r ?o} UNION {?s ?p ?r} }", s));
        return Boolean.valueOf(sparqlResultSets.get(0).getResultSet().get(0));
    }

    private Map<String, String> findResourcesBySynonyms(String question) {
        Map<String, String> rdfResources = new HashMap<>();

        List<String> coOccurrences = getNeighborCoOccurrencePermutations(Arrays.asList(question.split(NON_WORD_CHARACTERS_REGEX)));
        coOccurrences.parallelStream().forEach(coOccurrence -> {

            Set<String> puttyEntities = Sets.newHashSet(patty_relations.search(coOccurrence));
            puttyEntities.forEach(s -> rdfResources.put("http://dbpedia.org/ontology/" + s, coOccurrence));
        });

        WordNetWrapper wordNet = new WordNetWrapper();
        Map<String, String> synonyms = wordNet.lookUpWords(question);
        synonyms.forEach((synonym, origin) -> {
            if (synonym.contains(" ")) {
                String[] words = synonym.split(NON_WORD_CHARACTERS_REGEX);
                String wordsJoined = joinCapitalizedLemmas(words, false, true);
                tryDBpediaResourceNamingCombinations(ontologyURIs, words, wordsJoined).forEach(s -> rdfResources.put(s, origin));
                tryDBpediaResourceNamingCombinations(ontologyURIs, words, joinCapitalizedLemmas(words, true, true)).forEach(s -> rdfResources.put(s, origin));
                searchInDBOIndex(Strings.join(words, " ")).forEach(s -> rdfResources.put(s, origin));
                Arrays.asList(words).forEach(s -> searchInDBOIndex(s).forEach(s1 -> rdfResources.put(s1, origin)));
            } else {
                searchInDBOIndex(synonym).forEach(s -> rdfResources.put(s, origin));
            }
        });
        return rdfResources;
    }

    private boolean isResource(String s) {
        return s.startsWith("http://dbpedia.org/resource/");
    }

    private boolean isOntology(String s) {
        return s.startsWith("http://dbpedia.org/ontology/") || s.startsWith("http://www.w3.org/1999/02/22-rdf-syntax-ns#type") || s.startsWith("http://dbpedia.org/datatype/");
    }

    private Set<String> searchInDBOIndex(String coOccurrence) {
        DBOIndex dboIndex = new DBOIndex();
        //The DBOIndex Class throws a NullPointerException when you search for a number
        if (StringUtils.isNumeric(coOccurrence)) {
            return new HashSet<>();
        } else if (coOccurrence.length() < 2) {
            //Don't use words like 'a'
            return new HashSet<>();
        } else {
            List<String> search = dboIndex.search(coOccurrence);
            Set<String> resultsInDBOIndex = search.parallelStream()
                    .filter(s -> {
                        String[] split = s.split("/");
                        String baseResourceName = split[split.length - 1];
                        double ratio = getLevenshteinRatio(coOccurrence, baseResourceName);
                        return ratio <= 0.5;
                    })
                    .collect(Collectors.toSet());

            IndexDBO_classes indexDBO_classes = new IndexDBO_classes();
            List<String> indexDBO_classesSearch = indexDBO_classes.search(coOccurrence);
            Set<String> resultsInDBOIndexClass = getResultsInDBOIndexFilteredByRatio(coOccurrence, indexDBO_classesSearch);
            List<String> indexDBO_propertySearch = new ArrayList<>();
            if (!Stopwords.isStopword(coOccurrence)) {
                IndexDBO_properties indexDBO_properties = new IndexDBO_properties();
                indexDBO_propertySearch = indexDBO_properties.search(coOccurrence);
                try {
                    indexDBO_properties.close();
                } catch (NullPointerException e) {
                    log.error("NullPointerException when trying to close IndexDBO_properties!", e);
                }
            }
            Set<String> resultsInDBOIndexProperty = getResultsInDBOIndexFilteredByRatio(coOccurrence, indexDBO_propertySearch);

            resultsInDBOIndex.addAll(resultsInDBOIndexClass);
            resultsInDBOIndex.addAll(resultsInDBOIndexProperty);
            return resultsInDBOIndex;
        }
    }

    private Set<String> getResultsInDBOIndexFilteredByRatio(String coOccurrence, List<String> indexDBO_classesSearch) {
        return indexDBO_classesSearch.parallelStream()
                .filter(s -> {
                    String[] split = s.split("/");
                    String baseResourceName = split[split.length - 1];
                    double ratio = Utilities.getLevenshteinRatio(coOccurrence, baseResourceName);
                    //TODO instead of using string similarity use the shortest one (e.g. Television instead of TelevisionShow) if it exists
                    return ratio < 0.5;
                })
                .collect(Collectors.toSet());
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

    public Set<String> generateQueries(Map<String, QueryTemplateMapping> mappings, boolean useSynonyms) {
        return generateQueries(mappings, null, useSynonyms);
    }

    private String joinCapitalizedLemmas(String[] strings, boolean capitalizeFirstLetter, boolean useLemma) {
        final String[] result = {""};
        List<String> list = Arrays.asList(strings);
        list = list.parallelStream().filter(s -> !s.isEmpty()).collect(Collectors.toList());
        if (useLemma) {
            list.forEach(s -> result[0] += StringUtils.capitalize(new Sentence(s).lemma(0)));
        } else {
            list.forEach(s -> result[0] += StringUtils.capitalize(s));
        }
        //The first letter is lowercase sometimes
        if (capitalizeFirstLetter) {
            return StringUtils.capitalize(result[0]);
        } else {
            return StringUtils.uncapitalize(result[0]);
        }
    }

    Set<String> fillPatterns(Map<String, String> rdfResources, List<String> suitableMappings) {
        Set<String> sparqlQueries = new HashSet<>();
        Set<String> baseResources = rdfResources.keySet().parallelStream().map(SPARQLUtilities::getRedirect).collect(Collectors.toSet());

        for (String pattern : suitableMappings) {
            List<String> classResources = new ArrayList<>();
            List<String> propertyResources = new ArrayList<>();
            for (String resource : baseResources) {
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

    private List<String> getSuitableMappings(Map<String, QueryTemplateMapping> mappings, int queryType, String graph) {
        List<QueryTemplateMapping> templatesForGraph = new ArrayList<>();
        if (graph == null) {
            templatesForGraph = new ArrayList<>(mappings.values());
        } else {
            QueryTemplateMapping mapping = mappings.get(graph);
            if (mapping != null) {
                templatesForGraph.add(mapping);
            }
        }
        List<String> result = new ArrayList<>();
        if (queryType == SPARQLUtilities.SELECT_SUPERLATIVE_ASC_QUERY) {
            result = templatesForGraph.parallelStream()
                    .map(QueryTemplateMapping::getSelectSuperlativeAscTemplate)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream).collect(Collectors.toList());
        } else if (queryType == SPARQLUtilities.SELECT_SUPERLATIVE_DESC_QUERY) {
            result = templatesForGraph.parallelStream()
                    .map(QueryTemplateMapping::getSelectSuperlativeDescTemplate)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream).collect(Collectors.toList());
        } else if (queryType == SPARQLUtilities.SELECT_COUNT_QUERY) {
            result = templatesForGraph.parallelStream()
                    .map(QueryTemplateMapping::getSelectCountTemplates)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream).collect(Collectors.toList());
        } else if (queryType == SPARQLUtilities.ASK_QUERY) {
            result = templatesForGraph.parallelStream()
                    .map(QueryTemplateMapping::getAskTemplates)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream).collect(Collectors.toList());

            //templatesForGraph.forEach(queryTemplateMapping -> result.addAll(queryTemplateMapping.getAskTemplates()));
        }

        if (queryType == SPARQLUtilities.SELECT_QUERY) {
            result = templatesForGraph.parallelStream()
                    .map(QueryTemplateMapping::getSelectTemplates)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream).collect(Collectors.toList());

            //templatesForGraph.forEach(queryTemplateMapping -> result.addAll(queryTemplateMapping.getSelectTemplates()));
        } else {
            result.addAll(templatesForGraph.parallelStream()
                    .map(QueryTemplateMapping::getAskTemplates)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream).collect(Collectors.toList()));
            result.addAll(templatesForGraph.parallelStream()
                    .map(QueryTemplateMapping::getSelectTemplates)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream).collect(Collectors.toList()));

            // templatesForGraph.forEach(queryTemplateMapping -> result.addAll(queryTemplateMapping.getAskTemplates()));
            // templatesForGraph.forEach(queryTemplateMapping -> result.addAll(queryTemplateMapping.getSelectTemplates()));
        }
        return result;
    }

    Map<String, String> getProperties(String words) {
        if (words.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, String> result = new HashMap<>();
        final String relevantPos = "JJ.*|NN.*|VB.*";
        Annotation document = new Annotation(words);
        StanfordCoreNLP pipeline = StanfordPipelineProvider.getSingletonPipelineInstance();
        pipeline.annotate(document);
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
                String pos = token.get(PartOfSpeechAnnotation.class);
                String lemma = token.get(LemmaAnnotation.class);
                if (pos.matches(relevantPos) && !Stopwords.isStopword(lemma)) {
                    Set<String> matchingLemmaProperties = properties.parallelStream()
                            .filter(property -> property.equalsIgnoreCase(String.format("http://dbpedia.org/property/%s", lemma)))
                            .collect(Collectors.toSet());
                    matchingLemmaProperties.forEach(s -> result.put(s, token.value()));
                    Set<String> matchingProperties = properties.parallelStream()
                            .filter(property -> property.equalsIgnoreCase(String.format("http://dbpedia.org/property/%s", words)))
                            .collect(Collectors.toSet());
                    matchingProperties.forEach(s -> result.put(s, token.value()));
                }
            }
        }

        if (this.queryType == SPARQLUtilities.SELECT_COUNT_QUERY || this.queryType == SPARQLUtilities.SELECT_QUERY) {
            Map<String, String> pos = SemanticAnalysisHelper.getPOS(words);
            Set<String> nouns = pos.keySet().parallelStream().filter(s -> pos.get(s).startsWith("NN")).collect(Collectors.toSet());
            nouns.parallelStream().forEach(s -> {
                String propertyCandidate = "http://dbpedia.org/property/" + s + "Total";
                if (properties.contains(propertyCandidate)) {
                    result.put(propertyCandidate, s);
                }
            });
        }

        List<String> coOccurrences = getNeighborCoOccurrencePermutations(Arrays.asList(words.split(NON_WORD_CHARACTERS_REGEX)));
        coOccurrences.parallelStream().forEach(coOccurrence -> {
            if (!Stopwords.isStopword(coOccurrence)) {
                String propertyCandidate = "http://dbpedia.org/property/" + joinCapitalizedLemmas(coOccurrence.split(NON_WORD_CHARACTERS_REGEX), false, false);
                if (properties.contains(propertyCandidate)) {
                    result.put(propertyCandidate, coOccurrence);
                }
            }
        });
        return result;
    }

    Map<String, String> getOntologyClass(String words) {
        if (words.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, String> result = new HashMap<>();
        final String relevantPos = "JJ.*|NN.*|VB.*";
        Annotation document = new Annotation(words);
        StanfordCoreNLP pipeline = StanfordPipelineProvider.getSingletonPipelineInstance();
        pipeline.annotate(document);
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
                String pos = token.get(PartOfSpeechAnnotation.class);
                String lemma = token.get(LemmaAnnotation.class);
                if (pos.matches(relevantPos)) {
                    Set<String> matchingClasses = ontologyNodes.parallelStream()
                            .filter(rdfNode -> rdfNode.toString().equalsIgnoreCase(String.format("http://dbpedia.org/ontology/%s", lemma)))
                            .map(RDFNode::toString)
                            .collect(Collectors.toSet());
                    matchingClasses.forEach(s -> result.put(s, token.value()));
                }
            }
        }

        if (this.queryType == SPARQLUtilities.SELECT_COUNT_QUERY) {
            Map<String, String> pos = SemanticAnalysisHelper.getPOS(words);
            Set<String> nouns = pos.keySet().parallelStream().filter(s -> pos.get(s).startsWith("NN")).collect(Collectors.toSet());
            nouns.parallelStream().forEach(s -> {
                String propertyCandidateLowercase = "http://dbpedia.org/ontology/" + s + "Total";
                Set<String> ontologyClassCandidates = ontologyNodes.parallelStream()
                        .filter(rdfNode -> rdfNode.toString().equals(propertyCandidateLowercase))
                        .map(RDFNode::toString)
                        .collect(Collectors.toSet());

                String propertyCandidateUppercase = "http://dbpedia.org/ontology/" + StringUtils.capitalize(s) + "Total";
                ontologyClassCandidates.addAll(ontologyNodes.parallelStream()
                        .filter(rdfNode -> rdfNode.toString().equals(propertyCandidateUppercase))
                        .map(RDFNode::toString)
                        .collect(Collectors.toSet()));
                ontologyClassCandidates.forEach(matchingClass -> result.put(matchingClass, s));
            });
        }

        String[] wordsSplitted = words.split(NON_WORD_CHARACTERS_REGEX);
        String lowercaseCandidate = "http://dbpedia.org/ontology/" + joinCapitalizedLemmas(wordsSplitted, false, false);
        if (ontologyNodes.parallelStream().anyMatch(rdfNode -> rdfNode.toString().equals(lowercaseCandidate))) {
            result.put(lowercaseCandidate, words);
        }
        String uppercaseCandidate = "http://dbpedia.org/ontology/" + joinCapitalizedLemmas(wordsSplitted, true, false);
        if (ontologyNodes.parallelStream().anyMatch(rdfNode -> rdfNode.toString().equals(uppercaseCandidate))) {
            result.put(uppercaseCandidate, words);
        }

        String lowercaseLemmaCandidate = "http://dbpedia.org/ontology/" + joinCapitalizedLemmas(wordsSplitted, false, true);
        if (ontologyNodes.parallelStream().anyMatch(rdfNode -> rdfNode.toString().equals(lowercaseLemmaCandidate))) {
            result.put(lowercaseLemmaCandidate, words);
        }
        String uppercaseLemmaCandidate = "http://dbpedia.org/ontology/" + joinCapitalizedLemmas(wordsSplitted, true, true);
        if (ontologyNodes.parallelStream().anyMatch(rdfNode -> rdfNode.toString().equals(uppercaseLemmaCandidate))) {
            result.put(uppercaseLemmaCandidate, words);
        }

        return result;
    }

    private boolean resourceStartsLowercase(String rdfResource) {
        return rdfResource.startsWith("http://dbpedia.org/property/")
                || rdfResource.startsWith("http://www.w3.org/1999/02/22-rdf-syntax-ns#")
                || (rdfResource.contains("/") && rdfResource.charAt(rdfResource.length() - 1) != '/' && Character.isLowerCase(rdfResource.substring(rdfResource.lastIndexOf('/') + 1).charAt(0)));
    }

    public Map<String, String> getEntitiyToQuestionMapping() {
        return entitiyToQuestionMapping;
    }
}

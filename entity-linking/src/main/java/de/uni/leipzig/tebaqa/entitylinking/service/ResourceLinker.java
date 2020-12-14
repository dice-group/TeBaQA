package de.uni.leipzig.tebaqa.entitylinking.service;

import de.uni.leipzig.tebaqa.entitylinking.nlp.StopWordsUtil;
import de.uni.leipzig.tebaqa.entitylinking.util.PropertyUtil;
import de.uni.leipzig.tebaqa.tebaqacommons.elasticsearch.SearchService;
import de.uni.leipzig.tebaqa.tebaqacommons.model.*;
import de.uni.leipzig.tebaqa.tebaqacommons.nlp.Lang;
import de.uni.leipzig.tebaqa.tebaqacommons.nlp.SemanticAnalysisHelper;
import de.uni.leipzig.tebaqa.tebaqacommons.util.TextUtilities;
import edu.stanford.nlp.semgraph.SemanticGraph;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ResourceLinker {

    private static final Logger LOGGER = Logger.getLogger(ResourceLinker.class);

    private String question;
    private final Lang language;

    private Set<String> coOccurrences;
    private Set<String> propertyUris;
    private Set<EntityCandidate> entityCandidates;
    private Set<PropertyCandidate> propertyCandidates;
    private Set<ClassCandidate> classCandidates;
    private Set<EntityCandidate> literalCandidates;

    private final SemanticAnalysisHelper semanticAnalysisHelper;
    private final SearchService searchService;
    private final DisambiguationService disambiguationService;

    public ResourceLinker(String question, Lang language) throws IOException {
        this.question = question;
        this.language = language;
        this.coOccurrences = new HashSet<>();
        this.propertyUris = new HashSet<>();
        this.entityCandidates = new HashSet<>();
        this.propertyCandidates = new HashSet<>();
        this.classCandidates = new HashSet<>();
//        this.semanticAnalysisHelper = language.getSemanticAnalysisHelper();
        this.semanticAnalysisHelper = new SemanticAnalysisHelper(new RestServiceConfiguration("http", "tebaqa.cs.upb.de", "8085"), language);
        this.searchService = new SearchService(PropertyUtil.getElasticSearchConnectionProperties());
        this.disambiguationService = new DisambiguationService(this.searchService);
    }

    public String getQuestion() {
        return question;
    }

    public Lang getLanguage() {
        return language;
    }

    public Set<String> getCoOccurrences() {
        return coOccurrences;
    }

    public void setCoOccurrences(Set<String> coOccurrences) {
        this.coOccurrences = coOccurrences;
    }

    public Set<String> getPropertyUris() {
        return propertyUris;
    }

    public Set<EntityCandidate> getEntityCandidates() {
        return entityCandidates;
    }

    public Set<PropertyCandidate> getPropertyCandidates() {
        return propertyCandidates;
    }

    public Set<ClassCandidate> getClassCandidates() {
        return classCandidates;
    }

    public Set<EntityCandidate> getLiteralCandidates() {
        return literalCandidates;
    }

    public void linkEntities() {

        // Remove unnecessary
        question = this.cleanQuestion(question);
        SemanticGraph semanticGraph = semanticAnalysisHelper.extractDependencyGraph(question);
        String[] wordsFromQuestion = question.replaceAll("[\\-.?¿!,;]", "").split("\\s+");

        // Prepare co-occurences
        List<String> coOccurrenceList = TextUtilities.getNeighborCoOccurrencePermutations(Arrays.asList(wordsFromQuestion));
        coOccurrenceList.sort((s1, s2) -> -(s1.length() - s2.length())); // sort ascending based on length

        List<String> filteredCoOccurrences = new ArrayList<>();
        if (false) { // TODO if(semanticGraph != null) but this removes some important co-occurrences
            for (String coOccurrence : coOccurrenceList) {
                if (!StopWordsUtil.containsOnlyStopwords(coOccurrence, language) && TextUtilities.isDependent(coOccurrence, semanticGraph))
                    filteredCoOccurrences.add(coOccurrence);
            }
        } else {
            for (String coOccurrence : coOccurrenceList) {
                if (!StopWordsUtil.containsOnlyStopwords(coOccurrence, language))
                    filteredCoOccurrences.add(coOccurrence);
            }
        }
        coOccurrenceList = filteredCoOccurrences;


        coOccurrenceList.sort((s1, s2) -> -(s1.length() - s2.length()));
        coOccurrenceList = coOccurrenceList.stream().filter(s -> s.split("\\s+").length < 7).collect(Collectors.toList());
        this.coOccurrences.addAll(coOccurrenceList);

        HashMap<String, Set<EntityCandidate>> ambiguousEntityCandidates = new HashMap<>();
        for (String coOccurrence : coOccurrenceList) {

            // 1. Link entities
            Set<EntityCandidate> matchedEntities = searchService.searchEntities(coOccurrence);
            if (matchedEntities.size() > 0 && matchedEntities.size() <= 20) {
                entityCandidates.addAll(matchedEntities);
            } else if (matchedEntities.size() > 20) {
                ambiguousEntityCandidates.put(coOccurrence, matchedEntities);
            }
            //search for Countries
            Set<EntityCandidate> countryEntities = searchService.searchEntitiesOfType(coOccurrence, "http://dbpedia.org/ontology/Country");
            if (countryEntities.size() < 100) {
                entityCandidates.addAll(countryEntities);
            }

            // 2. Link classes
            String searchTerm = coOccurrence;
            if (!coOccurrence.contains(" ")) {
                String stripped = coOccurrence.replace("'s", "");
                Map<String, String> lemmas = semanticAnalysisHelper.getLemmas(stripped);
                String lemma = lemmas.get(stripped);
                if (lemma != null) {
                    searchTerm = lemma;
                }
            }
            classCandidates.addAll(searchService.searchClasses(searchTerm));

            // 3. Link properties
            propertyCandidates.addAll(searchService.searchProperties(coOccurrence));

            // TODO ? 4. literal linking
//            literalCandidates.addAll(index.searchLiteral(coOccurrence, 100));
//            literalCandidates.forEach(lc -> propertyUris.addAll(((EntityCandidate) lc).getConnectedPropertiesObject()));
        }

        Set<EntityCandidate> disambiguatedEntities = this.disambiguationService.disambiguateEntities(ambiguousEntityCandidates, this.entityCandidates, this.propertyCandidates, Optional.empty());
        entityCandidates.addAll(disambiguatedEntities);

        this.removeDuplicates();

        LOGGER.info("EntityExtraction finished");
    }

    private String cleanQuestion(String question) {
        question = semanticAnalysisHelper.removeQuestionWords(question);
        question = question.replace("(", "");
        question = question.replace(")", "");
        return question;
    }

    private void removeDuplicates() {
        Map<String, List<EntityCandidate>> entitiesByUri = this.entityCandidates.stream().collect(Collectors.groupingBy(EntityCandidate::getUri));
        Set<EntityCandidate> uniqueEntityCandidates = entitiesByUri.keySet().stream().map(
                uri -> {
                    double bestScore = entitiesByUri.get(uri).stream().mapToDouble(ResourceCandidate::getDistanceScore).min().getAsDouble();
                    EntityCandidate bestForThisUri = entitiesByUri.get(uri).stream()
                            .filter(entityCandidate -> entityCandidate.getDistanceScore() == bestScore)
                            .max(Comparator.comparingInt(value -> value.getCoOccurrence().length())).get();
                    return bestForThisUri;
                })
                .collect(Collectors.toSet());
        this.entityCandidates.clear();
        this.entityCandidates.addAll(uniqueEntityCandidates);
        this.entityCandidates.forEach(entityCandidate -> {
            propertyUris.addAll(entityCandidate.getConnectedPropertiesSubject());
            propertyUris.addAll(entityCandidate.getConnectedPropertiesObject());
        });

        Map<String, List<PropertyCandidate>> propertiesByUri = this.propertyCandidates.stream().collect(Collectors.groupingBy(PropertyCandidate::getUri));
        Set<PropertyCandidate> uniquePropertyCandidates = propertiesByUri.keySet().stream().map(
                uri -> {
                    double bestScore = propertiesByUri.get(uri).stream().mapToDouble(ResourceCandidate::getDistanceScore).min().getAsDouble();
                    PropertyCandidate bestForThisUri = propertiesByUri.get(uri).stream()
                            .filter(candidate -> candidate.getDistanceScore() == bestScore)
                            .max(Comparator.comparingInt(value -> value.getCoOccurrence().length())).get();
                    return bestForThisUri;
                })
                .collect(Collectors.toSet());
        this.propertyCandidates.clear();
        this.propertyCandidates.addAll(uniquePropertyCandidates);

        Map<String, List<ClassCandidate>> classesByUri = this.classCandidates.stream().collect(Collectors.groupingBy(ClassCandidate::getUri));
        Set<ClassCandidate> uniqueClassCandidates = classesByUri.keySet().stream().map(
                uri -> {
                    double bestScore = classesByUri.get(uri).stream().mapToDouble(ResourceCandidate::getDistanceScore).min().getAsDouble();
                    ClassCandidate bestForThisUri = classesByUri.get(uri).stream()
                            .filter(candidate -> candidate.getDistanceScore() == bestScore)
                            .max(Comparator.comparingInt(value -> value.getCoOccurrence().length())).get();
                    return bestForThisUri;
                })
                .collect(Collectors.toSet());
        this.classCandidates.clear();
        this.classCandidates.addAll(uniqueClassCandidates);
    }

    public void printInfos() {
        System.out.println("Entities");
        entityCandidates.forEach(ent -> System.out.println(ent.getCoOccurrence() + "->(" + ent.getUri() + ";" + Collections.singletonList(ent.getResourceLabels()).get(0) + ")"));
        System.out.println("Properties");
        propertyCandidates.forEach(ent -> System.out.println(ent.getCoOccurrence() + "->(" + ent.getUri() + ";" + Collections.singletonList(ent.getResourceLabels()).get(0) + ")"));
        System.out.println("Class");
        classCandidates.forEach(ent -> System.out.println(ent.getCoOccurrence() + "->(" + ent.getUri() + ";" + Collections.singletonList(ent.getResourceLabels()).get(0) + ")"));
    }


    public static void main(String[] args) throws IOException {
        ResourceLinker linker = new ResourceLinker("What is the height of the Eiffel Tower?", Lang.EN);
        linker.linkEntities();
        linker.printInfos();
    }

}

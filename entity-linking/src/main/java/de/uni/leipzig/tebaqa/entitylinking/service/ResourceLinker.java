package de.uni.leipzig.tebaqa.entitylinking.service;

import de.uni.leipzig.tebaqa.entitylinking.nlp.StopWordsUtil;
import de.uni.leipzig.tebaqa.entitylinking.util.PropertyUtil;
import de.uni.leipzig.tebaqa.tebaqacommons.elasticsearch.SearchService;
import de.uni.leipzig.tebaqa.tebaqacommons.model.ClassCandidate;
import de.uni.leipzig.tebaqa.tebaqacommons.model.EntityCandidate;
import de.uni.leipzig.tebaqa.tebaqacommons.model.PropertyCandidate;
import de.uni.leipzig.tebaqa.tebaqacommons.nlp.ISemanticAnalysisHelper;
import de.uni.leipzig.tebaqa.tebaqacommons.nlp.Lang;
import de.uni.leipzig.tebaqa.tebaqacommons.util.TextUtilities;
import edu.stanford.nlp.semgraph.SemanticGraph;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.*;

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

    private final ISemanticAnalysisHelper semanticAnalysisHelper;
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
        this.semanticAnalysisHelper = language.getSemanticAnalysisHelper();
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

        List<String> dependentCoOccurrences = new ArrayList<>();
        if (semanticGraph != null) {
            for (String coOccurrence : coOccurrenceList) {
                if (!StopWordsUtil.containsOnlyStopwords(coOccurrence, Lang.EN) && TextUtilities.isDependent(coOccurrence, semanticGraph))
                    dependentCoOccurrences.add(coOccurrence);
            }
            coOccurrenceList = dependentCoOccurrences;
        }
        coOccurrenceList.sort((s1, s2) -> -(s1.length() - s2.length()));
        this.coOccurrences.addAll(coOccurrenceList);

        HashMap<String, Set<EntityCandidate>> ambiguousEntityCandidates = new HashMap<>();
        for (String coOccurrence : coOccurrenceList) {

            // 1. Link entities
            Set<EntityCandidate> matchedEntities = searchService.searchEntities(coOccurrence);
            if (matchedEntities.size() > 0 && matchedEntities.size() <= 20) {
                entityCandidates.addAll(matchedEntities);
                matchedEntities.forEach(cand -> {
                    propertyUris.addAll(cand.getConnectedPropertiesSubject());
                    propertyUris.addAll(cand.getConnectedPropertiesObject());
                });
            } else if (matchedEntities.size() > 20) {
                ambiguousEntityCandidates.put(coOccurrence, matchedEntities);
            }
            //search for Countries
            Set<EntityCandidate> countryEntities = searchService.searchEntitiesOfType(coOccurrence, "http://dbpedia.org/ontology/Country");
            if (countryEntities.size() < 100) {
                countryEntities.forEach(cand -> {
                    propertyUris.addAll(cand.getConnectedPropertiesSubject());
                    propertyUris.addAll(cand.getConnectedPropertiesObject());
                });
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
        disambiguatedEntities.forEach(cand -> propertyUris.addAll(cand.getConnectedPropertiesSubject()));
        disambiguatedEntities.forEach(cand -> propertyUris.addAll(cand.getConnectedPropertiesObject()));

        LOGGER.info("EntityExtraction finished");
    }

    private String cleanQuestion(String question) {
        question = semanticAnalysisHelper.removeQuestionWords(question);
        question = question.replace("(", "");
        question = question.replace(")", "");
        return question;
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

package de.uni.leipzig.tebaqa.entitylinking.service;

import de.uni.leipzig.tebaqa.tebaqacommons.model.ClassCandidate;
import de.uni.leipzig.tebaqa.tebaqacommons.model.EntityCandidate;
import de.uni.leipzig.tebaqa.tebaqacommons.model.PropertyCandidate;
import de.uni.leipzig.tebaqa.tebaqacommons.model.ResourceCandidate;
import de.uni.leipzig.tebaqa.tebaqacommons.util.TextUtilities;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class SearchService {

    private static final Logger LOGGER = Logger.getLogger(SearchService.class);

    // TODO Externalize?
    private static final double MIN_SCORE_NORMAL = 0.32;
    private static final double MIN_SCORE_WITH_NUMBERS = 0.05;
    private final ElasticSearchEntityIndex index;

    // TODO implement caching here

    public SearchService() throws IOException {
        this.index = new ElasticSearchEntityIndex();
    }

    public SearchService(ElasticSearchEntityIndex index) {
        this.index = index;
    }


    public Set<EntityCandidate> searchEntities(String coOccurrence) {
        return this.searchEntities(coOccurrence, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public Set<EntityCandidate> searchEntitiesOfType(String coOccurrence, String typeFilter) {
        return this.searchEntities(coOccurrence, Optional.of(typeFilter), Optional.empty(), Optional.empty());
    }

    public Set<EntityCandidate> searchEntitiesWithConnectedEntity(String coOccurrence, String connectedEntity) {
        return this.searchEntities(coOccurrence, Optional.empty(), Optional.of(connectedEntity), Optional.empty());
    }

    public Set<EntityCandidate> searchEntitiesWithConnectedProperty(String coOccurrence, String connectedProperty) {
        return this.searchEntities(coOccurrence, Optional.empty(), Optional.empty(), Optional.of(connectedProperty));
    }

    private Set<EntityCandidate> searchEntities(String coOccurrence, Optional<String> typeFilter, Optional<String> connectedEntity, Optional<String> connectedProperty) {
        Set<EntityCandidate> foundEntities;

        try {
            foundEntities = index.searchEntity(coOccurrence, connectedEntity, connectedProperty, typeFilter);
            foundEntities = getBestCandidates(coOccurrence, foundEntities);

        } catch (IOException e) {
            LOGGER.info("Searching entities for co-occurrence: " + coOccurrence);
            LOGGER.error("Failed to search entities: " + e.getMessage());
            foundEntities = Collections.emptySet();
        }
        return foundEntities;
    }


    public Set<PropertyCandidate> searchProperties(String coOccurrence, boolean searchSynonyms) {
        Set<PropertyCandidate> propertyCandidates;

        try {
            propertyCandidates = index.searchProperty(coOccurrence, searchSynonyms);
            propertyCandidates = getBestCandidates(coOccurrence, propertyCandidates);

        } catch (IOException e) {
            LOGGER.info("Searching properties for co-occurrence: " + coOccurrence);
            LOGGER.error("Failed to search properties: " + e.getMessage());
            propertyCandidates = Collections.emptySet();
        }
        return propertyCandidates;
    }

    public Set<PropertyCandidate> searchProperties(String coOccurrence) {
        return this.searchProperties(coOccurrence, true);
    }

    public Set<ClassCandidate> searchClasses(String coOccurrence) {
        Set<ClassCandidate> classCandidates;
        try {
            classCandidates = index.searchClass(coOccurrence);
            classCandidates = getBestCandidates(coOccurrence, classCandidates);

        } catch (IOException e) {
            LOGGER.info("Searching classes for co-occurrence: " + coOccurrence);
            LOGGER.error("Failed to search classes: " + e.getMessage());
            classCandidates = Collections.emptySet();
        }

        return classCandidates;
    }


//    public Set<ResourceCandidate> getbestResourcesByLevenstheinRatio(String coOccurrence, String type, boolean searchSynonyms, String typeFilter) {
//        Set<ResourceCandidate> foundResources;
//        if (type.equals("entity") && typeFilter != null)
//            foundResources = index.searchEntity(coOccurrence, Optional.empty(), Optional.empty(), Optional.of(typeFilter));
//        else if (type.equals("entity"))
//            foundResources = index.searchEntity(coOccurrence, Optional.empty(), Optional.empty(), Optional.empty());
//
//        else if (type.equals("property"))
//            foundResources = index.searchResource(coOccurrence, "property", searchSynonyms);
//        else {
//            if (!coOccurrence.contains(" ")) {
//                Map<String, String> lemmas = semanticAnalysisHelper.getLemmas(coOccurrence.replace("'s", ""));
//                String lem = lemmas.get(coOccurrence.replace("'s", ""));
//                if (lem != null) coOccurrence = lem;
//            }
//            foundResources = index.searchResource(coOccurrence, "class", false);
//        }
//        Set<ResourceCandidate> bestResourcesByLevenstheinRatio = getbestResourcesByLevenstheinRatio(coOccurrence, foundResources, type, searchSynonyms);
//        //bestResourcesByLevenstheinRatio.forEach(c->c.setGroup(group));
//        //if(foundResources.size()==100)return foundResources;
//        return bestResourcesByLevenstheinRatio;
//
//    }

    public <T> Set<T> getBestCandidates(String coOccurrence, Set<? extends ResourceCandidate> foundResources) {
        Set<ResourceCandidate> bestCandidates = new HashSet<>();
        for (ResourceCandidate resource : foundResources) {
            String bestMatchedLabel = resource.getBestLabel();
            double ratio = resource.getLevenshteinDistanceScore();

            // Add found entity to final result if it's levenstein score is below threshold (i.e. a good match)
            if (ratio <= getLevensteinThreshold(coOccurrence, bestMatchedLabel)) {
                double numberOfWords = (TextUtilities.countWords(coOccurrence));
                double relFactor = (numberOfWords - (2 * ratio * numberOfWords));
                resource.setRelatednessFactor(relFactor);
                bestCandidates.add(resource);
            }
        }
//        if (clazz.equalsIgnoreCase(EntityCandidate.class.toString()) && bestCandidates.size() == 0 && foundResources.size() == 100)
//            return (Set<T>) foundResources;

        return (Set<T>) bestCandidates;
    }


//    private Set<IResourceCandidate> getBestEntityCandidates(String coOccurrence, Set<IResourceCandidate> foundResources, String type, boolean syn) {
//        Set<IResourceCandidate> bestCandidates = new HashSet<>();
////        double minScore = 0.25;
//        for (IResourceCandidate resource : foundResources) {
//            double ratio = 1;
//            String bestMatchedLabel = null;
//            for (String label : resource.getResourceLabels()) {
//                //double tempRatio = Utilities.getLevenshteinRatio(coOccurrence, label);
//                double tempRatio;
//                if (resource instanceof PropertyCandidate && !syn)
//                    tempRatio = calculateAverageLevensteinRatioByWord(coOccurrence, label);
//                else tempRatio = getLevenshteinRatio(coOccurrence, label);
//                if (tempRatio < ratio) {
//                    ratio = tempRatio;
//                    bestMatchedLabel = label;
//                    double numberOfWords = (TextUtilities.countWords(coOccurrence));
//                    double relFactor = (numberOfWords - (2 * ratio * numberOfWords));
//                    //double relFactor = 1- ratio;
//                    resource.setRelatednessFactor(relFactor);
//                }
//            }
//            if (ratio <= getLevensteinThreshold(coOccurrence, bestMatchedLabel)) {
//                resource.setCoOccurrence(coOccurrence);
//                //minScore = ratio;
//                resource.setLevensteinScore(ratio);
//                //bestCandidates.clear();
//                bestCandidates.add(resource);
//            }
//        }
//        if (bestCandidates.size() == 0 && foundResources.size() == 100 && type.equals("entity"))
//            return foundResources;
//        return bestCandidates;
//    }

    private static double getLevensteinThreshold(String coOccurrence, String matchedResource) {
        // If the co-occurence contains a number, then there should be high similarity. This helps in reducing matched
        // entities in cases like Gleis 1 or LSA 460 where there is a high chance of getting many matches.
        if (matchedResource != null && (coOccurrence.matches(".*\\d+.*") || matchedResource.matches(".*\\d+.*"))) {
            return MIN_SCORE_WITH_NUMBERS;
        }

        return MIN_SCORE_NORMAL;
    }


    public static void main(String[] args) throws IOException {
//        SearchService s = new SearchService();
//        Set<EntityCandidate> candidates = s.searchEntities("the Eiffel Tower");
//        candidates.forEach(ent -> System.out.println(ent.getCoOccurrence() + "->(" + ent.getUri() + ";" + Collections.singletonList(ent.getResourceLabels()).get(0) + ")"));
//        System.out.println(SearchService.getLevenshteinRatio("the height", "The Flight"));

    }
}

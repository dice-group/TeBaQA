package de.uni.leipzig.tebaqa.tebaqacommons.elasticsearch;

import de.uni.leipzig.tebaqa.tebaqacommons.model.*;
import de.uni.leipzig.tebaqa.tebaqacommons.util.TextUtilities;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;

public class SearchService {

    private static final Logger LOGGER = Logger.getLogger(SearchService.class);

    // TODO Externalize?
    public static final double MATCHING_THRESHOLD_ENTITY = 0.1;
    public static final double MATCHING_THRESHOLD_CLASS = 0.2;
    public static final double MATCHING_THRESHOLD_PROPERTY = 0.32;
    public static final double MATCHING_THRESHOLD_PROPERTY_DISAMBIGUATION = 0.15;
    public static final double MATCHING_SCORE_WITH_NUMBERS = 0.05;
//    private static final double MIN_SCORE_NORMAL = 0.32;
//    private static final double MIN_SCORE_WITH_NUMBERS = 0.05;
    private final ElasticSearchClient index;

    // TODO implement caching here

    public SearchService(ESConnectionProperties props) throws IOException {
        this.index = new ElasticSearchClient(props);
    }

    public SearchService(ElasticSearchClient index) {
        this.index = index;
    }

    public void shutdown() {
        try {
            this.index.close();
        } catch (Exception e) {
            // pass
        }
    }

    public Set<EntityCandidate> searchEntities(String coOccurrence) {
        return this.searchEntities(Optional.of(coOccurrence), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public Set<EntityCandidate> searchEntitiesOfType(String coOccurrence, String typeFilter) {
        return this.searchEntities(Optional.of(coOccurrence), Optional.of(typeFilter), Optional.empty(), Optional.empty());
    }

    public Set<EntityCandidate> searchEntitiesWithConnectedEntity(String coOccurrence, String connectedEntity) {
        return this.searchEntities(Optional.of(coOccurrence), Optional.empty(), Optional.of(connectedEntity), Optional.empty());
    }

    public Set<EntityCandidate> searchEntitiesWithConnectedProperty(String coOccurrence, String connectedProperty) {
        return this.searchEntities(Optional.of(coOccurrence), Optional.empty(), Optional.empty(), Optional.of(connectedProperty));
    }

    public Set<EntityCandidate> searchEntities(Optional<String> coOccurrence, Optional<String> typeFilter, Optional<String> connectedEntity, Optional<String> connectedProperty) {
        Set<EntityCandidate> foundEntities;

        try {
            foundEntities = index.searchEntity(coOccurrence, connectedEntity, connectedProperty, typeFilter);
            if (coOccurrence.isPresent()) foundEntities = getBestCandidates(coOccurrence.get(), foundEntities, EntityCandidate.class);

        } catch (IOException e) {
            LOGGER.info("Searching entities for co-occurrence: " + coOccurrence);
            LOGGER.error("Failed to search entities: " + e.getMessage());
            foundEntities = Collections.emptySet();
        }
        return foundEntities;
    }

    public EntityCandidate searchEntityById(@Nonnull String entityUri) {
        EntityCandidate searchResult = null;
        Set<EntityCandidate> foundEntities = this.searchEntitiesByIds(Lists.newArrayList(entityUri));
        if(foundEntities.size() == 1)
            searchResult = foundEntities.stream().findFirst().get();

        return searchResult;
    }

    public Set<EntityCandidate> searchEntitiesByIds(@Nonnull Collection<String> entityUris) {
        Set<EntityCandidate> foundEntities;
        try {
            foundEntities = index.searchEntitiesByIds(entityUris);
        } catch (IOException e) {
            LOGGER.info("Searching entities by ID for " + entityUris.size() + " URIs");
            LOGGER.error("Failed to search entities: " + e.getMessage());
            foundEntities = Collections.emptySet();
        }
        return foundEntities;
    }

    public PropertyCandidate searchPropertyById(@Nonnull String propertyUri) {
        PropertyCandidate searchResult = null;
        Set<PropertyCandidate> foundProperties = this.searchPropertiesByIds(Lists.newArrayList(propertyUri));
        if(foundProperties.size() == 1)
            searchResult = foundProperties.stream().findFirst().get();

        return searchResult;
    }
    
    public Set<PropertyCandidate> searchPropertiesByIds(@Nonnull Collection<String> propertyUris) {
        Set<PropertyCandidate> foundProperties;
        try {
            foundProperties = index.searchPropertiesByIds(propertyUris);
        } catch (IOException e) {
            LOGGER.info("Searching properties by ID for " + propertyUris.size() + " URIs");
            LOGGER.error("Failed to search properties: " + e.getMessage());
            foundProperties = Collections.emptySet();
        }
        return foundProperties;
    }
    
    public Set<PropertyCandidate> searchProperties(String coOccurrence, boolean searchSynonyms) {
        Set<PropertyCandidate> propertyCandidates;

        try {
            propertyCandidates = index.searchProperty(coOccurrence, searchSynonyms);
            propertyCandidates = getBestCandidates(coOccurrence, propertyCandidates, PropertyCandidate.class);

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

    public ClassCandidate searchClassById(@Nonnull String classUri) {
        ClassCandidate searchResult = null;
        Set<ClassCandidate> foundClasses = this.searchClassesByIds(Lists.newArrayList(classUri));
        if(foundClasses.size() == 1)
            searchResult = foundClasses.stream().findFirst().get();

        return searchResult;
    }

    public Set<ClassCandidate> searchClassesByIds(@Nonnull Collection<String> classUris) {
        Set<ClassCandidate> foundClasses;
        try {
            foundClasses = index.searchClassesByIds(classUris);
        } catch (IOException e) {
            LOGGER.info("Searching classes by ID for " + classUris.size() + " URIs");
            LOGGER.error("Failed to search classes: " + e.getMessage());
            foundClasses = Collections.emptySet();
        }
        return foundClasses;
    }


    public Set<ClassCandidate> searchClasses(String coOccurrence) {
        Set<ClassCandidate> classCandidates;
        try {
            classCandidates = index.searchClass(coOccurrence);
            classCandidates = getBestCandidates(coOccurrence, classCandidates, ClassCandidate.class);

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

    public <T> Set<T> getBestCandidates(String coOccurrence, Set<? extends ResourceCandidate> foundResources, Class<T> clazz) {
        double threshold;
        if(coOccurrence.matches(".*\\d+.*"))
            threshold = MATCHING_SCORE_WITH_NUMBERS;
        else {
            if (clazz.equals(ClassCandidate.class)) {
                threshold = MATCHING_THRESHOLD_CLASS;
            } else if (clazz.equals(PropertyCandidate.class)) {
                threshold = MATCHING_THRESHOLD_PROPERTY;
            } else if (clazz.equals(EntityCandidate.class)) {
                threshold = MATCHING_THRESHOLD_ENTITY;
            } else {
                threshold = MATCHING_THRESHOLD_ENTITY;
            }
        }

        return this.getBestCandidates(coOccurrence, foundResources, threshold);
    }

//    public <T> Set<T> getBestCandidates(String coOccurrence, Set<? extends ResourceCandidate> foundResources, Double thresholdOverride) {
//        Set<ResourceCandidate> bestCandidates = new HashSet<>();
//        for (ResourceCandidate resource : foundResources) {
//            boolean coOccurrenceNull = resource.getCoOccurrence() == null;
//            String bestMatchedLabel = coOccurrenceNull ? resource.getBestLabelFor(coOccurrence) : resource.getBestLabel();
//            double ratio = coOccurrenceNull ? resource.getDistanceScoreFor(coOccurrence) : resource.getDistanceScore();
//
//            // Add found entity to final result if it's levenstein score is below threshold (i.e. a good match)
//            double matchingThreshold = thresholdOverride != null ? thresholdOverride : getLevensteinThreshold(coOccurrence, bestMatchedLabel);
//            if (ratio <= matchingThreshold) {
//                double numberOfWords = (TextUtilities.countWords(coOccurrence));
//                double relFactor = (numberOfWords - (2 * ratio * numberOfWords));
//                resource.setRelatednessFactor(relFactor);
//                bestCandidates.add(resource);
//            }
//        }
////        if (clazz.equalsIgnoreCase(EntityCandidate.class.toString()) && bestCandidates.size() == 0 && foundResources.size() == 100)
////            return (Set<T>) foundResources;
//
//        return (Set<T>) bestCandidates;
//    }

    public <T> Set<T> getBestCandidates(String coOccurrence, Set<? extends ResourceCandidate> foundResources, double matchingThreshold) {
        Set<ResourceCandidate> bestCandidates = new HashSet<>();
        for (ResourceCandidate resource : foundResources) {
            boolean coOccurrenceNull = resource.getCoOccurrence() == null;
//            String bestMatchedLabel = coOccurrenceNull ? resource.getBestLabelFor(coOccurrence) : resource.getBestLabel();
            double ratio = coOccurrenceNull ? resource.getDistanceScoreFor(coOccurrence) : resource.getDistanceScore();

            // Add found entity to final result if it's levenstein score is below threshold (i.e. a good match)
//            double matchingThreshold = thresholdOverride != null ? thresholdOverride : getLevensteinThreshold(coOccurrence, bestMatchedLabel);
            if (ratio <= matchingThreshold) {
                double numberOfWords = (TextUtilities.countWords(coOccurrence));
                double relFactor = (numberOfWords - (2 * ratio * numberOfWords));
                resource.setRelatednessFactor(relFactor);
                bestCandidates.add(resource);
            }
        }

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

    public static void main(String[] args) throws IOException {
//        SearchService s = new SearchService();
//        Set<EntityCandidate> candidates = s.searchEntities("the Eiffel Tower");
//        candidates.forEach(ent -> System.out.println(ent.getCoOccurrence() + "->(" + ent.getUri() + ";" + Collections.singletonList(ent.getResourceLabels()).get(0) + ")"));
//        System.out.println(SearchService.getLevenshteinRatio("the height", "The Flight"));

    }
}

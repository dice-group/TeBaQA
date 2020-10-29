package de.uni.leipzig.tebaqa.entitylinking.service;

import com.google.common.collect.Sets;
import de.uni.leipzig.tebaqa.tebaqacommons.model.EntityCandidate;
import de.uni.leipzig.tebaqa.tebaqacommons.model.PropertyCandidate;

import java.util.*;

public class DisambiguationService {

    private final SearchService searchService;

    public DisambiguationService(SearchService searchService) {
        this.searchService = searchService;
    }

    public Set<EntityCandidate> disambiguateEntities(Map<String, Set<EntityCandidate>> ambiguousEntityCandidates, Set<EntityCandidate> knownEntities, Set<PropertyCandidate> knownProperties, Optional<String> type) {
        Set<EntityCandidate> result = new HashSet<>();
        for (String coOccurrence : ambiguousEntityCandidates.keySet()) {
            Set<EntityCandidate> disambiguatedEntities = findEntitiesWithConnectedResources(coOccurrence, knownEntities, knownProperties, type);
            if (disambiguatedEntities.size() > 0 && disambiguatedEntities.size() <= 10) {
                result.addAll(disambiguatedEntities);
            } else {
                int max = 0;
                EntityCandidate mostPopular = null;
                for (EntityCandidate c : ambiguousEntityCandidates.get(coOccurrence)) {
                    int m = c.getConnectedResourcesSubject().size() +
                            c.getConnectedResourcesObject().size();
                    if (m > max) {
                        max = m;
                        mostPopular = c;
                    }
                }
                if (mostPopular != null && mostPopular.getCoOccurrence() != null)
                    result.add(mostPopular);
            }
        }
        return result;
    }

    public Set<EntityCandidate> findEntitiesWithConnectedResources(String coOccurrence, Set<EntityCandidate> knownEntities, Set<PropertyCandidate> knownProperties, Optional<String> type) {
        HashMap<String, EntityCandidate> candidates = new HashMap<>();
        for (EntityCandidate entityCandidate : knownEntities) {
            Set<EntityCandidate> cs = searchService.searchEntitiesWithConnectedEntity(coOccurrence, entityCandidate.getUri());
            cs.forEach(c -> {
                if (!candidates.containsKey(c.getUri())) candidates.put(c.getUri(), c);
            });
        }
        if (candidates.size() == 0) {
            for (PropertyCandidate propertyCandidate : knownProperties) {
                Set<EntityCandidate> cs = searchService.searchEntitiesWithConnectedProperty(coOccurrence, propertyCandidate.getUri());
                cs.forEach(c -> {
                    if (!candidates.containsKey(c.getUri())) candidates.put(c.getUri(), c);
                });
            }
        }

        Set<EntityCandidate> bestCandidates = searchService.getBestCandidates(coOccurrence, Sets.newHashSet(candidates.values()));
        if (bestCandidates.size() >= 100)
            bestCandidates.clear();
        return bestCandidates;
    }

}

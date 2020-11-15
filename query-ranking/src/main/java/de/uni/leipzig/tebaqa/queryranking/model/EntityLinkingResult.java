package de.uni.leipzig.tebaqa.queryranking.model;

import de.uni.leipzig.tebaqa.queryranking.util.QueryRankingUtils;
import de.uni.leipzig.tebaqa.tebaqacommons.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class EntityLinkingResult {
    private final Set<String> coOccurrences;
    private final Set<ClassCandidate> classCandidates;
    private final Set<PropertyCandidate> propertyCandidates;
    private final Set<EntityCandidate> entityCandidates;
    private final Set<EntityCandidate> literalCandidates;
    private final Set<String> propertyUris;

    public EntityLinkingResult(Set<String> coOccurrences, Set<ClassCandidate> classCandidates, Set<PropertyCandidate> propertyCandidates, Set<EntityCandidate> entityCandidates, Set<String> propertyUris) {
        this.coOccurrences = coOccurrences;
        this.classCandidates = classCandidates;
        this.propertyCandidates = propertyCandidates;
        this.entityCandidates = entityCandidates;
        this.propertyUris = propertyUris;
        this.literalCandidates = Collections.emptySet();
    }

    public EntityLinkingResult(EntityLinkingResponseBean entityLinkingResponse) {
        this.coOccurrences = entityLinkingResponse.getCoOccurrences();
        this.classCandidates = entityLinkingResponse.getClassCandidates();
        this.propertyCandidates = entityLinkingResponse.getPropertyCandidates();
        this.entityCandidates = entityLinkingResponse.getEntityCandidates();
        this.propertyUris = entityLinkingResponse.getPropertyUris();
        this.literalCandidates = Collections.emptySet();
    }

    public Set<String> getCoOccurrences() {
        return coOccurrences;
    }

    public Set<ClassCandidate> getClassCandidates() {
        return classCandidates;
    }

    public Set<PropertyCandidate> getPropertyCandidates() {
        return propertyCandidates;
    }

    public Set<EntityCandidate> getEntityCandidates() {
        return entityCandidates;
    }

    public Set<EntityCandidate> getLiteralCandidates() {
        return literalCandidates;
    }

    public Set<String> getPropertyUris() {
        return propertyUris;
    }

    public double getAverageRelatednessScore(Set<String> uris) {
        List<ResourceCandidate> resources = new ArrayList<>();
        for (String uri : uris) {
            ResourceCandidate cr = QueryRankingUtils.detectCandidateByUri(entityCandidates, uri);
            if (cr != null) resources.add(cr);
            ResourceCandidate cp = QueryRankingUtils.detectCandidateByUri(propertyCandidates, uri);
            if (cp != null) resources.add(cp);
            ResourceCandidate cc = QueryRankingUtils.detectCandidateByUri(classCandidates, uri);
            if (cc != null) resources.add(cc);
        }
        double score = 0;
        for (ResourceCandidate c : resources)
            score += c.getRelatednessFactor();
        return score / resources.size();
    }

    public int getPopularity(Set<String> resources) {
        int popularity = 0;
        for (String res : resources) {
            ResourceCandidate ent = QueryRankingUtils.detectCandidateByUri(entityCandidates, res);
            if (ent != null) {
                popularity += ((EntityCandidate) ent).getConnectedPropertiesSubject().size() + ((EntityCandidate) ent).getConnectedPropertiesObject().size();
            }
            popularity += 0;
        }
        return popularity;
    }
}

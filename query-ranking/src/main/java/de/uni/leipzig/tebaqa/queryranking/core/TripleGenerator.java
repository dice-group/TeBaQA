package de.uni.leipzig.tebaqa.queryranking.core;

import com.google.common.collect.Lists;
import de.uni.leipzig.tebaqa.queryranking.elasticsearch.SearchProvider;
import de.uni.leipzig.tebaqa.queryranking.model.CompoundTriples;
import de.uni.leipzig.tebaqa.queryranking.model.EntityLinkingResult;
import de.uni.leipzig.tebaqa.queryranking.model.Triple;
import de.uni.leipzig.tebaqa.queryranking.model.TripleTemplate;
import de.uni.leipzig.tebaqa.queryranking.util.Constants;
import de.uni.leipzig.tebaqa.queryranking.util.QueryRankingUtils;
import de.uni.leipzig.tebaqa.tebaqacommons.elasticsearch.SearchService;
import de.uni.leipzig.tebaqa.tebaqacommons.model.ClassCandidate;
import de.uni.leipzig.tebaqa.tebaqacommons.model.EntityCandidate;
import de.uni.leipzig.tebaqa.tebaqacommons.model.PropertyCandidate;
import de.uni.leipzig.tebaqa.tebaqacommons.model.ResourceCandidate;
import de.uni.leipzig.tebaqa.tebaqacommons.util.JSONUtils;
import org.apache.jena.vocabulary.RDF;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.uni.leipzig.tebaqa.queryranking.util.QueryRankingUtils.detectCandidateByUri;
import static de.uni.leipzig.tebaqa.queryranking.util.QueryRankingUtils.hasOverlap;

public class TripleGenerator {

    public static final String COUNTRY_PROP = "country_prop";

    private final Set<String> coOccurrences;
    private final Collection<EntityCandidate> entityCandidates;
    private final Collection<ClassCandidate> classCandidates;
    private final Collection<PropertyCandidate> propertyCandidates;
    private final Collection<EntityCandidate> literalCandidates;
    private final Set<String> propertyUris;
    private final SearchService searchService;

    public TripleGenerator(EntityLinkingResult linkedEntities) {
        this.coOccurrences = linkedEntities.getCoOccurrences();
        this.entityCandidates = linkedEntities.getEntityCandidates();
        this.classCandidates = linkedEntities.getClassCandidates();
        this.propertyCandidates = linkedEntities.getPropertyCandidates();
        this.literalCandidates = linkedEntities.getLiteralCandidates();
        this.propertyUris = linkedEntities.getPropertyUris();
        this.searchService = SearchProvider.getSingletonSearchClient();
    }

    private Set<Triple> getCountryTriples() {
        Set<Triple> triples = new HashSet<>();
        for (EntityCandidate candidate : entityCandidates) {
            if (candidate.getTypes().contains(Constants.DBO_COUNTRY)) {
                Triple triple = new Triple(TripleTemplate.VARIABLE_PLACEHOLDER, COUNTRY_PROP, candidate.getUri());
                //TODO verify
                triple.multiplyRating(candidate.getSimilarityScore());
                triple.addUsedEntity(candidate);
                triples.add(triple);
            }
        }
        return triples;
    }

    private Set<Triple> getCategoryTriples() {
        Set<Triple> triples = new HashSet<>();
        for (EntityCandidate candidate : entityCandidates) {
            if (candidate.getTypes().contains(Constants.SKOS_CONCEPT)) {
                Triple triple = new Triple(TripleTemplate.VARIABLE_PLACEHOLDER, Constants.DC_SUBJECT, candidate.getUri());
                //TODO verify
                triple.multiplyRating(candidate.getSimilarityScore());
                triple.addUsedEntity(candidate);
                triples.add(triple);
            }
        }
        return triples;
    }

    private Set<Triple> generateRDFTypeTriples() {
        Set<Triple> triples = new HashSet<>();
        for (ClassCandidate candidate : classCandidates) {
            Triple triple = new Triple(TripleTemplate.VARIABLE_PLACEHOLDER, Constants.INSTANCE_OF, candidate.getUri());
            triple.multiplyRating(candidate.getSimilarityScore()); // TODO verify rating
            triple.addUsedClass(candidate);
            triples.add(triple);
        }
        return triples;
    }

    public Set<Triple> generateSingleTriples(TripleTemplate tripleTemplate) {
        Set<Triple> singleTriples = new HashSet<>();

        TripleTemplate.Pattern templatePattern = tripleTemplate.getPattern();
        if (templatePattern != null) {
            // TODO clarify this
//        List<ResourceCandidate>candidatesCurrent=findProperties(Lists.newArrayList(propertyUris),coOccurrences);
            Collection<PropertyCandidate> filteredProperties = getImportantProperties();
            singleTriples.addAll(generateTriplesWithTwoResources(templatePattern, filteredProperties));

            if (templatePattern.equals(TripleTemplate.Pattern.V_R_R)) {
                singleTriples.addAll(this.getCountryTriples());
                singleTriples.addAll(this.getCategoryTriples());
                singleTriples.addAll(this.generateRDFTypeTriples());
            }
        }
        return singleTriples;
    }

    private Collection<PropertyCandidate> getImportantProperties() {
        Set<PropertyCandidate> importantPropertyCandidates = new HashSet<>();

        Set<String> alreadyCollected = new HashSet<>();
        Stream<PropertyCandidate> stream = this.propertyCandidates.stream()
                .filter(propertyCandidate -> propertyUris.contains(propertyCandidate.getUri()));

        stream.forEach(propertyCandidate -> {
            if (!alreadyCollected.contains(propertyCandidate.getUri())) {
                importantPropertyCandidates.add(propertyCandidate);
                alreadyCollected.add(propertyCandidate.getUri());
            }
        });

        return !importantPropertyCandidates.isEmpty() ? importantPropertyCandidates : propertyCandidates;
    }

    private Set<Triple> generateTriplesWithTwoResources(TripleTemplate.Pattern templatePattern, Collection<PropertyCandidate> properties) {
        Set<Triple> triples = new HashSet<>();
        if (TripleTemplate.Pattern.V_R_R.equals(templatePattern)) {
            for (EntityCandidate ec : entityCandidates) {
                for (String connectedProp : ec.getConnectedPropertiesObject()) {
                    ResourceCandidate matchingProp = detectCandidateByUri(properties, connectedProp);
                    if (matchingProp != null && !hasOverlap(ec.getCoOccurrence(), matchingProp.getCoOccurrence())) {
//                        Triple t = new Triple(TripleTemplate.VARIABLE_PLACEHOLDER, connectedProp, ec.getUri());
//                        t.multiplyRating(ec.getLevenshteinDistanceScore() * ec.getRelatednessFactor()); // TODO verify
                        Triple t = new Triple(null, matchingProp, ec);
                        t.addUsedEntity(ec);
                        t.addUsedProperty((PropertyCandidate) matchingProp);
                        triples.add(t);
                    }
                }
            }

            // Add literal candidates
            for (EntityCandidate lc : literalCandidates) {
                for (String connectedProp : lc.getConnectedPropertiesObject()) {
                    ResourceCandidate matchingProp = detectCandidateByUri(properties, connectedProp);
                    if (matchingProp != null && !hasOverlap(lc.getCoOccurrence(), matchingProp.getCoOccurrence())) {
//                        Triple t = new Triple(TripleTemplate.VARIABLE_PLACEHOLDER, connectedProp, lc.getUri(), true);
//                        t.multiplyRating(lc.getLevenshteinDistanceScore() * lc.getRelatednessFactor()); // TODO verify
                        Triple t = new Triple(null, matchingProp, lc, true);
                        t.addUsedEntity(lc);
                        t.addUsedProperty((PropertyCandidate) matchingProp);
                        triples.add(t);
                    }
                }
            }
        }

        if (TripleTemplate.Pattern.R_R_V.equals(templatePattern)) {
            for (EntityCandidate ec : entityCandidates) {
                for (String connectedProp : ec.getConnectedPropertiesSubject()) {
                    ResourceCandidate matchingProp = detectCandidateByUri(properties, connectedProp);
                    if (matchingProp != null && !hasOverlap(ec.getCoOccurrence(), matchingProp.getCoOccurrence())) {
//                        Triple t = new Triple(ec.getUri(), connectedProp, TripleTemplate.VARIABLE_PLACEHOLDER);
//                        t.multiplyRating(ec.getLevenshteinDistanceScore() * ec.getRelatednessFactor()); // TODO verify
                        Triple t = new Triple(ec, matchingProp, null);
                        t.addUsedEntity(ec);
                        t.addUsedProperty((PropertyCandidate) matchingProp);
                        triples.add(t);
                    }
                }
            }
        }

        return triples;
    }

    public Set<Triple> generateTypePropertyTriples(Triple alreadyKnownTriple, TripleTemplate template) {
        Set<Triple> triples = new HashSet<>();

        for (ResourceCandidate prop : propertyCandidates) {
            Set<EntityCandidate> entityCandidates = searchService.searchEntities(Optional.empty(), Optional.empty(), Optional.of(prop.getUri()), Optional.of(alreadyKnownTriple.getObject()));
            if (template.getSubject().equals(alreadyKnownTriple.getSubject()) || template.getSubject().equals(alreadyKnownTriple.getObject())) {
                entityCandidates.forEach(cand -> {
                    if (cand.getConnectedPropertiesSubject().contains(prop.getUri())) {
                        Triple t = new Triple(template.getSubject(), prop.getUri(), template.getObject());
                        t.multiplyRating(prop.getSimilarityScore());
                        t.addUsedProperty((PropertyCandidate) prop);
                        triples.add(t);
                    }
                });
            } else {
                entityCandidates.forEach(cand -> {
                    if (cand.getConnectedPropertiesObject().contains(prop.getUri())) {
                        Triple t = new Triple(template.getSubject(), prop.getUri(), template.getObject());
                        t.multiplyRating(prop.getSimilarityScore());
                        t.addUsedProperty((PropertyCandidate) prop);
                        triples.add(t);
                    }
                });
            }
        }

        return triples;
    }

    public List<CompoundTriples> generateCompoundTriples(TripleTemplate templateWith2Res, TripleTemplate templateWith1Res, Set<Triple> triplesWith2Res) {
        List<CompoundTriples> tripleCompounds = new ArrayList<>();
        for (Triple candidate : triplesWith2Res) {
            Set<Triple> compoundCandidates;

            if (TripleTemplate.Pattern.V_R_V.sameAs(templateWith1Res.getPatternString()) &&
//                    (candidate.getPredicate().equalsIgnoreCase("a") || candidate.getPredicate().equalsIgnoreCase(RDF.type.getURI())))
                    (candidate.getPredicate().equalsIgnoreCase(Constants.INSTANCE_OF)))
                compoundCandidates = generateTypePropertyTriples(candidate, templateWith1Res);
            else
                compoundCandidates = generateTuplesWithTwoVariables(candidate, templateWith1Res);

            compoundCandidates.forEach(candComp -> {
                CompoundTriples compound = new CompoundTriples(candidate, candComp);
                if (isValidCompound(compound, templateWith2Res, templateWith1Res)
                        && hasCoOccurrenceOverlap(Lists.newArrayList(compound.getKnownTriple(), compound.getNewTriple())))
                    tripleCompounds.add(compound);
            });
        }
        return tripleCompounds;

    }

    private Set<Triple> generateTuplesWithTwoVariables(Triple alreadyKnownTriple, TripleTemplate template) {
        Set<Triple> triplesFound;

        if (TripleTemplate.Pattern.V_R_V.equals(template.getPattern())) {
            triplesFound = twoVariablesVRV(alreadyKnownTriple, template);
        } else if (TripleTemplate.Pattern.V_V_R.equals(template.getPattern())) {
            triplesFound = twoVariablesVVR(alreadyKnownTriple, template);
        } else {
            triplesFound = new HashSet<>();
        }

        return triplesFound;
    }

    private Set<Triple> twoVariablesVVR(Triple alreadyKnownTriple, TripleTemplate template) {
        // TODO discuss, VVR pattern was to connect those triples where a subject maybe common?
        // TODO Does it make sense to enable this only when property is a rdf:type?
        Set<Triple> triplesFound = new HashSet<>();

//        if(alreadyKnownTriple.isPredicateRDFTypeProperty()){
        for (ResourceCandidate ec : entityCandidates) {
            // Check that entityCandidate is not used to replace a placeholder in already known triple
            if (!alreadyKnownTriple.getObject().equals(ec.getUri())) {
                Triple t = new Triple(template.getSubject(), template.getPredicate(), ec.getUri());
                t.multiplyRating(ec.getSimilarityScore()); // TODO rating, verify
                t.addUsedEntity((EntityCandidate) ec);
                triplesFound.add(t);
            }
        }

        for (ResourceCandidate lc : literalCandidates) {
            // Check that literalCandidate is not used to replace a placeholder in already known triple
            if (!alreadyKnownTriple.getSubject().equals(lc.getUri()) && !alreadyKnownTriple.getObject().equals(lc.getUri())) {
                Triple t = new Triple(template.getSubject(), template.getPredicate(), lc.getUri(), true);
                t.multiplyRating(lc.getSimilarityScore()); // TODO rating, verify
                t.addUsedEntity((EntityCandidate) lc);
                triplesFound.add(t);
            }
        }
//        }
        return triplesFound;
    }

    private Set<Triple> twoVariablesVRV(Triple alreadyKnownTriple, TripleTemplate template) {
        Set<String> relevantResourceCandidates = new HashSet<>();

        //for(Triple triple:alreadyKnownTriples){
        if (alreadyKnownTriple.getSubject().startsWith("http") && !alreadyKnownTriple.getPredicate().equals(COUNTRY_PROP)) {
            Optional<EntityCandidate> ent = entityCandidates.stream().filter(ec -> ec.getUri().equalsIgnoreCase(alreadyKnownTriple.getSubject())).findFirst();
            ent.ifPresent(cand -> relevantResourceCandidates.addAll(cand.getConnectedResourcesSubject()));
        } else if (alreadyKnownTriple.getObject().startsWith("http") && !alreadyKnownTriple.getPredicate().equals(COUNTRY_PROP)) {
            Optional<EntityCandidate> ent = entityCandidates.stream().filter(ec -> ec.getUri().equalsIgnoreCase(alreadyKnownTriple.getObject())).findFirst();
            ent.ifPresent(cand -> relevantResourceCandidates.addAll(cand.getConnectedResourcesObject()));
        }
        //}

        Set<Triple> triples = new HashSet<>();
        List<String> uris = Lists.newArrayList(relevantResourceCandidates);

        if (uris.size() > 0) {
            List<EntityCandidate> cands = new ArrayList<>();
            int max = uris.size();
            int current = 0;
            while (max > current + 10 && current < 1000) {
                cands.addAll(searchService.searchEntitiesByIds(uris.subList(current, current + 10)));
                current += 10;
            }
            if (max < 1000)
                cands.addAll(searchService.searchEntitiesByIds(uris.subList(current, max)));


            Set<String> relevantPropertiesCandidate = new HashSet<>();
            if (template.getSubject().equals(alreadyKnownTriple.getSubject()) ||
                    template.getSubject().equals(alreadyKnownTriple.getObject())) {
                cands.forEach(cand -> relevantPropertiesCandidate.addAll(cand.getConnectedPropertiesSubject()));
                Set<PropertyCandidate> properties = findProperties(relevantPropertiesCandidate, coOccurrences);
                properties.forEach(prop -> {
                    Triple triple = new Triple(template.getSubject(), prop.getUri(), TripleTemplate.VARIABLE_PLACEHOLDER);
                    triple.multiplyRating(prop.getSimilarityScore());
                    triple.addUsedProperty(prop);
                    triples.add(triple);
                });
            } else {
                cands.forEach(cand -> relevantPropertiesCandidate.addAll(cand.getConnectedPropertiesObject()));
                Set<PropertyCandidate> properties = findProperties(relevantPropertiesCandidate, coOccurrences);
                properties.forEach(prop -> {
                    Triple triple = new Triple(TripleTemplate.VARIABLE_PLACEHOLDER, prop.getUri(), template.getObject());
                    triple.multiplyRating(prop.getSimilarityScore());
                    triple.addUsedProperty(prop);
                    triples.add(triple);
                });
            }
        }
        return triples;
    }

    private Set<PropertyCandidate> findProperties(Set<String> propertyUrisToFind, Set<String> coOccurrences) {

//        Set<PropertyCandidate> foundProperties = propertyCandidates.stream().filter(propertyCandidate -> propertyUrisToFind.contains(propertyCandidate.getUri())).collect(Collectors.toSet());
//        List<String> urisToFind = propertyCandidates.stream().map(ResourceCandidate::getUri).collect(Collectors.toList());
//        List<String> notFound = new ArrayList<>(Sets.difference(propertyUrisToFind, urisToFind));

//        int max = notFound.size();
        ArrayList<String> urisToFind = new ArrayList<>(propertyUrisToFind);
        int max = urisToFind.size();
        int current = 0;
        Set<PropertyCandidate> foundProperties = new HashSet<>(max);
        while (max > current + 10) {
            foundProperties.addAll(searchService.searchPropertiesByIds(urisToFind.subList(current, current + 10)));
            current += 10;
        }
        foundProperties.addAll(searchService.searchPropertiesByIds(urisToFind.subList(current, urisToFind.size())));

        List<PropertyCandidate> resourceCandidatesFiltered = new ArrayList<>();
        double minScore = 0.15;
        for (String coOccurrence : coOccurrences) {
            Set<PropertyCandidate> bestCandidates = searchService.getBestCandidates(coOccurrence, foundProperties, minScore);

            // Deep copy objects to avoid conflicts
            bestCandidates = bestCandidates.stream().map(propertyCandidate -> JSONUtils.safeDeepCopy(propertyCandidate, PropertyCandidate.class)).filter(Objects::nonNull).collect(Collectors.toSet());

            bestCandidates.forEach(propertyCandidate -> propertyCandidate.setCoOccurrenceAndScore(coOccurrence));
            resourceCandidatesFiltered.addAll(bestCandidates);
            bestCandidates.clear();
        }

        Set<PropertyCandidate> bestScoredUniqueUris = new HashSet<>();
        Map<String, List<PropertyCandidate>> uriToCandidates = resourceCandidatesFiltered.stream().collect(Collectors.groupingBy(ResourceCandidate::getUri));
        uriToCandidates.values().forEach(candidateList -> candidateList.stream().max(Comparator.comparingDouble(PropertyCandidate::getSimilarityScore)).ifPresent(bestScoredUniqueUris::add));

        //avoid Gb propblem
        foundProperties.clear();
        resourceCandidatesFiltered.clear();

        return bestScoredUniqueUris;
    }

    private static boolean isValidCompound(CompoundTriples compoundTriples, TripleTemplate templateWith2Res, TripleTemplate templateWith1Res) {
        if (templateWith2Res.getSubject().equals(templateWith1Res.getSubject()) &&
                !compoundTriples.getKnownTriple().getSubject().equals(compoundTriples.getNewTriple().getSubject()))
            return false;
        if (!templateWith2Res.getSubject().equals(templateWith1Res.getSubject()) &&
                compoundTriples.getKnownTriple().getSubject().equals(compoundTriples.getNewTriple().getSubject()))
            return false;
        if (templateWith2Res.getPredicate().equals(templateWith1Res.getPredicate()) &&
                !compoundTriples.getKnownTriple().getPredicate().equals(compoundTriples.getNewTriple().getPredicate()))
            return false;
        if (!templateWith2Res.getPredicate().equals(templateWith1Res.getPredicate()) &&
                compoundTriples.getKnownTriple().getPredicate().equals(compoundTriples.getNewTriple().getPredicate()))
            return false;
        if (templateWith2Res.getObject().equals(templateWith1Res.getObject()) &&
                !compoundTriples.getKnownTriple().getObject().equals(compoundTriples.getNewTriple().getObject()))
            return false;
        if (!templateWith2Res.getObject().equals(templateWith1Res.getObject()) &&
                compoundTriples.getKnownTriple().getObject().equals(compoundTriples.getNewTriple().getObject()))
            return false;
        if (templateWith2Res.getSubject().equals(templateWith1Res.getObject()) &&
                !compoundTriples.getKnownTriple().getSubject().equals(compoundTriples.getNewTriple().getObject()))
            return false;
        if (!templateWith2Res.getSubject().equals(templateWith1Res.getObject()) &&
                compoundTriples.getKnownTriple().getSubject().equals(compoundTriples.getNewTriple().getObject()))
            return false;
        if (templateWith2Res.getObject().equals(templateWith1Res.getSubject()) &&
                !compoundTriples.getKnownTriple().getObject().equals(compoundTriples.getNewTriple().getSubject()))
            return false;
        if (!templateWith2Res.getObject().equals(templateWith1Res.getSubject()) &&
                compoundTriples.getKnownTriple().getObject().equals(compoundTriples.getNewTriple().getSubject()))
            return false;
        return true;
    }

    private boolean hasCoOccurrenceOverlap(Collection<Triple> triples) {
        List<String> uris = new ArrayList<>();
        for (Triple triple : triples) {
            if (triple.getSubject().startsWith("http")) uris.add(triple.getSubject());
            if (triple.getPredicate().startsWith("http")) uris.add(triple.getPredicate());
            if (triple.getObject().startsWith("http")) uris.add(triple.getObject());
        }
        return hasCoOccurrenceOverlap(uris);
    }

    public boolean hasCoOccurrenceOverlap(List<String> uris) {
        List<ResourceCandidate> resources = new ArrayList<>();
        for (String uri : uris) {
            Optional<EntityCandidate> cr = entityCandidates.stream().filter(can -> can.getUri().equalsIgnoreCase(uri)).findFirst();
            cr.ifPresent(resources::add);
            Optional<PropertyCandidate> cp = propertyCandidates.stream().filter(can -> can.getUri().equalsIgnoreCase(uri)).findFirst();
            cp.ifPresent(resources::add);
            Optional<ClassCandidate> cc = classCandidates.stream().filter(can -> can.getUri().equalsIgnoreCase(uri)).findFirst();
            cc.ifPresent(resources::add);
        }

        List<String> mappedCoOccurrences = new ArrayList<>();
        for (ResourceCandidate rc : resources) {
            for (String co : mappedCoOccurrences)
                if (QueryRankingUtils.hasOverlap(rc.getCoOccurrence(), co)) return false;
            mappedCoOccurrences.add(rc.getCoOccurrence());
        }
        return true;
    }
}

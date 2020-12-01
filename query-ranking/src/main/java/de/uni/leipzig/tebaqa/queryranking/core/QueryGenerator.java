package de.uni.leipzig.tebaqa.queryranking.core;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import de.uni.leipzig.tebaqa.queryranking.model.*;
import de.uni.leipzig.tebaqa.queryranking.util.QueryRankingUtils;
import de.uni.leipzig.tebaqa.tebaqacommons.model.EntityCandidate;
import de.uni.leipzig.tebaqa.tebaqacommons.model.QueryRankingResponseBean;
import de.uni.leipzig.tebaqa.tebaqacommons.model.RatedQuery;
import de.uni.leipzig.tebaqa.tebaqacommons.util.JSONUtils;

import java.util.*;
import java.util.stream.Collectors;

public class QueryGenerator {

    private final Collection<String> queryTemplates;
    private final EntityLinkingResult linkedEntities;

    public QueryGenerator(EntityLinkingResult linkedEntities, Collection<String> queryTemplates) {
        this.linkedEntities = linkedEntities;
        this.queryTemplates = queryTemplates;
    }

    public QueryRankingResponseBean generateQueries() {
        Set<RatedQuery> generatedQueries = new HashSet<>();

        for (String queryTemplate : queryTemplates) {
            Set<RatedQuery> ratedQueries = fillQueryTemplate(queryTemplate);
            ratedQueries.forEach(ratedQuery -> ratedQuery.setQueryTemplate(queryTemplate));
            generatedQueries.addAll(ratedQueries);
        }

        return new QueryRankingResponseBean(generatedQueries);
    }

    private Set<RatedQuery> fillQueryTemplate(String queryTemplate) {
        List<TripleTemplate> tripleTemplates = QueryRankingUtils.extractTripleTemplates(queryTemplate);
        tripleTemplates = tripleTemplates.parallelStream()
                .filter(s -> !s.getPatternString().toLowerCase().contains("filter")
                        && !s.getPatternString().toLowerCase().contains("optional"))
                .collect(Collectors.toList());
        int resourceCount = QueryRankingUtils.countResourcesToMatch(tripleTemplates);

        List<RatedMapping> mappings = generateTripleCandidateMappings(tripleTemplates, linkedEntities);

        // Remove invalid mappings which use literal as a subject
//        List<String> disallowedLiteralPlaceholders = new ArrayList<>();
//        for (TripleTemplate template : tripleTemplates) {
//            if (template.isSubjectAResource()) {
//                disallowedLiteralPlaceholders.add(template.getSubject());
//            }
//        }
//
//        List<RatedMapping> invalidMappings = new ArrayList<>();
//        mappings.forEach(mapping -> {
//            for (String placeholder : mapping.keySet()) {
//                if (disallowedLiteralPlaceholders.contains(placeholder) && !isUrlResource(mapping.get(placeholder))) {
//                    invalidMappings.add(mapping);
//                    break;
//                }
//            }
//        });
//        mappings.removeAll(invalidMappings);

        Set<RatedQuery> queries = new HashSet<>();
        if (mappings.size() > 0 && mappings.get(0).size() == resourceCount) {
            mappings = rankMappings(mappings, tripleTemplates);
            for (RatedMapping mapping : mappings) {
                String query = "" + queryTemplate;
                for (String key : mapping.keySet()) {
                    String val = mapping.get(key);
                    if (val.equals(TripleGenerator.COUNTRY_PROP)) query = query.replace(key, "?county_var");

                    if (val.equals("a"))
                        query = query.replace(key, val);
                    else {
                        query = query.replace(key, mapping.get(key));
                    }
                }
                queries.add(new RatedQuery(query, mapping.getUsedEntities(), mapping.getUsedProperties(), mapping.getUsedClasses(), mapping.getRating()));
            }
        }

        minifyResponse(queries);
        return queries;
    }

    /* Remove connectedProperty* and connectedResource* attributes from RatedQuery.usedEntities
     * This is useful because type:Country entities have large number of connected resources which increases the
     * response size*/
    private static void minifyResponse(Set<RatedQuery> queries) {
        queries.forEach(ratedQuery -> {
            Set<EntityCandidate> usedEntities = ratedQuery.getUsedEntities();
            Set<EntityCandidate> minifiedUsedEntities = new HashSet<>(usedEntities.size());
            usedEntities.forEach(entity -> {
                EntityCandidate copy = JSONUtils.safeDeepCopy(entity, EntityCandidate.class);
                if (copy != null) {
                    copy.getConnectedPropertiesObject().clear();
                    copy.getConnectedPropertiesSubject().clear();
                    copy.getConnectedResourcesObject().clear();
                    copy.getConnectedResourcesSubject().clear();
                    minifiedUsedEntities.add(copy);
                }
            });
            ratedQuery.setUsedEntities(minifiedUsedEntities);
        });
    }

    private static List<RatedMapping> generateTripleCandidateMappings(List<TripleTemplate> tripleTemplates, EntityLinkingResult linkedEntities) {
        TripleGenerator tripleGenerator = new TripleGenerator(linkedEntities);

        // Triple with two resources calculation
        HashMap<TripleTemplate, Set<Triple>> candidatesWith2Res = new HashMap<>();
        tripleTemplates.forEach(tripleTemplate -> {
            if ((tripleTemplate.isSubjectAResource() || tripleTemplate.isObjectAResource()) && tripleTemplate.isPredicateAResource()) {
                Set<Triple> tripleCandidates = tripleGenerator.generateSingleTriples(tripleTemplate);
                if (tripleTemplate.isObjectAResource())
                    tripleCandidates.forEach(c -> c.setSubject(tripleTemplate.getSubject()));
                if (tripleTemplate.isSubjectAResource())
                    tripleCandidates.forEach(c -> c.setObject(tripleTemplate.getObject()));
                candidatesWith2Res.put(tripleTemplate, tripleCandidates);
            }
        });

        // Compound triples calculation
        List<CompoundTripleCandidates> compoundCandidates = new ArrayList<>();
        if (tripleTemplates.size() > candidatesWith2Res.size()) {

            // Only process tripleTemplates with single resource,
            // filter out tripleTemplates with two resources because they are already processed above.
            List<TripleTemplate> templatesWith1Resource = tripleTemplates.stream().filter(triplePattern -> !candidatesWith2Res.containsKey(triplePattern)).collect(Collectors.toList());

            for (TripleTemplate patternWith1Res : templatesWith1Resource) {

                for (TripleTemplate patternWith2Res : candidatesWith2Res.keySet()) {
                    if (patternWith2Res.getSubject().equals(patternWith1Res.getSubject()) ||
                            patternWith2Res.getSubject().equals(patternWith1Res.getObject()) ||
                            patternWith2Res.getObject().equals(patternWith1Res.getSubject()) ||
                            patternWith2Res.getObject().equals(patternWith1Res.getObject())
                    ) {
                        List<CompoundTriples> generatedCompounds = tripleGenerator.generateCompoundTriples(patternWith2Res, patternWith1Res, candidatesWith2Res.get(patternWith2Res));
                        if (generatedCompounds.size() > 0) {
                            compoundCandidates.add(new CompoundTripleCandidates(patternWith2Res, patternWith1Res, generatedCompounds));
                            break;
                        }
                    }
                }
            }
        }

        compoundCandidates.forEach(compoundCandidate -> candidatesWith2Res.remove(compoundCandidate.getTemplateWith2Res()));

        List<RatedMapping> mappings = new ArrayList<>();
        compoundCandidates.forEach(comp -> {
            mappings.addAll(generateMappingFromCompound(comp.getCandidates(), comp.getTemplateWith2Res(), comp.getTemplateWith1Res()));
        });
        candidatesWith2Res.keySet().forEach(pat -> {
            mappings.addAll(generateMappingFromSingleTriple(candidatesWith2Res.get(pat), pat));
        });
        List<RatedMapping> mergeMappings = mergeMappings(mappings);
        return mergeMappings;

    }

    private static List<RatedMapping> generateMappingFromCompound(List<CompoundTriples> compounds, TripleTemplate templateWith2Res, TripleTemplate templateWith1Res) {
        List<RatedMapping> mappings = new ArrayList<>();

        compounds.forEach(comp -> {
            RatedMapping mapping = new RatedMapping();
            Triple tripleWith2Res = comp.getKnownTriple();
            Triple tripleWith1Res = comp.getNewTriple();

            mapping.addUsedEntities(tripleWith1Res.getUsedEntities());
            mapping.addUsedClasses(tripleWith1Res.getUsedClasses());
            mapping.addUsedProperties(tripleWith1Res.getUsedProperties());
            mapping.addUsedEntities(tripleWith2Res.getUsedEntities());
            mapping.addUsedClasses(tripleWith2Res.getUsedClasses());
            mapping.addUsedProperties(tripleWith2Res.getUsedProperties());

            // TODO Discuss how to combine two ratings
            mapping.multiplyRating(tripleWith2Res.getRating());
            mapping.multiplyRating(tripleWith1Res.getRating());

            // Handle pattern with two resources
            String predicateValue = tripleWith2Res.getPredicate();
            if (!predicateValue.equals("a") && !predicateValue.equals(TripleGenerator.COUNTRY_PROP))
                predicateValue = "<" + predicateValue + ">";
            mapping.put(templateWith2Res.getPredicate(), predicateValue);

            if (templateWith2Res.isSubjectAResource())
                mapping.put(templateWith2Res.getSubject(), "<" + tripleWith2Res.getSubject() + ">");
            else {
                String objectValue = tripleWith2Res.getObject();
                if (tripleWith2Res.isLiteralObject())
                    objectValue = "\"" + objectValue + "\"";
                else
                    objectValue = "<" + objectValue + ">";
                mapping.put(templateWith2Res.getObject(), objectValue);
            }

            // Handle pattern with one resource
            if (TripleTemplate.Pattern.V_R_V.equals(templateWith1Res.getPattern())) {
                predicateValue = tripleWith1Res.getPredicate();
                if (!predicateValue.equals("a") && !predicateValue.equals(TripleGenerator.COUNTRY_PROP))
                    predicateValue = "<" + predicateValue + ">";
                mapping.put(templateWith1Res.getPredicate(), predicateValue);
            } else if (TripleTemplate.Pattern.V_V_R.equals(templateWith1Res.getPattern())) {
                String objectValue = tripleWith1Res.getObject();
                if (tripleWith1Res.isLiteralObject())
                    objectValue = "\"" + objectValue + "\"";
                else
                    objectValue = "<" + objectValue + ">";
                mapping.put(templateWith1Res.getObject(), objectValue);
            }

            mappings.add(mapping);
        });
        return mappings;
    }

    private static List<RatedMapping> generateMappingFromSingleTriple(Set<Triple> triples, TripleTemplate pattern) {
        List<RatedMapping> mappings = new ArrayList<>();
        triples.forEach(trip -> {
            RatedMapping mapping = new RatedMapping();
            mapping.addUsedEntities(trip.getUsedEntities());
            mapping.addUsedClasses(trip.getUsedClasses());
            mapping.addUsedProperties(trip.getUsedProperties());
            mapping.multiplyRating(trip.getRating());

            if (pattern.getSubject().startsWith(TripleTemplate.RESOURCE_PREFIX_IDENTIFIER))
                mapping.put(pattern.getSubject(), "<" + trip.getSubject() + ">");
            else {
                String objectValue = trip.getObject();
                if (trip.isLiteralObject())
                    objectValue = "\"" + objectValue + "\"";
                else
                    objectValue = "<" + objectValue + ">";
                mapping.put(pattern.getObject(), objectValue);
            }

            String predValue = trip.getPredicate();
            if (!predValue.equals("a") && !predValue.equals(TripleGenerator.COUNTRY_PROP))
                predValue = "<" + predValue + ">";
            mapping.put(pattern.getPredicate(), predValue);
            mappings.add(mapping);
        });
        return mappings;
    }

    private static List<RatedMapping> mergeMappings(List<RatedMapping> mappings) {
        List<RatedMapping> mergedMappings = new ArrayList<>();

        for (int x = 0; x < mappings.size(); x++) {
            for (int y = x + 1; y < mappings.size(); y++) {
                boolean validMerge = true;
                RatedMapping merged = new RatedMapping();

                RatedMapping set1 = mappings.get(x);
                merged.addUsedEntities(set1.getUsedEntities());
                merged.addUsedClasses(set1.getUsedClasses());
                merged.addUsedProperties(set1.getUsedProperties());

                RatedMapping set2 = mappings.get(y);
                merged.addUsedEntities(set2.getUsedEntities());
                merged.addUsedClasses(set2.getUsedClasses());
                merged.addUsedProperties(set2.getUsedProperties());

                // Average the ratings
                merged.multiplyRating((set1.getRating() + set2.getRating()) / 2);
//                merged.multiplyRating(set2.getRating());

                // If both sets have same keys, ignore pair.
                if (set1.keySet().containsAll(set2.keySet()) && set2.keySet().containsAll(set1.keySet())) {
                    validMerge = false;
                }

                ImmutableSet<String> commonKeys = Sets.intersection(set1.keySet(), set2.keySet()).immutableCopy();

                // Make sure that if a key exists in both sets, value associated with this key is same.
                if (validMerge) {
                    for (String commonKey : commonKeys) {
                        if (!Objects.equals(set1.get(commonKey), set2.get(commonKey))) {
                            validMerge = false;
                            break;
                        }
                    }
                    if (validMerge) {
                        // Add common keys to merged
                        commonKeys.forEach(commonKey -> merged.put(commonKey, set1.get(commonKey)));
                    }
                }

                // Add remaining keys from set1
                if (validMerge) validMerge = copyRemainingEntries(set1, merged, commonKeys);

                // Add remaining keys from set2
                if (validMerge) validMerge = copyRemainingEntries(set2, merged, commonKeys);

                // Everything went well, successful merge. Add it to mergedMappings.
                if (validMerge)
                    mergedMappings.add(merged);
            }
        }

        if (mergedMappings.size() == 0) return mappings;
        else return mergeMappings(mergedMappings);
    }

    private static boolean copyRemainingEntries(RatedMapping from, RatedMapping to, Set<String> commonKeys) {
        boolean success = false;

        // Prepare remaining mappings
        ImmutableSet<String> remainingKeys = Sets.difference(from.keySet(), commonKeys).immutableCopy();
        Map<String, String> remainingMappings = new HashMap<>();
        for (String remainingKey : remainingKeys) {
            remainingMappings.put(remainingKey, from.get(remainingKey));
        }

        // Merge is invalid if two different keys map to the same value. Check this condition.
        if (Sets.intersection(Sets.newHashSet(to.values()), Sets.newHashSet(remainingMappings.values())).immutableCopy().size() == 0) {
            to.putAll(remainingMappings);
            success = true;
        }

        return success;
    }

    private List<RatedMapping> rankMappings(List<RatedMapping> mappings, List<TripleTemplate> patterns) {
        ArrayList<RatedMapping> rankedMappings = new ArrayList<>();
        for (RatedMapping mapping : mappings) {
            boolean added = false;
            for (int i = 0; i < rankedMappings.size(); i++) {
                if (compareMappings(rankedMappings.get(i), mapping, patterns)) {
                    rankedMappings.add(i, mapping);
                    added = true;
                    break;
                }

            }
            if (!added) rankedMappings.add(mapping);
        }
        return rankedMappings;
    }

    private boolean compareMappings(RatedMapping rankedMapping, RatedMapping mapping, List<TripleTemplate> patterns) {
        // TODO DIscuss
        Set<String> valuesRanked = Sets.newHashSet(rankedMapping.values());
        Set<String> valuesMapping = Sets.newHashSet(mapping.values());
        if (containsProp(mapping, "country_prop") && !containsProp(rankedMapping, "country_prop"))
            return false;
        else if (!containsProp(mapping, "country_prop") && containsProp(rankedMapping, "country_prop"))
            return true;

        if (containsProp(mapping, "a") && !containsProp(rankedMapping, "a"))
            return false;
        else if (!containsProp(mapping, "a") && containsProp(rankedMapping, "a"))
            return true;

        List<String[]> connResources = new ArrayList<>();
        List<String[]> connResourcesRanked = new ArrayList<>();
        patterns.forEach(pat -> {
            if (pat.getSubject().startsWith(TripleTemplate.RESOURCE_PREFIX_IDENTIFIER) && pat.getPredicate().startsWith(TripleTemplate.RESOURCE_PREFIX_IDENTIFIER)) {
                connResources.add(new String[]{mapping.get(pat.getSubject()), mapping.get(pat.getPredicate())});
                connResourcesRanked.add(new String[]{rankedMapping.get(pat.getSubject()), rankedMapping.get(pat.getPredicate())});
            }
            if (pat.getObject().startsWith(TripleTemplate.RESOURCE_PREFIX_IDENTIFIER) && pat.getPredicate().startsWith(TripleTemplate.RESOURCE_PREFIX_IDENTIFIER)) {
                connResources.add(new String[]{mapping.get(pat.getObject()), mapping.get(pat.getPredicate())});
                connResourcesRanked.add(new String[]{rankedMapping.get(pat.getObject()), rankedMapping.get(pat.getPredicate())});
            }
        });
        double avgRelatednessMapping = linkedEntities.getAverageRelatednessScore(valuesMapping);
        double avgRelatednessMappingRanked = linkedEntities.getAverageRelatednessScore(valuesRanked);
        if (avgRelatednessMapping > avgRelatednessMappingRanked) return true;
        else if (avgRelatednessMappingRanked > avgRelatednessMapping) return false;
        int popMapping = linkedEntities.getPopularity(valuesMapping);
        int popRanked = linkedEntities.getPopularity(valuesRanked);
        if (popMapping > popRanked) return true;
        else if (popMapping < popRanked) return false;
        // TODO enable these?
//        int numberOfSemDependanciesMapping=linkedEntities.getNumberOfDependantResourceCombinations(connResources);
//        int numberOfSemDependanciesMappingRanked=linkedEntities.getNumberOfDependantResourceCombinations(connResourcesRanked);
//        if(numberOfSemDependanciesMapping>numberOfSemDependanciesMappingRanked)return true;
//        else if(numberOfSemDependanciesMappingRanked<numberOfSemDependanciesMapping)return false;

        return false;
    }

    private static boolean containsProp(RatedMapping mapping, String prop) {
        return mapping.values().contains(prop);
    }
}

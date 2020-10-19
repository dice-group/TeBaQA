package de.uni.leipzig.tebaqa.entitylinking.service;

import de.uni.leipzig.tebaqa.tebaqacommons.nlp.ISemanticAnalysisHelper;
import de.uni.leipzig.tebaqa.tebaqacommons.nlp.SemanticAnalysisHelperEnglish;

import java.util.*;

public class EntityLinkingService {

    private ISemanticAnalysisHelper semanticAnalysisHelper;

    public EntityLinkingService() {
        this.semanticAnalysisHelper = new SemanticAnalysisHelperEnglish();
    }

    public void extractEntities(String question) {
        question = semanticAnalysisHelper.removeQuestionWords(question);
        question = question.replace("(", "");
        question = question.replace(")", "");
//        SemanticGraph semanticGraph = semanticAnalysisHelper.extractDependencyGraph(question);
//        String[] wordsFromQuestion = question.replaceAll("[\\-.?¿!,;]", "").split("\\s+");
//        //List<CooccurenceGroup> coOccurrences = getNeighborCoOccurrencePermutationsGroups(wordsFromQuestion);
//        coOccurrences = getNeighborCoOccurrencePermutations(Arrays.asList(wordsFromQuestion));
//        coOccurrences.sort((s1, s2) -> -(s1.length() - s2.length()));
//        //List<ResourceCandidate> entityCandidates = new ArrayList<ResourceCandidate>();
//        HashMap<String, List<ResourceCandidate>> ambiqueResourceCandidates = new HashMap<>();
//        //List<ResourceCandidate> propertyCandidates = new ArrayList<ResourceCandidate>();
//        //List<ResourceCandidate> propertySynonymCandidates = new ArrayList<ResourceCandidate>();
//        //List<ResourceCandidate> ambiquePropertyCandidates = new ArrayList<ResourceCandidate>();
//        //List<ResourceCandidate> classCandidates = new ArrayList<ResourceCandidate>();
//        List<String> dependantCoOccurrences = new ArrayList<>();
//        if (semanticGraph != null) {
//            for (String coOccurrence : coOccurrences) {
//                if (!wordsGenerator.containsOnlyStopwords(coOccurrence, "en") && isdependant(coOccurrence, semanticGraph))
//                    dependantCoOccurrences.add(coOccurrence);
//            }
//            coOccurrences = dependantCoOccurrences;
//        }
//        coOccurrences.sort((s1, s2) -> -(s1.length() - s2.length()));
//        //List<ResourceCandidate> ambiqueClassCandidates = new ArrayList<ResourceCandidate>();
//        propertyUris = new HashSet<>();
//
//        for (String coOccurence : coOccurrences) {
//            literalCandidates.addAll(index.searchLiteral(coOccurence, 100));
//            literalCandidates.forEach(lc -> propertyUris.addAll(((EntityCandidate) lc).getConnectedPropertiesObject()));
//
//            Set<ResourceCandidate> best = getbestResourcesByLevenstheinRatio(coOccurence, "entity", false, null);
//            best.forEach(cand -> {
//                propertyUris.addAll(((EntityCandidate) cand).getConnectedPropertiesSubject());
//                propertyUris.addAll(((EntityCandidate) cand).getConnectedPropertiesObject());
//            });
//
//            if (best.size() > 0 && best.size() <= 20) {
//                entityCandidates.addAll(best);
//            } else if (best.size() > 20) {
//                List<ResourceCandidate> candidates = new ArrayList<>();
//                candidates.addAll(best);
//                ambiqueResourceCandidates.put(coOccurence, candidates);
//            }
//            //search for Countries
//            best = getbestResourcesByLevenstheinRatio(coOccurence, "entity", false, "http://dbpedia.org/ontology/Country");
//            if (best.size() < 100) {
//                best.forEach(cand -> {
//                    propertyUris.addAll(((EntityCandidate) cand).getConnectedPropertiesSubject());
//                    propertyUris.addAll(((EntityCandidate) cand).getConnectedPropertiesObject());
//                });
//                entityCandidates.addAll(best);
//            }
//            best = getbestResourcesByLevenstheinRatio(coOccurence, "property", true, null);
//            propertyCandidates.addAll(best);
//            best = getbestResourcesByLevenstheinRatio(coOccurence, "class", false, null);
//            classCandidates.addAll(best);
//
//        }
//
//        //propertyCandidates.addAll(findProperties(Lists.newArrayList(propertyUris),coOccurrences));
//        Set<String> ambCoOccurences = new HashSet<>();
//
//        //Disambiguate Ambique Entities
//        ambiqueResourceCandidates.keySet().forEach(key -> ambCoOccurences.add(key));
//        for (String coOccurence : ambCoOccurences) {
//            Set<ResourceCandidate> best = disambiguateAmbiqueEntity(coOccurence, getCommonType(ambiqueResourceCandidates.get(coOccurence)),
//                    entityCandidates, propertyCandidates);
//            if (best.size() > 0 && best.size() <= 10) {
//                entityCandidates.addAll(best);
//                best.forEach(cand -> propertyUris.addAll(((EntityCandidate) cand).getConnectedPropertiesSubject()));
//                best.forEach(cand -> propertyUris.addAll(((EntityCandidate) cand).getConnectedPropertiesObject()));
//                ambiqueResourceCandidates.remove(coOccurence);
//            } else {
//                int max = 0;
//                ResourceCandidate mostPopular = null;
//                for (ResourceCandidate c : ambiqueResourceCandidates.get(coOccurence)) {
//                    int m = ((EntityCandidate) c).getConnectedResourcesSubject().size() +
//                            ((EntityCandidate) c).getConnectedResourcesObject().size();
//                    if (m > max) {
//                        max = m;
//                        mostPopular = c;
//                    }
//                }
//                if (mostPopular != null && mostPopular.getCoOccurence() != null)
//                    entityCandidates.add(mostPopular);
//            }
//
//        }

        /*for(TripleTemplate template:templates){
            List<ResourceCandidate>candidatesCurrent=findProperties(Lists.newArrayList(propertyUris),coOccurrences);
            if(template.getSubject().equals("r")&&template.getPredicate().equals("r")||
            template.getPredicate().equals("r")&&template.getObject().equals("r"))candidateTriples.addAll(generateTuplesWithTwoResources(template,entityCandidates,candidatesCurrent,classCandidates));
        }
        for(TripleTemplate template:templates){
            if(template.getSubject().equals("v")&&template.getObject().equals("v"))
                candidateTriples.addAll(generateTuplesWithTwoVariables(entityCandidates,coOccurrences,candidateTriples));
        }
        candidateTriples.addAll(generateTypeTriples(classCandidates,propertyCandidates));
*/
        System.out.println("EntityExtraction finished");
    }

}

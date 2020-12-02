package de.uni.leipzig.tebaqa.helper;

import com.google.common.collect.Sets;
import de.uni.leipzig.tebaqa.controller.ElasticSearchEntityIndex;
import de.uni.leipzig.tebaqa.controller.SemanticAnalysisHelper;
import de.uni.leipzig.tebaqa.model.*;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;

import java.io.IOException;
import java.util.*;

import static de.uni.leipzig.tebaqa.helper.QueryMappingFactoryLabels.getNeighborCoOccurrencePermutations;
import static de.uni.leipzig.tebaqa.helper.QueryMappingFactoryLabels.getNeighborCoOccurrencePermutationsGroups;
import static de.uni.leipzig.tebaqa.helper.Utilities.getLevenshteinRatio;


public class ResourceLinker {

    public List<ResourceCandidate>mappedEntities;
    public List<ResourceCandidate>mappedProperties;
    public List<ResourceCandidate>mappedClasses;
    SemanticAnalysisHelper semanticAnalysisHelper;
    ElasticSearchEntityIndex index;
    WordsGenerator wordsGenerator;
    public List<SPTupel>spTupels;
    public List<POTupel>poTupels;
    public List<SOTupel>soTupels;
    List<ResourceCandidate>unmappedCandidates;
    public ResourceLinker(SemanticAnalysisHelper semanticAnalysisHelper){
        this.semanticAnalysisHelper=semanticAnalysisHelper;
        wordsGenerator=new WordsGenerator();
        try {
            index=new ElasticSearchEntityIndex();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    boolean coOccurenceContains(String coOccurence1,String coOccurence2){
        return coOccurence1.contains(coOccurence2)||coOccurence2.contains(coOccurence1);
    }
    private String getOverlappingCoOccurence(Set<String>scoredCoOccurences,String coOccurence){
        for(String scoredCoOccurence:scoredCoOccurences){
            if(coOccurenceContains(scoredCoOccurence,coOccurence))return scoredCoOccurence;
        }
        return null;
    }
    private double calculatedAverageLevenstheinRatioByWord(String val1,String val2){
        String[]wordsVal1=val1.split(" ");
        String[]wordsVal2=val2.split(" ");
        double sum=0;
        String[]maxArray;
        String[]minArray;
        if(wordsVal1.length>wordsVal2.length){
            maxArray=wordsVal1;
            minArray=wordsVal2;
        }
        else{
            maxArray=wordsVal2;
            minArray=wordsVal1;
        }
        for(String word1:maxArray) {
            if (!word1.contains("(")){
                double min = 1;
            for (String word2 : minArray) {
                double levenstheinscore = Utilities.getLevenshteinRatio(word1, word2);
                if (levenstheinscore < min) min = levenstheinscore;
            }
            sum += min;
        }
        }
        return sum/maxArray.length;

    }
    private Set<ResourceCandidate> getbestResourcesByLevenstheinRatio(String coOccurrence, String type,boolean searchSynonyms){
        Set<ResourceCandidate> foundResources;
        if(type.equals("entity"))
            foundResources = index.searchEntity(coOccurrence,Optional.empty(),Optional.empty(),Optional.empty());
        else if(type.equals("property"))
            foundResources=index.searchResource(coOccurrence,"property",searchSynonyms);
        else foundResources=index.searchResource(coOccurrence,"class",false);
        Set<ResourceCandidate> bestResourcesByLevenstheinRatio = getbestResourcesByLevenstheinRatio(coOccurrence, foundResources, type,searchSynonyms);
        //bestResourcesByLevenstheinRatio.forEach(c->c.setGroup(group));
        //if(foundResources.size()==100)return foundResources;
        return bestResourcesByLevenstheinRatio;

    }
    private Set<ResourceCandidate>getbestResourcesByLevenstheinRatio(String coOccurence,Set<ResourceCandidate>foundResources,String type,boolean syn){
        Set<ResourceCandidate> bestResourcesByLevenstheinRatio = new HashSet<>();
        double minScore = 0.2;
        for (ResourceCandidate resource : foundResources) {
            double ratio=1;
            for(String label:resource.getResourceString()) {
                //double ratiotemp = Utilities.getLevenshteinRatio(coOccurence, label);
                double ratiotemp;
                if(resource instanceof PropertyCandidate&&!syn)
                ratiotemp=calculatedAverageLevenstheinRatioByWord(coOccurence,label.toLowerCase());
                else ratiotemp = Utilities.getLevenshteinRatio(coOccurence, label.toLowerCase());
                if(ratiotemp < ratio) {
                    ratio = ratiotemp;
                    double relFactor = TextUtilities.countWords(coOccurence) - ratio;
                    resource.setRelatednessFactor(relFactor);
                }
            }
            if (ratio < minScore) {
                resource.setCoOccurence(coOccurence);
                //minScore = ratio;
                resource.setLevenstheinScore(ratio);
                //bestResourcesByLevenstheinRatio.clear();
                bestResourcesByLevenstheinRatio.add(resource);
            } /*else if (ratio == minScore){
                resource.setCoOccurence(coOccurence);
                resource.setLevenstheinScore(ratio);
                bestResourcesByLevenstheinRatio.add(resource);
            }*/
        }
        if(bestResourcesByLevenstheinRatio.size()==0&&foundResources.size()==100&&type.equals("entity"))
            return foundResources;
        return bestResourcesByLevenstheinRatio;
    }
    private Set<ResourceCandidate>disambiguateAmbiqueEntity(String coOccurence,Optional<String>type,List<ResourceCandidate>entityCandidates,List<ResourceCandidate>propertyCandidates){
        HashMap<String,ResourceCandidate>candidates=new HashMap<>();
        for (ResourceCandidate enitityCandidate : entityCandidates) {
            Set<ResourceCandidate> cs = index.searchEntity(coOccurence, Optional.of(enitityCandidate.getUri()), Optional.empty(), type);
            cs.forEach(c-> {if(!candidates.containsKey(c.getUri())) candidates.put(c.getUri(),c);});
        }
        for (ResourceCandidate propertyCandidate : propertyCandidates) {
            Set<ResourceCandidate> cs = index.searchEntity(coOccurence, Optional.empty(),Optional.of(propertyCandidate.getUri()), Optional.empty());
            cs.forEach(c-> {if(!candidates.containsKey(c.getUri())) candidates.put(c.getUri(),c);});
        }
        Set<ResourceCandidate> bestResourcesByLevenstheinRatio=getbestResourcesByLevenstheinRatio(coOccurence,Sets.newHashSet(candidates.values()),"entity",false);
        if(bestResourcesByLevenstheinRatio.size()>=100)
            bestResourcesByLevenstheinRatio.clear();
        return bestResourcesByLevenstheinRatio;
    }
    Optional<String>getCommonType(List<ResourceCandidate>entityCandidates){
        HashMap<String,Integer>types=new HashMap<>();
        for(ResourceCandidate candidate:entityCandidates){
            for(String type:((EntityCandidate)candidate).getTypes()){
                if(types.containsKey(type)){
                    int value=types.get(type)+1;
                    types.put(type,value);
                }
                else types.put(type,1);
            }
        }
        for(String type:types.keySet()){
            if(types.get(type)/entityCandidates.size()>0.9)return Optional.of(type);
        }
        return Optional.empty();
    }
    boolean isdependant(String coOccurence, SemanticGraph g){
        String[]words=coOccurence.split("\\s+");
        if(words.length==1)return true;
        boolean depend=true;
        Set<String>unmatched=new HashSet<>();
        unmatched.addAll(Arrays.asList());
        for(int i=0;i<words.length&&depend;i++){
            List<IndexedWord> words1=g.getAllNodesByWordPattern(words[i]);
            boolean isdependant = false;
            for(int j=0;j<words.length&&!isdependant;j++) {
                if (j != i) {
                    List<IndexedWord> words2=g.getAllNodesByWordPattern(words[j]);
                    for(int k=0;k<words1.size()&&!isdependant;k++){
                        for(int l=0;l<words2.size()&&!isdependant;l++){
                            if(g.containsEdge(words1.get(k),words2.get(l))||g.containsEdge(words2.get(l),words1.get(k)))
                                isdependant=true;
                        }
                    }

                }
            }
            if(!isdependant)depend=false;
        }
        return depend;
    }
    public void extractEntities(String question) {
        //Map<String, List<String[]>> coOccurenceToentitiyCandidateMapping = new HashMap<>();
        //Map<String, List<String[]>> coOccurenceTopropertyCandidateMapping = new HashMap<>();
        //Map<String, List<String[]>> coOccurenceToclassCandidateMapping = new HashMap<>();
        question = semanticAnalysisHelper.removeQuestionWords(question);
        SemanticGraph g=semanticAnalysisHelper.extractDependencyGraph(question);
        String[] wordsFromQuestion = question.replaceAll("[\\-.?¿!,;]", "").split("\\s+");
        //List<CooccurenceGroup> coOccurrences = getNeighborCoOccurrencePermutationsGroups(wordsFromQuestion);
        List<String> coOccurrences = getNeighborCoOccurrencePermutations(Arrays.asList(wordsFromQuestion));
        coOccurrences.sort((s1, s2) -> -(s1.length() - s2.length()));
        List<ResourceCandidate> entityCandidates = new ArrayList<ResourceCandidate>();
        HashMap<String,List<ResourceCandidate>> ambiqueResourceCandidates = new HashMap<>();
        List<ResourceCandidate> propertyCandidates = new ArrayList<ResourceCandidate>();
        List<ResourceCandidate> propertySynonymCandidates = new ArrayList<ResourceCandidate>();
        //List<ResourceCandidate> ambiquePropertyCandidates = new ArrayList<ResourceCandidate>();
        List<ResourceCandidate> classCandidates = new ArrayList<ResourceCandidate>();
        List<String> dependantCoOccurrences=new ArrayList<>();
        if(g!=null) {
            for (String coOccurrence : coOccurrences) {
                if (isdependant(coOccurrence, g)) dependantCoOccurrences.add(coOccurrence.toLowerCase());
            }
            coOccurrences=dependantCoOccurrences;
        }
        coOccurrences.sort((s1, s2) -> -(s1.length() - s2.length()));
        //List<ResourceCandidate> ambiqueClassCandidates = new ArrayList<ResourceCandidate>();
        for(String coOccurence:coOccurrences) {
            if (!wordsGenerator.containsOnlyStopwords(coOccurence, "en")) {
                Set<ResourceCandidate> best = getbestResourcesByLevenstheinRatio( coOccurence, "entity",false);
                if (best.size() > 0 && best.size() <= 10) {
                    entityCandidates.addAll(best);
                } else if (best.size() > 10) {
                    List<ResourceCandidate> candidates = new ArrayList<>();
                    candidates.addAll(best);
                    ambiqueResourceCandidates.put(coOccurence, candidates);
                }
                best = getbestResourcesByLevenstheinRatio( coOccurence, "property",false);
                if (best.size() > 0 /*&& best.size() <= 10*/) propertyCandidates.addAll(best);
                /*best = getbestResourcesByLevenstheinRatio( coOccurence, "property",true);
                if (best.size() > 0) propertySynonymCandidates.addAll(best);*/
                //if (best.size() > 10) ambiquePropertyCandidates.addAll(best);
                best = getbestResourcesByLevenstheinRatio( coOccurence, "class",false);
                if (best.size() > 0 /*&& best.size() < 10*/) classCandidates.addAll(best);
                //if (best.size() > 50) ambiqueClassCandidates.addAll(best);
            }
        }


        /*for(ResourceCandidate ent:entityCandidates){
            for(ResourceCandidate syn:propertySynonymCandidates){
                if(((EntityCandidate)ent).getConnectedPropertiesSubject().contains(syn.getUri())||
                        ((EntityCandidate)ent).getConnectedPropertiesObject().contains(syn.getUri()))
                    propertyCandidates.add(syn);
            }
        }*/
        Set<String>ambCoOccurences=new HashSet<>();

        //Disambiguate Ambique Entities
        ambiqueResourceCandidates.keySet().forEach(key->ambCoOccurences.add(key));
        for(String coOccurence:ambCoOccurences){
            Set<ResourceCandidate> best = disambiguateAmbiqueEntity(coOccurence,getCommonType(ambiqueResourceCandidates.get(coOccurence)),
                    entityCandidates,propertyCandidates);
            if (best.size() > 0 && best.size() <= 10){
                entityCandidates.addAll(best);
                ambiqueResourceCandidates.remove(coOccurence);
            }
        }
        entityCandidates.forEach(ec->System.out.println("Ent Candidtate"+ec.getCoOccurence()+"->"+ec.getUri()));
        propertyCandidates.forEach(ec->System.out.println("Prop Candidtate"+ec.getCoOccurence()+"->"+ec.getUri()));
        classCandidates.forEach(ec->System.out.println("Class Candidtate"+ec.getCoOccurence()+"->"+ec.getUri()));
        soTupels=new ArrayList<>();
        Set<String>mappedCoOccurences=new HashSet<>();
        for(ResourceCandidate entCandidate1:entityCandidates){
            for(ResourceCandidate entCandidate2:entityCandidates){
                if(!coOccurenceContains(entCandidate1.getCoOccurence(),entCandidate2.getCoOccurence())){
                    if(((EntityCandidate)entCandidate1).getConnectedResourcesSubject().contains(entCandidate2.getUri())||
                            ((EntityCandidate)entCandidate1).getConnectedResourcesObject().contains(entCandidate2.getUri())){
                        soTupels.add(new SOTupel((EntityCandidate)entCandidate1,(EntityCandidate) entCandidate2));
                        mappedCoOccurences.addAll(Arrays.asList(entCandidate1.getCoOccurence().split(" ")));
                        mappedCoOccurences.addAll(Arrays.asList(entCandidate2.getCoOccurence().split(" ")));
                        //entCandidate1.setLinkingScore(entCandidate1.getLinkingScore()+1);
                        //entCandidate2.setLinkingScore(entCandidate2.getLinkingScore()+1);
                    }
                }

            }

        }
        spTupels=new ArrayList<>();
        poTupels=new ArrayList<>();
        for(ResourceCandidate propertyCandidate:propertyCandidates){
            for (ResourceCandidate resourceCandidate:entityCandidates){
                if(!coOccurenceContains(propertyCandidate.getCoOccurence(),resourceCandidate.getCoOccurence())){
                    if(((EntityCandidate)resourceCandidate).getConnectedPropertiesSubject().contains(propertyCandidate.getUri())){
                        //resourceCandidate.setLinkingScore(resourceCandidate.getLinkingScore()+1);
                        //propertyCandidate.setLinkingScore(propertyCandidate.getLinkingScore()+1);
                        spTupels.add(new SPTupel((EntityCandidate) resourceCandidate,(PropertyCandidate) propertyCandidate));
                        mappedCoOccurences.addAll(Arrays.asList(propertyCandidate.getCoOccurence().split(" ")));
                        mappedCoOccurences.addAll(Arrays.asList(resourceCandidate.getCoOccurence().split(" ")));
                    }
                    if(((EntityCandidate)resourceCandidate).getConnectedPropertiesObject().contains(propertyCandidate.getUri())){
                        //resourceCandidate.setLinkingScore(resourceCandidate.getLinkingScore()+1);
                        //propertyCandidate.setLinkingScore(propertyCandidate.getLinkingScore()+1);
                        poTupels.add(new POTupel((PropertyCandidate) propertyCandidate,(EntityCandidate) resourceCandidate));
                        mappedCoOccurences.addAll(Arrays.asList(propertyCandidate.getCoOccurence().split(" ")));
                        mappedCoOccurences.addAll(Arrays.asList(resourceCandidate.getCoOccurence().split(" ")));

                    }
                }
            }
        }
        mappedEntities=new ArrayList<>();
        entityCandidates.forEach(ent->{
            Set<String>substrings=new HashSet<>();
            substrings.addAll(Arrays.asList(ent.getCoOccurence().split(" ")));
            if(Sets.intersection(mappedCoOccurences,substrings).size()==0)
                mappedEntities.add(ent);
        });
        mappedProperties=new ArrayList<>();
        propertyCandidates.forEach(ent->{
            Set<String>substrings=new HashSet<>();
            substrings.addAll(Arrays.asList(ent.getCoOccurence().split(" ")));
            if(Sets.intersection(mappedCoOccurences,substrings).size()==0)
                mappedProperties.add(ent);
        });
        /*mappedEntities=new ArrayList<>();

        this.mappedEntities=filterByGroupAndLinkingScore(entityCandidates);
        this.mappedProperties=filterByGroupAndLinkingScore(propertyCandidates);*/
        this.mappedClasses=classCandidates;
        //this.mappedEntities=new ArrayList<>();
        //this.mappedProperties=new ArrayList<>();
    }

    private void filterTuples(){
        HashMap<String,List<SPTupel>>coOccurenceToSPTupel=new HashMap<>();
        HashMap<String,List<POTupel>>coOccurenceToPOTupel=new HashMap<>();
        HashMap<String,List<SOTupel>>coOccurenceToSOTupel=new HashMap<>();
        this.spTupels.forEach(sp->{
            if(coOccurenceToSPTupel.containsKey(sp.getSubject().getCoOccurence()))coOccurenceToSPTupel.get(sp.getSubject()).add(sp);
            else{
                ArrayList<SPTupel> l=new ArrayList<>();
                l.add(sp);
                coOccurenceToSPTupel.put(sp.getSubject().getCoOccurence(),l);
            }
            if(coOccurenceToSPTupel.containsKey(sp.getPredicate().getCoOccurence()))coOccurenceToSPTupel.get(sp.getPredicate()).add(sp);
            else{
                ArrayList<SPTupel> l=new ArrayList<>();
                l.add(sp);
                coOccurenceToSPTupel.put(sp.getPredicate().getCoOccurence(),l);
            }
        });


    }
    List<ResourceCandidate>filterByGroupAndLinkingScore(List<ResourceCandidate>candidates){
        HashMap<String,Double> maxLinkingScore=new HashMap<>();
        HashMap<String,List<ResourceCandidate>> coOccurenceToBestCandidate=new HashMap<>();
        for(ResourceCandidate candidate: candidates){
            String overlappingCoOccurence=getOverlappingCoOccurence(maxLinkingScore.keySet(),candidate.getCoOccurence());
            if(overlappingCoOccurence!=null){
                if (candidate.getLinkingScore() > maxLinkingScore.get(overlappingCoOccurence)) {
                    coOccurenceToBestCandidate.remove(overlappingCoOccurence);
                    maxLinkingScore.remove(overlappingCoOccurence);
                    maxLinkingScore.put(candidate.getCoOccurence(), candidate.getLinkingScore());
                    List<ResourceCandidate>cl=new ArrayList<>();
                    cl.add(candidate);
                    coOccurenceToBestCandidate.put(candidate.getCoOccurence(), cl);

                }
                else if(candidate.getLinkingScore()==(double)(maxLinkingScore.get(overlappingCoOccurence))){
                    coOccurenceToBestCandidate.get(overlappingCoOccurence).add(candidate);
                }
            }
            /*if(maxLinkingScore.containsKey(candidate.getCoOccurence())) {
                if (candidate.getLinkingScore() > maxLinkingScore.get(candidate.getGroup())) {
                    maxLinkingScore.put(candidate.getGroup(), candidate.getLinkingScore());
                    List<ResourceCandidate>cl=new ArrayList<>();
                    cl.add(candidate);
                    groupToBestCandidate.put(candidate.getGroup(), cl);
                }
                else if(candidate.getLinkingScore()==(double)(maxLinkingScore.get(candidate.getGroup()))){
                    groupToBestCandidate.get(candidate.getGroup()).add(candidate);
                }
            }*/

                else{
                    maxLinkingScore.put(candidate.getCoOccurence(),candidate.getLinkingScore());
                    List<ResourceCandidate>cl=new ArrayList<>();
                    cl.add(candidate);
                    coOccurenceToBestCandidate.put(candidate.getCoOccurence(),cl);
                }

        }
        List<ResourceCandidate>mappedResources=new ArrayList<>();
        coOccurenceToBestCandidate.keySet().forEach(s->mappedResources.addAll(coOccurenceToBestCandidate.get(s)));
        return mappedResources;

    }
}

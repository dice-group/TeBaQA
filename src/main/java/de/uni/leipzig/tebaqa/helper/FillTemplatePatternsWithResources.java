package de.uni.leipzig.tebaqa.helper;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import de.uni.leipzig.tebaqa.controller.ElasticSearchEntityIndex;
import de.uni.leipzig.tebaqa.controller.SemanticAnalysisHelper;
import de.uni.leipzig.tebaqa.model.*;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;

import java.io.IOException;
import java.util.*;

import static de.uni.leipzig.tebaqa.helper.QueryMappingFactoryLabels.getNeighborCoOccurrencePermutations;

public class FillTemplatePatternsWithResources {
    SemanticAnalysisHelper semanticAnalysisHelper;
    ElasticSearchEntityIndex index;
    WordsGenerator wordsGenerator;
    List<Triple>candidateTriples;
    List<ResourceCandidate>entityCandidates;
    List<ResourceCandidate>propertyCandidates;
    List<ResourceCandidate>classCandidates;
    List<ResourceCandidate>literalCandidates;
    List<TripleTemplate>templates;
    Set<String>propertyUris;
    List<String> coOccurrences;
    SemanticGraph semanticGraph;
    private static double MIN_SCORE_NORMAL = 0.32;
    private static double MIN_SCORE_WITH_NUMBERS = 0.05;

    public FillTemplatePatternsWithResources(List<TripleTemplate>templates,SemanticAnalysisHelper semanticAnalysisHelper){
        this.semanticAnalysisHelper=semanticAnalysisHelper;
        this.candidateTriples=new ArrayList<>();
        this.entityCandidates=new ArrayList<>();
        this.propertyCandidates=new ArrayList<>();
        this.literalCandidates=new ArrayList<>();
        this.classCandidates=new ArrayList<>();
        this.templates=templates;
        wordsGenerator=new WordsGenerator();
        try {
            index=new ElasticSearchEntityIndex();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public FillTemplatePatternsWithResources(SemanticAnalysisHelper semanticAnalysisHelper){
        this.semanticAnalysisHelper=semanticAnalysisHelper;
        this.candidateTriples=new ArrayList<>();
        this.entityCandidates=new ArrayList<>();
        this.propertyCandidates=new ArrayList<>();
        this.classCandidates=new ArrayList<>();
        this.literalCandidates=new ArrayList<>();
        wordsGenerator=new WordsGenerator();
        this.templates=new ArrayList<>();
        this.templates.add(new TripleTemplate("r_r_v"));
        this.templates.add(new TripleTemplate("v_r_r"));
        this.templates.add(new TripleTemplate("v_r_v"));
        this.templates.add(new TripleTemplate("r_r_r"));
        this.templates.add(new TripleTemplate("r_r_lit"));
        this.templates.add(new TripleTemplate("v_r_lit"));
        try {
            index=new ElasticSearchEntityIndex();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    boolean coOccurenceContains(String coOccurence1,String coOccurence2){
        return coOccurence1.contains(coOccurence2)||coOccurence2.contains(coOccurence1);
    }
    private String getOverlappingCoOccurence(Set<String> scoredCoOccurences, String coOccurence){
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
        int diff=maxArray.length-minArray.length;

        for(String word1:minArray) {
            if (!word1.contains("(")){
                double min = 1;
                for (String word2 : maxArray) {
                    double levenstheinscore = Utilities.getLevenshteinRatio(word1, word2);
                    if (levenstheinscore < min) min = levenstheinscore;
                }
                sum += min;
            }
        }
        /*for(String word1:minArray) {
                double min = 1;
                int bestIndex=-1;
                for (String word2 : maxArray) {
                    if (!word2.contains("(")){
                    double levenstheinscore = Utilities.getLevenshteinRatio(word1, word2);
                    if (levenstheinscore < min){
                        min = levenstheinscore;
                        bestIndex=word2.indexOf(word2);
                    }
                }
                maxArray[bestIndex]="";
                sum += min;
            }
        }*/
        return (sum/maxArray.length)+diff*0.1;

    }
    private Set<ResourceCandidate> getbestResourcesByLevenstheinRatio(String coOccurrence, String type,boolean searchSynonyms,String typeFilter){
        Set<ResourceCandidate> foundResources;
        if(type.equals("entity")&&typeFilter!=null)
            foundResources = index.searchEntity(coOccurrence, Optional.empty(),Optional.empty(),Optional.of(typeFilter));
        else if(type.equals("entity"))
            foundResources = index.searchEntity(coOccurrence, Optional.empty(),Optional.empty(),Optional.empty());

        else if(type.equals("property"))
            foundResources=index.searchResource(coOccurrence,"property",searchSynonyms);
        else {
            if(!coOccurrence.contains(" ")) {
                Map<String, String> lemmas = semanticAnalysisHelper.getLemmas(coOccurrence.replace("'s",""));
                String lem=lemmas.get(coOccurrence.replace("'s",""));
                if(lem!=null)coOccurrence=lem;
            }
            foundResources = index.searchResource(coOccurrence, "class", false);
        }
        Set<ResourceCandidate> bestResourcesByLevenstheinRatio = getbestResourcesByLevenstheinRatio(coOccurrence, foundResources, type,searchSynonyms);
        //bestResourcesByLevenstheinRatio.forEach(c->c.setGroup(group));
        //if(foundResources.size()==100)return foundResources;
        return bestResourcesByLevenstheinRatio;

    }
    private Set<ResourceCandidate>getbestResourcesByLevenstheinRatio(String coOccurence,Set<ResourceCandidate>foundResources,String type,boolean syn){
        Set<ResourceCandidate> bestResourcesByLevenstheinRatio = new HashSet<>();
//        double minScore = 0.25;
        for (ResourceCandidate resource : foundResources) {
            double ratio=1;
            String bestMatchedLabel=null;
            for(String label:resource.getResourceString()) {
                //double ratiotemp = Utilities.getLevenshteinRatio(coOccurence, label);
                double ratiotemp;
                if(resource instanceof PropertyCandidate &&!syn)
                    ratiotemp=calculatedAverageLevenstheinRatioByWord(coOccurence,label);
                else ratiotemp = Utilities.getLevenshteinRatio(coOccurence, label);
                if(ratiotemp < ratio) {
                    ratio = ratiotemp;
                    bestMatchedLabel = label;
                    double numberOfWords=(TextUtilities.countWords(coOccurence));
                    double relFactor = (numberOfWords - (2*ratio*numberOfWords));
                    //double relFactor = 1- ratio;
                    resource.setRelatednessFactor(relFactor);
                }
            }
            if (ratio <= getLevensteinThreshold(coOccurence, bestMatchedLabel)) {
                resource.setCoOccurence(coOccurence);
                //minScore = ratio;
                resource.setLevenstheinScore(ratio);
                //bestResourcesByLevenstheinRatio.clear();
                bestResourcesByLevenstheinRatio.add(resource);
            }
        }
        if(bestResourcesByLevenstheinRatio.size()==0&&foundResources.size()==100&&type.equals("entity"))
            return foundResources;
        return bestResourcesByLevenstheinRatio;
    }

    private static double getLevensteinThreshold(String coOccurence, String matched)
    {
        // If the co-occurence contains a number, then there should be high similarity. This helps in reducing matched
        // entities in cases like Gleis 1 or LSA 460 where there is a high chance of getting many matches.
        if(matched != null && (coOccurence.matches(".*\\d+.*") || matched.matches(".*\\d+.*")))
        {
            return MIN_SCORE_WITH_NUMBERS;
        }

        return MIN_SCORE_NORMAL;
    }

    private Set<ResourceCandidate>disambiguateAmbiqueEntity(String coOccurence,Optional<String>type,List<ResourceCandidate>entityCandidates,List<ResourceCandidate>propertyCandidates){
        HashMap<String,ResourceCandidate> candidates=new HashMap<>();
        if(!coOccurence.contains(" ")){
            Set<ResourceCandidate> cs = index.searchEntityWithTypeFilter(coOccurence,"http://dbpedia.org/ontology/Person",100);
            cs.forEach(c-> {if(!candidates.containsKey(c.getUri())) candidates.put(c.getUri(),c);});
        }
        if(!coOccurence.contains(" ")){
            Set<ResourceCandidate> cs = index.searchEntityWithTypeFilter(coOccurence,"http://dbpedia.org/ontology/Person",100);
            cs.forEach(c-> {if(!candidates.containsKey(c.getUri())) candidates.put(c.getUri(),c);});
        }
        if(candidates.size()>=100||candidates.size()==0) {
            candidates.clear();
            for (ResourceCandidate enitityCandidate : entityCandidates) {
                Set<ResourceCandidate> cs = index.searchEntity(coOccurence, Optional.of(enitityCandidate.getUri()), Optional.empty(), type);
                cs.forEach(c -> {
                    if (!candidates.containsKey(c.getUri())) candidates.put(c.getUri(), c);
                });
            }
            for (ResourceCandidate propertyCandidate : propertyCandidates) {
                Set<ResourceCandidate> cs = index.searchEntity(coOccurence, Optional.empty(), Optional.of(propertyCandidate.getUri()), Optional.empty());
                cs.forEach(c -> {
                    if (!candidates.containsKey(c.getUri())) candidates.put(c.getUri(), c);
                });
            }
        }
        Set<ResourceCandidate> bestResourcesByLevenstheinRatio=getbestResourcesByLevenstheinRatio(coOccurence, Sets.newHashSet(candidates.values()),"entity",false);
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
        String[]words=coOccurence.replace("’s","").split("\\s+");
        if(words.length==1||words.length==2||coOccurence.contains(" by"))return true;
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
            if(!isdependant&&words[i].length()>1&&!words[i].equals("by"))depend=false;
        }
        return depend;
    }
    private List<ResourceCandidate>findProperties(List<String>uris,List<String>coOccurences){
        List<String>notFound=new ArrayList<>();
        List<ResourceCandidate>resourceCandidates=new ArrayList<>();
        for(String uri:uris){
            ResourceCandidate cand = getByURI(uri,propertyCandidates);
            if(cand!=null)resourceCandidates.add(cand);
            else notFound.add(uri);
        }
        int max= notFound.size();
        int current=0;
        while(max>current+10) {
            resourceCandidates.addAll(index.searchPropertiesById(notFound.subList(current, current + 10)));
            current+=10;
        }
        resourceCandidates.addAll(index.searchPropertiesById(notFound.subList(current,notFound.size())));
        List<ResourceCandidate>resourceCandidatesFiltered=new ArrayList<>();
        double minScore = 0.2;
        for (ResourceCandidate resource : resourceCandidates) {
            double ratio=1;
            for(String label:resource.getResourceString()) {
                //double ratiotemp = Utilities.getLevenshteinRatio(coOccurence, label);
                double ratiotemp=1;
                if(resource instanceof PropertyCandidate) {
                    for (String coOccurence : coOccurences) {
                        ratiotemp = calculatedAverageLevenstheinRatioByWord(coOccurence, label.toLowerCase());
                        if (ratiotemp < ratio) {
                            ratio = ratiotemp;
                            double relFactor = TextUtilities.countWords(coOccurence) - ratio;
                            resource.setRelatednessFactor(relFactor);
                            resource.setCoOccurence(coOccurence);
                        }
                    }
                }
            }
            if (ratio <= minScore) {
                resourceCandidatesFiltered.add(resource);

                //minScore = ratio;
                resource.setLevenstheinScore(ratio);
                if(!containsByURI(resource.getUri(),propertyCandidates))propertyCandidates.add(resource);
                //bestResourcesByLevenstheinRatio.clear();

            }
        }
        //avoid Gb propblem
        resourceCandidates.clear();
        return resourceCandidatesFiltered;
    }
    public void extractEntities(String question) {
        question = semanticAnalysisHelper.removeQuestionWords(question);
        question=question.replace("(","");
        question=question.replace(")","");
        semanticGraph=semanticAnalysisHelper.extractDependencyGraph(question);
        String[] wordsFromQuestion = question.replaceAll("[\\-.?¿!,;]", "").split("\\s+");
        //List<CooccurenceGroup> coOccurrences = getNeighborCoOccurrencePermutationsGroups(wordsFromQuestion);
        coOccurrences = getNeighborCoOccurrencePermutations(Arrays.asList(wordsFromQuestion));
        coOccurrences.sort((s1, s2) -> -(s1.length() - s2.length()));
        //List<ResourceCandidate> entityCandidates = new ArrayList<ResourceCandidate>();
        HashMap<String,List<ResourceCandidate>> ambiqueResourceCandidates = new HashMap<>();
        //List<ResourceCandidate> propertyCandidates = new ArrayList<ResourceCandidate>();
        //List<ResourceCandidate> propertySynonymCandidates = new ArrayList<ResourceCandidate>();
        //List<ResourceCandidate> ambiquePropertyCandidates = new ArrayList<ResourceCandidate>();
        //List<ResourceCandidate> classCandidates = new ArrayList<ResourceCandidate>();
        List<String> dependantCoOccurrences=new ArrayList<>();
        if(semanticGraph!=null) {
            for (String coOccurrence : coOccurrences) {
                if (!wordsGenerator.containsOnlyStopwords(coOccurrence,"en")&&isdependant(coOccurrence, semanticGraph))
                    dependantCoOccurrences.add(coOccurrence);
            }
            coOccurrences=dependantCoOccurrences;
        }
        coOccurrences.sort((s1, s2) -> -(s1.length() - s2.length()));
        //List<ResourceCandidate> ambiqueClassCandidates = new ArrayList<ResourceCandidate>();
        propertyUris=new HashSet<>();

        for(String coOccurence:coOccurrences) {
            literalCandidates.addAll(index.searchLiteral(coOccurence,100));
            Set<ResourceCandidate> best = getbestResourcesByLevenstheinRatio( coOccurence, "entity",false,null);
            best.forEach(cand->{
                propertyUris.addAll(((EntityCandidate)cand).getConnectedPropertiesSubject());
                propertyUris.addAll(((EntityCandidate)cand).getConnectedPropertiesObject());
            });

            if (best.size() > 0 && best.size() <= 20) {
                entityCandidates.addAll(best);
            } else if (best.size() > 20) {
                List<ResourceCandidate> candidates = new ArrayList<>();
                candidates.addAll(best);
                ambiqueResourceCandidates.put(coOccurence, candidates);
            }
            //search for Countries
            best = getbestResourcesByLevenstheinRatio( coOccurence, "entity",false,"http://dbpedia.org/ontology/Country");
            if(best.size()<100) {
                best.forEach(cand ->{
                    propertyUris.addAll(((EntityCandidate) cand).getConnectedPropertiesSubject());
                    propertyUris.addAll(((EntityCandidate) cand).getConnectedPropertiesObject());});
                entityCandidates.addAll(best);
            }
            best = getbestResourcesByLevenstheinRatio( coOccurence, "property",true,null);
            propertyCandidates.addAll(best);
            best = getbestResourcesByLevenstheinRatio( coOccurence, "class",false,null);
            classCandidates.addAll(best);

        }

        //propertyCandidates.addAll(findProperties(Lists.newArrayList(propertyUris),coOccurrences));
        Set<String>ambCoOccurences=new HashSet<>();

        //Disambiguate Ambique Entities
        ambiqueResourceCandidates.keySet().forEach(key->ambCoOccurences.add(key));
        for(String coOccurence:ambCoOccurences){
            Set<ResourceCandidate> best = disambiguateAmbiqueEntity(coOccurence,getCommonType(ambiqueResourceCandidates.get(coOccurence)),
                    entityCandidates,propertyCandidates);
            if (best.size() > 0 && best.size() <= 10){
                entityCandidates.addAll(best);
                best.forEach(cand->propertyUris.addAll(((EntityCandidate)cand).getConnectedPropertiesSubject()));
                best.forEach(cand->propertyUris.addAll(((EntityCandidate)cand).getConnectedPropertiesObject()));
                ambiqueResourceCandidates.remove(coOccurence);
            }
            else{
                int max=0;
                ResourceCandidate mostPopular=null;
                for(ResourceCandidate c:ambiqueResourceCandidates.get(coOccurence)){
                    int m=((EntityCandidate)c).getConnectedResourcesSubject().size()+
                            ((EntityCandidate)c).getConnectedResourcesObject().size();
                    if(m>max){
                        max=m;
                        mostPopular=c;
                    }
                }
                if(mostPopular!=null&&mostPopular.getCoOccurence()!=null)
                entityCandidates.add(mostPopular);
            }

        }

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

    public int getPopularity(Set<String> resources){
        int popularity=0;
        for (String res:resources){
            if (containsByURI(res, entityCandidates)) {
                EntityCandidate ent = (EntityCandidate) getByURI(res, entityCandidates);
                popularity += ent.getConnectedPropertiesSubject().size() + ent.getConnectedPropertiesObject().size();
            }
            popularity += 0;
        }
        return popularity;
    }
    boolean depend(String word1,String word2, SemanticGraph g){

        List<IndexedWord> words1=g.getAllNodesByWordPattern(word1);
        boolean isdependant = false;
            for(int j=0;j<words1.size();j++) {
                List<IndexedWord> words2=g.getAllNodesByWordPattern(word2);
                for(int k=0;k<words1.size()&&!isdependant;k++){
                        for(int l=0;l<words2.size()&&!isdependant;l++){
                            if(g.containsEdge(words1.get(k),words2.get(l))||g.containsEdge(words2.get(l),words1.get(k)))
                                return true;
                        }
                    }


            }

        return false;
    }
    public int getNumberOfDependantResourceCombinations(List<String[]>connResources){
        int numberOfDepResources=0;

        for(String[]connResource:connResources) {
            if(containsByURI(connResource[0], entityCandidates)&&!connResource[1].equals("a")&&!connResource[1].equals("country_prop")
                    &&!connResource[1].equals("http://purl.org/dc/terms/subject")) {
                ResourceCandidate ent = getByURI(connResource[0], entityCandidates);
                ResourceCandidate pred = getByURI(connResource[1], propertyCandidates);
                if(pred==null)
                    System.out.println();
                String[] entWords = ent.getCoOccurence().split(" ");

                String[] predWords = pred.getCoOccurence().split(" ");

                for(String entWord:entWords){
                    for(String predWord:predWords){
                        if(depend(entWord,predWord,semanticGraph))
                            numberOfDepResources++;
                    }
                }
            }
        }
        return numberOfDepResources;
    }
    double getAverageRelatednessScore(Set<String>uris){
        List<ResourceCandidate>resources=new ArrayList<>();
        for(String uri:uris){
            ResourceCandidate cr= getByURI(uri,entityCandidates);
            if(cr!=null)resources.add(cr);
            ResourceCandidate cp= getByURI(uri,propertyCandidates);
            if(cp!=null)resources.add(cp);
            ResourceCandidate cc= getByURI(uri,classCandidates);
            if(cc!=null)resources.add(cc);
        }
        double score=0;
        for(ResourceCandidate c:resources)
            score+=c.getRelatednessFactor();
        return score/resources.size();
    }
    public boolean acceptByCooccurence(List<String>uris){
        List<ResourceCandidate>resources=new ArrayList<>();
        for(String uri:uris){
            ResourceCandidate cr= getByURI(uri,entityCandidates);
            if(cr!=null)resources.add(cr);
            ResourceCandidate cp= getByURI(uri,propertyCandidates);
            if(cp!=null)resources.add(cp);
            ResourceCandidate cc= getByURI(uri,classCandidates);
            if(cc!=null)resources.add(cc);
        }
        List<String>mappedCooccurences=new ArrayList<>();
        for(ResourceCandidate rc:resources){
            for(String co:mappedCooccurences)
            if(coOccurenceContains(rc.getCoOccurence(),co))return false;
            mappedCooccurences.add(rc.getCoOccurence());
        }
        return true;
    }
    private boolean containsByURI(String uri,List<ResourceCandidate> candidates){
        for(ResourceCandidate cand:candidates) {
            if (cand.getUri().equals(uri)) return true;
        }
        return false;

    }
    public ResourceCandidate getByURI(String uri,List<ResourceCandidate> candidates){
        for(ResourceCandidate cand:candidates) {
            if (cand.getUri().equals(uri)) return cand;
        }
        return null;

    }
    public List<Triple> generateTuplesWithTwoResources(TripleTemplate template, List<ResourceCandidate> entityCandidates,
                                                        List<ResourceCandidate> propertyCandidates){
        List<Triple>triples=new ArrayList<>();
        if(template.getSubject().equals("v")){
            entityCandidates.forEach(ec->{
                ((EntityCandidate)ec).getConnectedPropertiesObject().forEach(prop->{
                    if(containsByURI(prop,propertyCandidates)
                            &&!coOccurenceContains(ec.getCoOccurence(),getByURI(prop,propertyCandidates).getCoOccurence())) {
                        Triple t=new Triple("var", prop, ec.getUri());
                        if(!tripleContains(triples,t))
                            triples.add(t);
                    }
                });

            });
        }
        if(template.getObject().equals("v")){
            entityCandidates.forEach(ec->{
                ((EntityCandidate)ec).getConnectedPropertiesSubject().forEach(prop->{
                    if(containsByURI(prop,propertyCandidates)
                    &&!coOccurenceContains(ec.getCoOccurence(),getByURI(prop,propertyCandidates).getCoOccurence())) {
                        Triple t=new Triple(ec.getUri(), prop, "var");
                        if(!tripleContains(triples,t)) triples.add(t);
                    }
                });

            });
        }

        return triples;
    }

    public List<Triple> generateTuplesWithTwoVariables(Triple alreadyKnownTriple,TripleTemplate template) {
        List<Triple> triplesFound;

        if(TripleTemplate.Pattern.V_R_V.equals(template.getPattern())) {
            triplesFound = twoVariablesVRV(alreadyKnownTriple, template);
        } else if(TripleTemplate.Pattern.V_V_R.equals(template.getPattern())) {
            triplesFound = twoVariablesVVR(alreadyKnownTriple, template);
        } else {
            triplesFound = new ArrayList<>();
        }

        return triplesFound;
    }

    private List<Triple> twoVariablesVVR(Triple alreadyKnownTriple, TripleTemplate template) {
        Set<Triple> triplesFound = new HashSet<>();

//        if(alreadyKnownTriple.isPredicateRDFTypeProperty()){
        for(ResourceCandidate ec : entityCandidates)
        {
            // Check that entityCandidate is not used to replace a placeholder in already known triple
            if(!alreadyKnownTriple.getObject().equals(ec.getUri()))
            {
                Triple t = new Triple(template.getSubject(), template.getPredicate(), ec.getUri());
                triplesFound.add(t);
            }
        }
//        }
        return new ArrayList<>(triplesFound);
    }

    private List<Triple> twoVariablesVRV(Triple alreadyKnownTriple, TripleTemplate template) {
        Set<String> relevantResourceCandidates = new HashSet<>();

        //for(Triple triple:alreadyKnownTriples){
        if (alreadyKnownTriple.getSubject().startsWith("http")) {
            EntityCandidate ent = (EntityCandidate) getByURI(alreadyKnownTriple.getSubject(), entityCandidates);
            relevantResourceCandidates.addAll(ent.getConnectedResourcesSubject());
        } else if (alreadyKnownTriple.getObject().startsWith("http")) {
            EntityCandidate ent = (EntityCandidate) getByURI(alreadyKnownTriple.getObject(), entityCandidates);
            if (ent !=null) relevantResourceCandidates.addAll(ent.getConnectedResourcesObject());
        }
        //}
        List<String> uris = Lists.newArrayList(relevantResourceCandidates);
        List<ResourceCandidate> cands = new ArrayList<>();
        int max = uris.size();

        int current = 0;
        while (max > current + 10&&current<1000) {
            cands.addAll(index.searchEntitiesById(uris.subList(current, current + 10)));
            current += 10;
        }
        if(max<1000)
            cands.addAll(index.searchEntitiesById(uris.subList(current, max)));

        List<Triple> triplesFound = new ArrayList<>();
        if (template.getSubject().equals(alreadyKnownTriple.getSubject())||
                template.getSubject().equals(alreadyKnownTriple.getObject())){
            Set relevantPropertiesCandidate = new HashSet();
            cands.forEach(cand -> relevantPropertiesCandidate.addAll(((EntityCandidate) cand).getConnectedPropertiesSubject()));
            List<ResourceCandidate> properties = findProperties(Lists.newArrayList(relevantPropertiesCandidate), coOccurrences);
            properties.forEach(prop -> {
                Triple t = new Triple(template.getSubject(), prop.getUri(), "var");
                if (!tripleContains(triplesFound, t)) triplesFound.add(t);
            });
            //propertyCandidates.addAll(properties);
        }
        else {
            Set relevantPropertiesCandidate = new HashSet();
            cands.forEach(cand -> relevantPropertiesCandidate.addAll(((EntityCandidate) cand).getConnectedPropertiesObject()));
            List<ResourceCandidate> properties = findProperties(Lists.newArrayList(relevantPropertiesCandidate), coOccurrences);

            //propertyCandidates.addAll(properties);
            properties.forEach(prop -> {
                Triple t = new Triple("var", prop.getUri(), template.getObject());
                if (!tripleContains(triplesFound, t)) triplesFound.add(t);
            });
        }
        return triplesFound;
    }


    private boolean tripleContains(List<Triple>triples,Triple triple){
        for(Triple t:triples){
            if(triple.getSubject().equals(t.getSubject())&&
            triple.getPredicate().equals(t.getPredicate())&&
            triple.getObject().equals(t.getObject()))
                return true;
        }
        return false;
    }
    public List<Triple> generateTypePropertyTriples(Triple alreadyKnownTriple,TripleTemplate template){
        List<Triple>triples=new ArrayList<>();
        //List<ResourceCandidate>entityCandidates=new ArrayList<>();
        int currindex=0;

            for(ResourceCandidate prop:propertyCandidates){
                Set<ResourceCandidate>entityCandidates = index.searchByType(Optional.empty(),Optional.of(prop.getUri()),Optional.of(alreadyKnownTriple.getObject()),100);
                if (template.getSubject().equals(alreadyKnownTriple.getSubject())||
                        template.getSubject().equals(alreadyKnownTriple.getObject())){
                    entityCandidates.forEach(cand -> {
                        if (((EntityCandidate) cand).getConnectedPropertiesSubject().contains(prop.getUri())) {
                            Triple t = new Triple(template.getSubject(), prop.getUri(), template.getObject());
                            if (!tripleContains(triples, t))
                                triples.add(t);
                        }
                    });
                }else{
                    entityCandidates.forEach(cand -> {
                        if (((EntityCandidate) cand).getConnectedPropertiesObject().contains(prop.getUri())) {
                            Triple t = new Triple(template.getSubject(), prop.getUri(), template.getObject());
                            if (!tripleContains(triples, t))
                                triples.add(t);
                        }
                    });
                }
            }


        return triples;
    }
    public List<Triple> generateTypeTriplesEntity(){
        List<Triple>triples=new ArrayList<>();
        //List<ResourceCandidate>entityCandidates=new ArrayList<>();
        int currindex=0;
        for(ResourceCandidate classCand:classCandidates){
            for(ResourceCandidate ent:propertyCandidates){
                Set<ResourceCandidate>entityCandidates = index.searchByType(Optional.of(ent.getUri()),Optional.empty(),Optional.of(classCand.getUri()),100);
                entityCandidates.forEach(cand ->{
                    Triple tClass =new Triple("varType_"+currindex,"a",classCand.getUri());
                    if(!tripleContains(triples,tClass))triples.add(tClass);
                    if(((EntityCandidate)cand).getConnectedResourcesSubject().contains(ent)) {
                        Triple t = new Triple("varType_" + currindex,"propvar" , ent.getUri());
                        if (!tripleContains(triples,t))
                            triples.add(t);
                    }
                    else {
                        Triple t = new Triple(ent.getUri(), "propVar", "varType" + currindex);
                        if (!tripleContains(triples,t))
                            triples.add(t);
                    }
                });
            }

        }
        return triples;
    }
    public List<Triple>generateSingleTypeTriples(){
        List<Triple>triples=new ArrayList<>();
        for(ResourceCandidate type:classCandidates){
            triples.add(new Triple("var","a",type.getUri()));
        }
        return triples;
    }
    public List<Triple>generateEntityObjectRestictions(){
        List<Triple>triples=new ArrayList<>();
        for(ResourceCandidate entCand:entityCandidates){
            for(String uri:((EntityCandidate)entCand).getConnectedResourcesSubject()) {
                ArrayList<ResourceCandidate> cands = Lists.newArrayList((index.searchEntitiesById(Lists.newArrayList(uri))));
                if(cands.size()>0){
                    Triple t = new Triple(entCand.getUri(), "varProp", "varMapped");
                    for (ResourceCandidate propCand : propertyCandidates) {
                        if (((EntityCandidate) cands.get(0)).getConnectedPropertiesSubject().contains(propCand.getUri())) {
                            if (!tripleContains(triples, t)) triples.add(t);
                            Triple t2 = new Triple("varMapped", propCand.getUri(), "unknown");
                            if (!tripleContains(triples, t2)) triples.add(t2);
                        }
                        if (((EntityCandidate) cands.get(0)).getConnectedPropertiesObject().contains(propCand.getUri())) {
                            if (!tripleContains(triples, t)) triples.add(t);
                            Triple t2 = new Triple("unknown", propCand.getUri(), "varMapped");
                            if (!tripleContains(triples, t2)) triples.add(t2);
                        }
                    }
                }


            }
            for(String uri:((EntityCandidate)entCand).getConnectedResourcesObject()) {
                ArrayList<ResourceCandidate> cands = Lists.newArrayList((index.searchEntitiesById(Lists.newArrayList(uri))));
                if(cands.size()>0){
                    Triple t = new Triple("varMapped", "varProp", entCand.getUri());
                    for (ResourceCandidate propCand : propertyCandidates) {
                        if (((EntityCandidate) cands.get(0)).getConnectedPropertiesSubject().contains(propCand.getUri())) {
                            if (!tripleContains(triples, t)) triples.add(t);
                            Triple t2 = new Triple("varMapped", propCand.getUri(), "unknown");
                            if (!tripleContains(triples, t2)) triples.add(t2);
                        }

                    }
                }


            }
        }
        return triples;
    }
    public List<Triple>getCountryTriples(){
        List<Triple>triples=new ArrayList<>();
        for(ResourceCandidate cand:entityCandidates){
            if(((EntityCandidate)cand).getTypes().contains("http://dbpedia.org/ontology/Country"))
                triples.add(new Triple("var","country_prop",cand.getUri()));
        }
        return triples;
    }
    public List<Triple>getCategoryTriples(){
        List<Triple>triples=new ArrayList<>();
        for(ResourceCandidate cand:entityCandidates){
            if(((EntityCandidate)cand).getTypes().contains("http://www.w3.org/2004/02/skos/core#Concept"))
                triples.add(new Triple("var","http://purl.org/dc/terms/subject",cand.getUri()));
        }
        return triples;
    }
    public List<Triple> generateSingleTriples(Set<String> templates){
        List<ResourceCandidate>candidatesCurrent=findProperties(Lists.newArrayList(propertyUris),coOccurrences);
        List<Triple>triples=new ArrayList<>();
        templates.forEach(t->triples.addAll(generateTuplesWithTwoResources(new TripleTemplate(t),entityCandidates,candidatesCurrent)));
        return triples;
    }
}

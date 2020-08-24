package de.uni.leipzig.tebaqa.helper;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import de.uni.leipzig.tebaqa.model.*;
import joptsimple.internal.Strings;
import org.aksw.qa.commons.nlp.nerd.Spotlight;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.binding.BindingMap;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementVisitorBase;
import org.apache.jena.sparql.syntax.ElementWalker;
import org.apache.log4j.Logger;
import org.semarglproject.vocab.RDF;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Utilities {

    public static Pattern BETWEEN_CIRCUMFLEX = Pattern.compile("\\^(.*?)\\^");
    public static Pattern SPARQL_VARIABLE = Pattern.compile("\\?\\w+");
    public static Pattern BETWEEN_LACE_BRACES = Pattern.compile("<(.*?)>");
    public static Pattern BETWEEN_CURLY_BRACES = Pattern.compile("\\{(.*?)\\}");
    public static Pattern ARGUMENTS_BETWEEN_SPACES = Pattern.compile("\\S+");

    private static Logger log = Logger.getLogger(Utilities.class);

    /**
     * Use reflection to create a Spotlight instance with a given URL.
     * This is a workaround because the API from {@link Spotlight} is down.
     *
     * @param url The URL of the Spotlight instance.
     * @return A {@link Spotlight} instance with a custom URL.
     */
    public static Spotlight createCustomSpotlightInstance(String url) {
        Class<?> clazz = Spotlight.class;
        Spotlight spotlight;
        try {
            spotlight = (Spotlight) clazz.newInstance();
            Field requestURLField = spotlight.getClass().getDeclaredField("requestURL");
            requestURLField.setAccessible(true);
            requestURLField.set(spotlight, url);
        } catch (InstantiationException | IllegalAccessException | NoSuchFieldException e) {
            spotlight = new Spotlight();
            log.error("Unable to change the Spotlight API URL using reflection. Using it's default value.", e);
        }
        return spotlight;
    }

    public static List<String> extractTriples(String query) {
        List<String> triples = new ArrayList<>();
        Matcher curlyBracesMatcher = BETWEEN_CURLY_BRACES.matcher(query);
        while (curlyBracesMatcher.find()) {
            String group = curlyBracesMatcher.group().replace("{", "").replace("}", "");
            int lastFoundTripleIndex = 0;
            for (int i = 0; i < group.length(); i++) {
                char currentChar = group.charAt(i);
                //TODO handle triples with predicate-object lists and object lists; see: https://stackoverflow.com/a/18214862/2514164
                if (Character.compare(currentChar, '.') == 0
                        || Character.compare(currentChar, ';') == 0
                        || Character.compare(currentChar, ',') == 0) {
                    String possibleTriple = group.substring(lastFoundTripleIndex, i);
                    int openingAngleBrackets = StringUtils.countMatches(possibleTriple, "<");
                    int closingAngleBrackets = StringUtils.countMatches(possibleTriple, ">");
                    if (openingAngleBrackets == closingAngleBrackets) {
                        lastFoundTripleIndex = i;
                        String t = removeDotsFromStartAndEnd(possibleTriple);
                        t = fixCompoundPattern(t, triples);
                        if (!t.isEmpty()) {
                            triples.add(t);
                        }
                    }
                }
            }
            //Add the last triple which may not end with a .
            String t = removeDotsFromStartAndEnd(group.substring(lastFoundTripleIndex, group.length()));
            t = fixCompoundPattern(t, triples);
            if (!t.isEmpty()) {
                triples.add(t);
            }
        }

        return triples;
    }

    private static String fixCompoundPattern(String currentPattern, List<String> previousTriples)
    {
        // To handle compound patterns which express two patterns with same subject with a semicolon
        // e.g. ?cs res/0 res/1 ; ?rel res/2 ; res/3 ?tel
        if(currentPattern.startsWith(";") && previousTriples.size() > 0)
        {
            String previousTriple = previousTriples.get(previousTriples.size()-1); // Get previous triple to retrieve subject from it
            String previousTripleSubject = previousTriple.split("\\s{1,}")[0];

            // Replace leading semicolon in current triple with previous triple's subject
            currentPattern = previousTripleSubject + currentPattern.substring(1);
        }

        return currentPattern;
    }

    private static String determinePatternStyle(TripleTemplate pattern){
        String style = "sub_pred_obj";

        String subject = "v";
        String predicate = "v";
        String object = "v";

        if(pattern.isSubjectAResource())
            subject = "r";
        if(pattern.isPredicateAResource())
            predicate = "r";
        if(pattern.isObjectAResource())
            object = "r";

        return String.format("%s_%s_%s", subject, predicate, object);
    }

    private static String removeDotsFromStartAndEnd(String possibleTriple) {
        possibleTriple = possibleTriple.trim();
        if (!possibleTriple.isEmpty()) {
            if (Character.compare(possibleTriple.charAt(0), '.') == 0) {
                possibleTriple = possibleTriple.substring(1, possibleTriple.length()).trim();
            }
            if (!possibleTriple.isEmpty() && Character.compare(possibleTriple.charAt(possibleTriple.length() - 1), '.') == 0) {
                possibleTriple = possibleTriple.substring(0, possibleTriple.length() - 1).trim();
            }
        }
        return possibleTriple;
    }

    public static boolean isEven(int x) {
        return (x % 2) == 0;
    }

    private static ResourceCandidate[] initTupel(int ind1,int ind2,ResourceCandidate val1,ResourceCandidate val2,int size){
        ResourceCandidate[] mapping = new ResourceCandidate[size];
        mapping[ind1] = val1;
        mapping[ind2] = val2;
        return mapping;
    }
    private static List<ResourceCandidate[]>generateVarTupelMappings(List<VarTupel>varTupels,ResourceLinker resourceLinker,int size){
        List<ResourceCandidate[]>varMappings=new ArrayList<>();
        HashMap<ResourceCandidate[],Set<SPTupel>>alreadyMappedSPs=new HashMap<>();
        HashMap<ResourceCandidate[],Set<POTupel>>alreadyMappedPOs=new HashMap<>();
        HashMap<ResourceCandidate[],Set<SOTupel>>alreadyMappedSOs=new HashMap<>();
        Boolean init=true;
        for(VarTupel varTupel:varTupels){
            int ind1=Integer.parseInt(varTupel.getVar1().substring(varTupel.getVar1().indexOf("_")+1));
            int ind2=Integer.parseInt(varTupel.getVar2().substring(varTupel.getVar1().indexOf("_")+1));
            if(varTupel.getType().equals("sp")) {
                for(SPTupel spTupel:resourceLinker.spTupels){
                    if(init) {
                        ResourceCandidate[] mapping=null;
                        mapping = initTupel(ind1, ind2, spTupel.getSubject(), spTupel.getPredicate(), size);
                        varMappings.add(mapping);
                        Set<SPTupel> mappedSpTupels = new HashSet<>();
                        mappedSpTupels.add(spTupel);
                        alreadyMappedSPs.put(mapping, mappedSpTupels);
                        alreadyMappedPOs.put(mapping, new HashSet<>());
                        alreadyMappedSOs.put(mapping, new HashSet<>());
                    }
                    else{
                        varMappings.forEach(mapping->{
                            if(!alreadyMappedSPs.get(mapping).contains(spTupel)){
                                if(mapping[ind1]!=null&&mapping[ind1]==spTupel.getSubject()){
                                    mapping[ind2]=spTupel.getPredicate();
                                    alreadyMappedSPs.get(mapping).add(spTupel);

                                }
                                else if(mapping[ind2]!=null&&mapping[ind2]==spTupel.getPredicate()){
                                    mapping[ind1]=spTupel.getSubject();
                                    alreadyMappedSPs.get(mapping).add(spTupel);

                                }
                                else if(mapping[ind1]==null&&mapping[ind2]==null){
                                    mapping[ind1]=spTupel.getSubject();
                                    mapping[ind2]=spTupel.getPredicate();
                                    alreadyMappedSPs.get(mapping).add(spTupel);
                                }
                            }

                        });
                    }

                }
            }
            if (varTupel.getType().equals("po")) {
                for(POTupel poTupel:resourceLinker.poTupels){

                    if(init) {
                        ResourceCandidate[] mapping = null;
                        mapping=initTupel(ind1, ind2, poTupel.getPredicate(), poTupel.getObject(), size);
                        varMappings.add(mapping);
                        Set<POTupel> mappedPoTupels = new HashSet<>();
                        mappedPoTupels.add(poTupel);
                        alreadyMappedPOs.put(mapping, mappedPoTupels);
                        alreadyMappedSPs.put(mapping,new HashSet<>());
                        alreadyMappedSOs.put(mapping, new HashSet<>());
                    }
                    else{
                        varMappings.forEach(mapping->{
                            if(!alreadyMappedPOs.get(mapping).contains(poTupel)){
                                if(mapping[ind1]!=null&&mapping[ind1]==poTupel.getPredicate()){
                                    mapping[ind2]=poTupel.getObject();
                                    alreadyMappedPOs.get(mapping).add(poTupel);

                                }
                                else if(mapping[ind2]!=null&&mapping[ind2]==poTupel.getObject()){
                                    mapping[ind1]=poTupel.getPredicate();
                                    alreadyMappedPOs.get(mapping).add(poTupel);

                                }
                                else if(mapping[ind1]==null&&mapping[ind2]==null){
                                    mapping[ind1]=poTupel.getPredicate();
                                    mapping[ind2]=poTupel.getObject();
                                    alreadyMappedPOs.get(mapping).add(poTupel);
                                }
                            }

                        });
                    }


                }
            }
            if (varTupel.getType().equals("so")) {
                for(SOTupel soTupel:resourceLinker.soTupels){
                    if(init) {
                        ResourceCandidate[] mapping1 = initTupel(ind1, ind2, soTupel.getSubject(), soTupel.getObject(), size);
                        ResourceCandidate[] mapping2 = initTupel(ind1, ind2, soTupel.getObject(), soTupel.getSubject(), size);
                        varMappings.add(mapping1);
                        varMappings.add(mapping2);
                        Set<SOTupel> mappedSoTupels = new HashSet<>();
                        mappedSoTupels.add(soTupel);
                        alreadyMappedSOs.put(mapping1, mappedSoTupels);
                        alreadyMappedSOs.put(mapping2, mappedSoTupels);
                    }
                    else{
                        varMappings.forEach(mapping->{
                            if(!alreadyMappedSOs.get(mapping).contains(soTupel)){
                                if(mapping[ind1]!=null&&mapping[ind1]==soTupel.getSubject()){
                                    mapping[ind2]=soTupel.getObject();
                                    alreadyMappedSOs.get(mapping).add(soTupel);

                                }
                                else if(mapping[ind2]!=null&&mapping[ind2]==soTupel.getObject()){
                                    mapping[ind1]=soTupel.getSubject();
                                    alreadyMappedSOs.get(mapping).add(soTupel);

                                }
                                else if(mapping[ind1]==null&&mapping[ind2]==null){
                                    ResourceCandidate[]mapping2=new ResourceCandidate[size];
                                    for(int i=0;i<mapping.length;i++) mapping[i]=mapping[i];

                                    mapping[ind1]=soTupel.getSubject();
                                    mapping[ind2]=soTupel.getObject();

                                    mapping2[ind1]=soTupel.getObject();
                                    mapping2[ind2]=soTupel.getSubject();
                                    varMappings.add(mapping2);
                                    alreadyMappedSOs.get(mapping).add(soTupel);
                                    alreadyMappedSPs.put(mapping,new HashSet<>());
                                    alreadyMappedPOs.put(mapping, new HashSet<>());
                                    alreadyMappedSOs.put(mapping2,alreadyMappedSOs.get(mapping));
                                    alreadyMappedSPs.put(mapping2,new HashSet<>());
                                    alreadyMappedPOs.put(mapping2, new HashSet<>());
                                }
                            }

                        });
                    }
                }
            }
            init=false;

        }
        return varMappings;
    }
    private static List<ResourceCandidate[]>generateTypeValMappings(List<ResourceCandidate[]>varMappings,ResourceLinker resourceLinker,List<String>vars,int size){
        List<ResourceCandidate[]>varMappingsWithClasses=new ArrayList<>();
        if(varMappings.isEmpty()){
            for(String typeVar:vars){
                ResourceCandidate[]mappingnew=new ResourceCandidate[size];
                int index=Integer.parseInt(typeVar.substring(typeVar.indexOf("_")+1));
                for(ResourceCandidate cand:resourceLinker.mappedClasses) {
                    mappingnew[index]=cand;
                    varMappingsWithClasses.add(mappingnew);
                }

            }
        }
        else {
            for (String typeVar : vars) {
                int index = Integer.parseInt(typeVar.substring(typeVar.indexOf("_") + 1));
                for (ResourceCandidate[] mapping : varMappings) {

                    for (ResourceCandidate cand : resourceLinker.mappedClasses) {
                        ResourceCandidate[] mappingnew = new ResourceCandidate[size];
                        for (int i = 0; i < mapping.length; i++) mappingnew[i] = mapping[i];
                        mappingnew[index] = cand;
                        varMappingsWithClasses.add(mappingnew);
                    }
                }
            }
        }
        return varMappingsWithClasses;
    }
    private static List<ResourceCandidate[]>generateSingleValMappings(List<ResourceCandidate[]>varMappings,ResourceLinker resourceLinker,Map<String,String>vars,int size){
        List<ResourceCandidate[]>varMappingsWithClasses=new ArrayList<>();
        if(varMappings.isEmpty()){
            for(String singleVar:vars.keySet()){
                ResourceCandidate[]mappingnew=new ResourceCandidate[size];
                int index=Integer.parseInt(singleVar.substring(singleVar.indexOf("_")+1));
                if(vars.get(singleVar).equals("res")) {
                    for (ResourceCandidate cand : resourceLinker.mappedEntities) {
                        mappingnew[index] = cand;
                        varMappingsWithClasses.add(mappingnew);
                    }
                }else{
                    for (ResourceCandidate cand : resourceLinker.mappedProperties) {
                        mappingnew[index] = cand;
                        varMappingsWithClasses.add(mappingnew);
                    }
                }

            }
        }
        else {
            for (String singleVar : vars.keySet()) {
                int index = Integer.parseInt(singleVar.substring(singleVar.indexOf("_") + 1));
                for (ResourceCandidate[] mapping : varMappings) {
                    if(vars.get(singleVar).equals("res")) {
                        for (ResourceCandidate cand : resourceLinker.mappedEntities) {
                            ResourceCandidate[] mappingnew = new ResourceCandidate[size];
                            for (int i = 0; i < mapping.length; i++) mappingnew[i] = mapping[i];
                            mappingnew[index] = cand;
                            varMappingsWithClasses.add(mappingnew);
                        }
                    }
                    else{
                        for (ResourceCandidate cand : resourceLinker.mappedProperties) {
                            ResourceCandidate[] mappingnew = new ResourceCandidate[size];
                            for (int i = 0; i < mapping.length; i++) mappingnew[i] = mapping[i];
                            mappingnew[index] = cand;
                            varMappingsWithClasses.add(mappingnew);
                        }
                    }
                }
            }
        }
        return varMappingsWithClasses;
    }
    private TripleTemplate transformTripleTemplate(String templatePattern){
        TripleTemplate temp=new TripleTemplate(templatePattern.split(" "));
        String subject="v";
        String predicate="v";
        String object="v";
        if(temp.getSubject().startsWith("res"))
            subject="";
        if(temp.getPredicate().startsWith("res"))
            predicate="r";
        if(temp.getObject().startsWith("res"))
            object="r";
        return new TripleTemplate(new String[]{subject,predicate,object});
    }
    private static List<Triple> generateSingleTriples(TripleTemplate triple,FillTemplatePatternsWithResources tripleGenerator){
        Set<String>triplesToCheck=new HashSet<>();
        List<Triple>foundTriples=new ArrayList<>();
        if (triple.getSubject().startsWith("res") && triple.getPredicate().startsWith("res")) triplesToCheck.add("r_r_v");
        else if (triple.getObject().startsWith("res") && triple.getPredicate().startsWith("res")) triplesToCheck.add("v_r_r");
        foundTriples.addAll(tripleGenerator.generateSingleTriples(triplesToCheck));
        if(triplesToCheck.contains("v_r_r")){
            foundTriples.addAll(tripleGenerator.getCountryTriples());
            foundTriples.addAll(tripleGenerator.getCategoryTriples());
            foundTriples.addAll(tripleGenerator.generateSingleTypeTriples());
        }
        return foundTriples;
    }
    /*private static List<Triple> generateCompoundTriplePatterns(List<TripleTemplate>triples,FillTemplatePatternsWithResources tripleGenerator,List<Triple>knownPatterns){
        Set<String>triplesToCheck=new HashSet<>();
        for(TripleTemplate triple:triples) {
            if (!triple.getSubject().startsWith("res") && triple.getPredicate().startsWith("res")&&!triple.getSubject().startsWith("res"))
                triplesToCheck.add("v_r_v");
        }
        List<Triple>foundCandidates=new ArrayList<>();
        if(triplesToCheck.size()>0)foundCandidates.addAll(tripleGenerator.generateTuplesWithTwoVariables(knownPatterns));
        return foundCandidates;
    }*/
    private boolean needCompoundTriples(List<TripleTemplate> triples){
        for(TripleTemplate triple:triples) {
            if (!triple.getSubject().startsWith("res") && triple.getPredicate().startsWith("res")&&!triple.getSubject().startsWith("res"))
                return true;
        }
        return false;
    }
    private TripleTemplate getCompoundTriple(List<TripleTemplate> triples){
        for(TripleTemplate triple:triples) {
            if (!triple.getSubject().startsWith("res") && triple.getPredicate().startsWith("res")&&!triple.getSubject().startsWith("res"))
                return triple;
        }
        return null;
    }
    private List<TripleTemplate>getSingleTriples(List<TripleTemplate> triples){
        List<TripleTemplate>singleTriples=new ArrayList<>();
        for(TripleTemplate triple:triples) {
            if (triple.getSubject().startsWith("res") && triple.getPredicate().startsWith("res"))
                singleTriples.add(triple);
            else if (triple.getObject().startsWith("res") && triple.getPredicate().startsWith("res"))
                singleTriples.add(triple);
        }
        return singleTriples;
    }
    private void fillpattern(List<TripleTemplate>patterns,List<Triple>singleCandidateTriples,List<Triple>compoundCandidateTriples,
                             List<Triple>additionalResourceRestrictuions,List<Triple>typeCandidateTriples,List<Triple>typeCompoundTriples){
        Set<String>resourcesToMatch=new HashSet<>();
        patterns.forEach(p->{
            if(p.getSubject().startsWith("res"))resourcesToMatch.add(p.getSubject());
            if(p.getPredicate().startsWith("res"))resourcesToMatch.add(p.getPredicate());
            if(p.getObject().startsWith("res"))resourcesToMatch.add(p.getObject());
        });
        List<HashMap<String,String>>mappings=new ArrayList<>();


    }
    private static List<String>generateQueriesWithOnePattern(String query,String patternString,TripleTemplate pattern,List<Triple>singleCandidateTriples,List<Triple>typeTriples) {
        List<String> queries = new ArrayList<>();
        if (!pattern.getObject().startsWith("res")) {
            for (Triple t : singleCandidateTriples) {
                if (!t.getSubject().equals("var")) {
                    queries.add(query.replace(patternString,"<" + t.getSubject() + "> " + "<" + t.getPredicate() + "> " + pattern.getObject()));
                }
            }
        } else {
            for (Triple t : singleCandidateTriples) {
                if (!t.getObject().equals("var")&&!t.getPredicate().equals("country_prop")) {
                    queries.add(query.replace(patternString,pattern.getSubject() + "<" + t.getPredicate() + "> " + "<"+t.getObject()+">"));
                }
            }
        }
        return queries;
    }
    private static boolean connectSubject(TripleTemplate compound,TripleTemplate single){
        if (!single.getObject().startsWith("res")){
            if(single.getObject().equals(compound.getSubject()))return true;
            else return false;
        }
        else{
            if(single.getSubject().equals(compound.getSubject()))return true;
            else return false;
        }
    }

    private static boolean acceptTripleComound(Triple[]compound,TripleTemplate t1,TripleTemplate t2){
        if(t1.getSubject().equals(t2.getSubject())&&
            !compound[0].getSubject().equals(compound[1].getSubject()))return false;
        if(!t1.getSubject().equals(t2.getSubject())&&
                compound[0].getSubject().equals(compound[1].getSubject()))return false;
        if(t1.getPredicate().equals(t2.getPredicate())&&
                !compound[0].getPredicate().equals(compound[1].getPredicate()))return false;
        if(!t1.getPredicate().equals(t2.getPredicate())&&
                compound[0].getPredicate().equals(compound[1].getPredicate()))return false;
        if(t1.getObject().equals(t2.getObject())&&
                !compound[0].getObject().equals(compound[1].getObject()))return false;
        if(!t1.getObject().equals(t2.getObject())&&
                compound[0].getObject().equals(compound[1].getObject()))return false;
        if(t1.getSubject().equals(t2.getObject())&&
                !compound[0].getSubject().equals(compound[1].getObject()))return false;
        if(!t1.getSubject().equals(t2.getObject())&&
                compound[0].getSubject().equals(compound[1].getObject()))return false;
        if(t1.getObject().equals(t2.getSubject())&&
                !compound[0].getObject().equals(compound[1].getSubject()))return false;
        if(!t1.getObject().equals(t2.getSubject())&&
                compound[0].getObject().equals(compound[1].getSubject()))return false;
        return true;
    }
    private static boolean acceptResourceCooccurenceMapping(Triple[]triples,FillTemplatePatternsWithResources tripleGenerator){
        List<String>uris=new ArrayList<>();
        for(int i=0;i<triples.length;i++){
            if(triples[i].getSubject().startsWith("http"))uris.add(triples[i].getSubject());
            if(triples[i].getPredicate().startsWith("http"))uris.add(triples[i].getPredicate());
            if(triples[i].getObject().startsWith("http"))uris.add(triples[i].getObject());
        }
        return tripleGenerator.acceptByCooccurence(uris);
    }
    private static List<Triple[]> generateCompound(TripleTemplate source,TripleTemplate target,List<Triple>sourceCandidates,FillTemplatePatternsWithResources tripleGenerator) {
        List<Triple[]>tripleCompounds=new ArrayList<>();
        for(Triple candidate:sourceCandidates) {
            List<Triple> compoundCandidates;

            if(TripleTemplate.Pattern.V_R_V.sameAs(target.getPatternString()) &&
                    (candidate.getPredicate().equalsIgnoreCase("a") || candidate.getPredicate().equalsIgnoreCase(RDF.TYPE)))
                compoundCandidates = tripleGenerator.generateTypePropertyTriples(candidate, target);
            else
                compoundCandidates = tripleGenerator.generateTuplesWithTwoVariables(candidate, target);

            compoundCandidates.forEach(candComp->{
                Triple[] tcomp = new Triple[]{candidate, candComp};
                if (acceptTripleComound(tcomp, source, target)
                        &&acceptResourceCooccurenceMapping(tcomp,tripleGenerator))
                    tripleCompounds.add(tcomp);
            });
        }
        return tripleCompounds;

    }
    private static int countResourcesToMatch(List<TripleTemplate>patterns){
        Set<String>resources=new HashSet<>();
        patterns.forEach(p->{
            if(p.getSubject().startsWith("res"))resources.add(p.getSubject());
            if(p.getPredicate().startsWith("res"))resources.add(p.getPredicate());
            if(p.getObject().startsWith("res"))resources.add(p.getObject());
        });
        return resources.size();
    }

    private static List<HashMap<String,String>> generateMappingsWithNPatterns(FillTemplatePatternsWithResources tripleGenerator,List<TripleTemplate>patterns) {
        // Triple with two resources calculation
        HashMap<TripleTemplate, List<Triple>> candidatesWith2Res = new HashMap<>();
        patterns.forEach(pattern -> {
            if ((pattern.isSubjectAResource() || pattern.isObjectAResource()) && pattern.isPredicateAResource()) {
                List<Triple> tripleCandidates = generateSingleTriples(pattern, tripleGenerator);
                if (pattern.isObjectAResource()) tripleCandidates.forEach(c -> c.setSubject(pattern.getSubject()));
                if (pattern.isSubjectAResource()) tripleCandidates.forEach(c -> c.setObject(pattern.getObject()));
                candidatesWith2Res.put(pattern, tripleCandidates);
            }
        });

        // Compound calculation
        List<CompoundPatternCandidates> compoundss=new ArrayList<>();
        if(patterns.size()>candidatesWith2Res.size()) {

//            // Only process patterns with single resource, filter out patterns with two resources because they are already processed above.
            List<TripleTemplate> allPatternsWith1Resource = patterns.stream().filter(triplePattern -> !candidatesWith2Res.containsKey(triplePattern)).collect(Collectors.toList());

            for(TripleTemplate patternWith1Res:allPatternsWith1Resource) {

                for(TripleTemplate patternWith2Res:candidatesWith2Res.keySet()){
                    if(patternWith2Res.getSubject().equals(patternWith1Res.getSubject())||
                            patternWith2Res.getSubject().equals(patternWith1Res.getObject())||
                            patternWith2Res.getObject().equals(patternWith1Res.getSubject())||
                            patternWith2Res.getObject().equals(patternWith1Res.getObject())
                    ) {
                        List<Triple[]>foundcompounds=generateCompound(patternWith2Res,patternWith1Res,candidatesWith2Res.get(patternWith2Res),tripleGenerator);
                        if(foundcompounds.size()>0) {
                            compoundss.add(new CompoundPatternCandidates(patternWith2Res, patternWith1Res, foundcompounds));
                            break;
                        }
                    }
                }

            }

        }


        compoundss.forEach(compoundCandidate -> candidatesWith2Res.remove(compoundCandidate.getPatternWith2Res()));

        List<HashMap<String,String>>mappings=new ArrayList<>();

        compoundss.forEach(comp ->{
            mappings.addAll(gernerateMappingFromCompound(comp.getCandidates(), comp.getPatternWith2Res(), comp.getPatternWith1Res()));
        });
        candidatesWith2Res.keySet().forEach(pat->{
            mappings.addAll(gernerateMappingFromSingleTriple(candidatesWith2Res.get(pat),pat));
        });
        List<HashMap<String, String>> mergeMappings = mergeMappings(mappings, tripleGenerator);
        return mergeMappings;

    }

    private static List<HashMap<String, String>> mergeMappings(List<HashMap<String, String>> mappings, FillTemplatePatternsWithResources tripleGenerator) {
        List<HashMap<String, String>> mergedMappings = new ArrayList<>();

        for (int x = 0; x < mappings.size(); x++) {
            for (int y = x + 1; y < mappings.size(); y++) {
                boolean validMerge = true;
                HashMap<String, String> merged = new HashMap<>();

                HashMap<String, String> set1 = mappings.get(x);
                HashMap<String, String> set2 = mappings.get(y);

                // If both sets have same keys, ignore pair.
                if (set1.keySet().containsAll(set2.keySet()) && set2.keySet().containsAll(set1.keySet())) {
                    validMerge = false;
                }

                ImmutableSet<String> commonKeys = Sets.intersection(set1.keySet(), set2.keySet()).immutableCopy();

                // Make sure that if a key exists in both sets, value associated with this key is same.
                if(validMerge){
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

        if(mergedMappings.size()==0) return mappings;
        else return mergeMappings(mergedMappings, tripleGenerator);
    }

    private static boolean copyRemainingEntries(HashMap<String, String> from, HashMap<String, String> to, Set<String> commonKeys) {
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

    private static List<HashMap<String,String>>gernerateMappingFromSingleTriple(List<Triple>triples,TripleTemplate pattern1){
        List<HashMap<String,String>>mappings=new ArrayList<>();
        triples.forEach(trip->{
            HashMap<String,String>mapping=new HashMap<>();
            if(pattern1.getSubject().startsWith("res"))mapping.put(pattern1.getSubject(),trip.getSubject());
            else mapping.put(pattern1.getObject(),trip.getObject());
            mapping.put(pattern1.getPredicate(),trip.getPredicate());
            mappings.add(mapping);
        });
        return mappings;
    }
    private static List<HashMap<String,String>>gernerateMappingFromCompound(List<Triple[]>compounds,TripleTemplate templateWith2Res,TripleTemplate templateWith1Res){
        List<HashMap<String,String>>mappings=new ArrayList<>();

        compounds.forEach(comp->{
            HashMap<String,String>mapping=new HashMap<>();
            Triple tripleWith2Res = comp[0];
            Triple tripleWith1Res = comp[1];

            // Handle pattern with two resources
            mapping.put(templateWith2Res.getPredicate(), tripleWith2Res.getPredicate());
            if(templateWith2Res.isSubjectAResource())
                mapping.put(templateWith2Res.getSubject(), tripleWith2Res.getSubject());
            else
                mapping.put(templateWith2Res.getObject(), tripleWith2Res.getObject());

            // Handle pattern with one resource
            if(TripleTemplate.Pattern.V_R_V.equals(templateWith1Res.getPattern()))
                mapping.put(templateWith1Res.getPredicate(), tripleWith1Res.getPredicate());
            else if(TripleTemplate.Pattern.V_V_R.equals(templateWith1Res.getPattern()))
                mapping.put(templateWith1Res.getObject(), tripleWith1Res.getObject());

            mappings.add(mapping);
        });
        return mappings;
    }
    private void generateTriplesWithTwoPatterns(FillTemplatePatternsWithResources tripleGenerator,
                                                TripleTemplate pattern1,TripleTemplate pattern2){
        List<Triple>candidates1=new ArrayList<>();
        List<Triple>candidates2=new ArrayList<>();
        if(pattern1.getSubject().startsWith("res")||pattern1.getObject().startsWith("res")) {
            candidates1.addAll(generateSingleTriples(pattern1, tripleGenerator));
            if (pattern1.getObject().startsWith("res"))candidates1.forEach(c->c.setSubject(pattern1.getSubject()));
            if (pattern1.getSubject().startsWith("res"))candidates1.forEach(c->c.setObject(pattern1.getObject()));
        }
        if(pattern2.getSubject().startsWith("res")||pattern2.getObject().startsWith("res")) {
            candidates2.addAll(generateSingleTriples(pattern2, tripleGenerator));
            if (pattern2.getObject().startsWith("res"))candidates1.forEach(c->c.setSubject(pattern1.getSubject()));
            if (pattern2.getSubject().startsWith("res"))candidates1.forEach(c->c.setObject(pattern1.getObject()));
        }
        List<Triple[]>tripleCompounds=new ArrayList<>();

        TripleTemplate templateWithTwoVariables=null;
        List<Triple> candsToMatch=null;
        if(!pattern1.getSubject().startsWith("res")&&!pattern1.getObject().startsWith("res")){
            templateWithTwoVariables=pattern1;
            candsToMatch=candidates2;
        }
        if(!pattern2.getSubject().startsWith("res")&&!pattern2.getObject().startsWith("res")){
            templateWithTwoVariables=pattern2;
            candsToMatch=candidates1;
        }
        if(templateWithTwoVariables!=null){
            for(Triple candidate:candsToMatch) {
                List<Triple> compoundCandidates = tripleGenerator.generateTuplesWithTwoVariables(candidate, templateWithTwoVariables);
                compoundCandidates.forEach(candComp->{
                    Triple[] tcomp = new Triple[]{candidate, candComp};
                    if (acceptTripleComound(tcomp, pattern1, pattern2)
                            &&acceptResourceCooccurenceMapping(tcomp,tripleGenerator))
                        tripleCompounds.add(tcomp);
                });
            }

        }
        else{
            for(Triple t1:candidates1){
                candidates2.forEach(t2->{
                    Triple[] tcomp = new Triple[]{t1,t2};
                    if(acceptTripleComound(tcomp,pattern1,pattern2)
                            &&acceptResourceCooccurenceMapping(tcomp,tripleGenerator))
                        tripleCompounds.add(tcomp);
                });
            }
        }


    }
    private static boolean containsProp(HashMap<String,String>mapping,String prop){
        Set<String>values=Sets.newHashSet(mapping.values());
        if(values.contains(prop))return true;
        else return false;
    }
    private static boolean compareMappings(HashMap<String,String> rankedMapping,HashMap<String,String>mapping,
                                List<TripleTemplate> patterns,FillTemplatePatternsWithResources resourceGenerator){
        Set<String>valuesRanked=Sets.newHashSet(rankedMapping.values());
        Set<String>valuesMapping=Sets.newHashSet(mapping.values());
        if(containsProp(mapping,"country_prop")&&!containsProp(rankedMapping,"coutry_prop"))
            return false;
        else if(!containsProp(mapping,"country_prop")&&containsProp(rankedMapping,"coutry_prop"))
            return true;
        if(containsProp(mapping,"a")&&!containsProp(rankedMapping,"a"))
            return false;
        else if(!containsProp(mapping,"a")&&containsProp(rankedMapping,"a"))
            return true;

        List<String[]>connResources=new ArrayList<>();
        List<String[]>connResourcesRanked=new ArrayList<>();
        patterns.forEach(pat->{
            if(pat.getSubject().startsWith("res")&&pat.getPredicate().startsWith("res")) {
                connResources.add(new String[]{mapping.get(pat.getSubject()), mapping.get(pat.getPredicate())});
                connResourcesRanked.add(new String[]{rankedMapping.get(pat.getSubject()), rankedMapping.get(pat.getPredicate())});
            }
            if(pat.getObject().startsWith("res")&&pat.getPredicate().startsWith("res")) {
                connResources.add(new String[]{mapping.get(pat.getObject()), mapping.get(pat.getPredicate())});
                connResourcesRanked.add(new String[]{rankedMapping.get(pat.getObject()), rankedMapping.get(pat.getPredicate())});
            }
        });
        double avgRelatednessMapping=resourceGenerator.getAverageRelatednessScore(valuesMapping);
        double avgRelatednessMappingRanked=resourceGenerator.getAverageRelatednessScore(valuesRanked);
        if(avgRelatednessMapping>avgRelatednessMappingRanked)return true;
        else if(avgRelatednessMappingRanked>avgRelatednessMapping)return false;
        int popMapping=resourceGenerator.getPopularity(valuesMapping);
        int popRanked=resourceGenerator.getPopularity(valuesRanked);
        if(popMapping>popRanked)return true;
        else if(popMapping<popRanked)return false;
//        int numberOfSemDependanciesMapping=resourceGenerator.getNumberOfDependantResourceCombinations(connResources);
//        int numberOfSemDependanciesMappingRanked=resourceGenerator.getNumberOfDependantResourceCombinations(connResourcesRanked);
//        if(numberOfSemDependanciesMapping>numberOfSemDependanciesMappingRanked)return true;
//        else if(numberOfSemDependanciesMappingRanked<numberOfSemDependanciesMapping)return false;

        return false;
    }
    private static List<HashMap<String,String>>rankMappings(List<HashMap<String,String>>mappings,
                                                     List<TripleTemplate>patterns,FillTemplatePatternsWithResources tripleGenerator){
        ArrayList<HashMap<String,String>>rankedMappings=new ArrayList<>();
        for(HashMap mapping:mappings){
            boolean added=false;
            for(int i=0;i<rankedMappings.size();i++){
                if(compareMappings(rankedMappings.get(i),mapping,patterns,tripleGenerator)) {
                    rankedMappings.add(i, mapping);
                    added=true;
                    break;
                }

            }
            if(!added) rankedMappings.add(mapping);
        }
        return rankedMappings;
    }

    public static List<String>fillTemplates(String pattern,FillTemplatePatternsWithResources tripleGenerator){
        List<String> triples = extractTriples(pattern);
        List<String> triplesWithoutFilters = triples.parallelStream()
                .filter(s -> !s.toLowerCase().contains("filter") && !s.toLowerCase().contains("optional"))
                .collect(Collectors.toList());
        List<TripleTemplate>patterns=new ArrayList<>();
        triplesWithoutFilters.forEach(t->patterns.add(new TripleTemplate(t.split(" "))));

        //List<Triple>compoundCandidateTriples=new ArrayList<>();
        /*if(triplesWithoutFilters.size()>1) {
            compoundCandidateTriples.addAll(generateCompoundTriplePatterns(patterns, tripleGenerator, singleCandidateTriples));
        }*/
        List<Triple>typeCandidateTriples=new ArrayList<>();
        List<HashMap<String,String>>mappings=generateMappingsWithNPatterns(tripleGenerator,patterns);
        List<String>queries=new ArrayList<>();
        if(mappings.size()>0&&mappings.get(0).size()==countResourcesToMatch(patterns)) {
            mappings = rankMappings(mappings, patterns, tripleGenerator);
            for(HashMap<String,String>mapping:mappings){
                String query=""+pattern;
                for(String key:mapping.keySet()){
                    String val=mapping.get(key);
                    if(val.equals("country_prop"))query=query.replace(key,"?county_var");
                    if(val.equals("a"))query=query.replace(key,val);
                    else query=query.replace(key,"<"+mapping.get(key)+">");
                }
                queries.add(query);
            }
        }
        return queries;

        /*if(patterns.size()==1) {
            List<Triple> singleCandidateTriples = new ArrayList<>();
            singleCandidateTriples.addAll(generateSingleTriples(patterns.get(0), tripleGenerator));
            typeCandidateTriples.addAll(tripleGenerator.generateSingleTypeTriples());
            List<String> queries = generateQueriesWithOnePattern(pattern, triples.get(0), patterns.get(0), singleCandidateTriples, typeCandidateTriples);
        }*/
        /*if(patterns.size()==2) {
            List<Triple>triplesPat1=new ArrayList<>();
            List<Triple>triplesPat2=new ArrayList<>();
            if(patterns.get(0).getSubject().startsWith("res")||patterns.get(0).getObject().startsWith("res")) {
                    triplesPat1.addAll(generateSingleTriples(patterns.get(0), tripleGenerator));
            }
            if(patterns.get(1).getSubject().startsWith("res")||patterns.get(1).getObject().startsWith("res")) {
                triplesPat2.addAll(generateSingleTriples(patterns.get(1), tripleGenerator));
            }
            List<Triple[]>tripleCompounds=new ArrayList<>();
            if(!triplesPat1.isEmpty()&&!triplesPat2.isEmpty()){
                for(Triple t1:triplesPat1){
                    triplesPat2.forEach(t2->{
                        Triple[] tcomp = new Triple[]{t1,t2};
                        if(acceptTripleComound(tcomp,patterns.get(0),patterns.get(1))
                                &&acceptResourceCooccurenceMapping(tcomp,tripleGenerator))
                        tripleCompounds.add(tcomp);
                    });
                }
            }
            else if(triplesPat1.isEmpty()&&!patterns.get(0).getSubject().startsWith("res")&&!patterns.get(0).getObject().startsWith("res")&&!triplesPat2.isEmpty()){
                for(Triple t: triplesPat2) {
                    List<Triple> compoundCandidates = tripleGenerator.generateTuplesWithTwoVariables(t, connectSubject(patterns.get(0), patterns.get(1)));
                    if(connectSubject(patterns.get(0),patterns.get(1)))t.setSubject("v_matched");
                    else t.setObject("v_matched");
                    compoundCandidates.forEach(comp -> {
                        Triple[] tcomp = new Triple[]{t, comp};
                        if (acceptTripleComound(tcomp, patterns.get(0), patterns.get(1))
                        &&acceptResourceCooccurenceMapping(tcomp,tripleGenerator))
                            tripleCompounds.add(tcomp);
                    });
                }
            }
            else if(triplesPat2.isEmpty()&&!patterns.get(1).getSubject().startsWith("res")&&!patterns.get(1).getObject().startsWith("res")&&!triplesPat1.isEmpty()){
                for(Triple t: triplesPat1){
                    List<Triple>compoundCandidates=tripleGenerator.generateTuplesWithTwoVariables(t,connectSubject(patterns.get(1),patterns.get(0)));
                    if(connectSubject(patterns.get(0),patterns.get(1)))t.setSubject("v_matched");
                    else t.setObject("v_matched");
                    compoundCandidates.forEach(comp->{
                        Triple[] tcomp=new Triple[]{t,comp};
                        if(acceptTripleComound(tcomp,patterns.get(0),patterns.get(1))
                                &&acceptResourceCooccurenceMapping(tcomp,tripleGenerator))
                        tripleCompounds.add(tcomp);
                    });
                }
            }*/


        /*List<Triple>typeCompoundTriples=new ArrayList<>();
        if(triplesWithoutFilters.size()>1) {
            typeCompoundTriples.addAll(tripleGenerator.generateTypeTriplesEntity());
            typeCompoundTriples.addAll(tripleGenerator.generateTypePropertyTriples());
        }*/

    }

    public static HashMap<String,String> fillWithTuples(String pattern,ResourceLinker resourceLinker){
        List<String> triples = extractTriples(pattern);
        List<String> triplesWithoutFilters = triples.parallelStream()
                .filter(s -> !s.toLowerCase().contains("filter") && !s.toLowerCase().contains("optional"))
                .collect(Collectors.toList());
        Map<String, String> replacements = new HashMap<>();
        for (String triple : triplesWithoutFilters) {
            Matcher m = ARGUMENTS_BETWEEN_SPACES.matcher(triple);
            while (m.find()) {
                String argument = m.group();
                if (argument.startsWith("<^")) {
                    String replacement = "?var_" + argument.substring(argument.indexOf("_")+1,argument.indexOf("^>"));
                    pattern = pattern.replace(argument, replacement);
                    replacements.put(argument, replacement);
                }
            }
        }
        try {
            Query query = QueryFactory.create(pattern);
            List<VarTupel>varTupels=new ArrayList<>();
            Map<String,String>singleVars=new HashMap<>();
            List<String>typeVars=new ArrayList<>();
            ElementWalker.walk(query.getQueryPattern(),
                    new ElementVisitorBase() {
                        public void visit(ElementPathBlock el) {
                            Iterator<TriplePath> triples = el.patternElts();
                            while (triples.hasNext()) {
                                TriplePath t = triples.next();
                                if(t.getSubject().toString().startsWith("?var_")&&t.getPredicate().toString().startsWith("?var_")){
                                    varTupels.add(new VarTupel(t.getSubject().toString(),t.getPredicate().toString(),"sp"));
                                }
                                else if(t.getPredicate().toString().startsWith("?var_")&&t.getObject().toString().startsWith("?var_")){
                                    varTupels.add(new VarTupel(t.getPredicate().toString(),t.getObject().toString(),"po"));
                                }
                                else if(t.getSubject().toString().startsWith("?var_")&&t.getObject().toString().startsWith("?var_")){
                                    varTupels.add(new VarTupel(t.getSubject().toString(),t.getObject().toString(),"so"));
                                }
                                else if (t.getPredicate().toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
                                    typeVars.add(t.getObject().toString());
                                }
                                else if(t.getSubject().toString().startsWith("?var_"))
                                    singleVars.put(t.getSubject().toString(),"res");
                                else if(t.getPredicate().toString().startsWith("?var_"))
                                    singleVars.put(t.getPredicate().toString(),"prop");
                                else if(t.getObject().toString().startsWith("?var_"))
                                    singleVars.put(t.getObject().toString(),"res");
                            }

                        }
                    }

            );
            List<ResourceCandidate[]>varMappings=generateVarTupelMappings(varTupels,resourceLinker,replacements.size());
            if(typeVars.size()>0) varMappings=generateTypeValMappings(varMappings,resourceLinker,typeVars,replacements.size());
            if(singleVars.size()>0)varMappings=generateSingleValMappings(varMappings,resourceLinker,singleVars,replacements.size());


            StringBuilder bindingsGenerator = new StringBuilder();
            List<Var>vars= new ArrayList<Var>();

            for(int i=0;i<varMappings.get(0).length;i++){
                Var uri =Var.alloc("var_"+i);
                vars.add(uri);
            }
            List<Binding> bindings=new ArrayList<>();
            for(ResourceCandidate[]mapping:varMappings){
                if(!ArrayUtils.contains(mapping,null)) {
                    BindingMap bmap=BindingFactory.create();
                    for (int i = 0; i <mapping.length;i++){
                        bmap.add(vars.get(i), NodeFactory.createURI(mapping[i].getUri()));
                    }
                    bindings.add(bmap);
                }

            }
            /*for (String var : values.keySet())
                bindingsGenerator.append(" VALUES (" + var + ") {(<" + (Strings.join(values.get(var), ">) (<")) + ">)}");
            pattern = pattern.replaceFirst("\\{", "{" + bindingsGenerator.toString());*/

            //pattern=addToLastTriple(pattern,bindingsGenerator.toString());
            String patternString=query.toString();
            query.setValuesDataBlock(vars,bindings);
            HashMap<String,String>result=new HashMap<>();
            result.put(query.toString(),patternString);
            return result;
        }catch (Exception e){
            return null;
        }
    }
    public static String fillPattern(String pattern,ResourceLinker resourceLinker){
        List<String> triples = extractTriples(pattern);
        List<String> triplesWithoutFilters = triples.parallelStream()
                .filter(s -> !s.toLowerCase().contains("filter") && !s.toLowerCase().contains("optional"))
                .collect(Collectors.toList());
        Map<String, String> replacements = new HashMap<>();
        for (String triple : triplesWithoutFilters) {
            Matcher m = ARGUMENTS_BETWEEN_SPACES.matcher(triple);
            while (m.find()) {
                String argument = m.group();
                if (argument.startsWith("<^")) {
                        String replacement = "?var_" + argument.substring(argument.indexOf("_")+1,argument.indexOf("^>"));
                        pattern = pattern.replace(argument, replacement);
                        replacements.put(argument, replacement);
                    }
                }
            }
            try {
                Query query = QueryFactory.create(pattern);

                HashMap<String, List<String>> values = new HashMap<>();
                ElementWalker.walk(query.getQueryPattern(),
                        new ElementVisitorBase() {
                            public void visit(ElementPathBlock el) {
                                Iterator<TriplePath> triples = el.patternElts();
                                while (triples.hasNext()) {
                                    TriplePath t = triples.next();
                                    if (t.getSubject().toString().startsWith("?var_")) {
                                        if (!values.containsKey(t.getSubject())) {
                                            List<String> bind = new ArrayList<>();
                                            resourceLinker.mappedEntities.forEach(res -> bind.add(res.getUri()));
                                            values.put(t.getSubject().toString(), bind);
                                        }

                                        //Var uri =Var.alloc(t.getSubject());
                                        //vars.add(uri);
                                        //resourceLinker.mappedEntities.forEach(ent -> bindings.add(BindingFactory.binding(uri, NodeFactory.createURI(ent.getUri()))));
                                    }
                                    if (t.getPredicate().toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
                                        if (!values.containsKey(t.getSubject())) {
                                            List<String> bind = new ArrayList<>();
                                            resourceLinker.mappedClasses.forEach(res -> bind.add(res.getUri()));
                                            values.put(t.getObject().toString(), bind);
                                        }
                                        //Var uri =Var.alloc(t.getObject());
                                        //vars.add(uri);
                                        //resourceLinker.mappedClasses.forEach(ent -> bindings.add(BindingFactory.binding(uri, NodeFactory.createURI(ent.getUri()))));
                                    } else if (t.getObject().toString().startsWith("?var_")) {
                                        if (!values.containsKey(t.getObject())) {
                                            List<String> bind = new ArrayList<>();
                                            resourceLinker.mappedEntities.forEach(res -> bind.add(res.getUri()));
                                            values.put(t.getObject().toString(), bind);
                                        }
                                        //Var uri =Var.alloc(t.getObject());
                                        //vars.add(uri);
                                        //resourceLinker.mappedEntities.forEach(ent -> bindings.add(BindingFactory.binding(uri, NodeFactory.createURI(ent.getUri()))));
                                    }
                                    if (t.getPredicate().toString().startsWith("?var_")) {
                                        if (!values.containsKey(t.getPredicate())) {
                                            List<String> bind = new ArrayList<>();
                                            resourceLinker.mappedProperties.forEach(res -> bind.add(res.getUri()));
                                            values.put(t.getPredicate().toString(), bind);
                                        }

                                    }

                                }

                            }
                        }

                );
                StringBuilder bindingsGenerator = new StringBuilder();
                for (String var : values.keySet())
                    bindingsGenerator.append(" VALUES (" + var + ") {(<" + (Strings.join(values.get(var), ">) (<")) + ">)}");
                pattern = pattern.replaceFirst("\\{", "{" + bindingsGenerator.toString());

                //pattern=addToLastTriple(pattern,bindingsGenerator.toString());
                //query.setValuesDataBlock(vars,bindings);
                return pattern;
            }catch (Exception e){
                return null;
            }
    }
    static String fillPattern(String pattern, List<String> classResources, List<String> propertyResources) {
        List<String> triples = extractTriples(pattern);
        List<String> triplesWithoutFilters = triples.parallelStream()
                .filter(s -> !s.toLowerCase().contains("filter") && !s.toLowerCase().contains("optional"))
                .collect(Collectors.toList());
        int classReplacementCount = 0;
        int propertyReplacementCount = 0;
        Map<String, String> replacements = new HashMap<>();
        for (String triple : triplesWithoutFilters) {
            Matcher m = ARGUMENTS_BETWEEN_SPACES.matcher(triple);
            int position = 0;
            while (m.find()) {
                String argument = m.group();
                if (argument.startsWith("<^")) {
                    if (position == 0 || position == 2) {
                        String replacement = "?class_" + classReplacementCount;
                        pattern = pattern.replace(argument, replacement);
                        replacements.put(argument, replacement);
                        classReplacementCount++;
                    } else if (position == 1) {
                        String replacement = "?property_" + propertyReplacementCount;
                        pattern = pattern.replace(argument, replacement);
                        replacements.put(argument, replacement);
                        propertyReplacementCount++;
                    } else {
                        log.error("Invalid position in triple:" + triple);
                    }
                }
                position++;
            }
        }
        StringBuilder classValues = new StringBuilder();
        if (classReplacementCount > 0) {
            for (int i = 0; i < triplesWithoutFilters.size(); i++) {
                classValues.append(String.format(" VALUES (?class_%d) {(<", i)).append(Strings.join(classResources, ">) (<")).append(">)}");
            }
        }

        StringBuilder propertyValues = new StringBuilder();
        if (propertyReplacementCount > 0) {
            for (int i = 0; i < triplesWithoutFilters.size(); i++) {
                propertyValues.append(String.format(" VALUES (?property_%d) {(<", i)).append(Strings.join(propertyResources, ">) (<")).append(">)}");
            }
        }



        //String filterClauses = SPARQLUtilities.createFilterClauses(triplesWithoutFilters, replacements);
        String filterClauses="";
        return addToLastTriple(pattern, classValues.append(propertyValues.toString()).append(filterClauses).toString());
    }

    private static String addToLastTriple(String pattern, String s) {
        Matcher m = BETWEEN_CURLY_BRACES.matcher(pattern);


        String lastTriple = "";
        String newLastTriple = "";
        while (m.find()) {
            lastTriple = m.group(m.groupCount());
            newLastTriple = lastTriple;
            if (!newLastTriple.trim().endsWith(".")) {
                newLastTriple = newLastTriple + ".";
            }
            newLastTriple = newLastTriple + s;
        }
        String result = pattern.replace(lastTriple, newLastTriple);
        if (result.isEmpty()) {
            log.error("Unable to put together SPARQL Query: " + pattern + "; And: " + s);
        }
        return result;
    }
    public static void main(String[]args){
        String pattern="SELECT ?uri WHERE {{?s ?p ?o}UNION{?s ?p ?o}}";
        addToLastTriple(pattern,"test");
    }
    public static <E> List<List<E>> generatePermutations(List<E> original) {
        if (original.size() == 0) {
            List<List<E>> result = new ArrayList<>();
            result.add(new ArrayList<>());
            return result;
        }
        E firstElement = original.remove(0);
        List<List<E>> returnValue = new ArrayList<>();
        List<List<E>> permutations = generatePermutations(original);
        for (List<E> smallerPermutated : permutations) {
            for (int index = 0; index <= smallerPermutated.size(); index++) {
                List<E> temp = new ArrayList<>(smallerPermutated);
                temp.add(index, firstElement);
                returnValue.add(temp);
            }
        }
        return returnValue;
    }

    public static double getLevenshteinRatio(String s, String s2) {
        if(s == null || s2 == null){
            return 1;
        }
        int lfd = StringUtils.getLevenshteinDistance(s2, s);
        return ((double) lfd) / (Math.max(s2.length(), s.length()));
    }
}

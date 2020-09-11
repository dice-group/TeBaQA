package de.uni.leipzig.tebaqa.helper;


import de.uni.leipzig.tebaqa.controller.PipelineController;
import de.uni.leipzig.tebaqa.controller.SemanticAnalysisHelper;
import de.uni.leipzig.tebaqa.controller.SemanticAnalysisHelperGerman;
import de.uni.leipzig.tebaqa.model.QueryTemplateMapping;
import de.uni.leipzig.tebaqa.model.ResourceCandidate;
import de.uni.leipzig.tebaqa.model.TripleTemplate;
import de.uni.leipzig.tebaqa.spring.AnnotateQualD8;
import edu.cmu.lti.jawjaw.db.*;
import edu.cmu.lti.jawjaw.pobj.*;
import org.aksw.qa.commons.datastructure.IQuestion;
import org.aksw.qa.commons.datastructure.Question;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

public class FillPatternTest {
    @Test
    public void fillTest(){
        //String pattern="SELECT (COUNT(DISTINCT ?uri) AS ?c) WHERE{ ?uri a <^var_0^> ; <^var_1^> <^var_2^> }";
        String pattern="SELECT DISTINCT ?uri WHERE {<^var_0^> <^var_1^>  ?x. <^var_2^> ?prop <^var_0^>. ?s ?p <^var_3^>}";
        //String pattern="SELECT ?uri WHERE{ ?uri a <^var_0^> ; <^var_1^> <^var_2^> }";
        String query="What is the birthplace of Angela Merkel?\n\n";
        ResourceLinker l=new ResourceLinker(new SemanticAnalysisHelper());

        l.extractEntities(query);
        Utilities u=new Utilities();
        u.fillWithTuples(pattern,l);
        //u.fillPattern(pattern,l);
        //SPARQLUtilities.retrieveBinings(u.fillWithTuples(pattern,l));
    }

    @Test
    public void resQuery(){
        String query="SELECT DISTINCT ?uri WHERE { { ?uri a <http://dbpedia.org/ontology/City> } UNION { ?uri a <http://dbpedia.org/ontology/Town> } ?uri <http://dbpedia.org/ontology/country> <http://dbpedia.org/resource/Germany> ; <http://dbpedia.org/ontology/populationTotal> ?population FILTER ( ?population > 250000 ) }";
        Query q= QueryFactory.create(query);

        List<String>res=q.getResultVars();
        System.out.println();
    }

    @Test
    public void fill(){
        //String pattern="SELECT (COUNT(DISTINCT ?uri) AS ?c) WHERE{ ?uri a <^var_0^> ; <^var_1^> <^var_2^> }";
        String pattern="SELECT DISTINCT ?uri WHERE {res/2 res/3 ?uri .}";
        //String pattern="SELECT ?uri WHERE{ ?uri a <^var_0^> ; <^var_1^> <^var_2^> }";
        String query="Wie ist die Länge von Gleis 1 des Leipziger Hauptbahnhofs?";

        query = "Welche Verkehrsbehinderungen gibt es in Rostock?";
        pattern = "SELECT DISTINCT ?uri WHERE { ?uri res/0 res/1 ; res/2 res/3 }";

        SemanticAnalysisHelper h=new SemanticAnalysisHelperGerman();
        h.determineQueryType(query);
        FillTemplatePatternsWithResources l=new FillTemplatePatternsWithResources(h);
        l.extractEntities(query);
//        Utilities u=new Utilities();
//        List<String>quer=u.fillTemplates(pattern,l);
//        System.out.println();
        //u.fillPattern(pattern,l);
        //SPARQLUtilities.retrieveBinings(u.fillWithTuples(pattern,l));
    }
    @Test
    public void fillTestTriplePatterns(){
        //String pattern="SELECT (COUNT(DISTINCT ?uri) AS ?c) WHERE{ ?uri a <^var_0^> ; <^var_1^> <^var_2^> }";
        String pattern="v_r_v";
        String pattern2="v_r_r";
        List<TripleTemplate>templates=new ArrayList();
        templates.add(new TripleTemplate(pattern));
        templates.add(new TripleTemplate(pattern2));
        //String pattern="SELECT ?uri WHERE{ ?uri a <^var_0^> ; <^var_1^> <^var_2^> }";
        String query="Which airports are located in California, USA?";
        //FillTemplatePatternsWithResources templateFiller=new FillTemplatePatternsWithResources(templates, new SemanticAnalysisHelper());
        FillTemplatePatternsWithResources templateFiller=new FillTemplatePatternsWithResources( new SemanticAnalysisHelper());
        templateFiller.extractEntities(query);
        Utilities u=new Utilities();
        //u.fillWithTuples(pattern,l);
        //u.fillPattern(pattern,l);
        //SPARQLUtilities.retrieveBinings(u.fillWithTuples(pattern,l));
    }
    @Test
    public void testWordNetWrapper(){
        //WordNetWrapper wordNet = new WordNetWrapper();
        //Map<String, String> synonyms = wordNet.lookUpWords("death");
        String word="death";
        POS pos= POS.n;
  /*      Set<String> synonyms = JAWJAW.findSynonyms(word, pos);
        Set<String> hypernyms = JAWJAW.findHypernyms(word, pos);
        Set<String> hyponyms = JAWJAW.findHyponyms(word, pos);
        Set<String> meronyms = JAWJAW.findMeronyms(word, pos);
        Set<String> holonyms = JAWJAW.findHolonyms(word, pos);
        Set<String> translations = JAWJAW.findTranslations(word, pos);
        Set<String> definitions = JAWJAW.findDefinitions(word, pos);
        Set<String> attributes = JAWJAW.findAttributes(word, pos);
        Set<String> causes = JAWJAW.findCauses(word, pos);
        Set<String> domains = JAWJAW.findDomains(word, pos);
        Set<String> entailment = JAWJAW.findEntailments(word, pos);
        Set<String> fitsInDomains = JAWJAW.findInDomains(word, pos);
        Set<String> seeAlso = JAWJAW.findSeeAlso(word, pos);
        Set<String> antonyms = JAWJAW.findAntonyms(word, pos);
        Set<String> hasInstances = JAWJAW.findHasInstances(word, pos);
        Set<String> instances = JAWJAW.findInstances(word, pos);
        Set<String> simliarTo = JAWJAW.findSimilarTo(word, pos);
        System.out.println("synonyms of " + word + " : \t" + synonyms);
        System.out.println("hypernyms of " + word + " : \t" + hypernyms);
        System.out.println("hyponyms of " + word + " : \t" + hyponyms);
        System.out.println("meronyms of " + word + " : \t" + meronyms);
        System.out.println("holonyms of " + word + " : \t" + holonyms);
        System.out.println("translations of " + word + " : \t" + translations);
        System.out.println("definitions of " + word + " : \t" + definitions);
        System.out.println("attributes " + word + " : \t" + attributes);
        System.out.println("causes " + word + " : \t" + causes);
        System.out.println("domains " + word + " : \t" + domains);
        System.out.println("entailment " + word + " : \t" + entailment);
        System.out.println("fits in domains " + word + " : \t" + fitsInDomains);
        System.out.println("see also " + word + " : \t" + seeAlso);
        System.out.println("antonyms " + word + " : \t" + antonyms);
        System.out.println("has instances " + word + " : \t" + hasInstances);
        System.out.println("instances " + word + " : \t" + instances);
        System.out.println("simliarTo " + word + " : \t" + simliarTo);
*/
        List<Word> words = WordDAO.findWordsByLemmaAndPos(word, pos);
        for(Word word1:words){
        List<Sense> senses = SenseDAO.findSensesByWordid(((Word)word1).getWordid());
        for(Sense s:senses) {
            String synsetId = ((Sense) s).getSynset();
            Synset synset = SynsetDAO.findSynsetBySynset(synsetId);

            SynsetDef synsetDef = SynsetDefDAO.findSynsetDefBySynsetAndLang(synsetId, Lang.eng);
            List<Synlink> synlinks = SynlinkDAO.findSynlinksBySynset(synsetId);
            System.out.println(word1);
            System.out.println(s);
            System.out.println(synset);
            System.out.println(synsetDef);
            System.out.println("Links");
            for (Synlink l : synlinks)
                System.out.println(SynsetDAO.findSynsetBySynset(l.getSynset2()));
            System.out.println();
        }
        System.out.println();
        }
    }

    public static void main(String[] args) {
//        benchmark();
        generateQueriesForAllTemplates(args);

    }

    public static void generateQueriesForAllTemplates(String[] args) {
        String query = "Gib die Beschreibung der Carsharing Station in der Budapester Str. aus.";
        query = "Welche Verkehrsbehinderungen gibt es in Rostock?";
        query = "Welche Apotheke hat die Mailadresse rosen-apotheke-hro@t-online.de?";
        query = "Welche Postanschrift hat die Ort Edling?";
        PipelineController qaPipeline = PipelineProvider.getQAPipeline();
        Map<String, QueryTemplateMapping> mappings = qaPipeline.getMappings();

        SemanticAnalysisHelper h=new SemanticAnalysisHelperGerman();
        h.determineQueryType(query);
        FillTemplatePatternsWithResources l=new FillTemplatePatternsWithResources(h);
        l.extractEntities(query);

//        Set<String> allTemplates = new HashSet<>();
//        mappings.values().forEach(queryTemplateMapping -> allTemplates.addAll(queryTemplateMapping.getAllAvailableTemples()));
//
//        System.out.println(allTemplates);
//
//        for(String template : allTemplates)
//        {
//            List<String> generatedQueries = Utilities.fillTemplates(template,l);
//            System.out.println(generatedQueries);
//        }

//        Utilities.fillTemplates("SELECT (COUNT(?ft) AS ?count) WHERE { ?ft res/0 res/1 ; ?rel res/2 }", l);
        System.out.println(Utilities.fillTemplates("SELECT DISTINCT ?uri WHERE { ?uri res/0 res/1 ; res/2 res/3 }", l));
//        Utilities.fillTemplates("SELECT DISTINCT ?bf WHERE { ?bf res/0 res/1 . ?gl res/2 ?bf } GROUP BY ?bf ORDER BY DESC(COUNT(?gl)) LIMIT 1", l);
//        SPARQLUtilities.retrieveBinings(u.fillWithTuples(pattern,l));
    }

    public static void benchmark() {
        List<IQuestion> questions = AnnotateQualD8.loadLimbo();
        int total = 0;
        int found = 0;
        SemanticAnalysisHelper h=new SemanticAnalysisHelperGerman();

        boolean printAll = true;
        for(IQuestion q : questions)
        {
            String questionString = q.getLanguageToQuestion().get("de");
//            if(!questionString.equals("Wie ist die Höhe von Gleis 1 des Hamburger Hbfs?"))
//                continue;

//            // Override
//            questionString = "ist die";

            System.out.println("QUESTION: " + questionString);
            String sparqlQuery = q.getSparqlQuery();
            if(sparqlQuery == null)
                continue;

            Query query = QueryFactory.create(sparqlQuery);
            List<String> queryElements = Arrays.asList(query.getQueryPattern().toString().split("\\s{1,}"));
            queryElements = queryElements.stream()
                    .filter(element -> element.startsWith("<http"))
                    .map(element -> element.replace("<", "").replace(">", ""))
                    .collect(Collectors.toList());

            h.determineQueryType(questionString);
            FillTemplatePatternsWithResources l=new FillTemplatePatternsWithResources(h);
            l.extractEntities(questionString);

            List<String> allFound = new ArrayList<>();
            allFound.addAll(l.entityCandidates.stream().map(ResourceCandidate::getUri).collect(Collectors.toList()));
            allFound.addAll(l.classCandidates.stream().map(ResourceCandidate::getUri).collect(Collectors.toList()));
            allFound.addAll(l.propertyCandidates.stream().map(ResourceCandidate::getUri).collect(Collectors.toList()));

            total += queryElements.size();

            List<String> retainList = new ArrayList<>(queryElements);
            retainList.retainAll(allFound);
            List<String> deleteList = new ArrayList<>(queryElements);
            deleteList.removeAll(allFound);

            found += retainList.size();

            if(printAll || deleteList.size() > 0) {
                // Sort for print
                queryElements.sort(String::compareTo);
                allFound.sort(String::compareTo);
                retainList.sort(String::compareTo);
                deleteList.sort(String::compareTo);


                System.out.println("QUERY: " + sparqlQuery);
                System.out.println("All in Query: " + queryElements);
                System.out.println("All EL responses: " + allFound);
                System.out.println(String.format("Found(%s/%s): %s", retainList.size(), queryElements.size(), retainList));
                System.out.println(String.format("Not found(%s/%s): %s", deleteList.size(), queryElements.size(), deleteList));
                System.out.println();
            }
        }

        System.out.println("Coverage %: " + (found/(double)total)*100);
    }
}

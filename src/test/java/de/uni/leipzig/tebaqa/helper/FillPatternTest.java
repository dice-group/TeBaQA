package de.uni.leipzig.tebaqa.helper;


import de.uni.leipzig.tebaqa.controller.SemanticAnalysisHelper;
import de.uni.leipzig.tebaqa.controller.SemanticAnalysisHelperGerman;
import de.uni.leipzig.tebaqa.model.TripleTemplate;
import edu.cmu.lti.jawjaw.db.*;
import edu.cmu.lti.jawjaw.pobj.*;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

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
        SemanticAnalysisHelper h=new SemanticAnalysisHelperGerman();
        h.determineQueryType(query);
        FillTemplatePatternsWithResources l=new FillTemplatePatternsWithResources(h);
        l.extractEntities(query);
        Utilities u=new Utilities();
        List<String>quer=u.fillTemplates(pattern,l);
        System.out.println();
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
}

package de.uni.leipzig.tebaqa.controller;

import com.google.common.collect.Lists;
import com.hp.hpl.jena.rdf.model.RDFNode;
import de.uni.leipzig.tebaqa.helper.NTripleParser;
import de.uni.leipzig.tebaqa.helper.SPARQLUtilities;
import de.uni.leipzig.tebaqa.model.CustomQuestion;
import de.uni.leipzig.tebaqa.model.QueryTemplateMapping;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SemanticAnalysisHelperTest {
    @Test
    public void testExtractTemplatesMapsGraph() throws Exception {
        List<CustomQuestion> customQuestions = new ArrayList<>();
        Map<String, List<String>> goldenAnswers = new HashMap<>();
        String graph = " {\"1\" @\"p\" \"2\"}";
        customQuestions.add(new CustomQuestion("PREFIX res: <http://dbpedia.org/resource> " +
                "PREFIX dbo: <http://dbpedia.org/ontology> " +
                "SELECT DISTINCT ?uri " +
                "WHERE { " +
                "        res:Douglas_Hofstadter dbo:award ?uri . " +
                "}", "Which awards did Douglas Hofstadter win?", null, graph, goldenAnswers));
        SemanticAnalysisHelper analysisHelper = new SemanticAnalysisHelper();
        NTripleParser nTripleParser = new NTripleParser();
        Set<RDFNode> nodes = nTripleParser.getNodes();
        List<String> dBpediaProperties = SPARQLUtilities.getDBpediaProperties();
        Map<String, QueryTemplateMapping> mappings = analysisHelper.extractTemplates(customQuestions, Lists.newArrayList(nodes), dBpediaProperties);

        assertTrue(mappings.size() == 1);
        assertTrue(mappings.containsKey(graph));
    }

    @Test
    public void testExtractTemplatesContainsSelectQueryPattern() throws Exception {
        List<CustomQuestion> customQuestions = new ArrayList<>();
        Map<String, List<String>> goldenAnswers = new HashMap<>();
        String graph = " {\"1\" @\"p\" \"2\"}";
        customQuestions.add(new CustomQuestion("PREFIX res: <http://dbpedia.org/resource> " +
                "PREFIX dbo: <http://dbpedia.org/ontology> " +
                "SELECT DISTINCT ?uri " +
                "WHERE { " +
                "        res:Douglas_Hofstadter dbo:award ?uri . " +
                "}", "Which awards did Douglas Hofstadter win?", null, graph, goldenAnswers));
        SemanticAnalysisHelper analysisHelper = new SemanticAnalysisHelper();
        NTripleParser nTripleParser = new NTripleParser();
        Set<RDFNode> nodes = nTripleParser.getNodes();
        List<String> dBpediaProperties = SPARQLUtilities.getDBpediaProperties();
        Map<String, QueryTemplateMapping> mappings = analysisHelper.extractTemplates(customQuestions, Lists.newArrayList(nodes), dBpediaProperties);

        Set<String> expectedSelectPatterns = new HashSet<>();
        expectedSelectPatterns.add("SELECT DISTINCT ?uri WHERE { <^VAR_0^> <^VAR_1^> ?uri . }");

        assertTrue(mappings.size() == 1);
        assertEquals(expectedSelectPatterns, mappings.get(graph).getSelectTemplates());
    }

    @Test
    public void testExtractTemplatesContainsSelectQueryPattern2() throws Exception {
        List<CustomQuestion> customQuestions = new ArrayList<>();
        Map<String, List<String>> goldenAnswers = new HashMap<>();
        String graph = " {\"1\" @\"p\" \"2\"}";
        customQuestions.add(new CustomQuestion("SELECT DISTINCT ?uri WHERE {  <http://dbpedia.org/resource/San_Pedro_de_Atacama> <http://dbpedia.org/ontology/timeZone> ?uri . }",
                "What is the timezone in San Pedro de Atacama?", null, graph, goldenAnswers));
        SemanticAnalysisHelper analysisHelper = new SemanticAnalysisHelper();
        NTripleParser nTripleParser = new NTripleParser();
        Set<RDFNode> nodes = nTripleParser.getNodes();
        List<String> dBpediaProperties = SPARQLUtilities.getDBpediaProperties();
        Map<String, QueryTemplateMapping> mappings = analysisHelper.extractTemplates(customQuestions, Lists.newArrayList(nodes), dBpediaProperties);

        Set<String> expectedSelectPatterns = new HashSet<>();
        expectedSelectPatterns.add("SELECT DISTINCT ?uri WHERE { <^VAR_0^> <^VAR_1^> ?uri . }");

        assertTrue(mappings.size() == 1);
        assertEquals(expectedSelectPatterns, mappings.get(graph).getSelectTemplates());
    }

}
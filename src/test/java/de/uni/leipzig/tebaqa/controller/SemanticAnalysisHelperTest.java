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

    @Test
    public void testExtractTemplatesDetectsIsomorphTemplates() throws Exception {
        List<CustomQuestion> customQuestions = new ArrayList<>();
        Map<String, List<String>> goldenAnswers = new HashMap<>();
        String graph = " {\"1\" @\"p\" \"2\"}";
        customQuestions.add(new CustomQuestion("SELECT DISTINCT ?uri WHERE {  <http://dbpedia.org/resource/San_Pedro_de_Atacama> <http://dbpedia.org/ontology/timeZone> ?uri . }",
                "What is the timezone in San Pedro de Atacama?", null, graph, goldenAnswers));
        customQuestions.add(new CustomQuestion("SELECT DISTINCT ?num WHERE {  <http://dbpedia.org/resource/Colombo_Lighthouse> <http://dbpedia.org/ontology/height> ?num . } ",
                "How high is the lighthouse in Colombo?", null, graph, goldenAnswers));
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
    public void testExtractTemplatesIgnoresCount() throws Exception {
        List<CustomQuestion> customQuestions = new ArrayList<>();
        Map<String, List<String>> goldenAnswers = new HashMap<>();
        String graph = " {\"1\" @\"p\" \"2\"}";
        customQuestions.add(new CustomQuestion("SELECT (COUNT(DISTINCT ?x) as ?c) WHERE {  <http://dbpedia.org/resource/Turkmenistan> <http://dbpedia.org/ontology/language> ?x . } ",
                "How many languages are spoken in Turkmenistan?", null, graph, goldenAnswers));
        customQuestions.add(new CustomQuestion("SELECT DISTINCT ?num WHERE {  <http://dbpedia.org/resource/Colombo_Lighthouse> <http://dbpedia.org/ontology/height> ?num . } ",
                "How high is the lighthouse in Colombo?", null, graph, goldenAnswers));
        customQuestions.add(new CustomQuestion("SELECT (COUNT(DISTINCT ?x) as ?c) WHERE {  <http://dbpedia.org/resource/Turkmenistan> <http://dbpedia.org/ontology/language> ?x . } ",
                "How many languages are spoken in Turkmenistan?", null, graph, goldenAnswers));
        SemanticAnalysisHelper analysisHelper = new SemanticAnalysisHelper();
        NTripleParser nTripleParser = new NTripleParser();
        Set<RDFNode> nodes = nTripleParser.getNodes();
        List<String> dBpediaProperties = SPARQLUtilities.getDBpediaProperties();
        Map<String, QueryTemplateMapping> mappings = analysisHelper.extractTemplates(customQuestions, Lists.newArrayList(nodes), dBpediaProperties);

        Set<String> expectedSelectPatterns = new HashSet<>();
        expectedSelectPatterns.add("SELECT DISTINCT ?num WHERE { <^VAR_0^> <^VAR_1^> ?num . }");

        assertTrue(mappings.size() == 1);
        assertEquals(expectedSelectPatterns, mappings.get(graph).getSelectTemplates());
    }

    @Test
    public void testExtractTemplatesIgnoresSum() throws Exception {
        List<CustomQuestion> customQuestions = new ArrayList<>();
        Map<String, List<String>> goldenAnswers = new HashMap<>();
        String graph = " {\"1\" @\"p\" \"2\"}";
        customQuestions.add(new CustomQuestion("SELECT (SUM(DISTINCT ?x) as ?c) WHERE {  <http://dbpedia.org/resource/Turkmenistan> <http://dbpedia.org/ontology/language> ?x . } ",
                "How many languages are spoken in Turkmenistan?", null, graph, goldenAnswers));
        customQuestions.add(new CustomQuestion("SELECT DISTINCT ?num WHERE {  <http://dbpedia.org/resource/Colombo_Lighthouse> <http://dbpedia.org/ontology/height> ?num . } ",
                "How high is the lighthouse in Colombo?", null, graph, goldenAnswers));
        customQuestions.add(new CustomQuestion("SELECT (SUM(DISTINCT ?x) as ?c) WHERE {  <http://dbpedia.org/resource/Turkmenistan> <http://dbpedia.org/ontology/language> ?x . } ",
                "How many languages are spoken in Turkmenistan?", null, graph, goldenAnswers));
        SemanticAnalysisHelper analysisHelper = new SemanticAnalysisHelper();
        NTripleParser nTripleParser = new NTripleParser();
        Set<RDFNode> nodes = nTripleParser.getNodes();
        List<String> dBpediaProperties = SPARQLUtilities.getDBpediaProperties();
        Map<String, QueryTemplateMapping> mappings = analysisHelper.extractTemplates(customQuestions, Lists.newArrayList(nodes), dBpediaProperties);

        Set<String> expectedSelectPatterns = new HashSet<>();
        expectedSelectPatterns.add("SELECT DISTINCT ?num WHERE { <^VAR_0^> <^VAR_1^> ?num . }");

        assertTrue(mappings.size() == 1);
        assertEquals(expectedSelectPatterns, mappings.get(graph).getSelectTemplates());
    }

    @Test
    public void testExtractTemplatesIgnoresAvg() throws Exception {
        List<CustomQuestion> customQuestions = new ArrayList<>();
        Map<String, List<String>> goldenAnswers = new HashMap<>();
        String graph = " {\"1\" @\"p\" \"2\"}";
        customQuestions.add(new CustomQuestion("SELECT (AVG(DISTINCT ?x) as ?c) WHERE {  <http://dbpedia.org/resource/Turkmenistan> <http://dbpedia.org/ontology/language> ?x . } ",
                "How many languages are spoken in Turkmenistan?", null, graph, goldenAnswers));
        customQuestions.add(new CustomQuestion("SELECT DISTINCT ?num WHERE {  <http://dbpedia.org/resource/Colombo_Lighthouse> <http://dbpedia.org/ontology/height> ?num . } ",
                "How high is the lighthouse in Colombo?", null, graph, goldenAnswers));
        customQuestions.add(new CustomQuestion("SELECT (AVG(DISTINCT ?x) as ?c) WHERE {  <http://dbpedia.org/resource/Turkmenistan> <http://dbpedia.org/ontology/language> ?x . } ",
                "How many languages are spoken in Turkmenistan?", null, graph, goldenAnswers));
        SemanticAnalysisHelper analysisHelper = new SemanticAnalysisHelper();
        NTripleParser nTripleParser = new NTripleParser();
        Set<RDFNode> nodes = nTripleParser.getNodes();
        List<String> dBpediaProperties = SPARQLUtilities.getDBpediaProperties();
        Map<String, QueryTemplateMapping> mappings = analysisHelper.extractTemplates(customQuestions, Lists.newArrayList(nodes), dBpediaProperties);

        Set<String> expectedSelectPatterns = new HashSet<>();
        expectedSelectPatterns.add("SELECT DISTINCT ?num WHERE { <^VAR_0^> <^VAR_1^> ?num . }");

        assertTrue(mappings.size() == 1);
        assertEquals(expectedSelectPatterns, mappings.get(graph).getSelectTemplates());
    }

    @Test
    public void testExtractTemplatesIgnoresMin() throws Exception {
        List<CustomQuestion> customQuestions = new ArrayList<>();
        Map<String, List<String>> goldenAnswers = new HashMap<>();
        String graph = " {\"1\" @\"p\" \"2\"}";
        customQuestions.add(new CustomQuestion("SELECT (MIN(DISTINCT ?x) as ?c) WHERE {  <http://dbpedia.org/resource/Turkmenistan> <http://dbpedia.org/ontology/language> ?x . } ",
                "How many languages are spoken in Turkmenistan?", null, graph, goldenAnswers));
        customQuestions.add(new CustomQuestion("SELECT DISTINCT ?num WHERE {  <http://dbpedia.org/resource/Colombo_Lighthouse> <http://dbpedia.org/ontology/height> ?num . } ",
                "How high is the lighthouse in Colombo?", null, graph, goldenAnswers));
        customQuestions.add(new CustomQuestion("SELECT (MIN(DISTINCT ?x) as ?c) WHERE {  <http://dbpedia.org/resource/Turkmenistan> <http://dbpedia.org/ontology/language> ?x . } ",
                "How many languages are spoken in Turkmenistan?", null, graph, goldenAnswers));
        SemanticAnalysisHelper analysisHelper = new SemanticAnalysisHelper();
        NTripleParser nTripleParser = new NTripleParser();
        Set<RDFNode> nodes = nTripleParser.getNodes();
        List<String> dBpediaProperties = SPARQLUtilities.getDBpediaProperties();
        Map<String, QueryTemplateMapping> mappings = analysisHelper.extractTemplates(customQuestions, Lists.newArrayList(nodes), dBpediaProperties);

        Set<String> expectedSelectPatterns = new HashSet<>();
        expectedSelectPatterns.add("SELECT DISTINCT ?num WHERE { <^VAR_0^> <^VAR_1^> ?num . }");

        assertTrue(mappings.size() == 1);
        assertEquals(expectedSelectPatterns, mappings.get(graph).getSelectTemplates());
    }

    @Test
    public void testExtractTemplatesIgnoresMax() throws Exception {
        List<CustomQuestion> customQuestions = new ArrayList<>();
        Map<String, List<String>> goldenAnswers = new HashMap<>();
        String graph = " {\"1\" @\"p\" \"2\"}";
        customQuestions.add(new CustomQuestion("SELECT (MAX(DISTINCT ?x) as ?c) WHERE {  <http://dbpedia.org/resource/Turkmenistan> <http://dbpedia.org/ontology/language> ?x . } ",
                "How many languages are spoken in Turkmenistan?", null, graph, goldenAnswers));
        customQuestions.add(new CustomQuestion("SELECT DISTINCT ?num WHERE {  <http://dbpedia.org/resource/Colombo_Lighthouse> <http://dbpedia.org/ontology/height> ?num . } ",
                "How high is the lighthouse in Colombo?", null, graph, goldenAnswers));
        customQuestions.add(new CustomQuestion("SELECT (MAX(DISTINCT ?x) as ?c) WHERE {  <http://dbpedia.org/resource/Turkmenistan> <http://dbpedia.org/ontology/language> ?x . } ",
                "How many languages are spoken in Turkmenistan?", null, graph, goldenAnswers));
        SemanticAnalysisHelper analysisHelper = new SemanticAnalysisHelper();
        NTripleParser nTripleParser = new NTripleParser();
        Set<RDFNode> nodes = nTripleParser.getNodes();
        List<String> dBpediaProperties = SPARQLUtilities.getDBpediaProperties();
        Map<String, QueryTemplateMapping> mappings = analysisHelper.extractTemplates(customQuestions, Lists.newArrayList(nodes), dBpediaProperties);

        Set<String> expectedSelectPatterns = new HashSet<>();
        expectedSelectPatterns.add("SELECT DISTINCT ?num WHERE { <^VAR_0^> <^VAR_1^> ?num . }");

        assertTrue(mappings.size() == 1);
        assertEquals(expectedSelectPatterns, mappings.get(graph).getSelectTemplates());
    }

    @Test
    public void testExtractTemplatesIgnoresFilter() throws Exception {
        List<CustomQuestion> customQuestions = new ArrayList<>();
        Map<String, List<String>> goldenAnswers = new HashMap<>();
        String graph = " {\"1\" @\"p\" \"2\"}";
        customQuestions.add(new CustomQuestion("SELECT (FILTER(DISTINCT ?x) as ?c) WHERE {  <http://dbpedia.org/resource/Turkmenistan> <http://dbpedia.org/ontology/language> ?x . } ",
                "How many languages are spoken in Turkmenistan?", null, graph, goldenAnswers));
        customQuestions.add(new CustomQuestion("SELECT DISTINCT ?num WHERE {  <http://dbpedia.org/resource/Colombo_Lighthouse> <http://dbpedia.org/ontology/height> ?num . } ",
                "How high is the lighthouse in Colombo?", null, graph, goldenAnswers));
        customQuestions.add(new CustomQuestion("SELECT (FILTER(DISTINCT ?x) as ?c) WHERE {  <http://dbpedia.org/resource/Turkmenistan> <http://dbpedia.org/ontology/language> ?x . } ",
                "How many languages are spoken in Turkmenistan?", null, graph, goldenAnswers));
        SemanticAnalysisHelper analysisHelper = new SemanticAnalysisHelper();
        NTripleParser nTripleParser = new NTripleParser();
        Set<RDFNode> nodes = nTripleParser.getNodes();
        List<String> dBpediaProperties = SPARQLUtilities.getDBpediaProperties();
        Map<String, QueryTemplateMapping> mappings = analysisHelper.extractTemplates(customQuestions, Lists.newArrayList(nodes), dBpediaProperties);

        Set<String> expectedSelectPatterns = new HashSet<>();
        expectedSelectPatterns.add("SELECT DISTINCT ?num WHERE { <^VAR_0^> <^VAR_1^> ?num . }");

        assertTrue(mappings.size() == 1);
        assertEquals(expectedSelectPatterns, mappings.get(graph).getSelectTemplates());
    }

    @Test
    public void testExtractTemplatesIgnoresBound() throws Exception {
        List<CustomQuestion> customQuestions = new ArrayList<>();
        Map<String, List<String>> goldenAnswers = new HashMap<>();
        String graph = " {\"1\" @\"p\" \"2\"}";
        customQuestions.add(new CustomQuestion("SELECT (BOUND(DISTINCT ?x) as ?c) WHERE {  <http://dbpedia.org/resource/Turkmenistan> <http://dbpedia.org/ontology/language> ?x . } ",
                "How many languages are spoken in Turkmenistan?", null, graph, goldenAnswers));
        customQuestions.add(new CustomQuestion("SELECT DISTINCT ?num WHERE {  <http://dbpedia.org/resource/Colombo_Lighthouse> <http://dbpedia.org/ontology/height> ?num . } ",
                "How high is the lighthouse in Colombo?", null, graph, goldenAnswers));
        customQuestions.add(new CustomQuestion("SELECT (BOUND(DISTINCT ?x) as ?c) WHERE {  <http://dbpedia.org/resource/Turkmenistan> <http://dbpedia.org/ontology/language> ?x . } ",
                "How many languages are spoken in Turkmenistan?", null, graph, goldenAnswers));
        SemanticAnalysisHelper analysisHelper = new SemanticAnalysisHelper();
        NTripleParser nTripleParser = new NTripleParser();
        Set<RDFNode> nodes = nTripleParser.getNodes();
        List<String> dBpediaProperties = SPARQLUtilities.getDBpediaProperties();
        Map<String, QueryTemplateMapping> mappings = analysisHelper.extractTemplates(customQuestions, Lists.newArrayList(nodes), dBpediaProperties);

        Set<String> expectedSelectPatterns = new HashSet<>();
        expectedSelectPatterns.add("SELECT DISTINCT ?num WHERE { <^VAR_0^> <^VAR_1^> ?num . }");

        assertTrue(mappings.size() == 1);
        assertEquals(expectedSelectPatterns, mappings.get(graph).getSelectTemplates());
    }


    @Test
    public void testExtractTemplatesIgnoresLimit() throws Exception {
        List<CustomQuestion> customQuestions = new ArrayList<>();
        Map<String, List<String>> goldenAnswers = new HashMap<>();
        String graph = " {\"1\" @\"p\" \"2\"}";
        customQuestions.add(new CustomQuestion("SELECT ?x WHERE {  <http://dbpedia.org/resource/Turkmenistan> <http://dbpedia.org/ontology/language> ?x . } LIMIT 1 ",
                "How many languages are spoken in Turkmenistan?", null, graph, goldenAnswers));
        customQuestions.add(new CustomQuestion("SELECT DISTINCT ?num WHERE {  <http://dbpedia.org/resource/Colombo_Lighthouse> <http://dbpedia.org/ontology/height> ?num . } ",
                "How high is the lighthouse in Colombo?", null, graph, goldenAnswers));
        customQuestions.add(new CustomQuestion("SELECT ?x WHERE {  <http://dbpedia.org/resource/Turkmenistan> <http://dbpedia.org/ontology/language> ?x . } LIMIT 1 ",
                "How many languages are spoken in Turkmenistan?", null, graph, goldenAnswers));
        SemanticAnalysisHelper analysisHelper = new SemanticAnalysisHelper();
        NTripleParser nTripleParser = new NTripleParser();
        Set<RDFNode> nodes = nTripleParser.getNodes();
        List<String> dBpediaProperties = SPARQLUtilities.getDBpediaProperties();
        Map<String, QueryTemplateMapping> mappings = analysisHelper.extractTemplates(customQuestions, Lists.newArrayList(nodes), dBpediaProperties);

        Set<String> expectedSelectPatterns = new HashSet<>();
        expectedSelectPatterns.add("SELECT DISTINCT ?num WHERE { <^VAR_0^> <^VAR_1^> ?num . }");

        assertTrue(mappings.size() == 1);
        assertEquals(expectedSelectPatterns, mappings.get(graph).getSelectTemplates());
    }

    @Test
    public void testDetectQuestionAnswerTypeNumberAnswer() throws Exception {
        int answerType = SemanticAnalysisHelper.detectQuestionAnswerType("How many cities exist?");
        assertEquals(SemanticAnalysisHelper.NUMBER_ANSWER_TYPE, answerType);
    }

    @Test
    public void testDetectQuestionAnswerTypeNumberAnswer2() throws Exception {
        int answerType = SemanticAnalysisHelper.detectQuestionAnswerType("How many companies were founded in the same year as Google?");
        assertEquals(SemanticAnalysisHelper.NUMBER_ANSWER_TYPE, answerType);
    }

    @Test
    public void testDetectQuestionAnswerTypeBooleanAnswer() throws Exception {
        int answerType = SemanticAnalysisHelper.detectQuestionAnswerType("Is there a god?");
        assertEquals(SemanticAnalysisHelper.BOOLEAN_ANSWER_TYPE, answerType);
    }

    @Test
    public void testDetectQuestionAnswerTypeBooleanAnswer2() throws Exception {
        int answerType = SemanticAnalysisHelper.detectQuestionAnswerType("Does Neymar play for Real Madrid?");
        assertEquals(SemanticAnalysisHelper.BOOLEAN_ANSWER_TYPE, answerType);
    }

    @Test
    public void testDetectQuestionAnswerTypeBooleanAnswer3() throws Exception {
        int answerType = SemanticAnalysisHelper.detectQuestionAnswerType("Was the Cuban Missile Crisis earlier than the Bay of Pigs Invasion?");
        assertEquals(SemanticAnalysisHelper.BOOLEAN_ANSWER_TYPE, answerType);
    }

    @Test
    public void testDetectQuestionAnswerTypeListAnswer() throws Exception {
        int answerType = SemanticAnalysisHelper.detectQuestionAnswerType("Give me all professional skateboarders from Sweden.");
        assertEquals(SemanticAnalysisHelper.LIST_ANSWER_TYPE, answerType);
    }

    @Test
    public void testDetectQuestionAnswerTypeListAnswer2() throws Exception {
        int answerType = SemanticAnalysisHelper.detectQuestionAnswerType("Which ingredients do I need for carrot cake?");
        assertEquals(SemanticAnalysisHelper.LIST_ANSWER_TYPE, answerType);
    }

    @Test
    public void testDetectQuestionAnswerTypeListAnswer3() throws Exception {
        int answerType = SemanticAnalysisHelper.detectQuestionAnswerType("List all episodes of the first season of the HBO television series The Sopranos.");
        assertEquals(SemanticAnalysisHelper.LIST_ANSWER_TYPE, answerType);
    }

    @Test
    public void testDetectQuestionAnswerTypeSingleAnswer() throws Exception {
        int answerType = SemanticAnalysisHelper.detectQuestionAnswerType("Which computer scientist won an oscar?");
        assertEquals(SemanticAnalysisHelper.SINGLE_RESOURCE_TYPE, answerType);
    }

    @Test
    public void testDetectQuestionAnswerTypeSingleAnswer2() throws Exception {
        int answerType = SemanticAnalysisHelper.detectQuestionAnswerType("In which UK city are the headquarters of the MI6?");
        assertEquals(SemanticAnalysisHelper.SINGLE_RESOURCE_TYPE, answerType);
    }

    @Test
    public void testGetBestAnswerWithSameResult() throws Exception {
        int answerType = SemanticAnalysisHelper.detectQuestionAnswerType("Which books by Kerouac were published by Viking Press?");
        SemanticAnalysisHelper semanticAnalysisHelper = new SemanticAnalysisHelper();
        List<Map<Integer, List<String>>> results = new ArrayList<>();
        Map<Integer, List<String>> result1 = new HashMap<>();
        List<String> resultSet1 = new ArrayList<>();
        resultSet1.add("http://dbpedia.org/resource/On_the_Road");
        resultSet1.add("http://dbpedia.org/resource/Atop_an_Underwood:_Early_Stories_and_Other_Writings");
        resultSet1.add("http://dbpedia.org/resource/Door_Wide_Open");
        result1.put(answerType, resultSet1);
        results.add(result1);

        Map<Integer, List<String>> result2 = new HashMap<>();
        List<String> resultSet2 = new ArrayList<>();
        resultSet2.add("http://dbpedia.org/resource/On_the_Road");
        resultSet2.add("http://dbpedia.org/resource/Atop_an_Underwood:_Early_Stories_and_Other_Writings");
        resultSet2.add("http://dbpedia.org/resource/Door_Wide_Open");
        result2.put(answerType, resultSet2);
        results.add(result2);

        HashSet<String> bestAnswer = semanticAnalysisHelper.getBestAnswer(results, new StringBuilder(), answerType);
        assertEquals(3, bestAnswer.size());
        assertTrue(bestAnswer.contains("http://dbpedia.org/resource/On_the_Road"));
        assertTrue(bestAnswer.contains("http://dbpedia.org/resource/Atop_an_Underwood:_Early_Stories_and_Other_Writings"));
        assertTrue(bestAnswer.contains("http://dbpedia.org/resource/Door_Wide_Open"));
    }
}

package de.uni.leipzig.tebaqa.helper;

import com.hp.hpl.jena.rdf.model.RDFNode;
import de.uni.leipzig.tebaqa.controller.SemanticAnalysisHelper;
import de.uni.leipzig.tebaqa.model.CustomQuestion;
import de.uni.leipzig.tebaqa.model.QueryTemplateMapping;
import de.uni.leipzig.tebaqa.model.SPARQLResultSet;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class QueryMappingFactoryTest {

    @Before
    public void setUp() {
        StanfordPipelineProvider.getSingletonPipelineInstance();
    }

    @Test
    public void testCreateQueryPattern() {
        String question = "In which country does the Nile start?";
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "SELECT DISTINCT ?uri WHERE { res:Nile dbo:city ?uri . }";
        Map<String, String> posSequence = new HashMap<>();
        posSequence.put("start", "VB0");
        posSequence.put("city", "NN1");
        posSequence.put("Nile", "NNP2");
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, posSequence, query, nodes);

        assertEquals("SELECT DISTINCT ?uri WHERE { <^NNP2^> <^NN1^> ?uri . }", queryMappingFactory.getQueryPattern());
    }

    @Test
    public void testCreateQueryPatternUriMismatch() {
        String question = "In which country does the Nile start?";
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "SELECT DISTINCT ?uri WHERE { res:Niles dbo:city ?uri . }";
        Map<String, String> posSequence = new HashMap<>();
        posSequence.put("start", "VB0");
        posSequence.put("city", "NN1");
        posSequence.put("Nile", "NNP2");
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, posSequence, query, nodes);

        assertEquals("SELECT DISTINCT ?uri WHERE { <^NNP2^> <^NN1^> ?uri . }", queryMappingFactory.getQueryPattern());
    }

    @Test
    public void testCreateQueryPatternTestsForCompleteResourceString() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "SELECT DISTINCT ?uri WHERE { res:Nile dbo:city ?uri . }";
        String question = "In which country does the Nile_FooBar_FooBar start?";
        Map<String, String> posSequence = new HashMap<>();
        posSequence.put("start", "VB0");
        posSequence.put("city", "NN1");
        posSequence.put("Nile_FooBar_FooBar", "NNP2");
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, posSequence, query, nodes);

        assertEquals("SELECT DISTINCT ?uri WHERE { <http://dbpedia.org/resource/Nile> <^NN1^> ?uri . }",
                queryMappingFactory.getQueryPattern());
    }

    @Test
    public void testCreateQueryPatternWithEntityFromSpotlight() {
        String query = "SELECT DISTINCT ?uri WHERE {  <http://dbpedia.org/resource/Yeti_Airlines>" +
                " <http://dbpedia.org/resource/Airport> ?uri . }";
        String question = "Which airport does Yeti Airlines serve?";
        Map<String, String> posSequence = new HashMap<>();
        posSequence.put("serve", "VB0");
        posSequence.put("airport", "NNS1");
        posSequence.put("Airlines", "NNP2");
        posSequence.put("Yeti", "NNP3");
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, posSequence, query, nodes);

        assertEquals("SELECT DISTINCT ?uri WHERE { <^NNP3_NNP2^> <^NNS1^> ?uri . }",
                queryMappingFactory.getQueryPattern());
    }


    @Test
    public void testCreateQueryPatternWithUnknownSpotlightEntity() {
        String query = "SELECT DISTINCT ?uri WHERE {  <http://dbpedia.org/resource/Yeti_Airlines>" +
                " <http://dbpedia.org/resource/Airport> ?uri . }";
        String question = "Which airports does Yeti Airlines serve?";
        Map<String, String> posSequence = new HashMap<>();
        posSequence.put("serve", "VB0");
        posSequence.put("airports", "NNP1");
        posSequence.put("Airlines", "NNP2");
        posSequence.put("Yeti", "NNP3");
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, posSequence, query, nodes);

        assertEquals("SELECT DISTINCT ?uri WHERE { <^NNP3_NNP2^> <^NNP1^> ?uri . }",
                queryMappingFactory.getQueryPattern());
    }


    @Test
    public void testCreateQueryPatternWithMultiWordEntity() {
        String query = "SELECT DISTINCT ?uri WHERE {  " +
                "<http://dbpedia.org/resource/San_Pedro_de_Atacama> <http://dbpedia.org/ontology/timeZone> ?uri . } ";
        String question = "What is the timezone in San Pedro de Atacama?";
        Map<String, String> posSequence = new HashMap<>();
        posSequence.put("San", "NNP0");
        posSequence.put("timezone", "NN1");
        posSequence.put("Pedro", "NNP2");
        posSequence.put("de", "FW");
        posSequence.put("Atacama", "NNP3");
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, posSequence, query, nodes);

        assertEquals("SELECT DISTINCT ?uri WHERE { <^NNP0_NNP2_FW_NNP3^> <^NN1^> ?uri . } ",
                queryMappingFactory.getQueryPattern());
    }

    @Test
    public void testQueryWithNamespaces() {
        String question = "In which country does the Nile start?";
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "SELECT DISTINCT ?uri WHERE { res:Nile dbo:city ?uri . }";
        Map<String, String> posSequence = new HashMap<>();
        posSequence.put("start", "VB0");
        posSequence.put("city", "NN1");
        posSequence.put("Nile", "NNP2");
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, posSequence, query, nodes);

        assertEquals("SELECT DISTINCT ?uri WHERE { <^NNP2^> <^NN1^> ?uri . }", queryMappingFactory.getQueryPattern());
    }

    //TODO Fix test
    @Test
    @Ignore
    public void testQuery2() {
        String question = "Give me all launch pads operated by NASA.";
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> \n" +
                "PREFIX res: <http://dbpedia.org/resource/> \n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
                "SELECT DISTINCT ?uri \n" +
                "WHERE { \n" +
                "        ?uri rdf:type dbo:LaunchPad .  \n" +
                "        ?uri dbo:operator res:NASA . \n" +
                "}";
        Map<String, String> posSequence = new HashMap<>();
        posSequence.put("pads", "NNS1");
        posSequence.put("NASA", "NNP4");
        posSequence.put("me", "PRP0");
        posSequence.put("launch", "NN2");
        posSequence.put("operated", "VBN3");
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, posSequence, query, nodes);

        assertEquals("SELECT DISTINCT ?uri WHERE { ?uri <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <^NN2_NNS1^> . ?uri <^VBN3^> <^NNP4^> . }",
                queryMappingFactory.getQueryPattern());
    }


    @Test
    public void testGetQueryMappingsWithMatchingArguments() {
        String question = "In which country does the Nile start?";
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "SELECT DISTINCT ?uri WHERE { res:Nile dbo:city ?uri . }";
        Map<String, String> posSequence = new HashMap<>();
        posSequence.put("start", "VB0");
        posSequence.put("city", "NN1");
        posSequence.put("Nile", "NNP2");
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());

        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, posSequence, query, nodes);


        QueryTemplateMapping template1 = new QueryTemplateMapping(2, 2);
        template1.addSelectTemplate("SELECT DISTINCT ?uri WHERE { <^NNP4^> <^NN3^> ?x . <^NN2^> <^NN1^> ?uri }",
                "SELECT DISTINCT ?uri WHERE { ?class_0 ?property_0 ?x . ?class_1 ?property_1 ?uri .} ");

        Map<String, QueryTemplateMapping> mappings = new HashMap<>();
        mappings.put("1", template1);

        List<String> actual = queryMappingFactory.generateQueries(mappings, false);
        assertTrue(actual.size() == 1);
    }


    @Test
    public void testGetQueryMappingsWithTooFewArguments() {
        String question = "In which country does the Nile start?";
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "SELECT DISTINCT ?uri WHERE { res:Nile dbo:city ?uri . }";
        Map<String, String> posSequence = new HashMap<>();
        posSequence.put("start", "VB0");
        posSequence.put("city", "NN1");
        posSequence.put("Nile", "NNP2");
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, posSequence, query, nodes);

        QueryTemplateMapping template1 = new QueryTemplateMapping(1, 1);
        template1.addSelectTemplate("SELECT DISTINCT ?uri WHERE { <^NNP4^> <^NN3^> ?x }", "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "SELECT DISTINCT ?uri WHERE { res:Nile dbo:city ?uri . }");

        List<String> expected = new ArrayList<>();
        expected.add("SELECT DISTINCT ?uri WHERE { ?class_0 ?property_0 ?x . VALUES (?class_0) {(<http://dbpedia.org/resource/Nile>) (<http://dbpedia.org/ontology/Country>)} VALUES (?property_0) {(<http://dbpedia.org/ontology/country>) (<http://dbpedia.org/ontology/deathPlace>) (<http://dbpedia.org/ontology/start>) (<http://dbpedia.org/ontology/birthPlace>)}}");

        Map<String, QueryTemplateMapping> mappings = new HashMap<>();
        mappings.put("1", template1);

        List<String> actual = queryMappingFactory.generateQueries(mappings, false);
        assertTrue(actual.size() == 1);
        assertTrue(actual.get(0).startsWith("SELECT DISTINCT ?uri WHERE { ?class_0 ?property_0 ?x . VALUES (?class_0) {("));
    }

    @Test
    public void testGetQueryMappingsWithTooMuchProperties() {
        String question = "In which country does the Nile start?";
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "SELECT DISTINCT ?uri WHERE { res:Nile dbo:city ?uri . }";
        Map<String, String> posSequence = new HashMap<>();
        posSequence.put("start", "VB0");
        posSequence.put("city", "NN1");
        posSequence.put("Nile", "NNP2");
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, posSequence, query, nodes);

        QueryTemplateMapping template1 = new QueryTemplateMapping(1, 0);
        template1.addSelectTemplate("SELECT DISTINCT ?uri WHERE { <^NNP4^> a ?x }", "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "SELECT DISTINCT ?uri WHERE { res:Nile dbo:city ?uri . }");

        List<String> expected = new ArrayList<>();
        expected.add("SELECT DISTINCT ?uri WHERE { ?class_0 a ?x . VALUES (?class_0) {(<http://dbpedia.org/resource/The_Nile>) (<http://dbpedia.org/resource/Nile>) (<http://dbpedia.org/resource/Country>) (<http://dbpedia.org/ontology/Country>)}}");

        Map<String, QueryTemplateMapping> mappings = new HashMap<>();
        mappings.put("", template1);

        assertEquals(expected, queryMappingFactory.generateQueries(mappings, false));
    }

    @Test
    public void testGetQueryMappingsWithTooMuchClasses() {
        String question = "In which country does the Nile start?";
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "SELECT DISTINCT ?uri WHERE { res:Nile dbo:city ?uri . }";
        Map<String, String> posSequence = new HashMap<>();
        posSequence.put("start", "VB0");
        posSequence.put("city", "NN1");
        posSequence.put("Nile", "NNP2");
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, posSequence, query, nodes);

        QueryTemplateMapping template1 = new QueryTemplateMapping(0, 1);
        template1.addSelectTemplate("SELECT DISTINCT ?uri WHERE { ?x <^NN3^> ?x }", "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "SELECT DISTINCT ?uri WHERE { res:Nile dbo:city ?uri . }");

        Map<String, QueryTemplateMapping> mappings = new HashMap<>();
        mappings.put("1", template1);

        assertTrue(queryMappingFactory.generateQueries(mappings, false).size() == 1);
    }

    @Test
    public void testGetQueryMappingsWithTooMuchArguments() {
        String question = "In which country does the Nile start?";
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "SELECT DISTINCT ?uri WHERE { res:Nile dbo:city ?uri . }";
        Map<String, String> posSequence = new HashMap<>();
        posSequence.put("start", "VB0");
        posSequence.put("city", "NN1");
        posSequence.put("Nile", "NNP2");
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, posSequence, query, nodes);

        QueryTemplateMapping template1 = new QueryTemplateMapping(3, 3);
        template1.addSelectTemplate("SELECT DISTINCT ?uri WHERE { <^NNP4^> <^NN3^> ?x. <^NNP4^> <^NN3^> ?x. <^NNP4^> <^NN3^> ?x }", "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "SELECT DISTINCT ?uri WHERE { res:Nile dbo:city ?uri . }");

        Map<String, QueryTemplateMapping> mappings = new HashMap<>();
        mappings.put("1", template1);

        assertTrue(queryMappingFactory.generateQueries(mappings, false).size() == 1);
    }

    @Test
    public void testGetQueryMappingsWithEscapedDot() {
        String question = "In which country does the Nile start?";
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "SELECT DISTINCT ?uri WHERE { res:Nile dbo:city ?uri . }";
        Map<String, String> posSequence = new HashMap<>();
        posSequence.put("start", "VB0");
        posSequence.put("city", "NN1");
        posSequence.put("Nile", "NNP2");
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, posSequence, query, nodes);

        QueryTemplateMapping template1 = new QueryTemplateMapping(0, 0);
        template1.addSelectTemplate("SELECT DISTINCT ?uri WHERE { <^http://foo.bar.some.thing^> <^NN3^> ?x. <http://some.url> <^NN3^> ?x. <http://foo.bar.com> <^NN3^> ?x } ", "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "SELECT DISTINCT ?uri WHERE { res:Nile dbo:city ?uri . }");

        Map<String, QueryTemplateMapping> mappings = new HashMap<>();
        mappings.put("", template1);

        List<String> actual = queryMappingFactory.generateQueries(mappings, false);
        assertTrue(actual.size() == 1);
        assertTrue(actual.get(0).contains("<http://some.url>"));
        assertTrue(actual.get(0).contains("<http://foo.bar.com>"));
    }

    @Test
    public void testCreateQueryPatternEccentricMethod() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "SELECT DISTINCT ?uri WHERE { res:Nile dbo:city ?uri . }";
        String question = "In which country does the Nile start?";
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        List<String> properties = SPARQLUtilities.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        assertEquals("SELECT DISTINCT ?uri WHERE { <^VAR_0^> <^VAR_1^> ?uri . }", queryMappingFactory.getQueryPattern());
    }

    @Test
    public void testCreateQueryPatternEccentricMethodTwoTriples() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "SELECT DISTINCT ?uri WHERE { res:Nile dbo:city ?uri . res:Nile dbo:city ?uri . }";
        String question = "In which country does the Nile start?";
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        List<String> properties = SPARQLUtilities.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        assertEquals("SELECT DISTINCT ?uri WHERE { <^VAR_0^> <^VAR_1^> ?uri . <^VAR_2^> <^VAR_3^> ?uri . }", queryMappingFactory.getQueryPattern());
    }

    @Test
    @Ignore
    //TODO Implement the recognition of multi-word entities like "http://dbpedia.org/resource/Game_of_Thrones
    public void testExtractResources() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "Does Breaking Bad have more episodes than Game of Thrones?";
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        List<String> properties = SPARQLUtilities.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractResources(question, false);
        assertTrue(actual.contains("http://dbpedia.org/resource/Breaking_Bad"));
        assertTrue(actual.contains("http://dbpedia.org/resource/Game_of_Thrones"));
    }

    @Test
    @Ignore
    //TODO Implement the recognition of multi-word entities like "http://dbpedia.org/resource/Game_of_Thrones
    public void testExtractResourcesIgnoresCase() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "In which city was the president of Montenegro born?";
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        List<String> properties = SPARQLUtilities.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractResources(question, false);
        assertTrue(actual.contains("http://dbpedia.org/resource/President_of_Montenegro"));
    }

    @Test
    public void testExtractResourcesDetectsOntologiesWithPlural() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "Give me all source countries.";
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        List<String> properties = SPARQLUtilities.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractResources(question, false);
        assertTrue(actual.contains("http://dbpedia.org/ontology/sourceCountry"));
    }

    @Test
    public void testExtractResourcesDetectsOntologies() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "Give me a source country.";
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        List<String> properties = SPARQLUtilities.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractResources(question, false);
        assertTrue(actual.contains("http://dbpedia.org/ontology/sourceCountry"));
    }

    @Test
    public void testExtractResourcesDetectsOntologiesFirstLetterCapitalized() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "Which television shows were created by Walt Disney?";
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        List<String> properties = SPARQLUtilities.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractResources(question, false);
        assertTrue(actual.contains("http://dbpedia.org/ontology/TelevisionShow"));
    }

    @Test
    public void testExtractResourcesDetectsOntologies2() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "Is Christian Bale starring in Velvet Goldmine?";
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        List<String> properties = SPARQLUtilities.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractResources(question, false);
        //TODO should starring be detected? the lemma 'star' is only detected at the moment.
        assertTrue(actual.contains("http://dbpedia.org/ontology/starring"));
    }

    @Test
    public void testExtractResourcesDetectsOntologiesDetectsOriginalForm() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "What company is founded by John Smith?";
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        List<String> properties = SPARQLUtilities.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractResources(question, false);
        assertTrue(actual.contains("http://dbpedia.org/ontology/foundedBy"));
    }

    @Test
    public void testExtractResourcesDetectsOntologiesDetectsOriginalForm2() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "Is Christian Bale starring in Batman Begins?";
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        List<String> properties = SPARQLUtilities.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractResources(question, false);
        assertTrue(actual.contains("http://dbpedia.org/ontology/starring"));
    }

    @Test
    public void testExtractResourcesDetectsResourcesWithMultipleWords() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "Who developed the video game World of Warcraft?";
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        List<String> properties = SPARQLUtilities.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractResources(question, false);
        assertTrue(actual.contains("http://dbpedia.org/resource/World_of_Warcraft"));
    }

    @Test
    public void testFindResourceInFullTextDetectsResourcesWithMultipleWords() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "Who developed the video game World of Warcraft?";
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        List<String> properties = SPARQLUtilities.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.findResourcesInFullText("World of Warcraft");
        //assertTrue(actual.size() == 1);
        assertTrue(actual.contains("http://dbpedia.org/resource/World_of_Warcraft"));
    }

    @Test
    public void testFindResourceInFullTextDetectsResourcesWithMultipleWords2() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "Who developed the video game World of Warcraft?";
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        List<String> properties = SPARQLUtilities.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.findResourcesInFullText("Game of Thrones");
        assertTrue(actual.contains("http://dbpedia.org/resource/Game_of_Thrones"));
    }

    @Test
    public void testExtractResourcesOnlyUsesBiggestMatchBetweenWordgroupAndOntology() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX res: <http://dbpedia.org/resource/> SELECT DISTINCT ?uri WHERE {          res:Suriname dbo:officialLanguage ?uri . }";
        String question = "What is the official language of Suriname?";
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        List<String> properties = SPARQLUtilities.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractResources(question, false);
        assertTrue(actual.contains("http://dbpedia.org/ontology/officialLanguage"));
        assertTrue(actual.contains("http://dbpedia.org/ontology/otherLanguage"));
        assertTrue(!actual.contains("http://dbpedia.org/ontology/ProgrammingLanguage"));
        assertTrue(!actual.contains("http://dbpedia.org/ontology/languageCode"));
        assertTrue(!actual.contains("http://dbpedia.org/ontology/namedByLanguage"));
        assertTrue(!actual.contains("http://dbpedia.org/ontology/titleLanguage"));
        assertTrue(!actual.contains("http://dbpedia.org/ontology/languageFamily"));
        assertTrue(!actual.contains("http://dbpedia.org/ontology/deFactoLanguage"));
        assertTrue(!actual.contains("http://dbpedia.org/ontology/regionalLanguage"));
        assertTrue(!actual.contains("http://dbpedia.org/ontology/programmingLanguage"));
    }

    @Test
    public void testExtractResourcesWithoutSynonyms() {
        String query = "PREFIX res: <http://dbpedia.org/resource/> PREFIX dbo: <http://dbpedia.org/ontology/> SELECT DISTINCT ?uri WHERE {res:Douglas_Hofstadter dbo:award ?uri .}";
        String question = "Which awards did Douglas Hofstadter win?";
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        List<String> properties = SPARQLUtilities.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractResources(question, false);
        assertTrue(!actual.contains("http://dbpedia.org/ontology/birthPlace"));
        assertTrue(!actual.contains("http://dbpedia.org/ontology/deathPlace"));
    }

    @Test
    public void testExtractResourcesWontUseBe() {
        String query = "PREFIX res: <http://dbpedia.org/resource/> PREFIX dbo: <http://dbpedia.org/ontology/> SELECT DISTINCT ?uri WHERE {res:Douglas_Hofstadter dbo:award ?uri .}";
        String question = "What is Batman's real name?";
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        List<String> properties = SPARQLUtilities.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractResources(question, false);
        assertTrue(!actual.contains("http://dbpedia.org/property/be"));
    }

    @Test
    public void testExtractResourcesWontUseBeWithSynonyms() {
        String query = "PREFIX res: <http://dbpedia.org/resource/> PREFIX dbo: <http://dbpedia.org/ontology/> SELECT DISTINCT ?uri WHERE {res:Douglas_Hofstadter dbo:award ?uri .}";
        String question = "What is Batman's real name?";
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        List<String> properties = SPARQLUtilities.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractResources(question, true);
        assertTrue(!actual.contains("http://dbpedia.org/property/be"));
    }

    @Test
    public void testExtractResourcesWontUseCost() {
        String query = "PREFIX res: <http://dbpedia.org/resource/> PREFIX dbo: <http://dbpedia.org/ontology/> SELECT DISTINCT ?uri WHERE {res:Douglas_Hofstadter dbo:award ?uri .}";
        String question = "Who was Vincent van Gogh inspired by?";
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        List<String> properties = SPARQLUtilities.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractResources(question, false);
        assertTrue(!actual.contains("http://dbpedia.org/ontology/cost"));
    }

    @Test
    public void testExtractResourcesWontUseCostWithSynonyms() {
        String query = "PREFIX res: <http://dbpedia.org/resource/> PREFIX dbo: <http://dbpedia.org/ontology/> SELECT DISTINCT ?uri WHERE {res:Douglas_Hofstadter dbo:award ?uri .}";
        String question = "Who was Vincent van Gogh inspired by?";
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        List<String> properties = SPARQLUtilities.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractResources(question, true);
        assertTrue(!actual.contains("http://dbpedia.org/ontology/cost"));
    }

    @Test
    public void testExtractResourcesWontUseMapWithSynonyms() {
        String query = "PREFIX res: <http://dbpedia.org/resource/> PREFIX dbo: <http://dbpedia.org/ontology/> SELECT DISTINCT ?uri WHERE {res:Douglas_Hofstadter dbo:award ?uri .}";
        String question = "Where was Bach born?";
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        List<String> properties = SPARQLUtilities.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractResources(question, true);
        assertTrue(!actual.contains("http://dbpedia.org/ontology/map"));
    }

    @Test
    public void testExtractResourcesWontUseQuestionWords() {
        String query = "PREFIX res: <http://dbpedia.org/resource/> PREFIX dbo: <http://dbpedia.org/ontology/> SELECT DISTINCT ?uri WHERE {res:Douglas_Hofstadter dbo:award ?uri .}";
        String question = "Where was Bach born?";
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        List<String> properties = SPARQLUtilities.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractResources(question, true);
        assertTrue(!actual.contains("http://dbpedia.org/resource/Where"));
    }

    @Test
    public void testExtractResourcesWontUseQuestionWordsForSynonyms() {
        String query = "PREFIX res: <http://dbpedia.org/resource/> PREFIX dbo: <http://dbpedia.org/ontology/> SELECT DISTINCT ?uri WHERE {res:Douglas_Hofstadter dbo:award ?uri .}";
        String question = "Who was the pope that founded the Vatican Television?";
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        List<String> properties = SPARQLUtilities.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractResources(question, true);
        assertTrue(!actual.contains("http://dbpedia.org/ontology/deathPlace"));
    }

    @Test
    @Ignore
    public void testExtractResourcesUsesHypernyms() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX res: <http://dbpedia.org/resource/> SELECT DISTINCT ?uri WHERE {res:Abraham_Lincoln dbo:spouse ?uri.}";
        String question = "Who was the wife of U.S. president Lincoln?";
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        List<String> properties = SPARQLUtilities.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractResources(question, false);
        assertTrue(actual.contains("http://dbpedia.org/ontology/spouse"));
    }

    @Test
    public void testGenerateQueries() {
        String graph = " {\"1\" @\"p\" \"2\"}";
        String query = "SELECT DISTINCT ?uri WHERE {  <http://dbpedia.org/resource/San_Pedro_de_Atacama> <http://dbpedia.org/ontology/timeZone> ?uri . }";
        String question = "What is the timezone in San Pedro de Atacama?";
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        List<String> properties = SPARQLUtilities.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        SemanticAnalysisHelper semanticAnalysisHelper = new SemanticAnalysisHelper();
        List<CustomQuestion> customQuestions = new ArrayList<>();
        customQuestions.add(new CustomQuestion("SELECT DISTINCT ?num WHERE {  <http://dbpedia.org/resource/Colombo_Lighthouse> <http://dbpedia.org/ontology/height> ?num . }",
                "", null, graph, new HashMap<>()));
        List<String> dBpediaProperties = SPARQLUtilities.getDBpediaProperties();
        Map<String, QueryTemplateMapping> mappings = semanticAnalysisHelper.extractTemplates(customQuestions, newArrayList(nodes), dBpediaProperties);

        List<String> actualQueries = queryMappingFactory.generateQueries(mappings, graph, new ArrayList<>(), false);
        SPARQLResultSet sparqlResultSet = SPARQLUtilities.executeSPARQLQuery(actualQueries.get(0));

        assertTrue(actualQueries.size() == 1);
        assertTrue(sparqlResultSet.getResultSet().size() == 1);
        assertTrue(sparqlResultSet.getResultSet().get(0).equals("http://dbpedia.org/resource/Time_in_Chile"));
    }

    @Test
    public void testGenerateQueriesWithMultipleTriples() {
        String graph = " {\"1\" @\"p\" \"2\"}";
        String query = "SELECT DISTINCT ?uri WHERE {  \n" +
                "    ?uri <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Film> .  " +
                "    ?uri <http://dbpedia.org/ontology/starring> <http://dbpedia.org/resource/Julia_Roberts> .  " +
                "    ?uri <http://dbpedia.org/ontology/starring> <http://dbpedia.org/resource/Richard_Gere> . " +
                "}";
        String question = "In which films did Julia Roberts as well as Richard Gere play?";
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        List<String> properties = SPARQLUtilities.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        SemanticAnalysisHelper semanticAnalysisHelper = new SemanticAnalysisHelper();
        List<CustomQuestion> customQuestions = new ArrayList<>();
        customQuestions.add(new CustomQuestion("SELECT DISTINCT ?uri WHERE {  \n" +
                "    ?uri <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Film> .  " +
                "    ?uri <http://dbpedia.org/ontology/starring> <http://dbpedia.org/resource/Julia_Roberts> .  " +
                "    ?uri <http://dbpedia.org/ontology/starring> <http://dbpedia.org/resource/Richard_Gere> . " +
                "}",
                "", null, graph, new HashMap<>()));
        List<String> dBpediaProperties = SPARQLUtilities.getDBpediaProperties();
        Map<String, QueryTemplateMapping> mappings = semanticAnalysisHelper.extractTemplates(customQuestions, newArrayList(nodes), dBpediaProperties);

        List<String> actualQueries = queryMappingFactory.generateQueries(mappings, graph, new ArrayList<>(), false);

        assertTrue(actualQueries.size() == 1);
        assertTrue(actualQueries.get(0).contains("?uri ?property_0 ?class_0 . ?uri ?property_1 ?class_1 . ?uri ?property_2 ?class_2 ."));
    }

    @Test
    public void testGenerateQueriesWithFilterInQueryTemplate() {
        String graph = " {\"1\" @\"p\" \"2\"}";
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX dbp: <http://dbpedia.org/property/> PREFIX res: <http://dbpedia.org/resource/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?uri  WHERE {  ?uri rdf:type dbo:City .         ?uri dbo:isPartOf res:New_Jersey .         ?uri dbp:populationTotal ?inhabitants .         FILTER (?inhabitants > 100000) . }";
        String question = "Give me all cities in New Jersey with more than 100000 inhabitants.";
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        List<String> properties = SPARQLUtilities.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        SemanticAnalysisHelper semanticAnalysisHelper = new SemanticAnalysisHelper();
        List<CustomQuestion> customQuestions = new ArrayList<>();
        customQuestions.add(new CustomQuestion("PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX dbp: <http://dbpedia.org/property/> PREFIX res: <http://dbpedia.org/resource/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?uri  WHERE {  ?uri rdf:type dbo:City .         ?uri dbo:isPartOf res:New_Jersey .         ?uri dbp:populationTotal ?inhabitants . }",
                "", null, graph, new HashMap<>()));
        List<String> dBpediaProperties = SPARQLUtilities.getDBpediaProperties();
        Map<String, QueryTemplateMapping> mappings = semanticAnalysisHelper.extractTemplates(customQuestions, newArrayList(nodes), dBpediaProperties);

        List<String> expectedQueries = new ArrayList<>();
        expectedQueries.add("SELECT DISTINCT ?uri WHERE { ?uri ?property_0 ?class_0 . ?uri ?property_1 ?class_1 . ?uri ?property_2 ?inhabitants .  VALUES (?class_0) {(<http://dbpedia.org/ontology/City>) (<http://dbpedia.org/ontology/Street>) (<http://dbpedia.org/resource/New_Jersey>)} VALUES (?class_1) {(<http://dbpedia.org/ontology/City>) (<http://dbpedia.org/ontology/Street>) (<http://dbpedia.org/resource/New_Jersey>)} VALUES (?class_2) {(<http://dbpedia.org/ontology/City>) (<http://dbpedia.org/ontology/Street>) (<http://dbpedia.org/resource/New_Jersey>)} VALUES (?property_0) {(<http://dbpedia.org/ontology/city>) (<http://dbpedia.org/property/new>) (<http://dbpedia.org/property/city>) (<http://dbpedia.org/property/jersey>) (<http://dbpedia.org/ontology/ruling>)} VALUES (?property_1) {(<http://dbpedia.org/ontology/city>) (<http://dbpedia.org/property/new>) (<http://dbpedia.org/property/city>) (<http://dbpedia.org/property/jersey>) (<http://dbpedia.org/ontology/ruling>)} VALUES (?property_2) {(<http://dbpedia.org/ontology/city>) (<http://dbpedia.org/property/new>) (<http://dbpedia.org/property/city>) (<http://dbpedia.org/property/jersey>) (<http://dbpedia.org/ontology/ruling>)} FILTER (CONCAT( ?uri, ?property_0, ?class_0 ) != CONCAT( ?uri, ?property_1, ?class_1 ))  FILTER (CONCAT( ?uri, ?property_0, ?class_0 ) != CONCAT( ?uri, ?property_2, ?inhabitants ))  FILTER (CONCAT( ?uri, ?property_1, ?class_1 ) != CONCAT( ?uri, ?property_0, ?class_0 ))  FILTER (CONCAT( ?uri, ?property_1, ?class_1 ) != CONCAT( ?uri, ?property_2, ?inhabitants ))  FILTER (CONCAT( ?uri, ?property_2, ?inhabitants ) != CONCAT( ?uri, ?property_0, ?class_0 ))  FILTER (CONCAT( ?uri, ?property_2, ?inhabitants ) != CONCAT( ?uri, ?property_1, ?class_1 )) }");

        List<String> actualQueries = queryMappingFactory.generateQueries(mappings, graph, new ArrayList<>(), false);

        assertTrue(actualQueries.size() == 1);
        assertTrue(actualQueries.get(0).startsWith("SELECT DISTINCT ?uri WHERE { ?uri ?property_0 ?class_0 . ?uri ?property_1 ?class_1 . ?uri ?property_2 ?inhabitants ."));
    }

    @Test
    public void testGenerateQueriesStringLiteralInQuery() {
        String graph = " {\"1\" @\"p\" \"2\"}";
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?uri WHERE {          ?uri dbo:office 'President of the United States' .          ?uri dbo:orderInOffice '16th' . }";
        String question = "Who was the 16th president of the United States?";
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        List<String> properties = SPARQLUtilities.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        SemanticAnalysisHelper semanticAnalysisHelper = new SemanticAnalysisHelper();
        List<CustomQuestion> customQuestions = new ArrayList<>();
        customQuestions.add(new CustomQuestion("PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?uri WHERE {          ?uri dbo:office 'President of the United States' .          ?uri dbo:orderInOffice '16th' . }",
                "", null, graph, new HashMap<>()));
        List<String> dBpediaProperties = SPARQLUtilities.getDBpediaProperties();
        Map<String, QueryTemplateMapping> mappings = semanticAnalysisHelper.extractTemplates(customQuestions, newArrayList(nodes), dBpediaProperties);

        List<String> actualQueries = queryMappingFactory.generateQueries(mappings, graph, new ArrayList<>(), false);

        assertTrue(actualQueries.size() == 1);
        assertTrue(actualQueries.get(0).startsWith("SELECT DISTINCT ?uri WHERE { ?uri ?property_0 'President of the United States' . ?uri ?property_1 '16th' .  "));
    }

    @Test
    public void testGenerateQueriesStringLiteralInQuery2() {
        String graph = " {\"1\" @\"p\" \"2\"}";
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> ASK  WHERE {  ?uri rdf:type dbo:VideoGame .         ?uri rdfs:label 'Battle Chess'@en . }";
        String question = "Is there a video game called Battle Chess?";
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        List<String> properties = SPARQLUtilities.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        SemanticAnalysisHelper semanticAnalysisHelper = new SemanticAnalysisHelper();
        List<CustomQuestion> customQuestions = new ArrayList<>();
        customQuestions.add(new CustomQuestion("PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> ASK  WHERE {  ?uri rdf:type dbo:VideoGame .         ?uri rdfs:label 'Battle Chess'@en . }",
                "", null, graph, new HashMap<>()));
        List<String> dBpediaProperties = SPARQLUtilities.getDBpediaProperties();
        Map<String, QueryTemplateMapping> mappings = semanticAnalysisHelper.extractTemplates(customQuestions, newArrayList(nodes), dBpediaProperties);

        List<String> actualQueries = queryMappingFactory.generateQueries(mappings, graph, new ArrayList<>(), false);

        assertTrue(actualQueries.size() == 1);
        assertTrue(actualQueries.get(0).startsWith("ASK WHERE { ?uri ?property_0 ?class_0 . ?uri ?property_1 'Battle Chess'@en .  VALUES "));
        assertTrue(actualQueries.get(0).contains("CONCAT( ?uri, ?property_1, 'Battle Chess'@en )"));
    }

    @Test
    public void testGenerateQueriesForSuperlativeAscQuestion() {
        String graph = " {\"1\" @\"p\" \"2\"}";
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX res: <http://dbpedia.org/resource/> SELECT DISTINCT ?uri WHERE { res:Meryl_Streep dbo:child ?uri . ?uri dbo:birthDate ?d . } ORDER BY ASC(?d) OFFSET 0 LIMIT 1";
        String question = "Who is the oldest child of Meryl Streep?";
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        List<String> properties = SPARQLUtilities.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        SemanticAnalysisHelper semanticAnalysisHelper = new SemanticAnalysisHelper();
        List<CustomQuestion> customQuestions = new ArrayList<>();
        customQuestions.add(new CustomQuestion("PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX res: <http://dbpedia.org/resource/> SELECT DISTINCT ?uri WHERE { res:Meryl_Streep dbo:child ?uri . ?uri dbo:birthDate ?d . } ORDER BY ASC(?d) OFFSET 0 LIMIT 1",
                "", null, graph, new HashMap<>()));
        List<String> dBpediaProperties = SPARQLUtilities.getDBpediaProperties();
        Map<String, QueryTemplateMapping> mappings = semanticAnalysisHelper.extractTemplates(customQuestions, newArrayList(nodes), dBpediaProperties);

        List<String> actualQueries = queryMappingFactory.generateQueries(mappings, graph, new ArrayList<>(), false);

        assertTrue(actualQueries.size() == 1);
        assertTrue(actualQueries.get(0).startsWith("SELECT DISTINCT ?uri WHERE { ?class_0 ?property_0 ?uri . ?uri ?property_1 ?d . "));
        assertTrue(actualQueries.get(0).endsWith("ORDER BY ASC(?d) OFFSET 0 LIMIT 1"));
    }

    @Test
    public void testGenerateQueriesForSuperlativeDescQuestion() {
        String graph = " {\"1\" @\"p\" \"2\"}";
        String query = "SELECT DISTINCT ?uri WHERE { ?uri a <http://dbpedia.org/ontology/Company> . ?uri <http://dbpedia.org/ontology/location> <http://dbpedia.org/resource/India> . ?uri <http://dbpedia.org/ontology/numberOfEmployees> ?n . } ORDER BY DESC(?n) OFFSET 0 LIMIT 1";
        String question = "Which Indian company has the most employees?";
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        List<String> properties = SPARQLUtilities.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        SemanticAnalysisHelper semanticAnalysisHelper = new SemanticAnalysisHelper();
        List<CustomQuestion> customQuestions = new ArrayList<>();
        customQuestions.add(new CustomQuestion("SELECT DISTINCT ?uri WHERE { ?uri a <http://dbpedia.org/ontology/Company> . ?uri <http://dbpedia.org/ontology/location> <http://dbpedia.org/resource/India> . ?uri <http://dbpedia.org/ontology/numberOfEmployees> ?n . } ORDER BY DESC(?n) OFFSET 0 LIMIT 1",
                "", null, graph, new HashMap<>()));
        List<String> dBpediaProperties = SPARQLUtilities.getDBpediaProperties();
        Map<String, QueryTemplateMapping> mappings = semanticAnalysisHelper.extractTemplates(customQuestions, newArrayList(nodes), dBpediaProperties);

        List<String> actualQueries = queryMappingFactory.generateQueries(mappings, graph, new ArrayList<>(), false);

        assertTrue(actualQueries.size() == 1);
        assertTrue(actualQueries.get(0).startsWith("SELECT DISTINCT ?uri WHERE { ?uri a ?class_0 . ?uri ?property_0 ?class_1 . ?uri ?property_1 ?n . "));
        assertTrue(actualQueries.get(0).endsWith(" ORDER BY DESC(?n) OFFSET 0 LIMIT 1"));
    }
}

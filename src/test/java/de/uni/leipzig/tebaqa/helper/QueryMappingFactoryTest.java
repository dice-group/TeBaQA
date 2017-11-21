package de.uni.leipzig.tebaqa.helper;

import com.hp.hpl.jena.rdf.model.RDFNode;
import de.uni.leipzig.tebaqa.controller.SemanticAnalysisHelper;
import de.uni.leipzig.tebaqa.model.CustomQuestion;
import de.uni.leipzig.tebaqa.model.QueryTemplateMapping;
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
    public void setUp() throws Exception {
        StanfordPipelineProvider.getSingletonPipelineInstance();
    }

    @Test
    public void testCreateQueryPattern() throws Exception {
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
    public void testCreateQueryPatternUriMismatch() throws Exception {
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
    public void testCreateQueryPatternTestsForCompleteResourceString() throws Exception {
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
    public void testCreateQueryPatternWithEntityFromSpotlight() throws Exception {
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
    public void testCreateQueryPatternWithUnknownSpotlightEntity() throws Exception {
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
    public void testCreateQueryPatternWithMultiWordEntity() throws Exception {
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

        assertEquals("SELECT DISTINCT ?uri WHERE { <^NNP0_NNP2_FW_NNP3^> <^NN1^> ?uri . }",
                queryMappingFactory.getQueryPattern());
    }

    @Test
    public void testQueryWithNamespaces() throws Exception {
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
    public void testQuery2() throws Exception {
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
    public void testGetQueryMappingsWithMatchingArguments() throws Exception {
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
        template1.addSelectTemplate("SELECT DISTINCT ?uri WHERE { <^NNP4^> <^NN3^> ?x . <^NN2^> <^NN1^> ?uri }");

        QueryTemplateMapping template2 = new QueryTemplateMapping(2, 2);
        template2.addSelectTemplate("SELECT DISTINCT ?uri WHERE { <^NNP4^> <^NN3^> ?x . <^NNP4^> <^NN3^> ?x }");

        Map<String, QueryTemplateMapping> mappings = new HashMap<>();
        mappings.put("1", template1);
        mappings.put("2", template2);

        List<String> expected = new ArrayList<>();
        expected.add("SELECT DISTINCT ?uri WHERE { ?class_ ?property_ ?x . ?class_ ?property_ ?uri . VALUES (?class_) {(<http://dbpedia.org/resource/Nile>) (<http://dbpedia.org/ontology/Country>)} VALUES (?property_) {(<http://dbpedia.org/ontology/country>) (<http://dbpedia.org/ontology/start>)}}");
        expected.add("SELECT DISTINCT ?uri WHERE { ?class_ ?property_ ?x . ?class_ ?property_ ?x . VALUES (?class_) {(<http://dbpedia.org/resource/Nile>) (<http://dbpedia.org/ontology/Country>)} VALUES (?property_) {(<http://dbpedia.org/ontology/country>) (<http://dbpedia.org/ontology/start>)}}");

        assertEquals(expected, queryMappingFactory.generateQueries(mappings));
    }


    @Test
    public void testGetQueryMappingsWithTooFewArguments() throws Exception {
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
        template1.addSelectTemplate("SELECT DISTINCT ?uri WHERE { <^NNP4^> <^NN3^> ?x }");

        QueryTemplateMapping template2 = new QueryTemplateMapping(1, 1);
        template2.addSelectTemplate("SELECT DISTINCT ?uri WHERE { <^NNP4^> <^NN3^> ?x }");

        List<String> expected = new ArrayList<>();
        expected.add("SELECT DISTINCT ?uri WHERE { ?class_ ?property_ ?x . VALUES (?class_) {(<http://dbpedia.org/resource/Nile>) (<http://dbpedia.org/ontology/Country>)} VALUES (?property_) {(<http://dbpedia.org/ontology/country>) (<http://dbpedia.org/ontology/start>)}}");

        Map<String, QueryTemplateMapping> mappings = new HashMap<>();
        mappings.put("1", template1);
        mappings.put("2", template2);

        assertEquals(expected, queryMappingFactory.generateQueries(mappings));
    }

    @Test
    public void testGetQueryMappingsWithTooMuchProperties() throws Exception {
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
        template1.addSelectTemplate("SELECT DISTINCT ?uri WHERE { <^NNP4^> a ?x }");

        List<String> expected = new ArrayList<>();
        expected.add("SELECT DISTINCT ?uri WHERE { ?class_ a ?x . VALUES (?class_) {(<http://dbpedia.org/resource/Nile>) (<http://dbpedia.org/ontology/Country>)} VALUES (?property_) {(<http://dbpedia.org/ontology/country>) (<http://dbpedia.org/ontology/start>)}}");

        Map<String, QueryTemplateMapping> mappings = new HashMap<>();
        mappings.put("", template1);

        assertEquals(expected, queryMappingFactory.generateQueries(mappings));
    }

    @Test
    public void testGetQueryMappingsWithTooMuchClasses() throws Exception {
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
        template1.addSelectTemplate("SELECT DISTINCT ?uri WHERE { ?x <^NN3^> ?x }");

        List<String> expected = new ArrayList<>();
        expected.add("SELECT DISTINCT ?uri WHERE { ?x ?property_ ?x . VALUES (?class_) {(<http://dbpedia.org/resource/Nile>) (<http://dbpedia.org/ontology/Country>)} VALUES (?property_) {(<http://dbpedia.org/ontology/country>) (<http://dbpedia.org/ontology/start>)}}");

        Map<String, QueryTemplateMapping> mappings = new HashMap<>();
        mappings.put("1", template1);

        assertEquals(expected, queryMappingFactory.generateQueries(mappings));
    }

    @Test
    public void testGetQueryMappingsWithTooMuchArguments() throws Exception {
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
        template1.addSelectTemplate("SELECT DISTINCT ?uri WHERE { <^NNP4^> <^NN3^> ?x. <^NNP4^> <^NN3^> ?x. <^NNP4^> <^NN3^> ?x }");

        QueryTemplateMapping template2 = new QueryTemplateMapping(3, 3);
        template2.addSelectTemplate("SELECT DISTINCT ?uri WHERE { <^NNP4^> <^NN3^> ?x . <^NNP4^> <^NN3^> ?x . <^NNP4^> <^NN3^> ?x }");

        Map<String, QueryTemplateMapping> mappings = new HashMap<>();
        mappings.put("1", template1);
        mappings.put("1", template2);

        List<String> expected = new ArrayList<>();
        expected.add("SELECT DISTINCT ?uri WHERE { ?class_ ?property_ ?x . ?class_ ?property_ ?x . ?class_ ?property_ ?x . VALUES (?class_) {(<http://dbpedia.org/resource/Nile>) (<http://dbpedia.org/ontology/Country>)} VALUES (?property_) {(<http://dbpedia.org/ontology/country>) (<http://dbpedia.org/ontology/start>)}}");

        assertEquals(expected, queryMappingFactory.generateQueries(mappings));
    }

    @Test
    public void testGetQueryMappingsWithEscapedDot() throws Exception {
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

        List<String> expected = new ArrayList<>();
        expected.add("SELECT DISTINCT ?uri WHERE { ?class_ ?property_ ?x. <http://some.url> ?property_ ?x. <http://foo.bar.com> ?property_ ?x . VALUES (?class_) {(<http://dbpedia.org/resource/Nile>) (<http://dbpedia.org/ontology/Country>)} VALUES (?property_) {(<http://dbpedia.org/ontology/country>) (<http://dbpedia.org/ontology/start>)}}");


        QueryTemplateMapping template1 = new QueryTemplateMapping(0, 0);
        template1.addSelectTemplate("SELECT DISTINCT ?uri WHERE { <^http://foo.bar.some.thing^> <^NN3^> ?x. <http://some.url> <^NN3^> ?x. <http://foo.bar.com> <^NN3^> ?x }");

        Map<String, QueryTemplateMapping> mappings = new HashMap<>();
        mappings.put("", template1);

        assertEquals(expected, queryMappingFactory.generateQueries(mappings));
    }

    @Test
    public void testCreateQueryPatternEccentricMethod() throws Exception {
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
    public void testCreateQueryPatternEccentricMethodTwoTriples() throws Exception {
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
    //TODO
    public void testExtractResources() throws Exception {
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

        Set<String> actual = queryMappingFactory.extractResources(question);
        assertTrue(actual.contains("http://dbpedia.org/resource/Breaking_Bad"));
        assertTrue(actual.contains("http://dbpedia.org/resource/Game_of_Thrones"));
    }

    @Test
    public void testExtractResourcesDetectsOntologiesWithPlural() throws Exception {
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

        Set<String> actual = queryMappingFactory.extractResources(question);
        assertTrue(actual.contains("http://dbpedia.org/ontology/sourceCountry"));
    }

    @Test
    public void testExtractResourcesDetectsOntologies() throws Exception {
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

        Set<String> actual = queryMappingFactory.extractResources(question);
        assertTrue(actual.contains("http://dbpedia.org/ontology/sourceCountry"));
    }

    @Test
    public void testExtractResourcesDetectsOntologiesFirstLetterCapitalized() throws Exception {
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

        Set<String> actual = queryMappingFactory.extractResources(question);
        assertTrue(actual.contains("http://dbpedia.org/ontology/TelevisionShow"));
    }

    @Test
    public void testExtractResourcesDetectsOntologies2() throws Exception {
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

        Set<String> actual = queryMappingFactory.extractResources(question);
        //TODO should starring be detected? the lemma 'star' is only detected at the moment.
        assertTrue(actual.contains("http://dbpedia.org/ontology/starring"));
    }

    @Test
    public void testExtractResourcesDetectsOntologiesDetectsOriginalForm() throws Exception {
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

        Set<String> actual = queryMappingFactory.extractResources(question);
        assertTrue(actual.contains("http://dbpedia.org/ontology/foundedBy"));
    }

    @Test
    public void testExtractResourcesDetectsOntologiesDetectsOriginalForm2() throws Exception {
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

        Set<String> actual = queryMappingFactory.extractResources(question);
        assertTrue(actual.contains("http://dbpedia.org/ontology/starring"));
    }

    @Test
    public void testExtractResourcesDetectsResourcesWithMultipleWords() throws Exception {
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

        Set<String> actual = queryMappingFactory.extractResources(question);
        assertTrue(actual.contains("http://dbpedia.org/resource/World_of_Warcraft"));
    }

    @Test
    @Ignore
    //TODO Ignoring the case isn't implemented yet.
    public void testExtractResourcesIgnoresCase() throws Exception {
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

        Set<String> actual = queryMappingFactory.extractResources(question);
        assertTrue(actual.contains("http://dbpedia.org/resource/President_of_Montenegro"));
    }

    @Test
    public void testGenerateQueries() throws Exception {
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

        List<String> expectedQueries = new ArrayList<>();
        expectedQueries.add("SELECT DISTINCT ?num WHERE { ?class_0 ?property_0 ?num .  VALUES (?class_0) {(<http://dbpedia.org/resource/San_Pedro_de_Atacama>)} VALUES (?property_0) {(<http://dbpedia.org/property/san>) (<http://dbpedia.org/property/be>) (<http://dbpedia.org/ontology/timeZone>) (<http://dbpedia.org/property/pedro>) (<http://dbpedia.org/property/timeZone>) (<http://dbpedia.org/property/timezone>)} VALUES (?property_1) {(<http://dbpedia.org/property/san>) (<http://dbpedia.org/property/be>) (<http://dbpedia.org/ontology/timeZone>) (<http://dbpedia.org/property/pedro>) (<http://dbpedia.org/property/timeZone>) (<http://dbpedia.org/property/timezone>)} VALUES (?property_2) {(<http://dbpedia.org/property/san>) (<http://dbpedia.org/property/be>) (<http://dbpedia.org/ontology/timeZone>) (<http://dbpedia.org/property/pedro>) (<http://dbpedia.org/property/timeZone>) (<http://dbpedia.org/property/timezone>)} VALUES (?property_3) {(<http://dbpedia.org/property/san>) (<http://dbpedia.org/property/be>) (<http://dbpedia.org/ontology/timeZone>) (<http://dbpedia.org/property/pedro>) (<http://dbpedia.org/property/timeZone>) (<http://dbpedia.org/property/timezone>)} VALUES (?property_4) {(<http://dbpedia.org/property/san>) (<http://dbpedia.org/property/be>) (<http://dbpedia.org/ontology/timeZone>) (<http://dbpedia.org/property/pedro>) (<http://dbpedia.org/property/timeZone>) (<http://dbpedia.org/property/timezone>)} VALUES (?property_5) {(<http://dbpedia.org/property/san>) (<http://dbpedia.org/property/be>) (<http://dbpedia.org/ontology/timeZone>) (<http://dbpedia.org/property/pedro>) (<http://dbpedia.org/property/timeZone>) (<http://dbpedia.org/property/timezone>)}}");

        List<String> actualQueries = queryMappingFactory.generateQueries(mappings, graph, new ArrayList<>());

        assertEquals(expectedQueries, actualQueries);
    }

    @Test
    public void testGenerateQueriesWithMultipleTriples() throws Exception {
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

        List<String> expectedQueries = new ArrayList<>();
        expectedQueries.add("SELECT DISTINCT ?uri WHERE { " +
                "    ?uri ?property_0 ?class_1 . " +
                "    ?uri ?property_1 ?class_2 . " +
                "    ?uri ?property_2 ?class_3 .  " +
                "    VALUES (?class_0) {(<http://dbpedia.org/resource/Julia_Roberts>) (<http://dbpedia.org/resource/Richard_Gere>) (<http://dbpedia.org/ontology/Film>)} " +
                "    VALUES (?class_1) {(<http://dbpedia.org/resource/Julia_Roberts>) (<http://dbpedia.org/resource/Richard_Gere>) (<http://dbpedia.org/ontology/Film>)} " +
                "    VALUES (?class_2) {(<http://dbpedia.org/resource/Julia_Roberts>) (<http://dbpedia.org/resource/Richard_Gere>) (<http://dbpedia.org/ontology/Film>)} " +
                "    VALUES (?property_0) {(<http://dbpedia.org/ontology/starring>) (<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>)}" +
                "    VALUES (?property_1) {(<http://dbpedia.org/ontology/starring>) (<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>)}" +
                "    VALUES (?property_2) {(<http://dbpedia.org/ontology/starring>) (<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>)}" +
                "    FILTER (CONCAT(?uri, ?property_0, ?class_0 ) != CONCAT(?uri, ?property_1, ?class_1 ))" +
                "    FILTER (CONCAT(?uri, ?property_1, ?class_1 ) != CONCAT(?uri, ?property_2, ?class_2 ))" +
                "}\n");

        List<String> actualQueries = queryMappingFactory.generateQueries(mappings, graph, new ArrayList<>());

        assertEquals(expectedQueries, actualQueries);
    }
}

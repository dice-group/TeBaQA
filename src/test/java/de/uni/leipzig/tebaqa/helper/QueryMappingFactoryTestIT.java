package de.uni.leipzig.tebaqa.helper;

import com.google.common.collect.Lists;
import com.hp.hpl.jena.rdf.model.RDFNode;
import de.uni.leipzig.tebaqa.controller.SemanticAnalysisHelper;
import de.uni.leipzig.tebaqa.model.CustomQuestion;
import de.uni.leipzig.tebaqa.model.QueryTemplateMapping;
import de.uni.leipzig.tebaqa.model.SPARQLResultSet;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.*;

public class QueryMappingFactoryTestIT {

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
        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
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
        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
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

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
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
        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
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
        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
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

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
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

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
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

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
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

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());

        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, posSequence, query, nodes);


        QueryTemplateMapping template1 = new QueryTemplateMapping(2, 2);
        template1.addSelectTemplate("SELECT DISTINCT ?uri WHERE { <^NNP4^> <^NN3^> ?x . <^NN2^> <^NN1^> ?uri }",
                "SELECT DISTINCT ?uri WHERE { ?class_0 ?property_0 ?x . ?class_1 ?property_1 ?uri .} ");

        Map<String, QueryTemplateMapping> mappings = new HashMap<>();
        mappings.put("1", template1);

        List<String> actual = queryMappingFactory.generateQueries(mappings, false);
        assertEquals(1, actual.size());
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

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, posSequence, query, nodes);

        QueryTemplateMapping template1 = new QueryTemplateMapping(1, 1);
        template1.addSelectTemplate("SELECT DISTINCT ?uri WHERE { <^NNP4^> <^NN3^> ?x }", "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "SELECT DISTINCT ?uri WHERE { res:Nile dbo:city ?uri . }");

        Map<String, QueryTemplateMapping> mappings = new HashMap<>();
        mappings.put("1", template1);

        List<String> actual = queryMappingFactory.generateQueries(mappings, false);
        assertEquals(1, actual.size());
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

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, posSequence, query, nodes);

        QueryTemplateMapping template1 = new QueryTemplateMapping(1, 0);
        template1.addSelectTemplate("SELECT DISTINCT ?uri WHERE { <^NNP4^> a ?x }", "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "SELECT DISTINCT ?uri WHERE { res:Nile dbo:city ?uri . }");

        Map<String, QueryTemplateMapping> mappings = new HashMap<>();
        mappings.put("", template1);

        List<String> actual = queryMappingFactory.generateQueries(mappings, false);
        assertEquals(1, actual.size());
        assertTrue(actual.get(0).startsWith("SELECT DISTINCT ?uri WHERE { ?class_0 a ?x . VALUES (?class_0) {(<http://dbpedia.org/resource/"));
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

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, posSequence, query, nodes);

        QueryTemplateMapping template1 = new QueryTemplateMapping(0, 1);
        template1.addSelectTemplate("SELECT DISTINCT ?uri WHERE { ?x <^NN3^> ?x }", "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "SELECT DISTINCT ?uri WHERE { res:Nile dbo:city ?uri . }");

        Map<String, QueryTemplateMapping> mappings = new HashMap<>();
        mappings.put("1", template1);

        assertEquals(1, queryMappingFactory.generateQueries(mappings, false).size());
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

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, posSequence, query, nodes);

        QueryTemplateMapping template1 = new QueryTemplateMapping(3, 3);
        template1.addSelectTemplate("SELECT DISTINCT ?uri WHERE { <^NNP4^> <^NN3^> ?x. <^NNP4^> <^NN3^> ?x. <^NNP4^> <^NN3^> ?x }", "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "SELECT DISTINCT ?uri WHERE { res:Nile dbo:city ?uri . }");

        Map<String, QueryTemplateMapping> mappings = new HashMap<>();
        mappings.put("1", template1);

        assertEquals(1, queryMappingFactory.generateQueries(mappings, false).size());
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

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
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

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        assertEquals("SELECT DISTINCT ?uri WHERE { <^VAR_0^> <^VAR_1^> ?uri . }", queryMappingFactory.getQueryPattern());
    }

    @Test
    public void testCreateQueryPatternEccentricMethodTwoTriples() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "SELECT DISTINCT ?uri WHERE { res:Nile dbo:city ?uri . res:Nile dbo:city ?uri . }";
        String question = "In which country does the Nile start?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        assertEquals("SELECT DISTINCT ?uri WHERE { <^VAR_0^> <^VAR_1^> ?uri . <^VAR_2^> <^VAR_3^> ?uri . }", queryMappingFactory.getQueryPattern());
    }

    @Test
    public void testExtractResources() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "Does Breaking Bad have more episodes than Game of Thrones?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
        assertTrue(actual.contains("http://dbpedia.org/resource/Breaking_Bad"));
        assertTrue(actual.contains("http://dbpedia.org/resource/Game_of_Thrones"));
    }

    @Test
    public void testExtractResourcesOnlyUsesSimiliarEntitiesToQuestionWords() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "Are there any castles in the United States?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
        assertFalse(actual.contains("http://dbpedia.org/resource/Are"));
        assertFalse(actual.contains("http://dbpedia.org/ontology/Earthquake"));
    }

    @Test
    public void testExtractResourcesDetectsOntology() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "What is the alma mater of the chancellor of Germany Angela Merkel?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
        assertTrue(actual.contains("http://dbpedia.org/ontology/almaMater"));
        assertTrue(actual.contains("http://dbpedia.org/resource/Angela_Merkel"));
    }

    @Test
    @Ignore
    public void testExtractResourcesDetectsOntologyFromMapping() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "How large is the area of UK?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
        assertTrue(actual.contains("http://dbpedia.org/ontology/areaTotal"));
        assertTrue(actual.contains("http://dbpedia.org/resource/United_Kingdom"));
    }

    @Test
    public void testExtractResourcesDetectsOntologyFromMapping2() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "Who is the author of the interpretation of dreams?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
        assertTrue(actual.contains("http://dbpedia.org/ontology/author"));
        assertTrue(actual.contains("http://dbpedia.org/resource/The_Interpretation_of_Dreams"));
    }

    @Test
    public void testExtractResourcesDetectsOntologyFromMapping3() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "What is the birth name of Adele?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
        assertTrue(actual.contains("http://dbpedia.org/ontology/birthName"));
        assertTrue(actual.contains("http://dbpedia.org/resource/Adele"));
    }

    @Test
    public void testExtractResourcesDetectsOntologyFromMapping4() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "How many awards has Bertrand Russell?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
        assertTrue(actual.contains("http://dbpedia.org/property/awards"));
        assertTrue(actual.contains("http://dbpedia.org/resource/Bertrand_Russell"));
    }

    @Test
    @Ignore
    public void testExtractResourcesDetectsOntologyFromMapping6() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "When will start the final match of the football world cup 2018?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
        assertTrue(actual.contains("http://dbpedia.org/property/date"));
    }

    @Test
    public void testExtractResourcesDetectsOntologyFromMapping7() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "how much is the elevation of Düsseldorf Airport ?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
        assertTrue(actual.contains("http://dbpedia.org/resource/Düsseldorf_Airport"));
        assertTrue(actual.contains("http://dbpedia.org/ontology/elevation"));
    }

    @Test
    @Ignore
    public void testExtractResourcesDetectsOntologyFromMapping8() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "how much is the total population of  european union?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
        assertTrue(actual.contains("http://dbpedia.org/resource/European_Union"));
        assertTrue(actual.contains("http://dbpedia.org/ontology/populationTotal"));
    }

    @Test
    public void testExtractResourcesDetectsOntologyFromMapping9() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "Who are the founders of  BlaBlaCar?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
        assertTrue(actual.contains("http://dbpedia.org/resource/BlaBlaCar"));
        assertTrue(actual.contains("http://dbpedia.org/property/founders"));
    }

    @Test
    @Ignore
    public void testExtractResourcesDetectsOntologyFromMapping10() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "how many foreigners speak German?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
        assertTrue(actual.contains("http://dbpedia.org/property/speakers"));
    }

    @Test
    public void testExtractResourcesDetectsOntologyFromMapping11() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "Where is the birthplace of Goethe?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
        assertTrue(actual.contains("http://dbpedia.org/resource/Johann_Wolfgang_von_Goethe"));
        assertTrue(actual.contains("http://dbpedia.org/ontology/birthPlace"));
    }

    @Test
    public void testExtractResourcesDetectsOntologyFromMapping12() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "Where is the origin of Carolina reaper?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
        assertTrue(actual.contains("http://dbpedia.org/resource/Carolina_Reaper"));
        assertTrue(actual.contains("http://dbpedia.org/ontology/origin"));
    }

    @Test
    @Ignore
    public void testExtractResourcesDetectsOntologyFromMapping13() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "How much is the population of Mexico City ?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
        assertTrue(actual.contains("http://dbpedia.org/resource/Mexico_City"));
        assertTrue(actual.contains("http://dbpedia.org/ontology/populationTotal"));
    }

    @Test
    @Ignore
    public void testExtractResourcesDetectsOntologyFromMapping14() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "What is the nick name of Baghdad?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
        assertTrue(actual.contains("http://xmlns.com/foaf/0.1/nick"));
        assertTrue(actual.contains("http://dbpedia.org/resource/Baghdad"));
    }

    @Test
    @Ignore
    public void testExtractResourcesDetectsOntologyFromMapping15() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "Who is the novelist of the work a song of ice and fire?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
        assertTrue(actual.contains("http://dbpedia.org/property/author"));
        assertTrue(actual.contains("http://dbpedia.org/resource/A_Song_of_Ice_and_Fire"));
    }

    @Test
    public void testExtractResourcesDetectsOntologyFromMapping16() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "What is the percentage of area water in Brazil?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
        assertTrue(actual.contains("http://dbpedia.org/ontology/percentageOfAreaWater"));
        assertTrue(actual.contains("http://dbpedia.org/resource/Brazil"));
    }

    @Test
    @Ignore
    public void testExtractResourcesDetectsOntologyFromMapping17() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "How much is the population of Iraq?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
        assertTrue(actual.contains("http://dbpedia.org/ontology/populationTotal"));
        assertTrue(actual.contains("http://dbpedia.org/resource/Iraq"));
    }

    @Test
    @Ignore
    public void testExtractResourcesDetectsOntologyFromMapping18() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "What is the population of Cairo?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
        assertTrue(actual.contains("http://dbpedia.org/ontology/populationTotal"));
        assertTrue(actual.contains("http://dbpedia.org/resource/Cairo"));
    }

    @Test
    @Ignore
    public void testExtractResourcesDetectsOntologyFromMapping20() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "How large is the total area of North Rhine-Westphalia";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
        assertTrue(actual.contains("http://dbpedia.org/resource/North_Rhine-Westphalia"));
        assertTrue(actual.contains("http://dbpedia.org/ontology/areaTotal"));
    }

    @Test
    @Ignore
    public void testExtractResourcesDetectsOntologyFromMapping21() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "What is the original title of the interpretation of dreams?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
        assertTrue(actual.contains("http://dbpedia.org/resource/The_Interpretation_of_Dreams"));
        assertTrue(actual.contains("http://xmlns.com/foaf/0.1/name"));
    }

    @Test
    public void testExtractResourcesDetectsOntologyFromMapping22() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "Who are the writers of the Wall album of Pink Floyd?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
        assertTrue(actual.contains("http://dbpedia.org/resource/The_Wall"));
    }

    @Test
    @Ignore
    //TODO Detect William_Shakespeare from Shakespeare with Apache OpenNLP or something else
    public void testExtractResourcesDetectsOntologyFromMapping23() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "When was the death  of  Shakespeare?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
        assertTrue(actual.contains("http://dbpedia.org/ontology/deathDate"));
        assertTrue(actual.contains("http://dbpedia.org/resource/William_Shakespeare"));
    }

    @Test
    public void testExtractResourcesDetectsOntologyFromMapping24() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "What is the largest city in america?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
        assertTrue(actual.contains("http://dbpedia.org/ontology/largestCity"));
    }

    @Test
    public void testExtractResourcesDetectsOntologyFromMapping25() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "What is the highest mountain in the Bavarian Alps?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
        assertTrue(actual.contains("http://dbpedia.org/property/highest"));
        assertTrue(actual.contains("http://dbpedia.org/resource/Bavarian_Alps"));
    }

    @Test
    public void testExtractResourcesIgnoresCase() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "In which city was the president of Montenegro born?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
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

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
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

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
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

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
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

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
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

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
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

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
        assertTrue(actual.contains("http://dbpedia.org/ontology/starring"));
    }

    @Test
    public void testExtractResourcesWontUseQuestionPhrase() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "Give me the birth place of Frank Sinatra.";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
        assertTrue(!actual.contains("http://dbpedia.org/resource/Give"));
        assertTrue(!actual.contains("http://dbpedia.org/resource/S/lay_w/Me"));
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

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
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

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
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

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.findResourcesInFullText("Game of Thrones");
        assertTrue(actual.contains("http://dbpedia.org/resource/Game_of_Thrones"));
    }

    @Test
    public void testFindResourceInFullTextDetectsResourcesWithMultipleWords3() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "ASK WHERE { " +
                "        res:Breaking_Bad dbo:numberOfEpisodes ?x . " +
                "        res:Game_of_Thrones dbo:numberOfEpisodes ?y . " +
                "        FILTER (?y > ?x) " +
                "}";
        String question = "Who is the novelist of the work a song of ice and fire?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.findResourcesInFullText("a song of ice and fire");
        assertTrue(actual.contains("http://dbpedia.org/resource/A_Song_of_Ice_and_Fire"));
    }

    @Test
    public void testExtractResourcesOnlyUsesBiggestMatchBetweenWordgroupAndOntology() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX res: <http://dbpedia.org/resource/> SELECT DISTINCT ?uri WHERE {          res:Suriname dbo:officialLanguage ?uri . }";
        String question = "What is the official language of Suriname?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
        assertTrue(actual.contains("http://dbpedia.org/ontology/officialLanguage"));
        assertFalse(actual.contains("http://dbpedia.org/ontology/ProgrammingLanguage"));
        assertFalse(actual.contains("http://dbpedia.org/ontology/languageCode"));
        assertFalse(actual.contains("http://dbpedia.org/ontology/namedByLanguage"));
        assertFalse(actual.contains("http://dbpedia.org/ontology/titleLanguage"));
        assertFalse(actual.contains("http://dbpedia.org/ontology/languageFamily"));
        assertFalse(actual.contains("http://dbpedia.org/ontology/deFactoLanguage"));
        assertFalse(actual.contains("http://dbpedia.org/ontology/regionalLanguage"));
        assertFalse(actual.contains("http://dbpedia.org/ontology/programmingLanguage"));
    }

    @Test
    public void testExtractResourcesWithSpecialCharacters() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX res: <http://dbpedia.org/resource/> SELECT DISTINCT ?uri WHERE {          res:Suriname dbo:officialLanguage ?uri . }";
        String question = "Österreich";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
        assertTrue(actual.contains("http://dbpedia.org/resource/Österreich"));
    }

    @Test
    public void testExtractResourcesWithoutSynonyms() {
        String query = "PREFIX res: <http://dbpedia.org/resource/> PREFIX dbo: <http://dbpedia.org/ontology/> SELECT DISTINCT ?uri WHERE {res:Douglas_Hofstadter dbo:award ?uri .}";
        String question = "Which awards did Douglas Hofstadter win?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
        assertFalse(actual.contains("http://dbpedia.org/ontology/birthPlace"));
        assertFalse(actual.contains("http://dbpedia.org/ontology/deathPlace"));
    }

    @Test
    public void testExtractResourcesWontUseBe() {
        String query = "PREFIX res: <http://dbpedia.org/resource/> PREFIX dbo: <http://dbpedia.org/ontology/> SELECT DISTINCT ?uri WHERE {res:Douglas_Hofstadter dbo:award ?uri .}";
        String question = "What is Batman's real name?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
        assertFalse(actual.contains("http://dbpedia.org/property/be"));
    }

    @Test
    public void testExtractResourcesWontUseStopwordThe() {
        String query = "PREFIX res: <http://dbpedia.org/resource/> PREFIX dbo: <http://dbpedia.org/ontology/> SELECT DISTINCT ?uri WHERE {res:Douglas_Hofstadter dbo:award ?uri .}";
        String question = "Give me all members of the The Prodigy!";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
        assertFalse(actual.contains("http://dbpedia.org/resource/The_The"));
    }

    @Test
    public void testExtractResourcesWontUseBeWithSynonyms() {
        String query = "PREFIX res: <http://dbpedia.org/resource/> PREFIX dbo: <http://dbpedia.org/ontology/> SELECT DISTINCT ?uri WHERE {res:Douglas_Hofstadter dbo:award ?uri .}";
        String question = "What is Batman's real name?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntitiesUsingSynonyms(question);
        assertFalse(actual.contains("http://dbpedia.org/property/be"));
    }

    @Test
    public void testExtractResourcesWontUseCost() {
        String query = "PREFIX res: <http://dbpedia.org/resource/> PREFIX dbo: <http://dbpedia.org/ontology/> SELECT DISTINCT ?uri WHERE {res:Douglas_Hofstadter dbo:award ?uri .}";
        String question = "Who was Vincent van Gogh inspired by?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
        assertFalse(actual.contains("http://dbpedia.org/ontology/cost"));
    }

    @Test
    public void testExtractResourcesWontUseCostWithSynonyms() {
        String query = "PREFIX res: <http://dbpedia.org/resource/> PREFIX dbo: <http://dbpedia.org/ontology/> SELECT DISTINCT ?uri WHERE {res:Douglas_Hofstadter dbo:award ?uri .}";
        String question = "Who was Vincent van Gogh inspired by?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntitiesUsingSynonyms(question);
        assertFalse(actual.contains("http://dbpedia.org/ontology/cost"));
    }

    @Test
    public void testExtractResourcesWontUseMapWithSynonyms() {
        String query = "PREFIX res: <http://dbpedia.org/resource/> PREFIX dbo: <http://dbpedia.org/ontology/> SELECT DISTINCT ?uri WHERE {res:Douglas_Hofstadter dbo:award ?uri .}";
        String question = "Where was Bach born?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntitiesUsingSynonyms(question);
        assertFalse(actual.contains("http://dbpedia.org/ontology/map"));
    }

    @Test
    public void testExtractResourcesWontUseQuestionWords() {
        String query = "PREFIX res: <http://dbpedia.org/resource/> PREFIX dbo: <http://dbpedia.org/ontology/> SELECT DISTINCT ?uri WHERE {res:Douglas_Hofstadter dbo:award ?uri .}";
        String question = "Where was Bach born?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntitiesUsingSynonyms(question);
        assertFalse(actual.contains("http://dbpedia.org/resource/Where"));
    }

    @Test
    public void testExtractResourcesWontUseQuestionWordsForSynonyms() {
        String query = "PREFIX res: <http://dbpedia.org/resource/> PREFIX dbo: <http://dbpedia.org/ontology/> SELECT DISTINCT ?uri WHERE {res:Douglas_Hofstadter dbo:award ?uri .}";
        String question = "Who was the pope that founded the Vatican Television?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntitiesUsingSynonyms(question);
        assertFalse(actual.contains("http://dbpedia.org/ontology/deathPlace"));
    }

    @Test
    @Ignore
    public void testExtractResourcesUsesHypernyms() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX res: <http://dbpedia.org/resource/> SELECT DISTINCT ?uri WHERE {res:Abraham_Lincoln dbo:spouse ?uri.}";
        String question = "Who was the wife of U.S. president Lincoln?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        Set<String> actual = queryMappingFactory.extractEntities(question);
        assertTrue(actual.contains("http://dbpedia.org/ontology/spouse"));
    }

    @Test
    public void testGenerateQueries() {
        String graph = " {\"1\" @\"p\" \"2\"}";
        String query = "SELECT DISTINCT ?uri WHERE {  <http://dbpedia.org/resource/San_Pedro_de_Atacama> <http://dbpedia.org/ontology/timeZone> ?uri . }";
        String question = "What is the timezone in San Pedro de Atacama?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        SemanticAnalysisHelper semanticAnalysisHelper = new SemanticAnalysisHelper();
        List<CustomQuestion> customQuestions = new ArrayList<>();
        customQuestions.add(new CustomQuestion("SELECT DISTINCT ?num WHERE {  <http://dbpedia.org/resource/Colombo_Lighthouse> <http://dbpedia.org/ontology/height> ?num . }",
                "", null, graph, new HashMap<>()));
        List<String> dBpediaProperties = DBpediaPropertiesProvider.getDBpediaProperties();
        Map<String, QueryTemplateMapping> mappings = semanticAnalysisHelper.extractTemplates(customQuestions, newArrayList(nodes), dBpediaProperties);

        List<String> actualQueries = queryMappingFactory.generateQueries(mappings, graph, false);
        List<SPARQLResultSet> sparqlResultSets = SPARQLUtilities.executeSPARQLQuery(actualQueries.get(0));

        assertEquals(1, actualQueries.size());
        assertEquals(1, sparqlResultSets.size());
        assertEquals(1, sparqlResultSets.get(0).getResultSet().size());
        assertEquals("http://dbpedia.org/resource/Time_in_Chile", sparqlResultSets.get(0).getResultSet().get(0));
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

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        SemanticAnalysisHelper semanticAnalysisHelper = new SemanticAnalysisHelper();

        List<CustomQuestion> customQuestions = new ArrayList<>();
        customQuestions.add(new CustomQuestion("SELECT DISTINCT ?uri WHERE {  \n" +
                "    ?uri <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Film> .  " +
                "    ?uri <http://dbpedia.org/ontology/starring> <http://dbpedia.org/resource/Julia_Roberts> .  " +
                "    ?uri <http://dbpedia.org/ontology/starring> <http://dbpedia.org/resource/Richard_Gere> . " +
                "}",
                "", null, graph, new HashMap<>()));
        Map<String, QueryTemplateMapping> mappings = semanticAnalysisHelper.extractTemplates(customQuestions, newArrayList(nodes), properties);


        List<String> actualQueries = queryMappingFactory.generateQueries(mappings, graph, false);

        assertEquals(1, actualQueries.size());
        assertTrue(actualQueries.get(0).contains("?uri ?property_0 ?class_0 . ?uri ?property_1 ?class_1 . ?uri ?property_2 ?class_2 ."));
    }

    @Test
    public void testGenerateQueriesWithFilterInQueryTemplate() {
        String graph = " {\"1\" @\"p\" \"2\"}";
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX dbp: <http://dbpedia.org/property/> PREFIX res: <http://dbpedia.org/resource/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?uri  WHERE {  ?uri rdf:type dbo:City .         ?uri dbo:isPartOf res:New_Jersey .         ?uri dbp:populationTotal ?inhabitants .         FILTER (?inhabitants > 100000) . }";
        String question = "Give me all cities in New Jersey with more than 100000 inhabitants.";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        SemanticAnalysisHelper semanticAnalysisHelper = new SemanticAnalysisHelper();
        List<CustomQuestion> customQuestions = new ArrayList<>();
        customQuestions.add(new CustomQuestion("PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX dbp: <http://dbpedia.org/property/> PREFIX res: <http://dbpedia.org/resource/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?uri  WHERE {  ?uri rdf:type dbo:City .         ?uri dbo:isPartOf res:New_Jersey .         ?uri dbp:populationTotal ?inhabitants . }",
                "", null, graph, new HashMap<>()));
        List<String> dBpediaProperties = DBpediaPropertiesProvider.getDBpediaProperties();
        Map<String, QueryTemplateMapping> mappings = semanticAnalysisHelper.extractTemplates(customQuestions, newArrayList(nodes), dBpediaProperties);

        List<String> actualQueries = queryMappingFactory.generateQueries(mappings, graph, false);

        assertEquals(1, actualQueries.size());
        assertTrue(actualQueries.get(0).startsWith("SELECT DISTINCT ?uri WHERE { ?uri ?property_0 ?class_0 . ?uri ?property_1 ?class_1 . ?uri ?property_2 ?inhabitants ."));
    }

    @Test
    public void testGenerateQueriesStringLiteralInQuery() {
        String graph = " {\"1\" @\"p\" \"2\"}";
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?uri WHERE {          ?uri dbo:office 'President of the United States' .          ?uri dbo:orderInOffice '16th' . }";
        String question = "Who was the 16th president of the United States?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        SemanticAnalysisHelper semanticAnalysisHelper = new SemanticAnalysisHelper();
        List<CustomQuestion> customQuestions = new ArrayList<>();
        customQuestions.add(new CustomQuestion("PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?uri WHERE {          ?uri dbo:office 'President of the United States' .          ?uri dbo:orderInOffice '16th' . }",
                "", null, graph, new HashMap<>()));
        List<String> dBpediaProperties = DBpediaPropertiesProvider.getDBpediaProperties();
        Map<String, QueryTemplateMapping> mappings = semanticAnalysisHelper.extractTemplates(customQuestions, newArrayList(nodes), dBpediaProperties);

        List<String> actualQueries = queryMappingFactory.generateQueries(mappings, graph, false);

        assertEquals(1, actualQueries.size());
        assertTrue(actualQueries.get(0).startsWith("SELECT DISTINCT ?uri WHERE { ?uri ?property_0 'President of the United States' . ?uri ?property_1 '16th' .  "));
    }

    @Test
    public void testGenerateQueriesStringLiteralInQuery2() {
        String graph = " {\"1\" @\"p\" \"2\"}";
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> ASK  WHERE {  ?uri rdf:type dbo:VideoGame .         ?uri rdfs:label 'Battle Chess'@en . }";
        String question = "Is there a video game called Battle Chess?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        SemanticAnalysisHelper semanticAnalysisHelper = new SemanticAnalysisHelper();
        List<CustomQuestion> customQuestions = new ArrayList<>();
        customQuestions.add(new CustomQuestion("PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> ASK  WHERE {  ?uri rdf:type dbo:VideoGame .         ?uri rdfs:label 'Battle Chess'@en . }",
                "", null, graph, new HashMap<>()));
        List<String> dBpediaProperties = DBpediaPropertiesProvider.getDBpediaProperties();
        Map<String, QueryTemplateMapping> mappings = semanticAnalysisHelper.extractTemplates(customQuestions, newArrayList(nodes), dBpediaProperties);

        List<String> actualQueries = queryMappingFactory.generateQueries(mappings, graph, false);

        assertEquals(1, actualQueries.size());
        assertTrue(actualQueries.get(0).startsWith("ASK WHERE { ?uri ?property_0 ?class_0 . ?uri ?property_1 'Battle Chess'@en .  VALUES "));
        assertTrue(actualQueries.get(0).contains("CONCAT( ?uri, ?property_1, 'Battle Chess'@en )"));
    }

    @Test
    public void testGenerateQueriesForSuperlativeAscQuestion() {
        String graph = " {\"1\" @\"p\" \"2\"}";
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX res: <http://dbpedia.org/resource/> SELECT DISTINCT ?uri WHERE { res:Meryl_Streep dbo:child ?uri . ?uri dbo:birthDate ?d . } ORDER BY ASC(?d) OFFSET 0 LIMIT 1";
        String question = "Who is the oldest child of Meryl Streep?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        SemanticAnalysisHelper semanticAnalysisHelper = new SemanticAnalysisHelper();
        List<CustomQuestion> customQuestions = new ArrayList<>();
        customQuestions.add(new CustomQuestion("PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX res: <http://dbpedia.org/resource/> SELECT DISTINCT ?uri WHERE { res:Meryl_Streep dbo:child ?uri . ?uri dbo:birthDate ?d . } ORDER BY ASC(?d) OFFSET 0 LIMIT 1",
                "", null, graph, new HashMap<>()));
        List<String> dBpediaProperties = DBpediaPropertiesProvider.getDBpediaProperties();
        Map<String, QueryTemplateMapping> mappings = semanticAnalysisHelper.extractTemplates(customQuestions, newArrayList(nodes), dBpediaProperties);

        List<String> actualQueries = queryMappingFactory.generateQueries(mappings, graph, false);

        assertEquals(1, actualQueries.size());
        assertTrue(actualQueries.get(0).startsWith("SELECT DISTINCT ?uri WHERE { ?class_0 ?property_0 ?uri . ?uri ?property_1 ?d . "));
        assertTrue(actualQueries.get(0).endsWith("ORDER BY ASC(?d) OFFSET 0 LIMIT 1"));
    }

    @Test
    public void testGenerateQueriesForSuperlativeDescQuestion() {
        String graph = " {\"1\" @\"p\" \"2\"}";
        String query = "SELECT DISTINCT ?uri WHERE { ?uri a <http://dbpedia.org/ontology/Company> . ?uri <http://dbpedia.org/ontology/location> <http://dbpedia.org/resource/India> . ?uri <http://dbpedia.org/ontology/numberOfEmployees> ?n . } ORDER BY DESC(?n) OFFSET 0 LIMIT 1";
        String question = "Which Indian company has the most employees?";

        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> properties = DBpediaPropertiesProvider.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        SemanticAnalysisHelper semanticAnalysisHelper = new SemanticAnalysisHelper();
        List<CustomQuestion> customQuestions = new ArrayList<>();
        customQuestions.add(new CustomQuestion("SELECT DISTINCT ?uri WHERE { ?uri a <http://dbpedia.org/ontology/Company> . ?uri <http://dbpedia.org/ontology/location> <http://dbpedia.org/resource/India> . ?uri <http://dbpedia.org/ontology/numberOfEmployees> ?n . } ORDER BY DESC(?n) OFFSET 0 LIMIT 1",
                "", null, graph, new HashMap<>()));
        List<String> dBpediaProperties = DBpediaPropertiesProvider.getDBpediaProperties();
        Map<String, QueryTemplateMapping> mappings = semanticAnalysisHelper.extractTemplates(customQuestions, newArrayList(nodes), dBpediaProperties);

        List<String> actualQueries = queryMappingFactory.generateQueries(mappings, graph, false);

        assertEquals(1, actualQueries.size());
        assertTrue(actualQueries.get(0).startsWith("SELECT DISTINCT ?uri WHERE { ?uri a ?class_0 . ?uri ?property_0 ?class_1 . ?uri ?property_1 ?n . "));
        assertTrue(actualQueries.get(0).endsWith(" ORDER BY DESC(?n) OFFSET 0 LIMIT 1"));
    }

    @Test
    public void testGetPropertyWithMultipleWords() {
        String question = "population density rank";
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "SELECT DISTINCT ?uri WHERE { res:Nile dbo:city ?uri . }";
        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> dBpediaProperties = DBpediaPropertiesProvider.getDBpediaProperties();

        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, dBpediaProperties);
        List<String> properties = queryMappingFactory.getProperties("population density rank");

        assertTrue(properties.contains("http://dbpedia.org/property/populationDensityRank"));
    }

    @Test
    public void testGetClassWithMultipleWords() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "SELECT DISTINCT ?uri WHERE { res:Nile dbo:city ?uri . }";
        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> dBpediaProperties = DBpediaPropertiesProvider.getDBpediaProperties();

        QueryMappingFactory queryMappingFactory = new QueryMappingFactory("", query, nodes, dBpediaProperties);
        List<String> classes = queryMappingFactory.getOntologyClass("populated place");

        assertTrue(classes.contains("http://dbpedia.org/ontology/PopulatedPlace"));
    }

    @Test
    public void testGetClassWithMultipleWords2() {
        String query = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "SELECT DISTINCT ?uri WHERE { res:Nile dbo:city ?uri . }";
        List<RDFNode> nodes = Lists.newArrayList(NTripleParser.getNodes());
        List<String> dBpediaProperties = DBpediaPropertiesProvider.getDBpediaProperties();

        QueryMappingFactory queryMappingFactory = new QueryMappingFactory("", query, nodes, dBpediaProperties);
        List<String> classes = queryMappingFactory.getOntologyClass("How much is the population density rank of Germany?");

        assertTrue(classes.contains("http://dbpedia.org/ontology/PopulatedPlace"));
    }
}

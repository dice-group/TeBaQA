package de.uni.leipzig.tebaqa.helper;

import com.hp.hpl.jena.rdf.model.RDFNode;
import de.uni.leipzig.tebaqa.model.QueryTemplateMapping;
import org.assertj.core.util.Lists;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class QueryMappingFactoryTest {

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


        List<QueryTemplateMapping> templates = new ArrayList<>();
        QueryTemplateMapping template1 = new QueryTemplateMapping(2, 2);
        template1.addSelectTemplate("SELECT DISTINCT ?uri WHERE { <^NNP4^> <^NN3^> ?x . <^NN2^> <^NN1^> ?uri }");
        templates.add(template1);

        QueryTemplateMapping template2 = new QueryTemplateMapping(2, 2);
        template2.addSelectTemplate("SELECT DISTINCT ?uri WHERE { <^NNP4^> <^NN3^> ?x . <^NNP4^> <^NN3^> ?x }");
        templates.add(template2);

        Map<String, List<QueryTemplateMapping>> mappings = new HashMap<>();
        mappings.put("", templates);

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

        List<QueryTemplateMapping> templates = new ArrayList<>();
        QueryTemplateMapping template1 = new QueryTemplateMapping(1, 1);
        template1.addSelectTemplate("SELECT DISTINCT ?uri WHERE { <^NNP4^> <^NN3^> ?x }");
        templates.add(template1);

        QueryTemplateMapping template2 = new QueryTemplateMapping(1, 1);
        template2.addSelectTemplate("SELECT DISTINCT ?uri WHERE { <^NNP4^> <^NN3^> ?x }");
        templates.add(template2);

        List<String> expected = new ArrayList<>();
        expected.add("SELECT DISTINCT ?uri WHERE { ?class_ ?property_ ?x . VALUES (?class_) {(<http://dbpedia.org/resource/Nile>) (<http://dbpedia.org/ontology/Country>)} VALUES (?property_) {(<http://dbpedia.org/ontology/country>) (<http://dbpedia.org/ontology/start>)}}");

        Map<String, List<QueryTemplateMapping>> mappings = new HashMap<>();
        mappings.put("", templates);

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

        List<QueryTemplateMapping> templates = new ArrayList<>();
        QueryTemplateMapping template1 = new QueryTemplateMapping(1, 0);
        template1.addSelectTemplate("SELECT DISTINCT ?uri WHERE { <^NNP4^> a ?x }");
        templates.add(template1);

        List<String> expected = new ArrayList<>();
        expected.add("SELECT DISTINCT ?uri WHERE { ?class_ a ?x . VALUES (?class_) {(<http://dbpedia.org/resource/Nile>) (<http://dbpedia.org/ontology/Country>)} VALUES (?property_) {(<http://dbpedia.org/ontology/country>) (<http://dbpedia.org/ontology/start>)}}");

        Map<String, List<QueryTemplateMapping>> mappings = new HashMap<>();
        mappings.put("", templates);

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

        List<QueryTemplateMapping> templates = new ArrayList<>();
        QueryTemplateMapping template1 = new QueryTemplateMapping(0, 1);
        template1.addSelectTemplate("SELECT DISTINCT ?uri WHERE { ?x <^NN3^> ?x }");
        templates.add(template1);

        List<String> expected = new ArrayList<>();
        expected.add("SELECT DISTINCT ?uri WHERE { ?x ?property_ ?x . VALUES (?class_) {(<http://dbpedia.org/resource/Nile>) (<http://dbpedia.org/ontology/Country>)} VALUES (?property_) {(<http://dbpedia.org/ontology/country>) (<http://dbpedia.org/ontology/start>)}}");

        Map<String, List<QueryTemplateMapping>> mappings = new HashMap<>();
        mappings.put("", templates);

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

        List<QueryTemplateMapping> templates = new ArrayList<>();
        QueryTemplateMapping template1 = new QueryTemplateMapping(3, 3);
        template1.addSelectTemplate("SELECT DISTINCT ?uri WHERE { <^NNP4^> <^NN3^> ?x. <^NNP4^> <^NN3^> ?x. <^NNP4^> <^NN3^> ?x }");
        templates.add(template1);

        QueryTemplateMapping mapping2 = new QueryTemplateMapping(3, 3);
        mapping2.addSelectTemplate("SELECT DISTINCT ?uri WHERE { <^NNP4^> <^NN3^> ?x . <^NNP4^> <^NN3^> ?x . <^NNP4^> <^NN3^> ?x }");
        templates.add(mapping2);

        Map<String, List<QueryTemplateMapping>> mappings = new HashMap<>();
        mappings.put("", templates);

        List<String> expected = new ArrayList<>();
        expected.add("SELECT DISTINCT ?uri WHERE { ?class_ ?property_ ?x. ?class_ ?property_ ?x. ?class_ ?property_ ?x . VALUES (?class_) {(<http://dbpedia.org/resource/Nile>) (<http://dbpedia.org/ontology/Country>)} VALUES (?property_) {(<http://dbpedia.org/ontology/country>) (<http://dbpedia.org/ontology/start>)}}");
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


        List<QueryTemplateMapping> templates = new ArrayList<>();
        QueryTemplateMapping template1 = new QueryTemplateMapping(0, 0);
        template1.addSelectTemplate("SELECT DISTINCT ?uri WHERE { <^http://foo.bar.some.thing^> <^NN3^> ?x. <http://some.url> <^NN3^> ?x. <http://foo.bar.com> <^NN3^> ?x }");
        templates.add(template1);

        Map<String, List<QueryTemplateMapping>> mappings = new HashMap<>();
        mappings.put("", templates);

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
}

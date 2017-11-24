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
        template1.addSelectTemplate("SELECT DISTINCT ?uri WHERE { <^NNP4^> <^NN3^> ?x . <^NN2^> <^NN1^> ?uri }", "SELECT DISTINCT ?uri WHERE { ?class_0 ?property_0 ?x . ?class_1 ?property_1 ?uri . VALUES (?class_0) {(<http://dbpedia.org/resource/Nile>) (<http://dbpedia.org/ontology/CountrySeat>) (<http://dbpedia.org/ontology/Country>)} VALUES (?class_1) {(<http://dbpedia.org/resource/Nile>) (<http://dbpedia.org/ontology/CountrySeat>) (<http://dbpedia.org/ontology/Country>)} VALUES (?property_0) {(<http://dbpedia.org/ontology/sportCountry>) (<http://dbpedia.org/ontology/countryRank>) (<http://dbpedia.org/ontology/startReign>) (<http://dbpedia.org/ontology/mouthCountry>) (<http://dbpedia.org/ontology/lawCountry>) (<http://dbpedia.org/ontology/usingCountry>) (<http://dbpedia.org/ontology/capitalCountry>) (<http://dbpedia.org/ontology/startWct>) (<http://dbpedia.org/ontology/country>) (<http://dbpedia.org/ontology/countryOrigin>) (<http://dbpedia.org/ontology/startPoint>) (<http://dbpedia.org/ontology/startYear>) (<http://dbpedia.org/ontology/startDate>) (<http://dbpedia.org/ontology/start>) (<http://dbpedia.org/ontology/twinCountry>) (<http://dbpedia.org/ontology/sourceCountry>) (<http://dbpedia.org/ontology/startWqs>)} VALUES (?property_1) {(<http://dbpedia.org/ontology/sportCountry>) (<http://dbpedia.org/ontology/countryRank>) (<http://dbpedia.org/ontology/startReign>) (<http://dbpedia.org/ontology/mouthCountry>) (<http://dbpedia.org/ontology/lawCountry>) (<http://dbpedia.org/ontology/usingCountry>) (<http://dbpedia.org/ontology/capitalCountry>) (<http://dbpedia.org/ontology/startWct>) (<http://dbpedia.org/ontology/country>) (<http://dbpedia.org/ontology/countryOrigin>) (<http://dbpedia.org/ontology/startPoint>) (<http://dbpedia.org/ontology/startYear>) (<http://dbpedia.org/ontology/startDate>) (<http://dbpedia.org/ontology/start>) (<http://dbpedia.org/ontology/twinCountry>) (<http://dbpedia.org/ontology/sourceCountry>) (<http://dbpedia.org/ontology/startWqs>)} FILTER (CONCAT( ?class_0, ?property_0, ?x ) != CONCAT( ?class_1, ?property_1, ?uri ))  FILTER (CONCAT( ?class_1, ?property_1, ?uri ) != CONCAT( ?class_0, ?property_0, ?x )) }");

        Map<String, QueryTemplateMapping> mappings = new HashMap<>();
        mappings.put("1", template1);

        List<String> expected = new ArrayList<>();
        expected.add("SELECT DISTINCT ?uri WHERE { ?class_0 ?property_0 ?x . ?class_1 ?property_1 ?uri . VALUES (?class_0) {(<http://dbpedia.org/datatype/Area>) (<http://dbpedia.org/ontology/State>) (<http://dbpedia.org/ontology/RestArea>) (<http://dbpedia.org/ontology/Area>) (<http://dbpedia.org/ontology/Territory>) (<http://dbpedia.org/ontology/CountrySeat>) (<http://dbpedia.org/ontology/Country>) (<http://dbpedia.org/ontology/BoardGame>) (<http://dbpedia.org/resource/Nile>) (<http://dbpedia.org/ontology/CrossCountrySkier>) (<http://dbpedia.org/ontology/Arena>) (<http://dbpedia.org/ontology/HistoricalCountry>) (<http://dbpedia.org/ontology/SkiArea>)} VALUES (?class_1) {(<http://dbpedia.org/datatype/Area>) (<http://dbpedia.org/ontology/State>) (<http://dbpedia.org/ontology/RestArea>) (<http://dbpedia.org/ontology/Area>) (<http://dbpedia.org/ontology/Territory>) (<http://dbpedia.org/ontology/CountrySeat>) (<http://dbpedia.org/ontology/Country>) (<http://dbpedia.org/ontology/BoardGame>) (<http://dbpedia.org/resource/Nile>) (<http://dbpedia.org/ontology/CrossCountrySkier>) (<http://dbpedia.org/ontology/Arena>) (<http://dbpedia.org/ontology/HistoricalCountry>) (<http://dbpedia.org/ontology/SkiArea>)} VALUES (?property_0) {(<http://dbpedia.org/ontology/GeopoliticalOrganisation/areaMetro>) (<http://dbpedia.org/ontology/continent>) (<http://dbpedia.org/ontology/sourceConfluenceCountry>) (<http://dbpedia.org/ontology/actScore>) (<http://dbpedia.org/ontology/deathPlace>) (<http://dbpedia.org/ontology/mouthState>) (<http://dbpedia.org/ontology/areaQuote>) (<http://dbpedia.org/ontology/areaOfSearch>) (<http://dbpedia.org/ontology/startOccupation>) (<http://dbpedia.org/ontology/startYear>) (<http://dbpedia.org/ontology/isPartOf>) (<http://dbpedia.org/ontology/floorArea>) (<http://dbpedia.org/ontology/start>) (<http://dbpedia.org/ontology/serviceStartDate>) (<http://dbpedia.org/ontology/secondDriverCountry>) (<http://dbpedia.org/ontology/urbanArea>) (<http://dbpedia.org/ontology/lowestState>) (<http://dbpedia.org/ontology/workArea>) (<http://dbpedia.org/ontology/part>) (<http://dbpedia.org/ontology/land>) (<http://dbpedia.org/ontology/managementCountry>) (<http://dbpedia.org/ontology/wholeArea>) (<http://dbpedia.org/ontology/areaMetro>) (<http://dbpedia.org/ontology/thirdDriverCountry>) (<http://dbpedia.org/ontology/dateAct>) (<http://dbpedia.org/ontology/Planet/surfaceArea>) (<http://dbpedia.org/ontology/locationCountry>) (<http://dbpedia.org/ontology/landtag>) (<http://dbpedia.org/ontology/mouthCountry>) (<http://dbpedia.org/ontology/areaWater>) (<http://dbpedia.org/ontology/country>) (<http://dbpedia.org/ontology/area>) (<http://dbpedia.org/ontology/federalState>) (<http://dbpedia.org/ontology/locatedInArea>) (<http://dbpedia.org/resource/start_up>) (<http://dbpedia.org/ontology/stateOfOrigin>) (<http://dbpedia.org/ontology/Building/floorArea>) (<http://dbpedia.org/ontology/sportCountry>) (<http://dbpedia.org/ontology/hraState>) (<http://dbpedia.org/ontology/PopulatedPlace/area>) (<http://dbpedia.org/ontology/countryRank>) (<http://dbpedia.org/ontology/serviceStartYear>) (<http://dbpedia.org/ontology/areaUrban>) (<http://dbpedia.org/ontology/capitalCountry>) (<http://dbpedia.org/ontology/startDateTime>) (<http://dbpedia.org/ontology/areaDate>) (<http://dbpedia.org/ontology/state>) (<http://dbpedia.org/ontology/deathCause>) (<http://dbpedia.org/ontology/startCareer>) (<http://dbpedia.org/ontology/countryOrigin>) (<http://dbpedia.org/ontology/PopulatedPlace/areaMetro>) (<http://dbpedia.org/ontology/startDate>) (<http://dbpedia.org/ontology/ccaState>) (<http://dbpedia.org/ontology/surfaceArea>) (<http://dbpedia.org/ontology/dressCode>) (<http://dbpedia.org/ontology/Galaxy/surfaceArea>) (<http://dbpedia.org/ontology/alpsMainPart>) (<http://dbpedia.org/ontology/landArea>) (<http://dbpedia.org/ontology/startYearOfSales>) (<http://dbpedia.org/ontology/wingArea>) (<http://dbpedia.org/ontology/rankArea>) (<http://dbpedia.org/resource/get_going>) (<http://dbpedia.org/ontology/startWqs>) (<http://dbpedia.org/ontology/filmFareAward>) (<http://dbpedia.org/ontology/PopulatedPlace/areaUrban>) (<http://dbpedia.org/ontology/areaTotal>) (<http://dbpedia.org/ontology/startReign>) (<http://dbpedia.org/ontology/areaCode>) (<http://dbpedia.org/ontology/principalArea>) (<http://dbpedia.org/ontology/poleDriverCountry>) (<http://dbpedia.org/ontology/areaLand>) (<http://dbpedia.org/ontology/startPoint>) (<http://dbpedia.org/ontology/waterArea>) (<http://dbpedia.org/ontology/fareZone>) (<http://dbpedia.org/ontology/maximumArea>) (<http://dbpedia.org/ontology/sourceCountry>) (<http://dbpedia.org/resource/start_out>) (<http://dbpedia.org/ontology/nation>) (<http://dbpedia.org/ontology/stateDelegate>) (<http://dbpedia.org/ontology/routeStart>) (<http://dbpedia.org/ontology/usingCountry>) (<http://dbpedia.org/ontology/managerClub>) (<http://dbpedia.org/ontology/manager>) (<http://dbpedia.org/ontology/causeOfDeath>) (<http://dbpedia.org/ontology/fastestDriverCountry>) (<http://dbpedia.org/ontology/sovereignCountry>) (<http://dbpedia.org/ontology/broadcastArea>) (<http://dbpedia.org/ontology/firstDriverCountry>) (<http://dbpedia.org/ontology/highestState>) (<http://dbpedia.org/ontology/sourceState>) (<http://dbpedia.org/ontology/areaRural>) (<http://dbpedia.org/ontology/birthPlace>) (<http://dbpedia.org/ontology/lawCountry>) (<http://dbpedia.org/ontology/PopulatedPlace/areaTotal>) (<http://dbpedia.org/ontology/areaRank>) (<http://dbpedia.org/ontology/startWct>) (<http://dbpedia.org/ontology/governmentCountry>) (<http://dbpedia.org/ontology/isCityState>) (<http://dbpedia.org/ontology/twinCountry>)} VALUES (?property_1) {(<http://dbpedia.org/ontology/GeopoliticalOrganisation/areaMetro>) (<http://dbpedia.org/ontology/continent>) (<http://dbpedia.org/ontology/sourceConfluenceCountry>) (<http://dbpedia.org/ontology/actScore>) (<http://dbpedia.org/ontology/deathPlace>) (<http://dbpedia.org/ontology/mouthState>) (<http://dbpedia.org/ontology/areaQuote>) (<http://dbpedia.org/ontology/areaOfSearch>) (<http://dbpedia.org/ontology/startOccupation>) (<http://dbpedia.org/ontology/startYear>) (<http://dbpedia.org/ontology/isPartOf>) (<http://dbpedia.org/ontology/floorArea>) (<http://dbpedia.org/ontology/start>) (<http://dbpedia.org/ontology/serviceStartDate>) (<http://dbpedia.org/ontology/secondDriverCountry>) (<http://dbpedia.org/ontology/urbanArea>) (<http://dbpedia.org/ontology/lowestState>) (<http://dbpedia.org/ontology/workArea>) (<http://dbpedia.org/ontology/part>) (<http://dbpedia.org/ontology/land>) (<http://dbpedia.org/ontology/managementCountry>) (<http://dbpedia.org/ontology/wholeArea>) (<http://dbpedia.org/ontology/areaMetro>) (<http://dbpedia.org/ontology/thirdDriverCountry>) (<http://dbpedia.org/ontology/dateAct>) (<http://dbpedia.org/ontology/Planet/surfaceArea>) (<http://dbpedia.org/ontology/locationCountry>) (<http://dbpedia.org/ontology/landtag>) (<http://dbpedia.org/ontology/mouthCountry>) (<http://dbpedia.org/ontology/areaWater>) (<http://dbpedia.org/ontology/country>) (<http://dbpedia.org/ontology/area>) (<http://dbpedia.org/ontology/federalState>) (<http://dbpedia.org/ontology/locatedInArea>) (<http://dbpedia.org/resource/start_up>) (<http://dbpedia.org/ontology/stateOfOrigin>) (<http://dbpedia.org/ontology/Building/floorArea>) (<http://dbpedia.org/ontology/sportCountry>) (<http://dbpedia.org/ontology/hraState>) (<http://dbpedia.org/ontology/PopulatedPlace/area>) (<http://dbpedia.org/ontology/countryRank>) (<http://dbpedia.org/ontology/serviceStartYear>) (<http://dbpedia.org/ontology/areaUrban>) (<http://dbpedia.org/ontology/capitalCountry>) (<http://dbpedia.org/ontology/startDateTime>) (<http://dbpedia.org/ontology/areaDate>) (<http://dbpedia.org/ontology/state>) (<http://dbpedia.org/ontology/deathCause>) (<http://dbpedia.org/ontology/startCareer>) (<http://dbpedia.org/ontology/countryOrigin>) (<http://dbpedia.org/ontology/PopulatedPlace/areaMetro>) (<http://dbpedia.org/ontology/startDate>) (<http://dbpedia.org/ontology/ccaState>) (<http://dbpedia.org/ontology/surfaceArea>) (<http://dbpedia.org/ontology/dressCode>) (<http://dbpedia.org/ontology/Galaxy/surfaceArea>) (<http://dbpedia.org/ontology/alpsMainPart>) (<http://dbpedia.org/ontology/landArea>) (<http://dbpedia.org/ontology/startYearOfSales>) (<http://dbpedia.org/ontology/wingArea>) (<http://dbpedia.org/ontology/rankArea>) (<http://dbpedia.org/resource/get_going>) (<http://dbpedia.org/ontology/startWqs>) (<http://dbpedia.org/ontology/filmFareAward>) (<http://dbpedia.org/ontology/PopulatedPlace/areaUrban>) (<http://dbpedia.org/ontology/areaTotal>) (<http://dbpedia.org/ontology/startReign>) (<http://dbpedia.org/ontology/areaCode>) (<http://dbpedia.org/ontology/principalArea>) (<http://dbpedia.org/ontology/poleDriverCountry>) (<http://dbpedia.org/ontology/areaLand>) (<http://dbpedia.org/ontology/startPoint>) (<http://dbpedia.org/ontology/waterArea>) (<http://dbpedia.org/ontology/fareZone>) (<http://dbpedia.org/ontology/maximumArea>) (<http://dbpedia.org/ontology/sourceCountry>) (<http://dbpedia.org/resource/start_out>) (<http://dbpedia.org/ontology/nation>) (<http://dbpedia.org/ontology/stateDelegate>) (<http://dbpedia.org/ontology/routeStart>) (<http://dbpedia.org/ontology/usingCountry>) (<http://dbpedia.org/ontology/managerClub>) (<http://dbpedia.org/ontology/manager>) (<http://dbpedia.org/ontology/causeOfDeath>) (<http://dbpedia.org/ontology/fastestDriverCountry>) (<http://dbpedia.org/ontology/sovereignCountry>) (<http://dbpedia.org/ontology/broadcastArea>) (<http://dbpedia.org/ontology/firstDriverCountry>) (<http://dbpedia.org/ontology/highestState>) (<http://dbpedia.org/ontology/sourceState>) (<http://dbpedia.org/ontology/areaRural>) (<http://dbpedia.org/ontology/birthPlace>) (<http://dbpedia.org/ontology/lawCountry>) (<http://dbpedia.org/ontology/PopulatedPlace/areaTotal>) (<http://dbpedia.org/ontology/areaRank>) (<http://dbpedia.org/ontology/startWct>) (<http://dbpedia.org/ontology/governmentCountry>) (<http://dbpedia.org/ontology/isCityState>) (<http://dbpedia.org/ontology/twinCountry>)} FILTER (CONCAT( ?class_0, ?property_0, ?x ) != CONCAT( ?class_1, ?property_1, ?uri ))  FILTER (CONCAT( ?class_1, ?property_1, ?uri ) != CONCAT( ?class_0, ?property_0, ?x )) }");

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
        template1.addSelectTemplate("SELECT DISTINCT ?uri WHERE { <^NNP4^> <^NN3^> ?x }", "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "SELECT DISTINCT ?uri WHERE { res:Nile dbo:city ?uri . }");

        List<String> expected = new ArrayList<>();
        expected.add("SELECT DISTINCT ?uri WHERE { ?class_0 ?property_0 ?x . VALUES (?class_0) {(<http://dbpedia.org/datatype/Area>) (<http://dbpedia.org/ontology/State>) (<http://dbpedia.org/ontology/RestArea>) (<http://dbpedia.org/ontology/Area>) (<http://dbpedia.org/ontology/Territory>) (<http://dbpedia.org/ontology/CountrySeat>) (<http://dbpedia.org/ontology/Country>) (<http://dbpedia.org/ontology/BoardGame>) (<http://dbpedia.org/resource/Nile>) (<http://dbpedia.org/ontology/CrossCountrySkier>) (<http://dbpedia.org/ontology/Arena>) (<http://dbpedia.org/ontology/HistoricalCountry>) (<http://dbpedia.org/ontology/SkiArea>)} VALUES (?property_0) {(<http://dbpedia.org/ontology/GeopoliticalOrganisation/areaMetro>) (<http://dbpedia.org/ontology/continent>) (<http://dbpedia.org/ontology/sourceConfluenceCountry>) (<http://dbpedia.org/ontology/actScore>) (<http://dbpedia.org/ontology/deathPlace>) (<http://dbpedia.org/ontology/mouthState>) (<http://dbpedia.org/ontology/areaQuote>) (<http://dbpedia.org/ontology/areaOfSearch>) (<http://dbpedia.org/ontology/startOccupation>) (<http://dbpedia.org/ontology/startYear>) (<http://dbpedia.org/ontology/isPartOf>) (<http://dbpedia.org/ontology/floorArea>) (<http://dbpedia.org/ontology/start>) (<http://dbpedia.org/ontology/serviceStartDate>) (<http://dbpedia.org/ontology/secondDriverCountry>) (<http://dbpedia.org/ontology/urbanArea>) (<http://dbpedia.org/ontology/lowestState>) (<http://dbpedia.org/ontology/workArea>) (<http://dbpedia.org/ontology/part>) (<http://dbpedia.org/ontology/land>) (<http://dbpedia.org/ontology/managementCountry>) (<http://dbpedia.org/ontology/wholeArea>) (<http://dbpedia.org/ontology/areaMetro>) (<http://dbpedia.org/ontology/thirdDriverCountry>) (<http://dbpedia.org/ontology/dateAct>) (<http://dbpedia.org/ontology/Planet/surfaceArea>) (<http://dbpedia.org/ontology/locationCountry>) (<http://dbpedia.org/ontology/landtag>) (<http://dbpedia.org/ontology/mouthCountry>) (<http://dbpedia.org/ontology/areaWater>) (<http://dbpedia.org/ontology/country>) (<http://dbpedia.org/ontology/area>) (<http://dbpedia.org/ontology/federalState>) (<http://dbpedia.org/ontology/locatedInArea>) (<http://dbpedia.org/resource/start_up>) (<http://dbpedia.org/ontology/stateOfOrigin>) (<http://dbpedia.org/ontology/Building/floorArea>) (<http://dbpedia.org/ontology/sportCountry>) (<http://dbpedia.org/ontology/hraState>) (<http://dbpedia.org/ontology/PopulatedPlace/area>) (<http://dbpedia.org/ontology/countryRank>) (<http://dbpedia.org/ontology/serviceStartYear>) (<http://dbpedia.org/ontology/areaUrban>) (<http://dbpedia.org/ontology/capitalCountry>) (<http://dbpedia.org/ontology/startDateTime>) (<http://dbpedia.org/ontology/areaDate>) (<http://dbpedia.org/ontology/state>) (<http://dbpedia.org/ontology/deathCause>) (<http://dbpedia.org/ontology/startCareer>) (<http://dbpedia.org/ontology/countryOrigin>) (<http://dbpedia.org/ontology/PopulatedPlace/areaMetro>) (<http://dbpedia.org/ontology/startDate>) (<http://dbpedia.org/ontology/ccaState>) (<http://dbpedia.org/ontology/surfaceArea>) (<http://dbpedia.org/ontology/dressCode>) (<http://dbpedia.org/ontology/Galaxy/surfaceArea>) (<http://dbpedia.org/ontology/alpsMainPart>) (<http://dbpedia.org/ontology/landArea>) (<http://dbpedia.org/ontology/startYearOfSales>) (<http://dbpedia.org/ontology/wingArea>) (<http://dbpedia.org/ontology/rankArea>) (<http://dbpedia.org/resource/get_going>) (<http://dbpedia.org/ontology/startWqs>) (<http://dbpedia.org/ontology/filmFareAward>) (<http://dbpedia.org/ontology/PopulatedPlace/areaUrban>) (<http://dbpedia.org/ontology/areaTotal>) (<http://dbpedia.org/ontology/startReign>) (<http://dbpedia.org/ontology/areaCode>) (<http://dbpedia.org/ontology/principalArea>) (<http://dbpedia.org/ontology/poleDriverCountry>) (<http://dbpedia.org/ontology/areaLand>) (<http://dbpedia.org/ontology/startPoint>) (<http://dbpedia.org/ontology/waterArea>) (<http://dbpedia.org/ontology/fareZone>) (<http://dbpedia.org/ontology/maximumArea>) (<http://dbpedia.org/ontology/sourceCountry>) (<http://dbpedia.org/resource/start_out>) (<http://dbpedia.org/ontology/nation>) (<http://dbpedia.org/ontology/stateDelegate>) (<http://dbpedia.org/ontology/routeStart>) (<http://dbpedia.org/ontology/usingCountry>) (<http://dbpedia.org/ontology/managerClub>) (<http://dbpedia.org/ontology/manager>) (<http://dbpedia.org/ontology/causeOfDeath>) (<http://dbpedia.org/ontology/fastestDriverCountry>) (<http://dbpedia.org/ontology/sovereignCountry>) (<http://dbpedia.org/ontology/broadcastArea>) (<http://dbpedia.org/ontology/firstDriverCountry>) (<http://dbpedia.org/ontology/highestState>) (<http://dbpedia.org/ontology/sourceState>) (<http://dbpedia.org/ontology/areaRural>) (<http://dbpedia.org/ontology/birthPlace>) (<http://dbpedia.org/ontology/lawCountry>) (<http://dbpedia.org/ontology/PopulatedPlace/areaTotal>) (<http://dbpedia.org/ontology/areaRank>) (<http://dbpedia.org/ontology/startWct>) (<http://dbpedia.org/ontology/governmentCountry>) (<http://dbpedia.org/ontology/isCityState>) (<http://dbpedia.org/ontology/twinCountry>)}}");

        Map<String, QueryTemplateMapping> mappings = new HashMap<>();
        mappings.put("1", template1);

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
        template1.addSelectTemplate("SELECT DISTINCT ?uri WHERE { <^NNP4^> a ?x }", "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "SELECT DISTINCT ?uri WHERE { res:Nile dbo:city ?uri . }");

        List<String> expected = new ArrayList<>();
        expected.add("SELECT DISTINCT ?uri WHERE { ?class_0 a ?x . VALUES (?class_0) {(<http://dbpedia.org/datatype/Area>) (<http://dbpedia.org/ontology/State>) (<http://dbpedia.org/ontology/RestArea>) (<http://dbpedia.org/ontology/Area>) (<http://dbpedia.org/ontology/Territory>) (<http://dbpedia.org/ontology/CountrySeat>) (<http://dbpedia.org/ontology/Country>) (<http://dbpedia.org/ontology/BoardGame>) (<http://dbpedia.org/resource/Nile>) (<http://dbpedia.org/ontology/CrossCountrySkier>) (<http://dbpedia.org/ontology/Arena>) (<http://dbpedia.org/ontology/HistoricalCountry>) (<http://dbpedia.org/ontology/SkiArea>)}}");

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
        template1.addSelectTemplate("SELECT DISTINCT ?uri WHERE { ?x <^NN3^> ?x }", "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "SELECT DISTINCT ?uri WHERE { res:Nile dbo:city ?uri . }");

        Map<String, QueryTemplateMapping> mappings = new HashMap<>();
        mappings.put("1", template1);

        assertTrue(queryMappingFactory.generateQueries(mappings).size() == 1);
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
        template1.addSelectTemplate("SELECT DISTINCT ?uri WHERE { <^NNP4^> <^NN3^> ?x. <^NNP4^> <^NN3^> ?x. <^NNP4^> <^NN3^> ?x }", "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "SELECT DISTINCT ?uri WHERE { res:Nile dbo:city ?uri . }");

        Map<String, QueryTemplateMapping> mappings = new HashMap<>();
        mappings.put("1", template1);

        assertTrue(queryMappingFactory.generateQueries(mappings).size() == 1);
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

        QueryTemplateMapping template1 = new QueryTemplateMapping(0, 0);
        template1.addSelectTemplate("SELECT DISTINCT ?uri WHERE { <^http://foo.bar.some.thing^> <^NN3^> ?x. <http://some.url> <^NN3^> ?x. <http://foo.bar.com> <^NN3^> ?x } ", "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "SELECT DISTINCT ?uri WHERE { res:Nile dbo:city ?uri . }");

        Map<String, QueryTemplateMapping> mappings = new HashMap<>();
        mappings.put("", template1);

        List<String> expected = new ArrayList<>();
        expected.add("SELECT DISTINCT ?uri WHERE { ?class_0 ?property_0 ?x. <http://some.url> ?property_0 ?x. <http://foo.bar.com> ?property_0 ?x . VALUES (?class_0) {(<http://dbpedia.org/datatype/Area>) (<http://dbpedia.org/ontology/State>) (<http://dbpedia.org/ontology/RestArea>) (<http://dbpedia.org/ontology/Area>) (<http://dbpedia.org/ontology/Territory>) (<http://dbpedia.org/ontology/CountrySeat>) (<http://dbpedia.org/ontology/Country>) (<http://dbpedia.org/ontology/BoardGame>) (<http://dbpedia.org/resource/Nile>) (<http://dbpedia.org/ontology/CrossCountrySkier>) (<http://dbpedia.org/ontology/Arena>) (<http://dbpedia.org/ontology/HistoricalCountry>) (<http://dbpedia.org/ontology/SkiArea>)} VALUES (?class_1) {(<http://dbpedia.org/datatype/Area>) (<http://dbpedia.org/ontology/State>) (<http://dbpedia.org/ontology/RestArea>) (<http://dbpedia.org/ontology/Area>) (<http://dbpedia.org/ontology/Territory>) (<http://dbpedia.org/ontology/CountrySeat>) (<http://dbpedia.org/ontology/Country>) (<http://dbpedia.org/ontology/BoardGame>) (<http://dbpedia.org/resource/Nile>) (<http://dbpedia.org/ontology/CrossCountrySkier>) (<http://dbpedia.org/ontology/Arena>) (<http://dbpedia.org/ontology/HistoricalCountry>) (<http://dbpedia.org/ontology/SkiArea>)} VALUES (?class_2) {(<http://dbpedia.org/datatype/Area>) (<http://dbpedia.org/ontology/State>) (<http://dbpedia.org/ontology/RestArea>) (<http://dbpedia.org/ontology/Area>) (<http://dbpedia.org/ontology/Territory>) (<http://dbpedia.org/ontology/CountrySeat>) (<http://dbpedia.org/ontology/Country>) (<http://dbpedia.org/ontology/BoardGame>) (<http://dbpedia.org/resource/Nile>) (<http://dbpedia.org/ontology/CrossCountrySkier>) (<http://dbpedia.org/ontology/Arena>) (<http://dbpedia.org/ontology/HistoricalCountry>) (<http://dbpedia.org/ontology/SkiArea>)} VALUES (?property_0) {(<http://dbpedia.org/ontology/GeopoliticalOrganisation/areaMetro>) (<http://dbpedia.org/ontology/continent>) (<http://dbpedia.org/ontology/sourceConfluenceCountry>) (<http://dbpedia.org/ontology/actScore>) (<http://dbpedia.org/ontology/deathPlace>) (<http://dbpedia.org/ontology/mouthState>) (<http://dbpedia.org/ontology/areaQuote>) (<http://dbpedia.org/ontology/areaOfSearch>) (<http://dbpedia.org/ontology/startOccupation>) (<http://dbpedia.org/ontology/startYear>) (<http://dbpedia.org/ontology/isPartOf>) (<http://dbpedia.org/ontology/floorArea>) (<http://dbpedia.org/ontology/start>) (<http://dbpedia.org/ontology/serviceStartDate>) (<http://dbpedia.org/ontology/secondDriverCountry>) (<http://dbpedia.org/ontology/urbanArea>) (<http://dbpedia.org/ontology/lowestState>) (<http://dbpedia.org/ontology/workArea>) (<http://dbpedia.org/ontology/part>) (<http://dbpedia.org/ontology/land>) (<http://dbpedia.org/ontology/managementCountry>) (<http://dbpedia.org/ontology/wholeArea>) (<http://dbpedia.org/ontology/areaMetro>) (<http://dbpedia.org/ontology/thirdDriverCountry>) (<http://dbpedia.org/ontology/dateAct>) (<http://dbpedia.org/ontology/Planet/surfaceArea>) (<http://dbpedia.org/ontology/locationCountry>) (<http://dbpedia.org/ontology/landtag>) (<http://dbpedia.org/ontology/mouthCountry>) (<http://dbpedia.org/ontology/areaWater>) (<http://dbpedia.org/ontology/country>) (<http://dbpedia.org/ontology/area>) (<http://dbpedia.org/ontology/federalState>) (<http://dbpedia.org/ontology/locatedInArea>) (<http://dbpedia.org/resource/start_up>) (<http://dbpedia.org/ontology/stateOfOrigin>) (<http://dbpedia.org/ontology/Building/floorArea>) (<http://dbpedia.org/ontology/sportCountry>) (<http://dbpedia.org/ontology/hraState>) (<http://dbpedia.org/ontology/PopulatedPlace/area>) (<http://dbpedia.org/ontology/countryRank>) (<http://dbpedia.org/ontology/serviceStartYear>) (<http://dbpedia.org/ontology/areaUrban>) (<http://dbpedia.org/ontology/capitalCountry>) (<http://dbpedia.org/ontology/startDateTime>) (<http://dbpedia.org/ontology/areaDate>) (<http://dbpedia.org/ontology/state>) (<http://dbpedia.org/ontology/deathCause>) (<http://dbpedia.org/ontology/startCareer>) (<http://dbpedia.org/ontology/countryOrigin>) (<http://dbpedia.org/ontology/PopulatedPlace/areaMetro>) (<http://dbpedia.org/ontology/startDate>) (<http://dbpedia.org/ontology/ccaState>) (<http://dbpedia.org/ontology/surfaceArea>) (<http://dbpedia.org/ontology/dressCode>) (<http://dbpedia.org/ontology/Galaxy/surfaceArea>) (<http://dbpedia.org/ontology/alpsMainPart>) (<http://dbpedia.org/ontology/landArea>) (<http://dbpedia.org/ontology/startYearOfSales>) (<http://dbpedia.org/ontology/wingArea>) (<http://dbpedia.org/ontology/rankArea>) (<http://dbpedia.org/resource/get_going>) (<http://dbpedia.org/ontology/startWqs>) (<http://dbpedia.org/ontology/filmFareAward>) (<http://dbpedia.org/ontology/PopulatedPlace/areaUrban>) (<http://dbpedia.org/ontology/areaTotal>) (<http://dbpedia.org/ontology/startReign>) (<http://dbpedia.org/ontology/areaCode>) (<http://dbpedia.org/ontology/principalArea>) (<http://dbpedia.org/ontology/poleDriverCountry>) (<http://dbpedia.org/ontology/areaLand>) (<http://dbpedia.org/ontology/startPoint>) (<http://dbpedia.org/ontology/waterArea>) (<http://dbpedia.org/ontology/fareZone>) (<http://dbpedia.org/ontology/maximumArea>) (<http://dbpedia.org/ontology/sourceCountry>) (<http://dbpedia.org/resource/start_out>) (<http://dbpedia.org/ontology/nation>) (<http://dbpedia.org/ontology/stateDelegate>) (<http://dbpedia.org/ontology/routeStart>) (<http://dbpedia.org/ontology/usingCountry>) (<http://dbpedia.org/ontology/managerClub>) (<http://dbpedia.org/ontology/manager>) (<http://dbpedia.org/ontology/causeOfDeath>) (<http://dbpedia.org/ontology/fastestDriverCountry>) (<http://dbpedia.org/ontology/sovereignCountry>) (<http://dbpedia.org/ontology/broadcastArea>) (<http://dbpedia.org/ontology/firstDriverCountry>) (<http://dbpedia.org/ontology/highestState>) (<http://dbpedia.org/ontology/sourceState>) (<http://dbpedia.org/ontology/areaRural>) (<http://dbpedia.org/ontology/birthPlace>) (<http://dbpedia.org/ontology/lawCountry>) (<http://dbpedia.org/ontology/PopulatedPlace/areaTotal>) (<http://dbpedia.org/ontology/areaRank>) (<http://dbpedia.org/ontology/startWct>) (<http://dbpedia.org/ontology/governmentCountry>) (<http://dbpedia.org/ontology/isCityState>) (<http://dbpedia.org/ontology/twinCountry>)} VALUES (?property_1) {(<http://dbpedia.org/ontology/GeopoliticalOrganisation/areaMetro>) (<http://dbpedia.org/ontology/continent>) (<http://dbpedia.org/ontology/sourceConfluenceCountry>) (<http://dbpedia.org/ontology/actScore>) (<http://dbpedia.org/ontology/deathPlace>) (<http://dbpedia.org/ontology/mouthState>) (<http://dbpedia.org/ontology/areaQuote>) (<http://dbpedia.org/ontology/areaOfSearch>) (<http://dbpedia.org/ontology/startOccupation>) (<http://dbpedia.org/ontology/startYear>) (<http://dbpedia.org/ontology/isPartOf>) (<http://dbpedia.org/ontology/floorArea>) (<http://dbpedia.org/ontology/start>) (<http://dbpedia.org/ontology/serviceStartDate>) (<http://dbpedia.org/ontology/secondDriverCountry>) (<http://dbpedia.org/ontology/urbanArea>) (<http://dbpedia.org/ontology/lowestState>) (<http://dbpedia.org/ontology/workArea>) (<http://dbpedia.org/ontology/part>) (<http://dbpedia.org/ontology/land>) (<http://dbpedia.org/ontology/managementCountry>) (<http://dbpedia.org/ontology/wholeArea>) (<http://dbpedia.org/ontology/areaMetro>) (<http://dbpedia.org/ontology/thirdDriverCountry>) (<http://dbpedia.org/ontology/dateAct>) (<http://dbpedia.org/ontology/Planet/surfaceArea>) (<http://dbpedia.org/ontology/locationCountry>) (<http://dbpedia.org/ontology/landtag>) (<http://dbpedia.org/ontology/mouthCountry>) (<http://dbpedia.org/ontology/areaWater>) (<http://dbpedia.org/ontology/country>) (<http://dbpedia.org/ontology/area>) (<http://dbpedia.org/ontology/federalState>) (<http://dbpedia.org/ontology/locatedInArea>) (<http://dbpedia.org/resource/start_up>) (<http://dbpedia.org/ontology/stateOfOrigin>) (<http://dbpedia.org/ontology/Building/floorArea>) (<http://dbpedia.org/ontology/sportCountry>) (<http://dbpedia.org/ontology/hraState>) (<http://dbpedia.org/ontology/PopulatedPlace/area>) (<http://dbpedia.org/ontology/countryRank>) (<http://dbpedia.org/ontology/serviceStartYear>) (<http://dbpedia.org/ontology/areaUrban>) (<http://dbpedia.org/ontology/capitalCountry>) (<http://dbpedia.org/ontology/startDateTime>) (<http://dbpedia.org/ontology/areaDate>) (<http://dbpedia.org/ontology/state>) (<http://dbpedia.org/ontology/deathCause>) (<http://dbpedia.org/ontology/startCareer>) (<http://dbpedia.org/ontology/countryOrigin>) (<http://dbpedia.org/ontology/PopulatedPlace/areaMetro>) (<http://dbpedia.org/ontology/startDate>) (<http://dbpedia.org/ontology/ccaState>) (<http://dbpedia.org/ontology/surfaceArea>) (<http://dbpedia.org/ontology/dressCode>) (<http://dbpedia.org/ontology/Galaxy/surfaceArea>) (<http://dbpedia.org/ontology/alpsMainPart>) (<http://dbpedia.org/ontology/landArea>) (<http://dbpedia.org/ontology/startYearOfSales>) (<http://dbpedia.org/ontology/wingArea>) (<http://dbpedia.org/ontology/rankArea>) (<http://dbpedia.org/resource/get_going>) (<http://dbpedia.org/ontology/startWqs>) (<http://dbpedia.org/ontology/filmFareAward>) (<http://dbpedia.org/ontology/PopulatedPlace/areaUrban>) (<http://dbpedia.org/ontology/areaTotal>) (<http://dbpedia.org/ontology/startReign>) (<http://dbpedia.org/ontology/areaCode>) (<http://dbpedia.org/ontology/principalArea>) (<http://dbpedia.org/ontology/poleDriverCountry>) (<http://dbpedia.org/ontology/areaLand>) (<http://dbpedia.org/ontology/startPoint>) (<http://dbpedia.org/ontology/waterArea>) (<http://dbpedia.org/ontology/fareZone>) (<http://dbpedia.org/ontology/maximumArea>) (<http://dbpedia.org/ontology/sourceCountry>) (<http://dbpedia.org/resource/start_out>) (<http://dbpedia.org/ontology/nation>) (<http://dbpedia.org/ontology/stateDelegate>) (<http://dbpedia.org/ontology/routeStart>) (<http://dbpedia.org/ontology/usingCountry>) (<http://dbpedia.org/ontology/managerClub>) (<http://dbpedia.org/ontology/manager>) (<http://dbpedia.org/ontology/causeOfDeath>) (<http://dbpedia.org/ontology/fastestDriverCountry>) (<http://dbpedia.org/ontology/sovereignCountry>) (<http://dbpedia.org/ontology/broadcastArea>) (<http://dbpedia.org/ontology/firstDriverCountry>) (<http://dbpedia.org/ontology/highestState>) (<http://dbpedia.org/ontology/sourceState>) (<http://dbpedia.org/ontology/areaRural>) (<http://dbpedia.org/ontology/birthPlace>) (<http://dbpedia.org/ontology/lawCountry>) (<http://dbpedia.org/ontology/PopulatedPlace/areaTotal>) (<http://dbpedia.org/ontology/areaRank>) (<http://dbpedia.org/ontology/startWct>) (<http://dbpedia.org/ontology/governmentCountry>) (<http://dbpedia.org/ontology/isCityState>) (<http://dbpedia.org/ontology/twinCountry>)} VALUES (?property_2) {(<http://dbpedia.org/ontology/GeopoliticalOrganisation/areaMetro>) (<http://dbpedia.org/ontology/continent>) (<http://dbpedia.org/ontology/sourceConfluenceCountry>) (<http://dbpedia.org/ontology/actScore>) (<http://dbpedia.org/ontology/deathPlace>) (<http://dbpedia.org/ontology/mouthState>) (<http://dbpedia.org/ontology/areaQuote>) (<http://dbpedia.org/ontology/areaOfSearch>) (<http://dbpedia.org/ontology/startOccupation>) (<http://dbpedia.org/ontology/startYear>) (<http://dbpedia.org/ontology/isPartOf>) (<http://dbpedia.org/ontology/floorArea>) (<http://dbpedia.org/ontology/start>) (<http://dbpedia.org/ontology/serviceStartDate>) (<http://dbpedia.org/ontology/secondDriverCountry>) (<http://dbpedia.org/ontology/urbanArea>) (<http://dbpedia.org/ontology/lowestState>) (<http://dbpedia.org/ontology/workArea>) (<http://dbpedia.org/ontology/part>) (<http://dbpedia.org/ontology/land>) (<http://dbpedia.org/ontology/managementCountry>) (<http://dbpedia.org/ontology/wholeArea>) (<http://dbpedia.org/ontology/areaMetro>) (<http://dbpedia.org/ontology/thirdDriverCountry>) (<http://dbpedia.org/ontology/dateAct>) (<http://dbpedia.org/ontology/Planet/surfaceArea>) (<http://dbpedia.org/ontology/locationCountry>) (<http://dbpedia.org/ontology/landtag>) (<http://dbpedia.org/ontology/mouthCountry>) (<http://dbpedia.org/ontology/areaWater>) (<http://dbpedia.org/ontology/country>) (<http://dbpedia.org/ontology/area>) (<http://dbpedia.org/ontology/federalState>) (<http://dbpedia.org/ontology/locatedInArea>) (<http://dbpedia.org/resource/start_up>) (<http://dbpedia.org/ontology/stateOfOrigin>) (<http://dbpedia.org/ontology/Building/floorArea>) (<http://dbpedia.org/ontology/sportCountry>) (<http://dbpedia.org/ontology/hraState>) (<http://dbpedia.org/ontology/PopulatedPlace/area>) (<http://dbpedia.org/ontology/countryRank>) (<http://dbpedia.org/ontology/serviceStartYear>) (<http://dbpedia.org/ontology/areaUrban>) (<http://dbpedia.org/ontology/capitalCountry>) (<http://dbpedia.org/ontology/startDateTime>) (<http://dbpedia.org/ontology/areaDate>) (<http://dbpedia.org/ontology/state>) (<http://dbpedia.org/ontology/deathCause>) (<http://dbpedia.org/ontology/startCareer>) (<http://dbpedia.org/ontology/countryOrigin>) (<http://dbpedia.org/ontology/PopulatedPlace/areaMetro>) (<http://dbpedia.org/ontology/startDate>) (<http://dbpedia.org/ontology/ccaState>) (<http://dbpedia.org/ontology/surfaceArea>) (<http://dbpedia.org/ontology/dressCode>) (<http://dbpedia.org/ontology/Galaxy/surfaceArea>) (<http://dbpedia.org/ontology/alpsMainPart>) (<http://dbpedia.org/ontology/landArea>) (<http://dbpedia.org/ontology/startYearOfSales>) (<http://dbpedia.org/ontology/wingArea>) (<http://dbpedia.org/ontology/rankArea>) (<http://dbpedia.org/resource/get_going>) (<http://dbpedia.org/ontology/startWqs>) (<http://dbpedia.org/ontology/filmFareAward>) (<http://dbpedia.org/ontology/PopulatedPlace/areaUrban>) (<http://dbpedia.org/ontology/areaTotal>) (<http://dbpedia.org/ontology/startReign>) (<http://dbpedia.org/ontology/areaCode>) (<http://dbpedia.org/ontology/principalArea>) (<http://dbpedia.org/ontology/poleDriverCountry>) (<http://dbpedia.org/ontology/areaLand>) (<http://dbpedia.org/ontology/startPoint>) (<http://dbpedia.org/ontology/waterArea>) (<http://dbpedia.org/ontology/fareZone>) (<http://dbpedia.org/ontology/maximumArea>) (<http://dbpedia.org/ontology/sourceCountry>) (<http://dbpedia.org/resource/start_out>) (<http://dbpedia.org/ontology/nation>) (<http://dbpedia.org/ontology/stateDelegate>) (<http://dbpedia.org/ontology/routeStart>) (<http://dbpedia.org/ontology/usingCountry>) (<http://dbpedia.org/ontology/managerClub>) (<http://dbpedia.org/ontology/manager>) (<http://dbpedia.org/ontology/causeOfDeath>) (<http://dbpedia.org/ontology/fastestDriverCountry>) (<http://dbpedia.org/ontology/sovereignCountry>) (<http://dbpedia.org/ontology/broadcastArea>) (<http://dbpedia.org/ontology/firstDriverCountry>) (<http://dbpedia.org/ontology/highestState>) (<http://dbpedia.org/ontology/sourceState>) (<http://dbpedia.org/ontology/areaRural>) (<http://dbpedia.org/ontology/birthPlace>) (<http://dbpedia.org/ontology/lawCountry>) (<http://dbpedia.org/ontology/PopulatedPlace/areaTotal>) (<http://dbpedia.org/ontology/areaRank>) (<http://dbpedia.org/ontology/startWct>) (<http://dbpedia.org/ontology/governmentCountry>) (<http://dbpedia.org/ontology/isCityState>) (<http://dbpedia.org/ontology/twinCountry>)} FILTER (CONCAT( ?class_0, ?property_2, ?x ) != CONCAT( <http://some.url>, ?property_2, ?x ))  FILTER (CONCAT( ?class_0, ?property_2, ?x ) != CONCAT( <http://foo.bar.com>, ?property_2, ?x ))  FILTER (CONCAT( <http://some.url>, ?property_2, ?x ) != CONCAT( ?class_0, ?property_2, ?x ))  FILTER (CONCAT( <http://some.url>, ?property_2, ?x ) != CONCAT( <http://foo.bar.com>, ?property_2, ?x ))  FILTER (CONCAT( <http://foo.bar.com>, ?property_2, ?x ) != CONCAT( ?class_0, ?property_2, ?x ))  FILTER (CONCAT( <http://foo.bar.com>, ?property_2, ?x ) != CONCAT( <http://some.url>, ?property_2, ?x )) } ");

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
    @Ignore
    //TODO Implement the recognition of multi-word entities like "http://dbpedia.org/resource/Game_of_Thrones
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
    @Ignore
    //TODO Implement the recognition of multi-word entities like "http://dbpedia.org/resource/Game_of_Thrones
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
    @Ignore
    //TODO Implement
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
        expectedQueries.add("SELECT DISTINCT ?num WHERE { ?class_0 ?property_0 ?num .  VALUES (?class_0) {(<http://dbpedia.org/resource/San_Pedro_de_Atacama>)} VALUES (?property_0) {(<http://dbpedia.org/property/san>) (<http://dbpedia.org/ontology/cost>) (<http://dbpedia.org/property/be>) (<http://dbpedia.org/ontology/deathPlace>) (<http://dbpedia.org/ontology/timeZone>) (<http://dbpedia.org/property/pedro>) (<http://dbpedia.org/ontology/birthPlace>) (<http://dbpedia.org/ontology/ruling>) (<http://dbpedia.org/ontology/unitCost>) (<http://dbpedia.org/property/timeZone>) (<http://dbpedia.org/property/timezone>)}}");

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
        expectedQueries.add("SELECT DISTINCT ?uri WHERE { ?uri ?property_0 ?class_0 . ?uri ?property_1 ?class_1 . ?uri ?property_2 ?class_2 .  VALUES (?class_0) {(<http://dbpedia.org/ontology/Play>) (<http://dbpedia.org/resource/Richard_Gere>) (<http://dbpedia.org/ontology/Case>) (<http://dbpedia.org/ontology/MovieGenre>) (<http://dbpedia.org/ontology/Artwork>) (<http://dbpedia.org/resource/Julia_Roberts>) (<http://dbpedia.org/ontology/UnitOfWork>) (<http://dbpedia.org/ontology/Film>) (<http://dbpedia.org/ontology/BoardGame>) (<http://dbpedia.org/ontology/Castle>) (<http://dbpedia.org/ontology/Work>) (<http://dbpedia.org/ontology/MovieDirector>)} VALUES (?class_1) {(<http://dbpedia.org/ontology/Play>) (<http://dbpedia.org/resource/Richard_Gere>) (<http://dbpedia.org/ontology/Case>) (<http://dbpedia.org/ontology/MovieGenre>) (<http://dbpedia.org/ontology/Artwork>) (<http://dbpedia.org/resource/Julia_Roberts>) (<http://dbpedia.org/ontology/UnitOfWork>) (<http://dbpedia.org/ontology/Film>) (<http://dbpedia.org/ontology/BoardGame>) (<http://dbpedia.org/ontology/Castle>) (<http://dbpedia.org/ontology/Work>) (<http://dbpedia.org/ontology/MovieDirector>)} VALUES (?class_2) {(<http://dbpedia.org/ontology/Play>) (<http://dbpedia.org/resource/Richard_Gere>) (<http://dbpedia.org/ontology/Case>) (<http://dbpedia.org/ontology/MovieGenre>) (<http://dbpedia.org/ontology/Artwork>) (<http://dbpedia.org/resource/Julia_Roberts>) (<http://dbpedia.org/ontology/UnitOfWork>) (<http://dbpedia.org/ontology/Film>) (<http://dbpedia.org/ontology/BoardGame>) (<http://dbpedia.org/ontology/Castle>) (<http://dbpedia.org/ontology/Work>) (<http://dbpedia.org/ontology/MovieDirector>)} VALUES (?property_0) {(<http://dbpedia.org/ontology/filmNumber>) (<http://dbpedia.org/ontology/dressCode>) (<http://dbpedia.org/ontology/pictureFormat>) (<http://dbpedia.org/ontology/picture>) (<http://dbpedia.org/ontology/filmVersion>) (<http://dbpedia.org/ontology/actScore>) (<http://dbpedia.org/ontology/playRole>) (<http://dbpedia.org/ontology/filmAudioType>) (<http://dbpedia.org/ontology/managerClub>) (<http://dbpedia.org/ontology/voice>) (<http://dbpedia.org/ontology/manager>) (<http://dbpedia.org/ontology/movie>) (<http://dbpedia.org/property/film>) (<http://dbpedia.org/ontology/causeOfDeath>) (<http://dbpedia.org/ontology/film>) (<http://dbpedia.org/ontology/filmRuntime>) (<http://dbpedia.org/property/play>) (<http://dbpedia.org/ontology/award>) (<http://dbpedia.org/ontology/filmFareAward>) (<http://dbpedia.org/ontology/workArea>) (<http://dbpedia.org/ontology/work>) (<http://dbpedia.org/ontology/deathCause>) (<http://dbpedia.org/ontology/debutWork>) (<http://dbpedia.org/property/richard>) (<http://dbpedia.org/ontology/fareZone>) (<http://dbpedia.org/ontology/dateAct>) (<http://dbpedia.org/ontology/numberOfFilms>) (<http://dbpedia.org/ontology/pictureDescription>)} VALUES (?property_1) {(<http://dbpedia.org/ontology/filmNumber>) (<http://dbpedia.org/ontology/dressCode>) (<http://dbpedia.org/ontology/pictureFormat>) (<http://dbpedia.org/ontology/picture>) (<http://dbpedia.org/ontology/filmVersion>) (<http://dbpedia.org/ontology/actScore>) (<http://dbpedia.org/ontology/playRole>) (<http://dbpedia.org/ontology/filmAudioType>) (<http://dbpedia.org/ontology/managerClub>) (<http://dbpedia.org/ontology/voice>) (<http://dbpedia.org/ontology/manager>) (<http://dbpedia.org/ontology/movie>) (<http://dbpedia.org/property/film>) (<http://dbpedia.org/ontology/causeOfDeath>) (<http://dbpedia.org/ontology/film>) (<http://dbpedia.org/ontology/filmRuntime>) (<http://dbpedia.org/property/play>) (<http://dbpedia.org/ontology/award>) (<http://dbpedia.org/ontology/filmFareAward>) (<http://dbpedia.org/ontology/workArea>) (<http://dbpedia.org/ontology/work>) (<http://dbpedia.org/ontology/deathCause>) (<http://dbpedia.org/ontology/debutWork>) (<http://dbpedia.org/property/richard>) (<http://dbpedia.org/ontology/fareZone>) (<http://dbpedia.org/ontology/dateAct>) (<http://dbpedia.org/ontology/numberOfFilms>) (<http://dbpedia.org/ontology/pictureDescription>)} VALUES (?property_2) {(<http://dbpedia.org/ontology/filmNumber>) (<http://dbpedia.org/ontology/dressCode>) (<http://dbpedia.org/ontology/pictureFormat>) (<http://dbpedia.org/ontology/picture>) (<http://dbpedia.org/ontology/filmVersion>) (<http://dbpedia.org/ontology/actScore>) (<http://dbpedia.org/ontology/playRole>) (<http://dbpedia.org/ontology/filmAudioType>) (<http://dbpedia.org/ontology/managerClub>) (<http://dbpedia.org/ontology/voice>) (<http://dbpedia.org/ontology/manager>) (<http://dbpedia.org/ontology/movie>) (<http://dbpedia.org/property/film>) (<http://dbpedia.org/ontology/causeOfDeath>) (<http://dbpedia.org/ontology/film>) (<http://dbpedia.org/ontology/filmRuntime>) (<http://dbpedia.org/property/play>) (<http://dbpedia.org/ontology/award>) (<http://dbpedia.org/ontology/filmFareAward>) (<http://dbpedia.org/ontology/workArea>) (<http://dbpedia.org/ontology/work>) (<http://dbpedia.org/ontology/deathCause>) (<http://dbpedia.org/ontology/debutWork>) (<http://dbpedia.org/property/richard>) (<http://dbpedia.org/ontology/fareZone>) (<http://dbpedia.org/ontology/dateAct>) (<http://dbpedia.org/ontology/numberOfFilms>) (<http://dbpedia.org/ontology/pictureDescription>)} FILTER (CONCAT( ?uri, ?property_0, ?class_0 ) != CONCAT( ?uri, ?property_1, ?class_1 ))  FILTER (CONCAT( ?uri, ?property_0, ?class_0 ) != CONCAT( ?uri, ?property_2, ?class_2 ))  FILTER (CONCAT( ?uri, ?property_1, ?class_1 ) != CONCAT( ?uri, ?property_0, ?class_0 ))  FILTER (CONCAT( ?uri, ?property_1, ?class_1 ) != CONCAT( ?uri, ?property_2, ?class_2 ))  FILTER (CONCAT( ?uri, ?property_2, ?class_2 ) != CONCAT( ?uri, ?property_0, ?class_0 ))  FILTER (CONCAT( ?uri, ?property_2, ?class_2 ) != CONCAT( ?uri, ?property_1, ?class_1 )) }");

        List<String> actualQueries = queryMappingFactory.generateQueries(mappings, graph, new ArrayList<>());

        assertEquals(expectedQueries, actualQueries);
    }

    @Test
    public void testGenerateQueriesWithFilterInQueryTemplate() throws Exception {
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
        expectedQueries.add("SELECT DISTINCT ?uri WHERE { ?uri ?property_0 ?class_0 . ?uri ?property_1 ?class_1 . ?uri ?property_2 ?inhabitants .  VALUES (?class_0) {(<http://dbpedia.org/ontology/City>) (<http://dbpedia.org/ontology/Openswarm>) (<http://dbpedia.org/ontology/VideoGame>) (<http://dbpedia.org/ontology/Street>) (<http://dbpedia.org/resource/New_Jersey>)} VALUES (?class_1) {(<http://dbpedia.org/ontology/City>) (<http://dbpedia.org/ontology/Openswarm>) (<http://dbpedia.org/ontology/VideoGame>) (<http://dbpedia.org/ontology/Street>) (<http://dbpedia.org/resource/New_Jersey>)} VALUES (?class_2) {(<http://dbpedia.org/ontology/City>) (<http://dbpedia.org/ontology/Openswarm>) (<http://dbpedia.org/ontology/VideoGame>) (<http://dbpedia.org/ontology/Street>) (<http://dbpedia.org/resource/New_Jersey>)} VALUES (?property_0) {(<http://dbpedia.org/ontology/locationCity>) (<http://dbpedia.org/ontology/cityRank>) (<http://dbpedia.org/ontology/highestBreak>) (<http://dbpedia.org/ontology/presentMunicipality>) (<http://dbpedia.org/ontology/presentName>) (<http://dbpedia.org/property/city>) (<http://dbpedia.org/ontology/founder>) (<http://dbpedia.org/ontology/cityLink>) (<http://dbpedia.org/ontology/citySince>) (<http://dbpedia.org/datatype/hand>) (<http://dbpedia.org/ontology/city>) (<http://dbpedia.org/property/new>) (<http://dbpedia.org/ontology/cityType>) (<http://dbpedia.org/ontology/currentCity>) (<http://dbpedia.org/ontology/hand>) (<http://dbpedia.org/ontology/worldOpen>) (<http://dbpedia.org/property/jersey>) (<http://dbpedia.org/ontology/ruling>) (<http://dbpedia.org/ontology/foundedBy>)} VALUES (?property_1) {(<http://dbpedia.org/ontology/locationCity>) (<http://dbpedia.org/ontology/cityRank>) (<http://dbpedia.org/ontology/highestBreak>) (<http://dbpedia.org/ontology/presentMunicipality>) (<http://dbpedia.org/ontology/presentName>) (<http://dbpedia.org/property/city>) (<http://dbpedia.org/ontology/founder>) (<http://dbpedia.org/ontology/cityLink>) (<http://dbpedia.org/ontology/citySince>) (<http://dbpedia.org/datatype/hand>) (<http://dbpedia.org/ontology/city>) (<http://dbpedia.org/property/new>) (<http://dbpedia.org/ontology/cityType>) (<http://dbpedia.org/ontology/currentCity>) (<http://dbpedia.org/ontology/hand>) (<http://dbpedia.org/ontology/worldOpen>) (<http://dbpedia.org/property/jersey>) (<http://dbpedia.org/ontology/ruling>) (<http://dbpedia.org/ontology/foundedBy>)} VALUES (?property_2) {(<http://dbpedia.org/ontology/locationCity>) (<http://dbpedia.org/ontology/cityRank>) (<http://dbpedia.org/ontology/highestBreak>) (<http://dbpedia.org/ontology/presentMunicipality>) (<http://dbpedia.org/ontology/presentName>) (<http://dbpedia.org/property/city>) (<http://dbpedia.org/ontology/founder>) (<http://dbpedia.org/ontology/cityLink>) (<http://dbpedia.org/ontology/citySince>) (<http://dbpedia.org/datatype/hand>) (<http://dbpedia.org/ontology/city>) (<http://dbpedia.org/property/new>) (<http://dbpedia.org/ontology/cityType>) (<http://dbpedia.org/ontology/currentCity>) (<http://dbpedia.org/ontology/hand>) (<http://dbpedia.org/ontology/worldOpen>) (<http://dbpedia.org/property/jersey>) (<http://dbpedia.org/ontology/ruling>) (<http://dbpedia.org/ontology/foundedBy>)} FILTER (CONCAT( ?uri, ?property_0, ?class_0 ) != CONCAT( ?uri, ?property_1, ?class_1 ))  FILTER (CONCAT( ?uri, ?property_0, ?class_0 ) != CONCAT( ?uri, ?property_2, ?inhabitants ))  FILTER (CONCAT( ?uri, ?property_1, ?class_1 ) != CONCAT( ?uri, ?property_0, ?class_0 ))  FILTER (CONCAT( ?uri, ?property_1, ?class_1 ) != CONCAT( ?uri, ?property_2, ?inhabitants ))  FILTER (CONCAT( ?uri, ?property_2, ?inhabitants ) != CONCAT( ?uri, ?property_0, ?class_0 ))  FILTER (CONCAT( ?uri, ?property_2, ?inhabitants ) != CONCAT( ?uri, ?property_1, ?class_1 )) }");

        List<String> actualQueries = queryMappingFactory.generateQueries(mappings, graph, new ArrayList<>());

        assertEquals(expectedQueries, actualQueries);
    }

    @Test
    public void testGenerateQueriesWithOrderByInQueryTemplate() throws Exception {
        String graph = " {\"1\" @\"p\" \"2\"}";
        String query = "SELECT DISTINCT ?uri WHERE { ?uri a <http://dbpedia.org/ontology/Company> . ?uri <http://dbpedia.org/ontology/location> <http://dbpedia.org/resource/India> . ?uri <http://dbpedia.org/ontology/numberOfEmployees> ?n . }";
        String question = "Which Indian company has the most employees?";
        NTripleParser nTripleParser = new NTripleParser();
        List<RDFNode> nodes = Lists.newArrayList(nTripleParser.getNodes());
        List<String> properties = SPARQLUtilities.getDBpediaProperties();
        QueryMappingFactory queryMappingFactory = new QueryMappingFactory(question, query, nodes, properties);

        SemanticAnalysisHelper semanticAnalysisHelper = new SemanticAnalysisHelper();
        List<CustomQuestion> customQuestions = new ArrayList<>();
        customQuestions.add(new CustomQuestion("SELECT DISTINCT ?uri WHERE { ?uri a <http://dbpedia.org/ontology/Company> . ?uri <http://dbpedia.org/ontology/location> <http://dbpedia.org/resource/India> . ?uri <http://dbpedia.org/ontology/numberOfEmployees> ?n . }",
                "", null, graph, new HashMap<>()));
        List<String> dBpediaProperties = SPARQLUtilities.getDBpediaProperties();
        Map<String, QueryTemplateMapping> mappings = semanticAnalysisHelper.extractTemplates(customQuestions, newArrayList(nodes), dBpediaProperties);

        List<String> expectedQueries = new ArrayList<>();
        expectedQueries.add("SELECT DISTINCT ?uri WHERE { ?uri a ?class_0 . ?uri ?property_0 ?class_1 . ?uri ?property_1 ?n .  VALUES (?class_0) {(<http://dbpedia.org/resource/India>) (<http://dbpedia.org/ontology/Company>) (<http://dbpedia.org/ontology/Birth>) (<http://dbpedia.org/ontology/BusCompany>)} VALUES (?class_1) {(<http://dbpedia.org/resource/India>) (<http://dbpedia.org/ontology/Company>) (<http://dbpedia.org/ontology/Birth>) (<http://dbpedia.org/ontology/BusCompany>)} VALUES (?class_2) {(<http://dbpedia.org/resource/India>) (<http://dbpedia.org/ontology/Company>) (<http://dbpedia.org/ontology/Birth>) (<http://dbpedia.org/ontology/BusCompany>)} VALUES (?property_0) {(<http://dbpedia.org/ontology/deathPlace>) (<http://dbpedia.org/ontology/birthName>) (<http://dbpedia.org/ontology/leaderParty>) (<http://dbpedia.org/property/company>) (<http://dbpedia.org/ontology/birthDate>) (<http://dbpedia.org/ontology/causeOfDeath>) (<http://dbpedia.org/ontology/party>) (<http://dbpedia.org/ontology/company>) (<http://dbpedia.org/ontology/partyNumber>) (<http://dbpedia.org/ontology/birthPlace>) (<http://dbpedia.org/ontology/owningCompany>) (<http://dbpedia.org/ontology/deathCause>) (<http://dbpedia.org/ontology/feature>) (<http://dbpedia.org/ontology/otherParty>) (<http://dbpedia.org/ontology/mostWins>) (<http://dbpedia.org/ontology/designCompany>) (<http://dbpedia.org/ontology/owner>) (<http://dbpedia.org/ontology/mostDownPoint>) (<http://dbpedia.org/ontology/splitFromParty>) (<http://dbpedia.org/property/employee>) (<http://dbpedia.org/ontology/hasInput>) (<http://dbpedia.org/ontology/birthYear>) (<http://dbpedia.org/property/indian>) (<http://dbpedia.org/ontology/productionCompany>) (<http://dbpedia.org/ontology/distributingCompany>) (<http://dbpedia.org/ontology/parentCompany>) (<http://dbpedia.org/ontology/numberOfEmployees>) (<http://dbpedia.org/ontology/birthSign>)} VALUES (?property_1) {(<http://dbpedia.org/ontology/deathPlace>) (<http://dbpedia.org/ontology/birthName>) (<http://dbpedia.org/ontology/leaderParty>) (<http://dbpedia.org/property/company>) (<http://dbpedia.org/ontology/birthDate>) (<http://dbpedia.org/ontology/causeOfDeath>) (<http://dbpedia.org/ontology/party>) (<http://dbpedia.org/ontology/company>) (<http://dbpedia.org/ontology/partyNumber>) (<http://dbpedia.org/ontology/birthPlace>) (<http://dbpedia.org/ontology/owningCompany>) (<http://dbpedia.org/ontology/deathCause>) (<http://dbpedia.org/ontology/feature>) (<http://dbpedia.org/ontology/otherParty>) (<http://dbpedia.org/ontology/mostWins>) (<http://dbpedia.org/ontology/designCompany>) (<http://dbpedia.org/ontology/owner>) (<http://dbpedia.org/ontology/mostDownPoint>) (<http://dbpedia.org/ontology/splitFromParty>) (<http://dbpedia.org/property/employee>) (<http://dbpedia.org/ontology/hasInput>) (<http://dbpedia.org/ontology/birthYear>) (<http://dbpedia.org/property/indian>) (<http://dbpedia.org/ontology/productionCompany>) (<http://dbpedia.org/ontology/distributingCompany>) (<http://dbpedia.org/ontology/parentCompany>) (<http://dbpedia.org/ontology/numberOfEmployees>) (<http://dbpedia.org/ontology/birthSign>)} VALUES (?property_2) {(<http://dbpedia.org/ontology/deathPlace>) (<http://dbpedia.org/ontology/birthName>) (<http://dbpedia.org/ontology/leaderParty>) (<http://dbpedia.org/property/company>) (<http://dbpedia.org/ontology/birthDate>) (<http://dbpedia.org/ontology/causeOfDeath>) (<http://dbpedia.org/ontology/party>) (<http://dbpedia.org/ontology/company>) (<http://dbpedia.org/ontology/partyNumber>) (<http://dbpedia.org/ontology/birthPlace>) (<http://dbpedia.org/ontology/owningCompany>) (<http://dbpedia.org/ontology/deathCause>) (<http://dbpedia.org/ontology/feature>) (<http://dbpedia.org/ontology/otherParty>) (<http://dbpedia.org/ontology/mostWins>) (<http://dbpedia.org/ontology/designCompany>) (<http://dbpedia.org/ontology/owner>) (<http://dbpedia.org/ontology/mostDownPoint>) (<http://dbpedia.org/ontology/splitFromParty>) (<http://dbpedia.org/property/employee>) (<http://dbpedia.org/ontology/hasInput>) (<http://dbpedia.org/ontology/birthYear>) (<http://dbpedia.org/property/indian>) (<http://dbpedia.org/ontology/productionCompany>) (<http://dbpedia.org/ontology/distributingCompany>) (<http://dbpedia.org/ontology/parentCompany>) (<http://dbpedia.org/ontology/numberOfEmployees>) (<http://dbpedia.org/ontology/birthSign>)} FILTER (CONCAT( ?uri, 'a', ?class_0 ) != CONCAT( ?uri, ?property_0, ?class_1 ))  FILTER (CONCAT( ?uri, 'a', ?class_0 ) != CONCAT( ?uri, ?property_1, ?n ))  FILTER (CONCAT( ?uri, ?property_0, ?class_1 ) != CONCAT( ?uri, 'a', ?class_0 ))  FILTER (CONCAT( ?uri, ?property_0, ?class_1 ) != CONCAT( ?uri, ?property_1, ?n ))  FILTER (CONCAT( ?uri, ?property_1, ?n ) != CONCAT( ?uri, 'a', ?class_0 ))  FILTER (CONCAT( ?uri, ?property_1, ?n ) != CONCAT( ?uri, ?property_0, ?class_1 )) }");

        List<String> actualQueries = queryMappingFactory.generateQueries(mappings, graph, new ArrayList<>());

        assertEquals(expectedQueries, actualQueries);
    }

    @Test
    public void testGenerateQueriesStringLiteralInQuery() throws Exception {
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

        List<String> expectedQueries = new ArrayList<>();
        expectedQueries.add("SELECT DISTINCT ?uri WHERE { ?uri ?property_0 'President of the United States' . ?uri ?property_1 '16th' .  VALUES (?property_0) {(<http://dbpedia.org/ontology/presidentGeneralCouncil>) (<http://dbpedia.org/property/states>) (<http://dbpedia.org/ontology/chairperson>) (<http://dbpedia.org/property/be>) (<http://dbpedia.org/ontology/presidentGeneralCouncilMandate>) (<http://dbpedia.org/ontology/chairLabel>) (<http://dbpedia.org/ontology/deathPlace>) (<http://dbpedia.org/ontology/vicePresident>) (<http://dbpedia.org/ontology/birthPlace>) (<http://dbpedia.org/ontology/presidentRegionalCouncil>) (<http://dbpedia.org/ontology/cost>) (<http://dbpedia.org/ontology/chairman>) (<http://dbpedia.org/ontology/apcPresident>) (<http://dbpedia.org/ontology/chairmanTitle>) (<http://dbpedia.org/property/president>) (<http://dbpedia.org/ontology/unitCost>) (<http://dbpedia.org/ontology/president>)} VALUES (?property_1) {(<http://dbpedia.org/ontology/presidentGeneralCouncil>) (<http://dbpedia.org/property/states>) (<http://dbpedia.org/ontology/chairperson>) (<http://dbpedia.org/property/be>) (<http://dbpedia.org/ontology/presidentGeneralCouncilMandate>) (<http://dbpedia.org/ontology/chairLabel>) (<http://dbpedia.org/ontology/deathPlace>) (<http://dbpedia.org/ontology/vicePresident>) (<http://dbpedia.org/ontology/birthPlace>) (<http://dbpedia.org/ontology/presidentRegionalCouncil>) (<http://dbpedia.org/ontology/cost>) (<http://dbpedia.org/ontology/chairman>) (<http://dbpedia.org/ontology/apcPresident>) (<http://dbpedia.org/ontology/chairmanTitle>) (<http://dbpedia.org/property/president>) (<http://dbpedia.org/ontology/unitCost>) (<http://dbpedia.org/ontology/president>)} FILTER (CONCAT( ?uri, ?property_0, 'President of the United States' ) != CONCAT( ?uri, ?property_1, '16th' ))  FILTER (CONCAT( ?uri, ?property_1, '16th' ) != CONCAT( ?uri, ?property_0, 'President of the United States' )) }");

        List<String> actualQueries = queryMappingFactory.generateQueries(mappings, graph, new ArrayList<>());

        assertEquals(expectedQueries, actualQueries);
    }

    @Test
    public void testGenerateQueriesStringLiteralInQuery2() throws Exception {
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

        List<String> expectedQueries = new ArrayList<>();
        expectedQueries.add("ASK WHERE { ?uri ?property_0 ?class_0 . ?uri ?property_1 'Battle Chess'@en .  VALUES (?class_0) {(<http://dbpedia.org/ontology/TelevisionSeason>) (<http://dbpedia.org/ontology/MobilePhone>) (<http://dbpedia.org/ontology/ChessPlayer>) (<http://dbpedia.org/resource/Battle_Chess>) (<http://dbpedia.org/ontology/MilitaryConflict>) (<http://dbpedia.org/ontology/CardGame>) (<http://dbpedia.org/ontology/TelevisionDirector>) (<http://dbpedia.org/ontology/BoardGame>) (<http://dbpedia.org/resource/Video_game>) (<http://dbpedia.org/ontology/GivenName>) (<http://dbpedia.org/ontology/TelevisionStation>) (<http://dbpedia.org/ontology/Surname>) (<http://dbpedia.org/ontology/TelevisionHost>) (<http://dbpedia.org/ontology/ProtectedArea>) (<http://dbpedia.org/ontology/TelevisionPersonality>) (<http://dbpedia.org/ontology/Game>) (<http://dbpedia.org/ontology/Name>) (<http://dbpedia.org/ontology/VideoGame>) (<http://dbpedia.org/ontology/TelevisionEpisode>) (<http://dbpedia.org/ontology/TelevisionShow>)} VALUES (?class_1) {(<http://dbpedia.org/ontology/TelevisionSeason>) (<http://dbpedia.org/ontology/MobilePhone>) (<http://dbpedia.org/ontology/ChessPlayer>) (<http://dbpedia.org/resource/Battle_Chess>) (<http://dbpedia.org/ontology/MilitaryConflict>) (<http://dbpedia.org/ontology/CardGame>) (<http://dbpedia.org/ontology/TelevisionDirector>) (<http://dbpedia.org/ontology/BoardGame>) (<http://dbpedia.org/resource/Video_game>) (<http://dbpedia.org/ontology/GivenName>) (<http://dbpedia.org/ontology/TelevisionStation>) (<http://dbpedia.org/ontology/Surname>) (<http://dbpedia.org/ontology/TelevisionHost>) (<http://dbpedia.org/ontology/ProtectedArea>) (<http://dbpedia.org/ontology/TelevisionPersonality>) (<http://dbpedia.org/ontology/Game>) (<http://dbpedia.org/ontology/Name>) (<http://dbpedia.org/ontology/VideoGame>) (<http://dbpedia.org/ontology/TelevisionEpisode>) (<http://dbpedia.org/ontology/TelevisionShow>)} VALUES (?property_0) {(<http://dbpedia.org/ontology/otherName>) (<http://dbpedia.org/ontology/pictureFormat>) (<http://dbpedia.org/ontology/phonePrefix>) (<http://dbpedia.org/ontology/ngcName>) (<http://dbpedia.org/ontology/formerName>) (<http://dbpedia.org/ontology/addressInRoad>) (<http://dbpedia.org/ontology/battleHonours>) (<http://dbpedia.org/ontology/latinName>) (<http://dbpedia.org/ontology/cost>) (<http://dbpedia.org/ontology/colonialName>) (<http://dbpedia.org/ontology/teamName>) (<http://dbpedia.org/ontology/iupacName>) (<http://dbpedia.org/resource/video_game>) (<http://dbpedia.org/property/be>) (<http://dbpedia.org/ontology/leaderName>) (<http://dbpedia.org/ontology/peopleName>) (<http://dbpedia.org/ontology/conflict>) (<http://dbpedia.org/ontology/sameName>) (<http://dbpedia.org/ontology/areaCode>) (<http://dbpedia.org/ontology/oldName>) (<http://dbpedia.org/ontology/originalName>) (<http://dbpedia.org/ontology/policeName>) (<http://dbpedia.org/property/video>) (<http://dbpedia.org/ontology/gameModus>) (<http://dbpedia.org/ontology/commonName>) (<http://dbpedia.org/ontology/address>) (<http://dbpedia.org/ontology/battle>) (<http://dbpedia.org/ontology/picture>) (<http://dbpedia.org/ontology/nameDay>) (<http://dbpedia.org/property/game>) (<http://dbpedia.org/ontology/meshName>) (<http://dbpedia.org/ontology/birthName>) (<http://dbpedia.org/ontology/amateurFight>) (<http://dbpedia.org/ontology/fight>) (<http://dbpedia.org/ontology/name>) (<http://dbpedia.org/ontology/televisionSeries>) (<http://dbpedia.org/ontology/map>) (<http://dbpedia.org/ontology/reignName>) (<http://dbpedia.org/ontology/firstGame>) (<http://dbpedia.org/ontology/phonePrefixLabel>) (<http://dbpedia.org/property/battle>) (<http://dbpedia.org/ontology/presentName>) (<http://dbpedia.org/ontology/gameEngine>) (<http://dbpedia.org/ontology/statName>) (<http://dbpedia.org/ontology/greekName>) (<http://dbpedia.org/ontology/spouseName>) (<http://dbpedia.org/ontology/signName>) (<http://dbpedia.org/ontology/callSign>) (<http://dbpedia.org/property/call>) (<http://dbpedia.org/ontology/gameArtist>) (<http://dbpedia.org/ontology/unitCost>) (<http://dbpedia.org/ontology/pictureDescription>) (<http://dbpedia.org/ontology/colourName>)} VALUES (?property_1) {(<http://dbpedia.org/ontology/otherName>) (<http://dbpedia.org/ontology/pictureFormat>) (<http://dbpedia.org/ontology/phonePrefix>) (<http://dbpedia.org/ontology/ngcName>) (<http://dbpedia.org/ontology/formerName>) (<http://dbpedia.org/ontology/addressInRoad>) (<http://dbpedia.org/ontology/battleHonours>) (<http://dbpedia.org/ontology/latinName>) (<http://dbpedia.org/ontology/cost>) (<http://dbpedia.org/ontology/colonialName>) (<http://dbpedia.org/ontology/teamName>) (<http://dbpedia.org/ontology/iupacName>) (<http://dbpedia.org/resource/video_game>) (<http://dbpedia.org/property/be>) (<http://dbpedia.org/ontology/leaderName>) (<http://dbpedia.org/ontology/peopleName>) (<http://dbpedia.org/ontology/conflict>) (<http://dbpedia.org/ontology/sameName>) (<http://dbpedia.org/ontology/areaCode>) (<http://dbpedia.org/ontology/oldName>) (<http://dbpedia.org/ontology/originalName>) (<http://dbpedia.org/ontology/policeName>) (<http://dbpedia.org/property/video>) (<http://dbpedia.org/ontology/gameModus>) (<http://dbpedia.org/ontology/commonName>) (<http://dbpedia.org/ontology/address>) (<http://dbpedia.org/ontology/battle>) (<http://dbpedia.org/ontology/picture>) (<http://dbpedia.org/ontology/nameDay>) (<http://dbpedia.org/property/game>) (<http://dbpedia.org/ontology/meshName>) (<http://dbpedia.org/ontology/birthName>) (<http://dbpedia.org/ontology/amateurFight>) (<http://dbpedia.org/ontology/fight>) (<http://dbpedia.org/ontology/name>) (<http://dbpedia.org/ontology/televisionSeries>) (<http://dbpedia.org/ontology/map>) (<http://dbpedia.org/ontology/reignName>) (<http://dbpedia.org/ontology/firstGame>) (<http://dbpedia.org/ontology/phonePrefixLabel>) (<http://dbpedia.org/property/battle>) (<http://dbpedia.org/ontology/presentName>) (<http://dbpedia.org/ontology/gameEngine>) (<http://dbpedia.org/ontology/statName>) (<http://dbpedia.org/ontology/greekName>) (<http://dbpedia.org/ontology/spouseName>) (<http://dbpedia.org/ontology/signName>) (<http://dbpedia.org/ontology/callSign>) (<http://dbpedia.org/property/call>) (<http://dbpedia.org/ontology/gameArtist>) (<http://dbpedia.org/ontology/unitCost>) (<http://dbpedia.org/ontology/pictureDescription>) (<http://dbpedia.org/ontology/colourName>)} FILTER (CONCAT( ?uri, ?property_0, ?class_0 ) != CONCAT( ?uri, ?property_1, 'Battle Chess'@en ))  FILTER (CONCAT( ?uri, ?property_1, 'Battle Chess'@en ) != CONCAT( ?uri, ?property_0, ?class_0 )) }");

        List<String> actualQueries = queryMappingFactory.generateQueries(mappings, graph, new ArrayList<>());

        assertEquals(expectedQueries, actualQueries);
    }
}

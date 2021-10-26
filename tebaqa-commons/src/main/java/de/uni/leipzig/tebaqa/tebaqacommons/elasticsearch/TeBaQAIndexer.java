package de.uni.leipzig.tebaqa.tebaqacommons.elasticsearch;

import com.google.common.collect.Lists;
import de.uni.leipzig.tebaqa.tebaqacommons.model.ESConnectionProperties;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.ntriples.NTriplesParser;
import org.openrdf.rio.turtle.TurtleParser;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class TeBaQAIndexer {

    private static final Logger LOGGER = LogManager.getLogger(TeBaQAIndexer.class);
    private static final String DEFAULT_PROPERTIES = "src/main/resources/indexing.properties";

    public enum InputSource {
        URL,
        FILE
    }

    // Constants
    private static final String TTL = "ttl";
    private static final String NT = "nt";
    private static final String OWL = "owl";
    private static final String BZ2 = ".bz2";

    public static final String TYPE_PREDICATE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type".toLowerCase();
    public static final List<String> CLASS_TYPES = Lists.newArrayList(
            "http://www.w3.org/2002/07/owl#Class".toLowerCase(),
            "http://www.w3.org/2000/01/rdf-schema#Class".toLowerCase());
    public static final List<String> PROPERTY_TYPES = Lists.newArrayList(
            "http://www.w3.org/2002/07/owl#ObjectProperty".toLowerCase(),
            "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property".toLowerCase(),
            "http://www.w3.org/2002/07/owl#DatatypeProperty".toLowerCase()
    );
    public static final List<String> LABEL_PREDICATES = Lists.newArrayList(
            "http://www.w3.org/2004/02/skos/core#prefLabel".toLowerCase(),
            "http://www.w3.org/2000/01/rdf-schema#label".toLowerCase(),
            "http://schema.org/name".toLowerCase(),
            "http://www.w3.org/2004/02/skos/core#altLabel".toLowerCase(),
            "http://xmlns.com/foaf/0.1/name".toLowerCase(),
            "http://dbpedia.org/ontology/abbreviation".toLowerCase(),
            "http://dbpedia.org/ontology/demonym".toLowerCase(),
            "http://xmlns.com/foaf/0.1/nick".toLowerCase(),
            "http://dbpedia.org/ontology/synonym".toLowerCase(),
            //"http://xmlns.com/foaf/0.1/givenName".toLowerCase()
            "http://xmlns.com/foaf/0.1/surname".toLowerCase(),
            "http://dbpedia.org/ontology/alternativeName".toLowerCase()
    );

    private final ESConnectionProperties esConnectionProperties;

    public TeBaQAIndexer(ESConnectionProperties esProps) {
        this.esConnectionProperties = esProps;
    }

    public static void main(String[] args) {
        String indexingProperties = DEFAULT_PROPERTIES;
        if (args.length > 0) {
            LOGGER.info("Property file specified as argument: " + args[0]);
            if (Files.notExists(Paths.get(args[0]))) {
                LOGGER.error("Specified property file does not exist or cannot be opened.. exiting!");
                return;
            }
            indexingProperties = args[0];
        }
        try {
            Properties prop = new Properties();
            InputStream input = new FileInputStream(indexingProperties);
            prop.load(input);

            LOGGER.info(prop);

            ESConnectionProperties esProps = new ESConnectionProperties(prop.getProperty("target.host.scheme"),
                    prop.getProperty("target.host.name"), prop.getProperty("target.host.port"),
                    prop.getProperty("target.index.entity.name"), prop.getProperty("target.index.class.name"),
                    prop.getProperty("target.index.property.name"), prop.getProperty("target.index.literal.name"));

            TeBaQAIndexer runner = new TeBaQAIndexer(esProps);

            boolean indexOntologyFlag = "true".equalsIgnoreCase(prop.getProperty("index.ontology.flag"));
            if (indexOntologyFlag) {
                String propertyIndex = esProps.getPropertyIndex();
                LOGGER.info("The Property index will be here: " + propertyIndex);

                String classIndex = esProps.getClassIndex();
                LOGGER.info("The resource index will be here: " + classIndex);

                List<String> ontologyFiles = new ArrayList<>();
                String sourceOntologyFolder = prop.getProperty("source.ontology.folder");
                for (File file : Objects.requireNonNull(new File(sourceOntologyFolder).listFiles())) {
                    if (file.getName().endsWith("ttl") || file.getName().endsWith("nt") || file.getName().endsWith("owl")) {
                        ontologyFiles.add(file.getAbsolutePath());
                    }
                }
                LOGGER.info("Reading " + ontologyFiles.size() + " ontology files from " + sourceOntologyFolder);

                // Index ontology
                runner.indexOntologyFiles(ontologyFiles);
            }

            boolean indexDataFlag = "true".equalsIgnoreCase(prop.getProperty("index.data.flag"));
            if (indexDataFlag) {
                String entityIndex = esProps.getEntityIndex();
                LOGGER.info("The resource index will be here: " + entityIndex);

                List<String> dataFiles = new ArrayList<>();
                String sourceDataFolder = prop.getProperty("source.data.folder");

                for (File file : Objects.requireNonNull(new File(sourceDataFolder).listFiles())) {
                    if (file.getName().endsWith("bz2") || file.getName().endsWith("ttl") || file.getName().endsWith("nt") || file.getName().endsWith("owl")) {
                        dataFiles.add(file.getAbsolutePath());
                    }
                }
                LOGGER.info("Reading " + dataFiles.size() + " data files from " + sourceDataFolder);

                String excludedPredicatesFile = prop.getProperty("source.data.exclude.file");
                Set<String> excludedPredicates;
                if (excludedPredicatesFile != null && !excludedPredicatesFile.isEmpty() && Files.exists(Paths.get(excludedPredicatesFile))) {
                    BufferedReader bufferedReader = new BufferedReader(new FileReader(excludedPredicatesFile));
                    String line = bufferedReader.readLine();

                    excludedPredicates = new HashSet<>();
                    while (line != null) {
                        excludedPredicates.add(line.toLowerCase()); // Add in lowercase for comparison later
                        line = bufferedReader.readLine();
                    }

                } else {
                    LOGGER.error("Predicate exclusion file does not exist or cannot be opened: " + excludedPredicatesFile);
                    excludedPredicates = Collections.emptySet();
                }

                runner.indexDataFilesOrFailSilently(dataFiles, excludedPredicates);
            }

        } catch (IOException e) {
            LOGGER.error("Error while creating index. Maybe the index is corrupt now.", e);
        }
    }

    public void indexOntologyURLs(List<String> ontologyFileUrls) {
        indexOntologyInputs(ontologyFileUrls, InputSource.URL);
    }

    public void indexOntologyFiles(List<String> filePaths) {
        indexOntologyInputs(filePaths, InputSource.FILE);
    }

    private void indexOntologyInputs(List<String> inputs, InputSource inputSource) {

        try {
            ElasticSearchIndexer indexer = new ElasticSearchIndexer(esConnectionProperties);
            indexer.createOntologyIndexes();
            indexer.createBulkProcessor();

            Set<String> foundClasses = new HashSet<>();
            Set<String> foundPredicates = new HashSet<>();
            Map<String, List<String>> subjectUriToLabels = new HashMap<>();

            // Find classes and properties from each vocabulary file
            for (String input : inputs) {

                if (InputSource.URL.equals(inputSource)) {
                    URL urlObj = new URL(input);
                    populateClassesAndProperties(urlObj.openStream(), urlObj.getFile(), foundClasses, foundPredicates, subjectUriToLabels);
                } else if (InputSource.FILE.equals(inputSource)) {
                    File fileObj = new File(input);
                    populateClassesAndProperties(new FileInputStream(fileObj.getAbsolutePath()), fileObj.getName(), foundClasses, foundPredicates, subjectUriToLabels);
                }

            }

            // Index classes and properties found above
            for (String cl : foundClasses) {
                if (subjectUriToLabels.containsKey(cl))
                    indexer.indexClass(cl, subjectUriToLabels.get(cl));
            }
            for (String cl : foundPredicates) {
                if (subjectUriToLabels.containsKey(cl))
                    indexer.indexProperty(cl, subjectUriToLabels.get(cl));
            }

            indexer.commit();
            indexer.close();

        } catch (Exception e) {
            LOGGER.error("Error while creating ontology indexes: " + e.getMessage());
            LOGGER.error(e);
        }
    }

    private void populateClassesAndProperties(InputStream inputStream, String inputName, Set<String> foundClasses, Set<String> foundPredicates, Map<String, List<String>> subjectUriToLabels) throws FileNotFoundException {
        LOGGER.info("Start parsing: " + inputName);

        Lang lang;
        if (inputName.endsWith(TTL)) {
            lang = Lang.TTL;
        } else {
            lang = Lang.NTRIPLES;
        }

        Iterator<Triple> tripleIterator = RDFDataMgr.createIteratorTriples(inputStream, lang, "");
        tripleIterator.forEachRemaining(triple -> {
            String subject = triple.getSubject().toString();
            String predicate = triple.getPredicate().toString();
            String object = triple.getObject().toString();

            if (CLASS_TYPES.contains(object.toLowerCase()))
                foundClasses.add(subject);
            else if (PROPERTY_TYPES.contains(object.toLowerCase()))
                foundPredicates.add(subject);

            if (LABEL_PREDICATES.contains(predicate)) {
//                Literal l = (Literal) triple.getObject().getLiteralValue();
                String label = triple.getObject().getLiteral().getLexicalForm();
//                String lang = l.getLanguage();
//                if (lang != null && lang.equals("de")) {
                if (!subjectUriToLabels.containsKey(subject))
                    subjectUriToLabels.put(subject, new ArrayList<>());
                subjectUriToLabels.get(subject).add(label);

//                }
            }
        });

        LOGGER.info("Finished parsing: " + inputName);
    }

    public void indexDataFilesOrFailSilently(List<String> dataFiles, Set<String> excludedPredicates) {
        ElasticSearchIndexer indexer = null;
        try {
            indexer = new ElasticSearchIndexer(esConnectionProperties);
            indexer.createEntityIndex();
            indexer.createBulkProcessor();

            for (String url : dataFiles) {
                try {
                    indexDataInput(indexer, url, InputSource.FILE, excludedPredicates);
                } catch (Exception e) {
                    LOGGER.error("Failed to index input: " + url, e);
                }
            }
            indexer.commit();
            indexer.close();
        } catch (Exception e) {
            LOGGER.error("Error while creating TripleIndex.", e);
        } finally {
            if (indexer != null) {
                indexer.commit();
                indexer.close();
            }
        }
    }

    public void indexDataFilesOrThrowException(List<String> dataFiles, Set<String> excludedPredicates) throws Exception {
        ElasticSearchIndexer indexer = null;
        try {
            indexer = new ElasticSearchIndexer(esConnectionProperties);
            indexer.createEntityIndex();
            indexer.createBulkProcessor();

            for (String url : dataFiles) {
                try {
                    indexDataInput(indexer, url, InputSource.FILE, excludedPredicates);
                } catch (Exception e) {
                    LOGGER.error("Failed to index input: " + url, e);
                    throw e;
                }
            }

        } catch (Exception e) {
            LOGGER.error("Error while creating TripleIndex.", e);
            throw e;
        } finally {
            if (indexer != null) {
                indexer.commit();
                indexer.close();
            }
        }
    }

    public void indexDataURLsOrFailSilently(List<String> dataFileUrls, Set<String> excludedPredicates) {
        ElasticSearchIndexer indexer = null;
        try {
            indexer = new ElasticSearchIndexer(esConnectionProperties);
            indexer.createEntityIndex();
            indexer.createBulkProcessor();

            for (String url : dataFileUrls) {
                try {
                    indexDataInput(indexer, url, InputSource.URL, excludedPredicates);
                } catch (Exception e) {
                    LOGGER.error("Failed to index input: " + url, e);
                }
            }
            indexer.commit();
            indexer.close();
        } catch (Exception e) {
            LOGGER.error("Error while creating TripleIndex.", e);
        } finally {
            if (indexer != null) {
                indexer.commit();
                indexer.close();
            }
        }
    }

    public void indexDataURLsOrThrowException(List<String> dataFileUrls, Set<String> excludedPredicates) throws Exception {
        ElasticSearchIndexer indexer = null;
        try {
            indexer = new ElasticSearchIndexer(esConnectionProperties);

            indexer.createEntityIndex();
            indexer.createBulkProcessor();

            for (String url : dataFileUrls) {
                try {
                    indexDataInput(indexer, url, InputSource.URL, excludedPredicates);
                } catch (Exception e) {
                    LOGGER.error("Error while creating TripleIndex.", e);
                    throw e;
                }
            }

        } catch (Exception e) {
            LOGGER.error("Error while creating TripleIndex.", e);
            throw e;
        } finally {
            if (indexer != null) {
                indexer.commit();
                indexer.close();
            }
        }
    }

    private void indexDataInput(ElasticSearchIndexer indexer, String input, InputSource inputSource, Collection<String> excludedPredicates) throws Exception {
        if (InputSource.FILE.equals(inputSource)) {
            indexDataInput(indexer, new FileInputStream(input), input, excludedPredicates);
        } else if (InputSource.URL.equals(inputSource)) {
            URL url = new URL(input);
            String fileName = url.getFile();
            indexDataInput(indexer, url.openStream(), fileName, excludedPredicates);
        }
    }

    private void indexDataInput(ElasticSearchIndexer indexer, InputStream inputStream, String inputName, Collection<String> excludedPredicates) throws RDFParseException, RDFHandlerException, IOException {
        LOGGER.info("Start parsing: " + inputName);

        try {
            // Set the parser based on the triple language used in file
            RDFParser parser;
            if (inputName.endsWith(TTL) || inputName.endsWith(TTL + ".bz2")) {
                parser = new TurtleParser();
            } else {
                parser = new NTriplesParser();
            }
            TripleStatementHandler statementHandler = new TripleStatementHandler(indexer, excludedPredicates);
            parser.setRDFHandler(statementHandler);
            parser.setStopAtFirstError(false);


            // Set the input stream based on file type
//            if (fileName.endsWith(".bz2")) {
//                parser.parse(new BZip2CompressorInputStream(url.openStream()), "");
//            } else {
//                parser.parse(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8), "");
//            }
            if (inputName.endsWith(".bz2")) {
                parser.parse(new BZip2CompressorInputStream(inputStream), "");
            } else {
                parser.parse(new InputStreamReader(inputStream, StandardCharsets.UTF_8), "");
            }

            // Flush out remaining bulk
            statementHandler.index();
        } catch (RDFParseException | MalformedURLException e) {
            LOGGER.error("Failed parsing (Malformed RDF data): " + inputName);
        }

        LOGGER.info("Finished parsing: " + inputName);
    }


//    private static SearchResponse queryIndex(QueryBuilder queryBuilder, int maxNumberOfResults, String indexName) throws IOException {
//        SearchRequest searchRequest = new SearchRequest();
//        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
//        searchSourceBuilder.query(queryBuilder);
//        TopHitsAggregationBuilder aggregationBuilder = AggregationBuilders.topHits("top").size(maxNumberOfResults);
//        searchSourceBuilder.aggregation(aggregationBuilder);
//
//        searchSourceBuilder.size(maxNumberOfResults);
//        searchRequest.source(searchSourceBuilder);
//        searchRequest.indices(indexName);
//        return client.search(searchRequest, RequestOptions.DEFAULT);
//    }

//    public static Set<String> readClasses() {
//        Charset charset = StandardCharsets.UTF_8;
//        Set<String> uris = new HashSet<>(74000);
//        try (BufferedReader reader = new BufferedReader(Files.newBufferedReader(Paths.get("all_classes_wiki.txt"), charset))) {
//
//            String classUri = reader.readLine();
//            while (classUri != null) {
//                uris.add(classUri);
//                classUri = reader.readLine();
//            }
//
//        } catch (IOException x) {
//            System.err.format("IOException: %s%n", x);
//        }
//
//        System.out.println("read classes: " + uris.size());
//        return uris;
//    }
//
//    public static void writeClasses() {
//        Charset charset = StandardCharsets.UTF_8;
//        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get("all_classes_wiki.txt"), charset))) {
//            for (String content : allClassUris) {
//                writer.println(content);
//            }
//        } catch (IOException x) {
//            System.err.format("IOException: %s%n", x);
//        }
//    }
//
//    private static void readTriplesAndIndex() throws IOException {
//        final Scroll scroll = new Scroll(TimeValue.timeValueMinutes(30L));
//        SearchRequest searchRequest = new SearchRequest(wikidata_triples_index);
//        searchRequest.scroll(scroll);
//        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
//        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
//        searchSourceBuilder.size(100000);
//        searchSourceBuilder.sort("_doc");
//
//        searchRequest.source(searchSourceBuilder);
//
//        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
//        String scrollId = searchResponse.getScrollId();
//        SearchHit[] searchHits = searchResponse.getHits().getHits();
//
//        System.out.println("Took: " + searchResponse.getTook().getMillis());
//
//        int readDocs = 0;
//        while (searchHits != null && searchHits.length > 0) {
//            readDocs += searchHits.length;
//            if (readDocs % 5000000 == 0)
//                System.out.println("Took: " + searchResponse.getTook().getMillis());
//
//
//            wikidataResourceIndexing(searchHits);
//
//            SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
//            scrollRequest.scroll(scroll);
//
//            try {
//                searchResponse = getFromScroll(scrollRequest);
//
//                scrollId = searchResponse.getScrollId();
//                searchHits = searchResponse.getHits().getHits();
//            } catch (Exception e) {
//                e.printStackTrace();
//
//                System.out.println("RETRYING ...");
//                try {
//                    searchResponse = getFromScroll(scrollRequest);
//
//                    scrollId = searchResponse.getScrollId();
//                    searchHits = searchResponse.getHits().getHits();
//                } catch (Exception e1) {
//                    e1.printStackTrace();
//                    searchHits = null;
//                }
//            }
//        }
//
//        ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
//        clearScrollRequest.addScrollId(scrollId);
//        ClearScrollResponse clearScrollResponse = client.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
//        boolean succeeded = clearScrollResponse.isSucceeded();
//        System.out.println(succeeded);
//    }
//
//    private static SearchResponse getFromScroll(SearchScrollRequest scrollRequest) throws IOException {
//        return client.scroll(scrollRequest, RequestOptions.DEFAULT);
//    }
//

}

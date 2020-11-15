package de.uni.leipzig.tebaqa.tebaqacommons.elasticsearch;

import de.uni.leipzig.tebaqa.tebaqacommons.model.ClassCandidate;
import de.uni.leipzig.tebaqa.tebaqacommons.model.ESConnectionProperties;
import de.uni.leipzig.tebaqa.tebaqacommons.model.EntityCandidate;
import de.uni.leipzig.tebaqa.tebaqacommons.model.PropertyCandidate;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHits;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHitsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.util.*;

import static org.elasticsearch.index.query.QueryBuilders.termQuery;

public class ElasticSearchClient {

    private static final int DEFAULT_MAX_RESULT_SIZE = 100;
    private static final String DOC_ID = "_id";
    private static final String TYPE = "type";
    private static final String LABEL = "label";
    private static final String URI = "uri";
    private static final String SYNONYMS = "synonyms";
    private static final String CONNECTED_RESOURCE_SUBJECT = "connected_resource_subject";
    private static final String CONNECTED_RESOURCE_OBJECT = "connected_resource_object";
    private static final String CONNECTED_PROPERTY_SUBJECT = "connected_property_subject";
    private static final String CONNECTED_PROPERTY_OBJECT = "connected_property_object";
    private String entityIndex;
    private String propertyIndex;
    private String classIndex;

    private RestHighLevelClient client;

    public ElasticSearchClient(ESConnectionProperties prop) throws IOException {

//        Properties prop = new Properties();
//        InputStream input = null;
//        input = new FileInputStream("src/main/resources/entityLinking.properties");
//        prop.load(input);
        String envHost = System.getenv("Elasticsearch_host");
        String elhost = envHost != null ? envHost : prop.getHostname();
        String envPort = System.getenv("Elasticsearch_host");
        int elport = Integer.parseInt(envPort != null ? envPort : prop.getPort());
        String envScheme = System.getenv("Elasticsearch_host");
        String scheme = envScheme != null ? envScheme : prop.getScheme();
        client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(elhost, elport, scheme)));
        String envDefaultIndex = System.getenv("Elasticsearch_host");
        this.entityIndex = envDefaultIndex != null ? envDefaultIndex : prop.getEntityIndex();
        this.propertyIndex = envDefaultIndex != null ? envDefaultIndex : prop.getPropertyIndex();
        this.classIndex = envDefaultIndex != null ? envDefaultIndex : prop.getClassIndex();
    }

    public Set<EntityCandidate> searchEntity(Optional<String> coOccurrence, Optional<String> connectedEntity, Optional<String> connectedProperty, Optional<String> type) throws IOException {
        return searchEntity(coOccurrence, connectedEntity, connectedProperty, type, DEFAULT_MAX_RESULT_SIZE);
    }

    public Set<EntityCandidate> searchEntity(Optional<String> coOccurrence, Optional<String> connectedEntity, Optional<String> connectedProperty, Optional<String> type, int maxNumberOfResults) throws IOException {
        Set<EntityCandidate> candidates;
        BoolQueryBuilder rootQueryBuilder = new BoolQueryBuilder();

        // 1. Co-occurrence
        coOccurrence.ifPresent(coOccurrenceVal -> {
            QueryBuilder coOccurrenceMatchFuzzy = new MatchQueryBuilder(LABEL + ".full", coOccurrenceVal).operator(Operator.AND).fuzziness(2).prefixLength(0).maxExpansions(50).fuzzyTranspositions(true);
            QueryBuilder coOccurrenceMatchTerm = new MatchQueryBuilder(LABEL + ".raw", coOccurrenceVal).operator(Operator.AND);
            QueryBuilder coOccurrenceTermFuzzy = QueryBuilders.fuzzyQuery(LABEL + ".raw", coOccurrenceVal).fuzziness(Fuzziness.TWO).prefixLength(0).maxExpansions(50);

            BoolQueryBuilder coOccurrenceCombinedQueries = new BoolQueryBuilder();
            if (coOccurrenceVal.contains(" ")) coOccurrenceCombinedQueries.should(coOccurrenceMatchFuzzy.boost(1));
            coOccurrenceCombinedQueries.should(coOccurrenceTermFuzzy.boost(2));
            coOccurrenceCombinedQueries.should(coOccurrenceMatchTerm.boost(3));
            rootQueryBuilder.must(coOccurrenceCombinedQueries);
        });

        // 2. Do not find isolated entities i.e. entities not connected with any other entities through properties
        ExistsQueryBuilder subjectPropertyExists = new ExistsQueryBuilder(CONNECTED_PROPERTY_SUBJECT);
        ExistsQueryBuilder objectPropertyExists = new ExistsQueryBuilder(CONNECTED_PROPERTY_OBJECT);

        BoolQueryBuilder propertyExistsCombined = new BoolQueryBuilder();
        propertyExistsCombined.should(subjectPropertyExists);
        propertyExistsCombined.should(objectPropertyExists);
        rootQueryBuilder.must(propertyExistsCombined);

        // 3. Connected property filter
        connectedProperty.ifPresent(prop -> {
            BoolQueryBuilder propertyBoolQuery = new BoolQueryBuilder();
            QueryBuilder q1 = termQuery(CONNECTED_PROPERTY_SUBJECT, prop);
            QueryBuilder q2 = termQuery(CONNECTED_PROPERTY_OBJECT, prop);
            propertyBoolQuery.should(q1);
            propertyBoolQuery.should(q2);
            rootQueryBuilder.must(propertyBoolQuery);
        });

        // 4. Connected entity filter
        connectedEntity.ifPresent(res -> {
            BoolQueryBuilder resourceBoolQuery = new BoolQueryBuilder();
            QueryBuilder q1 = termQuery(CONNECTED_RESOURCE_SUBJECT, res);
            QueryBuilder q2 = termQuery(CONNECTED_RESOURCE_OBJECT, res);
            resourceBoolQuery.should(q1);
            resourceBoolQuery.should(q2);
            rootQueryBuilder.must(resourceBoolQuery);
        });

        // 5. Type filter
        type.ifPresent(tp -> {
            QueryBuilder q = termQuery(TYPE, tp);
            rootQueryBuilder.must(q);
        });

        candidates = queryEntityIndex(rootQueryBuilder, maxNumberOfResults);
        coOccurrence.ifPresent(s -> candidates.forEach(entityCandidate -> entityCandidate.setCoOccurrenceAndScore(s)));
        return candidates;
    }

    public Set<EntityCandidate> searchEntitiesByIds(Collection<String> entityUris) throws IOException {
        BoolQueryBuilder queryBuilder = new BoolQueryBuilder();
        for (String uri : entityUris) {
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder(DOC_ID, uri);
            queryBuilder.should(termQueryBuilder);
        }

        return queryEntityIndex(queryBuilder, DEFAULT_MAX_RESULT_SIZE);
    }


    private Set<EntityCandidate> queryEntityIndex(QueryBuilder queryBuilder, int maxNumberOfResults) throws IOException {
        SearchResponse searchResponse = this.queryIndex(queryBuilder, maxNumberOfResults, entityIndex);
        //SearchHit[] hits = searchResponse.getHits().getHits();
        TopHits topHits = searchResponse.getAggregations().get("top");
        Set<EntityCandidate> candidates = new HashSet<>();
        String name;
        Set<String> connectedResourcesSubject, connectedResourcesObject, connectedPropertiesSubject, connectedPropertiesObject, types, label;
        for (SearchHit hit : topHits.getHits()) {
            Map<String, Object> sources = hit.getSourceAsMap();
            name = hit.getId();
            label = prepareSetFromSource(sources.get(LABEL));
            connectedPropertiesSubject = prepareSetFromSource(sources.get(CONNECTED_PROPERTY_SUBJECT));
            connectedPropertiesObject = prepareSetFromSource(sources.get(CONNECTED_PROPERTY_OBJECT));
            connectedResourcesSubject = prepareSetFromSource(sources.get(CONNECTED_RESOURCE_SUBJECT));
            connectedResourcesObject = prepareSetFromSource(sources.get(CONNECTED_RESOURCE_OBJECT));
            types = prepareSetFromSource(sources.get(TYPE));
            /*if(types.contains("http://dbpedia.org/ontology/Person")){
                HashSet<String>surnames=new HashSet<>();
                for(String l:label) {
                    if (l.contains(" ")&&!l.contains("(")&&!WordsGenerator.containsAnyStopword(l,"en")) {
                        String[] parts = l.split(" ");
                        for (int i = parts.length-1; i > 0; i--) {
                            String n="";
                            for(int j=i;j<parts.length;j++)
                                n+=parts[j]+" ";
                            surnames.add(n);
                        }
                    }
                }
                label.addAll(surnames);
            }*/
            EntityCandidate candidate = new EntityCandidate(name, label, connectedPropertiesSubject, connectedPropertiesObject, connectedResourcesSubject, connectedResourcesObject, types);
            candidates.add(candidate);
        }

        return candidates;
    }

    /*
    Common method which queries a given index as per the QueryBuilder instance
    * */
    private SearchResponse queryIndex(QueryBuilder queryBuilder, int maxNumberOfResults, String indexName) throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(queryBuilder);
        TopHitsAggregationBuilder aggregationBuilder = AggregationBuilders.topHits("top").size(maxNumberOfResults);
        searchSourceBuilder.aggregation(aggregationBuilder);

        searchSourceBuilder.size(maxNumberOfResults);
        searchRequest.source(searchSourceBuilder);
        searchRequest.indices(indexName);
        return client.search(searchRequest, RequestOptions.DEFAULT);
    }


    private Set<String> prepareSetFromSource(Object source) {
        Set<String> target = new HashSet<>();
        if (source instanceof Collection)
            target.addAll((ArrayList<String>) source);
        else if (source instanceof String)
            target.add((String) source);
        return target;
    }

    public Set<PropertyCandidate> searchProperty(String coOccurrence, boolean searchSynonyms) throws IOException {
        return this.searchProperty(coOccurrence, DEFAULT_MAX_RESULT_SIZE, searchSynonyms);
    }

    public Set<PropertyCandidate> searchProperty(String coOccurrence, int maxNumberOfResults, boolean searchSynonyms) throws IOException {
        QueryBuilder queryBuilder;

        MatchQueryBuilder m1 = new MatchQueryBuilder(LABEL, coOccurrence).operator(Operator.AND).fuzziness(Fuzziness.AUTO).prefixLength(0).maxExpansions(2).fuzzyTranspositions(true);
        if (searchSynonyms) {
            queryBuilder = new BoolQueryBuilder();
            MatchQueryBuilder m2 = new MatchQueryBuilder(SYNONYMS, coOccurrence).operator(Operator.AND).fuzziness(Fuzziness.AUTO).prefixLength(0).maxExpansions(2).fuzzyTranspositions(true);
            ((BoolQueryBuilder) queryBuilder).should(m1);
            ((BoolQueryBuilder) queryBuilder).should(m2);
        } else
            queryBuilder = m1;

        Set<PropertyCandidate> propertyCandidates = queryPropertyIndex(queryBuilder, maxNumberOfResults, searchSynonyms);
        propertyCandidates.forEach(propertyCandidate -> propertyCandidate.setCoOccurrenceAndScore(coOccurrence));
        return propertyCandidates;
    }

    public Set<PropertyCandidate> searchPropertiesByIds(Collection<String> propertyUris) throws IOException {
        BoolQueryBuilder queryBuilder = new BoolQueryBuilder();
        for (String uri : propertyUris) {
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder(URI, uri);
            queryBuilder.should(termQueryBuilder);
        }

        return queryPropertyIndex(queryBuilder, DEFAULT_MAX_RESULT_SIZE, true);
    }

    private Set<PropertyCandidate> queryPropertyIndex(QueryBuilder queryBuilder, int maxNumberOfResults, boolean searchSynonyms) throws IOException {
        SearchResponse searchResponse = this.queryIndex(queryBuilder, maxNumberOfResults, propertyIndex);

        TopHits topHits = searchResponse.getAggregations().get("top");
        Set<PropertyCandidate> candidates = new HashSet<>();

        for (SearchHit hit : topHits.getHits().getHits()) {
            Map<String, Object> sources = hit.getSourceAsMap();
            Object foundLabels = sources.get(LABEL);
            Set<String> labels = prepareSetFromSource(foundLabels);

            if (searchSynonyms) {
                labels.addAll(prepareSetFromSource(sources.get(SYNONYMS)));
            }
            candidates.add(new PropertyCandidate(sources.get(URI).toString(), labels));
        }

        return candidates;
    }

    public Set<ClassCandidate> searchClass(String coOccurrence) throws IOException {
        return this.searchClass(coOccurrence, DEFAULT_MAX_RESULT_SIZE);
    }

    public Set<ClassCandidate> searchClass(String coOccurrence, int maxNumberOfResults) throws IOException {
        QueryBuilder queryBuilder = new MatchQueryBuilder(LABEL, coOccurrence).operator(Operator.AND).fuzziness(Fuzziness.AUTO).prefixLength(0).maxExpansions(2).fuzzyTranspositions(true);
        Set<ClassCandidate> classCandidates = this.queryClassIndex(queryBuilder, maxNumberOfResults);
        classCandidates.forEach(classCandidate -> classCandidate.setCoOccurrenceAndScore(coOccurrence));
        return classCandidates;
    }

    private Set<ClassCandidate> queryClassIndex(QueryBuilder queryBuilder, int maxNumberOfResults) throws IOException {
        SearchResponse searchResponse = this.queryIndex(queryBuilder, maxNumberOfResults, classIndex);

        TopHits topHits = searchResponse.getAggregations().get("top");
        Set<ClassCandidate> classCandidates = new HashSet<>((int) topHits.getHits().totalHits);

        for (SearchHit hit : topHits.getHits().getHits()) {
            Map<String, Object> sources = hit.getSourceAsMap();
            Set<String> labels = prepareSetFromSource(sources.get(LABEL));
            classCandidates.add(new ClassCandidate(sources.get(URI).toString(), labels));
        }

        return classCandidates;
    }


//    public Set<ResourceCandidate> searchLiteral(String coOccurence,int maxNumberOfResults) {
//        BoolQueryBuilder booleanQueryBuilder = new BoolQueryBuilder();
//        Set<ResourceCandidate> candidates=new HashSet<>();
//        //QueryBuilder queryBuilderMatchFuzzy=new MatchQueryBuilder("literal"+".full", coOccurence).operator(Operator.AND).fuzziness(2).prefixLength(0).maxExpansions(50).fuzzyTranspositions(true);
//        //queryBuilder = new MatchQueryBuilder(LABEL, coOccurence).operator(Operator.AND);
//        //NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
//        //}
//        //else {
//        //if(coOccurence.length()<4)
//        //QueryBuilder queryBuilderMatchTerm= new MatchQueryBuilder("literal"+".raw", coOccurence).operator(Operator.AND);
////        QueryBuilder queryBuilderTermFuzzy=QueryBuilders.fuzzyQuery("literal"+".raw",coOccurence).fuzziness(Fuzziness.TWO).prefixLength(0).maxExpansions(50);
//        QueryBuilder queryBuilderTerm=QueryBuilders.termQuery("literal"+".raw",coOccurence);
//        //}
//
//        try {
//
//            candidates = getFromIndexLiteral(maxNumberOfResults, queryBuilderTerm);
//            //cache.put(bq, triples);
//
//        } catch (Exception e) {
//            //log.error(e.getLocalizedMessage() + " -> " + subject);
//            e.printStackTrace();
//        }
//        candidates.forEach(resourceCandidate -> resourceCandidate.setCoOccurence(coOccurence));
//        return candidates;
//    }
//    private Set<ResourceCandidate> getFromIndexLiteral(int maxNumberOfResults, QueryBuilder bq) throws IOException {
//        SearchRequest searchRequest = new SearchRequest();
//        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
//        searchSourceBuilder.query(bq);
//        TopHitsAggregationBuilder tl= AggregationBuilders.topHits("top").size(maxNumberOfResults);
//        searchSourceBuilder.aggregation(tl);
//
//        searchSourceBuilder.size(maxNumberOfResults);
//        searchRequest.source(searchSourceBuilder);
//        searchRequest.indices("limboliteralsfin");
//        //searchRequest.source().aggregation(AggregationBuilders.max("prominence").script(new Script("doc['"+CONNECTED_RESOURCE_SUBJECT+"'].values.length + doc['"+CONNECTED_RESOURCE_OBJECT+"'].values.length ")));
//        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
//        //SearchHit[] hits = searchResponse.getHits().getHits();
//        TopHits topHits = searchResponse.getAggregations().get("top");
//        Set<ResourceCandidate> candidates = new HashSet<>();
//        String name;
//        Set<String>connectedResourcesSubject,connectedResourcesObject,connectedPropertiesSubject,connectedPropertiesObject,types,label;
//        for (SearchHit hit : topHits.getHits()) {
//            Map<String, Object> sources = hit.getSourceAsMap();
//            name = sources.get("literal").toString();
//            label = new HashSet<>();
//            label.add(sources.get("literal").toString());
//
//            connectedPropertiesSubject=new HashSet<>();
//            connectedPropertiesObject=new HashSet<>();
//            connectedPropertiesObject.add(sources.get("property").toString());
//            connectedResourcesSubject=new HashSet<>();
//            connectedResourcesObject=new HashSet<>();
//            connectedResourcesObject.add(sources.get("subject").toString());
//            types=new HashSet<>();
//            types.add("literal");
//            ResourceCandidate candidate = new EntityCandidate(name,label,connectedPropertiesSubject,connectedPropertiesObject,connectedResourcesSubject,connectedResourcesObject,types);
//            candidates.add(candidate);
//        }
//
//        return candidates;
//    }

    public void close() throws IOException {
        client.close();
    }

}

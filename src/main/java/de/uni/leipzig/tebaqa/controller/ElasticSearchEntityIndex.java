package de.uni.leipzig.tebaqa.controller;

import com.google.common.collect.Lists;
import de.uni.leipzig.tebaqa.helper.FillTemplatePatternsWithResources;
import de.uni.leipzig.tebaqa.helper.WordsGenerator;
import de.uni.leipzig.tebaqa.model.*;
import meka.core.A;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.HttpHost;
import org.apache.jena.query.Query;
import org.apache.lucene.index.Terms;
import org.apache.stanbol.enhancer.engines.dbpspotlight.model.CandidateResource;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHits;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHitsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.ScriptSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortMode;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.*;

import static org.elasticsearch.index.query.QueryBuilders.*;

public class ElasticSearchEntityIndex {

    private UrlValidator urlValidator = new UrlValidator();
    private RestHighLevelClient client;
    final String TYPE="type";
    final String LABEL="label";
    final String CONNECTED_RESOURCE_SUBJECT="connected_resource_subject";
    final String CONNECTED_RESOURCE_OBJECT="connected_resource_object";
    final String CONNECTED_PROPERTY_SUBJECT="connected_property_subject";
    final String CONNECTED_PROPERTY_OBJECT="connected_property_object";
    private String entityIndex;
    private String propertyIndex;
    private String classIndex;

    public ElasticSearchEntityIndex()throws IOException {

        Properties prop = new Properties();
        InputStream input = null;
        input = new FileInputStream("src/main/resources/entityLinking.properties");
        prop.load(input);
        String envHost = System.getenv("Elasticsearch_host");
        String elhost = envHost != null ? envHost : prop.getProperty("el_hostname");
        String envPort = System.getenv("Elasticsearch_host");
        int elport =Integer.valueOf(envPort != null ? envPort : prop.getProperty("el_port"));
        String envScheme = System.getenv("Elasticsearch_host");
        String scheme =envScheme != null ? envScheme : prop.getProperty("scheme");
        client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(elhost, elport, scheme)));
        String envDefaultIndex = System.getenv("Elasticsearch_host");
        this.entityIndex =envDefaultIndex != null ? envDefaultIndex : prop.getProperty("resource_index");
        this.propertyIndex =envDefaultIndex != null ? envDefaultIndex : prop.getProperty("property_index");
        this.classIndex =envDefaultIndex != null ? envDefaultIndex : prop.getProperty("class_index");
    }

    public List<String[]> search(String coOcurrence,String index) {
        int defaultMaxNumberOfDocsRetrievedFromIndex = 100;
        return search(coOcurrence, defaultMaxNumberOfDocsRetrievedFromIndex,index);
    }

    public List<String[]> search(String coOcurrence, int maxNumberOfResults,String index) {
        QueryBuilder queryBuilder;
        if(coOcurrence.contains(" ")) {
            queryBuilder = new MatchQueryBuilder("label", coOcurrence).operator(Operator.AND).fuzziness(Fuzziness.AUTO).prefixLength(0).maxExpansions(2).fuzzyTranspositions(true);
            //NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
        }
        else
            queryBuilder=QueryBuilders.fuzzyQuery("label",coOcurrence).fuzziness(Fuzziness.AUTO).prefixLength(0).maxExpansions(2);

            List<String[]> resources=new ArrayList<>();
        try {
            resources = getFromIndex(maxNumberOfResults, queryBuilder,index);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        //cache.put(bq, triples);

        return resources;
    }


    private List<String[]> getFromIndex(int maxNumberOfResults, QueryBuilder bq,String index) throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(bq);
        searchSourceBuilder.size(maxNumberOfResults);
        searchRequest.source(searchSourceBuilder);
        searchRequest.indices(index);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHit[] hits = searchResponse.getHits().getHits();

        List<String[]> resources = new ArrayList<>();
        for (SearchHit hit : hits) {
            Map<String, Object> sources = hit.getSourceAsMap();
            resources.add(new String[]{sources.get("url").toString(),sources.get("label").toString()});
        }

        return resources;
    }
    public List<Triple> search(String subject, String predicate, String object) {
        int defaultMaxNumberOfDocsRetrievedFromIndex = 100;
        return search(subject, predicate, object, defaultMaxNumberOfDocsRetrievedFromIndex);
    }
    public List<Triple> search(String subject, String predicate, String object, int maxNumberOfResults) {
        BoolQueryBuilder booleanQueryBuilder = new BoolQueryBuilder();
        List<Triple> triples = new ArrayList<>();

        try {
            /*if (subject != null && subject.equals("http://aksw.org/notInWiki")) {

            }*/

            if (subject != null) {
                QueryBuilder q = termQuery("subject", subject);
                booleanQueryBuilder.must(q);
            }
            if (predicate != null) {
                QueryBuilder q = termQuery("predicate", predicate);
                booleanQueryBuilder.must(q);
            }
            if (object != null) {
                QueryBuilder q = termQuery("object_uri", object);
                booleanQueryBuilder.must(q);
            }
            else{
                QueryBuilder q=existsQuery("object_uri");
            }
            /*if (object != null && object.length() > 0) {
                QueryBuilder bq;
                if (urlValidator.isValid(object)) {

                    bq = termQuery("object_uri", object);
                    booleanQueryBuilder.must(bq);

                } else {
                    bq = matchQuery("object_literal",object).operator(Operator.AND);
                    booleanQueryBuilder.must(bq);

                }

            }*/
            triples = getFromIndexLinks(maxNumberOfResults, booleanQueryBuilder);
            //cache.put(bq, triples);

        } catch (Exception e) {
            //log.error(e.getLocalizedMessage() + " -> " + subject);
            e.printStackTrace();
        }
        return triples;
    }

    private List<Triple> getFromIndexLinks(int maxNumberOfResults, QueryBuilder bq) throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(bq);
        searchSourceBuilder.size(maxNumberOfResults);
        searchRequest.source(searchSourceBuilder);
        searchRequest.indices("trindexqa2");
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHit[] hits = searchResponse.getHits().getHits();

        List<Triple> triples = new ArrayList<>();
        String s, p, o;
        for (SearchHit hit : hits) {
            Map<String, Object> sources = hit.getSourceAsMap();
            s = sources.get("subject").toString();
            p = sources.get("predicate").toString();
            if (sources.containsKey("object_uri"))
                o = sources.get("object_uri").toString();
            else
                o = sources.get("object_literal").toString();
            Triple triple = new Triple(s, p, o);
            triples.add(triple);
        }

        return triples;
    }
    public Set<ResourceCandidate> searchResource(String coOcurrence,String type,boolean synonym) {
        int defaultMaxNumberOfDocsRetrievedFromIndex = 150;
        return searchResource(coOcurrence, defaultMaxNumberOfDocsRetrievedFromIndex,type,synonym);
    }

    public Set<ResourceCandidate> searchResource(String coOcurrence, int maxNumberOfResults,String type,boolean synonym) {
        QueryBuilder queryBuilder;




        //if(coOcurrence.contains(" ")) {
            MatchQueryBuilder m1=new MatchQueryBuilder(LABEL, coOcurrence).operator(Operator.AND).fuzziness(Fuzziness.AUTO).prefixLength(0).maxExpansions(2).fuzzyTranspositions(true);
            if(synonym) {
                queryBuilder = new BoolQueryBuilder();
                MatchQueryBuilder m2 = new MatchQueryBuilder("synonyms", coOcurrence).operator(Operator.AND).fuzziness(Fuzziness.AUTO).prefixLength(0).maxExpansions(2).fuzzyTranspositions(true);
                ((BoolQueryBuilder) queryBuilder).should(m1);
                ((BoolQueryBuilder) queryBuilder).should(m2);
            }
            else queryBuilder=m1;
        //}
        /*else {
            FuzzyQueryBuilder m1 = QueryBuilders.fuzzyQuery(LABEL, coOcurrence).fuzziness(Fuzziness.AUTO).prefixLength(0).maxExpansions(2);
            if(synonym) {
                queryBuilder = new BoolQueryBuilder();
                FuzzyQueryBuilder m2 = QueryBuilders.fuzzyQuery("synonyms", coOcurrence).fuzziness(Fuzziness.AUTO).prefixLength(0).maxExpansions(2);
                ((BoolQueryBuilder) queryBuilder).should(m1);
                ((BoolQueryBuilder) queryBuilder).should(m2);
            }
            else queryBuilder=m1;
        }*/

        Set<ResourceCandidate> resources=new HashSet<>();
        try {
            resources = getFromIndexRescource(maxNumberOfResults, queryBuilder,type,synonym);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        //cache.put(bq, triples);

        return resources;
    }
    public Set<ResourceCandidate> searchResourceBySynonym(String coOcurrence, int maxNumberOfResults,String type,boolean synonym) {
        QueryBuilder queryBuilder;
        String searchIn="synonyms";
        if(coOcurrence.contains(" ")) {
            queryBuilder = new MatchQueryBuilder(searchIn, coOcurrence).operator(Operator.AND).fuzziness(Fuzziness.AUTO).prefixLength(0).maxExpansions(2).fuzzyTranspositions(true);
            //NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
        }
        else
            queryBuilder=QueryBuilders.fuzzyQuery(searchIn,coOcurrence).fuzziness(Fuzziness.AUTO).prefixLength(0).maxExpansions(2);
            //queryBuilder=QueryBuilders.matchQuery(searchIn,coOcurrence);


        Set<ResourceCandidate> resources=new HashSet<>();
        try {
            resources = getFromIndexRescource(maxNumberOfResults, queryBuilder,type,synonym);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        //cache.put(bq, triples);

        return resources;
    }
    public Set<ResourceCandidate> searchPropertiesById(List<String> propertyUris) {
        BoolQueryBuilder queryBuilder;
        String searchIn=LABEL;
        queryBuilder=new BoolQueryBuilder();
        for(String uri:propertyUris){
            TermQueryBuilder termQueryBuilder=new TermQueryBuilder("uri",uri);
            queryBuilder.should(termQueryBuilder);
        }

        Set<ResourceCandidate> resources=new HashSet<>();
        try {
            resources = getFromIndexRescource(100, queryBuilder,"property",true);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        //cache.put(bq, triples);

        return resources;
    }
    public Set<ResourceCandidate> searchEntitiesById(List<String> resourceUris) {
        BoolQueryBuilder queryBuilder;
        String searchIn=LABEL;
        queryBuilder=new BoolQueryBuilder();
        for(String uri:resourceUris){
            TermQueryBuilder termQueryBuilder=new TermQueryBuilder("_id",uri);
            queryBuilder.should(termQueryBuilder);
        }

        Set<ResourceCandidate> resources=new HashSet<>();
        try {
            resources = getFromIndexEntity(100, queryBuilder);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        //cache.put(bq, triples);

        return resources;
    }


    private Set<ResourceCandidate> getFromIndexRescource(int maxNumberOfResults, QueryBuilder bq,String type,boolean searchSynonyms) throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        TopHitsAggregationBuilder tl= AggregationBuilders.topHits("top").size(100);
        searchSourceBuilder.aggregation(tl);
        searchSourceBuilder.query(bq);
        searchSourceBuilder.size(maxNumberOfResults);
        searchRequest.source(searchSourceBuilder);
        if(type.equals("property"))
            searchRequest.indices(propertyIndex);
        else searchRequest.indices(classIndex);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        //SearchHit[] hits = searchResponse.getHits().getHits();
        TopHits topHits = searchResponse.getAggregations().get("top");
        SearchHit[] hits = searchResponse.getHits().getHits();
        Set<ResourceCandidate> resources = new HashSet<>();
        for (SearchHit hit : topHits.getHits().getHits()) {
            Map<String, Object> sources = hit.getSourceAsMap();
            if(type.equals("property")) {
                Set<String>labels=new HashSet<>();
                Object foundLabels = sources.get(LABEL);
                if(foundLabels instanceof List)
                    labels.addAll((ArrayList<String>)foundLabels);
                else
                    labels.add(foundLabels.toString());
                if(searchSynonyms) {
                    ArrayList<String>syns=(ArrayList)sources.get("synonyms");
                    if(syns!=null)
                    labels.addAll(syns);
                    //labels.addAll((ArrayList)sources.get("synonyms"));
                }
                resources.add(new PropertyCandidate(sources.get("uri").toString(), labels));
            }
            else{
                Set<String>labels=new HashSet<>();
                labels.addAll((ArrayList)sources.get(LABEL));
                resources.add(new ClassCandidate(sources.get("uri").toString(),labels));
            }
        }

        return resources;
    }
    public Set<ResourceCandidate> searchEntity(String coOccurence, Optional<String> linkedResource,Optional<String>linkedProperty,Optional<String>type) {
        int defaultMaxNumberOfDocsRetrievedFromIndex = 100;
        return searchEntity(coOccurence, linkedResource, linkedProperty,type, defaultMaxNumberOfDocsRetrievedFromIndex);
    }
    public Set<ResourceCandidate> searchEntityWithTypeFilter(String coOccurence,String typeToFilter,int maxNumberOfResults) {
        BoolQueryBuilder booleanQueryBuilder = new BoolQueryBuilder();
        Set<ResourceCandidate> candidates=new HashSet<>();
        QueryBuilder queryBuilder;

        if(coOccurence.contains(" ")) {
            queryBuilder = new MatchQueryBuilder(LABEL, coOccurence).operator(Operator.AND).fuzziness(2).prefixLength(0).maxExpansions(50).fuzzyTranspositions(true);
            //NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
        }
        else{
            if(coOccurence.length()<4)
                queryBuilder = new MatchQueryBuilder(LABEL, coOccurence).operator(Operator.AND);
            else queryBuilder=QueryBuilders.fuzzyQuery(LABEL,coOccurence).fuzziness(Fuzziness.TWO).prefixLength(0).maxExpansions(50);
        }
        booleanQueryBuilder.must(queryBuilder);
        ExistsQueryBuilder property=new ExistsQueryBuilder(CONNECTED_PROPERTY_SUBJECT);
        ExistsQueryBuilder resource=new ExistsQueryBuilder(CONNECTED_PROPERTY_OBJECT);


        BoolQueryBuilder connect=new BoolQueryBuilder();
        connect.should(property);
        connect.should(resource);
        booleanQueryBuilder.must(connect);
        QueryBuilder q = termQuery(TYPE, typeToFilter);
        booleanQueryBuilder.mustNot(q);
        try {
            /*boolean stop=false;
            int i=0;
            while(!stop&&(candidates.size()>=100||candidates.size()==0)) {
                if (i > 0){
                    ScriptQueryBuilder sq = scriptQuery(
                            new Script("doc['" + CONNECTED_RESOURCE_SUBJECT + "'].values.length > " + i + " && doc['" + CONNECTED_RESOURCE_OBJECT + "'].values.length > " + i)
                    );
                booleanQueryBuilder.must(sq);
            }
                candidates = getFromIndexEntity(maxNumberOfResults, booleanQueryBuilder);
                if(i==0&&candidates.size()==0)stop=true;
                //else if(candidates.size()==0) i-=2;
                else i+=2;
            }*/
            //cache.put(bq, triples);
            candidates = getFromIndexEntity(maxNumberOfResults, booleanQueryBuilder);
        } catch (Exception e) {
            //log.error(e.getLocalizedMessage() + " -> " + subject);
            e.printStackTrace();
        }
        return candidates;
    }
    public Set<ResourceCandidate> searchLiteral(String coOccurence,int maxNumberOfResults) {
        BoolQueryBuilder booleanQueryBuilder = new BoolQueryBuilder();
        Set<ResourceCandidate> candidates=new HashSet<>();
        //QueryBuilder queryBuilderMatchFuzzy=new MatchQueryBuilder("literal"+".full", coOccurence).operator(Operator.AND).fuzziness(2).prefixLength(0).maxExpansions(50).fuzzyTranspositions(true);
        //queryBuilder = new MatchQueryBuilder(LABEL, coOccurence).operator(Operator.AND);
        //NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
        //}
        //else {
        //if(coOccurence.length()<4)
        //QueryBuilder queryBuilderMatchTerm= new MatchQueryBuilder("literal"+".raw", coOccurence).operator(Operator.AND);
        QueryBuilder queryBuilderTermFuzzy=QueryBuilders.fuzzyQuery("literal"+".raw",coOccurence).fuzziness(Fuzziness.TWO).prefixLength(0).maxExpansions(50);
        //}

        try {

            candidates = getFromIndexLiteral(maxNumberOfResults, queryBuilderTermFuzzy);
            //cache.put(bq, triples);

        } catch (Exception e) {
            //log.error(e.getLocalizedMessage() + " -> " + subject);
            e.printStackTrace();
        }
        candidates.forEach(resourceCandidate -> resourceCandidate.setCoOccurence(coOccurence));
        return candidates;
    }
    private Set<ResourceCandidate> getFromIndexLiteral(int maxNumberOfResults, QueryBuilder bq) throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(bq);
        TopHitsAggregationBuilder tl= AggregationBuilders.topHits("top").size(maxNumberOfResults);
        searchSourceBuilder.aggregation(tl);

        searchSourceBuilder.size(maxNumberOfResults);
        searchRequest.source(searchSourceBuilder);
        searchRequest.indices("limboliterals");
        //searchRequest.source().aggregation(AggregationBuilders.max("prominence").script(new Script("doc['"+CONNECTED_RESOURCE_SUBJECT+"'].values.length + doc['"+CONNECTED_RESOURCE_OBJECT+"'].values.length ")));
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        //SearchHit[] hits = searchResponse.getHits().getHits();
        TopHits topHits = searchResponse.getAggregations().get("top");
        Set<ResourceCandidate> candidates = new HashSet<>();
        String name;
        Set<String>connectedResourcesSubject,connectedResourcesObject,connectedPropertiesSubject,connectedPropertiesObject,types,label;
        for (SearchHit hit : topHits.getHits()) {
            Map<String, Object> sources = hit.getSourceAsMap();
            name = sources.get("literal").toString();
            label = new HashSet<>();
            label.add(sources.get("literal").toString());

            connectedPropertiesSubject=new HashSet<>();
            connectedPropertiesObject=new HashSet<>();
            connectedPropertiesObject.add(sources.get("property").toString());
            connectedResourcesSubject=new HashSet<>();
            connectedResourcesObject=new HashSet<>();
            connectedResourcesObject.add(sources.get("subject").toString());
            types=new HashSet<>();
            types.add("literal");
            ResourceCandidate candidate = new EntityCandidate(name,label,connectedPropertiesSubject,connectedPropertiesObject,connectedResourcesSubject,connectedResourcesObject,types);
            candidates.add(candidate);
        }

        return candidates;
    }
    public Set<ResourceCandidate> searchEntity(String coOccurence, Optional<String> linkedResource,Optional<String>linkedProperty,Optional<String>type,int maxNumberOfResults) {
        BoolQueryBuilder booleanQueryBuilder = new BoolQueryBuilder();
        Set<ResourceCandidate> candidates=new HashSet<>();
        QueryBuilder queryBuilderMatchFuzzy=new MatchQueryBuilder(LABEL+".full", coOccurence).operator(Operator.AND).fuzziness(2).prefixLength(0).maxExpansions(50).fuzzyTranspositions(true);
            //queryBuilder = new MatchQueryBuilder(LABEL, coOccurence).operator(Operator.AND);
            //NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
        //}
        //else {
            //if(coOccurence.length()<4)
        QueryBuilder queryBuilderMatchTerm= new MatchQueryBuilder(LABEL+".raw", coOccurence).operator(Operator.AND);
        QueryBuilder queryBuilderTermFuzzy=QueryBuilders.fuzzyQuery(LABEL+".raw",coOccurence).fuzziness(Fuzziness.TWO).prefixLength(0).maxExpansions(50);
        //}
        BoolQueryBuilder combQueries=new BoolQueryBuilder();
        if(coOccurence.contains(" ")) combQueries.should(queryBuilderMatchFuzzy.boost(1));
        combQueries.should(queryBuilderTermFuzzy.boost(2));
        combQueries.should(queryBuilderMatchTerm.boost(3));
        booleanQueryBuilder.must(combQueries);
        ExistsQueryBuilder property=new ExistsQueryBuilder(CONNECTED_PROPERTY_SUBJECT);
        ExistsQueryBuilder resource=new ExistsQueryBuilder(CONNECTED_PROPERTY_OBJECT);

        BoolQueryBuilder connect=new BoolQueryBuilder();
        connect.should(property);
        connect.should(resource);
        booleanQueryBuilder.must(connect);
        linkedProperty.ifPresent(prop->{
            BoolQueryBuilder propertyBoolQuery = new BoolQueryBuilder();
            QueryBuilder q1 = termQuery(CONNECTED_PROPERTY_SUBJECT, prop);
            QueryBuilder q2 = termQuery(CONNECTED_PROPERTY_OBJECT, prop);
            propertyBoolQuery.should(q1);
            propertyBoolQuery.should(q2);
            booleanQueryBuilder.must(propertyBoolQuery);
        });
        linkedResource.ifPresent(res->{
            BoolQueryBuilder resourceBoolQuery = new BoolQueryBuilder();
            QueryBuilder q1 = termQuery(CONNECTED_RESOURCE_SUBJECT, res);
            QueryBuilder q2 = termQuery(CONNECTED_RESOURCE_OBJECT, res);
            resourceBoolQuery.should(q1);
            resourceBoolQuery.should(q2);
            booleanQueryBuilder.must(resourceBoolQuery);
        });
        /*linkedResourceObject.ifPresent(res->{
            QueryBuilder q = termQuery(CONNECTED_RESOURCE_OBJECT, res);
            booleanQueryBuilder.must(q);
        });*/
        type.ifPresent(tp->{
            QueryBuilder q = termQuery(TYPE, tp);
            booleanQueryBuilder.must(q);
        });
        try {
            /*boolean stop=false;
            int i=0;
            while(!stop&&(candidates.size()>=100||candidates.size()==0)) {
                if(i>0) {
                    Map<String,Object> m=new HashMap<>();

                    String scriptString="doc['" + CONNECTED_RESOURCE_SUBJECT + "'].values.length >  params.m && doc['" + CONNECTED_RESOURCE_OBJECT + "'].values.length > params.m";
                    m.put("m",i);
                    Script script=new Script(ScriptType.INLINE,"painless",scriptString,m);
                    ScriptQueryBuilder sq = scriptQuery(script);
                    booleanQueryBuilder.must(sq);
                }
                candidates = getFromIndexEntity(maxNumberOfResults, booleanQueryBuilder);
                if(i==0&&candidates.size()==0)stop=true;
                //else if(candidates.size()==0) i-=2;
                else i+=2;
            }*/
            candidates = getFromIndexEntity(maxNumberOfResults, booleanQueryBuilder);
            //cache.put(bq, triples);

        } catch (Exception e) {
            //log.error(e.getLocalizedMessage() + " -> " + subject);
            e.printStackTrace();
        }
        return candidates;
    }

    public Set<ResourceCandidate> searchByType(Optional<String> linkedResource,Optional<String>linkedProperty,Optional<String>type,int maxNumberOfResults) {
        BoolQueryBuilder booleanQueryBuilder = new BoolQueryBuilder();
        Set<ResourceCandidate> candidates=new HashSet<>();
        ExistsQueryBuilder property=new ExistsQueryBuilder(CONNECTED_PROPERTY_SUBJECT);
        ExistsQueryBuilder resource=new ExistsQueryBuilder(CONNECTED_PROPERTY_OBJECT);
        BoolQueryBuilder connect=new BoolQueryBuilder();
        connect.should(property);
        connect.should(resource);
        booleanQueryBuilder.must(connect);
        linkedProperty.ifPresent(prop->{
            BoolQueryBuilder propertyBoolQuery = new BoolQueryBuilder();
            QueryBuilder q1 = termQuery(CONNECTED_PROPERTY_SUBJECT, prop);
            QueryBuilder q2 = termQuery(CONNECTED_PROPERTY_OBJECT, prop);
            propertyBoolQuery.should(q1);
            propertyBoolQuery.should(q2);
            booleanQueryBuilder.must(propertyBoolQuery);
        });
        linkedResource.ifPresent(res->{
            BoolQueryBuilder resourceBoolQuery = new BoolQueryBuilder();
            QueryBuilder q1 = termQuery(CONNECTED_RESOURCE_SUBJECT, res);
            QueryBuilder q2 = termQuery(CONNECTED_RESOURCE_OBJECT, res);
            resourceBoolQuery.should(q1);
            resourceBoolQuery.should(q2);
            booleanQueryBuilder.must(resourceBoolQuery);
        });
        /*linkedResourceObject.ifPresent(res->{
            QueryBuilder q = termQuery(CONNECTED_RESOURCE_OBJECT, res);
            booleanQueryBuilder.must(q);
        });*/
        type.ifPresent(tp->{
            QueryBuilder q = termQuery(TYPE, tp);
            booleanQueryBuilder.must(q);
        });
        try {

            candidates = getFromIndexEntity(maxNumberOfResults, booleanQueryBuilder);
            //cache.put(bq, triples);

        } catch (Exception e) {
            //log.error(e.getLocalizedMessage() + " -> " + subject);
            e.printStackTrace();
        }
        return candidates;
    }

    private Set<ResourceCandidate> getFromIndexEntity(int maxNumberOfResults, QueryBuilder bq) throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(bq);
        TopHitsAggregationBuilder tl= AggregationBuilders.topHits("top").size(maxNumberOfResults);
        searchSourceBuilder.aggregation(tl);

        searchSourceBuilder.size(maxNumberOfResults);
        searchRequest.source(searchSourceBuilder);
        searchRequest.indices(entityIndex);
        //searchRequest.source().aggregation(AggregationBuilders.max("prominence").script(new Script("doc['"+CONNECTED_RESOURCE_SUBJECT+"'].values.length + doc['"+CONNECTED_RESOURCE_OBJECT+"'].values.length ")));
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        //SearchHit[] hits = searchResponse.getHits().getHits();
        TopHits topHits = searchResponse.getAggregations().get("top");
        Set<ResourceCandidate> candidates = new HashSet<>();
        String name;
        Set<String>connectedResourcesSubject,connectedResourcesObject,connectedPropertiesSubject,connectedPropertiesObject,types,label;
        for (SearchHit hit : topHits.getHits()) {
            Map<String, Object> sources = hit.getSourceAsMap();
            name = hit.getId();
            label = new HashSet<>();
            label.addAll((ArrayList<String>)sources.get(LABEL));

            connectedPropertiesSubject=new HashSet<>();
            connectedPropertiesSubject.addAll((ArrayList<String>)sources.get(CONNECTED_PROPERTY_SUBJECT));
            connectedPropertiesObject=new HashSet<>();
            connectedPropertiesObject.addAll((ArrayList<String>)sources.get(CONNECTED_PROPERTY_OBJECT));
            connectedResourcesSubject=new HashSet<>();
            connectedResourcesSubject.addAll((ArrayList<String>)sources.get(CONNECTED_RESOURCE_SUBJECT));
            connectedResourcesObject=new HashSet<>();
            connectedResourcesObject.addAll((ArrayList<String>)sources.get(CONNECTED_RESOURCE_OBJECT));
            types=new HashSet<>();
            types.addAll((ArrayList<String>)sources.get(TYPE));
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
            ResourceCandidate candidate = new EntityCandidate(name,label,connectedPropertiesSubject,connectedPropertiesObject,connectedResourcesSubject,connectedResourcesObject,types);
            candidates.add(candidate);
        }

        return candidates;
    }
    public void close() throws IOException {
        client.close();
    }
    public static void main(String[]args){
        try {
            WordsGenerator w=new WordsGenerator();
            ElasticSearchEntityIndex en=new ElasticSearchEntityIndex();
            List<String>a=new ArrayList<>();
            a.add("http://dbpedia.org/resource/BlaBlaCar");
            //Set<ResourceCandidate>uris=en.searchEntitiesById(a);
            //Set<ResourceCandidate>uris=en.searchEntity("Länge",Optional.empty(),Optional.empty(),Optional.empty(),100);
            Set<ResourceCandidate>uris;
//
//            FillTemplatePatternsWithResources template = new FillTemplatePatternsWithResources()
//            getbestResourcesByLevenstheinRatio();

//            uris=en.searchEntity("LSA 460", Optional.empty(),Optional.empty(), Optional.empty());
//            for(ResourceCandidate res:uris)
//                System.out.println(res.getUri());

            uris=en.searchResource("EMail Adresse","property",true);
            for(ResourceCandidate res:uris)
                System.out.println(res.getUri());

//            uris=en.searchResource("Marken","class",false);
//            for(ResourceCandidate res:uris)
//                System.out.println(res.getUri());

            en.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

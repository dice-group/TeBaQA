package de.uni.leipzig.tebaqa.controller;

import de.uni.leipzig.tebaqa.model.Triple;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.elasticsearch.index.query.QueryBuilders.existsQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

public class ElasticSearchEntityIndex {

    private UrlValidator urlValidator = new UrlValidator();
    private RestHighLevelClient client;
    public ElasticSearchEntityIndex()throws IOException {
        Properties prop = new Properties();
        InputStream input = null;
        input = new FileInputStream("src/main/resources/application.properties");
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
        String index =envDefaultIndex != null ? envDefaultIndex : prop.getProperty("entity_index");
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
    public void close() throws IOException {
        client.close();
    }
    public static void main(String[]args){
        try {
            ElasticSearchEntityIndex en=new ElasticSearchEntityIndex();
            List<String[]>uris=en.search("Tankstellen","class");
            for(String[] res:uris)
                System.out.println(res[0]+" "+res[1]);
            en.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

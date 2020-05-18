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

    public List<String[]> search(String coOcurrence,String index,String lang) {
        int defaultMaxNumberOfDocsRetrievedFromIndex = 100;
        return search(coOcurrence, defaultMaxNumberOfDocsRetrievedFromIndex,index,lang);
    }

    public List<String[]> search(String coOcurrence, int maxNumberOfResults,String index,String lang) {
        QueryBuilder queryBuilder;
        String language;
        if(lang.equals("en"))
            language="label_en";
        else language="label_de";
        if(coOcurrence.contains(" ")) {
            queryBuilder = new MatchQueryBuilder(language, coOcurrence).operator(Operator.AND).fuzziness(Fuzziness.TWO).prefixLength(0).maxExpansions(2).fuzzyTranspositions(true);
            //NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
        }
        else
            queryBuilder=QueryBuilders.fuzzyQuery(language,coOcurrence).fuzziness(Fuzziness.TWO).prefixLength(0).maxExpansions(2);

            List<String[]> resources=new ArrayList<>();
        try {
            resources = getFromIndex(maxNumberOfResults, queryBuilder,index,language);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        //cache.put(bq, triples);

        return resources;
    }


    private List<String[]> getFromIndex(int maxNumberOfResults, QueryBuilder bq,String index,String label) throws IOException {
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
            resources.add(new String[]{sources.get("uri").toString(),sources.get(label).toString()});
        }

        return resources;
    }



    public void close() throws IOException {
        client.close();
    }
    public static void main(String[]args){
        try {
            ElasticSearchEntityIndex en=new ElasticSearchEntityIndex();
            List<String[]>uris=en.search("Distributionen","propdcat3","de");
            for(String[] res:uris)
                System.out.println(res[0]+" "+res[1]);
            en.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

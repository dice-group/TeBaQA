package de.uni.leipzig.tebaqa.indexGenerationDcat;

import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class WriteDcatIndex {
    RestHighLevelClient client;
    BulkProcessor bulkProcessor;
    private String resourceIndex;
    private String propertyIndex;
    private String classIndex;
    final String TYPE="type";
    final String LABEL="label";
    final String SYNONYMS="synonyms";
    final String CONNECTED_RESOURCE_SUBJECT="connected_resource_subject";
    final String CONNECTED_RESOURCE_OBJECT="connected_resource_object";
    final String CONNECTED_PROPERTY_SUBJECT="connected_property_subject";
    final String CONNECTED_PROPERTY_OBJECT="connected_property_object";
    public WriteDcatIndex()throws IOException {
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
    }
    public void setOntologieIndexes(String predicateIndex,String classIndex){
        this.classIndex=classIndex;
        generateOntIndex(classIndex);
        this.propertyIndex=predicateIndex;
        generateOntIndex(predicateIndex);
    }
    void generateOntIndex(String index){
        try {
            GetIndexRequest getIndexRequest = new GetIndexRequest();
            getIndexRequest.indices(index);
            boolean exists= client.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
            if(!exists) {
                XContentBuilder settingsBuilder = null;
                settingsBuilder = jsonBuilder()
                        .startObject()
                        .startObject("analysis")
                        .startObject("analyzer")
                        .startObject("literal_analyzer")
                        .field("type","custom")
                        .field("tokenizer", "lowercase")
                        .field("filter","asciifolding")
                        .endObject()
                        .endObject()
                        .endObject()
                        .endObject();
                XContentBuilder mappingsBuilder =jsonBuilder()
                        .startObject()
                        .startObject("properties")
                        .startObject("uri")
                        .field("type", "keyword")
                        .endObject()
                        .startObject("label_en")
                        .field("type", "text")
                        .field("analyzer", "standard")
                        .endObject()
                        .startObject("label_de")
                        .field("type", "text")
                        .field("analyzer", "standard")
                        .endObject()
                        // .startObject("synonyms")
                        //.field("type", "text")
                        //.field("analyzer", "standard")
                        //.endObject()
                        .endObject()
                        .endObject();
                CreateIndexRequest request = new CreateIndexRequest(index);
                request.mapping("_doc", mappingsBuilder);
                //request.settings(Settings.EMPTY);
                //request.settings(settingsBuilder);
                CreateIndexResponse createIndexResponse = client.indices().create(request, RequestOptions.DEFAULT);
                System.out.println(createIndexResponse);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createBulkProcessor() {
        BulkProcessor.Listener listener = new BulkProcessor.Listener() {
            @Override
            public void beforeBulk(long executionId, BulkRequest request) {

                //System.out.println("before bulk");
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request,
                                  BulkResponse response) {
                //System.out.println("after bulk1");
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request,
                                  Throwable failure) {
                //System.out.println("after bulk2");
            }
        };
        BulkProcessor.Builder builder = BulkProcessor.builder(
                (req, bulkListener) ->
                        client.bulkAsync(req, RequestOptions.DEFAULT, bulkListener),
                listener);
        builder.setBulkActions(1000);
        builder.setBulkSize(new ByteSizeValue(1L, ByteSizeUnit.MB));
        builder.setConcurrentRequests(0);
        builder.setFlushInterval(TimeValue.timeValueSeconds(10L));
        builder.setBackoffPolicy(BackoffPolicy
                .constantBackoff(TimeValue.timeValueSeconds(1L), 3));
        bulkProcessor=builder.build();
    }

    public void indexClass(String uri, String labelEn,String labelDe)throws IOException{
        IndexRequest indexRequest = new IndexRequest(classIndex, "_doc")
                .source(jsonBuilder()
                        .startObject()
                        .field("uri", uri)
                        .field("label_en", labelEn)
                        .field("label_de", labelDe)
                        .endObject()
                );
        bulkProcessor.add(indexRequest);
    }
    public void indexProperty(String uri,String labelEn,String labelDe)throws IOException{
        IndexRequest indexRequest;

            indexRequest = new IndexRequest(propertyIndex, "_doc")
                    .source(jsonBuilder()
                            .startObject()
                            .field("uri", uri)
                            .field("label_en", labelEn)
                            .field("label_de", labelDe)
                            .endObject()
                    );

        bulkProcessor.add(indexRequest);
    }
    public void close() {
        try {
            if(bulkProcessor!=null)
                bulkProcessor.close();
            if(client!=null)
                client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

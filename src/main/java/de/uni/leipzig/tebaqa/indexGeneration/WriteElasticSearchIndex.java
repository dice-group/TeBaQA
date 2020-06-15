package de.uni.leipzig.tebaqa.indexGeneration;

import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;

import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequestBuilder;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
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

public class WriteElasticSearchIndex {

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
        public WriteElasticSearchIndex()throws IOException {
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
                            .startObject("label")
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
        public void setResourceIndexes(String resourceIndex){
            this.resourceIndex=resourceIndex;
            GetIndexRequest getIndexRequest = new GetIndexRequest();
            getIndexRequest.indices(resourceIndex);
            boolean exists = false;
            try {
                exists = client.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
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
                            .startObject("connected_resource_subject")
                            .field("type", "keyword")
                            .endObject()
                            .startObject("connected_resource_object")
                            .field("type", "keyword")
                            .endObject()
                            .startObject("connected_property_subject")
                            .field("type", "keyword")
                            .endObject()
                            .startObject("connected_property_object")
                            .field("type", "keyword")
                            .endObject()
                            .startObject("type")
                            .field("type", "keyword")
                            .endObject()
                            .startObject("label")
                            .field("type","text")
                            .startObject("fields")
                            .startObject("raw")
                            .field("type","keyword")
                            .endObject()
                            .startObject("full")
                            .field("type", "text")
                            .field("analyzer", "standard")
                            .endObject()
                            .endObject()
                            //.field("type", "text")
                            //.field("analyzer", "standard")
                            //.field("analyzer", "literal_analyzer")
                            .endObject()
                            .endObject()
                            .endObject();
                    CreateIndexRequest request = new CreateIndexRequest(this.resourceIndex);
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
        public void indexResource(String name,String nameToLabels,String nameToTypes,String nameToResource,String nameToProperties)throws IOException{
            IndexRequest indexRequest = new IndexRequest(resourceIndex, "_doc",name)
                    .source(jsonBuilder()
                            .startObject()
                            .array(LABEL, nameToLabels.split(",,"))
                            .array(TYPE,nameToTypes.split(",,"))
                            .array(CONNECTED_PROPERTY_SUBJECT,nameToResource.split(",,"))
                            .array(CONNECTED_RESOURCE_OBJECT,nameToProperties.split(",,"))
                            .endObject()
                    );
            bulkProcessor.add(indexRequest);
        }
    public void upsertResource(String name,Set nameToLabels,Set nameToTypes,Set nameToResourceSubject,Set nameToResourceObject,Set nameToPropertiesSubject,Set nameToPropertiesObject)throws IOException{
        Map<String,Object> m=new HashMap<>();

        String scriptString = "ctx._source."+CONNECTED_RESOURCE_SUBJECT+".addAll(params.resourceSubject);\n";
        scriptString+="ctx._source."+CONNECTED_RESOURCE_OBJECT+".addAll(params.resourceObject);\n";
        scriptString+="ctx._source."+CONNECTED_PROPERTY_SUBJECT+".addAll(params.propertysubject);\n";
        scriptString+="ctx._source."+CONNECTED_PROPERTY_OBJECT+".addAll(params.propertyobject);\n";
        scriptString+= "ctx._source."+TYPE+".addAll(params.type);\n";
        scriptString+="ctx._source."+LABEL+".addAll(params.label);\n";
        m.put("resourceSubject",nameToResourceSubject.toArray());
        m.put("resourceObject",nameToResourceObject.toArray());
        m.put("propertysubject",nameToPropertiesSubject.toArray());
        m.put("propertyobject",nameToPropertiesObject.toArray());
        m.put("type",nameToTypes.toArray());
        m.put("label",nameToLabels.toArray());


        Script script=new Script(ScriptType.INLINE,"painless",scriptString,m);
        try {
            IndexRequest indexRequest = null;


            indexRequest = new IndexRequest(resourceIndex, "_doc", name)
                    .source(jsonBuilder()
                            .startObject()
                            .array(CONNECTED_RESOURCE_SUBJECT, nameToResourceSubject.toArray())
                            .array(CONNECTED_RESOURCE_OBJECT, nameToResourceObject.toArray())
                            .array(CONNECTED_PROPERTY_SUBJECT, nameToPropertiesSubject.toArray())
                            .array(CONNECTED_PROPERTY_OBJECT, nameToPropertiesObject.toArray())
                            .array(TYPE,nameToTypes.toArray())
                            .array(LABEL,nameToLabels.toArray())
                            .endObject()
                    );
            indexRequest.id(name);
            UpdateRequest updateRequest = new UpdateRequest(resourceIndex, "_doc", name)
                    .script(script)
                    .upsert(indexRequest);

            bulkProcessor.add(updateRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
        public void indexClass(String uri,List<String> label)throws IOException{
            IndexRequest indexRequest = new IndexRequest("classlimbo", "_doc")
                        .source(jsonBuilder()
                                .startObject()
                                .field("uri", uri)
                                .array("label", label.toArray())
                                .endObject()
                        );
            bulkProcessor.add(indexRequest);
        }
    public void indexProperty(String uri,List<String> label)throws IOException{
        IndexRequest indexRequest = new IndexRequest("propertylimbo", "_doc")
                .source(jsonBuilder()
                        .startObject()
                        .field("uri", uri)
                        .array("label", label.toArray())
                        .endObject()
                );
        bulkProcessor.add(indexRequest);
    }
        public void indexProperty(String uri,String label,List<String>synonyms)throws IOException{
            IndexRequest indexRequest;
            if(synonyms!=null) {
                indexRequest = new IndexRequest("propertylimbo", "_doc")
                        .source(jsonBuilder()
                                .startObject()
                                .field("uri", uri)
                                .field("label", label)
                                .array(SYNONYMS, synonyms.toArray())
                                .endObject()
                        );
            }
            else{
                indexRequest = new IndexRequest("propertylimbo", "_doc")
                        .source(jsonBuilder()
                                .startObject()
                                .field("uri", uri)
                                .field("label", label)
                                .endObject()
                        );
            }
            bulkProcessor.add(indexRequest);
        }
        /*public void upsertLabel(String documentUri, String label){
            String scriptString="ctx._source."+LABEL+".add(params.label);\n";
            Map<String,Object> m=new HashMap<>();
                                m.put("label",label);
            Script script=new Script(ScriptType.INLINE,"painless",scriptString,m);
            try {
                IndexRequest indexRequest = null;

                indexRequest = new IndexRequest(resourceIndex, "_doc", documentUri)
                        .source(jsonBuilder()
                                .startObject()
                                .array(LABEL, label)
                                .array(TYPE,new String[]{})
                                .array(CONNECTED_PROPERTY_SUBJECT,new String[]{})
                                .array(CONNECTED_RESOURCE,new String[]{})
                                .endObject()
                        );
                indexRequest.id(documentUri);
                UpdateRequest updateRequest = new UpdateRequest(resourceIndex, "_doc", documentUri)
                        .script(script)
                        .upsert(indexRequest);

                bulkProcessor.add(updateRequest);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public void upsertRelResource(String documentUri, String relatedResource,String property){
            Map<String,Object> m=new HashMap<>();

            String scriptString = "ctx._source."+CONNECTED_RESOURCE+".add(params.resource);\n";
            scriptString+="ctx._source."+CONNECTED_PROPERTY_SUBJECT+".add(params.property);\n";
            m.put("resource",relatedResource);
            m.put("property",property);


            Script script=new Script(ScriptType.INLINE,"painless",scriptString,m);
            try {
                IndexRequest indexRequest = null;


                indexRequest = new IndexRequest(resourceIndex, "_doc", documentUri)
                        .source(jsonBuilder()
                                .startObject()
                                .array(CONNECTED_RESOURCE, relatedResource)
                                .array(CONNECTED_PROPERTY_SUBJECT, property)
                                .array(TYPE,new String[]{})
                                .array(LABEL,new String[]{})
                                .endObject()
                        );
                indexRequest.id(documentUri);
                UpdateRequest updateRequest = new UpdateRequest(resourceIndex, "_doc", documentUri)
                        .script(script)
                        .upsert(indexRequest);

                bulkProcessor.add(updateRequest);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void upsertType(String documentUri, String type){

            Map<String,Object> m=new HashMap<>();
            String scriptString = "ctx._source."+TYPE+".add(params.type);\n";
                m.put("type",type);


            Script script=new Script(ScriptType.INLINE,"painless",scriptString,m);
            try {
                IndexRequest indexRequest = null;

                indexRequest = new IndexRequest(resourceIndex, "_doc", documentUri)
                        .source(jsonBuilder()
                                .startObject()
                                .array(TYPE,type)
                                .array(LABEL,new String[]{})
                                .array(CONNECTED_PROPERTY_SUBJECT,new String[]{})
                                .array(CONNECTED_RESOURCE,new String[]{})
                                .endObject()
                        );
                indexRequest.id(documentUri);
                UpdateRequest updateRequest = new UpdateRequest(resourceIndex, "_doc", documentUri)
                        .script(script)
                        .upsert(indexRequest);

                bulkProcessor.add(updateRequest);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/
    /*public void upsertPredicate(String documentUri, String predicate){

        Map<String,Object> m=new HashMap<>();
        String scriptString = "ctx._source."+CONNECTED_PROPERTY_SUBJECT+".add(params.predicate);\n";
        m.put("predicate",predicate);


        Script script=new Script(ScriptType.INLINE,"painless",scriptString,m);
        try {
            IndexRequest indexRequest = null;

            indexRequest = new IndexRequest(resourceIndex, "_doc", documentUri)
                    .source(jsonBuilder()
                            .startObject()
                            .array(TYPE,new String[]{})
                            .array(LABEL,new String[]{})
                            .array(CONNECTED_PROPERTY_SUBJECT,predicate)
                            .array(CONNECTED_RESOURCE,new String[]{})
                            .endObject()
                    );
            indexRequest.id(documentUri);
            UpdateRequest updateRequest = new UpdateRequest(resourceIndex, "_doc", documentUri)
                    .script(script)
                    .upsert(indexRequest);

            bulkProcessor.add(updateRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void upsertConnectedResource(String documentUri, String resource){

        Map<String,Object> m=new HashMap<>();
        String scriptString = "ctx._source."+CONNECTED_RESOURCE+".add(params.resource);\n";
        m.put("resource",resource);


        Script script=new Script(ScriptType.INLINE,"painless",scriptString,m);
        try {
            IndexRequest indexRequest = null;

            indexRequest = new IndexRequest(resourceIndex, "_doc", documentUri)
                    .source(jsonBuilder()
                            .startObject()
                            .array(TYPE,new String[]{})
                            .array(LABEL,new String[]{})
                            .array(CONNECTED_PROPERTY_SUBJECT,new String[]{})
                            .array(CONNECTED_RESOURCE,resource)
                            .endObject()
                    );
            indexRequest.id(documentUri);
            UpdateRequest updateRequest = new UpdateRequest(resourceIndex, "_doc", documentUri)
                    .script(script)
                    .upsert(indexRequest);

            bulkProcessor.add(updateRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/
        public void commit() {

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

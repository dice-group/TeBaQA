package de.uni.leipzig.tebaqa.tebaqacommons.elasticsearch;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import de.uni.leipzig.tebaqa.tebaqacommons.model.ESConnectionProperties;
import org.apache.http.HttpHost;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class ElasticSearchIndexer {

    private static final Logger LOGGER = LogManager.getLogger(ElasticSearchIndexer.class);

    // Security is not needed. Only unique values need to be generated. sha1 is sufficient for that.
    private final HashFunction hasher = Hashing.sha1();

    // Constants
    final String URI = "uri";
    final String TYPE = "type";
    final String LABEL = "label";
    final String SYNONYMS = "synonyms";
    final String CONNECTED_RESOURCE_SUBJECT = "connected_resource_subject";
    final String CONNECTED_RESOURCE_OBJECT = "connected_resource_object";
    final String CONNECTED_PROPERTY_SUBJECT = "connected_property_subject";
    final String CONNECTED_PROPERTY_OBJECT = "connected_property_object";

    private final RestHighLevelClient client;
    private BulkProcessor bulkProcessor;
    private String entityIndex;
    private String literalIndex;
    private String propertyIndex;
    private String classIndex;

    public ElasticSearchIndexer(ESConnectionProperties properties) {

        client = new RestHighLevelClient(
                RestClient.builder(new HttpHost(properties.getHostname(), Integer.parseInt(properties.getPort()), properties.getScheme()))
                        .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                                .setConnectTimeout(180000)
                                .setConnectionRequestTimeout(180000)
                                .setSocketTimeout(180000)).setMaxRetryTimeoutMillis(90000)
        );
        this.entityIndex = properties.getEntityIndex();
        this.propertyIndex = properties.getPropertyIndex();
        this.classIndex = properties.getClassIndex();
        this.literalIndex = properties.getLiteralIndex();
    }

    public void setAndCreateOntologyIndexes(String propertyIndex, String classIndex) throws IOException {
        this.classIndex = classIndex;
        this.propertyIndex = propertyIndex;
        createOntologyIndexes();
    }

    public void createOntologyIndexes() throws IOException {
        createOntologyIndex(this.classIndex);
        createOntologyIndex(this.propertyIndex);
    }

    public void setAndCreateEntityIndex(String entityIndex) throws IOException {
        this.entityIndex = entityIndex;
        this.createEntityIndex();
    }

    //    TODO check why settings builder is unused
    public void createEntityIndex() throws IOException {

        GetIndexRequest getIndexRequest = new GetIndexRequest();
        getIndexRequest.indices(this.entityIndex);

        boolean exists = client.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
        if (!exists) {
            XContentBuilder settingsBuilder = null;
            settingsBuilder = jsonBuilder()
                    .startObject()
                    .startObject("analysis")
                    .startObject("analyzer")
                    .startObject("literal_analyzer")
                    .field("type", "custom")
                    .field("tokenizer", "lowercase")
                    .field("filter", "asciifolding")
                    .endObject()
                    .endObject()
                    .endObject()
                    .endObject();
            XContentBuilder mappingsBuilder = jsonBuilder()
                    .startObject()
                    .startObject("properties")
                    .startObject(CONNECTED_RESOURCE_SUBJECT)
                    .field("type", "keyword")
                    .endObject()
                    .startObject(CONNECTED_RESOURCE_OBJECT)
                    .field("type", "keyword")
                    .endObject()
                    .startObject(CONNECTED_PROPERTY_SUBJECT)
                    .field("type", "keyword")
                    .endObject()
                    .startObject(CONNECTED_PROPERTY_OBJECT)
                    .field("type", "keyword")
                    .endObject()
                    .startObject(TYPE)
                    .field("type", "keyword")
                    .endObject()
                    .startObject(URI)
                    .field("type", "keyword")
                    .endObject()
                    .startObject(LABEL)
                    .field("type", "text")
                    .startObject("fields")
                    .startObject("raw")
                    .field("type", "keyword")
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
            CreateIndexRequest request = new CreateIndexRequest(this.entityIndex);
            request.mapping("_doc", mappingsBuilder);
            //request.settings(Settings.EMPTY);
            //request.settings(settingsBuilder);
            CreateIndexResponse createIndexResponse = client.indices().create(request, RequestOptions.DEFAULT);
            LOGGER.info("Entity index created: " + this.entityIndex);
        }


    }


    //    TODO check why settings builder is unused
    private void createOntologyIndex(String index) throws IOException {
        GetIndexRequest getIndexRequest = new GetIndexRequest();
        getIndexRequest.indices(index);
        boolean exists = client.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
        if (!exists) {
            XContentBuilder settingsBuilder = null;
            settingsBuilder = jsonBuilder()
                    .startObject()
                    .startObject("analysis")
                    .startObject("analyzer")
                    .startObject("literal_analyzer")
                    .field("type", "custom")
                    .field("tokenizer", "lowercase")
                    .field("filter", "asciifolding")
                    .endObject()
                    .endObject()
                    .endObject()
                    .endObject();
            XContentBuilder mappingsBuilder = jsonBuilder()
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
            LOGGER.info("Ontology index created: " + index);
        }
    }

    public void setLiteralIndexes(String literalIndex) {
        this.literalIndex = literalIndex;
        GetIndexRequest getIndexRequest = new GetIndexRequest();
        getIndexRequest.indices(literalIndex);
        boolean exists = false;
        try {
            exists = client.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
            if (!exists) {
                XContentBuilder settingsBuilder = null;
                settingsBuilder = jsonBuilder()
                        .startObject()
                        .startObject("analysis")
                        .startObject("analyzer")
                        .startObject("literal_analyzer")
                        .field("type", "custom")
                        .field("tokenizer", "lowercase")
                        .field("filter", "asciifolding")
                        .endObject()
                        .endObject()
                        .endObject()
                        .endObject();
                XContentBuilder mappingsBuilder = jsonBuilder()
                        .startObject()
                        .startObject("properties")
                        .startObject("subject")
                        .field("type", "keyword")
                        .endObject()
                        .startObject("property")
                        .field("type", "keyword")
                        .endObject()
                        .startObject("literal")
                        .field("type", "text")
                        .startObject("fields")
                        .startObject("raw")
                        .field("type", "keyword")
                        .endObject()
                        .startObject("full")
                        .field("type", "text")
                        .field("analyzer", "standard")
                        .endObject()
                        .endObject()
                        .endObject()
                        .endObject()
                        //.field("type", "text")
                        //.field("analyzer", "standard")
                        //.field("analyzer", "literal_analyzer")
                        .endObject();
                CreateIndexRequest request = new CreateIndexRequest(this.literalIndex);
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
        builder.setBulkActions(-1);
        builder.setBulkSize(new ByteSizeValue(20L, ByteSizeUnit.MB));
        builder.setConcurrentRequests(0);
        builder.setFlushInterval(TimeValue.timeValueSeconds(10L));
        builder.setBackoffPolicy(BackoffPolicy
                .constantBackoff(TimeValue.timeValueSeconds(1L), 3));
        bulkProcessor = builder.build();
    }

    public void indexLiteral(String subject, String property, String literal) throws IOException {
        IndexRequest indexRequest = new IndexRequest(literalIndex, "_doc")
                .source(jsonBuilder()
                        .startObject()
                        .field("subject", subject)
                        .field("property", property)
                        .field("literal", literal)
                        .endObject()
                );
        bulkProcessor.add(indexRequest);
    }

    public void upsertResource(String name, Set<String> nameToLabels, Set<String> nameToTypes,
                               Set<String> nameToResourceSubject, Set<String> nameToResourceObject,
                               Set<String> nameToPropertiesSubject, Set<String> nameToPropertiesObject) {

        Map<String, Object> m = new HashMap<>();

        String scriptString = "ctx._source." + CONNECTED_RESOURCE_SUBJECT + ".addAll(params.resourceSubject);\n" +
                "ctx._source." + CONNECTED_RESOURCE_OBJECT + ".addAll(params.resourceObject);\n" +
                "ctx._source." + CONNECTED_PROPERTY_SUBJECT + ".addAll(params.propertysubject);\n" +
                "ctx._source." + CONNECTED_PROPERTY_OBJECT + ".addAll(params.propertyobject);\n" +
                "ctx._source." + TYPE + ".addAll(params.type);\n" +
                "ctx._source." + LABEL + ".addAll(params.label);\n" +
                "ctx._source." + URI + " = params.uri;\n";
        m.put("resourceSubject", nameToResourceSubject.toArray());
        m.put("resourceObject", nameToResourceObject.toArray());
        m.put("propertysubject", nameToPropertiesSubject.toArray());
        m.put("propertyobject", nameToPropertiesObject.toArray());
        m.put("type", nameToTypes.toArray());
        m.put("label", nameToLabels.toArray());
        m.put("uri", name);


        Script script = new Script(ScriptType.INLINE, "painless", scriptString, m);
        String uriHash = hasher.hashString(name, StandardCharsets.UTF_8).toString();
        try {
            IndexRequest indexRequest;

            indexRequest = new IndexRequest(entityIndex, "_doc", uriHash)
                    .source(jsonBuilder()
                            .startObject()
                            .array(CONNECTED_RESOURCE_SUBJECT, nameToResourceSubject.toArray())
                            .array(CONNECTED_RESOURCE_OBJECT, nameToResourceObject.toArray())
                            .array(CONNECTED_PROPERTY_SUBJECT, nameToPropertiesSubject.toArray())
                            .array(CONNECTED_PROPERTY_OBJECT, nameToPropertiesObject.toArray())
                            .array(TYPE, nameToTypes.toArray())
                            .array(LABEL, nameToLabels.toArray())
                            .field(URI, name)
                            .endObject()
                    );
            indexRequest.id(uriHash);
            UpdateRequest updateRequest = new UpdateRequest(entityIndex, "_doc", uriHash)
                    .script(script)
                    .upsert(indexRequest);

            bulkProcessor.add(updateRequest);
        } catch (IOException e) {
            LOGGER.error("Failed to index: " + name);
            LOGGER.error(e);
        }
    }

    public void indexClass(String uri, List<String> label) throws IOException {
        IndexRequest indexRequest = new IndexRequest(this.classIndex, "_doc")
                .source(jsonBuilder()
                        .startObject()
                        .field(this.URI, uri)
                        .array(this.LABEL, label.toArray())
                        .endObject()
                );
        bulkProcessor.add(indexRequest);
    }

    public void indexProperty(String uri, List<String> label) throws IOException {
        IndexRequest indexRequest = new IndexRequest(this.propertyIndex, "_doc")
                .source(jsonBuilder()
                        .startObject()
                        .field(this.URI, uri)
                        .array(this.LABEL, label.toArray())
                        .endObject()
                );
        bulkProcessor.add(indexRequest);
    }

    public void indexProperty(String uri, String label, List<String> synonyms) throws IOException {
        IndexRequest indexRequest;
        if (synonyms != null) {
            indexRequest = new IndexRequest(this.propertyIndex, "_doc")
                    .source(jsonBuilder()
                            .startObject()
                            .field(this.URI, uri)
                            .field(this.LABEL, label)
                            .array(this.SYNONYMS, synonyms.toArray())
                            .endObject()
                    );
        } else {
            indexRequest = new IndexRequest(this.propertyIndex, "_doc")
                    .source(jsonBuilder()
                            .startObject()
                            .field(this.URI, uri)
                            .field(this.LABEL, label)
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
        if (bulkProcessor != null)
            bulkProcessor.flush();
    }


    public void close() {
        try {
            if (bulkProcessor != null)
                bulkProcessor.close();
            if (client != null)
                client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteIndex(String indexName) {
        DeleteIndexRequest request = new DeleteIndexRequest(indexName);
        try {
            AcknowledgedResponse deleteIndexResponse = client.indices().delete(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

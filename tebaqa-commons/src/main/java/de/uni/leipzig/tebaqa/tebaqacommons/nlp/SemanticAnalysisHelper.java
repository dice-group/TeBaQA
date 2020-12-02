package de.uni.leipzig.tebaqa.tebaqacommons.nlp;

import de.uni.leipzig.tebaqa.tebaqacommons.model.QueryType;
import de.uni.leipzig.tebaqa.tebaqacommons.model.QuestionAnswerType;
import de.uni.leipzig.tebaqa.tebaqacommons.model.RestServiceConfiguration;
import de.uni.leipzig.tebaqa.tebaqacommons.util.RestConstants;
import de.uni.leipzig.tebaqa.tebaqacommons.util.RestServiceConnector;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.ProtobufAnnotationSerializer;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.util.Pair;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

@SuppressWarnings({"unchecked", "rawtypes"})
public class SemanticAnalysisHelper {

    private final String serviceBaseUrl;
    private final Lang lang;

    public SemanticAnalysisHelper(Lang lang) throws IOException {
        this.lang = lang;
        Properties props = new Properties();
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("nlp.properties");
        props.load(inputStream);

        String scheme = props.getProperty("service.nlp.scheme");
        String host = props.getProperty("service.nlp.host");
        String port = props.getProperty("service.nlp.port");

        if (scheme == null || host == null || port == null)
            throw new IOException("NLP properties could not be loaded!");

        this.serviceBaseUrl = new RestServiceConfiguration(scheme, host, port).getUrl();
    }

    public SemanticAnalysisHelper(RestServiceConfiguration serviceConfiguration, Lang lang) {
        this.serviceBaseUrl = serviceConfiguration.getUrl();
        this.lang = lang;
    }

    private JSONObject prepareRequest(String text) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("text", text);
        requestBody.put("lang", lang.getLanguageCode());
        return requestBody;
    }

    // TODO
    public Annotation annotate(String text) {
        String requestBody = prepareRequest(text).toString();
        ResponseEntity<String> responseEntity = RestServiceConnector.postJson(this.serviceBaseUrl + RestConstants.NLP_ANNOTATIONS, requestBody, String.class);
        String response = responseEntity.getBody();
        System.out.println(response);
        ProtobufAnnotationSerializer serializer = new ProtobufAnnotationSerializer();
        Annotation annotation = null;
        try {
            assert response != null;
            ByteArrayInputStream is = new ByteArrayInputStream(response.getBytes());
            Pair<Annotation, InputStream> pair = serializer.read(is);
            annotation = pair.first();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return annotation;
    }

    public Map<String, String> getPosTags(String text) {
        String requestBody = prepareRequest(text).toString();
        ResponseEntity<Map> posMapping = RestServiceConnector.postJson(this.serviceBaseUrl + RestConstants.NLP_POS_TAGS, requestBody, Map.class);
        return (Map<String, String>) posMapping.getBody();
    }

    public String removeQuestionWords(String question) {
        String requestBody = prepareRequest(question).toString();
        ResponseEntity<String> posMapping = RestServiceConnector.postJson(this.serviceBaseUrl + RestConstants.NLP_CLEAN_QUESTION_WORDS, requestBody, String.class);
        return posMapping.getBody();
    }

    public SemanticGraph extractDependencyGraph(String text) {
        String requestBody = prepareRequest(text).toString();
        ResponseEntity<String> posMapping = RestServiceConnector.postJson(this.serviceBaseUrl + RestConstants.NLP_DEPENDENCY_GRAPH, requestBody, String.class);
        return SemanticGraph.valueOf(posMapping.getBody());
    }

    public Map<String, String> getLemmas(String text) {
        String requestBody = prepareRequest(text).toString();
        ResponseEntity<LinkedHashMap> posMapping = RestServiceConnector.postJson(this.serviceBaseUrl + RestConstants.NLP_LEMMAS, requestBody, LinkedHashMap.class);
        return (Map<String, String>) posMapping.getBody();
    }

    public QueryType mapQuestionToQueryType(String question) {
        String requestBody = prepareRequest(question).toString();
        ResponseEntity<String> posMapping = RestServiceConnector.postJson(this.serviceBaseUrl + RestConstants.NLP_QUERY_TYPE, requestBody, String.class);
        return QueryType.forCode(posMapping.getBody());
    }

    public QuestionAnswerType detectQuestionAnswerType(String question) {
        String requestBody = prepareRequest(question).toString();
        ResponseEntity<String> posMapping = RestServiceConnector.postJson(this.serviceBaseUrl + RestConstants.NLP_QUESTION_ANSWER_TYPE, requestBody, String.class);
        return QuestionAnswerType.forCode(posMapping.getBody());
    }

    public static void main(String[] args) throws IOException {
        SemanticAnalysisHelper helper = new SemanticAnalysisHelper(new RestServiceConfiguration("http", "tebaqa.cs.upb.de", "8085"), Lang.EN);
        String q = "Who is the president of the United States?";
//        System.out.println(helper.detectQuestionAnswerType(q));
//        System.out.println(helper.mapQuestionToQueryType(q));
//        System.out.println(helper.getLemmas(q));
//        System.out.println(helper.getPosTags(q));
//        System.out.println(helper.removeQuestionWords(q));
//        System.out.println(helper.extractDependencyGraph(q));
        System.out.println(helper.annotate("We are all children of the same God."));

    }

}

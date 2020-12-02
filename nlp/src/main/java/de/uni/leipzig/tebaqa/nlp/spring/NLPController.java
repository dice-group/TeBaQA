package de.uni.leipzig.tebaqa.nlp.spring;

import de.uni.leipzig.tebaqa.nlp.core.NLPAnalyzer;
import de.uni.leipzig.tebaqa.nlp.core.NLPLang;
import de.uni.leipzig.tebaqa.nlp.model.NLPRequestBody;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.JSONOutputter;
import edu.stanford.nlp.pipeline.ProtobufAnnotationSerializer;
import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
public class NLPController {

    private static final Logger LOGGER = Logger.getLogger(NLPController.class.getName());
    private static final Map<NLPLang, NLPAnalyzer> instances = new HashMap<>();
    private static final JSONOutputter JSON_OUTPUTTER = new JSONOutputter();
    ProtobufAnnotationSerializer serializer = new ProtobufAnnotationSerializer();

    // TODO
    @PostMapping(value = "/annotations", produces = {"application/x-protobuf"})
    public String annotate(@RequestBody NLPRequestBody body) throws IOException {
        String text = body.getText();
        NLPLang lang = NLPLang.getForCode(body.getLang());

        LOGGER.info(String.format("/annotations received POST request with: text='%s' & lang=%s", text, lang));
        if (text == null || text.trim().isEmpty() || lang == null) {
            LOGGER.error("Received request with invalid parameter(s)!");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid parameters provided!");
        }

        NLPAnalyzer semanticAnalysisHelper = getFor(lang);
        Annotation annotation = semanticAnalysisHelper.annotate(text);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        serializer.write(annotation, os);
        return os.toString();
    }

    @PostMapping(value = "/pos-tags", produces = {"application/json"})
    public Map<String, String> getPosTags(@RequestBody NLPRequestBody body) {
        String text = body.getText();
        NLPLang lang = NLPLang.getForCode(body.getLang());

        LOGGER.info(String.format("/getPosTags received POST request with: text='%s' & lang=%s", text, lang));
        if (text == null || text.trim().isEmpty() || lang == null) {
            LOGGER.error("Received request with invalid parameter(s)!");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid parameters provided!");
        }

        NLPAnalyzer semanticAnalysisHelper = getFor(lang);
        return semanticAnalysisHelper.getPosTags(text);
    }

    @PostMapping(value = "/cleaned-question", produces = {"application/json"})
    public String removeQuestionWords(@RequestBody NLPRequestBody body) {
        String text = body.getText();
        NLPLang lang = NLPLang.getForCode(body.getLang());

        LOGGER.info(String.format("/removeQuestionWords received POST request with: text='%s' & lang=%s", text, lang));
        if (text == null || text.trim().isEmpty() || lang == null) {
            LOGGER.error("Received request with invalid parameter(s)!");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid parameters provided!");
        }

        NLPAnalyzer semanticAnalysisHelper = getFor(lang);
        return semanticAnalysisHelper.removeQuestionWords(text);
    }

    @PostMapping(
            value = "/dependency-graph",
            produces = {"application/json"}
    )
    public String extractDependencyGraph(@RequestBody NLPRequestBody body) {
        String text = body.getText();
        NLPLang lang = NLPLang.getForCode(body.getLang());

        LOGGER.info(String.format("/extractDependencyGraph received POST request with: text='%s' & lang=%s", text, lang));
        if (text == null || text.trim().isEmpty() || lang == null) {
            LOGGER.error("Received request with invalid parameter(s)!");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid parameters provided!");
        }

        NLPAnalyzer semanticAnalysisHelper = getFor(lang);
        return semanticAnalysisHelper.extractDependencyGraph(text).toCompactString();
    }

    @PostMapping(
            value = "/lemmas",
            produces = {"application/json"}
    )
    public Map<String, String> getLemmas(@RequestBody NLPRequestBody body) {
        String text = body.getText();
        NLPLang lang = NLPLang.getForCode(body.getLang());

        LOGGER.info(String.format("/getLemmas received POST request with: text='%s' & lang=%s", text, lang));
        if (text == null || text.trim().isEmpty() || lang == null) {
            LOGGER.error("Received request with invalid parameter(s)!");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid parameters provided!");
        }

        NLPAnalyzer semanticAnalysisHelper = getFor(lang);
        return semanticAnalysisHelper.getLemmas(text);
    }

    @PostMapping(
            value = "/query-type",
            produces = {"application/json"}
    )
    public String mapQuestionToQueryType(@RequestBody NLPRequestBody body) {
        String text = body.getText();
        NLPLang lang = NLPLang.getForCode(body.getLang());

        LOGGER.info(String.format("/mapQuestionToQueryType received POST request with: text='%s' & lang=%s", text, lang));
        if (text == null || text.trim().isEmpty() || lang == null) {
            LOGGER.error("Received request with invalid parameter(s)!");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid parameters provided!");
        }

        NLPAnalyzer semanticAnalysisHelper = getFor(lang);
        return semanticAnalysisHelper.mapQuestionToQueryType(text).getCode();
    }

    @PostMapping(
            value = "/answer-type",
            produces = {"application/json"}
    )
    public String detectQuestionAnswerType(@RequestBody NLPRequestBody body) {
        String text = body.getText();
        NLPLang lang = NLPLang.getForCode(body.getLang());

        LOGGER.info(String.format("/detectQuestionAnswerType received POST request with: text='%s' & lang=%s", text, lang));
        if (text == null || text.trim().isEmpty() || lang == null) {
            LOGGER.error("Received request with invalid parameter(s)!");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid parameters provided!");
        }

        NLPAnalyzer semanticAnalysisHelper = getFor(lang);
        return semanticAnalysisHelper.detectQuestionAnswerType(text).getCode();
    }

    private NLPAnalyzer getFor(NLPLang lang) {
        NLPAnalyzer analysisHelper = instances.get(lang);
        if (analysisHelper == null) {
            analysisHelper = lang.getSemanticAnalysisHelper();
            instances.put(lang, analysisHelper);
        }
        return analysisHelper;
    }
}

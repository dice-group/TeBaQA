package de.uni.leipzig.tebaqa.tebaqacommons.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.uni.leipzig.tebaqa.tebaqacommons.model.PropertyCandidate;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;

public class JSONUtils {

    public static String convertToJSONString(Object object) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(object);
    }

    public static <T> T JSONStringToObject(String JSONString, Class<T> clazz) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper.readValue(JSONString, clazz);
    }

    // Fails silently
    public static <T> T safeDeepCopy(T object, Class<T> clazz) {
        try {
            return deepCopy(object, clazz);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <T> T deepCopy(T object, Class<T> clazz) throws IOException {
        return JSONStringToObject(convertToJSONString(object), clazz);
    }
}

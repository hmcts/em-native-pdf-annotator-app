package uk.gov.hmcts.reform.em.npa.functional.bdd.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;

public class JsonUtils {

    private JsonUtils() {}

    private static final ObjectMapper mapper = new ObjectMapper();

    private static ClassLoader classLoader = JsonUtils.class.getClassLoader();

    public static String getJsonInput(String testName) throws IOException {

        File testFile = getTestFile(testName);
        return mapper.writeValueAsString(mapper.readValue(testFile, Object.class));
    }

    public static <T> T toObjectFromTestName(String testName, Class<T> clazz) throws IOException {

        return mapper.readValue(getJsonInput(testName), clazz);
    }

    public static <T> T toObjectFromJson(String json, Class<T> clazz) throws IOException {
        return mapper.readValue(json, clazz);
    }

    public static String toJson(Object obj) throws JsonProcessingException {
        return mapper.writeValueAsString(obj);
    }

    private static File getTestFile(String testName) throws FileNotFoundException {
        String filename = "json/" + testName + ".json";
        File file = new File(Objects.requireNonNull(classLoader.getResource(filename)).getFile());

        if (!file.exists()) {
            throw new FileNotFoundException("Unable to find file " + filename);
        }
        return file;
    }
}

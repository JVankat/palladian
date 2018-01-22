package ws.palladian.classifiers.cloudservices;

import java.io.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.io.IOUtils;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;

/**
 * Google Cloud Vision API for label detection (using large generic model).
 * 
 * @see https://cloud.google.com/vision/docs/
 * @author David Urbansky
 */
public class GoogleCloudVision {

    private final String apiKey;

    public GoogleCloudVision(String apiKey) {
        this.apiKey = apiKey;
    }

    public List<String> classify(File image) throws IOException {
        return classify(image, 10);
    }

    public List<String> classify(File image, int maxNumberOfLabels) throws IOException {

        List<String> labels = new ArrayList<>();

        if (maxNumberOfLabels < 1) {
            maxNumberOfLabels = 1;
        }

        HttpClient httpClient = new HttpClient();

        String body = "{\"requests\":[{\"image\":{\"content\":\"XXX\"},\"features\":[{" + "\"type\":\"LABEL_DETECTION\",\"maxResults\":" + maxNumberOfLabels + "}]}]}";

        FileInputStream fileInputStreamReader = new FileInputStream(image);
        byte[] bytes = new byte[(int)image.length()];
        fileInputStreamReader.read(bytes);
        String encodedFile = Base64.getEncoder().encodeToString(bytes);
        body = body.replace("XXX", encodedFile);

        PostMethod post = new PostMethod("https://vision.googleapis.com/v1/images:annotate?key=" + apiKey);
        post.setRequestEntity(new StringRequestEntity(body.toString()));

        try {
            httpClient.executeMethod(post);
            String methodResult = IOUtils.toString(post.getResponseBodyAsStream());

            // System.out.println(methodResult);

            JsonObject responseJson = new JsonObject(methodResult);
            JsonArray responses = responseJson.tryGetJsonArray("responses");
            JsonArray labelAnnotations = responses.tryGetJsonObject(0).tryGetJsonArray("labelAnnotations");

            for (int i = 0; i < labelAnnotations.size(); i++) {
                labels.add(labelAnnotations.tryGetJsonObject(i).tryGetString("description"));
            }

        } catch (JsonException e) {
            e.printStackTrace();
        } finally {
            post.releaseConnection();
        }

        return labels;
    }

    public String recognizeText(File image) throws IOException {

        HttpClient httpClient = new HttpClient();

        String body = "{\"requests\": [{\"image\":{\"content\":\"XXX\"},\"features\": [{\"type\": \"DOCUMENT_TEXT_DETECTION\"}]}]}";

        FileInputStream fileInputStreamReader = new FileInputStream(image);
        byte[] bytes = new byte[(int)image.length()];
        fileInputStreamReader.read(bytes);
        String encodedFile = Base64.getEncoder().encodeToString(bytes);
        body = body.replace("XXX", encodedFile);

                PostMethod post = new PostMethod("https://vision.googleapis.com/v1/images:annotate?key=" + apiKey);
        post.setRequestEntity(new StringRequestEntity(body.toString()));

        try {
            httpClient.executeMethod(post);
            String methodResult = IOUtils.toString(post.getResponseBodyAsStream());

            // System.out.println(methodResult);

            JsonObject responseJson = new JsonObject(methodResult);
            JsonArray responses = responseJson.tryGetJsonArray("responses");
            return responses.tryGetJsonObject(0).tryGetJsonObject("fullTextAnnotation").tryGetString("text");

        } catch (JsonException e) {
            e.printStackTrace();
        } finally {
            post.releaseConnection();
        }

        return "";
    }

    public static void main(String... args) throws Exception {
        GoogleCloudVision cv = new GoogleCloudVision("apiKey");

        // clasification
        List<String> labels = cv.classify(new File("dog.jpg"), 10);
        CollectionHelper.print(labels);

        // ocr
        System.out.println(cv.recognizeText(new File("data/temp/menu.jpg")));
    }
}
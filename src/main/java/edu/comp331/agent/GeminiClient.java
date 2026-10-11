package edu.comp331.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class GeminiClient {
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private final String apiKey;
    private final String model;

    public GeminiClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    public String model() {
        return model;
    }

    public LlmResponse send(List<Message> messages) throws IOException, InterruptedException {
        String body = buildRequestBody(messages);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + model + ":generateContent"))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Gemini API error " + response.statusCode() + ": " + response.body());
        }
        return parseResponse(response.body());
    }

    static String buildRequestBody(List<Message> messages) throws IOException {
        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode contents = root.putArray("contents");
        for (Message m : messages) {
            ObjectNode c = contents.addObject();
            c.put("role", m.role());
            c.putArray("parts").addObject().put("text", m.content());
        }
        return MAPPER.writeValueAsString(root);
    }

    static LlmResponse parseResponse(String json) throws IOException {
        JsonNode root = MAPPER.readTree(json);
        JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (textNode.isMissingNode()) {
            throw new IOException("No text in response: " + json);
        }
        JsonNode usage = root.path("usageMetadata");
        return new LlmResponse(
                textNode.asText(),
                usage.path("promptTokenCount").asInt(-1),
                usage.path("candidatesTokenCount").asInt(-1),
                usage.path("totalTokenCount").asInt(-1));
    }
}

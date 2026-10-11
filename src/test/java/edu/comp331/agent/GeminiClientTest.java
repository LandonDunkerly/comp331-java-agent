package edu.comp331.agent;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeminiClientTest {
    @Test
    void parsesTextAndTokens() throws Exception {
        String json = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"hi\"}]}}],"
                + "\"usageMetadata\":{\"promptTokenCount\":3,\"candidatesTokenCount\":1,\"totalTokenCount\":4}}";
        LlmResponse r = GeminiClient.parseResponse(json);
        assertEquals("hi", r.text());
        assertEquals(4, r.totalTokens());
    }

    @Test
    void buildsRequestWithRoleAndText() throws Exception {
        String body = GeminiClient.buildRequestBody(List.of(Message.user("hello")));
        assertTrue(body.contains("\"role\":\"user\""));
        assertTrue(body.contains("\"text\":\"hello\""));
    }
}

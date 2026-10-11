package edu.comp331.agent;

import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final String DEFAULT_MODEL = "gemini-3.8-flash";

    public static void main(String[] args) throws Exception {
        boolean verbose = false;
        String model = System.getenv().getOrDefault("GEMINI_MODEL", DEFAULT_MODEL);
        List<String> promptWords = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--verbose", "-v" -> verbose = true;
                case "--model" -> {
                    if (i + 1 >= args.length) {
                        System.err.println("--model requires a value");
                        System.exit(2);
                    }
                    model = args[++i];
                }
                default -> promptWords.add(args[i]);
            }
        }

           String prompt = "Say hello in one sentence.";
        if (prompt.isEmpty()) {
            System.err.println("Usage: mvn exec:java -Dexec.args=\"[--verbose] [--model NAME] <prompt>\"");
            System.exit(2);
        }

        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("GEMINI_API_KEY is not set. See README for setup.");
            System.exit(1);
        }

        List<Message> messages = List.of(Message.user(prompt));
        GeminiClient client = new GeminiClient(apiKey, model);

        if (verbose) {
            System.err.println("[verbose] model:  " + client.model());
            System.err.println("[verbose] prompt: " + prompt);
            System.err.println("[verbose] messages: " + messages);
        }

        LlmResponse response = client.send(messages);
        System.out.println(response.text());

        if (verbose) {
            System.err.printf("[verbose] tokens: prompt=%d output=%d total=%d%n",
                    response.promptTokens(), response.outputTokens(), response.totalTokens());
        }
    }
}

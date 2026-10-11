   package edu.comp331.agent;

   import java.util.ArrayList;
   import java.util.List;

   public class Main {
        private static final String MODEL = "gemini-3.5-flash-lite";

       public static void main(String[] args) throws Exception {
           boolean verbose = false;
           List<String> promptWords = new ArrayList<>();

           for (String arg : args) {
               if (arg.equals("--verbose")) {
                   verbose = true;
               } else {
                   promptWords.add(arg);
               }
           }

           String prompt = String.join(" ", promptWords).trim();
           if (prompt.isEmpty()) {
               System.err.println("Usage: mvn exec:java -Dexec.args=\"[--verbose] <prompt>\"");
               System.exit(2);
           }

           String apiKey = System.getenv("GEMINI_API_KEY");
           if (apiKey == null || apiKey.isBlank()) {
               System.err.println("GEMINI_API_KEY is not set.");
               System.exit(1);
           }

           List<Message> messages = List.of(Message.user(prompt));
           GeminiClient client = new GeminiClient(apiKey, MODEL);

           if (verbose) {
               System.err.println("[verbose] model:  " + client.model());
               System.err.println("[verbose] prompt: " + prompt);
           }

           LlmResponse response = client.send(messages);
           System.out.println(response.text());

           if (verbose) {
               System.err.printf("[verbose] tokens: prompt=%d output=%d total=%d%n",
                       response.promptTokens(), response.outputTokens(), response.totalTokens());
           }
       }
   }
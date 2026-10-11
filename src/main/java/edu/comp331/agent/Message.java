package edu.comp331.agent;

public record Message(String role, String content) {
    public static Message user(String content) {
        return new Message("user", content);
    }

    public static Message model(String content) {
        return new Message("model", content);
    }
}

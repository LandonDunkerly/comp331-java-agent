package edu.comp331.agent;


public record LlmResponse(String text, int promptTokens, int outputTokens, int totalTokens) {}

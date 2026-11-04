package edu.comp331.agent.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class ReadFileTool implements AgentTool {
    public static final int MAX_CHARS = 10_000;

    private final Sandbox sandbox;

    public ReadFileTool(Sandbox sandbox) {
        this.sandbox = sandbox;
    }

    @Override
    public String name() {
        return "read_file";
    }

    @Override
    public String description() {
        return "Reads a UTF-8 text file inside the sandbox and returns its content, cut off at "
                + MAX_CHARS + " characters. Required argument 'file_path' is relative to the sandbox root.";
    }

    @Override
    public String execute(Map<String, String> args) {
        String filePath = args == null ? null : args.get("file_path");
        if (filePath == null || filePath.isBlank()) {
            return "Error: Missing required argument 'file_path'";
        }
        Path file;
        try {
            file = sandbox.resolve(filePath);
        } catch (IllegalArgumentException e) {
            return "Error: " + e.getMessage();
        } catch (IOException e) {
            return "Error: Could not resolve path: " + filePath;
        }
        if (!Files.exists(file)) {
            return "Error: File not found: " + filePath;
        }
        if (!Files.isRegularFile(file)) {
            return "Error: Not a file: " + filePath;
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            char[] buffer = new char[MAX_CHARS + 1];
            int total = 0;
            int read;
            while (total < buffer.length && (read = reader.read(buffer, total, buffer.length - total)) != -1) {
                total += read;
            }
            if (total > MAX_CHARS) {
                return new String(buffer, 0, MAX_CHARS)
                        + "\n[File \"" + filePath + "\" truncated at " + MAX_CHARS + " characters]";
            }
            return new String(buffer, 0, total);
        } catch (CharacterCodingException e) {
            return "Error: File is not valid UTF-8 text: " + filePath;
        } catch (IOException e) {
            return "Error: Could not read file: " + filePath;
        }
    }
}

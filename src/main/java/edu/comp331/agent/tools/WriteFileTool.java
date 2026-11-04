package edu.comp331.agent.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class WriteFileTool implements AgentTool {
    private final Sandbox sandbox;

    public WriteFileTool(Sandbox sandbox) {
        this.sandbox = sandbox;
    }

    @Override
    public String name() {
        return "write_file";
    }

    @Override
    public String description() {
        return "Creates or overwrites a UTF-8 text file inside the sandbox, creating parent directories as needed. "
                + "Required arguments: 'file_path' (relative to the sandbox root) and 'content'.";
    }

    @Override
    public String execute(Map<String, String> args) {
        String filePath = args == null ? null : args.get("file_path");
        String content = args == null ? null : args.get("content");
        if (filePath == null || filePath.isBlank()) {
            return "Error: Missing required argument 'file_path'";
        }
        if (content == null) {
            return "Error: Missing required argument 'content'";
        }
        Path file;
        try {
            file = sandbox.resolve(filePath);
        } catch (IllegalArgumentException e) {
            return "Error: " + e.getMessage();
        } catch (IOException e) {
            return "Error: Could not resolve path: " + filePath;
        }
        if (Files.isSymbolicLink(file)) {
            return "Error: Refusing to write through a symbolic link: " + filePath;
        }
        if (Files.isDirectory(file)) {
            return "Error: Not a file: " + filePath;
        }
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(file, content, StandardCharsets.UTF_8);
            return "Wrote " + content.length() + " characters to " + filePath;
        } catch (IOException e) {
            return "Error: Could not write file: " + filePath;
        }
    }
}

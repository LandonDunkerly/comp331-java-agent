package edu.comp331.agent.tools;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class ListFilesTool implements AgentTool {
    private final Sandbox sandbox;

    public ListFilesTool(Sandbox sandbox) {
        this.sandbox = sandbox;
    }

    @Override
    public String name() {
        return "list_files";
    }

    @Override
    public String description() {
        return "Lists files and directories inside the sandbox with size and whether each is a directory. "
                + "Optional argument 'directory' is a path relative to the sandbox root (default '.').";
    }

    @Override
    public String execute(Map<String, String> args) {
        String directory = args == null ? null : args.get("directory");
        if (directory == null) {
            directory = ".";
        }
        Path dir;
        try {
            dir = sandbox.resolve(directory);
        } catch (IllegalArgumentException e) {
            return "Error: " + e.getMessage();
        } catch (IOException e) {
            return "Error: Could not resolve path: " + directory;
        }
        if (!Files.exists(dir)) {
            return "Error: Directory not found: " + directory;
        }
        if (!Files.isDirectory(dir)) {
            return "Error: Not a directory: " + directory;
        }
        List<Path> entries = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                entries.add(entry);
            }
            entries.sort(Comparator.comparing(p -> p.getFileName().toString()));
            if (entries.isEmpty()) {
                return "(empty directory)";
            }
            List<String> lines = new ArrayList<>();
            for (Path entry : entries) {
                BasicFileAttributes attrs =
                        Files.readAttributes(entry, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                lines.add("- " + entry.getFileName() + ": file_size=" + attrs.size()
                        + " bytes, is_dir=" + attrs.isDirectory());
            }
            return String.join("\n", lines);
        } catch (IOException e) {
            return "Error: Could not list directory: " + directory;
        }
    }
}

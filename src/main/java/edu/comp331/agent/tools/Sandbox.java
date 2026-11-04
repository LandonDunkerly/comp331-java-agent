package edu.comp331.agent.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class Sandbox {
    private final Path root;

    public Sandbox(Path root) throws IOException {
        this.root = Objects.requireNonNull(root, "root").toRealPath();
    }

    public Path root() {
        return root;
    }

    public Path resolve(String userPath) throws IOException {
        if (userPath == null || userPath.isBlank()) {
            throw new IllegalArgumentException("Path is required");
        }
        Path target = root.resolve(userPath).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Path is outside the sandbox: " + userPath);
        }
        Path existing = target;
        while (existing != null && !Files.exists(existing)) {
            existing = existing.getParent();
        }
        if (existing != null && !existing.toRealPath().startsWith(root)) {
            throw new IllegalArgumentException("Path escapes the sandbox through a link: " + userPath);
        }
        return target;
    }
}
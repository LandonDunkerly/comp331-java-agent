package edu.comp331.agent.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WriteFileToolTest {

    @TempDir
    Path base;

    Path root;
    Path outside;
    WriteFileTool tool;

    @BeforeEach
    void setUp() throws IOException {
        Path realBase = base.toRealPath();
        root = Files.createDirectory(realBase.resolve("calculator"));
        outside = Files.createDirectory(realBase.resolve("outside"));
        Files.createDirectory(realBase.resolve("calculator-evil"));
        Files.writeString(root.resolve("README.md"), "hello");
        Files.createDirectory(root.resolve("docs"));
        Files.writeString(outside.resolve("secret.txt"), "OUTSIDE-CONTENT");
        tool = new WriteFileTool(new Sandbox(root));
    }

    private String write(String filePath, String content) {
        return tool.execute(Map.of("file_path", filePath, "content", content));
    }

    private void symlinkOrSkip(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            assumeTrue(false, "symbolic links are not available here");
        }
    }

    @Test
    void nameIsWriteFile() {
        assertEquals("write_file", tool.name());
        assertFalse(tool.description().isBlank());
    }

    @Test
    void createsNewFile() throws IOException {
        assertEquals("Wrote 5 characters to hello.txt", write("hello.txt", "world"));
        assertEquals("world", Files.readString(root.resolve("hello.txt")));
    }

    @Test
    void overwritesExistingFileInsteadOfAppending() throws IOException {
        assertTrue(write("README.md", "new").startsWith("Wrote"));
        assertEquals("new", Files.readString(root.resolve("README.md")));
    }

    @Test
    void createsMissingParentDirectories() throws IOException {
        assertTrue(write("a/b/c.txt", "deep").startsWith("Wrote"));
        assertEquals("deep", Files.readString(root.resolve("a/b/c.txt")));
    }

    @Test
    void writesEmptyContent() throws IOException {
        assertTrue(write("empty.txt", "").startsWith("Wrote 0 characters"));
        assertEquals("", Files.readString(root.resolve("empty.txt")));
    }

    @Test
    void writesAbsolutePathInsideRoot() throws IOException {
        assertTrue(write(root.resolve("abs.txt").toString(), "abs").startsWith("Wrote"));
        assertEquals("abs", Files.readString(root.resolve("abs.txt")));
    }

    @Test
    void keepsMultiLineAndUnicodeContentUnchanged() throws IOException {
        String content = "h\u00e9llo\nw\u00f6rld";
        write("text.txt", content);
        assertEquals(content, Files.readString(root.resolve("text.txt")));
    }

    @Test
    void missingFilePathReturnsError() {
        assertTrue(tool.execute(Map.of("content", "x")).startsWith("Error:"));
        assertTrue(tool.execute(null).startsWith("Error:"));
    }

    @Test
    void missingContentReturnsErrorAndWritesNothing() {
        assertTrue(tool.execute(Map.of("file_path", "x.txt")).startsWith("Error:"));
        assertFalse(Files.exists(root.resolve("x.txt")));
    }

    @Test
    void blankFilePathReturnsError() {
        assertTrue(write("   ", "x").startsWith("Error:"));
    }

    @Test
    void directoryTargetReturnsError() {
        String out = write("docs", "x");
        assertTrue(out.startsWith("Error:"));
        assertTrue(out.contains("Not a file"));
    }

    @Test
    void rootTargetReturnsError() {
        assertTrue(write(".", "x").startsWith("Error:"));
    }

    @Test
    void parentTraversalWritesNothingOutside() {
        assertTrue(write("../evil.txt", "pwned").startsWith("Error:"));
        assertFalse(Files.exists(root.resolveSibling("evil.txt")));
    }

    @Test
    void deepTraversalWritesNothingOutside() {
        assertTrue(write("docs/../../evil.txt", "pwned").startsWith("Error:"));
        assertFalse(Files.exists(root.resolveSibling("evil.txt")));
    }

    @Test
    void absolutePathOutsideWritesNothing() {
        assertTrue(write(outside.resolve("new.txt").toString(), "pwned").startsWith("Error:"));
        assertFalse(Files.exists(outside.resolve("new.txt")));
    }

    @Test
    void siblingDirectoryWithSharedPrefixWritesNothing() {
        assertTrue(write("../calculator-evil/x.txt", "pwned").startsWith("Error:"));
        assertFalse(Files.exists(root.resolveSibling("calculator-evil").resolve("x.txt")));
    }

    @Test
    void symlinkToFileOutsideIsNotOverwritten() throws IOException {
        symlinkOrSkip(root.resolve("escape-file"), outside.resolve("secret.txt"));
        assertTrue(write("escape-file", "pwned").startsWith("Error:"));
        assertEquals("OUTSIDE-CONTENT", Files.readString(outside.resolve("secret.txt")));
    }

    @Test
    void writingUnderSymlinkedDirectoryOutsideWritesNothing() {
        symlinkOrSkip(root.resolve("escape-dir"), outside);
        assertTrue(write("escape-dir/new.txt", "pwned").startsWith("Error:"));
        assertFalse(Files.exists(outside.resolve("new.txt")));
    }

    @Test
    void danglingSymlinkToOutsideFileWritesNothing() {
        symlinkOrSkip(root.resolve("dangling-file"), outside.resolve("created-by-link.txt"));
        assertTrue(write("dangling-file", "pwned").startsWith("Error:"));
        assertFalse(Files.exists(outside.resolve("created-by-link.txt")));
    }

    @Test
    void danglingSymlinkedDirectoryOutsideWritesNothing() {
        symlinkOrSkip(root.resolve("dangling-dir"), outside.resolve("missing-dir"));
        assertTrue(write("dangling-dir/x.txt", "pwned").startsWith("Error:"));
        assertFalse(Files.exists(outside.resolve("missing-dir")));
    }

    @Test
    void symlinkInsideSandboxIsRefusedAndTargetUnchanged() throws IOException {
        symlinkOrSkip(root.resolve("readme-link"), root.resolve("README.md"));
        String out = write("readme-link", "changed");
        assertTrue(out.startsWith("Error:"));
        assertTrue(out.contains("symbolic link"));
        assertEquals("hello", Files.readString(root.resolve("README.md")));
    }
    
    @Test
    void absolutePathWithDotDotIntoOutsideWritesNothing() {
        assertTrue(write(root + "/../outside/escape.txt", "pwned").startsWith("Error:"));
        assertFalse(Files.exists(outside.resolve("escape.txt")));
    }

    @Test
    void absolutePathToSiblingDirectoryWritesNothing() {
        Path target = root.resolveSibling("calculator-evil").resolve("x.txt");
        assertTrue(write(target.toString(), "pwned").startsWith("Error:"));
        assertFalse(Files.exists(target));
    }

    @Test
    void symlinkFollowedByDotDotWritesInsideSandboxOnly() throws IOException {
        symlinkOrSkip(root.resolve("link-out"), outside);
        assertTrue(write("link-out/../safe.txt", "ok").startsWith("Wrote"));
        assertEquals("ok", Files.readString(root.resolve("safe.txt")));
        assertFalse(Files.exists(outside.resolve("safe.txt")));
    }
}

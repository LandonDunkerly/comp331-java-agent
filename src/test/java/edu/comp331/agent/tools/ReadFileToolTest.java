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

class ReadFileToolTest {

    @TempDir
    Path base;

    Path root;
    Path outside;
    ReadFileTool tool;

    @BeforeEach
    void setUp() throws IOException {
        Path realBase = base.toRealPath();
        root = Files.createDirectory(realBase.resolve("calculator"));
        outside = Files.createDirectory(realBase.resolve("outside"));
        Files.writeString(root.resolve("README.md"), "hello");
        Files.createDirectory(root.resolve("docs"));
        Files.writeString(root.resolve("docs/guide.md"), "guide");
        Files.writeString(outside.resolve("secret.txt"), "OUTSIDE-CONTENT");
        tool = new ReadFileTool(new Sandbox(root));
    }

    private String read(String filePath) {
        return tool.execute(Map.of("file_path", filePath));
    }

    private void symlinkOrSkip(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            assumeTrue(false, "symbolic links are not available here");
        }
    }

    @Test
    void nameIsReadFile() {
        assertEquals("read_file", tool.name());
        assertFalse(tool.description().isBlank());
    }

    @Test
    void readsSmallFileInFull() {
        assertEquals("hello", read("README.md"));
    }

    @Test
    void readsNestedFile() {
        assertEquals("guide", read("docs/guide.md"));
    }

    @Test
    void readsAbsolutePathInsideRoot() {
        assertEquals("hello", read(root.resolve("README.md").toString()));
    }

    @Test
    void keepsMultiLineContentUnchanged() throws IOException {
        Files.writeString(root.resolve("lines.txt"), "a\nb\nc");
        assertEquals("a\nb\nc", read("lines.txt"));
    }

    @Test
    void emptyFileReturnsEmptyString() throws IOException {
        Files.writeString(root.resolve("empty.txt"), "");
        assertEquals("", read("empty.txt"));
    }

    @Test
    void fileExactlyAtLimitIsNotTruncated() throws IOException {
        String content = "x".repeat(ReadFileTool.MAX_CHARS);
        Files.writeString(root.resolve("exact.txt"), content);
        assertEquals(content, read("exact.txt"));
    }

    @Test
    void fileOneCharOverLimitIsTruncated() throws IOException {
        Files.writeString(root.resolve("over.txt"), "x".repeat(ReadFileTool.MAX_CHARS + 1));
        String out = read("over.txt");
        assertTrue(out.startsWith("x".repeat(ReadFileTool.MAX_CHARS)));
        assertTrue(out.contains("truncated at " + ReadFileTool.MAX_CHARS + " characters"));
    }

    @Test
    void longFileIsCappedAtLimit() throws IOException {
        Files.writeString(root.resolve("long.txt"), "y".repeat(25_000));
        String out = read("long.txt");
        assertTrue(out.startsWith("y".repeat(ReadFileTool.MAX_CHARS)));
        assertFalse(out.contains("y".repeat(ReadFileTool.MAX_CHARS + 1)));
        assertTrue(out.length() < ReadFileTool.MAX_CHARS + 200);
        assertTrue(out.contains("truncated"));
    }

    @Test
    void missingFileReturnsError() {
        String out = read("nope.txt");
        assertTrue(out.startsWith("Error:"));
        assertTrue(out.contains("not found"));
    }

    @Test
    void directoryInsteadOfFileReturnsError() {
        String out = read("docs");
        assertTrue(out.startsWith("Error:"));
        assertTrue(out.contains("Not a file"));
    }

    @Test
    void missingArgumentReturnsError() {
        assertTrue(tool.execute(Map.of()).startsWith("Error:"));
        assertTrue(tool.execute(null).startsWith("Error:"));
    }

    @Test
    void blankPathReturnsError() {
        assertTrue(read("   ").startsWith("Error:"));
    }

    @Test
    void parentTraversalReturnsErrorAndLeaksNothing() {
        String out = read("../outside/secret.txt");
        assertTrue(out.startsWith("Error:"));
        assertFalse(out.contains("OUTSIDE-CONTENT"));
    }

    @Test
    void absolutePathOutsideReturnsErrorAndLeaksNothing() {
        String out = read(outside.resolve("secret.txt").toString());
        assertTrue(out.startsWith("Error:"));
        assertFalse(out.contains("OUTSIDE-CONTENT"));
    }

    @Test
    void symlinkToFileOutsideReturnsErrorAndLeaksNothing() {
        symlinkOrSkip(root.resolve("escape-file"), outside.resolve("secret.txt"));
        String out = read("escape-file");
        assertTrue(out.startsWith("Error:"));
        assertFalse(out.contains("OUTSIDE-CONTENT"));
    }

    @Test
    void symlinkToFileInsideIsRead() {
        symlinkOrSkip(root.resolve("readme-link"), root.resolve("README.md"));
        assertEquals("hello", read("readme-link"));
    }

    @Test
    void binaryFileReturnsError() throws IOException {
        Files.write(root.resolve("data.bin"), new byte[] {(byte) 0xC3, (byte) 0x28, (byte) 0xFF});
        String out = read("data.bin");
        assertTrue(out.startsWith("Error:"));
        assertTrue(out.contains("not valid UTF-8"));
    }
    
    @Test
    void readsDotSlashRelativePath() {
        assertEquals("hello", read("./README.md"));
    }

    @Test
    void fileThroughSymlinkedDirectoryOutsideReturnsErrorAndLeaksNothing() {
        symlinkOrSkip(root.resolve("escape-dir"), outside);
        String out = read("escape-dir/secret.txt");
        assertTrue(out.startsWith("Error:"));
        assertFalse(out.contains("OUTSIDE-CONTENT"));
    }
}

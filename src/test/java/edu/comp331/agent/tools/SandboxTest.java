package edu.comp331.agent.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SandboxTest {

    @TempDir
    Path base;

    Path root;
    Path outside;
    Sandbox sandbox;

    @BeforeEach
    void setUp() throws IOException {
        Path realBase = base.toRealPath();
        root = Files.createDirectory(realBase.resolve("calculator"));
        outside = Files.createDirectory(realBase.resolve("outside"));
        Files.createDirectory(realBase.resolve("calculator-evil"));
        Files.writeString(root.resolve("README.md"), "hello");
        Files.createDirectory(root.resolve("docs"));
        Files.writeString(root.resolve("docs/guide.md"), "guide");
        Files.writeString(outside.resolve("secret.txt"), "secret");
        sandbox = new Sandbox(root);
    }

    private void assertBlocked(String input) {
        assertThrows(IllegalArgumentException.class, () -> sandbox.resolve(input));
    }

    private void symlinkOrSkip(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            assumeTrue(false, "symbolic links are not available here");
        }
    }

    @Test
    void allowsFileAtRoot() throws IOException {
        assertEquals(root.resolve("README.md"), sandbox.resolve("README.md"));
    }

    @Test
    void allowsDotSlashPrefix() throws IOException {
        assertEquals(root.resolve("README.md"), sandbox.resolve("./README.md"));
    }

    @Test
    void allowsNestedFile() throws IOException {
        assertEquals(root.resolve("docs/guide.md"), sandbox.resolve("docs/guide.md"));
    }

    @Test
    void allowsDotAsRoot() throws IOException {
        assertEquals(root, sandbox.resolve("."));
    }

    @Test
    void allowsRedundantSegmentsThatStayInside() throws IOException {
        assertEquals(root.resolve("README.md"), sandbox.resolve("docs/../README.md"));
    }

    @Test
    void allowsAbsolutePathInsideRoot() throws IOException {
        assertEquals(root.resolve("README.md"), sandbox.resolve(root.resolve("README.md").toString()));
    }

    @Test
    void allowsFileThatDoesNotExistYet() throws IOException {
        assertEquals(root.resolve("new/Out.txt"), sandbox.resolve("new/Out.txt"));
    }

    @Test
    void allowsSymlinkPointingInsideRoot() throws IOException {
        symlinkOrSkip(root.resolve("inside-link"), root.resolve("README.md"));
        assertEquals(root.resolve("inside-link"), sandbox.resolve("inside-link"));
    }

    @Test
    void blocksParentTraversal() {
        assertBlocked("../README.md");
    }

    @Test
    void blocksTraversalThatReentersAfterEscaping() {
        assertBlocked("docs/../../outside/secret.txt");
    }

    @Test
    void blocksDeepTraversal() {
        assertBlocked("a/../../b");
    }

    @Test
    void blocksAbsolutePathOutsideRoot() {
        assertBlocked(outside.resolve("secret.txt").toString());
    }

    @Test
    void blocksAbsoluteTraversalToMissingFile() {
        assertBlocked(root + "/../../nonexistent.txt");
    }

    @Test
    void blocksAbsoluteTraversalToExistingFile() {
        assertBlocked(root + "/../outside/secret.txt");
    }

    @Test
    void blocksSiblingDirectoryWithSharedPrefixRelative() {
        assertBlocked("../calculator-evil/file.txt");
    }

    @Test
    void blocksSiblingDirectoryWithSharedPrefixAbsolute() {
        assertBlocked(root.resolveSibling("calculator-evil").resolve("file.txt").toString());
    }

    @Test
    void blocksNullPath() {
        assertBlocked(null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   ", "\t"})
    void blocksBlankPaths(String input) {
        assertBlocked(input);
    }

    @Test
    void blocksSymlinkToFileOutsideRoot() {
        symlinkOrSkip(root.resolve("escape-file"), outside.resolve("secret.txt"));
        assertBlocked("escape-file");
    }

    @Test
    void blocksNewFileUnderSymlinkedDirectoryOutsideRoot() {
        symlinkOrSkip(root.resolve("escape-dir"), outside);
        assertBlocked("escape-dir/new.txt");
    }
}
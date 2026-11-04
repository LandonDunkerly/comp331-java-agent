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

class ListFilesToolTest {

    @TempDir
    Path base;

    Path root;
    Path outside;
    ListFilesTool tool;

    @BeforeEach
    void setUp() throws IOException {
        Path realBase = base.toRealPath();
        root = Files.createDirectory(realBase.resolve("calculator"));
        outside = Files.createDirectory(realBase.resolve("outside"));
        Files.writeString(root.resolve("README.md"), "hello");
        Files.createDirectory(root.resolve("docs"));
        Files.writeString(root.resolve("docs/guide.md"), "guide");
        Files.writeString(outside.resolve("secret.txt"), "secret");
        tool = new ListFilesTool(new Sandbox(root));
    }

    private String list(String directory) {
        return tool.execute(Map.of("directory", directory));
    }

    private void symlinkOrSkip(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            assumeTrue(false, "symbolic links are not available here");
        }
    }

    @Test
    void nameIsListFiles() {
        assertEquals("list_files", tool.name());
        assertFalse(tool.description().isBlank());
    }

    @Test
    void listsRootWhenNoDirectoryGiven() {
        String out = tool.execute(Map.of());
        assertTrue(out.contains("- README.md: file_size=5 bytes, is_dir=false"));
        assertTrue(out.contains("- docs: file_size="));
    }

    @Test
    void listsRootWhenArgsAreNull() {
        assertTrue(tool.execute(null).contains("- README.md"));
    }

    @Test
    void listsRootForDot() {
        assertTrue(list(".").contains("- README.md"));
    }

    @Test
    void listsSubdirectory() {
        assertEquals("- guide.md: file_size=5 bytes, is_dir=false", list("docs"));
    }

    @Test
    void marksDirectoriesAndFilesDifferently() {
        String docsLine = tool.execute(Map.of()).lines().filter(l -> l.startsWith("- docs:")).findFirst().orElse("");
        assertTrue(docsLine.endsWith("is_dir=true"));
    }

    @Test
    void entriesAreSortedByName() throws IOException {
        Files.createDirectory(root.resolve("sorted"));
        Files.writeString(root.resolve("sorted/b.txt"), "b");
        Files.writeString(root.resolve("sorted/a.txt"), "a");
        assertEquals("- a.txt: file_size=1 bytes, is_dir=false\n- b.txt: file_size=1 bytes, is_dir=false",
                list("sorted"));
    }

    @Test
    void emptyDirectoryIsReported() throws IOException {
        Files.createDirectory(root.resolve("empty"));
        assertEquals("(empty directory)", list("empty"));
    }

    @Test
    void missingDirectoryReturnsError() {
        String out = list("nope");
        assertTrue(out.startsWith("Error:"));
        assertTrue(out.contains("not found"));
    }

    @Test
    void fileInsteadOfDirectoryReturnsError() {
        String out = list("README.md");
        assertTrue(out.startsWith("Error:"));
        assertTrue(out.contains("Not a directory"));
    }

    @Test
    void parentTraversalReturnsErrorAndLeaksNothing() {
        String out = list("..");
        assertTrue(out.startsWith("Error:"));
        assertFalse(out.contains("- outside:"));
        assertFalse(out.contains("- calculator:"));
    }

    @Test
    void absolutePathOutsideReturnsError() {
        String out = list(outside.toString());
        assertTrue(out.startsWith("Error:"));
        assertFalse(out.contains("secret.txt"));
    }

    @Test
    void blankDirectoryReturnsError() {
        assertTrue(list("   ").startsWith("Error:"));
    }

    @Test
    void symlinkedDirectoryOutsideReturnsError() {
        symlinkOrSkip(root.resolve("escape-dir"), outside);
        String out = list("escape-dir");
        assertTrue(out.startsWith("Error:"));
        assertFalse(out.contains("secret.txt"));
    }
    
    @Test
    void allowsRedundantSegmentsThatStayInside() {
        assertEquals(list("docs"), list("docs/../docs"));
    }

    @Test
    void allowsAbsolutePathInsideRoot() {
        assertEquals(list("docs"), list(root.resolve("docs").toString()));
    }

    @Test
    void allowsSymlinkPointingInsideRoot() {
        symlinkOrSkip(root.resolve("docs-link"), root.resolve("docs"));
        assertEquals(list("docs"), list("docs-link"));
    }
}

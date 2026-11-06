package edu.comp331.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.comp331.agent.tools.RunJavaTestsTool;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ToolDemoTest {

    @TempDir
    Path base;

    Path sandboxDir;
    AtomicInteger runnerCalls;
    RunJavaTestsTool.CommandRunner runner;
    ByteArrayOutputStream captured;
    PrintStream out;

    @BeforeEach
    void setUp() throws IOException {
        sandboxDir = Files.createDirectory(base.toRealPath().resolve("calculator"));
        Files.writeString(sandboxDir.resolve("pom.xml"), "<project/>");
        Path source = sandboxDir.resolve(ToolDemo.READ_PATH);
        Files.createDirectories(source.getParent());
        Files.writeString(source, "public class Calculator {}\n");
        Files.writeString(base.resolve("README.md"), "outside readme");
        runnerCalls = new AtomicInteger();
        runner = (command, directory, timeout) -> {
            runnerCalls.incrementAndGet();
            return new RunJavaTestsTool.Result(0, "Tests run: 3, Failures: 0\nBUILD SUCCESS", false);
        };
        captured = new ByteArrayOutputStream();
        out = new PrintStream(captured, true, StandardCharsets.UTF_8);
    }

    private String output() {
        return captured.toString(StandardCharsets.UTF_8);
    }

    @Test
    void demoRunsEveryStepAndBlocksTheThreeUnsafeCalls() throws IOException {
        assertEquals(0, ToolDemo.run(sandboxDir, runner, out));
        String text = output();
        assertTrue(text.contains("- pom.xml:"));
        assertTrue(text.contains("public class Calculator {}"));
        assertTrue(text.contains("Exit code: 0"));
        assertTrue(text.contains("Demo result: OK"));
        assertEquals(3, text.lines().filter(line -> line.startsWith("Error:")).count());
    }

    @Test
    void demoWritesTheHarmlessFileInsideTheSandboxOnly() throws IOException {
        ToolDemo.run(sandboxDir, runner, out);
        assertEquals(ToolDemo.WRITE_CONTENT, Files.readString(sandboxDir.resolve(ToolDemo.WRITE_PATH)));
        assertFalse(Files.exists(sandboxDir.resolveSibling("escape.txt")));
        assertEquals("outside readme", Files.readString(base.resolve("README.md")));
    }

    @Test
    void demoRunsTheTestToolExactlyOnce() {
        ToolDemo.run(sandboxDir, runner, out);
        assertEquals(1, runnerCalls.get());
    }

    @Test
    void demoFailsCleanlyWhenTheSandboxIsMissing() {
        assertEquals(1, ToolDemo.run(base.resolve("missing"), runner, out));
        assertTrue(output().contains("Sandbox directory not found"));
        assertEquals(0, runnerCalls.get());
    }
}

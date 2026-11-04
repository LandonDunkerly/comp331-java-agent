package edu.comp331.agent.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RunJavaTestsToolTest {

    static final class FakeRunner implements RunJavaTestsTool.CommandRunner {
        int calls;
        List<String> command;
        Path workingDirectory;
        Duration timeout;
        RunJavaTestsTool.Result result = new RunJavaTestsTool.Result(0, "BUILD SUCCESS", false);
        IOException ioFailure;
        InterruptedException interruption;

        @Override
        public RunJavaTestsTool.Result run(List<String> command, Path workingDirectory, Duration timeout)
                throws IOException, InterruptedException {
            calls++;
            this.command = command;
            this.workingDirectory = workingDirectory;
            this.timeout = timeout;
            if (ioFailure != null) {
                throw ioFailure;
            }
            if (interruption != null) {
                throw interruption;
            }
            return result;
        }
    }

    @TempDir
    Path base;

    Path root;
    FakeRunner runner;
    RunJavaTestsTool tool;

    @BeforeEach
    void setUp() throws IOException {
        root = Files.createDirectory(base.toRealPath().resolve("calculator"));
        Files.writeString(root.resolve("pom.xml"), "<project/>");
        runner = new FakeRunner();
        tool = new RunJavaTestsTool(new Sandbox(root), runner);
    }

    private static List<String> javaVersionCommand() {
        return List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-version");
    }

    @Test
    void nameIsRunJavaTests() {
        assertEquals("run_java_tests", tool.name());
        assertFalse(tool.description().isBlank());
    }

    @Test
    void runsOnlyTheFixedMvnTestCommandInTheSandboxRoot() {
        tool.execute(Map.of());
        assertEquals(1, runner.calls);
        assertEquals(RunJavaTestsTool.COMMAND, runner.command);
        assertEquals(List.of("-B", "test"), runner.command.subList(1, 3));
        assertEquals(3, runner.command.size());
        assertEquals(root, runner.workingDirectory);
    }

    @Test
    void passesTheDefaultTimeoutToTheRunner() {
        tool.execute(Map.of());
        assertEquals(RunJavaTestsTool.DEFAULT_TIMEOUT, runner.timeout);
    }

    @Test
    void nullArgsAreAllowed() {
        assertTrue(tool.execute(null).contains("Exit code: 0"));
        assertEquals(1, runner.calls);
    }

    @Test
    void passingRunReportsExitCodeZero() {
        String out = tool.execute(Map.of());
        assertTrue(out.contains("Exit code: 0"));
        assertTrue(out.contains("tests passed"));
        assertTrue(out.contains("BUILD SUCCESS"));
    }

    @Test
    void failingRunReportsNonZeroExitCode() {
        runner.result = new RunJavaTestsTool.Result(1, "BUILD FAILURE", false);
        String out = tool.execute(Map.of());
        assertTrue(out.contains("Exit code: 1"));
        assertTrue(out.contains("tests failed"));
        assertTrue(out.contains("BUILD FAILURE"));
    }

    @Test
    void timeoutReturnsError() {
        runner.result = new RunJavaTestsTool.Result(-1, "partial output", true);
        String out = tool.execute(Map.of());
        assertTrue(out.startsWith("Error:"));
        assertTrue(out.contains("timed out"));
    }

    @Test
    void longOutputKeepsOnlyTheEnd() {
        runner.result = new RunJavaTestsTool.Result(0, "a".repeat(40_000) + "END-MARKER", false);
        String out = tool.execute(Map.of());
        assertTrue(out.contains("END-MARKER"));
        assertTrue(out.contains("truncated"));
        assertTrue(out.length() < RunJavaTestsTool.MAX_OUTPUT_CHARS + 500);
        assertFalse(out.contains("a".repeat(RunJavaTestsTool.MAX_OUTPUT_CHARS + 1)));
    }

    @Test
    void anyArgumentIsRejectedAndNothingIsRun() {
        for (String key : List.of("command", "args", "test", "goal", "file_path")) {
            String out = tool.execute(Map.of(key, "rm -rf /"));
            assertTrue(out.startsWith("Error:"));
        }
        assertEquals(0, runner.calls);
    }

    @Test
    void missingPomReturnsErrorAndNothingIsRun() throws IOException {
        Files.delete(root.resolve("pom.xml"));
        assertTrue(tool.execute(Map.of()).contains("No pom.xml"));
        assertEquals(0, runner.calls);
    }

    @Test
    void startFailureReturnsErrorInsteadOfThrowing() {
        runner.ioFailure = new IOException("Cannot run program \"mvn\"");
        String out = tool.execute(Map.of());
        assertTrue(out.startsWith("Error:"));
        assertTrue(out.contains("Maven"));
    }

    @Test
    void interruptionReturnsErrorAndKeepsTheInterruptFlag() {
        runner.interruption = new InterruptedException("stop");
        String out = tool.execute(Map.of());
        assertTrue(out.startsWith("Error:"));
        assertTrue(Thread.interrupted());
    }

    @Test
    void realRunnerCapturesOutputAndExitCode() throws Exception {
        RunJavaTestsTool.Result result =
                RunJavaTestsTool.processRunner().run(javaVersionCommand(), root, Duration.ofSeconds(30));
        assertEquals(0, result.exitCode());
        assertFalse(result.timedOut());
        assertTrue(result.output().toLowerCase().contains("version"));
    }

    @Test
    void realRunnerStopsAProcessThatExceedsTheTimeout() throws Exception {
        RunJavaTestsTool.Result result =
                RunJavaTestsTool.processRunner().run(javaVersionCommand(), root, Duration.ofMillis(1));
        assertTrue(result.timedOut());
    }

    @Test
    void shellInjectionStringsPassedAsArgumentsAreRejectedAndNothingIsRun() {
        List<String> attempts = List.of(
                "mvn test && echo hacked",
                "mvn test; cat /etc/passwd",
                "mvn test | grep PASS",
                "mvn test $(whoami)",
                "mvn -q test",
                "mvn test -DskipTests");
        for (String attempt : attempts) {
            assertTrue(tool.execute(Map.of("command", attempt)).startsWith("Error:"));
        }
        assertEquals(0, runner.calls);
    }
}

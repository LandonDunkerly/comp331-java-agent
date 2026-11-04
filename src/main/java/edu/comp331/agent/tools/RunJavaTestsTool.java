package edu.comp331.agent.tools;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class RunJavaTestsTool implements AgentTool {
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(120);
    public static final int MAX_OUTPUT_CHARS = 10_000;
    public static final List<String> COMMAND = List.of(mavenExecutable(), "-B", "test");
    private static final int MAX_CAPTURE_CHARS = 200_000;

    public interface CommandRunner {
        Result run(List<String> command, Path workingDirectory, Duration timeout)
                throws IOException, InterruptedException;
    }

    public record Result(int exitCode, String output, boolean timedOut) {
    }

    private final Sandbox sandbox;
    private final CommandRunner runner;

    public RunJavaTestsTool(Sandbox sandbox) {
        this(sandbox, processRunner());
    }

    public RunJavaTestsTool(Sandbox sandbox, CommandRunner runner) {
        this.sandbox = sandbox;
        this.runner = runner;
    }

    @Override
    public String name() {
        return "run_java_tests";
    }

    @Override
    public String description() {
        return "Runs 'mvn test' in the sandbox project and returns the exit code and the end of the output. "
                + "Takes no arguments. No other command can be run.";
    }

    @Override
    public String execute(Map<String, String> args) {
        if (args != null && !args.isEmpty()) {
            return "Error: run_java_tests takes no arguments; it only runs 'mvn test' in the sandbox";
        }
        Path root = sandbox.root();
        if (!Files.isRegularFile(root.resolve("pom.xml"))) {
            return "Error: No pom.xml found in the sandbox";
        }
        Result result;
        try {
            result = runner.run(COMMAND, root, DEFAULT_TIMEOUT);
        } catch (IOException e) {
            return "Error: Could not start 'mvn'. Is Maven installed and on the PATH?";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Error: Test run was interrupted";
        }
        String output = tail(result.output());
        if (result.timedOut()) {
            return "Error: 'mvn test' timed out after " + DEFAULT_TIMEOUT.toSeconds() + " seconds\n" + output;
        }
        String verdict = result.exitCode() == 0 ? "tests passed" : "tests failed";
        return "Command: " + String.join(" ", COMMAND)
                + "\nExit code: " + result.exitCode() + " (" + verdict + ")"
                + "\n--- output ---\n" + output;
    }

    public static CommandRunner processRunner() {
        return RunJavaTestsTool::runProcess;
    }

    private static String mavenExecutable() {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        return windows ? "mvn.cmd" : "mvn";
    }

    private static String tail(String output) {
        if (output.length() <= MAX_OUTPUT_CHARS) {
            return output;
        }
        return "[output truncated, showing last " + MAX_OUTPUT_CHARS + " characters]\n"
                + output.substring(output.length() - MAX_OUTPUT_CHARS);
    }

    private static Result runProcess(List<String> command, Path workingDirectory, Duration timeout)
            throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command)
                .directory(workingDirectory.toFile())
                .redirectErrorStream(true)
                .start();
        StringBuilder captured = new StringBuilder();
        Thread reader = new Thread(() -> {
            try (InputStream in = process.getInputStream()) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    synchronized (captured) {
                        if (captured.length() < MAX_CAPTURE_CHARS) {
                            captured.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
                        }
                    }
                }
            } catch (IOException ignored) {
                return;
            }
        });
        reader.setDaemon(true);
        reader.start();
        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
            process.waitFor();
        }
        reader.join(2000);
        String output;
        synchronized (captured) {
            output = captured.toString();
        }
        return new Result(finished ? process.exitValue() : -1, output, !finished);
    }
}

package edu.comp331.agent;

import edu.comp331.agent.tools.ListFilesTool;
import edu.comp331.agent.tools.ReadFileTool;
import edu.comp331.agent.tools.RunJavaTestsTool;
import edu.comp331.agent.tools.Sandbox;
import edu.comp331.agent.tools.WriteFileTool;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class ToolDemo {
    public static final Path DEFAULT_SANDBOX = Path.of("sandbox", "calculator");
    static final String READ_PATH = "src/main/java/edu/comp331/sandbox/calculator/Calculator.java";
    static final String WRITE_PATH = "notes/agent-demo.txt";
    static final String WRITE_CONTENT = "Written by the tool demo.\n";

    private ToolDemo() {
    }

    public static int run(Path sandboxDir, RunJavaTestsTool.CommandRunner runner, PrintStream out) {
        Sandbox sandbox;
        try {
            sandbox = new Sandbox(sandboxDir);
        } catch (IOException e) {
            out.println("Sandbox directory not found: " + sandboxDir);
            return 1;
        }
        ListFilesTool listFiles = new ListFilesTool(sandbox);
        ReadFileTool readFile = new ReadFileTool(sandbox);
        WriteFileTool writeFile = new WriteFileTool(sandbox);
        RunJavaTestsTool runTests = new RunJavaTestsTool(sandbox, runner);

        out.println("Sandbox: " + sandbox.root());
        boolean ok = true;
        ok &= show(out, 1, "list_files directory=.",
                listFiles.execute(Map.of("directory", ".")), false, 40, 0);
        ok &= show(out, 2, "read_file file_path=" + READ_PATH,
                readFile.execute(Map.of("file_path", READ_PATH)), false, 12, 0);
        ok &= show(out, 3, "write_file file_path=" + WRITE_PATH,
                writeFile.execute(Map.of("file_path", WRITE_PATH, "content", WRITE_CONTENT)), false, 40, 0);
        String readBack = readFile.execute(Map.of("file_path", WRITE_PATH));
        ok &= show(out, 4, "read_file file_path=" + WRITE_PATH + " (read back)", readBack, false, 40, 0);
        ok &= readBack.equals(WRITE_CONTENT);
        ok &= show(out, 5, "BLOCKED read_file file_path=../../README.md",
                readFile.execute(Map.of("file_path", "../../README.md")), true, 40, 0);
        ok &= show(out, 6, "BLOCKED write_file file_path=../escape.txt",
                writeFile.execute(Map.of("file_path", "../escape.txt", "content", "should never be written")),
                true, 40, 0);
        ok &= show(out, 7, "BLOCKED run_java_tests command=\"rm -rf /\"",
                runTests.execute(Map.of("command", "rm -rf /")), true, 40, 0);
        ok &= show(out, 8, "run_java_tests", runTests.execute(Map.of()), false, 3, 12);

        out.println();
        out.println(ok ? "Demo result: OK" : "Demo result: FAILED");
        return ok ? 0 : 1;
    }

    private static boolean show(PrintStream out, int number, String title, String output,
                                boolean expectBlocked, int head, int tail) {
        boolean blocked = output.startsWith("Error:");
        out.println();
        out.println("=== " + number + ". " + title + " ===");
        out.println(preview(output, head, tail));
        return blocked == expectBlocked;
    }

    private static String preview(String text, int head, int tail) {
        List<String> lines = text.lines().toList();
        if (lines.size() <= head + tail) {
            return text;
        }
        StringBuilder shown = new StringBuilder(String.join("\n", lines.subList(0, head)));
        shown.append("\n... (").append(lines.size() - head - tail).append(" more lines) ...");
        if (tail > 0) {
            shown.append("\n").append(String.join("\n", lines.subList(lines.size() - tail, lines.size())));
        }
        return shown.toString();
    }
}

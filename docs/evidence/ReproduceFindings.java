import edu.comp331.agent.tools.Sandbox;
import edu.comp331.agent.tools.WriteFileTool;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReproduceFindings {

    static final class CopilotSandbox {
        private final Path root;

        CopilotSandbox(Path root) {
            this.root = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
        }

        Path resolve(String userPath) {
            if (userPath == null || userPath.isBlank()) {
                throw new IllegalArgumentException("Path is required");
            }
            Path candidate = Path.of(userPath);
            Path normalized = candidate.isAbsolute() ? candidate : root.resolve(candidate).normalize();
            Path realRoot;
            try {
                realRoot = root.toRealPath(LinkOption.NOFOLLOW_LINKS);
            } catch (IOException e) {
                throw new IllegalArgumentException("Invalid sandbox root: " + root, e);
            }
            Path realCandidate;
            try {
                realCandidate = normalized.toRealPath(LinkOption.NOFOLLOW_LINKS);
            } catch (NoSuchFileException e) {
                realCandidate = normalized;
            } catch (IOException e) {
                throw new IllegalArgumentException("Invalid path: " + userPath, e);
            }
            if (!realCandidate.startsWith(realRoot)) {
                throw new IllegalArgumentException("Path escapes sandbox root: " + userPath);
            }
            return realCandidate;
        }
    }

    static String outcome(java.util.concurrent.Callable<Path> call) {
        try {
            return "ALLOWED  -> " + call.call();
        } catch (Exception e) {
            return "blocked  (" + e.getMessage() + ")";
        }
    }

    public static void main(String[] args) throws Exception {
        int seconds = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        Path base = Files.createTempDirectory("repro").toRealPath();
        Path root = Files.createDirectory(base.resolve("calculator"));
        Path outside = Files.createDirectory(base.resolve("outside"));
        Files.writeString(outside.resolve("secret.txt"), "OUTSIDE-CONTENT");
        Files.createSymbolicLink(root.resolve("escape-link"), outside.resolve("secret.txt"));
        Files.createSymbolicLink(root.resolve("outer"), outside);
        CopilotSandbox copilot = new CopilotSandbox(root);
        Sandbox mine = new Sandbox(root);

        System.out.println("=== 1. Copilot's Sandbox versus my Sandbox ===");
        String[][] inputs = {
                {"symlink inside the sandbox pointing to a file outside it", "escape-link"},
                {"absolute path with .. to a file that does not exist", root + "/../../nonexistent.txt"}};
        for (String[] input : inputs) {
            System.out.println(input[0] + " (" + input[1] + ")");
            System.out.println("  Copilot's: " + outcome(() -> copilot.resolve(input[1])));
            System.out.println("  Mine:      " + outcome(() -> mine.resolve(input[1])));
        }

        System.out.println();
        System.out.println("=== 2. Copilot's claimed symlink-plus-.. escape (outer -> outside, input outer/../secret.txt) ===");
        Path resolved = mine.resolve("outer/../secret.txt");
        System.out.println("Sandbox returned: " + resolved);
        System.out.println("Inside the sandbox root: " + resolved.startsWith(mine.root()));

        System.out.println();
        System.out.println("=== 3. Check-then-use race against WriteFileTool (" + seconds + " seconds) ===");
        Path swap = root.resolve("swap");
        WriteFileTool tool = new WriteFileTool(mine);
        AtomicBoolean stop = new AtomicBoolean(false);
        Thread attacker = new Thread(() -> {
            while (!stop.get()) {
                try {
                    Files.deleteIfExists(swap.resolve("x.txt"));
                    Files.deleteIfExists(swap);
                    Files.createDirectory(swap);
                    Files.delete(swap);
                    Files.createSymbolicLink(swap, outside);
                    Files.delete(swap);
                } catch (IOException e) {
                    continue;
                }
            }
        });
        attacker.start();
        long end = System.currentTimeMillis() + seconds * 1000L;
        long attempts = 0;
        long escapes = 0;
        while (System.currentTimeMillis() < end) {
            tool.execute(Map.of("file_path", "swap/x.txt", "content", "ESCAPED"));
            attempts++;
            if (Files.exists(outside.resolve("x.txt"))) {
                escapes++;
                Files.delete(outside.resolve("x.txt"));
            }
        }
        stop.set(true);
        attacker.join();
        System.out.println("write attempts: " + attempts);
        System.out.println("files that landed outside the sandbox: " + escapes);
    }
}

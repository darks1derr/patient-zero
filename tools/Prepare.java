import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import javax.tools.ToolProvider;

/** Personalize the draft without changing the algorithm source files.
 * java tools/Prepare.java EXACT_MOODLE_ID "NAME" "GROUP"
 */
public class Prepare {
    static String run(String... arguments) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        cmd.addAll(List.of(arguments));
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (p.waitFor() != 0) throw new IllegalStateException(output);
        return output;
    }
    static String section(String text, String name, String content) {
        String first = "<!-- " + name + " START -->", last = "<!-- " + name + " END -->";
        int start = text.indexOf(first), end = text.indexOf(last);
        if (start < 0 || end < start) throw new IllegalStateException("README section missing: " + name);
        return text.substring(0, start + first.length()) + "\n" + content + "\n" + text.substring(end);
    }
    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args[0].isBlank() || args[0].equals("YOUR_STUDENT_ID")) {
            System.out.println("Usage: java tools/Prepare.java EXACT_MOODLE_ID \"NAME\" \"GROUP\"");
            return;
        }
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("A JDK 17+ is required.");
        System.out.print(run("tools/MakeVariant.java", args[0], "data"));
        Files.writeString(Path.of("data", "student-id.txt"), args[0] + "\n");
        List<String> options = new ArrayList<>(List.of("-encoding", "UTF-8", "-d", "out"));
        try (var paths = Files.list(Path.of("src"))) {
            paths.filter(p -> p.toString().endsWith(".java")).sorted().forEach(p -> options.add(p.toString()));
        }
        if (compiler.run(null, null, null, options.toArray(new String[0])) != 0)
            throw new IllegalStateException("Compilation failed.");
        String result = run("-cp", "out", "Main");
        System.out.print(result);
        String border = Arrays.stream(result.lines().toArray(String[]::new))
                .filter(line -> line.startsWith("Border length: ")).findFirst().orElseThrow()
                .substring("Border length: ".length());
        String marker = Files.readString(Path.of("data", "markers.txt")).trim().split("\\s+")[1];
        int length = Integer.parseInt(border);
        String explanation = "My marker is `" + marker + "` and its Border length is **" + length + "**. ";
        explanation += length == 0 ? "It has no non-empty proper prefix that is also a suffix."
                : "Its longest proper prefix that is also a suffix is `" + marker.substring(0, length) + "`.";
        Path report = Path.of("README.md");
        String text = Files.readString(report);
        text = text.replaceFirst("(?m)^Student ID:.*$", java.util.regex.Matcher.quoteReplacement("Student ID: " + args[0]));
        if (args.length > 1) text = text.replaceFirst("(?m)^Name:.*$", java.util.regex.Matcher.quoteReplacement("Name: " + args[1]));
        if (args.length > 2) text = text.replaceFirst("(?m)^Group:.*$", java.util.regex.Matcher.quoteReplacement("Group: " + args[2]));
        text = section(text, "OUTPUT", "```text\n" + result.strip() + "\n```");
        text = section(text, "BORDER", explanation);
        text = text.replace("**Status:** The five algorithms are implemented and checked. Personal data, personal output, the marker-specific answer and the final screenshot are pending the student's exact Moodle ID. This draft is not ready for submission yet.",
                "**Status:** Personal input files and program output have been generated. The final screenshot, identity review and student review of the AI-assisted draft are still required before submission.");
        text = text.replace("- Personal-ID validation and the personal lab screenshot: pending.",
                "- Main has been run on the personal files. Verify all nine lab ticks and add the personal screenshot before submission.");
        Files.writeString(report, text);
        System.out.println("README updated. Open output/outbreak.html, verify the nine ticks and save output/outbreak.png.");
    }
}

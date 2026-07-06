package preprocessing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CopyExpander {

    private static final Pattern COPY_PATTERN = Pattern.compile(
            "^COPY\\s+([^\\s.]+)(?:\\s+REPLACING\\s+===(.*?)===\\s+BY\\s+===(.*?)===)?\\s*\\.?$",
            Pattern.CASE_INSENSITIVE
    );

    private final Path copybookDirectory;

    public CopyExpander(Path copybookDirectory) {
        this.copybookDirectory = copybookDirectory;
    }

    public String expand(String source) throws IOException {
        StringBuilder result = new StringBuilder();

        for (String line : source.lines().toList()) {
            String trimmed = line.trim();

            if (!startsWithCopyKeyword(trimmed)) {
                result.append(line).append(System.lineSeparator());
                continue;
            }

            String expanded = expandCopyLine(trimmed);
            result.append(expanded).append(System.lineSeparator());
        }

        return result.toString();
    }

    private String expandCopyLine(String line) throws IOException {
        Matcher matcher = COPY_PATTERN.matcher(line);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid COPY statement or REPLACING clause: " + line);
        }

        String fileName = matcher.group(1);
        Path copybook = resolveCopybook(fileName);
        String content = Files.readString(copybook);

        String oldText = matcher.group(2);
        String newText = matcher.group(3);
        if (oldText != null) {
            content = content.replace(oldText, newText);
        }

        return content;
    }

    private Path resolveCopybook(String fileName) {
        Path exact = copybookDirectory.resolve(fileName + ".babycob");
        if (Files.exists(exact)) {
            return exact;
        }

        Path lowercase = copybookDirectory.resolve(fileName.toLowerCase(Locale.ROOT) + ".babycob");
        if (Files.exists(lowercase)) {
            return lowercase;
        }

        throw new IllegalArgumentException("COPY copybook not found: " + fileName);
    }

    private boolean startsWithCopyKeyword(String text) {
        return text.regionMatches(true, 0, "COPY", 0, "COPY".length())
                && (text.length() == "COPY".length() || Character.isWhitespace(text.charAt("COPY".length())));
    }
}

package preprocessing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CopyExpander {

    private final Path copybookDirectory;

    public CopyExpander(Path copybookDirectory) {
        this.copybookDirectory = copybookDirectory;
    }

    public String expand(String source) throws IOException {
        StringBuilder result = new StringBuilder();

        for (String line : source.lines().toList()) {
            String trimmed = line.trim();

            if (!trimmed.startsWith("COPY ")) {
                result.append(line).append(System.lineSeparator());
                continue;
            }

            String expanded = expandCopyLine(trimmed);
            result.append(expanded).append(System.lineSeparator());
        }

        return result.toString();
    }

    private String expandCopyLine(String line) throws IOException {
        String withoutDot = line.endsWith(".")
                ? line.substring(0, line.length() - 1)
                : line;

        String[] parts = withoutDot.split("\\s+");

        String fileName = parts[1];
        Path copybook = copybookDirectory.resolve(fileName + ".babycob");

        String content = Files.readString(copybook);

        if (withoutDot.contains("REPLACING")) {
            String oldText = extractBetween(line, "===", "===");
            String remaining = line.substring(line.indexOf("BY") + 2);
            String newText = extractBetween(remaining, "===", "===");

            content = content.replace(oldText, newText);
        }

        return content;
    }

    private String extractBetween(String text, String start, String end) {
        int startIndex = text.indexOf(start);
        int endIndex = text.indexOf(end, startIndex + start.length());

        if (startIndex < 0 || endIndex < 0) {
            throw new IllegalArgumentException("Invalid COPY literal: " + text);
        }

        return text.substring(startIndex + start.length(), endIndex);
    }
}
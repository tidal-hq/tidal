package net.tidalhq.tidal.util;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TablistUtil {
    public static Optional<String> extract(String line, Pattern pattern) {
        Matcher matcher = pattern.matcher(line);
        return matcher.find() ? Optional.of(matcher.group(1).trim()) : Optional.empty();
    }

    public static Optional<String> findLine(String[] lines, String contains) {
        return Arrays.stream(lines)
                .filter(l -> !l.isBlank())
                .filter(l -> l.contains(contains))
                .findFirst();
    }

    public static String[] splitLines(String text) {
        return text == null ? new String[0] : text.split("\n");
    }
}

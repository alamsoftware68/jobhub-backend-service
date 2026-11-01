package com.jobsearch.util;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Small, dependency-free text helpers. */
public final class TextUtils {

    private static final Pattern BLOCK_TAGS =
            Pattern.compile("(?i)<\\s*(br|/p|/div|/li|/h[1-6]|/tr)\\s*/?>");
    private static final Pattern LIST_ITEM = Pattern.compile("(?i)<\\s*li[^>]*>");
    private static final Pattern ANY_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern MULTI_SPACE = Pattern.compile("[ \\t\\x0B\\f\\u00A0]+");
    private static final Pattern MULTI_NEWLINE = Pattern.compile("\\n\\s*\\n\\s*\\n+");
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");

    private TextUtils() {
    }

    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** Converts HTML to readable plain text, keeping paragraph and list breaks. */
    public static String stripHtml(String html) {
        if (html == null) {
            return null;
        }
        String text = BLOCK_TAGS.matcher(html).replaceAll("\n");
        text = LIST_ITEM.matcher(text).replaceAll("\n• ");
        text = ANY_TAG.matcher(text).replaceAll(" ");
        text = text.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&#039;", "'")
                .replace("\r", "");
        text = MULTI_SPACE.matcher(text).replaceAll(" ");
        text = Arrays.stream(text.split("\n")).map(String::strip).reduce((a, b) -> a + "\n" + b).orElse("");
        text = MULTI_NEWLINE.matcher(text).replaceAll("\n\n");
        return text.strip();
    }

    /** Lower-case, accent-free, alphanumeric-only form used for duplicate detection. */
    public static String normalizeKey(String s) {
        if (s == null) {
            return "";
        }
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        n = n.toLowerCase(Locale.ROOT)
                .replace("ß", "ss")
                .replaceAll("\\(m/w/d\\)|\\(w/m/d\\)|\\(m/f/d\\)|\\(f/m/d\\)|\\(all genders\\)|\\(d/m/w\\)|\\(m/w/x\\)", "")
                .replaceAll("\\b(gmbh|ag|se|kg|ug|mbh|co|inc|ltd)\\b", "");
        return NON_ALNUM.matcher(n).replaceAll(" ").strip();
    }

    public static String truncate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, Math.max(0, max - 1)).strip() + "…";
    }

    /** Splits a comma-separated string into trimmed, non-empty values. */
    public static List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::strip)
                .filter(v -> !v.isEmpty())
                .toList();
    }

    public static String joinCsv(List<String> values) {
        if (values == null) {
            return "";
        }
        return String.join(",", values.stream().map(String::strip).filter(v -> !v.isEmpty()).toList());
    }
}

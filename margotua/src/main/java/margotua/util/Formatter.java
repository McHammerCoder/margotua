package margotua.util;

import java.util.regex.Matcher;

public class Formatter {
    public static String prefixLines(String prefix, String input) {
        return input.replaceAll(".*\\R|.+\\z", Matcher.quoteReplacement(prefix) + "$0");
    }

    public static String removeEmptyBlocks(String input) {
        return input.replaceAll("\\{\n +\n *\\}", "{}");
    }
}

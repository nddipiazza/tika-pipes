package org.apache.tika.pipes.engine;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlUtils {

  private static Map<String, String> ENCODE_URL_REPLACEMENTS = new ConcurrentHashMap<String, String>() {{
    put(" ", "%20");
    put("\"", "%22");
    put("<", "%3C");
    put(">", "%3E");
    put("#", "%23");
    put("{", "%7B");
    put("}", "%7D");
    put("|", "%7C");
    put("\\", "%5C");
    put("^", "%5E");
    put("[", "%5B");
    put("]", "%5D");
    put("`", "%60");
  }};
  private static ThreadLocal<Set<Matcher>> ENCODE_URL_MATCHERS = makeThreadLocalMatchers(
      ENCODE_URL_REPLACEMENTS.keySet(), Pattern.LITERAL);

  public static String encodeURL(String url) {
    Set<Matcher> matchers = ENCODE_URL_MATCHERS.get();
    for (Matcher matcher : matchers) {
      url = matcher.reset(url).replaceAll(Matcher.quoteReplacement(
          ENCODE_URL_REPLACEMENTS.get(matcher.pattern().pattern())));
    }
    return url;
  }

  public static ThreadLocal<Set<Matcher>> makeThreadLocalMatchers(final Set<String> regexes,
                                                                  final int flags) {
    return new ThreadLocal<Set<Matcher>>() {
      @Override
      public Set<Matcher> initialValue() {
        Set<Matcher> matchers = new LinkedHashSet<>(regexes.size());
        for (String regex : regexes) {
          matchers.add(Pattern.compile(regex, flags).matcher(""));
        }
        return matchers;
      }
    };
  }
}

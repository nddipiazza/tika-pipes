package org.apache.tika.pipes.engine;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class MimeTypeToFriendlyName {

  static private final List<Pattern> WORD = Arrays.asList(Pattern.compile("(?:\\.?do[ct][mx]?$|application/.*word)"));
  static private final List<Pattern> EXCEL = Arrays.asList(Pattern.compile("(?:\\.?xl[ast][mxb]?$|(?:application/vnd\\.(?:ms-excel|openxmlformats-officedocument\\.spreadsheet)))"));
  static private final List<Pattern> PPT = Arrays.asList(Pattern.compile("(?:\\.?pp[ast][mx]?|(?:application/vnd\\.(?:ms-powerpoint|.*presentationml)))"));
  static private final List<Pattern> PDF = Arrays.asList(Pattern.compile("(?:application/)??\\.?pdf"));
  static private final List<Pattern> WEB_PAGE = Arrays.asList(Pattern.compile("(?:(?:application|text)/(?:html|xhtml|xhtml\\+xml)(; charset=.*)?)"));
  static private final List<Pattern> VISIO = Arrays.asList(Pattern.compile("(?:\\.?vs[dlstw][mx]?$|application/(?:vnd\\.|x)(?:ms)?-?visio)"));
  static private final List<Pattern> PROJECT = Arrays.asList(Pattern.compile("\\.?mpp|(?:z??z??-??application/(?:zz-winassoc-mpp|mpp|(?:(?:vnd\\.|x-(?:dos_)??)?ms[\\-_]?proje?c?t?)))"));
  static private final List<Pattern> IMG = Arrays.asList(Pattern.compile("(?:image/)?(?:.?j[pif][gefi][gf]?)"),
      Pattern.compile("(?:image/)?(?:.?gif)"),
      Pattern.compile("(?:image/)?(?:.?png)"),
      Pattern.compile("(?:image/)?(?:.?tiff?(?:-fx)?)"));
  static private final List<Pattern> EMAIL = Arrays.asList(Pattern.compile("(?:\\.msg|\\.eml|ms-outlook|message/rfc822|application/vnd.ms-outlook)"));
  static private final List<Pattern> TEXT = Arrays.asList(Pattern.compile("(?:\\.?txt|(?:application|text)/(?:rtf|plain)(; charset=.*)?)"));
  static private final List<Pattern> XML = Arrays.asList(Pattern.compile("(?:(?:application|text)/xml(; charset=.*)?)"));
  static private final List<Pattern> ZIP = Arrays.asList(Pattern.compile("(?:application/)?\\.?zipx?"));
  static private final List<Pattern> ACCESS = Arrays.asList(Pattern.compile("\\.?[AaMm][Dd][BbEeFfNnPpTtWw]|(?:(?:acc)[Dd][AaBbEeRrTt])|(?:application/x?-?msaccess)"));
  static private final List<Pattern> ONE_NOTE = Arrays.asList(Pattern.compile("(?:application/)??\\.?onenote; format=(?:one|onetoc2|package)"));

  public static String getFriendlyNameFromMimeType(String mimeType) {
    if (WORD.stream().anyMatch(p -> p.matcher(mimeType).matches())) {
      return "Word";
    } else if (EXCEL.stream().anyMatch(p -> p.matcher(mimeType).matches())) {
      return "Excel";
    } else if (PPT.stream().anyMatch(p -> p.matcher(mimeType).matches())) {
      return "Powerpoint";
    } else if (PDF.stream().anyMatch(p -> p.matcher(mimeType).matches())) {
      return "PDF";
    } else if (WEB_PAGE.stream().anyMatch(p -> p.matcher(mimeType).matches())) {
      return "Web Page";
    } else if (VISIO.stream().anyMatch(p -> p.matcher(mimeType).matches())) {
      return "Visio";
    } else if (PROJECT.stream().anyMatch(p -> p.matcher(mimeType).matches())) {
      return "Project";
    } else if (IMG.stream().anyMatch(p -> p.matcher(mimeType).matches())) {
      return "Image";
    } else if (EMAIL.stream().anyMatch(p -> p.matcher(mimeType).matches())) {
      return "Email Message";
    } else if (TEXT.stream().anyMatch(p -> p.matcher(mimeType).matches())) {
      return "Text";
    } else if (XML.stream().anyMatch(p -> p.matcher(mimeType).matches())) {
      return "XML";
    } else if (ZIP.stream().anyMatch(p -> p.matcher(mimeType).matches())) {
      return "Zip";
    } else if (ACCESS.stream().anyMatch(p -> p.matcher(mimeType).matches())) {
      return "Access";
    } else if (ONE_NOTE.stream().anyMatch(p -> p.matcher(mimeType).matches())) {
      return "One Note";
    }
    return "Other";
  }
}

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class SearchHandler implements URLHandler {
  ArrayList<String> searchterms = new ArrayList<>();
  final String MESSAGE404 = "404 not found";

  public String handleRequest(URI url) {
    HashMap<String, String> querysearchpairs = new HashMap<>();
    if (url.getPath().matches("/+")) {
      return "<!DOCTYPE html>\n"
          + "<html>\n"
          + "<body>\n"
          + "<form action=\"/search\">\n"
          + "  <label for=\"s\">Search term:</label><br>\n"
          + "  <input type=\"text\" id=\"s\" name=\"s\" value=\"\"><br>\n"
          + "  <input type=\"submit\" value=\"Search!\">\n"
          + "</form>\n"
          + "\n"
          + "</body>\n"
          + "</html>";
    } else if (url.getQuery() == null) {
      return MESSAGE404;
    }
    try {
      String[] query = url.getQuery().split("[&]");
      for (String kvpair : query) {
        String[] pair = kvpair.split("=");
        String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8.name());
        String value = URLDecoder.decode(pair[1], StandardCharsets.UTF_8.name());
        if (key.equalsIgnoreCase("s")) {
          querysearchpairs.put(key, value);
        }
      }
    } catch (UnsupportedEncodingException e) {
      throw new Error("System does not support UTF-8 URL decoding");
    }
    if (url.getPath().equals("/add")) {
      for (Map.Entry<String, String> kvpair : querysearchpairs.entrySet()) {
        if (kvpair.getKey().equalsIgnoreCase("s")) {
          searchterms.add(kvpair.getValue());
          System.out.println("Added search term: " + kvpair.getValue());
        }
      }
      return "Added new search term(s).";
    } else if (url.getPath().equals("/search")) {
      StringBuilder response = new StringBuilder();
      for (Map.Entry<String, String> kvpair : querysearchpairs.entrySet()) {
        if (kvpair.getKey().equalsIgnoreCase("s")) {
          for (String s : searchterms) {
            if (containsCaseInsensitive(s, kvpair.getValue())) {
              response.append(kvpair.getValue());
              response.append(" in ");
              response.append(s);
              response.append('\n');
              System.out.println("Found search term: " + kvpair.getValue());
            }
          }
        }
      }
      if (response.toString().isEmpty()) {
        return "No results found.";
      }
      return response.toString();
    }
    return MESSAGE404;
  }

  boolean containsCaseInsensitive(String a, String b) {
    return Pattern.compile(Pattern.quote(b), Pattern.CASE_INSENSITIVE).matcher(a).find();
  }
}

class SearchServer {
  public static void main(String[] args) throws IOException {
    SearchHandler handler = new SearchHandler();
    switch (args.length) {
      case 0:
        System.out.println("Missing port number! Try any number between 1024 to 49151");
        return;
      case 1:
        System.out.println("Did not find directory to search. Running normally.");
        break;
      case 2:
        String dir = args[1];
        File toSearch = new File(dir);
        if (!toSearch.exists()) {
          System.out.println("No such directory: " + dir);
        }
        System.out.println("Found directory to add to search terms: " + dir);
        handler.searchterms.addAll(
            getFiles(toSearch).stream().map((File f) -> f.getPath()).collect(Collectors.toList()));
        break;
      default:
        System.out.println("Too many arguments: " + args);
        return;
    }
    int port = Integer.parseInt(args[0].trim());
    Server.start(port, handler);
  }

  public static List<File> getFiles(File start) throws IOException {
    List<File> result = new ArrayList<>();
    if (start.isFile()) {
      result.add(start);
    } else {
      File[] paths = start.listFiles();
      for (File subFile : paths) {
        result.addAll(getFiles(subFile));
      }
    }
    return result;
  }
}

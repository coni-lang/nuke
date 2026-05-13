import java.io.File;
public class TestName {
    public static void main(String[] args) throws Exception {
        String basePath = "/Users/nico/cool/npkm/nuke/example-java-lib";
        File ednFile = new File(basePath, "nuke.edn");
        String content = new String(java.nio.file.Files.readAllBytes(ednFile.toPath()));
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(":name\\s+\"([^\"]+)\"").matcher(content);
        if (m.find()) {
            System.out.println("Found name: " + m.group(1));
        } else {
            System.out.println("Name not found!");
        }
    }
}

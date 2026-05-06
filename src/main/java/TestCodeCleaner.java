import preprocessing.CodeCleaner;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class TestCodeCleaner {
    public static void main(String[] args) throws Exception {
        
        // Read the test_cleaner.babycob file
        InputStream is = TestCodeCleaner.class.getResourceAsStream("/examples/test_cleaner.babycob");
        
        if (is == null) {
            throw new RuntimeException("File not found.");
        }
        
        // Convert input stream to string
        String fileContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        
        System.out.println("=== ORIGINAL FILE ===");
        System.out.println(fileContent);
        System.out.println("\n=== CLEANED CODE ===");
        
        // Clean the code
        String cleanedCode = CodeCleaner.cleanCode(fileContent);
        System.out.println(cleanedCode);
    }
}

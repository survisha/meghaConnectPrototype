// Simple BCrypt hash generator for demo passwords
// Compile and run: javac GenerateBCryptHashes.java && java GenerateBCryptHashes

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerateBCryptHashes {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        
        String[][] credentials = {
            {"hcm", "hcm123"},
            {"admin", "admin123"},
            {"saidul", "osd123"},
            {"jtsecy", "jts123"},
            {"cmo", "cmo123"},
            {"deo1", "deo123"}
        };
        
        System.out.println("-- BCrypt password hashes for V11 migration:");
        for (String[] cred : credentials) {
            String hash = encoder.encode(cred[1]);
            System.out.println(String.format("UPDATE users SET password_hash = '%s' WHERE username = '%s';  -- %s", 
                hash, cred[0], cred[1]));
        }
    }
}

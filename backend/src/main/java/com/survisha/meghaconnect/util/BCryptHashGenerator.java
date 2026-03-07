package com.survisha.meghaconnect.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Standalone BCrypt hash generator (not a Spring Boot application)
 * Run: mvn exec:java "-Dexec.mainClass=com.survisha.meghaconnect.util.BCryptHashGenerator"
 */
public class BCryptHashGenerator {
    
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        
        System.out.println("\n===============================================");
        System.out.println("BCRYPT PASSWORD HASHES FOR V11 MIGRATION");
        System.out.println("===============================================\n");
        
        String[][] credentials = {
            {"hcm", "hcm123"},
            {"admin", "admin123"},
            {"saidul", "osd123"},
            {"jtsecy", "jts123"},
            {"cmo", "cmo123"},
            {"deo1", "deo123"}
        };
        
        for (String[] cred : credentials) {
            String hash = encoder.encode(cred[1]);
            System.out.println(String.format("UPDATE users SET password_hash = '%s' WHERE username = '%s';  -- %s", 
                hash, cred[0], cred[1]));
        }
        
        System.out.println("\n===============================================\n");
        System.exit(0);
    }
}

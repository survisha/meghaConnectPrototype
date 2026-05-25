package com.survisha.meghaconnect.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Standalone BCrypt hash generator for one controlled username/password pair.
 * Run with: mvn exec:java "-Dexec.mainClass=com.survisha.meghaconnect.util.BCryptHashGenerator" "-Dexec.args=username password"
 */
public class BCryptHashGenerator {
    
    public static void main(String[] args) {
        if (args.length != 2 || args[0].isBlank() || args[1].isBlank()) {
            System.err.println("Usage: BCryptHashGenerator <username> <password>");
            System.exit(2);
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        String hash = encoder.encode(args[1]);
        System.out.println(String.format("UPDATE users SET password_hash = '%s' WHERE username = '%s';",
                hash, args[0]));
        System.exit(0);
    }
}

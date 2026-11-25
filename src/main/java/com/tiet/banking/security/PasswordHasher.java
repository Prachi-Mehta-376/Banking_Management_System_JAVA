package com.tiet.banking.security;

import com.tiet.banking.exception.AuthenticationException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PasswordHasher {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_BYTES = 16;
    private static final int KEY_LENGTH_BITS = 256;

    private final int iterations;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordHasher(int iterations) {
        this.iterations = iterations;
    }

    public HashedPassword hash(char[] password) {
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        byte[] derived = derive(password, salt, iterations);
        return new HashedPassword(
                Base64.getEncoder().encodeToString(derived),
                Base64.getEncoder().encodeToString(salt),
                ALGORITHM + ":" + iterations);
    }

    public boolean verify(char[] password, String expectedHashBase64, String saltBase64, String algoMetadata) {
        if (password == null || expectedHashBase64 == null || saltBase64 == null) {
            return false;
        }
        int parsedIterations = parseIterations(algoMetadata);
        byte[] salt = Base64.getDecoder().decode(saltBase64);
        byte[] expected = Base64.getDecoder().decode(expectedHashBase64);
        byte[] actual = derive(password, salt, parsedIterations);
        return MessageDigest.isEqual(expected, actual);
    }

    private int parseIterations(String algoMetadata) {
        if (algoMetadata == null || !algoMetadata.contains(":")) {
            return iterations;
        }
        String[] parts = algoMetadata.split(":");
        try {
            int parsed = Integer.parseInt(parts[1]);
            return parsed > 0 ? parsed : iterations;
        } catch (NumberFormatException ex) {
            return iterations;
        }
    }

    private byte[] derive(char[] password, byte[] salt, int iterationCount) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterationCount, KEY_LENGTH_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new AuthenticationException("Password hashing configuration error.");
        } finally {
            spec.clearPassword();
        }
    }

    public record HashedPassword(String hashBase64, String saltBase64, String algoMetadata) {
    }
}

package com.bigbrightpaints.tally.util;

import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

/**
 * Utility class for generating hashes and deterministic IDs
 */
@UtilityClass
public class HashUtil {

    private static final String SHA256 = "SHA-256";

    /**
     * Generate SHA-256 hash from input string
     */
    public static String generateHash(String input) {
        if (input == null) {
            return null;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(SHA256);
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Generate deterministic ID from prefix and source data
     */
    public static String generateDeterministicId(String prefix, String sourceData) {
        if (sourceData == null) {
            throw new IllegalArgumentException("Source data cannot be null");
        }

        String hash = generateHash(sourceData);
        String shortHash = hash.substring(0, 12).toUpperCase();

        if (prefix != null && !prefix.isEmpty()) {
            return prefix + "_" + shortHash;
        }
        return shortHash;
    }

    /**
     * Generate deterministic UUID from source data
     */
    public static UUID generateDeterministicUuid(String sourceData) {
        if (sourceData == null) {
            throw new IllegalArgumentException("Source data cannot be null");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(SHA256);
            byte[] hashBytes = digest.digest(sourceData.getBytes(StandardCharsets.UTF_8));

            // Use first 16 bytes for UUID
            byte[] uuidBytes = new byte[16];
            System.arraycopy(hashBytes, 0, uuidBytes, 0, 16);

            // Set version and variant bits
            uuidBytes[6] &= 0x0f;  // Clear version
            uuidBytes[6] |= 0x40;  // Set to version 4
            uuidBytes[8] &= 0x3f;  // Clear variant
            uuidBytes[8] |= 0x80;  // Set to IETF variant

            long msb = 0;
            long lsb = 0;
            for (int i = 0; i < 8; i++) {
                msb = (msb << 8) | (uuidBytes[i] & 0xff);
            }
            for (int i = 8; i < 16; i++) {
                lsb = (lsb << 8) | (uuidBytes[i] & 0xff);
            }

            return new UUID(msb, lsb);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Generate short code from source data
     */
    public static String generateShortCode(String sourceData, int length) {
        if (sourceData == null) {
            throw new IllegalArgumentException("Source data cannot be null");
        }
        if (length < 4 || length > 32) {
            throw new IllegalArgumentException("Length must be between 4 and 32");
        }

        String hash = generateHash(sourceData);
        return hash.substring(0, length).toUpperCase();
    }

    /**
     * Generate Base64 encoded hash
     */
    public static String generateBase64Hash(String input) {
        if (input == null) {
            return null;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(SHA256);
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Convert byte array to hex string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Check if two hashes match
     */
    public static boolean hashesMatch(String hash1, String hash2) {
        if (hash1 == null || hash2 == null) {
            return false;
        }
        return hash1.equalsIgnoreCase(hash2);
    }
}
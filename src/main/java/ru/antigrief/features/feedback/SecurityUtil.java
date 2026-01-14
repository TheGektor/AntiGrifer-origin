package ru.antigrief.features.feedback;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Utility for secure storage of sensitive data (Webhook URLs).
 * Uses AES/GCM/NoPadding.
 */
public class SecurityUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    /**
     * Generates the secret key at runtime to avoid storing it as a plain string.
     * Splitting logic makes it harder to extract via string analysis.
     */
    private static SecretKeySpec getKey() {
        // Dynamic key construction
        byte[] key = new byte[16]; // 128-bit key
        
        // Pseudo-math to generate bytes
        key[0] = (byte) (0x4A ^ 0x12);
        key[1] = (byte) 20;
        key[2] = (byte) (key[0] + key[1]);
        key[3] = 115; // 's'
        key[4] = 33;
        key[5] = (byte) (0xFF - 0xAA);
        key[6] = 100;
        key[7] = 55;
        
        int[] tail = {90, 11, 22, 33, 44, 12, 13, 14};
        for (int i = 0; i < tail.length; i++) {
            key[8 + i] = (byte) tail[i];
        }

        return new SecretKeySpec(key, "AES");
    }

    public static String decrypt(String encryptedText) {
        try {
            if (encryptedText == null || encryptedText.isEmpty()) return null;
            
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            
            // Extract IV
            byte[] iv = Arrays.copyOfRange(decoded, 0, GCM_IV_LENGTH);
            // Extract CipherText
            byte[] cipherText = Arrays.copyOfRange(decoded, GCM_IV_LENGTH, decoded.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, getKey(), spec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("CRITICAL: Failed to decrypt webhook URL. Check SecurityUtil.");
            e.printStackTrace();
            return null;
        }
    }

    // Helper for the Administrator to generate the encrypted string once
    public static String encrypt(String plainText) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, getKey(), spec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Combine IV + CipherText
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        // Run this main method locally to get your encrypted string.
        // Replace the URL below with your real Discord Webhook URL.
        String webhookUrl = "https://discord.com/api/webhooks/YOUR_ID/YOUR_TOKEN";
        
        if (args.length > 0) {
            webhookUrl = args[0];
        }

        System.out.println("--- ENCRYPTION TOOL ---");
        System.out.println("Original: " + webhookUrl);
        String encrypted = encrypt(webhookUrl);
        System.out.println("Encrypted: " + encrypted);
        System.out.println("Decrypt Check: " + decrypt(encrypted));
        System.out.println("-----------------------");
        System.out.println("Copy the 'Encrypted' string into DiscordService.java");
    }
}

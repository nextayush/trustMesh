package com.quantumprovenance.crypto.symmetric;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

@Service
public class AESEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;
    private final SecureRandom secureRandom = new SecureRandom();

    public record EncryptedPayload(byte[] ciphertext, byte[] iv) {}

    public EncryptedPayload encrypt(byte[] plaintext, byte[] aesKey) throws Exception {
        byte[] iv = new byte[IV_LENGTH_BYTE];
        secureRandom.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
        
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, parameterSpec);
        byte[] ciphertext = cipher.doFinal(plaintext);

        return new EncryptedPayload(ciphertext, iv);
    }

    public byte[] decrypt(byte[] ciphertext, byte[] aesKey, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
        
        cipher.init(Cipher.DECRYPT_MODE, keySpec, parameterSpec);
        return cipher.doFinal(ciphertext);
    }
}

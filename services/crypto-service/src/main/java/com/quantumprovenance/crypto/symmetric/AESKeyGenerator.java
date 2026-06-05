package com.quantumprovenance.crypto.symmetric;

import org.springframework.stereotype.Service;
import java.security.SecureRandom;

@Service
public class AESKeyGenerator {

    private final SecureRandom secureRandom = new SecureRandom();

    public byte[] generateKey() {
        byte[] key = new byte[32]; // 256 bits
        secureRandom.nextBytes(key);
        return key;
    }
}

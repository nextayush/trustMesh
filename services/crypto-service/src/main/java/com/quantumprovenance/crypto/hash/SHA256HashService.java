package com.quantumprovenance.crypto.hash;

import org.springframework.stereotype.Service;
import java.security.MessageDigest;

@Service
public class SHA256HashService {

    public byte[] hash(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}

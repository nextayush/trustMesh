package com.quantumprovenance.crypto.pqc;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.springframework.stereotype.Component;

@Component
public class HybridKeyDerivation {

    /**
     * Derives a single secure AES-256 key from a post-quantum ML-KEM secret
     * and a classical X25519 ECDH secret using HKDF-SHA256.
     *
     * @param mlkemSecret  The secret extracted from ML-KEM decapsulation.
     * @param x25519Secret The secret derived from classical X25519 ECDH key exchange.
     * @param info         Contextual application info parameter for HKDF.
     * @return 32-byte derived key.
     */
    public byte[] deriveHybridSecret(byte[] mlkemSecret, byte[] x25519Secret, byte[] info) {
        if (mlkemSecret == null || x25519Secret == null) {
            throw new IllegalArgumentException("Inputs secrets cannot be null");
        }

        // Concatenate ML-KEM secret and X25519 secret
        byte[] combinedSecret = new byte[mlkemSecret.length + x25519Secret.length];
        System.arraycopy(mlkemSecret, 0, combinedSecret, 0, mlkemSecret.length);
        System.arraycopy(x25519Secret, 0, combinedSecret, mlkemSecret.length, x25519Secret.length);

        // Initialize HKDF Bytes Generator with SHA-256 digest
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA256Digest());
        
        // Salt is left as null (which HKDF defaults to a string of zeros)
        HKDFParameters hkdfParameters = new HKDFParameters(combinedSecret, null, info);
        hkdf.init(hkdfParameters);

        byte[] derivedKey = new byte[32]; // 256-bit key for AES-256
        hkdf.generateBytes(derivedKey, 0, derivedKey.length);

        return derivedKey;
    }
}

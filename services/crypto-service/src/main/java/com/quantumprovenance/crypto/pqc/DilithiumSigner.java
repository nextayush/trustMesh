package com.quantumprovenance.crypto.pqc;

import org.bouncycastle.pqc.jcajce.spec.DilithiumParameterSpec;
import org.springframework.stereotype.Component;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

@Component
public class DilithiumSigner {

    private final SecureRandom secureRandom = new SecureRandom();

    public record DilithiumKeyPair(byte[] publicKey, byte[] privateKey) {}

    public DilithiumKeyPair generateKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("Dilithium", "BCPQC");
            kpg.initialize(DilithiumParameterSpec.dilithium3, secureRandom);
            KeyPair kp = kpg.generateKeyPair();
            
            return new DilithiumKeyPair(kp.getPublic().getEncoded(), kp.getPrivate().getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Dilithium key pair", e);
        }
    }

    public byte[] sign(byte[] privateKeyBytes, byte[] message) {
        try {
            KeyFactory kf = KeyFactory.getInstance("Dilithium", "BCPQC");
            PrivateKey privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

            Signature signature = Signature.getInstance("Dilithium", "BCPQC");
            signature.initSign(privateKey);
            signature.update(message);
            
            return signature.sign();
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign message", e);
        }
    }

    public boolean verify(byte[] publicKeyBytes, byte[] message, byte[] signature) {
        try {
            KeyFactory kf = KeyFactory.getInstance("Dilithium", "BCPQC");
            PublicKey publicKey = kf.generatePublic(new X509EncodedKeySpec(publicKeyBytes));

            Signature sig = Signature.getInstance("Dilithium", "BCPQC");
            sig.initVerify(publicKey);
            sig.update(message);
            
            return sig.verify(signature);
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify signature", e);
        }
    }
}

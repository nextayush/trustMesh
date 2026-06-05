package com.quantumprovenance.crypto.pqc;

import org.bouncycastle.jcajce.SecretKeyWithEncapsulation;
import org.bouncycastle.jcajce.spec.KEMExtractSpec;
import org.bouncycastle.jcajce.spec.KEMGenerateSpec;
import org.bouncycastle.pqc.jcajce.spec.KyberParameterSpec;
import org.springframework.stereotype.Component;

import javax.crypto.KeyGenerator;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

@Component
public class MLKEMKeyEncapsulator {

    private final SecureRandom secureRandom = new SecureRandom();

    public record MLKEMKeyPair(byte[] publicKey, byte[] privateKey) {}
    public record EncapsulationResult(byte[] sharedSecret, byte[] ciphertext) {}

    public MLKEMKeyPair generateKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("Kyber", "BCPQC");
            kpg.initialize(KyberParameterSpec.kyber768, secureRandom);
            KeyPair kp = kpg.generateKeyPair();
            
            return new MLKEMKeyPair(kp.getPublic().getEncoded(), kp.getPrivate().getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Kyber key pair", e);
        }
    }

    public EncapsulationResult encapsulate(byte[] publicKeyBytes) {
        try {
            KeyFactory kf = KeyFactory.getInstance("Kyber", "BCPQC");
            PublicKey publicKey = kf.generatePublic(new X509EncodedKeySpec(publicKeyBytes));

            KeyGenerator keyGen = KeyGenerator.getInstance("Kyber", "BCPQC");
            keyGen.init(new KEMGenerateSpec(publicKey, "Secret"));
            SecretKeyWithEncapsulation secKey = (SecretKeyWithEncapsulation) keyGen.generateKey();

            return new EncapsulationResult(secKey.getEncoded(), secKey.getEncapsulation());
        } catch (Exception e) {
            throw new RuntimeException("Failed to encapsulate key", e);
        }
    }

    public byte[] decapsulate(byte[] privateKeyBytes, byte[] ciphertext) {
        try {
            KeyFactory kf = KeyFactory.getInstance("Kyber", "BCPQC");
            PrivateKey privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

            KeyGenerator keyGen = KeyGenerator.getInstance("Kyber", "BCPQC");
            keyGen.init(new KEMExtractSpec(privateKey, ciphertext, "Secret"));
            SecretKeyWithEncapsulation secKey = (SecretKeyWithEncapsulation) keyGen.generateKey();

            return secKey.getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("Failed to decapsulate key", e);
        }
    }
}

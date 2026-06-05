package com.quantumprovenance.crypto.pqc;

import com.quantumprovenance.crypto.hash.MerkleHashService;
import com.quantumprovenance.crypto.symmetric.AESEncryptionService;
import com.quantumprovenance.crypto.symmetric.AESKeyGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.Security;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PQCServiceTest {

    @BeforeAll
    public static void setupProviders() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        if (Security.getProvider(BouncyCastlePQCProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastlePQCProvider());
        }
    }

    @Test
    public void testMLKEMKeyEncapsulation() {
        MLKEMKeyEncapsulator encapsulator = new MLKEMKeyEncapsulator();

        // 1. Generate Key Pair
        MLKEMKeyEncapsulator.MLKEMKeyPair keyPair = encapsulator.generateKeyPair();
        assertNotNull(keyPair.publicKey());
        assertNotNull(keyPair.privateKey());
        assertTrue(keyPair.publicKey().length > 0);
        assertTrue(keyPair.privateKey().length > 0);

        // 2. Encapsulate AES key
        MLKEMKeyEncapsulator.EncapsulationResult result = encapsulator.encapsulate(keyPair.publicKey());
        assertNotNull(result.sharedSecret());
        assertNotNull(result.ciphertext());
        assertEquals(32, result.sharedSecret().length); // ML-KEM-768 shared secret size is 32 bytes

        // 3. Decapsulate ciphertext
        byte[] decapsulatedSecret = encapsulator.decapsulate(keyPair.privateKey(), result.ciphertext());
        assertArrayEquals(result.sharedSecret(), decapsulatedSecret);
    }

    @Test
    public void testDilithiumDigitalSignatures() {
        DilithiumSigner signer = new DilithiumSigner();

        // 1. Generate Key Pair
        DilithiumSigner.DilithiumKeyPair keyPair = signer.generateKeyPair();
        assertNotNull(keyPair.publicKey());
        assertNotNull(keyPair.privateKey());

        // 2. Sign message
        byte[] message = "Quantum-Provenance-Manifest-Payload".getBytes();
        byte[] signature = signer.sign(keyPair.privateKey(), message);
        assertNotNull(signature);
        assertTrue(signature.length > 0);

        // 3. Verify signature
        boolean isValid = signer.verify(keyPair.publicKey(), message, signature);
        assertTrue(isValid);

        // 4. Verify tampering is caught
        byte[] tamperedMessage = "Quantum-Provenance-Manifest-Payloax".getBytes();
        boolean isValidTampered = signer.verify(keyPair.publicKey(), tamperedMessage, signature);
        assertFalse(isValidTampered);
    }

    @Test
    public void testHybridKeyDerivation() {
        HybridKeyDerivation hybrid = new HybridKeyDerivation();
        byte[] mlkemSecret = new byte[32];
        byte[] x25519Secret = new byte[32];
        byte[] info = "hybrid-key-info-context".getBytes();

        // Fill with dummy values
        Arrays.fill(mlkemSecret, (byte) 0xAA);
        Arrays.fill(x25519Secret, (byte) 0xBB);

        byte[] derived = hybrid.deriveHybridSecret(mlkemSecret, x25519Secret, info);
        assertNotNull(derived);
        assertEquals(32, derived.length);
    }

    @Test
    public void testAESEncryptionService() throws Exception {
        AESEncryptionService service = new AESEncryptionService();
        AESKeyGenerator keyGen = new AESKeyGenerator();
        
        byte[] aesKey = keyGen.generateKey();
        byte[] plaintext = "Secret Shipment Manifest Data".getBytes();

        AESEncryptionService.EncryptedPayload payload = service.encrypt(plaintext, aesKey);
        assertNotNull(payload.ciphertext());
        assertNotNull(payload.iv());
        assertEquals(12, payload.iv().length);

        byte[] decrypted = service.decrypt(payload.ciphertext(), aesKey, payload.iv());
        assertArrayEquals(plaintext, decrypted);
    }

    @Test
    public void testMerkleRootCalculation() {
        MerkleHashService service = new MerkleHashService();
        byte[] hash1 = new byte[32];
        byte[] hash2 = new byte[32];
        Arrays.fill(hash1, (byte) 0x11);
        Arrays.fill(hash2, (byte) 0x22);

        byte[] root = service.calculateMerkleRoot(List.of(hash1, hash2));
        assertNotNull(root);
        assertEquals(32, root.length);
    }
}

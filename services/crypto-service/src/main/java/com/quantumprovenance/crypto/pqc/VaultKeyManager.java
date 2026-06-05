package com.quantumprovenance.crypto.pqc;

import org.springframework.stereotype.Component;
import org.springframework.vault.core.VaultKeyValueOperations;
import org.springframework.vault.core.VaultKeyValueOperationsSupport.KeyValueBackend;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class VaultKeyManager {

    private final VaultKeyValueOperations kvOps;

    public VaultKeyManager(VaultTemplate vaultTemplate) {
        // Vault dev mode enables KV v2 secrets at the path "secret"
        this.kvOps = vaultTemplate.opsForKeyValue("secret", KeyValueBackend.KV_2);
    }

    public void storeKeyPair(String keyId, byte[] publicKey, byte[] privateKey) {
        Map<String, String> keyData = new HashMap<>();
        keyData.put("publicKey", Base64.getEncoder().encodeToString(publicKey));
        keyData.put("privateKey", Base64.getEncoder().encodeToString(privateKey));

        String path = "quantum-provenance/keys/" + keyId;
        kvOps.put(path, keyData);
        System.out.println("[Vault] Stored key pair under path: " + path);
    }

    public byte[] getPrivateKey(String keyId) {
        String path = "quantum-provenance/keys/" + keyId;
        VaultResponse response = kvOps.get(path);
        if (response == null || response.getData() == null) {
            throw new IllegalArgumentException("Key not found in Vault: " + keyId);
        }

        Map<String, Object> data = (Map<String, Object>) response.getRequiredData().get("data");
        if (data == null || !data.containsKey("privateKey")) {
            throw new IllegalArgumentException("Private key missing for: " + keyId);
        }

        return Base64.getDecoder().decode((String) data.get("privateKey"));
    }

    public byte[] getPublicKey(String keyId) {
        String path = "quantum-provenance/keys/" + keyId;
        VaultResponse response = kvOps.get(path);
        if (response == null || response.getData() == null) {
            throw new IllegalArgumentException("Key not found in Vault: " + keyId);
        }

        Map<String, Object> data = (Map<String, Object>) response.getRequiredData().get("data");
        if (data == null || !data.containsKey("publicKey")) {
            throw new IllegalArgumentException("Public key missing for: " + keyId);
        }

        return Base64.getDecoder().decode((String) data.get("publicKey"));
    }
}

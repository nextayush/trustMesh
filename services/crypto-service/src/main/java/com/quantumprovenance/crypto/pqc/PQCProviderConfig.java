package com.quantumprovenance.crypto.pqc;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import java.security.Security;

@Configuration
public class PQCProviderConfig {

    @PostConstruct
    public void registerProviders() {
        // Register the signed standard BouncyCastle provider
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        
        // Register the signed Post-Quantum Cryptography BouncyCastle provider
        if (Security.getProvider(BouncyCastlePQCProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastlePQCProvider());
        }
        
        System.out.println("[PQC-Provider] Registered signed BouncyCastle and BouncyCastlePQC providers successfully.");
    }
}

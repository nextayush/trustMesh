package com.quantumprovenance.crypto.pqc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;

import java.net.URI;

@Configuration
public class VaultConfig {

    @Value("${vault.uri:http://localhost:8200}")
    private String vaultUri;

    @Value("${vault.token:dev-root-token}")
    private String vaultToken;

    @Bean
    public VaultTemplate vaultTemplate() {
        VaultEndpoint endpoint = VaultEndpoint.from(URI.create(vaultUri));
        return new VaultTemplate(endpoint, new TokenAuthentication(vaultToken));
    }
}

package com.quantumprovenance.storage.service;

public interface ContentAddressableStorageService {
    
    /**
     * Stores the encrypted payload off-chain and returns its content-addressable hash.
     *
     * @param content Encrypted payload bytes.
     * @return Unique Content Identifier (CID or SHA-256 Hash).
     */
    String store(byte[] content);

    /**
     * Retrieves the encrypted payload off-chain using its content-addressable identifier.
     *
     * @param address Unique Content Identifier (CID or SHA-256 Hash).
     * @return Encrypted payload bytes.
     */
    byte[] retrieve(String address);
}

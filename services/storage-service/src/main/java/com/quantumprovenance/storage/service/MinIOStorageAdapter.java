package com.quantumprovenance.storage.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;

@Service
public class MinIOStorageAdapter implements ContentAddressableStorageService {

    private final MinioClient minioClient;
    private final String bucketName;

    public MinIOStorageAdapter(
            MinioClient minioClient, 
            @Value("${minio.bucket:quantum-provenance-manifests}") String bucketName
    ) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
    }

    @Override
    public String store(byte[] content) {
        try {
            // 1. Calculate SHA-256 Hash of content to use as Content Identifier (CID)
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(content);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            String cid = hexString.toString();

            // 2. Upload to MinIO bucket
            try (InputStream bais = new ByteArrayInputStream(content)) {
                minioClient.putObject(
                    PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(cid)
                        .stream(bais, content.length, -1)
                        .contentType("application/octet-stream")
                        .build()
                );
            }

            System.out.println("[Storage] Stored payload off-chain under CID: " + cid);
            return cid;
        } catch (Exception e) {
            throw new RuntimeException("Failed to store content in MinIO", e);
        }
    }

    @Override
    public byte[] retrieve(String address) {
        try {
            try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(address)
                    .build()
            )) {
                return stream.readAllBytes();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve content from MinIO for CID: " + address, e);
        }
    }
}

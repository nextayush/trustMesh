package com.quantumprovenance.crypto.hash;

import org.springframework.stereotype.Service;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

@Service
public class MerkleHashService {

    public byte[] calculateMerkleRoot(List<byte[]> leafHashes) {
        if (leafHashes == null || leafHashes.isEmpty()) {
            return new byte[32]; // Return empty 32-byte array
        }

        List<byte[]> currentLevel = new ArrayList<>(leafHashes);

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            while (currentLevel.size() > 1) {
                List<byte[]> nextLevel = new ArrayList<>();

                for (int i = 0; i < currentLevel.size(); i += 2) {
                    byte[] left = currentLevel.get(i);
                    byte[] right;

                    if (i + 1 < currentLevel.size()) {
                        right = currentLevel.get(i + 1);
                    } else {
                        right = left; // Duplicate last node for odd trees
                    }

                    byte[] concatenated = new byte[left.length + right.length];
                    System.arraycopy(left, 0, concatenated, 0, left.length);
                    System.arraycopy(right, 0, concatenated, left.length, right.length);

                    nextLevel.add(digest.digest(concatenated));
                }

                currentLevel = nextLevel;
            }

            return currentLevel.getFirst();
        } catch (Exception e) {
            throw new RuntimeException("Error calculating Merkle root", e);
        }
    }
}

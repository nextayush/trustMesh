package com.quantumprovenance.blockchain.web3j;

import org.web3j.codegen.SolidityFunctionWrapperGenerator;
import java.io.File;

public class WrapperGenerator {
    public static void main(String[] args) {
        System.out.println("=============================================");
        System.out.println("  Programmatic Web3j Wrapper Generation      ");
        System.out.println("=============================================");

        String rootPath = new File("").getAbsolutePath();
        System.out.println("Project Root: " + rootPath);

        // Adjust paths based on whether run from root or service directory
        String baseContractsPath = rootPath.endsWith("blockchain-service") 
            ? "../../contracts" 
            : "contracts";

        String outputSrcPath = rootPath.endsWith("blockchain-service") 
            ? "src/main/java" 
            : "services/blockchain-service/src/main/java";

        String[] contracts = {"ShipmentRegistry", "ProvenanceAnchor", "ABACPolicyEngine"};
        String packageName = "com.quantumprovenance.contracts.generated";

        for (String contract : contracts) {
            String abiPath = baseContractsPath + "/build-raw/" + contract + ".abi";
            String binPath = baseContractsPath + "/build-raw/" + contract + ".bin";

            System.out.println("Generating wrapper for " + contract + "...");
            System.out.println("ABI: " + abiPath);
            System.out.println("BIN: " + binPath);
            System.out.println("Output: " + outputSrcPath);

            try {
                SolidityFunctionWrapperGenerator.main(new String[] {
                    "-b", binPath,
                    "-a", abiPath,
                    "-o", outputSrcPath,
                    "-p", packageName
                });
                System.out.println("Successfully generated wrapper for " + contract);
            } catch (Exception e) {
                System.err.println("Failed to generate wrapper for " + contract + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        System.out.println("Wrapper generation process finished.");
    }
}

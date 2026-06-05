package com.quantumprovenance.blockchain.service;

import com.quantumprovenance.contracts.generated.ShipmentRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class ShipmentAnchorServiceIT {

    private ShipmentAnchorService service;
    private String registryAddress;

    @BeforeEach
    public void setup() throws Exception {
        // 1. Read registry address from deployed-addresses.json
        String rootPath = new File("").getAbsolutePath();
        String jsonPath = rootPath.endsWith("blockchain-service")
                ? "../../contracts/build-raw/deployed-addresses.json"
                : "contracts/build-raw/deployed-addresses.json";

        String jsonContent = new String(Files.readAllBytes(Paths.get(jsonPath)));
        // Simple manual parsing of JSON for addresses
        String marker = "\"ShipmentRegistry\": \"";
        int start = jsonContent.indexOf(marker) + marker.length();
        int end = jsonContent.indexOf("\"", start);
        registryAddress = jsonContent.substring(start, end);
        System.out.println("Test loaded registry address: " + registryAddress);

        // 2. Build Web3j elements pointing to Anvil
        Web3j web3j = Web3j.build(new HttpService("http://localhost:8545"));
        // Use default Anvil Account #0
        Credentials credentials = Credentials.create("0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80");
        RawTransactionManager txManager = new RawTransactionManager(web3j, credentials, 31337);
        DefaultGasProvider gasProvider = new DefaultGasProvider();

        service = new ShipmentAnchorService(web3j, txManager, gasProvider);

        // Inject the registryAddress via reflection or field settings.
        // We can use reflection to set the private field "registryAddress".
        java.lang.reflect.Field field = ShipmentAnchorService.class.getDeclaredField("registryAddress");
        field.setAccessible(true);
        field.set(service, registryAddress);
    }

    @Test
    public void testOnChainShipmentLifecycle() throws Exception {
        String shipmentId = "SHIP-TEST-" + System.currentTimeMillis();
        String cid = "QmXoypizjW3WknFiJnKLwHCnL72vedxjQkDDP1mXWo6uco";
        byte[] merkleRoot = new byte[32];
        Arrays.fill(merkleRoot, (byte) 0x99);

        // 1. Register shipment
        service.registerShipment(shipmentId, cid, merkleRoot);

        // 2. Fetch and Verify
        ShipmentRegistry.Shipment shipment = service.getShipment(shipmentId);
        assertTrue(shipment.exists);
        assertEquals(shipmentId, shipment.shipmentId);
        assertEquals(cid, shipment.contentIdentifier);
        assertArrayEquals(merkleRoot, shipment.merkleRoot);
        assertEquals(0, shipment.status.intValue()); // CREATED

        // 3. Update Status
        service.updateShipmentStatus(shipmentId, 3, cid, merkleRoot); // IN_TRANSIT
        shipment = service.getShipment(shipmentId);
        assertEquals(3, shipment.status.intValue()); // IN_TRANSIT
    }
}

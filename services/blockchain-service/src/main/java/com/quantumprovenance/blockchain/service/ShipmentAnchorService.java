package com.quantumprovenance.blockchain.service;

import com.quantumprovenance.contracts.generated.ShipmentRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigInteger;

@Service
public class ShipmentAnchorService {

    private final Web3j web3j;
    private final TransactionManager transactionManager;
    private final ContractGasProvider gasProvider;

    @Value("${ethereum.contracts.registry-address:0xDc64a140Aa3E981100a9becA4E685f962f0cF6C9}")
    private String registryAddress;

    public ShipmentAnchorService(
            Web3j web3j, 
            TransactionManager transactionManager, 
            ContractGasProvider gasProvider
    ) {
        this.web3j = web3j;
        this.transactionManager = transactionManager;
        this.gasProvider = gasProvider;
    }

    private ShipmentRegistry loadRegistry() {
        return ShipmentRegistry.load(registryAddress, web3j, transactionManager, gasProvider);
    }

    public void registerShipment(String shipmentId, String cid, byte[] merkleRoot) throws Exception {
        ShipmentRegistry registry = loadRegistry();
        
        // Call Solidity: registerShipment(string,string,bytes32)
        registry.registerShipment(shipmentId, cid, merkleRoot).send();
        System.out.println("[Blockchain] Registered shipment on-chain: " + shipmentId);
    }

    public void updateShipmentStatus(String shipmentId, int status, String cid, byte[] merkleRoot) throws Exception {
        ShipmentRegistry registry = loadRegistry();
        
        // Call Solidity: updateShipmentStatus(string,uint8,string,bytes32)
        registry.updateShipmentStatus(
                shipmentId, 
                BigInteger.valueOf(status), 
                cid, 
                merkleRoot
        ).send();
        System.out.println("[Blockchain] Updated status on-chain for: " + shipmentId + " to " + status);
    }

    public void transferOwnership(String shipmentId, String newOwnerAddress) throws Exception {
        ShipmentRegistry registry = loadRegistry();
        
        // Call Solidity: transferOwnership(string,address)
        registry.transferOwnership(shipmentId, newOwnerAddress).send();
        System.out.println("[Blockchain] Transferred owner on-chain for: " + shipmentId + " to " + newOwnerAddress);
    }

    public ShipmentRegistry.Shipment getShipment(String shipmentId) throws Exception {
        ShipmentRegistry registry = loadRegistry();
        return registry.getShipment(shipmentId).send();
    }
}

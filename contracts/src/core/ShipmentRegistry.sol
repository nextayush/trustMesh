// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "../interfaces/IShipmentRegistry.sol";
import "../interfaces/IABACPolicyEngine.sol";
import "../interfaces/IProvenanceAnchor.sol";
import "./ShipmentStorage.sol";
import "@openzeppelin/contracts/access/Ownable.sol";

contract ShipmentRegistry is IShipmentRegistry, Ownable {
    ShipmentStorage public immutable shipmentStorage;
    IABACPolicyEngine public policyEngine;
    IProvenanceAnchor public provenanceAnchor;

    event PolicyEngineUpdated(address indexed engine);
    event ProvenanceAnchorUpdated(address indexed anchor);

    modifier onlyAuthorized(string calldata shipmentId, string memory operation) {
        if (address(policyEngine) != address(0)) {
            require(
                policyEngine.validateAccess(msg.sender, shipmentId, operation),
                "ABAC Access Denied"
            );
        }
        _;
    }

    constructor(
        address _shipmentStorage,
        address _policyEngine,
        address _provenanceAnchor
    ) Ownable(msg.sender) {
        require(_shipmentStorage != address(0), "Invalid storage address");
        shipmentStorage = ShipmentStorage(_shipmentStorage);
        policyEngine = IABACPolicyEngine(_policyEngine);
        provenanceAnchor = IProvenanceAnchor(_provenanceAnchor);
    }

    function setPolicyEngine(address _policyEngine) external onlyOwner {
        policyEngine = IABACPolicyEngine(_policyEngine);
        emit PolicyEngineUpdated(_policyEngine);
    }

    function setProvenanceAnchor(address _provenanceAnchor) external onlyOwner {
        provenanceAnchor = IProvenanceAnchor(_provenanceAnchor);
        emit ProvenanceAnchorUpdated(_provenanceAnchor);
    }

    function registerShipment(
        string calldata shipmentId,
        string calldata contentIdentifier,
        bytes32 merkleRoot
    ) external override {
        // Anyone can register if they are initialized, or check registry if needed.
        // For security, register registers the shipment with the caller as creator/owner
        shipmentStorage.createShipment(shipmentId, contentIdentifier, merkleRoot, msg.sender);

        if (address(provenanceAnchor) != address(0)) {
            provenanceAnchor.anchorEvent(
                shipmentId,
                contentIdentifier,
                merkleRoot,
                uint8(ShipmentStatus.CREATED),
                msg.sender
            );
        }
    }

    function updateShipmentStatus(
        string calldata shipmentId,
        ShipmentStatus status,
        string calldata contentIdentifier,
        bytes32 merkleRoot
    ) external override onlyAuthorized(shipmentId, "UPDATE_STATUS") {
        shipmentStorage.updateShipment(shipmentId, status, contentIdentifier, merkleRoot);

        if (address(provenanceAnchor) != address(0)) {
            provenanceAnchor.anchorEvent(
                shipmentId,
                contentIdentifier,
                merkleRoot,
                uint8(status),
                msg.sender
            );
        }
    }

    function transferOwnership(
        string calldata shipmentId,
        address newOwner
    ) external override onlyAuthorized(shipmentId, "TRANSFER_OWNERSHIP") {
        require(newOwner != address(0), "Invalid new owner address");
        
        shipmentStorage.updateOwner(shipmentId, newOwner);

        IShipmentRegistry.Shipment memory s = shipmentStorage.getShipment(shipmentId);

        if (address(provenanceAnchor) != address(0)) {
            provenanceAnchor.anchorEvent(
                shipmentId,
                s.contentIdentifier,
                s.merkleRoot,
                uint8(s.status),
                msg.sender
            );
        }
    }

    function getShipment(
        string calldata shipmentId
    ) external view override returns (Shipment memory) {
        return shipmentStorage.getShipment(shipmentId);
    }
}

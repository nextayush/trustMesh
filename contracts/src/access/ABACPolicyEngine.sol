// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "../interfaces/IABACPolicyEngine.sol";
import "../interfaces/IShipmentRegistry.sol";
import "./AccessRegistry.sol";
import "@openzeppelin/contracts/access/Ownable.sol";

contract ABACPolicyEngine is IABACPolicyEngine, Ownable {
    AccessRegistry public immutable accessRegistry;
    address public shipmentRegistry;

    event ShipmentRegistryUpdated(address indexed registry);

    constructor(address _accessRegistry) Ownable(msg.sender) {
        require(_accessRegistry != address(0), "Invalid access registry address");
        accessRegistry = AccessRegistry(_accessRegistry);
    }

    function setShipmentRegistry(address _shipmentRegistry) external onlyOwner {
        require(_shipmentRegistry != address(0), "Invalid shipment registry address");
        shipmentRegistry = _shipmentRegistry;
        emit ShipmentRegistryUpdated(_shipmentRegistry);
    }

    function validateAccess(
        address user,
        string calldata shipmentId,
        string calldata operation
    ) external view override returns (bool) {
        if (shipmentRegistry == address(0)) {
            return false;
        }

        AccessRegistry.Attributes memory userAttr = accessRegistry.getAttributes(user);
        if (!userAttr.registered) {
            return false;
        }

        // Admins can bypass standard checks
        if (keccak256(abi.encodePacked(userAttr.role)) == keccak256(abi.encodePacked("ADMIN"))) {
            return true;
        }

        IShipmentRegistry.Shipment memory shipment = IShipmentRegistry(shipmentRegistry).getShipment(shipmentId);
        if (!shipment.exists) {
            return false;
        }

        bytes32 opHash = keccak256(abi.encodePacked(operation));

        if (opHash == keccak256(abi.encodePacked("VIEW_MANIFEST"))) {
            // Owner and creator can always view
            if (user == shipment.currentOwner || user == shipment.creator) {
                return true;
            }
            // Customs authority of destination can view
            if (keccak256(abi.encodePacked(userAttr.organization)) == keccak256(abi.encodePacked("Customs"))) {
                return true;
            }
            // Auditor organization can view
            if (keccak256(abi.encodePacked(userAttr.role)) == keccak256(abi.encodePacked("AUDITOR"))) {
                return true;
            }
            return false;
        } 
        
        if (opHash == keccak256(abi.encodePacked("UPDATE_STATUS"))) {
            // Current owner can update status
            if (user == shipment.currentOwner) {
                return true;
            }
            // A carrier belonging to the same organization as the current owner can update status
            bool isCarrier = keccak256(abi.encodePacked(userAttr.role)) == keccak256(abi.encodePacked("CARRIER"));
            AccessRegistry.Attributes memory ownerAttr = accessRegistry.getAttributes(shipment.currentOwner);
            if (isCarrier && keccak256(abi.encodePacked(userAttr.organization)) == keccak256(abi.encodePacked(ownerAttr.organization))) {
                return true;
            }
            return false;
        }

        if (opHash == keccak256(abi.encodePacked("TRANSFER_OWNERSHIP"))) {
            // Only current owner can initiate transfer
            return (user == shipment.currentOwner);
        }

        return false;
    }
}

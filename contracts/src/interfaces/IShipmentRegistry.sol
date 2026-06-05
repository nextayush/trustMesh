// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

interface IShipmentRegistry {
    enum ShipmentStatus {
        CREATED,
        PACKED,
        DISPATCHED,
        IN_TRANSIT,
        CUSTOMS_CLEARED,
        DELIVERED
    }

    struct Shipment {
        string shipmentId;
        string contentIdentifier; // CID on IPFS/MinIO
        bytes32 merkleRoot;
        ShipmentStatus status;
        uint256 timestamp;
        address currentOwner;
        address creator;
        bool exists;
    }

    function registerShipment(
        string calldata shipmentId,
        string calldata contentIdentifier,
        bytes32 merkleRoot
    ) external;

    function updateShipmentStatus(
        string calldata shipmentId,
        ShipmentStatus status,
        string calldata contentIdentifier,
        bytes32 merkleRoot
    ) external;

    function transferOwnership(
        string calldata shipmentId,
        address newOwner
    ) external;

    function getShipment(
        string calldata shipmentId
    ) external view returns (Shipment memory);
}

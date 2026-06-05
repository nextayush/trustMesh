// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "../interfaces/IShipmentRegistry.sol";
import "@openzeppelin/contracts/access/Ownable.sol";

contract ShipmentStorage is Ownable {
    mapping(string => IShipmentRegistry.Shipment) private _shipments;
    mapping(address => bool) public authorizedControllers;

    modifier onlyController() {
        require(authorizedControllers[msg.sender] || msg.sender == owner(), "Not authorized to modify storage");
        _;
    }

    constructor() Ownable(msg.sender) {}

    function setAuthorizedController(address controller, bool authorized) external onlyOwner {
        authorizedControllers[controller] = authorized;
    }

    function createShipment(
        string calldata shipmentId,
        string calldata contentIdentifier,
        bytes32 merkleRoot,
        address creator
    ) external onlyController {
        require(!_shipments[shipmentId].exists, "Shipment already exists");
        
        _shipments[shipmentId] = IShipmentRegistry.Shipment({
            shipmentId: shipmentId,
            contentIdentifier: contentIdentifier,
            merkleRoot: merkleRoot,
            status: IShipmentRegistry.ShipmentStatus.CREATED,
            timestamp: block.timestamp,
            currentOwner: creator,
            creator: creator,
            exists: true
        });
    }

    function updateShipment(
        string calldata shipmentId,
        IShipmentRegistry.ShipmentStatus status,
        string calldata contentIdentifier,
        bytes32 merkleRoot
    ) external onlyController {
        require(_shipments[shipmentId].exists, "Shipment does not exist");
        
        IShipmentRegistry.Shipment storage s = _shipments[shipmentId];
        s.status = status;
        s.contentIdentifier = contentIdentifier;
        s.merkleRoot = merkleRoot;
        s.timestamp = block.timestamp;
    }

    function updateOwner(
        string calldata shipmentId,
        address newOwner
    ) external onlyController {
        require(_shipments[shipmentId].exists, "Shipment does not exist");
        _shipments[shipmentId].currentOwner = newOwner;
    }

    function getShipment(
        string calldata shipmentId
    ) external view returns (IShipmentRegistry.Shipment memory) {
        return _shipments[shipmentId];
    }
}

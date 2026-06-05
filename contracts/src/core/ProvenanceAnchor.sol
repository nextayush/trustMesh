// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "../interfaces/IProvenanceAnchor.sol";
import "@openzeppelin/contracts/access/Ownable.sol";

contract ProvenanceAnchor is IProvenanceAnchor, Ownable {
    mapping(string => AnchorRecord[]) private _anchors;
    mapping(address => bool) public authorizedCallers;

    event EventAnchored(
        string indexed shipmentId,
        string contentIdentifier,
        bytes32 merkleRoot,
        uint8 status,
        address indexed updater
    );

    modifier onlyAuthorized() {
        require(authorizedCallers[msg.sender] || msg.sender == owner(), "Not authorized to anchor");
        _;
    }

    constructor() Ownable(msg.sender) {}

    function setAuthorizedCaller(address caller, bool authorized) external onlyOwner {
        authorizedCallers[caller] = authorized;
    }

    function anchorEvent(
        string calldata shipmentId,
        string calldata contentIdentifier,
        bytes32 merkleRoot,
        uint8 status,
        address updater
    ) external override onlyAuthorized {
        _anchors[shipmentId].push(AnchorRecord({
            contentIdentifier: contentIdentifier,
            merkleRoot: merkleRoot,
            status: status,
            timestamp: block.timestamp,
            updater: updater
        }));

        emit EventAnchored(shipmentId, contentIdentifier, merkleRoot, status, updater);
    }

    function getEventHistory(
        string calldata shipmentId
    ) external view override returns (AnchorRecord[] memory) {
        return _anchors[shipmentId];
    }
}

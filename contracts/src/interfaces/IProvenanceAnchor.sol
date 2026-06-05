// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

interface IProvenanceAnchor {
    struct AnchorRecord {
        string contentIdentifier;
        bytes32 merkleRoot;
        uint8 status;
        uint256 timestamp;
        address updater;
    }

    function anchorEvent(
        string calldata shipmentId,
        string calldata contentIdentifier,
        bytes32 merkleRoot,
        uint8 status,
        address updater
    ) external;

    function getEventHistory(
        string calldata shipmentId
    ) external view returns (AnchorRecord[] memory);
}

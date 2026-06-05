// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

interface IABACPolicyEngine {
    function validateAccess(
        address user,
        string calldata shipmentId,
        string calldata operation
    ) external view returns (bool);
}

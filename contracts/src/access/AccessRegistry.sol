// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "@openzeppelin/contracts/access/Ownable.sol";

contract AccessRegistry is Ownable {
    struct Attributes {
        string organization;
        string country;
        uint8 clearanceLevel;
        string role;
        bool registered;
    }

    mapping(address => Attributes) private _registry;

    event AttributesUpdated(
        address indexed user,
        string organization,
        string country,
        uint8 clearanceLevel,
        string role
    );

    constructor() Ownable(msg.sender) {}

    function setAttributes(
        address user,
        string calldata organization,
        string calldata country,
        uint8 clearanceLevel,
        string calldata role
    ) external onlyOwner {
        _registry[user] = Attributes({
            organization: organization,
            country: country,
            clearanceLevel: clearanceLevel,
            role: role,
            registered: true
        });

        emit AttributesUpdated(user, organization, country, clearanceLevel, role);
    }

    function getAttributes(
        address user
    ) external view returns (Attributes memory) {
        return _registry[user];
    }
}

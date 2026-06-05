import { expect } from "chai";
import { ethers } from "hardhat";
import { 
  AccessRegistry, 
  ABACPolicyEngine, 
  ProvenanceAnchor, 
  ShipmentStorage, 
  ShipmentRegistry 
} from "../typechain-types";
import { SignerWithAddress } from "@nomicfoundation/hardhat-ethers/signers";

describe("Quantum-Provenance Contracts Suite", function () {
  let accessRegistry: AccessRegistry;
  let policyEngine: ABACPolicyEngine;
  let provenanceAnchor: ProvenanceAnchor;
  let shipmentStorage: ShipmentStorage;
  let shipmentRegistry: ShipmentRegistry;

  let owner: SignerWithAddress;
  let supplier: SignerWithAddress;
  let carrier: SignerWithAddress;
  let customs: SignerWithAddress;
  let auditor: SignerWithAddress;
  let unauthorized: SignerWithAddress;

  beforeEach(async function () {
    [owner, supplier, carrier, customs, auditor, unauthorized] = await ethers.getSigners();

    // 1. Deploy Access Registry
    const AccessRegistryFactory = await ethers.getContractFactory("AccessRegistry");
    accessRegistry = await AccessRegistryFactory.deploy();

    // 2. Deploy ABAC Policy Engine
    const ABACPolicyEngineFactory = await ethers.getContractFactory("ABACPolicyEngine");
    policyEngine = await ABACPolicyEngineFactory.deploy(await accessRegistry.getAddress());

    // 3. Deploy Provenance Anchor
    const ProvenanceAnchorFactory = await ethers.getContractFactory("ProvenanceAnchor");
    provenanceAnchor = await ProvenanceAnchorFactory.deploy();

    // 4. Deploy Shipment Storage
    const ShipmentStorageFactory = await ethers.getContractFactory("ShipmentStorage");
    shipmentStorage = await ShipmentStorageFactory.deploy();

    // 5. Deploy Shipment Registry
    const ShipmentRegistryFactory = await ethers.getContractFactory("ShipmentRegistry");
    shipmentRegistry = await ShipmentRegistryFactory.deploy(
      await shipmentStorage.getAddress(),
      await policyEngine.getAddress(),
      await provenanceAnchor.getAddress()
    );

    // Authorize Shipment Registry in Storage and Anchor
    await shipmentStorage.setAuthorizedController(await shipmentRegistry.getAddress(), true);
    await provenanceAnchor.setAuthorizedCaller(await shipmentRegistry.getAddress(), true);

    // Link Registry to Policy Engine
    await policyEngine.setShipmentRegistry(await shipmentRegistry.getAddress());

    // Setup Attributes in Access Registry
    await accessRegistry.setAttributes(supplier.address, "LogiCorp", "US", 2, "SUPPLIER");
    await accessRegistry.setAttributes(carrier.address, "LogiCorp", "US", 1, "CARRIER");
    await accessRegistry.setAttributes(customs.address, "Customs", "DE", 3, "OFFICER");
    await accessRegistry.setAttributes(auditor.address, "AuditCo", "FR", 3, "AUDITOR");
    await accessRegistry.setAttributes(owner.address, "AdminOrg", "US", 4, "ADMIN");
  });

  describe("Shipment Lifecycle & Traceability", function () {
    const shipmentId = "SHIP-2026-001";
    const cid = "QmXoypizjW3WknFiJnKLwHCnL72vedxjQkDDP1mXWo6uco";
    const merkleRoot = ethers.zeroPadValue(ethers.toUtf8Bytes("merkle-root-seed"), 32);

    it("should successfully register a new shipment", async function () {
      await shipmentRegistry.connect(supplier).registerShipment(shipmentId, cid, merkleRoot);

      const shipment = await shipmentRegistry.getShipment(shipmentId);
      expect(shipment.exists).to.be.true;
      expect(shipment.shipmentId).to.equal(shipmentId);
      expect(shipment.creator).to.equal(supplier.address);
      expect(shipment.currentOwner).to.equal(supplier.address);
      expect(shipment.status).to.equal(0); // IShipmentRegistry.ShipmentStatus.CREATED
    });

    it("should reject updates from unauthorized users", async function () {
      await shipmentRegistry.connect(supplier).registerShipment(shipmentId, cid, merkleRoot);

      // Unauthorized attempts status update
      await expect(
        shipmentRegistry.connect(unauthorized).updateShipmentStatus(
          shipmentId,
          1, // PACKED
          cid,
          merkleRoot
        )
      ).to.be.revertedWith("ABAC Access Denied");
    });

    it("should allow status updates from authorized carriers inside the same organization", async function () {
      await shipmentRegistry.connect(supplier).registerShipment(shipmentId, cid, merkleRoot);

      // Carrier is from 'LogiCorp' (same as Supplier) and role is 'CARRIER'
      await shipmentRegistry.connect(carrier).updateShipmentStatus(
        shipmentId,
        1, // PACKED
        cid,
        merkleRoot
      );

      const shipment = await shipmentRegistry.getShipment(shipmentId);
      expect(shipment.status).to.equal(1);
    });

    it("should audit updates and record histories in ProvenanceAnchor", async function () {
      await shipmentRegistry.connect(supplier).registerShipment(shipmentId, cid, merkleRoot);
      await shipmentRegistry.connect(carrier).updateShipmentStatus(
        shipmentId,
        1, // PACKED
        cid,
        merkleRoot
      );

      const history = await provenanceAnchor.getEventHistory(shipmentId);
      expect(history.length).to.equal(2);
      expect(history[0].status).to.equal(0);
      expect(history[0].updater).to.equal(supplier.address);
      expect(history[1].status).to.equal(1);
      expect(history[1].updater).to.equal(carrier.address);
    });

    it("should allow owner to transfer ownership and evaluate policies correctly after transfer", async function () {
      await shipmentRegistry.connect(supplier).registerShipment(shipmentId, cid, merkleRoot);

      // Transfer to customs
      await shipmentRegistry.connect(supplier).getFunction("transferOwnership(string,address)")(shipmentId, customs.address);

      let shipment = await shipmentRegistry.getShipment(shipmentId);
      expect(shipment.currentOwner).to.equal(customs.address);

      // Old owner (supplier) should no longer be able to update status
      await expect(
        shipmentRegistry.connect(supplier).updateShipmentStatus(
          shipmentId,
          3, // IN_TRANSIT
          cid,
          merkleRoot
        )
      ).to.be.revertedWith("ABAC Access Denied");
    });
  });
});

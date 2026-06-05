import { ethers } from "hardhat";
import * as fs from "fs";
import * as path from "path";

async function main() {
  const [deployer] = await ethers.getSigners();
  console.log("=============================================");
  console.log("  Deploying Quantum-Provenance Smart Contracts");
  console.log("  Deployer Account: ", deployer.address);
  console.log("=============================================");

  // 1. Deploy AccessRegistry
  console.log("Deploying AccessRegistry...");
  const AccessRegistry = await ethers.getContractFactory("AccessRegistry");
  const accessRegistry = await AccessRegistry.deploy();
  await accessRegistry.waitForDeployment();
  const accessRegistryAddress = await accessRegistry.getAddress();
  console.log(`AccessRegistry deployed to: ${accessRegistryAddress}`);

  // 2. Deploy ABACPolicyEngine
  console.log("Deploying ABACPolicyEngine...");
  const ABACPolicyEngine = await ethers.getContractFactory("ABACPolicyEngine");
  const policyEngine = await ABACPolicyEngine.deploy(accessRegistryAddress);
  await policyEngine.waitForDeployment();
  const policyEngineAddress = await policyEngine.getAddress();
  console.log(`ABACPolicyEngine deployed to: ${policyEngineAddress}`);

  // 3. Deploy ProvenanceAnchor
  console.log("Deploying ProvenanceAnchor...");
  const ProvenanceAnchor = await ethers.getContractFactory("ProvenanceAnchor");
  const provenanceAnchor = await ProvenanceAnchor.deploy();
  await provenanceAnchor.waitForDeployment();
  const provenanceAnchorAddress = await provenanceAnchor.getAddress();
  console.log(`ProvenanceAnchor deployed to: ${provenanceAnchorAddress}`);

  // 4. Deploy ShipmentStorage
  console.log("Deploying ShipmentStorage...");
  const ShipmentStorage = await ethers.getContractFactory("ShipmentStorage");
  const shipmentStorage = await ShipmentStorage.deploy();
  await shipmentStorage.waitForDeployment();
  const shipmentStorageAddress = await shipmentStorage.getAddress();
  console.log(`ShipmentStorage deployed to: ${shipmentStorageAddress}`);

  // 5. Deploy ShipmentRegistry (The Logic Controller)
  console.log("Deploying ShipmentRegistry...");
  const ShipmentRegistry = await ethers.getContractFactory("ShipmentRegistry");
  const shipmentRegistry = await ShipmentRegistry.deploy(
    shipmentStorageAddress,
    policyEngineAddress,
    provenanceAnchorAddress
  );
  await shipmentRegistry.waitForDeployment();
  const shipmentRegistryAddress = await shipmentRegistry.getAddress();
  console.log(`ShipmentRegistry deployed to: ${shipmentRegistryAddress}`);

  // 6. Link and Authorize
  console.log("Configuring contracts relationships...");
  
  // Authorize ShipmentRegistry in Storage
  await (await shipmentStorage.setAuthorizedController(shipmentRegistryAddress, true)).wait();
  console.log("Authorized ShipmentRegistry in ShipmentStorage");

  // Authorize ShipmentRegistry in ProvenanceAnchor
  await (await provenanceAnchor.setAuthorizedCaller(shipmentRegistryAddress, true)).wait();
  console.log("Authorized ShipmentRegistry in ProvenanceAnchor");

  // Link ShipmentRegistry in PolicyEngine
  await (await policyEngine.setShipmentRegistry(shipmentRegistryAddress)).wait();
  console.log("Linked ShipmentRegistry in ABACPolicyEngine");

  console.log("=============================================");
  console.log("  Contracts Deployment and Configuration Done! ");
  console.log("=============================================");

  // Output Addresses JSON
  const addresses = {
    AccessRegistry: accessRegistryAddress,
    ABACPolicyEngine: policyEngineAddress,
    ProvenanceAnchor: provenanceAnchorAddress,
    ShipmentStorage: shipmentStorageAddress,
    ShipmentRegistry: shipmentRegistryAddress,
    deployer: deployer.address,
    timestamp: new Date().toISOString()
  };

  const outputDir = path.join(__dirname, "../build-raw");
  if (!fs.existsSync(outputDir)) {
    fs.mkdirSync(outputDir, { recursive: true });
  }

  fs.writeFileSync(
    path.join(outputDir, "deployed-addresses.json"),
    JSON.stringify(addresses, null, 2),
    "utf8"
  );
  console.log("Saved deployed addresses to contracts/build-raw/deployed-addresses.json");
}

main()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error(error);
    process.exit(1);
  });

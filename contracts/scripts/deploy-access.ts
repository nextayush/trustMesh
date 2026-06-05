import { ethers } from "hardhat";
import * as fs from "fs";
import * as path from "path";

async function main() {
  const [deployer, supplier, carrier, customs] = await ethers.getSigners();
  console.log("=============================================");
  console.log("  Seeding Access Control Attributes          ");
  console.log("=============================================");

  const addressesPath = path.join(__dirname, "../build-raw/deployed-addresses.json");
  if (!fs.existsSync(addressesPath)) {
    console.error("deployed-addresses.json not found! Deploy core contracts first.");
    process.exit(1);
  }

  const addresses = JSON.parse(fs.readFileSync(addressesPath, "utf8"));
  const accessRegistryAddress = addresses.AccessRegistry;

  const accessRegistry = await ethers.getContractAt("AccessRegistry", accessRegistryAddress);

  console.log(`Seeding attributes using AccessRegistry at: ${accessRegistryAddress}`);

  // Seed Deployer as ADMIN
  console.log(`Setting deployer attributes for: ${deployer.address}`);
  await (await accessRegistry.setAttributes(deployer.address, "AdminOrg", "US", 4, "ADMIN")).wait();

  // Seed Supplier attributes
  console.log(`Setting supplier attributes for: ${supplier.address}`);
  await (await accessRegistry.setAttributes(supplier.address, "LogiCorp", "US", 2, "SUPPLIER")).wait();

  // Seed Carrier attributes
  console.log(`Setting carrier attributes for: ${carrier.address}`);
  await (await accessRegistry.setAttributes(carrier.address, "LogiCorp", "US", 1, "CARRIER")).wait();

  // Seed Customs Officer attributes
  console.log(`Setting customs officer attributes for: ${customs.address}`);
  await (await accessRegistry.setAttributes(customs.address, "Customs", "DE", 3, "OFFICER")).wait();

  console.log("=============================================");
  console.log("  Seeding Complete! Attributes registered.   ");
  console.log("=============================================");
}

main()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error(error);
    process.exit(1);
  });

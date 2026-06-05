// contracts/scripts/extract-abi-bin.js
const fs = require('fs');
const path = require('path');

const contracts = [
  { name: 'ShipmentRegistry', path: 'src/core/ShipmentRegistry.sol/ShipmentRegistry.json' },
  { name: 'ProvenanceAnchor', path: 'src/core/ProvenanceAnchor.sol/ProvenanceAnchor.json' },
  { name: 'ABACPolicyEngine', path: 'src/access/ABACPolicyEngine.sol/ABACPolicyEngine.json' }
];

const artifactsDir = path.join(__dirname, '../artifacts');
const outputDir = path.join(__dirname, '../build-raw');

if (!fs.existsSync(outputDir)) {
  fs.mkdirSync(outputDir, { recursive: true });
}

contracts.forEach(c => {
  const jsonPath = path.join(artifactsDir, c.path);
  if (!fs.existsSync(jsonPath)) {
    console.error(`Artifact not found: ${jsonPath}. Did you run npx hardhat compile?`);
    process.exit(1);
  }

  const data = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));
  
  // Write ABI
  fs.writeFileSync(
    path.join(outputDir, `${c.name}.abi`),
    JSON.stringify(data.abi),
    'utf8'
  );

  // Write Bytecode Bin
  fs.writeFileSync(
    path.join(outputDir, `${c.name}.bin`),
    data.bytecode,
    'utf8'
  );

  console.log(`Extracted ABI and BIN for ${c.name}`);
});

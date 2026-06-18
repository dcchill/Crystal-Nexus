# Processing Machines

Processing machines turn ores, dusts, fluids, plants, chemicals, and matter into more advanced materials. Most machines use FE and have GUI-bound inventories.

## Ore and Dust Processing

### Crystal Ore Crusher

The Crystal Ore Crusher converts raw ores into dusts.

- Converts raw ores into dusts.
- Output: 2 dust by default, 3 with an Efficiency Upgrade, 4 with a Carbon Efficiency Upgrade.
- Special case: Netherite Scrap Dust recipes output 4 regardless of the normal output amount.
- Energy cost: 4,096 FE per completed operation.
- Base processing time: 100 ticks, 75 with Acceleration Upgrade, 50 with Carbon Acceleration Upgrade.
- Inventory: input, output, and upgrade slots.
- Automation can insert raw ores and upgrades, and extract finished dust.
- Energy capacity: 10,240 FE.
- Max receive/extract: 2,048 FE.

### Crystal Dust Separator

The Crystal Dust Separator converts dusts into nuggets.

- Converts dusts into nuggets.
- Output: 12 nuggets by default, 14 with an Efficiency Upgrade, 16 with a Carbon Efficiency Upgrade.
- Energy cost: 4,096 FE per completed operation.
- Base processing time: 100 ticks, 75 with Acceleration Upgrade, 50 with Carbon Acceleration Upgrade.

Use it after crushing to stretch raw ore further.

### Crystal Smelter

The Crystal Smelter is part of the crystal-tier smelting line.

Use it for improved or specialized smelting once Crystal Machine Frames and crystal power are available.

### Iron Smelter

The Iron Smelter is the lower-tier powered smelter.

Use it as a first FE-powered furnace replacement.

### Invertium Smelter

The Invertium Smelter is the Invertium-tier smelting machine.

Build it once Invertium production is established.

### Chlorophyte Smelter

The Chlorophyte Smelter is the Chlorophyte-tier smelting machine.

Use it in the Chlorophyte processing branch.

### Ultima Smelter

Now with four times the smelting.

The in-game multiblock guide notes that it can smelt four stacks at once and automatically combines nuggets into ingots. It belongs in late-game bulk processing.

## Purification and Recrystallization

### Crystal Purifier

The Crystal Purifier is used to upgrade crystals and create Crystalized Alloy.

Important progression:

- Unstable Crystal to Stable Crystal.
- Higher crystal tiers through combining and purification recipes.
- Crystalized Alloy creation.

### Metallurgic Recrystallizer

Converts ingots into raw ores. Put an Invertium Crystal in the middle slot.

- Output: 1 by default.
- Output with Efficiency Upgrade: 2.
- Not OP at all.

This machine is like a reverse-processing machine. It is useful when a recipe specifically needs raw ore rather than ingots.

## Extraction and Generation

### Extractinator

The Extractinator works like a sifter to find materials in substrate.

Use it as a material extraction machine for low-value blocks and special substrates.

### Node Miner

Place it directly above an ore node to start mining.

Nodes can be found underground around its corresponding ores would generate.

Compatible node blocks include:

- Iron Node: outputs Raw Iron and costs 1,024 FE per operation.
- Copper Node: outputs Raw Copper and costs 1,024 FE per operation.
- Gold Node: outputs Raw Gold and costs 2,048 FE per operation.
- Ancient Debris Node: outputs Netherite Scrap Dust and costs 8,192 FE per operation.

The miner has three output slots and an upgrade slot. Base processing time is 250 ticks, 200 with Acceleration Upgrade, and 75 with Carbon Acceleration Upgrade.

How to use it:

1. Place the Node Miner directly above an ore node.
2. Supply FE.
3. Put an optional upgrade in the upgrade slot.
4. Pull outputs from the three output slots with conveyor belts.

### Node Extractor

Place it directly above a fluid node to start extracting.

Compatible fluid node blocks include:

- Oil Node: fills the internal tank with Crude Oil.
- Lava Node: fills the internal tank with Lava.

Each completed cycle fills 100 mB of fluid and consumes FE. Base processing time is 25 ticks, 20 with Acceleration Upgrade, and 10 with Carbon Acceleration Upgrade.

How to use it:

1. Place the Node Extractor directly above an Oil Node or Lava Node.
2. Supply FE.
3. Add an optional upgrade.
4. Extract fluid with pipes.

### Laser Quarry

The quarry mines within its current chunk, starting below the quarry and scanning downward from the outer ring inward. It costs 1,024 FE per mined block, mines one block per work step, and pauses while redstone-powered. It skips air, fluids, unbreakable blocks, and block entities. Drops are inserted into an inventory above the quarry first, then into its internal output slots.

How to use it:

1. Place the quarry in the chunk you want to mine.
2. Put a storage block directly above it if you want drops exported automatically.
3. Supply FE and it will start right away.

## Circuit and Computation Processing

### Chlorophyte Circuit Press

The Circuit Press creates circuits and chips.

### Computation Cluster

The Computation Cluster researches or decrypts SSDs.

Related items:

- Computation Node
- Encrypted SSD
- SSD
- Rare SSD
- Epic SSD

Research SSDs at the Computation Cluster. Encrypted SSDs can be decrypted there too.

How to use it:

1. Place Computation Nodes as needed for the setup.
2. Put an Encrypted SSD into the cluster.
3. Supply FE if the cluster asks for it.
4. Let the research/decryption finish, then take the resulting Common, Rare, or Epic SSD.

### SSD

Decrypted SSDs work like basic machine upgrades but with randomized modifiers. Higher tiers have a better chance of positive multipliers and higher max multipliers, with Epic SSDs having a chance of a god roll.

## Chemical and Biological Machines

### Chemical Reaction Chamber

Important machine used for chemical recipes and materials such as Conductive Alloy, Fertilizer, Nitrile/Base Compound, Polymer, and Synthetic Rubber.


### Biomatic Composter

Uses plant mass inputs such as crops, flowers, leaves, saplings, and seaweed to produce biomass.

### Biomatic Constructor

The Biomatic Constructor turns Biomass into copies of a selected plant item. It is useful when you already have one sample plant and want to grow that item into a steady supply.

How to use it:

1. Put Biomass in the main material input.
2. Put the plant you want to duplicate in the sample/template input.
3. Supply FE.
4. Pull the duplicated plant items from the output.

Good uses:

- Turning one rare sapling into many saplings.
- Making more crop seeds for farms.
- Supplying the Biomatic Simulator with repeatable biological inputs.

### Biomatic Simulator

Simulates biological outputs.

How to use it:

1. Choose the plant/tree/mushroom simulation recipe you want.
2. Insert the required biomass.
3. Supply FE.

## Fluid Machines

### Fluid Tank

Stores fluids. Use tanks between Node Extractors, pipes, packagers, and generators.

### Fluid Packager

Use the Fluid Packager to fill Fluid Cells automatically.

How to use it:

1. Pipe Crude Oil or Gasoline into the machine.
2. Put Empty Fuel Cells in the container input.
3. Put an optional Acceleration Upgrade or Carbon Acceleration Upgrade in the upgrade slot.
4. Supply FE.

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
- Inventory: 3 slots.
- Exposed output slots: 1 and 2.
- Exposed input slots: 0 and 2.
- Energy capacity: 10,240 FE.
- Max receive/extract: 2,048 FE.

Use it as the first ore doubling machine.

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

This machine is a reverse-processing machine. It is useful when a recipe specifically needs raw ore rather than ingots.

## Extraction and Generation

### Extractinator

The Extractinator has recipes for materials such as cobble, cobbled deepslate, gravel, soul sand, tarrock, and iron nugget outputs.

Use it as a material extraction machine for low-value blocks and special substrates.

### Ore Processor

The Ore Processor has a GUI and tank states. It appears to process or generate ores using fluid and FE.

Fill it with Crystal Gloop and FE to start generating ores.

### Node Miner

Place it directly above an ore node to start mining.

Compatible node blocks include:

- Iron Node: outputs Raw Iron and costs 1,024 FE per operation.
- Copper Node: outputs Raw Copper and costs 1,024 FE per operation.
- Gold Node: outputs Raw Gold and costs 2,048 FE per operation.
- Ancient Debris Node: outputs Netherite Scrap Dust and costs 8,192 FE per operation.

The miner has three output slots and an upgrade slot. Base processing time is 250 ticks, 200 with Acceleration Upgrade, and 75 with Carbon Acceleration Upgrade.

### Node Extractor

Place it directly above a fluid node to start extracting.

Compatible fluid node blocks include:

- Oil Node: fills the internal tank with Crude Oil.
- Lava Node: fills the internal tank with Lava.

Each completed cycle fills 100 mB of fluid and consumes FE. Base processing time is 25 ticks, 20 with Acceleration Upgrade, and 10 with Carbon Acceleration Upgrade.

### Laser Quarry

The quarry mines within its current chunk, starting below the quarry and scanning downward from the outer ring inward. It costs 1,024 FE per mined block, mines one block per work step, and pauses while redstone-powered. It skips air, fluids, unbreakable blocks, and block entities. Drops are inserted into an inventory above the quarry first, then into its internal output slots.

## Circuit and Computation Processing

### Chlorophyte Circuit Press

The Circuit Press creates circuits and chips.

Related items:

- Blank Circuit
- Carbon Chip
- Omega Chip

Use it to move from basic machine frames into advanced machines and upgrades.

### Computation Cluster

The Computation Cluster researches or decrypts SSDs.

Related items:

- Computation Node
- Encrypted SSD
- SSD
- Rare SSD
- Epic SSD

Research SSDs at the Computation Cluster. Encrypted SSDs can be decrypted there too.

## Chemical and Biological Machines

### Chemical Reaction Chamber

Used for chemical recipes and materials such as Conductive Alloy, Fertilizer, Nitrile/Base Compound, Polymer, and Synthetic Rubber.

Use it once the mod asks for polymer or chemical intermediates.

### Biomatic Composter

Uses biological inputs such as crops, flowers, leaves, saplings, and seaweed to produce biomass-related outputs.

Related outputs/items:

- Biomass
- Biomass Fuel
- Bonemeal conversion

### Biomatic Constructor

Part of the biological machine chain. Use it with Biomatic Simulation recipes and biological resources.

### Biomatic Simulator

Simulates biological outputs. Recipes include many tree and mushroom variants:

- Acacia
- Birch
- Cherry
- Crimson
- Dark Oak
- Jungle
- Oak
- Spruce
- Brown Mushroom
- Red Mushroom

## Fluid Machines

### Fluid Tank

Stores fluids. Use tanks between Node Extractors, pipes, packagers, and generators.

### Fluid Packager

Use the Fluid Packager to fill Fluid Cells automatically.

How it works:

- Slot 0 takes Empty Fuel Cells.
- Slot 1 receives filled cells.
- Slot 2 accepts acceleration upgrades.
- 250 mB Crude Oil creates one Oil Fluid Cell.
- 250 mB Gasoline creates one Gasoline Fuel Cell.
- Each filled cell costs 4,096 FE.
- Base processing time is 100 ticks, 75 with Acceleration Upgrade, 50 with Carbon Acceleration Upgrade.

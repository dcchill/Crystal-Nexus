# Energy and Power

Crystal Nexus uses FE for most machines. Power can come from generators, reactors, steam, matter conversion, singularities, and late-game multiblocks.

## Energy Storage

### Battery Cell Block

The Battery Cell block stores FE for machine networks and can push FE to neighboring energy receivers.

- Capacity: 4,096,000 FE.
- Adjacent batteries combine their capacity.
- Adjacent Battery blocks balance energy toward an average, then push excess FE to non-battery neighbors.
- Energy receive/extract: up to 4,096,000 FE in the element definition.
- Use: place next to machines, cables, or other batteries to create larger buffers.

How to use it:

1. Place it next to an Energy Generator, cable, or powered machine.
2. Place more Battery Cells directly touching it if you want a larger shared buffer.
3. Put machines next to the battery or connect them with cables.
4. The battery will balance with adjacent Battery blocks and push FE to nearby receivers.

### Item Battery Cells

The mod includes several battery item tiers:

- Battery Cell
- Dense Battery Cell
- Carbon Battery Cell
- Dark Matter Battery Cell

These are important for portable powered tools. For example, the Mining Laser drains FE from energy-capable battery items in the player inventory and offhand.

### Battery Monitor

The Battery Monitor shows the current / max FE a battery system can hold.  Place it connected to a Battery multiblock.

## Energy Cables

### Basic Energy Cable, Energy Cable, and Energy Cable Mk 2

These cables come in three different tiers, with each level increasing the maximum power transfer.

- Basic Energy Cable: 1,024 FE/t
- Energy Cable: 51,200 FE/t
- Energy Cable Mk 2: 512,000 FE/t

### AOE Charger

The AOE Charger fills energy-capable items in nearby player inventories.

- Base range: 24 blocks.
- Range Upgrade: 48 blocks.
- Carbon Range Upgrade: 64 blocks.
- Base transfer: 512 FE/t before upgrade and SSD modifiers.
- Max transfer multiplier: 8x by default.

## Generators

### Piston Generator

The Piston Generator is an early or mid-game generator with its own GUI.

Use it when you need FE before more advanced steam or reactor infrastructure is online.

How to use it:

1. Place it near your first machines or a Battery Cell.
2. Add the required fuel/input from its GUI.
3. Route FE into cables, batteries, or nearby machines.

### Invertium Piston Generator

The Invertium Piston Generator is the higher-tier counterpart to the Piston Generator.

Use it after unlocking Invertium materials.

How to use it:

1. Replace or supplement early Piston Generators once you have Invertium.
2. Feed it the required generator input.
3. Buffer the output in batteries before sending it to larger machine lines.

### Steam Engine

The Steam Engine consumes Steam from its fluid tank and generates FE internally while its progress runs.

- Requires at least 1,000 mB Steam to run.
- Drains 1,000 mB Steam when a cycle completes.
- Generates 64 FE/t internally during the cycle before pushing stored FE to adjacent energy receivers.

How to use it:

1. Pipe Steam into the Steam Engine.
2. Keep at least 1,000 mB Steam available for each cycle.
3. Put a Battery Cell, cable, or machine next to it to receive FE.
4. Add FE Efficiency Upgrades if your setup supports them.

### High Pressure Steam Engine

This is the upgraded steam engine tier.

- Requires at least 1,000 mB Steam to run.
- Drains 1,000 mB Steam when a cycle completes.
- Generates 256 FE/t internally during the cycle before pushing stored FE to adjacent energy receivers.

How to use it:

1. Use it the same way as the normal Steam Engine.
2. Feed it a steady Steam supply.
3. Give it strong output storage or transfer, because it produces more FE per tick.

## Steam Chain

### Steam Collector

- Collects steam from radioactive material in water.
- Also collects steam from Steam Chambers placed under it.

How to use it:

1. Place it directly above a Steam Chamber for the cleanest setup.
2. Pipe Steam out of the collector into Steam Engines.
3. For radioactive-water collection, place the collector above water with radioactive material below it.

### Steam Chamber

- Requires a Steam Collector directly above it.
- Requires a water bucket in the water input.
- Requires an item tagged as `crystalnexus:steam_fuel` (coal, charcoal, or a Coal Singularity).
- Sets itself running while valid; the Steam Collector above fills itself with 25 mB Steam per tick while the chamber is running.
- Coal Singularity works as non-consumed steam fuel; other fuels are consumed when the chamber completes a cycle.

The normal setup is Steam Chamber below, Steam Collector above, then fluid pipes from the collector to Steam Engines.

How to use it:

1. Place the Steam Chamber.
2. Place a Steam Collector directly above it.
3. Put a water bucket in the water input.
4. Put steam fuel in the fuel input.
5. Pipe the collector into Steam Engines.

## Reactor Power

The reactor system uses a multiblock-like set of blocks:

- Reactor Frame
- Reactor Computer
- Reactor Core
- Reactor Energy Output
- Reactor Fluid Input
- Reactor Waste Output
- Reactor Upgrade Chip
- Reactor Permafrost Upgrade Chip

The reactor GUI displays stock energy and fluid. The Permafrost Upgrade removes the need for coolant, while the Reactor Upgrade increases energy produced.

The gamerule `disableMeltdowns` disables reactor meltdowns.

## Late-Game Power

### Zero Point

Unlimited power.

Zero Point is ultimate endgame energy. It is a massive multiblock with a diameter of 25 blocks and a material list including:

- 1 Zero Point
- 1 Zero Point Core
- 52 Carbon Machine Frames
- 174 Carbon Fiber Blocks
- 15 Carbon Fiber Glass

Once built, it generates FE without fuel or coolant.

- Output cap: 1,024,000 FE/t per side by default.

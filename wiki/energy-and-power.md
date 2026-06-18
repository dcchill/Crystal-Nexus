# Energy and Power

Crystal Nexus uses FE for most machines. Power can come from crystals, generators, reactors, steam, matter conversion, singularities, and late-game multiblocks.

## Energy Storage

### Battery Cell Block

The Battery Cell block stores FE for machine networks and can push FE to neighboring energy receivers.

- Capacity: 4,096,000 FE.
- Adjacent batteries combine their capacity.
- Adjacent Battery blocks balance energy toward an average, then push excess FE to non-battery neighbors.
- Energy receive/extract: up to 4,096,000 FE in the element definition.
- Use: place next to machines, cables, or other batteries to create larger buffers.

How to use it:

1. Place it next to an Energy Generator, cable, beam receiver, or powered machine.
2. Place more Battery Cells directly touching it if you want a larger shared buffer.
3. Put machines next to the battery or connect them with cables/beams.
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

## Cables and Beam Transfer

### Basic Energy Cable, Energy Cable, and Energy Cable Mk 2

These cables come in three different tiers, with each level increasing the maximum power transfer.

- Basic Energy Cable: 1,024 FE/t
- Energy Cable: 51,200 FE/t
- Energy Cable Mk 2: 512,000 FE/t

### Crystal Energy Beam

- Transports energy through a beam.
- Aim the beam at a block that accepts energy.
- Passes through transparent blocks.
- It transfers up to 81,920 FE/t into the first energy receiver it finds.
- Energy Refractors and Energy Splitters can continue the beam path by receiving the beam origin data.

How to use it:

1. Place the Energy Beam so it points toward the receiver or next beam part.
2. Keep the path clear or use laser-transparent blocks.
3. Use an Energy Refractor when the beam needs to turn.
4. Use an Energy Splitter when you want one beam path to feed more than one route.

### Energy Refractor

Redirects an energy beam. Use it when a straight beam path cannot reach the target machine.

### Energy Splitter

Splits an energy beam by diverting a portion to a side output while letting the rest pass straight through.

### Conductive Energy System

The Conductive Variants send more FE/t than the basic energy beams:

- Conductive Energy Beam
- Conductive Energy Refractor
- Conductive Energy Splitter

## Crystal-Based Generation

### Crystal Energy Siphon

The Crystal Energy Siphon generates FE from End Crystals in range when supplied with an energy crystal. It inserts generated FE into the energy-capable block directly below it.

- Stable Energy Crystal: 512 FE/t.
- Controlled Energy Crystal: 1,024 FE/t.
- Regulated Energy Crystal: 2,048 FE/t.
- Ultimate Energy Crystal: 4,096 FE/t.
- Blutonium Energy Crystal: 8,192 FE/t.
- Godlike Energy Crystal: 10,240 FE/t.
- Dragon Energy Crystal: 2,048 FE/t.
- Each value is multiplied by the number of End Crystals found in range.
- Base End Crystal search reach is about 3 blocks from the Siphon.
- Range Upgrade raises that search reach to about 4.5 blocks.
- Carbon Range Upgrade raises it to about 6 blocks.
- The inserted crystal item takes random durability damage while at least one End Crystal is being used.

How to use it:

1. Place the machine above the block that should receive FE, such as a Battery Cell or machine.
2. Put an Energy Crystal in the main crystal slot.
3. Place End Crystals within range.
4. Add a Range Upgrade or Carbon Range Upgrade if the End Crystals are farther away.
5. Make sure the block below can receive FE.

## Generators

### Piston Generator

The Piston Generator is an early or mid-game generator with its own GUI.

Use it when you need FE before more advanced crystal, steam, or reactor infrastructure is online.

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
4. Add Efficiency Upgrades if your setup supports them.

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
- Requires an item tagged as `crystalnexus:steam_fuel` (blutonium items).
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

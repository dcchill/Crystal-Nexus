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

### Item Battery Cells

The mod includes several battery item tiers:

- Battery Cell
- Dense Battery Cell
- Carbon Battery Cell
- Dark Matter Battery Cell

These are important for portable powered tools. For example, the Mining Laser drains FE from energy-capable battery items in the player inventory and offhand.

### Battery Monitor

The Battery Monitor has a GUI and tick behavior. It is intended for checking or managing battery networks. Exact display behavior should be checked in-game, but it belongs near battery banks.

## Cables and Beam Transfer

### Basic Energy Cable, Energy Cable, and Energy Cable Mk 2

These blocks are the conventional cable tiers. Use them for compact local machine wiring.

- Basic Energy Cable: early network option.
- Energy Cable: standard network option.
- Energy Cable Mk 2: higher-tier network option.

### Crystal Energy Beam

- Transports energy through a beam.
- Aim the beam at a block that accepts energy.
- When it has stored FE, it searches up to 8 blocks in its configured direction, passing through blocks tagged `crystalnexus:lasertransparent`.
- It transfers up to 81,920 FE/t into the first energy receiver it finds.
- Energy Refractors and Energy Splitters can continue the beam path by receiving the beam origin data.

### Energy Refractor

Redirects an energy beam. Use it when a straight beam path cannot reach the target machine.

### Energy Splitter

Splits an energy beam by diverting a portion to a side output while letting the rest pass straight through.

### Conductive Energy System

Conductive variants exist for more advanced beam networks:

- Conductive Energy Beam
- Conductive Energy Refractor
- Conductive Energy Splitter

The conductive system uses the same general beam/refractor/splitter vocabulary. Check the conductive procedures before documenting exact differences from the normal beam line.

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

### Energy Extractor

- Converts EE-Matter back into FE.
- Use it as part of the late-game matter-to-energy loop.

## Generators

### Piston Generator

The Piston Generator is an early or mid-game generator with its own GUI.

Use it when you need FE before more advanced crystal, steam, or reactor infrastructure is online.

### Invertium Piston Generator

The Invertium Piston Generator is the higher-tier counterpart to the Piston Generator.

Use it after unlocking Invertium materials.

### Steam Engine

The Steam Engine consumes Steam from its fluid tank and generates FE internally while its progress runs.

- Requires at least 1,000 mB Steam to run.
- Drains 1,000 mB Steam when a cycle completes.
- Generates 64 FE/t internally during the cycle before pushing stored FE to adjacent energy receivers.

### High Pressure Steam Engine

This is the upgraded steam engine tier.

- Requires at least 1,000 mB Steam to run.
- Drains 1,000 mB Steam when a cycle completes.
- Generates 256 FE/t internally during the cycle before pushing stored FE to adjacent energy receivers.

## Steam Chain

### Steam Collector

- Collects steam from radioactive material in water.
- Also collects steam from Steam Chambers placed under it.

### Steam Chamber

- Requires a Steam Collector directly above it.
- Requires a water bucket in slot 0.
- Requires an item tagged as `crystalnexus:steam_fuel` in slot 2.
- Sets itself running while valid; the Steam Collector above fills itself with 25 mB Steam per tick while the chamber is running.
- Coal Singularity works as non-consumed steam fuel; other fuels are consumed when the chamber completes a cycle.

The normal setup is Steam Chamber below, Steam Collector above, then fluid pipes from the collector to Steam Engines.

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

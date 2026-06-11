# Multiblocks and Late Game

Crystal Nexus has several large systems that behave like multiblocks or require coordinated controller blocks.

## Reactor

Main blocks:

- Reactor Frame
- Reactor Computer
- Reactor Core
- Reactor Energy Output
- Reactor Fluid Input
- Reactor Waste Output

Related items:

- Reactor Upgrade Chip
- Reactor Permafrost Upgrade Chip
- Blutonium Waste
- Pure Blutonium
- Coal Singularity

The reactor GUI tracks stock energy and fluid. The Energy Output block exports power, Fluid Input handles coolant or fluid supply, and Waste Output handles byproducts.

Important notes:

- Reactor Upgrade Chip increases energy produced by the reactor.
- Reactor Permafrost Upgrade Chip removes the need for coolant.
- The gamerule `disableMeltdowns` disables reactor meltdowns.
- Coal Singularity is an infinite fuel source and works for reactors.
- Pure Blutonium is 75% more efficient than normal Blutonium.

## Reaction Chamber

Main blocks:

- Reaction Chamber Frame
- Reaction Chamber Core
- Reaction Chamber Computer
- Reaction Chamber Energy Input

The Reaction Chamber Computer checks for a Reaction Chamber Core next to it, then uses the multiblock checker to decide whether the controller can run.

How it works:

- Output slot accepts EE-Matter or empty space.
- Each completed operation creates 1 EE-Matter.
- Each operation costs 10,240,000 FE.
- Base processing time is 50 ticks, 25 with Acceleration Upgrade, and 5 with Carbon Acceleration Upgrade.

Related reaction types include:

- Beam Reaction with Endstone
- Beam Reaction with End Crystal
- Beam Reaction with Wither Skull
- Beam Reaction with Chlorophyte
- Beam Reaction with Chlorophyte Block
- Beam Reaction EE

## Particle Accelerator

Main blocks:

- Particle Accelerator Controller
- Particle Accelerator Tube
- Electromagnet

- Increases processing speed of Particle Accelerator.
- At least one needed.

Use the Accelerator Controller as the main GUI/controller. Tubes form the accelerator structure, while Electromagnets improve speed.

How it works:

- The controller accepts either a linear accelerator path or a horizontal ring.
- Path length must be at least 5 blocks and at most 64 blocks.
- At least 1 Electromagnet is required.
- Tubes and Electromagnets form the accelerator path.
- The accelerator drains 5,120 FE/t total, split across all Electromagnets.
- More Electromagnets reduce processing time with diminishing returns, down to a 100 tick floor.
- Inputs are unordered in slots 0, 2, 3, and 4.
- Output is slot 1.
- Use JEI for the exact accelerator input/output list.

## Matter and Singularity Systems

### Matter Transmutation Table

The Matter Transmutation Table has a GUI with a Craft button. It handles matter recipes and expensive transformations.

Known transmutation recipe targets include:

- Blaze Rod
- Compound-E
- Diamond
- Godlike Crystal
- Gold
- Gunpowder
- Iron
- Netherite Scrap
- Nether Star
- Obsidian
- Redstone
- Soul Sand
- Zero Point

### Singularity Compressor

The Singularity Compressor creates singularities from large quantities of resources.

Singularities include:

- Iron
- Diamond
- Gold
- Copper
- Redstone
- Quartz
- Coal
- Energy

### Matter Matrix

Converts items into EE-Matter.

Use it to turn items into EE-Matter, then feed the Energy Extractor if you want to convert EE-Matter back into FE.

## Zero Point

Zero Point is the extreme late-game power source.

Guide text:

- Massive multiblock.
- Diameter of 25 blocks.
- Generates FE with no requirements.
- Ultimate endgame energy.

Material list:

- 1 Zero Point
- 1 Zero Point Core
- 52 Carbon Machine Frames
- 174 Carbon Fiber Blocks
- 15 Carbon Fiber Glass

## Ultima Smelter

The Ultima Smelter is an advanced smelting system.

Guide text indicates:

- Smelts four stacks at once.
- Automatically combines nuggets into ingots.
- Combines crushing and smelting style processing.

Use it for high-throughput late-game processing.

## Blueprint Creator

Blocks:

- Blu-print Base
- Blu-print Frame
- Blu-print Controller

Build steps from the guide:

1. Build a full floor from Blu-print Base.
2. Add Blu-print Frame pillars on each corner.
3. Connect the pillars across the top.
4. Place the controller near the base.
5. Enter a name and press Save.
6. Everything inside the volume is saved as a schematic usable by the Build Gun.

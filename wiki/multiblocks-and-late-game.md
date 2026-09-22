# Multiblocks and Late Game

Crystal Nexus has several large systems that behave like multiblocks or require coordinated controller blocks.

## Reactor

Main blocks:

- Reactor Frame
- Reactor Computer
- Reactor Core
- Machine Energy Output
- Machine Fluid Input
- Reactor Waste Output
- Reactor Control Rod
- Carbon Moderator
- Neutron Reflector
- Coolant Channel
- Heat Conductor

Related items:

- Reactor Upgrade Chip
- Reactor Permafrost Upgrade Chip
- Blutonium Waste
- Blutonium Fuel Cell
- Spent Reactor Cell

The controller GUI pages through every Reactor Core and its three fuel cell slots, and tracks energy, coolant, and heat. The Energy Output block exports power, Fluid Input supplies coolant to connected Coolant Channels, and Waste Output collects Blutonium Waste. Multiblock Item Inputs load fuel cells into empty core slots; Multiblock Item Outputs collect spent cells when space is available.

Internal layout rules:

- Every Reactor Core column needs a Control Rod directly above it; right-click the rod to set its insertion and throttle that column.
- Carbon Moderators placed between two cores improve fuel efficiency and reduce heat. Neutron Reflectors beside a core increase its output.
- Coolant Channels must connect to a Machine Fluid Input. Heat Conductors extend cooling from the core to connected channels up to four blocks away.

Important notes:

- Reactor Upgrade Chip increases energy produced by the reactor.
- Reactor Permafrost Upgrade Chip removes the need for coolant.
- The gamerule `disableMeltdowns` disables reactor meltdowns.
- Three Blutonium Fuel Cells give a core full output; fewer cells proportionally reduce FE and heat.
- Existing fuel left in the controller's former fuel slot is returned when its GUI is opened; it no longer powers the reactor.

## Reaction Chamber

Main blocks:

- Reaction Chamber Frame
- Reaction Chamber Core
- Reaction Chamber Computer
- Machine Energy Input

The Reaction Chamber Computer is the controller for the Reaction Chamber, and is where the EE-Matter is created.

How it works:

- Output slot accepts EE-Matter or empty space.
- One EE-Matter costs 10,240,000 FE.
- Base processing time is 50 ticks, 25 with Acceleration Upgrade, and 5 with Carbon Acceleration Upgrade.

## Particle Accelerator

Main blocks:

- Particle Accelerator Controller
- Particle Accelerator Tube
- Electromagnet
    - Increases processing speed of Particle Accelerator.
    - At least one needed.

Use the Accelerator Controller as controller. Tubes form the accelerator structure, while Electromagnets improve speed.

How it works:

- The Particle Accelerator must be bult in a flat square, with a controller and at least one Electromagnet.
- Path length must be at least 5 blocks and at most 64 blocks.
- At least 1 Electromagnet is required.
- Tubes and Electromagnets form the accelerator path.
- The accelerator drains 5,120 FE/t total, split across all Electromagnets.
- More Electromagnets reduce processing time with diminishing returns, down to a 100 tick floor.

## Matter and Singularity Systems

### Matter Transmutation Table

The Matter Transmutation Table works like an advanced powered crafting table. It handles special recipes and matter transmutations.

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
- Outputs up to 1,024,000 FE/t per side by default.
- Ultimate endgame energy.

## Ultima Smelter

The Ultima Smelter is an advanced smelting system.

Guide text indicates:

- Smelts four stacks at once.
- Automatically combines nuggets into ingots.
- Combines crushing and smelting style processing.

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

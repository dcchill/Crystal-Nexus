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

The controller GUI pages through every Reactor Core and its three fuel cell slots, and tracks energy, coolant, and heat. The Energy Output block exports power, Fluid Input supplies the shared reactor coolant tank, and Waste Output collects Blutonium Waste. Multiblock Item Inputs load fuel cells into empty core slots; Multiblock Item Outputs collect spent cells when space is available.

Internal layout rules:

- Every Reactor Core column needs a Control Rod directly above it; right-click the rod to set its insertion and throttle that column.
- Carbon Moderators placed between two cores improve fuel efficiency and reduce heat. Neutron Reflectors beside a core increase its output.
- A Machine Fluid Input anywhere on the reactor shell supplies all Coolant Channels. Heat Conductors extend cooling from the core to connected channels up to four blocks away.

In the Reactor Multiblock Guide, wait for optimization to finish and click **Copy to Multiblock Plans**. Carry a blank plans item, or hold an existing plans item to overwrite it. Right-click a placed Reactor Computer once to preview the saved design and again to build it using materials from your inventory. The design rotates with the controller, restores the selected rod insertion, and remains on the plans item for reuse.

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

## Plasma Generator component grid

The formed Plasma Generator uses a 5 by 9 GUI grid. Each slot holds one Plasma Injector, Ferrosteel Heatsink, or Induction Coil. Injectors and Coils affect four directly touching slots; Heatsinks affect all eight neighbors, including diagonals. Components can be rearranged while running; their slots retain heat even after removal.

- **Plasma Injector:** uses half its calculated Argon throughput: 4.5 mB/t base, plus 1.5 per adjacent Injector and minus 0.5 per adjacent Coil. Fractional Argon use averages across ticks. It produces one plasma unit per mB and heat equal to throughput times (1 + adjacent Injector count).
- **Ferrosteel Heatsink:** provides up to 12 heat/t cooling to each Injector in all eight neighboring slots; multiple Heatsinks stack. Each Heatsink within those eight slots of a Coil reduces its FE conversion efficiency by 20%, down to a minimum of zero.
- **Induction Coil:** receives an equal share of plasma from each adjacent Injector and processes at most 8 plasma/t. Each Coil adjacent to an Injector adds one extraction stack for that Injector, multiplying its processed plasma's 80,000 FE/unit conversion before Heatsink interference. Extra Coils add backpressure but also increase extraction; excess plasma is wasted.

While generating, Injectors also gain 1 heat per 10,000 FE/t they produce. Passive cooling removes 1 heat/t plus 1% of the slot's current heat, so operating temperature settles according to FE output and Heatsink cooling. High output or insufficient cooling can still reach the 10,000-heat rupture limit. Cooling continues while idle. Rupture vents Argon, destroys heating cores, releases plasma, and resets heat. Released plasma rises through connected air, including downward detours through bottom breaches when they lead to a higher space. It disappears upon reaching the top buildable block; below that limit, it settles and stops moving when no higher space is reachable. Installed components and stored FE remain. Breaking a running controller also releases its plasma and drops its components. Normal shutdown removes the contained plasma trail.

The Argon buffer holds 10,000 mB. Injection pauses when there is insufficient Argon for a complete grid tick or the internal FE buffer is full. Unconnected Injectors still consume fuel and generate heat. Partial FE capacity discards excess output.

Eight isolated Heatsink-Injector-Coil groups produce 5,120,000 FE/t at 64 mB Argon/t with stable heat. Use alternating rows with `H I C C I H H I C`, then an empty row, the same full row, another empty row, and `H I C C I H . . .` (H = Heatsink, I = Injector, C = Coil). Alternating group orientation keeps Coils away from Heatsinks. Clustering Injectors increases throughput and heat, while adding Coils reduces throughput and adding Heatsinks near Coils reduces efficiency. The GUI reports actual output, demand, local heat, and component interactions. Hover a component to highlight its affected slots in green; holding a component over a grid slot previews its placement range.

Existing generators keep their fuel and FE but start with an empty component grid. Install components before operating them.

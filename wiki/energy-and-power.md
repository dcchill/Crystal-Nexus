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

### Gasoline Generator

Build a 3x3 Insulated Azurine Casing tube from `diesel_generator.nbt`: controller at one center end, then extend its centerline to 3-16 Gasoline Generator Driveshafts and cap the opposite end with casing. The all-casing structure is valid; optionally replace up to four casing blocks with Machine Fluid Inputs and up to two with Machine Energy Outputs. Output and fuel use scale linearly per driveshaft; Overfuel generates four times the FE at the same fuel rate.

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
- Machine Energy Output
- Machine Fluid Input
- Reactor Waste Output
- Reactor Control Rod
- Carbon Moderator
- Neutron Reflector
- Coolant Channel
- Heat Conductor
- Reactor Upgrade Chip
- Reactor Permafrost Upgrade Chip

Each Reactor Core holds three Blutonium Fuel Cells. Craft three cells from a Blutonium Ingot and three Iron Nuggets. The controller GUI pages through all cores and lets you insert or remove their cells; connected Multiblock Item Inputs also load empty slots. One loaded cell produces one third of a fully loaded core's FE and heat. Cells wear down as the reactor runs, and connected Multiblock Item Outputs collect spent cells. Fuel left in an old controller is returned when its GUI opens.

The GUI also displays stored energy, coolant, temperature, and reactor status. Each Reactor Core column needs a Control Rod above it, which can be inserted to throttle that column. Carbon Moderators between cores improve fuel efficiency and reduce heat, while adjacent Neutron Reflectors increase output. A Fluid Input anywhere on the reactor shell supplies all Coolant Channels; Heat Conductors can link core heat to those channels from up to four blocks away. The Permafrost Upgrade reduces coolant demand, while the Reactor Upgrade increases energy produced.

The gamerule `disableMeltdowns` disables reactor meltdowns.

## Late-Game Power

### Solar Engine

The Solar Engine generates FE from a contained star. Stars lose durability while the engine operates: the current containment-stress percentage is also the chance each tick to consume one durability. When durability runs out, the installed star becomes a Dead Star.

- Yellow Dwarf Star: 1,024 durability
- Orange Star: 2,048 durability
- Blue Star: 4,096 durability
- Pink Star: 8,192 durability

Star durability cannot be enchanted or repaired by combining stars in a crafting grid or an anvil. Lower extraction and sufficient water cooling reduce containment stress and extend star lifetime.

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

## Plasma Generator component grid

The formed Plasma Generator uses a 5 by 9 GUI grid. Each slot holds one Plasma Injector, Ferrosteel Heatsink, or Induction Coil. Injectors and Coils affect four directly touching slots; Heatsinks affect all eight neighbors, including diagonals. Components can be rearranged while running; their slots retain heat even after removal.

- **Plasma Injector:** uses half its calculated Argon throughput: 4.5 mB/t base, plus 1.5 per adjacent Injector and minus 0.5 per adjacent Coil. Fractional Argon use averages across ticks. It produces one plasma unit per mB and heat equal to throughput times (1 + adjacent Injector count).
- **Ferrosteel Heatsink:** provides up to 12 heat/t cooling to each Injector in all eight neighboring slots; multiple Heatsinks stack. Each Heatsink within those eight slots of a Coil reduces its FE conversion efficiency by 20%, down to a minimum of zero.
- **Induction Coil:** receives an equal share of plasma from each adjacent Injector and processes at most 8 plasma/t. Each Coil adjacent to an Injector adds one extraction stack for that Injector, multiplying its processed plasma's 80,000 FE/unit conversion before Heatsink interference. Extra Coils add backpressure but also increase extraction; excess plasma is wasted.

While generating, Injectors also gain 1 heat per 10,000 FE/t they produce. Passive cooling removes 1 heat/t plus 1% of the slot's current heat, so operating temperature settles according to FE output and Heatsink cooling. High output or insufficient cooling can still reach the 10,000-heat rupture limit. Cooling continues while idle. Rupture vents Argon, destroys heating cores, releases plasma, and resets heat. Released plasma rises through connected air, including downward detours through bottom breaches when they lead to a higher space. It disappears upon reaching the top buildable block; below that limit, it settles and stops moving when no higher space is reachable. Installed components and stored FE remain. Breaking a running controller also releases its plasma and drops its components. Normal shutdown removes the contained plasma trail.

The Argon buffer holds 10,000 mB. Injection pauses when there is insufficient Argon for a complete grid tick or the internal FE buffer is full. Unconnected Injectors still consume fuel and generate heat. Partial FE capacity discards excess output.

Eight isolated Heatsink-Injector-Coil groups produce 5,120,000 FE/t at 64 mB Argon/t with stable heat. Use alternating rows with `H I C C I H H I C`, then an empty row, the same full row, another empty row, and `H I C C I H . . .` (H = Heatsink, I = Injector, C = Coil). Alternating group orientation keeps Coils away from Heatsinks. Clustering Injectors increases throughput and heat, while adding Coils reduces throughput and adding Heatsinks near Coils reduces efficiency. The GUI reports actual output, demand, local heat, and component interactions. Hover a component to highlight its affected slots in green; holding a component over a grid slot previews its placement range.

Existing generators keep their fuel and FE but start with an empty component grid. Install components before operating them.

# Automation Notes

Use these notes when you start wiring machines into full factory lines.

## Basic FE Machine Line

A compact early processing line can be:

1. Energy Generator creates FE.
2. A Battery Cell is optional, but useful as a buffer.
3. Battery Cell, cable, beam transfer, or another FE source powers the Crystal Ore Crusher.
4. Crusher outputs into Crystal Dust Separator.
5. Separator outputs into Smelter.
6. Item Collector or Conveyor Belts move outputs to storage.

Add Efficiency Upgrades to machines that support them when output quantity matters more than power use.

## Ore Doubling and Beyond

The standard visible chain is:

- Raw ore to dust in Crystal Ore Crusher.
- Dust to nuggets in Crystal Dust Separator.
- Nuggets to ingots by crafting, smelting line, or late-game automatic combining.

The Ultima Smelter automatically combines nuggets into ingots, making it valuable for bulk ore processing.

## Power Layout Tips

- Put Battery Cells next to each other when you want a larger shared buffer.
- If your Energy Generator is a Crystal Energy Siphon, place the receiving block directly below it.
- Use beam transfer when cable paths are awkward.
- Use Refractors for turns and Splitters for branching beam networks.
- Use Tesseracts for long-distance FE transfer once Link Cards are available. The input Tesseract reads the linked coordinates from the card in slot 0 and sends up to 81,920 FE/t to a linked Tesseract Output.

## Item Routing Tips

- Use Item Collectors for loose drops. They pull item entities toward the block, store matching items in slot 0, use slot 2 as an optional filter, and stop while redstone-powered.
- Use Smart Splitters for one-item-at-a-time routing. Items enter from the back, filters are left/forward/right, and unmatched items try empty filter lanes before going to the overflow buffer.
- Leave a Smart Splitter lane filter empty only when that lane should accept unmatched items.
- Use Conveyor Belt Output variants to pull up to 32 items from the inventory behind them, and Conveyor Belt Input variants to insert belt items into the inventory in front.
- Use stacked Item Elevators for compact vertical factories. Upward elevators move one item per tick into the matching elevator above; downward elevators do the same downward.

## Fluid Automation Tips

- Use Node Extractors on Oil Nodes or Lava Nodes.
- Store fluid in Fluid Tanks.
- Use Pipe Straight for directional routes. It pulls from the side opposite its facing and pushes 100 mB/t toward its facing.
- Use Pipe Junction for turns/branches.
- Remember that Pipe Junctions do not push fluid into nearby machines or tanks.
- Use Fluid Packager to fill Fluid Cells automatically.

## Safe Reactor Practices

- Keep coolant/fluid supply stable unless using a Reactor Permafrost Upgrade Chip.
- Add Reactor Waste Output before running long-term fuel loops.
- Keep Geiger Counter and Hazmat protection available around Blutonium.
- Use the `disableMeltdowns` gamerule only if the pack/server wants reactors without meltdown risk.

## Mining Automation

### Node Miner Setup

1. Find an ore node.
2. Place the Node Miner directly above the node.
3. Provide FE.
4. Route outputs with belts, collectors, or storage.

### Node Extractor Setup

1. Find a fluid node.
2. Place the Node Extractor directly above the node.
3. Provide FE.
4. Pipe the fluid into a Fluid Tank or machine.

### Quarry Setup

The Laser Quarry mines the chunk it is placed in, scanning from one block below itself downward. It costs 1,024 FE per block, stops while redstone-powered, ignores fluids, unbreakable blocks, air, and block entities, and tries to send drops into an inventory above it before using its internal output slots. Pair it with:

- Battery buffer.
- High-tier energy transfer.
- Large storage.
- Inventory or depot storage above the quarry.
- Smart Splitter overflow route.
- Item Collector or depot system for output management.

## Upgrade Planning

Common upgrades:

- Range Upgrade: Crystal Energy Siphon reach goes from about 3 blocks to about 4.5 blocks.
- Carbon Range Upgrade: Crystal Energy Siphon reach goes to about 6 blocks.
- Acceleration Upgrade: common processing machines such as the Crusher, Dust Separator, and Fluid Packager go from 100 ticks to 75 ticks.
- Carbon Acceleration Upgrade: those same machines go from 100 ticks to 50 ticks.
- Efficiency Upgrade: Crusher output goes from 2 dust to 3 dust; Dust Separator output goes from 12 nuggets to 14 nuggets.
- Carbon Efficiency Upgrade: Crusher output goes to 4 dust; Dust Separator output goes to 16 nuggets.
- Reactor Upgrade Chip
- Reactor Permafrost Upgrade Chip
- Depot Storage Upgrade

Use standard upgrades first, then Carbon variants when the machine tier and recipes call for them.

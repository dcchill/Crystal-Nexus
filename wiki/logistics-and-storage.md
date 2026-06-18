# Logistics and Storage

Crystal Nexus includes blocks for moving items, storing inventory, routing energy, transporting fluids, and automating crafting.

## Item Collection and Movement

### Item Collector

- Works like a black hole and a hopper.
- Redstone signal disables this block.

How it works:

- Pulls item entities toward itself.
- Stores collected items.
- Has an optional item filter; if empty, any item type can be collected.
- Accepts a Range Upgrade.
- The base range is 12; Range Upgrade sets it to 25.
- A redstone signal disables the collector.

Use it to collect drops from farms, mob farms, or beam-breaking setups. Add redstone control when you need to pause collection.

### Conveyor Belts

Conveyor blocks:

- Conveyor Belt
- Input Conveyor Belt
- Output Conveyor Belt

Use belts to move items between machines where direct inventory transfer is not enough.

- Normal belts move items forward.
- Output belts pull up to 32 items from the attached inventory.
- Input belts insert items into the attached inventory.

### Item Elevator

Item Elevator blocks move items vertically.

- Item Elevator
- Downward Item Elevator
- GUI button: Toggle Direction

### Smart Splitter

The Smart Splitter has a simple filter layout:

- Input
- Overflow
- Filter slots
- Empty filter slots act as open lanes for unmatched items.

How it works:

- One slot is input.
- Colored slots are filters for left, forward, and right.
- Another slot is the overflow buffer.
- The splitter moves one item per tick.
- Filter priority is left, then forward, then right.
- If no filter matches, empty filter lanes act as "any unmatched item" lanes.
- If no route accepts the item, it tries to store it in the overflow buffer.
- Automation inserts input from the back, inserts filters from the top, and extracts overflow from the bottom.

## Storage

### Container

Works like a shulker box.

Use it as portable or compact inventory storage.

### Dimensional Depot

Depot-related items and blocks:

- Dimensional Depot Uplink
- Dimensional Depot Uploader
- Depot Downloader
- Depot Storage Upgrade

The depot system works like cloud storage for items. The uploader and downloader imply input/output endpoints, while the uplink and storage upgrade manage access and capacity.

## Automated Crafting

### Crafting Factory

- Automatically crafts based on the filtered item.
- Treats all recipes as shapeless/stackless.

How it works:

- The crafting grid holds recipe ingredients.
- The output slot holds crafted items.
- The output filter slot tells the machine which normal crafting recipe result to search for.
- Ingredient matching is shape-agnostic and item-type based.
- Each successful craft costs 256 FE when the block has an energy capability.

Use it for recipes where you want a steady automated output without building a full external crafting network. Feed ingredients through belts, collectors, or depots.

How to use it:

1. Put the desired output item in the output filter slot.
2. Feed ingredients into the crafting grid.
3. Supply FE.
4. Pull crafted items from the output slot.
5. Remember that it treats recipes as shape-agnostic, so it only cares about having enough matching ingredients.

## Fluid Logistics

### Pipe Straight

Standard fluid pipe segment.

### Pipe Junction

Works like a splitter/corner for pipes. It does not push fluid to nearby machines or tanks.

Use junctions for turns and branches. Pipe Junctions do not push fluid into nearby machines or tanks, so plan around the pipes or machines that handle insertion.

## Teleport and Linking

### Tesseract

The Tesseract exists in input and output forms.

- Input endpoint: sends energy to the linked output.
- Output endpoint: receives linked energy, then pushes to adjacent receivers.
- Related item: Link Card.

Use Link Cards to bind or configure linked transfer endpoints. The input Tesseract reads linked coordinates from its Link Card slot, requires the target position to be a Tesseract Output, and transfers up to 81,920 FE/t. The output endpoint then pushes stored FE to adjacent energy receivers.

### Warp Pad

Only works when bound to another Warp Pad.

Use paired Warp Pads for entities to step on them to activate.

## Factory Controllers

Factory blocks:

- Factory Energy Controller
- Factory Item Controller
- Factory Output Controller

These blocks use Link Cards to move energy or items between a central controller and up to five linked machines. Right-click a machine or inventory with a Link Card to store its coordinates, then put that card in one of the controller's link slots.

### Factory Energy Controller

Use this as a shared FE buffer for several machines.

How it works:

- Stores up to 65,536 FE.
- Accepts and exposes FE through its energy capability.
- The Link Card row targets up to five linked blocks.
- Each linked target can receive up to 16,038 FE/t from the controller.
- Energy is inserted into the linked block from its bottom side.

How to use it:

1. Charge the Factory Energy Controller with cables, beams, batteries, or another FE source.
2. Link cards to the machines you want to power.
3. Put those cards in the Link Card row.
4. Make sure the linked machines can receive FE on the bottom side or through a general energy capability.

### Factory Item Controller

Use this to push items from the controller into linked machines.

How it works:

- The Link Card row targets up to five linked inventories.
- The item input row pairs one-for-one with the Link Card row.
- Each item input sends to the linked target above it.
- Each pair moves one item per tick into the first target slot that accepts it.
- Side automation can insert into the paired item inputs from the top, back, right, front, and left sides.

How to use it:

1. Link cards to the machines or inventories that should receive items.
2. Put those cards in the Link Card row.
3. Feed matching input items into the paired item input row.
4. The controller will drip-feed one item per tick from each filled input slot to its linked target.

### Factory Output Controller

Use this to pull finished items back from linked machines into one central block.

How it works:

- The Link Card row targets up to five linked inventories.
- The output buffer row pairs one-for-one with the Link Card row.
- Each output buffer receives from the linked target above it.
- Each pair pulls one item per tick when the paired buffer slot can accept it.
- If the linked block has sided inventory rules, the controller pulls from its bottom output side first.

How to use it:

1. Link cards to machines that produce outputs.
2. Put those cards in the Link Card row.
3. Pull collected items from the output buffer row with hoppers, belts, pipes, or other automation.

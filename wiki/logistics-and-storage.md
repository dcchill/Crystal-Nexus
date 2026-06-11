# Logistics and Storage

Crystal Nexus includes blocks for moving items, storing inventory, routing energy, transporting fluids, and automating crafting.

## Item Collection and Movement

### Item Collector

- Works like a black hole and a hopper.
- Redstone signal disables this block.

How it works:

- Pulls item entities toward itself.
- Stores collected items in slot 0.
- Slot 2 is an optional item filter; if empty, any item type can be collected.
- Slot 1 accepts a Range Upgrade.
- Base internal pull `rangeCount` is 12; Range Upgrade sets it to 25. The pull search box is inflated by `rangeCount / 2`.
- A redstone signal disables the collector.

Use it to collect drops from farms, mob farms, or beam-breaking setups. Add redstone control when you need to pause collection.

### Conveyor Belts

Conveyor blocks:

- Conveyor Belt
- Input Conveyor Belt
- Output Conveyor Belt

Use belts to move item entities between machines where direct inventory transfer is not enough.

- Normal belts move item entities forward.
- Output belts pull up to 32 items from the inventory behind the belt.
- Input belts insert item entities into the inventory in front of the belt.

### Item Elevator

Item Elevator blocks move items vertically.

- Item Elevator
- Downward Item Elevator
- GUI button: Toggle Direction

Use item elevators when your machine room has stacked floors or vertical processing lines. Matching elevator blocks must be stacked together; each tick moves one item between the elevator inventories.

### Smart Splitter

The Smart Splitter has a simple filter layout:

- Input
- Overflow
- Filter slots
- Empty filter slots act as open lanes for unmatched items.

How it works:

- Slot 0 is input.
- Slots 1, 2, and 3 are filters for left, forward, and right.
- Slot 4 is the overflow buffer.
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

Use the depot system for long-distance or remote item storage workflows. The uploader and downloader imply input/output endpoints, while the uplink and storage upgrade manage access and capacity.

## Automated Crafting

### Crafting Factory

- Automatically crafts based on the filtered item.
- Treats all recipes as shapeless/stackless.

How it works:

- Slots 0-8 are the crafting inventory.
- Slot 9 is output.
- Slot 10 is the output filter; the machine searches normal crafting recipes for a result matching this item type.
- Ingredient matching is shape-agnostic and item-type based.
- Each successful craft costs 256 FE when the block has an energy capability.

Use it for recipes where you want a steady automated output without building a full external crafting network. Feed ingredients through belts, collectors, or depots.

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

Use Link Cards to bind or configure linked transfer endpoints. The input Tesseract reads linked coordinates from the card in slot 0, requires the target position to be a Tesseract Output, and transfers up to 81,920 FE/t. The output endpoint then pushes stored FE to adjacent energy receivers.

### Warp Pad

Only works when bound to another Warp Pad.

Use paired Warp Pads for player or entity movement after binding them together.

## Factory Controllers

Factory blocks:

- Factory Energy Controller
- Factory Item Controller
- Factory Output Controller

These are controller-style logistics blocks with GUIs and tick logic. Use them for larger factory builds where item and energy handling need a central controller.

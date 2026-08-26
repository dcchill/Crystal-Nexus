# Depot Programmer

The Depot Programmer is an in-game visual editor for depot automation. Its registry identifier remains `crystalnexus:depot_cli`, so existing blocks, items, recipes, menus, and worlds remain compatible.

## Programs

Open the block to enter the Programs tab. Choose **New**, drag blocks from the categorized palette into the central workspace, configure the selected block in the inspector, then choose **Test**, **Save**, and **Run**. Programs are saved per depot owner and execute on the server.

- Left-drag a palette block to create it.
- Drag workspace blocks to reorder them or place them inside control blocks.
- Drag reporter blocks onto compatible inputs.
- Middle- or right-drag to pan; use the mouse wheel to zoom.
- Press Delete to remove the selected block, or drag it to Trash.
- Declare variables with semicolon-separated `name:type=default` entries, such as `count:number=4; enabled:boolean=true`.
- Test validates without mutating the world. Run requires a saved, unchanged, valid revision.

The initial palette provides depot actions, crafting and processing actions, variables, integer operators, conditions, loops, waits, and depot state reporters. Block colors and shapes identify their category and value type. Hover a palette block for help. Validation and runtime errors highlight the responsible block.

See the [complete block-by-block tutorial](depot_programming_blocks.md) for every palette block, its inputs, its inspector fields, and worked examples.

Programs are bounded to 32 programs and 32 variables per owner, 256 nodes, nesting depth 16, 64 immediate instructions per tick, 10,000 loop iterations, and 100,000 executed instructions per run. One program can run at a time. Active programs and interpreter state survive world saves and restarts.

Crafting, smelting, and processing blocks reuse the depot crafting lifecycle. They wait for the captured job to complete and report cancellation or failure at the originating block. Unloaded or unpowered networks pause without force-loading chunks. Offline player-inventory actions fail clearly; synced third-party recipe work waits for the owner to reconnect when necessary.

JEI's recipe-transfer button inserts a configured Craft, Process, or Define Pattern block directly into the open program.

## Depot panel

The Crafting tab remains a native inventory and status panel. Search and page through available outputs, inspect the expandable crafting tree, select recipe and machine routes, start crafting jobs, and cancel active work safely. It replaces the former `find`, `list`, `status`, `queue`, and preference command workflows without opening a console.

All saves, validation, execution, ownership checks, registry checks, rate limits, distance checks, and depot mutations are authoritative on the server.

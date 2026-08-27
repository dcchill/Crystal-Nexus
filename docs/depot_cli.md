# Depot CLI

## Send items to cable-connected machines

`send <item> <amount>` executes the V1 `Send Item` action. The action names only
the item and amount; it never names a machine. Machine-connected Depot Cable
faces are considered in descending priority, and each face's allow/block filter
decides whether it is eligible. Items that cannot be inserted remain in Depot
storage.

Right-click a Depot Cable next to an item-handler machine to configure each
connected face's exact-item filters, allow/block mode, and priority.

## Programs

The Depot CLI's **Programs** tab stores event-driven `WHEN -> IF -> DO`
automation on the Depot network. V1 triggers are Item Added and Inventory
Changed; conditions cover item count, existence, and absence; actions use the
existing Craft service or the central cable Send Item route. Programs never
name machines. Disable, edit, or delete programs from the list, and use the
Crafting search page as the visual item palette when editing.

Immediate mutation chains carry transaction and source-program IDs. A program
runs at most once per transaction, transactions stop after 64 actions, and a
Depot executes at most 100 automation actions per server tick. Machine output
imported later starts a new legitimate transaction.

The **Crafting** tab is the primary interface. Search the output catalog, select an amount, and inspect the expandable crafting tree before pressing Start. Blue nodes are already stored, green nodes use crafting-table recipes, yellow nodes use a selected connected machine, and red nodes are missing or need a route selection. Select a tree node to persist a recipe or compatible machine preference. Crafting-table routes remain automatic; machine routes are only used after you select them. The active-job bar shows live progress and can safely cancel the job, returning its current materials.

The **Terminal** tab retains every command-line feature, including custom processing-pattern administration, storage commands, history, autocomplete, and JEI transfer.

Place the Depot CLI on the same Depot Cable network as your powered Depot Controller. The screen and status label turn on when the owner’s player-based depot is connected. Other players cannot use the terminal or its storage permissions.

Use registry identifiers when names are ambiguous, for example `take minecraft:iron_ingot 64`. `find iron`, `find mod:create`, `find tag:c:ingots`, and amount filters search storage. `list --sort amount-desc --page 2` pages through large depots. `deposit held`, `deposit inventory`, and `deposit <item> <amount>` preserve rejected items. `craft <item> <amount>` recursively crafts using standard crafting-table recipes. `process <item> <amount>` recursively creates an item whose final step runs in an external machine such as Mekanism or AE2; it also retains `process list`, `process add`, and `process remove` for custom processing patterns. `smelt <item> <amount>` runs only compatible furnace or electric-furnace recipes. Use `recipe add <machine_id> <output> <count> <input> <count>...` to program a machine recipe manually; a single input is valid. Use `recipe remove <output>` to delete a programmed machine recipe. Its machine target and ingredients are saved with the depot. Clicking JEI's plus button from the Depot CLI pre-fills this command for unprogrammed recipes, and then pre-fills `process` for that output after the recipe is added during the current session. Use `recipe list <item>`, `recipe prefer <item> <recipe_id>`, and `recipe clear <item>` to control which recipe is tried first for any intermediate or final item. Use `queue clear` to cancel the active crafting job and return its current materials to storage.

Press Up/Down for command history, Tab for server-limited autocomplete, Page Up/Page Down or the mouse wheel to scroll, Enter to submit, and Escape to close. `help`, `help take`, and `help craft` show syntax. All storage and crafting commands are validated and executed by the server.

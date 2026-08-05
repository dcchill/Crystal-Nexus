# Depot CLI

Place the Depot CLI on the same Depot Cable network as your powered Depot Controller. The screen and status label turn on when the owner’s player-based depot is connected. Other players cannot use the terminal or its storage permissions.

Use registry identifiers when names are ambiguous, for example `take minecraft:iron_ingot 64`. `find iron`, `find mod:create`, `find tag:c:ingots`, and amount filters search storage. `list --sort amount-desc --page 2` pages through large depots. `deposit held`, `deposit inventory`, and `deposit <item> <amount>` preserve rejected items. `craft <item> <amount>` recursively crafts missing ingredients from any available crafting path and requires a connected Depot Crafting Upgrade. Use `recipe list <item>`, `recipe prefer <item> <recipe_id>`, and `recipe clear <item>` to control which recipe is tried first for any intermediate or final item.

Press Up/Down for command history, Tab for server-limited autocomplete, Page Up/Page Down or the mouse wheel to scroll, Enter to submit, and Escape to close. `help`, `help take`, and `help craft` show syntax. All storage and crafting commands are validated and executed by the server.

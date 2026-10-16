# Universal Drone

Craft one Universal Drone using iron, copper, and a Battery Cell. Place it with right-click. Its owner, cargo, fluids, battery, home, six programs, and interrupted task persist with the entity. A destroyed drone drops one packed drone containing its contents; only its owner can deploy it. Packed drones are consumed when deployed, including in creative mode.

Right-click your drone to program it. Sneak-right-click with the Crystal Wrench cycles Transport, Mining, Builder, Courier, Fluid Transport, and Combat. Programs remain saved separately for each mode. Mode changes stop movement and active work.

Craft a Drone Controller from iron, redstone, and a compass. Use it on your drone to bind it and open the editor. Use the controller in the air to recall that drone. Sneak-use recalls all your drones. An unbound controller also recalls all. Recall works in every mode.

## Remote controls

- `/drone list` lists owned drone UUIDs, dimensions, and loaded status.
- `/drone recall <UUID>` recalls one drone; append `resume` to resume its interrupted task automatically.
- `/drone recall_all` recalls all owned drones; append `resume` for automatic resumption.
- `/drone open <UUID>` opens a loaded owned drone remotely in the same dimension.
- `/drone resume <UUID>` restores an interrupted task if its program and mode still match.
- `/drone authorize <UUID> <player>` permits that player as a follow, recall, or delivery target. It does not grant inventory or editing access.
- `/drone revoke <UUID> <player>` revokes that permission.

Recall snapshots all active tasks, wait/area progress, variables, destinations, and the program revision between atomic server operations. Inventory and fluid operations finish before recall can interrupt them. It follows the player's current position, then hovers nearby and follows further movement. The editor has Recall, Recall All, Home, Resume Task, and an Auto Resume toggle.

Different dimensions, unloaded chunks, offline recipients, insufficient power, blocked routes, and timeouts produce explicit messages. Drones never teleport or force-load chunks. A failed recall keeps the interrupted task available for Resume Task. Loaded drones are indexed persistently, so remote recall also reports drones whose chunks have since unloaded.

## Programming

The editor shares the Assembly Line graph serialization, connections, style, and cable drawing. The Assembly Line runtime routes machine inventories, so drones use a separate server executor with each active task advancing once per tick. This does not change Assembly Line execution.

Use the **Program** tab to build the graph. **Add Node** opens a searchable browser with Common, Movement, Mode Actions, Conditions, Logic, and Events categories. Search accepts plain-language names such as ?pick up items? and original names such as ?extract items?. Select a node to see only its relevant settings; scroll the settings panel when necessary. Node cards grow to show their full wrapped descriptions and destination summaries. The running node has a pulsing green glow. Ordinary actions show Next; conditions show True/False. Failure connections and technical settings are under Advanced.

Use the **Cargo & Destinations** tab to access cargo, player inventory, and nine dedicated Link Card slots. Link a card by using it on a block in either hand. The card records coordinates, dimension, clicked block face, and block type. Renaming a card in an anvil gives it a recognizable name in destination selectors. Installed cards are shared across all six modes and remain separate from cargo.

The normal workflow is **link a block ? install its card ? select that card on a node ? Save & Run**. Area nodes use two cards for their corners, patrol uses an editable ordered list of cards, and blueprint nodes use a card as their origin. Player actions use a dropdown of the owner and explicitly authorized players. Drop an item carried by the cursor into a filter field to select its ID without consuming it. Numeric settings display units; Wait uses seconds.

Replacing or removing a card affects subsequent tasks. The active instruction retains its resolved position and access face until completion, including across recall, resumption, and world reloads. Patrol refreshes its next destination when a new waypoint leg begins. Missing, unlinked, or cross-dimension cards produce a clear error; older cards without dimension metadata say ?Relink this card?. The existing coordinate fields remain compatible with other Link Card machines. Install the updated mod on both clients and servers; the drone menu protocol version rejects older incompatible menus.

**Save & Run** saves and starts the same validated graph on the server. Pause/Continue preserves the active instruction. Recall is always available; Actions contains Save Only, Start Over, Stop, Home, Recall All, Resume Previous Task, and Auto Resume. Edits survive tab switches, selection changes, and resizing. Esc automatically saves changed programs without starting them and closes after server confirmation; validation errors keep the editor open. Esc first dismisses an open browser or cancels a connection. Scroll over the canvas to zoom from 25% to 300%; middle-drag to pan, or use Actions > Fit Graph. Existing coordinate-based programs still work; their destinations can be replaced with cards through Assign Link Card, or edited manually under Advanced.

Ordinary actions follow Next when there is nothing to do (for example, no eligible dropped items); an explicitly connected Other output still takes precedence. Conditions keep separate True/False branches, and failures keep their failure branch. A collection loop can use Collect Dropped Items ? Wait ? Repeat, with Repeat targeting Collect Dropped Items. It stays active between drops and while pickup delays expire or cargo is full.

An ordinary action's **Next** socket can connect to multiple nodes. Connect it to each destination; reconnect the same pair to remove that link. Each outgoing branch runs simultaneously: every active task advances once per server tick, with separate timers, progress, filters, variables, and resolved destinations inherited from the fork. Successor tasks begin on the following tick. A node already active is not started twice; shared downstream nodes may run again after an earlier execution ends. All active nodes glow green. Persistent tasks such as Follow Player can run alongside collection or other work. The drone has one flight path: conflicting movement tasks wait for that path to finish, while other tasks continue. Inventory and world operations commit one at a time on the server thread. Recall/reload preserves every branch; recall events run separately before resumption. Stop and unhandled failures stop the whole program. True/False, Other, and failure outputs retain one destination each. Existing sequential programs and older queued saves remain supported.

Connections form an acyclic graph. Repeat jumps to a configured node ID; count 0 repeats indefinitely. This provides loops without changing Assembly Line graph validation. On Recall Complete is an event entry that runs once after successful recall. Connect it to Resume Previous Task for explicit resumption. Follow Player, Patrol, Maintain Distance, Defend Player, and Defend Area are persistent behaviors; use pause, stop, or recall to interrupt them.

Shared logic, navigation, recall, and sensor nodes are available in every mode. Mode actions are filtered in the palette and validated again on the server. Sensor results are stored in the `result` variable and select a true/false output. Compare and If Else compare a variable with a value. AND, OR, and NOT combine variables; in combat they also combine accumulated targeting filters. Start clears combat filters for a new loop, and Stop Attacking clears both filters and the target.

Advanced coordinates use `x y z`. Filters accept registry IDs such as `minecraft:iron_ingot` or tags such as `#minecraft:logs`. Find nodes save the nearest matching position; action nodes can use that selected position, or turn off Use Selected to use explicit coordinates. Player fields accept `owner` or an authorized UUID.

- Transport uses sided item capabilities, simulated capacity, and actual extraction/insertion results. Unaccepted items remain in their source or cargo; unexpected remainders drop safely if both inventories reject them.
- Courier follows an authorized moving recipient for Deliver Items and preserves undelivered cargo when their inventory fills. Follow Player itself is intentionally persistent; use Deliver Items to navigate and deliver as one operation.
- Fluid Transport uses sided fluid capabilities. A handler that changes its capacity between simulation and execution has its uncommitted remainder retained in a separate saved recovery tank. Insert Fluid drains retained recovery fluid first. Storage is 16,000 mB.
- Mining breaks each reachable block instantly. It selects a suitable pickaxe from the nine cargo slots using the item's harvest rules, including modded pickaxe tags; a weaker tool cannot harvest higher-tier ores. Tool durability, enchantments, normal loot, server break events, unbreakable blocks, home infrastructure, battery costs, and full cargo still apply. Drops that do not fit remain in the world.
- Builder can use **Pick Up Items** to retrieve materials from storage: link the container, install its card, choose it as the pickup destination, and set the item filter and amount. Connect pickup to Place Block or Build Area using a separate destination card for the building site. Storage access uses the existing sided inventory capabilities and protection checks. Builder consumes real block items through server placement rules and events. Area corners are inclusive, with a 4,096-block maximum. Blueprints contain at most 128 relative positions: `0 0 0=minecraft:stone; 1 0 0=minecraft:glass`. Next Blueprint Position selects a position/material; connect Place Block and Repeat to process the list.
- Combat uses basic melee, entity IDs/tags, boolean filter combinations, and server damage hooks. Players, owners, allied entities, and other drones owned by the same player are excluded. Tamed animals require an explicit filter and cannot belong to the owner or an authorized player. Pursuit is bounded to 32 blocks by default.

The battery holds 200,000 FE. Movement and work consume energy. Set Home over an FE source to recharge from that block or the block below it, or sneak-use a charged energy item on the drone. Low battery automatically attempts a safe return home, waits for recharge, then resumes. Manual recall overrides the ordinary low-battery work threshold when enough navigation energy remains.

Flight uses bounded vanilla flying paths through loaded terrain. A blocked route retries before reporting failure. Routes near an unloaded search region can fail conservatively even when the destination is loaded; load the surrounding terrain and retry.

## Validation

Focused checks: `test --tests "net.crystalnexus.drone.*"` and `runGameTestServer -PgameTestNamespace=crystalnexus_drones`. Use an isolated GameTest directory; the fixture is included under `data/crystalnexus_drones/structure/empty.nbt`.

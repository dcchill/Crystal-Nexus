# Depot Programmer block guide

This is the block-by-block reference for the **Depot Programmer**. Open the
`Programs` tab, make a program, drag blocks into the workspace, configure the
selected block in the inspector, then use **Test**, **Save**, and **Run**.
Test validates only; Run performs the real depot actions.

Every program begins at **When Run**. Command blocks execute top to bottom.
Rounded reporters supply values to matching inputs; hexagonal reporters supply
true/false conditions. Nested blocks belong in the highlighted slots of control
blocks.

## Event block

| Block | What it does |
| --- | --- |
| **When Run** | The single hat-shaped root of every program. Its attached stack starts when you select **Run**. It has no inputs and cannot be deleted. |

## Values and variables

Use registry IDs for item, recipe, and machine values, such as
`minecraft:iron_ingot`, `minecraft:furnace`, and
`minecraft:iron_ingot_from_smelting_iron_ore`.

Create variables in the program's **Variables** field, separated with semicolons:

```text
count:number=4; enabled:boolean=true; target:item=minecraft:iron_ingot
```

The supported types are `number`, `boolean`, `text`, `item`, and `block`
(machine/block ID). Each run starts with these defaults again.

| Value block | Type | Inspector field | Purpose |
| --- | --- | --- | --- |
| **number** | Number | `number` | A whole number. |
| **boolean** | Boolean | `bool` | `true` or `false`. |
| **text** | Text | `text` | Plain text, up to 128 characters. |
| **item id** | Item | `text` | A registered item ID. |
| **machine id** | Block | `text` | A registered block/machine ID. |
| **number/boolean/text/item/machine variable** | Matching type | `variable` | The current value of that typed variable. |

The typed variable reporter must match its declaration: an `item` variable uses
**item variable**, for example.

## Depot blocks

| Block | Inputs | What it does |
| --- | --- | --- |
| **take item x amount** | Item, number | Moves 1–4096 items from depot storage to the owner's inventory. |
| **deposit item x amount** | Item, number | Moves 1–4096 matching items from the owner's inventory to storage. |
| **deposit held item** | — | Deposits the owner's held stack. |
| **deposit inventory** | — | Deposits eligible items from the owner's inventory. |
| **stored item** | Item | Reports that item's stored count. |
| **inventory item** | Item | Reports that item's count in the owner's inventory. |
| **depot used** | — | Reports used capacity. |
| **depot free** | — | Reports free capacity. |
| **depot capacity** | — | Reports total capacity. |
| **network connected** | — | Reports whether the depot controller is powered and connected. |

Network-dependent actions wait for a powered controller without force-loading
chunks. Inventory-dependent blocks fail clearly while the owner is offline.

## Crafting blocks

| Block | Inputs | What it does |
| --- | --- | --- |
| **craft item x amount** | Item, number | Starts a normal depot crafting job, then waits for it. |
| **smelt item x amount** | Item, number | Starts a smelting job, then waits for it. |
| **cancel active job** | — | Safely cancels the active crafting job. |
| **prefer recipe recipe for item** | Item, text | Saves a preferred recipe ID for an output item. |
| **clear recipe for item** | Item | Removes that recipe preference. |
| **job active** | — | Reports whether this depot has an active crafting job. |
| **job progress** | — | Reports the active job's integer progress from 0–100, or 0 with no job. |

Craft, smelt, and process reuse the existing depot job system. Their program
resumes only after the captured job succeeds. A cancellation or unexpected job
end stops the program at the responsible block with an error.

## Processing blocks

| Block | Inputs | What it does |
| --- | --- | --- |
| **process item x amount** | Item, number | Starts a processing job and waits for it. |
| **remove pattern item** | Item | Removes the saved pattern for that output; warns when none exists. |
| **define machine pattern** | Inspector fields | Saves a processing pattern; see below. |
| **machine balancing enabled** | Boolean | Enables or disables balancing across suitable machines. |
| **prefer machine machine for item** | Item, machine/block | Saves a preferred machine for an output item. |
| **clear machine for item** | Item | Removes that machine preference. |

For **define machine pattern**, use the selected block's inspector fields:

```text
output=minecraft:iron_ingot
amount=1
machine=minecraft:furnace
inputs=minecraft:iron_ore=1; minecraft:coal=1
outputs=minecraft:iron_ingot=1
```

`output` must be a known item; `machine` must be a known block. `amount` and
each listed count must be 1–4096. Separate entries with `;` (or `,`) and use
`item_id=count`. Inputs are required. Blank outputs default to the primary
output and amount.

## Control blocks

| Block | Inputs and nested slots | What it does |
| --- | --- | --- |
| **if condition** | Boolean; `then`, `else` | Runs `then` when true, otherwise `else`. |
| **repeat count** | Number; `body` | Runs the body that many times. |
| **repeat until condition** | Boolean; `body` | Runs the body until the condition becomes true, checking again on a later tick. |
| **wait seconds seconds** | Number | Pauses for 0–86,400 seconds. |
| **wait until condition** | Boolean | Checks once per tick until true. |
| **stop program** | — | Ends the current run successfully; following blocks do not run. |

Loops are guarded by a 10,000-iteration limit. A run is also capped at 100,000
executed instructions, so a bad loop ends safely with a runtime error.

## Variable blocks

| Block | Setup | What it does |
| --- | --- | --- |
| **set variable** | Choose `variable` in the inspector; connect a same-type value. | Replaces the current variable value. |
| **change variable by value** | Choose a number variable; connect a number. | Adds to that number variable. |

Use **set variable** for every type. **Change variable by** accepts only number
variables.

## Operators

| Block | Inputs | Result |
| --- | --- | --- |
| **add**, **subtract**, **multiply** | Number, number | A number; arithmetic is integer-only. |
| **divide**, **modulo** | Number, number | A number; zero as the right input is a runtime error. |
| **less**, **greater**, **equals** | Number, number | A boolean; **equals** is numeric equality. |
| **and**, **or** | Boolean, boolean | A boolean. |
| **not** | Boolean | The inverted boolean. |

For example, place **less** in an **if** condition, connect **stored item** to
its left input, and connect a **number** block to its right input.

## A first program

To craft four iron ingots only when the depot has more than three iron ore:

1. Add **if** below **When Run**.
2. Put **less** in its condition, with **number 3** on the left and
   **stored item** (`minecraft:iron_ore`) on the right.
3. Put **craft item x amount** in the `then` slot with
   `minecraft:iron_ingot` and `4`.
4. **Test**, **Save**, then **Run**.

If the condition is false, no job starts. Otherwise the program waits for the
job and completes when it succeeds.

## Validation and limits

Test catches missing inputs, incompatible types, unknown registry IDs or
variables, invalid pattern fields, duplicate names/block IDs, and bad nesting
before anything changes. Limits are 32 programs per player, 32 variables per
program, 256 blocks, and 16 nesting levels. Only a saved, unchanged, valid
revision can run, and one program may be active per owner at a time.

For editor controls, see the [Depot Programmer overview](depot_cli.md).

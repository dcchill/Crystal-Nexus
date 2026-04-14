import os
import json
import glob

elements_dir = r"c:\Users\dsmar\MCreatorWorkspaces\crystalnexus\elements"
files = glob.glob(os.path.join(elements_dir, "*.json"))

blocks = []
multiblocks = {}

for f in files:
    with open(f, "r", encoding="utf-8") as file:
        try:
            data = json.load(file)
            if data.get("_type") == "block":
                blocks.append({
                    "name": data.get("definition", {}).get("name", "Unknown"),
                    "filename": os.path.basename(f)
                })
            elif data.get("_type") == "jeirecipetype":
                title = data.get("definition", {}).get("title", "")
                tables = data.get("definition", {}).get("craftingtables", [])
                multiblocks[title] = [t.get("value") for t in tables]
            elif data.get("_type") == "procedure":
                name = data.get("name", "")
                if "MultiblockCheck" in name or "multiblock" in name.lower():
                    # Just recording it exists
                    pass
        except:
            pass

with open("blocks_summary.txt", "w", encoding="utf-8") as out:
    out.write("Multiblocks found:\n")
    for title, tables in multiblocks.items():
        out.write(f"- {title}: {tables}\n")
    out.write("\nBlocks found:\n")
    for b in blocks:
         out.write(f"- {b['name']} ({b['filename']})\n")

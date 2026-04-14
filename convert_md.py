import os
import json
import glob
import re

workspace_dir = r"c:\Users\dsmar\MCreatorWorkspaces\crystalnexus"
elements_dir = os.path.join(workspace_dir, "elements")
block_tex_dir = os.path.join(workspace_dir, r"src\main\resources\assets\crystalnexus\textures\block")
item_tex_dir = os.path.join(workspace_dir, r"src\main\resources\assets\crystalnexus\textures\item")

with open("tutorial.txt", "r", encoding="utf-8") as f:
    text = f.read()

# Collect element mappings
element_textures = {} # Human name (lower) -> absolute paths to png

for f in glob.glob(os.path.join(elements_dir, "*.json")):
    try:
        with open(f, "r", encoding="utf-8") as file:
            data = json.load(file)
            
        t = data.get("_type")
        name = data.get("definition", {}).get("name", "") or data.get("name", "")
        if not name: continue
        name_lower = name.lower()
        
        # Determine texture
        tex = None
        if t in ["block"]:
            defn = data.get("definition", {})
            tex = defn.get("textureFront") or defn.get("texture") or defn.get("textureTop")
            if tex:
                png = os.path.join(block_tex_dir, tex + ".png")
                if os.path.exists(png):
                    element_textures[name_lower] = png
                else:
                    # sometimes the texture name is different, or it's mapped via mcreator file. Just Try.
                    pass
        elif t in ["item", "tool", "armor"]:
            defn = data.get("definition", {})
            tex = defn.get("itemTexture") or defn.get("texture")
            if tex:
                png = os.path.join(item_tex_dir, tex + ".png")
                if os.path.exists(png):
                    element_textures[name.lower()] = png
    except Exception:
        pass

# Now to convert txt to md
lines = text.split('\n')
md_lines = []

for i, line in enumerate(lines):
    line_stripped = line.strip()
    
    # Handle headings
    if line_stripped.startswith("=") or not line_stripped:
        if line_stripped.startswith("="):
            continue
        md_lines.append("")
        continue
    
    if re.match(r"^\d+\.\s", line_stripped):
        md_lines.append("## " + line_stripped)
        continue
        
    if line_stripped == "Crystal Nexus Mod - Block Tutorial & Guide":
        md_lines.append("# " + line_stripped)
        continue
        
    # Check if this is an item list line:
    if line_stripped and not line_stripped.startswith("- Function:") and not line_stripped.startswith("- How to Build:") and not line_stripped.startswith("- Configuration:") and not line_stripped.startswith("- Jetpack vs Hoverpack:") and not line_stripped.startswith("Nodes:") and not line_stripped.startswith("Crude Oil / Gas Generation:") and not line_stripped.startswith("This guide covers"):
        # This might be an item list e.g. "Battery Cell / Carbon Battery Cell / EEBattery"
        # Let's split by "/" and match
        parts = [p.strip() for p in line_stripped.split("/")]
        images = []
        for p in parts:
            # try fuzzy matching
            best_match = None
            pl = p.lower()
            if pl in element_textures:
                best_match = element_textures[pl]
            else:
                for k, v in element_textures.items():
                    if pl in k or k in pl:
                        best_match = v
                        break
            if best_match:
                # Add image markdown via relative paths
                rel_path = os.path.relpath(best_match, workspace_dir)
                rel_path = rel_path.replace('\\', '/')
                images.append(f"![{p}]({rel_path})")
                
        if images:
            md_lines.append("### " + line_stripped)
            md_lines.append(" ".join(images))
        else:
            md_lines.append("### " + line_stripped)
    else:
        # Just regular text or description
        md_lines.append(line)

with open("tutorial.md", "w", encoding="utf-8") as out:
    out.write("\n".join(md_lines))

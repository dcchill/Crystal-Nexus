package net.crystalnexus.celestial;

import com.google.gson.*;
import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.multiblock.HeavenshatterMaterials;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import java.util.*;

@EventBusSubscriber(modid = CrystalnexusMod.MODID)
public final class CometDefinitions extends SimpleJsonResourceReloadListener {
    public record Definition(ResourceLocation id, String name, int weight, int difficulty, int minDiameter,
                             int maxDiameter, int minDistance, int maxDistance, List<BlockState> palette) {
        public Definition { palette = List.copyOf(palette); }
    }
    private static List<Definition> definitions = List.of();
    private static final Set<ResourceLocation> warned = new HashSet<>();
    public CometDefinitions() { super(new Gson(), "celestial_comets"); }
    @SubscribeEvent public static void reload(AddReloadListenerEvent event) { event.addListener(new CometDefinitions()); }
    @Override protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager manager, ProfilerFiller profiler) {
        List<Definition> loaded = new ArrayList<>();
        entries.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            try { loaded.add(parse(entry.getKey(), entry.getValue().getAsJsonObject())); }
            catch (RuntimeException ex) { CrystalnexusMod.LOGGER.warn("Ignoring invalid comet definition {}: {}", entry.getKey(), ex.getMessage()); }
        });
        definitions = List.copyOf(loaded);
        warned.clear();
    }
    public static Definition parse(ResourceLocation id, JsonObject json) {
        String name = json.get("name").getAsString();
        int weight = json.get("weight").getAsInt(), difficulty = json.get("difficulty").getAsInt();
        int min = json.get("min_diameter").getAsInt(), max = json.get("max_diameter").getAsInt();
        int near = json.get("min_distance").getAsInt(), far = json.get("max_distance").getAsInt();
        if (name.isBlank() || name.length() > 80 || weight < 1 || weight > 1000 || difficulty < 1 || difficulty > 3
            || min < 7 || max > 14 || min > max || near < 1 || far > ScannerMath.BASE.range() || near > far)
            throw new IllegalArgumentException("Invalid comet ranges");
        List<BlockState> palette = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("palette")) {
            ResourceLocation block = ResourceLocation.parse(element.getAsString());
            if (!BuiltInRegistries.BLOCK.containsKey(block)) throw new IllegalArgumentException("Unknown block " + block);
            palette.add(BuiltInRegistries.BLOCK.get(block).defaultBlockState());
        }
        if (palette.size() < 2 || palette.size() > 5 || palette.stream().distinct().count() != palette.size())
            throw new IllegalArgumentException("Expected 2–5 distinct palette blocks");
        return new Definition(id, name, weight, difficulty, min, max, near, far, palette);
    }
    public static List<Definition> eligible(Level level, BlockPos pos) {
        return definitions.stream().filter(d -> {
            boolean safe = d.palette().stream().allMatch(s -> HeavenshatterMaterials.eligible(s, level, pos));
            if (!safe && warned.add(d.id())) CrystalnexusMod.LOGGER.warn("Ignoring unsafe comet palette {}", d.id());
            return safe;
        }).toList();
    }
}

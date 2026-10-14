package net.crystalnexus.celestial;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import java.util.*;

/** Server-owned puzzle. Public observations deliberately exclude the hidden target settings. */
public final class CelestialScanner {
    public final List<Comet> candidates = new ArrayList<>();
    public final Set<String> discoveries = new HashSet<>();
    private final Set<UUID> identified = new HashSet<>();
    public int selected, azimuth = 180, elevation = 45, frequency = 50;
    public int alignmentTicks, strength, frequencySignal, azimuthHint, elevationHint;
    public boolean angularAligned;
    private Comet locked;
    public ScannerMath.Parameters parameters() { return ScannerMath.BASE; }
    public Comet lockedComet() { return locked; }
    public Comet target() { return candidates.isEmpty() ? null : candidates.get(selected); }
    public boolean identified() { return target() != null && identified.contains(target().id()); }
    public void generate(ServerLevel level, BlockPos pos) {
        if (!candidates.isEmpty()) return;
        var definitions = CometDefinitions.eligible(level, pos);
        if (definitions.isEmpty()) return;
        int total = definitions.stream().mapToInt(CometDefinitions.Definition::weight).sum();
        int offset = level.random.nextInt(360);
        for (int i = 0; i < 5; i++) {
            int roll = level.random.nextInt(total);
            var definition = definitions.getFirst();
            for (var d : definitions) { roll -= d.weight(); if (roll < 0) { definition = d; break; } }
            int diameter = definition.minDiameter() + level.random.nextInt(definition.maxDiameter() - definition.minDiameter() + 1);
            candidates.add(Comet.create(new UUID(level.random.nextLong(), level.random.nextLong()), definition,
                (offset + i * 72 + level.random.nextInt(21)) % 360, 10 + level.random.nextInt(66), level.random.nextInt(101),
                definition.minDistance() + level.random.nextInt(definition.maxDistance() - definition.minDistance() + 1), diameter, level.random.nextLong()));
        }
        observe();
    }
    public void controls(int candidate, int bearing, int elevation, int frequency) {
        if (selected != candidate || azimuth != bearing || this.elevation != elevation || this.frequency != frequency) alignmentTicks = 0;
        selected = candidate; azimuth = bearing; this.elevation = elevation;
        observe();
        if (angularAligned) this.frequency = frequency;
        observe();
    }
    private void observe() {
        Comet c = target();
        if (c == null) return;
        boolean known = discoveries.contains(c.classification().toString());
        int a = ScannerMath.angularError(azimuth, c.azimuth()), e = Math.abs(elevation - c.elevation());
        strength = ScannerMath.strength(a, e, c.distance(), c.difficulty(), parameters());
        double tolerance = ScannerMath.tolerance(c.difficulty(), known, parameters());
        angularAligned = a <= tolerance && e <= tolerance;
        azimuthHint = a <= tolerance ? 0 : Integer.signum(Math.floorMod(c.azimuth() - azimuth + 180, 360) - 180);
        elevationHint = e <= tolerance ? 0 : Integer.signum(c.elevation() - elevation);
        frequencySignal = angularAligned ? Math.max(0, 100 - Math.abs(frequency - c.frequency()) * 2) : 0;
    }
    public void tick(boolean powered) {
        observe();
        Comet c = target();
        if (c == null || !powered) { alignmentTicks = 0; return; }
        boolean known = discoveries.contains(c.classification().toString());
        if (ScannerMath.aligned(ScannerMath.angularError(azimuth, c.azimuth()), Math.abs(elevation - c.elevation()),
            Math.abs(frequency - c.frequency()), c.difficulty(), known, parameters())) {
            alignmentTicks = Math.min(60, alignmentTicks + 1);
            if (alignmentTicks >= ScannerMath.identificationTicks(known, parameters())) {
                identified.add(c.id()); discoveries.add(c.classification().toString());
            }
        } else alignmentTicks = 0;
    }
    public boolean lock() {
        if (!identified() || alignmentTicks < 60) return false;
        locked = target(); return true;
    }
    public void reset() {
        candidates.clear(); identified.clear(); locked = null; selected = alignmentTicks = strength = frequencySignal = 0; angularAligned = false;
    }
    public CompoundTag publicState() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("selected", selected); tag.putInt("azimuth", azimuth); tag.putInt("elevation", elevation); tag.putInt("frequency", frequency);
        tag.putInt("strength", strength); tag.putInt("frequencySignal", frequencySignal); tag.putInt("stability", alignmentTicks);
        tag.putInt("azimuthHint", azimuthHint); tag.putInt("elevationHint", elevationHint); tag.putBoolean("angularAligned", angularAligned);
        tag.putBoolean("identified", identified()); tag.putBoolean("locked", locked != null); tag.putInt("discoveries", discoveries.size());
        ListTag markers = new ListTag();
        for (Comet c : candidates) {
            CompoundTag marker = new CompoundTag();
            marker.putInt("azimuth", ((c.azimuth() + 7) / 15 * 15) % 360);
            marker.putInt("elevation", (c.elevation() + 7) / 15 * 15);
            marker.putBoolean("known", discoveries.contains(c.classification().toString())); markers.add(marker);
        }
        tag.put("markers", markers);
        if (identified()) tag.put("information", target().information());
        return tag;
    }
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag(); tag.putInt("version", 1);
        tag.putInt("selected", selected); tag.putInt("azimuth", azimuth); tag.putInt("elevation", elevation); tag.putInt("frequency", frequency);
        ListTag comets = new ListTag(); candidates.forEach(c -> comets.add(c.save())); tag.put("candidates", comets);
        ListTag signatures = new ListTag(); discoveries.stream().sorted().forEach(s -> signatures.add(StringTag.valueOf(s))); tag.put("discoveries", signatures);
        ListTag ids = new ListTag(); identified.forEach(id -> ids.add(StringTag.valueOf(id.toString()))); tag.put("identified", ids);
        if (locked != null) tag.put("locked", locked.save());
        return tag;
    }
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        reset(); discoveries.clear();
        ListTag comets = tag.getList("candidates", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(5, comets.size()); i++) {
            try { candidates.add(Comet.load(comets.getCompound(i), registries)); }
            catch (RuntimeException ex) { net.crystalnexus.CrystalnexusMod.LOGGER.warn("Ignoring damaged comet snapshot", ex); }
        }
        selected = Math.clamp(tag.getInt("selected"), 0, Math.max(0, candidates.size() - 1));
        azimuth = Math.floorMod(tag.getInt("azimuth"), 360); elevation = Math.clamp(tag.getInt("elevation"), 0, 90); frequency = Math.clamp(tag.getInt("frequency"), 0, 100);
        for (Tag entry : tag.getList("discoveries", Tag.TAG_STRING)) discoveries.add(entry.getAsString());
        for (Tag entry : tag.getList("identified", Tag.TAG_STRING)) {
            try { identified.add(UUID.fromString(entry.getAsString())); } catch (IllegalArgumentException ignored) { }
        }
        if (tag.contains("locked")) {
            try { locked = Comet.load(tag.getCompound("locked"), registries); alignmentTicks = 60; }
            catch (RuntimeException ex) { net.crystalnexus.CrystalnexusMod.LOGGER.warn("Ignoring damaged comet lock", ex); }
        }
        observe();
    }
}

package net.crystalnexus.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.crystalnexus.CrystalnexusMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.profiling.ContinuousProfiler;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.timings.ObjectTimings;
import net.neoforged.neoforge.server.timings.TimeTracker;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Uses the profilers already built into Minecraft and NeoForge; no background sampling. */
@EventBusSubscriber(modid = CrystalnexusMod.MODID)
public final class NexusDebugCommand {
    private static CommandSourceStack owner;
    private static long deadline;
    private static int startedTick;
    private static long startedNanos;
    private static int capturedTicks;
    private static ContinuousProfiler sections;
    private static boolean recordingTick;

    public static boolean isCapturing(MinecraftServer server) { return owner != null && owner.getServer() == server; }

    public static ProfilerFiller profiler(Level level) {
        return owner != null && recordingTick && level.getServer() == owner.getServer() && owner.getServer().isSameThread()
                ? sections.getFiller() : InactiveProfiler.INSTANCE;
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("nexusdebug")
                .requires(source -> source.hasPermission(2))
                .executes(context -> status(context.getSource()))
                .then(Commands.literal("start").executes(context -> start(context.getSource(), 10))
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(5, 120))
                                .executes(context -> start(context.getSource(), IntegerArgumentType.getInteger(context, "seconds")))))
                .then(Commands.literal("stop").executes(context -> stop(context.getSource()))));
    }

    private static int status(CommandSourceStack source) {
        double mspt = source.getServer().getAverageTickTimeNanos() / 1_000_000.0;
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "Current MSPT: %.2f ms. Use /nexusdebug start [5-120 seconds] for measured TPS and machine timings.%s",
                mspt, owner == null ? "" : " Capture running.")), false);
        return 1;
    }

    private static int start(CommandSourceStack source, int seconds) {
        if (owner != null || source.getServer().isTimeProfilerRunning()) {
            source.sendFailure(Component.literal("A debug capture is already running. Finish it before starting another."));
            return 0;
        }
        owner = source;
        startedTick = source.getServer().getTickCount();
        startedNanos = System.nanoTime();
        deadline = startedNanos + seconds * 1_000_000_000L;
        capturedTicks = 0;
        sections = new ContinuousProfiler(System::nanoTime, source.getServer()::getTickCount);
        sections.enable();
        TimeTracker.BLOCK_ENTITY_UPDATE.reset();
        TimeTracker.ENTITY_UPDATE.reset();
        TimeTracker.BLOCK_ENTITY_UPDATE.enable(seconds);
        TimeTracker.ENTITY_UPDATE.enable(seconds);
        source.sendSuccess(() -> Component.literal("Profiling for " + seconds
                + " seconds. Keep the machine and its energy supply running normally. The report will be saved automatically."), false);
        return 1;
    }

    private static int stop(CommandSourceStack source) {
        if (owner == null || owner.getServer() != source.getServer()) {
            source.sendFailure(Component.literal("No Nexus debug capture is running."));
            return 0;
        }
        // A capture starts on the following tick so its section stack has a complete root frame.
        if (source.getServer().getTickCount() <= startedTick) {
            source.sendFailure(Component.literal("Capture just started; stop it on the next tick or wait for the automatic report."));
            return 0;
        }
        finish(source);
        return 1;
    }

    @SubscribeEvent
    public static void tickStart(ServerTickEvent.Pre event) {
        if (isCapturing(event.getServer())) {
            sections.getFiller().startTick();
            recordingTick = true;
        }
    }

    private static void endTick() {
        if (recordingTick) {
            sections.getFiller().endTick();
            recordingTick = false;
            capturedTicks++;
        }
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        if (isCapturing(event.getServer())) {
            endTick();
            if (event.getServer().getTickCount() > startedTick && System.nanoTime() >= deadline) finish(owner);
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        if (owner != null && owner.getServer() == event.getServer()) {
            sections.disable();
            sections = null;
            recordingTick = false;
            owner = null;
            TimeTracker.BLOCK_ENTITY_UPDATE.reset();
            TimeTracker.ENTITY_UPDATE.reset();
        }
    }

    private static <T> List<ObjectTimings<T>> sorted(TimeTracker<T> tracker) {
        return tracker.getTimingData().stream().filter(timing -> timing.getObject().get() != null)
                .sorted(Comparator.comparingDouble((ObjectTimings<T> timing) -> timing.getAverageTimings()).reversed())
                .limit(50).toList();
    }

    private static String blockLine(ObjectTimings<BlockEntity> timing) {
        var machine = timing.getObject().get();
        if (machine == null || machine.getLevel() == null) return "Unloaded block entity";
        return String.format(Locale.ROOT, "%.2f us/t | %s | %s | %s", timing.getAverageTimings() / 1000,
                BuiltInRegistries.BLOCK.getKey(machine.getBlockState().getBlock()),
                machine.getLevel().dimension().location(), machine.getBlockPos().toShortString());
    }

    private static String entityLine(ObjectTimings<Entity> timing) {
        var entity = timing.getObject().get();
        if (entity == null) return "Unloaded entity";
        return String.format(Locale.ROOT, "%.2f us/t | %s | %s | %s", timing.getAverageTimings() / 1000,
                BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()), entity.level().dimension().location(), entity.blockPosition().toShortString());
    }

    private static void finish(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        endTick();
        owner = null;
        try {
            var profile = sections.getResults();
            double elapsed = (System.nanoTime() - startedNanos) / 1_000_000_000.0;
            String summary = String.format(Locale.ROOT, "Nexus debug: %.2f TPS over %.2f s (%d ticks); current MSPT %.2f ms",
                    elapsed > 0 ? capturedTicks / elapsed : 0, elapsed, capturedTicks,
                    server.getAverageTickTimeNanos() / 1_000_000.0);
            var blocks = sorted(TimeTracker.BLOCK_ENTITY_UPDATE).stream().map(NexusDebugCommand::blockLine).toList();
            var entities = sorted(TimeTracker.ENTITY_UPDATE).stream().map(NexusDebugCommand::entityLine).toList();
            String report = summary + "\n\nTop block entities (including other mods):\n" + String.join("\n", blocks)
                    + "\n\nTop entities:\n" + String.join("\n", entities)
                    + "\n\nBlock/entity averages use NeoForge's last 99 timing samples, not the entire capture."
                    + "\nNew or intermittently ticking objects can include zero samples. Profiling adds overhead."
                    + "\n\nNexus stage profiler tree (nested sections are inclusive):\n" + profile.getProfilerResults();
            var directory = server.getServerDirectory().resolve("debug");
            Files.createDirectories(directory);
            var path = directory.resolve("crystalnexus-profile-" + System.currentTimeMillis() + ".txt");
            Files.writeString(path, report);
            source.sendSuccess(() -> Component.literal(summary), false);
            blocks.stream().limit(5).forEach(line -> source.sendSuccess(() -> Component.literal(line), false));
            source.sendSuccess(() -> Component.literal("Report saved: " + path.toAbsolutePath()), false);
            CrystalnexusMod.LOGGER.info("{}; report saved: {}", summary, path.toAbsolutePath());
        } catch (IOException exception) {
            source.sendFailure(Component.literal("Could not save debug report: " + exception.getMessage()));
            CrystalnexusMod.LOGGER.error("Could not save Nexus debug report", exception);
        } finally {
            sections.disable();
            sections = null;
            TimeTracker.BLOCK_ENTITY_UPDATE.reset();
            TimeTracker.ENTITY_UPDATE.reset();
        }
    }
}

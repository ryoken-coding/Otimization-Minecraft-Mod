package io.github.ryokencoding.omperf.server;

import io.github.ryokencoding.omperf.config.DiagnosticsConfig;
import io.github.ryokencoding.omperf.telemetry.ReportFormatter;
import io.github.ryokencoding.omperf.telemetry.TickSession;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.Permissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Common entrypoint. No client classes, mixins, world mutation, worker threads or custom packets. */
public final class OmperfMod implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("omperf");
    // Accessed only by server lifecycle/tick/command callbacks on the server thread.
    private MinecraftServer activeServer;
    private TickSession session;

    @Override
    public void onInitialize() {
        DiagnosticsConfig config = readConfig();
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) ->
                dispatcher.register(Commands.literal("omperf")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                        .executes(context -> {
                            String message;
                            if (!config.enabled()) {
                                message = "omperf: diagnostics disabled; edit config/omperf.properties and restart.";
                            } else if (session == null || activeServer != context.getSource().getServer()) {
                                message = "omperf: no active server session.";
                            } else {
                                var snapshot = session.report(System.nanoTime());
                                message = snapshot == null ? "omperf: wait 5 seconds between reports."
                                        : ReportFormatter.format(snapshot);
                            }
                            String reply = message;
                            context.getSource().sendSuccess(() -> Component.literal(reply), false);
                            return 1;
                        })));
        if (!config.enabled()) return; // No tick/lifecycle listeners or ring when disabled.

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            if (session != null) session.clear();
            activeServer = server;
            session = new TickSession(config.windowSamples());
        });
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            if (server == activeServer && session != null) session.startTick(System.nanoTime());
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server == activeServer && session != null) session.endTick(System.nanoTime());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            if (server == activeServer) {
                if (session != null) session.clear();
                session = null;
                activeServer = null;
            }
        });
    }

    private static DiagnosticsConfig readConfig() {
        try {
            return DiagnosticsConfig.load(FabricLoader.getInstance().getConfigDir().resolve("omperf.properties"));
        } catch (java.io.IOException | IllegalArgumentException | SecurityException exception) {
            // Do not log file contents, user paths, identifiers or remote telemetry.
            LOGGER.warn("omperf: invalid/unreadable config; diagnostics disabled. See configuration documentation.");
            return DiagnosticsConfig.DEFAULT;
        }
    }
}

package com.aibrigade.debug;

import com.aibrigade.bots.BotEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * DebugCommands - Commands for debugging bot AI
 *
 * Commands:
 * - /aibrigade debug enable/disable - Toggle debug visualization
 * - /aibrigade debug paths <true|false> - Toggle path visualization
 * - /aibrigade debug targets <true|false> - Toggle target visualization
 * - /aibrigade debug behaviors <true|false> - Toggle behavior state display
 * - /aibrigade debug groups <true|false> - Toggle group connection display
 * - /aibrigade debug ranges <true|false> - Toggle range circle display
 * - /aibrigade debug info - Show debug info for nearest bot
 */
public class DebugCommands {

    /**
     * Register debug commands
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("aibrigade")
            .then(Commands.literal("debug")
                .then(Commands.literal("enable")
                    .executes(ctx -> {
                        DebugVisualizer.setEnabled(true);
                        ctx.getSource().sendSuccess(() -> Component.literal("Debug visualization enabled"), true);
                        return 1;
                    }))
                .then(Commands.literal("disable")
                    .executes(ctx -> {
                        DebugVisualizer.setEnabled(false);
                        ctx.getSource().sendSuccess(() -> Component.literal("Debug visualization disabled"), true);
                        return 1;
                    }))
                .then(Commands.literal("paths")
                    .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> {
                            boolean value = BoolArgumentType.getBool(ctx, "value");
                            DebugVisualizer.setShowPaths(value);
                            ctx.getSource().sendSuccess(() -> Component.literal("Path visualization: " + value), true);
                            return 1;
                        })))
                .then(Commands.literal("targets")
                    .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> {
                            boolean value = BoolArgumentType.getBool(ctx, "value");
                            DebugVisualizer.setShowTargets(value);
                            ctx.getSource().sendSuccess(() -> Component.literal("Target visualization: " + value), true);
                            return 1;
                        })))
                .then(Commands.literal("behaviors")
                    .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> {
                            boolean value = BoolArgumentType.getBool(ctx, "value");
                            DebugVisualizer.setShowBehaviors(value);
                            ctx.getSource().sendSuccess(() -> Component.literal("Behavior visualization: " + value), true);
                            return 1;
                        })))
                .then(Commands.literal("groups")
                    .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> {
                            boolean value = BoolArgumentType.getBool(ctx, "value");
                            DebugVisualizer.setShowGroups(value);
                            ctx.getSource().sendSuccess(() -> Component.literal("Group visualization: " + value), true);
                            return 1;
                        })))
                .then(Commands.literal("ranges")
                    .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> {
                            boolean value = BoolArgumentType.getBool(ctx, "value");
                            DebugVisualizer.setShowRanges(value);
                            ctx.getSource().sendSuccess(() -> Component.literal("Range visualization: " + value), true);
                            return 1;
                        })))
                .then(Commands.literal("info")
                    .executes(ctx -> {
                        Entity entity = ctx.getSource().getEntity();
                        if (entity == null) {
                            ctx.getSource().sendFailure(Component.literal("Must be executed by a player"));
                            return 0;
                        }

                        // Find nearest bot
                        AABB searchArea = entity.getBoundingBox().inflate(10.0);
                        List<BotEntity> bots = entity.level().getEntitiesOfClass(BotEntity.class, searchArea);

                        if (bots.isEmpty()) {
                            ctx.getSource().sendFailure(Component.literal("No bots nearby"));
                            return 0;
                        }

                        BotEntity nearestBot = bots.get(0);
                        double nearestDist = entity.distanceToSqr(nearestBot);

                        for (BotEntity bot : bots) {
                            double dist = entity.distanceToSqr(bot);
                            if (dist < nearestDist) {
                                nearestBot = bot;
                                nearestDist = dist;
                            }
                        }

                        // Display debug info
                        List<String> info = DebugVisualizer.getDebugInfo(nearestBot);
                        ctx.getSource().sendSuccess(() -> Component.literal("=== Bot Debug Info ==="), false);
                        for (String line : info) {
                            ctx.getSource().sendSuccess(() -> Component.literal(line), false);
                        }

                        return 1;
                    }))
            )
        );
    }
}

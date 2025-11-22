package com.aibrigade.commands;

import com.aibrigade.bots.BotEntity;
import com.aibrigade.bots.BotManager;
import com.aibrigade.main.AIBrigadeMod;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * BotBuildingCommands - Commandes pour gérer la construction des bots
 *
 * Commandes:
 * - /bot building on [botName]  - Active la construction pour un bot ou tous
 * - /bot building off [botName] - Désactive la construction pour un bot ou tous
 */
public class BotBuildingCommands {

    /**
     * Enregistre les commandes
     * CRITICAL SECURITY FIX: Requires operator permission (level 2)
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("bot")
                // CRITICAL SECURITY FIX: Require operator permission (level 2)
                // Without this, ANY player could enable/disable building for ALL bots
                // This would allow massive griefing or breaking gameplay
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("building")
                    .then(Commands.literal("on")
                        .executes(BotBuildingCommands::enableBuildingAll)
                        .then(Commands.argument("botName", StringArgumentType.word())
                            .executes(BotBuildingCommands::enableBuildingSingle)))
                    .then(Commands.literal("off")
                        .executes(BotBuildingCommands::disableBuildingAll)
                        .then(Commands.argument("botName", StringArgumentType.word())
                            .executes(BotBuildingCommands::disableBuildingSingle))))
        );
    }

    /**
     * Active la construction pour tous les bots
     */
    private static int enableBuildingAll(CommandContext<CommandSourceStack> context) {
        BotManager botManager = AIBrigadeMod.getBotManager();

        if (botManager == null) {
            context.getSource().sendFailure(Component.literal("Bot manager not initialized"));
            return 0;
        }

        int count = 0;
        for (BotEntity bot : botManager.getActiveBots().values()) {
            bot.setCanPlaceBlocks(true);
            count++;
        }

        final int finalCount = count;
        context.getSource().sendSuccess(() ->
            Component.literal("✓ Construction activée pour " + finalCount + " bot(s)"),
            true);

        return Command.SINGLE_SUCCESS;
    }

    /**
     * Active la construction pour un bot spécifique
     */
    private static int enableBuildingSingle(CommandContext<CommandSourceStack> context) {
        String botName = StringArgumentType.getString(context, "botName");
        BotManager botManager = AIBrigadeMod.getBotManager();

        if (botManager == null) {
            context.getSource().sendFailure(Component.literal("Bot manager not initialized"));
            return 0;
        }

        BotEntity bot = botManager.findBotByName(botName);
        if (bot == null) {
            context.getSource().sendFailure(Component.literal("✗ Bot introuvable: " + botName));
            return 0;
        }

        bot.setCanPlaceBlocks(true);

        context.getSource().sendSuccess(() ->
            Component.literal("✓ Construction activée pour " + bot.getBotName()),
            true);

        return Command.SINGLE_SUCCESS;
    }

    /**
     * Désactive la construction pour tous les bots
     */
    private static int disableBuildingAll(CommandContext<CommandSourceStack> context) {
        BotManager botManager = AIBrigadeMod.getBotManager();

        if (botManager == null) {
            context.getSource().sendFailure(Component.literal("Bot manager not initialized"));
            return 0;
        }

        int count = 0;
        for (BotEntity bot : botManager.getActiveBots().values()) {
            bot.setCanPlaceBlocks(false);
            count++;
        }

        final int finalCount = count;
        context.getSource().sendSuccess(() ->
            Component.literal("✓ Construction désactivée pour " + finalCount + " bot(s)"),
            true);

        return Command.SINGLE_SUCCESS;
    }

    /**
     * Désactive la construction pour un bot spécifique
     */
    private static int disableBuildingSingle(CommandContext<CommandSourceStack> context) {
        String botName = StringArgumentType.getString(context, "botName");
        BotManager botManager = AIBrigadeMod.getBotManager();

        if (botManager == null) {
            context.getSource().sendFailure(Component.literal("Bot manager not initialized"));
            return 0;
        }

        BotEntity bot = botManager.findBotByName(botName);
        if (bot == null) {
            context.getSource().sendFailure(Component.literal("✗ Bot introuvable: " + botName));
            return 0;
        }

        bot.setCanPlaceBlocks(false);

        context.getSource().sendSuccess(() ->
            Component.literal("✓ Construction désactivée pour " + bot.getBotName()),
            true);

        return Command.SINGLE_SUCCESS;
    }
}

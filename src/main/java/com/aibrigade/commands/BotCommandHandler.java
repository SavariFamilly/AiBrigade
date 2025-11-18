package com.aibrigade.commands;

import com.aibrigade.main.AIBrigadeMod;
import com.aibrigade.bots.BotManager;
import com.aibrigade.ai.AIManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;

/**
 * BotCommandHandler - Handles all /aibrigade commands
 *
 * Available commands:
 * - /aibrigade spawn <solo|group> <number> [options...]
 * - /aibrigade assignleader <groupName> <leaderName>
 * - /aibrigade hostile <groupName1> <groupName2>
 * - /aibrigade givearmor <target> <full|partial> <materials>
 * - /aibrigade modify name <botName> <newName>
 * - /aibrigade modify armor <botName> <slot> <type> <materials>
 * - /aibrigade modify hand <botName> <item>
 * - /aibrigade modify offhand <botName> <item>
 * - /aibrigade setbehavior <target> <behavior>
 * - /aibrigade setradius <groupName> <radius>
 * - /aibrigade togglestatic <target>
 * - /aibrigade removebot <botName>
 * - /aibrigade removegroup <groupName>
 * - /aibrigade groupinfo <groupName>
 * - /aibrigade listbots
 * - /aibrigade listgroups
 *
 * All commands require operator permission level 2
 */
public class BotCommandHandler {

    /**
     * Register all AIBrigade commands
     *
     * @param dispatcher The command dispatcher
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("aibrigade")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("spawn")
                .then(Commands.literal("solo")
                    .then(Commands.argument("leader", StringArgumentType.string())
                        .then(Commands.argument("behavior", StringArgumentType.string())
                            .then(Commands.argument("radius", FloatArgumentType.floatArg(1.0f, 100.0f))
                                .then(Commands.argument("static", BoolArgumentType.bool())
                                    .then(Commands.argument("groupName", StringArgumentType.string())
                                        .executes(BotCommandHandler::spawnSoloBot)))))))
                .then(Commands.literal("group")
                    .then(Commands.argument("count", IntegerArgumentType.integer(1, 300))
                        .then(Commands.argument("leader", StringArgumentType.string())
                            .then(Commands.argument("behavior", StringArgumentType.string())
                                .then(Commands.argument("radius", FloatArgumentType.floatArg(1.0f, 100.0f))
                                    .then(Commands.argument("static", BoolArgumentType.bool())
                                        .then(Commands.argument("groupName", StringArgumentType.string())
                                            .executes(BotCommandHandler::spawnBotGroup)))))))))

            .then(Commands.literal("assignleader")
                .then(Commands.argument("groupName", StringArgumentType.string())
                    .then(Commands.argument("leaderName", StringArgumentType.string())
                        .executes(BotCommandHandler::assignLeader))))

            .then(Commands.literal("hostile")
                .then(Commands.argument("sourceGroup", StringArgumentType.string())
                    .then(Commands.argument("targetGroup", StringArgumentType.string())
                        .executes(BotCommandHandler::setHostile))))

            .then(Commands.literal("sethostiletogroup")
                .then(Commands.argument("groupName", StringArgumentType.string())
                    .executes(BotCommandHandler::setPlayerHostileToGroup)))

            .then(Commands.literal("followleader")
                .then(Commands.argument("groupName", StringArgumentType.string())
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .then(Commands.argument("radius", FloatArgumentType.floatArg(1.0f, 100.0f))
                            .executes(BotCommandHandler::setFollowLeader)))))

            .then(Commands.literal("givearmor")
                .then(Commands.argument("target", StringArgumentType.string())
                    .then(Commands.argument("type", StringArgumentType.string())
                        .then(Commands.argument("materials", StringArgumentType.string())
                            .executes(BotCommandHandler::giveArmor)))))

            .then(Commands.literal("modify")
                .then(Commands.literal("name")
                    .then(Commands.argument("botName", StringArgumentType.string())
                        .then(Commands.argument("newName", StringArgumentType.string())
                            .executes(BotCommandHandler::modifyName))))
                .then(Commands.literal("armor")
                    .then(Commands.argument("botName", StringArgumentType.string())
                        .then(Commands.argument("slot", StringArgumentType.string())
                            .then(Commands.argument("type", StringArgumentType.string())
                                .then(Commands.argument("materials", StringArgumentType.string())
                                    .executes(BotCommandHandler::modifyArmor))))))
                .then(Commands.literal("hand")
                    .then(Commands.argument("botName", StringArgumentType.string())
                        .then(Commands.argument("item", StringArgumentType.string())
                            .executes(BotCommandHandler::modifyHand))))
                .then(Commands.literal("offhand")
                    .then(Commands.argument("botName", StringArgumentType.string())
                        .then(Commands.argument("item", StringArgumentType.string())
                            .executes(BotCommandHandler::modifyOffhand)))))

            .then(Commands.literal("setbehavior")
                .then(Commands.argument("target", StringArgumentType.string())
                    .then(Commands.argument("behavior", StringArgumentType.string())
                        .executes(BotCommandHandler::setBehavior))))

            .then(Commands.literal("setradius")
                .then(Commands.argument("groupName", StringArgumentType.string())
                    .then(Commands.argument("radius", FloatArgumentType.floatArg(1.0f, 100.0f))
                        .executes(BotCommandHandler::setRadius))))

            .then(Commands.literal("togglestatic")
                .then(Commands.argument("target", StringArgumentType.string())
                    .executes(BotCommandHandler::toggleStatic)))

            .then(Commands.literal("removebot")
                .then(Commands.argument("botName", StringArgumentType.string())
                    .executes(BotCommandHandler::removeBot)))

            .then(Commands.literal("removegroup")
                .then(Commands.argument("groupName", StringArgumentType.string())
                    .executes(BotCommandHandler::removeGroup)))

            .then(Commands.literal("groupinfo")
                .then(Commands.argument("groupName", StringArgumentType.string())
                    .executes(BotCommandHandler::groupInfo)))


            .then(Commands.literal("listbots")
                .executes(BotCommandHandler::listBots))

            .then(Commands.literal("cleanupbots")
                .executes(BotCommandHandler::cleanupBots))

            .then(Commands.literal("listgroups")
                .executes(BotCommandHandler::listGroups))

            .then(Commands.literal("help")
                .executes(BotCommandHandler::showHelp))
        );

        AIBrigadeMod.LOGGER.info("AIBrigade commands registered");
    }

    /**
     * Command: /aibrigade spawn solo
     * Spawns a single bot
     */
    private static int spawnSoloBot(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = player.blockPosition();

        String leader = StringArgumentType.getString(context, "leader");
        String behavior = StringArgumentType.getString(context, "behavior");
        float radius = FloatArgumentType.getFloat(context, "radius");
        boolean isStatic = BoolArgumentType.getBool(context, "static");
        String groupName = StringArgumentType.getString(context, "groupName");

        BotManager botManager = AIBrigadeMod.getBotManager();
        if (botManager == null) {
            context.getSource().sendFailure(Component.literal("Bot manager not initialized"));
            return 0;
        }

        var bot = botManager.spawnBot(level, pos, leader, behavior, radius, isStatic, groupName);

        if (bot != null) {
            context.getSource().sendSuccess(() ->
                Component.literal("Spawned bot in group '" + groupName + "' with behavior '" + behavior + "'"),
                true);
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("Failed to spawn bot"));
            return 0;
        }
    }

    /**
     * Command: /aibrigade spawn group
     * Spawns multiple bots as a group
     */
    private static int spawnBotGroup(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = player.blockPosition();

        int count = IntegerArgumentType.getInteger(context, "count");
        String leader = StringArgumentType.getString(context, "leader");
        String behavior = StringArgumentType.getString(context, "behavior");
        float radius = FloatArgumentType.getFloat(context, "radius");
        boolean isStatic = BoolArgumentType.getBool(context, "static");
        String groupName = StringArgumentType.getString(context, "groupName");

        BotManager botManager = AIBrigadeMod.getBotManager();
        if (botManager == null) {
            context.getSource().sendFailure(Component.literal("Bot manager not initialized"));
            return 0;
        }

        int spawned = botManager.spawnBotGroup(level, pos, count, leader, behavior, radius, isStatic, groupName);

        context.getSource().sendSuccess(() ->
            Component.literal("Spawned " + spawned + "/" + count + " bots in group '" + groupName + "'"),
            true);

        return spawned;
    }

    /**
     * Command: /aibrigade assignleader
     * Assigns a new leader to a group
     */
    private static int assignLeader(CommandContext<CommandSourceStack> context) {
        String groupName = StringArgumentType.getString(context, "groupName");
        String leaderName = StringArgumentType.getString(context, "leaderName");

        BotManager botManager = AIBrigadeMod.getBotManager();
        if (botManager == null) {
            context.getSource().sendFailure(Component.literal("Bot manager not initialized"));
            return 0;
        }

        boolean success = botManager.assignLeader(groupName, leaderName);

        if (success) {
            context.getSource().sendSuccess(() ->
                Component.literal("Assigned leader '" + leaderName + "' to group '" + groupName + "'"),
                true);
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("Failed to assign leader - group not found"));
            return 0;
        }
    }

    /**
     * Command: /aibrigade hostile
     * Makes one group hostile towards another
     */
    private static int setHostile(CommandContext<CommandSourceStack> context) {
        String sourceGroup = StringArgumentType.getString(context, "sourceGroup");
        String targetGroup = StringArgumentType.getString(context, "targetGroup");

        BotManager botManager = AIBrigadeMod.getBotManager();
        if (botManager == null) {
            context.getSource().sendFailure(Component.literal("Bot manager not initialized"));
            return 0;
        }

        botManager.setGroupHostile(sourceGroup, targetGroup);

        context.getSource().sendSuccess(() ->
            Component.literal("Group '" + sourceGroup + "' is now hostile towards '" + targetGroup + "'"),
            true);

        return 1;
    }

    /**
     * Command: /aibrigade sethostiletogroup
     * Makes the executing player hostile to a group
     */
    private static int setPlayerHostileToGroup(CommandContext<CommandSourceStack> context) {
        String groupName = StringArgumentType.getString(context, "groupName");

        BotManager botManager = AIBrigadeMod.getBotManager();
        if (botManager == null) {
            context.getSource().sendFailure(Component.literal("Bot manager not initialized"));
            return 0;
        }

        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            botManager.setPlayerGroupRelationship(player.getUUID(), groupName,
                com.aibrigade.bots.TeamRelationship.HOSTILE);

            context.getSource().sendSuccess(() ->
                Component.literal("You are now hostile towards group '" + groupName + "'"),
                true);

            return 1;
        } catch (CommandSyntaxException e) {
            context.getSource().sendFailure(Component.literal("This command must be executed by a player"));
            return 0;
        }
    }

    /**
     * Command: /aibrigade followleader <groupName> <true/false> <radius>
     * Enables/disables follow leader mode for a group with specified radius
     * Automatically assigns the executing player as the leader
     * Selon le cahier des charges:
     * - 5/6 des bots suivent dans le radius défini
     * - 1/6 des bots suivent activement le leader
     */
    private static int setFollowLeader(CommandContext<CommandSourceStack> context) {
        String groupName = StringArgumentType.getString(context, "groupName");
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        float radius = FloatArgumentType.getFloat(context, "radius");

        BotManager botManager = AIBrigadeMod.getBotManager();
        if (botManager == null) {
            context.getSource().sendFailure(Component.literal("Bot manager not initialized"));
            return 0;
        }

        // CRITICAL FIX: Automatically assign the executing player as the leader
        if (enabled) {
            try {
                var player = context.getSource().getPlayerOrException();
                String leaderName = player.getGameProfile().getName();

                AIBrigadeMod.LOGGER.info("Auto-assigning leader {} to group {}", leaderName, groupName);
                boolean assignSuccess = botManager.assignLeader(groupName, leaderName);

                if (!assignSuccess) {
                    context.getSource().sendFailure(Component.literal("Failed to assign leader - group not found"));
                    return 0;
                }
            } catch (CommandSyntaxException e) {
                context.getSource().sendFailure(Component.literal("This command must be executed by a player"));
                return 0;
            }
        }

        boolean success = botManager.setFollowLeader(groupName, enabled, radius);

        if (success) {
            String playerName;
            try {
                playerName = context.getSource().getPlayerOrException().getGameProfile().getName();
            } catch (CommandSyntaxException e) {
                playerName = "Unknown";
            }

            String finalPlayerName = playerName;
            context.getSource().sendSuccess(() ->
                Component.literal("Follow leader mode " + (enabled ? "enabled" : "disabled") +
                    " for group '" + groupName + "' with radius " + radius +
                    (enabled ? "\n§7Leader: " + finalPlayerName : "") +
                    "\n§7(5/6 bots follow in radius, 1/6 follow actively)"),
                true);
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("Failed to set follow leader - group not found"));
            return 0;
        }
    }

    /**
     * Command: /aibrigade givearmor
     * Gives armor to a bot or group
     */
    private static int giveArmor(CommandContext<CommandSourceStack> context) {
        String target = StringArgumentType.getString(context, "target");
        String type = StringArgumentType.getString(context, "type");
        String materials = StringArgumentType.getString(context, "materials");

        BotManager botManager = AIBrigadeMod.getBotManager();
        if (botManager == null) {
            context.getSource().sendFailure(Component.literal("Bot manager not initialized"));
            return 0;
        }

        boolean isFull = type.equalsIgnoreCase("full");
        int equipped = botManager.giveArmor(target, isFull, materials);

        if (equipped > 0) {
            int finalEquipped = equipped;
            context.getSource().sendSuccess(() ->
                Component.literal("Equipped " + finalEquipped + " bot(s) with " + materials + " armor (" + type + ")"),
                true);
            return equipped;
        } else {
            context.getSource().sendFailure(Component.literal("Failed to equip armor - target not found"));
            return 0;
        }
    }

    /**
     * Command: /aibrigade setbehavior
     * Changes behavior of bot or group
     */
    private static int setBehavior(CommandContext<CommandSourceStack> context) {
        String target = StringArgumentType.getString(context, "target");
        String behavior = StringArgumentType.getString(context, "behavior");

        AIManager aiManager = AIBrigadeMod.getAIManager();
        if (aiManager == null) {
            context.getSource().sendFailure(Component.literal("AI manager not initialized"));
            return 0;
        }

        aiManager.applyGroupBehavior(target, behavior);

        context.getSource().sendSuccess(() ->
            Component.literal("Set behavior of '" + target + "' to '" + behavior + "'"),
            true);

        return 1;
    }

    /**
     * Command: /aibrigade setradius
     * Sets follow radius for a group
     */
    private static int setRadius(CommandContext<CommandSourceStack> context) {
        String groupName = StringArgumentType.getString(context, "groupName");
        float radius = FloatArgumentType.getFloat(context, "radius");

        AIManager aiManager = AIBrigadeMod.getAIManager();
        if (aiManager == null) {
            context.getSource().sendFailure(Component.literal("AI manager not initialized"));
            return 0;
        }

        aiManager.setGroupRadius(groupName, radius);

        context.getSource().sendSuccess(() ->
            Component.literal("Set follow radius of group '" + groupName + "' to " + radius + " blocks"),
            true);

        return 1;
    }

    /**
     * Command: /aibrigade togglestatic
     * Toggles static/mobile state of bot or group
     */
    private static int toggleStatic(CommandContext<CommandSourceStack> context) {
        String target = StringArgumentType.getString(context, "target");

        AIManager aiManager = AIBrigadeMod.getAIManager();
        if (aiManager == null) {
            context.getSource().sendFailure(Component.literal("AI manager not initialized"));
            return 0;
        }

        aiManager.toggleStatic(target);

        context.getSource().sendSuccess(() ->
            Component.literal("Toggled static state for '" + target + "'"),
            true);

        return 1;
    }

    /**
     * Command: /aibrigade removebot
     * Removes a single bot
     */
    private static int removeBot(CommandContext<CommandSourceStack> context) {
        String botName = StringArgumentType.getString(context, "botName");

        BotManager botManager = AIBrigadeMod.getBotManager();
        if (botManager == null) {
            context.getSource().sendFailure(Component.literal("Bot manager not initialized"));
            return 0;
        }

        // Find bot by name
        var bot = botManager.findBotByName(botName);
        if (bot == null) {
            context.getSource().sendFailure(Component.literal("Bot '" + botName + "' not found"));
            return 0;
        }

        // Remove the bot
        boolean removed = botManager.removeBot(bot.getUUID());

        if (removed) {
            context.getSource().sendSuccess(() ->
                Component.literal("Removed bot '" + botName + "'"),
                true);
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("Failed to remove bot"));
            return 0;
        }
    }

    /**
     * Command: /aibrigade removegroup
     * Removes an entire bot group
     */
    private static int removeGroup(CommandContext<CommandSourceStack> context) {
        String groupName = StringArgumentType.getString(context, "groupName");

        BotManager botManager = AIBrigadeMod.getBotManager();
        if (botManager == null) {
            context.getSource().sendFailure(Component.literal("Bot manager not initialized"));
            return 0;
        }

        int removed = botManager.removeGroup(groupName);

        if (removed > 0) {
            int finalRemoved = removed;
            context.getSource().sendSuccess(() ->
                Component.literal("Removed group '" + groupName + "' (" + finalRemoved + " bots)"),
                true);
            return removed;
        } else {
            context.getSource().sendFailure(Component.literal("Group not found"));
            return 0;
        }
    }

    /**
     * Command: /aibrigade groupinfo
     * Shows information about a group
     */
    private static int groupInfo(CommandContext<CommandSourceStack> context) {
        String groupName = StringArgumentType.getString(context, "groupName");

        BotManager botManager = AIBrigadeMod.getBotManager();
        if (botManager == null) {
            context.getSource().sendFailure(Component.literal("Bot manager not initialized"));
            return 0;
        }

        BotManager.BotGroup group = botManager.getBotGroups().get(groupName);

        if (group == null) {
            context.getSource().sendFailure(Component.literal("Group not found"));
            return 0;
        }

        context.getSource().sendSuccess(() ->
            Component.literal("Group '" + groupName + "':\n" +
                "  Leader: " + group.getLeaderName() + "\n" +
                "  Bot count: " + group.getBotIds().size() + "\n" +
                "  Follow radius: " + group.getFollowRadius()),
            false);

        return 1;
    }

    /**
     * Command: /aibrigade listbots
     * Lists all active bots
     */
    private static int listBots(CommandContext<CommandSourceStack> context) {
        BotManager botManager = AIBrigadeMod.getBotManager();
        if (botManager == null) {
            context.getSource().sendFailure(Component.literal("Bot manager not initialized"));
            return 0;
        }

        int count = botManager.getBotCount();
        int max = botManager.getMaxBots();

        context.getSource().sendSuccess(() ->
            Component.literal("Active bots: " + count + " / " + max),
            false);

        return 1;
    }

    /**
     * Command: /aibrigade cleanupbots
     * Manually triggers cleanup of dead bots
     */
    private static int cleanupBots(CommandContext<CommandSourceStack> context) {
        BotManager botManager = AIBrigadeMod.getBotManager();
        if (botManager == null) {
            context.getSource().sendFailure(Component.literal("Bot manager not initialized"));
            return 0;
        }

        int beforeCount = botManager.getBotCount();
        botManager.cleanupDeadBots();
        int afterCount = botManager.getBotCount();
        int cleaned = beforeCount - afterCount;

        context.getSource().sendSuccess(() ->
            Component.literal("Cleaned up " + cleaned + " dead bots. Active bots: " + afterCount + " / " + botManager.getMaxBots()),
            true);

        return 1;
    }

    /**
     * Command: /aibrigade listgroups
     * Lists all bot groups
     */
    private static int listGroups(CommandContext<CommandSourceStack> context) {
        BotManager botManager = AIBrigadeMod.getBotManager();
        if (botManager == null) {
            context.getSource().sendFailure(Component.literal("Bot manager not initialized"));
            return 0;
        }

        var groups = botManager.getBotGroups();

        if (groups.isEmpty()) {
            context.getSource().sendSuccess(() ->
                Component.literal("No bot groups found"),
                false);
            return 0;
        }

        StringBuilder message = new StringBuilder("Bot groups:\n");
        for (var entry : groups.entrySet()) {
            message.append("  - ").append(entry.getKey())
                   .append(" (").append(entry.getValue().getBotIds().size()).append(" bots)\n");
        }

        String finalMessage = message.toString();
        context.getSource().sendSuccess(() ->
            Component.literal(finalMessage),
            false);

        return groups.size();
    }

    /**
     * Command: /aibrigade help
     * Shows help information
     */
    private static int showHelp(CommandContext<CommandSourceStack> context) {
        String helpText = """
            === AIBrigade Commands ===
            /aibrigade spawn solo <leader> <behavior> <radius> <static> <groupName>
            /aibrigade spawn group <count> <leader> <behavior> <radius> <static> <groupName>
            /aibrigade assignleader <groupName> <leaderName>
            /aibrigade followleader <groupName> <true/false> <radius>
              -> 5/6 bots follow in radius, 1/6 follow actively
            /aibrigade hostile <sourceGroup> <targetGroup>
            /aibrigade givearmor <target> <full|partial> <materials>

            === Modify Commands ===
            /aibrigade modify name <botName> <newName>
              -> Changes bot name and syncs skin from new username
            /aibrigade modify armor <botName> <slot> <type> <materials>
              -> Slots: full, head, chest, legs, boots
              -> Types: random, preset
              -> Example: /aibrigade modify armor Bot1 full random diamondironleather
            /aibrigade modify hand <botName> <item>
              -> Example: /aibrigade modify hand Bot1 diamond_sword
            /aibrigade modify offhand <botName> <item>
              -> Example: /aibrigade modify offhand Bot1 shield

            === Other Commands ===
            /aibrigade setbehavior <target> <behavior>
            /aibrigade setradius <groupName> <radius>
            /aibrigade togglestatic <target>
            /aibrigade removebot <botName>
            /aibrigade removegroup <groupName>
            /aibrigade groupinfo <groupName>
            /aibrigade listbots - Show active bot count
            /aibrigade cleanupbots - Manually remove dead bots
            /aibrigade listgroups

            Behaviors: follow, patrol, raid, guard
            Armor materials: diamond, iron, chainmail, leather, gold, netherite
            Equipment: Weighted distribution (20% nothing, 15% iron pickaxe, 10% diamond pickaxe,
                       20% cooked beef, 20% iron sword, 15% diamond sword)

            Note: Dead bots are automatically cleaned every 5 seconds
            """;

        context.getSource().sendSuccess(() ->
            Component.literal(helpText),
            false);

        return 1;
    }

    /**
     * Command: /aibrigade modify name
     * Changes bot name and syncs skin from new username
     */
    private static int modifyName(CommandContext<CommandSourceStack> context) {
        String botName = StringArgumentType.getString(context, "botName");
        String newName = StringArgumentType.getString(context, "newName");

        BotManager botManager = AIBrigadeMod.getBotManager();
        if (botManager == null) {
            context.getSource().sendFailure(Component.literal("Bot manager not initialized"));
            return 0;
        }

        boolean success = BotModifyCommands.modifyBotName(botManager, botName, newName);

        if (success) {
            context.getSource().sendSuccess(() ->
                Component.literal("Changed bot '" + botName + "' name to '" + newName + "' and synced skin"),
                true);
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("Bot '" + botName + "' not found"));
            return 0;
        }
    }

    /**
     * Command: /aibrigade modify armor
     * Modifies bot armor with random or preset materials
     * Example: /aibrigade modify armor Bot1 full random diamondironleather
     */
    private static int modifyArmor(CommandContext<CommandSourceStack> context) {
        String botName = StringArgumentType.getString(context, "botName");
        String slot = StringArgumentType.getString(context, "slot");
        String type = StringArgumentType.getString(context, "type");
        String materials = StringArgumentType.getString(context, "materials");

        BotManager botManager = AIBrigadeMod.getBotManager();
        if (botManager == null) {
            context.getSource().sendFailure(Component.literal("Bot manager not initialized"));
            return 0;
        }

        boolean success = BotModifyCommands.modifyBotArmor(botManager, botName, slot, type, materials);

        if (success) {
            context.getSource().sendSuccess(() ->
                Component.literal("Equipped " + slot + " armor on bot '" + botName + "' (" + type + ": " + materials + ")"),
                true);
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("Failed to modify armor - bot not found or invalid parameters"));
            return 0;
        }
    }

    /**
     * Command: /aibrigade modify hand
     * Sets item in bot's main hand
     * Example: /aibrigade modify hand Bot1 diamond_sword
     */
    private static int modifyHand(CommandContext<CommandSourceStack> context) {
        String botName = StringArgumentType.getString(context, "botName");
        String item = StringArgumentType.getString(context, "item");

        BotManager botManager = AIBrigadeMod.getBotManager();
        if (botManager == null) {
            context.getSource().sendFailure(Component.literal("Bot manager not initialized"));
            return 0;
        }

        boolean success = BotModifyCommands.modifyBotHand(botManager, botName, item);

        if (success) {
            context.getSource().sendSuccess(() ->
                Component.literal("Set " + item + " in hand for bot '" + botName + "'"),
                true);
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("Failed to set hand item - bot or item not found"));
            return 0;
        }
    }

    /**
     * Command: /aibrigade modify offhand
     * Sets item in bot's offhand
     * Example: /aibrigade modify offhand Bot1 shield
     */
    private static int modifyOffhand(CommandContext<CommandSourceStack> context) {
        String botName = StringArgumentType.getString(context, "botName");
        String item = StringArgumentType.getString(context, "item");

        BotManager botManager = AIBrigadeMod.getBotManager();
        if (botManager == null) {
            context.getSource().sendFailure(Component.literal("Bot manager not initialized"));
            return 0;
        }

        boolean success = BotModifyCommands.modifyBotOffhand(botManager, botName, item);

        if (success) {
            context.getSource().sendSuccess(() ->
                Component.literal("Set " + item + " in offhand for bot '" + botName + "'"),
                true);
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("Failed to set offhand item - bot or item not found"));
            return 0;
        }
    }

}

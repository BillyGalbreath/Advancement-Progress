package net.pl3x.advancementprogress;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class AdvancementProgressPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        saveDefaultConfig();

        registerCommand(new Command("listbiomes", "adventure/adventuring_time", "adventure.adventuring_time", "biome.minecraft."));
        registerCommand(new Command("listbreeds", "husbandry/bred_all_animals", "husbandry.breed_all_animals", "entity.minecraft."));
        registerCommand(new Command("listfoods", "husbandry/balanced_diet", "husbandry.balanced_diet", "item.minecraft."));
        registerCommand(new Command("listmobs", "adventure/kill_all_mobs", "adventure.kill_all_mobs", "entity.minecraft."));
    }

    private void registerCommand(Command command) {
        PluginCommand pluginCmd = getCommand(command.commandName);
        if (pluginCmd != null) {
            pluginCmd.setExecutor(command);
        }
    }

    private class Command implements TabExecutor {
        private final String commandName;
        private final String advancementKey;
        private final String titleKey;
        private final String descriptionKey;
        private final String itemKey;

        public Command(String commandName, String advancementKey, String titleKey, String itemKey) {
            this.commandName = commandName;
            this.advancementKey = advancementKey;
            this.titleKey = String.format("advancements.%s.title", titleKey);
            this.descriptionKey = String.format("advancements.%s.description", titleKey);
            this.itemKey = itemKey;
        }

        @Override
        public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
            return Collections.emptyList();
        }

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.translatable("permissions.requires.player").color(NamedTextColor.RED));
                return true;
            }

            Advancement advancement = Bukkit.getAdvancement(new NamespacedKey("minecraft", this.advancementKey));
            if (advancement == null) {
                sender.sendMessage(Component.translatable("advancement.advancementNotFound", Component.text(this.advancementKey)).color(NamedTextColor.RED));
                return true;
            }

            Map<String, Boolean> map = new TreeMap<>();

            org.bukkit.advancement.AdvancementProgress progress = player.getAdvancementProgress(advancement);
            progress.getAwardedCriteria().forEach(name -> map.put(name, true));
            progress.getRemainingCriteria().forEach(name -> map.put(name, false));

            TranslatableComponent translatable = Component.translatable(this.titleKey);

            String hoverTxt = getConfig().getString("advancement-hover", "");
            if (!hoverTxt.isBlank()) {
                translatable = translatable.hoverEvent(HoverEvent.showText(
                        MiniMessage.miniMessage().deserialize(hoverTxt,
                                Placeholder.component("title", translatable),
                                Placeholder.component("description", Component.translatable(this.descriptionKey)),
                                Placeholder.unparsed("complete", Integer.toString(Collections.frequency(map.values(), true))),
                                Placeholder.unparsed("total", Integer.toString(map.size()))
                        )
                ));
            }

            sender.sendMessage(MiniMessage.miniMessage().deserialize(getConfig().getString("advancement-progress", ""),
                    Placeholder.component("advancement", translatable),
                    Placeholder.component("list", Component.join(
                            JoinConfiguration.separator(MiniMessage.miniMessage().deserialize(getConfig().getString("list-separator", ", "))),
                            map.entrySet().stream().map(entry ->
                                    Component.translatable(this.itemKey + entry.getKey().replace("minecraft:", ""))
                                            .color(entry.getValue() ? NamedTextColor.GREEN : NamedTextColor.RED)
                            ).toList()
                    ))
            ));
            return true;
        }
    }
}

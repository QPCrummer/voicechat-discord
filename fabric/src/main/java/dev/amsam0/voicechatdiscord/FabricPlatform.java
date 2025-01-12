package dev.amsam0.voicechatdiscord;

import com.mojang.brigadier.context.CommandContext;
import de.maxhenkel.voicechat.api.Player;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.ServerPlayer;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.*;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static dev.amsam0.voicechatdiscord.Core.api;
import static dev.amsam0.voicechatdiscord.FabricMod.LOGGER;

public class FabricPlatform implements Platform {
    public boolean isValidPlayer(Object sender) {
        if (sender instanceof CommandContext<?> source)
            return ((ServerCommandSource) source.getSource()).getPlayer() != null;
        return sender != null;
    }

    public ServerPlayer commandContextToPlayer(CommandContext<?> context) {
        return api.fromServerPlayer(((ServerCommandSource) context.getSource()).getPlayer());
    }

    public @Nullable Position getEntityPosition(ServerLevel level, UUID uuid) {
        ServerWorld world = (ServerWorld) level.getServerLevel();
        Entity entity = world.getEntity(uuid);
        return entity != null ?
                api.createPosition(
                        entity.getX(),
                        entity.getY(),
                        entity.getZ()
                )
                : null;
    }

    public double getEntityFacing(ServerLevel level, UUID uuid) {
        ServerWorld world = (ServerWorld) level.getServerLevel();
        Entity entity = world.getEntity(uuid);
        return entity != null ?
                Math.toRadians(entity.getFacing().asRotation())
                : 0.0;
    }

    public boolean isOperator(Object sender) {
        if (sender instanceof CommandContext<?> source)
            return ((ServerCommandSource) source.getSource()).hasPermissionLevel(2);
        if (sender instanceof ServerPlayerEntity player)
            return player.hasPermissionLevel(2);

        return false;
    }

    public boolean hasPermission(Object sender, String permission) {
        if (sender instanceof CommandContext<?> source)
            return Permissions.check((ServerCommandSource) source.getSource(), permission);
        if (sender instanceof ServerPlayerEntity player)
            return Permissions.check(player, permission);

        return false;
    }

    public void sendMessage(Object sender, String message) {
        if (sender instanceof ServerPlayerEntity player)
            player.sendMessage(toNative(mm(message)));
        else if (sender instanceof CommandContext<?> context) {
            ServerCommandSource source = (ServerCommandSource) context.getSource();
            source.sendMessage(toNative(mm(message)));
        } else
            warn("Seems like we are trying to send a message to a sender which was not recognized (it is a " + sender.getClass().getSimpleName() + "). Please report this on GitHub issues!");
    }

    public void sendMessage(Player player, String message) {
        ((ServerPlayerEntity) player.getPlayer()).sendMessage(toNative(mm(message)));
    }

    public String getName(Player player) {
        return ((PlayerEntity) player.getPlayer()).getName().getString();
    }

    public String getConfigPath() {
        return "config/voicechat-discord.yml";
    }

    public Loader getLoader() {
        return Loader.FABRIC;
    }

    public void info(String message) {
        LOGGER.info(ansi(mm(message)));
    }

    public void infoRaw(String message) {
        LOGGER.info(message);
    }

    // warn and error will already be colored yellow and red respectfully

    public void warn(String message) {
        LOGGER.warn(message);
    }

    public void error(String message) {
        LOGGER.error(message);
    }

    private Text toNative(Component component) {
        try {
            MutableText text;
            if (component instanceof TextComponent textComponent) {
                text = Text.literal(textComponent.content());
//                TextContent content;
//                try {
//                    // This should work in >=1.20.3
//                    content = PlainTextContent.of(textComponent.content());
//                    debug("used PlainTextContent");
//                } catch (NoClassDefFoundError ignored) {
//                    // In <=1.20.2, we can try to use reflection
//                    try {
//                        // Try to get the LiteralTextContent class and use its constructor
//                        content = (TextContent) Class
//                                .forName("net.minecraft.class_2585")
//                                .getDeclaredConstructor(String.class)
//                                .newInstance(textComponent.content());
//                        debug("used LiteralTextContent");
//                    } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException |
//                             IllegalAccessException | InvocationTargetException ignored2) {
//                        // If we can't use the official classes, try using our potentially broken TextContent implementation
//                        content = new Literal(textComponent.content());
//                        debug("used scuffed TextContent implementation");
//                    }
//                }
//                text = MutableText.of(content);
            } else {
                warn("Unimplemented component type: " + component.getClass().getName());
                return Text.of(LegacyComponentSerializer.legacySection().serialize(component));
            }

            Style style = Style.EMPTY;

            var font = component.font();
            if (font != null) {
                warn("Fonts are not implemented");
            }

            var color = component.color();
            if (color != null)
                style = style.withColor(TextColor.fromRgb(Integer.parseInt(color.asHexString().substring(1), 16)));

            for (var entry : component.decorations().entrySet()) {
                var decoration = entry.getKey();
                var state = entry.getValue();

                if (state != TextDecoration.State.TRUE)
                    continue;

                switch (decoration) {
                    case OBFUSCATED -> style = style.withObfuscated(true);
                    case BOLD -> style = style.withBold(true);
                    case STRIKETHROUGH -> style = style.withStrikethrough(true);
                    case UNDERLINED -> style = style.withUnderline(true);
                    case ITALIC -> style = style.withItalic(true);
                    default -> warn("Unknown decoration: " + decoration);
                }
            }

            var clickEvent = component.clickEvent();
            if (clickEvent != null) {
                ClickEvent.Action action = null;
                switch (clickEvent.action()) {
                    case OPEN_URL -> action = ClickEvent.Action.OPEN_URL;
                    case OPEN_FILE -> action = ClickEvent.Action.OPEN_FILE;
                    case RUN_COMMAND -> action = ClickEvent.Action.RUN_COMMAND;
                    case SUGGEST_COMMAND -> action = ClickEvent.Action.SUGGEST_COMMAND;
                    case CHANGE_PAGE -> action = ClickEvent.Action.CHANGE_PAGE;
                    case COPY_TO_CLIPBOARD -> action = ClickEvent.Action.COPY_TO_CLIPBOARD;
                    default -> warn("Unknown click event action: " + clickEvent.action());
                }
                style = style.withClickEvent(new ClickEvent(action, clickEvent.value()));
            }

            var hoverEvent = component.hoverEvent();
            if (hoverEvent != null) {
                warn("Hover events are not implemented");
            }

            var insertion = component.insertion();
            if (insertion != null) {
                warn("Insertions are not implemented");
            }

            text.setStyle(style);
            for (var child : component.children()) {
                text.append(toNative(child));
            }

            return text;
        } catch (Throwable e) {
            warn("Error when converting component to native: " + e.getMessage());
            debug(e);
            return Text.of(LegacyComponentSerializer.legacySection().serialize(component));
        }
    }

//    private record Literal(String string) implements TextContent {
//        public <T> Optional<T> visit(StringVisitable.Visitor<T> visitor) {
//            return visitor.accept(this.string);
//        }
//
//        @Override
//        public Type<?> getType() {
//            try {
//                return PlainTextContent.TYPE;
//            } catch (NoClassDefFoundError ignored) {
//                return KeybindTextContent.TYPE;
//            }
//        }
//
//        public <T> Optional<T> visit(StringVisitable.StyledVisitor<T> visitor, Style style) {
//            return visitor.accept(style, this.string);
//        }
//
//        public String toString() {
//            return "literal{" + this.string + "}";
//        }
//    }
}
package dev.amsam0.voicechatdiscord.util;

import com.mojang.brigadier.context.CommandContext;
import de.maxhenkel.voicechat.api.Position;
import org.jetbrains.annotations.Nullable;

public final class Util {
    public static double clamp(double val, double min, double max) {
        return Math.min(max, Math.max(min, val));
    }

    public static double distance(Position pos1, Position pos2) {
        return Math.sqrt(
                Math.pow(pos1.getX() - pos2.getX(), 2) +
                        Math.pow(pos1.getY() - pos2.getY(), 2) +
                        Math.pow(pos1.getZ() - pos2.getZ(), 2)
        );
    }

    public static double angle(double playerFacing, Position playerPosition, Position audioSourcePosition) {
        double deltaX = audioSourcePosition.getX() - playerPosition.getX();
        double deltaY = audioSourcePosition.getY() - playerPosition.getY();
        double angleToSource = Math.atan2(deltaY, deltaX);
        double relativeAngle = angleToSource - playerFacing;
        return Math.atan2(Math.sin(relativeAngle), Math.cos(relativeAngle));
    }

    public static String positionToString(Position pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    public static @Nullable <V> V getArgumentOr(CommandContext<?> context, final String name, final Class<V> clazz, @Nullable V or) {
        try {
            return context.getArgument(name, clazz);
        } catch (IllegalArgumentException ignored) {
            return or;
        }
    }
}

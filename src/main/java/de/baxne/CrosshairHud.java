package de.baxne;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;

public class CrosshairHud {
    /** Ändert die FORM des Fadenkreuzes: Gegner mit Schild = Form A, Gegner ohne Schild = Form B. */
    public static void render(GuiGraphicsExtractor g, DeltaTracker dt) {
        if (!Config.crosshairShapes) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || Compat.hideGui()) return;
        Player t = Util.targetPlayer();
        if (t == null) return;
        int shape = Util.isShielding(t) ? Config.shapeShield : Config.shapeEnemy;
        Shapes.draw(g, shape, g.guiWidth() / 2, g.guiHeight() / 2, Config.crosshairSize, Config.crosshairColor);
    }
}

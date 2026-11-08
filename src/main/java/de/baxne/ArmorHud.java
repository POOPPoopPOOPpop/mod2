package de.baxne;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Armor-HUD im Stil von Uku's Armor HUD: Rüstung (+ Hand) mit Haltbarkeit in Prozent. */
public class ArmorHud {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public static void render(GuiGraphicsExtractor g, DeltaTracker dt) {
        if (!Config.armorHud) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || Compat.hideGui()) return;

        List<ItemStack> items = new ArrayList<>();
        for (EquipmentSlot s : SLOTS) { ItemStack st = mc.player.getItemBySlot(s); if (!st.isEmpty()) items.add(st); }
        if (Config.hudShowHand) {
            ItemStack hand = mc.player.getMainHandItem();
            if (!hand.isEmpty() && hand.isDamageableItem()) items.add(hand);
        }

        int x = g.guiWidth() * Config.hudX / 100, y = g.guiHeight() * Config.hudY / 100;
        for (int i = 0; i < items.size(); i++) {
            ItemStack st = items.get(i);
            int ix = x + i * 26;
            Compat.drawItem(g, st, ix, y);
            if (st.isDamageableItem()) {
                int max = st.getMaxDamage();
                int pct = Math.max(0, Math.round(100f * (max - st.getDamageValue()) / max));
                int color = pct > 60 ? 0xFF55FF55 : pct > 30 ? 0xFFFFFF55 : 0xFFFF5555;
                g.text(mc.font, pct + "%", ix, y + 18, color, true);
            }
        }
    }
}

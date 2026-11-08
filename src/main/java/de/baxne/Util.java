package de.baxne;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.EquipmentSlot;

public final class Util {
    private static final Minecraft MC = Minecraft.getInstance();
    private Util() {}

    public static void select(int slot) {
        if (slot < 0 || slot > 8) return;
        MC.player.getInventory().setSelectedSlot(slot);
    }

    /** Sucht Hotbar-Slot (0-8), dessen Item das Prädikat erfüllt. */
    public static int findHotbar(java.util.function.Predicate<ItemStack> p) {
        Inventory inv = MC.player.getInventory();
        for (int i = 0; i < 9; i++) if (p.test(inv.getItem(i))) return i;
        return -1;
    }

    public static boolean isSpear(ItemStack s) {
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
        return id.endsWith("_spear");
    }

    public static boolean isAxe(ItemStack s) { return s.is(ItemTags.AXES); }
    public static boolean isMace(ItemStack s) { return s.is(Items.MACE); }

    public static boolean hasEnchant(ItemStack s, net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key) {
        var ench = EnchantmentHelper.getEnchantmentsForCrafting(s);
        for (Holder<net.minecraft.world.item.enchantment.Enchantment> h : ench.keySet())
            if (h.is(key)) return true;
        return false;
    }

    public static int findMaceWith(net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key) {
        return findHotbar(s -> isMace(s) && hasEnchant(s, key));
    }

    /** Hat das Ziel Rüstung an? -> Breach, sonst Density. */
    public static boolean isArmored(Player p) {
        for (EquipmentSlot es : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET})
            if (!p.getItemBySlot(es).isEmpty()) return true;
        return false;
    }

    public static int chooseMace(Player target) {
        int breach = findMaceWith(Enchantments.BREACH);
        int density = findMaceWith(Enchantments.DENSITY);
        int any = findHotbar(Util::isMace);
        if (Config.maceForceBreach && breach >= 0) return breach;
        if (target != null && isArmored(target)) {
            if (breach >= 0) return breach;
            if (density >= 0) return density;
        } else {
            if (density >= 0) return density;
            if (breach >= 0) return breach;
        }
        return any;
    }

    public static Player targetPlayer() {
        var hit = MC.hitResult;
        if (hit instanceof net.minecraft.world.phys.EntityHitResult ehr && ehr.getEntity() instanceof Player p && p != MC.player)
            return p;
        return null;
    }

    public static boolean isShielding(Player p) {
        return p.isBlocking();
    }

    public static void attack(net.minecraft.world.entity.Entity target) {
        if (target == null) return;
        MC.gameMode.attack(MC.player, target);
    }
}

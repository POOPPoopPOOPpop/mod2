package de.baxne;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;

import java.util.ArrayList;

/** Tick-Logik aller Module. */
public class Modules {
    private static final Minecraft MC = Minecraft.getInstance();

    private enum Stage { IDLE, AXE_WAIT, MACE_WAIT }
    private static Stage stage = Stage.IDLE;
    private static int timer = 0;
    private static Player stunTarget;
    private static boolean spearBusy = false;
    private static int fireworkCooldown = 0;
    private static double clickAccumulator = 0;
    private static boolean wasJump = false;
    private static int windTimer = -1, windCd = 0, totemCd = 0;

    // ---- kleiner Zeitplaner ----
    private static final class Task { int t; Runnable r; Task(int t, Runnable r) { this.t = t; this.r = r; } }
    private static final ArrayList<Task> tasks = new ArrayList<>();
    static void later(int ticks, Runnable r) { tasks.add(new Task(Math.max(0, ticks), r)); }
    private static void runTasks() {
        for (Task k : new ArrayList<>(tasks)) { if (k.t-- <= 0) { tasks.remove(k); k.r.run(); } }
    }

    public static void tick() {
        if (MC.player == null || MC.level == null || Compat.currentScreen() != null) return;
        runTasks();

        while (BaxneClient.CLICKER_KEY.consumeClick()) Config.autoClicker = !Config.autoClicker;
        while (BaxneClient.XRAY_KEY.consumeClick()) { Config.xray = !Config.xray; Compat.reloadChunks(); }

        // Spear Swap + Lunge
        if (Config.spearSwap && BaxneClient.SPEAR_KEY.consumeClick() && !spearBusy) {
            int slot = spearSlot();
            if (slot >= 0) {
                final int back = MC.player.getInventory().getSelectedSlot();
                Util.select(slot);
                spearBusy = true;
                later(Config.spearLungeDelay, Modules::lunge);
                // am Ende automatisch zurück zur letzten Waffe / zum letzten Item
                later(Config.spearLungeDelay + Config.spearBackDelay, () -> { Util.select(back); spearBusy = false; });
            }
        }

        // Elytra
        if (Config.elytraSwap && BaxneClient.ELYTRA_KEY.consumeClick()) swapElytra();
        elytraFirework();

        // Auto Mace Swap
        if (Config.autoMaceSwap && stage == Stage.IDLE && MC.player.fallDistance >= Config.minFallForSlam) {
            Player t = Util.targetPlayer();
            if (t != null) { int m = Util.chooseMace(t); if (m >= 0) Util.select(m); }
        }

        // Auto Stun Slam
        if (Config.autoStunSlam && BaxneClient.STUN_KEY.consumeClick() && stage == Stage.IDLE) {
            Player t = Util.targetPlayer();
            if (t != null && MC.player.distanceTo(t) <= Config.stunRange) {
                int axe = Util.findHotbar(Util::isAxe);
                if (axe >= 0) { Util.select(axe); stunTarget = t; stage = Stage.AXE_WAIT; timer = Config.stunDelay; }
            }
        }
        switch (stage) {
            case AXE_WAIT -> { if (timer-- <= 0) {
                if (stunTarget != null && stunTarget.isAlive()) Util.attack(stunTarget);
                int m = Util.chooseMace(stunTarget); if (m >= 0) Util.select(m);
                stage = Stage.MACE_WAIT; timer = Config.stunDelay; } }
            case MACE_WAIT -> { if (timer-- <= 0) {
                if (stunTarget != null && stunTarget.isAlive()) Util.attack(stunTarget);
                stage = Stage.IDLE; stunTarget = null; } }
            default -> {}
        }

        windJump();
        totem();
        triggerbot();
        if (Config.autoSprint && MC.options.keyUp.isDown() && !MC.player.isUsingItem()) MC.options.keySprint.setDown(true);
        autoClick();
    }

    private static int spearSlot() {
        return Config.spearSlot > 0 ? Config.spearSlot - 1 : Util.findHotbar(Util::isSpear);
    }

    /** Spear-Lunge: Modus 0 = Jab (simulierter Linksklick), Modus 1 = Rechtsklick. */
    private static void lunge() {
        if (Config.spearMode == 1) {
            MC.gameMode.useItem(MC.player, InteractionHand.MAIN_HAND);
            return;
        }
        if (!Compat.clickAttack()) {
            Player t = Util.targetPlayer();
            if (t != null) Util.attack(t);
        }
    }

    /** Taste X: Pearl senkrecht werfen -> Spear Lunge -> zurück zur letzten Waffe/zum letzten Item. */
    public static void pearlCombo() {
        if (MC.player == null || MC.level == null || Compat.currentScreen() != null) return;
        int pearl = Util.findHotbar(s -> s.is(Items.ENDER_PEARL));
        if (pearl < 0) return;
        final int back = MC.player.getInventory().getSelectedSlot();
        Util.select(pearl);
        float old = MC.player.getXRot();
        MC.player.setXRot(Config.pearlUp ? -90f : 90f);
        MC.gameMode.useItem(MC.player, InteractionHand.MAIN_HAND);
        MC.player.setXRot(old);
        if (Config.pearlThenSpear) {
            later(Config.pearlSpearDelay, () -> {
                int sp = spearSlot();
                if (sp >= 0) { Util.select(sp); lunge(); }
            });
        }
        later(Config.pearlSpearDelay + Config.pearlBackDelay, () -> Util.select(back));
    }

    /** Sprint + Sprung mit Windcharge in der Hotbar -> ein Windcharge wird unter dich geworfen. */
    private static void windJump() {
        if (windCd > 0) windCd--;
        boolean jump = MC.options.keyJump.isDown();
        if (Config.windJump && jump && !wasJump && MC.player.isSprinting() && MC.player.onGround() && windCd == 0
                && Util.findHotbar(s -> s.is(Items.WIND_CHARGE)) >= 0) {
            windTimer = Config.windDelay;
        }
        wasJump = jump;
        if (windTimer >= 0 && windTimer-- == 0) {
            int wc = Util.findHotbar(s -> s.is(Items.WIND_CHARGE));
            if (wc < 0) return;
            final int back = MC.player.getInventory().getSelectedSlot();
            Util.select(wc);
            float old = MC.player.getXRot();
            MC.player.setXRot(90f);   // senkrecht nach unten
            MC.gameMode.useItem(MC.player, InteractionHand.MAIN_HAND);
            MC.player.setXRot(old);
            later(1, () -> Util.select(back));
            windCd = 15;
        }
    }

    /** Totem automatisch in die Zweithand, wenn die Leben niedrig sind. */
    private static void totem() {
        if (totemCd > 0) { totemCd--; return; }
        if (!Config.autoTotem || MC.player.getHealth() > Config.totemHp) return;
        if (MC.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return;
        var inv = MC.player.getInventory();
        for (int i = 0; i < 36; i++) {
            if (inv.getItem(i).is(Items.TOTEM_OF_UNDYING)) {
                int slot = i < 9 ? 36 + i : i;
                Compat.pickupClick(slot);
                Compat.pickupClick(45);      // 45 = Zweithand
                Compat.pickupClick(slot);
                totemCd = 10;
                return;
            }
        }
    }

    /** Triggerbot: greift an, sobald ein Lebewesen im Fadenkreuz ist und der Cooldown bereit ist. */
    private static void triggerbot() {
        if (!Config.triggerbot || stage != Stage.IDLE || MC.options.keyAttack.isDown()) return;
        if (MC.hitResult instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le && le.isAlive()
                && MC.player.getAttackStrengthScale(0f) >= 0.95f) {
            Util.attack(le);
        }
    }

    private static void autoClick() {
        if (!Config.autoClicker || stage != Stage.IDLE || !MC.options.keyAttack.isDown()) { clickAccumulator = 0; return; }
        var hit = MC.hitResult;
        boolean entity = hit instanceof EntityHitResult;
        if (Config.clickerOnlyEntities && !entity) return;
        clickAccumulator += Config.cps / 20.0;
        if (clickAccumulator < 1) return;
        if (Config.clickerRespectCooldown && MC.player.getAttackStrengthScale(0f) < 0.95f) return;
        clickAccumulator -= 1;
        if (entity) Util.attack(((EntityHitResult) hit).getEntity());
    }

    private static void elytraFirework() {
        if (fireworkCooldown > 0) fireworkCooldown--;
        if (!Config.elytraAutoFirework || !MC.player.isFallFlying() || fireworkCooldown > 0) return;
        if (MC.player.getDeltaMovement().length() >= Config.fireworkMinSpeed) return;
        int fw = Util.findHotbar(s -> s.is(Items.FIREWORK_ROCKET));
        if (fw < 0) return;
        int old = MC.player.getInventory().getSelectedSlot();
        Util.select(fw);
        MC.gameMode.useItem(MC.player, InteractionHand.MAIN_HAND);
        Util.select(old);
        fireworkCooldown = 20;
    }

    private static void swapElytra() {
        var inv = MC.player.getInventory();
        boolean wearing = MC.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(Items.ELYTRA);
        for (int i = 0; i < inv.getContainerSize(); i++) {
            var s = inv.getItem(i);
            boolean match = wearing
                    ? net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().endsWith("_chestplate")
                    : s.is(Items.ELYTRA);
            if (!match) continue;
            int slot = i < 9 ? 36 + i : i;
            Compat.pickupClick(slot);
            Compat.pickupClick(6);   // 6 = Brustplatten-Slot
            Compat.pickupClick(slot);
            return;
        }
    }
}

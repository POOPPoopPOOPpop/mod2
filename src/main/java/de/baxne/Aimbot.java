package de.baxne;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Zielt automatisch auf das nächste passende Ziel (für Spaß auf privaten Servern). */
public final class Aimbot {
    private static final Minecraft MC = Minecraft.getInstance();
    private Aimbot() {}

    public static void tick() {
        if (!Config.aimbot || MC.player == null || MC.level == null) return;
        if (Config.aimOnlyWhileAttack && !MC.options.keyAttack.isDown()) return;

        Entity target = findNearest();
        if (target == null) return;

        Vec3 eye = MC.player.getEyePosition();
        Vec3 point = Config.aimAtHead && target instanceof LivingEntity le
                ? le.getEyePosition()
                : target.getBoundingBox().getCenter();
        double dx = point.x - eye.x, dy = point.y - eye.y, dz = point.z - eye.z;
        float wantYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float wantPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));

        float dYaw = Mth.wrapDegrees(wantYaw - MC.player.getYRot());
        float dPitch = wantPitch - MC.player.getXRot();

        // FOV-Begrenzung (360 = rundherum)
        if (Config.aimFov < 360 && Math.abs(dYaw) > Config.aimFov / 2.0) return;

        float smooth = (float) Math.max(1.0, Config.aimSmooth);
        MC.player.setYRot(MC.player.getYRot() + dYaw / smooth);
        MC.player.setXRot(Mth.clamp(MC.player.getXRot() + dPitch / smooth, -90f, 90f));
    }

    /** Nächstes Ziel nach Entfernung, gefiltert nach den Menü-Einstellungen. */
    public static Entity findNearest() {
        Entity best = null;
        double bestDist = Config.aimRange * Config.aimRange;
        for (Entity e : MC.level.entitiesForRendering()) {
            if (e == MC.player || !e.isAlive() || e.isSpectator()) continue;
            if (!allowed(e)) continue;
            double d = MC.player.distanceToSqr(e);
            if (d > bestDist) continue;
            if (Config.aimVisibleOnly && e instanceof LivingEntity le && !MC.player.hasLineOfSight(le)) continue;
            best = e;
            bestDist = d;
        }
        return best;
    }

    private static boolean allowed(Entity e) {
        if (e instanceof Player) return Config.aimPlayers;
        if (e instanceof Enemy) return Config.aimMobs;
        if (e instanceof Animal) return Config.aimAnimals;
        if (e instanceof LivingEntity) return Config.aimOthers;      // Dorfbewohner, Fische, ...
        return Config.aimAllEntities;                                 // Boote, Rüstungsständer, ...
    }
}

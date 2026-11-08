package de.baxne;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class BaxneClient implements ClientModInitializer {
    public static final String MOD_ID = "baxnes_crosshair";
    private static final KeyMapping.Category CAT =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

    public static KeyMapping MENU_KEY, SPEAR_KEY, STUN_KEY, ELYTRA_KEY, CLICKER_KEY, XRAY_KEY, AIM_KEY, PEARL_KEY, CLIP_KEY;
    public static KeyMapping[] ALL_KEYS;

    private static KeyMapping reg(String name, int key) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping("key.baxne." + name, key, CAT));
    }

    @Override
    public void onInitializeClient() {
        MENU_KEY    = reg("menu",    344);
        SPEAR_KEY   = reg("spear",   88);
        STUN_KEY    = reg("stun",    71);
        ELYTRA_KEY  = reg("elytra",  72);
        CLICKER_KEY = reg("clicker", 74);
        XRAY_KEY    = reg("xray",    86);
        LogoButton.register();
        AIM_KEY     = reg("aim",     75);
        PEARL_KEY   = reg("pearl",   67);
        CLIP_KEY    = reg("clip",    297);   // F8
        ALL_KEYS = new KeyMapping[]{MENU_KEY, SPEAR_KEY, STUN_KEY, ELYTRA_KEY, CLICKER_KEY, XRAY_KEY, AIM_KEY, PEARL_KEY, CLIP_KEY};

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (MENU_KEY.consumeClick()) Compat.setScreen(new MenuScreen());
            while (AIM_KEY.consumeClick()) Config.aimbot = !Config.aimbot;
            while (PEARL_KEY.consumeClick()) Modules.pearlCombo();
            while (CLIP_KEY.consumeClick()) Clips.saveClip(Config.clipSeconds);
            Clips.tick();
            Aimbot.tick();
            Modules.tick();
        });

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "shape_crosshair"), CrosshairHud::render);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "armor_hud"), ArmorHud::render);
    }
}

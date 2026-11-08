package de.baxne;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Logo-Button im ESC-Menü und in den Optionen (öffnet das Baxne's-Crosshair-Menü). */
public final class LogoButton {
    private static final Identifier SPRITE = Identifier.fromNamespaceAndPath("baxnes_crosshair", "logo");
    private LogoButton() {}

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            boolean pause = screen instanceof PauseScreen;
            if (!pause && !(screen instanceof OptionsScreen)) return;

            int x = w - 26, y = 6;
            if (pause) {
                // links neben den "Optionen..."-Button setzen
                String optionsLabel = Component.translatable("menu.options").getString();
                for (AbstractWidget b : Screens.getButtons(screen)) {
                    if (b.getMessage().getString().equals(optionsLabel)) { x = b.getX() - 24; y = b.getY(); break; }
                }
            }
            SpriteIconButton btn = SpriteIconButton.builder(Component.literal("Baxne's Crosshair"),
                    b -> Compat.setScreen(new MenuScreen(screen)), true)
                    .width(20)
                    .sprite(SPRITE, 16, 16)
                    .build();
            btn.setPosition(x, y);
            Screens.getButtons(screen).add(btn);
        });
    }
}

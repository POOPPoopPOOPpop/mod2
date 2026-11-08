package de.baxne;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Menü (Standard: RIGHT SHIFT) mit Kopf-Logo, Tabs, Slidern und Keybind-Editor. */
public class MenuScreen extends Screen {
    private static final Identifier LOGO = Identifier.fromNamespaceAndPath("baxnes_crosshair", "textures/gui/logo.png");
    private static final String[] TABS = {"Combat", "Spear", "Elytra", "Visuals", "Aim", "Move", "Clips", "Keys"};
    private static int tab = 0;
    private KeyMapping listening = null;
    private int x0, cy;
    private int seenRevision = -1;

    @Override
    public void tick() {
        super.tick();
        if (tab == 6 && seenRevision != Clips.revision) rebuild();
    }

    private final Screen parent;

    public MenuScreen() { this(null); }

    public MenuScreen(Screen parent) {
        super(Component.literal("Baxne's Crosshair"));
        this.parent = parent;
    }

    @Override
    public void onClose() {
        if (parent != null) Compat.setScreen(parent); else super.onClose();
    }

    private void K(int col, int row, String name, KeyMapping key) {
        addRenderableWidget(Button.builder(Component.literal(name + ": ").append(key.getTranslatedKeyMessage()),
                b -> { listening = key; b.setMessage(Component.literal("> Taste drücken <")); })
                .bounds(cx(col), ry(row), cw(col), 20).build());
    }

    private Component modeLabel() {
        return Component.literal("Lunge: " + (Config.spearMode == 0 ? "Jab (Linksklick)" : "Charge (Rechtsklick)"));
    }

    private void spearMode(int col, int row) {
        addRenderableWidget(Button.builder(modeLabel(), b -> { Config.spearMode = (Config.spearMode + 1) % 2; b.setMessage(modeLabel()); })
                .bounds(cx(col), ry(row), cw(col), 20).build());
    }

    // Spalten: 0 = links, 1 = rechts (je 148 breit), 2 = volle Breite
    private int cx(int col) { return col == 1 ? x0 + 152 : x0; }
    private int cw(int col) { return col == 2 ? 300 : 148; }
    private int ry(int row) { return cy + row * 24; }

    @Override
    protected void init() {
        x0 = width / 2 - 150;
        int top = height / 2 - 150;
        cy = top + 94;
        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            int k = i % 4, r = i / 4;
            Button b = Button.builder(Component.literal((i == tab ? "§e" : "") + TABS[i]), bt -> { tab = idx; listening = null; rebuild(); })
                    .bounds(x0 + k * 76, top + 44 + r * 22, 72, 20).build();
            addRenderableWidget(b);
        }
        switch (tab) {
            case 0 -> {
                T(0, 0, "Auto Stun Slam", () -> Config.autoStunSlam, v -> Config.autoStunSlam = v);
                T(1, 0, "Auto Mace Swap", () -> Config.autoMaceSwap, v -> Config.autoMaceSwap = v);
                T(0, 1, "Immer Breach", () -> Config.maceForceBreach, v -> Config.maceForceBreach = v);
                T(1, 1, "Autoclicker", () -> Config.autoClicker, v -> Config.autoClicker = v);
                T(0, 2, "Triggerbot", () -> Config.triggerbot, v -> Config.triggerbot = v);
                T(1, 2, "Cooldown beachten", () -> Config.clickerRespectCooldown, v -> Config.clickerRespectCooldown = v);
                S(0, 3, "Stun Delay", 0, 10, Config.stunDelay, v -> Config.stunDelay = (int) Math.round(v));
                S(1, 3, "Reichweite", 3, 6, Config.stunRange, v -> Config.stunRange = v);
                S(0, 4, "CPS", 1, 20, Config.cps, v -> Config.cps = v);
                T(0, 5, "Auto Totem", () -> Config.autoTotem, v -> Config.autoTotem = v);
                S(1, 5, "Totem unter HP", 2, 20, Config.totemHp, v -> Config.totemHp = v);
                K(0, 6, "Stun Taste", BaxneClient.STUN_KEY);
                K(1, 6, "Autoclicker Taste", BaxneClient.CLICKER_KEY);
            }
            case 1 -> {
                T(0, 0, "Spear Swap + Lunge", () -> Config.spearSwap, v -> Config.spearSwap = v);
                K(1, 0, "Taste", BaxneClient.SPEAR_KEY);
                spearMode(2, 1);
                S(0, 2, "Spear Slot (0=Auto)", 0, 9, Config.spearSlot, v -> Config.spearSlot = (int) Math.round(v));
                S(1, 2, "Lunge Delay", 0, 10, Config.spearLungeDelay, v -> Config.spearLungeDelay = (int) Math.round(v));
                S(2, 3, "Zurück zur letzten Waffe nach (Ticks)", 0, 30, Config.spearBackDelay, v -> Config.spearBackDelay = (int) Math.round(v));
            }
            case 2 -> {
                T(2, 0, "Elytra Swap", () -> Config.elytraSwap, v -> Config.elytraSwap = v);
                T(2, 1, "Auto Firework", () -> Config.elytraAutoFirework, v -> Config.elytraAutoFirework = v);
                S(2, 2, "Firework unter Speed", 0.2, 2.0, Config.fireworkMinSpeed, v -> Config.fireworkMinSpeed = v);
                K(2, 3, "Elytra Taste", BaxneClient.ELYTRA_KEY);
            }
            case 3 -> {
                T(0, 0, "Xray", () -> Config.xray, v -> { Config.xray = v; Compat.reloadChunks(); });
                B(1, 0, "Xray Blöcke wählen...", () -> Compat.setScreen(new XrayScreen(this)));
                T(0, 1, "Crosshair-Formen", () -> Config.crosshairShapes, v -> Config.crosshairShapes = v);
                T(1, 1, "Armor HUD", () -> Config.armorHud, v -> Config.armorHud = v);
                shape(0, 2, "Schild", true);
                shape(1, 2, "Kein Schild", false);
                S(0, 3, "Crosshair Größe", 2, 16, Config.crosshairSize, v -> Config.crosshairSize = (int) Math.round(v));
                T(1, 3, "HUD: Hand zeigen", () -> Config.hudShowHand, v -> Config.hudShowHand = v);
                S(0, 4, "HUD X %", 0, 100, Config.hudX, v -> Config.hudX = (int) Math.round(v));
                S(1, 4, "HUD Y %", 0, 100, Config.hudY, v -> Config.hudY = (int) Math.round(v));
                K(0, 5, "Xray Taste", BaxneClient.XRAY_KEY);
            }
            case 4 -> {
                T(0, 0, "Aimbot", () -> Config.aimbot, v -> Config.aimbot = v);
                T(1, 0, "Nur beim Angreifen", () -> Config.aimOnlyWhileAttack, v -> Config.aimOnlyWhileAttack = v);
                T(0, 1, "Spieler", () -> Config.aimPlayers, v -> Config.aimPlayers = v);
                T(1, 1, "Monster", () -> Config.aimMobs, v -> Config.aimMobs = v);
                T(0, 2, "Tiere", () -> Config.aimAnimals, v -> Config.aimAnimals = v);
                T(1, 2, "Andere Lebewesen", () -> Config.aimOthers, v -> Config.aimOthers = v);
                T(0, 3, "Alle Entities", () -> Config.aimAllEntities, v -> Config.aimAllEntities = v);
                T(1, 3, "Ziel: Kopf", () -> Config.aimAtHead, v -> Config.aimAtHead = v);
                T(0, 4, "Nur sichtbare", () -> Config.aimVisibleOnly, v -> Config.aimVisibleOnly = v);
                S(1, 4, "Reichweite", 3, 64, Config.aimRange, v -> Config.aimRange = v);
                S(0, 5, "Smoothing", 1, 20, Config.aimSmooth, v -> Config.aimSmooth = v);
                S(1, 5, "FOV", 10, 360, Config.aimFov, v -> Config.aimFov = v);
                K(0, 6, "Aimbot Taste", BaxneClient.AIM_KEY);
            }
            case 5 -> {
                T(0, 0, "Windcharge-Jump", () -> Config.windJump, v -> Config.windJump = v);
                S(1, 0, "Wind Delay", 0, 10, Config.windDelay, v -> Config.windDelay = (int) Math.round(v));
                T(0, 1, "Pearl -> Spear (X)", () -> Config.pearlThenSpear, v -> Config.pearlThenSpear = v);
                T(1, 1, "Pearl nach oben", () -> Config.pearlUp, v -> Config.pearlUp = v);
                S(0, 2, "Spear Delay", 0, 10, Config.pearlSpearDelay, v -> Config.pearlSpearDelay = (int) Math.round(v));
                S(1, 2, "Zurück Delay", 0, 20, Config.pearlBackDelay, v -> Config.pearlBackDelay = (int) Math.round(v));
                T(0, 3, "Auto Sprint", () -> Config.autoSprint, v -> Config.autoSprint = v);
                K(0, 4, "Pearl Taste", BaxneClient.PEARL_KEY);
            }
            case 6 -> {
                B(2, 0, "CLIP SPEICHERN (letzte Sekunden aus dem Puffer)", () -> Clips.saveClip(Config.clipSeconds));
                T(0, 1, "Aufnahme", () -> Config.clipRecording, v -> Config.clipRecording = v);
                K(1, 1, "Clip-Taste", BaxneClient.CLIP_KEY);
                S(0, 2, "Clip-Länge (Sek.)", 5, 120, Config.clipSeconds, v -> Config.clipSeconds = (int) Math.round(v));
                S(1, 2, "Puffer (Sek.)", 10, 120, Config.clipBufferSeconds, v -> Config.clipBufferSeconds = (int) Math.round(v));
                S(0, 3, "FPS", 4, 20, Config.clipFps, v -> Config.clipFps = (int) Math.round(v));
                S(1, 3, "Auflösung %", 25, 100, Config.clipScale, v -> Config.clipScale = (int) Math.round(v));
                S(0, 4, "Qualität %", 30, 95, Config.clipQuality, v -> Config.clipQuality = (int) Math.round(v));
                B(1, 4, "Clip-Ordner öffnen", Clips::openFolder);
                seenRevision = Clips.revision;
                java.util.List<java.nio.file.Path> clips = Clips.list(3);
                for (int i = 0; i < clips.size(); i++) {
                    final java.nio.file.Path p = clips.get(i);
                    addRenderableWidget(Button.builder(Component.literal("> " + p.getFileName()), b -> Clips.open(p))
                            .bounds(x0, ry(5 + i), 220, 20).build());
                    addRenderableWidget(Button.builder(Component.literal("Löschen"), b -> { Clips.delete(p); rebuild(); })
                            .bounds(x0 + 224, ry(5 + i), 76, 20).build());
                }
            }
            case 7 -> {
                for (int i = 0; i < BaxneClient.ALL_KEYS.length; i++) {
                    final KeyMapping key = BaxneClient.ALL_KEYS[i];
                    addRenderableWidget(Button.builder(keyLabel(key), b -> { listening = key; b.setMessage(Component.literal("> Taste drücken <")); })
                            .bounds(cx(i % 2), ry(i / 2), 148, 20).build());
                }
            }
        }
    }

    private Component keyLabel(KeyMapping k) {
        return Component.translatable(k.getName()).append(": ").append(k.getTranslatedKeyMessage());
    }

    /** Neue Taste für das wartende Keybind (ESC = abbrechen, ENTF = unbelegt). */
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (listening != null) {
            if (event.key() == 256) {
            } else if (event.key() == 261) {
                listening.setKey(InputConstants.UNKNOWN);
            } else {
                listening.setKey(InputConstants.getKey(event));
            }
            KeyMapping.resetMapping();
            minecraft.options.save();
            listening = null;
            rebuild();
            return true;
        }
        return super.keyPressed(event);
    }

    private void rebuild() { clearWidgets(); init(); }

    private void B(int col, int row, String name, Runnable r) {
        addRenderableWidget(Button.builder(Component.literal(name), b -> r.run()).bounds(cx(col), ry(row), cw(col), 20).build());
    }

    private void shape(int col, int row, String name, boolean shield) {
        addRenderableWidget(Button.builder(shapeLabel(name, shield), b -> {
            if (shield) Config.shapeShield = (Config.shapeShield + 1) % Shapes.NAMES.length;
            else Config.shapeEnemy = (Config.shapeEnemy + 1) % Shapes.NAMES.length;
            b.setMessage(shapeLabel(name, shield));
        }).bounds(cx(col), ry(row), cw(col), 20).build());
    }

    private Component shapeLabel(String n, boolean shield) {
        return Component.literal(n + ": " + Shapes.NAMES[shield ? Config.shapeShield : Config.shapeEnemy]);
    }

    private void T(int col, int row, String name, BooleanSupplier get, Consumer<Boolean> set) {
        addRenderableWidget(Button.builder(label(name, get.getAsBoolean()), b -> {
            set.accept(!get.getAsBoolean());
            b.setMessage(label(name, get.getAsBoolean()));
        }).bounds(cx(col), ry(row), cw(col), 20).build());
    }

    private Component label(String n, boolean on) { return Component.literal(n + ": " + (on ? "§aAN" : "§cAUS")); }

    private void S(int col, int row, String name, double min, double max, double cur, Consumer<Double> set) {
        addRenderableWidget(new AbstractSliderButton(cx(col), ry(row), cw(col), 20,
                Component.literal(name + ": " + fmt(cur)), (cur - min) / (max - min)) {
            @Override protected void updateMessage() { setMessage(Component.literal(name + ": " + fmt(min + value * (max - min)))); }
            @Override protected void applyValue() { set.accept(min + value * (max - min)); }
        });
    }

    private static String fmt(double d) { return d == Math.rint(d) ? String.valueOf((int) d) : String.format("%.1f", d); }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        g.fill(width / 2 - 160, height / 2 - 150, width / 2 + 160, height / 2 + 150, 0xCC101018);
        g.blit(RenderPipelines.GUI_TEXTURED, LOGO, width / 2 - 154, height / 2 - 146, 0, 0, 32, 32, 64, 64, 64, 64);
        g.text(font, "Baxne's Crosshair", width / 2 - 114, height / 2 - 134, 0xFFFFFFFF, true);
        if (tab == 6) g.text(font, Clips.statusLine(), width / 2 - 114, height / 2 - 120, 0xFFAAFFAA, true);
        super.extractRenderState(g, mx, my, delta);
    }

    @Override public boolean isPauseScreen() { return false; }
}

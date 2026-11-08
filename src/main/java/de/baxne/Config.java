package de.baxne;

import java.util.HashSet;
import java.util.Set;

/** Alle Einstellungen (im Menü änderbar). */
public class Config {
    // Module
    public static boolean spearSwap = true;
    public static boolean autoStunSlam = true;
    public static boolean autoMaceSwap = true;
    public static boolean xray = false;
    public static boolean crosshairShapes = true;
    public static boolean elytraSwap = true;
    public static boolean autoClicker = false;

    // Aimbot
    public static boolean aimbot = false;
    public static boolean aimPlayers = true;
    public static boolean aimMobs = true;
    public static boolean aimAnimals = true;
    public static boolean aimOthers = true;
    public static boolean aimAllEntities = false;
    public static boolean aimAtHead = true;
    public static boolean aimVisibleOnly = true;
    public static boolean aimOnlyWhileAttack = false;
    public static double aimRange = 12;
    public static double aimSmooth = 4;     // 1 = sofort, höher = weicher
    public static double aimFov = 360;

    // Combat extras
    public static boolean triggerbot = false;
    public static boolean autoTotem = false;
    public static double totemHp = 8;          // Lebenspunkte (2 = 1 Herz)

    // Movement
    public static boolean autoSprint = false;
    public static boolean windJump = true;     // Sprint + Sprung -> Windcharge unter dir
    public static int windDelay = 3;           // Ticks nach dem Sprung
    public static boolean pearlThenSpear = true;
    public static boolean pearlUp = true;      // Pearl senkrecht nach oben (sonst nach unten)
    public static int pearlSpearDelay = 3;
    public static int pearlBackDelay = 8;

    // Armor-HUD (Uku-Stil)
    public static boolean armorHud = true;
    public static boolean hudShowHand = true;
    public static int hudX = 70;               // Prozent der Bildschirmbreite
    public static int hudY = 88;               // Prozent der Bildschirmhöhe

    // Clips (Replay-Puffer)
    public static boolean clipRecording = true;
    public static int clipSeconds = 20;        // so viele Sekunden werden pro Clip gespeichert
    public static int clipBufferSeconds = 30;  // so viele Sekunden hält der Puffer im RAM
    public static int clipFps = 10;            // 4-20
    public static int clipScale = 50;          // Auflösung in Prozent
    public static int clipQuality = 65;        // JPEG-Qualität in Prozent

    // Spear
    public static int spearSlot = 0;          // 0 = automatisch suchen, 1-9 = fester Slot
    public static int spearLungeDelay = 2;
    public static int spearBackDelay = 8;     // Ticks nach dem Lunge, dann zurück zur letzten Waffe/zum letzten Item
    public static int spearMode = 0;          // 0 = Jab (Linksklick-Lunge), 1 = Rechtsklick (Charge)

    // Elytra
    public static boolean elytraAutoFirework = false;
    public static double fireworkMinSpeed = 0.8;   // Feuerwerk, wenn langsamer als das

    // Stun Slam
    public static int stunDelay = 3;
    public static double stunRange = 4.5;

    // Mace
    public static boolean maceForceBreach = false;   // false = automatisch Breach/Density
    public static double minFallForSlam = 1.5;

    // Autoclicker
    public static double cps = 10;
    public static boolean clickerRespectCooldown = true;
    public static boolean clickerOnlyEntities = true;

    // Crosshair-Formen (Index in Shapes.NAMES)
    public static int shapeShield = 2;   // Gegner blockt  -> X
    public static int shapeEnemy = 4;    // Gegner ohne Schild -> Raute
    public static int crosshairSize = 7;
    public static int crosshairColor = 0xFFFFFFFF;

    // Xray
    public static final Set<String> xrayBlocks = new HashSet<>(Set.of(
        "diamond_ore", "deepslate_diamond_ore", "ancient_debris", "emerald_ore",
        "deepslate_emerald_ore", "gold_ore", "deepslate_gold_ore", "iron_ore",
        "deepslate_iron_ore", "redstone_ore", "deepslate_redstone_ore",
        "lapis_ore", "deepslate_lapis_ore", "coal_ore", "deepslate_coal_ore",
        "copper_ore", "deepslate_copper_ore", "nether_gold_ore", "nether_quartz_ore",
        "chest", "ender_chest", "spawner", "lava", "water"));
}

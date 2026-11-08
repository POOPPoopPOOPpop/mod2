package de.baxne;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Versions-Brücke: Minecraft 26.x hat viele Methoden verschoben/umbenannt.
 * Hier werden sie zur Laufzeit per Name gesucht, damit die Mod trotzdem kompiliert und läuft.
 */
public final class Compat {
    private static final Minecraft MC = Minecraft.getInstance();
    private Compat() {}

    // ---------- Reflection-Helfer ----------
    private static Field findField(Class<?> c, String n) {
        for (; c != null; c = c.getSuperclass()) {
            try { Field f = c.getDeclaredField(n); f.setAccessible(true); return f; } catch (Throwable ignored) {}
        }
        return null;
    }

    private static Method findMethod(Class<?> c, String n, int params) {
        for (; c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods())
                if (m.getName().equals(n) && m.getParameterCount() == params) { m.setAccessible(true); return m; }
        }
        return null;
    }

    /** Minecraft selbst + alle Client-Objekte, die es hält (gui, options, levelRenderer, ...). */
    private static List<Object> roots() {
        List<Object> out = new ArrayList<>();
        out.add(MC);
        for (Class<?> c = MC.getClass(); c != null; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive()) continue;
                try {
                    f.setAccessible(true);
                    Object v = f.get(MC);
                    if (v != null && v.getClass().getName().startsWith("net.minecraft.client")) out.add(v);
                } catch (Throwable ignored) {}
            }
        }
        return out;
    }

    // ---------- Screens ----------
    public static Screen currentScreen() {
        for (Object o : roots()) {
            Field f = findField(o.getClass(), "screen");
            if (f != null && Screen.class.isAssignableFrom(f.getType())) {
                try { return (Screen) f.get(o); } catch (Throwable ignored) {}
            }
            for (String n : new String[]{"screen", "getScreen"}) {
                Method m = findMethod(o.getClass(), n, 0);
                if (m != null && Screen.class.isAssignableFrom(m.getReturnType())) {
                    try { return (Screen) m.invoke(o); } catch (Throwable ignored) {}
                }
            }
        }
        return null;
    }

    public static void setScreen(Screen s) {
        for (Object o : roots()) {
            for (Method m : o.getClass().getMethods()) {
                if (m.getName().equals("setScreen") && m.getParameterCount() == 1
                        && m.getParameterTypes()[0].isAssignableFrom(s.getClass())) {
                    try { m.invoke(o, s); return; } catch (Throwable ignored) {}
                }
            }
        }
        System.err.println("[Baxne] setScreen nicht gefunden");
    }

    // ---------- HUD versteckt (F1) ----------
    public static boolean hideGui() {
        for (Object o : roots()) {
            Field f = findField(o.getClass(), "hideGui");
            if (f != null && f.getType() == boolean.class) {
                try { return f.getBoolean(o); } catch (Throwable ignored) {}
            }
            for (String n : new String[]{"hideGui", "isHideGui", "isHidden"}) {
                Method m = findMethod(o.getClass(), n, 0);
                if (m != null && m.getReturnType() == boolean.class) {
                    try { return (boolean) m.invoke(o); } catch (Throwable ignored) {}
                }
            }
        }
        return false;
    }

    // ---------- Chunks neu laden (Xray) ----------
    public static void reloadChunks() {
        Object lr = MC.levelRenderer;
        if (lr == null) return;
        for (String n : new String[]{"allChanged", "needsUpdate", "setAllDirty", "markAllDirty"}) {
            Method m = findMethod(lr.getClass(), n, 0);
            if (m != null) { try { m.invoke(lr); return; } catch (Throwable ignored) {} }
        }
        System.err.println("[Baxne] Chunk-Reload nicht gefunden (Xray: F3+A druecken)");
    }

    // ---------- Linksklick simulieren (Spear-Jab / Lunge) ----------
    public static boolean clickAttack() {
        try {
            Field f = findField(net.minecraft.client.KeyMapping.class, "key");
            if (f == null) return false;
            Object key = f.get(MC.options.keyAttack);
            if (key == null) return false;
            for (Method m : net.minecraft.client.KeyMapping.class.getDeclaredMethods()) {
                if (m.getName().equals("click") && Modifier.isStatic(m.getModifiers()) && m.getParameterCount() == 1
                        && m.getParameterTypes()[0].isInstance(key)) {
                    m.setAccessible(true);
                    m.invoke(null, key);
                    return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    // ---------- Item-Icon im HUD ----------
    private static Method itemM;
    private static boolean itemSearched;

    public static void drawItem(Object g, net.minecraft.world.item.ItemStack s, int x, int y) {
        if (!itemSearched) {
            itemSearched = true;
            search:
            for (String n : new String[]{"item", "renderItem", "renderFakeItem"}) {
                for (Method m : g.getClass().getMethods()) {
                    Class<?>[] p = m.getParameterTypes();
                    if (m.getName().equals(n) && p.length == 3 && p[0] == net.minecraft.world.item.ItemStack.class
                            && p[1] == int.class && p[2] == int.class) { itemM = m; break search; }
                }
            }
            if (itemM == null) System.err.println("[Baxne] Item-Icon-Methode nicht gefunden, HUD zeigt nur Text");
        }
        if (itemM != null) { try { itemM.invoke(g, s, x, y); } catch (Throwable ignored) {} }
    }

    // ---------- Inventar-Klick (Elytra-Swap) ----------
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void pickupClick(int slot) {
        try {
            Class<?> click = null;
            for (String cn : new String[]{"net.minecraft.world.inventory.ClickType", "net.minecraft.world.inventory.ContainerInput"}) {
                try { click = Class.forName(cn); break; } catch (Throwable ignored) {}
            }
            if (click == null) { System.err.println("[Baxne] ClickType nicht gefunden"); return; }
            Object pickup = Enum.valueOf((Class<Enum>) click, "PICKUP");
            for (Method m : MC.gameMode.getClass().getMethods()) {
                if (m.getParameterCount() == 5 && (m.getName().equals("handleInventoryMouseClick")
                        || m.getName().equals("handleContainerInput") || m.getName().equals("handleContainerClick"))) {
                    m.invoke(MC.gameMode, MC.player.inventoryMenu.containerId, slot, 0, pickup, MC.player);
                    return;
                }
            }
            System.err.println("[Baxne] Container-Klick-Methode nicht gefunden");
        } catch (Throwable t) { t.printStackTrace(); }
    }
}

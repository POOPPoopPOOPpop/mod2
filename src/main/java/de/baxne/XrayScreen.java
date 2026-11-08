package de.baxne;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Xray-Blockauswahl: alle Blöcke des Spiels, mit Suche und Seiten. */
public class XrayScreen extends Screen {
    private static final int ROWS = 8;
    private final Screen parent;
    private final List<String> all = new ArrayList<>();
    private List<String> filtered = new ArrayList<>();
    private final List<Button> rowButtons = new ArrayList<>();
    private String query = "";
    private int page = 0;

    public XrayScreen(Screen parent) {
        super(Component.literal("Xray Blöcke"));
        this.parent = parent;
        BuiltInRegistries.BLOCK.keySet().forEach(id -> all.add(id.getPath()));
        all.sort(String::compareTo);
        applyFilter();
    }

    private void applyFilter() {
        filtered = new ArrayList<>();
        String q = query.toLowerCase(Locale.ROOT).replace(' ', '_');
        for (String s : all) if (q.isEmpty() || s.contains(q)) filtered.add(s);
        int maxPage = Math.max(0, (filtered.size() - 1) / ROWS);
        if (page > maxPage) page = maxPage;
    }

    @Override
    protected void init() {
        int x = width / 2 - 150, top = height / 2 - 125;
        EditBox box = new EditBox(font, x, top, 300, 20, Component.literal("Suche"));
        box.setHint(Component.literal("Block suchen..."));
        box.setValue(query);
        box.setResponder(s -> { query = s; page = 0; applyFilter(); buildRows(); });
        addRenderableWidget(box);

        addRenderableWidget(Button.builder(Component.literal("< Zurück"), b -> { if (page > 0) { page--; buildRows(); } })
                .bounds(x, top + 24 + ROWS * 22 + 4, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Weiter >"), b -> {
            if ((page + 1) * ROWS < filtered.size()) { page++; buildRows(); } })
                .bounds(x + 74, top + 24 + ROWS * 22 + 4, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Alle Erze"), b -> {
            for (String s : all) if (s.endsWith("_ore") || s.equals("ancient_debris")) Config.xrayBlocks.add(s);
            buildRows(); }).bounds(x + 148, top + 24 + ROWS * 22 + 4, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Alle aus"), b -> { Config.xrayBlocks.clear(); buildRows(); })
                .bounds(x + 222, top + 24 + ROWS * 22 + 4, 78, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Fertig"), b -> onClose())
                .bounds(x, top + 24 + ROWS * 22 + 28, 300, 20).build());
        buildRows();
    }

    private void buildRows() {
        for (Button b : rowButtons) removeWidget(b);
        rowButtons.clear();
        int x = width / 2 - 150, top = height / 2 - 125;
        for (int i = 0; i < ROWS; i++) {
            int idx = page * ROWS + i;
            if (idx >= filtered.size()) break;
            final String name = filtered.get(idx);
            Button b = Button.builder(label(name), btn -> {
                if (!Config.xrayBlocks.remove(name)) Config.xrayBlocks.add(name);
                btn.setMessage(label(name));
            }).bounds(x, top + 24 + i * 22, 300, 20).build();
            rowButtons.add(b);
            addRenderableWidget(b);
        }
    }

    private Component label(String name) {
        boolean on = Config.xrayBlocks.contains(name);
        return Component.literal((on ? "§a[X] §f" : "§7[  ] §7") + name.replace('_', ' '));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        g.fill(width / 2 - 160, height / 2 - 150, width / 2 + 160, height / 2 + 125 + 52, 0xCC101018);
        super.extractRenderState(g, mx, my, delta);
        int pages = Math.max(1, (filtered.size() + ROWS - 1) / ROWS);
        g.text(font, "Seite " + (page + 1) + "/" + pages + "  -  " + filtered.size() + " Blöcke, "
                + Config.xrayBlocks.size() + " aktiv", width / 2 - 150, height / 2 - 143, 0xFFAAAAAA, true);
    }

    @Override
    public void onClose() {
        Compat.reloadChunks();
        Compat.setScreen(parent);
    }

    @Override public boolean isPauseScreen() { return false; }
}

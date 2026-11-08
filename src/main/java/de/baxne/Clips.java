package de.baxne;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Clip-System (Replay-Puffer): nimmt dauerhaft die letzten Sekunden als JPEG-Frames im RAM auf.
 * Im Menü (Tab "Clips") oder per Taste speicherst du die letzten N Sekunden als MJPEG-AVI (ohne Ton).
 */
public final class Clips {
    private static final Minecraft MC = Minecraft.getInstance();
    private Clips() {}

    private record Frame(byte[] jpeg, int w, int h, int fps) {}

    private static final ArrayDeque<Frame> BUF = new ArrayDeque<>();
    private static long bufBytes = 0;
    private static final long MAX_BYTES = 256L * 1024 * 1024;

    private static final ExecutorService ENC = Executors.newSingleThreadExecutor(r -> daemon(r, "baxne-enc"));
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> daemon(r, "baxne-io"));
    private static Thread daemon(Runnable r, String n) { Thread t = new Thread(r, n); t.setDaemon(true); return t; }

    private static volatile boolean pending = false;
    private static volatile long pendingSince = 0;
    private static int tickCount = 0;

    private static volatile String status = "";
    private static volatile long statusTime = 0;
    public static volatile int revision = 0;

    // ---------- Status ----------
    private static void setStatus(String s) { status = s; statusTime = System.currentTimeMillis(); }

    public static String statusLine() {
        if (!status.isEmpty() && System.currentTimeMillis() - statusTime < 8000) return status;
        int n; long bytes;
        synchronized (BUF) { n = BUF.size(); bytes = bufBytes; }
        int fps = Math.max(1, Math.round(20f / Math.max(1, Math.round(20f / Math.max(1, Config.clipFps)))));
        return (Config.clipRecording ? "Aufnahme läuft" : "Aufnahme aus") + " | Puffer: " + (n / fps) + " Sek., "
                + (bytes / (1024 * 1024)) + " MB";
    }

    // ---------- Aufnahme ----------
    public static void tick() {
        if (!Config.clipRecording || MC.level == null || MC.player == null) return;
        if (Compat.currentScreen() != null) return;
        int interval = Math.max(1, Math.round(20f / Math.max(1, Config.clipFps)));
        if (++tickCount < interval) return;
        tickCount = 0;
        if (pending) {
            if (System.currentTimeMillis() - pendingSince > 2000) pending = false; else return;
        }
        pending = true;
        pendingSince = System.currentTimeMillis();
        final int fps = Math.max(1, 20 / interval);
        try {
            if (!shoot(img -> process(img, fps))) {
                pending = false;
                Config.clipRecording = false;
                setStatus("Screenshot-API in 26.3 nicht gefunden - Aufnahme aus");
            }
        } catch (Throwable t) {
            pending = false;
            t.printStackTrace();
        }
    }

    // --- Reflection auf die Screenshot-Funktion von Minecraft ---
    private static boolean shoot(Consumer<Object> cb) throws Exception {
        Class<?> sc;
        try { sc = Class.forName("net.minecraft.client.Screenshot"); } catch (Throwable t) { return false; }
        Object target = MC.getClass().getMethod("getMainRenderTarget").invoke(MC);
        for (Method m : sc.getDeclaredMethods()) {
            if (!m.getName().equals("takeScreenshot") || !Modifier.isStatic(m.getModifiers())) continue;
            Class<?>[] p = m.getParameterTypes();
            m.setAccessible(true);
            if (p.length == 2 && p[0].isInstance(target) && Consumer.class.isAssignableFrom(p[1])) { m.invoke(null, target, cb); return true; }
            if (p.length == 3 && p[0].isInstance(target) && p[1] == int.class && Consumer.class.isAssignableFrom(p[2])) { m.invoke(null, target, 1, cb); return true; }
            if (p.length == 1 && p[0].isInstance(target) && !Void.TYPE.equals(m.getReturnType())) { cb.accept(m.invoke(null, target)); return true; }
        }
        return false;
    }

    private static Method mW, mH, mPx, mGet, mClose;
    private static boolean mInit;

    private static void initMethods(Class<?> c) {
        if (mInit) return;
        mInit = true;
        try { mW = c.getMethod("getWidth"); mH = c.getMethod("getHeight"); } catch (Throwable ignored) {}
        for (String n : new String[]{"getPixelsABGR", "getPixelsRGBA", "getPixels"}) {
            try { Method m = c.getMethod(n); if (m.getReturnType() == int[].class) { mPx = m; break; } } catch (Throwable ignored) {}
        }
        try { mGet = c.getMethod("getPixel", int.class, int.class); } catch (Throwable ignored) {}
        try { mClose = c.getMethod("close"); } catch (Throwable ignored) {}
    }

    private static void process(Object img, int fps) {
        try {
            initMethods(img.getClass());
            int w = (int) mW.invoke(img), h = (int) mH.invoke(img);
            double sc = Config.clipScale / 100.0;
            int ow = Math.max(2, (int) (w * sc)) & ~1, oh = Math.max(2, (int) (h * sc)) & ~1;
            int[] px = mPx != null ? (int[]) mPx.invoke(img) : null;
            int[] out = new int[ow * oh];
            for (int y = 0; y < oh; y++) {
                int sy = y * h / oh;
                for (int x = 0; x < ow; x++) {
                    int sx = x * w / ow;
                    int p = px != null ? px[sy * w + sx] : (int) mGet.invoke(img, sx, sy);   // ABGR
                    out[y * ow + x] = ((p & 0xFF) << 16) | (p & 0xFF00) | ((p >> 16) & 0xFF);
                }
            }
            try { if (mClose != null) mClose.invoke(img); } catch (Throwable ignored) {}
            final int fw = ow, fh = oh;
            ENC.submit(() -> encode(out, fw, fh, fps));
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            pending = false;
        }
    }

    private static void encode(int[] rgb, int w, int h, int fps) {
        try {
            BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            bi.setRGB(0, 0, w, h, rgb, 0, w);
            ImageWriter wr = ImageIO.getImageWritersByFormatName("jpg").next();
            ImageWriteParam prm = wr.getDefaultWriteParam();
            prm.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            prm.setCompressionQuality(Math.max(0.1f, Math.min(0.95f, Config.clipQuality / 100f)));
            ByteArrayOutputStream bos = new ByteArrayOutputStream(32768);
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(bos)) {
                wr.setOutput(ios);
                wr.write(null, new IIOImage(bi, null, null), prm);
            }
            wr.dispose();
            byte[] jpg = bos.toByteArray();
            synchronized (BUF) {
                BUF.addLast(new Frame(jpg, w, h, fps));
                bufBytes += jpg.length;
                int max = Math.max(fps * 2, Config.clipBufferSeconds * fps);
                while (BUF.size() > max || bufBytes > MAX_BYTES) bufBytes -= BUF.pollFirst().jpeg().length;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    // ---------- Speichern ----------
    public static Path folder() {
        Path dir = FabricLoader.getInstance().getGameDir().resolve("baxne_clips");
        try { Files.createDirectories(dir); } catch (IOException ignored) {}
        return dir;
    }

    public static void saveClip(int seconds) {
        List<Frame> snap;
        synchronized (BUF) { snap = new ArrayList<>(BUF); }
        if (snap.isEmpty()) { setStatus("Kein Clip im Puffer (Aufnahme an?)"); return; }
        Frame last = snap.get(snap.size() - 1);
        List<Frame> same = new ArrayList<>();
        for (Frame f : snap) if (f.w() == last.w() && f.h() == last.h() && f.fps() == last.fps()) same.add(f);
        int want = Math.max(1, seconds * last.fps());
        List<Frame> frames = same.subList(Math.max(0, same.size() - want), same.size());
        final List<Frame> out = new ArrayList<>(frames);
        final String name = "clip_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                + "_" + (out.size() / last.fps()) + "s.avi";
        setStatus("Speichere Clip...");
        IO.submit(() -> {
            try {
                writeAvi(folder().resolve(name), out, last.w(), last.h(), last.fps());
                revision++;
                setStatus("Gespeichert: " + name);
            } catch (Throwable t) {
                t.printStackTrace();
                setStatus("Fehler beim Speichern: " + t.getMessage());
            }
        });
    }

    private static void w4(ByteBuffer b, String s) { b.put(s.getBytes(StandardCharsets.US_ASCII)); }

    /** Schreibt einen MJPEG-AVI (spielt in VLC, Windows-Medienwiedergabe je nach Codec). */
    private static void writeAvi(Path file, List<Frame> frames, int w, int h, int fps) throws IOException {
        int n = frames.size();
        long moviPayload = 4;
        int maxChunk = 0;
        for (Frame f : frames) { int len = f.jpeg().length; moviPayload += 8 + len + (len & 1); maxChunk = Math.max(maxChunk, len); }
        long idxPayload = 16L * n;
        long riffSize = 4 + 200 + (8 + moviPayload) + (8 + idxPayload);

        ByteBuffer b = ByteBuffer.allocate(224).order(ByteOrder.LITTLE_ENDIAN);
        w4(b, "RIFF"); b.putInt((int) riffSize); w4(b, "AVI ");
        w4(b, "LIST"); b.putInt(192); w4(b, "hdrl");
        w4(b, "avih"); b.putInt(56);
        b.putInt(1_000_000 / fps); b.putInt(maxChunk * fps); b.putInt(0); b.putInt(0x10);
        b.putInt(n); b.putInt(0); b.putInt(1); b.putInt(maxChunk); b.putInt(w); b.putInt(h);
        b.putInt(0); b.putInt(0); b.putInt(0); b.putInt(0);
        w4(b, "LIST"); b.putInt(116); w4(b, "strl");
        w4(b, "strh"); b.putInt(56); w4(b, "vids"); w4(b, "MJPG");
        b.putInt(0); b.putShort((short) 0); b.putShort((short) 0); b.putInt(0);
        b.putInt(1); b.putInt(fps); b.putInt(0); b.putInt(n); b.putInt(maxChunk); b.putInt(-1); b.putInt(0);
        b.putShort((short) 0); b.putShort((short) 0); b.putShort((short) w); b.putShort((short) h);
        w4(b, "strf"); b.putInt(40); b.putInt(40); b.putInt(w); b.putInt(h);
        b.putShort((short) 1); b.putShort((short) 24); w4(b, "MJPG");
        b.putInt(w * h * 3); b.putInt(0); b.putInt(0); b.putInt(0); b.putInt(0);
        w4(b, "LIST"); b.putInt((int) moviPayload); w4(b, "movi");

        try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(file), 1 << 16)) {
            os.write(b.array(), 0, b.position());
            ByteBuffer hd = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
            for (Frame f : frames) {
                hd.clear(); w4(hd, "00dc"); hd.putInt(f.jpeg().length);
                os.write(hd.array(), 0, 8);
                os.write(f.jpeg());
                if ((f.jpeg().length & 1) == 1) os.write(0);
            }
            ByteBuffer ix = ByteBuffer.allocate(8 + 16 * n).order(ByteOrder.LITTLE_ENDIAN);
            w4(ix, "idx1"); ix.putInt(16 * n);
            int off = 4;
            for (Frame f : frames) {
                w4(ix, "00dc"); ix.putInt(0x10); ix.putInt(off); ix.putInt(f.jpeg().length);
                off += 8 + f.jpeg().length + (f.jpeg().length & 1);
            }
            os.write(ix.array(), 0, ix.position());
        }
    }

    // ---------- Clip-Liste / Öffnen ----------
    public static List<Path> list(int max) {
        try (Stream<Path> s = Files.list(folder())) {
            return s.filter(p -> p.getFileName().toString().endsWith(".avi"))
                    .sorted(Comparator.comparingLong((Path p) -> p.toFile().lastModified()).reversed())
                    .limit(max).toList();
        } catch (IOException e) { return List.of(); }
    }

    public static void delete(Path p) { try { Files.deleteIfExists(p); revision++; } catch (IOException ignored) {} }

    public static void open(Path p) {
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) new ProcessBuilder("cmd", "/c", "start", "", p.toString()).start();
            else if (os.contains("mac")) new ProcessBuilder("open", p.toString()).start();
            else new ProcessBuilder("xdg-open", p.toString()).start();
        } catch (IOException e) { setStatus("Konnte nicht öffnen: " + e.getMessage()); }
    }

    public static void openFolder() { open(folder()); }
}

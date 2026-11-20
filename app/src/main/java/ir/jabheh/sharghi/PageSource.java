package ir.jabheh.sharghi;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.LruCache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pages of the book. Logical page 0 = PDF page 2 (bismillah), logical page 1 = credits page
 * drawn natively, logical page k (k >= 2) = PDF page k+1. PDF page 1 (blank) is skipped.
 */
final class PageSource {
    interface Listener {
        void onPageReady();
    }

    private final Context ctx;
    private final Context uiCtx;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private final Object lock = new Object();
    private final LruCache<String, Bitmap> cache;
    private final Set<String> pending = Collections.synchronizedSet(new HashSet<String>());
    private PdfRenderer renderer;
    private int pdfCount;
    private volatile boolean ready;
    private Listener listener;

    PageSource(Context c) {
        ctx = c.getApplicationContext();
        uiCtx = c;
        int kb = (int) (Runtime.getRuntime().maxMemory() / 1024 / 5);
        cache = new LruCache<String, Bitmap>(Math.max(kb, 16384)) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount() / 1024;
            }
        };
    }

    void setListener(Listener l) {
        listener = l;
    }

    /** Copies the PDF from assets to private storage (first run only) and opens it. */
    boolean open(String assetName) {
        try {
            File f = new File(ctx.getFilesDir(), "book.pdf");
            SharedPreferences p = ctx.getSharedPreferences("jabheh", 0);
            if (!f.exists() || f.length() == 0 || !assetName.equals(p.getString("pdf_src", ""))) {
                File tmp = new File(ctx.getFilesDir(), "book.tmp");
                try (InputStream in = ctx.getAssets().open(assetName);
                     OutputStream out = new FileOutputStream(tmp)) {
                    byte[] buf = new byte[65536];
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        out.write(buf, 0, n);
                    }
                }
                if (f.exists()) {
                    f.delete();
                }
                if (!tmp.renameTo(f)) {
                    return false;
                }
                p.edit().putString("pdf_src", assetName).apply();
            }
            ParcelFileDescriptor pfd = ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
            renderer = new PdfRenderer(pfd);
            pdfCount = renderer.getPageCount();
            ready = pdfCount >= 3;
            return ready;
        } catch (Throwable t) {
            return false;
        }
    }

    boolean isReady() {
        return ready;
    }

    int count() {
        return ready ? pdfCount : 0;
    }

    /** Returns the cached bitmap, or null and starts rendering it in the background. */
    Bitmap get(int page, int w, int h) {
        if (!ready || w <= 0 || h <= 0 || page < 0 || page >= pdfCount) {
            return null;
        }
        final String key = page + "_" + w + "x" + h;
        Bitmap b = cache.get(key);
        if (b != null) {
            return b;
        }
        if (!pending.add(key)) {
            return null;
        }
        final int fp = page;
        final int fw = w;
        final int fh = h;
        exec.execute(new Runnable() {
            @Override
            public void run() {
                final Bitmap bm = render(fp, fw, fh);
                if (bm != null) {
                    cache.put(key, bm);
                }
                pending.remove(key);
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        if (listener != null) {
                            listener.onPageReady();
                        }
                    }
                });
            }
        });
        return null;
    }

    // The part of each PDF page that is shown (fractions of the page); margins are cut away.
    private static final float CX0 = 0.125f;
    private static final float CX1 = 0.885f;
    private static final float CY0 = 0.045f;
    private static final float CY1 = 0.915f;
    private static final float PAGE_RATIO = 396.84f / 595.32f;
    /** Width / height of the shown part of a page. */
    static final float CROP_RATIO = ((CX1 - CX0) * 396.84f) / ((CY1 - CY0) * 595.32f);
    private static final int GOLD_RGB = 0x00F2D07A;

    private Bitmap render(int page, int w, int h) {
        try {
            if (page == 1) {
                return CreditsPage.draw(uiCtx, w, h);
            }
            int idx = page == 0 ? 1 : page;
            Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(out);
            if (idx == 2 || idx == 3) {
                // title page and colophon are coloured full pages: show them whole
                int pwid = w;
                int phei = (int) (w / PAGE_RATIO);
                if (phei > h) {
                    phei = h;
                    pwid = (int) (h * PAGE_RATIO);
                }
                Bitmap full = renderPdf(idx, pwid, phei);
                c.drawBitmap(full, (w - pwid) / 2f, (h - phei) / 2f, null);
                return out;
            }
            int wf = Math.round(w / (CX1 - CX0));
            int hf = Math.round(wf / PAGE_RATIO);
            Bitmap full = renderPdf(idx, wf, hf);
            int x0 = Math.round(wf * CX0);
            int y0 = Math.round(hf * CY0);
            int cw = Math.min(w, wf - x0);
            int ch = Math.min(h, hf - y0);
            int[] px = new int[cw * ch];
            full.getPixels(px, 0, cw, x0, y0, cw, ch);
            full.recycle();
            recolor(px, cw, ch);
            out.setPixels(px, 0, cw, 0, 0, cw, ch);
            return out;
        } catch (Throwable t) {
            return null;
        }
    }

    private static int lum(int p) {
        return (((p >> 16) & 255) * 30 + ((p >> 8) & 255) * 59 + (p & 255) * 11) / 100;
    }

    /** Black text becomes gold on a transparent page; photo areas keep their own colours. */
    private static void recolor(int[] px, int w, int h) {
        final int bsz = 16;
        int gw = (w + bsz - 1) / bsz;
        int gh = (h + bsz - 1) / bsz;
        boolean[] dense = new boolean[gw * gh];
        for (int by = 0; by < gh; by++) {
            for (int bx = 0; bx < gw; bx++) {
                int x1 = Math.min(w, (bx + 1) * bsz);
                int y1 = Math.min(h, (by + 1) * bsz);
                long sum = 0;
                int cnt = 0;
                int ink = 0;
                for (int y = by * bsz; y < y1; y += 2) {
                    for (int x = bx * bsz; x < x1; x += 2) {
                        int l = lum(px[y * w + x]);
                        sum += l;
                        cnt++;
                        if (l < 235) {
                            ink++;
                        }
                    }
                }
                dense[by * gw + bx] = cnt > 0 && ink * 2 > cnt && sum / cnt < 200;
            }
        }
        boolean[] photo = new boolean[gw * gh];
        boolean[] seen = new boolean[gw * gh];
        int[] stack = new int[gw * gh];
        for (int s = 0; s < dense.length; s++) {
            if (!dense[s] || seen[s]) {
                continue;
            }
            int sp = 0;
            stack[sp++] = s;
            seen[s] = true;
            int n = 0;
            int minx = gw;
            int maxx = -1;
            int miny = gh;
            int maxy = -1;
            while (sp > 0) {
                int k = stack[--sp];
                n++;
                int kx = k % gw;
                int ky = k / gw;
                if (kx < minx) minx = kx;
                if (kx > maxx) maxx = kx;
                if (ky < miny) miny = ky;
                if (ky > maxy) maxy = ky;
                if (kx > 0 && dense[k - 1] && !seen[k - 1]) {
                    seen[k - 1] = true;
                    stack[sp++] = k - 1;
                }
                if (kx < gw - 1 && dense[k + 1] && !seen[k + 1]) {
                    seen[k + 1] = true;
                    stack[sp++] = k + 1;
                }
                if (ky > 0 && dense[k - gw] && !seen[k - gw]) {
                    seen[k - gw] = true;
                    stack[sp++] = k - gw;
                }
                if (ky < gh - 1 && dense[k + gw] && !seen[k + gw]) {
                    seen[k + gw] = true;
                    stack[sp++] = k + gw;
                }
            }
            if (n >= 16) {
                for (int y = miny; y <= maxy; y++) {
                    for (int x = minx; x <= maxx; x++) {
                        photo[y * gw + x] = true;
                    }
                }
            }
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                int p = px[i];
                if (photo[(y / bsz) * gw + (x / bsz)]) {
                    px[i] = p | 0xFF000000;
                } else {
                    int a = (int) ((245 - lum(p)) * 1.45f);
                    if (a < 0) a = 0;
                    if (a > 255) a = 255;
                    px[i] = (a << 24) | GOLD_RGB;
                }
            }
        }
    }

    /** Renders one PDF page (0-based PDF index) at the given size. Blocking. */
    Bitmap renderPdf(int pdfIndex, int w, int h) {
        synchronized (lock) {
            PdfRenderer.Page p = renderer.openPage(pdfIndex);
            try {
                Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                bm.eraseColor(Color.WHITE);
                p.render(bm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                return bm;
            } finally {
                p.close();
            }
        }
    }
}

package ir.jabheh.sharghi;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;

/**
 * Single-page book view. The spine is on the right (Persian book). Dragging a finger to the
 * right turns to the next page, to the left goes back. Tapping the left or right side also turns.
 */
final class BookView extends View {
    interface Callback {
        void onPageChanged(int page);

        void onTurnStart();
    }

    private static final float RATIO = PageSource.CROP_RATIO;
    private static final int BG = 0xFF2B1B0E;
    private static final int PAPER = 0xFFF3EAD3;

    private PageSource src;
    private Bitmap backdrop;
    private final Rect full = new Rect();
    private Callback cb;
    private int page;
    private int count;
    private int pw;
    private int ph;
    private int dir;
    private float progress;
    private float downX;
    private boolean dragging;
    private VelocityTracker vt;
    private ValueAnimator anim;
    private final int slop;
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint();
    private final Paint shade = new Paint();
    private static final int MCOLS = 16;
    private static final int MROWS = 2;
    private final float[] verts = new float[(MCOLS + 1) * (MROWS + 1) * 2];
    private final int[] vcols = new int[(MCOLS + 1) * (MROWS + 1)];
    private Bitmap logo;
    private Bitmap flagBmp;

    BookView(Context c) {
        super(c);
        slop = ViewConfiguration.get(c).getScaledTouchSlop();
        logo = loadRes(c, "emblem_logo");
        flagBmp = loadRes(c, "emblem_flag");
    }

    private static Bitmap loadRes(Context c, String name) {
        try {
            int id = c.getResources().getIdentifier(name, "drawable", c.getPackageName());
            if (id == 0) {
                return null;
            }
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inScaled = false;
            return BitmapFactory.decodeResource(c.getResources(), id, o);
        } catch (Throwable t) {
            return null;
        }
    }

    void setBackdrop(Bitmap b) {
        backdrop = b;
        computeSize(getWidth(), getHeight());
        prefetch();
        invalidate();
    }

    void setCallback(Callback c) {
        cb = c;
    }

    void setSource(PageSource s) {
        src = s;
        count = s.count();
        s.setListener(new PageSource.Listener() {
            @Override
            public void onPageReady() {
                invalidate();
            }
        });
        computeSize(getWidth(), getHeight());
        prefetch();
        invalidate();
    }

    int getPage() {
        return page;
    }

    int getCount() {
        return count;
    }

    void setPage(int p) {
        if (anim != null) {
            anim.removeAllListeners();
            anim.cancel();
            anim = null;
        }
        if (count > 0) {
            page = Math.max(0, Math.min(count - 1, p));
        }
        dir = 0;
        progress = 0f;
        if (cb != null) {
            cb.onPageChanged(page);
        }
        prefetch();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        computeSize(w, h);
        prefetch();
    }

    private void computeSize(int w, int h) {
        if (w <= 0 || h <= 0) {
            return;
        }
        float ax0;
        float ax1;
        float ay0;
        float ay1;
        if (backdrop != null) {
            // keep the pages inside the golden frame of the background picture
            ax0 = 0.155f * w;
            ax1 = 0.845f * w;
            ay0 = 0.075f * h;
            ay1 = 0.925f * h;
        } else {
            ax0 = 0.0075f * w;
            ax1 = 0.9925f * w;
            ay0 = 0.0075f * h;
            ay1 = 0.9925f * h;
        }
        float aw = ax1 - ax0;
        float ah = ay1 - ay0;
        float pwf = aw;
        float phf = aw / RATIO;
        if (phf > ah) {
            phf = ah;
            pwf = ah * RATIO;
        }
        pw = (int) pwf;
        ph = (int) phf;
    }

    private void prefetch() {
        if (src == null || pw <= 0 || count <= 0) {
            return;
        }
        src.get(page, pw, ph);
        if (page + 1 < count) {
            src.get(page + 1, pw, ph);
        }
        if (page - 1 >= 0) {
            src.get(page - 1, pw, ph);
        }
        if (page + 2 < count) {
            src.get(page + 2, pw, ph);
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        if (backdrop != null) {
            full.set(0, 0, getWidth(), getHeight());
            c.drawBitmap(backdrop, null, full, paint);
        } else {
            c.drawColor(BG);
        }
        drawBook(c);
        drawCorners(c);
        if (flagBmp != null) {
            postInvalidateOnAnimation();
        }
    }

    private void drawCorners(Canvas c) {
        float w = getWidth();
        float h = getHeight();
        float cy = 0.052f * h;
        if (logo != null) {
            float ls = 0.15f * w;
            float cx = 0.115f * w;
            full.set((int) (cx - ls / 2), (int) (cy - ls / 2), (int) (cx + ls / 2), (int) (cy + ls / 2));
            c.drawBitmap(logo, null, full, paint);
        }
        if (flagBmp != null) {
            drawFlag(c, 0.885f * w, cy, 0.19f * w);
        }
    }

    private void drawFlag(Canvas c, float cx, float cy, float fw) {
        float fh = fw * flagBmp.getHeight() / flagBmp.getWidth();
        float x0 = cx - fw / 2f;
        float y0 = cy - fh / 2f;
        float amp = fh * 0.08f;
        float t = (SystemClock.uptimeMillis() % 100000L) / 1000f * 3.6f;
        int k = 0;
        int ci = 0;
        for (int r = 0; r <= MROWS; r++) {
            for (int col = 0; col <= MCOLS; col++) {
                float f = (float) col / MCOLS;
                float ph = f * 5.5f - t;
                float dy = (float) Math.sin(ph) * amp * (0.2f + 0.8f * f);
                verts[k++] = x0 + fw * f;
                verts[k++] = y0 + fh * r / MROWS + dy;
                int g = 232 + (int) (23f * Math.cos(ph));
                vcols[ci++] = 0xFF000000 | (g << 16) | (g << 8) | g;
            }
        }
        c.drawBitmapMesh(flagBmp, MCOLS, MROWS, verts, 0, vcols, 0, paint);
    }

    private void drawBook(Canvas c) {
        if (src == null || count <= 0 || pw <= 0) {
            return;
        }
        int left = (getWidth() - pw) / 2;
        int top = (getHeight() - ph) / 2;
        Rect dst = new Rect(left, top, left + pw, top + ph);
        float spine = dst.right;
        if (dir == 0) {
            drawPage(c, page, dst);
            return;
        }
        if (dir > 0) {
            float s = 1f - progress;
            drawUnder(c, page + 1, dst, spine - pw * s);
            drawFolding(c, page, dst, s, spine);
        } else {
            drawUnder(c, page, dst, spine - pw * progress);
            drawFolding(c, page - 1, dst, progress, spine);
        }
    }

    /** The page lying below the turning page; only the part the turning page has uncovered. */
    private void drawUnder(Canvas c, int p, Rect dst, float edge) {
        c.save();
        c.clipRect(dst.left, dst.top, edge, dst.bottom);
        drawPage(c, p, dst);
        c.restore();
    }

    private void drawPage(Canvas c, int p, Rect dst) {
        Bitmap b = src.get(p, pw, ph);
        if (b == null) {
            if (backdrop == null) {
                fill.setColor(PAPER);
                c.drawRect(dst, fill);
            }
        } else {
            c.drawBitmap(b, null, dst, paint);
        }
    }

    private void drawFolding(Canvas c, int p, Rect dst, float s, float spine) {
        if (s < 0.01f) {
            return;
        }
        c.save();
        c.scale(s, 1f, spine, dst.top);
        drawPage(c, p, dst);
        c.restore();

        float edge = spine - pw * s;
        float w1 = pw * 0.14f;
        shade.setShader(new LinearGradient(edge, 0, edge - w1, 0, 0x66000000, 0x00000000,
                Shader.TileMode.CLAMP));
        c.drawRect(edge - w1, dst.top, edge, dst.bottom, shade);

        float w2 = pw * s * 0.22f;
        shade.setShader(new LinearGradient(edge, 0, edge + w2, 0, 0x44000000, 0x00000000,
                Shader.TileMode.CLAMP));
        c.drawRect(edge, dst.top, edge + w2, dst.bottom, shade);
        shade.setShader(null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (src == null || count <= 0 || anim != null) {
            return true;
        }
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                dragging = false;
                if (vt != null) {
                    vt.recycle();
                }
                vt = VelocityTracker.obtain();
                vt.addMovement(e);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (vt == null) {
                    return true;
                }
                vt.addMovement(e);
                float dx = e.getX() - downX;
                if (!dragging && Math.abs(dx) > slop) {
                    dragging = true;
                }
                if (dragging) {
                    int d = dx > 0 ? 1 : -1;
                    if ((d > 0 && page + 1 >= count) || (d < 0 && page - 1 < 0)) {
                        dir = 0;
                        progress = 0f;
                    } else {
                        dir = d;
                        progress = Math.min(1f, Math.abs(dx) / (pw * 0.8f));
                    }
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
                float vx = 0f;
                if (vt != null) {
                    vt.addMovement(e);
                    vt.computeCurrentVelocity(1000);
                    vx = vt.getXVelocity();
                    vt.recycle();
                    vt = null;
                }
                if (dragging && dir != 0) {
                    boolean fling = (dir > 0 && vx > 900f) || (dir < 0 && vx < -900f);
                    finishTurn(progress > 0.35f || fling);
                } else if (!dragging) {
                    float x = e.getX();
                    int w = getWidth();
                    if (x < w * 0.3f) {
                        startTap(1);
                    } else if (x > w * 0.7f) {
                        startTap(-1);
                    }
                } else {
                    dir = 0;
                    progress = 0f;
                    invalidate();
                }
                dragging = false;
                return true;
            case MotionEvent.ACTION_CANCEL:
                if (vt != null) {
                    vt.recycle();
                    vt = null;
                }
                dragging = false;
                dir = 0;
                progress = 0f;
                invalidate();
                return true;
            default:
                return true;
        }
    }

    private void startTap(int d) {
        if ((d > 0 && page + 1 >= count) || (d < 0 && page - 1 < 0)) {
            return;
        }
        dir = d;
        progress = 0f;
        finishTurn(true);
    }

    private void finishTurn(final boolean commit) {
        float from = progress;
        float to = commit ? 1f : 0f;
        if (commit && cb != null) {
            cb.onTurnStart();
        }
        anim = ValueAnimator.ofFloat(from, to);
        anim.setDuration((long) (120 + 320 * Math.abs(to - from)));
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator a) {
                progress = (Float) a.getAnimatedValue();
                invalidate();
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator a) {
                anim = null;
                if (commit) {
                    page += dir;
                }
                dir = 0;
                progress = 0f;
                if (commit) {
                    if (cb != null) {
                        cb.onPageChanged(page);
                    }
                    prefetch();
                }
                invalidate();
            }
        });
        anim.start();
    }
}

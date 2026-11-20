package ir.jabheh.sharghi;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import java.util.ArrayList;
import java.util.List;

/** Draws the second page (credits) natively. All texts come from strings.xml. */
final class CreditsPage {
    private static final int GOLD = 0xFFFFDF8A;
    private static final int GOLD_SOFT = 0xFFD9B160;
    private static final int CREAM = 0xFFF2D07A;

    private static final int T_TITLE = 0;
    private static final int T_DIV = 1;
    private static final int T_LABEL = 2;
    private static final int T_VALUE = 3;
    private static final int T_SMALL = 4;

    private static final class Item {
        final int type;
        final int res;

        Item(int type, int res) {
            this.type = type;
            this.res = res;
        }
    }

    private CreditsPage() {
    }

    static Bitmap draw(Context ctx, int w, int h) {
        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);

        List<Item> items = new ArrayList<>();
        items.add(new Item(T_TITLE, R.string.credits_title));
        items.add(new Item(T_DIV, 0));
        items.add(new Item(T_LABEL, R.string.label_author));
        items.add(new Item(T_VALUE, R.string.val_author));
        items.add(new Item(T_LABEL, R.string.label_compiler));
        items.add(new Item(T_VALUE, R.string.val_compiler));
        items.add(new Item(T_DIV, 0));
        items.add(new Item(T_SMALL, R.string.val_about));
        items.add(new Item(T_DIV, 0));
        items.add(new Item(T_LABEL, R.string.label_dev));
        items.add(new Item(T_VALUE, R.string.val_dev));

        Typeface face = pickFace(ctx);
        float side = w * 0.14f;
        int textW = (int) (w - 2 * side);
        float scale = 1f;
        List<StaticLayout> layouts = new ArrayList<>();
        List<Float> gaps = new ArrayList<>();
        float total = 0f;

        for (int attempt = 0; attempt < 6; attempt++) {
            layouts.clear();
            gaps.clear();
            total = 0f;
            for (Item it : items) {
                if (it.type == T_DIV) {
                    layouts.add(null);
                    float g = w * 0.07f * scale;
                    gaps.add(g);
                    total += g;
                    continue;
                }
                TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
                tp.setTypeface(face);
                float size;
                float gap;
                int color;
                switch (it.type) {
                    case T_TITLE:
                        size = w * 0.115f;
                        gap = w * 0.02f;
                        color = GOLD;
                        break;
                    case T_LABEL:
                        size = w * 0.042f;
                        gap = w * 0.008f;
                        color = GOLD_SOFT;
                        break;
                    case T_SMALL:
                        size = w * 0.05f;
                        gap = w * 0.01f;
                        color = CREAM;
                        break;
                    default:
                        size = w * 0.056f;
                        gap = w * 0.045f;
                        color = CREAM;
                        break;
                }
                tp.setTextSize(size * scale);
                tp.setColor(color);
                StaticLayout sl = new StaticLayout(ctx.getString(it.res), tp, textW,
                        Layout.Alignment.ALIGN_CENTER, 1.3f, 0f, false);
                layouts.add(sl);
                gaps.add(gap * scale);
                total += sl.getHeight() + gap * scale;
            }
            if (total <= h * 0.78f) {
                break;
            }
            scale *= 0.9f;
        }

        float y = (h - total) / 2f;
        Paint orn = new Paint(Paint.ANTI_ALIAS_FLAG);
        orn.setColor(GOLD);
        orn.setStrokeWidth(w * 0.003f);
        for (int i = 0; i < layouts.size(); i++) {
            StaticLayout sl = layouts.get(i);
            if (sl == null) {
                drawDivider(c, orn, w, y + gaps.get(i) / 2f);
                y += gaps.get(i);
            } else {
                c.save();
                c.translate(side, y);
                sl.draw(c);
                c.restore();
                y += sl.getHeight() + gaps.get(i);
            }
        }
        return bm;
    }

    private static void drawDivider(Canvas c, Paint p, int w, float y) {
        float cx = w / 2f;
        float d = w * 0.014f;
        c.drawLine(w * 0.25f, y, cx - d * 2f, y, p);
        c.drawLine(cx + d * 2f, y, w * 0.75f, y, p);
        Path diamond = new Path();
        diamond.moveTo(cx, y - d);
        diamond.lineTo(cx + d, y);
        diamond.lineTo(cx, y + d);
        diamond.lineTo(cx - d, y);
        diamond.close();
        p.setStyle(Paint.Style.FILL);
        c.drawPath(diamond, p);
    }

    /** If a .ttf/.otf font is placed in assets it is used, otherwise the system serif font. */
    private static Typeface pickFace(Context ctx) {
        try {
            String[] names = ctx.getAssets().list("");
            if (names != null) {
                for (String n : names) {
                    String l = n.toLowerCase();
                    if (l.endsWith(".ttf") || l.endsWith(".otf")) {
                        return Typeface.createFromAsset(ctx.getAssets(), n);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return Typeface.create(Typeface.SERIF, Typeface.BOLD);
    }
}

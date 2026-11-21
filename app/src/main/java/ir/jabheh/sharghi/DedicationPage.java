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

/** Dedication page drawn natively (gold ornaments and text on a transparent page). */
final class DedicationPage {
    private static final int GOLD = 0xFFFFDF8A;
    private static final int GOLD_SOFT = 0xFFD9B160;
    private static final int CREAM = 0xFFF2D07A;

    private static final int T_SMALL = 0;
    private static final int T_TITLE = 1;
    private static final int T_LABEL = 2;
    private static final int T_BODY = 3;
    private static final int T_DIV = 4;
    private static final int T_STAR = 5;

    private static final String[] FA = {
        "\u0628\u0627 \u0646\u0647\u0627\u06CC\u062A \u0627\u0645\u062A\u0646\u0627\u0646 \u0648 \u0642\u062F\u0631\u062F\u0627\u0646\u06CC",
        "\u0627\u0632 \u062C\u0646\u0627\u0628 \u0622\u0642\u0627\u06CC \u0645\u0647\u0646\u062F\u0633 \u063A\u0641\u0648\u0631\u06CC",
        "\u0631\u06CC\u0627\u0633\u062A \u0645\u062D\u062A\u0631\u0645 \u0627\u0645\u0648\u0631 \u0627\u062F\u0627\u0631\u06CC \u0634\u0631\u06A9\u062A \u0628\u0647\u0631\u0647\u200C\u0628\u0631\u062F\u0627\u0631\u06CC \u0646\u0641\u062A \u0648 \u06AF\u0627\u0632 \u0634\u0631\u0642",
        "\u0628\u0647 \u067E\u0627\u0633 \u062D\u0645\u0627\u06CC\u062A\u200C\u0647\u0627\u06CC \u0628\u06CC\u200C\u062F\u0631\u06CC\u063A \u0648 \u0645\u062F\u0628\u0631\u0627\u0646\u0647 \u0627\u06CC\u0634\u0627\u0646 \u06A9\u0647 \u0628\u0633\u062A\u0631 \u062A\u0647\u06CC\u0647 \u0648 \u062A\u0648\u0644\u06CC\u062F \u0627\u06CC\u0646 \u0627\u062B\u0631 \u062F\u06CC\u062C\u06CC\u062A\u0627\u0644 \u0631\u0627 \u0641\u0631\u0627\u0647\u0645 \u0633\u0627\u062E\u062A\u0646\u062F"
    };

    private static final String[] EN = {
        "With utmost gratitude and appreciation",
        "To Mr. Engineer Ghafouri",
        "Honorable Head of Administrative Affairs, Sharq Oil and Gas Production Company",
        "In recognition of his generous and wise support, which provided the foundation for preparing and producing this digital work"
    };

    private static final String[] RU = {
        "\u0421 \u0433\u043B\u0443\u0431\u043E\u043A\u043E\u0439 \u0431\u043B\u0430\u0433\u043E\u0434\u0430\u0440\u043D\u043E\u0441\u0442\u044C\u044E \u0438 \u043F\u0440\u0438\u0437\u043D\u0430\u0442\u0435\u043B\u044C\u043D\u043E\u0441\u0442\u044C\u044E",
        "\u0413\u043E\u0441\u043F\u043E\u0434\u0438\u043D\u0443 \u0438\u043D\u0436\u0435\u043D\u0435\u0440\u0443 \u0413\u0430\u0444\u0443\u0440\u0438",
        "\u0423\u0432\u0430\u0436\u0430\u0435\u043C\u043E\u043C\u0443 \u0440\u0443\u043A\u043E\u0432\u043E\u0434\u0438\u0442\u0435\u043B\u044E \u0430\u0434\u043C\u0438\u043D\u0438\u0441\u0442\u0440\u0430\u0442\u0438\u0432\u043D\u043E\u0433\u043E \u0443\u043F\u0440\u0430\u0432\u043B\u0435\u043D\u0438\u044F \u041D\u0435\u0444\u0442\u0435\u0433\u0430\u0437\u043E\u0434\u043E\u0431\u044B\u0432\u0430\u044E\u0449\u0435\u0439 \u043A\u043E\u043C\u043F\u0430\u043D\u0438\u0438 \u00AB\u0428\u0430\u0440\u043A\u00BB",
        "\u0412 \u0437\u043D\u0430\u043A \u043F\u0440\u0438\u0437\u043D\u0430\u043D\u0438\u044F \u0435\u0433\u043E \u0449\u0435\u0434\u0440\u043E\u0439 \u0438 \u043C\u0443\u0434\u0440\u043E\u0439 \u043F\u043E\u0434\u0434\u0435\u0440\u0436\u043A\u0438, \u0441\u043E\u0437\u0434\u0430\u0432\u0448\u0435\u0439 \u043E\u0441\u043D\u043E\u0432\u0443 \u0434\u043B\u044F \u043F\u043E\u0434\u0433\u043E\u0442\u043E\u0432\u043A\u0438 \u0438 \u0441\u043E\u0437\u0434\u0430\u043D\u0438\u044F \u044D\u0442\u043E\u0433\u043E \u0446\u0438\u0444\u0440\u043E\u0432\u043E\u0433\u043E \u043F\u0440\u043E\u0438\u0437\u0432\u0435\u0434\u0435\u043D\u0438\u044F"
    };

    private static final String[] AR = {
        "\u0645\u0639 \u062E\u0627\u0644\u0635 \u0627\u0644\u0627\u0645\u062A\u0646\u0627\u0646 \u0648\u0627\u0644\u062A\u0642\u062F\u064A\u0631",
        "\u0625\u0644\u0649 \u0627\u0644\u0633\u064A\u062F \u0627\u0644\u0645\u0647\u0646\u062F\u0633 \u063A\u0641\u0648\u0631\u064A",
        "\u0627\u0644\u0631\u0626\u064A\u0633 \u0627\u0644\u0645\u062D\u062A\u0631\u0645 \u0644\u0644\u0634\u0624\u0648\u0646 \u0627\u0644\u0625\u062F\u0627\u0631\u064A\u0629 \u0641\u064A \u0634\u0631\u0643\u0629 \u062A\u0634\u063A\u064A\u0644 \u0627\u0644\u0646\u0641\u0637 \u0648\u0627\u0644\u063A\u0627\u0632 \u00AB\u0634\u0631\u0642\u00BB",
        "\u062A\u0642\u062F\u064A\u0631\u0627\u064B \u0644\u062F\u0639\u0645\u0647 \u0627\u0644\u0633\u062E\u064A \u0648\u0627\u0644\u062D\u0643\u064A\u0645 \u0627\u0644\u0630\u064A \u0647\u064A\u0651\u0623 \u0627\u0644\u0623\u0631\u0636\u064A\u0629 \u0644\u0625\u0639\u062F\u0627\u062F \u0648\u0625\u0646\u062A\u0627\u062C \u0647\u0630\u0627 \u0627\u0644\u0639\u0645\u0644 \u0627\u0644\u0631\u0642\u0645\u064A"
    };

    private DedicationPage() {
    }

    private static String[] lines(Context ctx) {
        String l = ctx.getSharedPreferences("jabheh", 0).getString("lang", "fa");
        if ("en".equals(l)) {
            return EN;
        } else if ("ru".equals(l)) {
            return RU;
        } else if ("ar".equals(l)) {
            return AR;
        }
        return FA;
    }

    /** Plain text of the page, used by the text reader. */
    static String text(Context ctx) {
        String[] t = lines(ctx);
        return t[0] + "\n" + t[1] + "\n" + t[2] + "\n" + t[3];
    }

    static Bitmap draw(Context ctx, int w, int h) {
        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        String[] t = lines(ctx);

        int[] types = {T_STAR, T_SMALL, T_DIV, T_TITLE, T_LABEL, T_DIV, T_BODY, T_STAR};
        int[] idx = {-1, 0, -1, 1, 2, -1, 3, -1};

        Typeface face = pickFace(ctx);
        float side = w * 0.13f;
        int textW = (int) (w - 2 * side);
        float scale = 1f;
        List<StaticLayout> layouts = new ArrayList<>();
        List<Float> heights = new ArrayList<>();
        float total = 0f;

        for (int attempt = 0; attempt < 8; attempt++) {
            layouts.clear();
            heights.clear();
            total = 0f;
            for (int i = 0; i < types.length; i++) {
                int ty = types[i];
                if (ty == T_STAR) {
                    float hh = w * 0.17f * scale;
                    layouts.add(null);
                    heights.add(hh);
                    total += hh;
                    continue;
                }
                if (ty == T_DIV) {
                    float hh = w * 0.075f * scale;
                    layouts.add(null);
                    heights.add(hh);
                    total += hh;
                    continue;
                }
                TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
                tp.setTypeface(face);
                float size;
                float gap;
                int color;
                switch (ty) {
                    case T_SMALL:
                        size = w * 0.05f;
                        gap = w * 0.01f;
                        color = GOLD_SOFT;
                        break;
                    case T_TITLE:
                        size = w * 0.092f;
                        gap = w * 0.03f;
                        color = GOLD;
                        break;
                    case T_LABEL:
                        size = w * 0.053f;
                        gap = w * 0.01f;
                        color = CREAM;
                        break;
                    default:
                        size = w * 0.05f;
                        gap = w * 0.01f;
                        color = CREAM;
                        break;
                }
                tp.setTextSize(size * scale);
                tp.setColor(color);
                StaticLayout sl = new StaticLayout(t[idx[i]], tp, textW,
                        Layout.Alignment.ALIGN_CENTER, 1.35f, 0f, false);
                layouts.add(sl);
                float hh = sl.getHeight() + gap * scale;
                heights.add(hh);
                total += hh;
            }
            if (total <= h * 0.86f) {
                break;
            }
            scale *= 0.9f;
        }

        float y = (h - total) / 2f;
        Paint orn = new Paint(Paint.ANTI_ALIAS_FLAG);
        orn.setColor(GOLD);
        orn.setStrokeWidth(w * 0.003f);
        for (int i = 0; i < types.length; i++) {
            float hh = heights.get(i);
            StaticLayout sl = layouts.get(i);
            if (types[i] == T_STAR) {
                drawStar(c, orn, w / 2f, y + hh / 2f, hh * 0.42f);
            } else if (types[i] == T_DIV) {
                drawDivider(c, orn, w, y + hh / 2f);
            } else {
                c.save();
                c.translate(side, y);
                sl.draw(c);
                c.restore();
            }
            y += hh;
        }
        return bm;
    }

    private static void drawStar(Canvas c, Paint p, float cx, float cy, float r) {
        p.setStyle(Paint.Style.STROKE);
        for (int k = 0; k < 2; k++) {
            Path path = new Path();
            for (int i = 0; i < 4; i++) {
                double a = Math.toRadians(90 * i + 45 * k);
                float x = cx + (float) Math.cos(a) * r;
                float y = cy + (float) Math.sin(a) * r;
                if (i == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }
            path.close();
            c.drawPath(path, p);
        }
        p.setStyle(Paint.Style.FILL);
        c.drawCircle(cx, cy, r * 0.16f, p);
        float line = r * 2.6f;
        c.drawLine(cx - r - line, cy, cx - r * 1.25f, cy, p);
        c.drawLine(cx + r * 1.25f, cy, cx + r + line, cy, p);
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

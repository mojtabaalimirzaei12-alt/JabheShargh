package ir.jabheh.sharghi;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

/** First page: base logo, credit line and base photo, all inside the gold frame. */
final class IntroPage {
    private static final int GOLD = 0xFFF2D07A;

    private IntroPage() {
    }

    private static Bitmap load(Context ctx, String name, int targetW) {
        try {
            int id = ctx.getResources().getIdentifier(name, "drawable", ctx.getPackageName());
            if (id == 0) {
                return null;
            }
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(ctx.getResources(), id, o);
            int s = 1;
            while (o.outWidth / (s * 2) >= targetW) {
                s *= 2;
            }
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = s;
            return BitmapFactory.decodeResource(ctx.getResources(), id, o2);
        } catch (Throwable t) {
            return null;
        }
    }

    static Bitmap draw(Context ctx, int w, int h) {
        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        float y = h * 0.02f;

        float logoSize = Math.min(w * 0.72f, h * 0.36f);
        Bitmap logo = load(ctx, "basij_logo", (int) logoSize);
        if (logo != null) {
            float lx = (w - logoSize) / 2f;
            c.drawBitmap(logo, null, new RectF(lx, y, lx + logoSize, y + logoSize), p);
        }
        y += logoSize + h * 0.012f;

        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setColor(GOLD);
        c.drawRect(w * 0.3f, y, w * 0.7f, y + Math.max(2f, w * 0.004f), line);
        y += h * 0.012f;

        TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        tp.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
        tp.setColor(GOLD);
        tp.setTextSize(w * 0.05f);
        StaticLayout sl = new StaticLayout("\u06a9\u0627\u0631\u06cc \u0627\u0632 \u06af\u0631\u0648\u0647 \u0646\u0631\u0645\u200c\u0627\u0641\u0632\u0627\u0631\u06cc \u062f\u06cc\u062c\u06cc\u062a\u0627\u0644 \u067e\u0627\u06cc\u06af\u0627\u0647 \u0642\u062f\u0633\n\u0634\u0631\u06a9\u062a \u0628\u0647\u0631\u0647\u200c\u0628\u0631\u062f\u0627\u0631\u06cc \u0646\u0641\u062a \u0648 \u06af\u0627\u0632 \u0634\u0631\u0642", tp, (int) (w * 0.94f),
                Layout.Alignment.ALIGN_CENTER, 1.3f, 0f, false);
        c.save();
        c.translate(w * 0.03f, y);
        sl.draw(c);
        c.restore();
        y += sl.getHeight() + h * 0.01f;

        c.drawRect(w * 0.3f, y, w * 0.7f, y + Math.max(2f, w * 0.004f), line);
        y += h * 0.02f;

        float bw = w * 0.012f;
        RectF box = new RectF(bw, y, w - bw, h - h * 0.02f);
        if (box.height() > 40) {
            c.drawRect(box, line);
            RectF in = new RectF(box.left + bw, box.top + bw, box.right - bw, box.bottom - bw);
            Bitmap photo = load(ctx, "basij_pic", (int) in.width());
            if (photo != null) {
                float dr = in.width() / in.height();
                float sr = (float) photo.getWidth() / photo.getHeight();
                Rect src;
                if (sr > dr) {
                    int sw = (int) (photo.getHeight() * dr);
                    int x0 = (photo.getWidth() - sw) / 2;
                    src = new Rect(x0, 0, x0 + sw, photo.getHeight());
                } else {
                    int sh = (int) (photo.getWidth() / dr);
                    int y0 = (photo.getHeight() - sh) / 2;
                    src = new Rect(0, y0, photo.getWidth(), y0 + sh);
                }
                c.drawBitmap(photo, src, in, p);
            }
        }
        return bm;
    }
}

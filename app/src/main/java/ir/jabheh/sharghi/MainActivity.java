package ir.jabheh.sharghi;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity implements BookView.Callback {
    private static final int SPLASH_MS = 2600;
    private static final int BG = 0xFF2B1B0E;
    private static final int GOLD = 0xFFE6C36A;
    private static final String MUSIC_ON = "\uD83D\uDD0A";
    private static final String MUSIC_OFF = "\uD83D\uDD07";

    private SharedPreferences prefs;
    private PageSource source;
    private BookView book;
    private ImageView cover;
    private View splash;
    private TextView tvPage;
    private TextView tvMark;
    private TextView tvMusic;
    private int volume = 50;
    private String lang = "fa";
    private FrameLayout bookFrame;
    private FrameLayout reader;
    private ImageView readerBg;
    private LinearLayout readerBox;
    private ScrollView readerScroll;
    private TextView tvReader;
    private TextView tvTranslate;
    private TextView tvText;
    private boolean readerVisible = false;
    private boolean readerBgSet = false;
    private boolean translateOn = false;
    private String[] pageTexts;
    private Translator translator;
    private int trToken = 0;
    private MediaPlayer player;
    private FlipSound flip;
    private boolean musicOn = true;
    private boolean musicAvailable = false;
    private boolean bookShown = false;
    private final Set<Integer> marks = new HashSet<>();

    @Override
    protected void attachBaseContext(Context base) {
        String l = base.getSharedPreferences("jabheh", MODE_PRIVATE).getString("lang", "fa");
        Locale loc = new Locale(l);
        Locale.setDefault(loc);
        Configuration cfg = new Configuration(base.getResources().getConfiguration());
        cfg.setLocale(loc);
        cfg.setLayoutDirection(loc);
        super.attachBaseContext(base.createConfigurationContext(cfg));
    }

    private void showLanguageDialog(boolean cancelable) {
        final String[] codes = {"en", "ru", "ar", "fa"};
        final String[] names = {"English", "\u0420\u0443\u0441\u0441\u043A\u0438\u0439",
                "\u0627\u0644\u0639\u0631\u0628\u064A\u0629", "\u0641\u0627\u0631\u0633\u06CC"};
        new AlertDialog.Builder(this)
                .setTitle("Language / \u042F\u0437\u044B\u043A / \u0627\u0644\u0644\u063A\u0629 / \u0632\u0628\u0627\u0646")
                .setItems(names, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        prefs.edit().putString("lang", codes[which]).apply();
                        recreate();
                    }
                })
                .setCancelable(cancelable)
                .show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        prefs = getSharedPreferences("jabheh", MODE_PRIVATE);
        lang = prefs.getString("lang", "fa");
        if (!prefs.contains("lang")) {
            // first run after installation: ask for the language before anything else
            FrameLayout blank = new FrameLayout(this);
            blank.setBackgroundColor(BG);
            setContentView(blank);
            showLanguageDialog(false);
            return;
        }
        musicOn = prefs.getBoolean("music", true);
        volume = prefs.getInt("vol", 50);
        loadMarks();
        flip = new FlipSound();
        source = new PageSource(this);

        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(BG);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        book = new BookView(this);
        book.setCallback(this);
        bookFrame = new FrameLayout(this);
        bookFrame.addView(book, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        reader = buildReader();
        bookFrame.addView(reader, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        col.addView(bookFrame, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        col.addView(buildBar(), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        frame.addView(col, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        cover = new ImageView(this);
        cover.setScaleType(ImageView.ScaleType.FIT_CENTER);
        cover.setBackgroundColor(BG);
        splash = cover;
        frame.addView(splash, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        setContentView(frame);
        startLoading();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private LinearLayout buildBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackgroundColor(0xFF1E130A);
        bar.setPadding(dp(4), dp(4), dp(4), dp(4));

        tvMusic = barItem(MUSIC_ON);
        tvMusic.setTextSize(20f);
        tvMusic.setVisibility(View.GONE);
        tvMusic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleMusic();
            }
        });
        TextView tvSettings = barItem("\u2699");
        tvSettings.setTextSize(22f);
        tvSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettingsDialog();
            }
        });
        TextView tvMarks = barItem(getString(R.string.btn_marks));
        tvMarks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMarksDialog();
            }
        });
        tvMark = barItem(getString(R.string.mark_off));
        tvMark.setTextSize(20f);
        tvMark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleMark();
            }
        });
        TextView tvGoto = barItem(getString(R.string.btn_goto));
        tvGoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showGotoDialog();
            }
        });
        tvPage = barItem("");
        tvPage.setTextColor(0xFFF6EBD0);
        tvPage.setClickable(false);

        tvText = barItem(getString(R.string.btn_text));
        tvText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleReader();
            }
        });

        bar.addView(tvMusic);
        bar.addView(tvSettings);
        bar.addView(tvMark);
        bar.addView(tvGoto);
        bar.addView(tvText);
        bar.addView(tvPage);
        return bar;
    }

    private TextView barItem(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(GOLD);
        t.setTextSize(12f);
        t.setGravity(Gravity.CENTER);
        t.setSingleLine(true);
        t.setPadding(dp(4), dp(10), dp(4), dp(10));
        t.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return t;
    }

    // ---------- loading ----------

    private String findAsset(String... exts) {
        try {
            String[] all = getAssets().list("");
            if (all != null) {
                for (String n : all) {
                    String l = n.toLowerCase();
                    for (String e : exts) {
                        if (l.endsWith(e)) {
                            return n;
                        }
                    }
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    /** The picture whose name contains "back" is the page background; any other picture is the cover. */
    private String findImage(boolean backdrop) {
        try {
            String[] all = getAssets().list("");
            if (all != null) {
                for (String n : all) {
                    String l = n.toLowerCase();
                    boolean img = l.endsWith(".jpg") || l.endsWith(".jpeg") || l.endsWith(".png")
                            || l.endsWith(".webp");
                    if (img && l.contains("back") == backdrop) {
                        return n;
                    }
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    private Bitmap loadBackdrop(String name) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            try (InputStream in = getAssets().open(name)) {
                BitmapFactory.decodeStream(in, null, o);
            }
            int s = 1;
            while (Math.max(o.outWidth, o.outHeight) / s > 1400) {
                s *= 2;
            }
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = s;
            try (InputStream in = getAssets().open(name)) {
                return BitmapFactory.decodeStream(in, null, o2);
            }
        } catch (Throwable t) {
            return null;
        }
    }

    private Bitmap loadCover(String name) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            try (InputStream in = getAssets().open(name)) {
                BitmapFactory.decodeStream(in, null, o);
            }
            int s = 1;
            while (Math.max(o.outWidth, o.outHeight) / s > 2000) {
                s *= 2;
            }
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = s;
            try (InputStream in = getAssets().open(name)) {
                Bitmap b = BitmapFactory.decodeStream(in, null, o2);
                if (b != null && b.getWidth() > b.getHeight() * 1.1f) {
                    // a wide picture of the whole printed cover: keep only the front cover (left part)
                    b = Bitmap.createBitmap(b, 0, 0, (int) (b.getWidth() * 0.474f), b.getHeight());
                }
                return b;
            }
        } catch (Throwable t) {
            return null;
        }
    }

    private void startLoading() {
        final long t0 = System.currentTimeMillis();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String backName = findImage(true);
                final Bitmap bgBmp = backName != null ? loadBackdrop(backName) : null;
                if (bgBmp != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            book.setBackdrop(bgBmp);
                            readerBg.setImageBitmap(bgBmp);
                            readerBgSet = true;
                        }
                    });
                }
                String coverName = findImage(false);
                final Bitmap cb = coverName != null ? loadCover(coverName) : null;
                if (cb != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            cover.setImageBitmap(cb);
                        }
                    });
                }
                String pdfName = findAsset(".pdf");
                final boolean ok = pdfName != null && source.open(pdfName);

                if (cb == null && ok) {
                    try {
                        int w = getResources().getDisplayMetrics().widthPixels;
                        int h = (int) (w * 595.32f / 396.84f);
                        final Bitmap fb = source.renderPdf(2, w, h);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                cover.setImageBitmap(fb);
                            }
                        });
                    } catch (Throwable ignored) {
                    }
                }

                String textName = findAsset(".txt");
                if (textName != null) {
                    try (java.io.InputStream in = getAssets().open(textName)) {
                        ByteArrayOutputStream bo = new ByteArrayOutputStream();
                        byte[] buf = new byte[65536];
                        int n;
                        while ((n = in.read(buf)) > 0) {
                            bo.write(buf, 0, n);
                        }
                        pageTexts = bo.toString("UTF-8").split("\f", -1);
                    } catch (Throwable ignored) {
                        pageTexts = null;
                    }
                }

                String musicName = findAsset(".mp3", ".ogg", ".m4a");
                if (musicName != null) {
                    try {
                        AssetFileDescriptor fd = getAssets().openFd(musicName);
                        MediaPlayer mp = new MediaPlayer();
                        mp.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
                        fd.close();
                        mp.setLooping(true);
                        float v = volume / 100f;
                        mp.setVolume(v, v);
                        mp.prepare();
                        player = mp;
                        musicAvailable = true;
                        if (musicOn) {
                            mp.start();
                        }
                    } catch (Throwable t) {
                        player = null;
                        musicAvailable = false;
                    }
                }

                long wait = SPLASH_MS - (System.currentTimeMillis() - t0);
                if (wait > 0) {
                    try {
                        Thread.sleep(wait);
                    } catch (InterruptedException ignored) {
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showBook(ok);
                    }
                });
            }
        }).start();
    }

    private void showBook(boolean ok) {
        if (!ok) {
            Toast.makeText(this, R.string.err_book, Toast.LENGTH_LONG).show();
            return;
        }
        bookShown = true;
        book.setSource(source);
        book.setPage(prefs.getInt("page", 0));
        if (musicAvailable) {
            tvMusic.setVisibility(View.VISIBLE);
            updateMusicLabel();
            if (musicOn && player != null && !player.isPlaying()) {
                player.start();
            }
        }
        splash.animate().alpha(0f).setDuration(700).withEndAction(new Runnable() {
            @Override
            public void run() {
                splash.setVisibility(View.GONE);
            }
        }).start();
    }

    // ---------- BookView.Callback ----------

    @Override
    public void onPageChanged(int page) {
        tvPage.setText(num(page + 1) + " / " + num(book.getCount()));
        tvMark.setText(marks.contains(page) ? R.string.mark_on : R.string.mark_off);
        prefs.edit().putInt("page", page).apply();
        refreshReader();
    }

    @Override
    public void onTurnStart() {
    }

    // ---------- bookmarks / goto ----------

    private void loadMarks() {
        marks.clear();
        Set<String> s = prefs.getStringSet("marks", new HashSet<String>());
        for (String x : s) {
            try {
                marks.add(Integer.parseInt(x));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void saveMarks() {
        Set<String> s = new HashSet<>();
        for (Integer i : marks) {
            s.add(String.valueOf(i));
        }
        prefs.edit().putStringSet("marks", s).apply();
    }

    private void toggleMark() {
        if (!bookShown) {
            return;
        }
        int p = book.getPage();
        if (marks.contains(p)) {
            marks.remove(p);
            Toast.makeText(this, R.string.mark_removed, Toast.LENGTH_SHORT).show();
        } else {
            marks.add(p);
            Toast.makeText(this, R.string.mark_added, Toast.LENGTH_SHORT).show();
        }
        saveMarks();
        tvMark.setText(marks.contains(p) ? R.string.mark_on : R.string.mark_off);
    }

    private void showMarksDialog() {
        if (!bookShown) {
            return;
        }
        if (marks.isEmpty()) {
            Toast.makeText(this, R.string.no_marks, Toast.LENGTH_SHORT).show();
            return;
        }
        final List<Integer> list = new ArrayList<>(marks);
        Collections.sort(list);
        String[] items = new String[list.size()];
        for (int i = 0; i < items.length; i++) {
            items[i] = getString(R.string.page_fmt, num(list.get(i) + 1));
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.marks_title)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        book.setPage(list.get(which));
                    }
                })
                .show();
    }

    private void showGotoDialog() {
        if (!bookShown) {
            return;
        }
        final EditText et = new EditText(this);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setHint(R.string.goto_hint);
        new AlertDialog.Builder(this)
                .setTitle(R.string.goto_title)
                .setView(et)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        try {
                            int n = Integer.parseInt(fromFa(et.getText().toString().trim()));
                            if (n >= 1 && n <= book.getCount()) {
                                book.setPage(n - 1);
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // ---------- text reader and translation ----------

    private TextView readerBtn(String text) {
        TextView b = new TextView(this);
        b.setText(text);
        b.setTextColor(GOLD);
        b.setTextSize(15f);
        b.setGravity(Gravity.CENTER);
        b.setSingleLine(true);
        b.setPadding(dp(6), dp(10), dp(6), dp(10));
        b.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return b;
    }

    private FrameLayout buildReader() {
        FrameLayout r = new FrameLayout(this);
        r.setBackgroundColor(BG);
        readerBg = new ImageView(this);
        readerBg.setScaleType(ImageView.ScaleType.FIT_XY);
        r.addView(readerBg, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        readerBox = new LinearLayout(this);
        readerBox.setOrientation(LinearLayout.VERTICAL);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TextView prev = readerBtn(getString(R.string.reader_prev));
        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                book.setPage(book.getPage() - 1);
            }
        });
        tvTranslate = readerBtn(getString(R.string.btn_translate));
        tvTranslate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onTranslateClick();
            }
        });
        TextView next = readerBtn(getString(R.string.reader_next));
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                book.setPage(book.getPage() + 1);
            }
        });
        row.addView(prev);
        row.addView(tvTranslate);
        row.addView(next);
        readerBox.addView(row);

        readerScroll = new ScrollView(this);
        tvReader = new TextView(this);
        tvReader.setTextColor(0xFFF2D07A);
        tvReader.setTextSize(17f);
        tvReader.setLineSpacing(0f, 1.5f);
        tvReader.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        tvReader.setGravity(Gravity.START);
        tvReader.setPadding(0, dp(8), 0, dp(24));
        readerScroll.addView(tvReader);
        readerBox.addView(readerScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        r.addView(readerBox, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        r.setVisibility(View.GONE);
        return r;
    }

    private void toggleReader() {
        if (!bookShown) {
            return;
        }
        readerVisible = !readerVisible;
        if (readerVisible) {
            int w = bookFrame.getWidth();
            int h = bookFrame.getHeight();
            if (readerBgSet) {
                readerBox.setPadding((int) (w * 0.17f), (int) (h * 0.09f), (int) (w * 0.17f), (int) (h * 0.09f));
            } else {
                readerBox.setPadding(dp(14), dp(10), dp(14), dp(10));
            }
            reader.setVisibility(View.VISIBLE);
        } else {
            reader.setVisibility(View.GONE);
        }
        tvText.setText(readerVisible ? R.string.btn_image : R.string.btn_text);
        refreshReader();
    }

    @Override
    public void onBackPressed() {
        if (readerVisible) {
            toggleReader();
        } else {
            super.onBackPressed();
        }
    }

    private String textFor(int p) {
        if (p == 0) {
            return "\u06a9\u0627\u0631\u06cc \u0627\u0632 \u06af\u0631\u0648\u0647 \u0646\u0631\u0645\u200c\u0627\u0641\u0632\u0627\u0631\u06cc \u062f\u06cc\u062c\u06cc\u062a\u0627\u0644 \u067e\u0627\u06cc\u06af\u0627\u0647 \u0642\u062f\u0633\n\u0634\u0631\u06a9\u062a \u0628\u0647\u0631\u0647\u200c\u0628\u0631\u062f\u0627\u0631\u06cc \u0646\u0641\u062a \u0648 \u06af\u0627\u0632 \u0634\u0631\u0642";
        }
        if (p == 1) {
            return getString(R.string.bismillah);
        }
        if (p == 2) {
            return getString(R.string.credits_title) + "\n"
                    + getString(R.string.label_author) + ": " + getString(R.string.val_author) + "\n"
                    + getString(R.string.label_compiler) + ": " + getString(R.string.val_compiler) + "\n"
                    + getString(R.string.val_about) + "\n"
                    + getString(R.string.label_dev) + ": " + getString(R.string.val_dev);
        }
        if (pageTexts != null && p - 1 < pageTexts.length) {
            return pageTexts[p - 1].trim();
        }
        return "";
    }

    private void refreshReader() {
        if (!readerVisible) {
            return;
        }
        int p = book.getPage();
        trToken++;
        tvTranslate.setText(translateOn ? R.string.btn_original : R.string.btn_translate);
        readerScroll.scrollTo(0, 0);
        if (p >= 3 && pageTexts == null) {
            tvReader.setText(R.string.no_bookpages);
            return;
        }
        String orig = textFor(p);
        if (orig.length() == 0) {
            tvReader.setText(R.string.no_text);
            return;
        }
        if (!translateOn || p < 3) {
            tvReader.setText(orig);
            return;
        }
        String cached = readCache(p);
        if (cached != null) {
            tvReader.setText(cached);
            return;
        }
        tvReader.setText(R.string.tr_busy);
        translatePage(p, orig, trToken);
    }

    private void onTranslateClick() {
        if ("fa".equals(lang)) {
            Toast.makeText(this, R.string.tr_fa_note, Toast.LENGTH_LONG).show();
            return;
        }
        if (translateOn) {
            translateOn = false;
            refreshReader();
            return;
        }
        ensureModelThen(new Runnable() {
            @Override
            public void run() {
                translateOn = true;
                refreshReader();
            }
        });
    }

    private boolean online() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm.getActiveNetworkInfo();
            return ni != null && ni.isConnected();
        } catch (Throwable t) {
            return false;
        }
    }

    private Translator getTranslator() {
        if (translator == null) {
            TranslatorOptions o = new TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.PERSIAN)
                    .setTargetLanguage(lang)
                    .build();
            translator = Translation.getClient(o);
        }
        return translator;
    }

    private void ensureModelThen(final Runnable go) {
        if (prefs.getBoolean("model_" + lang, false)) {
            go.run();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.dl_title)
                .setMessage(getString(R.string.dl_msg, langName(lang)))
                .setPositiveButton(R.string.dl_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        startDownload(go);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void startDownload(final Runnable go) {
        if (!online()) {
            Toast.makeText(this, R.string.dl_offline, Toast.LENGTH_LONG).show();
            return;
        }
        final AlertDialog wait = new AlertDialog.Builder(this)
                .setMessage(R.string.dl_wait)
                .setCancelable(false)
                .show();
        getTranslator().downloadModelIfNeeded(new DownloadConditions.Builder().build())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void v) {
                        prefs.edit().putBoolean("model_" + lang, true).apply();
                        wait.dismiss();
                        go.run();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        wait.dismiss();
                        Toast.makeText(MainActivity.this, R.string.dl_fail, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private static List<String> chunk(String s) {
        List<String> out = new ArrayList<>();
        while (s.length() > 450) {
            int cut = s.lastIndexOf(". ", 450);
            if (cut < 150) {
                cut = s.lastIndexOf(' ', 450);
            }
            if (cut < 1) {
                cut = 450;
            } else {
                cut += 1;
            }
            out.add(s.substring(0, cut).trim());
            s = s.substring(cut).trim();
        }
        if (s.length() > 0) {
            out.add(s);
        }
        return out;
    }

    private void translatePage(int p, String orig, int token) {
        List<String> items = new ArrayList<>();
        List<Boolean> paraEnd = new ArrayList<>();
        for (String para : orig.split("\n")) {
            String q = para.trim();
            if (q.length() == 0) {
                continue;
            }
            List<String> parts = chunk(q);
            for (int i = 0; i < parts.size(); i++) {
                items.add(parts.get(i));
                paraEnd.add(i == parts.size() - 1);
            }
        }
        translateNext(p, items, paraEnd, 0, new StringBuilder(), token);
    }

    private void translateNext(final int p, final List<String> items, final List<Boolean> paraEnd,
                               final int i, final StringBuilder out, final int token) {
        if (token != trToken) {
            return;
        }
        if (i >= items.size()) {
            String res = out.toString().trim();
            writeCache(p, res);
            tvReader.setText(res);
            return;
        }
        getTranslator().translate(items.get(i))
                .addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String r) {
                        if (token != trToken) {
                            return;
                        }
                        out.append(r).append(paraEnd.get(i) ? "\n\n" : " ");
                        tvReader.setText(out.toString());
                        translateNext(p, items, paraEnd, i + 1, out, token);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        if (token != trToken) {
                            return;
                        }
                        tvReader.setText(R.string.tr_fail);
                    }
                });
    }

    private File cacheFile(int p) {
        File d = new File(getFilesDir(), "tr2_" + lang);
        d.mkdirs();
        return new File(d, p + ".txt");
    }

    private String readCache(int p) {
        try (java.io.InputStream in = new FileInputStream(cacheFile(p))) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) > 0) {
                bo.write(b, 0, n);
            }
            return bo.toString("UTF-8");
        } catch (Throwable t) {
            return null;
        }
    }

    private void writeCache(int p, String s) {
        try (OutputStream out = new FileOutputStream(cacheFile(p))) {
            out.write(s.getBytes("UTF-8"));
        } catch (Throwable ignored) {
        }
    }

    // ---------- settings ----------

    private static String langName(String code) {
        if ("en".equals(code)) {
            return "English";
        } else if ("ru".equals(code)) {
            return "\u0420\u0443\u0441\u0441\u043A\u0438\u0439";
        } else if ("ar".equals(code)) {
            return "\u0627\u0644\u0639\u0631\u0628\u064A\u0629";
        }
        return "\u0641\u0627\u0631\u0633\u06CC";
    }

    private void showSettingsDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(16), dp(20), dp(8));
        TextView label = new TextView(this);
        label.setText(R.string.settings_volume);
        label.setTextSize(16f);
        box.addView(label);
        SeekBar sb = new SeekBar(this);
        sb.setMax(100);
        sb.setProgress(volume);
        sb.setPadding(dp(8), dp(16), dp(8), dp(8));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                volume = progress;
                if (player != null) {
                    try {
                        player.setVolume(volume / 100f, volume / 100f);
                    } catch (Throwable ignored) {
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar s) {
                prefs.edit().putInt("vol", volume).apply();
            }
        });
        box.addView(sb);
        TextView langBtn = new TextView(this);
        langBtn.setText(getString(R.string.settings_language) + ":  " + langName(lang));
        langBtn.setTextSize(16f);
        langBtn.setTextColor(0xFF1565C0);
        langBtn.setPadding(0, dp(18), 0, dp(10));
        langBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLanguageDialog(true);
            }
        });
        box.addView(langBtn);
        TextView marksBtn = new TextView(this);
        marksBtn.setText(R.string.btn_marks);
        marksBtn.setTextSize(16f);
        marksBtn.setTextColor(0xFF1565C0);
        marksBtn.setPadding(0, dp(10), 0, dp(10));
        marksBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMarksDialog();
            }
        });
        box.addView(marksBtn);
        if (!musicAvailable) {
            TextView none = new TextView(this);
            none.setText(R.string.settings_nomusic);
            none.setTextSize(13f);
            box.addView(none);
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.settings_title)
                .setView(box)
                .setPositiveButton(R.string.close, null)
                .show();
    }

    // ---------- music ----------

    private void toggleMusic() {
        musicOn = !musicOn;
        prefs.edit().putBoolean("music", musicOn).apply();
        if (player != null) {
            try {
                if (musicOn) {
                    player.start();
                } else {
                    player.pause();
                }
            } catch (Throwable ignored) {
            }
        }
        updateMusicLabel();
    }

    private void updateMusicLabel() {
        tvMusic.setText(musicOn ? MUSIC_ON : MUSIC_OFF);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (player != null && player.isPlaying()) {
                player.pause();
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (bookShown && musicOn && player != null && !player.isPlaying()) {
                player.start();
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (translator != null) {
                translator.close();
            }
        } catch (Throwable ignored) {
        }
        try {
            if (player != null) {
                player.release();
                player = null;
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    // ---------- digits ----------

    private String num(int n) {
        char zero;
        if ("fa".equals(lang)) {
            zero = '\u06F0';
        } else if ("ar".equals(lang)) {
            zero = '\u0660';
        } else {
            return String.valueOf(n);
        }
        String s = String.valueOf(n);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            sb.append((char) (zero + (s.charAt(i) - '0')));
        }
        return sb.toString();
    }

    static String fromFa(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= '\u06F0' && ch <= '\u06F9') {
                sb.append((char) ('0' + (ch - '\u06F0')));
            } else if (ch >= '\u0660' && ch <= '\u0669') {
                sb.append((char) ('0' + (ch - '\u0660')));
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}

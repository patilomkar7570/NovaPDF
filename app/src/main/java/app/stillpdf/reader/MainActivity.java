package app.stillpdf.reader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * NOVA PDF: A private, local-first PDF reader for phone, foldable, and tablet devices.
 * No ads, analytics, accounts, or network permissions.
 * Local rendering powered by Android PdfRenderer; text search powered by PDFBox-Android.
 */
public class MainActivity extends Activity {
    private static final int PICK_PDF_REQUEST = 4107;
    // Preserved for compatibility with previous releases
    private static final String PREFS = "stillpdf_preferences";
    private static final String KEY_DARK = "dark_mode";
    private static final String KEY_RECENTS = "recent_documents";
    private static final String KEY_BOOKMARKS = "page_bookmarks";
    private static final String KEY_LAST_URI = "last_uri";
    private static final String KEY_LAST_NAME = "last_name";

    private SharedPreferences prefs;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private boolean darkMode;
    private boolean reading;

    private Uri currentUri;
    private String currentName = "Document.pdf";
    private File currentFile;
    private ParcelFileDescriptor currentPfd;
    private PdfRenderer renderer;
    private int currentPage = 0;
    private int pageCount = 0;

    private float zoomScale = 1.0f;
    private boolean showThumbnailsPanel = true;
    private int thumbRequestId = 0;

    private ViewGroup root;
    private ImageView pageImage;
    private TextView pageCounter;
    private TextView bookmarkButton;
    private TextView zoomIndicator;
    private Button prevButton;
    private Button nextButton;
    private EditText searchInput;
    private Button nextSearchButton;
    private LinearLayout thumbnailContainer;
    private ScrollView thumbnailScroll;
    private final ArrayList<Integer> searchPages = new ArrayList<>();
    private int searchResultIndex = -1;
    private Bitmap displayedBitmap;
    private int openRequestId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        darkMode = prefs.getBoolean(KEY_DARK, false);
        PDFBoxResourceLoader.init(getApplicationContext());
        applySystemBars();

        if (savedInstanceState != null) {
            reading = savedInstanceState.getBoolean("reading", false);
            currentPage = savedInstanceState.getInt("currentPage", 0);
            zoomScale = savedInstanceState.getFloat("zoomScale", 1.0f);
            showThumbnailsPanel = savedInstanceState.getBoolean("showThumbnailsPanel", true);
            String savedUriStr = savedInstanceState.getString("currentUri");
            if (savedUriStr != null) currentUri = Uri.parse(savedUriStr);
            currentName = savedInstanceState.getString("currentName", "Document.pdf");
        }

        if (reading && currentUri != null) {
            openPdf(currentUri, currentName, currentPage);
        } else {
            buildHome();
            Uri incoming = getIncomingUri(getIntent());
            if (incoming != null) {
                String name = nameFromUri(incoming);
                openPdf(incoming, name, 0);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("reading", reading);
        outState.putInt("currentPage", currentPage);
        outState.putFloat("zoomScale", zoomScale);
        outState.putBoolean("showThumbnailsPanel", showThumbnailsPanel);
        if (currentUri != null) outState.putString("currentUri", currentUri.toString());
        outState.putString("currentName", currentName);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        applySystemBars();
        if (reading) {
            buildReader();
            renderPage(currentPage);
        } else {
            buildHome();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Uri incoming = getIncomingUri(intent);
        if (incoming != null) openPdf(incoming, nameFromUri(incoming), 0);
    }

    private Uri getIncomingUri(Intent intent) {
        if (intent == null) return null;
        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action)) return intent.getData();
        if (Intent.ACTION_SEND.equals(action)) {
            if (Build.VERSION.SDK_INT >= 33) return intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri.class);
            //noinspection deprecation
            return (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        }
        return null;
    }

    private int getWidthDp() {
        return getResources().getConfiguration().screenWidthDp;
    }

    private boolean isCompact() {
        return getWidthDp() < 600;
    }

    private boolean isMedium() {
        int w = getWidthDp();
        return w >= 600 && w < 840;
    }

    private boolean isExpanded() {
        return getWidthDp() >= 840;
    }

    private void applySystemBars() {
        Window window = getWindow();
        int background = colorBackground();
        window.setStatusBarColor(background);
        window.setNavigationBarColor(background);
        int flags = 0;
        if (!darkMode && Build.VERSION.SDK_INT >= 23) flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (!darkMode && Build.VERSION.SDK_INT >= 26) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        window.getDecorView().setSystemUiVisibility(flags);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int colorBackground() { return Color.parseColor(darkMode ? "#0F1115" : "#F6F8FB"); }
    private int colorSurface() { return Color.parseColor(darkMode ? "#181C23" : "#FFFFFF"); }
    private int colorSurfaceSubtle() { return Color.parseColor(darkMode ? "#222731" : "#EEF2F6"); }
    private int colorText() { return Color.parseColor(darkMode ? "#F2F5F9" : "#0F172A"); }
    private int colorMuted() { return Color.parseColor(darkMode ? "#94A3B8" : "#64748B"); }
    private int colorAccent() { return Color.parseColor("#3267E8"); }
    private int colorBorder() { return Color.parseColor(darkMode ? "#2B3240" : "#E2E8F0"); }

    private GradientDrawable backgroundShape(int fill, int radiusDp, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeColor != 0) drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private TextView text(String value, float sizeSp, boolean bold, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private Button makeButton(String label, boolean primary, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setPadding(dp(14), dp(8), dp(14), dp(8));
        button.setMinHeight(dp(44));
        button.setBackground(backgroundShape(primary ? colorAccent() : colorSurface(), 12,
                primary ? 0 : colorBorder()));
        button.setTextColor(primary ? Color.WHITE : colorText());
        button.setOnClickListener(v -> action.run());
        return button;
    }

    private LinearLayout horizontal() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    private LinearLayout vertical() {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        return column;
    }

    private LinearLayout.LayoutParams lp(int width, int height) {
        return new LinearLayout.LayoutParams(width < 0 ? width : dp(width), height < 0 ? height : dp(height));
    }

    private ImageView makeLogoView(int sizeDp) {
        ImageView logoView = new ImageView(this);
        int resId = getResources().getIdentifier(darkMode ? "ic_nova_logo_white" : "ic_nova_logo", "drawable", getPackageName());
        if (resId != 0) {
            logoView.setImageResource(resId);
        } else {
            logoView.setImageResource(getResources().getIdentifier("ic_launcher", "mipmap", getPackageName()));
        }
        logoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        return logoView;
    }

    private void buildHome() {
        reading = false;
        boolean compact = isCompact();
        int horizontalPadding = compact ? dp(16) : dp(32);
        int verticalPadding = compact ? dp(12) : dp(24);

        LinearLayout homeRoot = vertical();
        homeRoot.setBackgroundColor(colorBackground());
        homeRoot.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);

        // Header bar with NOVA logo, title, and theme toggle
        LinearLayout header = horizontal();
        ImageView logo = makeLogoView(compact ? 36 : 44);
        header.addView(logo, lp(compact ? 36 : 44, compact ? 36 : 44));

        LinearLayout brand = vertical();
        brand.setPadding(dp(12), 0, 0, 0);
        TextView title = text("NOVA PDF", compact ? 24 : 30, true, colorText());
        TextView subtitle = text("Private, local document reader", compact ? 13 : 14, false, colorMuted());
        brand.addView(title, lp(-1, compact ? 30 : 36));
        brand.addView(subtitle, lp(-1, compact ? 20 : 24));
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));

        TextView theme = text(darkMode ? "☀" : "☾", 22, true, colorText());
        theme.setGravity(Gravity.CENTER);
        theme.setBackground(backgroundShape(colorSurface(), 14, colorBorder()));
        header.addView(theme, lp(compact ? 44 : 48, compact ? 44 : 48));
        theme.setOnClickListener(v -> toggleTheme());
        homeRoot.addView(header, lp(-1, -2));

        // Privacy indicator
        TextView privacy = text("OFFLINE  •  NO ADS  •  PRIVATE & LOCAL", 11, true, colorAccent());
        privacy.setLetterSpacing(0.08f);
        LinearLayout.LayoutParams privacyParams = lp(-1, 26);
        privacyParams.topMargin = dp(8);
        homeRoot.addView(privacy, privacyParams);

        // Action Card (Open PDF & Continue Reading)
        LinearLayout actionCard = vertical();
        actionCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        actionCard.setBackground(backgroundShape(colorSurface(), 16, colorBorder()));

        TextView actionTitle = text("Open a document", compact ? 17 : 20, true, colorText());
        TextView actionHint = text("Select a PDF from your device storage. Documents are processed locally.", 13, false, colorMuted());
        actionCard.addView(actionTitle, lp(-1, -2));
        LinearLayout.LayoutParams hintParams = lp(-1, -2);
        hintParams.topMargin = dp(4);
        actionCard.addView(actionHint, hintParams);

        LinearLayout buttonRow = compact ? vertical() : horizontal();
        Button open = makeButton("＋  Open PDF", true, this::launchPicker);
        if (compact) {
            LinearLayout.LayoutParams openParams = lp(-1, 48);
            openParams.topMargin = dp(14);
            buttonRow.addView(open, openParams);
        } else {
            LinearLayout.LayoutParams openParams = lp(180, 48);
            openParams.topMargin = dp(14);
            buttonRow.addView(open, openParams);
        }

        String lastUriValue = prefs.getString(KEY_LAST_URI, null);
        if (lastUriValue != null && !lastUriValue.isEmpty()) {
            String lastName = prefs.getString(KEY_LAST_NAME, "Last document");
            Button resume = makeButton("Continue reading: " + lastName, false, () -> {
                try {
                    Uri uri = Uri.parse(lastUriValue);
                    int page = prefs.getInt(pageKey(uri), 0);
                    openPdf(uri, nameFromStored(uri, lastName), page);
                } catch (Exception e) {
                    showToast("Could not reopen that PDF. Please select it again.");
                }
            });
            if (compact) {
                LinearLayout.LayoutParams resumeParams = lp(-1, 48);
                resumeParams.topMargin = dp(8);
                buttonRow.addView(resume, resumeParams);
            } else {
                LinearLayout.LayoutParams resumeParams = lp(300, 48);
                resumeParams.topMargin = dp(14);
                resumeParams.leftMargin = dp(12);
                buttonRow.addView(resume, resumeParams);
            }
        }
        actionCard.addView(buttonRow, lp(-1, -2));

        LinearLayout.LayoutParams cardParams = lp(-1, -2);
        cardParams.topMargin = dp(12);
        homeRoot.addView(actionCard, cardParams);

        // Document sections: On compact, vertical stack; On medium/expanded tablets, side-by-side two columns
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        ArrayList<JSONObject> recents = readJsonList(KEY_RECENTS);
        ArrayList<JSONObject> bookmarks = readJsonList(KEY_BOOKMARKS);

        if (compact) {
            LinearLayout lists = vertical();
            buildSection(lists, "Recent Documents", recents, true);
            buildSection(lists, "Bookmarks", bookmarks, false);

            TextView footer = text("NOVA PDF  •  Offline & Private Document Reader", 12, false, colorMuted());
            footer.setGravity(Gravity.CENTER);
            footer.setOnClickListener(v -> showAboutDialog());
            LinearLayout.LayoutParams footerParams = lp(-1, 44);
            footerParams.topMargin = dp(16);
            lists.addView(footer, footerParams);

            scroll.addView(lists, new ScrollView.LayoutParams(-1, -2));
        } else {
            // Tablet / wide layout: Two-column layout
            LinearLayout twoColumns = horizontal();
            twoColumns.setGravity(Gravity.TOP);

            LinearLayout leftCol = vertical();
            buildSection(leftCol, "Recent Documents", recents, true);
            twoColumns.addView(leftCol, new LinearLayout.LayoutParams(0, -2, 1.0f));

            View spacer = new View(this);
            twoColumns.addView(spacer, lp(18, 1));

            LinearLayout rightCol = vertical();
            buildSection(rightCol, "Bookmarks", bookmarks, false);
            twoColumns.addView(rightCol, new LinearLayout.LayoutParams(0, -2, 1.0f));

            LinearLayout wideContainer = vertical();
            wideContainer.addView(twoColumns, lp(-1, -2));

            TextView footer = text("NOVA PDF  •  Offline & Private Document Reader", 12, false, colorMuted());
            footer.setGravity(Gravity.CENTER);
            footer.setOnClickListener(v -> showAboutDialog());
            LinearLayout.LayoutParams footerParams = lp(-1, 44);
            footerParams.topMargin = dp(24);
            wideContainer.addView(footer, footerParams);

            scroll.addView(wideContainer, new ScrollView.LayoutParams(-1, -2));
        }

        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, 0, 1.0f);
        scrollParams.topMargin = dp(8);
        homeRoot.addView(scroll, scrollParams);

        root = homeRoot;
        setContentView(root);
        applySystemBars();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("NOVA PDF")
                .setMessage("Version 1.1.0\n\n"
                        + "Licensed under the MIT License\n"
                        + "Copyright \u00A9 2026 patilomkar7570\n\n"
                        + "A private, distraction-free document reader for Android.\n"
                        + "100% offline. No advertisements, tracking, or cloud uploads.\n\n"
                        + "Third-party software:\n"
                        + "\u2022 PDFBox-Android 2.0.27.0 (Apache License 2.0)")
                .setPositiveButton("OK", null)
                .show();
    }

    private void buildSection(LinearLayout container, String title, ArrayList<JSONObject> items, boolean isRecent) {
        TextView header = text(title, 18, true, colorText());
        LinearLayout.LayoutParams headerParams = lp(-1, 38);
        headerParams.topMargin = dp(12);
        container.addView(header, headerParams);

        if (items.isEmpty()) {
            String emptyMsg = isRecent ? "Your recently opened PDFs will appear here." : "Tap the bookmark icon while reading to save pages.";
            TextView emptyView = text(emptyMsg, 13, false, colorMuted());
            emptyView.setPadding(dp(12), dp(12), dp(12), dp(12));
            emptyView.setBackground(backgroundShape(colorSurface(), 12, colorBorder()));
            container.addView(emptyView, lp(-1, -2));
        } else {
            for (JSONObject item : items) {
                String uriString = item.optString("uri", "");
                String name = item.optString("name", "Document.pdf");
                if (uriString.isEmpty()) continue;
                if (isRecent) {
                    addDocumentRow(container, name, "Tap to open", "›", () -> {
                        Uri uri = Uri.parse(uriString);
                        openPdf(uri, name, prefs.getInt(pageKey(uri), 0));
                    });
                } else {
                    int page = Math.max(0, item.optInt("page", 0));
                    addDocumentRow(container, name, "Page " + (page + 1), "★", () -> {
                        Uri uri = Uri.parse(uriString);
                        openPdf(uri, name, page);
                    });
                }
            }
        }
    }

    private void addDocumentRow(LinearLayout parent, String name, String subtitle, String glyph, Runnable click) {
        LinearLayout row = horizontal();
        row.setPadding(dp(14), dp(8), dp(14), dp(8));
        row.setBackground(backgroundShape(colorSurface(), 12, colorBorder()));

        LinearLayout words = vertical();
        TextView title = text(name, 14, true, colorText());
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        TextView caption = text(subtitle, 12, false, colorMuted());
        words.addView(title, lp(-1, 22));
        words.addView(caption, lp(-1, 18));
        row.addView(words, new LinearLayout.LayoutParams(0, -2, 1));

        TextView arrow = text(glyph, 18, true, colorAccent());
        arrow.setGravity(Gravity.CENTER);
        row.addView(arrow, lp(32, 38));
        row.setOnClickListener(v -> click.run());

        LinearLayout.LayoutParams params = lp(-1, 56);
        params.bottomMargin = dp(6);
        parent.addView(row, params);
    }

    private void toggleTheme() {
        darkMode = !darkMode;
        prefs.edit().putBoolean(KEY_DARK, darkMode).apply();
        applySystemBars();
        if (reading) {
            buildReader();
            renderPage(currentPage);
        } else {
            buildHome();
        }
    }

    private void launchPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        startActivityForResult(intent, PICK_PDF_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_PDF_REQUEST || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            getContentResolver().takePersistableUriPermission(uri, flags & Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {
        }
        openPdf(uri, nameFromUri(uri), 0);
    }

    private String nameFromUri(Uri uri) {
        if (uri == null) return "Document.pdf";
        String result = null;
        try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) result = cursor.getString(index);
            }
        } catch (Exception ignored) { }
        if (result == null || result.trim().isEmpty()) {
            String last = uri.getLastPathSegment();
            result = (last == null || last.trim().isEmpty()) ? "Document.pdf" : last;
        }
        return result;
    }

    private String nameFromStored(Uri uri, String fallback) {
        String found = nameFromUri(uri);
        return found == null || found.trim().isEmpty() ? fallback : found;
    }

    private void openPdf(Uri uri, String name, int requestedPage) {
        if (uri == null) return;
        final int requestId = ++openRequestId;
        closeRenderer();
        reading = true;
        currentUri = uri;
        currentName = (name == null || name.trim().isEmpty()) ? "Document.pdf" : name;
        prefs.edit().putString(KEY_LAST_URI, uri.toString()).putString(KEY_LAST_NAME, currentName).apply();
        upsertRecent(uri.toString(), currentName);

        LinearLayout loadingLayout = vertical();
        loadingLayout.setBackgroundColor(colorBackground());
        loadingLayout.setGravity(Gravity.CENTER);
        TextView loading = text("Opening PDF…", 18, true, colorText());
        loading.setGravity(Gravity.CENTER);
        loadingLayout.addView(loading, lp(-2, -2));
        root = loadingLayout;
        setContentView(root);
        applySystemBars();

        ioExecutor.execute(() -> {
            try {
                File file = copyIntoCache(uri);
                runOnUiThread(() -> {
                    if (requestId != openRequestId || isFinishing() || isDestroyed()) return;
                    currentFile = file;
                    try {
                        currentPfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
                        renderer = new PdfRenderer(currentPfd);
                        pageCount = renderer.getPageCount();
                        if (pageCount <= 0) throw new IOException("This PDF contains no pages.");
                        currentPage = Math.max(0, Math.min(requestedPage, pageCount - 1));
                        searchPages.clear();
                        searchResultIndex = -1;
                        buildReader();
                        renderPage(currentPage);
                    } catch (Exception e) {
                        closeRenderer();
                        reading = false;
                        buildHome();
                        showToast("Could not open this PDF. It may be damaged or password-protected.");
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (requestId != openRequestId || isFinishing() || isDestroyed()) return;
                    reading = false;
                    buildHome();
                    showToast("Could not read that file. Please choose the PDF again.");
                });
            }
        });
    }

    private File copyIntoCache(Uri uri) throws IOException {
        File folder = new File(getCacheDir(), "pdf_documents");
        if (!folder.exists() && !folder.mkdirs()) throw new IOException("Unable to create PDF cache directory.");
        String key = md5(uri.toString());
        File target = new File(folder, key + ".pdf");
        File temp = new File(folder, key + ".part");
        ContentResolver resolver = getContentResolver();
        try (InputStream input = resolver.openInputStream(uri);
             FileOutputStream output = new FileOutputStream(temp)) {
            if (input == null) throw new IOException("The selected file could not be opened.");
            byte[] buffer = new byte[32 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            output.flush();
        } catch (Exception e) {
            //noinspection ResultOfMethodCallIgnored
            temp.delete();
            if (e instanceof IOException) throw (IOException) e;
            throw new IOException("Could not copy the PDF", e);
        }
        if (temp.length() < 5) {
            //noinspection ResultOfMethodCallIgnored
            temp.delete();
            throw new IOException("The selected file is empty.");
        }
        if (target.exists() && !target.delete()) {
        }
        if (!temp.renameTo(target)) {
            //noinspection ResultOfMethodCallIgnored
            temp.delete();
            throw new IOException("Could not prepare the PDF for reading.");
        }
        return target;
    }

    private String md5(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) result.append(String.format(Locale.ROOT, "%02x", b & 0xff));
            return result.toString();
        } catch (Exception e) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private void buildReader() {
        reading = true;
        boolean compact = isCompact();

        LinearLayout readerRoot = vertical();
        readerRoot.setBackgroundColor(colorBackground());

        // Top Navigation Toolbar
        LinearLayout topBar = horizontal();
        topBar.setPadding(dp(8), 0, dp(12), 0);
        topBar.setBackgroundColor(colorSurface());

        TextView back = text("‹", 30, false, colorText());
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> goHome());
        topBar.addView(back, lp(40, 52));

        if (!compact) {
            ImageView miniLogo = makeLogoView(24);
            topBar.addView(miniLogo, lp(28, 28));
        }

        TextView title = text(currentName, compact ? 15 : 16, true, colorText());
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        title.setPadding(dp(8), 0, dp(8), 0);
        topBar.addView(title, new LinearLayout.LayoutParams(0, dp(52), 1));

        // Thumbnail sidebar toggle (tablets)
        if (!compact) {
            TextView thumbToggle = text("▤", 20, true, showThumbnailsPanel ? colorAccent() : colorMuted());
            thumbToggle.setGravity(Gravity.CENTER);
            thumbToggle.setOnClickListener(v -> toggleThumbnails());
            topBar.addView(thumbToggle, lp(40, 52));
        }

        // Zoom controls
        TextView zoomOut = text("－", 18, true, colorText());
        zoomOut.setGravity(Gravity.CENTER);
        zoomOut.setOnClickListener(v -> changeZoom(-0.25f));
        topBar.addView(zoomOut, lp(36, 52));

        zoomIndicator = text(Math.round(zoomScale * 100) + "%", 12, true, colorMuted());
        zoomIndicator.setGravity(Gravity.CENTER);
        zoomIndicator.setOnClickListener(v -> resetZoom());
        topBar.addView(zoomIndicator, lp(compact ? 42 : 50, 52));

        TextView zoomIn = text("＋", 18, true, colorText());
        zoomIn.setGravity(Gravity.CENTER);
        zoomIn.setOnClickListener(v -> changeZoom(0.25f));
        topBar.addView(zoomIn, lp(36, 52));

        // Bookmark button
        bookmarkButton = text(isBookmarked(currentUri, currentPage) ? "★" : "☆", 24, true, colorAccent());
        bookmarkButton.setGravity(Gravity.CENTER);
        bookmarkButton.setOnClickListener(v -> toggleBookmark());
        topBar.addView(bookmarkButton, lp(42, 52));

        // Theme toggle
        TextView theme = text(darkMode ? "☀" : "☾", 20, true, colorText());
        theme.setGravity(Gravity.CENTER);
        theme.setOnClickListener(v -> toggleTheme());
        topBar.addView(theme, lp(38, 52));

        readerRoot.addView(topBar, lp(-1, 52));

        // Search Bar
        LinearLayout searchRow = horizontal();
        searchRow.setPadding(dp(12), dp(6), dp(12), dp(6));
        searchRow.setBackgroundColor(colorSurfaceSubtle());

        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setTextSize(14);
        searchInput.setHint("Search text in PDF…");
        searchInput.setHintTextColor(colorMuted());
        searchInput.setTextColor(colorText());
        searchInput.setPadding(dp(12), 0, dp(12), 0);
        searchInput.setBackground(backgroundShape(colorSurface(), 10, colorBorder()));
        searchRow.addView(searchInput, new LinearLayout.LayoutParams(0, dp(38), 1));

        Button find = makeButton("Find", true, this::searchPdf);
        LinearLayout.LayoutParams findParams = lp(compact ? 60 : 76, 38);
        findParams.leftMargin = dp(6);
        searchRow.addView(find, findParams);

        nextSearchButton = makeButton("Next", false, this::nextSearchResult);
        LinearLayout.LayoutParams nextParams = lp(compact ? 60 : 76, 38);
        nextParams.leftMargin = dp(6);
        searchRow.addView(nextSearchButton, nextParams);

        readerRoot.addView(searchRow, lp(-1, 50));

        // Main Reading Area:
        LinearLayout centerSplit = horizontal();
        centerSplit.setBackgroundColor(darkMode ? Color.parseColor("#181B20") : Color.parseColor("#E5E9F0"));

        if (!compact && showThumbnailsPanel) {
            thumbnailScroll = new ScrollView(this);
            thumbnailScroll.setFillViewport(true);
            thumbnailScroll.setBackgroundColor(colorSurface());

            thumbnailContainer = vertical();
            thumbnailContainer.setPadding(dp(8), dp(8), dp(8), dp(8));
            thumbnailContainer.setGravity(Gravity.CENTER_HORIZONTAL);
            thumbnailScroll.addView(thumbnailContainer, new ScrollView.LayoutParams(-1, -2));

            centerSplit.addView(thumbnailScroll, lp(compact ? 120 : 170, -1));

            populateThumbnails();
        }

        // Center PDF Canvas
        ScrollView pageScroll = new ScrollView(this);
        pageScroll.setFillViewport(true);
        pageScroll.setBackgroundColor(darkMode ? Color.parseColor("#14171C") : Color.parseColor("#E9EDF4"));

        LinearLayout pageHolder = vertical();
        pageHolder.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        pageHolder.setPadding(dp(8), dp(12), dp(8), dp(12));

        pageImage = new ImageView(this);
        pageImage.setAdjustViewBounds(true);
        pageImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        pageImage.setBackgroundColor(Color.WHITE);
        pageHolder.addView(pageImage, new LinearLayout.LayoutParams(-1, -2));

        pageScroll.addView(pageHolder, new ScrollView.LayoutParams(-1, -2));
        centerSplit.addView(pageScroll, new LinearLayout.LayoutParams(0, -1, 1));

        readerRoot.addView(centerSplit, new LinearLayout.LayoutParams(-1, 0, 1));

        // Bottom Navigation Bar
        LinearLayout nav = horizontal();
        nav.setPadding(dp(16), dp(6), dp(16), dp(6));
        nav.setBackgroundColor(colorSurface());

        prevButton = makeButton("←  Prev", false, () -> setPage(currentPage - 1));
        nav.addView(prevButton, new LinearLayout.LayoutParams(0, dp(42), 1));

        pageCounter = text(pageLabel(), 14, true, colorText());
        pageCounter.setGravity(Gravity.CENTER);
        pageCounter.setOnClickListener(v -> showPageJumpDialog());
        nav.addView(pageCounter, new LinearLayout.LayoutParams(0, dp(42), 1));

        nextButton = makeButton("Next  →", true, () -> setPage(currentPage + 1));
        nav.addView(nextButton, new LinearLayout.LayoutParams(0, dp(42), 1));

        readerRoot.addView(nav, lp(-1, 54));

        root = readerRoot;
        setContentView(root);
        applySystemBars();
        updateReaderControls();
    }

    private void toggleThumbnails() {
        showThumbnailsPanel = !showThumbnailsPanel;
        buildReader();
        renderPage(currentPage);
    }

    private void changeZoom(float delta) {
        zoomScale = Math.max(0.5f, Math.min(3.0f, zoomScale + delta));
        renderPage(currentPage);
    }

    private void resetZoom() {
        zoomScale = 1.0f;
        renderPage(currentPage);
    }

    private void showPageJumpDialog() {
        if (pageCount <= 1) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Go to page (1 – " + pageCount + ")");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(currentPage + 1));
        input.setSelection(input.getText().length());
        builder.setView(input);

        builder.setPositiveButton("Go", (dialog, which) -> {
            try {
                int target = Integer.parseInt(input.getText().toString().trim()) - 1;
                if (target >= 0 && target < pageCount) {
                    setPage(target);
                } else {
                    showToast("Page number must be between 1 and " + pageCount);
                }
            } catch (Exception ignored) { }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Renders page thumbnails sequentially on an independent background thread
     * using its own dedicated PdfRenderer and ParcelFileDescriptor instances,
     * avoiding any IllegalStateException from concurrent page rendering.
     */
    private void populateThumbnails() {
        if (thumbnailContainer == null || currentFile == null || pageCount <= 0) return;
        thumbnailContainer.removeAllViews();
        final int reqId = ++thumbRequestId;

        int thumbWidthDp = isCompact() ? 100 : 140;
        int maxThumbs = Math.min(pageCount, 60);

        final ImageView[] thumbViews = new ImageView[maxThumbs];

        for (int i = 0; i < maxThumbs; i++) {
            final int pageIdx = i;
            LinearLayout thumbItem = vertical();
            thumbItem.setPadding(dp(4), dp(4), dp(4), dp(4));
            thumbItem.setGravity(Gravity.CENTER_HORIZONTAL);
            thumbItem.setBackground(backgroundShape(colorSurface(), 8,
                    (pageIdx == currentPage) ? colorAccent() : colorBorder()));

            TextView label = text("Page " + (pageIdx + 1), 11, pageIdx == currentPage,
                    (pageIdx == currentPage) ? colorAccent() : colorMuted());
            label.setGravity(Gravity.CENTER);
            thumbItem.addView(label, lp(-1, 18));

            ImageView thumbImg = new ImageView(this);
            thumbImg.setScaleType(ImageView.ScaleType.FIT_CENTER);
            thumbImg.setAdjustViewBounds(true);
            thumbImg.setBackgroundColor(Color.WHITE);
            thumbItem.addView(thumbImg, lp(thumbWidthDp, -2));

            thumbItem.setOnClickListener(v -> setPage(pageIdx));

            LinearLayout.LayoutParams itemLp = lp(-1, -2);
            itemLp.bottomMargin = dp(8);
            thumbnailContainer.addView(thumbItem, itemLp);

            thumbViews[i] = thumbImg;
        }

        final File pdfFile = currentFile;
        ioExecutor.execute(() -> {
            ParcelFileDescriptor pfd = null;
            PdfRenderer r = null;
            try {
                pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
                r = new PdfRenderer(pfd);
                for (int i = 0; i < maxThumbs; i++) {
                    if (reqId != thumbRequestId || isFinishing() || isDestroyed()) break;
                    Bitmap bm = null;
                    try (PdfRenderer.Page page = r.openPage(i)) {
                        int w = dp(thumbWidthDp);
                        float ratio = (float) page.getHeight() / Math.max(1, page.getWidth());
                        int h = Math.max(1, Math.round(w * ratio));
                        bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                        bm.eraseColor(Color.WHITE);
                        page.render(bm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    } catch (Exception ignored) { }

                    final int pageIdx = i;
                    final Bitmap finalBm = bm;
                    if (finalBm != null) {
                        runOnUiThread(() -> {
                            if (reqId == thumbRequestId && thumbViews[pageIdx] != null) {
                                thumbViews[pageIdx].setImageBitmap(finalBm);
                            }
                        });
                    }
                }
            } catch (Exception ignored) {
            } finally {
                if (r != null) { try { r.close(); } catch (Exception ignored) {} }
                if (pfd != null) { try { pfd.close(); } catch (Exception ignored) {} }
            }
        });
    }

    private String pageLabel() {
        return pageCount > 0 ? (currentPage + 1) + " / " + pageCount : "— / —";
    }

    private void updateReaderControls() {
        if (pageCounter != null) pageCounter.setText(pageLabel());
        if (bookmarkButton != null) bookmarkButton.setText(isBookmarked(currentUri, currentPage) ? "★" : "☆");
        if (zoomIndicator != null) zoomIndicator.setText(Math.round(zoomScale * 100) + "%");
        if (nextSearchButton != null) {
            boolean hasMatches = !searchPages.isEmpty();
            nextSearchButton.setEnabled(hasMatches);
            nextSearchButton.setAlpha(hasMatches ? 1.0f : 0.4f);
        }
        if (prevButton != null) {
            boolean canPrev = currentPage > 0;
            prevButton.setEnabled(canPrev);
            prevButton.setAlpha(canPrev ? 1.0f : 0.4f);
        }
        if (nextButton != null) {
            boolean canNext = currentPage < pageCount - 1;
            nextButton.setEnabled(canNext);
            nextButton.setAlpha(canNext ? 1.0f : 0.4f);
        }
    }

    private void updateThumbnailHighlight() {
        if (thumbnailContainer == null) return;
        int childCount = thumbnailContainer.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = thumbnailContainer.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout item = (LinearLayout) child;
                boolean selected = (i == currentPage);
                item.setBackground(backgroundShape(colorSurface(), 8, selected ? colorAccent() : colorBorder()));
                if (item.getChildCount() > 0 && item.getChildAt(0) instanceof TextView) {
                    TextView lbl = (TextView) item.getChildAt(0);
                    lbl.setTextColor(selected ? colorAccent() : colorMuted());
                    lbl.setTypeface(Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
                }
            }
        }
    }

    private void renderPage(int pageIndex) {
        if (renderer == null || pageIndex < 0 || pageIndex >= pageCount) return;
        currentPage = pageIndex;
        Bitmap nextBitmap = null;
        try (PdfRenderer.Page page = renderer.openPage(pageIndex)) {
            int displayWidth = getResources().getDisplayMetrics().widthPixels;
            int availableWidth = isCompact() ? displayWidth - dp(32) : (showThumbnailsPanel ? displayWidth - dp(240) : displayWidth - dp(80));
            int baseWidth = Math.max(dp(300), availableWidth);
            float ratio = (float) page.getHeight() / Math.max(1, page.getWidth());

            int targetWidth = Math.round(baseWidth * zoomScale);
            // Cap at 3600px to avoid memory overflow while preserving razor sharpness
            targetWidth = Math.min(3600, Math.max(dp(280), targetWidth));
            int targetHeight = Math.max(1, Math.round(targetWidth * ratio));

            nextBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
            nextBitmap.eraseColor(Color.WHITE);
            page.render(nextBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            if (displayedBitmap != null && !displayedBitmap.isRecycled()) displayedBitmap.recycle();
            displayedBitmap = nextBitmap;
            if (pageImage != null) pageImage.setImageBitmap(displayedBitmap);
        } catch (Exception e) {
            if (nextBitmap != null && !nextBitmap.isRecycled()) nextBitmap.recycle();
            showToast("Could not render page " + (pageIndex + 1) + ".");
        }
        prefs.edit().putInt(pageKey(currentUri), currentPage).apply();
        updateReaderControls();
        updateThumbnailHighlight();
    }

    private void setPage(int newPage) {
        if (newPage < 0 || newPage >= pageCount) return;
        renderPage(newPage);
        if (!searchPages.isEmpty()) {
            int match = searchPages.indexOf(newPage);
            if (match >= 0) searchResultIndex = match;
        }
    }

    private void searchPdf() {
        if (currentFile == null || searchInput == null) return;
        String query = searchInput.getText().toString().trim();
        if (query.isEmpty()) {
            showToast("Enter a word or phrase to search.");
            return;
        }
        hideKeyboard();
        if (nextSearchButton != null) nextSearchButton.setEnabled(false);
        showToast("Searching document…");
        File file = currentFile;
        ioExecutor.execute(() -> {
            ArrayList<Integer> matches = new ArrayList<>();
            String error = null;
            try (PDDocument document = PDDocument.load(file)) {
                PDFTextStripper stripper = new PDFTextStripper();
                for (int page = 1; page <= document.getNumberOfPages(); page++) {
                    stripper.setStartPage(page);
                    stripper.setEndPage(page);
                    String text = stripper.getText(document);
                    if (text != null && text.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))) {
                        matches.add(page - 1);
                    }
                }
            } catch (Exception e) {
                error = e.getMessage();
            }
            ArrayList<Integer> finalMatches = matches;
            String finalError = error;
            runOnUiThread(() -> {
                if (!reading || currentFile != file) return;
                searchPages.clear();
                searchPages.addAll(finalMatches);
                searchResultIndex = -1;
                updateReaderControls();
                if (finalError != null) {
                    showToast("Search failed. The PDF may be encrypted or unsupported.");
                } else if (finalMatches.isEmpty()) {
                    showToast("No selectable text found for ‘" + query + "’. Scanned PDFs may need OCR.");
                } else {
                    searchResultIndex = 0;
                    setPage(searchPages.get(0));
                    showToast(finalMatches.size() + (finalMatches.size() == 1 ? " matching page" : " matching pages"));
                }
            });
        });
    }

    private void nextSearchResult() {
        if (searchPages.isEmpty()) return;
        searchResultIndex = (searchResultIndex + 1) % searchPages.size();
        setPage(searchPages.get(searchResultIndex));
        showToast("Match " + (searchResultIndex + 1) + " of " + searchPages.size());
    }

    private void hideKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            View focus = getCurrentFocus();
            if (imm != null && focus != null) imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        } catch (Exception ignored) { }
    }

    private String pageKey(Uri uri) {
        return "page_" + md5(uri == null ? "none" : uri.toString());
    }

    private ArrayList<JSONObject> readJsonList(String key) {
        ArrayList<JSONObject> result = new ArrayList<>();
        String raw = prefs.getString(key, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item != null) result.add(item);
            }
        } catch (JSONException ignored) { }
        return result;
    }

    private void writeJsonList(String key, ArrayList<JSONObject> items) {
        JSONArray array = new JSONArray();
        for (JSONObject item : items) array.put(item);
        prefs.edit().putString(key, array.toString()).apply();
    }

    private void upsertRecent(String uri, String name) {
        ArrayList<JSONObject> items = readJsonList(KEY_RECENTS);
        ArrayList<JSONObject> updated = new ArrayList<>();
        try {
            updated.add(new JSONObject().put("uri", uri).put("name", name));
            for (JSONObject old : items) {
                if (!uri.equals(old.optString("uri", "")) && updated.size() < 10) updated.add(old);
            }
        } catch (JSONException ignored) { }
        writeJsonList(KEY_RECENTS, updated);
    }

    private boolean isBookmarked(Uri uri, int page) {
        if (uri == null) return false;
        for (JSONObject item : readJsonList(KEY_BOOKMARKS)) {
            if (uri.toString().equals(item.optString("uri", "")) && page == item.optInt("page", -1)) return true;
        }
        return false;
    }

    private void toggleBookmark() {
        if (currentUri == null) return;
        ArrayList<JSONObject> items = readJsonList(KEY_BOOKMARKS);
        boolean existed = false;
        ArrayList<JSONObject> updated = new ArrayList<>();
        for (JSONObject item : items) {
            boolean same = currentUri.toString().equals(item.optString("uri", "")) && currentPage == item.optInt("page", -1);
            if (same) existed = true; else updated.add(item);
        }
        if (!existed) {
            try {
                updated.add(0, new JSONObject().put("uri", currentUri.toString()).put("name", currentName).put("page", currentPage));
            } catch (JSONException ignored) { }
        }
        writeJsonList(KEY_BOOKMARKS, updated);
        updateReaderControls();
        showToast(existed ? "Bookmark removed" : "Page bookmarked");
    }

    private void goHome() {
        reading = false;
        closeRenderer();
        buildHome();
    }

    private void closeRenderer() {
        thumbRequestId++;
        if (displayedBitmap != null && !displayedBitmap.isRecycled()) displayedBitmap.recycle();
        displayedBitmap = null;
        if (renderer != null) {
            try { renderer.close(); } catch (Exception ignored) { }
        }
        renderer = null;
        if (currentPfd != null) {
            try { currentPfd.close(); } catch (Exception ignored) { }
        }
        currentPfd = null;
        pageImage = null;
        pageCounter = null;
        bookmarkButton = null;
        zoomIndicator = null;
        prevButton = null;
        nextButton = null;
        searchInput = null;
        nextSearchButton = null;
        thumbnailContainer = null;
        thumbnailScroll = null;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // When typing in search input, let all text events pass to the input field
        if (searchInput != null && searchInput.hasFocus()) {
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                searchPdf();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_ESCAPE) {
                hideKeyboard();
                return true;
            }
            return super.onKeyDown(keyCode, event);
        }

        if (reading) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_PAGE_UP) {
                setPage(currentPage - 1);
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_PAGE_DOWN || keyCode == KeyEvent.KEYCODE_SPACE) {
                setPage(currentPage + 1);
                return true;
            }
            if (event.isCtrlPressed()) {
                if (keyCode == KeyEvent.KEYCODE_F) {
                    if (searchInput != null) {
                        searchInput.requestFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
                    }
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_PLUS || keyCode == KeyEvent.KEYCODE_EQUALS) {
                    changeZoom(0.25f);
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_MINUS) {
                    changeZoom(-0.25f);
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_B) {
                    toggleBookmark();
                    return true;
                }
            }
            if (keyCode == KeyEvent.KEYCODE_ESCAPE) {
                hideKeyboard();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (reading) goHome();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        closeRenderer();
        ioExecutor.shutdown();
        super.onDestroy();
    }
}

package app.stillpdf.reader;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
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
import java.io.InputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * StillPDF: a local-first PDF reader. It has no ads, analytics, account system,
 * or network permissions. PDF pages are rendered by Android's PdfRenderer;
 * selectable-text search is powered by PDFBox-Android.
 */
public class MainActivity extends Activity {
    private static final int PICK_PDF_REQUEST = 4107;
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

    private LinearLayout root;
    private ImageView pageImage;
    private TextView pageCounter;
    private TextView bookmarkButton;
    private EditText searchInput;
    private Button nextSearchButton;
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
        buildHome();

        Uri incoming = getIncomingUri(getIntent());
        if (incoming != null) {
            String name = nameFromUri(incoming);
            openPdf(incoming, name, 0);
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

    private int colorBackground() { return Color.parseColor(darkMode ? "#101216" : "#F4F6FA"); }
    private int colorSurface() { return Color.parseColor(darkMode ? "#1A1E25" : "#FFFFFF"); }
    private int colorText() { return Color.parseColor(darkMode ? "#F2F4F8" : "#172033"); }
    private int colorMuted() { return Color.parseColor(darkMode ? "#A7AFBE" : "#667085"); }
    private int colorAccent() { return Color.parseColor("#3867E8"); }
    private int colorBorder() { return Color.parseColor(darkMode ? "#343B48" : "#E2E7F0"); }

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
        button.setTextAllCaps(false);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setPadding(dp(14), dp(8), dp(14), dp(8));
        button.setMinHeight(dp(46));
        button.setBackground(backgroundShape(primary ? colorAccent() : colorSurface(), 14,
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

    private void setPageMargins(View view, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        view.setLayoutParams(params);
    }

    private void buildHome() {
        reading = false;
        root = vertical();
        root.setBackgroundColor(colorBackground());
        root.setPadding(dp(20), dp(12), dp(20), dp(18));

        LinearLayout header = horizontal();
        LinearLayout brand = vertical();
        TextView title = text("StillPDF", 30, true, colorText());
        TextView subtitle = text("Your documents. Your space.", 14, false, colorMuted());
        brand.addView(title, lp(-1, 38));
        brand.addView(subtitle, lp(-1, 26));
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        TextView theme = text(darkMode ? "☀" : "☾", 24, true, colorText());
        theme.setGravity(Gravity.CENTER);
        theme.setBackground(backgroundShape(colorSurface(), 16, colorBorder()));
        header.addView(theme, lp(50, 50));
        theme.setOnClickListener(v -> toggleTheme());
        root.addView(header, lp(-1, -2));

        TextView privacy = text("OFFLINE  •  AD-FREE  •  PRIVATE", 11, true, colorAccent());
        privacy.setLetterSpacing(0.08f);
        LinearLayout.LayoutParams privacyParams = lp(-1, 28);
        privacyParams.topMargin = dp(8);
        root.addView(privacy, privacyParams);

        LinearLayout actionCard = vertical();
        actionCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        actionCard.setBackground(backgroundShape(colorSurface(), 20, colorBorder()));
        TextView actionTitle = text("A quieter place to read", 18, true, colorText());
        TextView actionHint = text("Open a PDF stored on your phone. Your files stay on your device.", 13, false, colorMuted());
        actionHint.setGravity(Gravity.TOP);
        actionCard.addView(actionTitle, lp(-1, 30));
        LinearLayout.LayoutParams hintParams = lp(-1, -2);
        hintParams.topMargin = dp(2);
        actionCard.addView(actionHint, hintParams);
        Button open = makeButton("＋   Open a PDF", true, this::launchPicker);
        LinearLayout.LayoutParams openParams = lp(-1, 48);
        openParams.topMargin = dp(16);
        actionCard.addView(open, openParams);
        String lastUriValue = prefs.getString(KEY_LAST_URI, null);
        if (lastUriValue != null && !lastUriValue.isEmpty()) {
            String lastName = prefs.getString(KEY_LAST_NAME, "Last document");
            Button resume = makeButton("Continue reading  ·  " + lastName, false, () -> {
                try {
                    Uri uri = Uri.parse(lastUriValue);
                    int page = prefs.getInt(pageKey(uri), 0);
                    openPdf(uri, nameFromStored(uri, lastName), page);
                } catch (Exception e) {
                    showToast("Could not reopen that PDF. Please choose it again.");
                }
            });
            LinearLayout.LayoutParams resumeParams = lp(-1, 48);
            resumeParams.topMargin = dp(8);
            actionCard.addView(resume, resumeParams);
        }
        LinearLayout.LayoutParams cardParams = lp(-1, -2);
        cardParams.topMargin = dp(10);
        root.addView(actionCard, cardParams);

        ScrollView listScroll = new ScrollView(this);
        listScroll.setFillViewport(false);
        LinearLayout lists = vertical();

        TextView recentHeader = text("Recent", 19, true, colorText());
        LinearLayout.LayoutParams sectionHeadParams = lp(-1, 38);
        sectionHeadParams.topMargin = dp(12);
        lists.addView(recentHeader, sectionHeadParams);
        ArrayList<JSONObject> recents = readJsonList(KEY_RECENTS);
        if (recents.isEmpty()) {
            lists.addView(text("Your recently opened PDFs will appear here.", 13, false, colorMuted()), lp(-1, 42));
        } else {
            for (JSONObject item : recents) {
                String uriString = item.optString("uri", "");
                String name = item.optString("name", "PDF document");
                if (uriString.isEmpty()) continue;
                addDocumentRow(lists, name, "PDF document  ·  Tap to continue", "›", () -> {
                    Uri uri = Uri.parse(uriString);
                    openPdf(uri, name, prefs.getInt(pageKey(uri), 0));
                });
            }
        }

        TextView bookmarksHeader = text("Bookmarks", 19, true, colorText());
        LinearLayout.LayoutParams bookmarkHeaderParams = lp(-1, 38);
        bookmarkHeaderParams.topMargin = dp(12);
        lists.addView(bookmarksHeader, bookmarkHeaderParams);
        ArrayList<JSONObject> bookmarks = readJsonList(KEY_BOOKMARKS);
        if (bookmarks.isEmpty()) {
            lists.addView(text("Tap the bookmark icon while reading to save a page.", 13, false, colorMuted()), lp(-1, 42));
        } else {
            for (JSONObject item : bookmarks) {
                String uriString = item.optString("uri", "");
                String name = item.optString("name", "PDF document");
                int page = Math.max(0, item.optInt("page", 0));
                if (uriString.isEmpty()) continue;
                addDocumentRow(lists, name, "Bookmarked page  " + (page + 1), "↗", () -> {
                    Uri uri = Uri.parse(uriString);
                    openPdf(uri, name, page);
                });
            }
        }

        TextView footer = text("No ads. No accounts. No tracking.", 12, false, colorMuted());
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerParams = lp(-1, 48);
        footerParams.topMargin = dp(12);
        lists.addView(footer, footerParams);

        listScroll.addView(lists);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, 0, 1);
        scrollParams.topMargin = dp(2);
        root.addView(listScroll, scrollParams);
        setContentView(root);
        applySystemBars();
    }

    private void addDocumentRow(LinearLayout parent, String name, String subtitle, String glyph, Runnable click) {
        LinearLayout row = horizontal();
        row.setPadding(dp(12), dp(6), dp(12), dp(6));
        row.setBackground(backgroundShape(colorSurface(), 15, colorBorder()));
        LinearLayout words = vertical();
        TextView title = text(name, 14, true, colorText());
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        TextView caption = text(subtitle, 12, false, colorMuted());
        words.addView(title, lp(-1, 24));
        words.addView(caption, lp(-1, 20));
        row.addView(words, new LinearLayout.LayoutParams(0, -2, 1));
        TextView arrow = text(glyph, 22, true, colorAccent());
        arrow.setGravity(Gravity.CENTER);
        row.addView(arrow, lp(34, 42));
        row.setOnClickListener(v -> click.run());
        LinearLayout.LayoutParams params = lp(-1, 58);
        params.bottomMargin = dp(7);
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
            // Some document providers do not support persisted grants; opening still works now.
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
        root = vertical();
        root.setBackgroundColor(colorBackground());
        root.setPadding(dp(20), dp(20), dp(20), dp(20));
        TextView loading = text("Opening PDF…", 18, true, colorText());
        loading.setGravity(Gravity.CENTER);
        root.addView(loading, new LinearLayout.LayoutParams(-1, -1));
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
            // Do not leave a partial file that might be mistaken for a complete PDF.
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
            // Replacement is optional; rename below will fail clearly if blocked.
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
        root = vertical();
        root.setBackgroundColor(colorBackground());

        LinearLayout top = horizontal();
        top.setPadding(dp(12), 0, dp(12), 0);
        TextView back = text("‹", 32, false, colorText());
        back.setGravity(Gravity.CENTER);
        top.addView(back, lp(42, 54));
        back.setOnClickListener(v -> goHome());
        TextView title = text(currentName, 15, true, colorText());
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(54), 1));
        bookmarkButton = text(isBookmarked(currentUri, currentPage) ? "★" : "☆", 25, true, colorAccent());
        bookmarkButton.setGravity(Gravity.CENTER);
        top.addView(bookmarkButton, lp(46, 54));
        bookmarkButton.setOnClickListener(v -> toggleBookmark());
        TextView theme = text(darkMode ? "☀" : "☾", 20, true, colorText());
        theme.setGravity(Gravity.CENTER);
        top.addView(theme, lp(38, 54));
        theme.setOnClickListener(v -> toggleTheme());
        root.addView(top, lp(-1, 54));

        LinearLayout searchRow = horizontal();
        searchRow.setPadding(dp(10), dp(7), dp(10), dp(7));
        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setTextSize(14);
        searchInput.setHint("Search text in this PDF");
        searchInput.setHintTextColor(colorMuted());
        searchInput.setTextColor(colorText());
        searchInput.setPadding(dp(12), 0, dp(12), 0);
        searchInput.setBackground(backgroundShape(colorSurface(), 12, colorBorder()));
        searchRow.addView(searchInput, new LinearLayout.LayoutParams(0, dp(42), 1));
        Button find = makeButton("Find", true, this::searchPdf);
        LinearLayout.LayoutParams findParams = lp(66, 42);
        findParams.leftMargin = dp(7);
        searchRow.addView(find, findParams);
        nextSearchButton = makeButton("Next", false, this::nextSearchResult);
        LinearLayout.LayoutParams nextParams = lp(65, 42);
        nextParams.leftMargin = dp(6);
        searchRow.addView(nextSearchButton, nextParams);
        root.addView(searchRow, lp(-1, 56));

        ScrollView pageScroll = new ScrollView(this);
        pageScroll.setFillViewport(true);
        pageScroll.setBackgroundColor(darkMode ? Color.parseColor("#222731") : Color.parseColor("#E9EDF5"));
        LinearLayout pageHolder = vertical();
        pageHolder.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        pageHolder.setPadding(dp(8), dp(12), dp(8), dp(12));
        pageImage = new ImageView(this);
        pageImage.setAdjustViewBounds(true);
        pageImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        pageImage.setBackgroundColor(Color.WHITE);
        pageHolder.addView(pageImage, new LinearLayout.LayoutParams(-1, -2));
        pageScroll.addView(pageHolder, new ScrollView.LayoutParams(-1, -2));
        root.addView(pageScroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout nav = horizontal();
        nav.setPadding(dp(14), dp(8), dp(14), dp(8));
        Button previous = makeButton("←  Previous", false, () -> setPage(currentPage - 1));
        nav.addView(previous, new LinearLayout.LayoutParams(0, dp(44), 1));
        pageCounter = text(pageLabel(), 13, true, colorText());
        pageCounter.setGravity(Gravity.CENTER);
        nav.addView(pageCounter, new LinearLayout.LayoutParams(0, dp(44), 1));
        Button next = makeButton("Next  →", true, () -> setPage(currentPage + 1));
        nav.addView(next, new LinearLayout.LayoutParams(0, dp(44), 1));
        root.addView(nav, lp(-1, 60));

        setContentView(root);
        applySystemBars();
        updateReaderControls();
    }

    private String pageLabel() {
        return pageCount > 0 ? (currentPage + 1) + " / " + pageCount : "— / —";
    }

    private void updateReaderControls() {
        if (pageCounter != null) pageCounter.setText(pageLabel());
        if (bookmarkButton != null) bookmarkButton.setText(isBookmarked(currentUri, currentPage) ? "★" : "☆");
        if (nextSearchButton != null) nextSearchButton.setEnabled(!searchPages.isEmpty());
    }

    private void renderPage(int pageIndex) {
        if (renderer == null || pageIndex < 0 || pageIndex >= pageCount) return;
        currentPage = pageIndex;
        Bitmap nextBitmap = null;
        try (PdfRenderer.Page page = renderer.openPage(pageIndex)) {
            int maxWidth = Math.max(dp(300), getResources().getDisplayMetrics().widthPixels - dp(56));
            float ratio = (float) page.getHeight() / Math.max(1, page.getWidth());
            int width = Math.min(maxWidth, 1800);
            int height = Math.max(1, Math.round(width * ratio));
            nextBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
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
        showToast("Searching the document…");
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
        searchInput = null;
        nextSearchButton = null;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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

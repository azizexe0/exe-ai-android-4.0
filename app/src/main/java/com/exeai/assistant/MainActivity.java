package com.exeai.assistant;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final int SPEECH = 41;
    private static final int IMAGE_PICK = 42;
    private static final int CAMERA = 43;
    private static final int IMPORT_QA = 44;

    private static final int RED = Color.rgb(255, 45, 55);
    private LinearLayout chat;
    private EditText input;
    private ScrollView scroll;
    private DB db;
    private TextToSpeech tts;
    private SharedPreferences prefs;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    private boolean ttsReady = false;
    private boolean voiceEnabled = true;
    private String selectedVoiceName = "";
    private float speechRate = 1.0f;
    private float speechPitch = 1.0f;
    private String lastImageText = "";

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(9, 9, 9));
        getWindow().setNavigationBarColor(Color.rgb(9, 9, 9));

        prefs = getSharedPreferences("exe_ai_settings", MODE_PRIVATE);
        voiceEnabled = prefs.getBoolean("voice_enabled", true);
        selectedVoiceName = prefs.getString("voice_name", "");
        speechRate = prefs.getFloat("speech_rate", 1.0f);
        speechPitch = prefs.getFloat("speech_pitch", 1.0f);

        db = new DB(this);
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true;
                tts.setLanguage(new Locale("tr", "TR"));
                applySavedVoice();
            }
        });

        buildUI();
        addMessage("EXE AI 4.0 hazır. Yazılı sohbet, sesli giriş, kamera fotoğrafı ve kalıcı hafıza kullanılabilir.\nGeliştiricim: Aziz EXE.", false);
    }

    private void buildUI() {
        LinearLayout base = new LinearLayout(this);
        base.setOrientation(LinearLayout.VERTICAL);
        base.setBackgroundColor(Color.rgb(10, 10, 10));

        TextView title = new TextView(this);
        title.setText("EXE AI 4.0");
        title.setTextColor(RED);
        title.setTextSize(26);
        title.setTypeface(null, 1);
        title.setPadding(18, 18, 12, 3);

        TextView sub = new TextView(this);
        sub.setText("PERSONAL AI ASSISTANT  •  RED / BLACK");
        sub.setTextColor(0xffaaaaaa);
        sub.setTextSize(10);
        sub.setPadding(20, 0, 12, 12);
        base.addView(title);
        base.addView(sub);

        scroll = new ScrollView(this);
        chat = new LinearLayout(this);
        chat.setOrientation(LinearLayout.VERTICAL);
        chat.setPadding(12, 10, 12, 10);
        scroll.addView(chat);
        base.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout tools = new LinearLayout(this);
        tools.setPadding(6, 4, 6, 4);
        addTool(tools, "🎙 Sesli", v -> listen());
        addTool(tools, "📷 Kamera", v -> openCamera());
        addTool(tools, "🖼 Görsel", v -> pickImage());
        addTool(tools, "⚙ Ayarlar", v -> showSettings());
        base.addView(tools);

        LinearLayout extraTools = new LinearLayout(this);
        extraTools.setPadding(6, 0, 6, 4);
        addTool(extraTools, "📚 Bilgi paketi", v -> importKnowledge());
        addTool(extraTools, "▶ YouTube", v -> launchApp("youtube"));
        addTool(extraTools, "⛏ Minecraft", v -> launchApp("minecraft"));
        addTool(extraTools, "⚙ Sistem", v -> openSettings());
        base.addView(extraTools);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setPadding(8, 8, 8, 12);
        input = new EditText(this);
        input.setSingleLine(false);
        input.setMinLines(1);
        input.setMaxLines(4);
        input.setHint("EXE AI'ye yaz...");
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(0xff888888);
        input.setPadding(12, 10, 12, 10);
        input.setBackgroundTintList(android.content.res.ColorStateList.valueOf(RED));
        bottom.addView(input, new LinearLayout.LayoutParams(0, -2, 1));

        Button send = new Button(this);
        send.setText("GÖNDER");
        send.setTextColor(Color.WHITE);
        send.setAllCaps(false);
        send.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xffb00020));
        send.setOnClickListener(v -> send());
        bottom.addView(send);
        base.addView(bottom);

        setContentView(base);
    }

    private void addTool(LinearLayout row, String label, View.OnClickListener click) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(10);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setPadding(2, 2, 2, 2);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff242424));
        b.setOnClickListener(click);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
        lp.setMargins(2, 0, 2, 0);
        row.addView(b, lp);
    }

    private void addMessage(String message, boolean user) {
        TextView t = new TextView(this);
        t.setText((user ? "SEN  ›  " : "EXE  ›  ") + message);
        t.setTextColor(user ? 0xffffb5b5 : Color.WHITE);
        t.setTextSize(15);
        t.setPadding(14, 12, 14, 12);
        t.setBackgroundColor(user ? 0xff351316 : 0xff202020);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 5, 0, 5);
        chat.addView(t, lp);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private void send() {
        String q = input.getText().toString().trim();
        if (q.isEmpty()) return;
        input.setText("");
        addMessage(q, true);
        hideKeyboard();

        String learningQuery = getLearningQuery(q);
        if (learningQuery != null) {
            if (learningQuery.isEmpty()) {
                reply("Öğrenmek istediğin konuyu komuttan sonra yaz. Örnek: *** RAM nedir?");
                return;
            }
            addMessage("İnternette ücretsiz kaynak aranıyor...", false);
            worker.execute(() -> learnFromInternet(learningQuery));
            return;
        }

        String answer = answer(q);
        reply(answer);
    }

    // Supported formats: "*** konu", "### konu", and also "***konu" / "###konu".
    private String getLearningQuery(String q) {
        String trimmed = q.trim();
        if (trimmed.startsWith("***") || trimmed.startsWith("###")) {
            return trimmed.substring(3).trim();
        }
        String lower = trimmed.toLowerCase(new Locale("tr", "TR"));
        if (lower.startsWith("öğren:") || lower.startsWith("ogren:")) {
            return trimmed.substring(trimmed.indexOf(':') + 1).trim();
        }
        return null;
    }

    private void learnFromInternet(String query) {
        HttpURLConnection connection = null;
        try {
            String searchUrl = "https://tr.wikipedia.org/w/api.php?action=query&list=search&srsearch="
                    + URLEncoder.encode(query, "UTF-8")
                    + "&format=json&utf8=1&srlimit=1";
            JSONObject searchJson = new JSONObject(httpGet(searchUrl));
            JSONArray results = searchJson.getJSONObject("query").getJSONArray("search");
            if (results.length() == 0) {
                postReply("Bu konu için ücretsiz kaynaklarda uygun bir sonuç bulamadım. Başka kelimelerle deneyebilirsin.");
                return;
            }

            JSONObject first = results.getJSONObject(0);
            String title = first.getString("title");
            String page = URLEncoder.encode(title.replace(' ', '_'), "UTF-8").replace("+", "%20");
            String summaryUrl = "https://tr.wikipedia.org/api/rest_v1/page/summary/" + page;
            JSONObject summary = new JSONObject(httpGet(summaryUrl));

            String answer = summary.optString("extract", "").trim();
            String source = summary.optString("content_urls", "");
            String sourceUrl = "https://tr.wikipedia.org/wiki/" + page;

            if (answer.isEmpty()) {
                answer = first.optString("snippet", "").replaceAll("<[^>]*>", "").trim();
            }
            if (answer.isEmpty()) {
                postReply("Kaynak bulundu ama okunabilir bir açıklama alınamadı.");
                return;
            }

            db.saveLearned(query, answer, sourceUrl);
            postReply("Öğrendim ve kalıcı hafızama kaydettim:\n" + answer + "\n\nKaynak: " + sourceUrl);
        } catch (Exception e) {
            postReply("İnternetten öğrenemedim. Bağlantını kontrol edip tekrar dene. Hata: " + safeError(e));
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private String httpGet(String address) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(address).openConnection();
        c.setRequestMethod("GET");
        c.setConnectTimeout(12000);
        c.setReadTimeout(15000);
        c.setRequestProperty("User-Agent", "EXE-AI-Android/4.0 (educational assistant)");
        c.setRequestProperty("Accept", "application/json");
        try (InputStream in = c.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) out.append(line);
            return out.toString();
        } finally {
            c.disconnect();
        }
    }

    private String safeError(Exception e) {
        String m = e.getMessage();
        return m == null ? e.getClass().getSimpleName() : m;
    }

    private void postReply(String text) {
        mainHandler.post(() -> reply(text));
    }

    private void reply(String text) {
        addMessage(text, false);
        if (tts != null && ttsReady && voiceEnabled) {
            tts.setSpeechRate(speechRate);
            tts.setPitch(speechPitch);
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "exe_reply");
        }
    }

    private String answer(String q) {
        String s = q.toLowerCase(new Locale("tr", "TR")).trim();

        if (s.contains("seni kim yaptı") || s.contains("seni kim yapti")
                || s.contains("kim kodladı") || s.contains("kim kodladi")) {
            return "Beni Aziz EXE kodladı. Ben EXE AI'yım!";
        }
        if (s.equals("adın ne") || s.equals("adin ne") || s.contains("ismin ne")) {
            return "Benim adım EXE AI. Kişisel yapay zekâ asistanınım.";
        }
        if (s.contains("merhaba") || s.equals("selam") || s.startsWith("selam ")) {
            return "Merhaba! Bugün ne konuşalım?";
        }
        if (s.contains("nasılsın") || s.contains("nasilsin")) {
            return "İyiyim, teşekkür ederim! Seninle sohbet etmeye hazırım. Sen nasılsın?";
        }
        if (s.contains("ne yapıyorsun") || s.contains("ne yapiyorsun")) {
            return "Senin mesajını okuyup yardımcı olmaya çalışıyorum. İstersen sohbet edebiliriz veya bir şey araştırabiliriz.";
        }
        if (s.contains("teşekkür") || s.contains("tesekkur")) {
            return "Rica ederim! Başka bir konuda da yardımcı olabilirim.";
        }
        if (s.contains("saat kaç") || s.contains("saat kac")) {
            return "Cihaz saati: " + new SimpleDateFormat("HH:mm", new Locale("tr", "TR")).format(new Date());
        }
        if (s.contains("youtube aç") || s.equals("youtube")) {
            launchApp("youtube");
            return "YouTube'u açmayı denedim.";
        }
        if (s.contains("minecraft aç") || s.equals("minecraft")) {
            launchApp("minecraft");
            return "Minecraft'ı açmayı denedim.";
        }
        if (s.contains("chrome aç") || s.equals("chrome")) {
            launchApp("chrome");
            return "Chrome'u açmayı denedim.";
        }
        if (s.contains("wifi") || s.contains("wi-fi")) {
            startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
            return "Wi-Fi ayarlarını açtım.";
        }
        if (s.contains("bluetooth")) {
            startActivity(new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS));
            return "Bluetooth ayarlarını açtım.";
        }
        if (s.contains("yardım") || s.equals("help")) {
            return "Komutlar:\n• *** konu — internetten öğren ve kaydet\n• ### konu — internetten öğren ve kaydet\n• 🎙 Sesli — konuşarak mesaj gönder\n• 📷 Kamera — fotoğraf çek ve yazıları oku\n• 📚 Bilgi paketi — soru-cevap dosyası içe aktar\n• ⚙ Ayarlar — sesini aç, kapat veya değiştir.";
        }

        String learned = db.findLearned(q);
        if (learned != null) return "Hafızamda kayıtlı bilgi:\n" + learned;

        String qa = db.knowledge(q);
        if (qa != null) return qa;

        if (!lastImageText.isEmpty() && (s.contains("resim") || s.contains("fotoğraf") || s.contains("fotograf")
                || s.contains("görsel") || s.contains("gorsel") || s.contains("kamerada"))) {
            return "Fotoğraftan okuyabildiğim yazılar:\n" + lastImageText
                    + "\n\nNot: Bu sürüm fotoğraftaki yazıları OCR ile okuyabilir; henüz tüm nesneleri ve sahneleri anlayan bir görsel dil modeli içermez.";
        }

        return "Bunu seninle konuşmak isterim! Şu an çevrimdışı sohbet bilgim sınırlı. Bir konuyu internetten öğrenmem için başına *** veya ### koyabilirsin. Öğrendiğim bilgiyi kalıcı hafızama kaydederim.";
    }

    private void launchApp(String name) {
        String[] packages;
        if (name.equals("youtube")) packages = new String[]{"com.google.android.youtube"};
        else if (name.equals("minecraft")) packages = new String[]{"com.mojang.minecraftpe", "com.mojang.minecrafttrialpe"};
        else packages = new String[]{"com.android.chrome"};

        for (String p : packages) {
            Intent i = getPackageManager().getLaunchIntentForPackage(p);
            if (i != null) {
                startActivity(i);
                return;
            }
        }
        Toast.makeText(this, "Uygulama bulunamadı veya kurulu değil.", Toast.LENGTH_SHORT).show();
    }

    private void openSettings() {
        startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
    }

    private void listen() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR");
        i.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "EXE AI'ye konuş");
        try {
            startActivityForResult(i, SPEECH);
        } catch (Exception e) {
            addMessage("Bu cihazda konuşma tanıma servisi bulunamadı.", false);
        }
    }

    private void openCamera() {
        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (i.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(i, CAMERA);
        } else {
            addMessage("Bu cihazda kamera uygulaması bulunamadı.", false);
        }
    }

    private void pickImage() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        startActivityForResult(i, IMAGE_PICK);
    }

    private void processImage(Uri uri) {
        try {
            InputImage image = InputImage.fromFilePath(this, uri);
            runOcr(image);
        } catch (Exception e) {
            addMessage("Görsel açılamadı: " + safeError(e), false);
        }
    }

    private void processBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            addMessage("Kameradan fotoğraf alınamadı. Tekrar deneyebilirsin.", false);
            return;
        }
        runOcr(InputImage.fromBitmap(bitmap, 0));
    }

    private void runOcr(InputImage image) {
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(image)
                .addOnSuccessListener(result -> {
                    lastImageText = result.getText() == null ? "" : result.getText().trim();
                    if (lastImageText.isEmpty()) {
                        addMessage("Fotoğrafta okunabilir yazı bulamadım. Bu sürüm fotoğraflardaki yazıları okuyabilir.", false);
                    } else {
                        addMessage("Fotoğraftan okuduğum yazılar:\n" + lastImageText
                                + "\n\nŞimdi bu yazılar hakkında soru sorabilirsin.", false);
                    }
                    recognizer.close();
                })
                .addOnFailureListener(e -> {
                    addMessage("Görsel okuma başarısız: " + safeError(e), false);
                    recognizer.close();
                });
    }

    private void importKnowledge() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        try {
            startActivityForResult(i, IMPORT_QA);
        } catch (Exception e) {
            addMessage("Dosya seçici açılamadı.", false);
        }
    }

    private void importTsv(Uri uri) {
        worker.execute(() -> {
            int imported = 0;
            int skipped = 0;
            SQLiteDatabase writable = db.getWritableDatabase();
            try (InputStream in = getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                if (in == null) throw new IllegalStateException("Dosya okunamadı");
                writable.beginTransaction();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty() || line.startsWith("#")) continue;
                    String[] parts = line.split("\\t", 3);
                    if (parts.length < 2) {}
           

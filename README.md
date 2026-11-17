# EXE AI 4.0

Android Studio / GitHub Actions için Java tabanlı Android projesi.

## Bu pakette çalışan temel özellikler

- Kırmızı-siyah sohbet arayüzü.
- Yazılı sohbet ve temel yerel sohbet yanıtları.
- Android konuşma tanıma servisiyle sesli giriş.
- Android Text-to-Speech ile sesli yanıt.
- Ayarlardan sesli yanıtı açıp kapatma.
- Cihazda yüklü Türkçe TTS sesleri arasından seçim; konuşma hızı ve tonu ayarı.
- Kamera uygulamasını açıp fotoğraf alma; fotoğraftaki yazıları ML Kit OCR ile okuma.
- Galeriden görsel seçip OCR yapma.
- `*** konu` ve `### konu` komutlarıyla Türkçe Wikipedia'da ücretsiz arama, kısa bilgi alma ve SQLite'a kalıcı kaydetme.
- Öğrenilen bilgileri, uygulama verileri silinmediği sürece cihazda tutma.
- TSV dosyasıyla soru-cevap veri tabanına toplu içe aktarma.
- YouTube, Minecraft, Chrome ve Android ayarlarını açma.
- GitHub Actions ile APK derleme.

## Öğrenme komutları

İkisi de aynı işi yapar:

- `*** RAM nedir?`
- `### RAM nedir?`

Üç yıldız veya üç diyezden sonra boşluk koyabilirsin; boşluk olmadan da çalışır.
İnternet gerekir. Bu örnek sürüm ücretsiz Türkçe Wikipedia API'sini kullanır; bütün interneti taramaz ve kaynakların erişim/kullanım sınırları olabilir.

## 2 milyon soru-cevap havuzu hakkında

Veri tabanı şeması ve TSV içe aktarma özelliği büyük veri kümelerini parça parça eklemek için hazırlanmıştır. **Bu ZIP'in içinde 2 milyon doğrulanmış soru-cevap bulunmuyor.** Pakette yalnızca örnek kayıtlar vardır. 2 milyon kaliteli Türkçe soru-cevap üretmek/derlemek ayrı bir veri hazırlama işidir; tekrarlı veya uydurma kayıtlarla sayı doldurulmamıştır.

TSV biçimi:
`Soru<TAB>Cevap<TAB>Kategori`

Uygulamadaki **Bilgi paketi** düğmesiyle `.tsv` dosyanı seçebilirsin. Büyük dosyalar uzun sürebilir ve yeterli boş depolama gerekir.

## Kamera ve sohbet sınırı

Bu sürüm kamerayı açar, fotoğraf alır ve fotoğraftaki yazıları OCR ile çıkarır. Sürekli canlı video analizi veya fotoğraftaki her nesneyi anlayan bir görsel dil modeli içermez.

Yerel sohbet cevapları temel kurallara ve veri tabanına dayanır; tam bir büyük dil modeli değildir. Daha serbest ve kapsamlı sohbet için ayrıca uyumlu bir yerel model ve model çalıştırma altyapısı gerekir.

## Ses sınırı

Sesli giriş Android'in konuşma tanıma servisini kullanır. `EXTRA_PREFER_OFFLINE` yalnızca tercihtir; cihazda Türkçe çevrimdışı tanıma paketi yoksa internet gerekebilir. TTS ses seçenekleri cihazda kurulu motor ve ses paketlerine bağlıdır.

## Kalıcı hafıza

Öğrenilen cevaplar ve kullanıcı kayıtları SQLite veritabanında tutulur. Uygulama kapatılıp cihaz yeniden başlatılsa da kalır. Uygulama verileri temizlenirse veya uygulama kaldırılırsa Android bu verileri silebilir. Depolama dolarsa yeni kayıt başarısız olabilir; uygulama otomatik olarak eski bilgileri silmez.

## GitHub Actions ile APK

1. ZIP'i aç.
2. İçindeki dosya ve klasörleri GitHub deposunun köküne yükle.
3. `Actions` sekmesine gir.
4. `Build EXE AI 4.0 APK` iş akışını seç.
5. `Run workflow` düğmesine bas.
6. İşlem bitince `EXE-AI-4.0-APK` artifact'ini indir.

Derleme için Java 17 ve Android SDK, GitHub Actions tarafından kurulur.

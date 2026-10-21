# Memasang Clear TV Islamic, langkah demi langkah

<p><a href="INSTALL.md">English</a> · <b>Bahasa Melayu</b> · <a href="../README.ms.md">Kembali ke README</a></p>

Panduan ini untuk sesiapa sahaja. Anda tidak perlu tahu coding, dan tidak perlu Java atau Android SDK: anda
cuma salin beberapa arahan dari halaman ini ke komputer. Ia mengambil masa lebih kurang 30 minit, dan semuanya
boleh dibatalkan.

**Apa yang diperlukan**

- **Android TV atau Google TV**, Android 8 ke atas (Sony, TCL, Sharp, Philips, Xiaomi, Hisense model Android,
  Chromecast with Google TV, Mi Box...). TV Samsung, LG dan Roku tidak boleh.
- **Komputer** (Windows atau Mac) yang guna **Wi-Fi yang sama** dengan TV.
- Pilihan: **rakaman azan** kegemaran anda dalam bentuk fail MP3.

Dalam arahan di bawah, tukar `192.168.1.50` kepada alamat TV anda sendiri (langkah 3). Arahan untuk Mac dan
Windows ditunjukkan berasingan: guna yang sesuai dengan komputer anda.

---

## Bahagian 1: di TV

### 1. Pastikan TV anda Android TV

Di TV: **Settings → Device Preferences → About**. Cari **Android TV OS version**: mesti **8 atau lebih**. Kalau
tiada perkataan "Android TV" langsung, TV anda tidak boleh pasang Clear TV Islamic; tetapi sebatang Google TV stick di
port HDMI boleh.

### 2. Hidupkan Developer options dan USB debugging

1. Masih di **About**, scroll ke bawah sampai jumpa **Build**.
2. Tekan **OK pada Build tujuh kali**. TV akan kata *"You are now a developer"*. (Kalau diminta PIN, masukkan.)
3. Tekan **Back** sekali. Ada menu baharu, **Developer options**, dalam Device Preferences.
4. Buka **Developer options** dan hidupkan **USB debugging**. Kalau ada juga **Network debugging** atau
   **ADB debugging**, hidupkan sekali.

Tiada kabel diperlukan: semuanya melalui Wi-Fi. Sesetengah TV memutuskan kuasa ke port USB bila USB debugging
hidup, jadi speaker atau pendrive USB mungkin senyap sehingga ia dimatikan.

### 3. Cari alamat TV dalam rangkaian

**Settings → Device Preferences → About → Status → IP address** (atau **Settings → Network & Internet →**
Wi-Fi anda). Catat nombor yang rupanya seperti `192.168.1.50` atau `10.0.0.23`. Abaikan nombor panjang yang ada
titik bertindih.

---

## Bahagian 2: di komputer

### 4. Pasang Python (sekali sahaja)

- **Mac:** buka **Terminal** (tekan Cmd + Space, taip *Terminal*, tekan Enter) dan taip `python3 --version`.
  Kalau keluar nombor versi, ia sudah ada. Kalau tidak, pasang dari
  [python.org/downloads](https://www.python.org/downloads/).
- **Windows:** pasang dari [python.org/downloads](https://www.python.org/downloads/). Di skrin pertama
  pemasang, **tandakan "Add python.exe to PATH"**.

### 5. Muat turun Clear TV Islamic

1. Buka [github.com/Pokka99/cleartv-Islamic](https://github.com/Pokka99/cleartv-Islamic).
2. Tekan butang hijau **Code**, kemudian **Download ZIP**.
3. Unzip ke folder **Documents**. Sekarang ada folder bernama `cleartv-Islamic-main`.

### 6. Buka terminal dalam folder itu

- **Mac:** dalam Terminal, taip:
  ```bash
  cd ~/Documents/cleartv-Islamic-main
  ```
- **Windows:** buka folder `cleartv-Islamic-main` dalam File Explorer, klik bar alamat, taip `powershell` dan
  tekan Enter. Kemudian benarkan skrip berjalan dalam tetingkap ini:
  ```powershell
  Set-ExecutionPolicy -Scope Process Bypass
  ```

Biarkan tetingkap ini terbuka untuk semua langkah seterusnya.

### 7. Sediakan alat (sekali sahaja)

- **Mac:**
  ```bash
  python3 -m venv tools/venv && tools/venv/bin/pip install -r tools/requirements.txt
  ```
- **Windows:**
  ```powershell
  py -m venv tools\venv; tools\venv\Scripts\pip install -r tools\requirements.txt
  ```

### 8. Sambung ke TV

Beritahu alat di mana TV anda, kemudian sambung:

- **Mac:**
  ```bash
  export TV_HOST=192.168.1.50
  tools/venv/bin/python -I tools/tv.py --probe
  ```
- **Windows:**
  ```powershell
  $env:TV_HOST = "192.168.1.50"
  tools\venv\Scripts\python -I tools\tv.py --probe
  ```

**Tengok TV:** keluar kotak **"Allow USB debugging?"**. Tandakan **Always allow from this computer** dan tekan
**OK**. Kemudian jalankan arahan kedua sekali lagi.

Berjaya bila keluar enam baris, dari `ro.product.model = ...` hingga `ro.product.manufacturer = ...`.
Kalau keluar *timeout* atau *connection refused*, USB debugging belum hidup atau alamat salah (langkah 2 dan 3).

### 9. Simpan salinan senarai aplikasi TV (untuk berjaga-jaga)

- **Mac:** `tools/venv/bin/python -I tools/tv.py 'pm list packages' > aplikasi-tv-saya.txt`
- **Windows:** `tools\venv\Scripts\python -I tools\tv.py 'pm list packages' > aplikasi-tv-saya.txt`

---

## Bahagian 3: pasang Clear TV Islamic

### 10. Pasang dan jadikan skrin utama

- **Mac:**
  ```bash
  APK=dist/clear-tv-islamic.apk launcher/build-and-install.sh
  ```
- **Windows:**
  ```powershell
  launcher\build-and-install.ps1 -Apk dist\clear-tv-islamic.apk
  ```

Skrin TV akan bertukar beberapa kali. Ia selesai bila keluar
**`Installed. Press Home on the remote to see it.`** Tekan **Home** pada remote: Clear TV Islamic kini skrin utama anda.

Kalau ada apa-apa yang tidak kena, skrip ini sendiri akan pulangkan skrin utama asal TV dan beritahu sebabnya.

### 11. Buat tetapan anda sendiri ("theme")

1. Salin folder `themes/islamik` dan namakan salinannya `theme-saya` (dalam folder `cleartv-Islamic-main`
   yang sama).
2. Buka `theme-saya/config.json` dengan editor teks (TextEdit di Mac, Notepad di Windows). Rupanya begini:
   ```json
   {
     "prayer_zone": "TRG04",
     "azan": true,
     "reduced_motion": true
   }
   ```
3. Tukar **`TRG04`** kepada **zon JAKIM** anda. Cari di [e-solat.gov.my](https://www.e-solat.gov.my): senarai
   zon ada kod seperti `WLY01` (Kuala Lumpur, Putrajaya), `SGR01` (Gombak, Petaling, Sepang, Hulu Langat,
   Hulu Selangor, Shah Alam), `JHR02` (Johor Bahru, Kota Tinggi, Mersing, Kulai) atau `TRG04` (Kemaman, Dungun).
4. Pilihan, untuk memilih empat aplikasi di dock, tambah satu baris dengan nama pakejnya, contohnya:
   `"dock_apps": ["com.google.android.youtube.tv", "com.netflix.ninja", "com.tm.unifitv", "com.amazon.amazonvideo.livingroom"],`
   Anda juga boleh susun aplikasi kemudian dengan remote: tekan lama **OK** pada aplikasi, pilih **Move**.

### 12. Pilih azan anda (pilihan)

`theme-saya` sudah ada azan: **`azan.mp3`** (Ustaz Asri Ibrahim, Rabbani) untuk Zohor, Asar, Maghrib dan
Isyak, dan **`azan-subuh.mp3`** untuk Subuh. Untuk suara lain, gantikan fail itu dengan rakaman anda sendiri,
dengan nama yang sama tepat.

### 13. Tambah gambar masjid (pilihan)

- **Mac:** `python3 tools/masjid-photos.py theme-saya`
- **Windows:** `py tools\masjid-photos.py theme-saya`

Ini memuat turun 13 gambar (Masjidil Haram, Kaabah, Masjid Nabawi, Quba, Masjid Kristal, Masjid Putra, Istanbul,
Brunei dan lain-lain, lebih kurang 9 MB) dari Wikimedia Commons ke `theme-saya/masjid`.

### 14. Hantar tetapan ke TV

- **Mac:** `tools/theme.sh theme-saya`
- **Windows:** `tools\theme.ps1 theme-saya`

Dengan gambar dan fail azan, ini mungkin mengambil beberapa minit. Home akan restart dengan waktu solat di bawah
jam. Waktu solat setahun sampai dari JAKIM dalam masa seminit (TV perlu internet).

### 15. Hidupkan screensaver Islamik

- **Mac:** `tools/venv/bin/python -I tools/tv.py 'settings put secure screensaver_components com.gathin.tvhome/.ClockDream'`
- **Windows:** `tools\venv\Scripts\python -I tools\tv.py 'settings put secure screensaver_components com.gathin.tvhome/.ClockDream'`

Sekarang, bila TV berehat, jam emas, waktu solat dan ayat Al-Quran keluar atas gambar masjid.

### 16. Biar TV bersihkan dirinya sendiri (pilihan)

Setiap 15 hari Clear TV Islamic membuang fail sisa aplikasi. Ia minta kebenaran TV sekali sahaja. Dengan seseorang di
depan TV, jalankan:

- **Mac:** `tools/venv/bin/python -I tools/tv.py 'touch /sdcard/Android/data/com.gathin.tvhome/files/clean-now'`
- **Windows:** `tools\venv\Scripts\python -I tools\tv.py 'touch /sdcard/Android/data/com.gathin.tvhome/files/clean-now'`

Tekan **Home** dan tunggu 20 saat. Bila TV tanya **"Allow USB debugging?"**, tandakan **Always allow** dan tekan
**OK**. Supaya ia terus berfungsi, biarkan USB debugging hidup.

**Siap.** Alhamdulillah.

---

## Membuang iklan dan bloat (pilihan, untuk yang berhati-hati)

Setiap jenama meletakkan aplikasi penjejakan dan iklan sendiri dalam TV. Folder [examples](../examples) ada
senarai yang telah diuji untuk banyak model TV. Mematikan aplikasi adalah selamat (tiada apa-apa dipadam, dan
satu arahan menghidupkannya semula), tetapi matikan hanya yang anda faham, sedikit demi sedikit, dan selepas
setiap kumpulan pastikan Home, remote, Wi-Fi dan HDMI masih berfungsi. Panduan penuh dalam
[CLEANYOURTV-README.md](CLEANYOURTV-README.md) (bahasa Inggeris). Kalau tidak mahu buat sendiri, pembantu AI
seperti Claude Code boleh buatkan bersama anda dengan selamat: lihat [AGENTS.md](../AGENTS.md).

## Membatalkan semuanya

- **Kembali ke skrin utama asal TV** (Mac; di Windows guna `tools\venv\Scripts\python`):
  ```bash
  tools/venv/bin/python -I tools/tv.py 'pm enable com.google.android.tvlauncher'
  tools/venv/bin/python -I tools/tv.py 'pm uninstall com.gathin.tvhome'
  ```
  Pada Google TV yang lebih baharu, baris pertama ialah `pm enable com.google.android.apps.tv.launcherx`.
- **Hidupkan semula aplikasi yang dimatikan:** `tools/restore.sh <senarai yang sama>` (Windows: `tools\restore.ps1`).
- **Buang tetapan anda:** `tools/theme.sh --reset` (Windows: `tools\theme.ps1 -Reset`).
- Akhir sekali anda boleh matikan **USB debugging** semula dalam Developer options.

## Kalau ada masalah

| Apa yang keluar | Apa nak buat |
|---|---|
| `timeout` atau `connection refused` masa sambung | USB debugging belum hidup, alamat salah, atau TV dan komputer di Wi-Fi berlainan. |
| Kotak "Allow USB debugging?" tidak keluar | Matikan dan hidupkan semula USB debugging, kemudian sambung lagi. |
| Windows kata skrip disekat | Jalankan `Set-ExecutionPolicy -Scope Process Bypass` dalam tetingkap yang sama (langkah 6). |
| Tiada waktu solat di bawah jam | Semak `prayer_zone` dalam `config.json`, pastikan TV ada internet, tunggu seminit. |
| Tiada bunyi azan | Fail mesti bernama tepat `azan.mp3` dan berada dalam `theme-saya`; hantar semula (langkah 14). |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Versi lain Clear TV Islamic sudah terpasang: buang dahulu (`pm uninstall com.gathin.tvhome`), kemudian ulang langkah 10. |

Jawapan lain dalam [TROUBLESHOOTING.md](TROUBLESHOOTING.md) (bahasa Inggeris).

# Clear TV Islamic

<p><b>Bahasa Melayu</b> · <a href="README.md">English</a> · <a href="docs/INSTALL.ms.md">Panduan pemasangan langkah demi langkah</a></p>

**Android TV atau Google TV anda, laju dan tanpa iklan, dengan waktu solat JAKIM, azan pada setiap waktu,
ayat Al-Quran dan gambar masjid-masjid indah di dunia, termasuk Mekah dan Madinah.**

Clear TV Islamic membuang aplikasi bloat dan iklan dari TV melalui Wi-Fi, tanpa root dan semuanya boleh
dibatalkan, lalu menggantikan skrin utama yang penuh iklan dengan skrin kaca yang ringan. Di belakang aplikasi
anda: Masjidil Haram, Masjid Nabawi, Masjid Kristal dan banyak lagi. Di bawah jam: lima waktu solat hari ini.
Bila TV berehat: jam emas, tarikh Hijri dan ayat Al-Quran. Bila masuk waktu: azan.

<p>
  <img src="docs/screenshots/promo-cleartv-1-home.jpg" width="32%" alt="TV anda, tenang dan tanpa iklan: Home dengan Masjid Quba di belakang aplikasi dan waktu solat di bawah jam">
  <img src="docs/screenshots/promo-cleartv-2-ciri.jpg" width="32%" alt="Setiap waktu, TV mengingatkan: waktu solat JAKIM, azan, ayat Al-Quran dan masjid di dunia">
  <img src="docs/screenshots/promo-cleartv-3-screensaver.jpg" width="32%" alt="Bila TV berehat, hati mengingati: screensaver jam emas, tarikh Hijri, waktu solat dan ayat Al-Quran">
</p>

**Baru pertama kali? Ikut [panduan pemasangan langkah demi langkah](docs/INSTALL.ms.md)**: tanpa coding, tanpa
Android SDK, lebih kurang 30 minit.

## Apa yang anda dapat

- **Waktu solat JAKIM** untuk zon anda di bawah jam: tarikh Hijri, lima waktu hari ini, waktu seterusnya
  ditonjolkan dan kiraan masa ("Asar dalam 12 min"). Waktu setahun diambil sekali dari
  [e-Solat JAKIM](https://www.e-solat.gov.my) dan disimpan dalam TV.
- **Azan pada setiap waktu**, sama ada TV di screensaver atau di Home: skrin azan penuh (bulan sabit emas dengan
  cincin cahaya) dan `azan.mp3` pilihan anda (`azan-subuh.mp3` untuk Subuh). Tekan apa-apa butang remote untuk
  hentikan. **Dalam aplikasi lain** (YouTube, Netflix, Live TV), jalur azan naik di bahagian bawah skrin dengan
  nama waktu, video di belakang berhenti seketika dan azan berkumandang. `"azan_over_apps": false` hanya
  memaparkan azan di Home dan screensaver.
- **Screensaver Islamik:** jam serif emas, tarikh Melayu dan Hijri, kad waktu solat dengan waktu seterusnya
  berbingkai emas, dan ayat Al-Quran dengan terjemahan yang bertukar setiap 3 minit. Teks Arab Uthmani dan
  terjemahan Kementerian Agama RI, melalui quran.com.
- **Gambar masjid di belakang Home dan screensaver:** Masjidil Haram, Kaabah, Masjid Nabawi, Masjid Quba,
  Masjid Kristal, Masjid Tengku Tengah Zaharah, Masjid Putra, Brunei, Istanbul, Islamabad, Lahore, Baitulmaqdis
  dan Shiraz, masing-masing dengan nama dan kredit jurugambar.
- **Sentiasa bersih.** Setiap 15 hari, bila Home tidak digunakan, TV membuang cache aplikasi (Play Store
  meninggalkan sisa muat turun berGB yang boleh buat aplikasi terbuka ke skrin kosong). Log masuk dan data
  aplikasi tidak disentuh; TV minta kebenaran sekali sahaja.
- **Kaca seperti Apple.** Dock, butang di bar atas dan menu menunjukkan gambar di belakangnya secara kabur,
  dengan kilauan di tepinya.
- **Satu pilihan yang meluncur** dari aplikasi ke aplikasi, tidak melompat.
- **Setiap aplikasi mewarnai bilik.** Cahaya di belakang Home bertukar ke warna aplikasi yang dipilih.
- **Susun aplikasi dengan remote.** Tekan lama OK pada aplikasi untuk menu **Move** dan **App info**.
- **Menu Inputs dalam kaca** untuk HDMI, AV dan Live TV.
- **Kembali seperti yang anda tinggalkan.** Balik dari aplikasi, aplikasi yang sama masih dipilih.
- **Ringan:** launcher menggunakan lebih kurang 80–100 MB RAM, satu gambar sahaja dalam memori, dan tiada apa
  yang berjalan bila anda dalam aplikasi lain.

| | |
|---|---|
| ![Home dengan Masjid Quba di belakang aplikasi, waktu solat di bawah jam dan nama masjid di atas dock](docs/screenshots/islamik-home.jpg) | ![Semua aplikasi, diskrol ke bawah atas gambar masjid](docs/screenshots/islamik-grid.jpg) |
| *Home, dengan masjid dan waktu solat* | *Semua aplikasi, pudar di bawah bar atas* |
| ![Menu Inputs kaca dengan HDMI 1 dipilih](docs/screenshots/islamik-inputs.jpg) | ![Screensaver Islamik: jam emas, tarikh Hijri, kad waktu solat dan ayat Al-Quran](docs/screenshots/screensaver-islamik.jpg) |
| *Inputs, dalam kaca* | *Screensaver Islamik* |
| ![Skrin azan: bulan sabit emas dan cincin cahaya, Telah masuk waktu Maghrib](docs/screenshots/azan.jpg) | ![Jalur azan di atas aplikasi lain: bulan sabit emas, Telah masuk waktu Maghrib, masa dan tarikh Hijri](docs/screenshots/azan-over-apps.jpg) |
| *Skrin azan pada waktu solat, di Home* | *Azan di atas aplikasi lain, termasuk YouTube dan Netflix* |

## Cara memasang

**Cara mudah, tanpa coding:** [panduan pemasangan langkah demi langkah](docs/INSTALL.ms.md). Ia memasang
`dist/clear-tv-islamic.apk` yang sudah siap, jadi anda cuma perlu Python, bukan Java atau Android SDK.

**Dengan pembantu AI** (Claude Code, Codex, Cursor, Copilot…): buka repositori ini dan minta ia pasang di TV
anda. [AGENTS.md](AGENTS.md) memberitahu pembantu itu cara melakukannya dengan selamat: semak model TV dulu,
backup, tanya sebelum setiap perubahan, tidak pernah uninstall, dan semak setiap langkah bersama anda.

Ringkasnya (Mac/Linux):

```bash
python3 -m venv tools/venv && tools/venv/bin/pip install -r tools/requirements.txt
export TV_HOST=192.168.1.50                                   # alamat IP TV anda
APK=dist/clear-tv-islamic.apk launcher/build-and-install.sh   # pasang dan jadikan Home
python3 tools/masjid-photos.py theme-saya                     # gambar masjid (pilihan)
tools/theme.sh theme-saya                                     # hantar tetapan, azan dan gambar
```

## Tetapan theme

Folder theme ialah folder dengan `config.json` ([themes/islamik](themes/islamik/config.json) sedia untuk disalin):

```json
{
  "prayer_zone": "TRG04",
  "azan": true,
  "reduced_motion": true
}
```

- `prayer_zone`: kod zon JAKIM anda, contohnya `WLY01` (Kuala Lumpur, Putrajaya), `SGR01` (Gombak, Petaling,
  Sepang, Hulu Langat, Hulu Selangor, Shah Alam), `JHR02` (Johor Bahru, Kota Tinggi, Mersing, Kulai), `TRG04`
  (Kemaman, Dungun). Senarai penuh di laman e-Solat. Kosongkan untuk mematikan semua ciri Islamik.
- `azan`: `false` mematikan azan.
- `masjid_home`: `false` untuk kembali ke gambar tempat mingguan dari Bing di Home.
- `dock_apps`: empat aplikasi di dock, ikut susunan.
- `azan.mp3` dan `azan-subuh.mp3` dalam folder yang sama: suara azan. [themes/islamik](themes/islamik) sudah ada
  azan Ustaz Asri Ibrahim (Rabbani) dan azan Subuh; gantikan dengan rakaman anda sendiri jika mahu, dengan nama
  yang sama. Tanpa fail ini, skrin azan keluar senyap selama 90 saat.
- `masjid/`: gambar masjid, dimuat turun oleh `tools/masjid-photos.py`.

Semua tetapan lain ada dalam [themes/README.md](themes/README.md) (bahasa Inggeris).

## TV yang boleh

Android TV dan Google TV, Android 8 ke atas, sama ada TV itu sendiri atau box yang dicucuk ke mana-mana TV.
[docs/DEVICES.md](docs/DEVICES.md) menyenaraikan model yang telah diuji.

TV LG (webOS) dan Samsung (Tizen) tidak boleh menjalankan aplikasi Android dan tidak membenarkan skrin utama
ditukar. Untuk Clear TV Islamic penuh, cucuk Google TV atau Android TV box (Chromecast with Google TV, Xiaomi Mi Box,
NVIDIA Shield) ke port HDMI.

## Kredit dan lesen

- Dibina di atas [CleanYourTV](https://github.com/Gathin23/cleanyourtv) © 2026 Gathin. Lesen MIT, lihat
  [LICENSE](LICENSE).
- Waktu solat dari [e-Solat JAKIM](https://www.e-solat.gov.my). Teks Al-Quran (Uthmani, Hafs) dan terjemahan
  Kemenag RI melalui [quran.com](https://quran.com).
- Gambar masjid dari Wikimedia Commons, setiap satu dengan kredit di skrin dan dalam
  [themes/islamik/masjid.json](themes/islamik/masjid.json). Gambar promo menggunakan foto Masjid Quba (Muhammad
  Mahdi Karim, GFDL 1.2), Masjidil Haram (Saudipics.com, CC BY-SA 4.0) dan Masjid Jame' Asr Hassanil Bolkiah
  (Jorge Láscar, CC BY 2.0).
- Rakaman azan dalam [themes/islamik](themes/islamik): Ustaz Asri Ibrahim (Rabbani), dan "Adzan Subuh Merdu".
  Pemilik repositori menyatakan ia bebas dikongsi; pemilik hak yang tidak bersetuju boleh membuka issue dan
  fail akan dibuang.
- Huruf Outfit di bawah SIL Open Font License (`launcher/FONT-LICENSE-OFL.txt`). Ikon Inputs dari Material Icons
  Google (Apache 2.0).

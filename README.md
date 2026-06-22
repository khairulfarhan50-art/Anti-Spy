# Anti-Spy Display Filter (Android & Windows Laptop)

Aplikasi *software-based* **Anti-Spy Display Filter** yang dirancang sebagai solusi privasi layar alternatif dari tempered glass *anti-spy* fisik. Keunggulan utama dari metode perangkat lunak ini adalah **sidik jari (fingerprint) di bawah layar tetap berfungsi 100% normal** karena tidak memblokir gelombang ultrasonik atau cahaya secara fisik.

Aplikasi ini menggambar lapisan pola privasi khusus (*dim*, *lines*, *crosshatch*, atau *noise*) di atas seluruh layar dengan sifat klik-tembus (*touch/mouse pass-through*).

---

## 🚀 Fitur Utama

### 📲 Versi Android Mobile
* **Background Foreground Service**: Filter berjalan secara konsisten di latar belakang tanpa dihentikan sistem.
* **Touch Pass-Through**: Sentuhan jari tetap menembus ke aplikasi di bawah filter tanpa hambatan.
* **Jetpack Compose UI**: Antarmuka modern bertema gelap dengan slider opasitas (kegelapan) dan pemilih pola.
* **Quick Settings Tile**: Sakelar cepat ON/OFF langsung dari bilah notifikasi status bar atas.
* **Fingerprint Safe**: Aman untuk sensor sidik jari jenis optik maupun ultrasonik di bawah layar.

### 💻 Versi Windows Desktop (Laptop)
* **Sangat Ringan (10 KB)**: Dibuat menggunakan C# Windows Forms native tanpa dependensi tambahan.
* **System Tray Mode**: Aplikasi berjalan di latar belakang dekat jam Windows, menjaga taskbar tetap bersih.
* **Multi-Monitor Support**: Filter otomatis menutupi seluruh monitor aktif secara virtual.
* **Click-Through Sempurna**: Menggunakan Win32 API untuk memastikan klik mouse, drag, dan input keyboard tidak terganggu.

---

## 📦 Unduh Aplikasi (Download)

Anda dapat mengunduh berkas aplikasi jadi secara gratis pada tab **[Releases](https://github.com/khairulfarhan50-art/Anti-Spy/releases)** di repositori ini:
* **Android**: Unduh berkas **`app-debug.apk`**
* **Windows Laptop**: Unduh berkas **`AntiSpyDesktop.exe`**

---

## 🛠️ Panduan Penggunaan

### Cara Memasang di Android:
1. Unduh dan pasang berkas **`app-debug.apk`** ke HP Android Anda.
2. Buka aplikasi dan tekan tombol **Berikan Izin** untuk mengaktifkan izin *Draw over other apps* (Tampilkan di atas aplikasi lain).
3. Berikan izin notifikasi (untuk Android 13+) agar Foreground Service dapat berjalan dengan lancar.
4. Aktifkan sakelar filter, sesuaikan tingkat kegelapan dengan slider, dan pilih pola filter yang Anda sukai.
5. *(Opsional)* Tarik status bar atas Anda, edit panel Quick Settings, dan tambahkan ubin **Anti-Spy Overlay** untuk kontrol instan.

### Cara Memasang di Windows Laptop:
1. Unduh berkas **`AntiSpyDesktop.exe`** lalu klik ganda untuk menjalankannya.
2. Cari ikon **Shield (Perisai)** di pojok kanan bawah Windows (System Tray dekat penunjuk jam).
3. **Klik Kanan** pada ikon Shield tersebut untuk mengontrol filter:
   * **Aktifkan Filter**: Hidupkan/matikan filter privasi (bisa juga dengan klik ganda ikon perisai).
   * **Pola Filter**: Pilih pola filter (*Hanya Redup*, *Garis Vertikal*, *Silang Diagonal*, atau *Bintik Noise*).
   * **Tingkat Kegelapan**: Sesuaikan intensitas kegelapan filter dari 10% hingga 90%.
   * **Keluar**: Menutup aplikasi sepenuhnya.

---

## 📂 Struktur Proyek
* `/app` — Proyek Android Native berbasis Kotlin + Jetpack Compose.
* `/desktop` — Proyek Windows Forms C# dan skrip compiler build-nya.
* `.gitignore` — Konfigurasi untuk mengecualikan berkas build sistem.

---

## 👨‍💻 Kontributor
* **[khairulfarhan50-art](https://github.com/khairulfarhan50-art)** — Pembuat & Pengembang Utama.

# GameSpace Universal

Port universal dari [GameSpace AxionOS](https://github.com/chaldeaprjkt) (Apache-2.0) agar bisa dipasang sebagai
aplikasi biasa di ROM OEM (XOS, ColorOS/OxygenOS, Funtouch/OriginOS, MIUI/HyperOS, dll.), minimal Android 13.

UI (Compose) dan seluruh logika game bar dipertahankan dari source asli. Yang diganti hanya lapisan sistem:

| Dulu (aplikasi sistem Axion) | Sekarang |
|---|---|
| `sharedUserId=system`, platform key, API hidden | App biasa + **Shizuku** (prioritas) atau **root** |
| Game Mode (`GameManager`) | `cmd game mode <mode> <pkg>` |
| FPS (`TaskFpsCallback`) | `dumpsys SurfaceFlinger --latency` (perkiraan) |
| Bypass charging, touch boost (prop Axion) | node sysfs/procfs (kandidat + node kustom), butuh root |
| Gestur screenshot 3 jari | kunci setting ROM dicari saat runtime |
| Screenshot | `input keyevent 120` |
| Boost memori (`AxMemoryManager`) | `am kill-all` |
| Tile (`AxPlatformClient`) | perintah `svc`/`settings`/`cmd` lewat Shizuku |
| Pemicu sesi game (patch SystemUI) | `GameWatcherService` (UsageStats / dumpsys) |
| `SceneTransitionLayout`, `AxionTheme` | implementasi pengganti (crossfade, Material3 dinamis) |

## Build
Push ke GitHub; workflow `.github/workflows/build.yml` membuat APK release bertanda tangan
(artifact `GameSpaceUniversal-release`). Lokal: `./gradlew assembleRelease`.
Ikon Material Symbols diunduh saat build (lihat `generateSymbols` di `app/build.gradle.kts`).

## Pemakaian
1. Pasang Shizuku, start (wireless debugging atau root), lalu buka Game Space -> layar Setup -> "Beri izin" -> "Beri semua izin otomatis".
2. Tambah game di Hub. Buka game; bubble game bar muncul.

## Catatan
Perubahan dari versi asli dan keterbatasannya ada di balasan yang menyertai file ini.

## Fitur tambahan (versi universal)
- Ping jaringan dan 1% low FPS di panel (kartu FPS).
- Kunci refresh rate (Settings > Performance, tile "Refresh rate", dan per-game).
- Auto-bersihkan RAM saat game mulai (global atau per-game).
- Batas pengisian saat bermain (Settings > Power, butuh node charge yang bisa ditulis).
- Statistik sesi dan riwayat bermain (halaman pengaturan tiap game).
- Profil per-game: refresh rate, auto-RAM, kecerahan awal.
- Timer sesi di header panel dan pengingat istirahat (Settings > Performance).
Crosshair kustom (gaya, warna, ukuran, opasitas, posisi) sudah ada di aplikasi asli.

## Fitur gaming tambahan (batch 2)
- 12 tile baru di sidebar: Show touches, Pointer location, Fast animations, Touch haptics, Mute media, Sound mode, Clear notifications, Turbo, Cool, Mini HUD, Pause charging, Refresh rate.
- Mini HUD mengambang (FPS, ping, CPU, suhu, RAM) dengan posisi dan isi yang bisa diatur.
- Baris CPU/suhu/RAM di kartu FPS panel; pilihan server ping.
- Peringatan suhu tinggi dan baterai rendah.
- Profil per-game: tambahan Do Not Disturb dan Turbo mode otomatis.
- Tile baru otomatis muncul sekali untuk pengguna lama.

## Dukungan per genre game (batch 3)
- Genre per game (Auto / Umum / MOBA / Shooter / Battle Royale / Racing / RPG) dipilih di layar detail Hub.
  Auto memakai daftar bawaan nama paket game populer yang terbatas; game lain dianggap Umum.
- Preset genre hanya mengisi default (refresh rate, Turbo, DND, RAM, Auto Cool, peringatan lag, isi HUD).
  Prioritas: profil per-game > pengaturan global > preset genre.
- Kualitas jaringan: ping, jitter, packet loss, penilaian GOOD/FAIR/POOR, peringatan lonjakan lag, info Wi-Fi/seluler.
- Monitor lengkap: CPU (beban, frekuensi), GPU, suhu, RAM, daya baterai.
- Timer match + 3 timer hitung mundur yang bisa diatur, penanda waktu match berkala.
- Auto Cool: mode Cool otomatis saat suhu mencapai ambang, kembali normal setelah turun 4 derajat.
- Statistik sesi diperluas: stabilitas FPS, ping rata-rata, jumlah lag mendadak.
Tidak ada fitur yang mengotomatiskan input atau membaca isi game (auto-aim, macro, ESP), karena itu curang dan berisiko banned.

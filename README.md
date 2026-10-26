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

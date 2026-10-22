# Game Space Universal

Versi universal (API publik saja) yang terinspirasi dari GameSpace AxionOS. Kode ditulis ulang, bukan disalin.
Tidak butuh platform key, `sharedUserId`, atau API hidden, sehingga bisa dipasang di ROM OEM mana pun (Android 10+).

## Build
1. Buka folder ini di Android Studio (JDK 17), biarkan sync, lalu Run. Atau `gradle wrapper --gradle-version 8.9` lalu `./gradlew assembleDebug`.
2. APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Fitur yang dihapus dari versi asli (tidak ada API publik)
Game Mode (GameManager), FPS counter, bypass charging, touch boost, screenshot 3 jari, kontrol memori internal Axion.

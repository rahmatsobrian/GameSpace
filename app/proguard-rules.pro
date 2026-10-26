# ---------------------------------------------------------------------------
# Aturan R8 untuk build release GameSpace Universal.
# ---------------------------------------------------------------------------
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Shizuku: ShellService dibuat lewat refleksi oleh proses Shizuku (nama kelas harus tetap),
# dan kelas AIDL Stub/Proxy dipakai lewat Binder.
-keep class com.android.axion.gamespace.shell.** { *; }
-keep class rikka.shizuku.** { *; }
-dontwarn rikka.**

# Gson: model data diserialisasi lewat refleksi (nama field harus tetap).
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.android.axion.gamespace.data.** { *; }
-keep class com.android.axion.gamespace.gamebar.mapper.TapMapping { *; }

# Komponen yang dipanggil sistem lewat nama kelas (juga sudah dijaga aturan manifest AGP).
-keep class com.android.axion.gamespace.gamebar.DanmakuServiceListener { *; }

# Peringatan kelas anotasi yang tidak ada saat runtime.
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
-dontwarn com.google.errorprone.annotations.**

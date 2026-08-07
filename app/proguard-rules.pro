# Hilt, Room and Compose ship their own consumer ProGuard rules inside their
# AARs, so they don't need manual keep rules here.

# Shizuku talks to a remote process over Binder/AIDL and does some of its
# permission-result plumbing via reflection — keep it intact.
-keep class rikka.shizuku.** { *; }
-dontwarn rikka.shizuku.**

# libsu's root-service IPC layer also relies on Binder marshalling for the
# classes it exposes across the shell boundary.
-keep class com.topjohnwu.superuser.** { *; }
-dontwarn com.topjohnwu.superuser.**

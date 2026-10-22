package com.android.axion.gamespace.shell;

interface IShellService {
    // Dipanggil Shizuku saat service dihentikan (kode transaksi tetap dari Shizuku).
    void destroy() = 16777114;

    // Menjalankan perintah shell di proses privileged (shell/root). Hasil: "<exitcode>\n<output>".
    String exec(String command, int timeoutMs) = 1;

    // UID proses service: 2000 = shell (Shizuku via adb), 0 = root.
    int uid() = 2;
}

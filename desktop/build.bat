@echo off
echo Mengompilasi AntiSpyDesktop...
C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe /target:winexe /out:AntiSpyDesktop.exe /reference:System.dll /reference:System.Windows.Forms.dll /reference:System.Drawing.dll AntiSpyDesktop.cs
if %errorlevel% neq 0 (
    echo Kompilasi GAGAL! Silakan periksa error di atas.
    pause
    exit /b %errorlevel%
)
echo Kompilasi BERHASIL! Berkas eksekusi dibuat: AntiSpyDesktop.exe
pause

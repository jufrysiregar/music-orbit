# 🎵 Music Orbit

> Aplikasi pemutar musik lokal offline-first yang dibangun dengan **Kotlin Multiplatform (KMP)** — satu codebase untuk Android dan iOS.

---

## 📱 Tentang Aplikasi

**Music Orbit** adalah aplikasi pemutar musik modern yang memutar file audio lokal langsung dari penyimpanan perangkat. Semua data disimpan secara lokal — tidak ada server, tidak ada koneksi internet yang diperlukan untuk memutar musik.

Aplikasi ini didesain dengan pengalaman yang berbeda dari pemutar musik kebanyakan: saat dibuka, pengguna langsung disambut oleh **layar pemutar utama** dengan lagu terakhir yang diputar — bukan daftar lagu. Daftar lagu diakses dengan tombol Back, menjadikan pengalaman mendengarkan lebih immersive.

**Seluruh teks antarmuka dalam Bahasa Indonesia.**

---

## ✨ Fitur Utama

### 🎧 Pemutaran Musik
- Memutar file audio lokal: `.mp3`, `.flac`, `.aac`, `.ogg`, `.m4a`
- Pemutaran latar belakang (background playback) — musik tetap berjalan saat aplikasi diminimize
- Notifikasi sistem dengan kontrol media (play/pause, next, previous)
- Kontrol lengkap: Play, Pause, Stop, Previous, Next
- Seek bar interaktif dengan indikator waktu berjalan dan total durasi

### 🗂️ Mode Pemutaran
Beralih antar mode dengan satu ketukan:
| Ikon | Mode | Deskripsi |
|------|------|-----------|
| 🔂 | **Urutan Normal** | Putar berurutan dari awal hingga akhir |
| 🔀 | **Acak (Shuffle)** | Putar lagu secara acak |
| 🔁 | **Ulangi 1 Lagu** | Ulangi lagu yang sama terus-menerus |

### 🔍 Metadata Presisi
Scanner membaca **ID3 tags** langsung dari file audio, bukan dari nama file. Nama lagu "Tinggal Kenangan" dan artis "Arya Arjuna" terbaca dengan benar, bukan sebagai `Tinggal_Kenangan_Arya_Arjuna.mp3`.
- Fallback otomatis: jika tag kosong, nama file diformat (underscore/hyphen → spasi)
- Menyimpan metadata ke database lokal (SQLDelight)

### 📋 Daftar Musik (Home Screen)
- Daftar semua musik lokal dalam tampilan yang rapi
- **Pencarian real-time** berdasarkan judul atau nama artis
- **Pengurutan** berdasarkan:
  - Terbaru (berdasarkan tanggal file)
  - Paling sering diputar
  - A - Z (judul)
  - Artis
- Preferensi pengurutan tersimpan otomatis

### 🎵 Manajemen Lirik
- Tampilkan lirik lagu yang tersimpan saat musik diputar
- Tombol **"Cari Lirik"** — membuka Google Search dengan kata kunci `"Lirik [Judul] [Artis]"` secara otomatis
- Kolom paste untuk menempelkan lirik dari browser dan menyimpannya **secara offline**
- Badge `[LIRIK]` ditampilkan pada daftar lagu yang sudah memiliki lirik tersimpan
- Dukungan file `.lrc` (synced lyrics) — cocokkan nama file audio dengan file `.lrc`

### 🎚️ Equalizer
- 5 preset bawaan: **Normal, Pop, Rock, Jazz, Classical**
- Slider band frekuensi untuk penyesuaian manual secara real-time
- Toggle aktif/nonaktif
- Pengaturan tersimpan otomatis
- Android: menggunakan `android.media.audiofx.Equalizer` terhubung ke session audio ExoPlayer

### ⚙️ Pengaturan
- **Bahasa** — pengaturan bahasa antarmuka
- **Waktu Tidur (Sleep Timer)** — otomatis menghentikan musik setelah: 15, 30, 45, 60 menit
- **Tema** — Terang / Gelap / Ikuti Sistem
- **Tentang Aplikasi**

### 📖 Edit Metadata
- Ketuk ikon pensil ✏️ di layar pemutar utama untuk mengedit **Judul Lagu** dan **Nama Artis**
- Perubahan langsung tersimpan ke database lokal

---

## 🗺️ Navigasi Aplikasi

Aplikasi ini **tidak memiliki Bottom Navigation Bar**. Navigasi murni via ikon dan tombol Back:

```
Buka Aplikasi
      │
      ▼
┌─────────────────────┐
│   Main Player       │  ← Layar utama (start destination)
│  (Layar Pemutar)    │
│                     │
│  [Back] ───────────►│──► Home Screen (Daftar Lagu)
│  [🎚 EQ] ──────────►│──► Equalizer Screen
│  [🎵 Lirik] ───────►│──► Lyrics Screen
└─────────────────────┘
                             │
                    [⚙️ Settings] ──► Settings Screen
                                           │
                                    [Tentang] ──► About Screen
```

**Mini Player** tampil mengambang di bagian bawah Home Screen saat musik sedang diputar — menampilkan judul lagu, album art kecil, tombol Stop dan Next. Ketuk Mini Player untuk kembali ke layar pemutar utama.

---

## 🏗️ Arsitektur

Music Orbit dibangun dengan **Kotlin Multiplatform (KMP)** — satu shared codebase untuk domain logic, database, dan repository, sementara implementasi platform-specific (audio, scanner, equalizer) menggunakan mekanisme `expect/actual`.

```
┌─────────────────────────────────────────────────────┐
│                   androidApp/                        │
│  Compose UI · Navigation · Koin · ViewModels         │
├─────────────────────────────────────────────────────┤
│                    iosApp/                           │
│  SwiftUI entry · Compose Multiplatform (stub)        │
├─────────────────────────────────────────────────────┤
│                   shared/                            │
│  ┌─────────────────────────────────────────────┐    │
│  │ commonMain                                  │    │
│  │  Domain Models · Repository Interfaces      │    │
│  │  Use Cases · SQLDelight DB · Koin SharedMod │    │
│  │  expect: AudioPlayer, MusicScanner, EQ      │    │
│  ├─────────────────────────────────────────────┤    │
│  │ androidMain                                 │    │
│  │  actual AudioPlayer  → Media3 ExoPlayer     │    │
│  │  actual MusicScanner → MediaStore + ID3     │    │
│  │  actual EqualizerEngine → AudioEffect API   │    │
│  ├─────────────────────────────────────────────┤    │
│  │ iosMain                                     │    │
│  │  actual AudioPlayer  → AVFoundation (stub)  │    │
│  │  actual MusicScanner → UIDocumentPicker     │    │
│  │  actual EqualizerEngine → AVAudioUnitEQ     │    │
│  └─────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────┘
```

### Design Pattern: MVVM + Clean Architecture

```
Compose UI  →  ViewModel  →  Use Case  →  Repository  →  SQLDelight / Settings
                                                    ↕
                                          expect/actual Platform APIs
```

---

## 🛠️ Tech Stack

| Kategori | Library | Versi |
|----------|---------|-------|
| **Language** | Kotlin Multiplatform | 2.0.21 |
| **UI** | Jetpack Compose (Material 3) | BOM 2024.12.01 |
| **DI** | Koin Multiplatform | 4.0.0 |
| **Database** | SQLDelight | 2.0.2 |
| **Preferences** | Multiplatform Settings (russhwolf) | 1.2.0 |
| **Audio (Android)** | AndroidX Media3 / ExoPlayer | 1.5.1 |
| **Image Loading** | Coil | 2.7.0 |
| **Navigation** | Navigation Compose | 2.8.5 |
| **Permissions** | Accompanist Permissions | 0.36.0 |
| **Coroutines** | Kotlinx Coroutines | 1.9.0 |
| **Testing** | Kotest + Property-Based Testing | 5.9.1 |
| **Build** | Gradle KTS + Version Catalog | 8.11.1 |
| **Min SDK** | Android 7.0 (API 24) | — |
| **Target SDK** | Android 15 (API 35) | — |

---

## 📁 Struktur Project

```
musicorbit/
├── shared/                          # KMP shared module
│   └── src/
│       ├── commonMain/
│       │   ├── kotlin/com/musicorbit/
│       │   │   ├── di/              # Koin SharedModule
│       │   │   ├── domain/
│       │   │   │   ├── model/       # Song, Lyrics, PlaybackState, PlayMode, SortOrder
│       │   │   │   ├── repository/  # Interface: SongRepository, LyricsRepository, PreferencesRepository
│       │   │   │   └── usecase/     # GetSongsUseCase, ScanMusicUseCase, dll.
│       │   │   ├── data/
│       │   │   │   ├── db/          # DatabaseDriverFactory (expect)
│       │   │   │   └── repository/  # Implementasi repository
│       │   │   └── platform/        # expect: AudioPlayer, MusicScanner, EqualizerEngine
│       │   └── sqldelight/          # MusicOrbitDatabase.sq (schema + queries)
│       ├── androidMain/             # actual: Media3, MediaStore, AudioEffect
│       └── iosMain/                 # actual stubs: AVFoundation, UIDocumentPicker
│
├── androidApp/                      # Android application module
│   └── src/main/
│       ├── java/com/musicorbit/android/
│       │   ├── MusicOrbitApp.kt     # Application + Koin init
│       │   ├── MainActivity.kt
│       │   ├── di/AndroidModule.kt  # Koin: SqlDriver, AudioPlayer, ViewModels
│       │   ├── service/MusicService.kt
│       │   └── ui/
│       │       ├── navigation/      # AppNavGraph, Screen
│       │       ├── screen/          # player, home, lyrics, equalizer, settings, about
│       │       ├── components/      # AlbumArtImage, MiniPlayer, LyricsBadge, SeekBar
│       │       ├── theme/           # Material 3 Theme, Typography
│       │       └── util/            # TimeFormatter, PermissionHandler
│       └── res/
│
├── iosApp/                          # iOS entry point (Swift)
│   └── iosApp/
│       ├── iOSApp.swift
│       └── ContentView.swift
│
├── gradle/
│   ├── libs.versions.toml           # Version catalog
│   └── wrapper/
├── build.gradle.kts                 # Root project
├── settings.gradle.kts
└── gradle.properties
```

---

## 🚀 Cara Menjalankan

### Prasyarat

- **Android Studio** Ladybug (2024.2.1) atau lebih baru
- **JDK 17**
- **Android SDK** API 24–35
- **Gradle** 8.11.1 (dikelola otomatis oleh wrapper)

### Android

1. Clone repository:
   ```bash
   git clone https://github.com/yourusername/musicorbit.git
   cd musicorbit
   ```

2. Buka di **Android Studio** → `File > Open` → pilih folder `musicorbit`

3. Tunggu Gradle sync selesai

4. Jalankan konfigurasi `androidApp` ke emulator atau perangkat fisik:
   ```
   Run > Run 'androidApp'
   ```

5. Izinkan akses media saat diminta pertama kali

### iOS (via CI/CD)

Build iOS dilakukan melalui **GitHub Actions** dengan `macos-latest` runner (memerlukan Xcode). Tidak dapat di-build langsung di Windows.

Konfigurasi workflow: `.github/workflows/build-ios.yml` *(tambahkan sendiri)*

```yaml
jobs:
  build-ios:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Build shared framework
        run: ./gradlew :shared:assembleReleaseXCFramework
      - name: Build iOS app
        run: xcodebuild -project iosApp/iosApp.xcodeproj ...
```

---

## 🗃️ Database Schema

SQLDelight menghasilkan type-safe Kotlin queries dari skema SQL berikut:

```sql
-- Tabel utama lagu
CREATE TABLE Song (
    id          INTEGER NOT NULL PRIMARY KEY,
    title       TEXT    NOT NULL,
    artist      TEXT    NOT NULL,
    album       TEXT    NOT NULL,
    duration    INTEGER NOT NULL,          -- milidetik
    filePath    TEXT    NOT NULL,
    albumArtUri TEXT,
    playCount   INTEGER NOT NULL DEFAULT 0,
    dateAdded   INTEGER NOT NULL           -- epoch milidetik
);

-- Tabel lirik (one-to-one dengan Song)
CREATE TABLE Lyrics (
    songId     INTEGER NOT NULL PRIMARY KEY REFERENCES Song(id) ON DELETE CASCADE,
    lyricsText TEXT    NOT NULL,
    hasLrc     INTEGER NOT NULL DEFAULT 0   -- 0=false, 1=true
);
```

---

## 🔑 Preferensi yang Tersimpan

Semua preferensi disimpan secara lokal menggunakan **Multiplatform Settings** (SharedPreferences pada Android):

| Key | Tipe | Default | Keterangan |
|-----|------|---------|------------|
| `last_played_song_id` | Long | — | ID lagu terakhir diputar |
| `sort_order` | String | `DATE_ADDED` | Urutan daftar lagu |
| `play_mode` | String | `SEQUENTIAL` | Mode pemutaran |
| `theme` | String | `SYSTEM` | Tema tampilan |
| `language` | String | `id` | Bahasa antarmuka |
| `sleep_timer` | Int | `0` | Durasi sleep timer (menit) |
| `eq_preset` | String | `Normal` | Preset equalizer aktif |
| `eq_bands` | String | `[]` | Level tiap band (JSON array) |
| `eq_enabled` | Boolean | `false` | Status aktif equalizer |

---

## 📱 Screenshots

> *Screenshots akan ditambahkan setelah build pertama selesai.*

| Main Player | Home Screen | Equalizer |
|-------------|-------------|-----------|
| *(soon)* | *(soon)* | *(soon)* |

---

## 🧪 Testing

Project menggunakan **Kotest** dengan **Property-Based Testing (PBT)** untuk memvalidasi correctness properties:

| Property | Deskripsi |
|----------|-----------|
| **P1** Metadata Non-Empty | Scanner selalu menghasilkan title dan artist yang tidak kosong |
| **P2** DB Round-Trip | Song yang di-upsert dapat dibaca kembali dengan data identik |
| **P3** Play Mode Cycle | Cyclic 3× selalu kembali ke mode awal |
| **P4** Search Correctness | Filter menghasilkan semua dan hanya lagu yang cocok (no false positive/negative) |
| **P5** Sort Ordering | Daftar tersortir memenuhi ordering predicate untuk setiap pasangan berdekatan |
| **P6** Lyrics Persistence | Lirik yang disimpan terbaca identik; badge `[LIRIK]` konsisten dengan data |
| **P7** Settings Round-Trip | Semua preferensi yang ditulis terbaca kembali dengan nilai identik |
| **P8** EQ Band Level | Level band yang di-set terbaca kembali dengan nilai yang sama |

Jalankan unit tests:
```bash
./gradlew :androidApp:test
./gradlew :shared:jvmTest
```

---

## 👨‍💻 Developer

**M. Juffri Siregar (Jufry Siregar) — Universitas Potensi Utama '19 — Informatika**

[![LinkedIn](https://img.shields.io/badge/LinkedIn-0A66C2?style=flat&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/mjuffrisiregar/)
[![Email](https://img.shields.io/badge/Email-EA4335?style=flat&logo=gmail&logoColor=white)](mailto:mjuffrisiregar@gmail.com)
[![Portfolio](https://img.shields.io/badge/Portfolio-000000?style=flat&logo=vercel&logoColor=white)](https://mjs-portofolio.vercel.app/)

---

## 📄 Lisensi

```
Copyright (c) 2024 MJS — Universitas Potensi Utama

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so.
```

---

<div align="center">
  <sub>M. Juffri Siregar (Jufry Siregar) - Universitas Potensi Utama '19 - Informatika</sub><br>
  <sub>v1.0.0</sub>
</div>

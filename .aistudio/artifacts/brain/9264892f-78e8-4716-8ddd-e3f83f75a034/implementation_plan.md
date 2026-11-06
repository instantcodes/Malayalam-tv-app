# Malayalam Live TV Streamer

A streaming Android application designed for live Malayalam television channels, featuring live playlist synchronization with offline fallback, advanced channel filtering, intuitive category navigation, persistent favorites, and an integrated Media3 player with resolution switching and full playback controls.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural and UX choices have been confirmed based on your requirements and preferences:

- **Live Sync & Offline Fallback**: The app will fetch the live M3U playlist from `https://iptv-org.github.io/iptv/languages/mal.m3u` upon launch using OkHttp, parse channel metadata (`tvg-id`, `tvg-logo`, `group-title`), and store the channels locally (via Room or SharedPreferences/DataStore cache) with a bundled fallback playlist for zero-downtime offline access.
- **Blacklist Filter**: Automatically sanitizes the playlist feed by strictly filtering out blacklisted channels (including *Mazhavil Manorama HD, Anand TV, Asianet Middle East, C Malayalam TV, DidikTV KPM, Garshom TV, Goodness TV, Harvest TV, Kairali Arabia, Kalika TV, meWATCH LIVE 1, MTA7 Asia, Nickelodeon (India), NTV7, RTM Parlimen Dewan Negara, RTM Parlimen Dewan Rakyat, Pogo, Potta-Divine, SelangorTV, Tuning Shalom America, Shekinah TV, Sonic, SONY SPORTS Ten 1, SONY SPORTS Ten 2, SONY SPORTS Ten 3, Vision (India), Star Sports 1, Star Sports 3*) using normalized name and alias matching.
- **Dual-Mode Player UI**: Seamless transitions between an embedded header/split-screen player with a collapsible channel drawer on mobile/tablets and an immersive, edge-to-edge fullscreen player with on-screen HUD controls.
- **Resolution & Audio Controls**: Custom playback control bar equipped with Play/Pause, Live scrubber/DVR timeline, Mute/Unmute audio toggle, Aspect Ratio fit toggle, Fullscreen toggle, and a dedicated Video Quality Selector (supporting Auto, 1080p, 720p, 480p, 360p via Media3 `TrackSelectionParameters`).
- **Cinematic Dark Theme**: Deep charcoal/slate foundation (`#0D1117`, `#161B22`) accented with warm, vibrant amber-gold (`#F59E0B`, `#FBBF24`) highlights and crisp typography.

---

## 1. Overview & Core Concept

- **What It Does**: Streams Malayalam live TV channels sourced from IPTV-Org directly on Android mobile, tablet, and TV displays. Provides category grouping (e.g., News, Entertainment, Music, Movies, Religious, Kids), search filtering, instant favorites toggling, and video stream diagnostics.
- **Target Audience**: Malayalam speakers and expatriate diaspora seeking access to regional Kerala broadcast streams with zero buffering delay, straightforward channel switching, and clear video controls.
- **Key Value**: Delivers a cable/satellite TV experience on handheld and large screens without subscription clutter, ads, or broken/unwanted channels.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **First Launch & Sync**:
   - The app opens with a cinematic amber-accented splash and immediately checks the network for the latest `mal.m3u` playlist.
   - Channel items are parsed, sanitized against the blacklist, grouped into categories, and displayed in an interactive horizontal category pill strip and channel grid.
   - If offline or on slow network, cached channels load instantly without blocking the user.

2. **Browsing & Category Filtering**:
   - Top bar provides quick search (instant filter by name), a "Favorites" filter tab (⭐), and dynamic category tabs (All, News, General, Entertainment, Music, etc.).
   - Each channel card displays the channel logo (via Coil with high-contrast placeholder fallback), category badge, live stream status, and one-tap favorite star.

3. **Playback Experience**:
   - Tapping any channel immediately launches playback in the top embedded player or transitions into fullscreen.
   - Tapping the video surfaces the media controller overlay (fades out after 4 seconds of inactivity):
     - **Top Bar**: Channel logo, channel name, category badge, and close/minimize icon.
     - **Center**: Rapid backward/forward skip or channel quick-hop, and central large Play/Pause toggle with animated state.
     - **Bottom Bar**: Live broadcast badge ("LIVE"), timeline scrub bar, current resolution badge, Mute/Volume toggle, **Quality Selector** button (gear/HD icon opening a clean modal sheet or popup with Auto, 1080p, 720p, 480p options), Aspect Ratio toggle (Fit, Fill, Zoom), and Fullscreen button.

4. **TV & Large Screen Mode**:
   - Adaptive responsive layout: On tablets and horizontal orientations, the player occupies the left pane while a channel list drawer occupies the right pane for easy channel surfing without interrupting playback.
   - Focusable navigation elements for D-pad/remote controllers on Android TV.

### Visual Identity & Theme Tokens

- **Aesthetic**: Premium cinema dark mode with warm golden highlights, inspired by modern streaming apps (Plex, Apple TV, YouTube TV).
- **Color Palette**:
  - `Background`: `#0A0D14` (Deep obsidian black)
  - `Surface`: `#121824` (Subtle dark slate card surface)
  - `SurfaceVariant`: `#1A2234` (Elevated menu and drawer surface)
  - `Primary / Accent`: `#F59E0B` (Vibrant amber gold)
  - `PrimaryContainer`: `#3B2807` (Subdued amber highlight container)
  - `OnPrimary`: `#000000` (High contrast text on gold)
  - `TextPrimary`: `#F8FAFC` (Pure crisp off-white)
  - `TextSecondary`: `#94A3B8` (Muted silver gray)
  - `LiveIndicator`: `#EF4444` (Vibrant broadcast red)
- **Typography**: Clean modern sans-serif hierarchy with bold channel titles, legible badge captions, and high-visibility player timecodes.

---

## 3. Key Product Decisions & Trade-Offs

- **Media Playback Engine: Media3 ExoPlayer (`androidx.media3:media3-exoplayer` + `media3-exoplayer-hls`)**:
  - *Chosen Approach*: Use official Android Media3 with HLS extension and `TrackSelectionParameters`.
  - *Why*: Supports adaptive bitrate (ABR) HLS live streams natively, provides granular video track resolution selection (overriding default bitrate to select 1080p, 720p, 480p), handles aspect ratio resizing (`AspectRatioFrameLayout`), and works across all Android versions without WebView hacks.
- **Channel Blacklist Strategy**:
  - *Chosen Approach*: Normalization engine with exact matching, substring match, and phonetic/punctuation cleanups (stripping spaces, brackets, hyphens, and casing) for the 28+ channels requested by the user.
  - *Why*: IPTV playlist channel titles often vary slightly over time (e.g. `Mazhavil Manorama HD` vs `Mazhavil Manorama (HD)` or `SONY SPORTS Ten 1` vs `Sony Sports Ten 1 HD`); a robust normalized match ensures unwanted channels never leak into the UI.
- **Local Persistence: Room Database & DataStore**:
  - *Chosen Approach*: Room Database for storing cached channels, categories, and favorite channel IDs, with DataStore for user preferences (dark mode, last played channel, default quality).
  - *Why*: Fast querying, instant offline launch, reactive StateFlow updates, and durable local storage.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                      Android Applet                         │
│                                                             │
│  ┌────────────────────┐          ┌───────────────────────┐  │
│  │  IPTV Playlist API │          │ Bundled Fallback M3U  │  │
│  │ (iptv-org/mal.m3u) │          │  (Offline Asset / Res)│  │
│  └─────────┬──────────┘          └──────────┬────────────┘  │
│            │ (HTTP GET via OkHttp)          │               │
│            ▼                                ▼               │
│  ┌───────────────────────────────────────────────────────┐  │
│  │           M3U Playlist Parser & Blacklist Filter      │  │
│  │  - Parses #EXTINF attributes (id, name, logo, group)  │  │
│  │  - Normalizes & prunes blacklisted 28+ channels       │  │
│  └──────────────────────────┬────────────────────────────┘  │
│                             │                               │
│                             ▼                               │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                ChannelRepository                      │  │
│  │  - Room Database / Local Cache                        │  │
│  │  - Favorites DAO & Stream Management                  │  │
│  └──────────────────────────┬────────────────────────────┘  │
│                             │ StateFlow                     │
│                             ▼                               │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                ChannelViewModel                       │  │
│  │  - Category filtering, search query                   │  │
│  │  - Active channel, track formats, playback state      │  │
│  └──────────────────────────┬────────────────────────────┘  │
│                             │                               │
│          ┌──────────────────┴──────────────────┐            │
│          ▼                                     ▼            │
│  ┌─────────────────────────┐       ┌──────────────────────┐ │
│  │   Jetpack Compose UI    │       │   Media3 ExoPlayer   │ │
│  │ - Dual-Mode Channel List│       │ - HLS Live playback  │ │
│  │ - Category Filter Bar   │◄─────►│ - Quality Selector   │ │
│  │ - Search & Favorites    │       │ - Sound / Fullscreen │ │
│  │ - Cinematic Amber Theme │       │ - TrackSelectionParam│ │
│  └─────────────────────────┘       └──────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### Data Model & Key State

- **`Channel`**:
  - `id: String` (Unique ID derived from stream URL or `tvg-id`)
  - `name: String` (Display channel title)
  - `streamUrl: String` (Direct `.m3u8` or stream endpoint)
  - `logoUrl: String?` (`tvg-logo` metadata)
  - `groupTitle: String` (Category: News, General, Entertainment, Music, etc.)
  - `isFavorite: Boolean`
- **`PlaybackState`**:
  - `currentChannel: Channel?`
  - `isPlaying: Boolean`
  - `isBuffering: Boolean`
  - `isFullscreen: Boolean`
  - `isMuted: Boolean`
  - `volume: Float`
  - `aspectRatioMode: AspectRatioMode` (Fit, Fill, Zoom)
  - `availableQualities: List<VideoQualityOption>` (Auto, 1080p, 720p, 480p, 360p)
  - `selectedQuality: VideoQualityOption`
  - `playbackPositionMs: Long`, `durationMs: Long`, `isLiveStream: Boolean`

### Interactive Component & State Mapping

- **Top Bar**: Search icon (opens search field), Category filter tabs, Favorite filter toggle, Refresh button (forces re-sync of playlist).
- **Embedded / Fullscreen Player (`AndroidView` wrapping `PlayerView` with custom Compose HUD)**:
  - Custom overlay with play/pause, scrub slider, live badge, volume/mute button.
  - Video quality button placed beside sound controls: displays current resolution badge (e.g. "HD 1080p" or "Auto"); tapping opens a resolution picker dialog with live track switching.
  - Fullscreen toggle: toggles immersive landscape orientation and hides system bars.
  - BackHandler: smoothly exits fullscreen back to dual-pane or compact list view.

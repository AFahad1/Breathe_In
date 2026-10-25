# 🌬️ Breathe In

**Catch yourself mid-scroll. Take a breath. See where your time really went.**

Breathe In is an Android app that interrupts doom scrolling. While you scroll Instagram Reels, YouTube Shorts or TikTok, it steps in **mid-session** with a calm, full-screen breathing exercise. Then **Google Gemini** shows you what you were actually watching: gym clips, memes, news, cat videos and more.

> Built for an AI hackathon. The AI features run on **Google Gemini** (`gemini-3.8-flash`).

---

## The problem

Short-form feeds are built to be endless. Minutes become hours, and you rarely notice until it's late. Screen-time apps only tell you *how long* you scrolled. They don't stop you in the moment, and they don't tell you *what* your time went to.

## What Breathe In does

| | |
|---|---|
| ⏱️ **Notices doom scrolling** | It tracks continuous time inside Reels, Shorts and TikTok. Time spent elsewhere in Instagram or YouTube doesn't count. |
| 🫁 **Interrupts mid-scroll** | When you hit your limit, a 5-second countdown fades in, then a guided breathing exercise: breathe in, hold, breathe out, 3 rounds. |
| 🧠 **Shows what you watched** | Gemini reads the captions, creator names and hashtags of the videos you scroll past and sorts them into categories. |
| 💬 **Reflects, gently** | Gemini writes a short, non-judgmental reflection, e.g. *"Most of your scrolling went to gym content. Maybe a short walk or a few push-ups now?"* |
| 🎯 **Lets you choose** | After the break, keep scrolling (the timer resets) or take a break (you go to the home screen). |

**Categories:** Gym & fitness · Memes · Comedy skits · Stand-up comedy · News · Cooking · Music · Guitar · Dance · Sports · Skills & DIY · Learning · Travel · Fashion & beauty · Gaming · Baby videos · Cat videos · Animal videos · Other

## How it works

```
 Instagram / YouTube on screen
            │
            ▼
 Accessibility service ──► detects Reels / Shorts (by view ID)
            │               └─► collects caption + creator text
            ▼
 Session monitor (every 1 s) ──► limit reached? ──► breathing overlay
            │
            ▼
 Gemini 3.8 Flash ──► category for each video (structured JSON)
            │       └─► a two-sentence reflection
            ▼
 Home dashboard: "What you watched" + "Your feed today"
```

- **Kotlin + Jetpack Compose**, Material 3, always-dark calm theme
- **Modular architecture**: `:core:*` contracts plus independent `:feature:*` modules (usage, scroll detection, monitor, overlay, insights, home…)
- **Gemini** through the Google GenAI Java SDK, with structured JSON output so every video gets exactly one known category
- **Works offline too**: without an API key, a keyword matcher on the phone sorts the videos instead
- **Privacy**: only caption and creator text from Reels / Shorts is sent, and only to be sorted. Nothing is read outside those feeds, and today's data stays on the phone and resets at midnight.

---

## 🚀 Try it yourself

### What you need

- [Android Studio](https://developer.android.com/studio) (latest stable)
- An Android phone (Android 8.0+) **or** an emulator with a **Google Play** system image (so you can install Instagram or YouTube)
- A free **Gemini API key** from [Google AI Studio](https://aistudio.google.com/apikey) (optional, but needed for the AI features)

### 1. Get the code

```bash
git clone https://github.com/fannanafahreen/Breathe_In.git
cd Breathe_In
```

### 2. Add your Gemini API key

Open `local.properties` in the project root. Android Studio creates it the first time you open the project. Add this line:

```properties
GEMINI_API_KEY=your-key-here
```

`local.properties` is gitignored, so **your key never gets pushed to GitHub**. You can also set a `GEMINI_API_KEY` environment variable instead.

> No key? The app still works. Videos are sorted by on-device keyword matching instead of Gemini.

### 3. Build and run

Open the project in Android Studio and press **Run ▶**, or from a terminal:

```bash
./gradlew installDebug        # macOS / Linux
gradlew.bat installDebug      # Windows
```

### 4. Grant permissions

The onboarding screen walks you through each one:

1. **Usage access**: to know which app is open
2. **Display over other apps**: to show the breathing break
3. **Notifications**: for the "looking out for you" notification
4. **Accessibility → "Breathe In feed detection"**: to detect Reels / Shorts and read captions

Then tap **Start monitoring**.

---

## 🎬 Demo in 2 minutes

1. On the home screen, tap **Choose apps and limits** and set **YouTube** to **10 s**.
2. Open YouTube and scroll **Shorts**.
3. After 10 seconds: *"You've been on YouTube Shorts for 10 s. Mostly cat videos. Take a breathing break in…"* 🫁
4. Breathe through it, then tap **Keep scrolling** or **Take a break**.
5. Go back to Breathe In and scroll down to **What you watched**. You'll see Gemini's reflection and your categories.
6. Tap **See everything** for every category and the recently watched list.

> **Tip:** in debug builds, **Developer tools** on the home screen has a **Test overlay** button that shows the breathing break instantly.

---

## Project structure

```
app/                  App shell, wiring (AppContainer), navigation
core/model            Data classes (ContentCategory, FeedItem, FeedInsights, …)
core/common           Shared interfaces between features
core/permissions      Permission checks + settings shortcuts
core/settings         Monitored apps and limits (DataStore)
feature/usage         Foreground app + today's usage (UsageStatsManager)
feature/scrolldetect  Accessibility service: Reels/Shorts detection + caption capture
feature/monitor       Foreground service + session tracking
feature/overlay       Breathing exercise overlay
feature/insights      Gemini sorting, "What you watched" card and screen
feature/home          Home dashboard
feature/apps          Pick apps and limits
feature/onboarding    Permission onboarding
feature/debug         Developer tools
```

Run the unit tests with `./gradlew testDebugUnitTest`.

---

## What's next

- 🧭 **Mindful scrolling tools**: help people enjoy the time they *choose* to scroll, with timely reminders that bring focus back
- 🛡️ **Stronger guardrails**: smarter limits that adapt to time of day and to the kind of content you're watching
- 🔬 **Grounded in research**: study the science of doom scrolling so every feature is backed by evidence
- 🔐 **Production-ready AI**: move Gemini calls behind a backend (or Firebase AI Logic) so no API key ships in the app
- 📱 **TikTok content insights** and a post-break check-in: *"Something worth sharing with your family?"*

---

## Notes

- The API key is built into the app for demo purposes only. Don't share debug APKs publicly.
- The view IDs used to detect Reels / Shorts are internal to Instagram and YouTube and can change with app updates. **Developer tools → Capture view IDs** helps find new ones.

<p align="center"><b>Take a breath before the next scroll. 🌬️</b></p>

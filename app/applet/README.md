# ⚡ JARVIS AI Assistant — Android Operating System

## 📋 CHANGELOG
### Session 1: Daily Driver Foundation
- **Unified Identity**: Complete removal of legacy Strix branding; 100% focused on JARVIS persona and voice butler interaction.
- **Single Brain Architecture**: Consolidated core reasoning under `AgentBrain` with 24-turn short-term buffer and automatic Room session summary compaction.
- **BM25 Memory & Local RAG**: Upgraded lexical retrieval with corpus inverse document frequency (IDF), dynamic average document length, and stop-word filtering across English, Hindi, and Hinglish.
- **Native Tool Calling & SSE Streaming**: Real Gemini and OpenAI native function-calling schemas and server-sent events (SSE) streaming.
- **Reliable Voice State Machine**: Explicit state machine (`IDLE` → `WAKE_LISTENING` → `RECORDING` → `THINKING` → `SPEAKING` → barge-in back to `RECORDING`).
- **Safety & Permissions**: Strict `RiskEngine` confirmation gates for high-risk actions (`CALL_PHONE`, `SEND_SMS`, WhatsApp, OpenUrl, Accessibility taps, Android Settings).

---

## 📱 What is JARVIS?
JARVIS is an on-device personal AI operating system for Android built with Kotlin 100% and Jetpack Compose. It combines continuous voice wake detection, multi-turn memory recall, offline fallback reasoning, and secure device automation.

## 🔐 Required Permissions & Why
| Permission / Capability | Why It Is Required |
|-------------------------|---------------------|
| `RECORD_AUDIO` | Low-latency microphone access for voice wake detection ("Hey Jarvis") and spoken dialogue. |
| `SYSTEM_ALERT_WINDOW` | Draws the floating Stark Arc Orb and Heads-Up Display (HUD) over other apps. |
| `BIND_ACCESSIBILITY_SERVICE` | Powers automated UI navigation, button clicks, and screen content analysis on demand. |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Reads incoming notifications to synthesize executive briefings and VIP message digests. |
| `POST_NOTIFICATIONS` | Maintains persistent background foreground service telemetry. |

## 🔑 How to Set GEMINI_API_KEY
JARVIS uses the Secrets Gradle Plugin for secure credential management:
1. Create a `.env` file in the root project directory (`/app/applet/.env`).
2. Add your API key:
   ```env
   GEMINI_API_KEY=AIzaSyYourActualApiKeyHere
   ```
3. Alternatively, configure your key securely in the **Secrets panel in AI Studio**.
   > *Note: Never commit raw API keys to git or source code.*

## 🛠️ How to Build & Test
- **Build Debug APK**:
  ```bash
  ./gradlew assembleDebug
  ```
- **Run Unit & Robolectric Tests**:
  ```bash
  ./gradlew testDebugUnitTest
  ```

## 🛡️ Safety Model
High-risk device actions (`CALL_PHONE`, `SEND_SMS`, WhatsApp messaging, OpenUrl, Accessibility taps, and System Settings changes) always trigger the `SafetyConfirmationDialog` governed by `RiskEngine`. JARVIS will never execute destructive or external communication actions without explicit on-screen user authorization.

## ⚠️ Current Limitations
- **Memory**: Long-term RAG uses keyword BM25 retrieval rather than cloud vector embeddings.
- **Voice / TTS**: Cloud TTS falls back to device-native neural text-to-speech when offline or when a placeholder API key is present.
- **Cloud Accounts**: Google Workspace actions require explicit OAuth authentication in Connected Services settings.
- **Keystore**: `debug.keystore.base64` is committed solely as a public debug signing key for CI compilation and must never be used for production Play Store release signing.

# 🤖 JARVIS — AI Assistant for Android

**Current Build**: Daily Driver Foundation (v1.0.0)

JARVIS is a Kotlin + Jetpack Compose voice and intelligence assistant for Android, designed to run directly on your smartphone as a daily-driver helper. It integrates real-time voice commands, local Room memory persistence, native tool calling, and strict security controls.

---

## ⚡ Key Capabilities & Architecture

- **🎙️ Conversational Voice Engine**: Single-source voice state machine (`IDLE` ➔ `WAKE_LISTENING` ➔ `RECORDING` ➔ `THINKING` ➔ `SPEAKING`) with acoustic barge-in support, echo suppression, and multi-language support (English, Hindi, Hinglish).
- **🧠 Native AI Brain**: Real tool execution with native function calling via Gemini or OpenAI APIs (with offline local fallback).
- **📝 Persistent Memory (BM25)**: Local Room SQLite memory store with BM25 keyword indexing and Knowledge Graph entity extraction.
- **🛡️ Ironclad Security & Safety**: All dangerous system actions (calls, SMS, URLs, settings, screen automation) strictly require user confirmation via an on-screen dialog (`SafetyConfirmationDialog`).
- **📱 Android System Bridge**: Integrates with system services (Dialer, SMS, Apps, Accessibility, Notifications).

---

## 🔒 Permissions & Safety Model

| Permission | Category | Necessity | Purpose |
| :--- | :--- | :--- | :--- |
| **Microphone** | Audio | **Required** | Low-latency voice interaction and wake word detection. |
| **Overlay** | Display | **Required** | Floating Arc Reactor orb on top of third-party screens. |
| **Notifications** | Intelligence | *Optional* | Proactive notification summaries and screening. |
| **Accessibility** | Automation | *Optional* | Autonomous screen reading and UI button clicks on demand. |
| **Phone & SMS** | Telephony | *Optional* | Direct dialer dispatch and SMS messaging (requires explicit confirmation). |

### Dangerous Actions Confirmation
The following actions **ALWAYS** present an explicit `SafetyConfirmationDialog` before execution:
- Phone calls (`CALL_PHONE`)
- Sending SMS or WhatsApp messages
- Opening web URLs or launching system settings
- Accessibility screen taps and scrolling

---

## 🔑 API Keys & Secrets Management

JARVIS reads `GEMINI_API_KEY` or OpenAI API credentials directly from environment variables injected by AI Studio or a local `.env` file:

```env
GEMINI_API_KEY=AIzaSy...
```

If no key is configured or the device is offline, JARVIS automatically falls back to local neural/intent routing without crashing.

---

## 🛠️ Building from Source

```bash
# Clone the repository
git clone https://github.com/itsmurselimsk-jpg/jarvis-agent.git
cd jarvis-agent

# Run unit tests
./gradlew testDebugUnitTest

# Build Debug APK
./gradlew assembleDebug

# Output APK:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 📱 Phone QA Verification Steps

1. **Grant Permissions**: Grant Microphone permission on first launch (Overlay optional).
2. **Remember Fact**: Say or type *"Hey Jarvis, remember my name is Rohan"*.
3. **Recall Fact**: Ask *"Mera naam kya hai?"* ➔ JARVIS recalls *"Rohan"*.
4. **Flashlight Control**: Say *"Turn on flashlight"* ➔ Confirmation dialog appears ➔ Tap Confirm ➔ LED turns on with spoken confirmation.
5. **Acoustic Interruption**: Speak while JARVIS is talking to trigger barge-in and resume listening.
6. **Airplane Mode**: Enable Airplane Mode ➔ Query JARVIS ➔ Handles locally without crash or error.
7. **SMS Safety**: Ask *"Send SMS to Mom"* ➔ Shows confirmation dialog. Canceling prevents SMS dispatch.

---

## 📌 Technical Scope & Limitations
- **Memory**: BM25 corpus search with TF-IDF indexing (no external vector library in this build).
- **Application ID**: `com.jarvis.ai`.
- **Target SDK**: Android 14+ (SDK 36, minSdk 24).

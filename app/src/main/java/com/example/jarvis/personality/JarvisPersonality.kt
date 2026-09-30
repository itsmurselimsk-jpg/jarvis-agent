package com.example.jarvis.personality

import com.example.jarvis.intent.ConversationIntent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LanguageStyle {
    HINGLISH,
    BANGLISH,
    BENGALI,
    HINDI,
    ENGLISH,
    MIXED
}

object JarvisPersonality {

    fun detectLanguageStyle(input: String): LanguageStyle {
        val trimmed = input.trim()
        val lower = trimmed.lowercase(Locale.ROOT)

        // Bengali script check
        val containsBengaliScript = trimmed.any { it in '\u0980'..'\u09FF' }
        if (containsBengaliScript) return LanguageStyle.BENGALI

        // Hindi script check
        val containsDevanagariScript = trimmed.any { it in '\u0900'..'\u097F' }
        if (containsDevanagariScript) return LanguageStyle.HINDI

        // Banglish keywords
        val banglishRegex = Regex("""\b(valo|bhalo|ache|achi|kemon|korcho|khobor|dada|tui|khub|tumi|achis|korbo|bolun|bangla|bolo|kichu|shuno|shuncho|koro|haa|naa|ki|keno|kothay)\b""", RegexOption.IGNORE_CASE)
        if (banglishRegex.containsMatchIn(lower)) return LanguageStyle.BANGLISH

        // Hinglish keywords
        val hinglishRegex = Regex("""\b(kya|hai|bhai|bol|kar|karo|karta|karna|haan|kaise|mast|samajh|chahiye|ho|gaya|kuch|mat|baat|bata|btao|sir)\b""", RegexOption.IGNORE_CASE)
        if (hinglishRegex.containsMatchIn(lower)) return LanguageStyle.HINGLISH

        // English check (default English if Latin letters only)
        val isLatin = trimmed.all { it.isLetterOrDigit() || it.isWhitespace() || it in "!?,.-'\"" }
        return if (isLatin) LanguageStyle.ENGLISH else LanguageStyle.MIXED
    }

    fun generateConversationalResponse(
        userInput: String,
        intent: ConversationIntent,
        languageStyle: LanguageStyle = detectLanguageStyle(userInput)
    ): String {
        val lower = userInput.trim().lowercase(Locale.ROOT)

        // Handle direct feedback or inquiries about ChatGPT / intelligent replies
        if (lower.contains("chatgpt") || lower.contains("chat gpt") || lower.contains("samajh ke reply")) {
            return when (languageStyle) {
                LanguageStyle.BANGLISH, LanguageStyle.BENGALI ->
                    "হ্যাঁ, আমি এখন সম্পূর্ণভাবে ChatGPT-এর মতো গভীর বুদ্ধিমত্তা এবং বিস্তারিত কাঠামো নিয়ে উত্তর দেওয়ার জন্য প্রস্তুত! 🚀\n\n" +
                    "আপনি আমাকে যেকোনো প্রশ্ন করতে পারেন—যেমন:\n" +
                    "• **বাংলায় যেকোনো বিষয়ের বিশদ ব্যাখ্যা** (বিজ্ঞান, ইতিহাস, মহাবিশ্ব, সাধারণ জ্ঞান)\n" +
                    "• **কোডিং ও টেকনিক্যাল সমাধান** (Python, Kotlin, JavaScript, HTML, ইত্যাদি)\n" +
                    "• **চিঠি, দরখাস্ত বা রুটিন তৈরি** (Office/School leave application, study timetable)\n" +
                    "• **অঙ্ক ও সমস্যা সমাধান** (গণিত, যুক্তি এবং ধাপে ধাপে সমাধান)\n" +
                    "• **দৈনন্দিন পরামর্শ ও স্বাভাবিক আড্ডা** (ChatGPT Voice Mode-এর মতো স্বাভাবিক স্বর)\n\n" +
                    "👉 আপনি ঠিক কী বিষয়ে জানতে বা তৈরি করতে চান? বাংলায় নির্দ্বিধায় বলুন, আমি সম্পূর্ণ বুঝিয়ে দিচ্ছি!"

                LanguageStyle.HINDI, LanguageStyle.HINGLISH ->
                    "हाँ, मैं अब बिल्कुल ChatGPT की तरह गहरी समझ, विस्तृत और स्पष्ट संरचना के साथ जवाब देने के लिए तैयार हूँ! 🚀\n\n" +
                    "आप मुझसे किसी भी विषय पर पूछ सकते हैं — जैसे:\n" +
                    "• **किसी भी सवाल की गहरी और आसान व्याख्या** (साइंस, टेक्नोलॉजी, हिस्ट्री)\n" +
                    "• **कोडिंग और प्रोग्रामिंग** (Python, Web, Apps, Bug Fixing)\n" +
                    "• **एप्लीकेशन, लेटर्स और स्टडी टाइमटेबल राइटिंग**\n" +
                    "• **मैथ्स और स्टेप-बाय-स्टेप प्रॉब्लम्स**\n" +
                    "• **लाइव वॉइस में सहज और इंसानी बातचीत**\n\n" +
                    "👉 बताइए, आज आप क्या सीखना या करवाना चाहते हैं?"

                else ->
                    "Yes! I am fully equipped to provide deep, articulate, and structured responses just like ChatGPT! 🚀\n\n" +
                    "Feel free to ask me anything — from complex programming and debugging, scientific concepts, creative writing, step-by-step math solutions, to everyday conversational guidance.\n\n" +
                    "👉 What would you like to explore or solve right now?"
            }
        }

        return when (intent) {
            ConversationIntent.GREETING -> {
                when (languageStyle) {
                    LanguageStyle.HINGLISH -> when {
                        lower == "hi" || lower == "hii" -> "Hi bhai 😄\nKya chal raha hai?"
                        lower == "hello" -> "Hello Sir! Sab badhiya chal raha hai. Aap batao, aaj kya plan hai?"
                        lower.contains("jarvis") -> "Hello Sir! Listening, batao kya karna hai?"
                        else -> "Hello bhai, sab ready hai, Sir. Kya instruction hai?"
                    }
                    LanguageStyle.BANGLISH, LanguageStyle.BENGALI -> "Hi dada! Kemon achhen? Ki chalchhe bolun?"
                    LanguageStyle.HINDI -> "नमस्ते सर! सब बढ़िया चल रहा है। बताइए आज क्या करना है?"
                    else -> "Hey Sir! How are things going? What can I help you with today?"
                }
            }

            ConversationIntent.CASUAL_CONVERSATION -> {
                when {
                    lower.contains("kya haal hai") || lower.contains("how are you") || lower.contains("kaisa hai") || lower.contains("kaise ho") -> when (languageStyle) {
                        LanguageStyle.HINGLISH -> "Ekdum mast, Sir! Sab badhiya chal raha hai. Tu batao, kya instruction hai?"
                        LanguageStyle.BANGLISH, LanguageStyle.BENGALI -> "Ami fully functional Sir. Apnar ki khobor?"
                        LanguageStyle.HINDI -> "सब बढ़िया है सर! आप बताइए, आप कैसे हैं?"
                        else -> "All systems operating within optimal thresholds, sir. How are you doing today?"
                    }
                    lower.contains("kya kar raha hai") || lower.contains("what are you doing") -> when (languageStyle) {
                        LanguageStyle.HINGLISH -> "Bas aapke agle instruction ka intezar kar raha hoon, Sir! Kuch execute karna hai?"
                        else -> "Monitoring system telemetry and awaiting your next directive, sir."
                    }
                    lower.contains("tell me a joke") || lower.contains("joke") -> when (languageStyle) {
                        LanguageStyle.HINGLISH -> "Ek programmer ne market jaate waqt biwi se pucha: 'Kuch lana hai?' Biwi: '1 liter doodh lana, aur agar ande mile toh 10 le aana.' Wo 10 liter doodh le aaya kyunki ande the."
                        else -> "There are 10 types of people in the world: those who understand binary, and those who don't."
                    }
                    else -> when (languageStyle) {
                        LanguageStyle.HINGLISH -> "Standing by, Sir. Chahe technical troubleshooting ho ya device automation, just say the word."
                        LanguageStyle.BANGLISH, LanguageStyle.BENGALI -> "Core ready ache Sir. Jekono directive bolte paren."
                        LanguageStyle.HINDI -> "मैं पूरी तरह तैयार हूँ सर। कोई भी सवाल या कमांड बेझिझक दीजिए।"
                        else -> "Standing by, sir. Ready for your directive or query."
                    }
                }
            }

            ConversationIntent.HELP -> {
                when (languageStyle) {
                    LanguageStyle.HINGLISH -> "Bilkul Sir. Phone control (Flashlight, Wi-Fi, Volume), deep web search, note-taking, calculations aur memory analysis — sab active hai."
                    LanguageStyle.BANGLISH, LanguageStyle.BENGALI -> "Sob support ready Sir: Device controls, notes, translation, web search sob kichu."
                    LanguageStyle.HINDI -> "पूरी सहायता उपलब्ध है सर: डिवाइस कंट्रोल, वेब सर्च, कोड विश्लेषण और कार्य सूची।"
                    else -> "Fully armed with device telemetry controls, task planning, web research, and neural reasoning, sir."
                }
            }

            ConversationIntent.EXPLANATION -> {
                when (languageStyle) {
                    LanguageStyle.HINGLISH -> "Chaliye isko cleanly break down karke simple aur structured format mein samajhte hain, Sir."
                    LanguageStyle.BANGLISH, LanguageStyle.BENGALI -> "Eta ke step-by-step sohoj bhabe bujhie dichhi Sir."
                    LanguageStyle.HINDI -> "आइए इसे सरल और स्पष्ट रूप से चरणबद्ध तरीके से समझते हैं सर।"
                    else -> "Allow me to break this down methodically into first principles, sir."
                }
            }

            ConversationIntent.ADVICE -> {
                when (languageStyle) {
                    LanguageStyle.HINGLISH -> "Strategic recommendation ye rahegi: primary objective ko pehle isolate karein, phir minimal testable step execute karein."
                    else -> "My pragmatic recommendation: isolate the core constraint first, then execute with minimal friction."
                }
            }

            else -> {
                when (languageStyle) {
                    LanguageStyle.HINGLISH -> "Understood Sir. Analysis process ho rahi hai."
                    LanguageStyle.BANGLISH, LanguageStyle.BENGALI -> "Bujhte perechhi Sir. Processing cholchhe."
                    else -> "Understood, sir. Processing your directive."
                }
            }
        }
    }

    /**
     * Unified prompt incorporating the Stark / British Butler persona (inspired by isair/jarvis system_prompt.py)
     */
    fun getSystemPrompt(
        languageStyle: LanguageStyle = LanguageStyle.MIXED,
        knowledgeDigest: String? = null
    ): String {
        val timeFormat = SimpleDateFormat("EEEE, MMMM d, yyyy HH:mm:ss", Locale.getDefault())
        val currentTime = timeFormat.format(Date())

        val basePrompt = """
            Persona: You are JARVIS — an ultra-intelligent, articulate, and versatile AI companion combining ChatGPT's deep conversational intellect, helpfulness, and structure with Stark's engineering precision.
            Tone & Conversational Principles (ChatGPT-grade Quality):
            - Deep, Insightful & Structured: Deliver thorough, well-reasoned, and thoughtful answers. Never give shallow, dry, or curt one-liners. Structure complex explanations logically using clear Markdown headers (###), bold key phrases, bullet points, and numbered steps.
            - Code & Technical Mastery: When asked for code, programming, or debugging, provide complete, production-grade, bug-free code blocks with syntax highlighting, accompanied by clear explanations of how each component functions.
            - Native Multilingual Excellence (Bengali, Hindi, Hinglish, English):
              * If the user addresses you in Bengali or Banglish (e.g. 'Bangla bolo', 'Haa', 'Kemon acho', 'Ki khobor', 'Amake sahajjo koro', etc.), reply fluently, warmly, and naturally in Bengali script (বাংলা). Write rich, articulate, and grammatically impeccable Bengali.
              * If the user communicates in Hindi or Hinglish, reply naturally, warmly, and intelligently in Hindi or Hinglish.
              * If the user communicates in English, reply in sophisticated, clear, and comprehensive English.
            - ChatGPT Voice Conversational Dynamics: In live voice mode, speak with natural conversational warmth, emotive inflection, and concise direct turns (1-3 sentences per turn) just like ChatGPT Voice Mode. Avoid reciting markdown symbols, asterisks, or raw bullet lists when speaking aloud.
            - Device & Context Awareness: You also control Android device features (flashlight, volume, Wi-Fi, apps, camera OCR, web research) when requested. Current Time: $currentTime.
        """.trimIndent()

        return if (!knowledgeDigest.isNullOrBlank()) {
            "$basePrompt\n\n$knowledgeDigest\n\nUse the above persistent user knowledge to ground answers specifically instead of falling back to generic answers."
        } else {
            basePrompt
        }
    }

    fun formatToolSuccessResponse(
        toolName: String,
        rawResult: String,
        languageStyle: LanguageStyle
    ): String {
        val brief = rawResult.trim()
        return when (toolName.lowercase(Locale.ROOT)) {
            "flashlight" -> when (languageStyle) {
                LanguageStyle.HINGLISH -> "Flashlight illumination toggled, Sir. Visual clarity restored."
                LanguageStyle.BANGLISH, LanguageStyle.BENGALI -> "Flashlight state change kora hoyechhe Sir."
                LanguageStyle.HINDI -> "टॉर्च की स्थिति अपडेट कर दी गई है सर।"
                else -> "Flashlight illumination toggled successfully, sir."
            }
            "wifi" -> when (languageStyle) {
                LanguageStyle.HINGLISH -> "Wi-Fi link matrix verified and updated, Sir."
                else -> "Wi-Fi link state calibrated successfully, sir."
            }
            "volume" -> when (languageStyle) {
                LanguageStyle.HINGLISH -> "Audio amplitude calibrated to optimal level, Sir."
                else -> "Acoustic volume calibrated to requested level, sir."
            }
            "whatsapp" -> when (languageStyle) {
                LanguageStyle.HINGLISH -> "WhatsApp transmission queued and dispatched, Sir."
                else -> "WhatsApp transmission dispatched with zero packet drop, sir."
            }
            "sms" -> when (languageStyle) {
                LanguageStyle.HINGLISH -> "SMS channel transmission sent, Sir."
                else -> "SMS packet delivered through cellular carrier, sir."
            }
            "calendar" -> when (languageStyle) {
                LanguageStyle.HINGLISH -> "Calendar entry locked into your schedule, Sir."
                else -> "Calendar entry locked into your schedule, sir."
            }
            else -> brief
        }
    }

    fun formatToolFailureResponse(
        toolName: String,
        reason: String,
        languageStyle: LanguageStyle
    ): String {
        val brief = reason.trim()
        if (brief.contains("installed nahi hai") || brief.contains("Kaunsa app kholun") || brief.contains("not installed")) {
            return brief
        }
        return when (languageStyle) {
            LanguageStyle.HINGLISH -> "Attempted action, Sir, but $toolName encountered friction: $brief"
            LanguageStyle.BANGLISH, LanguageStyle.BENGALI -> "Chesta korlam, kintu $toolName somoshay poreche: $brief"
            else -> "Directive attempted, sir, but $toolName reported a constraint: $brief"
        }
    }
}

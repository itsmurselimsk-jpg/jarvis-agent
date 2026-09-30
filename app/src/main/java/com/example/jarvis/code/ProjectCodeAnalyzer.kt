package com.example.jarvis.code

/**
 * Multi-file and Android project relationship inspector.
 * Analyzes explicitly supplied or attached source files (Kotlin, Java, Gradle, AndroidManifest).
 * STRICT POLICY: Never automatically scans the entire filesystem or unrequested directories.
 */
object ProjectCodeAnalyzer {

    data class ExplicitFile(
        val fileName: String,
        val content: String
    )

    data class ProjectAnalysisResult(
        val filesAnalyzed: List<String>,
        val declaredPermissions: List<String>,
        val apiUsagePermissionsNeeded: List<String>,
        val missingPermissions: List<String>,
        val buildIssues: List<String>,
        val overallFindings: List<CodeFinding>,
        val summary: String
    )

    fun analyzeExplicitFiles(files: List<ExplicitFile>): ProjectAnalysisResult {
        val findings = mutableListOf<CodeFinding>()
        val declaredPermissions = mutableListOf<String>()
        val permissionsNeeded = mutableListOf<String>()
        val buildIssues = mutableListOf<String>()

        // 1. Analyze Manifest for declared permissions
        val manifestFile = files.firstOrNull { it.fileName.equals("AndroidManifest.xml", ignoreCase = true) || it.content.contains("<manifest") }
        if (manifestFile != null) {
            val permRegex = Regex("""<uses-permission\s+android:name="([^"]+)"""")
            permRegex.findAll(manifestFile.content).forEach {
                val p = it.groupValues[1].substringAfterLast('.')
                declaredPermissions.add(p)
            }
        }

        // 2. Scan Kotlin / Java source files for API usages requiring permissions
        val sourceFiles = files.filter { it.fileName.endsWith(".kt") || it.fileName.endsWith(".java") }
        sourceFiles.forEach { file ->
            val content = file.content
            if (content.contains("Camera") || content.contains("takePicture") || content.contains("ImageCapture")) {
                permissionsNeeded.add("CAMERA")
            }
            if (content.contains("AudioRecord") || content.contains("MediaRecorder") || content.contains("RECORD_AUDIO")) {
                permissionsNeeded.add("RECORD_AUDIO")
            }
            if (content.contains("LocationManager") || content.contains("FusedLocationProviderClient")) {
                permissionsNeeded.add("ACCESS_FINE_LOCATION")
            }
            if (content.contains("BluetoothAdapter") || content.contains("BluetoothManager")) {
                permissionsNeeded.add("BLUETOOTH_CONNECT")
            }
            if (content.contains("HttpURLConnection") || content.contains("OkHttpClient") || content.contains("Retrofit")) {
                permissionsNeeded.add("INTERNET")
            }

            // Run deterministic static checks on individual file
            val fileRes = DeterministicCodeAnalyzer.analyze(file.content, file.fileName)
            findings.addAll(fileRes.findings)
        }

        // 3. Scan Gradle files for dependencies and plugins
        val gradleFiles = files.filter { it.fileName.contains("build.gradle") }
        gradleFiles.forEach { gradle ->
            if (gradle.content.contains("local.properties")) {
                buildIssues.add("Warning in ${gradle.fileName}: Avoid hardcoded local.properties references in Gradle scripts.")
            }
            if (gradle.content.contains("com.google.android.gms") && !files.any { it.fileName.contains("google-services.json") }) {
                buildIssues.add("Notice in ${gradle.fileName}: Google Play Services dependencies found. Verify google-services.json is present in app root if required.")
            }
        }

        // 4. Permission mismatch check
        val missingPermissions = mutableListOf<String>()
        if (manifestFile != null) {
            val distinctNeeded = permissionsNeeded.distinct()
            distinctNeeded.forEach { needed ->
                if (!declaredPermissions.contains(needed) && !declaredPermissions.any { it.equals(needed, ignoreCase = true) }) {
                    missingPermissions.add(needed)
                    findings.add(
                        CodeFinding(
                            severity = FindingSeverity.HIGH,
                            category = FindingCategory.API_USAGE,
                            file = manifestFile.fileName,
                            description = "Missing manifest permission: $needed",
                            explanation = "Source code utilizes APIs requiring '$needed' permission, but it is not declared in AndroidManifest.xml.",
                            suggestedFix = "Add '<uses-permission android:name=\"android.permission.$needed\" />' to AndroidManifest.xml."
                        )
                    )
                }
            }
        }

        val summary = buildString {
            append("Explicit Project Analysis: ${files.size} file(s) evaluated. ")
            if (manifestFile != null) {
                append("Found ${declaredPermissions.size} declared permission(s). ")
            }
            if (missingPermissions.isNotEmpty()) {
                append("Identified ${missingPermissions.size} missing permission declaration(s): ${missingPermissions.joinToString()}. ")
            }
            append("Total code findings across files: ${findings.size}.")
        }

        return ProjectAnalysisResult(
            filesAnalyzed = files.map { it.fileName },
            declaredPermissions = declaredPermissions,
            apiUsagePermissionsNeeded = permissionsNeeded.distinct(),
            missingPermissions = missingPermissions,
            buildIssues = buildIssues,
            overallFindings = findings,
            summary = summary
        )
    }
}

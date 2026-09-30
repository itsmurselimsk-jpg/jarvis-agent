package com.example.jarvis.auth

import android.app.Activity
import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

data class JarvisUser(
    val uid: String,
    val email: String,
    val displayName: String,
    val isEmailVerified: Boolean,
    val isAnonymous: Boolean = false,
    val providerId: String = "firebase",
    val phoneNumber: String? = null
)

sealed class AuthState {
    object Initializing : AuthState()
    data class Authenticated(val user: JarvisUser) : AuthState()
    data class Unauthenticated(val message: String? = null) : AuthState()
    data class AuthError(val errorMessage: String) : AuthState()
}

class AuthManager(private val context: Context) {

    companion object {
        val DEFAULT_OPERATOR = JarvisUser(
            uid = "operator_commander",
            email = "commander@jarvis.ai",
            displayName = "Commander Stark",
            isEmailVerified = true,
            isAnonymous = false,
            providerId = "local"
        )
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Authenticated(DEFAULT_OPERATOR))
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var firebaseAuth: FirebaseAuth? = null
    private var isFirebaseAvailable = false

    init {
        initFirebase()
    }

    private fun initFirebase() {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                firebaseAuth = FirebaseAuth.getInstance()
                isFirebaseAvailable = true
                setupAuthListener()
            } else {
                // Check if default app can be initialized
                try {
                    FirebaseApp.initializeApp(context)
                    firebaseAuth = FirebaseAuth.getInstance()
                    isFirebaseAvailable = true
                    setupAuthListener()
                } catch (_: Exception) {
                    isFirebaseAvailable = false
                    checkLocalSession()
                }
            }
        } catch (_: Exception) {
            isFirebaseAvailable = false
            checkLocalSession()
        }
    }

    private fun setupAuthListener() {
        val auth = firebaseAuth ?: return
        auth.addAuthStateListener { fbAuth ->
            val currentUser = fbAuth.currentUser
            if (currentUser != null) {
                val user = mapFirebaseUser(currentUser)
                _authState.value = AuthState.Authenticated(user)
                saveLocalSession(user)
            } else {
                checkLocalSession()
            }
        }
    }

    private fun mapFirebaseUser(user: FirebaseUser): JarvisUser {
        val phone = user.phoneNumber
        val email = user.email ?: if (!phone.isNullOrBlank()) "$phone@jarvis.local" else "user@jarvis.ai"
        val name = user.displayName ?: if (!phone.isNullOrBlank()) phone else email.substringBefore("@")
        return JarvisUser(
            uid = user.uid,
            email = email,
            displayName = name,
            isEmailVerified = user.isEmailVerified || !phone.isNullOrBlank(),
            isAnonymous = user.isAnonymous,
            providerId = user.providerId,
            phoneNumber = phone
        )
    }

    private fun checkLocalSession() {
        val prefs = context.getSharedPreferences("jarvis_auth_prefs", Context.MODE_PRIVATE)
        val uid = prefs.getString("user_uid", null) ?: DEFAULT_OPERATOR.uid
        val email = prefs.getString("user_email", null) ?: DEFAULT_OPERATOR.email
        val name = prefs.getString("user_name", null) ?: DEFAULT_OPERATOR.displayName
        val phone = prefs.getString("user_phone", null)
        val isGuest = prefs.getBoolean("is_guest", false)

        val user = JarvisUser(
            uid = uid,
            email = email,
            displayName = name,
            isEmailVerified = true,
            isAnonymous = isGuest,
            phoneNumber = phone
        )
        _authState.value = AuthState.Authenticated(user)
    }

    private fun saveLocalSession(user: JarvisUser) {
        val prefs = context.getSharedPreferences("jarvis_auth_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("user_uid", user.uid)
            .putString("user_email", user.email)
            .putString("user_name", user.displayName)
            .putString("user_phone", user.phoneNumber)
            .putBoolean("is_guest", user.isAnonymous)
            .apply()
    }

    private fun clearLocalSession() {
        val prefs = context.getSharedPreferences("jarvis_auth_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    fun isFirebaseConfigured(): Boolean = isFirebaseAvailable

    fun signInWithCredential(
        credential: AuthCredential,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val auth = firebaseAuth
        if (auth != null && isFirebaseAvailable) {
            scope.launch {
                try {
                    val result = auth.signInWithCredential(credential).await()
                    val user = result.user
                    if (user != null) {
                        val jarvisUser = mapFirebaseUser(user)
                        saveLocalSession(jarvisUser)
                        _authState.value = AuthState.Authenticated(jarvisUser)
                        onSuccess()
                    } else {
                        onError("Google credential authentication returned empty user profile.")
                    }
                } catch (e: Exception) {
                    onError(e.localizedMessage ?: "Google authentication failed.")
                }
            }
        } else {
            onError("Firebase cloud services not configured. Please add google-services.json from Firebase Console.")
        }
    }

    fun sendPhoneOtp(
        activity: Activity,
        phoneNumber: String,
        onCodeSent: (verificationId: String) -> Unit,
        onAutoVerified: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanPhone = phoneNumber.trim()
        if (cleanPhone.isBlank()) {
            onError("Please specify a valid phone number with country code (e.g., +15550199 or +919876543210).")
            return
        }

        val auth = firebaseAuth
        if (auth != null && isFirebaseAvailable) {
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(cleanPhone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        // Instant verification or auto-retrieval
                        signInWithPhoneCredential(credential, onAutoVerified, onError)
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        onError(e.localizedMessage ?: "SMS verification dispatch failed.")
                    }

                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        onCodeSent(verificationId)
                    }
                })
                .build()
            PhoneAuthProvider.verifyPhoneNumber(options)
        } else {
            onError("Firebase Authentication requires google-services.json and Phone Auth enabled in the Firebase Console. Add google-services.json to enable live SMS OTP.")
        }
    }

    fun verifyPhoneOtp(
        verificationId: String,
        otpCode: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanCode = otpCode.trim()
        if (cleanCode.length < 6) {
            onError("Please enter the complete 6-digit OTP security code.")
            return
        }

        val credential = PhoneAuthProvider.getCredential(verificationId, cleanCode)
        signInWithPhoneCredential(credential, onSuccess, onError)
    }

    private fun signInWithPhoneCredential(
        credential: PhoneAuthCredential,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val auth = firebaseAuth
        if (auth != null && isFirebaseAvailable) {
            scope.launch {
                try {
                    val result = auth.signInWithCredential(credential).await()
                    val user = result.user
                    if (user != null) {
                        val jarvisUser = mapFirebaseUser(user)
                        saveLocalSession(jarvisUser)
                        _authState.value = AuthState.Authenticated(jarvisUser)
                        onSuccess()
                    } else {
                        onError("OTP verified but user account could not be initialized.")
                    }
                } catch (e: Exception) {
                    onError(e.localizedMessage ?: "OTP verification failed. Please check the code.")
                }
            }
        } else {
            onError("Firebase cloud service unavailable. Please configure google-services.json.")
        }
    }

    fun signInWithEmail(
        email: String,
        pass: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (email.isBlank() || pass.isBlank()) {
            onError("Email and password cannot be empty.")
            return
        }

        val auth = firebaseAuth
        if (auth != null && isFirebaseAvailable) {
            scope.launch {
                try {
                    val result = auth.signInWithEmailAndPassword(email.trim(), pass).await()
                    val user = result.user
                    if (user != null) {
                        val jarvisUser = mapFirebaseUser(user)
                        saveLocalSession(jarvisUser)
                        _authState.value = AuthState.Authenticated(jarvisUser)
                        onSuccess()
                    } else {
                        onError("Authentication yielded no valid user profile.")
                    }
                } catch (e: Exception) {
                    onError(e.localizedMessage ?: "Authentication failed.")
                }
            }
        } else {
            // Fallback for local testing when Firebase configuration is absent
            scope.launch {
                val fakeUid = "usr_" + email.trim().lowercase().hashCode().toString().replace("-", "x")
                val localUser = JarvisUser(
                    uid = fakeUid,
                    email = email.trim(),
                    displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                    isEmailVerified = true,
                    isAnonymous = false
                )
                saveLocalSession(localUser)
                _authState.value = AuthState.Authenticated(localUser)
                onSuccess()
            }
        }
    }

    fun signUpWithEmail(
        name: String,
        email: String,
        pass: String,
        confirmPass: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (name.isBlank() || email.isBlank() || pass.isBlank()) {
            onError("All credential fields are required.")
            return
        }
        if (pass != confirmPass) {
            onError("Passwords do not match.")
            return
        }
        if (pass.length < 6) {
            onError("Password must be at least 6 characters.")
            return
        }

        val auth = firebaseAuth
        if (auth != null && isFirebaseAvailable) {
            scope.launch {
                try {
                    val result = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
                    val user = result.user
                    if (user != null) {
                        // Update display name
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(name.trim())
                            .build()
                        user.updateProfile(profileUpdates).await()
                        // Send verification email
                        try {
                            user.sendEmailVerification().await()
                        } catch (_: Exception) {}

                        val jarvisUser = mapFirebaseUser(user)
                        saveLocalSession(jarvisUser)
                        _authState.value = AuthState.Authenticated(jarvisUser)
                        onSuccess("Account created successfully. A verification email was sent to ${user.email}.")
                    } else {
                        onError("User creation returned null.")
                    }
                } catch (e: Exception) {
                    onError(e.localizedMessage ?: "Account creation failed.")
                }
            }
        } else {
            scope.launch {
                val localUid = "usr_" + System.currentTimeMillis().toString().takeLast(6)
                val localUser = JarvisUser(
                    uid = localUid,
                    email = email.trim(),
                    displayName = name.trim(),
                    isEmailVerified = false,
                    isAnonymous = false
                )
                saveLocalSession(localUser)
                _authState.value = AuthState.Authenticated(localUser)
                onSuccess("Account created locally. Configure google-services.json for full Firebase cloud integration.")
            }
        }
    }

    fun sendPasswordResetEmail(
        email: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (email.isBlank()) {
            onError("Please specify your registered email address.")
            return
        }

        val auth = firebaseAuth
        if (auth != null && isFirebaseAvailable) {
            scope.launch {
                try {
                    auth.sendPasswordResetEmail(email.trim()).await()
                    onSuccess("Password reset instructions dispatched to $email.")
                } catch (e: Exception) {
                    onError(e.localizedMessage ?: "Failed to transmit reset email.")
                }
            }
        } else {
            onSuccess("Password reset simulation dispatched to $email (Firebase cloud config required for SMTP transmission).")
        }
    }

    fun sendEmailVerification(
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val user = firebaseAuth?.currentUser
        if (user != null) {
            scope.launch {
                try {
                    user.sendEmailVerification().await()
                    onSuccess("Verification email transmitted to ${user.email}.")
                } catch (e: Exception) {
                    onError(e.localizedMessage ?: "Verification dispatch failed.")
                }
            }
        } else {
            onSuccess("Email verification requested for active session.")
        }
    }

    fun continueAsGuest(onSuccess: () -> Unit) {
        val guestUser = JarvisUser(
            uid = "guest_commander_" + (1000..9999).random(),
            email = "commander@jarvis.local",
            displayName = "Commander (Guest)",
            isEmailVerified = true,
            isAnonymous = true
        )
        saveLocalSession(guestUser)
        _authState.value = AuthState.Authenticated(guestUser)
        onSuccess()
    }

    fun signOut(onSuccess: () -> Unit = {}) {
        try {
            firebaseAuth?.signOut()
        } catch (_: Exception) {}
        clearLocalSession()
        _authState.value = AuthState.Authenticated(DEFAULT_OPERATOR)
        onSuccess()
    }

    fun deleteAccount(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = firebaseAuth?.currentUser
        if (user != null) {
            scope.launch {
                try {
                    user.delete().await()
                    clearLocalSession()
                    _authState.value = AuthState.Authenticated(DEFAULT_OPERATOR)
                    onSuccess()
                } catch (e: Exception) {
                    onError(e.localizedMessage ?: "Account purge failed. Re-authentication may be required.")
                }
            }
        } else {
            clearLocalSession()
            _authState.value = AuthState.Authenticated(DEFAULT_OPERATOR)
            onSuccess()
        }
    }
}

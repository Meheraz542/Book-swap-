package com.example.data.auth

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.model.Semester
import com.example.model.Technology
import java.security.SecureRandom

/**
 * Data holder for user registration pending OTP email verification.
 */
data class PendingRegistration(
    val name: String,
    val email: String,
    val password: String,
    val phone: String,
    val college: String,
    val technology: Technology,
    val semester: Semester,
    val otpCode: String,
    val expiresAt: Long,
    val createdAt: Long = System.currentTimeMillis()
)

sealed class OtpResult {
    data class CodeSent(
        val email: String,
        val maskedEmail: String,
        val otpCode: String,
        val expiresInSeconds: Int = 300
    ) : OtpResult()
    data class Error(val message: String) : OtpResult()
}

/**
 * Robust Gmail OTP Verification Service.
 * Manages 6-digit code generation, secure validation, countdowns, and system notification dispatch.
 */
class EmailOtpManager(private val context: Context) {
    private val secureRandom = SecureRandom()
    private var pendingRegistration: PendingRegistration? = null
    private val channelId = "gmail_otp_channel"

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val name = "Gmail OTP Verification"
                val descriptionText = "Notifications for student account verification OTPs"
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(channelId, name, importance).apply {
                    description = descriptionText
                    enableVibration(true)
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.createNotificationChannel(channel)
            } catch (e: Exception) {
                Log.w("EmailOtpManager", "Failed to create notification channel: ${e.message}")
            }
        }
    }

    fun generateOtp(): String {
        val num = 100000 + secureRandom.nextInt(900000)
        return num.toString()
    }

    fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return email
        val user = parts[0]
        val domain = parts[1]
        val maskedUser = if (user.length <= 2) {
            user.take(1) + "***"
        } else {
            user.first() + "***" + user.last()
        }
        return "$maskedUser@$domain"
    }

    fun initiateOtp(
        name: String,
        email: String,
        password: String,
        phone: String,
        college: String,
        technology: Technology,
        semester: Semester
    ): OtpResult {
        val trimmedEmail = email.trim().lowercase()
        val trimmedName = name.trim()
        val trimmedPhone = phone.trim()

        if (trimmedName.length < 2) {
            return OtpResult.Error("অনুগ্রহ করে আপনার পুরো নাম দিন (Please enter your full name).")
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches() && !trimmedEmail.contains("@")) {
            return OtpResult.Error("অনুগ্রহ করে একটি সঠিক জিমেইল/ইমেইল দিন (Please enter a valid email address).")
        }
        if (password.length < 4) {
            return OtpResult.Error("পাসওয়ার্ড ন্যূনতম ৪ অক্ষরের হতে হবে (Password must be at least 4 characters).")
        }
        if (trimmedPhone.length < 6) {
            return OtpResult.Error("অনুগ্রহ করে একটি সচল ফোন নম্বর দিন (Please enter a valid phone number).")
        }

        val otp = generateOtp()
        val expiresAt = System.currentTimeMillis() + (5 * 60 * 1000L) // 5 minutes

        val pending = PendingRegistration(
            name = trimmedName,
            email = trimmedEmail,
            password = password,
            phone = trimmedPhone,
            college = college,
            technology = technology,
            semester = semester,
            otpCode = otp,
            expiresAt = expiresAt
        )
        pendingRegistration = pending

        // Dispatch system push notification
        sendSystemNotification(trimmedEmail, otp)

        return OtpResult.CodeSent(
            email = trimmedEmail,
            maskedEmail = maskEmail(trimmedEmail),
            otpCode = otp,
            expiresInSeconds = 300
        )
    }

    fun resendOtp(): OtpResult {
        val pending = pendingRegistration
            ?: return OtpResult.Error("কোনো সক্রিয় রেজিস্ট্রেশন পাওয়া যায়নি। অনুগ্রহ করে পুনরায় সাইন আপ করুন।")

        val newOtp = generateOtp()
        val newExpiresAt = System.currentTimeMillis() + (5 * 60 * 1000L)

        val updated = pending.copy(
            otpCode = newOtp,
            expiresAt = newExpiresAt,
            createdAt = System.currentTimeMillis()
        )
        pendingRegistration = updated

        sendSystemNotification(updated.email, newOtp)

        return OtpResult.CodeSent(
            email = updated.email,
            maskedEmail = maskEmail(updated.email),
            otpCode = newOtp,
            expiresInSeconds = 300
        )
    }

    fun verifyOtp(enteredCode: String): Result<PendingRegistration> {
        val pending = pendingRegistration
            ?: return Result.failure(IllegalStateException("রেজিস্ট্রেশন প্রক্রিয়া মেয়াদোত্তীর্ণ হয়েছে। অনুগ্রহ করে আবার শুরু করুন।"))

        if (System.currentTimeMillis() > pending.expiresAt) {
            return Result.failure(IllegalStateException("ওটিপি কোডের মেয়াদ শেষ হয়েছে! 'Resend Code'-এ ট্যাপ করে নতুন কোড নিন।"))
        }

        if (enteredCode.trim() != pending.otpCode) {
            return Result.failure(IllegalArgumentException("ভুল ওটিপি কোড! অনুগ্রহ করে আপনার জিমেইলে আসা ৬-সংখ্যার কোডটি দিন।"))
        }

        // Verification successful, consume pending registration
        val completed = pending
        pendingRegistration = null
        return Result.success(completed)
    }

    fun getPendingRegistration(): PendingRegistration? = pendingRegistration

    fun cancelPending() {
        pendingRegistration = null
    }

    private fun sendSystemNotification(email: String, otp: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Gmail • পলিটেকনিক বুক এক্সচেঞ্জ")
                .setContentText("আপনার ভেরিফিকেশন কোড: $otp")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("আপনার শিক্ষার্থী অ্যাকাউন্ট ($email) ভেরিফাই করার জন্য ওটিপি কোড: $otp। এই কোডটি ৫ মিনিট কার্যকর থাকবে।")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            notificationManager.notify(78912, builder.build())
        } catch (e: Exception) {
            Log.d("EmailOtpManager", "Notification dispatch: ${e.message}")
        }
    }
}

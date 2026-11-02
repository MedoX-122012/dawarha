package com.dawwerha.app.data

import android.content.Context
import android.graphics.Bitmap
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dawwerha.app.domain.AnalysisResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.io.File

const val POINTS_PER_SCAN = 10
const val POINTS_PER_LISTING = 15
const val POINTS_PER_DELIVERY = 25

// غيّر كلمة مرور الأدمن قبل النشر
private const val ADMIN_USERNAME = "admin"
private const val DEFAULT_ADMIN_PASSWORD = "Admin@2026"

private val Context.sessionStore: DataStore<Preferences> by preferencesDataStore(name = "dawwerha_session")
private val USER_KEY = longPreferencesKey("user_id")

data class ListingDraft(
    val title: String,
    val material: String,
    val quantity: Double,
    val unit: String,
    val condition: String,
    val description: String,
    val city: String,
    val price: Double,
    val pickupTerms: String
)

class DawwerhaRepository(private val context: Context, private val dao: DawwerhaDao) {

    val currentUser: Flow<UserEntity?> = context.sessionStore.data
        .map { it[USER_KEY] }
        .flatMapLatest { id -> if (id == null) flowOf(null) else dao.observeUser(id) }

    // ---------- Auth ----------
    suspend fun seedAdminIfMissing() {
        if (dao.findUser(ADMIN_USERNAME) != null) return
        val salt = PasswordHasher.newSalt()
        dao.insertUser(
            UserEntity(
                fullName = "مدير النظام",
                username = ADMIN_USERNAME,
                passwordHash = PasswordHasher.hash(DEFAULT_ADMIN_PASSWORD, salt),
                salt = salt,
                role = Role.ADMIN
            )
        )
    }

    suspend fun register(fullName: String, username: String, password: String, city: String): String? {
        val u = username.trim().lowercase()
        if (fullName.isBlank()) return "أدخل اسمك"
        if (!Regex("^[a-z0-9_]{3,20}$").matches(u)) return "اسم المستخدم: 3 إلى 20 حرفًا إنجليزيًا أو أرقامًا أو _"
        if (password.length < 6) return "كلمة المرور يجب ألا تقل عن 6 أحرف"
        if (dao.findUser(u) != null) return "اسم المستخدم مستخدم بالفعل"
        val salt = PasswordHasher.newSalt()
        val id = dao.insertUser(
            UserEntity(
                fullName = fullName.trim(),
                username = u,
                passwordHash = PasswordHasher.hash(password, salt),
                salt = salt,
                city = city.trim()
            )
        )
        setSession(id)
        return null
    }

    suspend fun login(username: String, password: String): String? {
        val user = dao.findUser(username.trim().lowercase()) ?: return "بيانات الدخول غير صحيحة"
        if (!PasswordHasher.verify(password, user.salt, user.passwordHash)) return "بيانات الدخول غير صحيحة"
        setSession(user.id)
        return null
    }

    suspend fun logout() {
        context.sessionStore.edit { it.remove(USER_KEY) }
    }

    private suspend fun setSession(id: Long) {
        context.sessionStore.edit { it[USER_KEY] = id }
    }

    suspend fun updateProfile(user: UserEntity, fullName: String, city: String, bio: String): String? {
        if (fullName.isBlank()) return "الاسم لا يمكن أن يكون فارغًا"
        dao.updateUser(user.copy(fullName = fullName.trim(), city = city.trim(), bio = bio.trim()))
        return null
    }

    // ---------- Scans ----------
    fun observeScans(userId: Long) = dao.observeScans(userId)

    suspend fun saveScan(user: UserEntity, result: AnalysisResult, bitmap: Bitmap, weightKg: Double) {
        val material = result.material ?: return
        val path = saveImage(bitmap)
        dao.insertScan(
            ScanEntity(
                userId = user.id,
                itemName = result.itemName,
                material = material.name,
                imagePath = path,
                confidence = result.confidence,
                weightKg = weightKg
            )
        )
        dao.addPoints(user.id, POINTS_PER_SCAN)
    }

    // ---------- Market ----------
    fun observeListings() = dao.observeListings()
    fun observeListing(id: Long) = dao.observeListing(id)
    fun observeRequests(listingId: Long) = dao.observeRequests(listingId)

    suspend fun createListing(owner: UserEntity, draft: ListingDraft, bitmap: Bitmap?): String? {
        if (draft.title.isBlank()) return "أدخل اسم المادة"
        if (draft.quantity <= 0) return "الكمية يجب أن تكون أكبر من صفر"
        if (draft.price < 0) return "السعر لا يمكن أن يكون سالبًا"
        dao.insertListing(
            ListingEntity(
                ownerId = owner.id,
                ownerName = owner.fullName,
                title = draft.title.trim(),
                material = draft.material,
                quantity = draft.quantity,
                unit = draft.unit.ifBlank { "قطعة" },
                condition = draft.condition,
                description = draft.description.trim(),
                city = draft.city.trim(),
                price = draft.price,
                pickupTerms = draft.pickupTerms.trim(),
                imagePath = bitmap?.let { saveImage(it) }
            )
        )
        dao.addPoints(owner.id, POINTS_PER_LISTING)
        return null
    }

    suspend fun requestListing(listing: ListingEntity, requester: UserEntity, message: String): String? {
        if (listing.ownerId == requester.id) return "لا يمكنك طلب مادة من إعلانك"
        if (listing.status != ListingStatus.OPEN) return "هذا الإعلان مغلق"
        dao.insertRequest(
            RequestEntity(
                listingId = listing.id,
                requesterId = requester.id,
                requesterName = requester.fullName,
                message = message.trim().ifBlank { "أرغب في الحصول على هذه المادة" }
            )
        )
        return null
    }

    suspend fun setRequestStatus(listing: ListingEntity, request: RequestEntity, status: String) {
        dao.updateRequest(request.copy(status = status))
        if (status == RequestStatus.DELIVERED) {
            dao.updateListing(listing.copy(status = ListingStatus.CLOSED))
            dao.addPoints(listing.ownerId, POINTS_PER_DELIVERY)
        }
    }

    suspend fun deleteListing(listing: ListingEntity) = dao.deleteListing(listing)

    // ---------- Ideas ----------
    fun observeSavedIdeas(userId: Long) = dao.observeSavedIdeas(userId)

    suspend fun toggleSavedIdea(userId: Long, ideaId: String, save: Boolean) {
        if (save) dao.saveIdea(SavedIdeaEntity(userId, ideaId))
        else dao.unsaveIdea(userId, ideaId)
    }

    // ---------- Admin ----------
    fun observeUsers() = dao.observeUsers()

    suspend fun deleteUser(user: UserEntity): String? {
        if (user.role == Role.ADMIN) return "لا يمكن حذف حساب الأدمن"
        dao.deleteUser(user)
        return null
    }

    // ---------- Files ----------
    private fun saveImage(bitmap: Bitmap): String {
        val file = File(context.filesDir, "img_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        return file.absolutePath
    }
}

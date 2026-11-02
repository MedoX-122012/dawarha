package com.dawwerha.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object Role { const val USER = "USER"; const val ADMIN = "ADMIN" }
object ListingStatus { const val OPEN = "OPEN"; const val CLOSED = "CLOSED" }
object RequestStatus {
    const val PENDING = "PENDING"
    const val ACCEPTED = "ACCEPTED"
    const val REJECTED = "REJECTED"
    const val DELIVERED = "DELIVERED"
}

@Entity(tableName = "users", indices = [Index(value = ["username"], unique = true)])
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val username: String,
    val passwordHash: String,
    val salt: String,
    val role: String = Role.USER,
    val city: String = "",
    val bio: String = "",
    val points: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "scans", indices = [Index("userId")])
data class ScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val itemName: String,
    val material: String,
    val imagePath: String,
    val confidence: Float,
    val weightKg: Double,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "listings")
data class ListingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerId: Long,
    val ownerName: String,
    val title: String,
    val material: String,
    val quantity: Double,
    val unit: String,
    val condition: String,
    val description: String,
    val city: String,
    val price: Double,          // 0 = مجاني
    val pickupTerms: String,
    val imagePath: String?,
    val status: String = ListingStatus.OPEN,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "requests", indices = [Index("listingId")])
data class RequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listingId: Long,
    val requesterId: Long,
    val requesterName: String,
    val message: String,
    val status: String = RequestStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_ideas", primaryKeys = ["userId", "ideaId"])
data class SavedIdeaEntity(val userId: Long, val ideaId: String)

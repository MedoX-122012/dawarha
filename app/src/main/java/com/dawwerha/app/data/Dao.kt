package com.dawwerha.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DawwerhaDao {
    // Users
    @Insert suspend fun insertUser(user: UserEntity): Long
    @Update suspend fun updateUser(user: UserEntity)
    @Delete suspend fun deleteUser(user: UserEntity)
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun findUser(username: String): UserEntity?
    @Query("SELECT * FROM users WHERE id = :id")
    fun observeUser(id: Long): Flow<UserEntity?>
    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun observeUsers(): Flow<List<UserEntity>>
    @Query("UPDATE users SET points = points + :amount WHERE id = :id")
    suspend fun addPoints(id: Long, amount: Int)

    // Scans
    @Insert suspend fun insertScan(scan: ScanEntity): Long
    @Query("SELECT * FROM scans WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeScans(userId: Long): Flow<List<ScanEntity>>

    // Listings
    @Insert suspend fun insertListing(listing: ListingEntity): Long
    @Update suspend fun updateListing(listing: ListingEntity)
    @Delete suspend fun deleteListing(listing: ListingEntity)
    @Query("SELECT * FROM listings ORDER BY createdAt DESC")
    fun observeListings(): Flow<List<ListingEntity>>
    @Query("SELECT * FROM listings WHERE id = :id")
    fun observeListing(id: Long): Flow<ListingEntity?>

    // Requests
    @Insert suspend fun insertRequest(request: RequestEntity): Long
    @Update suspend fun updateRequest(request: RequestEntity)
    @Query("SELECT * FROM requests WHERE listingId = :listingId ORDER BY createdAt DESC")
    fun observeRequests(listingId: Long): Flow<List<RequestEntity>>

    // Saved ideas
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun saveIdea(item: SavedIdeaEntity)
    @Query("DELETE FROM saved_ideas WHERE userId = :userId AND ideaId = :ideaId")
    suspend fun unsaveIdea(userId: Long, ideaId: String)
    @Query("SELECT ideaId FROM saved_ideas WHERE userId = :userId")
    fun observeSavedIdeas(userId: Long): Flow<List<String>>
}

@Database(
    entities = [
        UserEntity::class, ScanEntity::class, ListingEntity::class,
        RequestEntity::class, SavedIdeaEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DawwerhaDb : RoomDatabase() {
    abstract fun dao(): DawwerhaDao
}

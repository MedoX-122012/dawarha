package com.dawwerha.app

import android.app.Application
import androidx.room.Room
import com.dawwerha.app.data.DawwerhaDb
import com.dawwerha.app.data.DawwerhaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DawwerhaApp : Application() {
    private val database by lazy {
        Room.databaseBuilder(this, DawwerhaDb::class.java, "dawwerha.db").build()
    }
    val repository by lazy { DawwerhaRepository(this, database.dao()) }

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch { repository.seedAdminIfMissing() }
    }
}

package com.example

import android.app.Application
import com.example.data.db.DorjaDatabase
import com.example.data.repository.DorjaRepository

class DorjaApp : Application() {
    val database by lazy { DorjaDatabase.getDatabase(this) }
    val repository by lazy { DorjaRepository(database) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: DorjaApp
            private set
    }
}

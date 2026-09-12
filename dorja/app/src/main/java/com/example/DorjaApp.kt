package com.example

import android.app.Application
import com.example.data.db.DorjaDatabase
import com.example.data.repository.DorjaRepository
import com.example.data.work.EvidenceRetentionWorker

class DorjaApp : Application() {
    val database by lazy { DorjaDatabase.getDatabase(this) }
    val repository by lazy { DorjaRepository(database) }
    val aiEngine by lazy { com.example.ai.DorjaAiEngine.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // GDPR-style evidence retention (Phase 3): one sweep at startup so an
        // expired retention window is honoured immediately, then daily after that.
        EvidenceRetentionWorker.runNow(this)
        EvidenceRetentionWorker.schedule(this)
    }

    companion object {
        lateinit var instance: DorjaApp
            private set
    }
}

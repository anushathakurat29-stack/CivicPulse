package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.CivicDao
import com.example.data.model.CommunityIssue
import com.example.data.model.IssueComment
import com.example.data.model.CitizenImpact

@Database(
    entities = [CommunityIssue::class, IssueComment::class, CitizenImpact::class],
    version = 3,
    exportSchema = false
)
abstract class CivicDatabase : RoomDatabase() {
    abstract fun civicDao(): CivicDao

    companion object {
        @Volatile
        private var INSTANCE: CivicDatabase? = null

        fun getDatabase(context: Context): CivicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CivicDatabase::class.java,
                    "civic_resolve_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

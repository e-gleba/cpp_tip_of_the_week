package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.CppTip

@Database(entities = [CppTip::class], version = 2, exportSchema = false)
abstract class CppDatabase : RoomDatabase() {
    abstract fun cppTipDao(): CppTipDao

    companion object {
        @Volatile
        private var INSTANCE: CppDatabase? = null

        fun getDatabase(context: Context): CppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CppDatabase::class.java,
                    "cpp_tips_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

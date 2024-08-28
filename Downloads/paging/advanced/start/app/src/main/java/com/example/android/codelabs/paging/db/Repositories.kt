package com.example.android.codelabs.paging.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.android.codelabs.paging.model.RemoteKeys
import com.example.android.codelabs.paging.model.Repository

@Database(
    entities = [Repository::class, RemoteKeys::class],
    version = 1,
    exportSchema = false
)
abstract class Repositories : RoomDatabase() {

    abstract fun repositoryDao(): RepositoriesDao
    abstract fun remoteKeysDao(): RemoteKeysDao

    companion object {
        @Volatile
        private var INSTANCE: Repositories? = null

        fun getInstance(context: Context): Repositories =
            INSTANCE ?: synchronized(this) {
                INSTANCE
                    ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                Repositories::class.java, "repositories"
            ).build()
    }
}
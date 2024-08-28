package com.example.android.codelabs.paging.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.android.codelabs.paging.model.Repository

@Dao
interface RepositoriesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(repositories:List<Repository>)

    @Query("SELECT * FROM repositories WHERE " +
            "name LIKE :queryString OR description LIKE :queryString " +
            "ORDER BY stars DESC, name ASC")
    fun reposByName(queryString: String): PagingSource<Int, Repository>

    @Query("DELETE FROM repositories")
    suspend fun clearRepositories()

}
/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.codelabs.paging.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.android.codelabs.paging.api.GithubService
import com.example.android.codelabs.paging.db.Repositories
import com.example.android.codelabs.paging.model.Repository
import kotlinx.coroutines.flow.Flow

class GithubRepository(
    private val service: GithubService,
    private val database: Repositories
) {

    fun getSearchResultStream(query: String): Flow<PagingData<Repository>> {

        // appending '%' so we can allow other characters to be before and after the query string
        val dbQuery = "%${query.replace(' ', '%')}%"
        val pagingSourceFactory =  { database.repositoryDao().reposByName(dbQuery)}

        @OptIn(ExperimentalPagingApi::class)
        return Pager(
            config = PagingConfig(
                pageSize = NETWORK_PAGE_SIZE,
                enablePlaceholders = false
            ),
            remoteMediator = RepositoryRemoteMediator(
                query, service, database
            ),
            pagingSourceFactory = pagingSourceFactory
//            pagingSourceFactory = {
//                GithubPagingSource(
//                    service, query
//                )
//            }
        ).flow
    }

    companion object {
        const val NETWORK_PAGE_SIZE = 5
    }
}

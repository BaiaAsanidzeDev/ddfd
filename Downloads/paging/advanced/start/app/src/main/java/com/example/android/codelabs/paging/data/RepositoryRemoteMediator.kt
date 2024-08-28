package com.example.android.codelabs.paging.data

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.example.android.codelabs.paging.api.GithubService
import com.example.android.codelabs.paging.api.IN_QUALIFIER
import com.example.android.codelabs.paging.db.Repositories
import com.example.android.codelabs.paging.model.RemoteKeys
import com.example.android.codelabs.paging.model.Repository
import retrofit2.HttpException
import java.io.IOException

private const val GITHUB_STARTING_PAGE_INDEX = 1

@OptIn(ExperimentalPagingApi::class)
class RepositoryRemoteMediator(
    private val query: String,
    private val service: GithubService,
    private val db: Repositories
) : RemoteMediator<Int, Repository>() {

    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, Repository>): RemoteKeys? {

        return state.pages.lastOrNull() {
            it.data.isNotEmpty()
        }?.data?.lastOrNull()
            ?.let { repo ->
                db.remoteKeysDao().remoteKeysRepoId(repo.id)
            }
    }

    private suspend fun getRemoteKeyForFirstItem(state: PagingState<Int, Repository>): RemoteKeys? {
        // Get the first page that was retrieved, that contained items.
        // From that first page, get the first item
        return state.pages.firstOrNull { it.data.isNotEmpty() }?.data?.firstOrNull()
            ?.let { repo ->
                // Get the remote keys of the first items retrieved
                db.remoteKeysDao().remoteKeysRepoId(repo.id)
            }
    }

    private suspend fun getRemoteKeyClosestToCurrentPosition(
        state: PagingState<Int, Repository>
    ): RemoteKeys? {
        // The paging library is trying to load data after the anchor position
        // Get the item closest to the anchor position
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.id?.let { repoId ->
                db.remoteKeysDao().remoteKeysRepoId(repoId)
            }
        }
    }


    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, Repository>
    ): MediatorResult {

        val page = when (loadType) {
//            LoadType.REFRESH gets called when it's the first time we're
//                    loading data, or when PagingDataAdapter.refresh() is called;
            LoadType.REFRESH -> {
                val remoteKeys = getRemoteKeyClosestToCurrentPosition(state)
                remoteKeys?.nextKey?.minus(1) ?: GITHUB_STARTING_PAGE_INDEX
            }

            LoadType.PREPEND -> {
                val remoteKeys = getRemoteKeyForFirstItem(state)
                // If remoteKeys is null, that means the refresh result is not in the database yet.
                val prevKey = remoteKeys?.prevKey
                if (prevKey == null) {
                    return MediatorResult.Success(endOfPaginationReached = remoteKeys != null)
                }
                prevKey

            }

            LoadType.APPEND -> {
                val remoteKeys = getRemoteKeyForLastItem(state)
                // If remoteKeys is null, that means the refresh result is not in the database yet.
                // We can return Success with endOfPaginationReached = false because Paging
                // will call this method again if RemoteKeys becomes non-null.
                // If remoteKeys is NOT NULL but its nextKey is null, that means we've reached
                // the end of pagination for append.
                val nextKey = remoteKeys?.nextKey
                if(nextKey == null) {
                    return MediatorResult.Success(endOfPaginationReached = remoteKeys != null)
                }
                nextKey
            }
        }

        val apiQuery = query + IN_QUALIFIER

        try {
            val apiResponse = service.searchRepos(
                query = apiQuery,
                page = page,
                itemsPerPage = state.config.pageSize
            )
            val repositories = apiResponse.items
            val endOfPaginationReached =
                repositories.isEmpty() // end of pagination, tu agar aris data uwer trues da agar agzavnis requestss

            db.withTransaction {

                if (loadType == LoadType.REFRESH) {
                    db.repositoryDao().clearRepositories()
                    db.remoteKeysDao().clearRemoteKeys()
                }
                val prevKey = if (page == GITHUB_STARTING_PAGE_INDEX) null else page - 1
                val nextKey = if (endOfPaginationReached) null else page + 1
                val keys = repositories.map {
                    RemoteKeys(repoId = it.id, prevKey = prevKey, nextKey = nextKey)
                }
                db.remoteKeysDao().insertAll(keys)
                db.repositoryDao().insertAll(repositories)
            }
            return MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (exception: IOException) {
            exception.printStackTrace()
            return MediatorResult.Error(exception)
        } catch (exception: HttpException) {
            exception.printStackTrace()
            return MediatorResult.Error(exception)
        }
    }
}
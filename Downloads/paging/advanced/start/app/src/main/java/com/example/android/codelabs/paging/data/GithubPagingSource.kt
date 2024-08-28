package com.example.android.codelabs.paging.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.android.codelabs.paging.api.GithubService
import com.example.android.codelabs.paging.api.IN_QUALIFIER
import com.example.android.codelabs.paging.data.GithubRepository.Companion.NETWORK_PAGE_SIZE
import com.example.android.codelabs.paging.model.Repository
import okio.IOException
import retrofit2.HttpException

private const val GITHUB_STARTING_PAGE_INDEX = 1

class GithubPagingSource(
    private val service: GithubService,
    private val query: String
) : PagingSource<Int, Repository>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Repository> {
        val position =params.key ?: GITHUB_STARTING_PAGE_INDEX
        val apiQuery = query + IN_QUALIFIER
        return try{
            val response = service.searchRepos(apiQuery, position, params.loadSize)
            val repositories = response.items
            val nextKey = if(repositories.isEmpty()){
                null
            }else{
                // initial load size = 3 * NETWORK_PAGE_SIZE
                // ensure we're not requesting duplicating items, at the 2nd request
                position + (params.loadSize / NETWORK_PAGE_SIZE)
            }
            LoadResult.Page(
                data = repositories,
                prevKey = if(position == GITHUB_STARTING_PAGE_INDEX) null else position-1,
                nextKey = nextKey
            )
        }catch (e:IOException){
            e.printStackTrace()
            return LoadResult.Error(e)
        }catch (e: HttpException){
            e.printStackTrace()
            return LoadResult.Error(e)
        }
    }

    // The refresh key is used for subsequent refresh calls to PagingSource.load after the initial load
    override fun getRefreshKey(state: PagingState<Int, Repository>): Int? {
        // We need to get the previous key (or next key if previous is null) of the page
        // that was closest to the most recently accessed index.
        // Anchor position is the most recently accessed index
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

//    By default, the initial load size is 3 * page size. That way, Paging ensures that the first
//    time the list is loaded the user will see enough items and doesn't trigger too many network requests,
//    if the user doesn't scroll past what's loaded.

}
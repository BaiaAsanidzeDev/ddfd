package com.example.android.codelabs.paging.ui

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.example.android.codelabs.paging.data.GithubRepository
import com.example.android.codelabs.paging.model.UiModel
import com.example.android.codelabs.paging.model.roundedStarCount
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SearchRepositoriesViewModel(
    private val repository: GithubRepository, private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val state: StateFlow<UiState>
    val pagingDataFlow: Flow<PagingData<UiModel>>

    val accept: (UiAction) -> Unit

    init {
        val initialQuery: String = savedStateHandle.get(LAST_SEARCH_QUERY) ?: DEFAULT_QUERY
        val lastQueryScrolled: String = savedStateHandle.get(LAST_QUERY_SCROLLED) ?: DEFAULT_QUERY
        val actionStateFlow = MutableSharedFlow<UiAction>()
        val searches = actionStateFlow.filterIsInstance<UiAction.Search>().distinctUntilChanged()
            .onStart { emit(UiAction.Search(query = initialQuery)) }
        val queriesScrolled =
            actionStateFlow.filterIsInstance<UiAction.Scroll>().distinctUntilChanged()
                // This is shared to keep the flow "hot" while caching the last query scrolled,
                // otherwise each flatMapLatest invocation would lose the last query scrolled,
                .shareIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
                    replay = 1
                ).onStart { emit(UiAction.Scroll(currentQuery = lastQueryScrolled)) }

        pagingDataFlow = searches.flatMapLatest {
            searchRepository(it.query)
        }.cachedIn(viewModelScope)

        state = combine(
            searches, queriesScrolled, ::Pair
        ).map { (search, scroll) ->
            UiState(
                query = search.query,
                lastQueryScrolled = scroll.currentQuery,
                // If the search query matches the scroll query, the user has scrolled
                hasNotScrolledForCurrentSearch = search.query != scroll.currentQuery
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = UiState()
        )

        accept = { action ->
            viewModelScope.launch {
                actionStateFlow.emit(action)
            }
        }

    }

    private fun searchRepository(query: String): Flow<PagingData<UiModel>> {
        val newResult: Flow<PagingData<UiModel>> = repository.getSearchResultStream(query)
            .map { pagingData ->
                pagingData.map {
                    UiModel.RepoItem(it)
                }
            }.map {
                it.insertSeparators { before: UiModel.RepoItem?, after: UiModel.RepoItem? ->
                    Log.d("dsdsds", "before  $before  after   $after")

                    if (after == null) {
                        // we're at the end of the list
                        return@insertSeparators null
                    }

                    if (before == null) {
                        // we're at the beginning of the list
                        return@insertSeparators UiModel.SeparatorItem("${after.roundedStarCount}0.000+ stars")
                    }


                    if (before.roundedStarCount > after.roundedStarCount) {
                        if (after.repo.stars >= 1) {
                            Log.d("dsdsds", " >1  start ${before.repo.stars}  rounded   ${before.roundedStarCount}  end ${after.repo.stars}  rounded   ${after.roundedStarCount}")

                            UiModel.SeparatorItem("${after.roundedStarCount}0.000+ stars")
                        } else {
                            Log.d("dsdsds", "else  start ${before.repo.stars}  rounded   ${before.roundedStarCount}  end ${after.repo.stars}  rounded   ${after.roundedStarCount}")

                            UiModel.SeparatorItem("< 10.000+ stars")
                        }
                    } else {
                        null
                    }
                }
            }
        return newResult
    }

    override fun onCleared() {
        savedStateHandle[LAST_SEARCH_QUERY] = state.value.query
        savedStateHandle[LAST_QUERY_SCROLLED] = state.value.lastQueryScrolled
        super.onCleared()
    }
}

sealed class UiAction {
    data class Search(val query: String) : UiAction()
    data class Scroll(
        val currentQuery: String
    ) : UiAction()
}

data class UiState(
    val query: String = DEFAULT_QUERY,
    val lastQueryScrolled: String = DEFAULT_QUERY,
    val hasNotScrolledForCurrentSearch: Boolean = false
)

private const val LAST_SEARCH_QUERY: String = "last_search_query"
private const val DEFAULT_QUERY = "Android"
private const val LAST_QUERY_SCROLLED: String = "last_query_scrolled"
package com.lunexa.gameshelf.ui.lists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunexa.gameshelf.data.GameRepository
import com.lunexa.gameshelf.data.model.Game
import com.lunexa.gameshelf.data.model.GameItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class WishSort(val title: String) {
    PRIORITY("По приоритету"),
    PRICE("По цене"),
    DATE("По дате покупки"),
    TITLE("По названию")
}

data class ListsUiState(
    val loading: Boolean = true,
    val favorites: List<GameItem> = emptyList(),
    val wishlist: List<GameItem> = emptyList(),
    val purchasedCount: Int = 0,
    val wishTotalPrice: Double = 0.0,
    val sort: WishSort = WishSort.PRIORITY,
    val showPurchased: Boolean = false
)

private data class WishOptions(val sort: WishSort = WishSort.PRIORITY, val showPurchased: Boolean = false)

class ListsViewModel(
    savedState: SavedStateHandle,
    private val repository: GameRepository
) : ViewModel() {

    val initialTab: Int = savedState.get<Int>("tab") ?: 0

    private val options = MutableStateFlow(WishOptions())

    val state: StateFlow<ListsUiState> = combine(repository.gameItems, options) { items, o ->
        val wish = items.filter { it.game.inWishlist }
        val visible = wish.filter { o.showPurchased || !it.game.wishPurchased }
        val sorted = when (o.sort) {
            WishSort.PRIORITY -> visible.sortedWith(
                compareBy<GameItem> { it.game.wishPurchased }
                    .thenByDescending { it.game.wishPriority }
                    .thenBy { it.game.wishBuyDate ?: Long.MAX_VALUE }
            )
            WishSort.PRICE -> visible.sortedWith(compareBy<GameItem> { it.game.wishPurchased }.thenBy { it.game.wishPrice ?: Double.MAX_VALUE })
            WishSort.DATE -> visible.sortedWith(compareBy<GameItem> { it.game.wishPurchased }.thenBy { it.game.wishBuyDate ?: Long.MAX_VALUE })
            WishSort.TITLE -> visible.sortedWith(compareBy<GameItem> { it.game.wishPurchased }.thenBy { it.game.title.lowercase() })
        }
        ListsUiState(
            loading = false,
            favorites = items.filter { it.game.isFavorite }.sortedBy { it.game.title.lowercase() },
            wishlist = sorted,
            purchasedCount = wish.count { it.game.wishPurchased },
            wishTotalPrice = wish.filter { !it.game.wishPurchased }.sumOf { it.game.wishPrice ?: 0.0 },
            sort = o.sort,
            showPurchased = o.showPurchased
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListsUiState())

    fun setSort(sort: WishSort) = options.update { it.copy(sort = sort) }
    fun toggleShowPurchased() = options.update { it.copy(showPurchased = !it.showPurchased) }

    fun togglePurchased(game: Game) {
        viewModelScope.launch { repository.update(game.copy(wishPurchased = !game.wishPurchased)) }
    }

    fun removeFromWishlist(game: Game) {
        viewModelScope.launch { repository.update(game.copy(inWishlist = false)) }
    }

    fun removeFromFavorites(game: Game) {
        viewModelScope.launch { repository.update(game.copy(isFavorite = false)) }
    }
}

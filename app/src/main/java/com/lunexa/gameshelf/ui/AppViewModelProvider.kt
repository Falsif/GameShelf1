package com.lunexa.gameshelf.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lunexa.gameshelf.AppContainer
import com.lunexa.gameshelf.GameShelfApp
import com.lunexa.gameshelf.ui.detail.GameDetailViewModel
import com.lunexa.gameshelf.ui.edit.EditGameViewModel
import com.lunexa.gameshelf.ui.home.HomeViewModel
import com.lunexa.gameshelf.ui.library.LibraryViewModel
import com.lunexa.gameshelf.ui.lists.ListsViewModel
import com.lunexa.gameshelf.ui.profile.ProfileViewModel
import com.lunexa.gameshelf.ui.settings.SettingsViewModel
import com.lunexa.gameshelf.ui.stats.StatsViewModel

private fun CreationExtras.container(): AppContainer =
    (this[APPLICATION_KEY] as GameShelfApp).container

/** Фабрика всех ViewModel приложения (MVVM без DI-фреймворка). */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { HomeViewModel(container().repository, container().settings) }
        initializer { LibraryViewModel(createSavedStateHandle(), container().repository, container().settings) }
        initializer { GameDetailViewModel(createSavedStateHandle(), container().repository, container().settings) }
        initializer {
            val c = container()
            EditGameViewModel(createSavedStateHandle(), c.repository, c.images, c.rawg, c.settings)
        }
        initializer { StatsViewModel(container().repository) }
        initializer { ListsViewModel(createSavedStateHandle(), container().repository) }
        initializer { ProfileViewModel(container().repository, container().settings, container().images) }
        initializer { SettingsViewModel(container()) }
    }
}

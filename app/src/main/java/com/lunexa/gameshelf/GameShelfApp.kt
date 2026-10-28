package com.lunexa.gameshelf

import android.app.Application
import com.lunexa.gameshelf.data.BackupManager
import com.lunexa.gameshelf.data.GameRepository
import com.lunexa.gameshelf.data.ImageStore
import com.lunexa.gameshelf.data.RawgClient
import com.lunexa.gameshelf.data.SettingsRepository
import com.lunexa.gameshelf.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Простой контейнер зависимостей. */
class AppContainer(app: Application) {
    val database: AppDatabase = AppDatabase.create(app)
    val images = ImageStore(app)
    val settings = SettingsRepository(app)
    val repository = GameRepository(database, images)
    val backup = BackupManager(app, database, images, settings)
    val rawg = RawgClient(app)
}

class GameShelfApp : Application() {
    lateinit var container: AppContainer
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Удаляем изображения, оставшиеся от отменённых форм
        appScope.launch { runCatching { container.backup.cleanupOrphans() } }
    }
}

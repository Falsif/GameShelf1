@file:OptIn(ExperimentalMaterial3Api::class)

package com.lunexa.gameshelf.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lunexa.gameshelf.BuildConfig
import com.lunexa.gameshelf.ui.AppViewModelProvider
import com.lunexa.gameshelf.ui.common.formatDateTime
import com.lunexa.gameshelf.ui.common.gamesCount
import com.lunexa.gameshelf.ui.components.ConfirmDialog
import com.lunexa.gameshelf.ui.components.LogoMark
import com.lunexa.gameshelf.ui.components.SectionCard
import com.lunexa.gameshelf.ui.theme.AccentOptions
import com.lunexa.gameshelf.ui.theme.ThemeMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val autoBackup by viewModel.autoBackup.collectAsStateWithLifecycle()
    val total by viewModel.gamesTotal.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current

    var confirmImport by remember { mutableStateOf<android.net.Uri?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    var confirmReset2 by remember { mutableStateOf(false) }
    var confirmRestore by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.exportBackup(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) confirmImport = uri
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Настройки") },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
    ) { padding ->
        val s = settings ?: return@Scaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ---------- Оформление ----------
            SectionCard {
                Title("Оформление")
                Label("Тема")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { i, mode ->
                        SegmentedButton(
                            selected = s.themeMode == mode,
                            onClick = { viewModel.setTheme(mode) },
                            shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size)
                        ) { Text(mode.title) }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Label("Акцентный цвет")
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AccentOptions.forEach { c ->
                        val selected = s.accent == c
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .border(
                                    if (selected) 3.dp else 0.dp,
                                    if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    CircleShape
                                )
                                .clickable { viewModel.setAccent(c) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) Icon(Icons.Filled.Check, "Выбран", tint = Color.White)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Label("Вид библиотеки")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = s.libraryGrid,
                        onClick = { viewModel.setGrid(true) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) { Text("Сетка") }
                    SegmentedButton(
                        selected = !s.libraryGrid,
                        onClick = { viewModel.setGrid(false) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) { Text("Список") }
                }
            }

            // ---------- Онлайн-каталог ----------
            SectionCard {
                Title("Онлайн-каталог RAWG")
                Text(
                    "Бесплатный ключ позволяет искать игры и автоматически заполнять данные и обложки. " +
                        "Без ключа и интернета приложение полностью работает в ручном режиме.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                var key by rememberSaveable(s.rawgKey) { mutableStateOf(s.rawgKey) }
                var visible by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it.trim() },
                    label = { Text("API-ключ") },
                    singleLine = true,
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, "Показать ключ")
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.saveRawgKey(key) }, enabled = key != s.rawgKey, shape = RoundedCornerShape(12.dp)) {
                        Text("Сохранить")
                    }
                    OutlinedButton(onClick = { viewModel.testRawgKey(key) }, enabled = !busy, shape = RoundedCornerShape(12.dp)) {
                        Text("Проверить")
                    }
                }
                Text(
                    "Получить ключ: rawg.io/apidocs",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clickable { runCatching { uriHandler.openUri("https://rawg.io/apidocs") } }
                )
            }

            // ---------- Данные ----------
            SectionCard {
                Title("Данные и резервные копии")
                Text(
                    "Все данные хранятся только на устройстве и сохраняются автоматически. В коллекции: ${gamesCount(total)}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                ActionRow(Icons.Outlined.FileDownload, "Экспорт в JSON", "Резервная копия с обложками в файл", enabled = !busy) {
                    val date = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).format(Date())
                    exportLauncher.launch("GameShelf_backup_$date.json")
                }
                ActionRow(Icons.Outlined.FileUpload, "Импорт из JSON", "Восстановить данные из резервной копии", enabled = !busy) {
                    importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain", "*/*"))
                }
                val auto = autoBackup
                if (auto != null) {
                    ActionRow(
                        Icons.Outlined.Restore, "Вернуть автокопию",
                        "Создана ${formatDateTime(auto)} перед импортом или сбросом", enabled = !busy
                    ) { confirmRestore = true }
                }
                HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
                ActionRow(
                    Icons.Outlined.DeleteForever, "Сбросить все данные",
                    "Удалить игры, сессии и профиль", enabled = !busy,
                    tint = MaterialTheme.colorScheme.error
                ) { confirmReset = true }
            }

            // ---------- О приложении ----------
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LogoMark(48.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("GameShelf", style = MaterialTheme.typography.titleLarge)
                        Text("Версия ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Персональная библиотека видеоигр: коллекция, статусы прохождения, оценки, игровое время, " +
                        "статистика и списки желаемого. Работает без интернета и без регистрации.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Разработчик: LUNEXA", style = MaterialTheme.typography.titleSmall)
                }
                Text(
                    "Данные об играх в онлайн-поиске предоставлены RAWG.io",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    "© ${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)} LUNEXA",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    confirmImport?.let { uri ->
        ConfirmDialog(
            title = "Восстановить из копии?",
            text = "Текущие данные будут заменены данными из файла. Перед заменой будет создана автоматическая копия, " +
                "чтобы изменения можно было отменить.",
            confirmText = "Восстановить",
            onConfirm = { confirmImport = null; viewModel.importBackup(uri) },
            onDismiss = { confirmImport = null }
        )
    }
    if (confirmRestore) {
        ConfirmDialog(
            title = "Вернуть автокопию?",
            text = "Текущие данные будут заменены автоматически сохранённой копией.",
            confirmText = "Вернуть",
            onConfirm = { confirmRestore = false; viewModel.restoreAuto() },
            onDismiss = { confirmRestore = false }
        )
    }
    if (confirmReset) {
        ConfirmDialog(
            title = "Сбросить все данные?",
            text = "Будут удалены все игры, сессии, прохождения и профиль. Рекомендуем сначала сделать экспорт в JSON.",
            confirmText = "Продолжить",
            destructive = true,
            onConfirm = { confirmReset = false; confirmReset2 = true },
            onDismiss = { confirmReset = false }
        )
    }
    if (confirmReset2) {
        ConfirmDialog(
            title = "Точно удалить?",
            text = "Это последнее подтверждение. Будет удалено: ${gamesCount(total)}.",
            confirmText = "Удалить всё",
            destructive = true,
            onConfirm = { confirmReset2 = false; viewModel.resetAll() },
            onDismiss = { confirmReset2 = false }
        )
    }
}

@Composable
private fun Title(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = tint) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = if (tint == MaterialTheme.colorScheme.error) tint else MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

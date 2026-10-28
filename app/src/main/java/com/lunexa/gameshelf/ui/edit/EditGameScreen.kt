@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.lunexa.gameshelf.ui.edit

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lunexa.gameshelf.data.RawgGame
import com.lunexa.gameshelf.data.model.GameStatus
import com.lunexa.gameshelf.data.model.splitList
import com.lunexa.gameshelf.ui.AppViewModelProvider
import com.lunexa.gameshelf.ui.common.GenrePresets
import com.lunexa.gameshelf.ui.common.PlatformPresets
import com.lunexa.gameshelf.ui.components.ConfirmDialog
import com.lunexa.gameshelf.ui.components.DateField
import com.lunexa.gameshelf.ui.components.GameCover
import com.lunexa.gameshelf.ui.components.SectionCard

@Composable
fun EditGameScreen(
    onBack: () -> Unit,
    onSaved: (Long, Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: EditGameViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val form = viewModel.form
    val errors = viewModel.errors
    val snackbar = remember { SnackbarHostState() }
    var confirmExit by remember { mutableStateOf(false) }
    var showRawg by rememberSaveable { mutableStateOf(false) }

    val message = viewModel.message
    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            viewModel.consumeMessage()
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.pickCover(uri)
    }

    val tryExit: () -> Unit = {
        if (viewModel.isDirty) {
            confirmExit = true
        } else {
            onBack()
        }
    }
    BackHandler(enabled = viewModel.isDirty) { confirmExit = true }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isNew) "Новая игра" else "Редактирование") },
                navigationIcon = {
                    IconButton(onClick = tryExit) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.save(onSaved = onSaved) },
                        enabled = !viewModel.saving && !viewModel.loading
                    ) {
                        if (viewModel.saving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text("Сохранить")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (viewModel.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (viewModel.isNew) {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.TravelExplore, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Найти в онлайн-каталоге", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Автоматически заполнит данные и обложку (RAWG)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(onClick = {
                            viewModel.rawgQuery = viewModel.rawgQuery.ifBlank { form.title }
                            viewModel.checkRawgKey()
                            showRawg = true
                        }, shape = RoundedCornerShape(12.dp)) { Text("Поиск") }
                    }
                }
            }

            // --- Обложка и название ---
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .width(112.dp)
                        .height(150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                ) {
                    if (form.coverUri != null) {
                        GameCover(form.coverUri, form.title, Modifier.fillMaxSize(), corner = 16.dp)
                        IconButton(
                            onClick = viewModel::removeCover,
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(28.dp)
                                .clip(CircleShape).background(Color.Black.copy(alpha = 0.55f))
                        ) { Icon(Icons.Filled.Close, "Убрать обложку", tint = Color.White, modifier = Modifier.size(16.dp)) }
                    } else {
                        Column(
                            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Filled.AddPhotoAlternate, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(6.dp))
                            Text("Обложка", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (viewModel.coverLoading) {
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(28.dp), color = Color.White)
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = form.title,
                        onValueChange = { v -> viewModel.update { it.copy(title = v) } },
                        label = { Text("Название *") },
                        isError = errors["title"] != null,
                        supportingText = errors["title"]?.let { { Text(it) } },
                        singleLine = false,
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Нажмите на обложку, чтобы выбрать изображение из галереи",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // --- Статус и оценка ---
            SectionCard {
                Text("Статус прохождения", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    GameStatus.entries.forEach { st ->
                        val color = Color(st.color)
                        FilterChip(
                            selected = form.status == st,
                            onClick = { viewModel.update { it.copy(status = st) } },
                            label = { Text(st.title) },
                            leadingIcon = { Box(Modifier.size(8.dp).clip(CircleShape).background(color)) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = color.copy(alpha = 0.22f))
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Оценка", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Text(
                        if (form.rating == 0) "Без оценки" else "${form.rating} / 10",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Slider(
                    value = form.rating.toFloat(),
                    onValueChange = { v -> viewModel.update { it.copy(rating = v.toInt()) } },
                    valueRange = 0f..10f,
                    steps = 9
                )
            }

            // --- Описание и сведения ---
            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Сведения об игре", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = form.description,
                        onValueChange = { v -> if (v.length <= 5000) viewModel.update { it.copy(description = v) } },
                        label = { Text("Описание") },
                        minLines = 3,
                        maxLines = 8,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = form.genre,
                        onValueChange = { v -> viewModel.update { it.copy(genre = v) } },
                        label = { Text("Жанр") },
                        supportingText = { Text("Несколько жанров — через запятую") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    val selectedGenres = splitList(form.genre).map { it.lowercase() }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        GenrePresets.forEach { g ->
                            FilterChip(
                                selected = g.lowercase() in selectedGenres,
                                onClick = { viewModel.addGenre(g) },
                                label = { Text(g) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                    OutlinedTextField(
                        value = form.developer,
                        onValueChange = { v -> viewModel.update { it.copy(developer = v) } },
                        label = { Text("Разработчик") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = form.publisher,
                        onValueChange = { v -> viewModel.update { it.copy(publisher = v) } },
                        label = { Text("Издатель") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    DateField("Дата выпуска", form.releaseDate, { d -> viewModel.update { it.copy(releaseDate = d) } })
                }
            }

            // --- Платформы ---
            SectionCard {
                Text("Платформы", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                val all = (PlatformPresets + form.platforms).distinct()
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    all.forEach { p ->
                        val selected = p in form.platforms
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.togglePlatform(p) },
                            label = { Text(p) },
                            leadingIcon = if (selected) {
                                { Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) }
                            } else null,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
                var custom by rememberSaveable { mutableStateOf("") }
                OutlinedTextField(
                    value = custom,
                    onValueChange = { custom = it },
                    label = { Text("Своя платформа") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { viewModel.addCustomPlatform(custom); custom = "" }),
                    trailingIcon = {
                        IconButton(onClick = { viewModel.addCustomPlatform(custom); custom = "" }) {
                            Icon(Icons.Outlined.Add, "Добавить платформу")
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                )
            }

            // --- Время и даты ---
            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Время прохождения", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Сессии и таймер на странице игры добавляются к этому времени автоматически.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = form.hours,
                            onValueChange = { v -> if (v.all(Char::isDigit) && v.length <= 6) viewModel.update { it.copy(hours = v) } },
                            label = { Text("Часы") },
                            isError = errors["hours"] != null,
                            supportingText = errors["hours"]?.let { { Text(it) } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = form.minutes,
                            onValueChange = { v -> if (v.all(Char::isDigit) && v.length <= 2) viewModel.update { it.copy(minutes = v) } },
                            label = { Text("Минуты") },
                            isError = errors["minutes"] != null,
                            supportingText = errors["minutes"]?.let { { Text(it) } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    DateField("Дата начала игры", form.startDate, { d -> viewModel.update { it.copy(startDate = d) } })
                    DateField(
                        "Дата завершения", form.finishDate, { d -> viewModel.update { it.copy(finishDate = d) } },
                        isError = errors["finish"] != null,
                        supporting = errors["finish"]
                    )
                }
            }

            // --- Избранное и желаемое ---
            SectionCard {
                SwitchRow("В избранном", "Самые любимые игры", form.isFavorite) { v -> viewModel.update { it.copy(isFavorite = v) } }
                SwitchRow("В списке желаемого", "Хочу купить или пройти", form.inWishlist) { v -> viewModel.update { it.copy(inWishlist = v) } }
                AnimatedVisibility(form.inWishlist) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                        OutlinedTextField(
                            value = form.wishPrice,
                            onValueChange = { v -> if (v.length <= 12) viewModel.update { it.copy(wishPrice = v) } },
                            label = { Text("Желаемая цена, ₽") },
                            isError = errors["price"] != null,
                            supportingText = errors["price"]?.let { { Text(it) } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        DateField("Предполагаемая дата покупки", form.wishBuyDate, { d -> viewModel.update { it.copy(wishBuyDate = d) } })
                        Text("Приоритет", style = MaterialTheme.typography.titleSmall)
                        val options = listOf(1 to "Низкий", 2 to "Средний", 3 to "Высокий")
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            options.forEachIndexed { index, (value, label) ->
                                SegmentedButton(
                                    selected = form.wishPriority == value,
                                    onClick = { viewModel.update { it.copy(wishPriority = value) } },
                                    shape = SegmentedButtonDefaults.itemShape(index, options.size)
                                ) { Text(label) }
                            }
                        }
                        SwitchRow("Куплено", "Игра уже приобретена", form.wishPurchased) { v -> viewModel.update { it.copy(wishPurchased = v) } }
                    }
                }
            }

            // --- Заметки ---
            OutlinedTextField(
                value = form.notes,
                onValueChange = { v -> if (v.length <= 5000) viewModel.update { it.copy(notes = v) } },
                label = { Text("Личные заметки") },
                minLines = 3,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { viewModel.save(onSaved = onSaved) },
                enabled = !viewModel.saving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text(if (viewModel.isNew) "Добавить в библиотеку" else "Сохранить изменения") }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmExit) {
        ConfirmDialog(
            title = "Выйти без сохранения?",
            text = "Внесённые изменения будут потеряны.",
            confirmText = "Выйти",
            destructive = true,
            onConfirm = { confirmExit = false; onBack() },
            onDismiss = { confirmExit = false },
            dismissText = "Остаться"
        )
    }
    if (viewModel.askDuplicate) {
        ConfirmDialog(
            title = "Игра уже есть в библиотеке",
            text = "«${form.title.trim()}» уже добавлена. Добавить ещё одну копию?",
            confirmText = "Добавить",
            onConfirm = { viewModel.dismissDuplicate(); viewModel.save(force = true, onSaved = onSaved) },
            onDismiss = viewModel::dismissDuplicate
        )
    }
    if (showRawg) {
        RawgSearchDialog(
            viewModel = viewModel,
            onDismiss = { showRawg = false },
            onOpenSettings = { showRawg = false; onOpenSettings() }
        )
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun RawgSearchDialog(
    viewModel: EditGameViewModel,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Закрыть") }
                    Text("Онлайн-каталог RAWG", style = MaterialTheme.typography.titleLarge)
                }
                OutlinedTextField(
                    value = viewModel.rawgQuery,
                    onValueChange = { viewModel.rawgQuery = it },
                    placeholder = { Text("Например, The Witcher 3") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = {
                        TextButton(onClick = viewModel::searchRawg) { Text("Найти") }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.searchRawg() }),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxSize()) {
                    when (val st = viewModel.rawgState) {
                        RawgState.Idle -> Hint(
                            Icons.Filled.TravelExplore,
                            "Введите название игры и нажмите «Найти». Данные и обложка заполнятся автоматически."
                        )
                        RawgState.NoKey -> Column(
                            Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.Key, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Для онлайн-поиска нужен бесплатный API-ключ RAWG. Получите его на rawg.io/apidocs и укажите в настройках.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = onOpenSettings) { Text("Открыть настройки") }
                            OutlinedButton(onClick = onDismiss, modifier = Modifier.padding(top = 8.dp)) { Text("Добавить вручную") }
                        }
                        RawgState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                        is RawgState.Error -> Column(
                            Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.CloudOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(st.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = viewModel::searchRawg) { Text("Повторить") }
                                Button(onClick = onDismiss) { Text("Ввести вручную") }
                            }
                        }
                        is RawgState.Results -> LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(st.items, key = { it.id }) { item ->
                                RawgRow(item, enabled = !viewModel.rawgApplying) {
                                    viewModel.applyRawg(item, onDone = onDismiss)
                                }
                            }
                        }
                    }
                    if (viewModel.rawgApplying) {
                        Box(
                            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color.White)
                                Spacer(Modifier.height(10.dp))
                                Text("Загружаем данные и обложку…", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Hint(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RawgRow(item: RawgGame, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GameCover(item.imageUrl, item.name, Modifier.width(84.dp).height(56.dp), corner = 10.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val meta = listOfNotNull(item.year, item.genres.take(2).joinToString(", ").ifEmpty { null }).joinToString(" · ")
            if (meta.isNotEmpty()) {
                Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (item.platforms.isNotEmpty()) {
                Text(
                    item.platforms.take(4).joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

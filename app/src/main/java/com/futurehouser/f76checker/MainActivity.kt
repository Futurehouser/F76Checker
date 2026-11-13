package com.futurehouser.f76checker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.launch
import java.text.Normalizer

private val PipGreen = Color(0xFF3CFF7A)
private val PipDim = Color(0xFF1F8A45)
private val PipBg = Color(0xFF0B140D)
private val PipSurface = Color(0xFF122016)
private val PipHeader = Color(0xFF17301F)

private val PipColors = darkColorScheme(
    primary = PipGreen,
    onPrimary = PipBg,
    secondary = PipDim,
    onSecondary = PipBg,
    background = PipBg,
    onBackground = PipGreen,
    surface = PipSurface,
    onSurface = PipGreen,
    surfaceVariant = PipHeader,
    onSurfaceVariant = PipGreen.copy(alpha = 0.75f),
    outline = PipDim,
    secondaryContainer = PipDim,
    onSecondaryContainer = PipBg,
)

enum class Filter { ALL, OWNED, MISSING }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(this)
        setContent {
            MaterialTheme(colorScheme = PipColors) {
                Surface(Modifier.fillMaxSize(), color = PipBg) { App(prefs) }
            }
        }
    }
}

private fun fold(s: String) =
    Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase()

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun App(prefs: Prefs) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var lang by remember { mutableStateOf(prefs.lang) }
    val s = remember(lang) { Strings(lang) }

    var categoryIndex by remember { mutableIntStateOf(0) }
    val category = CATEGORIES[categoryIndex]
    val items = remember(category) { Repository.items(context, category) }
    var owned by remember(category) { mutableStateOf(prefs.owned(category.id)) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(Filter.ALL) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

    // ---- updater state ----
    var release by remember { mutableStateOf<Release?>(null) }
    var progress by remember { mutableStateOf<Float?>(null) }
    fun checkUpdates(manual: Boolean) = scope.launch {
        try {
            val r = Updater.checkForUpdate()
            if (r != null && (manual || prefs.skippedVersion != r.version)) release = r
            else if (manual) Toast.makeText(context, s.upToDate, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            if (manual) Toast.makeText(context, s.updateError, Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(Unit) { checkUpdates(manual = false) }

    fun toggle(id: String) {
        owned = if (id in owned) owned - id else owned + id
        prefs.setOwned(category.id, owned)
    }

    val folded = fold(query.trim())
    val visible = items.filter { item ->
        (folded.isEmpty() || fold(item.en).contains(folded) || fold(item.es).contains(folded)) &&
            when (filter) {
                Filter.ALL -> true
                Filter.OWNED -> item.id in owned
                Filter.MISSING -> item.id !in owned
            }
    }
    val grouped = visible.groupBy { it.section }
    val ownedCount = items.count { it.id in owned }

    Scaffold(
        containerColor = PipBg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PipBg, titleContentColor = PipGreen),
                title = {
                    Text(
                        "F76 CHECKER",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                },
                actions = {
                    LanguageToggle(lang) { lang = it; prefs.lang = it }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = null, tint = PipGreen)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(s.checkUpdates) },
                                onClick = { menuOpen = false; checkUpdates(manual = true) })
                            DropdownMenuItem(
                                text = { Text(s.resetAll) },
                                onClick = { menuOpen = false; confirmReset = true })
                            DropdownMenuItem(
                                text = { Text("v${BuildConfig.VERSION_NAME}", color = PipDim) },
                                enabled = false, onClick = {})
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (CATEGORIES.size > 1) {
                ScrollableTabRow(
                    selectedTabIndex = categoryIndex,
                    containerColor = PipBg,
                    contentColor = PipGreen,
                    edgePadding = 8.dp
                ) {
                    CATEGORIES.forEachIndexed { i, c ->
                        Tab(selected = i == categoryIndex, onClick = { categoryIndex = i },
                            text = { Text(c.name(lang)) })
                    }
                }
            }

            // progress
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(category.name(lang).uppercase(), fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("$ownedCount / ${items.size}", fontFamily = FontFamily.Monospace)
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { if (items.isEmpty()) 0f else ownedCount.toFloat() / items.size },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = PipGreen,
                    trackColor = PipHeader,
                )
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(s.search) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, null) }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PipGreen, unfocusedBorderColor = PipDim,
                    cursorColor = PipGreen, focusedTextColor = PipGreen, unfocusedTextColor = PipGreen,
                )
            )

            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Filter.ALL to s.all, Filter.OWNED to s.owned, Filter.MISSING to s.missing).forEach { (f, label) ->
                    FilterChip(
                        selected = filter == f,
                        onClick = { filter = f },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PipGreen, selectedLabelColor = PipBg, labelColor = PipGreen
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true, selected = filter == f, borderColor = PipDim
                        )
                    )
                }
            }

            val listState = rememberLazyListState()
            LaunchedEffect(filter, query, categoryIndex) { listState.scrollToItem(0) }
            if (visible.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(s.noResults, color = PipDim) }
            } else LazyColumn(state = listState, modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)) {
                grouped.forEach { (section, list) ->
                    stickyHeader(key = "h_$section") {
                        val all = items.filter { it.section == section }
                        Row(
                            Modifier.fillMaxWidth().background(PipHeader).padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text((s.sections[section] ?: section).uppercase(), fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Text("${all.count { it.id in owned }}/${all.size}", fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp)
                        }
                    }
                    items(list, key = { it.id }) { item ->
                        ItemRow(
                            item = item,
                            lang = lang,
                            checked = item.id in owned,
                            onToggle = { toggle(item.id) },
                            onLongPress = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.wiki)))
                            }
                        )
                    }
                }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            text = { Text(s.resetConfirm) },
            confirmButton = {
                TextButton(onClick = {
                    owned = emptySet(); prefs.setOwned(category.id, owned); confirmReset = false
                }) { Text(s.ok) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(s.cancel) } },
            containerColor = PipSurface,
        )
    }

    release?.let { r ->
        AlertDialog(
            onDismissRequest = { if (progress == null) release = null },
            title = { Text(s.updateTitle) },
            text = {
                Column {
                    if (progress != null) {
                        Text(s.downloading)
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(progress = { progress ?: 0f }, modifier = Modifier.fillMaxWidth(),
                            color = PipGreen, trackColor = PipHeader)
                    } else {
                        Text(s.updateBody(r.version, BuildConfig.VERSION_NAME))
                        if (r.notes.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Text(s.releaseNotes, fontWeight = FontWeight.Bold)
                            Text(r.notes.take(800), fontSize = 13.sp, color = PipGreen.copy(alpha = 0.8f))
                        }
                    }
                }
            },
            confirmButton = {
                if (progress == null) TextButton(onClick = {
                    if (!Updater.canInstall(context)) {
                        Toast.makeText(context, s.allowInstall, Toast.LENGTH_LONG).show()
                        Updater.openInstallPermissionSettings(context)
                        return@TextButton
                    }
                    progress = 0f
                    scope.launch {
                        try {
                            val apk = Updater.download(context, r) { progress = it }
                            Updater.install(context, apk)
                            release = null
                        } catch (e: Exception) {
                            Toast.makeText(context, s.downloadError, Toast.LENGTH_SHORT).show()
                        } finally {
                            progress = null
                        }
                    }
                }) { Text(s.update) }
            },
            dismissButton = {
                if (progress == null) Row {
                    TextButton(onClick = { prefs.skippedVersion = r.version; release = null }) { Text(s.skip) }
                    TextButton(onClick = { release = null }) { Text(s.later) }
                }
            },
            containerColor = PipSurface,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LanguageToggle(lang: Lang, onChange: (Lang) -> Unit) {
    Row(
        Modifier.padding(end = 4.dp).clip(RoundedCornerShape(50)).border(1.dp, PipDim, RoundedCornerShape(50))
    ) {
        Lang.entries.forEach { l ->
            val sel = l == lang
            Text(
                l.name,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (sel) PipBg else PipGreen,
                modifier = Modifier
                    .background(if (sel) PipGreen else Color.Transparent)
                    .combinedClickable(onClick = { onChange(l) })
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemRow(item: Item, lang: Lang, checked: Boolean, onToggle: () -> Unit, onLongPress: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onToggle, onLongClick = onLongPress)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFDDE5DD)),
            contentAlignment = Alignment.Center
        ) {
            SubcomposeAsyncImage(
                model = item.image,
                contentDescription = item.name(lang),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(4.dp),
                loading = { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = PipDim) },
                error = { Text("?", color = PipDim, fontSize = 24.sp) }
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.name(lang), fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                color = if (checked) PipGreen else PipGreen.copy(alpha = 0.9f))
            val other = item.name(if (lang == Lang.ES) Lang.EN else Lang.ES)
            if (other != item.name(lang)) {
                Text(other, fontSize = 12.sp, color = PipDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = PipGreen, uncheckedColor = PipDim, checkmarkColor = PipBg)
        )
    }
}

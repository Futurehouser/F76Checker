package com.futurehouser.f76checker

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

val PipGreen = Color(0xFF3CFF7A)
val PipDim = Color(0xFF1F8A45)
val PipBg = Color(0xFF0B140D)
val PipSurface = Color(0xFF122016)
val PipHeader = Color(0xFF17301F)
val ImageBg = Color(0xFFDDE5DD)

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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(this)
        Sfx.enabled = prefs.sound
        Sfx.init(this)
        setContent {
            MaterialTheme(colorScheme = PipColors) {
                Surface(Modifier.fillMaxSize(), color = PipBg) { App(prefs) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(prefs: Prefs) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var lang by remember { mutableStateOf(prefs.lang) }
    val s = remember(lang) { Strings(lang) }
    var sound by remember { mutableStateOf(prefs.sound) }

    // null = home page, otherwise the open category's id
    var current by rememberSaveable { mutableStateOf<String?>(null) }
    val category = CATEGORIES.firstOrNull { it.id == current }

    val owned = remember {
        mutableStateMapOf<String, Set<String>>().apply { CATEGORIES.forEach { put(it.id, prefs.owned(it.id)) } }
    }
    val data = remember { CATEGORIES.associate { it.id to Repository.load(context, it) } }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    LaunchedEffect(drawerState) {
        snapshotFlow { drawerState.targetValue }.drop(1).collect { Sfx.play(Sound.DRAWER) }
    }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

    fun open(id: String?) {
        if (id != current) Sfx.play(if (id == null) Sound.BACK else Sound.NAV)
        current = id
        scope.launch { drawerState.close() }
    }

    BackHandler(enabled = drawerState.isOpen || current != null) {
        if (drawerState.isOpen) scope.launch { drawerState.close() } else open(null)
    }

    fun setOwned(catId: String, newSet: Set<String>) {
        owned[catId] = newSet
        prefs.setOwned(catId, newSet)
    }

    fun toggle(cat: Category, item: Item) {
        val cur = owned[cat.id].orEmpty()
        val checking = item.id !in cur
        val next = if (checking) cur + item.id else cur - item.id
        setOwned(cat.id, next)
        val sectionDone = checking && data.getValue(cat.id).items
            .filter { it.section == item.section }.all { it.id in next }
        Sfx.play(if (sectionDone && cat.isSets) Sound.COMPLETE else if (checking) Sound.CHECK else Sound.UNCHECK)
    }

    fun setSection(cat: Category, sectionId: String, check: Boolean) {
        val ids = data.getValue(cat.id).items.filter { it.section == sectionId }.map { it.id }
        val cur = owned[cat.id].orEmpty()
        setOwned(cat.id, if (check) cur + ids else cur - ids.toSet())
        Sfx.play(if (check) Sound.COMPLETE else Sound.UNCHECK)
    }

    // ---- updater ----
    var release by remember { mutableStateOf<Release?>(null) }
    var progress by remember { mutableStateOf<Float?>(null) }
    fun checkUpdates(manual: Boolean) = scope.launch {
        try {
            val r = Updater.checkForUpdate()
            if (r != null && (manual || prefs.skippedVersion != r.version)) {
                release = r; Sfx.play(Sound.ALERT)
            } else if (manual) Toast.makeText(context, s.upToDate, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            if (manual) Toast.makeText(context, s.updateError, Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(Unit) { checkUpdates(manual = false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = PipSurface, drawerContentColor = PipGreen) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("F76 CHECKER", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                        fontSize = 22.sp, letterSpacing = 2.sp, modifier = Modifier.padding(20.dp))
                    HorizontalDivider(color = PipDim)
                    Spacer(Modifier.height(8.dp))
                    DrawerItem(
                        label = s.home,
                        selected = current == null,
                        icon = { Icon(Icons.Filled.Home, null) },
                        onClick = { open(null) }
                    )
                    Text(s.categories.uppercase(), fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                        color = PipDim, modifier = Modifier.padding(start = 28.dp, top = 16.dp, bottom = 8.dp))
                    CATEGORIES.forEach { c ->
                        val total = data.getValue(c.id).items.size
                        val have = data.getValue(c.id).items.count { it.id in owned[c.id].orEmpty() }
                        DrawerItem(
                            label = c.name(lang),
                            selected = current == c.id,
                            icon = { Text(if (c.isSets) "▣" else "▤", fontSize = 18.sp) },
                            badge = "$have/$total",
                            onClick = { open(c.id) }
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    ) {
        Scaffold(
            containerColor = PipBg,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PipBg, titleContentColor = PipGreen),
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = s.openMenu, tint = PipGreen)
                        }
                    },
                    title = {
                        Text(
                            category?.name(lang)?.uppercase() ?: "F76 CHECKER",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            maxLines = 1,
                        )
                    },
                    actions = {
                        LanguageToggle(lang) { lang = it; prefs.lang = it; Sfx.play(Sound.LANG) }
                        Box {
                            IconButton(onClick = { menuOpen = true; Sfx.play(Sound.CLICK) }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = null, tint = PipGreen)
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text(if (sound) s.soundOn else s.soundOff) },
                                    onClick = {
                                        sound = !sound; prefs.sound = sound; Sfx.enabled = sound
                                        Sfx.play(Sound.CLICK); menuOpen = false
                                    })
                                DropdownMenuItem(
                                    text = { Text(s.checkUpdates) },
                                    onClick = { menuOpen = false; Sfx.play(Sound.CLICK); checkUpdates(manual = true) })
                                if (category != null) DropdownMenuItem(
                                    text = { Text(s.resetAll) },
                                    onClick = { menuOpen = false; Sfx.play(Sound.ALERT); confirmReset = true })
                                DropdownMenuItem(
                                    text = { Text("v${BuildConfig.VERSION_NAME}", color = PipDim) },
                                    enabled = false, onClick = {})
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                if (category == null) {
                    HomeScreen(lang, s, data, owned) { open(it.id) }
                } else {
                    CategoryScreen(
                        category = category,
                        data = data.getValue(category.id),
                        owned = owned[category.id].orEmpty(),
                        lang = lang,
                        s = s,
                        onToggle = { toggle(category, it) },
                        onSetSection = { sec, check -> setSection(category, sec, check) },
                    )
                }
            }
        }
    }

    if (confirmReset && category != null) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            text = { Text(s.resetConfirm) },
            confirmButton = {
                TextButton(onClick = {
                    setOwned(category.id, emptySet()); confirmReset = false; Sfx.play(Sound.UNCHECK)
                }) { Text(s.ok) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false; Sfx.play(Sound.CLICK) }) { Text(s.cancel) }
            },
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
                    Sfx.play(Sound.CLICK)
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
                            Sfx.play(Sound.ALERT)
                            Toast.makeText(context, s.downloadError, Toast.LENGTH_SHORT).show()
                        } finally {
                            progress = null
                        }
                    }
                }) { Text(s.update) }
            },
            dismissButton = {
                if (progress == null) Row {
                    TextButton(onClick = {
                        prefs.skippedVersion = r.version; release = null; Sfx.play(Sound.CLICK)
                    }) { Text(s.skip) }
                    TextButton(onClick = { release = null; Sfx.play(Sound.CLICK) }) { Text(s.later) }
                }
            },
            containerColor = PipSurface,
        )
    }
}

@Composable
private fun DrawerItem(
    label: String,
    selected: Boolean,
    icon: @Composable () -> Unit,
    badge: String? = null,
    onClick: () -> Unit,
) {
    NavigationDrawerItem(
        label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
        selected = selected,
        onClick = onClick,
        icon = icon,
        badge = badge?.let { { Text(it, fontFamily = FontFamily.Monospace, fontSize = 12.sp) } },
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = PipHeader,
            unselectedContainerColor = Color.Transparent,
            selectedTextColor = PipGreen, unselectedTextColor = PipGreen.copy(alpha = 0.85f),
            selectedIconColor = PipGreen, unselectedIconColor = PipGreen.copy(alpha = 0.85f),
            selectedBadgeColor = PipGreen, unselectedBadgeColor = PipDim,
        ),
        modifier = Modifier.padding(horizontal = 12.dp)
    )
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
                    .combinedClickable(onClick = { if (!sel) onChange(l) })
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

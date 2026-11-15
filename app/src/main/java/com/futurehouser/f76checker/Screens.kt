package com.futurehouser.f76checker

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import java.text.Normalizer

enum class Filter { ALL, OWNED, MISSING }

private fun fold(s: String) =
    Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase()

@Composable
fun WikiImage(url: String?, description: String, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(8.dp)).background(ImageBg),
        contentAlignment = Alignment.Center
    ) {
        if (url != null) SubcomposeAsyncImage(
            model = url,
            contentDescription = description,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(4.dp),
            loading = { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = PipDim) },
            error = { Text("?", color = PipDim, fontSize = 24.sp) }
        )
    }
}

@Composable
private fun Progress(have: Int, total: Int, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { if (total == 0) 0f else have.toFloat() / total },
        modifier = modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
        color = PipGreen,
        trackColor = PipHeader,
    )
}

// ---------------------------------------------------------------- Home

@Composable
fun HomeScreen(
    lang: Lang,
    s: Strings,
    data: Map<String, CategoryData>,
    owned: Map<String, Set<String>>,
    onOpen: (Category) -> Unit,
) {
    val context = LocalContext.current
    val total = data.values.sumOf { it.items.size }
    val have = CATEGORIES.sumOf { c -> data.getValue(c.id).items.count { it.id in owned[c.id].orEmpty() } }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, PipDim, RoundedCornerShape(12.dp))
                    .background(PipSurface).padding(16.dp)
            ) {
                Text(s.homeSubtitle, color = PipGreen.copy(alpha = 0.8f))
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.totalProgress.uppercase(), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f))
                    Text("$have / $total", fontFamily = FontFamily.Monospace)
                }
                Spacer(Modifier.height(6.dp))
                Progress(have, total)
            }
        }
        items(CATEGORIES, key = { it.id }) { c ->
            val d = data.getValue(c.id)
            val got = d.items.count { it.id in owned[c.id].orEmpty() }
            val cover = remember(c.id) { Repository.cover(context, c) }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .border(1.dp, PipDim, RoundedCornerShape(12.dp))
                    .background(PipSurface)
                    .clickable { onOpen(c) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WikiImage(cover, c.name(lang), 88.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(c.name(lang).uppercase(), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                        fontSize = 17.sp)
                    Text(
                        if (c.isSets) "${s.setsCount(d.sections.size)} · ${s.itemsCount(d.items.size)}"
                        else s.itemsCount(d.items.size),
                        fontSize = 13.sp, color = PipGreen.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Progress(got, d.items.size, Modifier.weight(1f))
                        Spacer(Modifier.width(10.dp))
                        Text("$got/${d.items.size}", fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    }
                }
                Text("›", fontSize = 28.sp, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

// ---------------------------------------------------------------- Category list

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryScreen(
    category: Category,
    data: CategoryData,
    owned: Set<String>,
    lang: Lang,
    s: Strings,
    onToggle: (Item) -> Unit,
    onSetSection: (String, Boolean) -> Unit,
) {
    val context = LocalContext.current
    var query by remember(category.id) { mutableStateOf("") }
    var filter by remember(category.id) { mutableStateOf(Filter.ALL) }
    val items = data.items
    val ownedCount = items.count { it.id in owned }

    val folded = fold(query.trim())
    val visible = items.filter { item ->
        val sec = data.sectionById[item.section]
        (folded.isEmpty() || fold(item.en).contains(folded) || fold(item.es).contains(folded) ||
            (category.isSets && sec != null && (fold(sec.en).contains(folded) || fold(sec.es).contains(folded)))) &&
            when (filter) {
                Filter.ALL -> true
                Filter.OWNED -> item.id in owned
                Filter.MISSING -> item.id !in owned
            }
    }
    val grouped = data.sections.mapNotNull { sec ->
        visible.filter { it.section == sec.id }.takeIf { it.isNotEmpty() }?.let { sec to it }
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (category.isSets) s.setsCount(data.sections.size) else s.itemsCount(items.size),
                    color = PipGreen.copy(alpha = 0.75f), modifier = Modifier.weight(1f)
                )
                Text("$ownedCount / ${items.size}", fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.height(6.dp))
            Progress(ownedCount, items.size)
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(s.search) },
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { query = ""; Sfx.play(Sound.CLICK) }) {
                    Icon(Icons.Filled.Clear, null)
                }
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
                    onClick = { if (filter != f) { filter = f; Sfx.play(Sound.CLICK) } },
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
        LaunchedEffect(filter, query, category.id) { listState.scrollToItem(0) }
        if (visible.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(s.noResults, color = PipDim) }
        } else LazyColumn(
            state = listState, modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            grouped.forEach { (section, list) ->
                val all = items.filter { it.section == section.id }
                val got = all.count { it.id in owned }
                stickyHeader(key = "h_${section.id}") {
                    if (category.isSets) SetHeader(section, lang, s, got, all.size,
                        onSetAll = { onSetSection(section.id, it) },
                        onLongPress = { section.wiki?.let { openUrl(context, it) } })
                    else Row(
                        Modifier.fillMaxWidth().background(PipHeader).padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(section.name(lang).uppercase(), fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text("$got/${all.size}", fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    }
                }
                items(list, key = { it.id }) { item ->
                    ItemRow(
                        item = item,
                        lang = lang,
                        checked = item.id in owned,
                        imageSize = if (category.isSets) 56.dp else 72.dp,
                        onToggle = { onToggle(item) },
                        onLongPress = { openUrl(context, item.wiki) }
                    )
                }
            }
        }
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    Sfx.play(Sound.NAV)
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SetHeader(
    section: Section,
    lang: Lang,
    s: Strings,
    got: Int,
    total: Int,
    onSetAll: (Boolean) -> Unit,
    onLongPress: () -> Unit,
) {
    val complete = got == total
    Row(
        Modifier.fillMaxWidth().background(PipHeader)
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WikiImage(section.image, section.name(lang), 64.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(section.name(lang).uppercase(), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val other = section.name(if (lang == Lang.ES) Lang.EN else Lang.ES)
            if (other != section.name(lang)) Text(other, fontSize = 11.sp, color = PipDim, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Progress(got, total, Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text(if (complete) "★ $got/$total" else "$got/$total", fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TriStateCheckbox(
                state = when (got) {
                    0 -> ToggleableState.Off
                    total -> ToggleableState.On
                    else -> ToggleableState.Indeterminate
                },
                onClick = { onSetAll(!complete) },
                colors = CheckboxDefaults.colors(checkedColor = PipGreen, uncheckedColor = PipDim, checkmarkColor = PipBg)
            )
            Text(s.wholeSet, fontSize = 9.sp, color = PipDim)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemRow(
    item: Item,
    lang: Lang,
    checked: Boolean,
    imageSize: Dp,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onToggle, onLongClick = onLongPress)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WikiImage(item.image, item.name(lang), imageSize)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.name(lang), fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
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

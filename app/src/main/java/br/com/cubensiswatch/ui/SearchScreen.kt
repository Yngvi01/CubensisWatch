package br.com.cubensiswatch.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.cubensiswatch.data.Cultivation
import br.com.cubensiswatch.data.Culture
import br.com.cubensiswatch.data.Lineage
import br.com.cubensiswatch.data.Material
import java.text.DateFormat
import java.time.Instant
import java.util.Locale
import br.com.cubensiswatch.data.Note

private data class SearchResult(
    val category: String,
    val title: String,
    val detail: String,
    val route: String,
    val facets: Set<String>,
    val date: Long?,
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SearchScreen(
    cultivations: List<Cultivation>,
    cultures: List<Culture>,
    lineages: List<Lineage>,
    materials: List<Material>,
    noteViewModel: NoteViewModel,
    onOpen: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf("Todos") }
    var facetFilter by remember { mutableStateOf("Todos") }
    var categoryMenu by remember { mutableStateOf(false) }
    var facetMenu by remember { mutableStateOf(false) }
    var dateTarget by remember { mutableStateOf<Boolean?>(null) }
    var dateFrom by remember { mutableStateOf<Long?>(null) }
    var dateUntil by remember { mutableStateOf<Long?>(null) }
    val notes by noteViewModel.search(query).collectAsStateWithLifecycle(initialValue = emptyList())
    val normalizedQuery = query.trim().lowercase(Locale.ROOT)
    val results = remember(cultivations, cultures, lineages, materials, notes, normalizedQuery) {
        if (normalizedQuery.isBlank()) emptyList() else buildList {
            cultivations.filter { listOf(it.code, it.name, it.species, it.variety, it.lineage, it.status, it.method, it.notes).any { value -> value.contains(normalizedQuery, ignoreCase = true) } }
                .forEach { add(SearchResult("Cultivo", "${it.code} · ${it.name}", "${it.species.ifBlank { "Espécie não informada" }} · ${it.status}", "cultivation/${it.id}", setOf(it.status, it.species, it.variety).filter(String::isNotBlank).toSet(), it.startedAt)) }
            cultures.filter { listOf(it.code, it.name, it.type, it.status, it.species, it.variety, it.notes).any { value -> value.contains(normalizedQuery, ignoreCase = true) } }
                .forEach { add(SearchResult("Cultura", "${it.code} · ${it.name}", "${it.type} · ${it.status}", "culture/${it.id}", setOf(it.type, it.status, it.species, it.variety).filter(String::isNotBlank).toSet(), it.createdAt)) }
            lineages.filter { listOf(it.code, it.name, it.species, it.variety, it.description).any { value -> value.contains(normalizedQuery, ignoreCase = true) } }
                .forEach { add(SearchResult("Linhagem", "${it.code} · ${it.name}", listOf(it.species, it.variety).filter(String::isNotBlank).joinToString(" · "), "lineage/${it.id}", setOf(it.species, it.variety).filter(String::isNotBlank).toSet(), it.createdAt)) }
            materials.filter { listOf(it.name, it.category, it.location, it.notes).any { value -> value.contains(normalizedQuery, ignoreCase = true) } }
                .forEach { add(SearchResult("Material", it.name, "${it.category} · ${it.quantity} ${it.unit}", "materiais", setOf(it.category), it.lastPurchaseAt)) }
            notes.forEach { add(SearchResult("Anotação", it.title, it.text.take(100), "anotacoes", it.tags.split(',').map(String::trim).filter(String::isNotBlank).toSet(), it.updatedAt)) }
        }
    }
    val categories = listOf("Todos") + results.map { it.category }.distinct().sorted()
    val facets = listOf("Todos") + results.asSequence()
        .filter { categoryFilter == "Todos" || it.category == categoryFilter }
        .flatMap { it.facets.asSequence() }
        .filter(String::isNotBlank)
        .distinct()
        .sorted()
        .toList()
    val filteredResults = results.filter { result ->
        (categoryFilter == "Todos" || result.category == categoryFilter) &&
            (facetFilter == "Todos" || facetFilter in result.facets) &&
            (dateFrom == null || result.date != null && result.date >= dateFrom!!) &&
            (dateUntil == null || result.date != null && result.date < dateUntil!! + MILLIS_PER_DAY)
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Busca local", style = MaterialTheme.typography.headlineMedium) }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Cultivos, culturas, linhagens, materiais e anotações") },
            )
        }
        if (normalizedQuery.isNotBlank()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) {
                            OutlinedButton(onClick = { categoryMenu = true }, modifier = Modifier.fillMaxWidth()) { Text(categoryFilter) }
                            DropdownMenu(categoryMenu, { categoryMenu = false }) {
                                categories.forEach { value -> DropdownMenuItem(text = { Text(value) }, onClick = { categoryFilter = value; facetFilter = "Todos"; categoryMenu = false }) }
                            }
                        }
                        Box(Modifier.weight(1f)) {
                            OutlinedButton(onClick = { facetMenu = true }, modifier = Modifier.fillMaxWidth(), enabled = facets.size > 1) { Text(facetFilter) }
                            DropdownMenu(facetMenu, { facetMenu = false }) {
                                facets.forEach { value -> DropdownMenuItem(text = { Text(value) }, onClick = { facetFilter = value; facetMenu = false }) }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { dateTarget = true }, modifier = Modifier.weight(1f)) {
                            Text(dateFrom?.let { "De ${dateLabel(it)}" } ?: "Data inicial")
                        }
                        OutlinedButton(onClick = { dateTarget = false }, modifier = Modifier.weight(1f)) {
                            Text(dateUntil?.let { "Até ${dateLabel(it)}" } ?: "Data final")
                        }
                    }
                    if (categoryFilter != "Todos" || facetFilter != "Todos" || dateFrom != null || dateUntil != null) {
                        TextButton(onClick = { categoryFilter = "Todos"; facetFilter = "Todos"; dateFrom = null; dateUntil = null }) { Text("Limpar filtros") }
                    }
                }
            }
        }
        if (normalizedQuery.isBlank()) {
            item { Text("Digite para pesquisar nos registros salvos neste dispositivo.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else if (filteredResults.isEmpty()) {
            item { Text("Nenhum registro encontrado com esses filtros.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            item { Text("${filteredResults.size} resultado(s)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(filteredResults, key = { "${it.category}:${it.route}:${it.title}" }) { result ->
                Card(Modifier.fillMaxWidth().clickable { onOpen(result.route) }) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(result.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(result.title, style = MaterialTheme.typography.titleMedium)
                        if (result.detail.isNotBlank()) Text(result.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    dateTarget?.let { isStart ->
        val selected = if (isStart) dateFrom else dateUntil
        val picker = rememberDatePickerState(initialSelectedDateMillis = selected ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { dateTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { if (isStart) dateFrom = it else dateUntil = it }
                    dateTarget = null
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { if (isStart) dateFrom = null else dateUntil = null; dateTarget = null }) { Text("Limpar") }
            },
        ) { DatePicker(state = picker) }
    }
}

private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
private fun dateLabel(value: Long): String = DateFormat.getDateInstance(DateFormat.SHORT).format(Instant.ofEpochMilli(value).toEpochMilli())

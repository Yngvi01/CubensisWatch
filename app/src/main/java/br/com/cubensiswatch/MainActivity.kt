package br.com.cubensiswatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import br.com.cubensiswatch.ui.CultivationDetailScreen
import br.com.cubensiswatch.ui.CultivationEditorScreen
import br.com.cubensiswatch.ui.CultivationListScreen
import br.com.cubensiswatch.ui.CultivationViewModel
import br.com.cubensiswatch.ui.CultureDetailScreen
import br.com.cubensiswatch.ui.CultureEditorScreen
import br.com.cubensiswatch.ui.CultureListScreen
import br.com.cubensiswatch.ui.DashboardViewModel
import br.com.cubensiswatch.ui.LineageDetailScreen
import br.com.cubensiswatch.ui.LineageEditorScreen
import br.com.cubensiswatch.ui.LineageListScreen
import br.com.cubensiswatch.ui.LineageViewModel
import br.com.cubensiswatch.ui.IncubatorViewModel
import br.com.cubensiswatch.ui.IncubatorListScreen
import br.com.cubensiswatch.ui.IncubatorEditorScreen
import br.com.cubensiswatch.ui.IncubatorDetailScreen
import br.com.cubensiswatch.ui.MaterialViewModel
import br.com.cubensiswatch.ui.MaterialScreen
import br.com.cubensiswatch.ui.ReminderViewModel
import br.com.cubensiswatch.ui.ReminderScreen
import br.com.cubensiswatch.ui.NoteViewModel
import br.com.cubensiswatch.ui.NoteScreen
import br.com.cubensiswatch.ui.NoteRecordOption
import br.com.cubensiswatch.ui.SearchScreen
import br.com.cubensiswatch.data.Reminder
import br.com.cubensiswatch.data.CultivationEvent
import br.com.cubensiswatch.data.LocalBackupManager
import java.text.DateFormat
import br.com.cubensiswatch.ui.theme.CubensisTheme

private data class Destination(val label: String, val route: String)
private val destinations = listOf(
    Destination("Início", "dashboard"), Destination("Cultivos", "cultivos"),
    Destination("Linhagens", "linhagens"), Destination("Culturas", "culturas"), Destination("Incubadora", "incubadora"),
    Destination("Materiais", "materiais"), Destination("Anotações", "anotacoes"),
    Destination("Busca", "busca"), Destination("Configurações", "configuracoes"),
)

class MainActivity : ComponentActivity() {
    private val dashboardViewModel: DashboardViewModel by viewModels {
        DashboardViewModel.Factory((application as CubensisWatchApp).cultivationRepository, (application as CubensisWatchApp).materialRepository)
    }
    private val cultivationViewModel: CultivationViewModel by viewModels {
        CultivationViewModel.Factory((application as CubensisWatchApp).cultivationRepository)
    }
    private val lineageViewModel: LineageViewModel by viewModels {
        LineageViewModel.Factory((application as CubensisWatchApp).lineageRepository)
    }
    private val incubatorViewModel: IncubatorViewModel by viewModels {
        IncubatorViewModel.Factory((application as CubensisWatchApp).incubatorRepository)
    }
    private val materialViewModel: MaterialViewModel by viewModels {
        MaterialViewModel.Factory((application as CubensisWatchApp).materialRepository)
    }
    private val reminderViewModel: ReminderViewModel by viewModels {
        ReminderViewModel.Factory((application as CubensisWatchApp).reminderRepository)
    }
    private val noteViewModel: NoteViewModel by viewModels {
        NoteViewModel.Factory((application as CubensisWatchApp).noteRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themePreferences = getSharedPreferences("appearance", MODE_PRIVATE)
        setContent {
            var themeMode by remember { mutableStateOf(themePreferences.getString("theme_mode", "system") ?: "system") }
            CubensisTheme(themeMode) {
                CubensisWatch(
                    dashboardViewModel, cultivationViewModel, lineageViewModel, incubatorViewModel,
                    materialViewModel, reminderViewModel, noteViewModel, (application as CubensisWatchApp).backupManager, themeMode,
                    onThemeModeChange = { selected ->
                        themeMode = selected
                        themePreferences.edit().putString("theme_mode", selected).apply()
                    },
                )
            }
        }
    }
}

@Composable
private fun CubensisWatch(viewModel: DashboardViewModel, cultivationViewModel: CultivationViewModel, lineageViewModel: LineageViewModel, incubatorViewModel: IncubatorViewModel, materialViewModel: MaterialViewModel, reminderViewModel: ReminderViewModel, noteViewModel: NoteViewModel, backupManager: LocalBackupManager, themeMode: String, onThemeModeChange: (String) -> Unit) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route.orEmpty()
    val active by viewModel.activeCount.collectAsStateWithLifecycle()
    val incubating by viewModel.incubatingCount.collectAsStateWithLifecycle()
    val problems by viewModel.problemCount.collectAsStateWithLifecycle()
    val lowStock by viewModel.lowStockCount.collectAsStateWithLifecycle()
    val activeCultures by lineageViewModel.activeCultureCount.collectAsStateWithLifecycle(initialValue = 0)
    val lineages by lineageViewModel.lineages.collectAsStateWithLifecycle(initialValue = emptyList())
    val cultures by lineageViewModel.cultures.collectAsStateWithLifecycle(initialValue = emptyList())
    val incubators by incubatorViewModel.incubators.collectAsStateWithLifecycle(initialValue = emptyList())
    val allCultivations by cultivationViewModel.cultivations.collectAsStateWithLifecycle(initialValue = emptyList())
    val materials by materialViewModel.materials.collectAsStateWithLifecycle(initialValue = emptyList())
    val reminders by reminderViewModel.reminders.collectAsStateWithLifecycle(initialValue = emptyList())
    val recentEvents by cultivationViewModel.recentEvents.collectAsStateWithLifecycle(initialValue = emptyList())
    val noteRecords = buildList {
        allCultivations.forEach { add(NoteRecordOption("cultivo", it.id, "${it.code} · ${it.name}")) }
        cultures.forEach { add(NoteRecordOption("cultura", it.id, "${it.code} · ${it.name}")) }
        lineages.forEach { add(NoteRecordOption("linhagem", it.id, "${it.code} · ${it.name}")) }
        materials.forEach { add(NoteRecordOption("material", it.id, it.name)) }
    }
    Scaffold(
        bottomBar = {
            NavigationBar {
                (destinations.take(4) + Destination("Mais", "mais")).forEach { destination ->
                    NavigationBarItem(
                        selected = when (destination.route) {
                            "cultivos" -> currentRoute == "cultivos" || currentRoute.startsWith("cultivation/") || currentRoute.startsWith("cultivations/new")
                            "linhagens" -> currentRoute == "linhagens" || currentRoute.startsWith("lineage/")
                            "culturas" -> currentRoute == "culturas" || currentRoute.startsWith("culture/")
                            "mais" -> currentRoute == "mais" || currentRoute == "incubadora" || currentRoute.startsWith("incubator/") || currentRoute in setOf("incubators/new", "materiais", "anotacoes", "busca", "configuracoes")
                            else -> currentRoute == destination.route
                        },
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = false }
                                launchSingleTop = true
                                restoreState = false
                            }
                        },
                        icon = { Text(destination.label.take(1)) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(navController, startDestination = "dashboard", modifier = Modifier.padding(padding)) {
            composable("dashboard") {
                DashboardScreen(active, incubating, activeCultures, lowStock, problems, reminders, recentEvents, allCultivations, materials,
                    onCreateCultivation = { navController.navigate("cultivations/new") },
                    onCreateCulture = { navController.navigate("cultures/new") },
                    onCreateMaterial = { navController.navigate("materiais") },
                    onCreateNote = { navController.navigate("anotacoes") },
                    onSearch = { navController.navigate("busca") },
                    onOpenCultivation = { navController.navigate("cultivation/$it") },
                    onOpenReminders = { navController.navigate("configuracoes") },
                    onOpenCultivations = { navController.navigate("cultivos") },
                    onOpenMaterials = { navController.navigate("materiais") },
                )
            }
            composable("cultivos") {
                CultivationListScreen(
                    viewModel = cultivationViewModel,
                    onCreate = { navController.navigate("cultivations/new") },
                    onOpen = { id -> navController.navigate("cultivation/$id") },
                )
            }
            composable(
                "cultivations/new?lineageId={lineageId}&sourceCultureId={sourceCultureId}",
                arguments = listOf(
                    navArgument("lineageId") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("sourceCultureId") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) { entry ->
                CultivationEditorScreen(
                    initial = null, lineages = lineages, cultures = cultures, incubators = incubators,
                    initialLineageId = entry.arguments?.getString("lineageId")?.toLongOrNull(),
                    initialSourceCultureId = entry.arguments?.getString("sourceCultureId")?.toLongOrNull(),
                    viewModel = cultivationViewModel,
                    onSaved = { id -> navController.popBackStack(); navController.navigate("cultivation/$id") },
                    onCancel = { navController.popBackStack() },
                )
            }
            composable("cultivation/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("id") ?: return@composable
                CultivationDetailScreen(
                    id = id, viewModel = cultivationViewModel,
                    onBack = { navController.popBackStack("cultivos", inclusive = false) },
                    onEdit = { navController.navigate("cultivation/$id/edit") },
                    onDeleted = { navController.popBackStack() },
                )
            }
            composable("cultivation/{id}/edit", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("id") ?: return@composable
                val cultivation by cultivationViewModel.cultivation(id).collectAsStateWithLifecycle(initialValue = null)
                if (cultivation != null) {
                    CultivationEditorScreen(
                        initial = cultivation, lineages = lineages, cultures = cultures, incubators = incubators, viewModel = cultivationViewModel,
                        onSaved = { navController.popBackStack() },
                        onCancel = { navController.popBackStack() },
                    )
                } else PlaceholderScreen("Carregando cultivo…")
            }
            composable("linhagens") {
                LineageListScreen(lineageViewModel, onCreate = { navController.navigate("lineages/new") }, onOpen = { navController.navigate("lineage/$it") })
            }
            composable("lineages/new") {
                LineageEditorScreen(null, lineageViewModel, onSaved = { id -> navController.popBackStack(); navController.navigate("lineage/$id") }, onCancel = { navController.popBackStack() })
            }
            composable("lineage/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("id") ?: return@composable
                LineageDetailScreen(
                    id, lineageViewModel,
                    onEdit = { navController.navigate("lineage/$id/edit") },
                    onAddCulture = { lineageId -> navController.navigate("cultures/new?lineageId=$lineageId") },
                    onCulture = { cultureId -> navController.navigate("culture/$cultureId") },
                    onCultivation = { cultivationId -> navController.navigate("cultivation/$cultivationId") },
                )
            }
            composable("lineage/{id}/edit", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("id") ?: return@composable
                val lineage by lineageViewModel.lineage(id).collectAsStateWithLifecycle(initialValue = null)
                if (lineage != null) LineageEditorScreen(lineage, lineageViewModel, onSaved = { navController.popBackStack() }, onCancel = { navController.popBackStack() })
                else PlaceholderScreen("Carregando linhagem…")
            }
            composable("culturas") {
                CultureListScreen(lineageViewModel, onCreate = { navController.navigate("cultures/new") }, onOpen = { navController.navigate("culture/$it") })
            }
            composable(
                "cultures/new?lineageId={lineageId}",
                arguments = listOf(navArgument("lineageId") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) { entry ->
                CultureEditorScreen(
                    null, entry.arguments?.getString("lineageId")?.toLongOrNull(),
                    lineages, cultures, incubators, lineageViewModel,
                    onSaved = { id -> navController.popBackStack(); navController.navigate("culture/$id") },
                    onCancel = { navController.popBackStack() },
                )
            }
            composable("culture/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("id") ?: return@composable
                CultureDetailScreen(
                    id, lineageViewModel,
                    onEdit = { navController.navigate("culture/$id/edit") },
                    onDeleted = { navController.popBackStack() },
                    onNewCultivation = { lineageId, cultureId -> navController.navigate("cultivations/new?lineageId=$lineageId&sourceCultureId=$cultureId") },
                )
            }
            composable("culture/{id}/edit", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("id") ?: return@composable
                val culture by lineageViewModel.culture(id).collectAsStateWithLifecycle(initialValue = null)
                if (culture != null) CultureEditorScreen(culture, culture?.lineageId, lineages, cultures, incubators, lineageViewModel, onSaved = { navController.popBackStack() }, onCancel = { navController.popBackStack() })
                else PlaceholderScreen("Carregando cultura…")
            }
            composable("incubadora") {
                IncubatorListScreen(incubatorViewModel, onOpen = { navController.navigate("incubator/$it") }, onCreate = { navController.navigate("incubators/new") })
            }
            composable("materiais") { MaterialScreen(materialViewModel) }
            composable("anotacoes") { NoteScreen(noteViewModel, noteRecords) }
            composable("busca") {
                SearchScreen(allCultivations, cultures, lineages, materials, noteViewModel) { route -> navController.navigate(route) }
            }
            composable("configuracoes") { ReminderScreen(reminderViewModel, themeMode, onThemeModeChange, backupManager) }
            composable("incubators/new") {
                IncubatorEditorScreen(null, incubatorViewModel, onSaved = { navController.popBackStack(); navController.navigate("incubator/$it") }, onCancel = { navController.popBackStack() })
            }
            composable("incubator/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("id") ?: return@composable
                IncubatorDetailScreen(id, incubatorViewModel, onEdit = { navController.navigate("incubator/$id/edit") })
            }
            composable("incubator/{id}/edit", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("id") ?: return@composable
                val incubator by incubatorViewModel.incubator(id).collectAsStateWithLifecycle(initialValue = null)
                if (incubator != null) IncubatorEditorScreen(incubator, incubatorViewModel, onSaved = { navController.popBackStack() }, onCancel = { navController.popBackStack() })
                else PlaceholderScreen("Carregando incubadora…")
            }
            destinations.drop(1).filterNot { it.route in setOf("cultivos", "linhagens", "culturas", "incubadora", "materiais", "anotacoes", "busca", "configuracoes") }.forEach { destination ->
                composable(destination.route) { PlaceholderScreen(destination.label) }
            }
            composable("mais") {
                MoreScreen(onOpen = { route -> navController.navigate(route) })
            }
        }
    }
}

@Composable
private fun DashboardScreen(
    active: Int,
    incubating: Int,
    activeCultures: Int,
    lowStock: Int,
    problems: Int,
    reminders: List<Reminder>,
    recentEvents: List<CultivationEvent>,
    cultivations: List<br.com.cubensiswatch.data.Cultivation>,
    materials: List<br.com.cubensiswatch.data.Material>,
    onCreateCultivation: () -> Unit,
    onCreateCulture: () -> Unit,
    onCreateMaterial: () -> Unit,
    onCreateNote: () -> Unit,
    onSearch: () -> Unit,
    onOpenCultivation: (Long) -> Unit,
    onOpenReminders: () -> Unit,
    onOpenCultivations: () -> Unit,
    onOpenMaterials: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Image(painterResource(br.com.cubensiswatch.R.drawable.app_logo), contentDescription = "Logo Cubensis Watch", modifier = Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
                Column {
                    Text("Cubensis Watch", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("Seu diário de cultivo, sempre à mão.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { Text("Visão geral", style = MaterialTheme.typography.titleLarge) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Cultivos ativos", active.toString(), Modifier.weight(1f))
                MetricCard("Em incubação", incubating.toString(), Modifier.weight(1f))
            }
        }
        item { MetricCard("Culturas ativas", activeCultures.toString(), Modifier.fillMaxWidth()) }
        item { MetricCard("Materiais abaixo do mínimo", lowStock.toString(), Modifier.fillMaxWidth().clickable(onClick = onOpenMaterials)) }
        item { MetricCard("Cultivos contaminados", problems.toString(), Modifier.fillMaxWidth().clickable(onClick = onOpenCultivations)) }
        val contaminatedCultivations = cultivations.filter { it.status == "Contaminado" }.take(3)
        if (contaminatedCultivations.isNotEmpty()) {
            item { Text("Acompanhar cultivos contaminados", style = MaterialTheme.typography.titleLarge) }
            items(contaminatedCultivations, key = { "dashboard-contaminated-${it.id}" }) { cultivation ->
                Card(Modifier.fillMaxWidth().clickable { onOpenCultivation(cultivation.id) }) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(cultivation.name, style = MaterialTheme.typography.titleMedium)
                        Text("${cultivation.code} · ${cultivation.species.ifBlank { "Espécie não informada" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        val lowStockMaterials = materials.filter { it.quantity < it.minimum }.sortedBy { it.quantity - it.minimum }.take(3)
        if (lowStockMaterials.isNotEmpty()) {
            item { Text("Precisam de reposição", style = MaterialTheme.typography.titleLarge) }
            items(lowStockMaterials, key = { "dashboard-low-stock-${it.id}" }) { material ->
                Card(Modifier.fillMaxWidth().clickable(onClick = onOpenMaterials)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(material.name, style = MaterialTheme.typography.titleMedium)
                            Text("Mínimo: ${material.minimum.prettyDashboard()} ${material.unit}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${material.quantity.prettyDashboard()} ${material.unit}", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        item { Text("Próximos lembretes", style = MaterialTheme.typography.titleLarge) }
        val nextReminders = reminders.filter { it.enabled }.sortedBy { it.dueAt }.take(3)
        if (nextReminders.isEmpty()) item { Text("Nenhum lembrete ativo.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        else items(nextReminders, key = { "dashboard-reminder-${it.id}" }) { reminder ->
            Card(Modifier.fillMaxWidth().clickable(onClick = onOpenReminders)) {
                Column(Modifier.padding(14.dp)) {
                    Text(reminder.title, style = MaterialTheme.typography.titleMedium)
                    Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(reminder.dueAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { Text("Atividade recente", style = MaterialTheme.typography.titleLarge) }
        if (recentEvents.isEmpty()) item { Text("Ainda não há atividade registrada.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        else items(recentEvents, key = { "dashboard-event-${it.id}" }) { event ->
            val cultivation = cultivations.firstOrNull { it.id == event.cultivationId }
            Card(Modifier.fillMaxWidth().clickable { onOpenCultivation(event.cultivationId) }) {
                Column(Modifier.padding(14.dp)) {
                    Text(event.type, style = MaterialTheme.typography.titleSmall)
                    Text(event.description, style = MaterialTheme.typography.bodyMedium)
                    Text("${cultivation?.code ?: "Cultivo"} · ${cultivation?.name.orEmpty()} · ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(event.occurredAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { Text("Acesso rápido", style = MaterialTheme.typography.titleLarge) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCreateCultivation, modifier = Modifier.fillMaxWidth()) { Text("＋  Novo cultivo") }
                OutlinedButton(onClick = onCreateCulture, modifier = Modifier.fillMaxWidth()) { Text("＋  Nova cultura") }
                OutlinedButton(onClick = onCreateNote, modifier = Modifier.fillMaxWidth()) { Text("＋  Nova observação") }
                OutlinedButton(onClick = onSearch, modifier = Modifier.fillMaxWidth()) { Text("⌕  Buscar registros") }
                OutlinedButton(onClick = onCreateMaterial, modifier = Modifier.fillMaxWidth()) { Text("＋  Adicionar material") }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("Tudo pronto para começar", style = MaterialTheme.typography.titleMedium)
                    Text("Registre um cultivo para acompanhar etapas, observações e colheitas.", modifier = Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun Double.prettyDashboard(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text("Esta área será construída nas próximas fases.", modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MoreScreen(onOpen: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Mais áreas", style = MaterialTheme.typography.headlineMedium)
        destinations.drop(4).forEach { destination ->
            OutlinedButton(onClick = { onOpen(destination.route) }, modifier = Modifier.fillMaxWidth()) {
                Text(destination.label)
            }
        }
    }
}

package br.com.cubensiswatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
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
import br.com.cubensiswatch.ui.theme.CubensisTheme

private data class Destination(val label: String, val route: String)
private val destinations = listOf(
    Destination("Início", "dashboard"), Destination("Cultivos", "cultivos"),
    Destination("Linhagens", "linhagens"), Destination("Culturas", "culturas"), Destination("Incubadora", "incubadora"),
    Destination("Materiais", "materiais"), Destination("Anotações", "anotacoes"),
    Destination("Configurações", "configuracoes"),
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CubensisTheme { CubensisWatch(dashboardViewModel, cultivationViewModel, lineageViewModel, incubatorViewModel, materialViewModel, reminderViewModel) } }
    }
}

@Composable
private fun CubensisWatch(viewModel: DashboardViewModel, cultivationViewModel: CultivationViewModel, lineageViewModel: LineageViewModel, incubatorViewModel: IncubatorViewModel, materialViewModel: MaterialViewModel, reminderViewModel: ReminderViewModel) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val active by viewModel.activeCount.collectAsStateWithLifecycle()
    val incubating by viewModel.incubatingCount.collectAsStateWithLifecycle()
    val lowStock by viewModel.lowStockCount.collectAsStateWithLifecycle()
    val activeCultures by lineageViewModel.activeCultureCount.collectAsStateWithLifecycle(initialValue = 0)
    val lineages by lineageViewModel.lineages.collectAsStateWithLifecycle(initialValue = emptyList())
    val cultures by lineageViewModel.cultures.collectAsStateWithLifecycle(initialValue = emptyList())
    val incubators by incubatorViewModel.incubators.collectAsStateWithLifecycle(initialValue = emptyList())
    Scaffold(
        bottomBar = {
            NavigationBar {
                (destinations.take(4) + Destination("Mais", "mais")).forEach { destination ->
                    NavigationBarItem(
                        selected = backStack?.destination?.route == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
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
                DashboardScreen(active, incubating, activeCultures, lowStock,
                    onCreateCultivation = { navController.navigate("cultivations/new") },
                    onCreateCulture = { navController.navigate("cultures/new") },
                    onCreateMaterial = { navController.navigate("materiais") },
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
            composable("configuracoes") { ReminderScreen(reminderViewModel) }
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
            destinations.drop(1).filterNot { it.route in setOf("cultivos", "linhagens", "culturas", "incubadora", "materiais", "configuracoes") }.forEach { destination ->
                composable(destination.route) { PlaceholderScreen(destination.label) }
            }
            composable("mais") {
                MoreScreen(onOpen = { route -> navController.navigate(route) })
            }
        }
    }
}

@Composable
private fun DashboardScreen(active: Int, incubating: Int, activeCultures: Int, lowStock: Int, onCreateCultivation: () -> Unit, onCreateCulture: () -> Unit, onCreateMaterial: () -> Unit) {
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
        item { MetricCard("Materiais abaixo do mínimo", lowStock.toString(), Modifier.fillMaxWidth()) }
        item { Text("Acesso rápido", style = MaterialTheme.typography.titleLarge) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCreateCultivation, modifier = Modifier.fillMaxWidth()) { Text("＋  Novo cultivo") }
                OutlinedButton(onClick = onCreateCulture, modifier = Modifier.fillMaxWidth()) { Text("＋  Nova cultura") }
                OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("＋  Nova observação") }
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

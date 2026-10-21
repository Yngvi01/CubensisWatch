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
import br.com.cubensiswatch.data.Material
import br.com.cubensiswatch.data.ShoppingItem
import br.com.cubensiswatch.data.StockMovement
import br.com.cubensiswatch.data.Cultivation

private val materialCategories = listOf("Substrato", "Higiene", "Consumível", "Equipamento", "Recipiente", "Ferramenta", "Outro")

@Composable
fun MaterialScreen(vm: MaterialViewModel) {
    var purchases by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Material?>(null) }
    var creating by remember { mutableStateOf(false) }
    var moving by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var addItem by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Long?>(null) }
    val materials by vm.materials.collectAsStateWithLifecycle(initialValue = emptyList())
    val shopping by vm.shopping.collectAsStateWithLifecycle(initialValue = emptyList())
    val cultivations by vm.cultivations.collectAsStateWithLifecycle(initialValue = emptyList())
    val error by vm.error.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (purchases) "Lista de compras" else "Materiais", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 16.dp))
            TextButton(onClick = { purchases = !purchases }) { Text(if (purchases) "Estoque" else "Compras (${shopping.count { !it.checked }})") }
        }
        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.clickable { vm.clearError() }.padding(bottom = 8.dp))
        if (purchases) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { addItem = true }) { Text("＋ Item") }
                OutlinedButton(onClick = { purchases = false; creating = true }) { Text("＋ Material") }
            }
            LazyColumn(contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (shopping.isEmpty()) item { Text("A lista de compras está vazia.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(shopping, key = { it.id }) { item -> ShoppingRow(item, materials.firstOrNull { it.id == item.materialId }, vm) { materialId -> moving = materialId to "Entrada" } }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { creating = true }) { Text("＋ Material") }; Text("${materials.count { it.quantity < it.minimum }} abaixo do mínimo", modifier = Modifier.padding(top = 14.dp), color = MaterialTheme.colorScheme.error) }
            LazyColumn(contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (materials.isEmpty()) item { Text("Nenhum material cadastrado ainda.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(materials, key = { it.id }) { material ->
                    Card(Modifier.fillMaxWidth().clickable { selected = material.id }) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(material.name, style = MaterialTheme.typography.titleLarge); if (material.quantity < material.minimum) Text("Baixo", color = MaterialTheme.colorScheme.error) }
                        Text("${material.quantity.pretty()} ${material.unit} · mín. ${material.minimum.pretty()} · ideal ${material.ideal.pretty()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${material.category}${material.location.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty()}", style = MaterialTheme.typography.bodySmall)
                    } }
                }
            }
        }
    }
    if (creating || editing != null) MaterialEditorDialog(editing, onDismiss = { creating = false; editing = null }) { item -> vm.save(item) { creating = false; editing = null } }
    if (selected != null) MaterialDetailDialog(selected!!, vm, onDismiss = { selected = null }, onEdit = { id -> editing = materials.firstOrNull { it.id == id }; selected = null }, onMove = { id, type -> moving = id to type })
    if (moving != null) MovementDialog(moving!!.first, moving!!.second, materials, cultivations, onDismiss = { moving = null }) { movement -> vm.move(movement) { moving = null } }
    if (addItem) SimpleTextDialog("Novo item de compra", "Nome do item", onDismiss = { addItem = false }) { vm.addShopping(it); addItem = false }
}

@Composable
private fun MaterialEditorDialog(initial: Material?, onDismiss: () -> Unit, onSave: (Material) -> Unit) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var category by remember(initial?.id) { mutableStateOf(initial?.category ?: "Outro") }
    var unit by remember(initial?.id) { mutableStateOf(initial?.unit ?: "un") }
    var quantity by remember(initial?.id) { mutableStateOf(initial?.quantity?.toString() ?: "0") }
    var minimum by remember(initial?.id) { mutableStateOf(initial?.minimum?.toString() ?: "0") }
    var ideal by remember(initial?.id) { mutableStateOf(initial?.ideal?.toString() ?: "0") }
    var location by remember(initial?.id) { mutableStateOf(initial?.location.orEmpty()) }
    var notes by remember(initial?.id) { mutableStateOf(initial?.notes.orEmpty()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (initial == null) "Novo material" else "Editar material") },
        text = { Column(Modifier.heightIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Nome *") }, singleLine = true)
            MenuField("Categoria: $category", materialCategories) { category = it }
            OutlinedTextField(unit, { unit = it }, label = { Text("Unidade") }, singleLine = true)
            OutlinedTextField(quantity, { quantity = it }, label = { Text(if (initial == null) "Estoque inicial" else "Quantidade atual (use ajuste para alterar)") }, singleLine = true, enabled = initial == null)
            OutlinedTextField(minimum, { minimum = it }, label = { Text("Estoque mínimo") }, singleLine = true)
            OutlinedTextField(ideal, { ideal = it }, label = { Text("Estoque ideal") }, singleLine = true)
            OutlinedTextField(location, { location = it }, label = { Text("Local") }, singleLine = true)
            OutlinedTextField(notes, { notes = it }, label = { Text("Observações") })
        } }, confirmButton = { TextButton(onClick = {
            val qty = quantity.toDoubleOrNull() ?: 0.0; val min = minimum.toDoubleOrNull() ?: 0.0; val target = ideal.toDoubleOrNull() ?: 0.0
            onSave(Material(initial?.id ?: 0, name, category, qty, unit, min, target, location, notes, initial?.lastPurchaseAt, initial?.price))
        }) { Text("Salvar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
private fun MaterialDetailDialog(id: Long, vm: MaterialViewModel, onDismiss: () -> Unit, onEdit: (Long) -> Unit, onMove: (Long, String) -> Unit) {
    val material by vm.material(id).collectAsStateWithLifecycle(initialValue = null)
    val movements by vm.movements(id).collectAsStateWithLifecycle(initialValue = emptyList())
    AlertDialog(onDismissRequest = onDismiss, title = { Text(material?.name ?: "Material") }, text = {
        Column(Modifier.heightIn(max = 500.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (material != null) {
                Text("${material!!.quantity.pretty()} ${material!!.unit} · mínimo ${material!!.minimum.pretty()} · ideal ${material!!.ideal.pretty()}")
                Text("Histórico", style = MaterialTheme.typography.titleMedium)
            }
            if (movements.isEmpty()) Text("Nenhuma movimentação registrada.")
            movements.take(8).forEach { Text("${it.type} ${it.quantity.pretty()} · ${it.reason}") }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton(onClick = { onMove(id, "Entrada") }) { Text("＋ Entrada") }
                TextButton(onClick = { onMove(id, "Saída") }) { Text("− Saída") }
                TextButton(onClick = { onMove(id, "Ajuste") }) { Text("Ajuste") }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }, dismissButton = { TextButton(onClick = { onEdit(id) }) { Text("Editar") } })
}

@Composable
private fun MovementDialog(id: Long, type: String, materials: List<Material>, cultivations: List<Cultivation>, onDismiss: () -> Unit, onSave: (StockMovement) -> Unit) {
    val material = materials.firstOrNull { it.id == id } ?: return
    var quantity by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf(if (type == "Entrada") "Compra" else "Consumo") }
    var notes by remember { mutableStateOf("") }
    var cultivationId by remember { mutableStateOf<Long?>(null) }
    var showCultivations by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("$type de ${material.name}") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Disponível: ${material.quantity.pretty()} ${material.unit}")
        OutlinedTextField(quantity, { quantity = it }, label = { Text(if (type == "Ajuste") "Nova quantidade" else "Quantidade") }, singleLine = true)
        OutlinedTextField(reason, { reason = it }, label = { Text("Motivo *") }, singleLine = true)
        Box { OutlinedButton(onClick = { showCultivations = true }) { Text("Cultivo relacionado: ${cultivations.firstOrNull { it.id == cultivationId }?.name ?: "Ninguno"}") }
            DropdownMenu(showCultivations, { showCultivations = false }) {
                DropdownMenuItem(text = { Text("Ninguno") }, onClick = { cultivationId = null; showCultivations = false })
                cultivations.forEach { cultivation -> DropdownMenuItem(text = { Text("${cultivation.code} · ${cultivation.name}") }, onClick = { cultivationId = cultivation.id; showCultivations = false }) }
            }
        }
        OutlinedTextField(notes, { notes = it }, label = { Text("Observação") })
    } }, confirmButton = { TextButton(onClick = { quantity.toDoubleOrNull()?.let { onSave(StockMovement(materialId = id, type = type, quantity = it, reason = reason, cultivationId = cultivationId, notes = notes)) } }) { Text("Registrar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
private fun ShoppingRow(item: ShoppingItem, material: Material?, vm: MaterialViewModel, onPurchase: (Long) -> Unit) {
    var qtyDialog by remember { mutableStateOf(false) }
    var qty by remember { mutableStateOf("") }
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row { Checkbox(item.checked, { vm.check(item, it) }); Column(Modifier.padding(top = 12.dp)) { Text(item.name); if (material != null) Text("Atual ${material.quantity.pretty()} ${material.unit}", style = MaterialTheme.typography.bodySmall) } }
            TextButton(onClick = { vm.remove(item) }) { Text("Remover") }
        }
        if (material != null && !item.checked) TextButton(onClick = { qtyDialog = true }) { Text("Registrar compra e repor estoque") }
    } }
    if (qtyDialog && material != null) AlertDialog(onDismissRequest = { qtyDialog = false }, title = { Text("Reposição de ${material.name}") },
        text = { OutlinedTextField(qty, { qty = it }, label = { Text("Quantidade comprada (${material.unit})") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { qty.toDoubleOrNull()?.let { amount -> vm.move(StockMovement(materialId = material.id, type = "Entrada", quantity = amount, reason = "Compra")) { if (material.quantity + amount >= material.minimum) vm.check(item, true) }; qtyDialog = false } }) { Text("Registrar") } },
        dismissButton = { TextButton(onClick = { qtyDialog = false }) { Text("Cancelar") } })
}

@Composable
private fun SimpleTextDialog(title: String, label: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { OutlinedTextField(value, { value = it }, label = { Text(label) }, singleLine = true) }, confirmButton = { TextButton(onClick = { onSave(value) }) { Text("Adicionar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
private fun MenuField(label: String, options: List<String>, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box { OutlinedButton(onClick = { open = true }) { Text(label) }; DropdownMenu(open, { open = false }) { options.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { onSelect(it); open = false }) } } }
}

private fun Double.pretty(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()

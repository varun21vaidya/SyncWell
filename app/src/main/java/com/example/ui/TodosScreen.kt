package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Todo
import com.example.ui.theme.*
import com.example.viewmodel.SyncWellViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodosScreen(viewModel: SyncWellViewModel) {
    val shortTermGoals by viewModel.shortTermGoals.collectAsState()
    val longTermGoals by viewModel.longTermGoals.collectAsState()

    var shortTermInput by remember { mutableStateOf("") }
    var longTermInput by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf("MEDIUM") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
    ) {
            // Header
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("GOALS DASHBOARD", color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("My Goals", color = MaterialTheme.colorScheme.onBackground, fontSize = 26.sp, fontWeight = FontWeight.Black)
                }
            }

            // ── SHORT TERM GOALS ──
            item {
                GoalSectionHeader(
                    title = "Today's Goals",
                    subtitle = "Resets at midnight",
                    completedCount = shortTermGoals.count { it.isCompleted },
                    total = shortTermGoals.size,
                    input = shortTermInput,
                    onInputChange = { shortTermInput = it },
                    selectedPriority = selectedPriority,
                    onPriorityChange = { selectedPriority = it },
                    onAdd = {
                        if (shortTermInput.isNotBlank()) {
                            viewModel.addTodo(shortTermInput, selectedPriority, "SHORT_TERM")
                            shortTermInput = ""
                        }
                    }
                )
            }

            if (shortTermGoals.isEmpty()) {
                item {
                    EmptyGoalPlaceholder("No goals yet. Add one above.")
                }
            } else {
                items(shortTermGoals, key = { it.id }) { todo ->
                    GoalItemRow(
                        todo = todo,
                        onToggle = { viewModel.toggleTodo(todo) },
                        onDelete = { viewModel.deleteTodo(todo) },
                        onEdit = { newTitle -> viewModel.updateTodoTitle(todo, newTitle) }
                    )
                }
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    thickness = 1.dp
                )
            }

            // ── LONG TERM GOALS ──
            item {
                GoalSectionHeader(
                    title = "Weekly Goals",
                    subtitle = "Resets Sunday midnight",
                    completedCount = longTermGoals.count { it.isCompleted },
                    total = longTermGoals.size,
                    input = longTermInput,
                    onInputChange = { longTermInput = it },
                    selectedPriority = selectedPriority,
                    onPriorityChange = { selectedPriority = it },
                    onAdd = {
                        if (longTermInput.isNotBlank()) {
                            viewModel.addTodo(longTermInput, selectedPriority, "LONG_TERM")
                            longTermInput = ""
                        }
                    }
                )
            }

            if (longTermGoals.isEmpty()) {
                item { EmptyGoalPlaceholder("No weekly goals yet.") }
            } else {
                items(longTermGoals, key = { it.id }) { todo ->
                    GoalItemRow(
                        todo = todo,
                        onToggle = { viewModel.toggleTodo(todo) },
                        onDelete = { viewModel.deleteTodo(todo) },
                        onEdit = { newTitle -> viewModel.updateTodoTitle(todo, newTitle) }
                    )
                }
            }
        }
    }

@Composable
fun GoalSectionHeader(
    title: String, subtitle: String, completedCount: Int, total: Int,
    input: String, onInputChange: (String) -> Unit,
    selectedPriority: String, onPriorityChange: (String) -> Unit,
    onAdd: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(title, fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
                Text(subtitle, color = Slate400, fontSize = 11.sp)
            }
            // Completion badge
            if (total > 0) {
                Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(EmeraldDim).padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Text("$completedCount / $total", color = EmeraldGlow, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }

        // Input row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input, onValueChange = onInputChange,
                placeholder = { Text("Add a goal...", fontSize = 13.sp) },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGlow)
            )
            IconButton(onClick = onAdd, modifier = Modifier.size(50.dp).background(EmeraldGlow, RoundedCornerShape(12.dp))) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = OledBlack)
            }
        }

        // Priority chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("HIGH", "MEDIUM", "LOW").forEach { p ->
                val isSelected = selectedPriority == p
                val chipBg = when(p) { "HIGH" -> if (isSelected) RoseCrimson else SurfaceTier2; "MEDIUM" -> if (isSelected) AmberFlame else SurfaceTier2; else -> if (isSelected) EmeraldGlow else SurfaceTier2 }
                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(chipBg)
                    .clickable { onPriorityChange(p) }.defaultMinSize(minWidth = 44.dp, minHeight = 36.dp).padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center) {
                    Text(p, color = if (isSelected) OledBlack else TextSecondary, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun EmptyGoalPlaceholder(text: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
        Text(text, color = TextMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalItemRow(todo: Todo, onToggle: () -> Unit, onDelete: () -> Unit, onEdit: (String) -> Unit) {
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(todo.title) }
    val priorityColor = when (todo.priority.uppercase()) { "HIGH" -> RoseCrimson; "MEDIUM" -> AmberFlame; else -> EmeraldGlow }

    Card(modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (todo.isCompleted) SurfaceTier1.copy(alpha = 0.6f) else SurfaceTier1),
        shape = RoundedCornerShape(14.dp), border = CardDefaults.outlinedCardBorder()
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Priority strip
            Box(modifier = Modifier.width(4.dp).height(28.dp).clip(RoundedCornerShape(2.dp)).background(priorityColor))
            Spacer(Modifier.width(10.dp))
            // Checkbox
            Box(modifier = Modifier.size(36.dp).clickable { onToggle() }, contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null,
                    tint = if (todo.isCompleted) EmeraldGlow else TextMuted,
                    modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(4.dp))
            // Title or edit field
            if (isEditing) {
                OutlinedTextField(value = editText, onValueChange = { editText = it },
                    modifier = Modifier.weight(1f).height(44.dp), singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGlow))
                IconButton(onClick = { onEdit(editText); isEditing = false }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Check, contentDescription = "Save", tint = EmeraldGlow)
                }
            } else {
                Text(text = todo.title, modifier = Modifier.weight(1f),
                    color = if (todo.isCompleted) TextMuted else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None)
                // Edit pencil icon
                IconButton(onClick = { isEditing = true; editText = todo.title }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextMuted, modifier = Modifier.size(16.dp))
                }
                // Delete
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RoseCrimson, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

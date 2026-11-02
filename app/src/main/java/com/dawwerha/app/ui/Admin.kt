package com.dawwerha.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dawwerha.app.data.DawwerhaRepository
import com.dawwerha.app.data.ListingEntity
import com.dawwerha.app.data.Role
import com.dawwerha.app.data.UserEntity
import kotlinx.coroutines.launch

@Composable
fun AdminScreen(repo: DawwerhaRepository, user: UserEntity, onBack: () -> Unit) {
    val users by repo.observeUsers().collectAsState(initial = emptyList())
    val listings by repo.observeListings().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedButton(onClick = onBack) { Text("رجوع") }
        Text("لوحة الإدارة", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard("المستخدمون", users.size.toString(), Modifier.weight(1f))
            StatCard("الإعلانات", listings.size.toString(), Modifier.weight(1f))
            StatCard("إجمالي النقاط", users.sumOf { it.points }.toString(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))

        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("المستخدمون") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("الإعلانات") })
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp)) }

        if (tab == 0 && users.isEmpty() || tab == 1 && listings.isEmpty()) {
            EmptyState("لا توجد عناصر بعد")
        }

        LazyColumn(Modifier.weight(1f)) {
            if (tab == 0) {
                items(users, key = { it.id }) { u ->
                    UserRow(u, onDelete = {
                        scope.launch { message = repo.deleteUser(u) ?: "تم حذف ${u.fullName}" }
                    })
                }
            } else {
                items(listings, key = { it.id }) { l ->
                    ListingRow(l, onDelete = {
                        scope.launch {
                            repo.deleteListing(l)
                            message = "تم حذف الإعلان"
                        }
                    })
                }
            }
        }
    }
}

@Composable
private fun UserRow(u: UserEntity, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(u.fullName, fontWeight = FontWeight.Bold)
                Text("@${u.username} • ${u.points} نقطة", style = MaterialTheme.typography.bodySmall)
                if (u.role == Role.ADMIN) Text("أدمن", color = MaterialTheme.colorScheme.primary)
            }
            if (u.role != Role.ADMIN) {
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "حذف") }
            }
        }
    }
}

@Composable
private fun ListingRow(l: ListingEntity, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(l.title, fontWeight = FontWeight.Bold)
                Text("بواسطة ${l.ownerName} • ${l.city}", style = MaterialTheme.typography.bodySmall)
                Text("الحالة: ${statusLabel(l.status)}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "حذف") }
        }
    }
}

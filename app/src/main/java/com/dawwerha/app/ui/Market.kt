package com.dawwerha.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dawwerha.app.data.*
import com.dawwerha.app.domain.MaterialType
import kotlinx.coroutines.launch

private val sortOptions = listOf("الأحدث", "الأقل سعرًا", "الأكبر كمية")

@Composable
fun MarketScreen(repo: DawwerhaRepository, nav: NavController) {
    val listings by repo.observeListings().collectAsState(initial = emptyList())
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<MaterialType?>(null) }
    var sort by remember { mutableStateOf(sortOptions[0]) }

    val shown = listings
        .filter { it.status == ListingStatus.OPEN }
        .filter { filter == null || it.material == filter?.name }
        .filter { query.isBlank() || it.title.contains(query, ignoreCase = true) }
        .let {
            when (sort) {
                "الأقل سعرًا" -> it.sortedBy { l -> l.price }
                "الأكبر كمية" -> it.sortedByDescending { l -> l.quantity }
                else -> it
            }
        }

    Scaffold(floatingActionButton = {
        FloatingActionButton(onClick = { nav.navigate("market/create") }) {
            Icon(Icons.Default.Add, contentDescription = "إضافة إعلان")
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("سوق المواد", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                label = { Text("ابحث عن مادة") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            ChipRow<MaterialType?>(
                options = listOf(null) + MaterialType.entries.filter { it != MaterialType.MIXED },
                selected = filter,
                label = { it?.arName ?: "الكل" },
                onSelect = { filter = it }
            )
            ChipRow(sortOptions, sort, label = { it }, onSelect = { sort = it })

            if (shown.isEmpty()) EmptyState("لا توجد مواد مطابقة. كن أول من يضيف مادة!")
            LazyColumn(Modifier.weight(1f)) {
                items(shown, key = { it.id }) { l ->
                    Card(
                        onClick = { nav.navigate("listing/${l.id}") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            ImageThumb(l.imagePath, 64.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(l.title, fontWeight = FontWeight.Bold)
                                Text("${materialLabel(MaterialType.entries.firstOrNull { it.name == l.material })} • ${l.quantity} ${l.unit}")
                                Text(l.city, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(if (l.price == 0.0) "مجاني" else "${l.price} ج.م", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateListingScreen(repo: DawwerhaRepository, user: UserEntity, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var material by remember { mutableStateOf(MaterialType.PLASTIC) }
    var quantity by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("قطعة") }
    var condition by remember { mutableStateOf("جيد") }
    var description by remember { mutableStateOf("") }
    var city by remember { mutableStateOf(user.city) }
    var price by remember { mutableStateOf("0") }
    var pickup by remember { mutableStateOf("") }
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) bitmap = loadBitmap(context, uri)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("إضافة إعلان", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        OutlinedButton(onClick = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
            Text(if (bitmap == null) "أضف صورة (اختياري)" else "تم اختيار صورة ✓")
        }
        OutlinedTextField(title, { title = it }, label = { Text("اسم المادة") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        SectionTitle("نوع المادة")
        ChipRow(MaterialType.entries.filter { it != MaterialType.MIXED }, material, label = { it.arName }, onSelect = { material = it })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                quantity, { quantity = it }, label = { Text("الكمية") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f)
            )
            OutlinedTextField(unit, { unit = it }, label = { Text("الوحدة") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        SectionTitle("الحالة")
        ChipRow(listOf("جديد", "ممتاز", "جيد", "مقبول"), condition, label = { it }, onSelect = { condition = it })
        OutlinedTextField(description, { description = it }, label = { Text("الوصف") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(city, { city = it }, label = { Text("الموقع التقريبي") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            price, { price = it }, label = { Text("السعر (0 = مجاني)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(pickup, { pickup = it }, label = { Text("شروط الاستلام") }, modifier = Modifier.fillMaxWidth())

        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                scope.launch {
                    error = repo.createListing(
                        user,
                        ListingDraft(
                            title = title, material = material.name,
                            quantity = quantity.toDoubleOrNull() ?: 0.0, unit = unit,
                            condition = condition, description = description, city = city,
                            price = price.toDoubleOrNull() ?: -1.0, pickupTerms = pickup
                        ),
                        bitmap
                    )
                    if (error == null) onDone()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("نشر الإعلان") }
    }
}

@Composable
fun ListingDetailScreen(repo: DawwerhaRepository, user: UserEntity, listingId: Long, onBack: () -> Unit) {
    val listing by repo.observeListing(listingId).collectAsState(initial = null)
    val requests by repo.observeRequests(listingId).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        OutlinedButton(onClick = onBack) { Text("رجوع") }
        val l = listing
        if (l == null) {
            EmptyState("هذا الإعلان غير متاح")
            return@Column
        }
        ImageThumb(l.imagePath, 200.dp)
        Text(l.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("النوع: ${materialLabel(MaterialType.entries.firstOrNull { it.name == l.material })}")
        Text("الكمية: ${l.quantity} ${l.unit} • الحالة: ${l.condition}")
        Text("الموقع: ${l.city}")
        Text("السعر: ${if (l.price == 0.0) "مجاني" else "${l.price} ج.م"}")
        Text("الوصف: ${l.description}")
        Text("شروط الاستلام: ${l.pickupTerms}")
        Text("الحالة: ${statusLabel(l.status)}")
        Spacer(Modifier.height(12.dp))

        if (l.ownerId == user.id) {
            SectionTitle("طلبات الاستلام")
            if (requests.isEmpty()) EmptyState("لا توجد طلبات بعد")
            requests.forEach { r ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(r.requesterName, fontWeight = FontWeight.Bold)
                        Text(r.message)
                        Text("الحالة: ${statusLabel(r.status)}", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            when (r.status) {
                                RequestStatus.PENDING -> {
                                    Button(onClick = { scope.launch { repo.setRequestStatus(l, r, RequestStatus.ACCEPTED) } }) { Text("قبول") }
                                    OutlinedButton(onClick = { scope.launch { repo.setRequestStatus(l, r, RequestStatus.REJECTED) } }) { Text("رفض") }
                                }
                                RequestStatus.ACCEPTED -> Button(onClick = {
                                    scope.launch { repo.setRequestStatus(l, r, RequestStatus.DELIVERED) }
                                }) { Text("تم التسليم") }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { scope.launch { repo.deleteListing(l); onBack() } },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Delete, null)
                Text("حذف الإعلان")
            }
        } else {
            val mine = requests.firstOrNull { it.requesterId == user.id }
            when {
                mine != null -> Text("حالة طلبك: ${statusLabel(mine.status)}", fontWeight = FontWeight.Bold)
                l.status == ListingStatus.OPEN -> {
                    OutlinedTextField(
                        message, { message = it }, label = { Text("رسالة لصاحب الإعلان") },
                        modifier = Modifier.fillMaxWidth(), minLines = 2
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { scope.launch { error = repo.requestListing(l, user, message) } },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("أرسل طلب الحصول على المادة") }
                }
                else -> Text("هذا الإعلان مغلق")
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        }
    }
}

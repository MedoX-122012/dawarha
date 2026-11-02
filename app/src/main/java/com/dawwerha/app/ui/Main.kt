package com.dawwerha.app.ui

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dawwerha.app.Brand
import com.dawwerha.app.data.*
import com.dawwerha.app.domain.AnalysisResult
import com.dawwerha.app.domain.MaterialAnalyzer
import com.dawwerha.app.domain.MaterialType
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(repo: DawwerhaRepository, user: UserEntity, nav: NavController) {
    val scans by repo.observeScans(user.id).collectAsState(initial = emptyList())
    val listings by repo.observeListings().collectAsState(initial = emptyList())

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("أهلًا ${user.fullName}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("هذا ملخص تأثيرك البيئي حتى الآن", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard("مواد تم تحليلها", scans.size.toString(), Modifier.weight(1f))
            StatCard("النقاط", user.points.toString(), Modifier.weight(1f))
            StatCard("المستوى", levelOf(user.points).toString(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = { nav.navigate("scan") }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("افتح الماسح الذكي")
        }

        SectionTitle("أحدث أفكار إعادة الاستخدام")
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            reuseIdeas.take(4).forEach { idea ->
                Card(onClick = { nav.navigate("ideas") }, modifier = Modifier.width(180.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(idea.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("${idea.material.arName} • ${idea.minutes} دقيقة", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        SectionTitle("أحدث المواد في السوق")
        if (listings.isEmpty()) EmptyState("لا توجد مواد معروضة بعد")
        listings.take(3).forEach { l ->
            Card(onClick = { nav.navigate("listing/${l.id}") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(l.title, fontWeight = FontWeight.Bold)
                        Text("${l.quantity} ${l.unit} • ${l.city}", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(if (l.price == 0.0) "مجاني" else "${l.price} ج.م")
                }
            }
        }

        SectionTitle("اختصارات")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { nav.navigate("ideas") }, modifier = Modifier.weight(1f)) { Text("الأفكار") }
            OutlinedButton(onClick = { nav.navigate("market") }, modifier = Modifier.weight(1f)) { Text("السوق") }
            OutlinedButton(onClick = { nav.navigate("impact") }, modifier = Modifier.weight(1f)) { Text("الأثر") }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun ScanScreen(repo: DawwerhaRepository, user: UserEntity, nav: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var result by remember { mutableStateOf<AnalysisResult?>(null) }
    var analyzing by remember { mutableStateOf(false) }
    var weightText by remember { mutableStateOf("0.5") }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) bitmap = bmp
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) bitmap = loadBitmap(context, uri)
    }

    LaunchedEffect(bitmap) {
        result = null
        val b = bitmap ?: return@LaunchedEffect
        analyzing = true
        result = MaterialAnalyzer.analyze(b)
        analyzing = false
    }

    Scaffold(snackbarHost = { SnackbarHost(snack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("الماسح الذكي", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("صوّر أي شيء تريد التخلص منه، أو اختر صورة من الجهاز.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { cameraLauncher.launch(null) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("تصوير")
                }
                OutlinedButton(
                    onClick = {
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("من الجهاز") }
            }

            bitmap?.let {
                Spacer(Modifier.height(12.dp))
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp))
                )
            }

            if (analyzing) {
                Spacer(Modifier.height(12.dp))
                CircularProgressIndicator()
                Text("جارٍ تحليل الصورة على جهازك...")
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = weightText,
                onValueChange = { weightText = it },
                label = { Text("الوزن التقريبي (كجم)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            result?.let { r ->
                Spacer(Modifier.height(12.dp))
                ResultCard(
                    r = r,
                    weight = weightText.toDoubleOrNull() ?: 0.0,
                    onSave = {
                        val b = bitmap
                        val w = weightText.toDoubleOrNull()
                        if (b == null || w == null || w <= 0) {
                            scope.launch { snack.showSnackbar("أدخل وزنًا صحيحًا أكبر من صفر") }
                        } else {
                            scope.launch {
                                repo.saveScan(user, r, b, w)
                                snack.showSnackbar("تم حفظ المادة في سجلك وكسبت $POINTS_PER_SCAN نقاط")
                            }
                        }
                    },
                    onIdeas = { nav.navigate("ideas") },
                    onMarket = { nav.navigate("market/create") }
                )
                if (r.material == null) {
                    Text(
                        "لم يتمكن النموذج من تحديد المادة بثقة كافية، لذلك لن نعرض نتيجة مضللة. جرّب صورة أوضح بإضاءة جيدة وخلفية بسيطة.",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultCard(r: AnalysisResult, weight: Double, onSave: () -> Unit, onIdeas: () -> Unit, onMarket: () -> Unit) {
    val m = r.material ?: return
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text("الاسم المتوقع: ${r.itemName}", fontWeight = FontWeight.Bold)
            Text("نوع المادة: ${m.arName}")
            Text("درجة الثقة: ${(r.confidence * 100).toInt()}%")
            if (r.confidence < 0.6f) {
                Text("الثقة منخفضة، تأكد من النتيجة قبل الاعتماد عليها.", color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(8.dp))
            Text("قابل لإعادة الاستخدام: ${if (m.reusable) "نعم" else "لا"}")
            Text("قابل للإصلاح: ${if (m.repairable) "نعم" else "لا"}")
            Text("قابل لإعادة التدوير: ${if (m.recyclable) "نعم" else "لا"}")
            Spacer(Modifier.height(8.dp))
            Text("طريقة التعامل: ${m.tip}")
            m.warning?.let { Text("تحذير: $it", color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(8.dp))
            Text("الأثر التقديري: %.2f كجم مكافئ CO2 (تقدير مبدئي)".format(weight * m.co2PerKg))
            Spacer(Modifier.height(12.dp))
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text("احفظ في سجل موادي") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onIdeas, modifier = Modifier.weight(1f)) { Text("أفكار لإعادة الاستخدام") }
                OutlinedButton(onClick = onMarket, modifier = Modifier.weight(1f)) { Text("أضفها للسوق") }
            }
        }
    }
}

@Composable
fun ImpactScreen(repo: DawwerhaRepository, user: UserEntity) {
    val scans by repo.observeScans(user.id).collectAsState(initial = emptyList())
    val totalKg = scans.sumOf { it.weightKg }
    val totalCo2 = scans.sumOf { s -> s.weightKg * (MaterialType.entries.firstOrNull { it.name == s.material }?.co2PerKg ?: 0.0) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("التأثير البيئي", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "القيم الفعلية هي الأوزان التي أدخلتها. قيم ثاني أكسيد الكربون تقديرية مبنية على معاملات مبدئية.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard("مواد محللة", scans.size.toString(), Modifier.weight(1f))
            StatCard("الوزن المحوّل (كجم)", "%.1f".format(totalKg), Modifier.weight(1f))
            StatCard("CO2 متجنب (تقديري)", "%.1f".format(totalCo2), Modifier.weight(1f))
        }

        SectionTitle("التأثير حسب نوع المادة")
        if (scans.isEmpty()) {
            EmptyState("ابدأ بتصوير أول مادة لتظهر إحصائياتك هنا")
        } else {
            MaterialType.entries.filter { it != MaterialType.MIXED }.forEach { m ->
                val kg = scans.filter { it.material == m.name }.sumOf { it.weightKg }
                if (kg > 0 && totalKg > 0) {
                    Text("${m.arName}: %.1f كجم".format(kg))
                    LinearProgressIndicator(
                        progress = { (kg / totalKg).toFloat() },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(repo: DawwerhaRepository, user: UserEntity, nav: NavController) {
    val scope = rememberCoroutineScope()
    var fullName by remember(user.id) { mutableStateOf(user.fullName) }
    var city by remember(user.id) { mutableStateOf(user.city) }
    var bio by remember(user.id) { mutableStateOf(user.bio) }
    var message by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(Brand.NaturalGreen),
            contentAlignment = Alignment.Center
        ) {
            Text(user.fullName.firstOrNull()?.toString() ?: "?", color = Color.White, style = MaterialTheme.typography.headlineMedium)
        }
        Text(user.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("@${user.username}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("المستوى ${levelOf(user.points)} • ${user.points} نقطة")
        LinearProgressIndicator(
            progress = { (user.points % 100) / 100f },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )

        OutlinedTextField(fullName, { fullName = it }, label = { Text("الاسم") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(city, { city = it }, label = { Text("المدينة") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(bio, { bio = it }, label = { Text("نبذة تعريفية") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                scope.launch { message = repo.updateProfile(user, fullName, city, bio) ?: "تم حفظ التعديلات" }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("حفظ التعديلات") }

        if (user.role == Role.ADMIN) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { nav.navigate("admin") }, modifier = Modifier.fillMaxWidth()) { Text("لوحة الإدارة") }
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { scope.launch { repo.logout() } },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) { Text("تسجيل الخروج") }
    }
}

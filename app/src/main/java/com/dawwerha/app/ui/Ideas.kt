package com.dawwerha.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dawwerha.app.data.DawwerhaRepository
import com.dawwerha.app.data.UserEntity
import com.dawwerha.app.domain.MaterialType
import kotlinx.coroutines.launch

data class ReuseIdea(
    val id: String,
    val title: String,
    val material: MaterialType,
    val minutes: Int,
    val difficulty: String,
    val cost: String,
    val tools: List<String>,
    val steps: List<String>,
    val benefit: String
)

val reuseIdeas = listOf(
    ReuseIdea(
        "plastic-planter", "أصص نباتات من زجاجات بلاستيك", MaterialType.PLASTIC, 30, "سهل", "منخفضة جدًا",
        listOf("مقص", "مسمار ساخن أو سكين", "تربة ونبات"),
        listOf("اشطف الزجاجة جيدًا وأزل الملصق.", "اقطع الجزء العلوي على ارتفاع مناسب.", "اثقب القاع لتصريف الماء.", "املأها بالتربة وازرع النبات."),
        "تقليل بلاستيك أحادي الاستخدام وتوفير أصص للمنزل أو المدرسة."
    ),
    ReuseIdea(
        "cardboard-organizer", "منظمات مكتب من علب الكرتون", MaterialType.CARDBOARD, 45, "سهل", "منخفضة جدًا",
        listOf("كتر", "شريط لاصق", "مسطرة"),
        listOf("اختر علبة نظيفة وجافة.", "اقطع الأجزاء العلوية لتكوين حاويات.", "ثبّت الأركان بالشريط اللاصق.", "زيّنها بورق ملون أو اتركها كما هي."),
        "تنظيم المكتب بدون شراء أدوات جديدة."
    ),
    ReuseIdea(
        "textile-bag", "حقيبة من ملابس قديمة", MaterialType.TEXTILE, 120, "متوسط", "منخفضة",
        listOf("ماكينة خياطة أو إبرة وخيط", "مقص قماش", "مشبك أو زر"),
        listOf("اختر قطعة قماش قوية ونظيفة.", "قُصّها بالمقاسات المطلوبة.", "خِط الأطراف وأضف الحزام.", "ركّب الإغلاق."),
        "تحويل ملابس غير مستخدمة إلى منتج عملي وتقليل النفايات النسيجية."
    ),
    ReuseIdea(
        "wood-stool", "كرسي صغير من خشب قديم", MaterialType.WOOD, 180, "صعب", "متوسطة",
        listOf("منشار", "مطرقة", "براغي", "ورق صنفرة"),
        listOf("فكّ القطع القديمة وأزل المسامير.", "صنفر الأسطح جيدًا.", "قطّع الأجزاء حسب التصميم.", "ثبّت القطع وأضف طبقة حماية آمنة."),
        "إطالة عمر الخشب وتجنب شراء أثاث جديد."
    ),
    ReuseIdea(
        "glass-storage", "أدوات تخزين من عبوات زجاجية", MaterialType.GLASS, 40, "سهل", "منخفضة جدًا",
        listOf("ماء ساخن وصابون", "ملصقات أو شريط"),
        listOf("اغسل العبوة جيدًا وأزل الملصق.", "جفّفها تمامًا.", "استخدمها لتخزين البقوليات أو الأدوات الصغيرة."),
        "تخزين منظم وآمن بدون تكلفة تقريبًا."
    ),
    ReuseIdea(
        "metal-pen-holder", "حامل أقلام من علب معدنية", MaterialType.METAL, 30, "سهل", "منخفضة جدًا",
        listOf("علبة معدنية نظيفة", "ورق صنفرة", "ورق لاصق"),
        listOf("اغسل العلبة وجففها.", "صنفر الحواف الحادة بعناية.", "زيّنها بورق لاصق ملون."),
        "حامل مكتبي بسيط مع الحذر من الحواف الحادة."
    ),
    ReuseIdea(
        "paper-notebook", "دفتر ملاحظات من ورق مستعمل", MaterialType.PAPER, 25, "سهل", "منخفضة جدًا",
        listOf("أوراق مطبوعة من جهة واحدة", "دبوس أو خيط"),
        listOf("اجمع الأوراق المطبوعة من جهة واحدة.", "اقطعها بنفس المقاس.", "اثقب الحافة واربطها بالخيط."),
        "تقليل استهلاك الورق الجديد في الملاحظات اليومية."
    ),
    ReuseIdea(
        "compost-bin", "سماد منزلي من بقايا الطعام", MaterialType.ORGANIC, 20, "سهل", "منخفضة",
        listOf("وعاء بغطاء", "بقايا خضار وفاكهة", "تربة أو أوراق جافة"),
        listOf("اجمع بقايا الخضار والفاكهة غير المطبوخة.", "ضع طبقات منها مع أوراق جافة.", "قلّبها أسبوعيًا وحافظ عليها رطبة."),
        "تحويل المخلفات العضوية إلى سماد للنباتات."
    )
)

@Composable
fun IdeasScreen(repo: DawwerhaRepository, user: UserEntity, onBack: () -> Unit) {
    val saved by repo.observeSavedIdeas(user.id).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var filter by remember { mutableStateOf<MaterialType?>(null) }
    var openId by remember { mutableStateOf<String?>(null) }

    val shown = reuseIdeas.filter { filter == null || it.material == filter }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedButton(onClick = onBack) { Text("رجوع") }
        Text("أفكار إعادة الاستخدام", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        ChipRow<MaterialType?>(
            options = listOf(null) + MaterialType.entries.filter { it != MaterialType.MIXED },
            selected = filter,
            label = { it?.arName ?: "الكل" },
            onSelect = { filter = it }
        )
        if (shown.isEmpty()) EmptyState("لا توجد أفكار لهذا النوع بعد")

        LazyColumn(Modifier.weight(1f)) {
            items(shown, key = { it.id }) { idea ->
                val isSaved = idea.id in saved
                val expanded = openId == idea.id
                Card(
                    onClick = { openId = if (expanded) null else idea.id },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(idea.title, fontWeight = FontWeight.Bold)
                                Text(
                                    "${idea.material.arName} • ${idea.minutes} دقيقة • ${idea.difficulty}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(onClick = { scope.launch { repo.toggleSavedIdea(user.id, idea.id, !isSaved) } }) {
                                Icon(
                                    if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = if (isSaved) "إزالة من المحفوظات" else "حفظ"
                                )
                            }
                        }
                        if (expanded) {
                            Spacer(Modifier.height(8.dp))
                            Text("التكلفة التقديرية: ${idea.cost}")
                            Text("الأدوات: ${idea.tools.joinToString("، ")}")
                            Spacer(Modifier.height(4.dp))
                            Text("خطوات التنفيذ:", fontWeight = FontWeight.Bold)
                            idea.steps.forEachIndexed { i, s -> Text("${i + 1}. $s") }
                            Spacer(Modifier.height(4.dp))
                            Text("الفائدة: ${idea.benefit}", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

package com.dawwerha.app.domain

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.tasks.await

enum class MaterialType(
    val arName: String,
    val keywords: List<String>,
    val reusable: Boolean,
    val repairable: Boolean,
    val recyclable: Boolean,
    val co2PerKg: Double,      // تقديري: استبدله بمعامل موثق قبل النشر
    val tip: String,
    val warning: String?
) {
    PLASTIC("بلاستيك", listOf("plastic", "polyethylene", "container", "bottle"), true, false, true, 1.5,
        "افصل الغطاء والملصق، واشطفه جيدًا، ثم ضعه في حاوية البلاستيك.", null),
    PAPER("ورق", listOf("paper", "newspaper", "document"), true, false, true, 1.0,
        "حافظ عليه جافًا ونظيفًا، وأزل الدبابيس والشرائط.",
        "الورق المبلل أو الملوث بالزيت لا يُعاد تدويره."),
    CARDBOARD("كرتون", listOf("cardboard", "carton", "box"), true, true, true, 0.9,
        "افرده، وأزل الشريط اللاصق، وحافظ عليه جافًا.",
        "الكرتون المبلل يفقد قيمته في إعادة التدوير."),
    GLASS("زجاج", listOf("glass", "jar"), true, false, true, 0.3,
        "لفّه بعناية قبل التخلص منه، وضعه في حاوية الزجاج.",
        "الزجاج المكسور حاد، تعامل معه بحذر."),
    METAL("معادن", listOf("metal", "aluminum", "aluminium", "steel", "can", "tin"), true, true, true, 2.0,
        "اشطف العلب وافرغها، وفكّ الأجزاء البلاستيكية إن أمكن.",
        "الحواف المعدنية الحادة قد تسبب جروحًا."),
    TEXTILE("أقمشة", listOf("textile", "fabric", "clothing", "cloth", "denim", "garment", "shirt"), true, true, false, 3.0,
        "اغسلها وقيّم حالتها، فالملابس السليمة مناسبة للتبرع أو الإصلاح.",
        "الأقمشة الملوثة لا تُقبل في الإعادة."),
    WOOD("أخشاب", listOf("wood", "timber", "plank", "furniture", "wooden"), true, true, true, 0.5,
        "أزل المسامير والدهانات، وفكّ القطع الكبيرة.",
        "تجنب حرق الأخشاب المدهونة أو المعالجة كيميائيًا."),
    ELECTRONICS("أجهزة إلكترونية", listOf("electronics", "mobile phone", "computer", "circuit", "cable", "keyboard", "battery"), true, true, true, 5.0,
        "لا تضعها مع المخلفات المنزلية، وسلّمها لنقطة تجميع متخصصة.",
        "البطاريات والشواحن قد تحتوي على مواد خطرة."),
    ORGANIC("مخلفات عضوية", listOf("food", "fruit", "vegetable", "leaf", "plant", "flower"), false, false, true, 0.4,
        "استخدمها في تسميد المنزل (كومبوست) إن أمكن.", null),
    MIXED("مواد مختلطة", emptyList(), false, false, false, 0.0,
        "افصل المكونات قبل التخلص منها إن أمكن.", null)
}

data class AnalysisResult(
    val material: MaterialType?,     // null = لم يتم التحديد بثقة
    val itemName: String,
    val confidence: Float,
    val labels: List<String>
)

object MaterialAnalyzer {
    private val labeler by lazy {
        ImageLabeling.getClient(ImageLabelerOptions.Builder().setConfidenceThreshold(0.5f).build())
    }

    suspend fun analyze(bitmap: Bitmap): AnalysisResult {
        val labels = labeler.process(InputImage.fromBitmap(bitmap, 0)).await()
            .map { it.text.lowercase() to it.confidence }

        if (labels.isEmpty()) return AnalysisResult(null, "غير محدد", 0f, emptyList())

        val scores = MaterialType.entries
            .filter { it != MaterialType.MIXED }
            .associateWith { m ->
                labels.sumOf { (text, conf) ->
                    if (m.keywords.any { k -> text == k || text.split(" ").contains(k) }) conf.toDouble() else 0.0
                }
            }

        val total = scores.values.sum()
        val best = scores.maxByOrNull { it.value } ?: return AnalysisResult(null, "غير محدد", 0f, emptyList())
        val topLabel = labels.first().first.replaceFirstChar { it.uppercase() }
        val allLabels = labels.take(5).map { it.first }

        if (best.value == 0.0 || total == 0.0) {
            return AnalysisResult(null, topLabel, 0f, allLabels)
        }

        val share = (best.value / total).toFloat()
        return if (share >= 0.5f) {
            AnalysisResult(best.key, topLabel, share, allLabels)
        } else {
            AnalysisResult(MaterialType.MIXED, topLabel, share, allLabels)
        }
    }
}

package com.dawwerha.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dawwerha.app.domain.MaterialType
import java.io.File

@Composable
fun SectionTitle(text: String) {
    Text(
        text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun EmptyState(text: String) {
    Text(
        text, style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
    )
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun <T> ChipRow(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
        items(options) { opt ->
            FilterChip(
                selected = opt == selected,
                onClick = { onSelect(opt) },
                label = { Text(label(opt)) }
            )
        }
    }
}

@Composable
fun ImageThumb(path: String?, size: Dp) {
    if (path == null) {
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Image, contentDescription = null)
        }
    } else {
        AsyncImage(
            model = File(path),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size)
        )
    }
}

fun loadBitmap(context: Context, uri: Uri): Bitmap? =
    context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = 2 })
    }

fun levelOf(points: Int): Int = points / 100 + 1

fun materialLabel(m: MaterialType?): String = m?.arName ?: "غير محدد"

fun statusLabel(status: String): String = when (status) {
    "PENDING" -> "قيد الانتظار"
    "ACCEPTED" -> "مقبول"
    "REJECTED" -> "مرفوض"
    "DELIVERED" -> "تم التسليم"
    "OPEN" -> "متاح"
    "CLOSED" -> "مغلق"
    else -> status
}

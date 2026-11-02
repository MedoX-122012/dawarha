package com.dawwerha.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.dawwerha.app.Brand
import com.dawwerha.app.data.DawwerhaRepository
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(repo: DawwerhaRepository, onRegister: () -> Unit) {
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    AuthLayout("أهلًا بك في دَوَّرها", "حوّل ما لا تحتاجه إلى شيء يحتاجه العالم") {
        OutlinedTextField(
            username, { username = it }, label = { Text("اسم المستخدم") },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            password, { password = it }, label = { Text("كلمة المرور") },
            singleLine = true, visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                scope.launch {
                    busy = true
                    error = repo.login(username, password)
                    busy = false
                }
            },
            enabled = !busy && username.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (busy) "جارٍ التحقق..." else "تسجيل الدخول") }
        TextButton(onClick = onRegister) { Text("ليس لديك حساب؟ أنشئ حسابًا جديدًا") }
    }
}

@Composable
fun RegisterScreen(repo: DawwerhaRepository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    AuthLayout("إنشاء حساب جديد", "انضم لمجتمع المدوّرين") {
        OutlinedTextField(fullName, { fullName = it }, label = { Text("الاسم") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(username, { username = it }, label = { Text("اسم المستخدم (إنجليزي)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(city, { city = it }, label = { Text("المدينة") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            password, { password = it }, label = { Text("كلمة المرور (6 أحرف على الأقل)") },
            singleLine = true, visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth()
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                scope.launch {
                    busy = true
                    error = repo.register(fullName, username, password, city)
                    busy = false
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        ) { Text("إنشاء الحساب") }
        TextButton(onClick = onBack) { Text("لديّ حساب بالفعل") }
    }
}

@Composable
private fun AuthLayout(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("DAWWERHA", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Brand.DarkGreen)
        Text("دَوَّرها", style = MaterialTheme.typography.titleMedium, color = Brand.NaturalGreen)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Column(Modifier.fillMaxWidth(), content = content)
    }
}

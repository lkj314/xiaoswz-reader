package com.xiaoswz.reader.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.xiaoswz.reader.data.AppContext
import com.xiaoswz.reader.data.auth.AuthRepository
import com.xiaoswz.reader.data.auth.AuthResult
import com.xiaoswz.reader.data.settings.AppSettingsRepository
import com.xiaoswz.reader.ui.components.AppTopBar
import com.xiaoswz.reader.ui.components.WhaleGlassCard
import com.xiaoswz.reader.ui.components.MetaButton
import com.xiaoswz.reader.ui.components.MetaButtonVariant
import com.xiaoswz.reader.ui.theme.GlassTokens
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(AccountMode.LOGIN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        // 预填已登录邮箱，方便「切换账号」时识别
        email = AppSettingsRepository(AppContext.app).accountEmailFlow.first().orEmpty()
    }

    Scaffold(
        topBar = { AppTopBar(title = if (mode == AccountMode.LOGIN) "登录" else "注册账号", onBack = onBack, showLogo = false) },
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            WhaleGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = mode == AccountMode.LOGIN,
                            onClick = { mode = AccountMode.LOGIN; error = null },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        ) { Text("登录") }
                        SegmentedButton(
                            selected = mode == AccountMode.REGISTER,
                            onClick = { mode = AccountMode.REGISTER; error = null },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        ) { Text("注册") }
                    }

                    Spacer(Modifier.height(14.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.trim(); error = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("邮箱") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; error = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("密码（至少 8 位）") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                    )

                    error?.let {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    MetaButton(
                        text = if (loading) "处理中…" else if (mode == AccountMode.LOGIN) "登录" else "注册并登录",
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                error = "请输入邮箱和密码"
                                return@MetaButton
                            }
                            loading = true
                            error = null
                            scope.launch {
                                val res = if (mode == AccountMode.LOGIN) {
                                    AuthRepository.login(email, password)
                                } else {
                                    val deviceId = AppSettingsRepository(AppContext.app).getDeviceId()
                                    AuthRepository.register(email, password, deviceId)
                                }
                                loading = false
                                when (res) {
                                    is AuthResult.Ok -> onBack()
                                    is AuthResult.Error -> error = res.message
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading,
                    )

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (mode == AccountMode.REGISTER) {
                            "注册将把本机设备账号升级为正式账号，书架与阅读进度自动保留；注册后即可评论与跨设备云同步。"
                        } else {
                            "登录后解锁评论与云同步。忘记密码可在本机重新注册（同一设备自动绑定）。"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = GlassTokens.SecondaryLabel,
                    )
                }
            }

            if (mode == AccountMode.LOGIN) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    MetaButton(
                        text = "没有账号？去注册",
                        onClick = { mode = AccountMode.REGISTER; error = null },
                        modifier = Modifier.weight(1f),
                        variant = MetaButtonVariant.Ghost,
                    )
                }
            }
        }
    }
}

private enum class AccountMode { LOGIN, REGISTER }

package com.vowser.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.vowser.client.AppViewModel
import com.vowser.client.model.AuthState
import com.vowser.client.ui.components.GenericAppBar
import com.vowser.client.ui.navigation.LocalScreenNavigator
import com.vowser.client.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScreen(
    viewModel: AppViewModel
) {
    val navigator = LocalScreenNavigator.current
    val authState by viewModel.authState.collectAsState()
    val userInfo by viewModel.userInfo.collectAsState()
    val userInfoLoading by viewModel.userInfoLoading.collectAsState()

    var showQuickVerify by remember { mutableStateOf(false) }

    // 화면 진입 시 유저 정보 새로고침
     LaunchedEffect(Unit) {
         viewModel.refreshUserInfo()
     }

    Scaffold(
        topBar = {
            GenericAppBar(title = "User")
        }
    ) { inner ->
        BoxWithConstraints(
            Modifier.fillMaxSize()
        ) {
            val maxWidth = this.maxWidth
            val hPad = maxWidth * 0.05f
            val vPad = maxHeight * 0.03f
            val colGap = maxWidth * 0.06f

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(horizontal = hPad, vertical = vPad),
                verticalArrangement = Arrangement.spacedBy(maxHeight * 0.03f)
            ) {
                when {
                    // 로딩 중
                    userInfoLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    // 비로그인 시
                    authState !is AuthState.Authenticated -> {
                        Text("로그인이 필요합니다.", color = MaterialTheme.colorScheme.onBackground)
                    }

                    // 유저 정보 표시
                    userInfo != null -> {
                        Column(
                            Modifier
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(colGap)) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.paddingMedium)) {
                                    InfoItem("이름", userInfo!!.name)
                                    InfoItem("가입 날짜", formatKoreanDate(userInfo!!.createdAt))
                                    InfoItem("휴대폰 번호", userInfo!!.phoneNumber ?: "-")
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.paddingMedium)) {
                                    InfoItem("네이버 아이디", userInfo!!.email)
                                    InfoItem("이메일", userInfo!!.email)
                                    InfoItem("생년월일", userInfo!!.birthdate ?: "-")
                                }
                            }
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(maxWidth * 0.02f)
                            ) {
                                OutlinedButton(
                                    onClick = { showQuickVerify = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(AppTheme.Dimensions.buttonHeight),
                                    border = ButtonDefaults.outlinedButtonBorder
                                ) {
                                    Text(
                                        "+  간편 인증 추가",
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                Button(
                                    onClick = {
                                        viewModel.logout()
                                        navigator.replaceAll(AppScreen.HOME)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(AppTheme.Dimensions.buttonHeight),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = Color.White
                                    )
                                ) { Text("로그아웃") }
                            }
                        }
                    }

                    // 정보 없음
                    else -> Text("유저 정보를 불러올 수 없습니다.", color = MaterialTheme.colorScheme.onBackground)
                }
            }

            QuickVerifyDialog(
                visible = showQuickVerify,
                onDismiss = { showQuickVerify = false },
                onSubmit = { method, name, birthdate, phoneNumber ->
                    // TODO: 간편 인증 로직 구현
                    showQuickVerify = false
                }
            )
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(AppTheme.Dimensions.paddingSmall))
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
    }
}

private fun formatKoreanDate(iso: String?): String =
    iso?.take(10)?.split("-")?.let { "${it[0]}년 ${it[1].toInt()}월 ${it[2].toInt()}일" } ?: "-"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickVerifyDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (method: String, name: String, birthdate: String, phoneNumber: String) -> Unit
) {
    if (!visible) return

    var method by remember { mutableStateOf("인증 방식 선택") }
    var name by remember { mutableStateOf("") }
    var birthdate by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        BoxWithConstraints {
            val padX = maxWidth * 0.06f
            val padY = maxHeight * 0.04f

            Card(
                modifier = Modifier.fillMaxWidth(0.9f),
                shape = RoundedCornerShape(AppTheme.Dimensions.borderRadiusLarge),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    Modifier.fillMaxWidth()
                        .padding(horizontal = padX, vertical = padY),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.paddingMedium)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "간편 인증 추가",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(AppTheme.Dimensions.buttonHeight)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "close",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    // 드롭다운
                    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.paddingSmall)) {
                        Text(
                            "인증 방식 선택",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        BoxWithConstraints {
                            val maxWidth = this.maxWidth

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(AppTheme.Dimensions.borderRadius))
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                        RoundedCornerShape(AppTheme.Dimensions.borderRadius)
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { expanded = true }
                                    .padding(horizontal = AppTheme.Dimensions.paddingMedium, vertical = 10.dp)
                            ) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        method,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        Icons.Default.ArrowDropDown, contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier
                                    .width(maxWidth)
                                    .heightIn(max = 320.dp)
                                    .clip(RoundedCornerShape(AppTheme.Dimensions.borderRadius))
                                    .background(MaterialTheme.colorScheme.background),
                            ) {
                                val items = listOf("네이버", "카카오", "토스")

                                items.forEach { label ->
                                    val interaction = remember { MutableInteractionSource() }
                                    val hovered by interaction.collectIsHoveredAsState()

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .hoverable(interaction)
                                            .clip(RoundedCornerShape(AppTheme.Dimensions.borderRadiusSmall))
                                            .background(
                                                if (hovered) MaterialTheme.colorScheme.surface
                                                else Color.Transparent
                                            )
                                            .clickable {
                                                method = label
                                                expanded = false
                                            }
                                            .padding(horizontal = AppTheme.Dimensions.paddingMedium, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = label,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 입력
                    LabeledInput(label = "이름", placeholder = "이름을 입력하세요") { name = it }
                    LabeledInput(label = "생년월일", placeholder = "mm/dd/yyyy") { birthdate = it }
                    LabeledInput(label = "핸드폰 번호", placeholder = "010-1234-5678") { phoneNumber = it }

                    Button(
                        onClick = { onSubmit(method, name, birthdate, phoneNumber) },
                        modifier = Modifier.fillMaxWidth().height(AppTheme.Dimensions.buttonHeight),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onBackground,
                            contentColor = MaterialTheme.colorScheme.background
                        )
                    ) { Text("추가하기") }
                }
            }
        }
    }
}

@Composable
private fun LabeledInput(
    label: String,
    placeholder: String,
    onValueChangeHook: (String) -> Unit
) {
    var value by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.paddingSmall)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(AppTheme.Dimensions.borderRadius))
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(AppTheme.Dimensions.borderRadius))
                .padding(horizontal = AppTheme.Dimensions.paddingMedium, vertical = 10.dp)
        ) {
            if (value.isEmpty()) {
                Text(placeholder, color = MaterialTheme.colorScheme.onSurface)
            }
            BasicTextField(
                value = value,
                onValueChange = { value = it; onValueChangeHook(it) },
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

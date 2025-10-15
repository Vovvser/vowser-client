package com.vowser.client.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import com.vowser.client.ui.navigation.LocalScreenNavigator
import com.vowser.client.ui.theme.AppTheme
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun HomeAppBar(
    isLoggedIn: Boolean,
    onContribution: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenUser: () -> Unit,
    onLogin: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                "Vowser", fontSize = AppTheme.Typography.titleLarge,
                modifier = Modifier.padding(start = AppTheme.Dimensions.paddingSmall)
            )
        },
        actions = {
            Row(
                modifier = Modifier.padding(end = AppTheme.Dimensions.paddingSmall)
            ) {
                // 기여모드 버튼
//                if (isLoggedIn) {
                OutlinedButton(
                    onClick = onContribution,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    contentPadding = PaddingValues(AppTheme.Dimensions.paddingMedium, 0.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(AppTheme.Dimensions.borderRadiusSmall)
                ) {
                    Icon(
                        painter = painterResource("drawable/write.png"),
                        contentDescription = "Contribution Mode",
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(AppTheme.Dimensions.paddingMedium))
                    Text(
                        text = "기여 모드",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.None
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    )
                }
                Spacer(modifier = Modifier.width(AppTheme.Dimensions.paddingSmall))
//                }

                // 유저 프로필 버튼
                IconButton(onClick = {
                    if (isLoggedIn) {
                        onOpenUser()
                    } else {
                        onLogin()
                    }
                }) {
                    Icon(Icons.Filled.AccountCircle, contentDescription = "User Profile")
                }

                // 설정 버튼
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground
        ),
        modifier = Modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericAppBar(
    title: String,
    modifier: Modifier = Modifier,
    onBackPress: (() -> Unit)? = null
) {
    val navigator = LocalScreenNavigator.current
    val handleBack = onBackPress ?: { navigator.pop() }
    TopAppBar(
        title = { Text(title, fontSize = AppTheme.Typography.titleLarge) },
        navigationIcon = {
            IconButton(onClick = handleBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground
        ),
        modifier = modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline
        )
    )
}

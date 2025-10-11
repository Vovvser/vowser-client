package com.vowser.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vowser.client.AppViewModel
import com.vowser.client.model.AuthState
import com.vowser.client.ui.components.GenericAppBar
import com.vowser.client.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScreen(
    viewModel: AppViewModel,
    onBackPress: () -> Unit,
    onLogout: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()

    Scaffold(
        topBar = {
            GenericAppBar(title = "User Profile", onBackPress = onBackPress)
        }
    ) { paddingValues ->
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(AppTheme.Dimensions.paddingMedium),
                verticalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.spacingMedium)
            ) {
                if (authState is AuthState.Authenticated) {
                    Text("유저 프로필", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("성함 : ${(authState as AuthState.Authenticated).name}")
                    Text("이메일 : ${(authState as AuthState.Authenticated).email}")
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = onLogout,
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.Colors.Error)
                    ) {
                        Text("로그아웃")
                    }
                } else {
                    Text("로그인이 되지 않았습니다.")
                }

            }
        }
    }
}
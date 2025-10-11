package com.vowser.client.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * 표준화된 에러/확인 다이얼로그 컴포넌트
 */
@Composable
fun ErrorDialog(
    visible: Boolean,
    title: String,
    message: String,
    type: ErrorDialogType = ErrorDialogType.ERROR,
    positiveButtonText: String = "확인",
    negativeButtonText: String? = null,
    onPositiveClick: () -> Unit,
    onNegativeClick: (() -> Unit)? = null,
    onDismiss: () -> Unit = {}
) {
    if (visible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            ErrorDialogContent(
                title = title,
                message = message,
                type = type,
                positiveButtonText = positiveButtonText,
                negativeButtonText = negativeButtonText,
                onPositiveClick = onPositiveClick,
                onNegativeClick = onNegativeClick
            )
        }
    }
}

@Composable
private fun ErrorDialogContent(
    title: String,
    message: String,
    type: ErrorDialogType,
    positiveButtonText: String,
    negativeButtonText: String?,
    onPositiveClick: () -> Unit,
    onNegativeClick: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .background(Color.White)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 아이콘
            Icon(
                imageVector = type.icon,
                contentDescription = null,
                tint = type.color,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 제목
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 메시지
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.Gray,
                    lineHeight = 20.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 버튼들
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (negativeButtonText != null) {
                    Arrangement.SpaceEvenly
                } else {
                    Arrangement.Center
                }
            ) {
                // 취소/아니오 버튼 (선택사항)
                negativeButtonText?.let { buttonText ->
                    Button(
                        onClick = {
                            onNegativeClick?.invoke()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor =Color.LightGray,
                            contentColor = Color.DarkGray
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = buttonText,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    if (negativeButtonText != null) {
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                }

                // 확인/예 버튼
                Button(
                    onClick = onPositiveClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = type.color,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = positiveButtonText,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

/**
 * 다이얼로그 타입별 아이콘과 색상 정의
 */
enum class ErrorDialogType(
    val icon: ImageVector,
    val color: Color
) {
    ERROR(Icons.Default.Warning, Color(0xFFE53E3E)),
    WARNING(Icons.Default.Warning, Color(0xFFFF8C00)),
    INFO(Icons.Default.Info, Color(0xFF3182CE)),
    QUESTION(Icons.Default.Info, Color(0xFF38A169))
}

/**
 * 사전 정의된 다이얼로그들
 */
object StandardDialogs {

    @Composable
    fun NetworkError(
        visible: Boolean,
        onRetryClick: () -> Unit,
        onDismiss: () -> Unit
    ) {
        ErrorDialog(
            visible = visible,
            title = "네트워크 오류",
            message = "인터넷 연결을 확인해주세요.\n잠시 후 다시 시도해보시기 바랍니다.",
            type = ErrorDialogType.ERROR,
            positiveButtonText = "확인",
            onPositiveClick = onRetryClick,
            onDismiss = onDismiss
        )
    }

    @Composable
    fun BrowserRetryDialog(
        visible: Boolean,
        onRetryClick: () -> Unit,
        onAlternativeClick: () -> Unit,
        onCancelClick: () -> Unit
    ) {
        ErrorDialog(
            visible = visible,
            title = "브라우저 오류",
            message = "브라우저 작업에 실패했습니다.\n계속 시도하시겠습니까?",
            type = ErrorDialogType.WARNING,
            positiveButtonText = "다시 시도",
            negativeButtonText = "취소",
            onPositiveClick = onRetryClick,
            onNegativeClick = onCancelClick
        )
    }

    @Composable
    fun ContributionFailureDialog(
        visible: Boolean,
        onRetryClick: () -> Unit,
        onLaterClick: () -> Unit,
        onGiveupClick: () -> Unit
    ) {
        ErrorDialog(
            visible = visible,
            title = "기여 데이터 전송 실패",
            message = "기여 데이터 전송에 실패했습니다.\n어떻게 하시겠습니까?",
            type = ErrorDialogType.QUESTION,
            positiveButtonText = "재시도",
            negativeButtonText = "나중에",
            onPositiveClick = onRetryClick,
            onNegativeClick = onLaterClick
        )
    }

    @Composable
    fun PlaywrightRestartDialog(
        visible: Boolean,
        onRestartClick: () -> Unit,
        onDismiss: () -> Unit
    ) {
        ErrorDialog(
            visible = visible,
            title = "브라우저 재시작 필요",
            message = "브라우저에 문제가 발생했습니다.\n브라우저를 재시작하시겠습니까?",
            type = ErrorDialogType.WARNING,
            positiveButtonText = "재시작",
            negativeButtonText = "취소",
            onPositiveClick = onRestartClick,
            onNegativeClick = onDismiss
        )
    }
}
package com.example.zhivoy.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.zhivoy.ui.theme.FitnessGradientEnd
import com.example.zhivoy.ui.theme.FitnessGradientStart

// Современная кнопка с градиентом
@Composable
fun ModernButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isGradient: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    isLoading: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .height(56.dp)
            .fillMaxWidth()
            .shadow(
                elevation = if (enabled && !isLoading) 8.dp else 0.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = FitnessGradientStart.copy(alpha = 0.5f)
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(0.dp) // Важно для центрирования градиента
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isGradient && enabled && !isLoading) {
                        Modifier.background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(FitnessGradientStart, FitnessGradientEnd)
                            )
                        )
                    } else if (enabled && !isLoading) {
                        Modifier.background(containerColor)
                    } else {
                        Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                ButtonLoadingIndicator()
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Обводная кнопка
@Composable
fun ModernOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(56.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 2.dp,
            color = if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

// Современная карточка
@Composable
fun ModernCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(20.dp),
            content = content
        )
    }
}

// Карточка с градиентом
@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = FitnessGradientStart.copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(FitnessGradientStart, FitnessGradientEnd)
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                content = content
            )
        }
    }
}

// Статистическая карточка
@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null
) {
    ModernCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .size(40.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        icon()
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Поле ввода
@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    isError: Boolean = false,
    errorMessage: String? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            isError = isError,
            enabled = enabled,
            singleLine = singleLine,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                errorBorderColor = MaterialTheme.colorScheme.error
            ),
            textStyle = MaterialTheme.typography.bodyLarge
        )
        if (isError && errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

// Современный график (Bar Chart)
@Composable
fun ModernBarChart(
    values: List<Int>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 150.dp
) {
    val maxValue = (values.maxOrNull() ?: 1).coerceAtLeast(1)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            values.forEachIndexed { index, value ->
                val barHeightFraction = value.toFloat() / maxValue
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(), // Теперь контейнер столбика занимает всю высоту Row
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    // Анимированное значение высоты
                    val animatedHeight by animateFloatAsState(
                        targetValue = barHeightFraction.coerceIn(0.05f, 1f),
                        animationSpec = tween(1000, easing = FastOutSlowInEasing),
                        label = "barHeight"
                    )

                    // Значение над столбиком
                    if (value > 0) {
                        Text(
                            text = if (value >= 1000) "${value / 1000}k" else value.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 8.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(animatedHeight) // Используем анимированную высоту
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(color, color.copy(alpha = 0.6f))
                                )
                            )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = labels.getOrNull(index) ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ========== ИНДИКАТОРЫ ЗАГРУЗКИ И СОСТОЯНИЯ ==========

// Skeleton loader для карточек
@Composable
fun SkeletonCard(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonAlpha"
    )

    ModernCard(modifier = modifier) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
            )
        }
    }
}

// Skeleton loader для StatCard
@Composable
fun SkeletonStatCard(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonAlpha"
    )

    ModernCard(modifier = modifier) {
        Column {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
            )
        }
    }
}

// Центральный индикатор загрузки
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(size),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp
        )
    }
}

// Индикатор загрузки в кнопке
@Composable
fun ButtonLoadingIndicator(
    modifier: Modifier = Modifier
) {
    CircularProgressIndicator(
        modifier = modifier.size(20.dp),
        color = Color.White,
        strokeWidth = 2.dp
    )
}

// Snackbar Host для отображения уведомлений
@Composable
fun ModernSnackbarHost(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier,
        snackbar = { data ->
            Snackbar(
                snackbarData = data,
                shape = RoundedCornerShape(12.dp),
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                actionColor = MaterialTheme.colorScheme.primary
            )
        }
    )
}

// Функция для показа успешного сообщения
suspend fun SnackbarHostState.showSuccess(message: String) {
    showSnackbar(
        message = message,
        duration = SnackbarDuration.Short
    )
}

// Функция для показа сообщения об ошибке
suspend fun SnackbarHostState.showError(message: String) {
    showSnackbar(
        message = message,
        duration = SnackbarDuration.Long
    )
}

// Композабл для отображения состояния загрузки с контентом
@Composable
fun LoadingContent(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    loadingContent: @Composable () -> Unit = { LoadingIndicator() },
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                loadingContent()
            }
        }
    }
}

// Композабл для отображения состояния загрузки или ошибки
@Composable
fun <T> AsyncContent(
    state: AsyncState<T>,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    loadingContent: @Composable () -> Unit = { LoadingIndicator() },
    errorContent: @Composable (String) -> Unit = { error ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Ошибка",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(16.dp))
                ModernButton(
                    text = "Повторить",
                    onClick = onRetry
                )
            }
        }
    },
    content: @Composable (T) -> Unit
) {
    when (state) {
        is AsyncState.Loading -> loadingContent()
        is AsyncState.Error -> errorContent(state.message)
        is AsyncState.Success -> content(state.data)
    }
}

// Состояния для асинхронных операций
sealed class AsyncState<out T> {
    object Loading : AsyncState<Nothing>()
    data class Success<T>(val data: T) : AsyncState<T>()
    data class Error(val message: String) : AsyncState<Nothing>()
}



package com.traffic_guard.ai.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.traffic_guard.ai.theme.AccentBlue
import com.traffic_guard.ai.theme.AccentRed
import com.traffic_guard.ai.theme.DarkBorder
import com.traffic_guard.ai.theme.LightBorder

enum class ButtonVariant {
    SOLID, OUTLINED, TEXT, DANGER
}

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.SOLID,
    icon: ImageVector? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Smooth micro-animation scale response on press
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "ButtonScale"
    )

    val contentModifier = Modifier
        .scale(scale)
        .then(modifier)

    when (variant) {
        ButtonVariant.SOLID -> {
            Button(
                onClick = onClick,
                enabled = enabled && !isLoading,
                interactionSource = interactionSource,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentBlue,
                    contentColor = Color.White,
                    disabledContainerColor = AccentBlue.copy(alpha = 0.5f),
                    disabledContentColor = Color.White.copy(alpha = 0.5f)
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                modifier = contentModifier
            ) {
                ButtonContent(text, icon, isLoading, Color.White)
            }
        }
        ButtonVariant.OUTLINED -> {
            val isDark = MaterialTheme.colorScheme.background.value == 0xFF0F172A.toULong()
            OutlinedButton(
                onClick = onClick,
                enabled = enabled && !isLoading,
                interactionSource = interactionSource,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp, 
                    if (isDark) DarkBorder else LightBorder
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isDark) Color.White else AccentBlue
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                modifier = contentModifier
            ) {
                ButtonContent(
                    text = text, 
                    icon = icon, 
                    isLoading = isLoading, 
                    color = if (isDark) Color.White else AccentBlue
                )
            }
        }
        ButtonVariant.TEXT -> {
            Button(
                onClick = onClick,
                enabled = enabled && !isLoading,
                interactionSource = interactionSource,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = AccentBlue,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = AccentBlue.copy(alpha = 0.5f)
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                modifier = contentModifier
            ) {
                ButtonContent(text, icon, isLoading, AccentBlue)
            }
        }
        ButtonVariant.DANGER -> {
            Button(
                onClick = onClick,
                enabled = enabled && !isLoading,
                interactionSource = interactionSource,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentRed,
                    contentColor = Color.White,
                    disabledContainerColor = AccentRed.copy(alpha = 0.5f)
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                modifier = contentModifier
            ) {
                ButtonContent(text, icon, isLoading, Color.White)
            }
        }
    }
}

@Composable
private fun ButtonContent(
    text: String,
    icon: ImageVector?,
    isLoading: Boolean,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = color,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = color
        )
    }
}

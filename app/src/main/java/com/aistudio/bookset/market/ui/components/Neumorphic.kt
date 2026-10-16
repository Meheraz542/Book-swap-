package com.aistudio.bookset.market.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aistudio.bookset.market.ui.theme.InputBackground
import com.aistudio.bookset.market.ui.theme.InputBorder
import com.aistudio.bookset.market.ui.theme.NeumorphicBackground
import com.aistudio.bookset.market.ui.theme.NeumorphicDarkShadow
import com.aistudio.bookset.market.ui.theme.NeumorphicLightShadow
import com.aistudio.bookset.market.ui.theme.NeumorphicSurface

/**
 * Soft Neumorphic Extruded Card / Container
 * Employs dual subtle shadows (light top-left, soft dark bottom-right)
 */
@Composable
fun NeumorphicCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = NeumorphicSurface,
    elevation: Dp = 6.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .drawBehind {
                val cornerRadius = 20.dp.toPx()
                // Top-left highlight
                drawCircle(
                    color = NeumorphicLightShadow.copy(alpha = 0.7f),
                    radius = size.maxDimension,
                    center = Offset(-10f, -10f)
                )
            }
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = NeumorphicDarkShadow.copy(alpha = 0.5f),
                spotColor = NeumorphicDarkShadow.copy(alpha = 0.6f)
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        backgroundColor.copy(alpha = 0.95f),
                        backgroundColor
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(400f, 400f)
                ),
                shape = shape
            )
            .border(1.dp, Color.White.copy(alpha = 0.45f), shape)
            .clip(shape)
    ) {
        content()
    }
}

/**
 * Soft Neumorphic Recessed Input Container
 * Creates an inset, debossed cavity feel for text fields
 */
@Composable
fun NeumorphicRecessedBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(InputBackground, shape)
            .border(1.2.dp, InputBorder.copy(alpha = 0.6f), shape)
            .clip(shape)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        content()
    }
}

/**
 * Soft Neumorphic Button with Dual Shadow & Ripple
 */
@Composable
fun NeumorphicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = NeumorphicSurface,
    elevation: Dp = 5.dp,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (enabled) elevation else 1.dp,
                shape = shape,
                ambientColor = NeumorphicDarkShadow.copy(alpha = 0.45f),
                spotColor = NeumorphicDarkShadow.copy(alpha = 0.6f)
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        backgroundColor.copy(alpha = 0.95f),
                        backgroundColor.copy(alpha = 0.85f)
                    )
                ),
                shape = shape
            )
            .border(1.dp, Color.White.copy(alpha = 0.6f), shape)
            .clip(shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(color = Color.Black.copy(alpha = 0.15f)),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

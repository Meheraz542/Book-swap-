package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SetCondition
import com.example.ui.theme.*

/**
 * Core Liquid Glass + Neumorphism + Inner Shadow modifier.
 * Satisfies all 3 design pillars:
 * 1. GLASS EFFECT: Frosted translucent surface, specular rim gradient stroke, top specular sheen.
 * 2. NEUMORPHISM: 3D extruded elevation with colored caustic drop shadow and responsive tactile press physics.
 * 3. INNER SHADOW: Real 3D physical inner bevel with top inner highlight and bottom/edge inner shadow.
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(28.dp),
    backgroundColor: Color = CardGlass,
    backgroundBrush: Brush? = null,
    borderBrush: Brush = GlassRimBrush,
    borderWidth: Dp = 1.2.dp,
    elevation: Dp = 6.dp,
    shadowColor: Color = SoftShadowColor,
    innerShadowColor: Color = Color(0x28000000),
    innerHighlightColor: Color = Color.White.copy(alpha = 0.70f),
    showTopSheen: Boolean = true,
    isPressed: Boolean = false
): Modifier = this
    .shadow(
        elevation = if (isPressed) (elevation * 0.35f).coerceAtLeast(1.dp) else elevation,
        shape = shape,
        ambientColor = shadowColor,
        spotColor = shadowColor
    )
    .clip(shape)
    .drawBehind {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@drawBehind

        // 1. Base Glass Surface (Translucent gradient or color)
        if (backgroundBrush != null) {
            drawRect(backgroundBrush)
        } else {
            drawRect(backgroundColor)
        }

        // 2. High-performance unified 3D Specular Inner Bevel (streamlined to single shader pass)
        val effectiveHighlightAlpha = if (isPressed) innerHighlightColor.alpha * 0.4f else innerHighlightColor.alpha
        val effectiveShadowAlpha = if (isPressed) (innerShadowColor.alpha * 1.4f).coerceAtMost(0.6f) else innerShadowColor.alpha
        drawRect(
            brush = Brush.verticalGradient(
                0.0f to innerHighlightColor.copy(alpha = effectiveHighlightAlpha),
                0.35f to Color.Transparent,
                0.70f to Color.Transparent,
                1.0f to innerShadowColor.copy(alpha = effectiveShadowAlpha)
            )
        )

        // 3. SPECULAR LIQUID GLASS REFLECTION SHEEN (Top pill shine highlight)
        if (showTopSheen && h >= 26f && w >= 36f) {
            val sheenHeight = (h * 0.32f).coerceIn(4f, 15f)
            val sheenWidth = w * 0.86f
            val left = (w - sheenWidth) / 2f
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isPressed) 0.12f else 0.42f),
                        Color.Transparent
                    ),
                    startY = 2f,
                    endY = 2f + sheenHeight
                ),
                topLeft = Offset(left, 2.5f),
                size = Size(sheenWidth, sheenHeight),
                cornerRadius = CornerRadius(sheenHeight / 2f, sheenHeight / 2f)
            )
        }
    }
    .border(borderWidth, borderBrush, shape)

/**
 * Ambient background backdrop that draws the soft studio lighting and chromatic caustics
 * (violet, cyan, and warm peach backlight bloom) with optimized GPU fill-rate.
 */
@Composable
fun LiquidGlassBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val w = size.width
                val h = size.height

                // Studio Canvas Base
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(BgCanvasGradientStart, BgCanvasGradientEnd)
                    )
                )

                // Top-Left Diffuse Violet Caustic Bloom
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(CausticPurple, Color.Transparent),
                        center = Offset(w * 0.15f, h * 0.18f),
                        radius = w * 0.65f
                    )
                )

                // Mid-Right Diffuse Cyan Caustic Bloom
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(CausticCyan, Color.Transparent),
                        center = Offset(w * 0.88f, h * 0.42f),
                        radius = w * 0.70f
                    )
                )
            },
        content = content
    )
}

/**
 * Full Liquid Glass Card matching the oversized floating sheet in the design image.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    backgroundColor: Color = CardGlass,
    backgroundBrush: Brush? = null,
    borderBrush: Brush = GlassRimBrush,
    elevation: Dp = 6.dp,
    onClick: (() -> Unit)? = null,
    testTag: String = "glass_card",
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed && onClick != null) 0.98f else 1f, label = "cardScale")

    Box(
        modifier = modifier
            .scale(scale)
            .liquidGlass(
                shape = shape,
                backgroundColor = backgroundColor,
                backgroundBrush = backgroundBrush,
                borderBrush = borderBrush,
                borderWidth = 1.2.dp,
                elevation = elevation,
                shadowColor = SoftShadowColor,
                innerShadowColor = Color(0x18000000),
                innerHighlightColor = Color.White.copy(alpha = 0.85f),
                showTopSheen = true,
                isPressed = isPressed && onClick != null
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(color = PrimaryPurple.copy(alpha = 0.2f))
                    ) { onClick() }
                } else Modifier
            )
            .testTag(testTag)
            .padding(18.dp)
    ) {
        Column(content = content)
    }
}

/**
 * 1. Primary Liquid Glass Pill Button (matching "Setvittley" in reference design)
 * Incorporates:
 * 1. GLASS EFFECT (translucent purple glass with specular rim and reflection sheen)
 * 2. NEUMORPHISM (soft purple extruded drop shadow with tactile press physics)
 * 3. INNER SHADOW (deep purple inner shadow bevel and crisp top inner highlight)
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconComposable: (@Composable () -> Unit)? = null,
    gradient: Brush? = null,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(height / 2f),
    testTag: String = "glass_button"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed && enabled) 0.95f else 1f, label = "btnScale")

    val baseBrush = if (!enabled) {
        Brush.linearGradient(listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8)))
    } else {
        gradient ?: PurpleButtonBrush
    }
    val shadowColor = if (enabled) PrimaryPurpleGlow else SoftShadowColor

    Box(
        modifier = modifier
            .scale(scale)
            .height(height)
            .liquidGlass(
                shape = shape,
                backgroundBrush = baseBrush,
                borderBrush = GlassRimBrush,
                borderWidth = 1.4.dp,
                elevation = if (enabled) 7.dp else 2.dp,
                shadowColor = shadowColor,
                innerShadowColor = PrimaryPurpleInnerShadow,
                innerHighlightColor = Color.White.copy(alpha = 0.75f),
                showTopSheen = true,
                isPressed = isPressed && enabled
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(color = Color.White)
            ) { onClick() }
            .testTag(testTag)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (iconComposable != null) {
                iconComposable()
                Spacer(modifier = Modifier.width(8.dp))
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            )
        }
    }
}

/**
 * 2. Secondary Liquid Glass Button (matching "Secondary" & "folect" in reference design)
 * Luminous Mint / Cyan Liquid Glass Pill with:
 * 1. GLASS EFFECT
 * 2. NEUMORPHISM
 * 3. INNER SHADOW
 */
@Composable
fun SecondaryGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 48.dp,
    testTag: String = "secondary_glass_button"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed && enabled) 0.95f else 1f, label = "secBtnScale")

    Box(
        modifier = modifier
            .scale(scale)
            .height(height)
            .liquidGlass(
                shape = RoundedCornerShape(height / 2f),
                backgroundBrush = CyanButtonBrush,
                borderBrush = GlassRimBrush,
                borderWidth = 1.3.dp,
                elevation = 5.dp,
                shadowColor = SecondaryCyanGlow,
                innerShadowColor = SecondaryCyanInnerShadow,
                innerHighlightColor = Color.White.copy(alpha = 0.85f),
                showTopSheen = true,
                isPressed = isPressed && enabled
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(color = SecondaryCyanText)
            ) { onClick() }
            .testTag(testTag)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SecondaryCyanText,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = SecondaryCyanText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * 3. Frosted White Liquid Glass Button (matching "Upgrade plan" / "Invite member" in design)
 * Crisp translucent white glass with:
 * 1. GLASS EFFECT
 * 2. NEUMORPHISM
 * 3. INNER SHADOW
 */
@Composable
fun FrostedGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    textColor: Color = TextPrimary,
    height: Dp = 48.dp,
    testTag: String = "frosted_glass_button"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "frostedBtnScale")

    Box(
        modifier = modifier
            .scale(scale)
            .height(height)
            .liquidGlass(
                shape = RoundedCornerShape(height / 2f),
                backgroundBrush = FrostedWhiteBrush,
                borderBrush = GlassRimBrush,
                borderWidth = 1.2.dp,
                elevation = 4.dp,
                shadowColor = FrostedWhiteGlow,
                innerShadowColor = FrostedWhiteInnerShadow,
                innerHighlightColor = Color.White.copy(alpha = 0.95f),
                showTopSheen = true,
                isPressed = isPressed
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = PrimaryPurple.copy(alpha = 0.2f))
            ) { onClick() }
            .testTag(testTag)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Liquid Glass Icon Button (matching circular action buttons, search, and add buttons in image)
 * 1. Glass Effect
 * 2. Neumorphism
 * 3. Inner Shadow
 */
enum class GlassIconTone {
    PRIMARY_PURPLE,
    CYAN,
    FROSTED_WHITE,
    AMBER,
    DANGER
}

@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shape: androidx.compose.ui.graphics.Shape = CircleShape,
    tone: GlassIconTone = GlassIconTone.FROSTED_WHITE,
    testTag: String = "glass_icon_button"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.92f else 1f, label = "iconBtnScale")

    val (bgBrush, borderBrush, shadowCol, innerShadowCol, tintColor) = when (tone) {
        GlassIconTone.PRIMARY_PURPLE -> listOf(
            PurpleButtonBrush,
            GlassRimBrush,
            PrimaryPurpleGlow,
            PrimaryPurpleInnerShadow,
            Color.White
        )
        GlassIconTone.CYAN -> listOf(
            CyanButtonBrush,
            GlassRimBrush,
            SecondaryCyanGlow,
            SecondaryCyanInnerShadow,
            SecondaryCyanText
        )
        GlassIconTone.FROSTED_WHITE -> listOf(
            FrostedWhiteBrush,
            GlassRimBrush,
            FrostedWhiteGlow,
            FrostedWhiteInnerShadow,
            TextPrimary
        )
        GlassIconTone.AMBER -> listOf(
            Brush.linearGradient(listOf(Color(0xFFFB923C), Color(0xFFF97316))),
            GlassRimBrush,
            AccentOrangeGlow,
            Color(0x387C2D12),
            Color.White
        )
        GlassIconTone.DANGER -> listOf(
            Brush.linearGradient(listOf(Color(0xFFFB7185), Color(0xFFE11D48))),
            GlassRimBrush,
            Color(0x40E11D48),
            Color(0x40881337),
            Color.White
        )
    }

    Box(
        modifier = modifier
            .scale(scale)
            .size(size)
            .liquidGlass(
                shape = shape,
                backgroundBrush = bgBrush as Brush,
                borderBrush = borderBrush as Brush,
                borderWidth = 1.3.dp,
                elevation = 5.dp,
                shadowColor = shadowCol as Color,
                innerShadowColor = innerShadowCol as Color,
                innerHighlightColor = Color.White.copy(alpha = 0.85f),
                showTopSheen = true,
                isPressed = isPressed
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = (tintColor as Color).copy(alpha = 0.3f))
            ) { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tintColor as Color,
            modifier = Modifier.size((size * 0.48f).coerceAtLeast(18.dp))
        )
    }
}

/**
 * Concentric Liquid Glass Dial Button (matching the iridescent multi-ring circular button
 * at the bottom-left of the reference design image).
 */
@Composable
fun LiquidGlassDialButton(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    testTag: String = "liquid_glass_dial"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.94f else 1f, label = "dialScale")

    Box(
        modifier = modifier
            .scale(scale)
            .size(size)
            // Outer iridescent ring with cyan/purple rim
            .shadow(10.dp, CircleShape, ambientColor = PrimaryPurpleGlow, spotColor = SecondaryCyanGlow)
            .clip(CircleShape)
            .background(
                Brush.sweepGradient(
                    listOf(
                        Color(0xFF818CF8).copy(alpha = 0.75f),
                        Color(0xFF22D3EE).copy(alpha = 0.75f),
                        Color(0xFFE879F9).copy(alpha = 0.75f),
                        Color(0xFF818CF8).copy(alpha = 0.75f)
                    )
                )
            )
            .border(
                2.dp,
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.95f), Color(0x6067E8F9), Color(0x60C084FC))
                ),
                CircleShape
            )
            .padding(6.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color.White)
            ) { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        // Inner recessed debossed well with inner shadow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .liquidGlass(
                    shape = CircleShape,
                    backgroundColor = Color.White.copy(alpha = 0.85f),
                    borderBrush = GlassRimBrush,
                    borderWidth = 1.2.dp,
                    elevation = 2.dp,
                    shadowColor = SoftShadowColor,
                    innerShadowColor = Color(0x35000000),
                    innerHighlightColor = Color.White,
                    showTopSheen = true,
                    isPressed = isPressed
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryPurple,
                modifier = Modifier.size((size * 0.38f).coerceAtLeast(20.dp))
            )
        }
    }
}

/**
 * Liquid Glass Search Bar Capsule (matching "Search projects..." & "Create workspace..." in image)
 * Includes specular top sheen, inner shadow track, and circular liquid glass action button.
 */
@Composable
fun GlassSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholderText: String = "Search books, subjects, authors...",
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .liquidGlass(
                shape = RoundedCornerShape(29.dp),
                backgroundColor = Color.White.copy(alpha = 0.88f),
                borderBrush = GlassRimBrush,
                borderWidth = 1.3.dp,
                elevation = 6.dp,
                shadowColor = SoftShadowColor,
                innerShadowColor = Color(0x18000000),
                innerHighlightColor = Color.White,
                showTopSheen = true
            )
            .then(if (readOnly && onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(start = 18.dp, end = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search",
                tint = PrimaryPurple,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            if (readOnly) {
                Text(
                    text = query.ifEmpty { placeholderText },
                    color = if (query.isEmpty()) TextMuted else TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
            } else {
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = {
                        Text(
                            text = placeholderText,
                            color = TextMuted,
                            fontSize = 15.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_text_input"),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = PrimaryPurple
                    )
                )
            }

            // Embedded Liquid Glass Filter Button (matches purple icon capsule in reference image)
            GlassIconButton(
                onClick = onFilterClick,
                icon = Icons.Outlined.Tune,
                contentDescription = "Filters",
                size = 42.dp,
                tone = GlassIconTone.PRIMARY_PURPLE,
                testTag = "filter_button"
            )
        }
    }
}

/**
 * Liquid Glass Filter / Category Chip
 * 1. Glass Effect
 * 2. Neumorphism
 * 3. Inner Shadow
 */
@Composable
fun LiquidGlassChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    testTag: String = "liquid_glass_chip"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.94f else 1f, label = "chipScale")

    val bgBrush = if (selected) PurpleButtonBrush else FrostedWhiteBrush
    val shadowColor = if (selected) PrimaryPurpleGlow else SoftShadowColor
    val innerShadowColor = if (selected) PrimaryPurpleInnerShadow else Color(0x18000000)
    val textColor = if (selected) Color.White else TextSecondary

    Box(
        modifier = modifier
            .scale(scale)
            .height(40.dp)
            .liquidGlass(
                shape = RoundedCornerShape(20.dp),
                backgroundBrush = bgBrush,
                borderBrush = GlassRimBrush,
                borderWidth = if (selected) 1.5.dp else 1.dp,
                elevation = if (selected) 5.dp else 2.dp,
                shadowColor = shadowColor,
                innerShadowColor = innerShadowColor,
                innerHighlightColor = Color.White.copy(alpha = 0.85f),
                showTopSheen = true,
                isPressed = isPressed
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = if (selected) Color.White else PrimaryPurple)
            ) { onClick() }
            .testTag(testTag)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

/**
 * Liquid Glass Status Badge with Inner Shadow
 */
@Composable
fun ConditionBadge(
    condition: SetCondition,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (condition) {
        SetCondition.LIKE_NEW -> BadgeLikeNewBg to BadgeLikeNewText
        SetCondition.GOOD -> BadgeGoodBg to BadgeGoodText
        SetCondition.USED -> BadgeUsedBg to BadgeUsedText
        SetCondition.HEAVILY_USED -> BadgeHeavilyUsedBg to BadgeHeavilyUsedText
    }

    Box(
        modifier = modifier
            .liquidGlass(
                shape = RoundedCornerShape(12.dp),
                backgroundColor = bg,
                borderBrush = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.8f), fg.copy(alpha = 0.3f))
                ),
                borderWidth = 1.dp,
                elevation = 2.dp,
                shadowColor = fg.copy(alpha = 0.15f),
                innerShadowColor = Color(0x18000000),
                innerHighlightColor = Color.White.copy(alpha = 0.6f),
                showTopSheen = false
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = condition.label,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Floating Liquid Glass Dock Navigation Bar matching design image
 */
enum class BottomNavTab {
    HOME, SEARCH, SELL, INBOX, PROFILE
}

@Composable
fun FloatingBottomNavBar(
    currentTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    inboxUnreadCount: Int = 3,
    modifier: Modifier = Modifier
) {
    // Exactly the 5 menu options requested by the user: Home, Search, Sell, Inbox, Profile
    val pillTabs = remember {
        listOf(
            Triple(BottomNavTab.HOME, Icons.Outlined.Home, "Home"),
            Triple(BottomNavTab.SEARCH, Icons.Outlined.Search, "Search"),
            Triple(BottomNavTab.SELL, Icons.Default.Add, "Sell"),
            Triple(BottomNavTab.INBOX, Icons.Outlined.ChatBubbleOutline, "Inbox"),
            Triple(BottomNavTab.PROFILE, Icons.Outlined.Person, "Profile")
        )
    }

    val activePillIndex = when (currentTab) {
        BottomNavTab.HOME -> 0
        BottomNavTab.SEARCH -> 1
        BottomNavTab.SELL -> 2
        BottomNavTab.INBOX -> 3
        BottomNavTab.PROFILE -> 4
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // The Unified Liquid Glass Capsule Dock with smooth sliding liquid indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(34.dp),
                    ambientColor = Color(0x35000000),
                    spotColor = Color(0x35000000)
                )
                .clip(RoundedCornerShape(34.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF475569).copy(alpha = 0.60f),
                            Color(0xFF1E293B).copy(alpha = 0.74f)
                        )
                    )
                )
                .border(
                    width = 1.3.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.80f),
                            Color.White.copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.50f)
                        )
                    ),
                    shape = RoundedCornerShape(34.dp)
                )
        ) {
            // Specular top highlight for the main container
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(6.dp)
                    .align(Alignment.TopCenter)
                    .padding(top = 2.5f.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    )
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 5.dp, vertical = 5.dp)
            ) {
                val totalWidth = maxWidth
                val tabWidth = totalWidth / 5
                val capsuleHeight = maxHeight

                // Sliding Liquid Glass Active Indicator Capsule
                if (activePillIndex in 0..4) {
                    val targetX = tabWidth * activePillIndex
                    val animatedX by animateDpAsState(
                        targetValue = targetX,
                        animationSpec = spring(
                            dampingRatio = 0.75f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "liquid_capsule_x"
                    )

                    Box(
                        modifier = Modifier
                            .offset(x = animatedX)
                            .width(tabWidth)
                            .height(capsuleHeight)
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(26.dp),
                                ambientColor = Color(0x30FFFFFF),
                                spotColor = Color(0x30FFFFFF)
                            )
                            .clip(RoundedCornerShape(26.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.40f),
                                        Color.White.copy(alpha = 0.22f)
                                    )
                                )
                            )
                            .border(
                                width = 1.2.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.90f),
                                        Color.White.copy(alpha = 0.40f)
                                    )
                                ),
                                shape = RoundedCornerShape(26.dp)
                            )
                    ) {
                        // Specular top sheen on active pill
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.82f)
                                .height(5.dp)
                                .align(Alignment.TopCenter)
                                .padding(top = 2.dp)
                                .clip(RoundedCornerShape(2.5f.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.60f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                }

                // Clickable Tab Buttons (Home, Search, Sell, Inbox, Profile)
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pillTabs.forEach { (tab, icon, label) ->
                        val isSelected = currentTab == tab
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val scale by animateFloatAsState(
                            targetValue = if (isPressed) 0.92f else 1f,
                            animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
                            label = "tab_scale"
                        )

                        val animColor by animateColorAsState(
                            targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.72f),
                            animationSpec = tween(200),
                            label = "tab_color"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .scale(scale)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) {
                                    onTabSelected(tab)
                                }
                                .testTag("nav_${tab.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = animColor,
                                        modifier = Modifier.size(21.dp)
                                    )
                                    if (tab == BottomNavTab.INBOX && inboxUnreadCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .offset(x = 12.dp, y = (-4).dp)
                                                .size(15.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFEF4444)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (inboxUnreadCount > 9) "9+" else "$inboxUnreadCount",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = label,
                                    color = animColor,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tactical Glassmorphic + Neumorphic Dropdown Field matching the reference image.
 * Features:
 * - Tactile 3D Neumorphic depth with dual drop caustics
 * - Frosted glass reflection sheen and specular rim
 * - Clean expandable popup menu with checkmarks on selected item
 * - Replaces tedious horizontal sliding chips with instant dropdown selection
 */
@Composable
fun <T> GlassNeumorphicDropdownField(
    label: String,
    selectedValue: T?,
    displayValue: (T?) -> String,
    options: List<T?>,
    onSelect: (T?) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Select an option",
    subtitle: ((T?) -> String?)? = null,
    supportingText: String? = null,
    leadingIcon: ImageVector? = null,
    testTag: String = "glass_dropdown"
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "arrow_rotation")
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.985f else 1f, label = "press_scale")

    Box(modifier = modifier.testTag(testTag)) {
        Column {
            // Dropdown Container Card with Glass Effect + Neumorphic depth
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scale)
                    .liquidGlass(
                        shape = RoundedCornerShape(18.dp),
                        backgroundColor = Color.White.copy(alpha = 0.92f),
                        borderBrush = GlassRimBrush,
                        borderWidth = 1.3.dp,
                        elevation = if (expanded) 6.dp else 4.dp,
                        shadowColor = if (expanded) PrimaryPurpleGlow else SoftShadowColor,
                        innerShadowColor = Color(0x10000000),
                        innerHighlightColor = Color.White,
                        showTopSheen = true,
                        isPressed = isPressed
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(color = PrimaryPurple.copy(alpha = 0.15f))
                    ) {
                        expanded = !expanded
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (leadingIcon != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PrimaryPurpleLight.copy(alpha = 0.75f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = leadingIcon,
                                    contentDescription = null,
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMuted,
                            letterSpacing = 0.3.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (selectedValue != null) displayValue(selectedValue) else placeholder,
                            fontSize = 14.sp,
                            fontWeight = if (selectedValue != null) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selectedValue != null) TextPrimary else TextMuted,
                            maxLines = 1
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Dropdown Arrow",
                        tint = if (expanded) PrimaryPurple else TextSecondary,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(rotation)
                    )
                }
            }

            if (!supportingText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = supportingText,
                    fontSize = 11.sp,
                    color = PrimaryPurple,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }

        // Dropdown Menu popup with Glass + Neumorphic styling
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            properties = androidx.compose.ui.window.PopupProperties(focusable = true),
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .background(Color.White.copy(alpha = 0.98f))
                .border(
                    BorderStroke(
                        1.2.dp,
                        Brush.linearGradient(
                            listOf(Color.White, Color(0xFFE2E8F0), PrimaryPurpleLight)
                        )
                    ),
                    RoundedCornerShape(18.dp)
                )
                .shadow(12.dp, RoundedCornerShape(18.dp), ambientColor = SoftShadowColor, spotColor = PrimaryPurpleGlow)
        ) {
            options.forEach { option ->
                val isSelected = option == selectedValue
                val text = displayValue(option)
                val sub = subtitle?.invoke(option)

                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = text,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) PrimaryPurple else TextPrimary
                                )
                                if (!sub.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = sub,
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                    modifier = Modifier
                        .then(
                            if (isSelected) Modifier.background(PrimaryPurpleLight.copy(alpha = 0.65f))
                            else Modifier
                        )
                )
            }
        }
    }
}

/**
 * Tactile Neumorphic Glass Card:
 * Combines soft neumorphic dual shadows with frosted glass transparency.
 */
@Composable
fun NeumorphicGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = CardGlass,
    elevation: Dp = 6.dp,
    onClick: (() -> Unit)? = null,
    testTag: String = "neumorphic_glass_card",
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed && onClick != null) 0.98f else 1f, label = "neuCardScale")

    Box(
        modifier = modifier
            .scale(scale)
            .liquidGlass(
                shape = shape,
                backgroundColor = backgroundColor,
                borderBrush = GlassRimBrush,
                borderWidth = 1.2.dp,
                elevation = elevation,
                shadowColor = SoftShadowColor,
                innerShadowColor = Color(0x14000000),
                innerHighlightColor = Color.White.copy(alpha = 0.85f),
                showTopSheen = true,
                isPressed = isPressed && onClick != null
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(color = PrimaryPurple.copy(alpha = 0.15f))
                    ) { onClick() }
                } else Modifier
            )
            .testTag(testTag)
            .padding(16.dp)
    ) {
        Column(content = content)
    }
}

/**
 * Animated Confirmation Dialog with a bouncy 'Pop' entrance effect,
 * soft rounded card, colored circular icon header, Cancel and Action buttons.
 * Used for high-stakes actions like Delete Listing and Log Out.
 */
@Composable
fun PopAnimatedConfirmationDialog(
    isOpen: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    description: String,
    confirmButtonText: String,
    confirmButtonColor: Color,
    icon: ImageVector,
    iconColor: Color,
    testTagConfirm: String
) {
    if (isOpen) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            var animateIn by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                animateIn = true
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A).copy(alpha = 0.40f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onDismissRequest() },
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = animateIn,
                    enter = scaleIn(
                        initialScale = 0.60f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(animationSpec = tween(180)),
                    exit = scaleOut(targetScale = 0.8f) + fadeOut(animationSpec = tween(120))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {} // Prevent dismiss on modal card click
                            .liquidGlass(
                                shape = RoundedCornerShape(28.dp),
                                backgroundBrush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.94f),
                                        Color(0xFFF8FAFC).copy(alpha = 0.86f),
                                        Color(0xFFEFF6FF).copy(alpha = 0.88f)
                                    )
                                ),
                                borderBrush = GlassRimBrush,
                                borderWidth = 1.5.dp,
                                elevation = 16.dp,
                                shadowColor = DeepShadowColor,
                                innerShadowColor = Color(0x14000000),
                                innerHighlightColor = Color.White.copy(alpha = 0.92f),
                                showTopSheen = true
                            )
                            .padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .liquidGlass(
                                        shape = CircleShape,
                                        backgroundColor = iconColor.copy(alpha = 0.12f),
                                        borderBrush = Brush.linearGradient(
                                            listOf(
                                                iconColor.copy(alpha = 0.45f),
                                                iconColor.copy(alpha = 0.15f)
                                            )
                                        ),
                                        borderWidth = 1.2.dp,
                                        elevation = 4.dp,
                                        shadowColor = iconColor.copy(alpha = 0.22f),
                                        showTopSheen = true
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = iconColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = title,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = description,
                                fontSize = 13.5.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 19.sp
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                FrostedGlassButton(
                                    text = "Cancel",
                                    onClick = onDismissRequest,
                                    textColor = TextSecondary,
                                    modifier = Modifier.weight(1f),
                                    height = 48.dp,
                                    testTag = "btn_popup_cancel"
                                )

                                val confirmInteractionSource = remember { MutableInteractionSource() }
                                val isConfirmPressed by confirmInteractionSource.collectIsPressedAsState()
                                val confirmScale by animateFloatAsState(
                                    targetValue = if (isConfirmPressed) 0.94f else 1f,
                                    label = "fluffyConfirmScale"
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1.35f)
                                        .scale(confirmScale)
                                        .height(48.dp)
                                        .shadow(
                                            elevation = 8.dp,
                                            shape = RoundedCornerShape(24.dp),
                                            ambientColor = confirmButtonColor.copy(alpha = 0.40f),
                                            spotColor = confirmButtonColor.copy(alpha = 0.55f)
                                        )
                                        .background(
                                            color = confirmButtonColor,
                                            shape = RoundedCornerShape(24.dp)
                                        )
                                        .border(
                                            width = 1.2.dp,
                                            brush = Brush.verticalGradient(
                                                listOf(
                                                    Color.White.copy(alpha = 0.45f),
                                                    Color.Transparent
                                                )
                                            ),
                                            shape = RoundedCornerShape(24.dp)
                                        )
                                        .clickable(
                                            interactionSource = confirmInteractionSource,
                                            indication = ripple(color = Color.White)
                                        ) { onConfirm() }
                                        .testTag(testTagConfirm),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = confirmButtonText,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.3.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}



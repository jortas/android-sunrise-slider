package com.jortas.sunriseslider.slider

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.jortas.sunriseslider.R
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/*
 Most of this code was copied from Google Compose Slider
*/

@Composable
fun SunriseSlider(
    modifier: Modifier = Modifier,
    value: Float,
    onValueChange: (Float) -> Unit = {},
    interactionSource: MutableInteractionSource = MutableInteractionSource(),
    enabled: Boolean = true,
    valueRangeParam: ClosedFloatingPointRange<Float>? = null,
    steps: Int = 0,
    values: List<Float> = emptyList(),
    onValueChangeFinished: (() -> Unit) = {},
    tutorialEnabled: Boolean = false,
    colors: SunriseSliderColors
) {
    val sliderValues = remember(values, steps) {
        values.ifEmpty {
            valueRangeToDiscreteValues(valueRangeParam, steps)
        }
    }
    val onValueChangeState = rememberUpdatedState(onValueChange)

    val valueRange = remember(sliderValues, valueRangeParam) {
        if (sliderValues.isEmpty()) {
            if (valueRangeParam == null) {
                throw RuntimeException("Slider must contain values or/and a valueRange")
            }
            valueRangeParam
        } else {
            var auxValueRange = sliderValues.minOrNull()!!..sliderValues.maxOrNull()!!
            valueRangeParam?.let {
                if (!auxValueRange.contains(it.start)) {
                    auxValueRange = it.start..auxValueRange.endInclusive
                }
                if (!auxValueRange.contains(it.endInclusive)) {
                    auxValueRange = auxValueRange.start..it.endInclusive
                }
            }
            auxValueRange
        }
    }

    val ticks = remember(sliderValues, valueRange) {
        valuesToTickFractions(sliderValues, valueRange)
    }

    BoxWithConstraints(
        modifier
            .wrapContentHeight()
            .requiredSizeIn(minHeight = ThumbRadius * 2)
            .sliderSemantics(value, ticks, enabled, onValueChange, valueRange, ticks.count())
            .focusable(interactionSource = interactionSource)
    ) {
        val maxPx = constraints.maxWidth.toFloat()
        val minPx = 0f

        fun scaleToUserValue(offset: Float) =
            scale(minPx, maxPx, offset, valueRange.start, valueRange.endInclusive)

        fun scaleToOffset(userValue: Float) =
            scale(valueRange.start, valueRange.endInclusive, userValue, minPx, maxPx)

        val scope = rememberCoroutineScope()
        val rawOffset = remember { mutableStateOf(scaleToOffset(value)) }
        val draggableState = remember(minPx, maxPx, valueRange) {
            SliderDraggableState {
                rawOffset.value = (rawOffset.value + it).coerceIn(minPx, maxPx)
                onValueChangeState.value.invoke(scaleToUserValue(rawOffset.value))
            }
        }

        CorrectValueSideEffect(::scaleToOffset, valueRange, rawOffset, value)

        val gestureEndAction = rememberUpdatedState<(Float) -> Unit> { velocity: Float ->
            val current = rawOffset.value
            val target = snapValueToTick(current, ticks, minPx, maxPx)
            if (current != target) {
                scope.launch {
                    animateToTarget(draggableState, current, target, velocity)
                    onValueChangeFinished.invoke()
                }
            } else if (!draggableState.isDragging) {
                // check ifDragging in case the change is still in progress (touch -> drag case)
                onValueChangeFinished.invoke()
            }
        }
        val press = Modifier.sliderPressModifier(
            draggableState, interactionSource, maxPx, rawOffset, gestureEndAction
        )

        val drag = Modifier.draggable(
            orientation = Orientation.Horizontal,
            interactionSource = interactionSource,
            onDragStopped = { velocity -> gestureEndAction.value.invoke(velocity) },
            startDragImmediately = false,
            state = draggableState
        )

        val coerced = value.coerceIn(valueRange.start, valueRange.endInclusive)
        val fraction = calcFraction(valueRange.start, valueRange.endInclusive, coerced)

        SliderImpl(
            modifier =
            press
                .then(drag),
            enabled,
            fraction,
            ticks,
            tutorialEnabled,
            colors,
            maxPx,
            interactionSource
        )
    }
}

@Composable
private fun SliderImpl(
    modifier: Modifier,
    enabled: Boolean,
    positionFraction: Float,
    tickFractions: List<Float>,
    tutorialEnabled: Boolean = false,
    colors: SunriseSliderColors,
    width: Float,
    interactionSource: MutableInteractionSource,
) {
    Box(
        modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        val trackStrokeWidth: Float
        val tickRadius: Float
        val thumbRadius: Float
        val widthDp: Dp
        with(LocalDensity.current) {
            trackStrokeWidth = TrackHeight.toPx()
            tickRadius = TickRadius.toPx()
            thumbRadius = ThumbRadius.toPx()
            widthDp = width.toDp()
        }

        val thumbSize = ThumbRadius * 2
        val center = Modifier.align(Alignment.CenterStart)

        Track(
            center,
            colors,
            positionFraction,
            thumbRadius,
            trackStrokeWidth
        )

        Ticks(
            modifier = center,
            colors = colors,
            positionFractionEnd = positionFraction,
            tickFractions = tickFractions,
            thumbRadius,
            tickRadius
        )

        val thumbPosition = (widthDp - thumbSize) * positionFraction

        Thumb(
            center,
            thumbPosition,
            interactionSource,
            colors,
            enabled,
            thumbSize
        )

        if (!enabled && tutorialEnabled) {
            val tutorialEndPosition = remember(widthDp) { (widthDp - thumbSize) * 0.6f }

            ThumbTutorial(
                modifier = center,
                offset = thumbPosition,
                tutorialEndPosition = tutorialEndPosition,
                colors = colors,
                thumbSize = thumbSize
            )
        }
    }
}

@Composable
private fun Thumb(
    modifier: Modifier,
    position: Dp,
    interactionSource: MutableInteractionSource,
    colors: SunriseSliderColors,
    enabled: Boolean,
    thumbSize: Dp
) {
    Box(modifier.padding(start = position)) {
        val interactions = remember { mutableStateListOf<Interaction>() }
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> interactions.add(interaction)
                    is PressInteraction.Release -> interactions.remove(interaction.press)
                    is PressInteraction.Cancel -> interactions.remove(interaction.press)
                    is DragInteraction.Start -> interactions.add(interaction)
                    is DragInteraction.Stop -> interactions.remove(interaction.start)
                    is DragInteraction.Cancel -> interactions.remove(interaction.start)
                }
            }
        }

        val elevation = if (interactions.isNotEmpty()) {
            ThumbPressedElevation
        } else {
            ThumbDefaultElevation
        }

        Box(
            Modifier
                .size(thumbSize, thumbSize)
                .indication(
                    interactionSource = interactionSource,
                    indication = rememberRipple(bounded = false, radius = ThumbRippleRadius)
                )
                .shadow(if (enabled) elevation else 0.dp, CircleShape, clip = false)
                .background(colors.inThumbColor, CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colors.thumbColor(enabled).value)
                    .align(Alignment.Center)
            )

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(colors.inThumbColor)
                    .align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun Track(
    modifier: Modifier,
    colors: SunriseSliderColors,
    positionFractionEnd: Float,
    thumbRadius: Float,
    trackStrokeWidth: Float,
) {

    val activeTrackBrush = colors.trackBrush(active = true)
    val inactiveTrackBrush = colors.trackBrush(active = false)
    Canvas(
        modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        val sliderLeft = Offset(thumbRadius, center.y)
        val sliderRight = Offset(size.width - thumbRadius, center.y)

        drawLine(
            inactiveTrackBrush.value,
            sliderLeft,
            sliderRight,
            trackStrokeWidth,
            StrokeCap.Round
        )
    }

    val paddingInDp = with(LocalDensity.current) {
        thumbRadius.toDp()
    }

    Canvas(
        modifier
            .padding(start = paddingInDp, end = paddingInDp)
            .fillMaxWidth(fraction = positionFractionEnd)
            .wrapContentHeight()
    ) {
        val sliderLeft = Offset(0f, center.y)
        val sliderRight = Offset(size.width, center.y)
        drawLine(
            activeTrackBrush.value,
            sliderLeft,
            sliderRight,
            trackStrokeWidth,
            StrokeCap.Round,
        )
    }
}

@Composable
private fun Ticks(
    modifier: Modifier,
    colors: SunriseSliderColors,
    positionFractionEnd: Float,
    tickFractions: List<Float>,
    thumbRadius: Float,
    tickRadius: Float
) {

    val inactiveColor = colors.tickColor(active = false)
    val activeColor = colors.tickColor(active = true)

    Canvas(
        modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        val isRtl = layoutDirection == LayoutDirection.Rtl
        val sliderLeft = Offset(thumbRadius, center.y)
        val sliderRight = Offset(size.width - thumbRadius, center.y)
        val sliderStart = if (isRtl) sliderRight else sliderLeft
        val sliderEnd = if (isRtl) sliderLeft else sliderRight

        tickFractions.groupBy { it > positionFractionEnd }.forEach { (afterFraction, list) ->
            drawPoints(
                points = list.map {
                    Offset(lerp(sliderStart, sliderEnd, it).x, center.y)
                },
                pointMode = PointMode.Points,
                color = (if (afterFraction) inactiveColor else activeColor).value,
                tickRadius * 2,
                StrokeCap.Round
            )
        }
    }
}

@Composable
private fun ThumbTutorial(
    modifier: Modifier,
    offset: Dp,
    tutorialEndPosition: Dp,
    colors: SunriseSliderColors,
    thumbSize: Dp
) {

    val infiniteTransition = rememberInfiniteTransition()
    val positionAndAlpha by infiniteTransition.animateValue(
        initialValue = Pair(offset, 1f),
        targetValue = Pair(tutorialEndPosition, 0f),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        typeConverter = TwoWayConverter(
            convertToVector = { positionAndAlpha: Pair<Dp, Float> ->
                AnimationVector2D(positionAndAlpha.first.value, positionAndAlpha.second)
            },
            convertFromVector = { vector: AnimationVector2D ->
                Pair(vector.v1.dp, vector.v2)
            }
        )
    )

    Box(
        modifier
            .padding(start = positionAndAlpha.first)
            .alpha(positionAndAlpha.second)
    ) {
        Box(
            Modifier
                .size(thumbSize, thumbSize)
                .shadow(ThumbDefaultElevation, CircleShape, clip = false)
                .background(Color.White, CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colors.thumbColor(false).value)
                    .align(Alignment.Center)
            ) {
                Icon(
                    painterResource(id = R.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = colors.inThumbColor,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                )
            }
        }
    }
}

private fun stepsToTickFractions(steps: Int): List<Float> {
    return if (steps == 0) emptyList() else List(steps) { it.toFloat() / (steps-1) }
}

private fun valueRangeToDiscreteValues(
    valueRange: ClosedFloatingPointRange<Float>?,
    steps: Int = 0
): List<Float> {
    if (valueRange == null) {
        throw RuntimeException("Slider must have a range or a list of values")
    }
    return stepsToTickFractions(steps).map { (valueRange.start) + (valueRange.endInclusive - valueRange.start) * it }
}

private fun valuesToTickFractions(
    values: List<Float>,
    valueRange: ClosedFloatingPointRange<Float>
): List<Float> {
    return values.map { (it - valueRange.start) / (valueRange.endInclusive - valueRange.start) }
}

// Scale x1 from a1..b1 range to a2..b2 range
private fun scale(a1: Float, b1: Float, x1: Float, a2: Float, b2: Float) =
    lerp(a2, b2, calcFraction(a1, b1, x1))

// Calculate the 0..1 fraction that `pos` value represents between `a` and `b`
private fun calcFraction(a: Float, b: Float, pos: Float) =
    (if (b - a == 0f) 0f else (pos - a) / (b - a)).coerceIn(0f, 1f)

// Internal to be referred to in tests
private val ThumbRadius = 18.dp
private val ThumbRippleRadius = 24.dp
private val ThumbDefaultElevation = 4.dp
private val ThumbPressedElevation = 6.dp

// Internal to be referred to in tests
internal val TrackHeight = 24.dp

internal val TickRadius = 4.dp

@Preview
@Composable
private fun SunriseSliderPreview() {

    val sunriseSliderColors = SunriseSliderColors(
        thumbColor = Color.Blue,
        thumbDisabledColor = Color.Gray,
        inThumbColor = Color.Blue,
        trackBrush = Brush.horizontalGradient(
            listOf(
                Color.Blue, Color(0xFF979797)
            ),
            tileMode = TileMode.Clamp
        ),
        inactiveTrackColor = Color.Blue.copy(alpha = 0.1f),
        tickActiveColor = Color.Blue,
        tickInactiveColor = Color.Blue.copy(alpha = 0.3f)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(8.dp)
            .background(Color.White)

    ) {
        SunriseSlider(
            value = 0f,
            values = listOf(0.2f, 0.4f, 1f),
            colors = sunriseSliderColors,
            valueRangeParam = 0f..1f
        )
    }
}
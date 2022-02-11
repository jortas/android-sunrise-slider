package com.jortas.sunriseslider.slider

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor


@Immutable
data class SunriseSliderColors(
    private val thumbColor: Color = Color.Unspecified,
    private val thumbDisabledColor: Color = Color.Unspecified,
    val inThumbColor: Color = Color.Unspecified,

    private val trackColor: Color = Color.Unspecified,
    private val trackBrush: Brush = SolidColor(trackColor),
    private val inactiveTrackColor: Color = Color.Unspecified,
    private val inactiveTrackBrush: Brush = SolidColor(inactiveTrackColor),

    private val tickActiveColor: Color = Color.Unspecified,
    private val tickInactiveColor: Color = Color.Unspecified,
) {

    @Composable
    fun thumbColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) thumbColor else thumbDisabledColor)
    }

    @Composable
    fun trackBrush(active: Boolean): State<Brush> {
        return rememberUpdatedState(
            if (active) trackBrush else inactiveTrackBrush
        )
    }

    @Composable
    fun tickColor(active: Boolean): State<Color> {
        return rememberUpdatedState(
            if (active) tickActiveColor else tickInactiveColor
        )
    }
}
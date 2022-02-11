package com.jortas.sunriseslider

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jortas.sunriseslider.slider.SunriseSlider
import com.jortas.sunriseslider.slider.SunriseSliderColors

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val composeView = this.findViewById<ComposeView>(R.id.composeView)
        val sunriseSliderColors = sunriseSliderColorsDefault()

        composeView.setContent {

            var enabledValue by remember { mutableStateOf(false) }
            var value by remember { mutableStateOf(-0.5f) }
            var values by remember { mutableStateOf(valuesList()) }
            var steps by remember { mutableStateOf(0) }
            var valueRange: ClosedFloatingPointRange<Float>? by remember { mutableStateOf(null) }
            val onValueChangeFinished by remember { mutableStateOf({}) }
            val tutorialEnabled by remember { mutableStateOf(true) }

            Column() {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(8.dp)
                ) {

                    val interactionSource = remember {
                        MutableInteractionSource()
                    }

                    val wasPressed = interactionSource.collectIsPressedAsState()

                    if (!enabledValue && wasPressed.value) {
                        enabledValue = true
                    }

                    SunriseSlider(
                        value = value,
                        onValueChange = { float: Float ->
                            value = float
                            onValueChangeFinished()
                            value = float
                        },
                        valueRangeParam = valueRange,
                        values = values,
                        steps = steps,
                        interactionSource = interactionSource,
                        enabled = enabledValue,
                        tutorialEnabled = tutorialEnabled,
                        onValueChangeFinished = onValueChangeFinished,
                        colors = sunriseSliderColors
                    )
                }

                //AUXILIARY MODIFIERS

                Row(
                    Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "VALUE: $value",
                        fontSize = 20.sp
                    )
                }

                Row(
                    Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    TextField(
                        value = "${valueRange?.start ?: ""}",
                        onValueChange = {
                            valueRange = it.toFloat()..(valueRange?.endInclusive ?: it.toFloat())
                        },
                        label = { Text("Min Range") }
                    )
                }

                Row(
                    Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    TextField(
                        value = "${valueRange?.endInclusive ?: ""}",
                        onValueChange = {
                            valueRange = (valueRange?.start ?: it.toFloat())..it.toFloat()
                        },
                        label = { Text("Max Range") }
                    )
                }

                Row(
                    Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    TextField(
                        value = "${steps ?: ""}",
                        onValueChange = {
                            steps = it.toInt()
                        },
                        label = { Text("Steps") }
                    )
                }

                Row(
                    Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Checkbox(
                        checked = values.isNotEmpty(),
                        onCheckedChange = {
                            values = if (it) valuesList() else emptyList()
                        }
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    Text("Enable discrete values ${valuesList()}")
                }

            }
        }
    }

    private fun valuesList() = listOf(-0.5f, 0.1f, 0.4f, 0.5f, 1f, 2f)

    private fun sunriseSliderColorsDefault() = SunriseSliderColors(
        thumbColor = ProjColors.lightBlue,
        thumbDisabledColor = ProjColors.silver,
        inThumbColor = ProjColors.white,
        trackBrush = Brush.horizontalGradient(
            listOf(
                ProjColors.lightBlue,
                ProjColors.grayish
            ),
            tileMode = TileMode.Clamp
        ),
        inactiveTrackColor = ProjColors.lightBlue.copy(alpha = 0.1f),
        tickActiveColor = ProjColors.white,
        tickInactiveColor = ProjColors.lightBlue.copy(alpha = 0.3f)
    )
}
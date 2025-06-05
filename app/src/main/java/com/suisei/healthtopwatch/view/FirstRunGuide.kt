package com.suisei.healthtopwatch.view

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import com.fappslab.tourtip.compose.extension.tooltipAnchor
import com.fappslab.tourtip.model.HighlightType
import com.fappslab.tourtip.model.TooltipModel
import com.suisei.healthtopwatch.model.setFirstRunDone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun Modifier.intervalGuide() = tooltipAnchor {
    TooltipModel(
        index = 0,
        title = { Text("Step 1") },
        message = { Text("You can ") },
        highlightType = HighlightType.Rounded
    )
}

fun Modifier.intervalDuringGuide() = tooltipAnchor {
    TooltipModel(
        index = 1,
        title = { Text("Step 1") },
        message = { Text("You can ") },
        highlightType = HighlightType.Rounded
    )
}

fun Modifier.pipGuide() = tooltipAnchor { // Attach the tooltip to any composable component.
    TooltipModel(
        index = 2,
        title = { Text("Step 1") },
        message = { Text("This is the first step of the tour.") },
        highlightType = HighlightType.Rounded
    )
}

fun Modifier.iconGuide(context: Context) = tooltipAnchor { // Attach the tooltip to any composable component.
    TooltipModel(
        index = 3,
        title = { Text("Step 1") },
        message = { Text("This is the first step of the tour.") },
        highlightType = HighlightType.Rounded,
        action = { controller ->
            TextButton(onClick = {
                CoroutineScope(Dispatchers.IO).launch {

                    context.setFirstRunDone()
                }
                controller.finishTourtip()}) { Text("Finish") }
        }
    )
}
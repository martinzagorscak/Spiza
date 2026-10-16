package com.mzagorscak.spiza.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.zIndex
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import org.jetbrains.compose.resources.painterResource
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.ic_grip_dots

@Composable
fun Draggable(
    offsetY: Float,
    isDragging: Boolean,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer { translationY = offsetY }
            .background(color = MaterialTheme.colorScheme.background),
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_grip_dots),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .draggable(
                    state = rememberDraggableState(onDelta = onDrag),
                    orientation = Orientation.Vertical,
                    onDragStarted = { onDragStart() },
                    onDragStopped = { onDragEnd() },
                ),
        )
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

@Composable
private fun DraggableItemsLayoutPreview() {
    val items = remember { mutableStateListOf("Milk", "Eggs", "Bread", "Butter", "Apples", "Coffee") }
    val listState = rememberLazyListState()
    var draggedKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    LazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
        items(items, key = { it }) { item ->
            val isDragging = draggedKey == item
            Draggable(
                offsetY = if (isDragging) dragOffset else 0f,
                isDragging = isDragging,
                onDragStart = {
                    draggedKey = item
                    dragOffset = 0f
                },
                onDrag = { dy ->
                    dragOffset += dy
                    val infos = listState.layoutInfo.visibleItemsInfo
                    val current = infos.firstOrNull { it.key == item }
                    if (current != null) {
                        val center = current.offset + current.size / 2f + dragOffset
                        val currentTop = current.offset
                        val currentBottom = currentTop + current.size
                        if (center < currentTop || center > currentBottom) {
                            val target = infos.firstOrNull {
                                it.key != item && center >= it.offset && center <= it.offset + it.size
                            }
                            if (target != null) {
                                items.add(target.index, items.removeAt(current.index))
                                dragOffset += current.offset - target.offset
                            }
                        }
                    }
                },
                onDragEnd = {
                    draggedKey = null
                    dragOffset = 0f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isDragging) Modifier else Modifier.animateItem()),
            ) {
                EditableCheckListItem(
                    text = item,
                    checked = false,
                    onTextChange = {},
                    onCheckedChange = {},
                    onDelete = {},
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun DraggableItemsPreviewLightMode() {
    TheAppTheme {
        DraggableItemsLayoutPreview()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun DraggableItemsPreviewDarkMode() {
    TheAppTheme(darkTheme = true) {
        DraggableItemsLayoutPreview()
    }
}

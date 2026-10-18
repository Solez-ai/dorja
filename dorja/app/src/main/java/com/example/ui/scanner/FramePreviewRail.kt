package com.example.ui.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun FramePreviewRail(
    framePaths: List<String>,
    selectedIndex: Int = -1,
    onFrameSelected: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .height(100.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(framePaths) { index, path ->
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(80.dp)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (selectedIndex == index)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        else
                            Color.Gray
                    )
                    .clickable { onFrameSelected(index) }
                    .semantics {
                        contentDescription = "Frame $${index + 1}"
                    },
                contentAlignment = Alignment.Center
            ) {
                if (path.isNotEmpty()) {
                    AsyncImage(
                        model = path,
                        contentDescription = "Frame thumbnail $index",
                        modifier = Modifier.matchParentSize()
                    )
                } else {
                    Text(
                        text = "$${index + 1}",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

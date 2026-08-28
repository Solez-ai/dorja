package com.example.ui.tour

import androidx.compose.runtime.Composable

/**
 * TourViewerScreen now delegates to the full PanoramaViewerScreen.
 * The panorama viewer handles room selection, 360° cylindrical display,
 * gyro panning, drag-to-pan, and gradient overlays.
 */
@Composable
fun TourViewerScreen(
    listingId: String,
    onBack: () -> Unit
) {
    PanoramaViewerScreen(
        listingId = listingId,
        onBack = onBack
    )
}

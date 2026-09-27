package com.example.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.DorjaColors
import com.slapps.cupertino.CupertinoActivityIndicator
import com.slapps.cupertino.CupertinoAlertDialog
import com.slapps.cupertino.ExperimentalCupertinoApi
import com.slapps.cupertino.cancel
import com.slapps.cupertino.default
import com.slapps.cupertino.destructive

/**
 * iOS-style activity indicator (segmented spinner) tinted with the DORJA palette.
 * Drop-in replacement for CircularProgressIndicator.
 */
@OptIn(ExperimentalCupertinoApi::class)
@Composable
fun DorjaActivityIndicator(
    modifier: Modifier = Modifier,
    color: Color = DorjaColors.Jol600
) {
    CupertinoActivityIndicator(modifier = modifier, color = color)
}

/**
 * iOS-style alert for simple confirm dialogs (title + message + actions).
 *
 * Complex form dialogs still use Material3 AlertDialog until they migrate
 * to Cupertino sheets; this adapter covers the plain confirm/cancel pattern.
 *
 * @param isDestructive renders the confirm action in iOS red (destructive style)
 */
@OptIn(ExperimentalCupertinoApi::class)
@Composable
fun DorjaAlert(
    onDismissRequest: () -> Unit,
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    isDestructive: Boolean = false,
    dismissLabel: String? = null,
    onDismiss: () -> Unit = onDismissRequest
) {
    CupertinoAlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        message = { Text(message) },
        buttons = {
            if (dismissLabel != null) {
                cancel(onClick = onDismiss, title = { Text(dismissLabel) })
            }
            if (isDestructive) {
                destructive(onClick = onConfirm, title = { Text(confirmLabel) })
            } else {
                default(onClick = onConfirm, title = { Text(confirmLabel) })
            }
        }
    )
}

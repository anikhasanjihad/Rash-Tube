package com.ahr.rashtube.ui.screens.onboarding

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ahr.rashtube.R
import com.ahr.rashtube.data.backup.ImportKind
import com.ahr.rashtube.data.backup.ImportSource
import com.ahr.rashtube.ui.components.shared.RashNavRow
import com.ahr.rashtube.ui.components.shared.RashRowGroup
import com.ahr.rashtube.ui.components.shared.RashSheetHeader
import com.ahr.rashtube.ui.components.shared.flowRowGroupShape
import com.ahr.rashtube.ui.components.shared.rememberFlowSheetState

private val SheetBottomPadding = 16.dp

/** Everything [source] can import, the same list Settings shows under that app. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImportSourceSheet(
    source: ImportSource,
    importProfileName: String?,
    onImport: (ImportKind) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberFlowSheetState(),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        RashSheetHeader(
            title = stringResource(source.titleRes),
            onClose = onDismiss,
            showDragHandle = false,
        )
        RashRowGroup(Modifier.verticalScroll(rememberScrollState()).padding(bottom = SheetBottomPadding)) {
            source.kinds.forEachIndexed { index, kind ->
                RashNavRow(
                    title = stringResource(kind.titleRes),
                    // Outside Default, say that a master backup reaches every profile it holds, not just this one.
                    supportingText =
                        if (kind == ImportKind.MASTER && importProfileName != null) {
                            stringResource(R.string.onboarding_import_master_profile)
                        } else {
                            stringResource(kind.descriptionRes)
                        },
                    leadingIcon = Icons.Outlined.FileOpen,
                    showChevron = false,
                    shape = flowRowGroupShape(index, source.kinds.size),
                    onClick = { onImport(kind) },
                )
            }
        }
    }
}

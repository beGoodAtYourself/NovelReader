package io.legado.app.ui.config.backupConfig

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.legado.app.R
import io.legado.app.ui.widget.components.modalBottomSheet.OptionCard
import io.legado.app.ui.widget.components.modalBottomSheet.OptionSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupOptionSheet(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onBackupToLocal: () -> Unit,
) {
    OptionSheet(
        show = show,
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.backup),
    ) {
        OptionCard(
            icon = Icons.Default.PhoneAndroid,
            text = stringResource(R.string.backup_to_local),
            onClick = onBackupToLocal,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreOptionSheet(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onRestoreFromLocal: () -> Unit,
) {
    OptionSheet(
        show = show,
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.restore),
    ) {
        OptionCard(
            icon = Icons.Default.PhoneAndroid,
            text = stringResource(R.string.restore_from_local),
            onClick = onRestoreFromLocal,
        )
    }
}

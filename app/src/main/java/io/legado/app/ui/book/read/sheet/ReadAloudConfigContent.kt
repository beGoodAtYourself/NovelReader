package io.legado.app.ui.book.read.sheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.domain.model.readaloud.ReadAloudSplitSymbol
import io.legado.app.feature.readaloud.battery.ReadAloudBatteryOptimizationRoute
import io.legado.app.feature.readaloud.overlay.ReadAloudOverlayPermissionRoute
import io.legado.app.ui.book.read.ReadBookIntent
import io.legado.app.ui.book.read.ReadBookUiState
import io.legado.app.ui.book.readaloud.player.ReadAloudPlayerIntent
import io.legado.app.ui.book.readaloud.player.ReadAloudPlayerUiState
import io.legado.app.ui.widget.components.modalBottomSheet.AppModalBottomSheet
import io.legado.app.ui.widget.components.settingItem.SliderSettingItem
import io.legado.app.ui.widget.components.settingItem.TinyClickableSettingItem
import io.legado.app.ui.widget.components.settingItem.TinySwitchSettingItem

@Composable
fun ReadAloudConfigContent(
    state: ReadBookUiState,
    playerState: ReadAloudPlayerUiState,
    onIntent: (ReadBookIntent) -> Unit,
    onPlayerIntent: (ReadAloudPlayerIntent) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * true 表示内容被整页宿主承载，数值项就地铺开成滑块；
     * false（默认）表示宿主是卡片弹层，数值项继续打开选择器弹层。
     */
    asPage: Boolean = false,
    /**
     * 卡片停在哪个 tab（0=常规，1=引擎与音色，2=角色与配乐）。宿主是窗口级浮层时这份状态
     * 必须存在浮层外面：压进整屏页（引擎与音色 / 角色与配乐里那几个跳转项）会把这层
     * composition 拆掉，`rememberPagerState` 的初值回到 0，用户看到的就是「从子项进去、
     * 回来落在常规」。
     */
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    /**
     * 是否展示「本章背景音乐 / AI 识别背景音乐」这两个正文内浮层入口。
     *
     * 它们发出的意图由阅读器 ViewModel 的分配域处理，听书播放页是 Activity 级浮层、
     * 没有这套宿主，传 false 以免留下点了没反应的死入口。
     */
    bgmSceneActionsEnabled: Boolean = true,
) {
    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
    ) {
        TinyClickableSettingItem(
            title = stringResource(R.string.local_reader_system_voice),
            description = stringResource(R.string.local_reader_offline_voice_summary),
            onClick = { onIntent(ReadBookIntent.OpenSystemTtsSettings) },
        )
        TinySwitchSettingItem(
            title = stringResource(R.string.ignore_audio_focus_title),
            checked = state.readAloudIgnoreAudioFocus,
            onCheckedChange = { onIntent(ReadBookIntent.SetReadAloudIgnoreAudioFocus(it)) },
        )
        TinySwitchSettingItem(
            title = stringResource(R.string.pause_read_aloud_while_phone_calls_title),
            checked = state.readAloudPauseOnPhoneCall,
            enabled = state.readAloudIgnoreAudioFocus,
            onCheckedChange = { onIntent(ReadBookIntent.SetReadAloudPauseOnPhoneCall(it)) },
        )
        TinySwitchSettingItem(
            title = stringResource(R.string.read_aloud_wake_lock),
            checked = state.readAloudWakeLock,
            onCheckedChange = { onIntent(ReadBookIntent.SetReadAloudWakeLock(it)) },
        )
        TinySwitchSettingItem(
            title = stringResource(R.string.show_read_aloud_capsule),
            checked = state.showReadAloudCapsule,
            onCheckedChange = { onIntent(ReadBookIntent.SetShowReadAloudCapsule(it)) },
        )
        if (state.showReadAloudCapsule) {
            ReadAloudOverlayPermissionRoute()
        }
        TinyClickableSettingItem(
            title = stringResource(R.string.tts_paragraph_interval),
            onClick = { onIntent(ReadBookIntent.OpenParagraphIntervalPicker) },
        )
        ReadAloudBatteryOptimizationRoute()
    }
}

/**
 * 整页宿主用的数值项：直接铺开滑块，不再叠一层选择器弹层。
 */
/**
 * 「按符号」划分方式的标点多选。
 *
 * 至少保留一个标点：全部取消会让「按符号」退化成整段，与用户刚选的划分方式矛盾。
 */
private fun symbolLabelRes(option: ReadAloudSplitSymbol): Int = when (option) {
    ReadAloudSplitSymbol.FullStop -> R.string.symbol_period
    ReadAloudSplitSymbol.Exclamation -> R.string.symbol_exclamation
    ReadAloudSplitSymbol.Question -> R.string.symbol_question
    ReadAloudSplitSymbol.Ellipsis -> R.string.symbol_ellipsis
    ReadAloudSplitSymbol.Semicolon -> R.string.symbol_semicolon
    ReadAloudSplitSymbol.Comma -> R.string.symbol_comma
    ReadAloudSplitSymbol.EnumerationComma -> R.string.symbol_enumeration_comma
    ReadAloudSplitSymbol.Colon -> R.string.symbol_colon
    ReadAloudSplitSymbol.Dot -> R.string.symbol_halfwidth_period
    ReadAloudSplitSymbol.Bang -> R.string.symbol_halfwidth_exclamation
    ReadAloudSplitSymbol.QuestionMark -> R.string.symbol_halfwidth_question
    ReadAloudSplitSymbol.HalfSemicolon -> R.string.symbol_halfwidth_semicolon
    ReadAloudSplitSymbol.HalfComma -> R.string.symbol_halfwidth_comma
    ReadAloudSplitSymbol.HalfColon -> R.string.symbol_halfwidth_colon
}

@Composable
fun ReadAloudNumberConfigSheet(
    show: Boolean,
    title: String,
    description: String,
    value: Int,
    defaultValue: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    AppModalBottomSheet(
        show = show,
        onDismissRequest = onDismissRequest,
        title = title,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            SliderSettingItem(
                title = title,
                description = description,
                value = value.toFloat(),
                defaultValue = defaultValue.toFloat(),
                valueRange = valueRange,
                onValueChange = { onValueChange(it.toInt()) },
            )
        }
    }
}

package com.mzagorscak.spiza.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import com.mzagorscak.spiza.ui.theme.padding200
import org.jetbrains.compose.resources.stringResource
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.checklist_item_placeholder
import spiza.shared.generated.resources.ic_close

private const val ALPHA_CHECKED = 0.5f
private const val ALPHA_UNCHECKED = 1f
private const val EMPTY = ""

@Composable
fun EditableCheckListItem(
    text: String,
    checked: Boolean,
    isCheckable: Boolean = true,
    onTextChange: (String) -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    singleLine: Boolean = true,
    placeholder: String = stringResource(Res.string.checklist_item_placeholder),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActions = KeyboardActions.Default,
    modifier: Modifier = Modifier,
) {
    TextField(
        text = text,
        onTextChange = onTextChange,
        appearance = TextFieldAppearance(
            variant = TextFieldVariant.Borderless,
            singleLine = singleLine,
            placeholder = placeholder,
            leadingIcon =
                if (isCheckable) {
                    { Checkbox(checked = checked, onCheckedChange = onCheckedChange) }
                } else {
                    null
                },
            trailingIcon = {
                if (text.isNotEmpty()) {
                    IconButton(
                        resource = Res.drawable.ic_close,
                        appearance = IconButtonAppearance(
                            variant = IconButtonVariant.IconBased,
                            size = IconButtonSize.Small,
                        ),
                        onClick = onDelete,
                    )
                }
            },
            readOnly = checked,
        ),
        keyboardOptions = keyboardOptions,
        onKeyboardAction = onKeyboardAction,
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (checked) ALPHA_CHECKED else ALPHA_UNCHECKED),
    )
}

@Composable
private fun EditableCheckListItemLayoutPreview() {
    var text by remember { mutableStateOf(EMPTY) }
    var checked by remember { mutableStateOf(false) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(padding200),
        modifier = Modifier.padding(padding200),
    ) {
        EditableCheckListItem(
            text = text,
            checked = checked,
            onTextChange = { text = it },
            onCheckedChange = { checked = it },
            onDelete = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun EditableCheckListItemPreviewLightMode() {
    TheAppTheme {
        EditableCheckListItemLayoutPreview()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun EditableCheckListItemPreviewDarkMode() {
    TheAppTheme(darkTheme = true) {
        EditableCheckListItemLayoutPreview()
    }
}

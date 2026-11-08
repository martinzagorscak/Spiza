package com.mzagorscak.spiza.ui.screens.createHousehold

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import com.mzagorscak.spiza.ui.components.Button
import com.mzagorscak.spiza.ui.components.ButtonAppearance
import com.mzagorscak.spiza.ui.components.ButtonSize
import com.mzagorscak.spiza.ui.components.ButtonVariant
import com.mzagorscak.spiza.ui.components.IconButton
import com.mzagorscak.spiza.ui.components.IconButtonAppearance
import com.mzagorscak.spiza.ui.components.IconButtonSize
import com.mzagorscak.spiza.ui.components.IconButtonVariant
import com.mzagorscak.spiza.ui.components.TextField
import com.mzagorscak.spiza.ui.components.TextFieldAppearance
import com.mzagorscak.spiza.ui.components.TextFieldVariant
import com.mzagorscak.spiza.ui.components.TopBar
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import com.mzagorscak.spiza.ui.theme.padding400
import org.jetbrains.compose.resources.stringResource
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.create_household_description_label
import spiza.shared.generated.resources.create_household_error
import spiza.shared.generated.resources.create_household_limit_reached
import spiza.shared.generated.resources.create_household_name_label
import spiza.shared.generated.resources.create_household_submit
import spiza.shared.generated.resources.create_household_title
import spiza.shared.generated.resources.edit_household_error
import spiza.shared.generated.resources.edit_household_submit
import spiza.shared.generated.resources.edit_household_title
import spiza.shared.generated.resources.error_quota_exceeded
import spiza.shared.generated.resources.ic_back

@Composable
fun CreateHouseholdScreen(
    state: CreateHouseholdState,
    callbacks: CreateHouseholdCallbacks,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(state.isCreated) {
        if (state.isCreated) callbacks.onCreated()
    }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(
                    if (state.isEditScenario) Res.string.edit_household_title else Res.string.create_household_title,
                ),
                leadingContent = {
                    IconButton(
                        resource = Res.drawable.ic_back,
                        onClick = callbacks.onBackClick,
                        appearance = IconButtonAppearance(
                            variant = IconButtonVariant.IconBased,
                            size = IconButtonSize.Medium,
                            tintColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->
        Column(
            verticalArrangement = Arrangement.spacedBy(padding400),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(padding400)
                .verticalScroll(rememberScrollState()),
        ) {
            TextField(
                text = state.name,
                onTextChange = callbacks.onNameChange,
                isError = state.hasError,
                appearance = TextFieldAppearance(
                    variant = TextFieldVariant.Outlined,
                    singleLine = true,
                    label = stringResource(Res.string.create_household_name_label),
                    supportingText = "${state.name.length}/${CreateHouseholdState.MAX_NAME_LENGTH}",
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            TextField(
                text = state.description,
                onTextChange = callbacks.onDescriptionChange,
                isError = state.hasError,
                appearance = TextFieldAppearance(
                    variant = TextFieldVariant.Outlined,
                    singleLine = false,
                    isOptional = true,
                    label = stringResource(Res.string.create_household_description_label),
                    supportingText = "${state.description.length}/${CreateHouseholdState.MAX_DESCRIPTION_LENGTH}",
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.hasError) {
                val errorState = state.errorState
                Text(
                    text = when {
                        errorState is CreateHouseholdState.ErrorState.HouseholdLimitReached ->
                            stringResource(Res.string.create_household_limit_reached, errorState.limit)
                        errorState == CreateHouseholdState.ErrorState.QuotaExceeded -> stringResource(Res.string.error_quota_exceeded)
                        state.isEditScenario -> stringResource(Res.string.edit_household_error)
                        else -> stringResource(Res.string.create_household_error)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                label = stringResource(
                    if (state.isEditScenario) Res.string.edit_household_submit else Res.string.create_household_submit,
                ),
                onClick = callbacks.onCreateClick,
                isEnabled = state.canSubmit,
                appearance = ButtonAppearance(
                    variant = ButtonVariant.Primary,
                    size = ButtonSize.Large,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

data class CreateHouseholdCallbacks(
    val onBackClick: () -> Unit,
    val onNameChange: (String) -> Unit,
    val onDescriptionChange: (String) -> Unit,
    val onCreateClick: () -> Unit,
    val onCreated: () -> Unit,
)

@Preview
@Composable
private fun CreateHouseholdScreenPreview() {
    TheAppTheme(darkTheme = true) {
        CreateHouseholdScreen(
            state = CreateHouseholdState(name = "Home", description = "Weekly groceries"),
            callbacks = CreateHouseholdCallbacks(
                onBackClick = {},
                onNameChange = {},
                onDescriptionChange = {},
                onCreateClick = {},
                onCreated = {},
            ),
        )
    }
}

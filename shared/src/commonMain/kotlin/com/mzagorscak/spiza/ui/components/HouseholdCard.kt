package com.mzagorscak.spiza.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.mzagorscak.spiza.ui.model.PresentableHousehold
import com.mzagorscak.spiza.ui.model.PresentableHouseholdMember
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import com.mzagorscak.spiza.ui.theme.padding100
import com.mzagorscak.spiza.ui.theme.padding200
import com.mzagorscak.spiza.ui.theme.roundedShape200
import org.jetbrains.compose.resources.stringResource
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.current_user
import spiza.shared.generated.resources.ic_close
import spiza.shared.generated.resources.ic_trash
import spiza.shared.generated.resources.member

private const val SINGLE_LINE_COUNT = 1

@Composable
fun HouseholdCard(
    household: PresentableHousehold,
    nameTrailingContent: @Composable (() -> Unit)? = null,
    onRemoveUserClick: (household: PresentableHousehold, member: PresentableHouseholdMember) -> Unit,
    modifier: Modifier = Modifier,
) {
    val householdOwnerId = household.members.firstOrNull { it.isOwner }?.id

    Column(
        modifier = modifier
            .clip(roundedShape200)
            .background(MaterialTheme.colorScheme.onBackground)
            .padding(padding200),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = padding100, end = padding200)
                .padding(bottom = padding200),
        ) {
            Text(
                text = household.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.background,
                maxLines = SINGLE_LINE_COUNT,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            nameTrailingContent?.let {
                it()
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        LazyColumn(modifier = Modifier.padding(top = padding200)) {
            items(items = household.members, key = PresentableHouseholdMember::id) { member ->
                // Show the remove user option only for owner removing other members or for non-owner removing themselves
                val showRemoveUserOption = (household.isCurrentUserOwner && householdOwnerId != member.id) ||
                    (household.isCurrentUserOwner.not() && member.isCurrentUser)

                HouseholdMemberItem(
                    showRemoveUserOption = showRemoveUserOption,
                    member = member,
                    onRemoveUserClick = {
                        householdOwnerId?.let {
                            onRemoveUserClick(
                                household,
                                member,
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun HouseholdMemberItem(
    showRemoveUserOption: Boolean,
    member: PresentableHouseholdMember,
    onRemoveUserClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListingItem(
        label = if (member.isCurrentUser) stringResource(Res.string.current_user) else member.name ?: stringResource(Res.string.member),
        onClick = null,
        leadingContent = {
            Avatar(
                photoUrl = member.photoUrl,
                size = AvatarSize.Small,
            )
        },
        trailingContent = {
            if (showRemoveUserOption) {
                IconButton(
                    resource = Res.drawable.ic_close,
                    appearance = IconButtonAppearance(
                        variant = IconButtonVariant.IconBased,
                        size = IconButtonSize.Medium,
                        tintColor = MaterialTheme.colorScheme.background,
                    ),
                    onClick = onRemoveUserClick,
                )
            }
        },
        // need to inverse the colors because it is the card background on a screen background
        backgroundColor = MaterialTheme.colorScheme.onBackground,
        onBackgroundColor = MaterialTheme.colorScheme.background,
        modifier = modifier,
    )
}

@Composable
private fun CardListLayoutPreview() {
    HouseholdCard(
        household = PresentableHousehold(
            id = "1",
            name = "Household 1",
            description = "Description 1",
            members = listOf(
                PresentableHouseholdMember(
                    id = "1",
                    name = "User 1",
                    photoUrl = null,
                    isOwner = true,
                    isCurrentUser = true,
                ),
                PresentableHouseholdMember(
                    id = "2",
                    name = "User 2",
                    photoUrl = null,
                    isOwner = false,
                    isCurrentUser = false,
                ),
                PresentableHouseholdMember(
                    id = "3",
                    name = "User 3",
                    photoUrl = null,
                    isOwner = false,
                    isCurrentUser = false,
                ),
            ),
            ownerId = "1",
            isCurrentUserOwner = true,
        ),
        nameTrailingContent = {
            IconButton(
                resource = Res.drawable.ic_trash,
                appearance = IconButtonAppearance(
                    variant = IconButtonVariant.IconBased,
                    size = IconButtonSize.Medium,
                ),
                onClick = {},
            )
        },
        onRemoveUserClick = { _, _ -> },
        modifier = Modifier.padding(padding200),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun CardListPreviewLightMode() {
    TheAppTheme {
        CardListLayoutPreview()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun CardListPreviewDarkMode() {
    TheAppTheme(darkTheme = true) {
        CardListLayoutPreview()
    }
}

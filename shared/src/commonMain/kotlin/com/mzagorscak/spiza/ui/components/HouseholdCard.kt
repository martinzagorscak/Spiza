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
import com.mzagorscak.spiza.ui.components.common.CTA
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import com.mzagorscak.spiza.ui.theme.padding100
import com.mzagorscak.spiza.ui.theme.padding200
import com.mzagorscak.spiza.ui.theme.roundedShape200
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.ic_close
import spiza.shared.generated.resources.ic_trash

private const val SINGLE_LINE_COUNT = 1

data class PresentableHouseholdMember(
    val id: String,
    val name: String,
    val photoUrl: String?,
    val cta: CTA?,
)

@Composable
fun HouseholdCard(
    name: String,
    nameTrailingContent: @Composable (() -> Unit)? = null,
    members: List<PresentableHouseholdMember>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(roundedShape200)
            .background(MaterialTheme.colorScheme.background)
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
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
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
            items(members) { member ->
                HouseholdMemberItem(member = member)
            }
        }
    }
}

@Composable
private fun HouseholdMemberItem(
    member: PresentableHouseholdMember,
    modifier: Modifier = Modifier,
) {
    ListingItem(
        label = member.name,
        onClick = null,
        leadingContent = {
            Avatar(
                photoUrl = member.photoUrl,
                size = AvatarSize.Medium,
            )
        },
        trailingContent = {
            member.cta?.let { cta ->
                cta.iconResource?.let { resource ->
                    IconButton(
                        resource = resource,
                        appearance = IconButtonAppearance(
                            variant = IconButtonVariant.IconBased,
                            size = IconButtonSize.Medium,
                        ),
                        onClick = cta.onClick,
                    )
                }
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun CardListLayoutPreview() {
    HouseholdCard(
        name = "Household Name",
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
        members = listOf(
            PresentableHouseholdMember(
                id = "1",
                name = "Member 1",
                photoUrl = null,
                cta = CTA(
                    label = "CTA 1",
                    iconResource = Res.drawable.ic_close,
                    onClick = {},
                ),
            ),
            PresentableHouseholdMember(
                id = "2",
                name = "Member 2",
                photoUrl = null,
                cta = CTA(
                    label = "CTA 2",
                    iconResource = Res.drawable.ic_close,
                    onClick = {},
                ),
            ),
        ),
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

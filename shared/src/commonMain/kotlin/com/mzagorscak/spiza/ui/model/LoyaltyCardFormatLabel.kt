package com.mzagorscak.spiza.ui.model

import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import org.jetbrains.compose.resources.StringResource
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.loyalty_cards_format_aztec
import spiza.shared.generated.resources.loyalty_cards_format_codabar
import spiza.shared.generated.resources.loyalty_cards_format_code_128
import spiza.shared.generated.resources.loyalty_cards_format_code_39
import spiza.shared.generated.resources.loyalty_cards_format_code_93
import spiza.shared.generated.resources.loyalty_cards_format_ean_13
import spiza.shared.generated.resources.loyalty_cards_format_ean_8
import spiza.shared.generated.resources.loyalty_cards_format_itf
import spiza.shared.generated.resources.loyalty_cards_format_pdf_417
import spiza.shared.generated.resources.loyalty_cards_format_qr_code
import spiza.shared.generated.resources.loyalty_cards_format_upc_a
import spiza.shared.generated.resources.loyalty_cards_format_upc_e

val LoyaltyCardFormat.label: StringResource
    get() = when (this) {
        LoyaltyCardFormat.Ean13 -> Res.string.loyalty_cards_format_ean_13
        LoyaltyCardFormat.Ean8 -> Res.string.loyalty_cards_format_ean_8
        LoyaltyCardFormat.UpcA -> Res.string.loyalty_cards_format_upc_a
        LoyaltyCardFormat.UpcE -> Res.string.loyalty_cards_format_upc_e
        LoyaltyCardFormat.Code128 -> Res.string.loyalty_cards_format_code_128
        LoyaltyCardFormat.Code39 -> Res.string.loyalty_cards_format_code_39
        LoyaltyCardFormat.Code93 -> Res.string.loyalty_cards_format_code_93
        LoyaltyCardFormat.Itf -> Res.string.loyalty_cards_format_itf
        LoyaltyCardFormat.Codabar -> Res.string.loyalty_cards_format_codabar
        LoyaltyCardFormat.QrCode -> Res.string.loyalty_cards_format_qr_code
        LoyaltyCardFormat.Aztec -> Res.string.loyalty_cards_format_aztec
        LoyaltyCardFormat.Pdf417 -> Res.string.loyalty_cards_format_pdf_417
    }

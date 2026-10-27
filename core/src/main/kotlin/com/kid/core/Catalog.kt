// Generated file. Do not edit.
package com.kid.core

import kotlinx.serialization.Serializable

/** A k-ID capability. `allCases` lists them all. */
@JvmInline
@Serializable
public value class PermissionName(public val rawValue: String) {
    override fun toString(): String = rawValue

    public companion object {
        public val MULTIPLAYER: PermissionName = PermissionName("multiplayer")
        public val LEADERBOARD_AND_RANKINGS: PermissionName = PermissionName("leaderboard-and-rankings")
        public val JOIN_GROUPS: PermissionName = PermissionName("join-groups")
        public val PUBLIC_PROFILE: PermissionName = PermissionName("public-profile")
        public val CUSTOM_AVATAR: PermissionName = PermissionName("custom-avatar")
        public val CUSTOM_USERNAME: PermissionName = PermissionName("custom-username")
        public val TEXT_CHAT_PRIVATE: PermissionName = PermissionName("text-chat-private")
        public val TEXT_CHAT_PUBLIC: PermissionName = PermissionName("text-chat-public")
        public val VOICE_CHAT: PermissionName = PermissionName("voice-chat")
        public val VIDEO_CHAT: PermissionName = PermissionName("video-chat")
        public val ONLINE_STATUS: PermissionName = PermissionName("online-status")
        public val PUBLIC_FRIEND_LIST: PermissionName = PermissionName("public-friend-list")
        public val SEND_ACCEPT_FRIEND_REQUESTS: PermissionName = PermissionName("send-accept-friend-requests")
        public val LINK_TO_THIRD_PARTY_CHAT: PermissionName = PermissionName("link-to-third-party-chat")
        public val VIRTUAL_EVENTS: PermissionName = PermissionName("virtual-events")
        public val PERSONALIZED_RECOMMENDATIONS: PermissionName = PermissionName("personalized-recommendations")
        public val TARGETED_ADS: PermissionName = PermissionName("targeted-ads")
        public val PROFILING: PermissionName = PermissionName("profiling")
        public val PUSH_NOTIFICATIONS: PermissionName = PermissionName("push-notifications")
        public val SHARE_TO_SOCIAL_MEDIA: PermissionName = PermissionName("share-to-social-media")
        public val DIRECT_MARKETING: PermissionName = PermissionName("direct-marketing")
        public val FORUMS: PermissionName = PermissionName("forums")
        public val IN_GAME_PURCHASES: PermissionName = PermissionName("in-game-purchases")
        public val LOOT_BOXES_PAID_COSMETIC_ONLY: PermissionName = PermissionName("loot-boxes-paid-cosmetic-only")
        public val LOOT_BOXES_PAID_GAMEPLAY_IMPACTING: PermissionName = PermissionName("loot-boxes-paid-gameplay-impacting")
        public val LOOT_BOXES_KOMPU_GACHA: PermissionName = PermissionName("loot-boxes-kompu-gacha")
        public val LOOT_BOXES_PAID_OPEN_LOOP: PermissionName = PermissionName("loot-boxes-paid-open-loop")
        public val SEND_GIFTS: PermissionName = PermissionName("send-gifts")
        public val SIMULATED_GAMBLING: PermissionName = PermissionName("simulated-gambling")
        public val VIRTUAL_PROPERTY_OWNERSHIP: PermissionName = PermissionName("virtual-property-ownership")
        public val CAMERA_ACCESS: PermissionName = PermissionName("camera-access")
        public val SHARE_GAME_CLIPS_SCREENSHOTS: PermissionName = PermissionName("share-game-clips-screenshots")
        public val PHOTO_VIDEO_SHARING: PermissionName = PermissionName("photo-video-sharing")
        public val REAL_TIME_LOCATION_SHARING: PermissionName = PermissionName("real-time-location-sharing")
        public val MODS: PermissionName = PermissionName("mods")
        public val GAMEPLAY_STREAMING: PermissionName = PermissionName("gameplay-streaming")
        public val GAMEPLAY_RECORDING: PermissionName = PermissionName("gameplay-recording")
        public val LINK_TO_THIRD_PARTY_STREAMING_APP: PermissionName = PermissionName("link-to-third-party-streaming-app")
        public val AI_CHAT: PermissionName = PermissionName("ai-chat")
        public val AI_VOICE_MODE: PermissionName = PermissionName("ai-voice-mode")
        public val AI_MEMORY: PermissionName = PermissionName("ai-memory")
        public val AI_MEDIA_GENERATION: PermissionName = PermissionName("ai-media-generation")
        public val AI_MODEL_TRAINING: PermissionName = PermissionName("ai-model-training")
        public val AI_COMPANION_CHATBOT: PermissionName = PermissionName("ai-companion-chatbot")
        public val AI_MEDIA_UPLOAD: PermissionName = PermissionName("ai-media-upload")
        public val AUGMENTED_REALITY: PermissionName = PermissionName("augmented-reality")
        public val MATURE_LANGUAGE: PermissionName = PermissionName("mature-language")
        public val MOTION_DATA: PermissionName = PermissionName("motion-data")
        public val RESTRICTED_ADVERTISING_CONTENT: PermissionName = PermissionName("restricted-advertising-content")

        public val allCases: List<PermissionName> = listOf(
            MULTIPLAYER,
            LEADERBOARD_AND_RANKINGS,
            JOIN_GROUPS,
            PUBLIC_PROFILE,
            CUSTOM_AVATAR,
            CUSTOM_USERNAME,
            TEXT_CHAT_PRIVATE,
            TEXT_CHAT_PUBLIC,
            VOICE_CHAT,
            VIDEO_CHAT,
            ONLINE_STATUS,
            PUBLIC_FRIEND_LIST,
            SEND_ACCEPT_FRIEND_REQUESTS,
            LINK_TO_THIRD_PARTY_CHAT,
            VIRTUAL_EVENTS,
            PERSONALIZED_RECOMMENDATIONS,
            TARGETED_ADS,
            PROFILING,
            PUSH_NOTIFICATIONS,
            SHARE_TO_SOCIAL_MEDIA,
            DIRECT_MARKETING,
            FORUMS,
            IN_GAME_PURCHASES,
            LOOT_BOXES_PAID_COSMETIC_ONLY,
            LOOT_BOXES_PAID_GAMEPLAY_IMPACTING,
            LOOT_BOXES_KOMPU_GACHA,
            LOOT_BOXES_PAID_OPEN_LOOP,
            SEND_GIFTS,
            SIMULATED_GAMBLING,
            VIRTUAL_PROPERTY_OWNERSHIP,
            CAMERA_ACCESS,
            SHARE_GAME_CLIPS_SCREENSHOTS,
            PHOTO_VIDEO_SHARING,
            REAL_TIME_LOCATION_SHARING,
            MODS,
            GAMEPLAY_STREAMING,
            GAMEPLAY_RECORDING,
            LINK_TO_THIRD_PARTY_STREAMING_APP,
            AI_CHAT,
            AI_VOICE_MODE,
            AI_MEMORY,
            AI_MEDIA_GENERATION,
            AI_MODEL_TRAINING,
            AI_COMPANION_CHATBOT,
            AI_MEDIA_UPLOAD,
            AUGMENTED_REALITY,
            MATURE_LANGUAGE,
            MOTION_DATA,
            RESTRICTED_ADVERTISING_CONTENT,
        )
    }
}

/** A country or subdivision code the jurisdiction picker offers. */
@JvmInline
@Serializable
public value class JurisdictionCode(public val rawValue: String) {
    override fun toString(): String = rawValue

    public companion object {
        public val AD: JurisdictionCode = JurisdictionCode("AD")
        public val AE: JurisdictionCode = JurisdictionCode("AE")
        public val AF: JurisdictionCode = JurisdictionCode("AF")
        public val AG: JurisdictionCode = JurisdictionCode("AG")
        public val AI: JurisdictionCode = JurisdictionCode("AI")
        public val AL: JurisdictionCode = JurisdictionCode("AL")
        public val AM: JurisdictionCode = JurisdictionCode("AM")
        public val AO: JurisdictionCode = JurisdictionCode("AO")
        public val AQ: JurisdictionCode = JurisdictionCode("AQ")
        public val AR: JurisdictionCode = JurisdictionCode("AR")
        public val AS: JurisdictionCode = JurisdictionCode("AS")
        public val AT: JurisdictionCode = JurisdictionCode("AT")
        public val AU: JurisdictionCode = JurisdictionCode("AU")
        public val AW: JurisdictionCode = JurisdictionCode("AW")
        public val AX: JurisdictionCode = JurisdictionCode("AX")
        public val AZ: JurisdictionCode = JurisdictionCode("AZ")
        public val BA: JurisdictionCode = JurisdictionCode("BA")
        public val BB: JurisdictionCode = JurisdictionCode("BB")
        public val BD: JurisdictionCode = JurisdictionCode("BD")
        public val BE: JurisdictionCode = JurisdictionCode("BE")
        public val BF: JurisdictionCode = JurisdictionCode("BF")
        public val BG: JurisdictionCode = JurisdictionCode("BG")
        public val BH: JurisdictionCode = JurisdictionCode("BH")
        public val BI: JurisdictionCode = JurisdictionCode("BI")
        public val BJ: JurisdictionCode = JurisdictionCode("BJ")
        public val BL: JurisdictionCode = JurisdictionCode("BL")
        public val BM: JurisdictionCode = JurisdictionCode("BM")
        public val BN: JurisdictionCode = JurisdictionCode("BN")
        public val BO: JurisdictionCode = JurisdictionCode("BO")
        public val BQ: JurisdictionCode = JurisdictionCode("BQ")
        public val BR: JurisdictionCode = JurisdictionCode("BR")
        public val BS: JurisdictionCode = JurisdictionCode("BS")
        public val BT: JurisdictionCode = JurisdictionCode("BT")
        public val BV: JurisdictionCode = JurisdictionCode("BV")
        public val BW: JurisdictionCode = JurisdictionCode("BW")
        public val BY: JurisdictionCode = JurisdictionCode("BY")
        public val BZ: JurisdictionCode = JurisdictionCode("BZ")
        public val CA: JurisdictionCode = JurisdictionCode("CA")
        public val CC: JurisdictionCode = JurisdictionCode("CC")
        public val CD: JurisdictionCode = JurisdictionCode("CD")
        public val CF: JurisdictionCode = JurisdictionCode("CF")
        public val CG: JurisdictionCode = JurisdictionCode("CG")
        public val CH: JurisdictionCode = JurisdictionCode("CH")
        public val CI: JurisdictionCode = JurisdictionCode("CI")
        public val CK: JurisdictionCode = JurisdictionCode("CK")
        public val CL: JurisdictionCode = JurisdictionCode("CL")
        public val CM: JurisdictionCode = JurisdictionCode("CM")
        public val CN: JurisdictionCode = JurisdictionCode("CN")
        public val CO: JurisdictionCode = JurisdictionCode("CO")
        public val CR: JurisdictionCode = JurisdictionCode("CR")
        public val CU: JurisdictionCode = JurisdictionCode("CU")
        public val CV: JurisdictionCode = JurisdictionCode("CV")
        public val CW: JurisdictionCode = JurisdictionCode("CW")
        public val CX: JurisdictionCode = JurisdictionCode("CX")
        public val CY: JurisdictionCode = JurisdictionCode("CY")
        public val CZ: JurisdictionCode = JurisdictionCode("CZ")
        public val DE: JurisdictionCode = JurisdictionCode("DE")
        public val DJ: JurisdictionCode = JurisdictionCode("DJ")
        public val DK: JurisdictionCode = JurisdictionCode("DK")
        public val DM: JurisdictionCode = JurisdictionCode("DM")
        public val DO: JurisdictionCode = JurisdictionCode("DO")
        public val DZ: JurisdictionCode = JurisdictionCode("DZ")
        public val EC: JurisdictionCode = JurisdictionCode("EC")
        public val EE: JurisdictionCode = JurisdictionCode("EE")
        public val EG: JurisdictionCode = JurisdictionCode("EG")
        public val EH: JurisdictionCode = JurisdictionCode("EH")
        public val ER: JurisdictionCode = JurisdictionCode("ER")
        public val ES: JurisdictionCode = JurisdictionCode("ES")
        public val ET: JurisdictionCode = JurisdictionCode("ET")
        public val FI: JurisdictionCode = JurisdictionCode("FI")
        public val FJ: JurisdictionCode = JurisdictionCode("FJ")
        public val FK: JurisdictionCode = JurisdictionCode("FK")
        public val FM: JurisdictionCode = JurisdictionCode("FM")
        public val FO: JurisdictionCode = JurisdictionCode("FO")
        public val FR: JurisdictionCode = JurisdictionCode("FR")
        public val GA: JurisdictionCode = JurisdictionCode("GA")
        public val GB: JurisdictionCode = JurisdictionCode("GB")
        public val GD: JurisdictionCode = JurisdictionCode("GD")
        public val GE: JurisdictionCode = JurisdictionCode("GE")
        public val GF: JurisdictionCode = JurisdictionCode("GF")
        public val GG: JurisdictionCode = JurisdictionCode("GG")
        public val GH: JurisdictionCode = JurisdictionCode("GH")
        public val GI: JurisdictionCode = JurisdictionCode("GI")
        public val GL: JurisdictionCode = JurisdictionCode("GL")
        public val GM: JurisdictionCode = JurisdictionCode("GM")
        public val GN: JurisdictionCode = JurisdictionCode("GN")
        public val GP: JurisdictionCode = JurisdictionCode("GP")
        public val GQ: JurisdictionCode = JurisdictionCode("GQ")
        public val GR: JurisdictionCode = JurisdictionCode("GR")
        public val GS: JurisdictionCode = JurisdictionCode("GS")
        public val GT: JurisdictionCode = JurisdictionCode("GT")
        public val GU: JurisdictionCode = JurisdictionCode("GU")
        public val GW: JurisdictionCode = JurisdictionCode("GW")
        public val GY: JurisdictionCode = JurisdictionCode("GY")
        public val HK: JurisdictionCode = JurisdictionCode("HK")
        public val HM: JurisdictionCode = JurisdictionCode("HM")
        public val HN: JurisdictionCode = JurisdictionCode("HN")
        public val HR: JurisdictionCode = JurisdictionCode("HR")
        public val HT: JurisdictionCode = JurisdictionCode("HT")
        public val HU: JurisdictionCode = JurisdictionCode("HU")
        public val ID: JurisdictionCode = JurisdictionCode("ID")
        public val IE: JurisdictionCode = JurisdictionCode("IE")
        public val IL: JurisdictionCode = JurisdictionCode("IL")
        public val IM: JurisdictionCode = JurisdictionCode("IM")
        public val IN: JurisdictionCode = JurisdictionCode("IN")
        public val IO: JurisdictionCode = JurisdictionCode("IO")
        public val IQ: JurisdictionCode = JurisdictionCode("IQ")
        public val IR: JurisdictionCode = JurisdictionCode("IR")
        public val IS: JurisdictionCode = JurisdictionCode("IS")
        public val IT: JurisdictionCode = JurisdictionCode("IT")
        public val JE: JurisdictionCode = JurisdictionCode("JE")
        public val JM: JurisdictionCode = JurisdictionCode("JM")
        public val JO: JurisdictionCode = JurisdictionCode("JO")
        public val JP: JurisdictionCode = JurisdictionCode("JP")
        public val KE: JurisdictionCode = JurisdictionCode("KE")
        public val KG: JurisdictionCode = JurisdictionCode("KG")
        public val KH: JurisdictionCode = JurisdictionCode("KH")
        public val KI: JurisdictionCode = JurisdictionCode("KI")
        public val KM: JurisdictionCode = JurisdictionCode("KM")
        public val KN: JurisdictionCode = JurisdictionCode("KN")
        public val KP: JurisdictionCode = JurisdictionCode("KP")
        public val KR: JurisdictionCode = JurisdictionCode("KR")
        public val KW: JurisdictionCode = JurisdictionCode("KW")
        public val KY: JurisdictionCode = JurisdictionCode("KY")
        public val KZ: JurisdictionCode = JurisdictionCode("KZ")
        public val LA: JurisdictionCode = JurisdictionCode("LA")
        public val LB: JurisdictionCode = JurisdictionCode("LB")
        public val LC: JurisdictionCode = JurisdictionCode("LC")
        public val LI: JurisdictionCode = JurisdictionCode("LI")
        public val LK: JurisdictionCode = JurisdictionCode("LK")
        public val LR: JurisdictionCode = JurisdictionCode("LR")
        public val LS: JurisdictionCode = JurisdictionCode("LS")
        public val LT: JurisdictionCode = JurisdictionCode("LT")
        public val LU: JurisdictionCode = JurisdictionCode("LU")
        public val LV: JurisdictionCode = JurisdictionCode("LV")
        public val LY: JurisdictionCode = JurisdictionCode("LY")
        public val MA: JurisdictionCode = JurisdictionCode("MA")
        public val MC: JurisdictionCode = JurisdictionCode("MC")
        public val MD: JurisdictionCode = JurisdictionCode("MD")
        public val ME: JurisdictionCode = JurisdictionCode("ME")
        public val MF: JurisdictionCode = JurisdictionCode("MF")
        public val MG: JurisdictionCode = JurisdictionCode("MG")
        public val MH: JurisdictionCode = JurisdictionCode("MH")
        public val MK: JurisdictionCode = JurisdictionCode("MK")
        public val ML: JurisdictionCode = JurisdictionCode("ML")
        public val MM: JurisdictionCode = JurisdictionCode("MM")
        public val MN: JurisdictionCode = JurisdictionCode("MN")
        public val MO: JurisdictionCode = JurisdictionCode("MO")
        public val MP: JurisdictionCode = JurisdictionCode("MP")
        public val MQ: JurisdictionCode = JurisdictionCode("MQ")
        public val MR: JurisdictionCode = JurisdictionCode("MR")
        public val MS: JurisdictionCode = JurisdictionCode("MS")
        public val MT: JurisdictionCode = JurisdictionCode("MT")
        public val MU: JurisdictionCode = JurisdictionCode("MU")
        public val MV: JurisdictionCode = JurisdictionCode("MV")
        public val MW: JurisdictionCode = JurisdictionCode("MW")
        public val MX: JurisdictionCode = JurisdictionCode("MX")
        public val MY: JurisdictionCode = JurisdictionCode("MY")
        public val MZ: JurisdictionCode = JurisdictionCode("MZ")
        public val NA: JurisdictionCode = JurisdictionCode("NA")
        public val NC: JurisdictionCode = JurisdictionCode("NC")
        public val NE: JurisdictionCode = JurisdictionCode("NE")
        public val NF: JurisdictionCode = JurisdictionCode("NF")
        public val NG: JurisdictionCode = JurisdictionCode("NG")
        public val NI: JurisdictionCode = JurisdictionCode("NI")
        public val NL: JurisdictionCode = JurisdictionCode("NL")
        public val NO: JurisdictionCode = JurisdictionCode("NO")
        public val NP: JurisdictionCode = JurisdictionCode("NP")
        public val NR: JurisdictionCode = JurisdictionCode("NR")
        public val NU: JurisdictionCode = JurisdictionCode("NU")
        public val NZ: JurisdictionCode = JurisdictionCode("NZ")
        public val OM: JurisdictionCode = JurisdictionCode("OM")
        public val PA: JurisdictionCode = JurisdictionCode("PA")
        public val PE: JurisdictionCode = JurisdictionCode("PE")
        public val PF: JurisdictionCode = JurisdictionCode("PF")
        public val PG: JurisdictionCode = JurisdictionCode("PG")
        public val PH: JurisdictionCode = JurisdictionCode("PH")
        public val PK: JurisdictionCode = JurisdictionCode("PK")
        public val PL: JurisdictionCode = JurisdictionCode("PL")
        public val PM: JurisdictionCode = JurisdictionCode("PM")
        public val PN: JurisdictionCode = JurisdictionCode("PN")
        public val PR: JurisdictionCode = JurisdictionCode("PR")
        public val PS: JurisdictionCode = JurisdictionCode("PS")
        public val PT: JurisdictionCode = JurisdictionCode("PT")
        public val PW: JurisdictionCode = JurisdictionCode("PW")
        public val PY: JurisdictionCode = JurisdictionCode("PY")
        public val QA: JurisdictionCode = JurisdictionCode("QA")
        public val RE: JurisdictionCode = JurisdictionCode("RE")
        public val RO: JurisdictionCode = JurisdictionCode("RO")
        public val RS: JurisdictionCode = JurisdictionCode("RS")
        public val RU: JurisdictionCode = JurisdictionCode("RU")
        public val RW: JurisdictionCode = JurisdictionCode("RW")
        public val SA: JurisdictionCode = JurisdictionCode("SA")
        public val SB: JurisdictionCode = JurisdictionCode("SB")
        public val SC: JurisdictionCode = JurisdictionCode("SC")
        public val SD: JurisdictionCode = JurisdictionCode("SD")
        public val SE: JurisdictionCode = JurisdictionCode("SE")
        public val SG: JurisdictionCode = JurisdictionCode("SG")
        public val SH: JurisdictionCode = JurisdictionCode("SH")
        public val SI: JurisdictionCode = JurisdictionCode("SI")
        public val SJ: JurisdictionCode = JurisdictionCode("SJ")
        public val SK: JurisdictionCode = JurisdictionCode("SK")
        public val SL: JurisdictionCode = JurisdictionCode("SL")
        public val SM: JurisdictionCode = JurisdictionCode("SM")
        public val SN: JurisdictionCode = JurisdictionCode("SN")
        public val SO: JurisdictionCode = JurisdictionCode("SO")
        public val SR: JurisdictionCode = JurisdictionCode("SR")
        public val SS: JurisdictionCode = JurisdictionCode("SS")
        public val ST: JurisdictionCode = JurisdictionCode("ST")
        public val SV: JurisdictionCode = JurisdictionCode("SV")
        public val SX: JurisdictionCode = JurisdictionCode("SX")
        public val SY: JurisdictionCode = JurisdictionCode("SY")
        public val SZ: JurisdictionCode = JurisdictionCode("SZ")
        public val TC: JurisdictionCode = JurisdictionCode("TC")
        public val TD: JurisdictionCode = JurisdictionCode("TD")
        public val TF: JurisdictionCode = JurisdictionCode("TF")
        public val TG: JurisdictionCode = JurisdictionCode("TG")
        public val TH: JurisdictionCode = JurisdictionCode("TH")
        public val TJ: JurisdictionCode = JurisdictionCode("TJ")
        public val TK: JurisdictionCode = JurisdictionCode("TK")
        public val TL: JurisdictionCode = JurisdictionCode("TL")
        public val TM: JurisdictionCode = JurisdictionCode("TM")
        public val TN: JurisdictionCode = JurisdictionCode("TN")
        public val TO: JurisdictionCode = JurisdictionCode("TO")
        public val TR: JurisdictionCode = JurisdictionCode("TR")
        public val TT: JurisdictionCode = JurisdictionCode("TT")
        public val TV: JurisdictionCode = JurisdictionCode("TV")
        public val TW: JurisdictionCode = JurisdictionCode("TW")
        public val TZ: JurisdictionCode = JurisdictionCode("TZ")
        public val UA: JurisdictionCode = JurisdictionCode("UA")
        public val UG: JurisdictionCode = JurisdictionCode("UG")
        public val UM: JurisdictionCode = JurisdictionCode("UM")
        public val US: JurisdictionCode = JurisdictionCode("US")
        public val UY: JurisdictionCode = JurisdictionCode("UY")
        public val UZ: JurisdictionCode = JurisdictionCode("UZ")
        public val VA: JurisdictionCode = JurisdictionCode("VA")
        public val VC: JurisdictionCode = JurisdictionCode("VC")
        public val VE: JurisdictionCode = JurisdictionCode("VE")
        public val VG: JurisdictionCode = JurisdictionCode("VG")
        public val VI: JurisdictionCode = JurisdictionCode("VI")
        public val VN: JurisdictionCode = JurisdictionCode("VN")
        public val VU: JurisdictionCode = JurisdictionCode("VU")
        public val WF: JurisdictionCode = JurisdictionCode("WF")
        public val WS: JurisdictionCode = JurisdictionCode("WS")
        public val YE: JurisdictionCode = JurisdictionCode("YE")
        public val YT: JurisdictionCode = JurisdictionCode("YT")
        public val ZA: JurisdictionCode = JurisdictionCode("ZA")
        public val ZM: JurisdictionCode = JurisdictionCode("ZM")
        public val ZW: JurisdictionCode = JurisdictionCode("ZW")
        public val AU_ACT: JurisdictionCode = JurisdictionCode("AU-ACT")
        public val AU_NSW: JurisdictionCode = JurisdictionCode("AU-NSW")
        public val AU_NT: JurisdictionCode = JurisdictionCode("AU-NT")
        public val AU_QLD: JurisdictionCode = JurisdictionCode("AU-QLD")
        public val AU_SA: JurisdictionCode = JurisdictionCode("AU-SA")
        public val AU_TAS: JurisdictionCode = JurisdictionCode("AU-TAS")
        public val AU_VIC: JurisdictionCode = JurisdictionCode("AU-VIC")
        public val AU_WA: JurisdictionCode = JurisdictionCode("AU-WA")
        public val CA_AB: JurisdictionCode = JurisdictionCode("CA-AB")
        public val CA_BC: JurisdictionCode = JurisdictionCode("CA-BC")
        public val CA_MB: JurisdictionCode = JurisdictionCode("CA-MB")
        public val CA_NB: JurisdictionCode = JurisdictionCode("CA-NB")
        public val CA_NS: JurisdictionCode = JurisdictionCode("CA-NS")
        public val CA_ON: JurisdictionCode = JurisdictionCode("CA-ON")
        public val CA_QC: JurisdictionCode = JurisdictionCode("CA-QC")
        public val CA_SK: JurisdictionCode = JurisdictionCode("CA-SK")
        public val PS_GZA: JurisdictionCode = JurisdictionCode("PS-GZA")
        public val PS_WBK: JurisdictionCode = JurisdictionCode("PS-WBK")
        public val AE_AZ: JurisdictionCode = JurisdictionCode("AE-AZ")
        public val AE_AJ: JurisdictionCode = JurisdictionCode("AE-AJ")
        public val AE_DU: JurisdictionCode = JurisdictionCode("AE-DU")
        public val AE_FU: JurisdictionCode = JurisdictionCode("AE-FU")
        public val AE_RK: JurisdictionCode = JurisdictionCode("AE-RK")
        public val AE_SH: JurisdictionCode = JurisdictionCode("AE-SH")
        public val AE_UQ: JurisdictionCode = JurisdictionCode("AE-UQ")
        public val GB_SCT: JurisdictionCode = JurisdictionCode("GB-SCT")
        public val US_AL: JurisdictionCode = JurisdictionCode("US-AL")
        public val US_AK: JurisdictionCode = JurisdictionCode("US-AK")
        public val US_AS: JurisdictionCode = JurisdictionCode("US-AS")
        public val US_AZ: JurisdictionCode = JurisdictionCode("US-AZ")
        public val US_AR: JurisdictionCode = JurisdictionCode("US-AR")
        public val US_CA: JurisdictionCode = JurisdictionCode("US-CA")
        public val US_CO: JurisdictionCode = JurisdictionCode("US-CO")
        public val US_CT: JurisdictionCode = JurisdictionCode("US-CT")
        public val US_DE: JurisdictionCode = JurisdictionCode("US-DE")
        public val US_DC: JurisdictionCode = JurisdictionCode("US-DC")
        public val US_FL: JurisdictionCode = JurisdictionCode("US-FL")
        public val US_GA: JurisdictionCode = JurisdictionCode("US-GA")
        public val US_GU: JurisdictionCode = JurisdictionCode("US-GU")
        public val US_HI: JurisdictionCode = JurisdictionCode("US-HI")
        public val US_ID: JurisdictionCode = JurisdictionCode("US-ID")
        public val US_IL: JurisdictionCode = JurisdictionCode("US-IL")
        public val US_IN: JurisdictionCode = JurisdictionCode("US-IN")
        public val US_IA: JurisdictionCode = JurisdictionCode("US-IA")
        public val US_KS: JurisdictionCode = JurisdictionCode("US-KS")
        public val US_KY: JurisdictionCode = JurisdictionCode("US-KY")
        public val US_LA: JurisdictionCode = JurisdictionCode("US-LA")
        public val US_ME: JurisdictionCode = JurisdictionCode("US-ME")
        public val US_MD: JurisdictionCode = JurisdictionCode("US-MD")
        public val US_MA: JurisdictionCode = JurisdictionCode("US-MA")
        public val US_MI: JurisdictionCode = JurisdictionCode("US-MI")
        public val US_MN: JurisdictionCode = JurisdictionCode("US-MN")
        public val US_MS: JurisdictionCode = JurisdictionCode("US-MS")
        public val US_MO: JurisdictionCode = JurisdictionCode("US-MO")
        public val US_MT: JurisdictionCode = JurisdictionCode("US-MT")
        public val US_NE: JurisdictionCode = JurisdictionCode("US-NE")
        public val US_NV: JurisdictionCode = JurisdictionCode("US-NV")
        public val US_NH: JurisdictionCode = JurisdictionCode("US-NH")
        public val US_NJ: JurisdictionCode = JurisdictionCode("US-NJ")
        public val US_NM: JurisdictionCode = JurisdictionCode("US-NM")
        public val US_NY: JurisdictionCode = JurisdictionCode("US-NY")
        public val US_NC: JurisdictionCode = JurisdictionCode("US-NC")
        public val US_ND: JurisdictionCode = JurisdictionCode("US-ND")
        public val US_MP: JurisdictionCode = JurisdictionCode("US-MP")
        public val US_OH: JurisdictionCode = JurisdictionCode("US-OH")
        public val US_OK: JurisdictionCode = JurisdictionCode("US-OK")
        public val US_OR: JurisdictionCode = JurisdictionCode("US-OR")
        public val US_PA: JurisdictionCode = JurisdictionCode("US-PA")
        public val US_PR: JurisdictionCode = JurisdictionCode("US-PR")
        public val US_RI: JurisdictionCode = JurisdictionCode("US-RI")
        public val US_SC: JurisdictionCode = JurisdictionCode("US-SC")
        public val US_SD: JurisdictionCode = JurisdictionCode("US-SD")
        public val US_TN: JurisdictionCode = JurisdictionCode("US-TN")
        public val US_TX: JurisdictionCode = JurisdictionCode("US-TX")
        public val US_UT: JurisdictionCode = JurisdictionCode("US-UT")
        public val US_VT: JurisdictionCode = JurisdictionCode("US-VT")
        public val US_VI: JurisdictionCode = JurisdictionCode("US-VI")
        public val US_VA: JurisdictionCode = JurisdictionCode("US-VA")
        public val US_WA: JurisdictionCode = JurisdictionCode("US-WA")
        public val US_WV: JurisdictionCode = JurisdictionCode("US-WV")
        public val US_WI: JurisdictionCode = JurisdictionCode("US-WI")
        public val US_WY: JurisdictionCode = JurisdictionCode("US-WY")

        public val allCases: List<JurisdictionCode> = listOf(
            AD,
            AE,
            AF,
            AG,
            AI,
            AL,
            AM,
            AO,
            AQ,
            AR,
            AS,
            AT,
            AU,
            AW,
            AX,
            AZ,
            BA,
            BB,
            BD,
            BE,
            BF,
            BG,
            BH,
            BI,
            BJ,
            BL,
            BM,
            BN,
            BO,
            BQ,
            BR,
            BS,
            BT,
            BV,
            BW,
            BY,
            BZ,
            CA,
            CC,
            CD,
            CF,
            CG,
            CH,
            CI,
            CK,
            CL,
            CM,
            CN,
            CO,
            CR,
            CU,
            CV,
            CW,
            CX,
            CY,
            CZ,
            DE,
            DJ,
            DK,
            DM,
            DO,
            DZ,
            EC,
            EE,
            EG,
            EH,
            ER,
            ES,
            ET,
            FI,
            FJ,
            FK,
            FM,
            FO,
            FR,
            GA,
            GB,
            GD,
            GE,
            GF,
            GG,
            GH,
            GI,
            GL,
            GM,
            GN,
            GP,
            GQ,
            GR,
            GS,
            GT,
            GU,
            GW,
            GY,
            HK,
            HM,
            HN,
            HR,
            HT,
            HU,
            ID,
            IE,
            IL,
            IM,
            IN,
            IO,
            IQ,
            IR,
            IS,
            IT,
            JE,
            JM,
            JO,
            JP,
            KE,
            KG,
            KH,
            KI,
            KM,
            KN,
            KP,
            KR,
            KW,
            KY,
            KZ,
            LA,
            LB,
            LC,
            LI,
            LK,
            LR,
            LS,
            LT,
            LU,
            LV,
            LY,
            MA,
            MC,
            MD,
            ME,
            MF,
            MG,
            MH,
            MK,
            ML,
            MM,
            MN,
            MO,
            MP,
            MQ,
            MR,
            MS,
            MT,
            MU,
            MV,
            MW,
            MX,
            MY,
            MZ,
            NA,
            NC,
            NE,
            NF,
            NG,
            NI,
            NL,
            NO,
            NP,
            NR,
            NU,
            NZ,
            OM,
            PA,
            PE,
            PF,
            PG,
            PH,
            PK,
            PL,
            PM,
            PN,
            PR,
            PS,
            PT,
            PW,
            PY,
            QA,
            RE,
            RO,
            RS,
            RU,
            RW,
            SA,
            SB,
            SC,
            SD,
            SE,
            SG,
            SH,
            SI,
            SJ,
            SK,
            SL,
            SM,
            SN,
            SO,
            SR,
            SS,
            ST,
            SV,
            SX,
            SY,
            SZ,
            TC,
            TD,
            TF,
            TG,
            TH,
            TJ,
            TK,
            TL,
            TM,
            TN,
            TO,
            TR,
            TT,
            TV,
            TW,
            TZ,
            UA,
            UG,
            UM,
            US,
            UY,
            UZ,
            VA,
            VC,
            VE,
            VG,
            VI,
            VN,
            VU,
            WF,
            WS,
            YE,
            YT,
            ZA,
            ZM,
            ZW,
            AU_ACT,
            AU_NSW,
            AU_NT,
            AU_QLD,
            AU_SA,
            AU_TAS,
            AU_VIC,
            AU_WA,
            CA_AB,
            CA_BC,
            CA_MB,
            CA_NB,
            CA_NS,
            CA_ON,
            CA_QC,
            CA_SK,
            PS_GZA,
            PS_WBK,
            AE_AZ,
            AE_AJ,
            AE_DU,
            AE_FU,
            AE_RK,
            AE_SH,
            AE_UQ,
            GB_SCT,
            US_AL,
            US_AK,
            US_AS,
            US_AZ,
            US_AR,
            US_CA,
            US_CO,
            US_CT,
            US_DE,
            US_DC,
            US_FL,
            US_GA,
            US_GU,
            US_HI,
            US_ID,
            US_IL,
            US_IN,
            US_IA,
            US_KS,
            US_KY,
            US_LA,
            US_ME,
            US_MD,
            US_MA,
            US_MI,
            US_MN,
            US_MS,
            US_MO,
            US_MT,
            US_NE,
            US_NV,
            US_NH,
            US_NJ,
            US_NM,
            US_NY,
            US_NC,
            US_ND,
            US_MP,
            US_OH,
            US_OK,
            US_OR,
            US_PA,
            US_PR,
            US_RI,
            US_SC,
            US_SD,
            US_TN,
            US_TX,
            US_UT,
            US_VT,
            US_VI,
            US_VA,
            US_WA,
            US_WV,
            US_WI,
            US_WY,
        )
    }
}

/** The English country names the jurisdiction picker starts from. */
@KIDInternalApi
public val COUNTRIES: List<JurisdictionOption> = listOf(
    JurisdictionOption("AD", "Andorra"),
    JurisdictionOption("AE", "United Arab Emirates"),
    JurisdictionOption("AF", "Afghanistan"),
    JurisdictionOption("AG", "Antigua and Barbuda"),
    JurisdictionOption("AI", "Anguilla"),
    JurisdictionOption("AL", "Albania"),
    JurisdictionOption("AM", "Armenia"),
    JurisdictionOption("AO", "Angola"),
    JurisdictionOption("AQ", "Antarctica"),
    JurisdictionOption("AR", "Argentina"),
    JurisdictionOption("AS", "American Samoa"),
    JurisdictionOption("AT", "Austria"),
    JurisdictionOption("AU", "Australia"),
    JurisdictionOption("AW", "Aruba"),
    JurisdictionOption("AX", "Aland Islands"),
    JurisdictionOption("AZ", "Azerbaijan"),
    JurisdictionOption("BA", "Bosnia and Herzegovina"),
    JurisdictionOption("BB", "Barbados"),
    JurisdictionOption("BD", "Bangladesh"),
    JurisdictionOption("BE", "Belgium"),
    JurisdictionOption("BF", "Burkina Faso"),
    JurisdictionOption("BG", "Bulgaria"),
    JurisdictionOption("BH", "Bahrain"),
    JurisdictionOption("BI", "Burundi"),
    JurisdictionOption("BJ", "Benin"),
    JurisdictionOption("BL", "Saint Barthelemy"),
    JurisdictionOption("BM", "Bermuda"),
    JurisdictionOption("BN", "Brunei Darussalam"),
    JurisdictionOption("BO", "Bolivia"),
    JurisdictionOption("BQ", "Bonaire, Sint Eustatius and Saba"),
    JurisdictionOption("BR", "Brazil"),
    JurisdictionOption("BS", "Bahamas"),
    JurisdictionOption("BT", "Bhutan"),
    JurisdictionOption("BV", "Bouvet Island"),
    JurisdictionOption("BW", "Botswana"),
    JurisdictionOption("BY", "Belarus"),
    JurisdictionOption("BZ", "Belize"),
    JurisdictionOption("CA", "Canada"),
    JurisdictionOption("CC", "Cocos (Keeling) Islands"),
    JurisdictionOption("CD", "Congo (Democratic Republic)"),
    JurisdictionOption("CF", "Central African Republic"),
    JurisdictionOption("CG", "Congo"),
    JurisdictionOption("CH", "Switzerland"),
    JurisdictionOption("CI", "Cote d'Ivoire"),
    JurisdictionOption("CK", "Cook Islands"),
    JurisdictionOption("CL", "Chile"),
    JurisdictionOption("CM", "Cameroon"),
    JurisdictionOption("CN", "China"),
    JurisdictionOption("CO", "Colombia"),
    JurisdictionOption("CR", "Costa Rica"),
    JurisdictionOption("CU", "Cuba"),
    JurisdictionOption("CV", "Cabo Verde"),
    JurisdictionOption("CW", "Curacao"),
    JurisdictionOption("CX", "Christmas Island"),
    JurisdictionOption("CY", "Cyprus"),
    JurisdictionOption("CZ", "Czechia"),
    JurisdictionOption("DE", "Germany"),
    JurisdictionOption("DJ", "Djibouti"),
    JurisdictionOption("DK", "Denmark"),
    JurisdictionOption("DM", "Dominica"),
    JurisdictionOption("DO", "Dominican Republic"),
    JurisdictionOption("DZ", "Algeria"),
    JurisdictionOption("EC", "Ecuador"),
    JurisdictionOption("EE", "Estonia"),
    JurisdictionOption("EG", "Egypt"),
    JurisdictionOption("EH", "Western Sahara"),
    JurisdictionOption("ER", "Eritrea"),
    JurisdictionOption("ES", "Spain"),
    JurisdictionOption("ET", "Ethiopia"),
    JurisdictionOption("FI", "Finland"),
    JurisdictionOption("FJ", "Fiji"),
    JurisdictionOption("FK", "Falkland Islands"),
    JurisdictionOption("FM", "Micronesia"),
    JurisdictionOption("FO", "Faroe Islands"),
    JurisdictionOption("FR", "France"),
    JurisdictionOption("GA", "Gabon"),
    JurisdictionOption("GB", "United Kingdom"),
    JurisdictionOption("GD", "Grenada"),
    JurisdictionOption("GE", "Georgia"),
    JurisdictionOption("GF", "French Guiana"),
    JurisdictionOption("GG", "Guernsey"),
    JurisdictionOption("GH", "Ghana"),
    JurisdictionOption("GI", "Gibraltar"),
    JurisdictionOption("GL", "Greenland"),
    JurisdictionOption("GM", "Gambia"),
    JurisdictionOption("GN", "Guinea"),
    JurisdictionOption("GP", "Guadeloupe"),
    JurisdictionOption("GQ", "Equatorial Guinea"),
    JurisdictionOption("GR", "Greece"),
    JurisdictionOption("GS", "South Georgia and the South Sandwich Islands"),
    JurisdictionOption("GT", "Guatemala"),
    JurisdictionOption("GU", "Guam"),
    JurisdictionOption("GW", "Guinea-Bissau"),
    JurisdictionOption("GY", "Guyana"),
    JurisdictionOption("HK", "Hong Kong"),
    JurisdictionOption("HM", "Heard Island and McDonald Islands"),
    JurisdictionOption("HN", "Honduras"),
    JurisdictionOption("HR", "Croatia"),
    JurisdictionOption("HT", "Haiti"),
    JurisdictionOption("HU", "Hungary"),
    JurisdictionOption("ID", "Indonesia"),
    JurisdictionOption("IE", "Ireland"),
    JurisdictionOption("IL", "Israel"),
    JurisdictionOption("IM", "Isle of Man"),
    JurisdictionOption("IN", "India"),
    JurisdictionOption("IO", "British Indian Ocean Territory"),
    JurisdictionOption("IQ", "Iraq"),
    JurisdictionOption("IR", "Iran"),
    JurisdictionOption("IS", "Iceland"),
    JurisdictionOption("IT", "Italy"),
    JurisdictionOption("JE", "Jersey"),
    JurisdictionOption("JM", "Jamaica"),
    JurisdictionOption("JO", "Jordan"),
    JurisdictionOption("JP", "Japan"),
    JurisdictionOption("KE", "Kenya"),
    JurisdictionOption("KG", "Kyrgyzstan"),
    JurisdictionOption("KH", "Cambodia"),
    JurisdictionOption("KI", "Kiribati"),
    JurisdictionOption("KM", "Comoros"),
    JurisdictionOption("KN", "Saint Kitts and Nevis"),
    JurisdictionOption("KP", "Korea (Democratic People's Republic)"),
    JurisdictionOption("KR", "Korea (Republic of)"),
    JurisdictionOption("KW", "Kuwait"),
    JurisdictionOption("KY", "Cayman Islands"),
    JurisdictionOption("KZ", "Kazakhstan"),
    JurisdictionOption("LA", "Lao People's Democratic Republic"),
    JurisdictionOption("LB", "Lebanon"),
    JurisdictionOption("LC", "Saint Lucia"),
    JurisdictionOption("LI", "Liechtenstein"),
    JurisdictionOption("LK", "Sri Lanka"),
    JurisdictionOption("LR", "Liberia"),
    JurisdictionOption("LS", "Lesotho"),
    JurisdictionOption("LT", "Lithuania"),
    JurisdictionOption("LU", "Luxembourg"),
    JurisdictionOption("LV", "Latvia"),
    JurisdictionOption("LY", "Libya"),
    JurisdictionOption("MA", "Morocco"),
    JurisdictionOption("MC", "Monaco"),
    JurisdictionOption("MD", "Moldova"),
    JurisdictionOption("ME", "Montenegro"),
    JurisdictionOption("MF", "Saint Martin (French part)"),
    JurisdictionOption("MG", "Madagascar"),
    JurisdictionOption("MH", "Marshall Islands"),
    JurisdictionOption("MK", "North Macedonia"),
    JurisdictionOption("ML", "Mali"),
    JurisdictionOption("MM", "Myanmar"),
    JurisdictionOption("MN", "Mongolia"),
    JurisdictionOption("MO", "Macao"),
    JurisdictionOption("MP", "Northern Mariana Islands"),
    JurisdictionOption("MQ", "Martinique"),
    JurisdictionOption("MR", "Mauritania"),
    JurisdictionOption("MS", "Montserrat"),
    JurisdictionOption("MT", "Malta"),
    JurisdictionOption("MU", "Mauritius"),
    JurisdictionOption("MV", "Maldives"),
    JurisdictionOption("MW", "Malawi"),
    JurisdictionOption("MX", "Mexico"),
    JurisdictionOption("MY", "Malaysia"),
    JurisdictionOption("MZ", "Mozambique"),
    JurisdictionOption("NA", "Namibia"),
    JurisdictionOption("NC", "New Caledonia"),
    JurisdictionOption("NE", "Niger"),
    JurisdictionOption("NF", "Norfolk Island"),
    JurisdictionOption("NG", "Nigeria"),
    JurisdictionOption("NI", "Nicaragua"),
    JurisdictionOption("NL", "Netherlands"),
    JurisdictionOption("NO", "Norway"),
    JurisdictionOption("NP", "Nepal"),
    JurisdictionOption("NR", "Nauru"),
    JurisdictionOption("NU", "Niue"),
    JurisdictionOption("NZ", "New Zealand"),
    JurisdictionOption("OM", "Oman"),
    JurisdictionOption("PA", "Panama"),
    JurisdictionOption("PE", "Peru"),
    JurisdictionOption("PF", "French Polynesia"),
    JurisdictionOption("PG", "Papua New Guinea"),
    JurisdictionOption("PH", "Philippines"),
    JurisdictionOption("PK", "Pakistan"),
    JurisdictionOption("PL", "Poland"),
    JurisdictionOption("PM", "Saint Pierre and Miquelon"),
    JurisdictionOption("PN", "Pitcairn"),
    JurisdictionOption("PR", "Puerto Rico"),
    JurisdictionOption("PS", "Palestine"),
    JurisdictionOption("PT", "Portugal"),
    JurisdictionOption("PW", "Palau"),
    JurisdictionOption("PY", "Paraguay"),
    JurisdictionOption("QA", "Qatar"),
    JurisdictionOption("RE", "Reunion"),
    JurisdictionOption("RO", "Romania"),
    JurisdictionOption("RS", "Serbia"),
    JurisdictionOption("RU", "Russian Federation"),
    JurisdictionOption("RW", "Rwanda"),
    JurisdictionOption("SA", "Saudi Arabia"),
    JurisdictionOption("SB", "Solomon Islands"),
    JurisdictionOption("SC", "Seychelles"),
    JurisdictionOption("SD", "Sudan"),
    JurisdictionOption("SE", "Sweden"),
    JurisdictionOption("SG", "Singapore"),
    JurisdictionOption("SH", "Saint Helena, Ascension and Tristan da Cunha"),
    JurisdictionOption("SI", "Slovenia"),
    JurisdictionOption("SJ", "Svalbard and Jan Mayen"),
    JurisdictionOption("SK", "Slovakia"),
    JurisdictionOption("SL", "Sierra Leone"),
    JurisdictionOption("SM", "San Marino"),
    JurisdictionOption("SN", "Senegal"),
    JurisdictionOption("SO", "Somalia"),
    JurisdictionOption("SR", "Suriname"),
    JurisdictionOption("SS", "South Sudan"),
    JurisdictionOption("ST", "Sao Tome and Principe"),
    JurisdictionOption("SV", "El Salvador"),
    JurisdictionOption("SX", "Sint Maarten (Dutch part)"),
    JurisdictionOption("SY", "Syrian Arab Republic"),
    JurisdictionOption("SZ", "Eswatini"),
    JurisdictionOption("TC", "Turks and Caicos Islands"),
    JurisdictionOption("TD", "Chad"),
    JurisdictionOption("TF", "French Southern Territories"),
    JurisdictionOption("TG", "Togo"),
    JurisdictionOption("TH", "Thailand"),
    JurisdictionOption("TJ", "Tajikistan"),
    JurisdictionOption("TK", "Tokelau"),
    JurisdictionOption("TL", "Timor-Leste"),
    JurisdictionOption("TM", "Turkmenistan"),
    JurisdictionOption("TN", "Tunisia"),
    JurisdictionOption("TO", "Tonga"),
    JurisdictionOption("TR", "Turkiye"),
    JurisdictionOption("TT", "Trinidad and Tobago"),
    JurisdictionOption("TV", "Tuvalu"),
    JurisdictionOption("TW", "Taiwan"),
    JurisdictionOption("TZ", "Tanzania"),
    JurisdictionOption("UA", "Ukraine"),
    JurisdictionOption("UG", "Uganda"),
    JurisdictionOption("UM", "United States Minor Outlying Islands"),
    JurisdictionOption("US", "United States"),
    JurisdictionOption("UY", "Uruguay"),
    JurisdictionOption("UZ", "Uzbekistan"),
    JurisdictionOption("VA", "Holy See"),
    JurisdictionOption("VC", "Saint Vincent and the Grenadines"),
    JurisdictionOption("VE", "Venezuela"),
    JurisdictionOption("VG", "Virgin Islands (British)"),
    JurisdictionOption("VI", "Virgin Islands (U.S.)"),
    JurisdictionOption("VN", "Viet Nam"),
    JurisdictionOption("VU", "Vanuatu"),
    JurisdictionOption("WF", "Wallis and Futuna"),
    JurisdictionOption("WS", "Samoa"),
    JurisdictionOption("YE", "Yemen"),
    JurisdictionOption("YT", "Mayotte"),
    JurisdictionOption("ZA", "South Africa"),
    JurisdictionOption("ZM", "Zambia"),
    JurisdictionOption("ZW", "Zimbabwe"),
)

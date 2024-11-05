package com.borda.zebra_rfid_reader_sdk.utils

import android.util.Log
import java.util.Locale

object RegionUtils {
    private val zebraRFD40SupportedRegions = mapOf(
        "AR" to "ARG", // Argentina
        "AU" to "AUS", // Australia
        "AT" to "AUT", // Austria
        "BE" to "BEL", // Belgium
        "BD" to "BGD", // Bangladesh
        "BG" to "BGR", // Bulgaria
        "BO" to "BOL", // Bolivia
        "BR" to "BRA", // Brazil
        "CH" to "CHE", // Switzerland
        "CL" to "CHL", // Chile
        "CN" to "CHN", // China
        "CO" to "COL", // Colombia
        "CR" to "CRI", // Costa Rica
        "CX" to "CXR", // Christmas Island
        "CY" to "CYP", // Cyprus
        "CZ" to "CZE", // Czech Republic
        "DK" to "DNK", // Denmark
        "EC" to "ECU", // Ecuador
        "ES" to "ESP", // Spain
        "EE" to "EST", // Estonia
        "US" to "FCC", // FCC region (USA and others under FCC rules)
        "FI" to "FIN", // Finland
        "FJ" to "FJI", // Fiji
        "FR" to "FRA", // France
        "GB" to "GBR", // United Kingdom
        "GT" to "GTM", // Guatemala
        "GU" to "GUM", // Guam
        "HK" to "HKG", // Hong Kong
        "HN" to "HND", // Honduras
        "HR" to "HRV", // Croatia
        "HU" to "HUN", // Hungary
        "ID" to "IDN", // Indonesia
        "IE" to "IRL", // Ireland
        "IS" to "ISL", // Iceland
        "KR" to "KOR", // South Korea
        "LA" to "LAO", // Laos
        "LI" to "LIE", // Liechtenstein
        "LT" to "LTU", // Lithuania
        "LV" to "LVA", // Latvia
        "MO" to "MAK", // Macao
        "MX" to "MEX", // Mexico
        "MP" to "MNP", // Northern Mariana Islands
        "MY" to "MYS", // Malaysia
        "NF" to "NFK", // Norfolk Island
        "NG" to "NGA", // Nigeria
        "NU" to "NIU", // Niue
        "NO" to "NOR", // Norway
        "NZ" to "NZL", // New Zealand
        "PA" to "PAN", // Panama
        "PE" to "PER", // Peru
        "PH" to "PHL", // Philippines
        "PT" to "PRT", // Portugal
        "PY" to "PRY", // Paraguay
        "RO" to "ROU", // Romania
        "SG" to "SGP", // Singapore
        "SV" to "SLV", // El Salvador
        "SK" to "SVK", // Slovakia
        "SI" to "SVN", // Slovenia
        "SE" to "SWE", // Sweden
        "TW" to "TWN", // Taiwan
        "UY" to "URY", // Uruguay
        "VE" to "VEN", // Venezuela
        "VI" to "VIR", // U.S. Virgin Islands
        "VN" to "VNM", // Vietnam
        "ZA" to "ZAF"  // South Africa
    )

    // Usage Example
    fun getDefaultRegion(): String {
        val currentRegion = Locale.getDefault().country
        Log.d("RegionUtils", "default region $currentRegion")
        val zebraRegion = zebraRFD40SupportedRegions[currentRegion]
        Log.d("RegionUtils", "zebra region $zebraRegion")
        return zebraRegion ?: "FRA"
    }

}
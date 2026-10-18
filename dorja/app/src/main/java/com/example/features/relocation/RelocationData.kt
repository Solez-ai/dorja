package com.example.features.relocation

data class RelocationDestination(
    val countryCode: String,
    val countryName: String,
    val region: String,
    val visaRequirements: String
)

object CountryRegistry {
    val countries = listOf(
        RelocationDestination("US", "United States", "North America", "ESTA/Visa"),
        RelocationDestination("CA", "Canada", "North America", "eTA/Visa"),
        RelocationDestination("UK", "United Kingdom", "Europe", "Visa"),
        RelocationDestination("AU", "Australia", "Oceania", "e-Visitor/Visa"),
        RelocationDestination("JP", "Japan", "Asia", "Visa Exemption/Visa"),
        RelocationDestination("SG", "Singapore", "Asia", "Visa Exemption/Visa"),
        RelocationDestination("AE", "United Arab Emirates", "Middle East", "Visa on Arrival/Visa"),
        RelocationDestination("DE", "Germany", "Europe", "Schengen Visa"),
        RelocationDestination("FR", "France", "Europe", "Schengen Visa"),
        RelocationDestination("NZ", "New Zealand", "Oceania", "Visa"),
        RelocationDestination("SG", "Singapore", "Asia", "Visa Exemption"),
        RelocationDestination("HK", "Hong Kong", "Asia", "Visa Exemption")
    )

    fun getCountry(code: String): RelocationDestination? {
        return countries.find { it.countryCode == code }
    }

    fun getAllCountries(): List<RelocationDestination> = countries

    fun searchCountries(query: String): List<RelocationDestination> {
        return countries.filter {
            it.countryName.contains(query, ignoreCase = true) ||
            it.countryCode.contains(query, ignoreCase = true)
        }
    }
}

data class RelocationMode(
    val isEnabled: Boolean = false,
    val selectedCountry: RelocationDestination? = null
)

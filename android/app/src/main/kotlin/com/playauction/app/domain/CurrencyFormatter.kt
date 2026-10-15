package com.playauction.app.domain

/**
 * CurrencyFormatter — exact Kotlin port of client/src/utils/playerUtils.js fmtCr
 *
 * Internal unit = "Lakhs" of primary currency.
 * INR: 100L = 1 Crore  → ₹2Cr, ₹50L
 * ZAR: 10L  = 1M ZAR   → R6.5M, R850K
 * USD: converts via fixed rates
 */
object CurrencyFormatter {

    data class CurrencyConfig(
        val symbol: String,
        val label: String,
        val unit: String,
        val name: String,
        val usdPerCr: Long
    )

    private val CONFIG = mapOf(
        "inr" to CurrencyConfig("₹", "INR", "Cr", "Indian Rupee", 120_000L),
        "zar" to CurrencyConfig("R",  "ZAR", "M",  "S.A. Rand",    540_000L),
        "usd" to CurrencyConfig("$",  "USD", "M",  "US Dollar",    120_000L)
    )

    fun format(lakhs: Double, currency: String = "inr", sourceCurrency: String = "inr"): String {
        val cfg = CONFIG[currency] ?: CONFIG["inr"]!!

        return when (currency) {
            "usd" -> {
                val usdRatePerCr = CONFIG[sourceCurrency]?.usdPerCr?.toDouble() ?: 120_000.0
                val totalDollars = (lakhs / 100.0) * usdRatePerCr
                val millions = totalDollars / 1_000_000.0
                if (millions >= 1) {
                    val formatted = String.format("%.2f", millions).trimEnd('0').trimEnd('.')
                    "\$${formatted}M"
                } else {
                    val k = (millions * 1000).toLong()
                    val roundedK = maxOf(10, (k / 10) * 10)
                    "\$${roundedK}K"
                }
            }
            "zar" -> {
                val millions = lakhs / 10.0
                if (millions >= 1) {
                    val formatted = String.format("%.2f", millions).trimEnd('0').trimEnd('.')
                    "R${formatted}M"
                } else {
                    val k = (millions * 1000).toLong()
                    "R${k}K"
                }
            }
            else -> { // INR
                val cr = lakhs / 100.0
                if (cr >= 1) {
                    val formatted = String.format("%.2f", cr).trimEnd('0').trimEnd('.')
                    "₹${formatted}Cr"
                } else {
                    val formatted = String.format("%.1f", lakhs).trimEnd('0').trimEnd('.')
                    "₹${formatted}L"
                }
            }
        }
    }

    fun getCurrencySymbol(currency: String = "inr") = CONFIG[currency]?.symbol ?: "₹"
    fun getCurrencyLabel(currency: String = "inr")  = CONFIG[currency]?.label  ?: "INR"

    /** Returns full purse display like "₹120Cr" */
    fun formatPurse(purse: Double, currency: String = "inr", sourceCurrency: String = "inr"): String =
        format(purse, currency, sourceCurrency)

    /** Returns role abbreviation matching the server's role parsing logic */
    fun getRoleDisplay(role: String?): String {
        if (role == null) return "BAT"
        return when {
            role.contains("wk", true) || role.contains("wicket", true) || role.contains("keeper", true) -> "WK"
            role.contains("all", true) || role.contains("ar", true) -> "AR"
            role.contains("bowl", true) || role.contains("bw", true) -> "BOWL"
            else -> "BAT"
        }
    }
}

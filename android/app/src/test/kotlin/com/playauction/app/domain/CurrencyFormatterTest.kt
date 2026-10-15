package com.playauction.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun testInrCroreFormatting() {
        // 200 Lakhs = ₹2Cr
        assertEquals("₹2Cr", CurrencyFormatter.format(200.0, "inr"))
        // 1250 Lakhs = ₹12.5Cr
        assertEquals("₹12.5Cr", CurrencyFormatter.format(1250.0, "inr"))
    }

    @Test
    fun testInrLakhFormatting() {
        // 50 Lakhs = ₹50L
        assertEquals("₹50L", CurrencyFormatter.format(50.0, "inr"))
        // 75.5 Lakhs = ₹75.5L
        assertEquals("₹75.5L", CurrencyFormatter.format(75.5, "inr"))
    }

    @Test
    fun testZarMillionFormatting() {
        // 410 internal units = R41M
        assertEquals("R41M", CurrencyFormatter.format(410.0, "zar"))
        // 65 internal units = R6.5M
        assertEquals("R6.5M", CurrencyFormatter.format(65.0, "zar"))
    }

    @Test
    fun testRoleDisplayParsing() {
        assertEquals("BAT", CurrencyFormatter.getRoleDisplay("Batsman"))
        assertEquals("BOWL", CurrencyFormatter.getRoleDisplay("Bowler"))
        assertEquals("AR", CurrencyFormatter.getRoleDisplay("All-Rounder"))
        assertEquals("WK", CurrencyFormatter.getRoleDisplay("Wicket Keeper"))
    }
}

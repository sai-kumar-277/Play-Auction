package com.playauction.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BidRulesEngineTest {

    @Test
    fun testIplMarqueeIncrement() {
        // IPL marquee: 25L increment
        val inc = BidRulesEngine.getMinIncrement(poolID = "marquee", currentAmount = 500.0, league = "ipl")
        assertEquals(25.0, inc, 0.001)

        val next = BidRulesEngine.getNextBidAmount(curAmt = 500.0, basePrice = 200.0, poolID = "marquee", league = "ipl")
        assertEquals(525.0, next, 0.001)
    }

    @Test
    fun testIplEmergingIncrementThreshold() {
        // Emerging: 5L under 200L, 25L at or above 200L
        val incBelow = BidRulesEngine.getMinIncrement(poolID = "emerging", currentAmount = 50.0, league = "ipl")
        assertEquals(5.0, incBelow, 0.001)

        val incAbove = BidRulesEngine.getMinIncrement(poolID = "emerging", currentAmount = 250.0, league = "ipl")
        assertEquals(25.0, incAbove, 0.001)
    }

    @Test
    fun testWplIncrements() {
        // WPL Marquee: 10L increment
        val incMarquee = BidRulesEngine.getMinIncrement(poolID = "marquee", currentAmount = 50.0, league = "wpl")
        assertEquals(10.0, incMarquee, 0.001)

        // WPL Uncapped: 2.5L increment
        val incUncapped = BidRulesEngine.getMinIncrement(poolID = "uncapped", currentAmount = 10.0, league = "wpl")
        assertEquals(2.5, incUncapped, 0.001)

        // WPL snap to nearest 0.5
        val snapped = BidRulesEngine.snapBidForLeague(12.3, "wpl")
        assertEquals(12.5, snapped, 0.001)
    }

    @Test
    fun testSa20Increments() {
        // SA20: 0.25 under 10M, 0.5 at or above 10M
        val incLow = BidRulesEngine.getMinIncrement(poolID = "pool1", currentAmount = 5.0, league = "sa20")
        assertEquals(0.25, incLow, 0.001)

        val incHigh = BidRulesEngine.getMinIncrement(poolID = "pool1", currentAmount = 15.0, league = "sa20")
        assertEquals(0.5, incHigh, 0.001)

        // SA20 snap to nearest 0.25
        val snapped = BidRulesEngine.snapBidForLeague(10.33, "sa20")
        assertEquals(10.25, snapped, 0.001)
    }

    @Test
    fun testFirstBidAtBasePrice() {
        // When curAmt is 0, first bid should equal base price
        val firstBid = BidRulesEngine.getNextBidAmount(curAmt = 0.0, basePrice = 200.0, poolID = "marquee", league = "ipl")
        assertEquals(200.0, firstBid, 0.001)
    }
}

package com.playauction.app.domain

/**
 * BidRulesEngine — exact Kotlin port of client/src/utils/bidRules.js
 * Keep in sync with server/utils/bidRules.js
 */
object BidRulesEngine {

    /** SA20: snap to nearest 0.25 */
    fun snapSa20Bid(amount: Double): Double {
        val n = amount.coerceAtLeast(0.0)
        return (Math.round(n * 4).toDouble()) / 4.0
    }

    /** WPL: snap to nearest 0.5 */
    fun snapWplBid(amount: Double): Double {
        val n = amount.coerceAtLeast(0.0)
        return (Math.round(n * 2).toDouble()) / 2.0
    }

    /** Snap bid to league-valid denomination */
    fun snapBidForLeague(amount: Double, league: String): Double = when (league.lowercase()) {
        "sa20" -> snapSa20Bid(amount)
        "wpl"  -> snapWplBid(amount)
        else   -> Math.round(amount.coerceAtLeast(0.0)).toDouble()
    }

    /**
     * Returns the minimum increment for a bid given pool and current amount.
     * Mirrors server/utils/bidRules.js getMinIncrement exactly.
     */
    fun getMinIncrement(poolID: String, currentAmount: Double, league: String = ""): Double {
        val lowerPool = poolID.lowercase()
        val curAmt = currentAmount.coerceAtLeast(0.0)

        if (league == "sa20") return if (curAmt < 10) 0.25 else 0.5

        if (league == "wpl") {
            return when {
                lowerPool == "marquee"       -> 10.0
                lowerPool == "uncapped"      -> 2.5
                else                         -> 5.0
            }
        }

        // IPL default
        return when {
            lowerPool.startsWith("marquee") || lowerPool.contains("pool1") || lowerPool.contains("pool2") -> 25.0
            lowerPool.contains("emerging") || lowerPool.contains("pool3") || lowerPool.contains("pool4") ->
                if (curAmt < 200) 5.0 else 25.0
            else -> 25.0
        }
    }

    /**
     * Returns the required (minimum valid) next bid amount.
     * Equivalent to getRequiredBid / getNextBidAmount on client.
     */
    fun getNextBidAmount(curAmt: Double, basePrice: Double, poolID: String, league: String): Double {
        val cur = curAmt.coerceAtLeast(0.0)
        val base = basePrice.coerceAtLeast(0.0)
        val inc = getMinIncrement(poolID, cur, league)
        val next = if (cur == 0.0) (if (base > 0) base else inc) else cur + inc
        return snapBidForLeague(next, league)
    }

    /**
     * Returns a list of bid options: [base, base+1inc, base+2inc, base+3inc, base+5inc]
     * for quick-tap bidding UI.
     */
    fun getBidOptions(currentBid: Double, basePrice: Double, poolID: String, league: String): List<Double> {
        val inc = getMinIncrement(poolID, currentBid, league)
        val start = getNextBidAmount(currentBid, basePrice, poolID, league)
        return listOf(
            start,
            snapBidForLeague(start + inc, league),
            snapBidForLeague(start + inc * 2, league),
            snapBidForLeague(start + inc * 4, league),
            snapBidForLeague(start + inc * 9, league)
        ).distinct()
    }
}

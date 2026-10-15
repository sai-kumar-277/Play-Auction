package com.playauction.app.data.socket

import com.google.gson.Gson
import com.playauction.app.data.model.CurrentBid
import com.playauction.app.data.model.Player
import com.playauction.app.data.model.RoomState
import org.junit.Assert.*
import org.junit.Test

class SocketStateParsingTest {

    private val gson = Gson()

    @Test
    fun testBidPlacedCompactParsing() {
        val json = """
            {
                "cb": {
                    "a": 1500.0,
                    "tid": "MI",
                    "tn": "Mumbai Indians",
                    "tc": "#004BA0",
                    "tl": "/ipl_logos/MI.png",
                    "on": "Manager1",
                    "isRtm": false
                },
                "t": 8
            }
        """.trimIndent()

        val parsed = gson.fromJson(json, BidPlacedEvent::class.java)
        assertNotNull(parsed)
        assertEquals(1500.0, parsed.currentBid.amount, 0.001)
        assertEquals("Mumbai Indians", parsed.currentBid.teamName)
        assertEquals(8, parsed.timer)
    }

    @Test
    fun testPlayerSoldEventParsing() {
        val json = """
            {
                "player": {
                    "_id": "p101",
                    "name": "Virat Kohli",
                    "role": "BAT",
                    "basePrice": 200.0,
                    "isOverseas": false
                },
                "winningBid": {
                    "amount": 2100.0,
                    "teamName": "Royal Challengers Bengaluru",
                    "teamColor": "#EC1C24"
                },
                "teams": []
            }
        """.trimIndent()

        val parsed = gson.fromJson(json, PlayerSoldEvent::class.java)
        assertNotNull(parsed)
        assertEquals("Virat Kohli", parsed.player.displayName)
        assertEquals(2100.0, parsed.winningBid?.amount ?: 0.0, 0.001)
        assertEquals("Royal Challengers Bengaluru", parsed.winningBid?.teamName)
    }

    @Test
    fun testQuizQuestionParsing() {
        val json = """
            {
                "quizIndex": 1,
                "text": "Who won the first IPL season in 2008?",
                "options": ["Rajasthan Royals", "Chennai Super Kings", "Delhi Daredevils", "Mumbai Indians"],
                "correctIndex": 0,
                "difficulty": "easy",
                "points": 1,
                "questionNumber": 1,
                "totalQuestions": 10,
                "timeLimit": 12
            }
        """.trimIndent()

        val parsed = gson.fromJson(json, com.playauction.app.data.model.QuizQuestion::class.java)
        assertNotNull(parsed)
        assertEquals(1, parsed.quizIndex)
        assertEquals(4, parsed.options.size)
        assertEquals(0, parsed.correctIndex)
        assertEquals(12, parsed.timeLimit)
    }
}

package com.nounish.launcher

import com.nounish.launcher.data.CachedNounsAuction
import com.nounish.launcher.data.LauncherSettings
import com.nounish.launcher.data.NounsAuction
import com.nounish.launcher.data.NounsImageCache
import com.nounish.launcher.data.NounsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class NounsAuctionTest {

    @Test
    fun testLauncherSettingsDefaults() {
        val settings = LauncherSettings()
        assertNull(settings.cachedNounsAuction)
        assertEquals(0L, settings.cachedNounsAuctionTimestamp)
        assertEquals("auto_rotate", settings.nounsDisplayMode)
        assertEquals("auction", settings.topRightCard)
        assertEquals("top_right", settings.nounsAuctionPosition)
        assertEquals("top_right", settings.homeScreenNotePosition)
        assertTrue(settings.showHomeNouns)
        assertTrue(settings.showNounsAuctionImage)
    }

    @Test
    fun testNounPicUrl() {
        assertEquals("https://noun.pics/2070.png", NounsImageCache.getNounPicUrl(2070))
        assertEquals("https://noun.pics/1.png", NounsImageCache.getNounPicUrl(1))
    }

    @Test
    fun testDecodeAuctionFromHex() {
        // Noun ID: 2070 (0x816)
        val nounIdHex = "0".repeat(61) + "816"
        // Amount: 8.5 ETH = 8500000000000000000 Wei = 0x75f46a7be7c0000
        val amountWei = BigInteger("8500000000000000000")
        val amountHex = amountWei.toString(16).padStart(64, '0')
        // Start time: 1794325979 = 0x6af33ddb
        val startHex = "0".repeat(56) + "6af33ddb"
        // End time: 1794412379 = 0x6af48f5b
        val endHex = "0".repeat(56) + "6af48f5b"
        // Bidder: 0x3a41e98d9e236319859f71c48e89ff5bb627f91c
        val bidderHex = "0".repeat(24) + "3a41e98d9e236319859f71c48e89ff5bb627f91c"
        // Settled: 0 (false)
        val settledHex = "0".repeat(64)

        val fullPayloadHex = "0x" + nounIdHex + amountHex + startHex + endHex + bidderHex + settledHex

        val auction = NounsRepository.decodeAuction(fullPayloadHex)
        assertNotNull(auction)
        assertEquals(2070, auction!!.nounId)
        assertEquals("8500000000000000000", auction.amountWei)
        assertEquals(8.5, auction.amountEth, 0.0001)
        assertEquals(1794325979L, auction.startTime)
        assertEquals(1794412379L, auction.endTime)
        assertEquals("0x3a41e98d9e236319859f71c48e89ff5bb627f91c", auction.bidder)
        assertFalse(auction.settled)
    }

    @Test
    fun testDecodeAuctionWithZeroBidsAndNullAddress() {
        val nounIdHex = "0".repeat(61) + "816"
        val amountHex = "0".repeat(64)
        val startHex = "0".repeat(56) + "6af33ddb"
        val endHex = "0".repeat(56) + "6af48f5b"
        val bidderHex = "0".repeat(64)
        val settledHex = "0".repeat(64)

        val fullPayloadHex = nounIdHex + amountHex + startHex + endHex + bidderHex + settledHex

        val auction = NounsRepository.decodeAuction(fullPayloadHex)
        assertNotNull(auction)
        assertEquals(2070, auction!!.nounId)
        assertEquals(0.0, auction.amountEth, 0.0001)
        assertEquals("0x0000000000000000000000000000000000000000", auction.bidder)
        assertFalse(auction.settled)
    }

    @Test
    fun testDecodeInvalidPayloadReturnsNull() {
        assertNull(NounsRepository.decodeAuction("0x1234"))
        assertNull(NounsRepository.decodeAuction(""))
        assertNull(NounsRepository.decodeAuction("not_hex"))
    }

    @Test
    fun testFormatAmount() {
        assertEquals("0.00 ETH", NounsRepository.formatAmount(0.0))
        assertEquals("0.00 ETH", NounsRepository.formatAmount(-1.0))
        assertEquals("8.50 ETH", NounsRepository.formatAmount(8.5))
        assertEquals("12.35 ETH", NounsRepository.formatAmount(12.3456))
    }

    @Test
    fun testFormatBidder() {
        assertEquals("No bids", NounsRepository.formatBidder("0x0000000000000000000000000000000000000000"))
        assertEquals("No bids", NounsRepository.formatBidder(""))
        assertEquals("0x3a41...f91c", NounsRepository.formatBidder("0x3a41e98d9e236319859f71c48e89ff5bb627f91c"))
    }

    @Test
    fun testFormatTimeLeft() {
        val nowMillis = 1000_000_000L * 1000L // 1,000,000,000 seconds
        val nowSeconds = 1000_000_000L

        // Settled
        assertEquals("Settled", NounsRepository.formatTimeLeft(nowSeconds + 3600, settled = true, nowMillis = nowMillis))

        // Ended
        assertEquals("Auction ended", NounsRepository.formatTimeLeft(nowSeconds - 10, settled = false, nowMillis = nowMillis))

        // > 1 hour (2h 15m)
        val end2h15m = nowSeconds + (2 * 3600) + (15 * 60)
        assertEquals("2h 15m left", NounsRepository.formatTimeLeft(end2h15m, settled = false, nowMillis = nowMillis))

        // < 1 hour (45m 30s)
        val end45m = nowSeconds + (45 * 60) + 30
        assertEquals("45m 30s left", NounsRepository.formatTimeLeft(end45m, settled = false, nowMillis = nowMillis))

        // < 1 minute (25s)
        val end25s = nowSeconds + 25
        assertEquals("25s left", NounsRepository.formatTimeLeft(end25s, settled = false, nowMillis = nowMillis))
    }

    @Test
    fun testToUiModel() {
        val nowMillis = 1000_000_000L * 1000L
        val nowSeconds = 1000_000_000L

        val auction = NounsAuction(
            nounId = 2070,
            amountWei = "8500000000000000000",
            amountEth = 8.5,
            startTime = nowSeconds - 3600,
            endTime = nowSeconds + 7200, // 2h left
            bidder = "0x3a41e98d9e236319859f71c48e89ff5bb627f91c",
            settled = false
        )

        val ui = NounsRepository.toUiModel(auction, isStale = true, ageMarker = "5m ago", nowMillis = nowMillis)
        assertEquals(2070, ui.nounId)
        assertEquals("8.50 ETH", ui.amountFormatted)
        assertEquals("2h 0m left", ui.timeLeftFormatted)
        assertEquals("0x3a41...f91c", ui.bidderFormatted)
        assertTrue(ui.isStale)
        assertEquals("5m ago", ui.ageMarker)
    }

    @Test
    fun testDecodeProposalStructAndTitle() {
        val slots = listOf(
            1002.toString(16).padStart(64, '0'), // 0: id
            "000000000000000000000000c7ccec521eed20fcddff8f95424816ac421c7d87", // 1: proposer
            3.toString(16).padStart(64, '0'),    // 2: proposalThreshold
            136.toString(16).padStart(64, '0'),  // 3: quorumVotes
            1791985259L.toString(16).padStart(64, '0'), // 4: eta
            26145681.toString(16).padStart(64, '0'), // 5: startBlock
            26174481.toString(16).padStart(64, '0'), // 6: endBlock
            215.toString(16).padStart(64, '0'),  // 7: forVotes
            0.toString(16).padStart(64, '0'),    // 8: againstVotes
            4.toString(16).padStart(64, '0'),    // 9: abstainVotes
            0.toString(16).padStart(64, '0'),    // 10: canceled
            0.toString(16).padStart(64, '0'),    // 11: vetoed
            1.toString(16).padStart(64, '0'),    // 12: executed
            1361.toString(16).padStart(64, '0'), // 13: totalSupply
            26124081L.toString(16).padStart(64, '0') // 14: creationBlock
        )
        val rawHex = "0x" + slots.joinToString("")
        val parsed = NounsRepository.decodeProposalStruct(1002, rawHex)
        assertNotNull(parsed)
        assertEquals(1002, parsed!!.id)
        assertEquals("0xc7ccec521eed20fcddff8f95424816ac421c7d87", parsed.proposer)
        assertEquals(136, parsed.quorum)
        assertEquals(215, parsed.forVotes)
        assertEquals(0, parsed.againstVotes)
        assertEquals(4, parsed.abstainVotes)
        assertEquals(26124081L, parsed.creationBlock)

        assertEquals("Active", NounsRepository.stateLabelForIndex(1))
        assertEquals("Executed", NounsRepository.stateLabelForIndex(7))
        assertEquals("Defeated", NounsRepository.stateLabelForIndex(3))

        val md = "\n# Migrate Remaining V1 Treasury to V2\n\nBody text here"
        assertEquals("Migrate Remaining V1 Treasury to V2", NounsRepository.extractProposalTitle(md, 1002))
        assertEquals("Proposal #999", NounsRepository.extractProposalTitle("", 999))
    }

    @Test
    fun testDecodeEnsNames() {
        val hexStr = "0x" + (
            "0000000000000000000000000000000000000000000000000000000000000020" +
            "0000000000000000000000000000000000000000000000000000000000000003" +
            "0000000000000000000000000000000000000000000000000000000000000060" +
            "00000000000000000000000000000000000000000000000000000000000000a0" +
            "00000000000000000000000000000000000000000000000000000000000000c0" +
            "000000000000000000000000000000000000000000000000000000000000000b" +
            "766974616c696b2e657468000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000000000000000000000000000" +
            "000000000000000000000000000000000000000000000000000000000000000e" +
            "7374726970796d6f666f2e657468000000000000000000000000000000000000"
        )
        val decoded = NounsRepository.decodeEnsNames(hexStr)
        assertEquals(3, decoded.size)
        assertEquals("vitalik.eth", decoded[0])
        assertEquals("", decoded[1])
        assertEquals("stripymofo.eth", decoded[2])
    }

    @Test
    fun testFormatBidderAndAddressWithEns() {
        assertEquals("vitalik.eth", NounsRepository.formatBidder("0xd8da6bf26964af9d7eed9e03e53415d37aa96045", "vitalik.eth"))
        assertEquals("0xd8da...6045", NounsRepository.formatBidder("0xd8da6bf26964af9d7eed9e03e53415d37aa96045", null))
        assertEquals("No bids", NounsRepository.formatBidder("0x0000000000000000000000000000000000000000", "vitalik.eth"))

        assertEquals("stripymofo.eth", NounsRepository.formatAddress("0xc7ccec521eed20fcddff8f95424816ac421c7d87", "stripymofo.eth"))
        assertEquals("0xc7cc...7d87", NounsRepository.formatAddress("0xc7ccec521eed20fcddff8f95424816ac421c7d87", null))
        assertEquals("", NounsRepository.formatAddress("0x0000000000000000000000000000000000000000", "stripymofo.eth"))
    }

    @Test
    fun testLiveAuctionCountdownDynamicTicks() {
        val endTimeSeconds = 1790000000L
        val t0Millis = 1790000000L * 1000L - 65_000L // 65 seconds left
        assertEquals("1m 5s left", NounsRepository.formatTimeLeft(endTimeSeconds, settled = false, nowMillis = t0Millis))

        val t5sLater = t0Millis + 5_000L // 60 seconds left
        assertEquals("1m 0s left", NounsRepository.formatTimeLeft(endTimeSeconds, settled = false, nowMillis = t5sLater))

        val t10sLater = t0Millis + 10_000L // 55 seconds left
        assertEquals("55s left", NounsRepository.formatTimeLeft(endTimeSeconds, settled = false, nowMillis = t10sLater))

        val tEnded = t0Millis + 70_000L // past end time
        assertEquals("Auction ended", NounsRepository.formatTimeLeft(endTimeSeconds, settled = false, nowMillis = tEnded))
    }

    @Test
    fun testAuctionClients() {
        val clients = NounsRepository.AUCTION_CLIENTS
        assertEquals(3, clients.size)

        val nounsWtf = clients.find { it.name == "nouns.wtf" }
        assertNotNull(nounsWtf)
        assertEquals("https://nouns.wtf", nounsWtf!!.buildUrl(2073))

        val nounWtf = clients.find { it.name == "noun.wtf" }
        assertNotNull(nounWtf)
        assertEquals("https://noun.wtf/noun/2073", nounWtf!!.buildUrl(2073))

        val nounsGame = clients.find { it.name == "nouns.game" }
        assertNotNull(nounsGame)
        assertEquals("https://nouns.game/auction", nounsGame!!.buildUrl(2073))

        val randomClient = NounsRepository.getRandomAuctionClient()
        assertTrue(clients.contains(randomClient))
    }
}

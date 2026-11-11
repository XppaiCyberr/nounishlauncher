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
}

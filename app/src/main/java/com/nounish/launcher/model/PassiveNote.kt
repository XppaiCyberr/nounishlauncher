package com.nounish.launcher.model

data class PassiveNote(
    val title: String,
    val body: String,
    val attribution: String = ""
)

val DEFAULT_PASSIVE_NOTES = listOf(
    PassiveNote(
        title = "Nounish ⌐◨-◨",
        body = "do good with no expectation of return, create positive externalities, embrace absurdity & difference, teach people about nouns & crypto, have fun and be honest.",
        attribution = "- PIP3"
    ),
    PassiveNote(
        title = "HODL",
        body = "Hold On for Dear Life. Originated from a 2013 Bitcoin forum typo. Holding through market volatility rather than panic selling.",
        attribution = "Crypto Slang"
    ),
    PassiveNote(
        title = "WAGMI",
        body = "We're All Gonna Make It. A community ethos of collective optimism, solidarity, and mutual encouragement.",
        attribution = "Crypto Slang"
    ),
    PassiveNote(
        title = "Positive Externalities",
        body = "Creating open benefits that spill over to the wider world without attempting to capture all the value for yourself.",
        attribution = "Nounish Ethos"
    ),
    PassiveNote(
        title = "GM ⌐◨-◨",
        body = "Good Morning. A universal greeting of shared positivity, welcoming builders across all time zones and cultures.",
        attribution = "Web3 Culture"
    ),
    PassiveNote(
        title = "DYOR",
        body = "Do Your Own Research. A reminder to think critically and verify information independently before making decisions.",
        attribution = "Crypto Principle"
    ),
    PassiveNote(
        title = "FUD",
        body = "Fear, Uncertainty, and Doubt. Sensationalist or misleading narratives spread to trigger panic or suppress adoption.",
        attribution = "Market Term"
    ),
    PassiveNote(
        title = "Zero-Sum vs Positive-Sum",
        body = "Zero-sum games only win when others lose. Positive-sum games expand the whole pie so everyone flourishes together.",
        attribution = "Game Theory"
    )
)

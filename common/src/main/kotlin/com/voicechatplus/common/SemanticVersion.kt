package com.voicechatplus.common

/**
 * A semantic version value used for capability and version negotiation.
 *
 * This is an original Voice Chat+ type. It deliberately does not model any
 * third-party protocol's version encoding.
 */
data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<SemanticVersion> {

    init {
        require(major >= 0 && minor >= 0 && patch >= 0) {
            "Version components must be non-negative"
        }
    }

    override fun compareTo(other: SemanticVersion): Int = compareValuesBy(
        this,
        other,
        { it.major },
        { it.minor },
        { it.patch },
    )

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        private val PATTERN = Regex("^(\\d+)\\.(\\d+)\\.(\\d+)$")

        @JvmStatic
        fun parse(value: String): SemanticVersion {
            val match = PATTERN.matchEntire(value.trim())
                ?: throw IllegalArgumentException("Invalid semantic version: '$value'")
            val (major, minor, patch) = match.destructured
            return SemanticVersion(major.toInt(), minor.toInt(), patch.toInt())
        }
    }
}

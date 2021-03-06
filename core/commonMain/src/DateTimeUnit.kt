/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.nanoseconds

sealed class DateTimeUnit {

    abstract operator fun times(scalar: Int): DateTimeUnit

    class TimeBased(val nanoseconds: Long) : DateTimeUnit() {
        private val unitName: String
        private val unitScale: Long

        init {
            require(nanoseconds > 0) { "Unit duration must be positive, but was $nanoseconds ns." }
            // find a concise string representation for the unit with this duration
            when {
                nanoseconds % 3600_000_000_000 == 0L -> {
                    unitName = "HOUR"
                    unitScale = nanoseconds / 3600_000_000_000
                }
                nanoseconds % 60_000_000_000 == 0L -> {
                    unitName = "MINUTE"
                    unitScale = nanoseconds / 60_000_000_000
                }
                nanoseconds % 1_000_000_000 == 0L -> {
                    unitName = "SECOND"
                    unitScale = nanoseconds / 1_000_000_000
                }
                nanoseconds % 1_000_000 == 0L -> {
                    unitName = "MILLISECOND"
                    unitScale = nanoseconds / 1_000_000
                }
                nanoseconds % 1_000 == 0L -> {
                    unitName = "MICROSECOND"
                    unitScale = nanoseconds / 1_000
                }
                else -> {
                    unitName = "NANOSECOND"
                    unitScale = nanoseconds
                }
            }
        }

        override fun times(scalar: Int): TimeBased = TimeBased(safeMultiply(nanoseconds, scalar.toLong()))

        @ExperimentalTime
        val duration: Duration = nanoseconds.nanoseconds

        override fun equals(other: Any?): Boolean =
                this === other || (other is TimeBased && this.nanoseconds == other.nanoseconds)

        override fun hashCode(): Int = nanoseconds.toInt() xor (nanoseconds shr Int.SIZE_BITS).toInt()

        override fun toString(): String = formatToString(unitScale, unitName)
    }

    sealed class DateBased : DateTimeUnit() {
        // TODO: investigate how to move subclasses up to DateTimeUnit scope
        class DayBased(val days: Int) : DateBased() {
            init {
                require(days > 0) { "Unit duration must be positive, but was $days days." }
            }

            override fun times(scalar: Int): DayBased = DayBased(safeMultiply(days, scalar))

            override fun equals(other: Any?): Boolean =
                    this === other || (other is DayBased && this.days == other.days)

            override fun hashCode(): Int = days xor 0x10000

            override fun toString(): String = if (days % 7 == 0)
                formatToString(days / 7, "WEEK")
            else
                formatToString(days, "DAY")
        }
        class MonthBased(val months: Int) : DateBased() {
            init {
                require(months > 0) { "Unit duration must be positive, but was $months months." }
            }

            override fun times(scalar: Int): MonthBased = MonthBased(safeMultiply(months, scalar))

            override fun equals(other: Any?): Boolean =
                    this === other || (other is MonthBased && this.months == other.months)

            override fun hashCode(): Int = months xor 0x20000

            override fun toString(): String = when {
                months % 12_00 == 0 -> formatToString(months / 12_00, "CENTURY")
                months % 12 == 0 -> formatToString(months / 12, "YEAR")
                months % 3 == 0 -> formatToString(months / 3, "QUARTER")
                else -> formatToString(months, "MONTH")
            }
        }
    }

    protected fun formatToString(value: Int, unit: String): String = if (value == 1) unit else "$value-$unit"
    protected fun formatToString(value: Long, unit: String): String = if (value == 1L) unit else "$value-$unit"

    companion object {
        val NANOSECOND = TimeBased(nanoseconds = 1)
        val MICROSECOND = NANOSECOND * 1000
        val MILLISECOND = MICROSECOND * 1000
        val SECOND = MILLISECOND * 1000
        val MINUTE = SECOND * 60
        val HOUR = MINUTE * 60
        val DAY = DateBased.DayBased(days = 1)
        val WEEK = DAY * 7
        val MONTH = DateBased.MonthBased(months = 1)
        val QUARTER = MONTH * 3
        val YEAR = MONTH * 12
        val CENTURY = YEAR * 100
    }
}

class RpgIoTime {
    companion object{
        const val TPS = 20
        private const val SECONDS_PER_DAY_NIGHT_CYCLE = 60.0 * 40//40 minutes
        const val TICKS_PER_DAY = TPS * SECONDS_PER_DAY_NIGHT_CYCLE
        const val STARTING_HOUR = 9
        const val DAYS_PER_MONTH = 28
        const val MONTHS_PER_YEAR = 13
        private const val DAYS_PER_WEEK = 7
        private const val HOURS_PER_DAY = 24
        private const val MINUTES_PER_HOUR = 60
        private val MONTHS = listOf(
            "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December", "Undecimber"
        )
        private val DAYS = listOf(
            "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
        )
    }

    private var tick: Int = (TICKS_PER_DAY * (STARTING_HOUR / HOURS_PER_DAY.toFloat())).toInt()
    fun advanceTime(){
        tick++
    }
    fun getTick(): Int{
        return tick
    }
    fun getPercentOfDay(): Double{
        return tick % TICKS_PER_DAY / TICKS_PER_DAY.toFloat()
    }
    fun getHourOfDay(): Int{
        return ((tick % TICKS_PER_DAY) / TICKS_PER_DAY * HOURS_PER_DAY).toInt()
    }
    fun getMinuteOfDay(): Int{
        return (((tick % TICKS_PER_DAY) / TICKS_PER_DAY * HOURS_PER_DAY * MINUTES_PER_HOUR) % MINUTES_PER_HOUR).toInt()
    }
    fun getDayOfWeek(): Int{
        return ((tick / TICKS_PER_DAY) % DAYS_PER_WEEK).toInt() + 1
    }
    fun getDayOfMonth(): Int{
        return ((tick / TICKS_PER_DAY) % DAYS_PER_MONTH).toInt() + 1
    }
    fun getMonthOfYear(): Int{
        return ((tick / TICKS_PER_DAY / DAYS_PER_MONTH) % MONTHS_PER_YEAR).toInt() + 1
    }
    fun getYear(): Int{
        return (tick / TICKS_PER_DAY / DAYS_PER_MONTH / MONTHS_PER_YEAR).toInt() + 1
    }
    fun getAmPm(): String{
        return if(getHourOfDay() < 12) "AM" else "PM"
    }
    fun getTimeString(): String{
        val hour = getHourOfDay()
        val minute = getMinuteOfDay()
        val hour12 = (hour % 12).let { if(it == 0) 12 else it }
        val hourString = if(hour12 < 10) " $hour12" else "$hour12"
        val minuteString = if(minute < 10) "0$minute" else "$minute"
        return "${DAYS[getDayOfWeek() - 1]}\n$hourString:$minuteString ${getAmPm()}\n${MONTHS[getMonthOfYear() - 1]} ${getDayOfMonth()}, ${getYear()}"
    }
    fun getTimeStringShort(): String{
        val hour = getHourOfDay()
        val minute = getMinuteOfDay()
        val hour12 = (hour % 12).let { if(it == 0) 12 else it }
        val hourString = if(hour12 < 10) " $hour12" else "$hour12"
        val minuteString = if(minute < 10) "0$minute" else "$minute"
        return "${getYear()}/${getMonthOfYear()}/${getDayOfMonth()} $hourString:$minuteString ${getAmPm()}"
    }

}
package com.termproject.sprintyou.ui.history

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import com.termproject.sprintyou.R
import com.termproject.sprintyou.data.SprintDatabaseProvider
import com.termproject.sprintyou.data.SprintHistoryItem
import com.termproject.sprintyou.databinding.ActivityCalendarBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class CalendarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalendarBinding
    private val adapter = HistoryAdapter()
    private val database by lazy { SprintDatabaseProvider.getDatabase(this) }
    private val zoneId: ZoneId = ZoneId.systemDefault()
    private var monthRecords: List<SprintHistoryItem> = emptyList()
    private var dotDecorator: SprintDotDecorator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecycler()
        bindCalendar()
        binding.btnBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        val today = LocalDate.now()
        val todayCalendarDay = today.toCalendarDay()
        binding.calendarView.setCurrentDate(todayCalendarDay)
        binding.calendarView.selectedDate = todayCalendarDay
        loadMonth(today)
    }

    private fun setupRecycler() {
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = adapter
    }

    private fun bindCalendar() {
        binding.calendarView.setOnMonthChangedListener { _, date ->
            loadMonth(date.toLocalDate())
        }
        binding.calendarView.setOnDateChangedListener { _, date, selected ->
            if (selected) {
                showRecordsFor(date.toLocalDate())
            }
        }
    }

    private fun loadMonth(date: LocalDate) {
        val firstDay = date.withDayOfMonth(1)
        val lastDay = firstDay.plusMonths(1).minusDays(1)
        val startMillis = firstDay.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = lastDay.plusDays(1).atStartOfDay(zoneId).toInstant().minusMillis(1).toEpochMilli()

        lifecycleScope.launch {
            val records = withContext(Dispatchers.IO) {
                database.sprintRecordDao().getBetween(startMillis, endMillis)
            }
            monthRecords = records
            updateDecorators(records)
            val selectedDate = binding.calendarView.selectedDate?.toLocalDate() ?: firstDay
            showRecordsFor(selectedDate.coerceIn(firstDay, lastDay))
        }
    }

    private fun updateDecorators(records: List<SprintHistoryItem>) {
        dotDecorator?.let { binding.calendarView.removeDecorator(it) }
        val dates = records
            .map { it.record.createdAt.toLocalDate() }
            .map { it.toCalendarDay() }
            .toSet()

        val color = ContextCompat.getColor(this, R.color.secondary_blue)
        val decorator = SprintDotDecorator(dates, color)
        binding.calendarView.addDecorator(decorator)
        dotDecorator = decorator
    }

    private fun showRecordsFor(date: LocalDate) {
        val filtered = monthRecords.filter { it.record.createdAt.toLocalDate() == date }
        adapter.submitList(filtered)
        binding.tvSelectedDate.text = date.format(DATE_FORMATTER)
        binding.tvEmptyState.isVisible = filtered.isEmpty()
        binding.rvHistory.isVisible = filtered.isNotEmpty()
    }

    private fun Long.toLocalDate(): LocalDate =
        Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()

    private fun LocalDate.coerceIn(min: LocalDate, max: LocalDate): LocalDate =
        when {
            this.isBefore(min) -> min
            this.isAfter(max) -> max
            else -> this
        }

    private fun CalendarDay.toLocalDate(): LocalDate =
        LocalDate.of(year, month + 1, day)

    private fun LocalDate.toCalendarDay(): CalendarDay =
        CalendarDay.from(year, monthValue - 1, dayOfMonth)

    private class SprintDotDecorator(
        private val dates: Set<CalendarDay>,
        private val color: Int
    ) : DayViewDecorator {
        override fun shouldDecorate(day: CalendarDay): Boolean = dates.contains(day)
        override fun decorate(view: DayViewFacade) {
            view.addSpan(DotSpan(8f, color))
        }
    }

    companion object {
        private val DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.getDefault())
    }
}

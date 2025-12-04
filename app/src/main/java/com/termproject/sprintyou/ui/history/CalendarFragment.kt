package com.termproject.sprintyou.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import com.termproject.sprintyou.R
import com.termproject.sprintyou.data.SprintDatabaseProvider
import com.termproject.sprintyou.data.SprintHistoryItem
import com.termproject.sprintyou.databinding.FragmentCalendarBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!
    private val adapter = HistoryAdapter()
    private val database by lazy { SprintDatabaseProvider.getDatabase(requireContext()) }
    private val zoneId: ZoneId = ZoneId.systemDefault()
    private var monthRecords: List<SprintHistoryItem> = emptyList()
    private var dotDecorator: SprintDotDecorator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler()
        bindCalendar()
    }

    override fun onResume() {
        super.onResume()
        val today = LocalDate.now()
        val todayCalendarDay = today.toCalendarDay()
        
        // Only set selection if not already selected (preserve state on tab switch if needed)
        // For now, let's keep it simple and refresh to today/month on resume as before, 
        // or check if calendar has selection.
        if (binding.calendarView.selectedDate == null) {
            binding.calendarView.setCurrentDate(todayCalendarDay)
            binding.calendarView.selectedDate = todayCalendarDay
            loadMonth(today)
        } else {
             // If we already have data, maybe don't reload? 
             // But data might have changed. Let's reload based on currently visible month.
             val currentMonth = binding.calendarView.currentDate.toLocalDate()
             loadMonth(currentMonth)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecycler() {
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
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

        viewLifecycleOwner.lifecycleScope.launch {
            val records = withContext(Dispatchers.IO) {
                database.sprintRecordDao().getBetween(startMillis, endMillis)
            }
            monthRecords = records
            updateDecorators(records)
            
            // If a date is selected, update list for that date. 
            // If not, default logic from activity was: first day or selected.
            val selectedDate = binding.calendarView.selectedDate?.toLocalDate() ?: firstDay
            // Ensure selectedDate is within the loaded month view? 
            // The original logic just coerced it.
            showRecordsFor(selectedDate.coerceIn(firstDay, lastDay))
        }
    }

    private fun updateDecorators(records: List<SprintHistoryItem>) {
        dotDecorator?.let { binding.calendarView.removeDecorator(it) }
        val dates = records
            .map { it.record.createdAt.toLocalDate() }
            .map { it.toCalendarDay() }
            .toSet()

        val color = ContextCompat.getColor(requireContext(), R.color.secondary_blue)
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


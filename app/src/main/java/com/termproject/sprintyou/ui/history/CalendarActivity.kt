package com.termproject.sprintyou.ui.history

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.termproject.sprintyou.databinding.ActivityCalendarBinding

class CalendarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalendarBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupViewPager() {
        val adapter = HistoryPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "캘린더"
                1 -> "목표 달성"
                else -> ""
            }
        }.attach()
    }

    private inner class HistoryPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> CalendarFragment()
                1 -> GoalListFragment()
                else -> throw IllegalArgumentException("Invalid position: $position")
            }
        }
    }
}
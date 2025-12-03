package com.termproject.sprintyou.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.termproject.sprintyou.R
import com.termproject.sprintyou.data.SprintHistoryItem
import com.termproject.sprintyou.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class HistoryAdapter :
    ListAdapter<SprintHistoryItem, HistoryAdapter.HistoryViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding =
            ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class HistoryViewHolder(
        private val binding: ItemHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SprintHistoryItem) {
            val context = binding.root.context
            val record = item.record
            binding.tvSprintTask.text = record.taskContent
            val goalTitle = item.mainGoalTitle ?: context.getString(R.string.history_unknown_goal)
            binding.tvGoalName.text = context.getString(
                R.string.history_goal_prefix,
                goalTitle
            )
            binding.tvDuration.text = context.getString(
                R.string.history_duration_format,
                (record.actualDurationSeconds / 60.0).roundToInt(),
                (record.targetDurationSeconds / 60.0).roundToInt()
            )
            binding.tvCreatedAt.text = DATE_FORMATTER.format(Date(record.createdAt))
        }
    }

    companion object {
        private val DATE_FORMATTER =
            SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())

        private val DiffCallback = object : DiffUtil.ItemCallback<SprintHistoryItem>() {
            override fun areItemsTheSame(oldItem: SprintHistoryItem, newItem: SprintHistoryItem): Boolean {
                return oldItem.record.sprintId == newItem.record.sprintId
            }

            override fun areContentsTheSame(oldItem: SprintHistoryItem, newItem: SprintHistoryItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}
package com.termproject.sprintyou.ui.history

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.termproject.sprintyou.data.MainGoal
import com.termproject.sprintyou.data.SprintRecord
import com.termproject.sprintyou.databinding.ItemGoalHeaderBinding
import com.termproject.sprintyou.databinding.ItemSprintChildBinding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class GoalWithSprints(
    val goal: MainGoal,
    val sprints: List<SprintRecord>,
    var isExpanded: Boolean = false
)

class GoalExpandableAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<Any>()
    private var data: List<GoalWithSprints> = emptyList()

    fun submitList(newData: List<GoalWithSprints>) {
        data = newData
        rebuildList()
    }

    private fun rebuildList() {
        items.clear()
        data.forEach { goalData ->
            items.add(goalData)
            if (goalData.isExpanded) {
                items.addAll(goalData.sprints)
            }
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is GoalWithSprints -> TYPE_HEADER
            is SprintRecord -> TYPE_CHILD
            else -> throw IllegalArgumentException("Invalid item type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                ItemGoalHeaderBinding.inflate(inflater, parent, false)
            )
            TYPE_CHILD -> ChildViewHolder(
                ItemSprintChildBinding.inflate(inflater, parent, false)
            )
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> holder.bind(items[position] as GoalWithSprints)
            is ChildViewHolder -> holder.bind(items[position] as SprintRecord)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class HeaderViewHolder(private val binding: ItemGoalHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = items[position] as? GoalWithSprints ?: return@setOnClickListener
                    item.isExpanded = !item.isExpanded
                    // In a real optimized list, we would use notifyItemRangeInserted/Removed
                    // But for simplicity, we rebuild
                    rebuildList()
                }
            }
        }

        @SuppressLint("SetTextI18n")
        fun bind(item: GoalWithSprints) {
            binding.tvGoalTitle.text = item.goal.title
            binding.tvStatus.text = "${item.goal.status.name} • ${formatDate(item.goal.createdAt)}"
            binding.tvCountBadge.text = item.sprints.size.toString()
            binding.ivArrow.rotation = if (item.isExpanded) 180f else 0f
            binding.tvCountBadge.isVisible = item.sprints.isNotEmpty()
        }

        private fun formatDate(timestamp: Long): String {
            return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .format(DATE_FORMATTER)
        }
    }

    inner class ChildViewHolder(private val binding: ItemSprintChildBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SprintRecord) {
            binding.tvTaskContent.text = item.taskContent
            binding.tvDate.text = formatDateTime(item.createdAt)
            binding.tvDuration.text = formatDuration(item.actualDurationSeconds)
        }

        @SuppressLint("DefaultLocale")
        private fun formatDuration(seconds: Long): String {
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            return String.format("%d분 %02d초", minutes, remainingSeconds)
        }

        private fun formatDateTime(timestamp: Long): String {
            return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .format(DATETIME_FORMATTER)
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_CHILD = 1

        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.getDefault())
        private val DATETIME_FORMATTER = DateTimeFormatter.ofPattern("M월 d일 a h:mm", Locale.getDefault())
    }
}

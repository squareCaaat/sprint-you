package com.termproject.sprintyou.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.termproject.sprintyou.data.SprintRecord
import com.termproject.sprintyou.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter :
    ListAdapter<SprintRecord, HistoryAdapter.HistoryViewHolder>(DiffCallback) {

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

        fun bind(record: SprintRecord) {
            binding.tvGoal.text = record.goalContent
            binding.tvCreatedAt.text = DATE_FORMATTER.format(Date(record.createdAt))
        }
    }

    companion object {
        private val DATE_FORMATTER =
            SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())

        private val DiffCallback = object : DiffUtil.ItemCallback<SprintRecord>() {
            override fun areItemsTheSame(oldItem: SprintRecord, newItem: SprintRecord): Boolean {
                return oldItem.uid == newItem.uid
            }

            override fun areContentsTheSame(oldItem: SprintRecord, newItem: SprintRecord): Boolean {
                return oldItem == newItem
            }
        }
    }
}




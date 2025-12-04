package com.termproject.sprintyou.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.termproject.sprintyou.data.SprintDatabaseProvider
import com.termproject.sprintyou.databinding.FragmentGoalListBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GoalListFragment : Fragment() {

    private var _binding: FragmentGoalListBinding? = null
    private val binding get() = _binding!!
    private val adapter = GoalExpandableAdapter()
    private val database by lazy { SprintDatabaseProvider.getDatabase(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoalListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.rvGoalList.layoutManager = LinearLayoutManager(context)
        binding.rvGoalList.adapter = adapter
        binding.tvComingSoon.isVisible = false
        
        loadData()
    }

    private fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val goalsWithSprints = withContext(Dispatchers.IO) {
                val goals = database.mainGoalDao().getAllGoals()
                val sprints = database.sprintRecordDao().getAllRecords()

                val sprintMap = sprints.groupBy { it.parentGoalId }
                
                goals.map { goal ->
                    GoalWithSprints(
                        goal = goal,
                        sprints = sprintMap[goal.goalId] ?: emptyList()
                    )
                }
            }
            
            adapter.submitList(goalsWithSprints)
            binding.tvComingSoon.isVisible = goalsWithSprints.isEmpty()
            binding.tvComingSoon.text = "아직 등록된 목표가 없습니다."
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
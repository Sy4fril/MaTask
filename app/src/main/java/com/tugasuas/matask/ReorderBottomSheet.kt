package com.tugasuas.matask

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tugasuas.matask.databinding.BottomSheetReorderBinding

class ReorderBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetReorderBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TaskViewModel by activityViewModels()
    private lateinit var adapter: ReorderCategoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetReorderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val categories = viewModel.categories.value ?: emptyList()
        adapter = ReorderCategoryAdapter(categories.toMutableList())
        binding.rvCategories.adapter = adapter
        binding.rvCategories.layoutManager = LinearLayoutManager(context)

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                adapter.moveItem(viewHolder.adapterPosition, target.adapterPosition)
                return true
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        })
        itemTouchHelper.attachToRecyclerView(binding.rvCategories)

        binding.btnDone.setOnClickListener {
            viewModel.updateCategoryOrder(adapter.getCategories())
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ReorderBottomSheet"
    }
}

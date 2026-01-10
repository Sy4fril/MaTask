package com.tugasuas.matask

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.Timestamp
import com.tugasuas.matask.databinding.BottomSheetAddTaskBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddTaskBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddTaskBinding? = null
    val binding get() = _binding!! // Make binding public

    private val viewModel: TaskViewModel by activityViewModels()
    var favorite = false
        private set
    var deadline: Timestamp? = null
        private set

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            val categoryNames = categories.map { it.name }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryNames)
            binding.actvCategories.setAdapter(adapter)
        }

        binding.btnDeadline.setOnClickListener { showDateTimePicker() }

        binding.chipDeadline.setOnCloseIconClickListener {
            deadline = null
            updateDeadlineChip()
        }

        binding.btnFavorite.setOnClickListener {
            favorite = !favorite
            binding.btnFavorite.setImageResource(
                if (favorite) R.drawable.ic_star else R.drawable.ic_star_outline
            )
        }
    }

    private fun showDateTimePicker() {
        val dialog = DateTimePickerDialog(deadline?.toDate()) { selectedDate ->
            deadline = Timestamp(selectedDate)
            updateDeadlineChip()
        }
        dialog.show(childFragmentManager, DateTimePickerDialog.TAG)
    }

    private fun updateDeadlineChip() {
        if (deadline != null) {
            val sdf = SimpleDateFormat("EEE, d MMM, HH:mm", Locale.getDefault())
            binding.chipDeadline.text = sdf.format(deadline!!.toDate())
            binding.chipDeadline.visibility = View.VISIBLE
        } else {
            binding.chipDeadline.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddTaskBottomSheet"
    }
}

package com.tugasuas.matask

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tugasuas.matask.databinding.BottomSheetSortBinding

class SortBottomSheet(private val onSortOptionSelected: (SortOrder) -> Unit) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSortBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSortBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.sortByMyOrder.setOnClickListener {
            onSortOptionSelected(SortOrder.MY_ORDER)
            dismiss()
        }
        binding.sortByDate.setOnClickListener {
            onSortOptionSelected(SortOrder.DATE)
            dismiss()
        }
        binding.sortByRecentlyStarred.setOnClickListener {
            onSortOptionSelected(SortOrder.RECENTLY_STARRED)
            dismiss()
        }
        binding.sortByAlphabetical.setOnClickListener {
            onSortOptionSelected(SortOrder.ALPHABETICAL)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SortBottomSheet"
    }
}

package com.tugasuas.matask

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tugasuas.matask.databinding.BottomSheetMoreOptionsBinding

class MoreOptionsBottomSheet(
    private val onRename: () -> Unit,
    private val onDelete: () -> Unit,
    private val onDeleteCompleted: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetMoreOptionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetMoreOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.renameList.setOnClickListener {
            onRename()
            dismiss()
        }
        binding.deleteList.setOnClickListener {
            onDelete()
            dismiss()
        }
        binding.deleteCompletedTasks.setOnClickListener {
            onDeleteCompleted()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "MoreOptionsBottomSheet"
    }
}

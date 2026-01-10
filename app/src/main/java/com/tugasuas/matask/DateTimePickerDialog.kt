package com.tugasuas.matask

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.tugasuas.matask.databinding.DialogDateTimePickerBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DateTimePickerDialog(
    private val initialDate: Date?,
    private val onDateTimeSelected: (Date) -> Unit
) : DialogFragment() {

    private var _binding: DialogDateTimePickerBinding? = null
    private val binding get() = _binding!!
    
    private val calendar = Calendar.getInstance()
    private var isTimeSet = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogDateTimePickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            // Atur lebar menjadi 90% dari layar agar tidak terlalu besar
            val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initialDate?.let { 
            calendar.time = it 
            isTimeSet = true
            updateTimeText()
        }
        
        binding.calendarView.date = calendar.timeInMillis
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        }

        binding.llSetTime.setOnClickListener {
            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                .setMinute(calendar.get(Calendar.MINUTE))
                .setTitleText("Setel waktu")
                .build()

            timePicker.addOnPositiveButtonClickListener {
                calendar.set(Calendar.HOUR_OF_DAY, timePicker.hour)
                calendar.set(Calendar.MINUTE, timePicker.minute)
                isTimeSet = true
                updateTimeText()
            }
            timePicker.show(childFragmentManager, "TIME_PICKER")
        }

        binding.btnCancel.setOnClickListener { dismiss() }
        
        binding.btnDone.setOnClickListener {
            onDateTimeSelected(calendar.time)
            dismiss()
        }
    }

    private fun updateTimeText() {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        binding.tvSelectedTime.text = "Setel waktu: ${sdf.format(calendar.time)}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "DateTimePickerDialog"
    }
}

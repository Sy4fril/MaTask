package com.tugasuas.matask

import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.tugasuas.matask.databinding.ItemTaskBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class TaskAdapter(
    private val viewModel: TaskViewModel
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private var tasks = mutableListOf<Task>()
    private val localeID = Locale("id", "ID")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount(): Int = tasks.size

    fun getTasks(): List<Task> {
        return tasks
    }

    fun setTasks(tasks: List<Task>) {
        this.tasks.clear()
        this.tasks.addAll(tasks)
        notifyDataSetChanged()
    }

    fun moveTask(fromPosition: Int, toPosition: Int) {
        Collections.swap(tasks, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }

    inner class TaskViewHolder(private val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: Task) {
            binding.tvTaskTitle.text = task.title
            binding.cbTask.isChecked = task.completed

            if (task.completed) {
                binding.tvTaskTitle.paintFlags = binding.tvTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvTaskTitle.setTextColor(Color.GRAY)
                
                val completedAt = task.completedTimestamp
                if (completedAt != null) {
                    binding.tvTaskDeadline.visibility = View.VISIBLE
                    val sdf = SimpleDateFormat("EEEE, dd MMM yyyy", localeID)
                    binding.tvTaskDeadline.text = "Selesai, ${sdf.format(completedAt.toDate())}"
                    binding.tvTaskDeadline.setTextColor(Color.GRAY)
                } else {
                    binding.tvTaskDeadline.visibility = View.GONE
                }
            } else {
                binding.tvTaskTitle.paintFlags = binding.tvTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvTaskTitle.setTextColor(binding.tvTaskTitle.textColors.defaultColor)
                
                val deadlineDateRaw = task.deadline
                if (deadlineDateRaw != null) {
                    binding.tvTaskDeadline.visibility = View.VISIBLE
                    val deadlineDate = deadlineDateRaw.toDate()
                    val now = Calendar.getInstance()
                    val deadline = Calendar.getInstance().apply { time = deadlineDate }

                    val nowMidnight = (now.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val deadlineMidnight = (deadline.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    val diffInMillis = deadlineMidnight.timeInMillis - nowMidnight.timeInMillis
                    val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)

                    val timeFormat = SimpleDateFormat("HH:mm", localeID)
                    val fullDateFormat = SimpleDateFormat("EEE, d MMM", localeID)

                    when {
                        diffInDays == 0L -> {
                            binding.tvTaskDeadline.text = "Hari ini, pukul ${timeFormat.format(deadlineDate)}"
                            binding.tvTaskDeadline.setTextColor(Color.RED)
                        }
                        diffInDays == 1L -> {
                            binding.tvTaskDeadline.text = "Besok, pukul ${timeFormat.format(deadlineDate)}"
                            binding.tvTaskDeadline.setTextColor(Color.parseColor("#FFA500"))
                        }
                        diffInDays == -1L -> {
                            binding.tvTaskDeadline.text = "Kemarin"
                            binding.tvTaskDeadline.setTextColor(Color.RED)
                        }
                        diffInDays < -1L -> {
                            binding.tvTaskDeadline.text = "${Math.abs(diffInDays)} hari yang lalu"
                            binding.tvTaskDeadline.setTextColor(Color.RED)
                        }
                        else -> {
                            binding.tvTaskDeadline.text = fullDateFormat.format(deadlineDate)
                            binding.tvTaskDeadline.setTextColor(Color.GRAY)
                        }
                    }
                } else {
                    binding.tvTaskDeadline.visibility = View.GONE
                }
            }

            binding.btnFavorite.setImageResource(
                if (task.favorite) R.drawable.ic_star else R.drawable.ic_star_outline
            )

            binding.cbTask.setOnCheckedChangeListener { _, isChecked ->
                if (task.completed != isChecked) {
                    viewModel.updateTask(task.copy(completed = isChecked))
                    if (isChecked) {
                        Toast.makeText(itemView.context, "Tugas selesai", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            binding.btnFavorite.setOnClickListener {
                viewModel.updateTask(task.copy(favorite = !task.favorite))
            }

            itemView.setOnClickListener { 
                val intent = Intent(itemView.context, TaskDetailActivity::class.java)
                intent.putExtra("EXTRA_TASK", task)
                itemView.context.startActivity(intent)
            }
        }
    }
}

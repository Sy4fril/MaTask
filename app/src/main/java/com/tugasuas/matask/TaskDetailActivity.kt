package com.tugasuas.matask

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Timestamp
import com.tugasuas.matask.databinding.ActivityTaskDetailBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskDetailBinding
    private val viewModel: TaskViewModel by viewModels()
    private var currentTask: Task? = null
    private var categories: List<Category> = emptyList()
    private var selectedCategoryId: String? = null
    private var isFavorite = false
    private var isDeleting = false // Flag untuk mencegah simpan otomatis saat menghapus

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentTask = intent.getParcelableExtra("EXTRA_TASK")

        if (currentTask == null) {
            finish()
            return
        }

        setupUI()
        observeViewModel()
        viewModel.getCategories()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { 
            saveTask()
            onBackPressed() 
        }

        currentTask?.let { task ->
            binding.etTaskTitle.setText(task.title)
            binding.etTaskDescription.setText(task.description)
            updateDeadlineText(task.deadline)
            isFavorite = task.favorite
            selectedCategoryId = task.categoryId
        }

        binding.tvDeadline.setOnClickListener {
            val dialog = DateTimePickerDialog(currentTask?.deadline?.toDate()) { selectedDate ->
                val newDeadline = Timestamp(selectedDate)
                currentTask?.deadline = newDeadline
                updateDeadlineText(newDeadline)
            }
            dialog.show(supportFragmentManager, DateTimePickerDialog.TAG)
        }

        binding.btnMarkAsComplete.setOnClickListener {
            currentTask?.let {
                it.completed = true
                viewModel.updateTask(it)
                Toast.makeText(this, "Tugas selesai", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.categories.observe(this) { categoryList ->
            categories = categoryList
            val categoryNames = categories.map { it.name }
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categoryNames)
            
            binding.actvCategories.setAdapter(adapter)

            val currentCategory = categories.find { it.id == selectedCategoryId }
            if (currentCategory != null) {
                binding.actvCategories.setText(currentCategory.name, false)
            }

            binding.actvCategories.setOnItemClickListener { _, _, position, _ ->
                selectedCategoryId = categories[position].id
            }
        }
    }

    private fun updateDeadlineText(deadline: Timestamp?) {
        if (deadline != null) {
            val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID"))
            binding.tvDeadline.text = sdf.format(deadline.toDate())
        } else {
            binding.tvDeadline.text = "Tambahkan tanggal/waktu"
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.task_detail_menu, menu)
        val favoriteItem = menu?.findItem(R.id.action_favorite)
        favoriteItem?.setIcon(if (isFavorite) R.drawable.ic_star else R.drawable.ic_star_outline)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_favorite -> {
                isFavorite = !isFavorite
                invalidateOptionsMenu()
                true
            }
            R.id.action_delete -> {
                showDeleteConfirmationDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDeleteConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Hapus Tugas?")
            .setMessage("Apakah Anda yakin ingin menghapus tugas ini? Tindakan ini tidak dapat dibatalkan.")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->
                deleteTask()
            }
            .show()
    }

    private fun deleteTask() {
        currentTask?.let {
            isDeleting = true // Aktifkan flag agar tidak disimpan di onPause
            viewModel.deleteTask(it)
            Toast.makeText(this, "Tugas berhasil dihapus", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        if (!isDeleting) {
            saveTask()
        }
    }

    private fun saveTask() {
        val title = binding.etTaskTitle.text.toString().trim()
        val description = binding.etTaskDescription.text.toString().trim()

        if (title.isNotEmpty()) {
            currentTask?.let {
                it.title = title
                it.description = description
                it.categoryId = selectedCategoryId ?: it.categoryId
                it.favorite = isFavorite
                viewModel.updateTask(it)
            }
        }
    }
}

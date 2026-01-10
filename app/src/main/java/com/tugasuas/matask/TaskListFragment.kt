package com.tugasuas.matask

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tugasuas.matask.databinding.FragmentTaskListBinding

class TaskListFragment : Fragment() {

    private var _binding: FragmentTaskListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TaskViewModel by activityViewModels()
    private lateinit var incompleteTaskAdapter: TaskAdapter
    private lateinit var completedTaskAdapter: TaskAdapter
    private var categoryId: String? = null
    private var isFavoritesPage: Boolean = false
    private var isOverduePage: Boolean = false
    private var currentSortOrder = SortOrder.MY_ORDER

    private var incompleteTasksLiveData: LiveData<List<Task>>? = null
    private var completedTasksLiveData: LiveData<List<Task>>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            categoryId = it.getString(ARG_CATEGORY_ID)
            isFavoritesPage = it.getBoolean(ARG_IS_FAVORITES)
            isOverduePage = it.getBoolean(ARG_IS_OVERDUE)
        }

        setupUI()
        setupInitialObservers()
    }

    override fun onResume() {
        super.onResume()
        checkNotificationPermissions()
    }

    private fun setupUI() {
        incompleteTaskAdapter = TaskAdapter(viewModel)
        binding.rvIncompleteTasks.apply {
            adapter = incompleteTaskAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        completedTaskAdapter = TaskAdapter(viewModel)
        binding.rvCompletedTasks.apply {
            adapter = completedTaskAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        binding.btnSort.setOnClickListener { 
            val sortBottomSheet = SortBottomSheet { newSortOrder ->
                currentSortOrder = newSortOrder
                reobserveTasks()
            }
            sortBottomSheet.show(childFragmentManager, SortBottomSheet.TAG)
        }
        
        binding.btnMore.setOnClickListener { showMoreMenu() }
        if(isFavoritesPage || isOverduePage) binding.btnMore.visibility = View.GONE

        binding.swipeRefreshLayout.setOnRefreshListener {
            reobserveTasks()
            binding.swipeRefreshLayout.postDelayed({ 
                binding.swipeRefreshLayout.isRefreshing = false
            }, 1000)
        }

        binding.btnOpenSettings.setOnClickListener {
            openAppSettings()
        }

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                if (isFavoritesPage || isOverduePage || currentSortOrder != SortOrder.MY_ORDER) return false
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                incompleteTaskAdapter.moveTask(fromPosition, toPosition)
                return true
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                if (!isFavoritesPage && !isOverduePage && currentSortOrder == SortOrder.MY_ORDER) {
                    viewModel.updateTaskOrder(incompleteTaskAdapter.getTasks())
                }
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.rvIncompleteTasks)
        
        binding.llCompletedHeader.setOnClickListener {
            if (binding.rvCompletedTasks.visibility == View.GONE) {
                binding.rvCompletedTasks.visibility = View.VISIBLE
                binding.ivCompletedArrow.setImageResource(R.drawable.ic_arrow_up)
            } else {
                binding.rvCompletedTasks.visibility = View.GONE
                binding.ivCompletedArrow.setImageResource(R.drawable.ic_arrow_down)
            }
        }

        // Sembunyikan bagian tugas selesai di tab Lewat Waktu
        if (isOverduePage) {
            binding.cardCompletedTasks.visibility = View.GONE
        }
    }

    private fun checkNotificationPermissions() {
        val context = requireContext()
        val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val canScheduleExactAlarms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
        if (!hasNotificationPermission || !canScheduleExactAlarms) {
            binding.cardNotificationWarning.visibility = View.VISIBLE
        } else {
            binding.cardNotificationWarning.visibility = View.GONE
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", requireContext().packageName, null)
        }
        startActivity(intent)
    }

    private fun setupInitialObservers() {
        when {
            isOverduePage -> binding.tvCategoryTitle.text = "Lewat Waktu"
            isFavoritesPage -> binding.tvCategoryTitle.text = "Favorites"
            else -> {
                viewModel.categories.observe(viewLifecycleOwner) { categories ->
                    val category = categories.find { it.id == categoryId }
                    binding.tvCategoryTitle.text = category?.name ?: ""
                }
            }
        }
        reobserveTasks()
    }

    private fun reobserveTasks() {
        incompleteTasksLiveData?.removeObservers(viewLifecycleOwner)
        incompleteTasksLiveData = viewModel.getIncompleteTasks(categoryId, isFavoritesPage, currentSortOrder, isOverduePage)
        incompleteTasksLiveData?.observe(viewLifecycleOwner) { tasks ->
            incompleteTaskAdapter.setTasks(tasks)
            if (tasks.isEmpty()) {
                binding.llEmptyState.visibility = View.VISIBLE
                binding.rvIncompleteTasks.visibility = View.GONE
            } else {
                binding.llEmptyState.visibility = View.GONE
                binding.rvIncompleteTasks.visibility = View.VISIBLE
            }
        }

        if (!isOverduePage) {
            completedTasksLiveData?.removeObservers(viewLifecycleOwner)
            completedTasksLiveData = viewModel.getCompletedTasks(categoryId, isFavoritesPage)
            completedTasksLiveData?.observe(viewLifecycleOwner) { tasks ->
                completedTaskAdapter.setTasks(tasks)
                binding.cardCompletedTasks.visibility = if (tasks.isEmpty()) View.GONE else View.VISIBLE
                binding.tvCompletedTitle.text = "Selesai (${tasks.size})"
            }
        }
    }

    private fun showMoreMenu() {
        val moreOptionsBottomSheet = MoreOptionsBottomSheet(
            onRename = { showRenameListDialog() },
            onDelete = { showDeleteListDialog() },
            onDeleteCompleted = { showDeleteAllCompletedDialog() }
        )
        moreOptionsBottomSheet.show(childFragmentManager, MoreOptionsBottomSheet.TAG)
    }

    private fun showRenameListDialog() {
        val currentCategory = viewModel.categories.value?.find { it.id == categoryId }
        if (currentCategory != null) {
            val dialogView = layoutInflater.inflate(R.layout.dialog_add_category, null)
            val etCategoryName = dialogView.findViewById<EditText>(R.id.etCategoryName)
            etCategoryName.setText(currentCategory.name)

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Ganti nama daftar")
                .setView(dialogView)
                .setPositiveButton("Ganti Nama") { _, _ ->
                    val newName = etCategoryName.text.toString()
                    if (newName.isNotEmpty()) {
                        viewModel.updateCategory(currentCategory.copy(name = newName))
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    private fun showDeleteListDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hapus daftar ini?")
            .setMessage("Semua tugas dalam daftar ini akan dihapus secara permanen.")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->
                val category = viewModel.categories.value?.find { it.id == categoryId }
                category?.let { viewModel.deleteCategory(it) }
            }
            .show()
    }

    private fun showDeleteAllCompletedDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hapus semua tugas yang telah selesai?")
            .setMessage("Tindakan ini tidak dapat diurungkan.")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->
                viewModel.deleteAllCompletedTasks(categoryId, isFavoritesPage)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CATEGORY_ID = "category_id"
        private const val ARG_IS_FAVORITES = "is_favorites"
        private const val ARG_IS_OVERDUE = "is_overdue"

        fun newInstance(categoryId: String?, isFavorites: Boolean = false, isOverdue: Boolean = false): TaskListFragment {
            val fragment = TaskListFragment()
            val args = Bundle()
            args.putString(ARG_CATEGORY_ID, categoryId)
            args.putBoolean(ARG_IS_FAVORITES, isFavorites)
            args.putBoolean(ARG_IS_OVERDUE, isOverdue)
            fragment.arguments = args
            return fragment
        }
    }
}

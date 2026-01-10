package com.tugasuas.matask

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.tugasuas.matask.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private val viewModel: TaskViewModel by viewModels()
    private lateinit var pagerAdapter: CategoryPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setupUI(currentUser)
        observeViewModel()

        viewModel.getCategories()
    }

    private fun setupUI(currentUser: FirebaseUser) {
        // Load profile image
        Glide.with(this)
            .load(currentUser.photoUrl)
            .placeholder(R.drawable.ic_profile_avatar) // Default avatar
            .apply(RequestOptions.circleCropTransform())
            .into(binding.btnProfile)

        binding.btnProfile.setOnClickListener {
            val accountSwitcherBottomSheet = AccountSwitcherBottomSheet()
            accountSwitcherBottomSheet.show(supportFragmentManager, AccountSwitcherBottomSheet.TAG)
        }

        pagerAdapter = CategoryPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            // Set text or icon here
        }.attach()

        binding.fabAddTask.setOnClickListener {
            showAddOptionsDialog()
        }
    }

    private fun observeViewModel() {
        viewModel.categories.observe(this) { categories ->
            pagerAdapter.clear()

            // Add Favorites Fragment
            pagerAdapter.addFragment(TaskListFragment.newInstance(null, isFavorites = true), "Favorites")

            if (categories.isEmpty()) {
                viewModel.addCategory(Category(name = "Default"))
            } else {
                categories.forEach { category ->
                    pagerAdapter.addFragment(TaskListFragment.newInstance(category.id), category.name)
                }
            }
            pagerAdapter.notifyDataSetChanged()

            // Redo the TabLayoutMediator to set titles and icons
            TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
                if (position == 0) {
                    tab.setIcon(R.drawable.ic_star)
                    tab.tag = "FAVORITES_TAB" // Tag for identification
                } else {
                    val categoryIndex = position - 1
                    if (categoryIndex < categories.size) {
                        val category = categories[categoryIndex]
                        tab.text = category.name
                        tab.tag = category // This is the crucial line
                        tab.view.setOnLongClickListener {
                            val reorderBottomSheet = ReorderBottomSheet()
                            reorderBottomSheet.show(supportFragmentManager, ReorderBottomSheet.TAG)
                            true
                        }
                    }
                }
            }.attach()
        }
    }

    private fun showAddOptionsDialog() {
        val options = arrayOf("Add Task", "Add Category")
        MaterialAlertDialogBuilder(this)
            .setTitle("Add")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAddTaskSheet()
                    1 -> showAddCategoryDialog()
                }
            }
            .show()
    }

   private fun showAddTaskSheet() {
    val selectedTab = binding.tabLayout.getTabAt(binding.tabLayout.selectedTabPosition)
    val activeCategory: Category? = when {
        selectedTab?.tag is Category -> selectedTab.tag as Category
        else -> viewModel.categories.value?.firstOrNull { it.name == "Default" } ?: viewModel.categories.value?.firstOrNull()
    }

    if (activeCategory == null) {
         MaterialAlertDialogBuilder(this)
            .setTitle("Cannot Add Task")
            .setMessage("Please add a category first.")
            .setPositiveButton("OK", null)
            .show()
        return
    }

    val bottomSheet = AddTaskBottomSheet()
    bottomSheet.show(supportFragmentManager, AddTaskBottomSheet.TAG)
    supportFragmentManager.executePendingTransactions()

    bottomSheet.binding.btnSave.setOnClickListener { 
        val title = bottomSheet.binding.etTaskTitle.text.toString().trim()
        val description = bottomSheet.binding.etDescription.text.toString().trim()
        val categoryNameFromDropdown = bottomSheet.binding.actvCategories.text.toString()
        val selectedCategoryInSheet = viewModel.categories.value?.find { it.name == categoryNameFromDropdown }

        if (title.isNotEmpty() && selectedCategoryInSheet != null) {
            val newTask = Task(
                title = title,
                description = description,
                deadline = bottomSheet.deadline,
                favorite = bottomSheet.favorite,
                categoryId = selectedCategoryInSheet.id,
                position = System.currentTimeMillis() 
            )
            viewModel.addTask(newTask)
            Toast.makeText(this, "Tugas berhasil ditambahkan", Toast.LENGTH_SHORT).show()
            bottomSheet.dismiss()
        }
    }

    val categories = viewModel.categories.value ?: emptyList()
    val categoryNames = categories.map { it.name }
    val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categoryNames)
    bottomSheet.binding.actvCategories.setAdapter(adapter)
    bottomSheet.binding.actvCategories.setText(activeCategory.name, false)
}


    private fun showAddCategoryDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_category, null)
        val etCategoryName = dialogView.findViewById<EditText>(R.id.etCategoryName)

        MaterialAlertDialogBuilder(this)
            .setTitle("Add Category")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = etCategoryName.text.toString()
                if (name.isNotEmpty()) {
                    viewModel.addCategory(Category(name = name))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

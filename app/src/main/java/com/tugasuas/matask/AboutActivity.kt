package com.tugasuas.matask

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tugasuas.matask.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }

        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        binding.tvVersion.text = "Versi ${packageInfo.versionName}"
    }
}

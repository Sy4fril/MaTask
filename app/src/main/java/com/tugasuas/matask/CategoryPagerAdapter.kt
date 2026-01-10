package com.tugasuas.matask

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class CategoryPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

    private val fragments = mutableListOf<Fragment>()
    private val titles = mutableListOf<String>()

    fun addFragment(fragment: Fragment, title: String) {
        fragments.add(fragment)
        titles.add(title)
    }

    fun clear() {
        fragments.clear()
        titles.clear()
    }

    fun getPageTitle(position: Int): CharSequence {
        return titles[position]
    }

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]
}

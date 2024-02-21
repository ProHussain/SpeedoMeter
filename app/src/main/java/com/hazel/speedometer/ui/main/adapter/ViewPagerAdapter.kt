package com.hazel.speedometer.ui.main.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.hazel.speedometer.ui.main.tabs.analogue.AnalogueFragment
import com.hazel.speedometer.ui.main.tabs.digital.DigitalFragment
import com.hazel.speedometer.ui.main.tabs.map.MapFragment

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int {
        return 3
    }

    override fun createFragment(position: Int): Fragment {
        return when(position) {
            0 -> DigitalFragment.newInstance()
            1 -> AnalogueFragment.newInstance()
            2 -> MapFragment.newInstance()
            else -> DigitalFragment.newInstance()
        }
    }
}
/*
 * Copyright (C) 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thecloudsite.aicode

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.thecloudsite.aicode.databinding.ActivityCalcBinding
import java.util.Locale

class MainActivity : AppCompatActivity() {

  private lateinit var binding: ActivityCalcBinding
  private lateinit var symbol: String

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setAppTheme(this)
    delegate.applyDayNight()

    binding = ActivityCalcBinding.inflate(layoutInflater)
    val view = binding.root
    setContentView(view)

    val symbolString = intent.getStringExtra("symbol")
    symbol = symbolString?.toUpperCase(Locale.ROOT) ?: ""

    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    binding.calcViewpager.adapter = object : FragmentStateAdapter(this) {
      override fun createFragment(position: Int): Fragment {
        return when (position) {
          0 -> {
            val instance = CalcFragment.newInstance(symbol)
            instance
          }
          1 -> {
            val instance = CalcProgFragment.newInstance(symbol)
            instance
          }
          else -> {
            val instance = CalcFragment.newInstance(symbol)
            instance
          }
        }
      }

      override fun getItemCount(): Int {
        return 2
      }
    }

    binding.calcViewpager.setCurrentItem(0, false)

    TabLayoutMediator(binding.tabLayout, binding.calcViewpager) { tab, position ->
      tab.text = when (position) {
        0 -> getString(R.string.calc_headline)
        1 -> getString(R.string.calc_prog_headline)
        else -> ""
      }
    }.attach()

    // Calc does not need the tab layout displayed.
    binding.tabLayout.visibility = View.GONE
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    // Inflate the menu; this adds items to the action bar if it is present.
    menuInflater.inflate(R.menu.calc_menu, menu)
    return true
  }

  fun onSettings(item: MenuItem) {
    val intent = Intent(this@MainActivity, CalcSettingsActivity::class.java)
    startActivity(intent)
  }

  override fun onSupportNavigateUp(): Boolean {
    onBackPressed()
    return true
  }
}

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

import android.app.AlertDialog.Builder
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.bold
import androidx.core.text.color
import androidx.core.text.italic
import androidx.core.text.scale
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.thecloudsite.aicode.R.id
import com.thecloudsite.aicode.R.string
import com.thecloudsite.aicode.R.xml
import com.thecloudsite.aicode.databinding.ActivityCalcSettingsBinding
import java.io.BufferedReader
import java.io.InputStreamReader

private lateinit var loadRequest: ActivityResultLauncher<String>
private lateinit var saveRequest: ActivityResultLauncher<String>

// https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.CreateDocument
class GetJsonContent : ActivityResultContracts.GetContent() {
    override fun createIntent(context: Context, input: String): Intent {
        val mimeTypes = arrayOf(

            // .json
            "application/json",
            "text/x-json",

            )

// Intent.createChooser(intent, getString(R.string.import_select_file)),

        return super.createIntent(context, input)
            .setType("*/*")
            .setAction(Intent.ACTION_OPEN_DOCUMENT)
            .putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
    }
}

class CreateJsonDocument : ActivityResultContracts.CreateDocument() {
    override fun createIntent(context: Context, input: String): Intent {
        return super.createIntent(context, input)
            .setType("application/json")
            .setAction(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .putExtra(Intent.EXTRA_TITLE, input)
    }
}

class CalcSettingsActivity : AppCompatActivity(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var binding: ActivityCalcSettingsBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCalcSettingsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        supportFragmentManager
            .beginTransaction()
            .replace(id.calc_settings, CalcSettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        loadRequest =
            registerForActivityResult(GetJsonContent())
            { uri ->
                loadCode(this, uri)
                finish()
            }

        saveRequest =
            registerForActivityResult(CreateJsonDocument())
            { uri ->
                saveCode(this, uri)
                finish()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun loadCode(
        context: Context,
        uri: Uri
    ) {
        try {
            context.contentResolver.openInputStream(uri)
                ?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        val text: String = reader.readText()

                        // https://developer.android.com/training/secure-file-sharing/retrieve-info

                        when (val type = context.contentResolver.getType(uri)) {
                            "application/json", "text/x-json" -> {

                                val sharedPreferences =
                                    PreferenceManager.getDefaultSharedPreferences(context /* Activity context */)

                                sharedPreferences
                                    .edit()
                                    .putString("calcCodeMap", text)
                                    .apply()

                                val msg = application.getString(
                                    R.string.load_code_msg
                                )
                                Toast.makeText(context, msg, Toast.LENGTH_LONG)
                                    .show()
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Toast.makeText(
                context, application.getString(R.string.import_error, e.message),
                Toast.LENGTH_LONG
            )
                .show()
        }
    }

    private fun saveCode(
        context: Context,
        exportJsonUri: Uri
    ) {
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context /* Activity context */)

        val jsonString = sharedPreferences.getString("calcCodeMap", "").toString()
        val msg = application.getString(
            R.string.save_code_msg
        )

        saveTextToFile(jsonString, msg, context, exportJsonUri)
    }

    override fun onResume() {
        super.onResume()
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences,
        key: String
    ) {
        when (key) {
            "app_theme" -> {
                setAppTheme(this)
                delegate.applyDayNight()
            }
        }
    }

    class CalcSettingsFragment : PreferenceFragmentCompat() {

        companion object {
        }

        override fun onCreatePreferences(
            savedInstanceState: Bundle?,
            rootKey: String?
        ) {
            setPreferencesFromResource(xml.calc_preferences, rootKey)

            // Set version info.
            val versionCode = BuildConfig.VERSION_CODE
            //val versionName = BuildConfig.VERSION_NAME
            val versionBuild = if (BuildConfig.DEBUG) {
                "(Debug)"
            } else {
                ""
            }
            val version: Preference? = findPreference("version")
            val versionStr = SpannableStringBuilder()
                .append("Version code")
                .color(
                    context?.getColor(R.color.settingsblue)!!
                ) { bold { append(" $versionCode $versionBuild") } }
//          ) { bold { append(" \t\t$versionCode") } }
//          .append("\nVersion name")
//          .color(
//              context?.getColor(R.color.settingsblue)!!
//          ) { bold { append(" \t$versionName $versionBuild") } }
                .italic {
                    scale(0.8f) {
                        color(0xffffbb33.toInt()) {
                            // https://convertcodes.com/unicode-converter-encode-decode-utf/
                            append(
                                "\n\u006a\u0075\u0072\u0067\u0065\u006e\u0031\u0037\u0038\u002c\u0020\u0061\u0069\u0061\u006d\u0061\u006e\u002c\u0020\u006b\u0065\u006e\u0064\u0079\u002c\u0020\u0062\u006c\u0075\u006c\u0062\u0020\u0061\u006e\u0064\u0020\u0074\u0075\u006c\u0062\u0070\u0069\u0072"
                            )
                        }
                    }
                }
            version?.summary = versionStr

            val titles: List<String> = listOf(
                // https://convertcodes.com/unicode-converter-encode-decode-utf/
                "\u0057\u0068\u0061\u0074\u0020\u0079\u006f\u0075\u0020\u0061\u0072\u0065\u0020\u006c\u006f\u006f\u006b\u0069\u006e\u0067\u0020\u0066\u006f\u0072",
                "\u0053\u0074\u006f\u0063\u006b\u0052\u006f\u006f\u006d",
                "\u004f\u0062\u0073\u0065\u0072\u0076\u0065"
            )

            val messages: List<String> = listOf(
                // https://convertcodes.com/unicode-converter-encode-decode-utf/
                "\u0069\u0073\u0020\u006e\u006f\u0074\u0020\u0068\u0065\u0072\u0065\u002e",
                "\u0053\u0074\u006f\u0063\u006b\u0020\u004d\u0061\u0072\u006b\u0065\u0074",
                "\u0074\u0068\u0065\u0020\u0073\u0065\u0063\u0072\u0065\u0074\u0020\u006d\u0065\u0073\u0073\u0061\u0067\u0065\u0020\u0069\u006e\u0020\u0074\u0068\u0065\u0020\u0041\u0049\u002e"
            )

            var versionClickCounter: Int = 0
            version?.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    versionClickCounter++
                    if (versionClickCounter % 10 == 0 && versionClickCounter <= messages.size * 10) {
                        val index = versionClickCounter / 10 - 1
                        Builder(requireContext())
                            .setTitle(titles[index])
                            .setMessage(messages[index])
                            .setPositiveButton(string.ok) { dialog, _ -> dialog.dismiss() }
                            .show()
                    }
                    true
                }

            val buttonExportCode: Preference? = findPreference("export_code")
            if (buttonExportCode != null) {
                buttonExportCode.onPreferenceClickListener =
                    Preference.OnPreferenceClickListener {
                        onExportCode()
                        true
                    }
            }

            val buttonImportCode: Preference? = findPreference("import_code")
            if (buttonImportCode != null) {
                buttonImportCode.onPreferenceClickListener =
                    Preference.OnPreferenceClickListener {
                        onImportCode()
                        true
                    }
            }
        }

        private fun onImportCode() {

            loadRequest.launch("")
        }

        private fun onExportCode() {
            // Set default filename.
            val jsonFileName = "AI-Code"
            saveRequest.launch(jsonFileName)
        }
    }
}
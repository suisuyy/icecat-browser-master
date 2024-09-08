/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.reference.browser.addons

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.FileProvider
import mozilla.components.feature.addons.Addon
import org.mozilla.reference.browser.R
import org.mozilla.reference.browser.ext.components

/**
 * An activity to manage add-ons.
 */
class AddonsActivity : AppCompatActivity() {
    private val selectXpiLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            org.mozilla.reference.browser.components.Core(this).installLocalXPI(this, it, onSuccess = {
                Toast.makeText(this, "Addon installed successfully", Toast.LENGTH_SHORT).show()
            }, onError = { exception ->
                Toast.makeText(this, "Failed to install addon: ${exception.message}", Toast.LENGTH_SHORT).show()
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_ons_man)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().apply {
                replace(R.id.container, AddonsFragment())
                commit()
            }
        }
        findViewById<Button>(R.id.button_select_xpi).setOnClickListener {
            openFilePicker()
        }
    }

    private fun openFilePicker() {
        selectXpiLauncher.launch("*/*")
    }
}



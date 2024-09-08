/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.reference.browser.tabs

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.tabstray.DefaultTabViewHolder
import mozilla.components.browser.tabstray.TabsAdapter
import mozilla.components.browser.tabstray.TabsTray
import mozilla.components.browser.tabstray.TabsTrayStyling
import mozilla.components.browser.tabstray.ViewHolderProvider
import mozilla.components.browser.thumbnails.loader.ThumbnailLoader
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.feature.tabs.tabstray.TabsFeature
import mozilla.components.support.base.feature.UserInteractionHandler
import org.mozilla.reference.browser.R
import org.mozilla.reference.browser.browser.BrowserFragment
import org.mozilla.reference.browser.ext.components
import org.mozilla.reference.browser.ext.requireComponents

/**
 * A fragment for displaying the tabs tray.
 */
class TabsTrayFragment : DialogFragment(), UserInteractionHandler {
    private var tabsFeature: TabsFeature? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Translucent_NoTitleBar)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_tabstray, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val trayAdapter = createAndSetupTabsTray(requireContext())

        tabsFeature = TabsFeature(
            trayAdapter,
            requireComponents.core.store,
            ::closeTabsTray,
        ) { !it.content.private }

        val tabsPanel: TabsPanel = view.findViewById(R.id.tabsPanel)
        val tabsToolbar: TabsToolbar = view.findViewById(R.id.tabsToolbar)

        tabsPanel.initialize(tabsFeature, updateTabsToolbar = ::updateTabsToolbar)
        tabsToolbar.initialize(tabsFeature) { closeTabsTray() }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog?.window?.setGravity(Gravity.TOP)
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
        dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH)
        tabsFeature?.start()
    }

    override fun onStop() {
        super.onStop()

        tabsFeature?.stop()
    }

    override fun onBackPressed(): Boolean {
        closeTabsTray()
        return true
    }

    private fun closeTabsTray() {
//        activity?.supportFragmentManager?.beginTransaction()?.apply {
//            if (activity?.supportFragmentManager?.findFragmentByTag("browser_fragment") == null) {
//                replace(R.id.container, BrowserFragment.create(), "browser_fragment")
//            }
//            commit()
//        }
        dismiss();
    }

    private fun updateTabsToolbar(isPrivate: Boolean) {
        val tabsToolbar = requireView().findViewById<TabsToolbar>(R.id.tabsToolbar)
        tabsToolbar.updateToolbar(isPrivate)
    }

    private fun createAndSetupTabsTray(context: Context): TabsTray {
        val layoutManager = LinearLayoutManager(context)
        val thumbnailLoader = ThumbnailLoader(context.components.core.thumbnailStorage)
        val trayStyling = TabsTrayStyling(itemBackgroundColor = Color.BLACK, itemTextColor = Color.WHITE)
        val viewHolderProvider: ViewHolderProvider = { viewGroup ->
            val view = LayoutInflater.from(context)
                .inflate(R.layout.browser_tabstray_item, viewGroup, false)

            DefaultTabViewHolder(view, thumbnailLoader)
        }
        
        val tabsAdapter = TabsAdapter(
            thumbnailLoader = thumbnailLoader,
            viewHolderProvider = viewHolderProvider,
            styling = trayStyling,
            delegate = object : TabsTray.Delegate {
                var lastSelectedTime: Long = 0;     
                var lastSelectedTabId: String? = null;
                override fun onTabSelected(tab: TabSessionState, source: String?) {
                    requireComponents.useCases.tabsUseCases.selectTab(tab.id)
                    if(System.currentTimeMillis() - lastSelectedTime < 1000 && lastSelectedTabId == tab.id) {  
                        closeTabsTray()
                    }
                    lastSelectedTime = System.currentTimeMillis();
                    lastSelectedTabId = tab.id;
                }

                override fun onTabClosed(tab: TabSessionState, source: String?) {
                    requireComponents.useCases.tabsUseCases.removeTab(tab.id)
                }

            },

        )

        val tabsTray = requireView().findViewById<RecyclerView>(R.id.tabsTray)
        tabsTray.layoutManager = layoutManager
        tabsTray.adapter = tabsAdapter

        TabsTouchHelper {
            requireComponents.useCases.tabsUseCases.removeTab(it.id)
        }.attachToRecyclerView(tabsTray)

        return tabsAdapter
    }
}

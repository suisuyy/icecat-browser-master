package org.mozilla.reference.browser.addons

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import androidx.fragment.app.DialogFragment
import mozilla.components.browser.state.action.WebExtensionAction
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.EngineView
import mozilla.components.concept.engine.window.WindowRequest
import mozilla.components.lib.state.ext.consumeFrom
import org.mozilla.reference.browser.R
import org.mozilla.reference.browser.ext.components

class WebExtensionActionPopupDialogFragment : DialogFragment(), EngineSession.Observer {
    private var engineSession: EngineSession? = null
    private lateinit var webExtensionId: String
    private var initialX = 0f
    private var initialY = 0f
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    private val addonSettingsEngineView: EngineView
        get() = requireView().findViewById<View>(R.id.addonSettingsEngineView) as EngineView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        webExtensionId = requireNotNull(arguments?.getString("web_extension_id"))
        engineSession = requireContext().components.core.store.state.extensions[webExtensionId]?.popupSession

        return inflater.inflate(R.layout.fragment_add_on_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = engineSession
        if (session != null) {
            addonSettingsEngineView.render(session)
            consumePopupSession()
        } else {
            consumeFrom(requireContext().components.core.store) { state ->
                state.extensions[webExtensionId]?.let { extState ->
                    extState.popupSession?.let {
                        if (engineSession == null) {
                            addonSettingsEngineView.render(it)
                            consumePopupSession()
                            engineSession = it
                        }
                    }
                }
            }
        }

        val closeButton = view.findViewById<Button>(R.id.close_button)
        closeButton.setOnClickListener {
            dismiss()
        }

        val resizeButton = view.findViewById<Button>(R.id.resize_button)
        resizeButton.setOnClickListener {
            resizePopup()
        }

        val draggableArea = view.findViewById<View>(R.id.draggable_area)
        draggableArea.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = dialog?.window?.attributes?.x?.toFloat() ?: 0f
                    initialY = dialog?.window?.attributes?.y?.toFloat() ?: 0f
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val offsetX = event.rawX - initialTouchX
                    val offsetY = event.rawY - initialTouchY
                    dialog?.window?.attributes = dialog?.window?.attributes?.apply {
                        x = (initialX + offsetX).toInt()
                        y = (initialY + offsetY).toInt()
                    }
                    dialog?.window?.attributes = dialog?.window?.attributes?.apply {
                        flags = flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            (resources.displayMetrics.heightPixels * 0.6).toInt()
        )
        engineSession?.register(this)
    }

    override fun onStop() {
        super.onStop()
        engineSession?.unregister(this)
    }

    override fun onWindowRequest(windowRequest: WindowRequest) {
        if (windowRequest.type == WindowRequest.Type.CLOSE) {
            dismiss()
        }
    }

    private fun consumePopupSession() {
        requireContext().components.core.store.dispatch(
            WebExtensionAction.UpdatePopupSessionAction(webExtensionId, popupSession = null),
        )
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setCanceledOnTouchOutside(false)
            window?.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            )
            window?.setFlags(
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            )
            window?.setDimAmount(0f)  // Set dim amount to 0 to prevent background dimming
            window?.attributes = window?.attributes?.apply {
                gravity = Gravity.NO_GRAVITY  // Remove any gravity constraints
                flags = flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            }
        }
    }

    private fun resizePopup() {
        val currentWidth = dialog?.window?.attributes?.width ?: 0
        val currentHeight = dialog?.window?.attributes?.height ?: 0

        val newSize = when {
            currentWidth == (resources.displayMetrics.widthPixels * 0.5).toInt() -> {
                Pair(
                    (resources.displayMetrics.widthPixels * 0.7).toInt(),
                    (resources.displayMetrics.heightPixels * 0.5).toInt()
                )
            }
            currentWidth == (resources.displayMetrics.widthPixels * 0.7).toInt() -> {
                Pair(
                    (resources.displayMetrics.widthPixels ).toInt(),
                    (resources.displayMetrics.heightPixels * 0.9).toInt()
                )
            }
            else -> {
                Pair(
                    (resources.displayMetrics.widthPixels * 0.5).toInt(),
                    (resources.displayMetrics.heightPixels * 0.4).toInt()
                )
            }
        }

        dialog?.window?.setLayout(newSize.first, newSize.second)
    }

    companion object {
        fun create(webExtensionId: String) = WebExtensionActionPopupDialogFragment().apply {
            arguments = Bundle().apply {
                putString("web_extension_id", webExtensionId)
            }
        }
    }
}
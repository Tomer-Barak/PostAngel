package com.postangel.screenshare

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat

/**
 * Dialog that explains how API keys are stored securely and used in the app
 */
class ApiKeySecurityDialog(context: Context) : Dialog(context) {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        setContentView(R.layout.dialog_api_key_security)          // Make links clickable and set HTML formatting
        val explanationText = findViewById<TextView>(R.id.securityExplanationText)
        explanationText.text = HtmlCompat.fromHtml(
            context.getString(R.string.api_key_security_explanation), 
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )
        explanationText.movementMethod = LinkMovementMethod.getInstance()
        
        // Set close button click listener
        findViewById<Button>(R.id.closeButton).setOnClickListener {
            dismiss()
        }
    }
    
    companion object {
        /**
         * Shows the API key security information dialog
         */
        fun show(context: Context) {
            ApiKeySecurityDialog(context).show()
        }
        
        /**
         * Shows a tooltip alert about API key security
         */
        fun showTooltip(context: Context) {
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_api_key_tooltip, null)
            
            val alertDialog = AlertDialog.Builder(context)
                .setView(dialogView)
                .create()
                
            dialogView.findViewById<Button>(R.id.btnMoreInfo).setOnClickListener {
                alertDialog.dismiss()
                show(context)
            }
            
            dialogView.findViewById<Button>(R.id.btnDismiss).setOnClickListener {
                alertDialog.dismiss()
            }
            
            alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            alertDialog.show()
        }
    }
}

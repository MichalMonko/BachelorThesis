package com.example.camerastreamapplication.fragments

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.example.camerastreamapplication.R

private const val HELP_MESSAGE_KEY = "help_message_resource"
private const val TITLE_MESSAGE_KEY = "title_message_resource"

class HelpFragment : DialogFragment(), View.OnClickListener
{
    // TODO: Rename and change types of parameters
    private var helpMessage: String? = null
    private var titleMessage: String? = null

    companion object
    {
        fun newInstance(titleMessage: String, helpMessage: String): HelpFragment
        {
            val fragment = HelpFragment()

            val arguments = Bundle()
            arguments.putString(TITLE_MESSAGE_KEY, titleMessage)
            arguments.putString(HELP_MESSAGE_KEY, helpMessage)

            fragment.arguments = arguments

            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        arguments?.let {
            helpMessage = it.getString(HELP_MESSAGE_KEY)
            titleMessage = it.getString(TITLE_MESSAGE_KEY)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View?
    {
        val rootView = inflater.inflate(R.layout.fragment_help, container, false)
        val titleView: TextView = rootView.findViewById(R.id.help_title_text_view)
        val helpView: TextView = rootView.findViewById(R.id.help_message_text_view)
        val returnButton: Button = rootView.findViewById(R.id.return_button)

        helpView.movementMethod = ScrollingMovementMethod()
        returnButton.setOnClickListener(this)

        if (helpMessage != null && titleMessage != null)
        {
            titleView.text = titleMessage
            helpView.text = helpMessage
        }
        else
        {
            throw IllegalArgumentException("Title or help message missing")
        }

        return rootView
    }

    override fun onClick(view: View?)
    {
        activity?.supportFragmentManager?.popBackStack()
    }
}

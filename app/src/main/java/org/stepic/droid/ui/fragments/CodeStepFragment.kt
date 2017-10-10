package org.stepic.droid.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import kotlinx.android.synthetic.main.fragment_step_attempt.*
import kotlinx.android.synthetic.main.view_code_quiz.*
import org.stepic.droid.R
import org.stepic.droid.base.App
import org.stepic.droid.core.presenters.CodePresenter
import org.stepic.droid.core.presenters.contracts.CodeView
import org.stepic.droid.model.Attempt
import org.stepic.droid.model.Reply
import org.stepic.droid.model.code.ProgrammingLanguage
import javax.inject.Inject

class CodeStepFragment : StepAttemptFragment(), CodeView {

    companion object {
        private const val CHOSEN_POSITION_KEY: String = "chosenPositionKey"
        fun newInstance(): CodeStepFragment = CodeStepFragment()
    }

    @Inject
    lateinit var codePresenter: CodePresenter

    override fun injectComponent() {
        App
                .componentManager()
                .stepComponent(step.id)
                .codeComponentBuilder()
                .build()
                .inject(this)
    }

    private var chosenProgrammingLanguageName: String? = null

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewGroup = (this.activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(R.layout.view_code_quiz, attemptContainer, false) as ViewGroup
        attemptContainer.addView(viewGroup)

        codeQuizChooseLangAction.setOnClickListener {
            val programmingLanguageServerName = codeQuizLanguagePicker.displayedValues[codeQuizLanguagePicker.value]
            chosenProgrammingLanguageName = programmingLanguageServerName

            val programmingLanguage = ProgrammingLanguage.serverNameToLanguage(programmingLanguageServerName)
            codeQuizAnswerField.text.clear()
            codeQuizAnswerField.setText(step.block?.options?.codeTemplates?.get(programmingLanguage))
            showLanguageChoosingView(false)
            showCodeQuizEditor()
        }

        codeQuizFullscreenAction.setOnClickListener {
            // TODO: 10/10/2017 implement
        }

        codeQuizResetAction.setOnClickListener {
            // TODO: 10/10/2017 implement
        }

        showCodeQuizEditor(false)
        codePresenter.attachView(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        codePresenter.detachView(this)
    }

    override fun showAttempt(attempt: Attempt) {
        codePresenter.onShowAttempt(attemptId = attempt.id, stepId = step.id)
    }

    private fun showLanguageChooser(languageNames: Array<String>) {
        codeQuizLanguagePicker.minValue = 0
        codeQuizLanguagePicker.maxValue = languageNames.size - 1
        codeQuizLanguagePicker.displayedValues = languageNames
        codeQuizLanguagePicker.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        codeQuizLanguagePicker.wrapSelectorWheel = false

        try {
            codeQuizLanguagePicker.setTextSize(50f) //Warning: reflection!
        } catch (exception: Exception) {
            //reflection failed -> ignore
        }

        showLanguageChoosingView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(CHOSEN_POSITION_KEY, codeQuizLanguagePicker.value)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.let {
            codeQuizLanguagePicker.value = it.getInt(CHOSEN_POSITION_KEY)
        }
    }

    private fun showCodeQuizEditor(needShow: Boolean = true) {
        val visibility = toVisibility(needShow)
        if (needShow) {
            codeQuizCurrentLanguage.text = chosenProgrammingLanguageName
        }

        codeQuizAnswerField.visibility = visibility
        stepAttemptSubmitButton.visibility = visibility
        codeQuizCurrentLanguage.visibility = visibility
        codeQuizDelimiter.visibility = visibility
        codeQuizFullscreenAction.visibility = visibility
        codeQuizResetAction.visibility = visibility
    }

    private fun showLanguageChoosingView(needShow: Boolean = true) {
        val visibility = toVisibility(needShow)

        codeQuizChooseLangAction.visibility = visibility
        codeQuizLanguagePicker.visibility = visibility
        codeQuizChooseLangTitle.visibility = visibility
    }

    private fun toVisibility(needShow: Boolean): Int {
        return if (needShow) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }


    override fun generateReply(): Reply {
        return Reply.Builder()
                .setLanguage(chosenProgrammingLanguageName)
                .setCode(codeQuizAnswerField.text.toString())
                .build()
    }

    override fun blockUIBeforeSubmit(needBlock: Boolean) {
        codeQuizAnswerField.isEnabled = !needBlock
    }

    override fun onRestoreSubmission() {
        val reply = submission.reply ?: return

        val text = reply.code
        codeQuizAnswerField.setText(text)
    }

    override fun onAttemptIsNotStored() {
        step.block?.options?.limits
                ?.keys
                ?.map {
                    it.serverPrintableName
                }
                ?.sorted()
                ?.toTypedArray()
                ?.let {
                    showLanguageChooser(it)
                }
    }

    override fun onShowStored(language: String, code: String) {
        chosenProgrammingLanguageName = language
        codeQuizAnswerField.text.clear()
        codeQuizAnswerField.setText(code)
        showLanguageChoosingView(false)
        showCodeQuizEditor()
    }
}

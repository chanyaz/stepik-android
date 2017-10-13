package org.stepic.droid.ui.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_step_attempt.*
import kotlinx.android.synthetic.main.view_code_quiz.*
import org.stepic.droid.R
import org.stepic.droid.analytic.Analytic
import org.stepic.droid.base.App
import org.stepic.droid.core.presenters.CodePresenter
import org.stepic.droid.core.presenters.contracts.CodeView
import org.stepic.droid.model.Attempt
import org.stepic.droid.model.Reply
import org.stepic.droid.model.Submission
import org.stepic.droid.ui.activities.CodePlaygroundActivity
import org.stepic.droid.ui.dialogs.ChangeCodeLanguageDialog
import org.stepic.droid.ui.dialogs.ResetCodeDialogFragment
import org.stepic.droid.ui.util.initForCodeLanguages
import javax.inject.Inject

class CodeStepFragment : StepAttemptFragment(),
        CodeView,
        ResetCodeDialogFragment.Callback,
        ChangeCodeLanguageDialog.Callback {
    companion object {
        private const val CHOSEN_POSITION_KEY: String = "chosenPositionKey"
        private const val CODE_PLAYGROUND_REQUEST = 153
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

            codeQuizAnswerField.setText(step.block?.options?.codeTemplates?.get(programmingLanguageServerName))
            showLanguageChoosingView(false)
            showCodeQuizEditor()
        }

        initLanguageChooser()

        codeQuizFullscreenAction.setOnClickListener {
            chosenProgrammingLanguageName?.let { lang ->
                if (submission?.status != Submission.Status.CORRECT) {
                    val intent = CodePlaygroundActivity.intentForLaunch(
                            activity,
                            codeQuizAnswerField.text.toString(),
                            lang,
                            step?.block?.options ?: throw IllegalStateException("can't find code options in code quiz"))

                    startActivityForResult(intent, CODE_PLAYGROUND_REQUEST)
                }
            }
        }

        codeQuizResetAction.setOnClickListener {
            if (checkForResetDialog()) {
                val dialog = ResetCodeDialogFragment.newInstance()
                if (!dialog.isAdded) {
                    dialog.show(childFragmentManager, null)
                }
            }
        }

        codeQuizCurrentLanguage.setOnClickListener {
            if (isOneLanguageAvailable()) {
                //we shouldn't change it
                return@setOnClickListener
            }

            if (checkForResetDialog()) {
                val dialog = ChangeCodeLanguageDialog.newInstance()
                if (!dialog.isAdded) {
                    dialog.show(childFragmentManager, null)
                }
            } else if (submission?.status != Submission.Status.CORRECT) {
                onChangeLanguage()
            }
        }

        initSamples()

        codePresenter.attachView(this)
    }

    private fun initSamples() {
        val newLine = "<br>"
        val stringBuilder = StringBuilder()
        step?.block?.options?.samples?.forEachIndexed { index, parcelableStringList ->
            if (index != 0) {
                stringBuilder.append(newLine)
                stringBuilder.append(newLine)
            }
            stringBuilder.append(getString(R.string.sample_input, index + 1))
            stringBuilder.append(newLine)
            stringBuilder.append(parcelableStringList[0])
            stringBuilder.append(newLine)
            stringBuilder.append(getString(R.string.sample_output, index + 1))
            stringBuilder.append(newLine)
            stringBuilder.append(parcelableStringList[1])

        }
        val samplesString = textResolver.fromHtml(stringBuilder.toString())
        codeQuizSamples.text = samplesString
    }

    override fun onDestroyView() {
        super.onDestroyView()
        codePresenter.detachView(this)
    }

    override fun showAttempt(attempt: Attempt) {
        codePresenter.onShowAttempt(attemptId = attempt.id, stepId = step.id)
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
        if (submission?.reply == null) {
            if (chosenProgrammingLanguageName == null) {
                //we haven't submission from the server and stored submission
                showLanguageChoosingView(false)
                showCodeQuizEditor()
            }
        } else {
            if (submission.status == Submission.Status.WRONG) {
                actionButton.setText(R.string.send)
                blockUIBeforeSubmit(false)
                if (submission.reply.code != codeQuizAnswerField.text.toString()) {
                    hideWrongStatus()
                    resetBackgroundOfAttempt()
                    hideHint()
                }
                submission = null
            } else {
                codeQuizAnswerField.setText(submission.reply.code)
                chosenProgrammingLanguageName = submission.reply.language
                showLanguageChoosingView(false)
                showCodeQuizEditor()
            }
        }
    }

    override fun onAttemptIsNotStored() {
        if (codeQuizAnswerField.visibility == View.VISIBLE) {
            return
        }

        if (isOneLanguageAvailable()) {
            val langTemplate = step?.block?.options?.codeTemplates?.entries?.singleOrNull()
            if (langTemplate == null) {
                analytic.reportEvent(Analytic.Error.TEMPLATE_WAS_NULL, step.id.toString())
                return
            }
            chosenProgrammingLanguageName = langTemplate.key
            codeQuizAnswerField.setText(langTemplate.value)

            showLanguageChoosingView(false)
            showCodeQuizEditor()
        } else {
            showCodeQuizEditor(false)
            showLanguageChoosingView()
        }
    }

    override fun onShowStored(language: String, code: String) {
        chosenProgrammingLanguageName = language
        codeQuizAnswerField.setText(code)
        showLanguageChoosingView(false)
        showCodeQuizEditor()
    }


    private fun initLanguageChooser() {
        step.block?.options?.limits
                ?.keys
                ?.sorted()
                ?.toTypedArray()
                ?.let {
                    codeQuizLanguagePicker.initForCodeLanguages(it)
                }
    }

    private fun showCodeQuizEditor(needShow: Boolean = true) {
        val visibility = toVisibility(needShow)
        if (needShow) {
            codeQuizCurrentLanguage.text = chosenProgrammingLanguageName
            step?.block?.options?.limits?.get(chosenProgrammingLanguageName)?.let { codeLimit ->
                val stringFromResources = getString(R.string.code_quiz_limits,
                        resources.getQuantityString(R.plurals.time_seconds, codeLimit.time, codeLimit.time),
                        codeLimit.memory.toString())
                val spanned = textResolver.fromHtml(stringFromResources)
                codeQuizLimits.setText(spanned)
            }

        }

        codeQuizAnswerField.visibility = visibility
        stepAttemptSubmitButton.visibility = visibility
        codeQuizCurrentLanguage.visibility = visibility
        codeQuizDelimiter.visibility = visibility
        codeQuizFullscreenAction.visibility = visibility
        codeQuizResetAction.visibility = visibility
        codeQuizLimits.visibility = visibility
    }

    private fun showLanguageChoosingView(needShow: Boolean = true) {
        val visibility = toVisibility(needShow)

        codeQuizChooseLangAction.visibility = visibility
        codeQuizLanguagePicker.visibility = visibility
        codeQuizChooseLangTitle.visibility = visibility
    }

    override fun onReset() {
        chosenProgrammingLanguageName?.let {
            val template = step?.block?.options?.codeTemplates?.get(it)
            codeQuizAnswerField.setText(template)
            resetBackgroundOfAttempt()
            hideHint()
            hideWrongStatus()
        }
    }

    override fun onChangeLanguage() {
        if (isOneLanguageAvailable()) {
            //we shouldn't do anything
            return
        }
        showCodeQuizEditor(false)
        showLanguageChoosingView()
        resetBackgroundOfAttempt()
        hideHint()
        hideWrongStatus()
    }

    private fun checkForResetDialog(): Boolean {
        chosenProgrammingLanguageName?.let {
            val template = step.block?.options?.codeTemplates?.get(it)
            val needDialog = template != null &&
                    template != codeQuizAnswerField.text.toString() &&
                    submission?.status != Submission.Status.CORRECT
            return needDialog
        }
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CODE_PLAYGROUND_REQUEST && resultCode == Activity.RESULT_OK) {
            chosenProgrammingLanguageName = data?.getStringExtra(CodePlaygroundActivity.LANG_KEY)
            val newCode = data?.getStringExtra(CodePlaygroundActivity.CODE_KEY)
            codeQuizAnswerField.setText(newCode)
            showCodeQuizEditor()
        }
    }

    private fun isOneLanguageAvailable(): Boolean =
            step?.block?.options?.codeTemplates?.size == 1

}
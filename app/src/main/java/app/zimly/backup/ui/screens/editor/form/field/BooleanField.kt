package app.zimly.backup.ui.screens.editor.form.field

class BooleanField(
    errorMessage: String = "This field is required.",
    validate: (value: Boolean) -> Boolean = { true },
    defaultValue: Boolean = false
) : BaseField<Boolean>(errorMessage, validate, defaultValue)
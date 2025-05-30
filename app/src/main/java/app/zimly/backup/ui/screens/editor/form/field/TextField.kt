package app.zimly.backup.ui.screens.editor.form.field

class TextField(
    errorMessage: String = "This field is required.",
    validate: (value: String) -> Boolean = { it.isNotEmpty() },
    defaultValue: String = ""
) : BaseField<String>(errorMessage, validate, defaultValue)
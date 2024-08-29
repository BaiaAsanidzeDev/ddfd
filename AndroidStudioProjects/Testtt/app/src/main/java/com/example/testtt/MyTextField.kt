package com.example.testtt

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource


@Composable
fun MyTextField(
    modifier: Modifier = Modifier,
    textFieldValue: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean = false,
    singleLine: Boolean = true,
    fieldType: FieldType = FieldType.Text,
    isEnabled: Boolean = true
) {
    val supportingText = when {
        textFieldValue.isEmpty() && fieldType == FieldType.Text -> stringResource(id = R.string.label)
        fieldType == FieldType.Email && !android.util.Patterns.EMAIL_ADDRESS.matcher(textFieldValue)
            .matches() -> stringResource(id = R.string.label)

        else -> ""
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(modifier = modifier.fillMaxWidth(),
            enabled = isEnabled,
            singleLine = singleLine,
            value = textFieldValue,
            isError = isError,
            onValueChange = { value ->
                onValueChange(value)
            },
            label = { Text(text = label) },
            shape = RoundedCornerShape(8),
            trailingIcon = {
                val condition = when (fieldType) {
                    FieldType.Text -> {
                        textFieldValue.isNotEmpty()
                    }

                    FieldType.Email -> {
                        android.util.Patterns.EMAIL_ADDRESS.matcher(textFieldValue).matches()
                    }
                }
                if (condition) {
                    Icon(imageVector = Icons.Filled.Check, contentDescription = null)
                }
            })
        if (isError) {
        }
    }
}

enum class FieldType {
    Email, Text
}


package com.traffic_guard.ai.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

enum class AuthInputType {
    EMAIL, PASSWORD, PHONE, TEXT
}

@Composable
fun AuthInputCard(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    inputType: AuthInputType,
    modifier: Modifier = Modifier,
    error: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var passwordVisible by remember { mutableStateOf(false) }

    val leadingIcon = when (inputType) {
        AuthInputType.EMAIL -> Icons.Default.Email
        AuthInputType.PASSWORD -> Icons.Default.Lock
        AuthInputType.PHONE -> Icons.Default.Phone
        AuthInputType.TEXT -> Icons.Default.Person
    }

    val trailingIcon = if (inputType == AuthInputType.PASSWORD) {
        if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
    } else null

    val visualTransformation = if (inputType == AuthInputType.PASSWORD && !passwordVisible) {
        PasswordVisualTransformation()
    } else {
        VisualTransformation.None
    }

    val keyboardOptions = KeyboardOptions(
        keyboardType = when (inputType) {
            AuthInputType.EMAIL -> KeyboardType.Email
            AuthInputType.PASSWORD -> KeyboardType.Password
            AuthInputType.PHONE -> KeyboardType.Phone
            AuthInputType.TEXT -> KeyboardType.Text
        },
        imeAction = imeAction
    )

    // Wrap elegant AppTextField component with correct properties
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        onTrailingIconClick = if (inputType == AuthInputType.PASSWORD) {
            { passwordVisible = !passwordVisible }
        } else null,
        error = error,
        singleLine = true,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = modifier.fillMaxWidth()
    )
}

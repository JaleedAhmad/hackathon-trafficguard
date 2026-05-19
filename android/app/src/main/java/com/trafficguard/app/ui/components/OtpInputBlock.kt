package com.traffic_guard.ai.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traffic_guard.ai.theme.AccentBlue
import com.traffic_guard.ai.theme.DarkBorder
import com.traffic_guard.ai.theme.LightBorder

@Composable
fun OtpInputBlock(
    code: String,
    onCodeChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
    isError: Boolean = false
) {
    val isDark = MaterialTheme.colorScheme.background.value == 0xFF0F172A.toULong()
    
    // We create focus requesters to manage focus traversal
    val focusRequesters = remember { List(length) { FocusRequester() } }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        for (i in 0 until length) {
            val char = code.getOrNull(i)?.toString() ?: ""
            val focusRequester = focusRequesters[i]

            OutlinedTextField(
                value = char,
                onValueChange = { newVal ->
                    if (newVal.length <= 1) {
                        val newCode = StringBuilder(code)
                        if (i < code.length) {
                            if (newVal.isEmpty()) {
                                newCode.deleteAt(i)
                            } else {
                                newCode.setCharAt(i, newVal[0])
                            }
                        } else {
                            if (newVal.isNotEmpty()) {
                                newCode.append(newVal)
                            }
                        }
                        
                        onCodeChanged(newCode.toString())

                        // Traverse focus forwards
                        if (newVal.isNotEmpty() && i < length - 1) {
                            focusRequesters[i + 1].requestFocus()
                        }
                        // Traverse focus backwards
                        if (newVal.isEmpty() && i > 0) {
                            focusRequesters[i - 1].requestFocus()
                        }
                    }
                },
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    textAlign = TextAlign.Center, 
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = if (isDark) Color.White else Color.Black,
                    unfocusedTextColor = if (isDark) Color.White else Color.Black,
                    focusedBorderColor = if (isError) Color.Red else AccentBlue,
                    unfocusedBorderColor = if (isError) Color.Red else if (isDark) DarkBorder else LightBorder,
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .focusRequester(focusRequester)
            )
        }
    }
}

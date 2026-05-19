package com.traffic_guard.ai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.traffic_guard.ai.data.EmergencyCenter
import com.traffic_guard.ai.theme.AccentRed
import com.traffic_guard.ai.theme.DarkBgCard
import com.traffic_guard.ai.theme.DarkBorder
import com.traffic_guard.ai.theme.LightBgCard
import com.traffic_guard.ai.theme.LightBorder

@Composable
fun EmergencyRouteCard(
    center: EmergencyCenter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.value == 0xFF0F172A.toULong()
    val isHospital = center.type == "HOSPITAL"

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) DarkBgCard else LightBgCard
        ),
        border = BorderStroke(1.dp, if (isDark) DarkBorder else LightBorder),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = if (isHospital) Icons.Default.LocalHospital else Icons.Default.Security,
                contentDescription = center.type,
                tint = if (isHospital) AccentRed else Color(0xFF10B981),
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = center.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) Color.White else Color.Black
                )
                Text(
                    text = "${center.distanceMeters}m away • ${center.estimatedMinutes} min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDark) Color.LightGray else Color.DarkGray
                )
            }
        }
    }
}

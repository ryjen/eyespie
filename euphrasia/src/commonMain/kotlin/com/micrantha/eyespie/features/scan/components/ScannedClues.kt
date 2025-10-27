package com.micrantha.eyespie.features.scan.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.micrantha.bluebell.arch.Dispatch
import com.micrantha.bluebell.ui.theme.Dimensions
import com.micrantha.eyespie.features.scan.entities.ScanClue
import com.micrantha.eyespie.features.scan.entities.ScanEditAction


@Composable
fun ScannedClues(modifier: Modifier = Modifier, clues: Collection<ScanClue>, dispatch: Dispatch) {

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimensions.content)
    ) {
        items(clues.toList()) { clue ->
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimensions.Border.small),
                border = BorderStroke(
                    if (clue.isSelected) 3.dp else 1.dp,
                    if (clue.isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Gray.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                onClick = { dispatch(ScanEditAction.SelectClue(clue.id)) }
            ) {
                Text(
                    overflow = TextOverflow.Visible,
                    softWrap = true,
                    text = "${clue.clue} (${clue.answer})"
                )
            }
        }
    }
}

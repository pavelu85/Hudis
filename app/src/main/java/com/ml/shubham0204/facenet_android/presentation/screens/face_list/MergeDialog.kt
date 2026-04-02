package com.ml.shubham0204.facenet_android.presentation.screens.face_list

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ml.shubham0204.facenet_android.data.PersonRecord
import kotlin.math.roundToInt

@Composable
fun MergeDialog(
    state: MergeDialogState,
    onConfirm: (keepId: Long, removeIds: List<Long>, name: String, notes: String, photoPath: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val persons = state.persons

    // Pre-select primary: person with most images; tie → first in list
    val defaultPrimary = persons.maxByOrNull { it.numImages } ?: persons.first()
    var primaryPerson by remember { mutableStateOf(defaultPrimary) }

    // Name pre-filled from primary; updates when primary changes
    var name by remember(primaryPerson) { mutableStateOf(primaryPerson.personName) }

    // Combine all non-blank notes, deduplicated
    val combinedNotes = persons.map { it.notes }.filter { it.isNotBlank() }.joinToString(", ")
    var notes by remember { mutableStateOf(combinedNotes) }

    // Photo: prefer primary's photo; fallback to first person with a photo
    val defaultPhoto = primaryPerson.profilePhotoPath
        ?: persons.firstOrNull { it.profilePhotoPath != null }?.profilePhotoPath
    var chosenPhotoPath by remember(primaryPerson) { mutableStateOf(defaultPhoto) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Merge ${persons.size} Persons") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.similarity > 0f) {
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                "Similarity: ${(state.similarity * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    )
                }

                // Primary person selector
                Text(
                    "Surviving person:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    persons.forEach { person ->
                        PersonCard(
                            person = person,
                            isPrimary = person.personID == primaryPerson.personID,
                            isPhotoSelected = chosenPhotoPath != null && chosenPhotoPath == person.profilePhotoPath,
                            onClick = {
                                primaryPerson = person
                                // Auto-switch photo to new primary if they have one
                                if (person.profilePhotoPath != null) chosenPhotoPath = person.profilePhotoPath
                            },
                        )
                    }
                }

                // Photo picker (only show if any person has a photo and primary doesn't auto-cover it)
                val personsWithPhotos = persons.filter { it.profilePhotoPath != null }
                if (personsWithPhotos.size > 1) {
                    Text(
                        "Profile photo:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        personsWithPhotos.forEach { person ->
                            PhotoChip(
                                person = person,
                                isSelected = chosenPhotoPath == person.profilePhotoPath,
                                onClick = { chosenPhotoPath = person.profilePhotoPath },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Merged name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val removeIds = persons
                        .filter { it.personID != primaryPerson.personID }
                        .map { it.personID }
                    onConfirm(primaryPerson.personID, removeIds, name.trim(), notes.trim(), chosenPhotoPath)
                },
                enabled = name.isNotBlank(),
            ) {
                Text("Merge")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(16.dp),
    )
}

@Composable
private fun PersonCard(
    person: PersonRecord,
    isPrimary: Boolean,
    isPhotoSelected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val borderWidth = if (isPrimary) 2.dp else 1.dp

    ElevatedCard(
        modifier = Modifier
            .width(96.dp)
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (person.profilePhotoPath != null) {
                AsyncImage(
                    model = person.profilePhotoPath,
                    contentDescription = "Photo of ${person.personName}",
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = person.personName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "${person.numImages} img(s)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isPrimary) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "Primary",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

@Composable
private fun PhotoChip(
    person: PersonRecord,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AsyncImage(
            model = person.profilePhotoPath,
            contentDescription = person.personName,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .border(borderWidth, borderColor, CircleShape),
            contentScale = ContentScale.Crop,
        )
        Text(
            text = person.personName,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

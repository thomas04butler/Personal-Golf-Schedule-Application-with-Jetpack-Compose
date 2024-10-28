package com.example.coursework

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import org.json.JSONObject
import com.example.coursework.models.GolfResult
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun GolfHistoryScreen(username: String, onBackPressed: () -> Unit) {
    val context = LocalContext.current
    val golfResultTable = remember { mutableStateListOf<GolfResultTableRow>() }
    val selectedRowIndex = remember { mutableStateOf(-1) }

    // Fetch golf results when the composable is initialized
    LaunchedEffect(Unit) {
        val results = fetchGolfResults(context)
        golfResultTable.addAll(results)
    }

    Surface(color = Color.White, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Back button as an arrow in the top left corner
            IconButton(
                onClick = { onBackPressed() },
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                text = "Welcome to your Golf Results",
                textAlign = TextAlign.Center,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            GolfResultTable(
                golfResultTable = golfResultTable,
                selectedRowIndex = selectedRowIndex.value,
                onRowClick = { index ->
                    selectedRowIndex.value = if (selectedRowIndex.value == index) -1 else index
                },
                onDeleteClick = { index ->
                    // Call deleteGolfResult function with the id of the row to delete
                    deleteGolfResult(context, golfResultTable[index].id)
                    // Remove the row from the list after deleting it from the database
                    golfResultTable.removeAt(index)
                }
            )

            AddGolfResultForm(context = context) { newRow ->
                golfResultTable.add(newRow)
            }
        }
    }
}

fun fetchGolfResults(context: Context): List<GolfResultTableRow> {
    val golfResultUri = Uri.parse("content://com.example.coursework.provider.golfresult/golf_result")
    val cursor = context.contentResolver.query(golfResultUri, null, null, null, null)
    val results = mutableListOf<GolfResultTableRow>()

    cursor?.use {
        while (it.moveToNext()) {
            val id = it.getLong(it.getColumnIndexOrThrow(GolfResultDatabaseHelper.COLUMN_ID))
            val datePlayed = it.getString(it.getColumnIndexOrThrow(GolfResultDatabaseHelper.COLUMN_DATE_PLAYED))
            val courseName = it.getString(it.getColumnIndexOrThrow(GolfResultDatabaseHelper.COLUMN_GOLF_COURSE))
            val numberOfHoles = it.getInt(it.getColumnIndexOrThrow(GolfResultDatabaseHelper.COLUMN_NUMBER_OF_HOLES))
            val sumOfPar = it.getInt(it.getColumnIndexOrThrow(GolfResultDatabaseHelper.COLUMN_PAR))
            val sumOfScore = it.getInt(it.getColumnIndexOrThrow(GolfResultDatabaseHelper.COLUMN_SCORE))
            val parValues = it.getString(it.getColumnIndexOrThrow(GolfResultDatabaseHelper.COLUMN_HOLE_PAR)).split(",")
            val scoreValues = it.getString(it.getColumnIndexOrThrow(GolfResultDatabaseHelper.COLUMN_HOLE_SCORE)).split(",")

            results.add(GolfResultTableRow(id, datePlayed, courseName, numberOfHoles, sumOfPar, sumOfScore, parValues, scoreValues))
        }
    }

    return results
}

fun deleteGolfResult(context: Context, id: Long) {
    val uri = Uri.parse("content://com.example.coursework.provider.golfresult/golf_result")
    context.contentResolver.delete(uri, "${GolfResultDatabaseHelper.COLUMN_ID} = ?", arrayOf(id.toString()))
}

@Composable
fun GolfResultTable(
    golfResultTable: List<GolfResultTableRow>,
    selectedRowIndex: Int,
    onRowClick: (Int) -> Unit,
    onDeleteClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        // Table headers
        TableHeaderRow()

        // Table rows
        Column(
            modifier = Modifier.padding(top = 8.dp)
        ) {
            golfResultTable.forEachIndexed { index, row ->
                GolfResultRow(
                    row = row,
                    selected = index == selectedRowIndex,
                    onClick = { onRowClick(index) },
                    onDeleteClick = { onDeleteClick(index) } // Pass the onDeleteClick callback
                )
                // Display detailed row information if the row is selected
                if (index == selectedRowIndex) {
                    GolfResultDetailRow(row)
                }
                // Add spacing between rows
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun AddGolfResultForm(
    context: Context,
    onAddRow: (GolfResultTableRow) -> Unit
) {
    var datePlayed by remember { mutableStateOf(TextFieldValue("")) }
    var courseName by remember { mutableStateOf(TextFieldValue("")) }
    var numberOfHoles by remember { mutableStateOf(TextFieldValue("")) }
    var parValues by remember { mutableStateOf(List(18) { TextFieldValue("") }) }
    var scoreValues by remember { mutableStateOf(List(18) { TextFieldValue("") }) }
    var dateError by remember { mutableStateOf(false) }
    var holesError by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        OutlinedTextField(
            value = datePlayed,
            onValueChange = {
                datePlayed = it
                dateError = !isValidDateFormat1(it.text)
            },
            label = { Text("Date Played (DD-MM-YYYY)") },
            isError = dateError,
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number
            ),
            modifier = Modifier.fillMaxWidth()
        )
        if (dateError) {
            Text(
                text = "Invalid date format. Use DD-MM-YYYY",
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = courseName,
            onValueChange = { courseName = it },
            label = { Text("Course Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = numberOfHoles,
            onValueChange = {
                numberOfHoles = it
                val numberOfHolesInt = it.text.toIntOrNull()
                holesError = numberOfHolesInt == null || numberOfHolesInt !in 1..18
            },
            label = { Text("Number of Holes") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = holesError,
            modifier = Modifier.fillMaxWidth()
        )
        if (holesError) {
            Text(
                text = "Number of holes must be between 1 and 18",
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val numberOfHolesInt = numberOfHoles.text.toIntOrNull() ?: 0
        for (i in 0 until numberOfHolesInt) {
            HoleResultRow(
                holeNumber = i + 1,
                parValue = parValues.getOrNull(i)?.text ?: "",
                scoreValue = scoreValues.getOrNull(i)?.text ?: "",
                onParValueChange = { newValue -> parValues = parValues.toMutableList().also { it[i] = TextFieldValue(newValue) } },
                onScoreValueChange = { newValue -> scoreValues = scoreValues.toMutableList().also { it[i] = TextFieldValue(newValue) } }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (!dateError && !holesError) {
                    val sumOfPar = parValues.take(numberOfHolesInt).sumBy { it.text.toIntOrNull() ?: 0 }
                    val sumOfScore = scoreValues.take(numberOfHolesInt).sumBy { it.text.toIntOrNull() ?: 0 }
                    val newRow = GolfResultTableRow(
                        id = UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE,
                        datePlayed = datePlayed.text,
                        courseName = courseName.text,
                        numberOfHoles = numberOfHolesInt,
                        sumOfPar = sumOfPar,
                        sumOfScore = sumOfScore,
                        parValues = parValues.take(numberOfHolesInt).map { it.text },
                        scoreValues = scoreValues.take(numberOfHolesInt).map { it.text }
                    )
                    onAddRow(newRow)
                    sendEmail(context, newRow)

                    val values = ContentValues().apply {
                        put(GolfResultDatabaseHelper.COLUMN_DATE_PLAYED, newRow.datePlayed)
                        put(GolfResultDatabaseHelper.COLUMN_GOLF_COURSE, newRow.courseName)
                        put(GolfResultDatabaseHelper.COLUMN_NUMBER_OF_HOLES, newRow.numberOfHoles)
                        put(GolfResultDatabaseHelper.COLUMN_PAR, newRow.sumOfPar)
                        put(GolfResultDatabaseHelper.COLUMN_SCORE, newRow.sumOfScore)
                        put(GolfResultDatabaseHelper.COLUMN_HOLE_PAR, newRow.parValues.joinToString(","))
                        put(GolfResultDatabaseHelper.COLUMN_HOLE_SCORE, newRow.scoreValues.joinToString(","))
                    }

                    val golfResultUri = Uri.parse("content://com.example.coursework.provider.golfresult/golf_result")
                    context.contentResolver.insert(golfResultUri, values)

                    datePlayed = TextFieldValue("")
                    courseName = TextFieldValue("")
                    numberOfHoles = TextFieldValue("")
                    parValues = List(18) { TextFieldValue("") }
                    scoreValues = List(18) { TextFieldValue("") }
                } else {
                    focusRequester.requestFocus()
                }
            },
            modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Blue,
                contentColor = Color.White
            )
        ) {
            Text("Add Row")
        }
    }
}

private fun sendEmail(context: Context, newRow: GolfResultTableRow) {
    val resultDetails = buildString {
        append("Date Played: ${newRow.datePlayed}\n")
        append("Course Name: ${newRow.courseName}\n")
        append("Number of Holes: ${newRow.numberOfHoles}\n")
        append("Sum of Par: ${newRow.sumOfPar}\n")
        append("Sum of Score: ${newRow.sumOfScore}\n")
        append("Par Values: ${newRow.parValues.joinToString()}\n")
        append("Score Values: ${newRow.scoreValues.joinToString()}")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_EMAIL, arrayOf("thomas04butler@gmail.com"))
        putExtra(Intent.EXTRA_SUBJECT, "New Golf Result")
        putExtra(Intent.EXTRA_TEXT, "New golf result details:\n$resultDetails")
    }

    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
    }
}

fun isValidDateFormat1(input: String): Boolean {
    val regex = """\d{2}-\d{2}-\d{4}""".toRegex()
    return regex.matches(input)
}

@Composable
fun HoleResultRow(
    holeNumber: Int,
    parValue: String,
    scoreValue: String,
    onParValueChange: (String) -> Unit,
    onScoreValueChange: (String) -> Unit
) {
    var parError by remember { mutableStateOf(false) }
    var scoreError by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
    ) {
        Text(text = "Hole $holeNumber", modifier = Modifier.weight(1f))

        OutlinedTextField(
            value = parValue,
            onValueChange = {
                onParValueChange(it)
                parError = !isValidParValue(it)
            },
            label = { Text("Par") },
            isError = parError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
        if (parError) {
            Text(
                text = "Par must be 3, 4, or 5",
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }

        OutlinedTextField(
            value = scoreValue,
            onValueChange = {
                onScoreValueChange(it)
                scoreError = !isValidScoreValue(it)
            },
            label = { Text("Score") },
            isError = scoreError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
        if (scoreError) {
            Text(
                text = "Score must be a number",
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

fun isValidParValue(input: String): Boolean {
    return input in listOf("3", "4", "5")
}

fun isValidScoreValue(input: String): Boolean {
    return input.toIntOrNull() != null
}

@Composable
fun TableHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Date",
            modifier = Modifier.weight(1.5f),
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Course",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Nº Holes",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Par",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Score",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.weight(0.5f)) // Adjusted weight for smaller last column
    }
}

@Composable
fun GolfResultRow(
    row: GolfResultTableRow,
    selected: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = row.datePlayed,
            modifier = Modifier.weight(1.5f),
            textAlign = TextAlign.Center
        )
        Text(
            text = row.courseName,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            text = row.numberOfHoles.toString(),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            text = row.sumOfPar.toString(),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            text = row.sumOfScore.toString(),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        IconButton(
            onClick = onDeleteClick,
            modifier = Modifier.weight(0.5f)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = Color.Red
            )
        }
    }
}

@Composable
fun GolfResultDetailRow(row: GolfResultTableRow) {
    Column(modifier = Modifier.padding(8.dp)) {
        row.parValues.zip(row.scoreValues).forEachIndexed { index, (par, score) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .fillMaxWidth()
            ) {
                // Adjust text size and weight
                Text(
                    text = "Hole ${index + 1}",
                    modifier = Modifier.weight(1f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Par: $par",
                    modifier = Modifier.weight(1f),
                    fontSize = 14.sp
                )
                Text(
                    text = "Score: $score",
                    modifier = Modifier.weight(1f),
                    fontSize = 14.sp
                )
            }
        }
    }
}

data class GolfResultTableRow(
    val id: Long,
    val datePlayed: String,
    val courseName: String,
    val numberOfHoles: Int,
    val sumOfPar: Int,
    val sumOfScore: Int,
    val parValues: List<String>,
    val scoreValues: List<String>
)

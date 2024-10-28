package com.example.coursework

import android.app.Activity
import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import android.content.Intent
import android.content.pm.PackageManager
import android.health.connect.datatypes.ExerciseRoute
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import org.json.JSONObject
import com.example.coursework.models.GolfEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.core.content.ContextCompat
import java.security.AccessController.checkPermission
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.location.LocationManager

@Composable
fun GolfScheduleScreen(username: String, onBackPressed: () -> Unit) {
    Log.d("Schedule", "Schedule for: $username")
    val context = LocalContext.current
    var golfEvents by remember { mutableStateOf(emptyList<GolfEvent>()) }
    var selectedEventIndex by remember { mutableStateOf(-1) }
    var weatherInfo by remember { mutableStateOf("") }

    // Launch effect to fetch golf events from the content provider
    LaunchedEffect(Unit) {
        fetchGolfEvents(context) { events ->
            golfEvents = events
        }
    }

    // Remember add row button click handler to prevent unnecessary recomposition
    val addRowButtonClickHandler = remember {
        { date: String, time: String, golfCourse: String, location: String ->
            val values = ContentValues().apply {
                put(GolfEventDatabaseHelper.COLUMN_DATE, date)
                put(GolfEventDatabaseHelper.COLUMN_TIME, time)
                put(GolfEventDatabaseHelper.COLUMN_GOLF_COURSE, golfCourse)
                put(GolfEventDatabaseHelper.COLUMN_LOCATION, location)
            }
            context.contentResolver.insert(GolfEventContentProvider.CONTENT_URI, values)
            fetchGolfEvents(context) { events ->
                golfEvents = events
            }
        }
    }

    // Delete row handler
    val deleteRowHandler = { id: Long ->
        deleteRow(context, id)
        fetchGolfEvents(context) { events ->
            golfEvents = events
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

            val uri = if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                Log.d("UserLocation", "Location: $latitude, $longitude")
                Uri.parse("geo:$latitude,$longitude?q=golf+courses")
            } else {
                Log.d("UserLocation", "Location is null, using default location")
                Uri.parse("geo:0,0?q=golf+courses")
            }

            val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
            }
            context.startActivity(mapIntent)
        } else {
            Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Main UI layout
    Surface(color = Color.White) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
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
                text = "Welcome to your Golf Schedule",
                textAlign = TextAlign.Center,
                fontSize = 30.sp,
                modifier = Modifier.padding(16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Table for displaying golf events
            Table(
                headers = listOf("DATE", "TIME", "COURSE", "LOCATION"),
                data = golfEvents,
                onLocationClick = { index ->
                    selectedEventIndex = if (selectedEventIndex == index) {
                        -1 // Deselect if clicked again
                    } else {
                        index // Select the clicked event
                    }
                    // Clear weather info when a different row is clicked
                    weatherInfo = ""
                },
                onDeleteClick = deleteRowHandler
            )
            // Details of selected event
            selectedEventIndex.takeIf { it != -1 }?.let { index ->
                val event = golfEvents[index]
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "Date: ${event.date}",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Time: ${event.time}",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Postcode: ${event.location}",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                fetchWeatherData(event.golfCourse, event.date, event.time, event.location) { weather ->
                                    weatherInfo = weather
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("View Weather")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = {
                                val location = event.location
                                Log.d("Maps", "Opening maps: $location")
                                val uri = Uri.parse("geo:0,0?q=$location")
                                val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                                mapIntent.setPackage("com.google.android.apps.maps")
                                context.startActivity(mapIntent)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("View Location")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Display weather information
                    Log.d("Weather", weatherInfo)
                    Text(weatherInfo)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            AddRowButton(onAddRow = addRowButtonClickHandler)


            Button(
                onClick = {
                    selectedEventIndex = -1

                    val permission = Manifest.permission.ACCESS_FINE_LOCATION
                    locationPermissionLauncher.launch(permission)
                },
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth()
            ) {
                Text("View Courses Near Me")
            }
        }
    }
}

fun shouldShowRequestPermissionRationale(context: Context, permission: String): Boolean {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        (context as? Activity)?.shouldShowRequestPermissionRationale(permission) ?: false
    } else {
        false
    }
}

@Composable
fun Table(
    headers: List<String>,
    data: List<GolfEvent>,
    onLocationClick: (Int) -> Unit,
    onDeleteClick: (Long) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            headers.forEach { header ->
                Text(
                    text = header,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        data.forEach { event ->
            Row(
                modifier = Modifier
                    .clickable { onLocationClick(data.indexOf(event)) } // Make the row clickable
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .background(
                        if (data.indexOf(event) % 2 == 0) Color.White else Color.LightGray
                    ),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = event.date,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Normal
                )
                Text(
                    text = event.time,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Normal
                )
                Text(
                    text = event.golfCourse,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Normal
                )
                Text(
                    text = event.location,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Normal
                )
                Button(
                    onClick = { onDeleteClick(event.id) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    ),
                    modifier = Modifier.size(width = 10.dp, height = 10.dp)
                ) {
                    Text(text = "")
                }
            }
        }
    }
}



fun deleteRow(context: Context, id: Long) {
    GlobalScope.launch {
        context.contentResolver.delete(
            GolfEventContentProvider.CONTENT_URI,
            "${GolfEventDatabaseHelper.COLUMN_ID}=?",
            arrayOf(id.toString())
        )
    }
}

fun fetchGolfEvents(context: Context, onGolfEventsFetched: (List<GolfEvent>) -> Unit) {
    GlobalScope.launch {
        val cursor = context.contentResolver.query(
            GolfEventContentProvider.CONTENT_URI,
            null, null, null, null
        )

        val golfEvents = mutableListOf<GolfEvent>()
        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(GolfEventDatabaseHelper.COLUMN_ID))
                val date = it.getString(it.getColumnIndexOrThrow(GolfEventDatabaseHelper.COLUMN_DATE))
                val time = it.getString(it.getColumnIndexOrThrow(GolfEventDatabaseHelper.COLUMN_TIME))
                val golfCourse = it.getString(it.getColumnIndexOrThrow(GolfEventDatabaseHelper.COLUMN_GOLF_COURSE))
                val location = it.getString(it.getColumnIndexOrThrow(GolfEventDatabaseHelper.COLUMN_LOCATION))
                golfEvents.add(GolfEvent(id, date, time, golfCourse, location))
            }
        }
        onGolfEventsFetched(golfEvents)
    }
}

fun fetchWeatherData(golfCourse: String, date: String, time: String, location: String, onWeatherInfoFetched: (String) -> Unit) {
    val apiKey = "2bfd2ed3c510fe1a01c866d524184280"
    GlobalScope.launch {
        var weatherInfo = ""
        try {
            val coordinates = getCoordinatesFromPostalCode(location)
            if (coordinates != null) {
                val (latitude, longitude) = coordinates
                val urlString = "https://api.openweathermap.org/data/2.5/weather?lat=$latitude&lon=$longitude&appid=$apiKey&units=metric"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val scanner = Scanner(inputStream)
                    val responseBody = StringBuilder()
                    while (scanner.hasNextLine()) {
                        responseBody.append(scanner.nextLine())
                    }
                    scanner.close()
                    inputStream.close()

                    val jsonResponse = JSONObject(responseBody.toString())
                    val main = jsonResponse.getJSONObject("main")
                    val temp = main.getDouble("temp")

                    val weatherArray = jsonResponse.getJSONArray("weather")
                    val weatherObject = weatherArray.getJSONObject(0)
                    val weatherDescription = weatherObject.getString("description")

                    val rain = if (jsonResponse.has("rain") && jsonResponse.getJSONObject("rain").has("1h")) {
                        val rainVolume = jsonResponse.getJSONObject("rain").getDouble("1h")
                        if (rainVolume > 0) "It's going to rain" else "It's not going to rain"
                    } else {
                        "It's not going to rain"
                    }

                    weatherInfo = "Temperature: $temp°C\nWeather: $weatherDescription\n$rain"
                } else {
                    weatherInfo = "Error: Failed to fetch weather data (${connection.responseMessage})"
                }
                connection.disconnect()
            } else {
                weatherInfo = "Error: Could not find coordinates for the given postal code"
            }
        } catch (e: Exception) {
            weatherInfo = "Errors: ${e.message}"
        }
        onWeatherInfoFetched(weatherInfo)
    }
}

private suspend fun getCoordinatesFromPostalCode(postalCode: String): Pair<Double, Double>? {
    val apiKey = "AIzaSyBTuY4zxBHjc7cn03oZUsvqrJKdqmqPkjE"
    val urlString = "https://maps.googleapis.com/maps/api/geocode/json?address=$postalCode&key=$apiKey"

    var coordinates: Pair<Double, Double>? = null

    withContext(Dispatchers.IO) {
        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                val scanner = Scanner(inputStream)
                val responseBody = StringBuilder()
                while (scanner.hasNextLine()) {
                    responseBody.append(scanner.nextLine())
                }
                scanner.close()
                inputStream.close()

                // Parse JSON response
                val jsonResponse = JSONObject(responseBody.toString())
                if (jsonResponse.getString("status") == "OK") {
                    val results = jsonResponse.getJSONArray("results")
                    if (results.length() > 0) {
                        val location = results.getJSONObject(0).getJSONObject("geometry").getJSONObject("location")
                        val lat = location.getDouble("lat")
                        val lng = location.getDouble("lng")
                        coordinates = Pair(lat, lng)
                    }
                }
            }
            connection.disconnect()
        } catch (e: Exception) {
            // Handle exception
            e.printStackTrace()
        }
    }

    return coordinates
}

@Composable
fun AddRowButton(onAddRow: (String, String, String, String) -> Unit) {
    val dateState = remember { mutableStateOf("") }
    val timeState = remember { mutableStateOf("") }
    val golfCourseState = remember { mutableStateOf("") }
    val locationState = remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(Modifier.padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = dateState.value,
            onValueChange = { dateState.value = it },
            label = { Text("Date") },
            modifier = Modifier
                .padding(vertical = 4.dp)
                .fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text)
        )
        OutlinedTextField(
            value = timeState.value,
            onValueChange = { timeState.value = it },
            label = { Text("Time") },
            modifier = Modifier
                .padding(vertical = 4.dp)
                .fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text)
        )
        OutlinedTextField(
            value = golfCourseState.value,
            onValueChange = { golfCourseState.value = it },
            label = { Text("Golf Course") },
            modifier = Modifier
                .padding(vertical = 4.dp)
                .fillMaxWidth()
        )
        OutlinedTextField(
            value = locationState.value,
            onValueChange = { locationState.value = it },
            label = { Text("Location (Postcode)") },
            modifier = Modifier
                .padding(vertical = 4.dp)
                .fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text)
        )
        Button(
            onClick = {
                if (isValidDateFormat(dateState.value) && isValidTimeFormat(timeState.value) && isValidPostcodeFormat(locationState.value)) {
                    val values = ContentValues().apply {
                        put(GolfEventDatabaseHelper.COLUMN_DATE, dateState.value)
                        put(GolfEventDatabaseHelper.COLUMN_TIME, timeState.value)
                        put(GolfEventDatabaseHelper.COLUMN_GOLF_COURSE, golfCourseState.value)
                        put(GolfEventDatabaseHelper.COLUMN_LOCATION, locationState.value)
                    }
                    onAddRow(dateState.value, timeState.value, golfCourseState.value, locationState.value)
                } else {
                    Toast.makeText(
                        context,
                        "Invalid date, time, or postcode format! (Date: DD-MM-YYYY, Time: HH:MM, Postcode: XXXX XXX)",
                        Toast.LENGTH_SHORT
                    ).show()
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

fun isValidDateFormat(input: String): Boolean {
    val regex = """\d{2}-\d{2}-\d{4}""".toRegex()
    return regex.matches(input)
}

fun isValidTimeFormat(input: String): Boolean {
    val regex = """([01][0-9]|2[0-3]):[0-5][0-9]""".toRegex()
    return regex.matches(input)
}

fun isValidPostcodeFormat(input: String): Boolean {
    val regex = """^(([A-Za-z]{1,2}\d{1,2}[A-Za-z]? \d{1,2}[A-Za-z]{2})|(\d{5}))$""".toRegex()
    return regex.matches(input)
}

fun inspectDatabaseContent(context: Context) {
    val contentResolver = context.contentResolver

    val uri = Uri.parse("content://com.example.coursework.provider/golf_events")

    val cursor = contentResolver.query(uri, null, null, null, null)

    cursor?.apply {
        if (moveToFirst()) {
            do {
                val id = getLong(getColumnIndexOrThrow(GolfEventDatabaseHelper.COLUMN_ID))
                val date = getString(getColumnIndexOrThrow(GolfEventDatabaseHelper.COLUMN_DATE))
                val time = getString(getColumnIndexOrThrow(GolfEventDatabaseHelper.COLUMN_TIME))
                val golfCourse = getString(getColumnIndexOrThrow(GolfEventDatabaseHelper.COLUMN_GOLF_COURSE))
                val location = getString(getColumnIndexOrThrow(GolfEventDatabaseHelper.COLUMN_LOCATION))

                Log.d("Database", "ID: $id, Date: $date, Time: $time, Golf Course: $golfCourse, Location: $location")
            } while (moveToNext())
        }
        close()
    }
}

data class GolfEvent(
    val id: Long,
    val date: String,
    val time: String,
    val golfCourse: String,
    val location: String
)

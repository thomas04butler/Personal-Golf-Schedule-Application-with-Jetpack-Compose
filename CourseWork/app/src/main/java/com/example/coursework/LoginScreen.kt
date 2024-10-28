import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@SuppressLint("RememberReturnType")
@Composable
fun LoginScreen(navController: NavHostController, isLoggedIn: MutableState<Boolean>, usernameState: MutableState<String>) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    val passwordState = remember { mutableStateOf("") }
    val isRegistering = remember { mutableStateOf(false) }
    val errorMessage = remember { mutableStateOf("") }


    fun getUserCredentials(): HashMap<String, String> {
        val jsonString = sharedPreferences.getString("userMap", "")
        return if (jsonString.isNullOrEmpty()) {
            hashMapOf()
        } else {
            HashMap(jsonString.split(",").map { it.split(":") }.associate { it[0] to it[1] })
        }
    }

    fun saveUserCredentials(userMap: HashMap<String, String>) {
        val jsonString = userMap.map { "${it.key}:${it.value}" }.joinToString(",")
        sharedPreferences.edit()
            .putString("userMap", jsonString)
            .apply()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isRegistering.value) "Welcome to Registering Screen" else "Welcome to Login Screen",
            modifier = Modifier.padding(bottom = 16.dp)
        )
        OutlinedTextField(
            value = usernameState.value,
            onValueChange = { usernameState.value = it },
            label = { Text("Username") },
            modifier = Modifier.padding(bottom = 16.dp)
        )
        OutlinedTextField(
            value = passwordState.value,
            onValueChange = { passwordState.value = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = errorMessage.value,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (!isRegistering.value) {
            // Login button
            Button(onClick = {
                val userMap = getUserCredentials()

                if (userMap.containsKey(usernameState.value) && userMap[usernameState.value] == passwordState.value) {
                    isLoggedIn.value = true
                    errorMessage.value = ""
                } else {
                    errorMessage.value = "Invalid credentials"
                }
            }) {
                Text("Login")
            }
            Text(
                text = "Don't have an account? Register",
                modifier = Modifier.padding(top = 8.dp).clickable {
                    isRegistering.value = true
                }
            )
        } else {
            // Registration mode
            Button(onClick = {
                val userMap = getUserCredentials()

                if (userMap.containsKey(usernameState.value)) {
                    errorMessage.value = "Username already exists. Please choose another one."
                } else {
                    userMap[usernameState.value] = passwordState.value
                    saveUserCredentials(userMap)
                    Log.d("Registration", "User registered: ${usernameState.value}")
                    errorMessage.value = ""
                    isRegistering.value = false
                }
            }) {
                Text("Register")
            }
            Text(
                text = "Already have an account? Login",
                modifier = Modifier.padding(top = 8.dp).clickable {
                    isRegistering.value = false
                }
            )
        }
    }
}

package com.example.coursework

import LoginScreen
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            val isLoggedIn = remember { mutableStateOf(false) }
            val usernameState = remember { mutableStateOf("") }

            if (isLoggedIn.value) {
                GolfApp(navController = navController, username = usernameState.value)
                Log.d("Registration", "User registered: ${usernameState.value}")
            } else {
                LoginScreen(
                    navController = navController,
                    isLoggedIn = isLoggedIn,
                    usernameState = usernameState
                )
            }
        }
    }
}

@Composable
fun GolfApp(navController: NavHostController, username: String) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            Greeting("Golfer's Companion", username) { route ->
                navController.navigate(route)
            }
        }
        composable(Screen.Calendar.route) {
            GolfScheduleScreen(username) {
                navController.popBackStack()
            }
        }
        composable(Screen.History.route) {
            GolfHistoryScreen(username) {
                navController.popBackStack()
            }
        }
        composable(Screen.Login.route) {
            LoginScreen(
                navController = navController,
                isLoggedIn = remember { mutableStateOf(false) },
                usernameState = remember { mutableStateOf("") }
            )
        }
    }
}

@Composable
fun Greeting(name: String, username: String, navigateToScreen: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val typography = MaterialTheme.typography
        Text(
            text = "Welcome to $name, $username",
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(onClick = {
            Log.d("Navigation", "Navigating to Calendar screen")
            navigateToScreen(Screen.Calendar.route)
        }) {
            Text("Go to Calendar")
        }
        Button(onClick = {
            Log.d("Navigation", "Navigating to History screen")
            navigateToScreen(Screen.History.route)
        }) {
            Text("Go to History")
        }
        Text(
            text = "Logout",
            modifier = Modifier.padding(top = 8.dp).clickable {
                Log.d("Logout", "Logout")
                navigateToScreen(Screen.Login.route)
            }
        )
    }
}

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Calendar : Screen("calendar")
    object History : Screen("history")
    object Login : Screen("login")
}
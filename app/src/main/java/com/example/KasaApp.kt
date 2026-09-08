package com.example

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.auth.FirebaseAuthServiceImpl
import com.example.data.local.KasaDatabase
import com.example.data.repository.ConversationRepositoryImpl
import com.example.data.repository.GeneratedImageRepositoryImpl
import com.example.data.repository.MemoryRepositoryImpl
import com.example.data.repository.StudyRepositoryImpl
import com.example.data.repository.UserRepositoryImpl
import com.example.navigation.KasaBottomBar
import com.example.navigation.KasaNavGraph
import com.example.navigation.Screen
import com.example.ui.theme.KasaTheme
import com.example.ui.theme.ThemeMode

@Composable
fun KasaApp() {
  val context = LocalContext.current
  val database = remember { KasaDatabase.getDatabase(context) }
  val userRepository = remember { UserRepositoryImpl(context) }
  val conversationRepository = remember { ConversationRepositoryImpl(database.conversationDao()) }
  val generatedImageRepository = remember { GeneratedImageRepositoryImpl(database.generatedImageDao()) }
  val studyRepository = remember { StudyRepositoryImpl(database.studySessionDao()) }
  val memoryRepository = remember { MemoryRepositoryImpl(database.memoryDao()) }
  val musicRepository = remember { com.example.data.repository.MusicRepositoryImpl(database.generatedSongDao()) }
  val authService = remember { FirebaseAuthServiceImpl(context) }

  val userSettings by userRepository.getUserSettings().collectAsState(initial = null)
  val themeMode = userSettings?.themeMode ?: ThemeMode.SYSTEM

  KasaTheme(themeMode = themeMode) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val shouldShowBottomBar = currentRoute in Screen.bottomNavItems.map { it.route }

    Scaffold(
      bottomBar = {
        if (shouldShowBottomBar) {
          KasaBottomBar(
            currentRoute = currentRoute,
            onNavigate = { screen ->
              if (currentRoute != screen.route) {
                navController.navigate(screen.route) {
                  popUpTo(Screen.Home.route) { saveState = true }
                  launchSingleTop = true
                  restoreState = true
                }
              }
            },
          )
        }
      },
      modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
      KasaNavGraph(
        navController = navController,
        userRepository = userRepository,
        conversationRepository = conversationRepository,
        generatedImageRepository = generatedImageRepository,
        studyRepository = studyRepository,
        memoryRepository = memoryRepository,
        authService = authService,
        musicRepository = musicRepository,
        modifier = Modifier.padding(innerPadding),
      )
    }
  }
}

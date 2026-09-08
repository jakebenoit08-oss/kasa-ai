package com.example.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.core.auth.AuthService
import com.example.data.repository.ConversationRepository
import com.example.data.repository.GeneratedImageRepository
import com.example.data.repository.MemoryRepository
import com.example.data.repository.StudyRepository
import com.example.data.repository.UserRepository
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.auth.AuthViewModel
import com.example.ui.screens.chat.ChatScreen
import com.example.ui.screens.chat.ChatViewModel
import com.example.ui.screens.create.CreateScreen
import com.example.ui.screens.create.CreateViewModel
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.live.LiveScreen
import com.example.ui.screens.live.LiveViewModel
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.screens.onboarding.OnboardingViewModel
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.SettingsViewModel
import com.example.ui.screens.splash.SplashScreen
import com.example.ui.screens.splash.SplashViewModel
import com.example.ui.screens.study.StudyScreen
import com.example.ui.screens.study.StudyViewModel

@Composable
fun KasaNavGraph(
  navController: NavHostController,
  userRepository: UserRepository,
  conversationRepository: ConversationRepository,
  generatedImageRepository: GeneratedImageRepository,
  studyRepository: StudyRepository,
  memoryRepository: MemoryRepository,
  authService: AuthService,
  musicRepository: com.example.data.repository.MusicRepository? = null,
  modifier: Modifier = Modifier,
) {
  NavHost(
    navController = navController,
    startDestination = Screen.Splash.route,
    modifier = modifier,
  ) {
    // Splash / Initial Routing
    composable(Screen.Splash.route) {
      val viewModel = SplashViewModel(userRepository, authService)
      SplashScreen(
        viewModel = viewModel,
        onNavigateTo = { targetRoute ->
          navController.navigate(targetRoute) {
            popUpTo(Screen.Splash.route) { inclusive = true }
          }
        }
      )
    }

    // Onboarding
    composable(Screen.Onboarding.route) {
      val viewModel = OnboardingViewModel(userRepository)
      OnboardingScreen(
        viewModel = viewModel,
        onFinishOnboarding = {
          navController.navigate(Screen.Auth.route) {
            popUpTo(Screen.Onboarding.route) { inclusive = true }
          }
        }
      )
    }

    // Authentication (Sign In, Sign Up, Guest Mode)
    composable(Screen.Auth.route) {
      val viewModel = AuthViewModel(authService, userRepository)
      AuthScreen(
        viewModel = viewModel,
        onAuthSuccess = {
          navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Auth.route) { inclusive = true }
          }
        }
      )
    }

    // Main App Destinations
    composable(Screen.Home.route) {
      val viewModel = HomeViewModel(userRepository, conversationRepository)
      HomeScreen(
        viewModel = viewModel,
        onNavigateToScreen = { screen ->
          navController.navigate(screen.route) {
            popUpTo(Screen.Home.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
          }
        },
        onNavigateToSettings = {
          navController.navigate(Screen.Settings.route)
        },
      )
    }

    composable(Screen.Chat.route) {
      val viewModel = ChatViewModel(userRepository, conversationRepository)
      ChatScreen(
        viewModel = viewModel,
        onNavigateToSettings = {
          navController.navigate(Screen.Settings.route)
        },
      )
    }

    composable(Screen.Live.route) {
      val viewModel = LiveViewModel(userRepository, conversationRepository)
      LiveScreen(
        viewModel = viewModel,
        onNavigateToChat = {
          navController.navigate(Screen.Chat.route) {
            popUpTo(Screen.Home.route)
            launchSingleTop = true
          }
        },
        onNavigateToSettings = {
          navController.navigate(Screen.Settings.route)
        },
      )
    }

    composable(Screen.Create.route) {
      val context = androidx.compose.ui.platform.LocalContext.current
      val resolvedMusicRepo = musicRepository ?: androidx.compose.runtime.remember {
        val db = com.example.data.local.KasaDatabase.getDatabase(context.applicationContext)
        com.example.data.repository.MusicRepositoryImpl(db.generatedSongDao())
      }
      val viewModel = androidx.compose.runtime.remember {
        CreateViewModel(
          userRepository = userRepository,
          generatedImageRepository = generatedImageRepository,
          musicRepository = resolvedMusicRepo,
          applicationContext = context.applicationContext,
        )
      }
      CreateScreen(
        viewModel = viewModel,
        onNavigateToSettings = {
          navController.navigate(Screen.Settings.route)
        },
      )
    }

    composable(Screen.Study.route) {
      val viewModel = StudyViewModel(userRepository, studyRepository)
      StudyScreen(
        viewModel = viewModel,
        onNavigateToSettings = {
          navController.navigate(Screen.Settings.route)
        },
      )
    }

    composable(Screen.Settings.route) {
      val viewModel = SettingsViewModel(
        userRepository = userRepository,
        memoryRepository = memoryRepository,
        authService = authService,
        conversationRepository = conversationRepository,
        studyRepository = studyRepository,
        imageRepository = generatedImageRepository,
      )
      SettingsScreen(
        viewModel = viewModel,
        onNavigateBack = {
          navController.popBackStack()
        },
        onSignedOut = {
          navController.navigate(Screen.Auth.route) {
            popUpTo(0) { inclusive = true }
          }
        },
      )
    }
  }
}

package com.example.ui.screens.study

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Biotech
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.study.EducationLevel
import com.example.data.model.study.QuestionSubmission
import com.example.data.model.study.QuizDifficulty
import com.example.data.model.study.QuizQuestion
import com.example.data.model.study.StudyChatMessage
import com.example.data.model.study.StudyLesson
import com.example.data.model.study.StudyProgressSummary
import com.example.data.model.study.StudySession
import com.example.data.model.study.StudySubject
import com.example.data.model.study.StudySubjectCatalog
import com.example.ui.components.KasaBadge
import com.example.ui.components.KasaCard
import com.example.ui.theme.KasaSpacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
  viewModel: StudyViewModel,
  onNavigateToSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(uiState.snackbarMessage) {
    uiState.snackbarMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearSnackbarMessage()
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "Study",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.width(8.dp))
            KasaBadge(
              text = "${uiState.selectedLevel.shortName} • ${uiState.selectedLevel.examContext}",
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        },
        actions = {
          IconButton(
            onClick = onNavigateToSettings,
            modifier = Modifier.testTag("study_settings_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.Settings,
              contentDescription = "Settings",
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
        ),
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
    modifier = modifier.testTag("study_screen"),
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Primary Tab Row
      TabRow(
        selectedTabIndex = uiState.currentTab.ordinal,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp),
      ) {
        StudyTab.values().forEach { tab ->
          Tab(
            selected = uiState.currentTab == tab,
            onClick = { viewModel.selectTab(tab) },
            text = {
              Text(
                text = tab.displayName,
                fontWeight = if (uiState.currentTab == tab) FontWeight.Bold else FontWeight.Normal,
              )
            },
            modifier = Modifier.testTag("study_tab_${tab.name.lowercase()}"),
          )
        }
      }

      // Error banner if any
      if (uiState.errorMessage != null) {
        ErrorBanner(
          message = uiState.errorMessage!!,
          onDismiss = { viewModel.clearErrorMessage() },
        )
      }

      // Content Area based on Tab
      Box(
        modifier = Modifier
          .fillMaxSize()
          .weight(1f),
        contentAlignment = Alignment.TopCenter,
      ) {
        when (uiState.currentTab) {
          StudyTab.LEARN -> LearnTabContent(viewModel = viewModel, uiState = uiState)
          StudyTab.QUIZ -> QuizTabContent(viewModel = viewModel, uiState = uiState)
          StudyTab.PROGRESS -> ProgressTabContent(viewModel = viewModel, uiState = uiState)
        }
      }
    }
  }
}

// -------------------------------------------------------------
// 1. LEARN & TUTOR TAB CONTENT
// -------------------------------------------------------------

@Composable
private fun LearnTabContent(
  viewModel: StudyViewModel,
  uiState: StudyUiState,
) {
  if (uiState.currentLesson != null) {
    LessonDetailView(
      lesson = uiState.currentLesson,
      followUpHistory = uiState.followUpHistory,
      followUpInput = uiState.followUpInput,
      isSendingFollowUp = uiState.isSendingFollowUp,
      onBack = { viewModel.clearLesson() },
      onFollowUpInputChange = { viewModel.updateFollowUpInput(it) },
      onSendFollowUp = { viewModel.sendFollowUp(it) },
      onStartPracticeQuiz = { viewModel.launchQuizFromLesson() },
    )
  } else if (uiState.isLoadingLesson) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(KasaSpacing.large),
      contentAlignment = Alignment.Center,
    ) {
      GeneratingCard(
        title = "Preparing your lesson...",
        subtitle = "Subject: ${uiState.selectedSubject.name} • Topic: ${uiState.topicInput}",
      )
    }
  } else {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 680.dp)
        .padding(horizontal = KasaSpacing.medium),
      verticalArrangement = Arrangement.spacedBy(KasaSpacing.medium),
    ) {
      item {
        Spacer(modifier = Modifier.height(KasaSpacing.small))
        StudyHeroHeaderCard(selectedLevel = uiState.selectedLevel)
      }

      // Education Level Selector
      item {
        EducationLevelSelectorCard(
          selectedLevel = uiState.selectedLevel,
          onSelectLevel = { viewModel.selectLevel(it) },
        )
      }

      // Subject Selector
      item {
        SubjectSelectorSection(
          selectedSubject = uiState.selectedSubject,
          onSelectSubject = { viewModel.selectSubject(it) },
        )
      }

      // Topic Input & Explorer
      item {
        TopicComposerCard(
          selectedSubject = uiState.selectedSubject,
          topicInput = uiState.topicInput,
          onTopicInputChange = { viewModel.updateTopicInput(it) },
          onSelectSampleTopic = { viewModel.selectSampleTopic(it) },
          onTeachTopic = { viewModel.teachTopic() },
        )
      }

      item {
        Spacer(modifier = Modifier.height(KasaSpacing.large))
      }
    }
  }
}

@Composable
private fun StudyHeroHeaderCard(selectedLevel: EducationLevel) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("study_hero_card"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(KasaSpacing.medium),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .size(46.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Outlined.School,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp),
        )
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column {
        Text(
          text = "KASA Study Companion",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = "Curriculum-aligned step-by-step tutoring and practice.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun EducationLevelSelectorCard(
  selectedLevel: EducationLevel,
  onSelectLevel: (EducationLevel) -> Unit,
) {
  KasaCard(testTag = "study_level_card") {
    Column(modifier = Modifier.fillMaxWidth()) {
      Text(
        text = "Education Level",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        EducationLevel.values().forEach { level ->
          FilterChip(
            selected = selectedLevel == level,
            onClick = { onSelectLevel(level) },
            label = {
              Text(
                text = level.shortName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selectedLevel == level) FontWeight.Bold else FontWeight.Normal,
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            modifier = Modifier
              .weight(1f)
              .testTag("study_level_${level.id}"),
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = selectedLevel.description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun SubjectSelectorSection(
  selectedSubject: StudySubject,
  onSelectSubject: (StudySubject) -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = "Select Subject",
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.padding(vertical = 4.dp),
    )

    // Core Subjects
    Text(
      text = "Core Subjects",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.primary,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(top = 4.dp, bottom = 6.dp),
    )

    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
      items(StudySubjectCatalog.CORE_SUBJECTS) { subject ->
        SubjectChipCard(
          subject = subject,
          isSelected = selectedSubject.id == subject.id,
          onClick = { onSelectSubject(subject) },
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Elective Subjects
    Text(
      text = "SHS Electives",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.primary,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(bottom = 6.dp),
    )

    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
      items(StudySubjectCatalog.ELECTIVE_SUBJECTS) { subject ->
        SubjectChipCard(
          subject = subject,
          isSelected = selectedSubject.id == subject.id,
          onClick = { onSelectSubject(subject) },
        )
      }
    }
  }
}

@Composable
private fun SubjectChipCard(
  subject: StudySubject,
  isSelected: Boolean,
  onClick: () -> Unit,
) {
  val icon = getSubjectIcon(subject.iconName)
  Card(
    modifier = Modifier
      .clickable { onClick() }
      .testTag("study_subject_${subject.id}"),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
    ),
    border = if (isSelected) {
      CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary))
    } else {
      CardDefaults.outlinedCardBorder()
    },
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = subject.name,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}

@Composable
private fun TopicComposerCard(
  selectedSubject: StudySubject,
  topicInput: String,
  onTopicInputChange: (String) -> Unit,
  onSelectSampleTopic: (String) -> Unit,
  onTeachTopic: () -> Unit,
) {
  KasaCard(testTag = "study_topic_composer") {
    Column(modifier = Modifier.fillMaxWidth()) {
      Text(
        text = "What would you like to learn in ${selectedSubject.name}?",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(8.dp))

      OutlinedTextField(
        value = topicInput,
        onValueChange = onTopicInputChange,
        placeholder = {
          Text(
            text = "Enter a topic (e.g. 'Quadratic Equations', 'Photosynthesis')...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
          )
        },
        trailingIcon = {
          if (topicInput.isNotBlank()) {
            IconButton(onClick = { onTopicInputChange("") }) {
              Icon(
                imageVector = Icons.Outlined.Clear,
                contentDescription = "Clear topic",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("study_topic_input"),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Sample topic pills
      Text(
        text = "Recommended Topics",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium,
      )
      Spacer(modifier = Modifier.height(6.dp))

      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        items(selectedSubject.sampleTopics) { topic ->
          Surface(
            modifier = Modifier
              .clickable { onSelectSampleTopic(topic) }
              .testTag("study_sample_topic_${topic.replace(" ", "_")}"),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          ) {
            Text(
              text = topic,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Button(
        onClick = onTeachTopic,
        enabled = topicInput.trim().isNotBlank(),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("study_teach_button"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
      ) {
        Icon(imageVector = Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "Teach Me This Topic", fontWeight = FontWeight.SemiBold)
      }
    }
  }
}

// -------------------------------------------------------------
// LESSON DETAIL VIEW
// -------------------------------------------------------------

@Composable
private fun LessonDetailView(
  lesson: StudyLesson,
  followUpHistory: List<StudyChatMessage>,
  followUpInput: String,
  isSendingFollowUp: Boolean,
  onBack: () -> Unit,
  onFollowUpInputChange: (String) -> Unit,
  onSendFollowUp: (String?) -> Unit,
  onStartPracticeQuiz: () -> Unit,
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .widthIn(max = 680.dp)
      .padding(horizontal = KasaSpacing.medium),
    verticalArrangement = Arrangement.spacedBy(KasaSpacing.medium),
  ) {
    item {
      Spacer(modifier = Modifier.height(KasaSpacing.small))
      // Back Navigation Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.testTag("study_lesson_back_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Back to topics",
          )
        }
        Text(
          text = lesson.topic,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        )
        Button(
          onClick = onStartPracticeQuiz,
          modifier = Modifier.testTag("study_lesson_practice_quiz_button"),
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
        ) {
          Icon(imageVector = Icons.Outlined.Quiz, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = "Practice Quiz", style = MaterialTheme.typography.labelMedium)
        }
      }
    }

    // Summary Card
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("study_lesson_summary_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        ),
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Outlined.Lightbulb,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Core Concept Summary",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = lesson.summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }
    }

    // Detailed Explanation Card
    item {
      KasaCard(testTag = "study_lesson_explanation_card") {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "Detailed Step-by-Step Explanation",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = lesson.detailedExplanation,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }
    }

    // Real-World Examples
    if (lesson.examples.isNotEmpty()) {
      item {
        KasaCard(testTag = "study_lesson_examples_card") {
          Column(modifier = Modifier.fillMaxWidth()) {
            Text(
              text = "Worked Examples & Real-World Applications",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            lesson.examples.forEachIndexed { idx, ex ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp),
                verticalAlignment = Alignment.Top,
              ) {
                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.secondaryContainer,
                  modifier = Modifier.size(22.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(
                      text = "${idx + 1}",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                  }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = ex,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.weight(1f),
                )
              }
            }
          }
        }
      }
    }

    // Key Points & Exam Pointers
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Card(
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
          ),
          border = CardDefaults.outlinedCardBorder(),
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = "Key Points",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            lesson.keyPoints.forEach { pt ->
              Text(
                text = "• $pt",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 2.dp),
              )
            }
          }
        }

        Card(
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
          ),
          border = CardDefaults.outlinedCardBorder(),
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = "Exam Tips",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            lesson.examPointers.forEach { pt ->
              Text(
                text = "• $pt",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 2.dp),
              )
            }
          }
        }
      }
    }

    // Interactive Follow-up Section
    item {
      Text(
        text = "Ask KASA Tutor",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 8.dp),
      )
    }

    // Follow-up conversation history
    items(followUpHistory) { msg ->
      val isUser = msg.role == "user"
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
      ) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.widthIn(max = 300.dp),
        ) {
          Text(
            text = msg.content,
            style = MaterialTheme.typography.bodySmall,
            color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(10.dp),
          )
        }
      }
    }

    if (isSendingFollowUp) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "KASA is answering...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    // Quick follow-up chips
    item {
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        items(lesson.suggestedFollowUps) { query ->
          Surface(
            modifier = Modifier
              .clickable { onSendFollowUp(query) }
              .testTag("study_followup_${query.replace(" ", "_")}"),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
          ) {
            Text(
              text = query,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
          }
        }
      }
    }

    // Follow-up Input Bar
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedTextField(
          value = followUpInput,
          onValueChange = onFollowUpInputChange,
          placeholder = {
            Text(
              text = "Ask anything about this lesson...",
              style = MaterialTheme.typography.bodySmall,
            )
          },
          modifier = Modifier
            .weight(1f)
            .testTag("study_followup_input"),
          shape = RoundedCornerShape(12.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
          onClick = { onSendFollowUp(null) },
          enabled = followUpInput.trim().isNotBlank() && !isSendingFollowUp,
          modifier = Modifier.testTag("study_followup_send_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Outlined.Send,
            contentDescription = "Send follow-up",
            tint = MaterialTheme.colorScheme.primary,
          )
        }
      }
      Spacer(modifier = Modifier.height(KasaSpacing.large))
    }
  }
}

// -------------------------------------------------------------
// 2. PRACTICE QUIZ TAB CONTENT
// -------------------------------------------------------------

@Composable
private fun QuizTabContent(
  viewModel: StudyViewModel,
  uiState: StudyUiState,
) {
  if (uiState.isReviewingQuiz && uiState.completedSession != null) {
    QuizReviewView(
      session = uiState.completedSession,
      onRetake = { viewModel.retakeCurrentQuiz() },
      onDone = { viewModel.clearQuiz() },
      onStudyTopic = {
        viewModel.selectTab(StudyTab.LEARN)
        viewModel.teachTopic(uiState.completedSession.topic)
      },
    )
  } else if (uiState.activeQuizQuestions.isNotEmpty()) {
    ActiveQuizView(
      questions = uiState.activeQuizQuestions,
      currentIndex = uiState.currentQuestionIndex,
      selectedAnswers = uiState.selectedAnswers,
      revealedHints = uiState.revealedHints,
      onSelectAnswer = { idx, ans -> viewModel.selectQuizAnswer(idx, ans) },
      onNext = { viewModel.nextQuestion() },
      onPrevious = { viewModel.previousQuestion() },
      onRevealHint = { viewModel.revealHint(it) },
      onSubmit = { viewModel.submitQuiz() },
      onCancel = { viewModel.clearQuiz() },
    )
  } else if (uiState.isGeneratingQuiz) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(KasaSpacing.large),
      contentAlignment = Alignment.Center,
    ) {
      GeneratingCard(
        title = "Generating Practice Quiz...",
        subtitle = "Subject: ${uiState.quizSubject.name} • Topic: ${uiState.quizTopic}",
      )
    }
  } else {
    // Quiz Setup Screen
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 680.dp)
        .padding(horizontal = KasaSpacing.medium),
      verticalArrangement = Arrangement.spacedBy(KasaSpacing.medium),
    ) {
      item {
        Spacer(modifier = Modifier.height(KasaSpacing.small))
        // Quiz Header Card
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("study_quiz_header_card"),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          ),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(KasaSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Outlined.Quiz,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
              )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
              Text(
                text = "Practice Quiz Generator",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "Test your mastery with instant step-by-step feedback.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      // Quiz Configuration Card
      item {
        KasaCard(testTag = "study_quiz_setup_card") {
          Column(modifier = Modifier.fillMaxWidth()) {
            Text(
              text = "Quiz Topic & Subject",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Subject row
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(StudySubjectCatalog.ALL_SUBJECTS) { subject ->
                FilterChip(
                  selected = uiState.quizSubject.id == subject.id,
                  onClick = { viewModel.updateQuizSubject(subject) },
                  label = { Text(subject.name, style = MaterialTheme.typography.labelSmall) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                  ),
                  modifier = Modifier.testTag("study_quiz_subject_${subject.id}"),
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
              value = uiState.quizTopic,
              onValueChange = { viewModel.updateQuizTopic(it) },
              placeholder = {
                Text("Enter topic for quiz (e.g. 'Photosynthesis', 'Simultaneous Equations')")
              },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("study_quiz_topic_input"),
              shape = RoundedCornerShape(12.dp),
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Difficulty Chips
            Text(
              text = "Difficulty Level",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              QuizDifficulty.values().forEach { diff ->
                FilterChip(
                  selected = uiState.quizDifficulty == diff,
                  onClick = { viewModel.updateQuizDifficulty(diff) },
                  label = { Text(diff.displayName, style = MaterialTheme.typography.labelSmall) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                  ),
                  modifier = Modifier.weight(1f).testTag("study_quiz_diff_${diff.id}"),
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Question Count Chips
            Text(
              text = "Number of Questions",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              listOf(3, 5, 10).forEach { count ->
                FilterChip(
                  selected = uiState.quizQuestionCount == count,
                  onClick = { viewModel.updateQuizQuestionCount(count) },
                  label = { Text("$count Questions", style = MaterialTheme.typography.labelSmall) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                  ),
                  modifier = Modifier.weight(1f).testTag("study_quiz_count_$count"),
                )
              }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
              onClick = { viewModel.startQuiz() },
              enabled = uiState.quizTopic.trim().isNotBlank(),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("study_start_quiz_button"),
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
              ),
            ) {
              Icon(imageVector = Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(text = "Start Practice Quiz", fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }

      // Honesty Disclaimer Card
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("study_quiz_honesty_card"),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
          ),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Outlined.Info,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "AI-generated practice questions are designed for conceptual mastery and curriculum practice. They are not official WAEC past questions.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(KasaSpacing.large))
      }
    }
  }
}

@Composable
private fun ActiveQuizView(
  questions: List<QuizQuestion>,
  currentIndex: Int,
  selectedAnswers: Map<Int, String>,
  revealedHints: Set<Int>,
  onSelectAnswer: (Int, String) -> Unit,
  onNext: () -> Unit,
  onPrevious: () -> Unit,
  onRevealHint: (Int) -> Unit,
  onSubmit: () -> Unit,
  onCancel: () -> Unit,
) {
  val question = questions.getOrNull(currentIndex) ?: return
  val selectedAnswer = selectedAnswers[currentIndex]
  val isLastQuestion = currentIndex == questions.size - 1
  val isHintRevealed = revealedHints.contains(currentIndex)

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .widthIn(max = 680.dp)
      .padding(horizontal = KasaSpacing.medium),
    verticalArrangement = Arrangement.spacedBy(KasaSpacing.medium),
  ) {
    item {
      Spacer(modifier = Modifier.height(KasaSpacing.small))
      // Progress Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        IconButton(
          onClick = onCancel,
          modifier = Modifier.testTag("study_quiz_cancel_button"),
        ) {
          Icon(imageVector = Icons.Outlined.Clear, contentDescription = "Exit Quiz")
        }
        Text(
          text = "Question ${currentIndex + 1} of ${questions.size}",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
        )
        KasaBadge(
          text = "AI Practice",
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
      }
    }

    // Question Card
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("study_active_question_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = CardDefaults.outlinedCardBorder(),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = question.questionText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }
    }

    // Multiple Choice Options
    if (question.options.isNotEmpty()) {
      itemsIndexed(question.options) { optIdx, optionText ->
        val isChosen = selectedAnswer == optionText || (selectedAnswer != null && optionText.startsWith(selectedAnswer))
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectAnswer(currentIndex, optionText) }
            .testTag("study_quiz_option_${optIdx + 1}"),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (isChosen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
          ),
          border = if (isChosen) {
            CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary))
          } else {
            CardDefaults.outlinedCardBorder()
          },
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              shape = CircleShape,
              color = if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.size(24.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = ('A' + optIdx).toString(),
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = if (isChosen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = optionText,
              style = MaterialTheme.typography.bodyMedium,
              color = if (isChosen) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    } else {
      // Short Answer input field
      item {
        OutlinedTextField(
          value = selectedAnswer.orEmpty(),
          onValueChange = { onSelectAnswer(currentIndex, it) },
          placeholder = { Text("Enter your solution...") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("study_short_answer_input"),
          shape = RoundedCornerShape(12.dp),
        )
      }
    }

    // Hint Section
    if (!question.hint.isNullOrBlank()) {
      item {
        if (isHintRevealed) {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(imageVector = Icons.Outlined.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
              Spacer(modifier = Modifier.width(8.dp))
              Text(text = question.hint, style = MaterialTheme.typography.bodySmall)
            }
          }
        } else {
          OutlinedButton(
            onClick = { onRevealHint(currentIndex) },
            modifier = Modifier.testTag("study_quiz_hint_button"),
            shape = RoundedCornerShape(10.dp),
          ) {
            Icon(imageVector = Icons.Outlined.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Need a Hint?", style = MaterialTheme.typography.labelMedium)
          }
        }
      }
    }

    // Navigation Controls
    item {
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (currentIndex > 0) {
          OutlinedButton(
            onClick = onPrevious,
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("study_quiz_prev_button"),
            shape = RoundedCornerShape(12.dp),
          ) {
            Text("Previous")
          }
        }

        if (isLastQuestion) {
          Button(
            onClick = onSubmit,
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("study_quiz_submit_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
          ) {
            Text("Submit Quiz", fontWeight = FontWeight.Bold)
          }
        } else {
          Button(
            onClick = onNext,
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("study_quiz_next_button"),
            shape = RoundedCornerShape(12.dp),
          ) {
            Text("Next Question")
          }
        }
      }
      Spacer(modifier = Modifier.height(KasaSpacing.large))
    }
  }
}

// -------------------------------------------------------------
// QUIZ REVIEW VIEW
// -------------------------------------------------------------

@Composable
private fun QuizReviewView(
  session: StudySession,
  onRetake: () -> Unit,
  onDone: () -> Unit,
  onStudyTopic: () -> Unit,
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .widthIn(max = 680.dp)
      .padding(horizontal = KasaSpacing.medium),
    verticalArrangement = Arrangement.spacedBy(KasaSpacing.medium),
  ) {
    item {
      Spacer(modifier = Modifier.height(KasaSpacing.small))
      // Score Hero Card
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("study_quiz_score_hero"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (session.scorePercentage >= 70) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(KasaSpacing.large),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
            text = "Quiz Completed",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "${session.scorePercentage}%",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "${session.correctQuestions} of ${session.totalQuestions} Questions Correct",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = when {
              session.scorePercentage >= 80 -> "Excellent mastery of ${session.topic}! Ready for advanced problems."
              session.scorePercentage >= 60 -> "Good effort! Review the step-by-step solutions below to solidify your understanding."
              else -> "Keep going! Review the concepts and explanations below to strengthen your grasp."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    // Action Buttons
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedButton(
          onClick = onStudyTopic,
          modifier = Modifier
            .weight(1f)
            .testTag("study_review_learn_button"),
          shape = RoundedCornerShape(10.dp),
        ) {
          Icon(imageVector = Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Study Topic", style = MaterialTheme.typography.labelMedium)
        }
        OutlinedButton(
          onClick = onRetake,
          modifier = Modifier
            .weight(1f)
            .testTag("study_review_retake_button"),
          shape = RoundedCornerShape(10.dp),
        ) {
          Icon(imageVector = Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Retake", style = MaterialTheme.typography.labelMedium)
        }
        Button(
          onClick = onDone,
          modifier = Modifier
            .weight(1f)
            .testTag("study_review_done_button"),
          shape = RoundedCornerShape(10.dp),
        ) {
          Text("Done", style = MaterialTheme.typography.labelMedium)
        }
      }
    }

    item {
      Text(
        text = "Step-by-Step Question Review",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 8.dp),
      )
    }

    // Question Submissions Review List
    items(session.submissions) { sub ->
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("study_review_item_${sub.questionNumber}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = CardDefaults.outlinedCardBorder(),
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Question ${sub.questionNumber}",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            KasaBadge(
              text = if (sub.isCorrect) "Correct" else "Needs Review",
              containerColor = if (sub.isCorrect) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
              contentColor = if (sub.isCorrect) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
            )
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = sub.questionText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
          )

          Spacer(modifier = Modifier.height(10.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
          Spacer(modifier = Modifier.height(10.dp))

          Text(
            text = "Your Answer: ${sub.selectedAnswer}",
            style = MaterialTheme.typography.bodySmall,
            color = if (sub.isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Medium,
          )
          if (!sub.isCorrect) {
            Text(
              text = "Correct Answer: ${sub.correctAnswer}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.SemiBold,
            )
          }

          Spacer(modifier = Modifier.height(8.dp))
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(
                text = "Explanation & Solution Working:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = sub.explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(KasaSpacing.large))
    }
  }
}

// -------------------------------------------------------------
// 3. PROGRESS & HISTORY TAB CONTENT
// -------------------------------------------------------------

@Composable
private fun ProgressTabContent(
  viewModel: StudyViewModel,
  uiState: StudyUiState,
) {
  val summary = uiState.progressSummary

  if (uiState.selectedHistoricalSession != null) {
    QuizReviewView(
      session = uiState.selectedHistoricalSession!!,
      onRetake = {
        viewModel.clearHistoricalSession()
        viewModel.selectTab(StudyTab.QUIZ)
      },
      onDone = { viewModel.clearHistoricalSession() },
      onStudyTopic = {
        val topic = uiState.selectedHistoricalSession!!.topic
        viewModel.clearHistoricalSession()
        viewModel.selectTab(StudyTab.LEARN)
        viewModel.teachTopic(topic)
      },
    )
  } else if (summary == null || summary.totalSessions == 0) {
    // True Empty State
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(KasaSpacing.large),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .widthIn(max = 360.dp)
          .testTag("study_progress_empty_state"),
      ) {
        Box(
          modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Outlined.School,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp),
          )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = "No study sessions yet.",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Complete a lesson or take a practice quiz to view your real progress, average scores, and focus areas here.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
          onClick = { viewModel.selectTab(StudyTab.LEARN) },
          shape = RoundedCornerShape(12.dp),
        ) {
          Text("Start Learning a Topic")
        }
      }
    }
  } else {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 680.dp)
        .padding(horizontal = KasaSpacing.medium),
      verticalArrangement = Arrangement.spacedBy(KasaSpacing.medium),
    ) {
      item {
        Spacer(modifier = Modifier.height(KasaSpacing.small))
        // Overall Analytics Banner
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("study_progress_analytics_card"),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
          ),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(KasaSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "${summary.totalSessions}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = "Quizzes Completed",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "${summary.averageScorePercentage}%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = "Average Score",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      // Weak Topics / Review Needed Section
      item {
        KasaCard(testTag = "study_weak_topics_card") {
          Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Focus Areas & Review",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (summary.weakTopics.isNotEmpty()) {
              Text(
                text = "Based on your real quiz results, you may want to review:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(modifier = Modifier.height(6.dp))
              summary.weakTopics.forEach { topic ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                      viewModel.selectTab(StudyTab.LEARN)
                      viewModel.teachTopic(topic)
                    }
                    .padding(vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = "• $topic",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                  )
                  Text(
                    text = "Study →",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
              }
            } else {
              Text(
                text = "Great job! All your completed topic quizzes average 60% or higher. Keep challenging yourself with new topics.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      // Subject Performance Breakdown
      if (summary.subjectPerformances.isNotEmpty()) {
        item {
          Text(
            text = "Subject Performance",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }

        items(summary.subjectPerformances) { subj ->
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column {
                Text(
                  text = subj.subjectName,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "${subj.totalSessions} sessions completed",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Text(
                text = "${subj.averageScorePercentage}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }
      }

      // Recent Sessions History
      item {
        Text(
          text = "Study History",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(top = 8.dp),
        )
      }

      items(summary.recentSessions) { session ->
        val dateFormat = remember { SimpleDateFormat("MMM d • h:mm a", Locale.getDefault()) }
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.viewHistoricalSession(session) }
            .testTag("study_history_session_${session.id}"),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = CardDefaults.outlinedCardBorder(),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = session.topic,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "${session.subject} • ${dateFormat.format(Date(session.createdAt))}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            KasaBadge(
              text = "${session.scorePercentage}%",
              containerColor = if (session.scorePercentage >= 70) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
              contentColor = if (session.scorePercentage >= 70) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(
              onClick = { viewModel.deleteSession(session.id) },
              modifier = Modifier.size(36.dp),
            ) {
              Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Delete session",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp),
              )
            }
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(KasaSpacing.large))
      }
    }
  }
}

// -------------------------------------------------------------
// HELPER COMPONENTS & ICON RESOLUTION
// -------------------------------------------------------------

@Composable
private fun GeneratingCard(title: String, subtitle: String) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("study_generating_card"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(KasaSpacing.large),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      CircularProgressIndicator(
        modifier = Modifier.size(44.dp),
        color = MaterialTheme.colorScheme.primary,
        strokeWidth = 3.dp,
      )
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = KasaSpacing.medium, vertical = 4.dp)
      .testTag("study_error_banner"),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
    ),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Outlined.ErrorOutline,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier.size(20.dp),
      )
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.weight(1f),
      )
      IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
        Icon(
          imageVector = Icons.Outlined.Clear,
          contentDescription = "Dismiss error",
          tint = MaterialTheme.colorScheme.onErrorContainer,
          modifier = Modifier.size(16.dp),
        )
      }
    }
  }
}

private fun getSubjectIcon(name: String): ImageVector {
  return when (name) {
    "Calculate" -> Icons.Outlined.Calculate
    "MenuBook" -> Icons.AutoMirrored.Outlined.MenuBook
    "Science" -> Icons.Outlined.Science
    "Public" -> Icons.Outlined.Public
    "Functions" -> Icons.Outlined.Functions
    "ElectricBolt" -> Icons.Outlined.ElectricBolt
    "Biotech" -> Icons.Outlined.Biotech
    "Eco" -> Icons.Outlined.Eco
    "TrendingUp" -> Icons.AutoMirrored.Outlined.TrendingUp
    "Map" -> Icons.Outlined.Map
    "AccountBalance" -> Icons.Outlined.AccountBalance
    "Computer" -> Icons.Outlined.Computer
    else -> Icons.Outlined.School
  }
}

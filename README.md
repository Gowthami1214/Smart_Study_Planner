# AI Smart Study Planner 🚀

A production-grade, AI-driven study planning application built with modern Android technologies. This app generates personalized, actionable study schedules based on your subjects and specific topics, helping you master any skill with structured progress tracking and smart reminders.

## ✨ Key Features

- **🧠 AI Schedule Generation**: Generates detailed study plans using Groq/Gemini AI based on multiple subject inputs.
- **🎯 Granular Topic Selection**: Don't just study a subject—select the specific topics you want to focus on.
- **⏱️ Focus Mode & Pomodoro**: Built-in Pomodoro timer for active tasks. Automatically toggles your device's "Do Not Disturb" mode while focusing.
- **💬 AI Chat Assistant**: In-app conversational AI to help you understand complex study topics and guide your learning.
- **📊 Interactive Progress Analytics**: Real-time mastery tracking with daily/weekly completion charts, streak counters, and dynamic progress bars.
- **🔔 Smart Reminders**: Reliable `AlarmManager` integration with customizable notification offsets (5, 10, 15 mins).
- **📝 Task-Level Control**:
  - Mark as Complete/Pending.
  - Reschedule or Delete individual tasks.
  - Regenerate specific tasks or entire plans using AI.
- **☁️ Firebase Integration**: 
  - **Firestore**: Professional data structure for active plans and history.
  - **Auth**: Secure user management including Sign Up, Login, and Password Recovery flows.
- **📴 Offline-First**: Built with Room database for seamless offline access and background syncing.
- **🎨 Premium UI/UX**: Designed with Material 3, featuring glassmorphic effects, ambient glows, dynamic bottom sheets, and grouped layouts.

## 🛠️ Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: Hilt
- **Local Database**: Room
- **Cloud Database**: Firebase Firestore
- **AI Engine**: Groq API (LLaMA 3)
- **Background Tasks**: AlarmManager + BroadcastReceiver

## 📱 Screenshots

| Login | Sign Up | Profile |
| :---: | :---: | :---: |
| <img src="screenshots/Login.jpeg" width="200" alt="Login"> | <img src="screenshots/SignUp_page.jpeg" width="200" alt="Sign Up"> | <img src="screenshots/Profile.jpeg" width="200" alt="Profile"> |

| Home | Today's Task | Progress |
| :---: | :---: | :---: |
| <img src="screenshots/Home.jpeg" width="200" alt="Home"> | <img src="screenshots/Todays_task.jpeg" width="200" alt="Today's Task"> | <img src="screenshots/ProgressPage.jpeg" width="200" alt="Progress"> |

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug or later
- Groq API Key (Set in `AiRepositoryImpl.kt`)
- Firebase Project (Configured with `google-services.json`)

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/Gowthami1214/Smart_Study_Planner.git
   ```
2. Open the project in Android Studio.
3. Add your `google-services.json` to the `app/` directory.
4. Replace the placeholder API key in `AiRepositoryImpl.kt` with your own.
5. Build and Run!

## 📂 Firestore Structure

```text
users/
  {userId}/
    plans/
      {planId}/
        tasks/
          {taskId} -> [StudyPlanItem]
    history/
      {historyId} -> [CompletedTaskRecord]
```

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request or open an issue for any feature requests or bug reports.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
Built with ❤️ by [Gowthami](https://github.com/Gowthami1214)

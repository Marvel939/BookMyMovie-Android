# Project Standards & Methodology

This document outlines the coding standards, naming conventions, and the technical methodology used in the **BookMyMovie** Android application.

## 1. Naming Conventions

To maintain codebase consistency and readability, the following naming conventions are strictly followed:

### 1.1 Variables & Properties
- **Style**: `camelCase`
- **Description**: All variable names, class properties, and local variables should start with a lowercase letter, with each subsequent word capitalized.
- **Example**: 
    ```kotlin
    val showtimeId: String = ""
    val screenName: String = ""
    val isUserLoggedIn: Boolean = false
    ```

### 1.2 Methods & Functions
- **Style**: `camelCase`
- **Description**: Function names should be descriptive verbs or verb-noun combinations.
- **Example**: 
    ```kotlin
    fun fetchNearbyTheatres(lat: Double, lng: Double) { ... }
    fun onCreate(savedInstanceState: Bundle?) { ... }
    ```

### 1.3 Classes & Interfaces
- **Style**: `PascalCase`
- **Description**: Class names, interfaces, and sealed classes should start with an uppercase letter.
- **Example**: 
    ```kotlin
    class MainActivity : ComponentActivity() { ... }
    data class SeatData(val seatId: String)
    ```

### 1.4 Constants
- **Style**: `UPPER_SNAKE_CASE`
- **Description**: Compile-time constants and static final fields.
- **Example**:
    ```kotlin
    const val BASE_URL = "https://api.example.com/"
    const val REQUEST_CODE_LOCATION = 101
    ```

---

## 2. Project Methodology

The project follows a modern, scalable, and modular approach to Android development.

### 2.1 Architecture (MVVM)
The application is built using the **Model-View-ViewModel (MVVM)** architectural pattern:
- **Model**: Represents the data layer (Firebase, Local Models).
- **View**: The UI layer built using **Jetpack Compose**.
- **ViewModel**: Acts as a bridge, managing UI state and interacting with the data layer.

### 2.2 Frontend Framework
- **Language**: Kotlin
- **UI Toolset**: **Jetpack Compose** – A modern toolkit for building native UI using a declarative approach.
- **Theming**: Material 3 Design for a consistent and modern look.

### 2.3 Backend & Services
- **Real-time Database**: **Firebase Realtime Database** / **Firestore** for seamless data synchronization.
- **Authentication**: **Firebase Auth** handles secure sign-in and user management.
- **Cloud Logic**: **Firebase Cloud Functions** are used for server-side operations (Node.js).
- **Payments**: **Stripe Integration** for handling ticket bookings and OTT purchases.

### 2.4 State Management & Data Flow
- Uses **ViewModels** with `StateFlow` or `MutableState` to provide reactive UI updates.
- **Repository Pattern**: Centralizes data access logic from Firebase and other services.

### 2.5 Navigation
- **Compose Navigation**: Handles screen transitions and deep linking within a single-activity architecture.

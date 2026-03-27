# Chatbot AI Activity Diagram

This diagram illustrates the flow of user interactions, internal logic processing, and external API calls for the AI Chatbot integrated into the BookMyMovie application.

```mermaid
graph TD
    Start([Start]) --> OpenScreen[User opens AI Chat Screen]
    
    subgraph Initialization
        OpenScreen --> InitTTS[Initialize Text-to-Speech]
        InitTTS --> LoadHistory[Load Chat History from Firebase]
    end
    
    LoadHistory --> UserInput{User Input?}
    
    subgraph Input Handling
        UserInput -- Text Message --> AddUserMsg
        UserInput -- Voice Mic --> STT[Speech-to-Text]
        STT --> AddUserMsg
        UserInput -- Edit Message --> DeleteOld[Delete Old Msg & AI Response]
        DeleteOld --> AddUserMsg
    end
    
    AddUserMsg[Add User Message to UI & Firebase] --> BookingCheck{isBookingFlowActive?}
    
    subgraph Booking Logic
        BookingCheck -- Yes --> StepHandle[Handle Step ASK_THEATRE or ASK_DATE]
        StepHandle --> ShowtimeCheck{Showtime Found?}
        ShowtimeCheck -- Yes --> Navigate[Navigate to Seat Selection]
        ShowtimeCheck -- No --> SuggestDates[Suggest Dates or Other Theatre]
        SuggestDates --> AIRes
    end
    
    BookingCheck -- No --> IntentCheck{Detect "Booking" intent?}
    
    subgraph Chat Logic
        IntentCheck -- Yes --> StartBooking[Search TMDB & Theatres]
        StartBooking --> SetStep[Set step = ASK_THEATRE]
        SetStep --> AIRes
        
        IntentCheck -- No --> CallAPI[Send History to OpenRouter Gemini]
        CallAPI --> AIRes[Display AI Response & Speak via TTS]
    end
    
    AIRes --> SaveAI[Save AI Response to Firebase]
    SaveAI --> UserInput
    
    Navigate --> End([End])
```

## Key Components
- **`ChatBotViewModel`**: Manages the state machine for booking, AI communication, and history.
- **`AiChatScreen`**: Handles UI rendering, voice permissions, and navigation triggers.
- **OpenRouter (Gemini)**: External AI model used for natural language processing.
- **Firebase Realtime Database**: Stores persistent chat history and showtime data.
- **TMDB API**: Used for movie search and details during the booking flow.
- **Text-to-Speech (TTS) & SpeechRecognizer**: Provides voice interaction capabilities.

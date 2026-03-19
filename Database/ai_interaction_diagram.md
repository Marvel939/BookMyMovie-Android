# AI Chatbot Interaction Diagram

Based on the architecture of your BookMyMovie application, this Interaction (Sequence) Diagram documents the exact lifecycle of how a Customer communicates with your Generative AI feature. 

It maps out how the user's input travels from the UI, gets stored in your database tables (`ai_chat_sessions`, `ai_chat_messages`), retrieves your local API key, and successfully connects to the external OpenRouter LLM infrastructure.

## Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    
    actor User as Customer
    participant App as BookMyMovie UI (Android)
    participant DB as Firebase / SQL Database
    participant Key as Local Assets (openrouter.key)
    participant AI as OpenRouter API (LLM)
    
    %% Phase 1: Session Initialization
    Note over User, DB: Phase 1: Launching the Chat
    User->>App: Clicks "AI Assistant" button
    activate App
    App->>DB: Query `ai_chat_sessions` (WHERE user_id = current_user)
    
    alt No Active Context
        DB->>DB: Insert new `ai_chat_sessions` row
        DB-->>App: Return new session_id
    else Cached Session Exists
        DB-->>App: Return existing session_id
        App->>DB: Query `ai_chat_messages` for conversation history
        DB-->>App: Return previous message payload
    end
    
    App-->>User: Open populated Chat Interface
    deactivate App
    
    %% Phase 2: Processing the Query
    Note over User, AI: Phase 2: Input (Text or Voice)
    
    alt Standard Text Input
        User->>App: Types question manually (e.g., "What's playing?")
    else Voice Interaction
        User->>App: Clicks Microphone & Speaks query
        App->>App: Android Speech-to-Text (STT) converts audio
    end
    
    activate App
    App->>DB: Insert into `ai_chat_messages` (role: "user")
    
    %% Security & Auth Step
    App->>Key: Read API Key safely at runtime
    Key-->>App: Supply OpenRouter API Secret
    App->>App: Compile JSON Payload (System Instructions + User Text)
    
    %% Network Execution
    App->>AI: HTTPS POST Request (Auth: Bearer Token)
    activate AI
    Note over App, AI: The LLM computes the response based on the prompt
    AI-->>App: Return successful JSON Text Stream
    deactivate AI
    
    %% Phase 3: Finalizing State
    Note over App, DB: Phase 3: Rendering & Saving
    App->>DB: Insert into `ai_chat_messages` (role: "model")
    App-->>User: Dislay AI Text Output on Screen
    deactivate App
```

## How this works:
1. **State Persistence**: The communication relies heavily on the `ai_chat_sessions` table. AI models are stateless, meaning they have no memory of the past. To fix this, your app first queries the database to see if the user was talking to the bot previously. If they were, the app rebuilds the context and feeds the whole memory string to the AI before asking the new question.
2. **Local Secrets**: You notice the App does not query the Database for the OpenRouter API Key. Instead, it reads `"openrouter.key"` directly from the hardened local Android binary avoiding unnecessary and insecure network trips.
3. **Dual Writing**: For every single interaction, your backend fires **two** `INSERT` commands into `ai_chat_messages`. First, tagging the customer's question with **role: "user"**, and secondly, capturing the AI's exact response tagged as **role: "model"**. Validating this schema allows you to review historical logs effortlessly.

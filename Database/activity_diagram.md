# BookMyMovie Activity Diagram

Based on analyzing the `database_schema.sql` file and the core features of your project, below is the formal **UML Activity Diagram** representing the primary dynamic workflow of the application: **The User Movie Booking & Management Flow**. 

This diagram captures the step-by-step actions, decision checks (diamonds), and parallel tracks that occur when a user interacts with the app.

## Activity Diagram (Flowchart)

```mermaid
flowchart TD
    %% Define UML Activity Nodes & Styles
    classDef startEnd fill:#333,stroke:#333,color:#fff,shape:circle,width:20px,height:20px
    classDef action fill:#e1f5fe,stroke:#03a9f4,stroke-width:2px,rx:10px
    classDef decision fill:#fff9c4,stroke:#fbc02d,shape:diamond,stroke-width:2px

    %% Start Node
    Start(((Start))):::startEnd

    %% Actions
    Login[Login / Authenticate User]:::action
    Browse[Browse Home Screen]:::action
    
    %% Main Menu Decision
    SelectAction{What do you<br>want to do?}:::decision
    
    %% Action Tracks
    ViewMovies[View Live Cinema Movies]:::action
    ViewStream[View Streaming Catalog]:::action
    ViewLib[Open User Library / Tickets]:::action
    
    %% Booking Track
    SelectMovie[Select Movie & Cinema]:::action
    SelectShowtime[Pick a Showtime]:::action
    CheckSeats{Are Seats<br>Available?}:::decision
    
    PickSeats[Select Platinum/Gold/Silver Seats]:::action
    AddFood{Pre-Order<br>Food?}:::decision
    PickFood[Add Items from Food Menu]:::action
    
    %% Checkout Track
    Checkout[Proceed to Checkout]:::action
    Payment{Payment<br>Successful?}:::decision
    ProcessTx[Process Payment Intent]:::action
    
    WriteDB_Booking[Insert into bookings & booking_seats]:::action
    LockSeats[Update seats is_booked=TRUE]:::action
    
    WriteDB_Stream[Insert into streaming_transactions]:::action
    UnlockStream[Add to user_library]:::action
    
    %% End Node
    End(((End))):::startEnd

    %% ==========================================
    %% Activity Flow Linking
    %% ==========================================
    Start --> Login
    Login --> Browse
    Browse --> SelectAction

    %% Split
    SelectAction -- "Book Theatre Tickets" --> ViewMovies
    SelectAction -- "Buy/Rent Digital Movies" --> ViewStream
    SelectAction -- "Check History" --> ViewLib

    %% Path 1: Theatre Booking
    ViewMovies --> SelectMovie
    SelectMovie --> SelectShowtime
    SelectShowtime --> CheckSeats
    CheckSeats -- "No" --> SelectShowtime
    CheckSeats -- "Yes" --> PickSeats
    
    PickSeats --> AddFood
    AddFood -- "Yes" --> PickFood
    PickFood --> Checkout
    AddFood -- "No" --> Checkout
    
    Checkout --> ProcessTx
    ProcessTx --> Payment
    
    Payment -- "Failed" --> Checkout
    Payment -- "Success" --> WriteDB_Booking
    WriteDB_Booking --> LockSeats
    LockSeats --> End
    
    %% Path 2: OTT Streaming Purchase
    ViewStream --> Checkout
    Payment -- "Success (Stream)" --> WriteDB_Stream
    WriteDB_Stream --> UnlockStream
    UnlockStream --> End

    %% Path 3: Managing Library/Profile
    ViewLib --> End
```

## Detailed Flow Analysis
Because an Activity Diagram models the dynamic control flow from action to action, here is how the dependencies reflect the actual SQL tables working under the hood:
1. **Initial Split (`SelectAction`)**: User decides whether to interact with the local theatre system (`cinemas`, `showtimes`), or the digital OTT system (`streaming_catalog`).
2. **Decision Nodes (`CheckSeats` & `AddFood`)**: Checking seats reads whether `is_booked = FALSE` in the `seats` table. The optional branch allows users to aggregate prices from the `food_menu` table before checking out.
3. **Synchronization (Writing changes)**: Once the `Payment` decision node confirms success via Stripe/Wallet, the flow diverges based on what was bought:
   - For Theatre: The system runs parallel actions to simultaneously record the invoice in `bookings` and physically lock the `seats`. 
   - For Streaming: The system logs the receipt in `streaming_transactions` and vaults the movie access rights into `user_library`.

You can preview this file natively in your markdown reader or drop the ````mermaid```` block into Mermaid Live Editor to view the full graph shape!

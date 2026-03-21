/**
 * NAVIGATION INTEGRATION GUIDE
 * 
 * This guide shows how to add the offer screens to your Navigation Graph
 * and Screen sealed class.
 */

// ============================================================================
// 1. ADD OFFER ROUTES TO Screen.kt (navigation/Screen.kt)
// ============================================================================

/*
// Add these lines to your sealed class Screen:

sealed class Screen(val route: String) {
    // ... existing screens ...
    
    // OFFERS SCREEN ROUTES
    object Offers : Screen("offers")
    data class OfferDetail(val offerId: String) : 
        Screen("offer_detail/{offerId}") {
        fun createRoute(offerId: String) = "offer_detail/$offerId"
    }
    object AdminOfferApproval : Screen("admin_offer_approval")
    object TheatreOwnerCreateOffer : Screen("theatre_owner_create_offer")
    object TheatreOwnerOfferHistory : Screen("theatre_owner_offer_history")
}
*/

// ============================================================================
// 2. UPDATE NavGraph.kt
// ============================================================================

/*
@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // ... existing screens ...
        
        // OFFERS SCREENS
        composable(
            route = Screen.Offers.route
        ) {
            val cityId = getCurrentUserCity() // Get from your auth/location system
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            
            OffersScreen(
                cityId = cityId,
                userId = userId,
                onOfferSelected = { offer ->
                    navController.navigate(Screen.OfferDetail(offer.id).createRoute(offer.id))
                }
            )
        }
        
        composable(
            route = Screen.OfferDetail.route,
            arguments = listOf(
                navArgument("offerId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val offerId = backStackEntry.arguments?.getString("offerId") ?: ""
            
            // You need to fetch the offer by ID in ViewModel
            val viewModel: OffersViewModel = viewModel()
            
            // Launch effect to load offer
            LaunchedEffect(offerId) {
                val offer = OffersRepository.getInstance().getOfferById(offerId)
                offer?.let { viewModel.selectOffer(it) }
            }
            
            val selectedOffer by viewModel.selectedOffer.collectAsState()
            
            selectedOffer?.let {
                OfferDetailScreen(
                    offer = it,
                    onBackClick = { navController.popBackStack() },
                    onApplyCoupons = { offer ->
                        // Navigate to booking screen with selected offer
                        navController.navigate(Screen.BookingSummary.route) {
                            popUpTo(Screen.Offers.route)
                        }
                    }
                )
            }
        }
        
        // ADMIN SCREENS
        composable(
            route = Screen.AdminOfferApproval.route
        ) {
            val adminId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            
            AdminOfferApprovalScreen(
                adminId = adminId
            )
        }
        
        // THEATRE OWNER SCREENS
        composable(
            route = Screen.TheatreOwnerCreateOffer.route
        ) {
            val theatreOwnerId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val theatreOwnerName = getCurrentUserName() // Get from your system
            val cityId = getCurrentUserCity() // Get from your system
            
            TheatreOwnerCreateOfferScreen(
                theatreOwnerId = theatreOwnerId,
                theatreOwnerName = theatreOwnerName,
                cityId = cityId,
                onBackClick = { navController.popBackStack() },
                onOfferCreated = { 
                    navController.navigate(Screen.TheatreOwnerOfferHistory.route) {
                        popUpTo(Screen.TheatreOwnerCreateOffer.route) { 
                            inclusive = true 
                        }
                    }
                }
            )
        }
        
        composable(
            route = Screen.TheatreOwnerOfferHistory.route
        ) {
            val theatreOwnerId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            
            TheatreOwnerOfferHistoryScreen(
                theatreOwnerId = theatreOwnerId,
                onBackClick = { navController.popBackStack() },
                onCreateNewOffer = {
                    navController.navigate(Screen.TheatreOwnerCreateOffer.route)
                }
            )
        }
    }
}
*/

// ============================================================================
// 3. HOW TO NAVIGATE TO OFFERS SCREENS FROM OTHER SCREENS
// ============================================================================

// In HomeScreen - Add offers carousel button
/*
Button(
    onClick = {
        navController.navigate(Screen.Offers.route)
    }
) {
    Text("View All Offers")
}
*/

// In AdminPanel - Add admin offers management
/*
IconButton(
    onClick = {
        navController.navigate(Screen.AdminOfferApproval.route)
    }
) {
    Icon(Icons.Filled.LocalOffer, contentDescription = "Manage Offers")
}
*/

// In TheatreOwnerPanel - Add offer management
/*
Button(
    onClick = {
        navController.navigate(Screen.TheatreOwnerOfferHistory.route)
    }
) {
    Text("My Offers")
}

Button(
    onClick = {
        navController.navigate(Screen.TheatreOwnerCreateOffer.route)
    }
) {
    Text("Create New Offer")
}
*/

// ============================================================================
// 4. PASS NAVCONTROLLER TO SCREENS IF NEEDED
// ============================================================================

// For screens that need to navigate, pass NavController:
/*
@Composable
fun UpdatedOffersScreen(
    navController: NavController,
    cityId: String = "1",
    userId: String = ""
) {
    // ... screen code ...
    
    OfferCard(
        offer = offer,
        onClick = { selectedOffer ->
            navController.navigate(
                Screen.OfferDetail(selectedOffer.id).createRoute(selectedOffer.id)
            )
        }
    )
}
*/

// ============================================================================
// 5. REQUIRED IMPORTS IN NavGraph.kt
// ============================================================================

/*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import com.example.bookmymovie.ui.screens.*
import com.example.bookmymovie.data.repository.OffersRepository
*/

// ============================================================================
// 6. TESTING NAVIGATION
// ============================================================================

// Add these test routes in your preview/debug menu temporarily:
/*
fun testOfferNavigation(navController: NavController) {
    // Test navigating to offers screen
    navController.navigate(Screen.Offers.route)
    
    // Test navigating to specific offer detail
    navController.navigate(Screen.OfferDetail("test-offer-id").createRoute("test-offer-id"))
    
    // Test admin screen
    navController.navigate(Screen.AdminOfferApproval.route)
    
    // Test theatre owner screens
    navController.navigate(Screen.TheatreOwnerCreateOffer.route)
    navController.navigate(Screen.TheatreOwnerOfferHistory.route)
}
*/

// ============================================================================
// 7. DEEP LINKING SUPPORT (Optional)
// ============================================================================

// If you want to support deep links:
/*
composable(
    route = "offer/{offerId}",
    deepLinks = listOf(
        navDeepLink {
            uriPattern = "bookmymovie://offer/{offerId}"
            action = Intent.ACTION_VIEW
        }
    )
) { backStackEntry ->
    val offerId = backStackEntry.arguments?.getString("offerId") ?: ""
    // Show offer detail screen
}
*/

// ============================================================================
// SUMMARY: Screen Navigation Structure
// ============================================================================

/*
HomeScreen
├─ [View All Offers button] → OffersScreen (Carousel + Personalized)
│  ├─ [Click Offer Card] → OfferDetailScreen
│  │  ├─ [Copy Coupon] → Copy to clipboard
│  │  └─ [Apply at Checkout] → Navigate to Booking Summary with coupon context
│  └─ Carousel auto-updates in real-time
│
AdminPanelScreen
├─ [Manage Offers button] → AdminOfferApprovalScreen
│  ├─ Pending Tab → List of pending offers
│  │  ├─ [Approve button] → Approve offer, auto-refresh carousel
│  │  └─ [Reject button] → Show rejection reason dialog
│  └─ History Tab → Approved/Rejected offers
│
TheatreOwnerPanelScreen
├─ [My Offers button] → TheatreOwnerOfferHistoryScreen
│  ├─ Pending Tab → Shows pending offers
│  ├─ Approved Tab → Shows approved offers
│  ├─ Rejected Tab → Shows rejected offers with reasons
│  └─ [Create New Offer button] → TheatreOwnerCreateOfferScreen
│     ├─ Fill form (title, description, discount, dates, coupons)
│     └─ [Submit] → Create offer request, navigate back
│
BookingSummaryScreen
└─ [Coupon Input Field] → Apply coupon from carousel/detail screen
   ├─ Validate coupon
   ├─ Show discount
   └─ Update final price
*/

// ============================================================================
// NOTES:
// ============================================================================

/*
1. Make sure all Screen routes are unique
2. Update your existing OffersScreen route if it exists (currently placeholder)
3. Pass navController to screens that need to navigate
4. Use popBackStack() appropriately to manage back navigation
5. Test navigation flow on both phones and tablets
6. Consider argument passing for future enhancements (e.g., pre-filled offer data)
*/

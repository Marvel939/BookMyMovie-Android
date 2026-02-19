package com.example.bookmymovie.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class WishlistItem(
    val movieId: String = "",
    val title: String = "",
    val posterUrl: String = "",
    val bannerUrl: String = "",
    val rating: String = "",
    val language: String = "",
    val genre: String = "",
    val duration: String = "",
    val description: String = "",
    val releaseDate: String = ""
)

data class Wishlist(
    val name: String = "",
    val movies: Map<String, WishlistItem> = emptyMap()
)

object WishlistManager {

    private val database = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    private fun userId(): String = auth.currentUser?.uid ?: ""

    private fun wishlistRef(): DatabaseReference =
        database.child("wishlists").child(userId())

    suspend fun createWishlist(name: String) {
        val key = wishlistRef().push().key ?: return
        wishlistRef().child(key).child("name").setValue(name).await()
    }

    suspend fun addMovieToWishlist(wishlistId: String, item: WishlistItem) {
        wishlistRef().child(wishlistId).child("movies").child(item.movieId).setValue(item).await()
    }

    suspend fun removeMovieFromWishlist(wishlistId: String, movieId: String) {
        wishlistRef().child(wishlistId).child("movies").child(movieId).removeValue().await()
    }

    suspend fun deleteWishlist(wishlistId: String) {
        wishlistRef().child(wishlistId).removeValue().await()
    }

    fun getWishlistsFlow(): Flow<Map<String, Wishlist>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val wishlists = mutableMapOf<String, Wishlist>()
                for (child in snapshot.children) {
                    val id = child.key ?: continue
                    val name = child.child("name").getValue(String::class.java) ?: ""
                    val movies = mutableMapOf<String, WishlistItem>()
                    for (movieSnap in child.child("movies").children) {
                        val item = movieSnap.getValue(WishlistItem::class.java)
                        if (item != null) {
                            movies[movieSnap.key ?: ""] = item
                        }
                    }
                    wishlists[id] = Wishlist(name, movies)
                }
                trySend(wishlists)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        wishlistRef().addValueEventListener(listener)
        awaitClose { wishlistRef().removeEventListener(listener) }
    }

    fun isMovieInAnyWishlistFlow(movieId: String): Flow<Set<String>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val wishlistIds = mutableSetOf<String>()
                for (child in snapshot.children) {
                    val id = child.key ?: continue
                    if (child.child("movies").hasChild(movieId)) {
                        wishlistIds.add(id)
                    }
                }
                trySend(wishlistIds)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        wishlistRef().addValueEventListener(listener)
        awaitClose { wishlistRef().removeEventListener(listener) }
    }
}

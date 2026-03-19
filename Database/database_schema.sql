-- =========================================================================
-- BookMyMovie - Relational Database Schema
-- Auto-generated from Firebase Realtime Database JSON Export
-- =========================================================================

-- 1. USERS & ROLES
CREATE TABLE users (
    user_id VARCHAR(100) PRIMARY KEY,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20),
    gender VARCHAR(20),
    dob VARCHAR(20),
    address TEXT,
    city VARCHAR(100),
    country_code VARCHAR(10),
    profile_image_url VARCHAR(500),
    created_at BIGINT
);

CREATE TABLE admin_users (
    admin_id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(255),
    email VARCHAR(255) UNIQUE NOT NULL,
    role VARCHAR(50),
    created_at BIGINT
);

CREATE TABLE theatre_owners (
    owner_uid VARCHAR(100) PRIMARY KEY,
    name VARCHAR(255),
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20),
    cinema_name VARCHAR(255),
    city VARCHAR(100),
    place_id VARCHAR(100),
    status VARCHAR(50),
    rejected_reason TEXT,
    registered_at BIGINT,
    approved_at BIGINT
);

-- 2. MOVIES (Derived to support Foreign Keys)
CREATE TABLE movies (
    movie_id VARCHAR(100) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    language VARCHAR(100),
    genre VARCHAR(255),
    duration VARCHAR(50),
    release_date VARCHAR(50),
    poster_url VARCHAR(500),
    banner_url VARCHAR(500),
    rating VARCHAR(20)
);

-- 3. CINEMAS & SCREENS
CREATE TABLE cinemas (
    place_id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address TEXT,
    lat DOUBLE PRECISION,
    lng DOUBLE PRECISION,
    rating DOUBLE PRECISION
);

CREATE TABLE cinema_photos (
    photo_id SERIAL PRIMARY KEY,
    place_id VARCHAR(100) NOT NULL REFERENCES cinemas(place_id) ON DELETE CASCADE,
    photo_url VARCHAR(1000) NOT NULL
);

CREATE TABLE cinema_screens (
    screen_id VARCHAR(100) PRIMARY KEY,
    place_id VARCHAR(100) NOT NULL REFERENCES cinemas(place_id) ON DELETE CASCADE,
    owner_uid VARCHAR(100) REFERENCES theatre_owners(owner_uid) ON DELETE SET NULL,
    screen_name VARCHAR(100),
    screen_type VARCHAR(50),
    total_seats INT,
    platinum_seats INT,
    gold_seats INT,
    silver_seats INT
);

CREATE TABLE seats (
    seat_id VARCHAR(100) PRIMARY KEY, -- usually row + col concatenated 
    screen_id VARCHAR(100) NOT NULL REFERENCES cinema_screens(screen_id) ON DELETE CASCADE,
    row_char VARCHAR(10),
    col_num INT,
    seat_type VARCHAR(50),
    price DECIMAL(10, 2),
    is_booked BOOLEAN DEFAULT FALSE,
    booked_by_uid VARCHAR(100) REFERENCES users(user_id) ON DELETE SET NULL
);

CREATE TABLE food_menu (
    item_id VARCHAR(100) PRIMARY KEY,
    cinema_id VARCHAR(100) NOT NULL REFERENCES cinemas(place_id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    description TEXT,
    price DECIMAL(10, 2),
    available BOOLEAN DEFAULT TRUE,
    image_url VARCHAR(500)
);

-- 4. SHOWTIMES
CREATE TABLE showtimes (
    showtime_id VARCHAR(100) PRIMARY KEY,
    movie_id VARCHAR(100) NOT NULL REFERENCES movies(movie_id) ON DELETE CASCADE,
    screen_id VARCHAR(100) REFERENCES cinema_screens(screen_id) ON DELETE CASCADE,
    date VARCHAR(20),
    time VARCHAR(20),
    language VARCHAR(100),
    platinum_price DECIMAL(10, 2),
    gold_price DECIMAL(10, 2),
    silver_price DECIMAL(10, 2)
);

CREATE TABLE showtime_requests (
    request_id VARCHAR(100) PRIMARY KEY,
    owner_uid VARCHAR(100) NOT NULL REFERENCES theatre_owners(owner_uid) ON DELETE CASCADE,
    place_id VARCHAR(100) NOT NULL REFERENCES cinemas(place_id) ON DELETE CASCADE,
    screen_id VARCHAR(100) NOT NULL REFERENCES cinema_screens(screen_id) ON DELETE CASCADE,
    movie_id VARCHAR(100) NOT NULL REFERENCES movies(movie_id) ON DELETE CASCADE,
    date VARCHAR(20),
    time VARCHAR(20),
    status VARCHAR(50),
    rejected_reason TEXT,
    platinum_price DECIMAL(10, 2),
    gold_price DECIMAL(10, 2),
    silver_price DECIMAL(10, 2),
    submitted_at BIGINT,
    reviewed_at BIGINT
);

-- 5. BOOKINGS & TICKETS
CREATE TABLE bookings (
    booking_id VARCHAR(100) PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    showtime_id VARCHAR(100) REFERENCES showtimes(showtime_id) ON DELETE SET NULL,
    movie_id VARCHAR(100) NOT NULL REFERENCES movies(movie_id) ON DELETE CASCADE,
    place_id VARCHAR(100) NOT NULL REFERENCES cinemas(place_id) ON DELETE CASCADE,
    screen_id VARCHAR(100) NOT NULL REFERENCES cinema_screens(screen_id) ON DELETE CASCADE,
    date VARCHAR(20),
    time VARCHAR(20),
    status VARCHAR(50),
    payment_status VARCHAR(50),
    payment_method VARCHAR(50),
    payment_intent_id VARCHAR(255),
    total_amount DECIMAL(10, 2),
    seat_amount DECIMAL(10, 2),
    food_amount DECIMAL(10, 2),
    convenience_fee_amount DECIMAL(10, 2),
    ticket_gst_amount DECIMAL(10, 2),
    ticket_gst_rate DECIMAL(5, 2),
    refundable_amount DECIMAL(10, 2),
    non_refundable_amount DECIMAL(10, 2),
    refund_id VARCHAR(255),
    refund_status VARCHAR(50),
    refund_reason TEXT,
    booked_at BIGINT,
    refunded_at BIGINT
);

CREATE TABLE booking_seats (
    booking_seat_id SERIAL PRIMARY KEY,
    booking_id VARCHAR(100) NOT NULL REFERENCES bookings(booking_id) ON DELETE CASCADE,
    seat_number VARCHAR(20) NOT NULL,
    seat_type VARCHAR(50)
);

CREATE TABLE booking_food_items (
    booking_food_id SERIAL PRIMARY KEY,
    booking_id VARCHAR(100) NOT NULL REFERENCES bookings(booking_id) ON DELETE CASCADE,
    item_name VARCHAR(255),
    qty INT,
    price DECIMAL(10, 2),
    total DECIMAL(10, 2)
);

-- 6. REVIEWS
CREATE TABLE reviews (
    review_id VARCHAR(100) PRIMARY KEY,
    movie_id VARCHAR(100) NOT NULL REFERENCES movies(movie_id) ON DELETE CASCADE,
    user_id VARCHAR(100) NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    rating INT,
    content TEXT,
    review_date VARCHAR(50),
    timestamp_val BIGINT
);

CREATE TABLE review_tags (
    tag_id SERIAL PRIMARY KEY,
    review_id VARCHAR(100) NOT NULL REFERENCES reviews(review_id) ON DELETE CASCADE,
    tag_name VARCHAR(100) NOT NULL
);

-- 7. AI CHAT HISTORY
CREATE TABLE ai_chat_sessions (
    session_id VARCHAR(100) PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE ai_chat_messages (
    message_id VARCHAR(100) PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL REFERENCES ai_chat_sessions(session_id) ON DELETE CASCADE,
    role VARCHAR(20),
    text_content TEXT,
    timestamp_val BIGINT
);

-- 8. STREAMING CATALOG & TRANSACTIONS
CREATE TABLE streaming_catalog (
    movie_id VARCHAR(100) PRIMARY KEY REFERENCES movies(movie_id) ON DELETE CASCADE,
    director VARCHAR(255),
    cast_list TEXT, -- Or a separate 1:N table 
    ott_platform VARCHAR(100),
    buy_price DECIMAL(10, 2),
    rent_price DECIMAL(10, 2),
    rent_duration_days INT,
    is_active BOOLEAN DEFAULT TRUE,
    is_exclusive BOOLEAN DEFAULT FALSE,
    stream_url VARCHAR(500),
    trailer_url VARCHAR(500),
    source VARCHAR(50)
);

CREATE TABLE streaming_transactions (
    transaction_id VARCHAR(100) PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    movie_id VARCHAR(100) NOT NULL REFERENCES movies(movie_id) ON DELETE CASCADE,
    type VARCHAR(50), -- rent or buy
    amount DECIMAL(10, 2),
    status VARCHAR(50),
    payment_intent_id VARCHAR(255),
    timestamp_val BIGINT
);

CREATE TABLE user_library (
    library_id VARCHAR(100) PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    movie_id VARCHAR(100) NOT NULL REFERENCES movies(movie_id) ON DELETE CASCADE,
    type VARCHAR(50), -- rent or buy
    amount_paid DECIMAL(10, 2),
    ott_platform VARCHAR(100),
    purchased_at BIGINT,
    expires_at BIGINT,
    payment_intent_id VARCHAR(255)
);

-- 9. WISHLISTS
CREATE TABLE wishlists (
    wishlist_id VARCHAR(100) PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    name VARCHAR(255)
);

CREATE TABLE wishlist_movies (
    wishlist_id VARCHAR(100) NOT NULL REFERENCES wishlists(wishlist_id) ON DELETE CASCADE,
    movie_id VARCHAR(100) NOT NULL REFERENCES movies(movie_id) ON DELETE CASCADE,
    PRIMARY KEY (wishlist_id, movie_id)
);

-- =========================================================================
-- INDEXES FOR PERFORMANCE
-- =========================================================================

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_theatre_owners_email ON theatre_owners(email);
CREATE INDEX idx_movies_title ON movies(title);

CREATE INDEX idx_cinema_screens_place_id ON cinema_screens(place_id);
CREATE INDEX idx_cinema_screens_owner_uid ON cinema_screens(owner_uid);

CREATE INDEX idx_seats_screen_id ON seats(screen_id);

CREATE INDEX idx_showtimes_movie_id ON showtimes(movie_id);
CREATE INDEX idx_showtimes_screen_id ON showtimes(screen_id);

CREATE INDEX idx_showtime_requests_status ON showtime_requests(status);
CREATE INDEX idx_showtime_requests_owner_uid ON showtime_requests(owner_uid);

CREATE INDEX idx_bookings_user_id ON bookings(user_id);
CREATE INDEX idx_bookings_showtime_id ON bookings(showtime_id);
CREATE INDEX idx_bookings_movie_id ON bookings(movie_id);
CREATE INDEX idx_bookings_place_id ON bookings(place_id);

CREATE INDEX idx_reviews_movie_id ON reviews(movie_id);
CREATE INDEX idx_reviews_user_id ON reviews(user_id);

CREATE INDEX idx_ai_chat_messages_session_id ON ai_chat_messages(session_id);

CREATE INDEX idx_streaming_tx_user_id ON streaming_transactions(user_id);
CREATE INDEX idx_user_library_user_id ON user_library(user_id);

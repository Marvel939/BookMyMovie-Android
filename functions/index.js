const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { setGlobalOptions } = require("firebase-functions/v2");
const nodemailer = require("nodemailer");
const PDFDocument = require("pdfkit");
const QRCode = require("qrcode");
const admin = require("firebase-admin");

admin.initializeApp();
const bucket = admin.storage().bucket("bookmymovie393.firebasestorage.app");

setGlobalOptions({ maxInstances: 10 });

function getMailTransporter() {
  return nodemailer.createTransport({
    host: "smtp.gmail.com",
    port: 465,
    secure: true,
    auth: {
      user: process.env.GMAIL_EMAIL,
      pass: process.env.GMAIL_PASSWORD,
    },
  });
}

// ── Create Stripe PaymentIntent ───────────────────────────────────────────────
exports.createPaymentIntent = onCall(async (request) => {
  const stripe = require("stripe")(process.env.STRIPE_SECRET);
  const { amount } = request.data;
  if (!amount || amount <= 0) throw new Error("Invalid amount");

  const paymentIntent = await stripe.paymentIntents.create({
    amount: amount * 100, // Convert INR to paise
    currency: "inr",
    automatic_payment_methods: { enabled: true },
  });

  return {
    clientSecret: paymentIntent.client_secret,
    paymentIntentId: paymentIntent.id,
  };
});

exports.processWalletPayment = onCall(async (request) => {
  const uid = request.auth?.uid;
  const amount = Number(request.data?.amount || 0);
  if (!uid) throw new HttpsError("unauthenticated", "Authentication required");
  if (!Number.isFinite(amount) || amount <= 0) {
    throw new HttpsError("invalid-argument", "Invalid amount");
  }

  const userRef = admin.database().ref(`users/${uid}`);
  const walletRef = userRef.child("walletBalance");

  let currentBalance = 0;
  try {
    const [walletBalSnap, walletAmountSnap, nestedWalletBalSnap] = await Promise.all([
      walletRef.get(),
      userRef.child("walletAmount").get(),
      userRef.child("wallet").child("balance").get(),
    ]);

    const candidates = [walletBalSnap.val(), walletAmountSnap.val(), nestedWalletBalSnap.val()]
      .map((v) => Number(v))
      .filter((v) => Number.isFinite(v));

    currentBalance = candidates.length > 0 ? candidates[0] : 0;
  } catch (e) {
    throw new HttpsError("internal", "Wallet balance could not be read");
  }

  if (currentBalance < amount) {
    throw new HttpsError("failed-precondition", "Not enough amount in wallet");
  }

  const nextBalance = Number((currentBalance - amount).toFixed(2));
  try {
    await walletRef.set(nextBalance);
  } catch (e) {
    throw new HttpsError("internal", "Wallet payment could not be processed");
  }

  const walletTxId = `WALLET_TX_${Date.now()}`;
  // Logging failure should not fail a successful payment debit.
  try {
    await admin.database().ref(`users/${uid}/wallet_transactions/${walletTxId}`).set({
      txId: walletTxId,
      type: "debit",
      amount,
      movieName: "",
      bookingId: "",
      createdAt: Date.now(),
      note: "Booking payment from wallet",
      balanceAfter: nextBalance,
    });
  } catch (e) {
    console.error("Wallet transaction logging failed:", e?.message || e);
  }

  return {
    success: true,
    walletTxId,
    balance: nextBalance,
  };
});

function parseShowTimestamp(dateStr, timeStr) {
  const dateMatch = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(dateStr || ""));
  const timeMatch = /^(\d{1,2}):(\d{2})\s*([AP]M)$/i.exec(String(timeStr || "").trim());
  if (!dateMatch || !timeMatch) return null;

  const year = Number(dateMatch[1]);
  const month = Number(dateMatch[2]) - 1;
  const day = Number(dateMatch[3]);
  let hour = Number(timeMatch[1]);
  const minute = Number(timeMatch[2]);
  const meridiem = timeMatch[3].toUpperCase();

  if (meridiem === "PM" && hour !== 12) hour += 12;
  if (meridiem === "AM" && hour === 12) hour = 0;

  return new Date(year, month, day, hour, minute, 0, 0).getTime();
}

exports.requestBookingRefund = onCall(async (request) => {
  const stripe = require("stripe")(process.env.STRIPE_SECRET);
  const uid = request.auth?.uid;
  const bookingId = request.data?.bookingId;

  if (!uid) throw new Error("Authentication required");
  if (!bookingId) throw new Error("Missing bookingId");

  const bookingRef = admin.database().ref(`bookings/${uid}/${bookingId}`);
  const allBookingRef = admin.database().ref(`all_bookings/${bookingId}`);
  const snap = await bookingRef.get();
  if (!snap.exists()) throw new Error("Booking not found");

  const booking = snap.val() || {};
  if (booking.userId !== uid) throw new Error("Unauthorized booking access");
  if ((booking.status || "").toLowerCase() !== "confirmed") {
    throw new Error("Only confirmed bookings can be refunded");
  }
  if ((booking.refundStatus || "none") === "succeeded") {
    throw new Error("Booking is already refunded");
  }

  const showTs = parseShowTimestamp(booking.date, booking.time);
  if (!showTs) throw new Error("Invalid booking show date/time");
  
  const now = Date.now();
  const timeUntilShow = showTs - now;
  const oneDay = 24 * 60 * 60 * 1000; // 86400000 ms
  const threeHours = 3 * 60 * 60 * 1000; // 10800000 ms
  const twoHours = 2 * 60 * 60 * 1000; // 7200000 ms
  const oneHour = 1 * 60 * 60 * 1000; // 3600000 ms
  
  // Calculate refund percentage based on time before show
  let refundPercentage = 0;
  let refundReason = "";
  
  if (timeUntilShow >= oneDay) {
    // >=1 day before show: 100% refund
    refundPercentage = 100;
    refundReason = "100% refund - more than 1 day before show";
  } else if (timeUntilShow >= threeHours) {
    // 3-24 hours before show: 75% refund
    refundPercentage = 75;
    refundReason = "75% refund - 3-24 hours before show";
  } else if (timeUntilShow >= twoHours) {
    // 2-3 hours before show: 50% refund
    refundPercentage = 50;
    refundReason = "50% refund - 2-3 hours before show";
  } else if (timeUntilShow >= oneHour) {
    // 1-2 hours before show: 0% refund
    refundPercentage = 0;
    throw new HttpsError("failed-precondition", "Refund not allowed - less than 2 hours before showtime");
  } else {
    // <1 hour before show: 0% refund
    refundPercentage = 0;
    throw new HttpsError("failed-precondition", "Refund not allowed - less than 1 hour before showtime");
  }
  
  // Check if this is a duplicate refund attempt for the same movie
  const userBookingsSnap = await admin.database().ref(`bookings/${uid}`).get();
  const userBookings = userBookingsSnap.val() || {};
  const movieName = booking.movieName || "";
  
  // Check if any other booking for the same movie has already been refunded
  const duplicateRefund = Object.entries(userBookings).some(([bId, bData]) => {
    return bId !== bookingId && 
           (bData.movieName || "").trim() === movieName.trim() && 
           (bData.refundStatus === "succeeded");
  });
  
  if (duplicateRefund) {
    throw new HttpsError("already-exists", "You have already received a refund for this movie. Only one refund per movie is allowed.");
  }

  const seatAmount = Number(booking.seatAmount || 0);
  const foodAmount = Number(booking.foodAmount || 0);
  const ticketGstAmount = Number(booking.ticketGstAmount || 0);
  const convenienceFeeAmount = Number(booking.convenienceFeeAmount || 0);
  const convenienceFeeGstAmount = Number(booking.convenienceFeeGstAmount || 0);

  // Calculate refund based on percentage of (seat + food amount) only - exclude taxes and fees
  const refundableBaseAmount = seatAmount + foodAmount;
  const refundableAmount = Math.round((refundableBaseAmount * refundPercentage) / 100);
  const nonRefundableAmount =
    ticketGstAmount +
    convenienceFeeAmount +
    convenienceFeeGstAmount +
    (refundableBaseAmount - refundableAmount);

  if (refundableAmount <= 0) {
    throw new Error("No refundable amount available for this booking");
  }

  const paymentMethod = String(booking.paymentMethod || "stripe").toLowerCase();

  let refundId = "";
  if (paymentMethod === "stripe") {
    if (!booking.paymentIntentId) {
      throw new Error("No payment reference found for this booking");
    }

    const refund = await stripe.refunds.create(
      {
        payment_intent: booking.paymentIntentId,
        amount: refundableAmount * 100,
        reason: "requested_by_customer",
        metadata: {
          bookingId,
          userId: uid,
        },
      },
      {
        idempotencyKey: `booking_refund_${bookingId}`,
      }
    );
    refundId = refund.id;
  } else {
    refundId = `WALLET_REFUND_${bookingId}_${Date.now()}`;
  }

  const updates = {
    status: "cancelled",
    paymentStatus: "partially_refunded",
    refundStatus: "succeeded",
    refundReason: refundReason,
    refundPercentage: refundPercentage,
    refundId,
    refundedAt: now,
    refundableAmount,
    nonRefundableAmount,
  };

  await Promise.all([
    bookingRef.update(updates),
    allBookingRef.update(updates),
  ]);

  const seatValues = booking.seats && typeof booking.seats === "object"
    ? Object.values(booking.seats)
    : [];
  const seatIds = seatValues.filter((s) => typeof s === "string");
  if (seatIds.length > 0 && booking.placeId && booking.screenId && booking.showtimeId) {
    const seatsBase = admin.database().ref(
      `seats/${booking.placeId}/${booking.screenId}/${booking.showtimeId}`
    );
    const seatUpdates = {};
    for (const seatId of seatIds) {
      seatUpdates[`${seatId}/booked`] = false;
      seatUpdates[`${seatId}/bookedByUid`] = "";
    }
    await seatsBase.update(seatUpdates);
  }

  const walletRef = admin.database().ref(`users/${uid}/walletBalance`);
  const walletTxId = `WALLET_TX_${Date.now()}`;
  const walletTxResult = await new Promise((resolve, reject) => {
    walletRef.transaction(
      (current) => Number((Number(current || 0) + refundableAmount).toFixed(2)),
      (error, committed, snapshot) => {
        if (error) return reject(error);
        resolve({ committed, balance: Number(snapshot?.val() || 0) });
      },
      false
    );
  });
  if (!walletTxResult.committed) throw new Error("Failed to credit wallet");

  await admin.database().ref(`users/${uid}/wallet_transactions/${walletTxId}`).set({
    txId: walletTxId,
    type: "credit",
    amount: refundableAmount,
    movieName: booking.movieName || "",
    bookingId,
    createdAt: now,
    note: "Refund credited to wallet",
    balanceAfter: walletTxResult.balance,
  });

  await admin.database().ref(`users/${uid}/refunds/${bookingId}`).set({
    bookingId,
    movieName: booking.movieName || "",
    refundedAmount: refundableAmount,
    nonRefundableAmount,
    refundedAt: now,
    status: "succeeded",
  });

  const userSnap = await admin.database().ref(`users/${uid}`).get();
  const user = userSnap.val() || {};
  const email = user.email || booking.userEmail || "";
  const countryCode = user.countryCode || "";
  const phone = user.phone || "";
  const fullPhone = `${countryCode}${phone}`;

  if (email && process.env.GMAIL_EMAIL && process.env.GMAIL_PASSWORD) {
    try {
      console.log("Sending refund email to:", email);
      const transporter = getMailTransporter();
      const info = await transporter.sendMail({
        from: `BookMyMovie <${process.env.GMAIL_EMAIL}>`,
        to: email,
        subject: `Refund Processed – ${booking.movieName || "Booking"}`,
        html: `
          <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;background:#1a1a2e;color:#fff;padding:24px;border-radius:12px;">
            <h2 style="color:#e50914;margin-top:0;">Refund Successful</h2>
            <p>Your refund has been processed and credited to your wallet.</p>
            <p><b>Movie:</b> ${booking.movieName || ""}</p>
            <p><b>Booking ID:</b> ${bookingId}</p>
            <p><b>Refund Percentage:</b> ${refundPercentage}%</p>
            <p><b>Refunded Amount:</b> ₹${refundableAmount}</p>
            <p><b>Non-refundable:</b> ₹${nonRefundableAmount} (GST + convenience fee)</p>
            <p style="font-size:12px;color:#999;"><i>${refundReason}</i></p>
          </div>
        `,
      });
      console.log("Refund email sent successfully. Message ID:", info.messageId);
    } catch (e) {
      console.error("Refund email send failed - Details:", {
        message: e.message,
        code: e.code,
        to: email
      });
    }
  } else {
    console.warn("Refund email not sent. Missing credentials or email. Email:", email, "GMAIL_EMAIL:", !!process.env.GMAIL_EMAIL, "GMAIL_PASSWORD:", !!process.env.GMAIL_PASSWORD);
  }

  if (fullPhone.startsWith("+") && process.env.TWILIO_ACCOUNT_SID && process.env.TWILIO_AUTH_TOKEN) {
    try {
      const twilio = require("twilio")(process.env.TWILIO_ACCOUNT_SID, process.env.TWILIO_AUTH_TOKEN);
      const twilioWhatsAppNumber = process.env.TWILIO_WHATSAPP_NUMBER || "whatsapp:+14155238886";
      const body =
        `✅ Refund Successful\n\n` +
        `Movie: ${booking.movieName || ""}\n` +
        `Booking ID: ${bookingId}\n` +
        `Refund Percentage: ${refundPercentage}%\n` +
        `Refunded Amount: ₹${refundableAmount}\n` +
        `Non-refundable: ₹${nonRefundableAmount} (GST + convenience fee)\n\n` +
        `Amount has been credited to your wallet.`;
      await twilio.messages.create({
        from: twilioWhatsAppNumber,
        to: `whatsapp:${fullPhone}`,
        body,
      });
    } catch (e) {
      console.error("Refund WhatsApp send failed:", e.message);
    }
  }

  return {
    success: true,
    bookingId,
    refundId,
    refundableAmount,
    nonRefundableAmount,
    walletTxId,
    walletBalance: walletTxResult.balance,
  };
});

// ── Generate Booking PDF ──────────────────────────────────────────────────────
async function generateBookingPdf(booking, qrBuffer) {
  return new Promise((resolve, reject) => {
    const doc = new PDFDocument({ margin: 50, size: "A4" });
    const chunks = [];
    doc.on("data", (chunk) => chunks.push(chunk));
    doc.on("end", () => resolve(Buffer.concat(chunks)));
    doc.on("error", reject);

    const seatsText = Array.isArray(booking.seats)
      ? booking.seats.join(", ")
      : booking.seats;

    // Header
    doc.fontSize(28).fillColor("#e50914").text("BookMyMovie", { align: "center" });
    doc.fontSize(13).fillColor("#555555").text("Booking Confirmation Ticket", { align: "center" });
    doc.moveDown(0.5);
    doc.moveTo(50, doc.y).lineTo(550, doc.y).strokeColor("#e50914").lineWidth(2).stroke();
    doc.moveDown(1);

    // Ticket details
    doc.fontSize(14).fillColor("#e50914").text("Ticket Details", 60);
    doc.moveDown(0.5);

    const drawRow = (label, value) => {
      const y = doc.y;
      doc.fontSize(11).fillColor("#777777").text(label, 60, y, { width: 160 });
      doc.fontSize(11).fillColor("#111111").text(value, 230, y, { width: 300 });
      doc.moveDown(0.1);
    };

    drawRow("Booking ID:", booking.bookingId);
    drawRow("Movie:", booking.movieName);
    drawRow("Cinema:", booking.cinemaName);
    drawRow("Screen:", `${booking.screenName} \u00B7 ${booking.screenType}`);
    drawRow("Date:", booking.date);
    drawRow("Time:", booking.time);
    drawRow("Language:", booking.language);
    drawRow("Seats:", seatsText);

    if (booking.discountAmount > 0) {
      drawRow(`Discount (${booking.discountCode}):`, `-\u20B9${booking.discountAmount}`);
    }

    doc.moveDown(0.8);
    doc.moveTo(50, doc.y).lineTo(550, doc.y).strokeColor("#dddddd").lineWidth(1).stroke();
    doc.moveDown(0.6);

    // Total
    doc.fontSize(15).fillColor("#e50914").text(`Total Paid: \u20B9${booking.totalAmount}`, 60);
    doc.moveDown(1);

    // QR code
    doc.moveTo(50, doc.y).lineTo(550, doc.y).strokeColor("#dddddd").lineWidth(1).stroke();
    doc.moveDown(0.8);
    doc.fontSize(12).fillColor("#333333").text("Scan this QR code at the cinema entrance", { align: "center" });
    doc.moveDown(0.5);
    const qrSize = 160;
    const qrX = (doc.page.width - qrSize) / 2;
    doc.image(qrBuffer, qrX, doc.y, { width: qrSize, height: qrSize });
    doc.moveDown(11.5);

    doc.moveTo(50, doc.y).lineTo(550, doc.y).strokeColor("#dddddd").lineWidth(1).stroke();
    doc.moveDown(0.6);

    // Footer
    doc.fontSize(10).fillColor("#aaaaaa").text(
      "Please show this ticket at the theatre entrance. Enjoy your movie!",
      { align: "center" }
    );
    doc.fontSize(9).fillColor("#cccccc").text(
      "This is an auto-generated ticket. No signature required.",
      { align: "center" }
    );

    doc.end();
  });
}

// ── Send Booking Confirmation Email ───────────────────────────────────────────
exports.sendBookingEmail = onCall(async (request) => {
  const { email, booking } = request.data;
  
  // Validate email and environment variables
  if (!email || !email.includes('@')) {
    console.error("Invalid email provided:", email);
    throw new HttpsError("invalid-argument", "Invalid email address");
  }
  
  if (!process.env.GMAIL_EMAIL || !process.env.GMAIL_PASSWORD) {
    console.error("Gmail credentials not configured. EMAIL:", !!process.env.GMAIL_EMAIL, "PASSWORD:", !!process.env.GMAIL_PASSWORD);
    throw new HttpsError("internal", "Email service not configured");
  }
  
  const seatsText = Array.isArray(booking.seats) ? booking.seats.join(", ") : booking.seats;

  try {
    // 1. Generate QR code as PNG buffer (for PDF) and data URL (for HTML email)
    console.log("Generating QR code for booking:", booking.bookingId);
    const [qrBuffer, qrDataUrl] = await Promise.all([
      QRCode.toBuffer(booking.bookingId, {
        errorCorrectionLevel: "M",
        margin: 2,
        width: 300,
        color: { dark: "#000000", light: "#FFFFFF" },
      }),
      QRCode.toDataURL(booking.bookingId, {
        errorCorrectionLevel: "M",
        margin: 2,
        width: 200,
      }),
    ]);
    console.log("QR code generated successfully");

    // 2. Generate PDF with all booking details + QR code
    console.log("Generating PDF for booking:", booking.bookingId);
    const pdfBuffer = await generateBookingPdf(booking, qrBuffer);
    console.log("PDF generated successfully, size:", pdfBuffer.length, "bytes");

    // 3. Send email with PDF attachment
    console.log("Initializing mail transporter");
    const transporter = getMailTransporter();
    
    console.log("Sending email to:", email);
    const info = await transporter.sendMail({
      from: `BookMyMovie <${process.env.GMAIL_EMAIL}>`,
      to: email,
      subject: `Booking Confirmed – ${booking.movieName}`,
      html: `
      <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;background:#1a1a2e;color:#fff;padding:30px;border-radius:12px;">
        <h1 style="color:#e50914;text-align:center;margin:0 0 4px 0;">BookMyMovie</h1>
        <p style="text-align:center;color:#aaa;margin:0 0 24px 0;">Your ticket is confirmed!</p>

        <div style="background:#16213e;padding:20px;border-radius:10px;margin-bottom:20px;">
          <h3 style="color:#e50914;margin:0 0 16px 0;">Ticket Details</h3>
          <table style="width:100%;border-collapse:collapse;">
            <tr><td style="padding:7px 0;color:#aaa;width:40%;">Booking ID</td>
                <td style="color:#fff;font-weight:bold;">${booking.bookingId}</td></tr>
            <tr><td style="padding:7px 0;color:#aaa;">Movie</td>
                <td style="color:#fff;font-weight:bold;">${booking.movieName}</td></tr>
            <tr><td style="padding:7px 0;color:#aaa;">Cinema</td>
                <td style="color:#fff;">${booking.cinemaName}</td></tr>
            <tr><td style="padding:7px 0;color:#aaa;">Screen</td>
                <td style="color:#fff;">${booking.screenName} &bull; ${booking.screenType}</td></tr>
            <tr><td style="padding:7px 0;color:#aaa;">Date</td>
                <td style="color:#fff;">${booking.date}</td></tr>
            <tr><td style="padding:7px 0;color:#aaa;">Time</td>
                <td style="color:#fff;">${booking.time}</td></tr>
            <tr><td style="padding:7px 0;color:#aaa;">Language</td>
                <td style="color:#fff;">${booking.language}</td></tr>
            <tr><td style="padding:7px 0;color:#aaa;">Seats</td>
                <td style="color:#fff;">${seatsText}</td></tr>
            ${booking.discountAmount > 0 ? `<tr><td style="padding:7px 0;color:#aaa;">Discount (${booking.discountCode || ''})</td>
                <td style="color:#2ECC71;">-₹${booking.discountAmount}</td></tr>` : ''}
            <tr style="border-top:1px solid #333;">
                <td style="padding:12px 0 4px 0;color:#aaa;font-size:15px;">Total Paid</td>
                <td style="color:#e50914;font-weight:bold;font-size:20px;padding-top:12px;">₹${booking.totalAmount}</td></tr>
          </table>
        </div>

        <div style="text-align:center;background:#16213e;padding:20px;border-radius:10px;margin-bottom:20px;">
          <p style="color:#aaa;margin:0 0 12px 0;font-size:13px;">Scan at Cinema Entry</p>
          <img src="${qrDataUrl}" width="160" height="160" style="border-radius:8px;" alt="QR Code"/>
        </div>

        <p style="text-align:center;color:#aaa;font-size:12px;margin:0;">
          Your booking ticket PDF is attached. Please show it at the theatre entrance.
        </p>
      </div>
    `,
      attachments: [
        {
          filename: `BookMyMovie-Ticket-${booking.bookingId}.pdf`,
          content: pdfBuffer,
          contentType: "application/pdf",
        },
      ],
    });
    
    console.log("Email sent successfully. Message ID:", info.messageId);
    return { success: true, messageId: info.messageId };
  } catch (error) {
    console.error("Email send error - Full Details:", {
      message: error.message,
      code: error.code,
      response: error.response,
      stack: error.stack
    });
    throw new HttpsError("internal", "Failed to send booking email: " + error.message);
  }
});

// ── Send Booking Confirmation via WhatsApp (with PDF) ─────────────────────────
exports.sendBookingWhatsApp = onCall(async (request) => {
  const { phone, booking } = request.data;

  console.log("sendBookingWhatsApp called with phone:", phone, "bookingId:", booking?.bookingId);

  // Validate phone number format
  if (!phone || typeof phone !== "string") {
    console.error("Invalid phone number provided:", phone);
    throw new HttpsError("invalid-argument", "Invalid phone number format");
  }

  // Phone must have country code
  if (!phone.startsWith("+")) {
    console.error("Phone number missing country code:", phone);
    throw new HttpsError("invalid-argument", "Phone number must include country code (e.g., +91...)");
  }

  if (!booking || !booking.bookingId) {
    console.error("Invalid booking data:", booking);
    throw new HttpsError("invalid-argument", "Invalid booking data");
  }

  const accountSid = process.env.TWILIO_ACCOUNT_SID;
  const authToken = process.env.TWILIO_AUTH_TOKEN;
  const twilioWhatsAppNumber = process.env.TWILIO_WHATSAPP_NUMBER || "whatsapp:+14155238886";

  if (!accountSid || !authToken) {
    console.error("Twilio credentials not configured. SID present:", !!accountSid, "Token present:", !!authToken);
    throw new HttpsError("internal", "Twilio service not configured");
  }

  const twilio = require("twilio")(accountSid, authToken);

  try {
    console.log("Preparing WhatsApp message for booking:", booking.bookingId);
    console.log("Booking data received:", {
      bookingId: booking?.bookingId,
      movieName: booking?.movieName,
      cinemaName: booking?.cinemaName,
      date: booking?.date,
      time: booking?.time,
      seats: booking?.seats
    });

    const seatsText = Array.isArray(booking.seats)
      ? booking.seats.join(", ")
      : (booking.seats || "N/A");

    // Build message body with fallback values
    const messageBody =
      `🎬 *Booking Confirmed!*\n\n` +
      `*Movie:* ${booking.movieName || "N/A"}\n` +
      `*Cinema:* ${booking.cinemaName || "N/A"}\n` +
      `*Screen:* ${booking.screenName || "N/A"} · ${booking.screenType || "N/A"}\n` +
      `*Date:* ${booking.date || "N/A"}\n` +
      `*Time:* ${booking.time || "N/A"}\n` +
      `*Language:* ${booking.language || "N/A"}\n` +
      `*Seats:* ${seatsText}\n` +
      (booking.discountAmount > 0 ? `*Discount (${booking.discountCode || 'Applied'}):* -₹${booking.discountAmount}\n` : "") +
      `*Total Paid:* ₹${booking.totalAmount || "0"}\n\n` +
      `*Booking ID:* ${booking.bookingId}\n\n` +
      `Your ticket PDF is attached. Enjoy the movie! 🍿`;

    let pdfUrl = null;
    let messageSentWithPDF = false;

    // Try to generate and upload PDF, but don't fail if it does
    try {
      console.log("Generating QR code for WhatsApp...");
      const qrBuffer = await QRCode.toBuffer(booking.bookingId, {
        errorCorrectionLevel: "M",
        margin: 2,
        width: 300,
        color: { dark: "#000000", light: "#FFFFFF" },
      });
      console.log("QR code generated successfully");

      console.log("Generating PDF for WhatsApp...");
      const pdfBuffer = await generateBookingPdf(booking, qrBuffer);
      console.log("PDF generated successfully, size:", pdfBuffer.length, "bytes");

      console.log("Uploading PDF to Firebase Storage...");
      const fileName = `booking_tickets/${booking.bookingId}.pdf`;
      const file = bucket.file(fileName);

      await file.save(pdfBuffer, {
        metadata: { contentType: "application/pdf" },
      });
      console.log("PDF uploaded successfully");

      await file.makePublic();
      pdfUrl = `https://storage.googleapis.com/${bucket.name}/${fileName}`;
      console.log("PDF made public, URL:", pdfUrl);
      messageSentWithPDF = true;
    } catch (pdfError) {
      console.warn("PDF generation/upload failed, will send message without PDF:", {
        message: pdfError.message,
        code: pdfError.code,
      });
      pdfUrl = null;
    }

    // Send WhatsApp message (with or without PDF)
    console.log("Sending WhatsApp to:", `whatsapp:${phone}`, "from:", twilioWhatsAppNumber, "withPDF:", messageSentWithPDF);
    
    const messageConfig = {
      from: twilioWhatsAppNumber,
      to: `whatsapp:${phone}`,
      body: messageBody,
    };

    // Only add mediaUrl if PDF was successfully generated and uploaded
    if (pdfUrl) {
      messageConfig.mediaUrl = [pdfUrl];
    }

    const message = await twilio.messages.create(messageConfig);

    console.log(`WhatsApp booking confirmation sent successfully to ${phone}. Message SID:`, message.sid, "WithPDF:", messageSentWithPDF);
    return { 
      success: true, 
      messageSid: message.sid, 
      pdfAttached: messageSentWithPDF,
      message: messageSentWithPDF ? "Booking confirmation with PDF sent" : "Booking confirmation sent (PDF generation failed, message sent without attachment)"
    };
  } catch (error) {
    console.error("WhatsApp send error - Full Details:", {
      message: error.message,
      code: error.code,
      status: error.status,
      phone: phone,
      bookingId: booking?.bookingId,
      stack: error.stack
    });
    throw new HttpsError("internal", "Failed to send WhatsApp message: " + error.message);
  }
});

// ── Send 15-min Show Reminder via WhatsApp ────────────────────────────────────
exports.sendShowReminderWhatsApp = onCall(async (request) => {
  const { phone, movieName, showDate, showTime, theaterName, seats } = request.data;

  console.log("sendShowReminderWhatsApp called for movie:", movieName, "phone:", phone);

  if (!phone || typeof phone !== "string" || !phone.startsWith("+")) {
    console.error("Invalid phone number for reminder:", phone);
    throw new HttpsError("invalid-argument", "Invalid phone number format. Must include country code (e.g., +91...)");
  }

  if (!movieName) {
    console.error("Missing required reminder data: movieName");
    throw new HttpsError("invalid-argument", "Missing movie name");
  }

  const accountSid = process.env.TWILIO_ACCOUNT_SID;
  const authToken = process.env.TWILIO_AUTH_TOKEN;
  const twilioWhatsAppNumber = process.env.TWILIO_WHATSAPP_NUMBER || "whatsapp:+14155238886";

  if (!accountSid || !authToken) {
    console.error("Twilio credentials not configured for reminder");
    throw new HttpsError("internal", "Twilio service not configured");
  }

  const twilio = require("twilio")(accountSid, authToken);

  try {
    const messageBody =
      `🎬 *Reminder!*\n\n` +
      `Your show for *${movieName}* starts in just *15 minutes!*\n\n` +
      `🏛️ *Theater:* ${theaterName}\n` +
      `📅 *Date:* ${showDate}\n` +
      `🕐 *Time:* ${showTime}\n` +
      `💺 *Seats:* ${seats}\n\n` +
      `Enjoy the movie! 🍿`;

    console.log("Sending 15-minute reminder to:", `whatsapp:${phone}`);
    const message = await twilio.messages.create({
      from: twilioWhatsAppNumber,
      to: `whatsapp:${phone}`,
      body: messageBody,
    });

    console.log(`WhatsApp show reminder sent successfully to ${phone}. Message SID:`, message.sid);
    return { success: true, messageSid: message.sid };
  } catch (error) {
    console.error("WhatsApp reminder error - Full Details:", {
      message: error.message,
      code: error.code,
      status: error.status,
      phone: phone,
      movieName: movieName,
      stack: error.stack
    });
    throw new HttpsError("internal", "Failed to send WhatsApp reminder: " + error.message);
  }
});

exports.notifyPaymentFailure = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Authentication required");

  const movieName = String(request.data?.movieName || "Movie");
  const amount = Number(request.data?.amount || 0);
  const reason = String(request.data?.reason || "Payment failed");

  const userSnap = await admin.database().ref(`users/${uid}`).get();
  const user = userSnap.val() || {};
  const email = user.email || "";
  const fullPhone = `${user.countryCode || ""}${user.phone || ""}`;

  if (email && process.env.GMAIL_EMAIL && process.env.GMAIL_PASSWORD) {
    try {
      console.log("Sending payment failure email to:", email);
      const transporter = getMailTransporter();
      const info = await transporter.sendMail({
        from: `BookMyMovie <${process.env.GMAIL_EMAIL}>`,
        to: email,
        subject: `Payment Failed – ${movieName}`,
        html: `
          <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;background:#1a1a2e;color:#fff;padding:24px;border-radius:12px;">
            <h2 style="color:#e50914;margin-top:0;">Payment Failed</h2>
            <p>Your payment could not be completed.</p>
            <p><b>Movie:</b> ${movieName}</p>
            <p><b>Requested Amount:</b> ₹${amount}</p>
            <p><b>Reason:</b> ${reason}</p>
          </div>
        `,
      });
      console.log("Payment failure email sent successfully. Message ID:", info.messageId);
    } catch (e) {
      console.error("Payment failure email send failed - Details:", {
        message: e.message,
        code: e.code,
        to: email,
        movieName: movieName
      });
    }
  } else {
    console.warn("Payment failure email not sent. Missing credentials or email. Email:", email, "GMAIL_EMAIL:", !!process.env.GMAIL_EMAIL, "GMAIL_PASSWORD:", !!process.env.GMAIL_PASSWORD);
  }

  if (fullPhone.startsWith("+") && process.env.TWILIO_ACCOUNT_SID && process.env.TWILIO_AUTH_TOKEN) {
    try {
      const twilio = require("twilio")(process.env.TWILIO_ACCOUNT_SID, process.env.TWILIO_AUTH_TOKEN);
      const twilioWhatsAppNumber = process.env.TWILIO_WHATSAPP_NUMBER || "whatsapp:+14155238886";
      const body =
        `❌ Payment Failed\n\n` +
        `Movie: ${movieName}\n` +
        `Requested Amount: ₹${amount}\n` +
        `Reason: ${reason}`;
      await twilio.messages.create({
        from: twilioWhatsAppNumber,
        to: `whatsapp:${fullPhone}`,
        body,
      });
    } catch (e) {
      console.error("Payment failure WhatsApp send failed:", e.message);
    }
  }

  return { success: true };
});

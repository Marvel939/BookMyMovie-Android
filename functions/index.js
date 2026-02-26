const { onCall } = require("firebase-functions/v2/https");
const { setGlobalOptions } = require("firebase-functions/v2");
const nodemailer = require("nodemailer");
const PDFDocument = require("pdfkit");
const QRCode = require("qrcode");
const admin = require("firebase-admin");

admin.initializeApp();
const bucket = admin.storage().bucket("bookmymovie393.firebasestorage.app");

setGlobalOptions({ maxInstances: 10 });

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

  return { clientSecret: paymentIntent.client_secret };
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
  const seatsText = Array.isArray(booking.seats) ? booking.seats.join(", ") : booking.seats;

  // 1. Generate QR code as PNG buffer (for PDF) and data URL (for HTML email)
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

  // 2. Generate PDF with all booking details + QR code
  const pdfBuffer = await generateBookingPdf(booking, qrBuffer);

  // 3. Send email with PDF attachment
  const transporter = nodemailer.createTransport({
    host: "smtp.gmail.com",
    port: 465,
    secure: true,
    auth: {
      user: process.env.GMAIL_EMAIL,
      pass: process.env.GMAIL_PASSWORD,
    },
  });

  try {
    await transporter.sendMail({
      from: `BookMyMovie <${process.env.GMAIL_EMAIL}>`,
      to: email,
      subject: `Booking Confirmed \u2013 ${booking.movieName}`,
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
            <tr style="border-top:1px solid #333;">
                <td style="padding:12px 0 4px 0;color:#aaa;font-size:15px;">Total Paid</td>
                <td style="color:#e50914;font-weight:bold;font-size:20px;padding-top:12px;">&#8377;${booking.totalAmount}</td></tr>
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
  } catch (error) {
    console.error("Email send error:", error.message, error);
    throw new Error("Failed to send email: " + error.message);
  }

  return { success: true };
});

// ── Send Booking Confirmation via WhatsApp (with PDF) ─────────────────────────
exports.sendBookingWhatsApp = onCall(async (request) => {
  const { phone, booking } = request.data;

  if (!phone || !booking) {
    throw new Error("Missing phone or booking data");
  }

  const accountSid = process.env.TWILIO_ACCOUNT_SID;
  const authToken = process.env.TWILIO_AUTH_TOKEN;
  const twilioWhatsAppNumber = process.env.TWILIO_WHATSAPP_NUMBER || "whatsapp:+14155238886";

  if (!accountSid || !authToken) {
    console.error("Twilio credentials not configured. SID present:", !!accountSid, "Token present:", !!authToken);
    throw new Error("Twilio credentials not configured");
  }

  const twilio = require("twilio")(accountSid, authToken);

  try {
    const seatsText = Array.isArray(booking.seats)
      ? booking.seats.join(", ")
      : booking.seats;

    // 1. Generate QR code buffer
    const qrBuffer = await QRCode.toBuffer(booking.bookingId, {
      errorCorrectionLevel: "M",
      margin: 2,
      width: 300,
      color: { dark: "#000000", light: "#FFFFFF" },
    });

    // 2. Generate PDF
    const pdfBuffer = await generateBookingPdf(booking, qrBuffer);

    // 3. Upload PDF to Firebase Storage and make it public
    const fileName = `booking_tickets/${booking.bookingId}.pdf`;
    const file = bucket.file(fileName);

    await file.save(pdfBuffer, {
      metadata: { contentType: "application/pdf" },
    });

    // Make file publicly accessible so Twilio can fetch it
    await file.makePublic();
    const pdfUrl = `https://storage.googleapis.com/${bucket.name}/${fileName}`;

    // 4. Send WhatsApp message with PDF
    const messageBody =
      `🎬 *Booking Confirmed!*\n\n` +
      `*Movie:* ${booking.movieName}\n` +
      `*Cinema:* ${booking.cinemaName}\n` +
      `*Screen:* ${booking.screenName} · ${booking.screenType}\n` +
      `*Date:* ${booking.date}\n` +
      `*Time:* ${booking.time}\n` +
      `*Language:* ${booking.language}\n` +
      `*Seats:* ${seatsText}\n` +
      `*Total Paid:* ₹${booking.totalAmount}\n\n` +
      `*Booking ID:* ${booking.bookingId}\n\n` +
      `Your ticket PDF is attached. Enjoy the movie! 🍿`;

    await twilio.messages.create({
      from: twilioWhatsAppNumber,
      to: `whatsapp:${phone}`,
      body: messageBody,
      mediaUrl: [pdfUrl],
    });

    console.log(`WhatsApp booking confirmation with PDF sent to ${phone}`);
    return { success: true };
  } catch (error) {
    console.error("WhatsApp send error:", error.message, error);
    throw new Error("Failed to send WhatsApp message: " + error.message);
  }
});

// ── Send 15-min Show Reminder via WhatsApp ────────────────────────────────────
exports.sendShowReminderWhatsApp = onCall(async (request) => {
  const { phone, movieName, showDate, showTime, theaterName, seats } = request.data;

  if (!phone || !movieName) {
    throw new Error("Missing required reminder data");
  }

  const accountSid = process.env.TWILIO_ACCOUNT_SID;
  const authToken = process.env.TWILIO_AUTH_TOKEN;
  const twilioWhatsAppNumber = process.env.TWILIO_WHATSAPP_NUMBER || "whatsapp:+14155238886";

  if (!accountSid || !authToken) {
    console.error("Twilio credentials not configured");
    throw new Error("Twilio credentials not configured");
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

    await twilio.messages.create({
      from: twilioWhatsAppNumber,
      to: `whatsapp:${phone}`,
      body: messageBody,
    });

    console.log(`WhatsApp show reminder sent to ${phone}`);
    return { success: true };
  } catch (error) {
    console.error("WhatsApp reminder error:", error.message, error);
    throw new Error("Failed to send WhatsApp reminder: " + error.message);
  }
});

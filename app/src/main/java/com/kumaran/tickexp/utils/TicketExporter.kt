package com.kumaran.tickexp.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.kumaran.tickexp.data.model.Ticket
import java.io.File
import java.io.FileOutputStream

object TicketExporter {
    fun exportToPdf(context: Context, ticket: Ticket): File? {
        val document = PdfDocument()
        // Standard A4-ish ratio but smaller for mobile ticket
        val pageInfo = PdfDocument.PageInfo.Builder(400, 700, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        // Background
        canvas.drawColor(Color.WHITE)

        // Header with Brand Color (Purple-ish)
        paint.color = android.graphics.Color.parseColor("#9C48EA")
        canvas.drawRect(0f, 0f, 400f, 80f, paint)
        
        paint.color = Color.WHITE
        paint.textSize = 24f
        paint.isFakeBoldText = true
        canvas.drawText("TickExp - ${ticket.type}", 20f, 50f, paint)

        // Body
        paint.color = Color.BLACK
        paint.isFakeBoldText = true
        paint.textSize = 20f
        canvas.drawText(ticket.title, 20f, 120f, paint)

        paint.isFakeBoldText = false
        paint.textSize = 14f
        paint.color = Color.GRAY
        var y = 160f
        
        fun drawField(label: String, value: String) {
            paint.color = Color.GRAY
            paint.isFakeBoldText = false
            canvas.drawText(label, 20f, y, paint)
            paint.color = Color.BLACK
            paint.isFakeBoldText = true
            canvas.drawText(value, 150f, y, paint)
            y += 30f
        }

        drawField("Date:", ticket.date)
        if (ticket.time.isNotEmpty()) drawField("Time:", ticket.time)
        if (ticket.source.isNotEmpty()) {
            drawField("From:", ticket.source)
            drawField("To:", ticket.destination)
        }
        if (ticket.theatre.isNotEmpty()) drawField("Location:", ticket.theatre)
        drawField("Seat(s):", ticket.seat)
        drawField("Price:", "Rs. ${ticket.price}")
        drawField("Status:", ticket.status)
        drawField("Booking ID:", ticket.id)

        // QR Code Section
        y += 40f
        paint.color = Color.BLACK
        paint.isFakeBoldText = true
        canvas.drawText("SCAN QR CODE FOR ENTRY", 80f, y, paint)
        
        y += 20f
        // Draw the QR Code if possible
        val qrBitmap = QRCodeGenerator.generate(ticket.qrData, 250)
        qrBitmap?.let {
            canvas.drawBitmap(it, 75f, y, null)
        } ?: run {
            paint.color = Color.LTGRAY
            canvas.drawRect(75f, y, 325f, y + 250f, paint)
            paint.color = Color.BLACK
            canvas.drawText("QR CODE DATA: ${ticket.qrData}", 80f, y + 125f, paint)
        }

        document.finishPage(page)

        val file = File(context.cacheDir, "Ticket_${ticket.id}.pdf")
        return try {
            val outputStream = FileOutputStream(file)
            document.writeTo(outputStream)
            outputStream.close()
            document.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            document.close()
            null
        }
    }

    fun shareTicket(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Ticket"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing ticket: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

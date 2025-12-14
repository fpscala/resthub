package uz.scala.services

import cats.effect.Sync
import cats.implicits._
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts

import java.awt.Color
import java.io.ByteArrayOutputStream

/**
 * Professional PDF generation service with design
 *
 * Features:
 * - Colors and styled boxes
 * - Headers and sections with borders
 * - Professional formatting
 * - Clean layout
 */
trait PdfService[F[_]] {
  def generateContractPdf(content: String): F[Array[Byte]]
}

object PdfService {
  def make[F[_]: Sync]: PdfService[F] = new Impl[F]

  private class Impl[F[_]: Sync] extends PdfService[F] {

    // Professional color palette
    private val primaryColor = new Color(59, 130, 246)     // Modern blue
    private val primaryDark = new Color(29, 78, 216)       // Dark blue
    private val accentColor = new Color(16, 185, 129)      // Green accent
    private val secondaryColor = new Color(249, 250, 251)  // Very light gray
    private val textColor = new Color(17, 24, 39)          // Almost black
    private val textMuted = new Color(107, 114, 128)       // Muted gray
    private val borderColor = new Color(229, 231, 235)     // Light border
    private val warningColor = new Color(245, 158, 11)     // Amber

    override def generateContractPdf(content: String): F[Array[Byte]] =
      Sync[F].blocking {
        val document = new PDDocument()

        try {
          val page = new PDPage(PDRectangle.A4)
          document.addPage(page)

          val mediaBox = page.getMediaBox
          val pageWidth = mediaBox.getWidth
          val pageHeight = mediaBox.getHeight
          val margin = 50f

          val contentStream = new PDPageContentStream(document, page)

          try {
            var yPosition = pageHeight - margin
            var inSignatureSection = false

            // Draw decorative top border
            drawTopBorder(contentStream, pageWidth, margin)
            yPosition -= 15

            // Parse contract sections
            val lines = content.split("\n").map(_.trim).toList

            lines.takeWhile(_ => yPosition >= 100).foreach { line =>
              if (line.startsWith("UY-JOY IJARASI SHARTNOMASI")) {
                // Draw professional header with gradient effect
                drawHeader(contentStream, line, yPosition, pageWidth, margin)
                yPosition -= 60
              } else if (line.startsWith("Shartnoma raqami:")) {
                yPosition = drawMetaInfo(contentStream, line, yPosition, pageWidth, margin, isFirst = true)
                yPosition -= 5
              } else if (line.startsWith("Sana:")) {
                yPosition = drawMetaInfo(contentStream, line, yPosition, pageWidth, margin, isFirst = false)
                yPosition -= 25
              } else if (line.matches("\\d+\\. .*")) {
                // Section headers with icon-like numbering
                yPosition = drawSectionHeader(contentStream, line, yPosition, pageWidth, margin)
                yPosition -= 10
                // Check if this is signature section
                if (line.contains("5.") || line.contains("IMZO")) {
                  inSignatureSection = true
                }
              } else if (line.startsWith("Ijara beruvchi") && !inSignatureSection) {
                yPosition = drawPartyHeader(contentStream, line, yPosition, pageWidth, margin)
                yPosition -= 5
              } else if (line.startsWith("Ijara oluvchi") && !inSignatureSection) {
                yPosition = drawPartyHeader(contentStream, line, yPosition, pageWidth, margin)
                yPosition -= 5
              } else if (line.startsWith("F.I.O:") || line.startsWith("Email:") ||
                         line.startsWith("Telefon:") || line.startsWith("Manzil:") ||
                         line.startsWith("Tavsif:")) {
                yPosition = drawDetailRow(contentStream, line, yPosition, pageWidth, margin)
              } else if (line.startsWith("Oylik to'lov:")) {
                yPosition = drawPriceHighlight(contentStream, line, yPosition, pageWidth, margin)
                yPosition -= 10
              } else if (line.startsWith("___") && inSignatureSection && !line.isEmpty) {
                // Draw both signature boxes side by side, only once
                yPosition = drawSignatureBoxes(contentStream, yPosition, pageWidth, margin)
                inSignatureSection = false // Don't draw again
              } else if (line.nonEmpty && !line.startsWith("___")) {
                yPosition = drawNormalText(contentStream, line, yPosition, pageWidth, margin)
              } else if (line.isEmpty) {
                yPosition -= 6 // Empty line spacing
              }
            }

            // Draw footer with decorative bottom border
            drawFooter(contentStream, pageWidth, pageHeight, margin)

          } finally {
            contentStream.close()
          }

          // Save to byte array
          val outputStream = new ByteArrayOutputStream()
          try {
            document.save(outputStream)
            outputStream.toByteArray
          } finally {
            outputStream.close()
          }
        } finally {
          document.close()
        }
      }

    // New helper methods
    private def drawTopBorder(cs: PDPageContentStream, pageWidth: Float, margin: Float): Unit = {
      cs.setStrokingColor(primaryColor)
      cs.setLineWidth(3f)
      cs.moveTo(margin, 820)
      cs.lineTo(pageWidth - margin, 820)
      cs.stroke()
    }

    private def drawFooter(cs: PDPageContentStream, pageWidth: Float, pageHeight: Float, margin: Float): Unit = {
      // Bottom decorative line
      cs.setStrokingColor(borderColor)
      cs.setLineWidth(1.5f)
      cs.moveTo(margin, 40)
      cs.lineTo(pageWidth - margin, 40)
      cs.stroke()

      // Footer text
      cs.beginText()
      cs.setNonStrokingColor(textMuted)
      cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8f)
      cs.newLineAtOffset(margin, 25)
      cs.showText("NestHub - Professional Rental Agreements")
      cs.endText()
    }

    private def drawHeader(
        cs: PDPageContentStream,
        text: String,
        y: Float,
        pageWidth: Float,
        margin: Float,
      ): Unit = {
      val boxHeight = 45f
      val boxY = y - boxHeight

      // Draw gradient-like effect with two layers
      cs.setNonStrokingColor(primaryDark)
      cs.addRect(margin, boxY, pageWidth - 2 * margin, boxHeight)
      cs.fill()

      // Overlay with slight transparency effect via lighter color
      cs.setNonStrokingColor(primaryColor)
      cs.addRect(margin, boxY + 5, pageWidth - 2 * margin, boxHeight - 10)
      cs.fill()

      // Draw white text with shadow effect
      cs.beginText()
      cs.setNonStrokingColor(Color.WHITE)
      cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18f)
      cs.newLineAtOffset(margin + 20, boxY + 15)
      cs.showText(text)
      cs.endText()

      // Add decorative corner elements
      cs.setNonStrokingColor(accentColor)
      cs.addRect(margin, boxY, 4, boxHeight)
      cs.fill()
      cs.addRect(pageWidth - margin - 4, boxY, 4, boxHeight)
      cs.fill()
    }

    private def drawMetaInfo(
        cs: PDPageContentStream,
        text: String,
        y: Float,
        pageWidth: Float,
        margin: Float,
        isFirst: Boolean,
      ): Float = {
      val boxWidth = 250f
      val boxHeight = 22f
      val boxX = pageWidth - margin - boxWidth
      val boxY = y - boxHeight

      // Draw subtle background
      cs.setNonStrokingColor(secondaryColor)
      cs.addRect(boxX, boxY, boxWidth, boxHeight)
      cs.fill()

      // Draw left accent bar
      cs.setNonStrokingColor(if (isFirst) primaryColor else accentColor)
      cs.addRect(boxX, boxY, 3, boxHeight)
      cs.fill()

      // Draw text
      cs.beginText()
      cs.setNonStrokingColor(textMuted)
      cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9f)
      cs.newLineAtOffset(boxX + 10, boxY + 8)
      cs.showText(text)
      cs.endText()

      boxY
    }

    private def drawSectionHeader(
        cs: PDPageContentStream,
        text: String,
        y: Float,
        pageWidth: Float,
        margin: Float,
      ): Float = {
      val boxHeight = 30f
      val boxY = y - boxHeight

      // Extract number from section
      val parts = text.split("\\.", 2)
      val number = if (parts.length == 2) parts(0) else "0"
      val title = if (parts.length == 2) parts(1).trim else text

      // Draw colored left panel for number
      cs.setNonStrokingColor(primaryColor)
      cs.addRect(margin, boxY, 35, boxHeight)
      cs.fill()

      // Draw main section background
      cs.setNonStrokingColor(secondaryColor)
      cs.addRect(margin + 35, boxY, pageWidth - 2 * margin - 35, boxHeight)
      cs.fill()

      // Draw bottom border
      cs.setStrokingColor(borderColor)
      cs.setLineWidth(1.5f)
      cs.moveTo(margin, boxY)
      cs.lineTo(pageWidth - margin, boxY)
      cs.stroke()

      // Draw number in white
      cs.beginText()
      cs.setNonStrokingColor(Color.WHITE)
      cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14f)
      cs.newLineAtOffset(margin + 12, boxY + 9)
      cs.showText(number)
      cs.endText()

      // Draw section title
      cs.beginText()
      cs.setNonStrokingColor(primaryDark)
      cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12f)
      cs.newLineAtOffset(margin + 45, boxY + 10)
      cs.showText(title)
      cs.endText()

      boxY
    }

    private def drawPartyHeader(
        cs: PDPageContentStream,
        text: String,
        y: Float,
        pageWidth: Float,
        margin: Float,
      ): Float = {
      val boxHeight = 20f
      val boxY = y - boxHeight

      // Draw colored accent box
      val color = if (text.contains("beruvchi")) primaryColor else accentColor
      cs.setNonStrokingColor(color)
      cs.addRect(margin + 10, boxY, 4, boxHeight)
      cs.fill()

      // Draw text with icon-like prefix
      cs.beginText()
      cs.setNonStrokingColor(textColor)
      cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10f)
      cs.newLineAtOffset(margin + 20, boxY + 6)
      cs.showText("• " + text)
      cs.endText()

      boxY
    }

    private def drawDetailRow(
        cs: PDPageContentStream,
        text: String,
        y: Float,
        pageWidth: Float,
        margin: Float,
      ): Float = {
      val parts = text.split(":", 2)
      if (parts.length == 2) {
        val key = parts(0).trim
        val value = parts(1).trim

        // Draw subtle row background
        cs.setNonStrokingColor(new Color(252, 252, 253))
        cs.addRect(margin + 15, y - 14, pageWidth - 2 * margin - 30, 16)
        cs.fill()

        // Draw key
        cs.beginText()
        cs.setNonStrokingColor(textMuted)
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 9f)
        cs.newLineAtOffset(margin + 25, y - 2)
        cs.showText(key + ":")
        cs.endText()

        // Draw value
        cs.beginText()
        cs.setNonStrokingColor(textColor)
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9f)
        cs.newLineAtOffset(margin + 100, y - 2)
        cs.showText(value)
        cs.endText()
      }
      y - 18
    }

    private def drawPriceHighlight(
        cs: PDPageContentStream,
        text: String,
        y: Float,
        pageWidth: Float,
        margin: Float,
      ): Float = {
      val parts = text.split(":", 2)
      if (parts.length == 2) {
        val value = parts(1).trim
        val boxHeight = 30f
        val boxY = y - boxHeight

        // Draw highlighted background
        cs.setNonStrokingColor(new Color(254, 249, 195)) // Light yellow
        cs.addRect(margin + 15, boxY, pageWidth - 2 * margin - 30, boxHeight)
        cs.fill()

        // Draw left accent
        cs.setNonStrokingColor(warningColor)
        cs.addRect(margin + 15, boxY, 4, boxHeight)
        cs.fill()

        // Draw label
        cs.beginText()
        cs.setNonStrokingColor(textMuted)
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 9f)
        cs.newLineAtOffset(margin + 30, boxY + 16)
        cs.showText("Oylik to'lov:")
        cs.endText()

        // Draw price in large text
        cs.beginText()
        cs.setNonStrokingColor(warningColor)
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14f)
        cs.newLineAtOffset(margin + 120, boxY + 11)
        cs.showText(value)
        cs.endText()

        boxY
      } else {
        y - 15
      }
    }

    private def drawNormalText(
        cs: PDPageContentStream,
        text: String,
        y: Float,
        pageWidth: Float,
        margin: Float,
      ): Float = {
      cs.beginText()
      cs.setNonStrokingColor(textColor)
      cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10f)
      cs.newLineAtOffset(margin + 20, y)
      cs.showText(text)
      cs.endText()
      y - 14
    }

    private def drawSignatureBoxes(
        cs: PDPageContentStream,
        y: Float,
        pageWidth: Float,
        margin: Float,
      ): Float = {
      val boxWidth = 180f
      val boxHeight = 60f
      val boxY = y - boxHeight - 10

      // Left box (Ijara beruvchi)
      val leftBoxX = margin + 20
      drawSingleSignatureBox(cs, leftBoxX, boxY, boxWidth, boxHeight, "Ijara beruvchi", primaryColor)

      // Right box (Ijara oluvchi)
      val rightBoxX = pageWidth - margin - boxWidth - 20
      drawSingleSignatureBox(cs, rightBoxX, boxY, boxWidth, boxHeight, "Ijara oluvchi", accentColor)

      boxY
    }

    private def drawSingleSignatureBox(
        cs: PDPageContentStream,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        label: String,
        color: Color,
      ): Unit = {
      // Draw signature box background
      cs.setNonStrokingColor(secondaryColor)
      cs.addRect(x, y, width, height)
      cs.fill()

      // Draw border
      cs.setStrokingColor(borderColor)
      cs.setLineWidth(1f)
      cs.addRect(x, y, width, height)
      cs.stroke()

      // Draw colored top bar
      cs.setNonStrokingColor(color)
      cs.addRect(x, y + height - 4, width, 4)
      cs.fill()

      // Draw label
      cs.beginText()
      cs.setNonStrokingColor(textMuted)
      cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 8f)
      cs.newLineAtOffset(x + 10, y + height - 15)
      cs.showText(label)
      cs.endText()

      // Draw signature line
      cs.setStrokingColor(textMuted)
      cs.setLineWidth(0.8f)
      cs.moveTo(x + 10, y + 15)
      cs.lineTo(x + width - 10, y + 15)
      cs.stroke()

      // Draw "Imzo / Signature" label
      cs.beginText()
      cs.setNonStrokingColor(textMuted)
      cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 6f)
      cs.newLineAtOffset(x + 10, y + 6)
      cs.showText("Imzo / Signature")
      cs.endText()
    }
  }
}

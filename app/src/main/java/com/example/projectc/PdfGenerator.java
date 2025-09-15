package com.example.projectc;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class PdfGenerator {

    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN = 40;
    private static final int HEADER_HEIGHT = 100;
    private static final int FOOTER_HEIGHT = 50;
    private static final int BASE_ROW_HEIGHT = 30;
    private static final int LABEL_WIDTH = 180;
    private static final int VALUE_WIDTH = 340;
    private static final int CELL_PADDING = 10;
    private static final int LINE_HEIGHT = 14;
    private static final int WATERMARK_SIZE = 400;

    public static void generatePdf(Context context, Complaint complaint) {

        // 1. First check if storage is available
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(context, "Storage not available", Toast.LENGTH_SHORT).show();
            return; // Exit if no storage
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Only do this check for older Android versions
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                Toast.makeText(context, "Storage not available", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // Paint setup
        Paint headerPaint = new Paint();
        headerPaint.setColor(Color.rgb(33, 150, 243));
        headerPaint.setStyle(Paint.Style.FILL);

        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.WHITE);
        titlePaint.setTextSize(24);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setTextAlign(Paint.Align.CENTER);

        Paint collegePaint = new Paint();
        collegePaint.setColor(Color.WHITE);
        collegePaint.setTextSize(16);
        collegePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        collegePaint.setTextAlign(Paint.Align.CENTER);

        Paint subtitlePaint = new Paint();
        subtitlePaint.setColor(Color.WHITE);
        subtitlePaint.setTextSize(14);
        subtitlePaint.setTextAlign(Paint.Align.CENTER);

        Paint labelPaint = new Paint();
        labelPaint.setColor(Color.BLACK);
        labelPaint.setTextSize(12);
        labelPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint valuePaint = new Paint();
        valuePaint.setColor(Color.BLACK);
        valuePaint.setTextSize(12);

        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.rgb(200, 200, 200));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(0.5f);

        Paint headerRowPaint = new Paint();
        headerRowPaint.setColor(Color.argb(150, 207, 228, 250));
        headerRowPaint.setStyle(Paint.Style.FILL);

        Paint oddRowPaint = new Paint();
        oddRowPaint.setColor(Color.argb(100, 255, 255, 255));
        oddRowPaint.setStyle(Paint.Style.FILL);

        Paint evenRowPaint = new Paint();
        evenRowPaint.setColor(Color.argb(100, 245, 245, 245));
        evenRowPaint.setStyle(Paint.Style.FILL);

        // Watermark
        drawWatermark(canvas, context);

        // Header
        canvas.drawRect(0, 0, PAGE_WIDTH, HEADER_HEIGHT, headerPaint);

        // Draw top-left logo
        try {
            Bitmap logo = BitmapFactory.decodeResource(context.getResources(), R.drawable.gceklogo);
            float logoHeight = 60f;
            float logoWidth = (logo.getWidth() * logoHeight) / logo.getHeight();
            Bitmap scaledLogo = Bitmap.createScaledBitmap(logo, (int) logoWidth, (int) logoHeight, true);
            canvas.drawBitmap(scaledLogo, MARGIN, (HEADER_HEIGHT - logoHeight) / 2, null);
        } catch (Exception e) {
            Log.e("PdfGenerator", "Logo draw error", e);
        }

        // Header text (centered)
        float centerX = PAGE_WIDTH / 2f;
        canvas.drawText("Government College of Engineering, Karad", centerX, HEADER_HEIGHT / 2f - 10, collegePaint);
        canvas.drawText("Complaint Details", centerX, HEADER_HEIGHT / 2f + 15, titlePaint);
        canvas.drawText("GCEK Electrifix App", centerX, HEADER_HEIGHT / 2f + 35, subtitlePaint);

        // Complaint data
        Map<String, String> complaintData = new LinkedHashMap<>();
        complaintData.put("Complaint ID", complaint.getId());
        complaintData.put("Description", complaint.getDescription());
        complaintData.put("Status", complaint.getStatus());
        complaintData.put("Date", new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(complaint.getDate()));
        complaintData.put("Type", complaint.getType());
        complaintData.put("Department", complaint.getDepartment());
        complaintData.put("Contact Person", complaint.getContactPerson());
        complaintData.put("Email", complaint.getEmail());
        complaintData.put("Phone", complaint.getPhone());
        complaintData.put("Priority", complaint.getPriority());
        complaintData.put("Location", complaint.getLocation());
        complaintData.put("Remarks", complaint.getRemarks());

        int currentY = HEADER_HEIGHT + 30;

        // Table Header
        canvas.drawRect(MARGIN, currentY, MARGIN + LABEL_WIDTH, currentY + BASE_ROW_HEIGHT, headerRowPaint);
        canvas.drawRect(MARGIN + LABEL_WIDTH, currentY, MARGIN + LABEL_WIDTH + VALUE_WIDTH, currentY + BASE_ROW_HEIGHT, headerRowPaint);
        canvas.drawText("Field", MARGIN + CELL_PADDING, currentY + BASE_ROW_HEIGHT - 10, labelPaint);
        canvas.drawText("Value", MARGIN + LABEL_WIDTH + CELL_PADDING, currentY + BASE_ROW_HEIGHT - 10, labelPaint);
        currentY += BASE_ROW_HEIGHT;

        // Rows
        int rowCount = 0;
        for (Map.Entry<String, String> entry : complaintData.entrySet()) {
            Paint rowPaint = (rowCount % 2 == 0) ? evenRowPaint : oddRowPaint;
            String label = entry.getKey();
            String value = entry.getValue() != null ? entry.getValue() : "";

            int requiredHeight = BASE_ROW_HEIGHT;
            int extraLines = estimateLineCount(value, valuePaint, VALUE_WIDTH - 2 * CELL_PADDING);
            if (extraLines > 1) requiredHeight += (extraLines - 1) * LINE_HEIGHT;

            canvas.drawRect(MARGIN, currentY, MARGIN + LABEL_WIDTH + VALUE_WIDTH, currentY + requiredHeight, rowPaint);
            canvas.drawRect(MARGIN, currentY, MARGIN + LABEL_WIDTH, currentY + requiredHeight, borderPaint);
            canvas.drawRect(MARGIN + LABEL_WIDTH, currentY, MARGIN + LABEL_WIDTH + VALUE_WIDTH, currentY + requiredHeight, borderPaint);

            canvas.drawText(label, MARGIN + CELL_PADDING, currentY + 18, labelPaint);
            drawMultilineText(canvas, valuePaint, value, MARGIN + LABEL_WIDTH + CELL_PADDING, currentY + 18, VALUE_WIDTH - 2 * CELL_PADDING, LINE_HEIGHT);
            currentY += requiredHeight;
            rowCount++;
        }

        // Footer
        Paint footerPaint = new Paint();
        footerPaint.setColor(Color.DKGRAY);
        footerPaint.setTextSize(10);
        footerPaint.setTextAlign(Paint.Align.CENTER);
        String footerText = "Generated on " + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(System.currentTimeMillis());
        canvas.drawText(footerText, PAGE_WIDTH / 2f, PAGE_HEIGHT - 20, footerPaint);

        document.finishPage(page);

        // Save
        String fileName = "Complaint_" + complaint.getId() + "_" +
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis()) + ".pdf";
        savePdfDocument(context, document, fileName);
    }

    private static void drawWatermark(Canvas canvas, Context context) {
        try {
            Bitmap originalLogo = BitmapFactory.decodeResource(context.getResources(), R.drawable.gceklogo);
            float scale = Math.min((float) WATERMARK_SIZE / originalLogo.getWidth(), (float) WATERMARK_SIZE / originalLogo.getHeight());
            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);

            Bitmap scaledLogo = Bitmap.createBitmap(originalLogo, 0, 0, originalLogo.getWidth(), originalLogo.getHeight(), matrix, true);
            int centerX = (PAGE_WIDTH - scaledLogo.getWidth()) / 2;
            int centerY = (PAGE_HEIGHT - scaledLogo.getHeight()) / 3;

            Paint watermarkPaint = new Paint();
            watermarkPaint.setAlpha(20);
            canvas.drawBitmap(scaledLogo, centerX, centerY, watermarkPaint);
        } catch (Exception e) {
            Log.e("PdfGenerator", "Error drawing watermark", e);
            Paint textWatermarkPaint = new Paint();
            textWatermarkPaint.setColor(Color.LTGRAY);
            textWatermarkPaint.setTextSize(80);
            textWatermarkPaint.setAlpha(15);
            textWatermarkPaint.setTextAlign(Paint.Align.CENTER);
            textWatermarkPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.save();
            canvas.rotate(-45, PAGE_WIDTH / 2f, PAGE_HEIGHT / 2f);
            canvas.drawText("GCEK KARAD", PAGE_WIDTH / 2f, PAGE_HEIGHT / 2f, textWatermarkPaint);
            canvas.restore();
        }
    }

    private static void drawMultilineText(Canvas canvas, Paint paint, String text, int x, int y, int maxWidth, int lineHeight) {
        if (text == null || text.isEmpty()) return;
        String[] lines = text.split("\n");
        for (String line : lines) {
            String[] words = line.split(" ");
            StringBuilder currentLine = new StringBuilder();
            for (String word : words) {
                String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
                if (paint.measureText(testLine) <= maxWidth) {
                    currentLine = new StringBuilder(testLine);
                } else {
                    canvas.drawText(currentLine.toString(), x, y, paint);
                    y += lineHeight;
                    currentLine = new StringBuilder(word);
                }
            }
            if (currentLine.length() > 0) {
                canvas.drawText(currentLine.toString(), x, y, paint);
                y += lineHeight;
            }
        }
    }

    private static int estimateLineCount(String text, Paint paint, int maxWidth) {
        if (text == null || text.isEmpty()) return 1;
        String[] lines = text.split("\n");
        int totalLines = 0;
        for (String line : lines) {
            String[] words = line.split(" ");
            StringBuilder currentLine = new StringBuilder();
            for (String word : words) {
                String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
                if (paint.measureText(testLine) <= maxWidth) {
                    currentLine = new StringBuilder(testLine);
                } else {
                    totalLines++;
                    currentLine = new StringBuilder(word);
                }
            }
            if (currentLine.length() > 0) totalLines++;
        }
        return Math.max(totalLines, 1);
    }

    private static void savePdfDocument(Context context, PdfDocument document, String fileName) {
        OutputStream outputStream = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore API for Android 10+
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                values.put(MediaStore.Downloads.RELATIVE_PATH, "Download/");

                Uri uri = context.getContentResolver().insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

                if (uri != null) {
                    outputStream = context.getContentResolver().openOutputStream(uri);
                }
            } else {
                // For older versions, use direct file access
                File downloadsDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs();
                }

                File file = new File(downloadsDir, fileName);
                outputStream = new FileOutputStream(file);
            }

            if (outputStream != null) {
                document.writeTo(outputStream);
                Toast.makeText(context, "PDF saved to Downloads: " + fileName,
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, "Failed to create PDF file",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("PdfGenerator", "Error saving PDF", e);
            Toast.makeText(context, "Error: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            document.close();
        }
    }
}

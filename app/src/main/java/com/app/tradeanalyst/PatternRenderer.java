package com.tradeanalyst.app;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import java.util.List;

/**
 * UTILITY ENGINE: PatternRenderer
 * Handles standard drawing operations onto the Canvas using translated coordinate pairs.
 */
public class PatternRenderer {

    /**
     * Renders a given PatternDrawingModel onto the screen.
     * Draws solid connections, pivot circles, dynamic price target indicators, and custom labels.
     * Supports exact snapped boundaries, necklines, breakouts, retests, and state colors.
     *
     * @param canvas The target drawing canvas.
     * @param pattern The coordinate model holding translated pixel coordinates.
     */
    public static void drawPattern(Canvas canvas, PatternDrawingModel pattern) {
        if (canvas == null || pattern == null) {
            return;
        }

        List<PatternDrawingModel.CanvasPoint> points = pattern.getPoints();
        if (points == null || points.isEmpty()) {
            return;
        }

        // 1. Determine Accent Color based on Phase 3 and Phase 6 Pattern Visual States
        int accentColor = Color.parseColor("#F59E0B"); // Default Yellow (STATE_FORMING)
        String state = pattern.getState() != null ? pattern.getState().toUpperCase() : "STATE_FORMING";
        
        if ("STATE_CONFIRMED".equals(state)) {
            accentColor = Color.parseColor("#10B981"); // Confirmed Green
        } else if ("STATE_RETESTING".equals(state)) {
            accentColor = Color.parseColor("#3B82F6"); // Retesting Blue
        } else if ("STATE_INVALIDATED".equals(state)) {
            accentColor = Color.parseColor("#EF4444"); // Invalidated Red
        } else if ("STATE_COMPLETED".equals(state)) {
            accentColor = Color.parseColor("#8B5CF6"); // Completed Purple
        }

        // 2. Configure Paints
        Paint vectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        vectorPaint.setColor(accentColor);
        vectorPaint.setStrokeWidth(5f);
        vectorPaint.setStyle(Paint.Style.STROKE);
        vectorPaint.setStrokeJoin(Paint.Join.ROUND);
        vectorPaint.setStrokeCap(Paint.Cap.ROUND);

        Paint nodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        nodePaint.setColor(accentColor);
        nodePaint.setStyle(Paint.Style.FILL);

        Paint innerNodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerNodePaint.setColor(Color.WHITE);
        innerNodePaint.setStyle(Paint.Style.FILL);

        Paint targetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        targetPaint.setColor(Color.parseColor("#10B981")); // Target Green
        targetPaint.setStrokeWidth(3f);
        targetPaint.setStyle(Paint.Style.STROKE);
        targetPaint.setPathEffect(new DashPathEffect(new float[]{15f, 15f}, 0));

        Paint stopLossPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        stopLossPaint.setColor(Color.parseColor("#EF4444")); // Stop Loss Red
        stopLossPaint.setStrokeWidth(3f);
        stopLossPaint.setStyle(Paint.Style.STROKE);
        stopLossPaint.setPathEffect(new DashPathEffect(new float[]{15f, 15f}, 0));

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(22f);
        textPaint.setTypeface(android.graphics.Typeface.MONOSPACE);

        Paint labelBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelBgPaint.setColor(Color.parseColor("#CC0D1612")); // Transparent background card
        labelBgPaint.setStyle(Paint.Style.FILL);

        // 3. Render Trajectory Projection Area (Shaded corridor pointing to future target)
        if (pattern.getProjectionStartX() > 0 && pattern.getProjectionEndX() > 0 && pattern.getProjectionTargetY() > 0) {
            Paint projPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            projPaint.setColor(Color.parseColor("#1A8B5CF6")); // Transparent Purple
            projPaint.setStyle(Paint.Style.FILL);

            float lastPointY = points.get(points.size() - 1).getY();

            Path projPath = new Path();
            projPath.moveTo(pattern.getProjectionStartX(), lastPointY);
            projPath.lineTo(pattern.getProjectionEndX(), pattern.getProjectionTargetY());
            projPath.lineTo(pattern.getProjectionEndX(), pattern.getProjectionTargetY() + 40); // projected variance height
            projPath.lineTo(pattern.getProjectionStartX(), lastPointY + 40);
            projPath.close();

            canvas.drawPath(projPath, projPaint);
        }

        // 4. Render Retest Zone (Shaded horizontal overlay indicating retest bounds)
        if (pattern.getRetestZoneTopY() > 0 && pattern.getRetestZoneBottomY() > 0) {
            Paint retestPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            retestPaint.setColor(Color.parseColor("#1E3B82F6")); // Transparent Blue
            retestPaint.setStyle(Paint.Style.FILL);

            canvas.drawRect(0, pattern.getRetestZoneTopY(), canvas.getWidth(), pattern.getRetestZoneBottomY(), retestPaint);

            Paint retestBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
            retestBorder.setColor(Color.parseColor("#3B82F6"));
            retestBorder.setStrokeWidth(2f);
            retestBorder.setStyle(Paint.Style.STROKE);
            retestBorder.setPathEffect(new DashPathEffect(new float[]{8f, 8f}, 0));

            canvas.drawLine(0, pattern.getRetestZoneTopY(), canvas.getWidth(), pattern.getRetestZoneTopY(), retestBorder);
            canvas.drawLine(0, pattern.getRetestZoneBottomY(), canvas.getWidth(), pattern.getRetestZoneBottomY(), retestBorder);
        }

        // 5. Draw Solid Geometric Paths connecting pivot nodes
        if (points.size() > 1) {
            Path path = new Path();
            path.moveTo(points.get(0).getX(), points.get(0).getY());
            for (int i = 1; i < points.size(); i++) {
                path.lineTo(points.get(i).getX(), points.get(i).getY());
            }
            canvas.drawPath(path, vectorPaint);
        }

        // 6. Draw Neckline (Dashed lines connecting shoulder/bottom troughs)
        List<PatternDrawingModel.CanvasPoint> neckline = pattern.getNecklinePoints();
        if (neckline != null && neckline.size() > 1) {
            Paint necklinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            necklinePaint.setColor(accentColor);
            necklinePaint.setStrokeWidth(4f);
            necklinePaint.setStyle(Paint.Style.STROKE);
            necklinePaint.setPathEffect(new DashPathEffect(new float[]{12f, 12f}, 0));

            Path neckPath = new Path();
            neckPath.moveTo(neckline.get(0).getX(), neckline.get(0).getY());
            for (int i = 1; i < neckline.size(); i++) {
                neckPath.lineTo(neckline.get(i).getX(), neckline.get(i).getY());
            }
            canvas.drawPath(neckPath, necklinePaint);
        }

        // 7. Draw node highlights at each pivot point
        for (PatternDrawingModel.CanvasPoint pt : points) {
            canvas.drawCircle(pt.getX(), pt.getY(), 12f, nodePaint);
            canvas.drawCircle(pt.getX(), pt.getY(), 6f, innerNodePaint);
        }

        // 8. Draw Breakout Confirmation Indicator (Circular beacon)
        if (pattern.getBreakoutX() > 0 && pattern.getBreakoutY() > 0) {
            Paint breakoutPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            breakoutPaint.setColor(Color.parseColor("#10B981")); // Breakthrough Green
            breakoutPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(pattern.getBreakoutX(), pattern.getBreakoutY(), 10f, breakoutPaint);

            Paint breakoutLine = new Paint(Paint.ANTI_ALIAS_FLAG);
            breakoutLine.setColor(Color.parseColor("#10B981"));
            breakoutLine.setStrokeWidth(3f);
            breakoutLine.setStyle(Paint.Style.STROKE);
            canvas.drawLine(pattern.getBreakoutX(), pattern.getBreakoutY() - 30, pattern.getBreakoutX(), pattern.getBreakoutY() + 30, breakoutLine);
        }

        // 9. Draw dynamic price target dashed lines
        if (pattern.getTargetY() > 0) {
            canvas.drawLine(0, pattern.getTargetY(), canvas.getWidth(), pattern.getTargetY(), targetPaint);
            String targetText = String.format("🎯 TARGET: $%,.2f", pattern.getTargetPrice());
            
            // Draw background pill card for text
            float textWidth = textPaint.measureText(targetText);
            canvas.drawRect(20, pattern.getTargetY() - 32, 30 + textWidth, pattern.getTargetY() - 4, labelBgPaint);
            canvas.drawText(targetText, 25, pattern.getTargetY() - 10, textPaint);
        }

        // 10. Draw dynamic stop loss dashed lines
        if (pattern.getStopLossY() > 0) {
            canvas.drawLine(0, pattern.getStopLossY(), canvas.getWidth(), pattern.getStopLossY(), stopLossPaint);
            String slText = String.format("🛡️ STOP LOSS: $%,.2f", pattern.getStopLossPrice());
            
            // Draw background pill card for text
            float textWidth = textPaint.measureText(slText);
            canvas.drawRect(20, pattern.getStopLossY() - 32, 30 + textWidth, pattern.getStopLossY() - 4, labelBgPaint);
            canvas.drawText(slText, 25, pattern.getStopLossY() - 10, textPaint);
        }

        // 11. Render contextual pattern label and state above boundaries
        float sumX = 0;
        float minY = Float.MAX_VALUE;
        for (PatternDrawingModel.CanvasPoint pt : points) {
            sumX += pt.getX();
            if (pt.getY() < minY) {
                minY = pt.getY();
            }
        }
        float centerX = sumX / points.size();
        float labelY = minY - 45; // Render 45px padding above the highest node

        // Format containing Pattern State cleanly
        String cleanStateText = state.replace("STATE_", "");
        String detailsText = String.format("%s (%.0f%%) [%s]", pattern.getType().toUpperCase(), pattern.getConfidence(), cleanStateText);
        float detailsWidth = textPaint.measureText(detailsText);
        
        // Safety bound constraints to keep text on-screen
        float textLeft = Math.max(10, centerX - (detailsWidth / 2f));
        if (textLeft + detailsWidth > canvas.getWidth()) {
            textLeft = canvas.getWidth() - detailsWidth - 10;
        }

        canvas.drawRect(textLeft - 8, labelY - 24, textLeft + detailsWidth + 8, labelY + 6, labelBgPaint);
        canvas.drawText(detailsText, textLeft, labelY, textPaint);
    }
}
package com.example.vitasegura;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

public class SaludMarkerView extends MarkerView {
    private final TextView tvContent;
    private final RelativeLayout rootLayout;
    private final String unidad;
    private int currentAlpha = 255;
    private final GradientDrawable bgDrawable;

    public SaludMarkerView(Context context, int layoutResource, String unidad) {
        super(context, layoutResource);
        tvContent = findViewById(R.id.tvContent);
        rootLayout = findViewById(R.id.marker_root);
        this.unidad = unidad;

        // Creamos el fondo gris oscuro manualmente para controlar su transparencia
        bgDrawable = new GradientDrawable();
        bgDrawable.setColor(Color.DKGRAY);
        bgDrawable.setCornerRadius(8f); // Bordes redondeados para que se vea mejor
        if (rootLayout != null) {
            rootLayout.setBackground(bgDrawable);
        }
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        tvContent.setText(String.format("%.1f %s", e.getY(), unidad));

        // Aplicar transparencia al texto
        tvContent.setTextColor(Color.argb(currentAlpha, 255, 255, 255));

        // Aplicar transparencia al fondo (cuadradito)
        if (bgDrawable != null) {
            bgDrawable.setAlpha(currentAlpha);
        }

        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2f), -getHeight());
    }

    public void setCustomAlpha(int alpha) {
        this.currentAlpha = alpha;
    }
}
package org.cao.frontend.renderer;

import java.awt.*;

public enum RowErrorBackgroundColor {
    ERROR(new Color(255, 158, 148)),
    WARNING(new Color(255, 255, 157));

    private Color color;

    RowErrorBackgroundColor(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }
}

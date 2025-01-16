package toniarts.openkeeper.utils;

public final class Color {
    private int red;
    private int green;
    private int blue;
    private int alpha;

    public Color(int red, int green, int blue) {
        this(red, green, blue, 255);
    }

    public Color(int red, int green, int blue, int alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

	public Color(int rgba, boolean hasalpha) {
		red = (rgba >> 16) & 0xFF;
		green = (rgba >> 8) & 0xFF;
		blue = (rgba >> 0) & 0xFF;
		alpha = 0xFF;
		if (hasalpha)
			alpha = (rgba >> 24) & 0xFF;
	}

	public int getRed() {
        return red;
    }

    public int getGreen() {
        return green;
    }

    public int getBlue() {
        return blue;
    }

    public int getAlpha() {
        return alpha;
    }
}
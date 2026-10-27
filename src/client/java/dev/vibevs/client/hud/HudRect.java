package dev.vibevs.client.hud;

/** Screen-space rectangle of a HUD element, filled in by the element while it renders. */
public record HudRect(int x, int y, int width, int height) {
	public boolean contains(double mouseX, double mouseY) {
		return mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y && mouseY < this.y + this.height;
	}
}

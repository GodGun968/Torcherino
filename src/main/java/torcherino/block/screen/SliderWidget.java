package torcherino.block.screen;

import com.mojang.blaze3d.platform.GlStateManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public abstract class SliderWidget extends ButtonWidget
{
	double progress;

	SliderWidget(int x, int y, int width, int height, double progress)
	{
		super(x, y, width, height, "");
		this.setProgress(progress);
		this.onProgressChanged();
	}

	protected abstract void onProgressChanged();

	protected int getTextureId(boolean boolean_1) { return 0; }
	public final void onPressed(double x, double y) { this.changeProgress(x); }
	private void changeProgress(double x) { this.setProgress((x - this.x - 4) / (double)(width - 8)); }
	public void onReleased(double x, double y) { super.playPressedSound(MinecraftClient.getInstance().getSoundLoader()); }
	protected void onDragged(double toX, double toY, double fromX, double fromY) { this.changeProgress(toX); }

	protected void drawBackground(MinecraftClient client, int cursorX, int cursorY)
	{
		if(visible)
		{
			client.getTextureManager().bindTexture(WIDGET_TEX);
			GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
			this.drawTexturedRect(x+(int) (progress*(double) (width-8)), y, 0, 66, 4, 20);
			this.drawTexturedRect(x+(int) (progress*(double) (width-8))+4, y, 196, 66, 4, 20);
		}
	}

	public void setProgress(double newProgress)
	{
		double oldProgress = progress;
		progress = MathHelper.clamp(newProgress, 0.0D, 1.0D);
		if (oldProgress != progress) this.onProgressChanged();
	}
}
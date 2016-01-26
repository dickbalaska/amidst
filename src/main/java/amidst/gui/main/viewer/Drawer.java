package amidst.gui.main.viewer;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;

import amidst.ResourceLoader;
import amidst.documentation.AmidstThread;
import amidst.documentation.CalledOnlyBy;
import amidst.documentation.NotThreadSafe;
import amidst.fragment.Fragment;
import amidst.fragment.FragmentGraph;
import amidst.fragment.FragmentGraphItem;
import amidst.fragment.drawer.FragmentDrawer;
import amidst.fragment.drawer.Graphics2DAccelerationCounter;
import amidst.gui.main.viewer.widget.Widget;
import amidst.mojangapi.world.Dimension;
import amidst.mojangapi.world.coordinates.Resolution;
import amidst.settings.Setting;

@NotThreadSafe
public class Drawer {
	private static final BufferedImage DROP_SHADOW_BOTTOM_LEFT = ResourceLoader
			.getImage("/amidst/gui/main/dropshadow/inner_bottom_left.png");
	private static final BufferedImage DROP_SHADOW_BOTTOM_RIGHT = ResourceLoader
			.getImage("/amidst/gui/main/dropshadow/inner_bottom_right.png");
	private static final BufferedImage DROP_SHADOW_TOP_LEFT = ResourceLoader
			.getImage("/amidst/gui/main/dropshadow/inner_top_left.png");
	private static final BufferedImage DROP_SHADOW_TOP_RIGHT = ResourceLoader
			.getImage("/amidst/gui/main/dropshadow/inner_top_right.png");
	private static final BufferedImage DROP_SHADOW_BOTTOM = ResourceLoader
			.getImage("/amidst/gui/main/dropshadow/inner_bottom.png");
	private static final BufferedImage DROP_SHADOW_TOP = ResourceLoader
			.getImage("/amidst/gui/main/dropshadow/inner_top.png");
	private static final BufferedImage DROP_SHADOW_LEFT = ResourceLoader
			.getImage("/amidst/gui/main/dropshadow/inner_left.png");
	private static final BufferedImage DROP_SHADOW_RIGHT = ResourceLoader
			.getImage("/amidst/gui/main/dropshadow/inner_right.png");
	private static final BufferedImage VOID_TEXTURE = ResourceLoader
			.getImage("/amidst/gui/main/void.png");

	private final AffineTransform originalLayerMatrix = new AffineTransform();
	private final AffineTransform layerMatrix = new AffineTransform();

	private final FragmentGraph graph;
	private final FragmentGraphToScreenTranslator translator;
	private final Zoom zoom;
	private final Movement movement;
	private final List<Widget> widgets;
	private final Iterable<FragmentDrawer> drawers;
	private final Setting<Dimension> dimensionSetting;
	private final TexturePaint voidTexturePaint;

	private Graphics2D g2d;
	private Graphics2DAccelerationCounter accelerationCounter = new Graphics2DAccelerationCounter();
	private int viewerWidth;
	private int viewerHeight;
	private Point mousePosition;
	private FontMetrics widgetFontMetrics;

	private long lastTime = System.currentTimeMillis();
	private float time;
	private float gfxAccelerationRatio = 0;

	@CalledOnlyBy(AmidstThread.EDT)
	public Drawer(FragmentGraph graph,
			FragmentGraphToScreenTranslator translator, Zoom zoom,
			Movement movement, List<Widget> widgets,
			Iterable<FragmentDrawer> drawers,
			Setting<Dimension> dimensionSetting) {
		this.graph = graph;
		this.translator = translator;
		this.zoom = zoom;
		this.movement = movement;
		this.widgets = widgets;
		this.drawers = drawers;
		this.dimensionSetting = dimensionSetting;
		this.voidTexturePaint = new TexturePaint(VOID_TEXTURE, new Rectangle(0,
				0, VOID_TEXTURE.getWidth(), VOID_TEXTURE.getHeight()));
	}

	@CalledOnlyBy(AmidstThread.EDT)
	public void drawCaptureImage(Graphics2D g2d, int viewerWidth,
			int viewerHeight, Point mousePosition, FontMetrics widgetFontMetrics) {
		this.g2d = g2d;
		this.viewerWidth = viewerWidth;
		this.viewerHeight = viewerHeight;
		this.mousePosition = mousePosition;
		this.widgetFontMetrics = widgetFontMetrics;
		this.time = 0;
		this.gfxAccelerationRatio = 0;
		updateTranslator();
		clear();
		drawFragments();
		drawWidgets();
	}

	@CalledOnlyBy(AmidstThread.EDT)
	public void draw(Graphics2D g2d, int viewerWidth, int viewerHeight,
			Point mousePosition, FontMetrics widgetFontMetrics) {
		this.g2d = g2d;
		this.viewerWidth = viewerWidth;
		this.viewerHeight = viewerHeight;
		this.mousePosition = mousePosition;
		this.widgetFontMetrics = widgetFontMetrics;
		this.time = calculateTimeSpanSinceLastDrawInSeconds();
		updateGraphicsAccelerationRatio();
		updateZoom();
		updateMovement();
		updateTranslator();
		clear();
		drawFragments();
		drawBorder();
		drawWidgets();
	}

	@CalledOnlyBy(AmidstThread.EDT)
	private float calculateTimeSpanSinceLastDrawInSeconds() {
		long currentTime = System.currentTimeMillis();
		float result = Math.min(Math.max(0, currentTime - lastTime), 100) / 1000.0f;
		lastTime = currentTime;
		return result;
	}
	
	/**
	 * Updates the value of this.gfxAccelerationRatio if enough statistics
	 * have been collected.
	 */
	@CalledOnlyBy(AmidstThread.EDT)		
	private void updateGraphicsAccelerationRatio() {
		
		if (accelerationCounter.getOperationCount() > 500) {
			this.gfxAccelerationRatio = accelerationCounter.AcceleratedRatio();
			accelerationCounter.Clear();
		}
	}

	@CalledOnlyBy(AmidstThread.EDT)
	private void updateZoom() {
		zoom.update(translator);
	}

	@CalledOnlyBy(AmidstThread.EDT)
	private void updateMovement() {
		movement.update(translator, mousePosition);
	}

	@CalledOnlyBy(AmidstThread.EDT)
	private void updateTranslator() {
		translator.update(viewerWidth, viewerHeight);
	}

	@CalledOnlyBy(AmidstThread.EDT)
	private void clear() {
		g2d.setColor(Color.black);
		g2d.fillRect(0, 0, viewerWidth, viewerHeight);
	}

	@CalledOnlyBy(AmidstThread.EDT)
	private void drawFragments() {
		// TODO: is this needed?
		Graphics2D old = g2d;
		g2d = (Graphics2D) g2d.create();
		doDrawFragments();
		g2d = old;
	}

	@CalledOnlyBy(AmidstThread.EDT)
	private void doDrawFragments() {
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		AffineTransform originalGraphicsTransform = g2d.getTransform();
		initOriginalLayerMatrix(originalGraphicsTransform);
		drawVoidTexture();
		drawLayers();
		g2d.setTransform(originalGraphicsTransform);
	}

	@CalledOnlyBy(AmidstThread.EDT)
	private void initOriginalLayerMatrix(
			AffineTransform originalGraphicsTransform) {
		double scale = zoom.getCurrentValue();
		originalLayerMatrix.setTransform(originalGraphicsTransform);
		originalLayerMatrix.translate(translator.getLeftOnScreen(),
				translator.getTopOnScreen());
		originalLayerMatrix.scale(scale, scale);
	}

	@CalledOnlyBy(AmidstThread.EDT)
	private void drawVoidTexture() {
		if (dimensionSetting.get().equals(Dimension.END)) {
			g2d.setTransform(originalLayerMatrix);
			g2d.scale(4, 4);
			g2d.setPaint(voidTexturePaint);
			int stepsPerFragment = Resolution.QUARTER.getStepsPerFragment();
			g2d.fillRect(0, 0, stepsPerFragment * graph.getFragmentsPerRow(),
					stepsPerFragment * graph.getFragmentsPerColumn());
		}
	}

	@CalledOnlyBy(AmidstThread.EDT)
	private void drawLayers() {
		for (FragmentDrawer drawer : drawers) {
			if (drawer.isEnabled()) {
				initLayerMatrix();
				for (FragmentGraphItem fragmentGraphItem : graph) {
					Fragment fragment = fragmentGraphItem.getFragment();
					if (drawer.isDrawUnloaded()) {
						setAlphaComposite(1.0f);
						g2d.setTransform(layerMatrix);
						drawer.draw(fragment, g2d, time);
					} else if (fragment.isLoaded()) {
						setAlphaComposite(fragment.getAlpha());
						g2d.setTransform(layerMatrix);
						drawer.draw(fragment, g2d, time);
					}
					updateLayerMatrix(fragmentGraphItem,
							graph.getFragmentsPerRow());
				}
				
				// Sweep any AccelerationCounter values from the fragment drawer 
				// into our tally.
				accelerationCounter.AddFrom(drawer.getAccelerationCounter());
				drawer.getAccelerationCounter().Clear();
			}
		}
		setAlphaComposite(1.0f);
	}

	@CalledOnlyBy(AmidstThread.EDT)
	private void initLayerMatrix() {
		layerMatrix.setTransform(originalLayerMatrix);
	}

	@CalledOnlyBy(AmidstThread.EDT)
	private void updateLayerMatrix(FragmentGraphItem fragmentGraphItem,
			int fragmentsPerRow) {
		layerMatrix.translate(Fragment.SIZE, 0);
		if (fragmentGraphItem.isEndOfLine()) {
			layerMatrix.translate(-Fragment.SIZE * fragmentsPerRow,
					Fragment.SIZE);
		}
	}

	@CalledOnlyBy(AmidstThread.EDT)
	private void drawBorder() {
		int width10 = viewerWidth - 10;
		int height10 = viewerHeight - 10;
		int width20 = viewerWidth - 20;
		int height20 = viewerHeight - 20;
		g2d.drawImage(DROP_SHADOW_TOP_LEFT, 0, 0, null);
		g2d.drawImage(DROP_SHADOW_TOP_RIGHT, width10, 0, null);
		g2d.drawImage(DROP_SHADOW_BOTTOM_LEFT, 0, height10, null);
		g2d.drawImage(DROP_SHADOW_BOTTOM_RIGHT, width10, height10, null);
		g2d.drawImage(DROP_SHADOW_TOP, 10, 0, width20, 10, null);
		g2d.drawImage(DROP_SHADOW_BOTTOM, 10, height10, width20, 10, null);
		g2d.drawImage(DROP_SHADOW_LEFT, 0, 10, 10, height20, null);
		g2d.drawImage(DROP_SHADOW_RIGHT, width10, 10, 10, height20, null);
		
		accelerationCounter.LogOperationPerformed(DROP_SHADOW_TOP_LEFT);
		accelerationCounter.LogOperationPerformed(DROP_SHADOW_TOP_RIGHT);
		accelerationCounter.LogOperationPerformed(DROP_SHADOW_BOTTOM_LEFT);
		accelerationCounter.LogOperationPerformed(DROP_SHADOW_BOTTOM_RIGHT);
		accelerationCounter.LogOperationPerformed(DROP_SHADOW_TOP);
		accelerationCounter.LogOperationPerformed(DROP_SHADOW_BOTTOM);
		accelerationCounter.LogOperationPerformed(DROP_SHADOW_LEFT);
		accelerationCounter.LogOperationPerformed(DROP_SHADOW_RIGHT);
	}

	@CalledOnlyBy(AmidstThread.EDT)
	private void drawWidgets() {
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		for (Widget widget : widgets) {
			widget.update(viewerWidth, viewerHeight, mousePosition,
					widgetFontMetrics, time, gfxAccelerationRatio);
			if (widget.isVisible()) {
				setAlphaComposite(widget.getAlpha());
				widget.draw(g2d);
			}
		}
	}

	@CalledOnlyBy(AmidstThread.EDT)
	private void setAlphaComposite(float alpha) {
		g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
				alpha));
	}
}

package builderb0y.bigglobe.settings;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.stream.IntStream;
import javax.swing.*;

import net.minecraft.util.Mth;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.settings.VoronoiDiagram2D.Cell;
import builderb0y.bigglobe.util.Derivative2D;

public class VornoiDisplay {

	public static void main(String[] args) {
		//LinkedArrayList.ASSERTS = true;
		SwingUtilities.invokeLater(() -> new VoronoiFrame().setVisible(true));
	}

	public static class VoronoiFrame extends JFrame {

		public VoronoiDiagram2D diagram;
		public VoronoiPanel panel;

		public VoronoiFrame() {
			super("Voronoi Viewer");
			this.newDiagram();
			this.panel = new VoronoiPanel(this);
			this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			this.setSize(512, 512);
			this.setLocationRelativeTo(null);
			this.getContentPane().setLayout(new BorderLayout());
			this.getContentPane().add(this.panel, BorderLayout.CENTER);
		}

		public void newDiagram() {
			this.diagram = new VoronoiDiagram2D(new Seed(12345L), 256, 192);
		}
	}

	public static class VoronoiPanel extends JPanel {

		public VoronoiFrame frame;
		public BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		public Insets dirtyRegion = new Insets(0, 0, 0, 0);
		public Point origin = new Point();
		public Point clickedLocation = new Point();
		public VoronoiDiagram2D.Cell clickedCell;

		public VoronoiPanel(VoronoiFrame frame) {
			this.frame = frame;
			this.clickedCell = frame.diagram.getCell(-141, -37);
			System.out.println(this.clickedCell);
			this.origin.x = -5540 - (512 >> 1);
			this.origin.y = -3887 - (512 >> 1);
			this.addMouseListener(new MouseAdapter() {

				@Override
				public void mousePressed(MouseEvent e) {
					((VoronoiPanel)(e.getComponent())).clickedLocation.setLocation(e.getX(), e.getY());
				}

				@Override
				public void mouseClicked(MouseEvent e) {
					VoronoiPanel panel = (VoronoiPanel)(e.getComponent());
					panel.clickedCell = panel.frame.diagram.getNearestCell(e.getX() + panel.origin.x, e.getY() + panel.origin.y);
					System.out.println(panel.clickedCell);
					panel.markCompletelyDirty();
				}
			});
			this.addMouseMotionListener(new MouseAdapter() {

				@Override
				public void mouseDragged(MouseEvent e) {
					VoronoiPanel panel = (VoronoiPanel)(e.getComponent());
					int dx = e.getX() - panel.clickedLocation.x;
					int dy = e.getY() - panel.clickedLocation.y;
					panel.clickedLocation.setLocation(e.getX(), e.getY());
					panel.dirtyRegion.top += dy;
					panel.dirtyRegion.bottom -= dy;
					panel.dirtyRegion.left += dx;
					panel.dirtyRegion.right -= dx;
					panel.origin.x -= dx;
					panel.origin.y -= dy;
					Graphics2D graphics = panel.image.createGraphics();
					try {
						graphics.copyArea(0, 0, panel.image.getWidth(), panel.image.getHeight(), dx, dy);
					}
					finally {
						graphics.dispose();
					}
					panel.repaint();
				}

				@Override
				public void mouseMoved(MouseEvent e) {
					VoronoiPanel panel = (VoronoiPanel)(e.getComponent());
					int realX = e.getX() + panel.origin.x;
					int realY = e.getY() + panel.origin.y;
					Cell cell = panel.frame.diagram.getNearestCell(realX, realY);
					Derivative2D derivative = new Derivative2D();
					cell.derivativeProgressToEdgeSquaredD(derivative, realX, realY);
					double mul = 1.0D / Math.sqrt(BigGlobeMath.squareD(derivative.dx, derivative.dy));
					derivative.dx *= mul;
					derivative.dy *= mul;
					Dimension size = Toolkit.getDefaultToolkit().getBestCursorSize(64, 64);
					BufferedImage cursorImage = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
					for (int y = 0; y < size.height; y++) {
						for (int x = 0; x < size.width; x++) {
							double relativeX = (x + 0.5D) / size.width * 2.0D - 1.0D;
							double relativeY = (y + 0.5D) / size.height * 2.0D - 1.0D;
							double r = Math.sqrt(BigGlobeMath.squareD(relativeX, relativeY));
							if (r > 0.875 && r < 1.0) {
								relativeX /= r;
								relativeY /= r;
								double dot = derivative.dx * relativeX + derivative.dy * relativeY;
								cursorImage.setRGB(x, y, dot > 0.99 ? -1 : 0xFF000000);
							}
						}
					}
					panel.setCursor(Toolkit.getDefaultToolkit().createCustomCursor(cursorImage, new Point(size.width >> 1, size.height >> 1), "direction"));
				}
			});
		}

		public void markCompletelyDirty() {
			Insets dirtyRegion = this.dirtyRegion;
			dirtyRegion.top = this.getHeight();
			dirtyRegion.left = dirtyRegion.right = dirtyRegion.bottom = 0;
			this.repaint();
		}

		public void repaintPixel(int x, int y) {
			int realX = x + this.origin.x;
			int realY = y + this.origin.y;
			int color;
			if (realX % this.frame.diagram.distance == 0 || realY % this.frame.diagram.distance == 0) {
				color = -1;
			}
			else {
				VoronoiDiagram2D.Cell cell = this.frame.diagram.getNearestCell(realX, realY);
				Derivative2D derivative = new Derivative2D();
				cell.derivativeProgressToEdgeSquaredD(derivative, realX, realY);
				double mul = 128.0D / Math.sqrt(BigGlobeMath.squareD(derivative.dx, derivative.dy));
				int red   = Mth.clamp((int)(derivative.dx * mul + 128.0D), 0, 255);
				int green = Mth.clamp((int)(derivative.dy * mul + 128.0D), 0, 255);
				int blue  = 0; //Mth.clamp((int)(derivative.value *   256.0D         ), 0, 255);
				color = (red << 16) | (green << 8) | blue;
				/*
				int brightness = Mth.floor(cell.progressToEdgeD(realX, realY) * 255.0D + 0.5D);
				if (cell.center.cellEquals(this.clickedCell.center)) {
					color = brightness << 16;
				}
				else foundAdjacent: {
					for (VoronoiDiagram2D.SeedPoint adjacent : this.clickedCell.adjacent) {
						if (cell.center.cellEquals(adjacent)) {
							color = brightness << 8;
							break foundAdjacent;
						}
					}
					color = brightness;
				}
				*/
				color |= 0xFF000000;
			}
			this.image.setRGB(x, y, color);
		}

		public void repaintArea(int minX, int maxX, int minY, int maxY) {
			final int clampedMinX = Mth.clamp(minX, 0, this.getWidth());
			final int clampedMaxX = Mth.clamp(maxX, 0, this.getWidth());
			final int clampedMinY = Mth.clamp(minY, 0, this.getHeight());
			final int clampedMaxY = Mth.clamp(maxY, 0, this.getHeight());
			if (clampedMaxX > clampedMinX && clampedMaxY > clampedMinY) {
				IntStream.range(clampedMinX, clampedMaxX).parallel().forEach(x -> {
					IntStream.range(clampedMinY, clampedMaxY).parallel().forEach(y -> {
						this.repaintPixel(x, y);
					});
				});
			}
		}

		@Override
		public void paintComponent(Graphics graphics) {
			Insets region = this.dirtyRegion;
			if (this.image.getWidth() != this.getWidth() || this.image.getHeight() != this.getHeight()) {
				this.image = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_ARGB);
				this.repaintArea(0, this.getWidth(), 0, this.getHeight());
			}
			else {
				if (region.top > 0) this.repaintArea(0, this.getWidth(), 0, region.top);
				if (region.bottom > 0) this.repaintArea(0, this.getWidth(), this.getHeight() - region.bottom, this.getHeight());
				if (region.left > 0) this.repaintArea(0, region.left, region.top, this.getHeight() - region.bottom);
				if (region.right > 0) this.repaintArea(this.getWidth() - region.right, this.getWidth(), region.top, this.getHeight() - region.bottom);
			}
			region.top = region.bottom = region.left = region.right = 0;
			graphics.drawImage(this.image, 0, 0, this);
		}
	}
}
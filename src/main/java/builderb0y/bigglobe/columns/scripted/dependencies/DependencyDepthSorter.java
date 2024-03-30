package builderb0y.bigglobe.columns.scripted.dependencies;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.objects.*;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.util.HashStrategies;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.util.UnregisteredObjectException;

public class DependencyDepthSorter {

	public static final Hash.Strategy<RegistryEntry<?>> REGISTRY_ENTRY_STRATEGY = HashStrategies.map(HashStrategies.identityStrategy(), UnregisteredObjectException::getKey);

	public final List<List<RegistryEntry<? extends DependencyView>>> results = new ArrayList<>(16);
	public final Object2IntOpenCustomHashMap<RegistryEntry<? extends DependencyView>> cache = new Object2IntOpenCustomHashMap<>(256, REGISTRY_ENTRY_STRATEGY);
	{ this.cache.defaultReturnValue(-1); }

	public int recursiveComputeDepth(RegistryEntry<? extends DependencyView> entry) {
		int depth = this.cache.getInt(entry);
		if (depth < 0) {
			OptionalInt optional = (
				skipNonColumnEntries(entry.value().streamDirectDependencies())
				.mapToInt(this::recursiveComputeDepth)
				.max()
			);
			depth = optional.isPresent() ? optional.getAsInt() + 1 : 0;
			this.cache.put(entry, depth);
			while (this.results.size() <= depth) {
				this.results.add(new ArrayList<>(16));
			}
			this.results.get(depth).add(entry);
		}
		return depth;
	}

	public static Stream<? extends RegistryEntry<? extends DependencyView>> skipNonColumnEntries(Stream<? extends RegistryEntry<? extends DependencyView>> stream) {
		return stream.flatMap(
			(RegistryEntry<? extends DependencyView> element) -> (
				element.value() instanceof ColumnEntry
				? Stream.of(element)
				: skipNonColumnEntries(element.value().streamDirectDependencies())
			)
		);
	}

	public void outputResults(String suffix) {
		BigGlobeMod.LOGGER.info("Generating column value dependency tree debug files, as requested in your config file...");
		if (this.outputGraphViz(suffix) || Files.notExists(FabricLoader.getInstance().getGameDir().resolve("bigglobe_column_values_" + suffix + ".png"))) {
			try (Graph graph = new Graph(this)) {
				graph.doAllTheThings(suffix);
			}
		}
		else {
			BigGlobeMod.LOGGER.info(".minecraft/bigglobe_column_values_" + suffix + ".gv.txt already exists and contents are up-to-date. Skipping image generation.");
		}
	}

	/** returns true if the file contents changed. */
	public boolean outputGraphViz(String suffix) {
		try {
			Path path = FabricLoader.getInstance().getGameDir().resolve("bigglobe_column_values_" + suffix + ".gv.txt");
			String text = this.getGraphVizText();
			if (Files.exists(path) && Files.readString(path).equals(text)) {
				return false;
			}
			else {
				Files.writeString(path, text, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
				return true;
			}
		}
		catch (IOException exception) {
			BigGlobeMod.LOGGER.warn("Exception generating graphviz file: ", exception);
			return false;
		}
	}

	public String getGraphVizText() {
		StringWriter writer = new StringWriter(65536);
		PrintWriter out = new PrintWriter(writer);
		out.println("digraph bigglobe_column_values {");
		out.println("\trankdir=\"RL\"");
		SubGraph root = new SubGraph();
		Map<RegistryEntry<? extends DependencyView>, SubGraph> lookup = new Object2ObjectOpenCustomHashMap<>(256, REGISTRY_ENTRY_STRATEGY);
		for (RegistryEntry<? extends DependencyView> entry : this.cache.keySet()) {
			String identifier = keyToString(UnregisteredObjectException.getKey(entry));
			SubGraph graph = root;
			String[] split = identifier.split("[/:]");
			for (String part : split) {
				graph = graph.computeIfAbsent(part, $ -> new SubGraph());
			}
			graph.fullName = identifier;
			lookup.put(entry, graph);
		}
		root.printAll(out, null, 0);
		this
		.cache
		.keySet()
		.stream()
		.sorted(Comparator.comparing(UnregisteredObjectException::getID))
		.forEachOrdered((RegistryEntry<? extends DependencyView> entry) -> {
			String dependencyString = (
				skipNonColumnEntries(entry.value().streamDirectDependencies())
				.map(UnregisteredObjectException::getKey)
				.sorted(Comparator.comparing(RegistryKey::getValue))
				.map(DependencyDepthSorter::keyToString)
				.map((String id) -> '"' + id + '"')
				.collect(Collectors.joining(" "))
			);
			if (!dependencyString.isEmpty()) {
				out.println("\t\"" + keyToString(UnregisteredObjectException.getKey(entry)) + "\" -> { " + dependencyString + " }");
			}
		});
		out.println('}');
		return writer.toString();
	}

	public static String keyToString(RegistryKey<?> key) {
		return key.getRegistry() + ":" + key.getValue();
	}

	public static class SubGraph extends HashMap<String, SubGraph> {

		public String fullName;

		public void printAll(PrintWriter stream, String name, int depth) {
			String indentation = "\t".repeat(depth);
			if (this.isEmpty()) {
				stream.println(indentation + '"' + this.fullName + "\" [ label = \"" + name + "\" ]");
			}
			else {
				if (name != null) {
					stream.println(indentation + "subgraph \"" + name + "\" {");
					stream.println(indentation + "\tcluster = true");
					stream.println(indentation + "\tlabel = \"" + name + '"');
				}
				this
				.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByKey())
				.forEachOrdered((Map.Entry<String, SubGraph> entry) ->{
					entry.getValue().printAll(stream, entry.getKey(), depth + 1);
				});
				if (name != null) stream.println(indentation + '}');
			}
		}
	}

	public static class Graph implements AutoCloseable {

		public static final int
			FONT_SIZE = 16,
			RECTANGLE_HEIGHT = 64,
			MARGIN = 32,
			HORIZONTAL_SPACING = 256,
			VERTICAL_SPACING = 32,
			RECTANGLE_CURVE = 16;

		public Map<RegistryEntry<? extends DependencyView>, Cell> cellLookup;
		public List<Column> columns;
		public Font font;
		public FontRenderContext fontRenderContext;
		public BufferedImage image;
		public Graphics2D graphics;
		public int largestColumn;

		public Graph(DependencyDepthSorter sorter) {
			BigGlobeMod.LOGGER.debug("Constructing dependency graph.");
			this.font = new Font(null, Font.PLAIN, FONT_SIZE);
			this.fontRenderContext = new FontRenderContext(new AffineTransform(), true, true);
			this.cellLookup = new Object2ObjectOpenCustomHashMap<>(256, REGISTRY_ENTRY_STRATEGY);
			int columnCount = sorter.results.size();
			this.columns = new ArrayList<>(columnCount);
			int columnPosition = MARGIN;
			for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
				Column column = new Column(this, columnIndex, columnPosition);
				this.columns.add(column);
				int columnWidth = 0;
				for (RegistryEntry<? extends DependencyView> entry : sorter.results.get(columnIndex)) {
					Cell cell = new Cell(column, UnregisteredObjectException.getID(entry).toString(), columnIndex);
					this.cellLookup.put(entry, cell);
					this.addCell(cell);
					columnWidth = Math.max(columnWidth, cell.textBounds.width);
				}
				column.width = columnWidth + RECTANGLE_CURVE;
				columnPosition += column.width + HORIZONTAL_SPACING;
				this.largestColumn = Math.max(this.largestColumn, sorter.results.get(columnIndex).size());
			}
		}

		public void doAllTheThings(String suffix) {
			BigGlobeMod.LOGGER.debug("Linking dependency graph.");
			this.link();
			BigGlobeMod.LOGGER.debug("Filing cells in dependency graph.");
			this.fillCells();
			BigGlobeMod.LOGGER.debug("Organizing dependency graph.");
			this.organize();
			BigGlobeMod.LOGGER.debug("Creating image for dependency graph.");
			this.createImage();
			BigGlobeMod.LOGGER.debug("Saving image for dependency graph.");
			this.saveImage(suffix);
			BigGlobeMod.LOGGER.debug("Done.");
		}

		public void link() {
			for (Map.Entry<RegistryEntry<? extends DependencyView>, Cell> dependant : this.cellLookup.entrySet()) {
				skipNonColumnEntries(dependant.getKey().value().streamDirectDependencies()).forEach((RegistryEntry<? extends DependencyView> dependency) -> {
					Cell left = this.cellLookup.get(dependency);
					dependant.getValue().leftCells.add(left);
					left.rightCells.add(dependant.getValue());
				});
			}
		}

		public void fillCells() {
			for (int columnIndex = 0, collumnCount = this.columns.size(); columnIndex < collumnCount; columnIndex++) {
				Column column = this.columns.get(columnIndex);
				while (column.rows.size() < this.largestColumn) {
					column.addCell(new Cell(column, null, columnIndex));
				}
			}
		}

		public void organize() {
			boolean swappedAny = true;
			while (swappedAny) {
				swappedAny = false;
				for (Column column : this.columns) {
					for (int index1 = 0, limit = column.rows.size() - 1; index1 < limit;) {
						int index2 = index1 + 1;
						Cell cell1 = column.rows.get(index1);
						Cell cell2 = column.rows.get(index2);
						double initialDistance = cell1.sumDistances() + cell2.sumDistances();
						int slotY = cell1.slotY;
						cell1.slotY = cell2.slotY;
						cell2.slotY = slotY;
						double finalDistance = cell1.sumDistances() + cell2.sumDistances();
						if (finalDistance < initialDistance) {
							column.rows.set(index1, cell2);
							column.rows.set(index2, cell1);
							swappedAny = true;
						}
						else {
							cell2.slotY = cell1.slotY;
							cell1.slotY = slotY;
						}
						index1 = index2;
					}
				}
			}
		}

		public void createImage() {
			Column lastColumn = this.columns.get(this.columns.size() - 1);
			int width     = lastColumn.posX + lastColumn.width + MARGIN;
			int height    = this.largestColumn * (RECTANGLE_HEIGHT + VERTICAL_SPACING) - VERTICAL_SPACING + (MARGIN << 1);
			this.image    = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			this.graphics = this.image.createGraphics();
			this.fillImage();
			this.drawLines();
			this.drawCells();
		}

		public void fillImage() {
			int[] pixel = { -1 };
			int width = this.image.getWidth();
			int height = this.image.getHeight();
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					this.image.getRaster().setDataElements(x, y, pixel);
				}
			}
		}

		public void drawLines() {
			for (Column column : this.columns) {
				for (Cell from : column.rows) {
					for (Cell to : from.rightCells) {
						this.drawLine(from, to);
					}
				}
			}
		}

		public void drawLine(Cell from, Cell to) {
			int x0 = from.getRightCenterX();
			int x1 = to.getLeftCenterX();
			int y0 = from.getRightCenterY();
			int y1 = to.getLeftCenterY();
			int c0 = from.color;
			int c1 = to.color;
			int prevY = y0;
			int[] pixel = new int[1];
			for (int x = x0; x < x1; x++) {
				float fraction = Interpolator.unmixLinear(x0, x1, x);
				int currY = (int)(Interpolator.mixSmooth(y0, y1, fraction));
				int stepY = Integer.signum(currY - prevY);
				int color = (
					(((int)(Interpolator.mixLinear((c0 >>> 16) & 255, (c1 >>> 16) & 255, fraction))) << 16) |
					(((int)(Interpolator.mixLinear((c0 >>>  8) & 255, (c1 >>>  8) & 255, fraction))) <<  8) |
					(((int)(Interpolator.mixLinear( c0         & 255,  c1         & 255, fraction)))      ) |
					0xFF000000
				);
				pixel[0] = color;
				if (prevY == currY) {
					this.image.getRaster().setDataElements(x, currY, pixel);
				}
				else for (int y = prevY; y != currY; y += stepY) {
					this.image.getRaster().setDataElements(x, y, pixel);
				}
				prevY = currY;
			}
		}

		public void drawCells() {
			for (Column column : this.columns) {
				for (Cell row : column.rows) {
					row.draw();
				}
			}
		}

		public void saveImage(String suffix) {
			try {
				ImageIO.write(this.image, "png", FabricLoader.getInstance().getGameDir().resolve("bigglobe_column_values_" + suffix + ".png").toFile());
			}
			catch (IOException exception) {
				BigGlobeMod.LOGGER.warn("Exception saving column value dependency chart image: ", exception);
			}
		}

		public void addCell(Cell cell) {
			Column column = this.columns.get(cell.slotX);
			column.addCell(cell);
			this.largestColumn = Math.max(this.largestColumn, column.rows.size());
		}

		@Override
		public void close() {
			if (this.graphics != null) {
				this.graphics.dispose();
				this.graphics = null;
			}
			this.image = null;
		}
	}

	public static class Column {

		public final Graph graph;
		public List<Cell> rows;
		public int slotX, posX, width;

		public Column(Graph graph, int slotX, int posX) {
			this.graph = graph;
			this.slotX = slotX;
			this.posX  = posX;
			this.rows  = new ArrayList<>(16);
		}

		public void addCell(Cell cell) {
			cell.slotY = this.rows.size();
			this.rows.add(cell);
		}
	}

	public static class Cell {

		public final Column column;
		public final String text;
		public final TextLayout layout;
		public final Rectangle textBounds;
		public int slotX, slotY, color;

		public Set<Cell> leftCells, rightCells;

		public Cell(Column column, String text, int depth) {
			this.column = column;
			this.text = text;
			this.layout = text == null ? null : new TextLayout(text, column.graph.font, column.graph.fontRenderContext);
			this.textBounds = this.layout == null ? new Rectangle() : this.layout.getPixelBounds(column.graph.fontRenderContext, 0.0F, 0.0F);
			this.slotX = depth;
			this.color = text == null ? 0 : text.hashCode();
			while ((this.color & 255) + ((this.color >>> 8) & 255) + ((this.color >>> 16) & 255) > 384) {
				this.color = HashCommon.mix(this.color);
			}
			this.color |= 0xFF000000;
			this.leftCells = new HashSet<>(0);
			this.rightCells = new HashSet<>(0);
		}

		public int getRectangleX() {
			return this.column.posX;
		}

		public int getRectangleY() {
			return this.slotY * (Graph.RECTANGLE_HEIGHT + Graph.VERTICAL_SPACING) + Graph.MARGIN;
		}

		public int getRectangleWidth() {
			return this.column.width;
		}

		public int getRectangleHeight() {
			return Graph.RECTANGLE_HEIGHT;
		}

		public int getTextX() {
			int desiredCenter = this.getRectangleX() + (this.getRectangleWidth() >> 1);
			int actualCenter = this.textBounds.x + (this.textBounds.width >> 1);
			return desiredCenter - actualCenter;
		}

		public int getTextY() {
			int desiredCenter = this.getRectangleY() + (this.getRectangleHeight() >> 1);
			int actualCenter = this.textBounds.y + (this.textBounds.height >> 1);
			return desiredCenter - actualCenter;
		}

		public int getLeftCenterX() {
			return this.getRectangleX();
		}

		public int getLeftCenterY() {
			return this.getRectangleY() + (this.getRectangleHeight() >> 1);
		}

		public int getRightCenterX() {
			return this.getRectangleX() + this.getRectangleWidth();
		}

		public int getRightCenterY() {
			return this.getRectangleY() + (this.getRectangleHeight() >> 1);
		}

		public double sumDistances() {
			double distance = 0.0D;
			for (Cell left : this.leftCells) {
				distance += Math.sqrt(BigGlobeMath.squareI(this.getLeftCenterX() - left.getRightCenterX(), this.getLeftCenterY() - left.getRightCenterY()));
			}
			for (Cell right : this.rightCells) {
				distance += Math.sqrt(BigGlobeMath.squareD(right.getLeftCenterX() - this.getRightCenterX(), right.getLeftCenterY() - this.getRightCenterY()));
			}
			return distance;
		}

		public void draw() {
			if (this.layout == null) return;
			Graphics2D graphics = this.column.graph.graphics;
			graphics.setColor(Color.WHITE);
			graphics.fillRoundRect(
				this.getRectangleX(),
				this.getRectangleY(),
				this.getRectangleWidth(),
				this.getRectangleHeight(),
				Graph.RECTANGLE_CURVE,
				Graph.RECTANGLE_CURVE
			);
			graphics.setColor(new Color(this.color));
			graphics.drawRoundRect(
				this.getRectangleX(),
				this.getRectangleY(),
				this.getRectangleWidth(),
				this.getRectangleHeight(),
				Graph.RECTANGLE_CURVE,
				Graph.RECTANGLE_CURVE
			);
			this.layout.draw(graphics, this.getTextX(), this.getTextY());
		}
	}
}
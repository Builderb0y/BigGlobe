import java.io.*;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.StringJoiner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class V6DataPackConverter {

	public static void main(String[] args) throws IOException {
		if (args.length < 1 || args.length > 2) {
			System.err.println(
				"""
				Usage: java V6DataPackConverter.java (input) (output)
				
				where (input) should be replaced with the path to the data pack you want to convert,
				and (output) should be replaced with the path to the output data pack you want to create.
				Note that (output) is optional, and can be omitted. If omitted, the output
				data pack will have the same path as the input data pack, suffixed with _V6.
				If any path contains a space character, it must be surrounded by quotes."""
			);
			return;
		}
		File inFile = new File(args[0]);
		if (!inFile.exists()) {
			System.err.println("Can't find the input file:");
			System.err.println(inFile.getAbsolutePath());
			System.err.println("Please make sure the path is correct.");
			return;
		}
		File outFile;
		if (args.length > 1) {
			outFile = new File(args[1]);
		}
		else got: {
			String path = inFile.getPath();
			for (int index = path.length(); --index >= 0; ) {
				char c = path.charAt(index);
				if (c == '.') {
					outFile = new File(path.substring(0, index) + "_V6" + path.substring(index));
					break got;
				}
				else if (c == File.separatorChar) {
					break;
				}
			}
			outFile = new File(path + "_V6");
		}
		if (outFile.exists()) {
			System.err.println("The output file already exists:");
			System.err.println(outFile.getAbsolutePath());
			return;
		}
		//now actually do the work.
		DataPackSource source = inFile.isDirectory() ? new DirectorySource(inFile) : new ZipSource(inFile);
		try (DataPackSink sink = outFile.getPath().endsWith(".zip") ? new ZipSink(outFile) : new DirectorySink(outFile)) {
			source.forEach(sink);
		}
	}

	public static abstract class DataPackSource {

		public final File root;

		public DataPackSource(File root) {
			this.root = root;
		}

		public abstract void forEach(DataPackSink sink) throws IOException;
	}

	public static class DirectorySource extends DataPackSource {

		public DirectorySource(File root) throws IOException {
			super(root);
			if (!new File(root, "data").isDirectory()) {
				throw new IOException("Can't find 'data' folder inside " + root.getAbsolutePath());
			}
			if (!new File(root, "pack.mcmeta").isFile()) {
				throw new IOException("Can't find 'pack.mcmeta' file inside " + root.getAbsolutePath());
			}
		}

		@Override
		public void forEach(DataPackSink sink) throws IOException {
			this.recursiveForEach(this.root, new String[0], sink);
		}

		public void recursiveForEach(File maybeDirectory, String[] pathSoFar, DataPackSink sink) throws IOException {
			String[] children = maybeDirectory.list();
			if (children != null) {
				String[] nextPath = Arrays.copyOf(pathSoFar, pathSoFar.length + 1);
				for (String child : children) {
					nextPath[pathSoFar.length] = child;
					this.recursiveForEach(new File(maybeDirectory, child), nextPath, sink);
				}
			}
			else if (maybeDirectory.isFile()) {
				try (FileInputStream stream = new FileInputStream(maybeDirectory)) {
					sink.accept(pathSoFar, stream);
				}
			}
			else {
				System.err.println("Ignoring " + maybeDirectory.getAbsolutePath() + " as it is neither a normal file, nor a directory.");
			}
		}
	}

	public static class ZipSource extends DataPackSource {

		public ZipSource(File root) {
			super(root);
		}

		@Override
		public void forEach(DataPackSink sink) throws IOException {
			try (ZipFile zip = new ZipFile(this.root)) {
				Enumeration<? extends ZipEntry> entries = zip.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();
					if (!entry.isDirectory()) {
						String[] path = entry.getName().split("/");
						try (InputStream stream = zip.getInputStream(entry)) {
							sink.accept(path, stream);
						}
					}
				}
			}
		}
	}

	public static abstract class DataPackSink implements Closeable {

		public final File root;

		public DataPackSink(File root) {
			this.root = root;
		}

		public abstract void accept(String[] path, InputStream stream) throws IOException;
	}

	public static class DirectorySink extends DataPackSink {

		public DirectorySink(File root) {
			super(root);
		}

		@Override
		public void accept(String[] path, InputStream stream) throws IOException {
			File file = new File(this.root, migratePath(path, File.separator));
			file.getParentFile().mkdirs();
			try (FileOutputStream out = new FileOutputStream(file)) {
				stream.transferTo(out);
			}
		}

		@Override
		public void close() throws IOException {}
	}

	public static class ZipSink extends DataPackSink {

		public final ZipOutputStream out;

		public ZipSink(File root) throws IOException {
			super(root);
			this.out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(root)));
		}

		@Override
		public void accept(String[] path, InputStream stream) throws IOException {
			ZipEntry entry = new ZipEntry(migratePath(path, "/"));
			this.out.putNextEntry(entry);
			stream.transferTo(this.out);
		}

		@Override
		public void close() throws IOException {
			this.out.finish();
			this.out.close();
		}
	}

	public static String migratePath(String[] path, String separator) {
		try {
			StringJoiner joiner = new StringJoiner(separator);
			int last = -1;
			if (path[0].equals("data")) {
				joiner.add(path[0]).add(path[1]);
				if (path[2].equals("tags")) {
					last = migratePath0(path, 2, joiner.add("tags"));
				}
				else {
					last = migratePath0(path, 1, joiner);
				}
			}
			while (++last < path.length) {
				joiner.add(path[last]);
			}
			return joiner.toString();
		}
		catch (ArrayIndexOutOfBoundsException exception) {
			return String.join(separator, path);
		}
	}

	public static int migratePath0(String[] path, int last, StringJoiner joiner) {
		switch (path[++last]) {
			case "bigglobe_custom_classes"   -> joiner.add("bigglobe").add("custom_class");
			case "bigglobe_extra_mob_spawns" -> joiner.add("bigglobe").add("extra_mob_spawn");
			case "bigglobe_noise_sources"    -> joiner.add("bigglobe").add("noise_source");
			case "bigglobe_script_files"     -> joiner.add("bigglobe").add("script_file");
			case "bigglobe_script_templates" -> joiner.add("bigglobe").add("script_template");
			case "bigglobe_wood_palettes"    -> joiner.add("bigglobe").add("wood_palette");
			case "worldgen" -> {
				switch (path[++last]) {
					case "bigglobe_column_value"               -> joiner.add("bigglobe").add("worldgen").add("column_value");
					case "bigglobe_decision_tree"              -> joiner.add("bigglobe").add("worldgen").add("decision_tree");
					case "bigglobe_feature_dispatchers"        -> joiner.add("bigglobe").add("worldgen").add("feature_dispatcher");
					case "bigglobe_layers"                     -> joiner.add("bigglobe").add("worldgen").add("terrain_layer");
					case "bigglobe_overrider"                  -> joiner.add("bigglobe").add("worldgen").add("overrider");
					case "bigglobe_script_structure_placement" -> joiner.add("bigglobe").add("worldgen").add("script_structure_piece");
					case "bigglobe_voronoi_settings"           -> joiner.add("bigglobe").add("worldgen").add("voronoi_settings");
					case "bigglobe_world_traits"               -> joiner.add("bigglobe").add("worldgen").add("world_trait");
					case "bigglobe_world_trait_impl"           -> joiner.add("bigglobe").add("worldgen").add("world_trait_impl");
					default                                    -> joiner                .add("worldgen").add(path[last]);
				}
			}
			default -> joiner.add(path[last]);
		}
		return last;
	}
}
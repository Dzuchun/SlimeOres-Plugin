package dzuchun.paper.slimeores.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;

import org.bukkit.Chunk;
import org.bukkit.block.Biome;
import org.bukkit.entity.Slime;

import dzuchun.paper.slimeores.data.VeinPersistedDataType;

public class Util {

	/**
	 * @author Dzuchun
	 * @param <T>            type of the objects
	 * @param iterable       Iterable to convert
	 * @param customToString Function used to convert each of the object to string
	 * @return string representation of Iterable This function was copied from
	 *         DzuchunLib
	 */
	public static <T> String iterableToString(Iterable<T> iterable, Function<T, String> customToString) {
		if (!iterable.iterator().hasNext()) {
			return String.format("%s[]", iterable.getClass().getName());
		}
		String res = "";
		for (T o : iterable) {
			res += customToString.apply(o) + ", ";
		}
		res = res.substring(0, res.length() - 3);
		return String.format("%s[%s]", iterable.getClass().getName(), res);
	}

	public static Integer readInt(InputStream stream) throws IOException {
		ByteBuffer buffer = ByteBuffer.wrap(stream.readNBytes(Integer.BYTES));
		return buffer.getInt();
	}

	public static void writeInt(OutputStream stream, Integer integer) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
		buffer.putInt(integer);
		stream.write(buffer.array());
	}

	public static void writeStringToBuf(ByteBuffer buf, String s) {
		byte[] bytes = s.getBytes();
		buf.putInt(bytes.length);
		buf.put(bytes);
	}

	public static String readStringFromBuffer(ByteBuffer buf) {
		int size = buf.getInt();
		byte[] bytes = new byte[size];
		buf.get(bytes);
		return new String(bytes);
	}

	public static class Point {
		public int x;
		public int z;

		public Point(int xIn, int zIn) {
			this.x = xIn;
			this.z = zIn;
		}
	}

	public static Collection<Biome> getBiomesInChunk(Chunk chunk, Iterator<Point> checkPattern) {
		ArrayList<Biome> res = new ArrayList<>(0);
		while (checkPattern.hasNext()) {
			Point p = checkPattern.next();
//			OrechunkPlugin.getInstance().LOG.warning(String.format("Checking point at [%d, %d]", p.x, p.z));
			int x = chunk.getX() * 16 + p.x;
			int z = chunk.getZ() * 16 + p.z;
			Biome b = chunk.getWorld().getHighestBlockAt(x, z).getBiome();
			if (!res.contains(b)) {
				res.add(b);
			}
		}
		return res;
	}

	public static <T> boolean containsAny(Collection<T> small, Collection<T> big) {
		Iterator<T> iter = small.iterator();
		while (iter.hasNext()) {
			T t = iter.next();
			if (big.contains(t)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isVein(Slime slime) {
		return VeinPersistedDataType.getVeinType(slime) != null;
	}
}

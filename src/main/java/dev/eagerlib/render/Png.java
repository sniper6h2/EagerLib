package dev.eagerlib.render;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Inflater;

final class Png {
	final int width;
	final int height;
	final int[] argb;

	private Png(int w, int h, int[] px) {
		this.width = w;
		this.height = h;
		this.argb = px;
	}

	static Png read(InputStream raw) throws IOException {
		DataInputStream in = new DataInputStream(raw);
		long sig = in.readLong();
		if (sig != 0x89504E470D0A1A0AL) throw new IOException("not a PNG");
		int w = 0, h = 0, depth = 0, type = 0, interlace = 0;
		byte[] palette = null;
		byte[] trns = null;
		ByteArrayOutputStream idat = new ByteArrayOutputStream();
		while (true) {
			int len = in.readInt();
			byte[] t = new byte[4];
			in.readFully(t);
			String chunk = new String(t, java.nio.charset.StandardCharsets.US_ASCII);
			byte[] data = new byte[len];
			in.readFully(data);
			in.readInt();
			switch (chunk) {
				case "IHDR" -> {
					w = ((data[0] & 255) << 24) | ((data[1] & 255) << 16) | ((data[2] & 255) << 8) | (data[3] & 255);
					h = ((data[4] & 255) << 24) | ((data[5] & 255) << 16) | ((data[6] & 255) << 8) | (data[7] & 255);
					depth = data[8] & 255;
					type = data[9] & 255;
					interlace = data[12] & 255;
				}
				case "PLTE" -> palette = data;
				case "tRNS" -> trns = data;
				case "IDAT" -> idat.write(data);
				default -> {}
			}
			if (chunk.equals("IEND")) break;
		}
		if (depth != 8 || interlace != 0) throw new IOException("unsupported PNG (depth " + depth + ", interlace " + interlace + ")");
		int channels = switch (type) {
			case 0 -> 1;
			case 2 -> 3;
			case 3 -> 1;
			case 4 -> 2;
			case 6 -> 4;
			default -> throw new IOException("unsupported colour type " + type);
		};
		int stride = w * channels;
		byte[] raw2 = new byte[(stride + 1) * h];
		Inflater inf = new Inflater();
		inf.setInput(idat.toByteArray());
		try {
			int off = 0;
			while (off < raw2.length && !inf.finished()) {
				int n = inf.inflate(raw2, off, raw2.length - off);
				if (n == 0 && (inf.needsInput() || inf.needsDictionary())) break;
				off += n;
			}
		} catch (java.util.zip.DataFormatException e) {
			throw new IOException(e);
		} finally {
			inf.end();
		}
		int[] out = new int[w * h];
		byte[] prev = new byte[stride];
		byte[] cur = new byte[stride];
		int p = 0;
		for (int y = 0; y < h; y++) {
			int filter = raw2[p++] & 255;
			System.arraycopy(raw2, p, cur, 0, stride);
			p += stride;
			for (int i = 0; i < stride; i++) {
				int a = i >= channels ? cur[i - channels] & 255 : 0;
				int b = prev[i] & 255;
				int c = i >= channels ? prev[i - channels] & 255 : 0;
				int x = cur[i] & 255;
				switch (filter) {
					case 1 -> x += a;
					case 2 -> x += b;
					case 3 -> x += (a + b) >> 1;
					case 4 -> {
						int pa = Math.abs(b - c), pb = Math.abs(a - c), pc = Math.abs(a + b - 2 * c);
						x += (pa <= pb && pa <= pc) ? a : (pb <= pc ? b : c);
					}
					default -> {}
				}
				cur[i] = (byte) x;
			}
			for (int x = 0; x < w; x++) {
				int i = x * channels;
				int r, g, bl, al = 255;
				switch (type) {
					case 0 -> { r = g = bl = cur[i] & 255; }
					case 4 -> { r = g = bl = cur[i] & 255; al = cur[i + 1] & 255; }
					case 2 -> { r = cur[i] & 255; g = cur[i + 1] & 255; bl = cur[i + 2] & 255; }
					case 3 -> {
						int idx = cur[i] & 255;
						r = palette[idx * 3] & 255;
						g = palette[idx * 3 + 1] & 255;
						bl = palette[idx * 3 + 2] & 255;
						if (trns != null && idx < trns.length) al = trns[idx] & 255;
					}
					default -> { r = cur[i] & 255; g = cur[i + 1] & 255; bl = cur[i + 2] & 255; al = cur[i + 3] & 255; }
				}
				out[y * w + x] = (al << 24) | (r << 16) | (g << 8) | bl;
			}
			byte[] tmp = prev;
			prev = cur;
			cur = tmp;
		}
		return new Png(w, h, out);
	}
}

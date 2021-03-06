package org.rapidoidx.buffer;

/*
 * #%L
 * rapidoid-x-buffer
 * %%
 * Copyright (C) 2014 - 2015 Nikolche Mihajlovski and contributors
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.rapidoid.annotation.Authors;
import org.rapidoid.annotation.Since;
import org.rapidoid.arr.Arr;
import org.rapidoid.pool.Pool;
import org.rapidoid.util.Constants;
import org.rapidoid.util.D;
import org.rapidoid.util.U;
import org.rapidoid.wrap.LongWrap;
import org.rapidoidx.bytes.ByteBufferBytes;
import org.rapidoidx.bytes.Bytes;
import org.rapidoidx.bytes.BytesUtil;
import org.rapidoidx.data.Range;
import org.rapidoidx.data.Ranges;

@Authors("Nikolche Mihajlovski")
@Since("3.0.0")
public class MultiBuf implements Buf, Constants {

	private final byte[] HELPER = new byte[20];

	private final Range HELPER_RANGE = new Range();

	private static final int TO_BYTES = 1;

	private static final int TO_CHANNEL = 2;

	private static final int TO_BUFFER = 3;

	private final Pool<ByteBuffer> bufPool;

	private final long factor;

	private final long addrMask;

	private final int singleCap;

	private ByteBuffer[] bufs = new ByteBuffer[10];

	private int bufN;

	private long shrinkN;

	private final String name;

	private long _position;

	private long _limit;

	private long _checkpoint;

	private OutputStream outputStream;

	private final ByteBufferBytes singleBytes = new ByteBufferBytes();

	private final Bytes multiBytes = new BufBytes(this);

	private Bytes _bytes = multiBytes;

	private long _size;

	private boolean readOnly = false;

	public MultiBuf(Pool<ByteBuffer> bufPool, int factor, String name) {
		this.bufPool = bufPool;
		this.name = name;
		this.singleCap = (int) Math.pow(2, factor);
		this.factor = factor;
		this.addrMask = addrMask();

		assert invariant(true);
	}

	private long addrMask() {
		long mask = 1;

		for (long i = 0; i < factor - 1; i++) {
			mask <<= 1;
			mask |= 1;
		}

		return mask;
	}

	@Override
	public boolean isSingle() {
		assert invariant(false);
		return bufN == 1;
	}

	@Override
	public byte get(long position) {
		assert invariant(false);
		assert position >= 0;

		validatePos(position, 1);

		position += shrinkN;

		ByteBuffer buf = bufs[(int) (position >> factor)];
		assert buf != null;

		assert invariant(false);
		return buf.get((int) (position & addrMask));
	}

	private void validatePos(long pos, long space) {
		if (pos < 0) {
			throw U.rte("Invalid position: " + pos);
		}

		long least = pos + space;

		boolean hasEnough = least <= _size() && least <= _limit;

		if (!hasEnough) {
			throw INCOMPLETE_READ;
		}
	}

	@Override
	public void put(long position, byte value) {
		assert invariant(true);
		assert position >= 0;

		validatePos(position, 1);

		position += shrinkN;

		ByteBuffer buf = bufs[(int) (position >> factor)];
		assert buf != null;

		buf.put((int) (position & addrMask), value);

		assert invariant(true);
	}

	@Override
	public long size() {
		assert invariant(false);

		assert _size == _size();

		return _size;
	}

	private long _size() {
		if (bufN > 0) {
			long s = (bufN - 1);

			s *= singleCap;
			s += bufs[bufN - 1].position();
			s -= shrinkN;

			return s;
		} else {
			return 0;
		}
	}

	private void expandUnit() {
		if (bufN == bufs.length) {
			bufs = Arr.expand(bufs, 2);
		}

		bufs[bufN] = bufPool.get();
		bufs[bufN].clear();

		bufN++;
	}

	@Override
	public void append(byte value) {
		assert invariant(true);

		writableBuf().put(value);

		sizeChanged();

		assert invariant(true);
	}

	/**
	 * Reads data from the channel and appends it to the buffer.
	 * 
	 * Precondition: received event that the channel has data to be read.
	 */
	@Override
	public long append(ReadableByteChannel channel) throws IOException {
		assert invariant(true);

		long totalRead = 0;

		try {

			boolean done;

			// precondition: the channel has data

			do {
				ByteBuffer dest = writableBuf();

				long space = dest.remaining();
				assert space > 0;

				long read = channel.read(dest);
				if (read >= 0) {
					totalRead += read;
				} else {
					// end of stream (e.g. the other end closed the connection)
					removeLastBufferIfEmpty();
					sizeChanged();

					assert invariant(true);
					return -1;
				}

				// if buffer wasn't filled -> no data is available in channel
				done = read < space;
			} while (!done);

		} finally {
			removeLastBufferIfEmpty();
			sizeChanged();
			assert invariant(true);
		}

		return totalRead;
	}

	@Override
	public void append(ByteBuffer src) {
		assert invariant(true);

		int theLimit = src.limit();

		while (src.hasRemaining()) {
			ByteBuffer dest = writableBuf();

			int space = dest.remaining();
			assert space > 0;

			if (src.remaining() > space) {
				// set limit to match only available space in dest
				src.limit(src.position() + space);
			}

			dest.put(src);

			// restore original limit
			src.limit(theLimit);
		}

		sizeChanged();

		assert invariant(true);
	}

	@Override
	public void append(byte[] src, int offset, int length) {
		assert invariant(true);

		long sizeBefore = _size();

		if (length > 0) {
			ByteBuffer buf = writableBuf();

			if (length <= buf.remaining()) {
				buf.put(src, offset, length);
			} else {
				int partLen = buf.remaining();
				buf.put(src, offset, partLen);
				assert buf.remaining() == 0;
				append(src, offset + partLen, length - partLen);
			}
		}

		sizeChanged();

		assert _size() - sizeBefore == length;

		assert invariant(true);
	}

	private ByteBuffer writableBuf() {
		if (bufN == 0) {
			expandUnit();
			return last();
		}

		ByteBuffer cbuf = last();

		if (!cbuf.hasRemaining()) {
			expandUnit();
			cbuf = last();
		}

		assert cbuf.hasRemaining();
		return cbuf;
	}

	private ByteBuffer last() {
		assert bufN > 0;
		return bufs[bufN - 1];
	}

	@Override
	public ByteBuffer first() {
		assert invariant(false);
		assert bufN > 0;
		return bufs[0];
	}

	@Override
	public ByteBuffer bufAt(int index) {
		assert invariant(false);
		assert bufN > index;
		return bufs[index];
	}

	@Override
	public long append(String s) {
		assert invariant(true);

		byte[] bytes = s.getBytes();
		append(bytes);

		sizeChanged();

		assert invariant(true);
		return bytes.length;
	}

	@Override
	public String toString() {
		return String.format("Buf " + name + " [size=" + _size() + ", units=" + unitCount() + ", trash=" + shrinkN
				+ ", pos=" + position() + ", limit=" + limit() + "] " + super.toString());
	}

	@Override
	public String data() {
		assert invariant(false);

		byte[] bytes = new byte[(int) _size()];
		long total = readAll(bytes, 0, 0, bytes.length);

		assert total == bytes.length;

		assert invariant(false);
		return new String(bytes);
	}

	@Override
	public String get(Range range) {
		assert invariant(false);

		if (range.isEmpty()) {
			return "";
		}

		byte[] bytes = new byte[(int) range.length];
		long total = readAll(bytes, 0, range.start, range.length);

		assert total == bytes.length;

		assert invariant(false);
		return new String(bytes);
	}

	@Override
	public void get(Range range, byte[] dest, int offset) {
		assert invariant(false);

		long total = readAll(dest, offset, range.start, range.length);

		assert total == range.length;
		assert invariant(false);
	}

	private long writeToHelper(Range range) {
		assert invariant(false);
		return readAll(HELPER, 0, range.start, range.length);
	}

	private long readAll(byte[] bytes, long destOffset, long offset, long length) {
		assert invariant(false);

		if (offset + length > _size()) {
			throw new IllegalArgumentException("offset + length > buffer size!");
		}

		long wrote;
		try {
			wrote = writeTo(TO_BYTES, offset, length, bytes, null, null, destOffset);
		} catch (IOException e) {
			throw U.rte(e);
		}

		assert invariant(false);
		return wrote;
	}

	@Override
	public long writeTo(WritableByteChannel channel) throws IOException {
		assert invariant(true);

		long wrote = writeTo(TO_CHANNEL, 0, _size(), null, channel, null, 0);
		assert U.must(wrote <= _size(), "Incorrect write to channel!");

		assert invariant(true);
		return wrote;
	}

	@Override
	public long writeTo(ByteBuffer buffer) {
		assert invariant(true);

		try {
			long wrote = writeTo(TO_BUFFER, 0, _size(), null, null, buffer, 0);
			assert wrote == _size();
			assert invariant(true);
			return wrote;
		} catch (IOException e) {
			assert invariant(true);
			throw U.rte(e);
		}
	}

	private long writeTo(int mode, long offset, long length, byte[] bytes, WritableByteChannel channel,
			ByteBuffer buffer, long destOffset) throws IOException {
		if (_size() == 0) {
			assert length == 0;
			return 0;
		}

		long fromPos = offset + shrinkN;
		long toPos = fromPos + length - 1;

		int fromInd = (int) (fromPos >> factor);
		int toInd = (int) (toPos >> factor);

		int fromAddr = (int) (fromPos & addrMask);
		int toAddr = (int) (toPos & addrMask);

		assert fromInd <= toInd;

		if (fromInd == toInd) {
			return writePart(bufs[fromInd], fromAddr, toAddr + 1, mode, bytes, channel, buffer, destOffset, -1);
		} else {
			return multiWriteTo(mode, fromInd, toInd, fromAddr, toAddr, bytes, channel, buffer, destOffset);
		}
	}

	private long multiWriteTo(int mode, int fromIndex, int toIndex, int fromAddr, int toAddr, byte[] bytes,
			WritableByteChannel channel, ByteBuffer buffer, long destOffset) throws IOException {

		ByteBuffer first = bufs[fromIndex];
		long len = singleCap - fromAddr;

		long wrote = writePart(first, fromAddr, singleCap, mode, bytes, channel, buffer, destOffset, len);
		if (wrote < len) {
			return wrote;
		}

		long wroteTotal = wrote;

		for (int i = fromIndex + 1; i < toIndex; i++) {

			wrote = writePart(bufs[i], 0, singleCap, mode, bytes, channel, buffer, destOffset + wroteTotal, singleCap);

			wroteTotal += wrote;

			if (wrote < singleCap) {
				return wroteTotal;
			}
		}

		ByteBuffer last = bufs[toIndex];
		wroteTotal += writePart(last, 0, toAddr + 1, mode, bytes, channel, buffer, destOffset + wroteTotal, toAddr + 1);

		return wroteTotal;
	}

	private long writePart(ByteBuffer src, int pos, int limit, int mode, byte[] bytes, WritableByteChannel channel,
			ByteBuffer buffer, long destOffset, long len) throws IOException {

		// backup buf positions
		int posBackup = src.position();
		int limitBackup = src.limit();

		src.position(pos);
		src.limit(limit);

		assert src.remaining() == len || len < 0;

		long count;

		switch (mode) {
		case TO_BYTES:
			if (len >= 0) {
				src.get(bytes, (int) destOffset, (int) len);
				count = len;
			} else {
				count = src.remaining();
				src.get(bytes, (int) destOffset, (int) count);
			}
			break;

		case TO_CHANNEL:
			count = 0;
			while (src.hasRemaining()) {
				long wrote = channel.write(src);
				count += wrote;
				if (wrote == 0) {
					break;
				}
			}
			break;

		case TO_BUFFER:
			count = src.remaining();
			buffer.put(src);
			break;

		default:
			throw U.notExpected();
		}

		// restore buf positions
		src.limit(limitBackup);
		src.position(posBackup);

		return count;
	}

	private boolean invariant(boolean writing) {
		if (this.readOnly) {
			assert !writing;
		}

		try {

			assert bufN >= 0;

			for (int i = 0; i < bufN - 1; i++) {
				ByteBuffer buf = bufs[i];
				assert buf.position() == singleCap;
				assert buf.limit() == singleCap;
				assert buf.capacity() == singleCap;
			}

			if (bufN > 0) {
				ByteBuffer buf = bufs[bufN - 1];
				assert buf == last();
				assert buf.position() > 0;
				assert buf.capacity() == singleCap;
			}

			return true;

		} catch (AssertionError e) {
			dumpBuffers();
			throw e;
		}
	}

	private void dumpBuffers() {
		System.out.println(">> BUFFER " + name + " HAS " + bufN + " PARTS:");

		for (int i = 0; i < bufN - 1; i++) {
			ByteBuffer buf = bufs[i];
			D.print(i + "]" + buf);
		}

		if (bufN > 0) {
			ByteBuffer buf = bufs[bufN - 1];
			D.print("LAST]" + buf);
		}
	}

	@Override
	public void deleteBefore(long count) {
		assert invariant(true);

		if (count == _size()) {
			clear();
			return;
		}

		shrinkN += count;

		while (shrinkN >= singleCap) {
			removeFirstBuf();
			shrinkN -= singleCap;
		}

		_position -= count;
		if (_position < 0) {
			_position = 0;
		}

		sizeChanged();

		assert invariant(true);
	}

	private void removeFirstBuf() {
		bufs[0].clear();
		bufPool.release(bufs[0]);

		for (int i = 0; i < bufN - 1; i++) {
			bufs[i] = bufs[i + 1];
		}

		bufN--;
	}

	private void removeLastBuf() {
		bufs[bufN - 1].clear();
		bufPool.release(bufs[bufN - 1]);
		bufN--;
		if (bufN == 0) {
			shrinkN = 0;
		}
	}

	private void removeLastBufferIfEmpty() {
		if (bufN > 0) {
			if (last().position() == 0) {
				removeLastBuf();
			}
		}
	}

	@Override
	public int unitCount() {
		assert invariant(false);
		return bufN;
	}

	@Override
	public int unitSize() {
		assert invariant(false);
		return singleCap;
	}

	@Override
	public void put(long position, byte[] bytes, int offset, int length) {
		assert invariant(true);

		// TODO optimize
		long pos = position;
		for (int i = offset; i < offset + length; i++) {
			put(pos++, bytes[i]);
		}

		assert invariant(true);
	}

	@Override
	public void append(byte[] bytes) {
		assert invariant(true);

		append(bytes, 0, bytes.length);

		assert invariant(true);
	}

	@Override
	public void deleteAfter(long position) {
		assert invariant(true);

		if (bufN == 0 || position == _size()) {
			assert invariant(true);
			return;
		}

		assert validPosition(position);

		if (bufN == 1) {
			long newPos = position + shrinkN;
			assert newPos <= Integer.MAX_VALUE && newPos <= singleCap;
			first().position((int) newPos);
			if (newPos == 0) {
				removeLastBuf();
			}
		} else {
			position += shrinkN;
			int index = (int) (position >> factor);
			int addr = (int) (position & addrMask);

			// make it the last buffer
			while (index < bufN - 1) {
				removeLastBuf();
			}

			ByteBuffer last = bufs[index];
			assert last() == last;

			if (addr > 0) {
				last.position(addr);
			} else {
				removeLastBuf();
				if (bufN > 0) {
					last().position(singleCap);
				}
			}
		}

		removeLastBufferIfEmpty();
		sizeChanged();

		assert invariant(true);
	}

	@Override
	public void deleteLast(long count) {
		assert invariant(true);

		deleteAfter(_size() - count);

		assert invariant(true);
	}

	private boolean validPosition(long position) {
		assert U.must(position >= 0 && position < _size(), "Invalid position: %s", position);
		return true;
	}

	@Override
	public void clear() {
		// don't assert invariant() here, invalid state is allowed before clear/reset

		for (int i = 0; i < bufN; i++) {
			bufs[i].clear();
			bufPool.release(bufs[i]);
		}

		readOnly = false;
		shrinkN = 0;
		bufN = 0;
		_position = 0;

		sizeChanged();

		assert invariant(true);
	}

	@Override
	public long getN(Range range) {
		assert invariant(false);

		assert range.length >= 1;

		if (range.length > 20) {
			assert invariant(false);
			throw U.rte("Too many digits!");
		}

		long count = writeToHelper(range);

		long value = 0;

		boolean negative = HELPER[0] == '-';
		int start = negative ? 1 : 0;

		for (int i = start; i < count; i++) {
			byte b = HELPER[i];
			if (b >= '0' && b <= '9') {
				long digit = b - '0';
				value = value * 10 + digit;
			} else {
				assert invariant(false);
				throw U.rte("Invalid number: '%s'", get(range));
			}
		}

		assert invariant(false);
		return negative ? -value : value;
	}

	@Override
	public ByteBuffer getSingle() {
		assert invariant(false);
		return isSingle() ? first() : null;
	}

	@Override
	public long putNumAsText(long position, long n, boolean forward) {
		assert invariant(true);

		long direction = forward ? 0 : -1;

		long space;

		if (n >= 0) {
			if (n < 10) {
				put(position, (byte) (n + '0'));
				space = 1;
			} else if (n < 100) {
				long dig1 = n / 10;
				long dig2 = n % 10;
				put(position + direction, (byte) (dig1 + '0'));
				put(position + direction + 1, (byte) (dig2 + '0'));
				space = 2;
			} else {
				long digitsN = (long) Math.ceil(Math.log10(n + 1));

				long pos = position + digitsN - 1 + direction * digitsN;
				if (!forward) {
					pos++;
				}

				while (true) {
					long digit = n % 10;
					byte dig = (byte) (digit + 48);
					put(pos--, dig);

					if (n < 10) {
						break;
					}
					n = n / 10;
				}
				space = digitsN;
			}
		} else {
			if (forward) {
				put(position, (byte) ('-'));
				space = putNumAsText(position + 1, -n, forward) + 1;
			} else {
				long digits = putNumAsText(position, -n, forward);
				put(position - digits, (byte) ('-'));
				space = digits + 1;
			}
		}

		assert invariant(true);
		return space;
	}

	@SuppressWarnings("unused")
	private long rebase(long pos, long bufInd) {
		return (bufInd << factor) + pos - shrinkN;
	}

	@Override
	public byte next() {
		assert invariant(false);

		byte b = get(_position++);

		assert invariant(false);
		return b;
	}

	@Override
	public void back(long count) {
		assert invariant(false);

		_position--;

		assert invariant(false);
	}

	@Override
	public byte peek() {
		assert invariant(false);

		byte b = get(_position);

		assert invariant(false);
		return b;
	}

	@Override
	public boolean hasRemaining() {
		assert invariant(false);

		boolean result = remaining() > 0;

		assert invariant(false);
		return result;
	}

	@Override
	public long remaining() {
		assert invariant(false);
		return _limit - _position;
	}

	@Override
	public long position() {
		assert invariant(false);
		return _position;
	}

	@Override
	public long limit() {
		assert invariant(false);
		return _limit;
	}

	private void sizeChanged() {
		_size = _size();
		_limit = _size();

		if (bufN == 1) {
			singleBytes.setBuf(bufs[0]);
			_bytes = singleBytes;
		} else {
			_bytes = multiBytes;
		}
	}

	@Override
	public void position(long position) {
		assert invariant(false);
		_position = position;
		assert invariant(false);
	}

	@Override
	public void limit(long limit) {
		assert invariant(false);
		_limit = limit;
		assert invariant(false);
	}

	@Override
	public void upto(byte value, Range range) {
		assert invariant(false);

		range.starts(_position);

		while (get(_position) != value) {
			_position++;
		}

		range.ends(_position);

		_position++;

		assert invariant(false);
	}

	@Override
	public ByteBuffer exposed() {
		assert invariant(false);

		ByteBuffer first = first();

		assert invariant(false);
		return first;
	}

	@Override
	public void scanUntil(byte value, Range range) {
		assert invariant(false);

		requireRemaining(1);

		long start = position();
		long limit = limit();
		long last = limit - 1;

		long fromPos = start + shrinkN;
		long toPos = last + shrinkN;

		int fromInd = (int) (fromPos >> factor);
		int toInd = (int) (toPos >> factor);

		int fromAddr = (int) (fromPos & addrMask);
		int toAddr = (int) (toPos & addrMask);

		assert U.must(fromInd >= 0, "bad start: %s", start);
		assert U.must(toInd >= 0, "bad end: %s", last);

		ByteBuffer src = bufs[fromInd];

		long absPos = start;

		for (int pos = fromAddr; pos < singleCap; pos++) {
			byte b = src.get(pos);

			if (b == value) {
				range.setInterval(start, absPos);
				position(absPos + 1);
				assert invariant(false);
				return;
			}

			absPos++;
		}

		for (int i = fromInd + 1; i < toInd; i++) {
			src = bufs[i];

			for (int pos = 0; pos < singleCap; pos++) {
				byte b = src.get(pos);

				if (b == value) {
					range.setInterval(start, absPos);
					position(absPos + 1);
					assert invariant(false);
					return;
				}

				absPos++;
			}
		}

		if (fromInd < toInd) {
			src = bufs[toInd];

			for (int pos = 0; pos <= toAddr; pos++) {
				byte b = src.get(pos);

				if (b == value) {
					range.setInterval(start, absPos);
					position(absPos + 1);
					assert invariant(false);
					return;
				}

				absPos++;
			}
		}

		position(limit);

		assert invariant(false);
		throw INCOMPLETE_READ;
	}

	@Override
	public void scanWhile(byte value, Range range) {
		assert invariant(false);

		requireRemaining(1);

		long start = position();
		long limit = limit();
		long last = limit - 1;

		long fromPos = start + shrinkN;
		long toPos = last + shrinkN;

		int fromInd = (int) (fromPos >> factor);
		int toInd = (int) (toPos >> factor);

		int fromAddr = (int) (fromPos & addrMask);
		int toAddr = (int) (toPos & addrMask);

		assert U.must(fromInd >= 0, "bad start: %s", start);
		assert U.must(toInd >= 0, "bad end: %s", last);

		ByteBuffer src = bufs[fromInd];

		long absPos = start;

		for (int pos = fromAddr; pos < singleCap; pos++) {
			byte b = src.get(pos);

			if (b != value) {
				range.setInterval(start, absPos);
				position(absPos);
				assert invariant(false);
				return;
			}

			absPos++;
		}

		for (int i = fromInd + 1; i < toInd; i++) {
			src = bufs[i];

			for (int pos = 0; pos < singleCap; pos++) {
				byte b = src.get(pos);

				if (b != value) {
					range.setInterval(start, absPos);
					position(absPos);
					assert invariant(false);
					return;
				}

				absPos++;
			}
		}

		if (fromInd < toInd) {
			src = bufs[toInd];

			for (int pos = 0; pos <= toAddr; pos++) {
				byte b = src.get(pos);

				if (b != value) {
					range.setInterval(start, absPos);
					position(absPos);
					assert invariant(false);
					return;
				}

				absPos++;
			}
		}

		position(limit);

		assert invariant(false);
		throw INCOMPLETE_READ;
	}

	private void requireRemaining(long n) {
		if (remaining() < n) {
			throw Buf.INCOMPLETE_READ;
		}
	}

	@Override
	public void skip(long count) {
		assert invariant(false);

		requireRemaining(count);
		_position += count;

		assert invariant(false);
	}

	@Override
	public int bufferIndexOf(long position) {
		assert invariant(false);

		assert position >= 0;

		validatePos(position, 1);

		position += shrinkN;

		int index = (int) (position >> factor);
		assert bufs[index] != null;

		assert invariant(false);
		return index;
	}

	@Override
	public int bufferOffsetOf(long position) {
		assert invariant(false);

		assert position >= 0;

		validatePos(position, 1);

		position += shrinkN;

		assert invariant(false);
		return (int) (position & addrMask);
	}

	@Override
	public int bufCount() {
		assert invariant(false);
		return bufN;
	}

	@Override
	public OutputStream asOutputStream() {
		assert invariant(false);

		if (outputStream == null) {
			outputStream = new OutputStream() {
				@Override
				public void write(int b) throws IOException {
					assert invariant(true);
					append((byte) b);
				}
			};
		}

		assert invariant(false);
		return outputStream;
	}

	@Override
	public String asText() {
		return get(new Range(0, size()));
	}

	@Override
	public Bytes bytes() {
		assert invariant(false);
		return _bytes;
	}

	@Override
	public void scanLn(Range line) {
		assert invariant(false);
		long pos = BytesUtil.parseLine(bytes(), line, position(), size());

		if (pos < 0) {
			assert invariant(false);
			throw INCOMPLETE_READ;
		}

		_position = pos;
		assert invariant(false);
	}

	@Override
	public void scanLnLn(Ranges lines) {
		assert invariant(false);

		long pos = BytesUtil.parseLines(bytes(), lines, position(), size());

		if (pos < 0) {
			assert invariant(false);
			throw INCOMPLETE_READ;
		}

		_position = pos;
		assert invariant(false);
	}

	@Override
	public void scanN(long count, Range range) {
		assert invariant(false);

		get(_position + count - 1);
		range.set(_position, count);
		_position += count;

		assert invariant(false);
	}

	@Override
	public String readLn() {
		assert invariant(false);

		scanLn(HELPER_RANGE);
		String result = get(HELPER_RANGE);

		assert invariant(false);
		return result;
	}

	@Override
	public String readN(long count) {
		assert invariant(false);

		scanN(count, HELPER_RANGE);
		String result = get(HELPER_RANGE);

		assert invariant(false);
		return result;
	}

	@Override
	public byte[] readNbytes(int count) {
		assert invariant(false);

		scanN(count, HELPER_RANGE);
		byte[] bytes = new byte[count];
		get(HELPER_RANGE, bytes, 0);

		assert invariant(false);
		return bytes;
	}

	@Override
	public void scanTo(byte sep, Range range, boolean failOnLimit) {
		assert invariant(false);

		long pos = BytesUtil.find(bytes(), _position, _limit, sep, true);

		if (pos >= 0) {
			consumeAndSkip(pos, range, 1);
		} else {
			if (failOnLimit) {
				assert invariant(false);
				throw INCOMPLETE_READ;
			} else {
				consumeAndSkip(_limit, range, 0);
			}
		}

		assert invariant(false);
	}

	@Override
	public long scanTo(byte sep1, byte sep2, Range range, boolean failOnLimit) {
		assert invariant(false);

		long pos1 = BytesUtil.find(bytes(), _position, _limit, sep1, true);
		long pos2 = BytesUtil.find(bytes(), _position, _limit, sep2, true);

		boolean found1 = pos1 >= 0;
		boolean found2 = pos2 >= 0;

		if (found1 && found2) {
			if (pos1 <= pos2) {
				consumeAndSkip(pos1, range, 1);
				assert invariant(false);
				return 1;
			} else {
				consumeAndSkip(pos2, range, 1);
				assert invariant(false);
				return 2;
			}
		} else if (found1 && !found2) {
			consumeAndSkip(pos1, range, 1);
			assert invariant(false);
			return 1;
		} else if (!found1 && found2) {
			consumeAndSkip(pos2, range, 1);
			assert invariant(false);
			return 2;
		} else {
			if (failOnLimit) {
				assert invariant(false);
				throw INCOMPLETE_READ;
			} else {
				consumeAndSkip(_limit, range, 0);
				assert invariant(false);
				return 0;
			}
		}
	}

	private void consumeAndSkip(long toPos, Range range, long skip) {
		range.setInterval(_position, toPos);
		_position = toPos + skip;
	}

	@Override
	public void scanLnLn(Ranges ranges, LongWrap result, byte end1, byte end2) {
		assert invariant(false);

		long nextPos = BytesUtil.parseLines(bytes(), ranges, result, _position, _limit, end1, end2);

		if (nextPos < 0) {
			throw Buf.INCOMPLETE_READ;
		}

		_position = nextPos;

		assert invariant(false);
	}

	@Override
	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	@Override
	public long checkpoint() {
		return _checkpoint;
	}

	@Override
	public void checkpoint(long checkpoint) {
		this._checkpoint = checkpoint;
	}

}

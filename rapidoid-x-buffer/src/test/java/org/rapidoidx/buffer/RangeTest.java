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

import org.junit.Test;
import org.rapidoid.annotation.Authors;
import org.rapidoid.annotation.Since;
import org.rapidoidx.data.Range;

@Authors("Nikolche Mihajlovski")
@Since("3.0.0")
public class RangeTest extends BufferTestCommons {

	@Test
	public void statisticalTest() {
		Range rng = new Range();
		isTrue(rng.isEmpty());

		long[] borders = { Long.MIN_VALUE, -1111, -1, 0, 1, 1111, Long.MAX_VALUE };

		for (int i = 0; i < borders.length; i++) {
			for (int j = 0; j < borders.length; j++) {
				check(rng, borders[i], borders[j]);
			}
		}

		for (int i = 0; i < 1000000; i++) {
			check(rng, rnd(), rnd());
		}
	}

	private void check(Range rng, long a, long b) {
		rng.set(a, b);

		eq(rng, a, b);
		eq(rng.limit(), rng.start + rng.length);
	}

}

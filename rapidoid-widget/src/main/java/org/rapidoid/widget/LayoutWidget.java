package org.rapidoid.widget;

/*
 * #%L
 * rapidoid-widget
 * %%
 * Copyright (C) 2014 - 2015 Nikolche Mihajlovski and contributors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.List;

import org.rapidoid.annotation.Authors;
import org.rapidoid.annotation.Since;
import org.rapidoid.html.Tag;
import org.rapidoid.util.U;

@Authors("Nikolche Mihajlovski")
@Since("2.3.0")
public class LayoutWidget extends AbstractWidget {

	private Object[] contents = {};

	private int cols = 1;

	@Override
	protected Object render() {
		List<Tag> rows = U.list();

		Tag row = row().class_("row row-separated");

		int n = 0;
		int colSize = 12 / cols;

		for (Object item : contents) {
			n++;
			if (n == cols + 1) {
				n = 1;
				rows.add(row);
				row = row().class_("row row-separated");
			}
			row = row.append(col_(colSize, item));
		}

		if (!row.isEmpty()) {
			rows.add(row);
		}

		return rows.toArray(new Tag[rows.size()]);
	}

	public Object[] contents() {
		return contents;
	}

	public LayoutWidget contents(Object... contents) {
		this.contents = contents;
		return this;
	}

	public int cols() {
		return cols;
	}

	public LayoutWidget cols(int cols) {
		this.cols = cols;
		return this;
	}

}

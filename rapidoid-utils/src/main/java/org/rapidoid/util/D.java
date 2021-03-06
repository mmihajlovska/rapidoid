package org.rapidoid.util;

import java.util.Map;
import java.util.Map.Entry;

import org.rapidoid.annotation.Authors;
import org.rapidoid.annotation.Since;

/*
 * #%L
 * rapidoid-utils
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

@Authors("Nikolche Mihajlovski")
@Since("2.4.0")
public class D {

	private D() {}

	public static void print(Object... values) {
		String text;

		if (values != null) {
			text = values.length == 1 ? U.readable(values[0]) : U.readable(values);
		} else {
			text = "null";
		}

		System.out.println(">" + text + "<");
	}

	public static void printKV(Map<?, ?> map) {
		for (Entry<?, ?> e : map.entrySet()) {
			System.out.println(e.getKey() + "=" + e.getValue());
		}
	}

}

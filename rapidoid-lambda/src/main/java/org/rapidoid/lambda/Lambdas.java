package org.rapidoid.lambda;

import java.util.Map;

/*
 * #%L
 * rapidoid-lambda
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

/**
 * @author Nikolche Mihajlovski
 * @since 2.0.0
 */
public class Lambdas {

	public static <FROM, TO> Mapper<FROM, TO> mapper(final Map<FROM, TO> map) {
		return new Mapper<FROM, TO>() {
			@Override
			public TO map(FROM key) throws Exception {
				return map.get(key);
			}
		};
	}

	public static <T> boolean eval(Predicate<T> predicate, T target) {
		try {
			return predicate.eval(target);
		} catch (Exception e) {
			throw new RuntimeException(String.format("Cannot evaluate predicate %s on target: %s", predicate, target),
					e);
		}
	}

	public static <FROM, TO> TO eval(Mapper<FROM, TO> mapper, FROM src) {
		try {
			return mapper.map(src);
		} catch (Exception e) {
			throw new RuntimeException(String.format("Cannot evaluate mapper %s on target: %s", mapper, src), e);
		}
	}

	public static <FROM> Object eval(Calc<FROM> calc, FROM src) {
		try {
			return calc.calc(src);
		} catch (Exception e) {
			throw new RuntimeException(String.format("Cannot evaluate calculation %s on target: %s", calc, src), e);
		}
	}

}

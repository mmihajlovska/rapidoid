package org.rapidoid.plugins.cache;

/*
 * #%L
 * rapidoid-plugins
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

import org.rapidoid.annotation.Authors;
import org.rapidoid.annotation.Since;
import org.rapidoid.concurrent.Callback;
import org.rapidoid.concurrent.Future;

@Authors("Nikolche Mihajlovski")
@Since("4.1.0")
public interface ICache<K, V> {

	void set(K key, V value, Callback<Void> callback);

	void set(K key, V value, long timeToLiveMs, Callback<Void> callback);

	void get(K key, Callback<V> callback);

	Future<Void> set(K key, V value);

	Future<Void> set(K key, V value, long timeToLiveMs);

	Future<V> get(K key);

}

package org.rapidoid.job;

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

import org.rapidoid.annotation.Authors;
import org.rapidoid.annotation.Since;
import org.rapidoid.concurrent.Callback;

@Authors("Nikolche Mihajlovski")
@Since("4.1.0")
public class CallbackExecutorJob<T> implements Runnable {

	private final Callback<T> callback;
	private final T result;
	private final Throwable error;

	public CallbackExecutorJob(Callback<T> callback, T result, Throwable error) {
		this.callback = callback;
		this.result = result;
		this.error = error;
	}

	@Override
	public void run() {
		try {
			callback.onDone(result, error);
		} catch (Exception e) {
			throw new RuntimeException("Error occured while executing callback!", e);
		}
	}

}

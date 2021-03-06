package org.rapidoid.widget.impl;

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

import java.io.Serializable;

import org.rapidoid.annotation.Authors;
import org.rapidoid.annotation.Since;

@Authors("Nikolche Mihajlovski")
@Since("2.0.0")
public class SessionVar<T extends Serializable> extends WidgetVar<T> {

	private static final long serialVersionUID = 2761159925375675659L;

	private final String sessionKey;

	private final T defaultValue;

	public SessionVar(String sessionKey, T defaultValue) {
		super(sessionKey, false);
		this.sessionKey = sessionKey;
		this.defaultValue = defaultValue;
	}

	@Override
	public T get() {
		T val = ctx().session(sessionKey, defaultValue);
		return val;
	}

	@Override
	public void set(T value) {
		ctx().session().put(sessionKey, value);
	}

}

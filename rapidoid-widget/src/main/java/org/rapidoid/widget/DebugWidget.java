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

import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;

import org.rapidoid.annotation.Authors;
import org.rapidoid.annotation.Since;
import org.rapidoid.util.U;

@Authors("Nikolche Mihajlovski")
@Since("2.3.1")
public class DebugWidget extends AbstractWidget {

	@Override
	protected Object render() {
		return arr(sessionPanel(), localPanel());
	}

	protected PanelWidget sessionPanel() {
		Map<String, Object> visibleAttributes = U.map();

		for (Entry<String, Serializable> e : ctx().session().entrySet()) {
			if (!e.getKey().startsWith("_")) {
				visibleAttributes.put(e.getKey(), e.getValue());
			}
		}

		return panel(grid(visibleAttributes)).header("Session scope");
	}

	protected PanelWidget localPanel() {
		Map<String, Object> visibleAttributes = U.map();

		for (Entry<String, Serializable> e : ctx().locals().entrySet()) {
			if (!e.getKey().startsWith("_")) {
				visibleAttributes.put(e.getKey(), e.getValue());
			}
		}

		return panel(grid(visibleAttributes)).header("Local scope");
	}

}

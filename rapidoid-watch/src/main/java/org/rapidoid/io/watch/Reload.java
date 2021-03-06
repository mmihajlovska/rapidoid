package org.rapidoid.io.watch;

/*
 * #%L
 * rapidoid-watch
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

import java.util.ArrayList;
import java.util.List;

import org.rapidoid.annotation.Authors;
import org.rapidoid.annotation.Since;
import org.rapidoid.log.Log;
import org.rapidoid.util.U;

@Authors("Nikolche Mihajlovski")
@Since("4.1.0")
public class Reload {

	public static ClassReloader createClassLoader(String dir) {
		Log.info("Creating class loader", "dir", dir);
		ClassLoader parentClassLoader = ClassReloader.class.getClassLoader();
		return new ClassReloader(dir, parentClassLoader, new ArrayList<String>());
	}

	public static synchronized List<Class<?>> reloadClasses(String dir, List<String> classnames) {
		ClassReloader classLoader = Reload.createClassLoader(dir);

		List<Class<?>> classes = U.list();

		for (String classname : classnames) {
			try {
				classes.add(classLoader.loadClass(classname));
			} catch (Throwable e) {
				Log.warn("Couldn't reload class!", e);
			}
		}

		return classes;
	}

}

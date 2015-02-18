package org.rapidoid.docs.eg8;

import org.rapidoid.app.Apps;

/*
 * #%L
 * rapidoid-docs
 * %%
 * Copyright (C) 2014 - 2015 Nikolche Mihajlovski
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

// Show the "search" box in the navigation

public class App {
	String title = "My app";
	Object content = "Hello!";
	String theme = "2";

	boolean full = false; // here
	boolean search = true; // here

	public static void main(String[] args) {
		Apps.run(args);
	}
}

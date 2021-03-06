package org.rapidoid.test;

/*
 * #%L
 * rapidoid-integration-tests
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

import org.junit.Test;
import org.rapidoid.annotation.Authors;
import org.rapidoid.annotation.Since;
import org.rapidoid.plugins.Plugins;
import org.rapidoid.plugins.templates.ITemplate;
import org.rapidoid.plugins.templates.MustacheTemplatesPlugin;
import org.rapidoid.plugins.templates.Templates;
import org.rapidoid.util.U;

@Authors("Nikolche Mihajlovski")
@Since("4.1.0")
public class MustacheTemplatesTest extends IntegrationTestCommons {

	@Test
	public void testTemplateLoading() {
		Plugins.register(new MustacheTemplatesPlugin());

		ITemplate templ = Templates.fromFile("templ1.html");

		eq(templ.render(U.map("x", "123")), "A:123:B:OK:C");
	}

}

package org.rapidoid.app;

/*
 * #%L
 * rapidoid-app
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
import org.rapidoid.app.builtin.AppPageGeneric;
import org.rapidoid.dispatch.PojoDispatchException;
import org.rapidoid.dispatch.PojoHandlerNotFoundException;
import org.rapidoid.http.Handler;
import org.rapidoid.http.HttpExchange;
import org.rapidoid.http.HttpExchangeInternals;
import org.rapidoid.http.HttpNotFoundException;
import org.rapidoid.io.CustomizableClassLoader;
import org.rapidoid.pages.Pages;
import org.rapidoid.rest.WebReq;
import org.rapidoid.util.U;
import org.rapidoid.util.UTILS;

@Authors("Nikolche Mihajlovski")
@Since("2.0.0")
public class AppHandler implements Handler {

	private CustomizableClassLoader classLoader;

	public AppHandler() {
		this(null);
	}

	public AppHandler(CustomizableClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public Object handle(final HttpExchange x) throws Exception {

		HttpExchangeInternals xi = (HttpExchangeInternals) x;
		xi.setClassLoader(classLoader);

		Object result;

		try {
			result = processReq(x);
		} catch (Exception e) {
			if (UTILS.rootCause(e) instanceof HttpNotFoundException) {
				throw U.rte(e);
			} else {
				// Log.error("Exception occured while processing request!", UTILS.rootCause(e));
				throw U.rte(e);
			}
		}

		return result;

	}

	public Object processReq(HttpExchange x) {
		HttpExchangeInternals xi = (HttpExchangeInternals) x;

		final AppClasses appCls = Apps.getAppClasses(x, xi.getClassLoader());

		Object result = dispatch(x, appCls);

		if (result != null) {
			return result;
		} else {
			throw x.notFound();
		}
	}

	public Object dispatch(HttpExchange x, AppClasses appCls) {
		HttpExchangeInternals xi = (HttpExchangeInternals) x;

		// static files
		if (x.isGetReq() && x.serveStaticFile()) {
			return x;
		}

		// REST services
		if (appCls.dispatcher != null) {
			try {
				return appCls.dispatcher.dispatch(new WebReq(x));
			} catch (PojoHandlerNotFoundException e) {
				// / just ignore, will try to dispatch a page next...
			} catch (PojoDispatchException e) {
				return x.error(e);
			}
		}

		// Prepare GUI state

		xi.loadState();

		// Instantiate main app object (if possible)

		Object app = Apps.instantiate(appCls.main, x);

		// GUI pages from file resources

		if (Pages.serveFromFile(x, app)) {
			return x;
		}

		// GUI pages from Java components

		Object result = Pages.dispatchIfExists(x, appCls.pages, app);
		if (result != null) {
			return result;
		}

		// App screens

		if (!U.isEmpty(appCls.screens) || appCls.main != null) {
			Object genericPage = new AppPageGeneric(x, appCls, app);
			return Pages.dispatch(x, genericPage);
		}

		throw x.notFound();
	}
}

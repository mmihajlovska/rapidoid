package org.rapidoid.http;

/*
 * #%L
 * rapidoid-http
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
import org.rapidoid.buffer.Buf;
import org.rapidoid.bytes.BytesUtil;
import org.rapidoid.data.Range;
import org.rapidoid.log.Log;
import org.rapidoid.util.SimpleHashTable;
import org.rapidoid.util.SimpleList;
import org.rapidoid.util.U;

@Authors("Nikolche Mihajlovski")
@Since("2.0.0")
public class HttpRouter implements Router {

	private class Route {
		Handler handler;
		byte[] action;
		byte[] path;

		@Override
		public String toString() {
			return new String(action) + ":" + new String(path);
		}
	}

	private final SimpleHashTable<Route> routes = new SimpleHashTable<Route>(10000);

	private Handler genericHandler;

	@Override
	public synchronized void generic(Handler handler) {
		this.genericHandler = handler;
	}

	private synchronized void addRoute(String action, String path, Handler handler) {
		assert action.length() >= 1;
		assert path.length() >= 1;

		Route route = new Route();
		route.handler = handler;
		route.action = action.getBytes();
		route.path = path.getBytes();

		long hash = hash(action, path);

		routes.put(hash, route);
	}

	private long hash(String action, String path) {
		return action.charAt(0) * 17 + action.length() * 19 + path.charAt(0);
	}

	private long hash(Buf buf, Range action, Range path) {
		return buf.get(action.start) * 17 + action.length * 19 + buf.get(path.start);
	}

	@Override
	public void dispatch(HttpExchangeImpl x) {
		Handler handler = findHandler(x);
		handle(handler, x);
	}

	private Handler findHandler(HttpExchangeImpl x) {

		Buf buf = x.input();
		Range action = x.verb_().range();
		Range path = x.path_().range();

		// serveStaticFileIfExists(x, buf, path);

		long hash = hash(buf, action, path);

		synchronized (this) {

			SimpleList<Route> candidates = routes.get(hash);
			candidates = routes.get(hash);

			if (candidates != null) {

				for (int i = 0; i < candidates.size(); i++) {
					Route route = candidates.get(i);

					if (BytesUtil.matches(buf.bytes(), action, route.action, true)
							&& BytesUtil.startsWith(buf.bytes(), path, route.path, true)) {

						int pos = path.start + route.path.length;
						if (path.limit() == pos || buf.get(pos) == '/') {
							x.setSubpath(pos, path.limit());
							return route.handler;
						}
					}
				}
			}

			if (genericHandler != null) {
				x.setSubpath(path.start, path.limit());
				return genericHandler;
			}
		}

		throw x.notFound();
	}

	@SuppressWarnings("unused")
	private void serveStaticFileIfExists(HttpExchangeImpl x, Buf buf, Range path) {
		if (x.isGetReq() && BytesUtil.find(buf.bytes(), path.start + 1, path.limit(), (byte) '.', true) >= 0) {
			if (x.serveStaticFile()) {
				return;
			}
		}
	}

	private void handle(Handler handler, HttpExchangeImpl x) {
		Object res;
		try {
			res = handler.handle(x);
		} catch (Exception e) {
			throw U.rte(e);
		}

		HttpProtocol.processResponse(x, res);
	}

	@Override
	public HttpRouter route(String action, String url, Handler handler) {
		// verbs are case-sensitive, forcing uppercase convention
		if (!action.matches("[A-Z_][A-Z0-9_]*")) {
			throw new IllegalArgumentException(
					"Only uppercase letters, digits and underscore are allowed! Invalid action: " + action);
		}

		if (!url.matches("[a-zA-Z0-9_/\\.\\-\\~]*")) {
			throw new IllegalArgumentException("Invalid url: " + url);
		}

		url = U.trimr(url, "/");

		if (!url.startsWith("/")) {
			url = "/" + url;
		}

		Log.info("Registering handler", "action", action, "url", url);

		addRoute(action, url, handler);

		return this;
	}

	@Override
	public HttpRouter route(String cmd, String url, String response) {
		route(cmd, url, contentHandler(response));
		return this;
	}

	@Override
	public HttpRouter serve(Handler handler) {
		generic(handler);
		return this;
	}

	@Override
	public HttpRouter serve(String response) {
		return serve(contentHandler(response));
	}

	@Override
	public HttpRouter get(String url, Handler handler) {
		return route("GET", url, handler);
	}

	@Override
	public HttpRouter post(String url, Handler handler) {
		return route("POST", url, handler);
	}

	@Override
	public HttpRouter put(String url, Handler handler) {
		return route("PUT", url, handler);
	}

	@Override
	public HttpRouter delete(String url, Handler handler) {
		return route("DELETE", url, handler);
	}

	private static Handler contentHandler(String response) {
		final byte[] bytes = response.getBytes();

		return new Handler() {
			@Override
			public Object handle(HttpExchange x) {
				x.html();
				return bytes;
			}
		};
	}

}

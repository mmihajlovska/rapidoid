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
import org.rapidoid.appctx.Applications;
import org.rapidoid.http.session.InMemorySessionStore;
import org.rapidoid.http.session.SessionStore;
import org.rapidoid.json.JSON;
import org.rapidoid.net.Protocol;
import org.rapidoid.net.impl.RapidoidServerLoop;

@Authors("Nikolche Mihajlovski")
@Since("2.0.0")
public class HTTPServerImpl extends RapidoidServerLoop implements HTTPServer {

	private final SessionStore session = new InMemorySessionStore();

	Applications applications = Applications.main();

	public HTTPServerImpl() {
		super(new HttpProtocol(), HttpExchangeImpl.class, null);
		((HttpProtocol) protocol).setServer(this);
		((HttpProtocol) protocol).setSessionStore(session);
	}

	@Override
	public HTTPServer start() {
		super.start();
		JSON.warmup();
		return this;
	}

	@Override
	public HTTPServer shutdown() {
		super.shutdown();
		return this;
	}

	@Override
	public HTTPInterceptor interceptor() {
		return ((HttpProtocol) protocol).getInterceptor();
	}

	@Override
	public HTTPServer interceptor(HTTPInterceptor interceptor) {
		((HttpProtocol) protocol).setInterceptor(interceptor);
		return this;
	}

	@Override
	public HTTPServer addUpgrade(String upgradeName, HttpUpgradeHandler upgradeHandler, Protocol upgradeProtocol) {
		((HttpProtocol) protocol).addUpgrade(upgradeName, upgradeHandler, upgradeProtocol);
		return this;
	}

	@Override
	public String toString() {
		return "HTTPServerImpl [session=" + session + ", applications=" + applications + "]";
	}

}

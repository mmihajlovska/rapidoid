package org.rapidoid.plugins.sms;

import org.rapidoid.annotation.Authors;
import org.rapidoid.annotation.Since;
import org.rapidoid.concurrent.Callback;
import org.rapidoid.job.Jobs;
import org.rapidoid.log.Log;
import org.rapidoid.util.Constants;
import org.rapidoid.util.U;

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

@Authors("Nikolche Mihajlovski")
@Since("4.1.0")
public class DefaultSMSPlugin extends AbstractSMSPlugin {

	private static final String MAIL_DESC = "SMS plugin implementation hasn't been registered, so cannot send SMS:\n"
			+ "To numbers: %s\nContent: %s" + Constants.SEPARATOR_LINE;

	@Override
	public void send(Iterable<String> toNumbers, String content, Callback<Void> callback) {

		Log.error(U.format(MAIL_DESC, toNumbers, content));

		Jobs.callIfNotNull(callback, null,
				U.rte("SMS plugin implementation hasn't been registered, so cannot send SMS!"));
	}

}

package demo.taskplanner;

/*
 * #%L
 * rapidoid-demo
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

import java.util.concurrent.TimeUnit;

import org.rapidoid.annotation.Authors;
import org.rapidoid.annotation.Cookie;
import org.rapidoid.annotation.GET;
import org.rapidoid.annotation.Header;
import org.rapidoid.annotation.POST;
import org.rapidoid.annotation.RESTful;
import org.rapidoid.annotation.Since;
import org.rapidoid.annotation.Transaction;
import org.rapidoid.http.HttpExchange;
import org.rapidoid.job.Jobs;
import org.rapidoid.main.Rapidoid;
import org.rapidoid.plugins.db.DB;
import org.rapidoid.plugins.email.Email;
import org.rapidoid.plugins.sms.SMS;
import org.rapidoid.util.U;

import demo.taskplanner.model.Task;

@Authors("Nikolche Mihajlovski")
@Since("2.0.0")
@RESTful
public class Main {

	public static void main(String[] args) {
		SMS.send("+1234567890", "Hey!", null);

		Email.send("someone@somewhere.rapidoid.io", "Hey!",
				"Dear Someone,\n\nHow are you?\nThis is just a test e-mail!\n\nRegards,\nSomebody", null);

		Rapidoid.run(args, "oauth-no-state");

		for (int i = 0; i < 120; i++) {
			Jobs.schedule(new Runnable() {
				@Override
				public void run() {
					Task task = new Task();
					DB.insert(task);
				}
			}, 100, TimeUnit.MILLISECONDS);
		}
	}

	@Transaction
	@GET
	public void tx() {
		Task task = new Task();
		task.title = "DON'T GO TO THE DATABASE!";
		String id = DB.insert(task);
		DB.update(id, task);
		throw U.rte("some failure!");
	}

	@GET("/task/page")
	public Iterable<Task> tasks(int page) {
		return page < 5 ? DB.getAll(Task.class) : null;
	}

	@POST("/fileup")
	public Object uploaded(HttpExchange x, byte[][] files, @Cookie("JSESSIONID") String js,
			@Cookie("COOKIEPACK") String cp, @Header("Host") String host, @Header("Accept") String accept) {
		return U.array(js, cp, host, accept, files);
	}

}

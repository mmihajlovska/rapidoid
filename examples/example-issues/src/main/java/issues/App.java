package issues;

import org.rapidoid.app.Apps;

@Authors("Nikolche Mihajlovski")
@Since("2.0.0")
public class App {

	public String title = "Issue Management";

	public boolean full = true;

	public boolean fluid = false;

	public String theme = "1";

	public static void main(String[] args) {
		Apps.run(args);
	}

}

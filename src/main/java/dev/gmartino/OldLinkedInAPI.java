package dev.gmartino;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OldLinkedInAPI {
	public static void main(String[] args) throws IOException {
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();

		String jsonString = Files.readString(
				Path.of(Objects.requireNonNull(classloader.getResource(
								"source/linkedin_api_all.json"))
						.getPath()), Charset.defaultCharset());
		HashSet<String> set = new HashSet<>();
		HashSet<String> finalSet = new HashSet<>();
		Pattern pattern = Pattern.compile("urn:li:fsd_company:[0-9]+\"");
		Matcher matcher = pattern.matcher(jsonString);
		while (matcher.find()) {
			set.add(matcher.group());
		}

		String link1 = " https://www.linkedin.com/jobs/search/?f_C=";
		String link2 = "&geoId=91000000&keywords=software%20engineer&location=European%20Union&origin=JOB_SEARCH_PAGE_JOB_FILTER&refresh=true";

		set.forEach(s -> finalSet.add(s.substring(19, s.length() - 1)));
		finalSet.forEach(System.out::println);

		String joined = String.join("%2C", finalSet);
		System.out.println(joined);
		System.out.println(link1 + joined + link2);
	}

}

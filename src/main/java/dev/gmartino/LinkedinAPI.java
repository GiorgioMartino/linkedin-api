package dev.gmartino;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.json.JSONArray;
import org.json.JSONObject;

public class LinkedinAPI {

	private static Map<String, String> finalMap = new HashMap<>();

	public static void main(String[] args) throws IOException {
		// Load JSON from file
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		String jsonString = Files.readString(
				Path.of(Objects.requireNonNull(classloader.getResource(
								"source/linkedin_api_all.json"))
						.getPath()), Charset.defaultCharset());

		JSONArray array = new JSONArray(jsonString);
		for (int k = 0; k < array.length(); k++) {
			JSONObject obj = array.getJSONObject(k);

			JSONObject data = obj.getJSONObject("data");
			JSONObject data1 = data.getJSONObject("data");

			if (data1.has("identityDashProfileComponentsByPagedListComponent")) {
				JSONObject identityDash = data1.getJSONObject(
						"identityDashProfileComponentsByPagedListComponent");
				handlePaginated(identityDash);
			} else
				handleV1(obj);
		}

		directIDSearch(jsonString);

		generateOutput(finalMap.keySet());

		writeToCSV();

		System.out.println("---------------- SUMMARY ----------------");
		System.out.println("Found " + finalMap.size() + " companies");
	}

	private static void writeToCSV() throws IOException {
		CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader("ID", "Company").build();

		try (final CSVPrinter printer = new CSVPrinter(new FileWriter("output/id_company.csv"), csvFormat)) {
			finalMap.forEach((id, company) -> {
				try {
					printer.printRecord(id, company);
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		}
	}

	private static void generateOutput(Set<String> ids) {
		String link1 = " https://www.linkedin.com/jobs/search/?f_C=";
		String link2 = "&geoId=91000000&keywords=software%20engineer&location=European%20Union&origin=JOB_SEARCH_PAGE_JOB_FILTER&refresh=true";

		String joined = String.join("%2C", ids);
		System.out.println("---------------- JOINED PARAMS ----------------");
		System.out.println(joined);
		System.out.println("---------------- LINK ----------------");
		System.out.println(link1 + joined + link2);

	}

	private static void directIDSearch(String jsonString) {
		HashSet<String> set = new HashSet<>();
		HashSet<String> finalSet = new HashSet<>();

		Pattern pattern = Pattern.compile("urn:li:fsd_company:[0-9]+\"");
		Matcher matcher = pattern.matcher(jsonString);
		while (matcher.find()) {
			set.add(matcher.group());
		}
		set.forEach(s -> finalSet.add(s.substring(19, s.length() - 1)));

		System.out.println("---------------- Not found IDs ----------------");
		finalSet.forEach(id -> {
			if (!finalMap.containsKey(id))
				System.out.println(id);
		});
	}

	private static void handleV1(JSONObject obj) {
		//		System.out.println("********************** Handling v1");
		JSONArray included = obj.getJSONArray("included");
		for (int i = 0; i < included.length(); i++) {
			JSONObject object = included.getJSONObject(i);
			if (object.has("components")) {
				JSONObject components = object.getJSONObject("components");
				JSONArray elements = components.getJSONArray("elements");
				for (int j = 0; j < elements.length(); j++) {
					try {
						findCompany(elements, j);
					} catch (Exception e) {
					}
				}
			}
		}
	}

	private static void handlePaginated(JSONObject identityDash) {
		//		System.out.println("********************** Handling Paginated");
		JSONArray elements = identityDash.getJSONArray("elements");
		for (int i = 0; i < elements.length(); i++) {
			findCompany(elements, i);
		}
	}

	private static void findCompany(JSONArray elements, int j) {
		//		System.out.println("Searching company " + j);
		try {
			JSONObject element = elements.getJSONObject(j);
			JSONObject elemComp = element.getJSONObject("components");
			JSONObject entityComponent = elemComp.getJSONObject("entityComponent");
			JSONObject titleV2 = entityComponent.getJSONObject("titleV2");
			JSONObject text = titleV2.getJSONObject("text");
			JSONObject attributesV2 = text.getJSONArray("attributesV2").getJSONObject(0);
			JSONObject detailData = attributesV2.getJSONObject("detailData");
			JSONObject stringFieldReference = detailData.getJSONObject("stringFieldReference");
			String urn = stringFieldReference.getString("urn");
			String id = urn.substring(19);
			String value = stringFieldReference.getString("value");
			//			System.out.println("Found company " + id + " with value " + value);
			finalMap.put(id, value);
		} catch (Exception e) {
			//			System.out.println("EEEEEEEEEEEEEEEEEEEEEEEE Error searching company " + j);
			//			System.out.println(e.getMessage());
		}
	}

}
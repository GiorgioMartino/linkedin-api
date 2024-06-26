package dev.gmartino;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LinkedinAPI {

	private static HashSet<String>     finalSet = new HashSet<>();
	private static Map<String, String> finalMap = new HashMap<>();

	public static void main(String[] args) throws IOException {
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		String jsonString = Files.readString(
				Path.of(Objects.requireNonNull(classloader.getResource("linkedin_api_all.json"))
						.getPath()), Charset.defaultCharset());

		HashSet<String> set = new HashSet<>();

		Pattern pattern = Pattern.compile("urn:li:fsd_company:[0-9]+\"");
		Matcher matcher = pattern.matcher(jsonString);
		while (matcher.find()) {
			set.add(matcher.group());
		}
		set.forEach(s -> finalSet.add(s.substring(19, s.length() - 1)));

		JSONArray array = new JSONArray(jsonString);
		for (int k = 0; k < array.length(); k++) {
			JSONObject obj = array.getJSONObject(k);

			JSONObject data = obj.getJSONObject("data");
			JSONObject data1 = data.getJSONObject("data");

			try {
				JSONObject identityDash = data1.getJSONObject(
						"identityDashProfileComponentsByPagedListComponent");
				handlePaginated(identityDash);
			} catch (JSONException e) {
				handle(obj);
			}
		}

		finalSet.forEach(id -> {
			if (!finalMap.containsKey(id))
				System.out.println(id);
		});
	}

	private static void handle(JSONObject obj) {
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
		JSONArray elements = identityDash.getJSONArray("elements");
		for (int i = 0; i < elements.length(); i++) {
			findCompany(elements, i);
		}
	}

	private static void findCompany(JSONArray elements, int j) {
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
		finalMap.put(id, value);
	}

}
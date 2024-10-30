package dev.gmartino;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LinkedinAPI {

    private static final Map<String, String> companiesMap = new HashMap<>();

    public static void main(String[] args) throws IOException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();

        String dir = "single_files/v2_30_oct_24/";
        String fullDir = Objects.requireNonNull(classloader.getResource(dir)).getPath();

        Set<String> files = Stream.of(Objects.requireNonNull(new File(fullDir).listFiles()))
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));

//        files.forEach(System.out::println);

        files.forEach(file -> {
            try {
                loadJSONFromFile(classloader, dir.concat(file));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        extractInfo(classloader);
    }

    private static void loadJSONFromFile(ClassLoader classloader, String fileName) throws IOException {
        // Load JSON from file
        String jsonString = Files.readString(
                Path.of(Objects.requireNonNull(classloader.getResource(
                                fileName))
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

        directIDSearch(jsonString, fileName);
    }

    private static void extractInfo(ClassLoader classloader) throws IOException {
        Map<String, String> finalCompanies = removeExcludedCompanies(classloader, "exclude_companies/exclude.csv");
        Map<String, String> finalCompaniesStrict = removeExcludedCompanies(classloader, "exclude_companies/exclude_strict.csv");

        generateOutput(finalCompanies.keySet(), "normal");
        generateOutput(finalCompaniesStrict.keySet(), "strict");

        writeToCSV();

        System.out.println("---------------- SUMMARY ----------------");
        System.out.println("Found " + companiesMap.size() + " companies");
        System.out.println("Kept " + finalCompanies.size() + " companies for normal");
        System.out.println("Kept " + finalCompaniesStrict.size() + " companies for strict");
    }

    private static Map<String, String> removeExcludedCompanies(ClassLoader classloader, String fileName) throws IOException {
        Reader in = new FileReader(Objects.requireNonNull(classloader.getResource(fileName)).getPath());

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader("Company")
                .setSkipHeaderRecord(true)
                .build();

        Iterable<CSVRecord> records = csvFormat.parse(in);
        List<String> companiesToExclude = new ArrayList<>();

//        System.out.println("---------------- Excluding companies ----------------");
        for (CSVRecord record : records) {
            String company = record.get("Company");
            companiesToExclude.add(company);
//            System.out.println(company);
        }
//        System.out.println("Excluded " + companiesToExclude.size() + " companies");

        Map<String, String> finalCompanies = new HashMap<>();

        companiesMap.forEach((id, company) -> {
            if (!companiesToExclude.contains(company)) {
                finalCompanies.put(id, company);
            }
        });

        return finalCompanies;
    }

    private static void writeToCSV() throws IOException {
        if (!Files.exists(Path.of("output/")))
            Files.createDirectory(Path.of("output/"));

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader("ID", "Company").build();

        try (final CSVPrinter printer = new CSVPrinter(new FileWriter("output/id_company.csv"), csvFormat)) {
            companiesMap.forEach((id, company) -> {
                try {
                    printer.printRecord(id, company);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private static void generateOutput(Set<String> ids, String type) {
        String link1 = " https://www.linkedin.com/jobs/search/?f_C=";
        String link2 = "&geoId=91000000&keywords=software%20engineer&location=European%20Union&origin=JOB_SEARCH_PAGE_JOB_FILTER&refresh=true";

        String joined = String.join("%2C", ids);
        System.out.println("---------------- " + type + " JOINED PARAMS ----------------");
        System.out.println(joined);
        System.out.println("----------------" + type + " LINK ----------------");
        System.out.println(link1 + joined + link2);

    }

    private static void directIDSearch(String jsonString, String fileName) {
        HashSet<String> set = new HashSet<>();
        HashSet<String> finalSet = new HashSet<>();

        Pattern pattern = Pattern.compile("urn:li:fsd_company:[0-9]+\"");
        Matcher matcher = pattern.matcher(jsonString);
        while (matcher.find()) {
            set.add(matcher.group());
        }
        set.forEach(s -> finalSet.add(s.substring(19, s.length() - 1)));

        System.out.println(String.format("---------------- Not found IDs in %s ----------------", fileName));
        finalSet.forEach(id -> {
            if (!companiesMap.containsKey(id))
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
                        System.out.println(e.getMessage());
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
        JSONObject text = null;
        try {
            JSONObject element = elements.getJSONObject(j);
            JSONObject elemComp = element.getJSONObject("components");
            JSONObject entityComponent = elemComp.getJSONObject("entityComponent");
            JSONObject titleV2 = entityComponent.getJSONObject("titleV2");
            text = titleV2.getJSONObject("text");
            JSONObject attributesV2 = text.getJSONArray("attributesV2").getJSONObject(0);
            JSONObject detailData = attributesV2.getJSONObject("detailData");
            JSONObject stringFieldReference = detailData.getJSONObject("stringFieldReference");
            String urn = stringFieldReference.getString("urn");
            String id = urn.substring(19);
            String value = stringFieldReference.getString("value");
            //			System.out.println("Found company " + id + " with value " + value);
            companiesMap.put(id, value);
        } catch (Exception e) {
            System.out.println("EEEEEEEEEEEEEEEEEEEEEEEE Error searching company " + j);
            System.out.println(e.getMessage());
            System.out.println(text);
        }
    }

}
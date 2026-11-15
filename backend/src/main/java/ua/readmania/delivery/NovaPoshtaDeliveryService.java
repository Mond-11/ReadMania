package ua.readmania.delivery;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import ua.readmania.delivery.dto.DeliveryCityResponse;
import ua.readmania.delivery.dto.DeliveryWarehouseResponse;

@Service
public class NovaPoshtaDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(NovaPoshtaDeliveryService.class);

    private final String apiUrl;
    private final String apiKey;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private final Map<String, List<DeliveryCityResponse>> citiesCache = new ConcurrentHashMap<>();
    private final Map<String, List<DeliveryWarehouseResponse>> warehousesCache = new ConcurrentHashMap<>();

    private static final List<DeliveryCityResponse> FALLBACK_CITIES = new ArrayList<>();
    private static final Map<String, List<DeliveryWarehouseResponse>> FALLBACK_WAREHOUSES = new LinkedHashMap<>();

    static {
        initFallbackData();
    }

    public NovaPoshtaDeliveryService(
            @Value("${app.novaposhta.api-url:https://api.novaposhta.ua/v2.0/json/}") String apiUrl,
            @Value("${app.novaposhta.api-key:}") String apiKey,
            @Value("${app.novaposhta.timeout-ms:3000}") long timeoutMs,
            ObjectMapper objectMapper) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));

        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public List<DeliveryCityResponse> searchCities(String query) {
        String normalizedQuery = query == null ? "" : query.trim();
        String cacheKey = normalizedQuery.toLowerCase(Locale.ROOT);

        List<DeliveryCityResponse> cached = citiesCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<DeliveryCityResponse> result;
        if (normalizedQuery.isBlank()) {
            result = new ArrayList<>(FALLBACK_CITIES);
        } else {
            result = fetchCitiesFromApi(normalizedQuery);
            if (result == null || result.isEmpty()) {
                log.debug("Falling back to local dictionary for cities query='{}'", normalizedQuery);
                result = getFallbackCities(normalizedQuery);
            }
        }

        citiesCache.put(cacheKey, result);
        return result;
    }

    public List<DeliveryWarehouseResponse> getWarehouses(String cityRef, String query) {
        if (cityRef == null || cityRef.isBlank()) {
            return Collections.emptyList();
        }

        String normalizedCityRef = cityRef.trim();
        String normalizedQuery = query == null ? "" : query.trim();
        String cacheKey = normalizedCityRef + ":" + normalizedQuery.toLowerCase(Locale.ROOT);

        List<DeliveryWarehouseResponse> cached = warehousesCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<DeliveryWarehouseResponse> result = fetchWarehousesFromApi(normalizedCityRef, normalizedQuery);
        if (result == null || result.isEmpty()) {
            log.debug("Falling back to local dictionary for warehouses cityRef='{}', query='{}'", normalizedCityRef, normalizedQuery);
            result = getFallbackWarehouses(normalizedCityRef, normalizedQuery);
        }

        warehousesCache.put(cacheKey, result);
        return result;
    }

    private List<DeliveryCityResponse> fetchCitiesFromApi(String query) {
        try {
            Map<String, Object> methodProperties = new HashMap<>();
            if (!query.isBlank()) {
                methodProperties.put("FindByString", query);
            }
            methodProperties.put("Limit", "20");

            Map<String, Object> requestBody = Map.of(
                    "apiKey", apiKey != null ? apiKey : "",
                    "modelName", "Address",
                    "calledMethod", "getCities",
                    "methodProperties", methodProperties
            );

            String responseBody = restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                return Collections.emptyList();
            }

            JsonNode rootNode = objectMapper.readTree(responseBody);
            return parseCities(rootNode);
        } catch (Exception e) {
            log.warn("Nova Poshta API getCities failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<DeliveryWarehouseResponse> fetchWarehousesFromApi(String cityRef, String query) {
        try {
            Map<String, Object> methodProperties = new HashMap<>();
            methodProperties.put("CityRef", cityRef);
            if (!query.isBlank()) {
                methodProperties.put("FindByString", query);
            }
            methodProperties.put("Limit", "50");

            Map<String, Object> requestBody = Map.of(
                    "apiKey", apiKey != null ? apiKey : "",
                    "modelName", "Address",
                    "calledMethod", "getWarehouses",
                    "methodProperties", methodProperties
            );

            String responseBody = restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                return Collections.emptyList();
            }

            JsonNode rootNode = objectMapper.readTree(responseBody);
            return parseWarehouses(rootNode);
        } catch (Exception e) {
            log.warn("Nova Poshta API getWarehouses failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<DeliveryCityResponse> parseCities(JsonNode rootNode) {
        if (rootNode == null || !rootNode.path("success").asBoolean(false)) {
            return Collections.emptyList();
        }
        JsonNode dataNode = rootNode.path("data");
        if (!dataNode.isArray() || dataNode.isEmpty()) {
            return Collections.emptyList();
        }
        List<DeliveryCityResponse> list = new ArrayList<>();
        for (JsonNode item : dataNode) {
            String ref = item.path("Ref").asText("");
            String name = item.path("Description").asText("");
            String area = item.path("AreaDescription").asText("");
            if (!area.isBlank() && !area.toLowerCase(Locale.ROOT).contains("область")) {
                area = area + " область";
            }
            String displayName;
            if (area.isBlank() || area.toLowerCase(Locale.ROOT).startsWith(name.toLowerCase(Locale.ROOT))) {
                displayName = "м. " + name;
            } else {
                String shortArea = area.replace(" область", " обл.");
                displayName = "м. " + name + " (" + shortArea + ")";
            }
            list.add(new DeliveryCityResponse(ref, name, area, displayName));
        }
        return list;
    }

    private List<DeliveryWarehouseResponse> parseWarehouses(JsonNode rootNode) {
        if (rootNode == null || !rootNode.path("success").asBoolean(false)) {
            return Collections.emptyList();
        }
        JsonNode dataNode = rootNode.path("data");
        if (!dataNode.isArray() || dataNode.isEmpty()) {
            return Collections.emptyList();
        }
        List<DeliveryWarehouseResponse> list = new ArrayList<>();
        for (JsonNode item : dataNode) {
            String ref = item.path("Ref").asText("");
            String number = item.path("Number").asText("");
            String label = item.path("Description").asText("");
            String cityDescription = item.path("CityDescription").asText("");

            String formattedAddress;
            if (!cityDescription.isBlank() && !label.toLowerCase(Locale.ROOT).contains(cityDescription.toLowerCase(Locale.ROOT))) {
                formattedAddress = "м. " + cityDescription + ", " + label;
            } else {
                formattedAddress = label;
            }

            list.add(new DeliveryWarehouseResponse(ref, number, label, formattedAddress));
        }
        return list;
    }

    private List<DeliveryCityResponse> getFallbackCities(String query) {
        if (query.isBlank()) {
            return new ArrayList<>(FALLBACK_CITIES);
        }
        String q = query.toLowerCase(Locale.ROOT);
        return FALLBACK_CITIES.stream()
                .filter(c -> c.getName().toLowerCase(Locale.ROOT).contains(q)
                        || c.getArea().toLowerCase(Locale.ROOT).contains(q)
                        || c.getDisplayName().toLowerCase(Locale.ROOT).contains(q))
                .toList();
    }

    private List<DeliveryWarehouseResponse> getFallbackWarehouses(String cityRef, String query) {
        List<DeliveryWarehouseResponse> warehouses = FALLBACK_WAREHOUSES.get(cityRef);
        if (warehouses == null) {
            for (DeliveryCityResponse city : FALLBACK_CITIES) {
                if (city.getRef().equalsIgnoreCase(cityRef) || city.getName().equalsIgnoreCase(cityRef)) {
                    warehouses = FALLBACK_WAREHOUSES.get(city.getRef());
                    break;
                }
            }
        }

        if (warehouses == null) {
            return Collections.emptyList();
        }

        if (query.isBlank()) {
            return new ArrayList<>(warehouses);
        }

        String q = query.toLowerCase(Locale.ROOT);
        return warehouses.stream()
                .filter(w -> w.getNumber().toLowerCase(Locale.ROOT).contains(q)
                        || w.getLabel().toLowerCase(Locale.ROOT).contains(q)
                        || w.getFormattedAddress().toLowerCase(Locale.ROOT).contains(q))
                .toList();
    }

    private static void initFallbackData() {
        // 1. Київ
        String kyivRef = "8d5a980d-391c-11dd-90d9-001a92567626";
        FALLBACK_CITIES.add(new DeliveryCityResponse(kyivRef, "Київ", "Київська область", "м. Київ (Київська обл.)"));
        FALLBACK_WAREHOUSES.put(kyivRef, List.of(
                new DeliveryWarehouseResponse("e718c644-4b33-11e4-ab6d-005056801329", "1", "Відділення №1 (до 1100 кг): вул. Пирогівський шлях, 135", "м. Київ, Відділення №1: вул. Пирогівський шлях, 135"),
                new DeliveryWarehouseResponse("e718c652-4b33-11e4-ab6d-005056801329", "15", "Відділення №15 (до 30 кг): вул. Малинська, 20-А", "м. Київ, Відділення №15: вул. Малинська, 20-А"),
                new DeliveryWarehouseResponse("e718c660-4b33-11e4-ab6d-005056801329", "25", "Відділення №25 (до 30 кг): вул. Хрещатик, 22", "м. Київ, Відділення №25: вул. Хрещатик, 22"),
                new DeliveryWarehouseResponse("e718c670-4b33-11e4-ab6d-005056801329", "50", "Відділення №50 (до 30 кг): просп. Берестейський, 20", "м. Київ, Відділення №50: просп. Берестейський, 20"),
                new DeliveryWarehouseResponse("e718c680-4b33-11e4-ab6d-005056801329", "2042", "Поштомат №2042: вул. Велика Васильківська, 45", "м. Київ, Поштомат №2042: вул. Велика Васильківська, 45"),
                new DeliveryWarehouseResponse("e718c690-4b33-11e4-ab6d-005056801329", "3150", "Поштомат №3150: вул. Антоновича, 176 (ТРЦ Ocean Plaza)", "м. Київ, Поштомат №3150: вул. Антоновича, 176 (ТРЦ Ocean Plaza)")
        ));

        // 2. Львів
        String lvivRef = "db5c88f5-391c-11dd-90d9-001a92567626";
        FALLBACK_CITIES.add(new DeliveryCityResponse(lvivRef, "Львів", "Львівська область", "м. Львів (Львівська обл.)"));
        FALLBACK_WAREHOUSES.put(lvivRef, List.of(
                new DeliveryWarehouseResponse("e728c644-4b33-11e4-ab6d-005056801329", "1", "Відділення №1 (до 1100 кг): вул. Городоцька, 359", "м. Львів, Відділення №1: вул. Городоцька, 359"),
                new DeliveryWarehouseResponse("e728c652-4b33-11e4-ab6d-005056801329", "4", "Відділення №4 (до 30 кг): вул. Промислова, 50/52", "м. Львів, Відділення №4: вул. Промислова, 50/52"),
                new DeliveryWarehouseResponse("e728c660-4b33-11e4-ab6d-005056801329", "8", "Відділення №8 (до 30 кг): вул. Героїв УПА, 77", "м. Львів, Відділення №8: вул. Героїв УПА, 77"),
                new DeliveryWarehouseResponse("e728c670-4b33-11e4-ab6d-005056801329", "5401", "Поштомат №5401: вул. Стрийська, 30", "м. Львів, Поштомат №5401: вул. Стрийська, 30"),
                new DeliveryWarehouseResponse("e728c680-4b33-11e4-ab6d-005056801329", "6205", "Поштомат №6205: пл. Ринок, 10", "м. Львів, Поштомат №6205: пл. Ринок, 10")
        ));

        // 3. Дніпро
        String dniproRef = "db5c88f0-391c-11dd-90d9-001a92567626";
        FALLBACK_CITIES.add(new DeliveryCityResponse(dniproRef, "Дніпро", "Дніпропетровська область", "м. Дніпро (Дніпропетровська обл.)"));
        FALLBACK_WAREHOUSES.put(dniproRef, List.of(
                new DeliveryWarehouseResponse("e738c644-4b33-11e4-ab6d-005056801329", "1", "Відділення №1 (до 1100 кг): вул. Маршала Малиновського, 114", "м. Дніпро, Відділення №1: вул. Маршала Малиновського, 114"),
                new DeliveryWarehouseResponse("e738c652-4b33-11e4-ab6d-005056801329", "5", "Відділення №5 (до 30 кг): вул. Шевченка, 10", "м. Дніпро, Відділення №5: вул. Шевченка, 10"),
                new DeliveryWarehouseResponse("e738c660-4b33-11e4-ab6d-005056801329", "4102", "Поштомат №4102: просп. Дмитра Яворницького, 50", "м. Дніпро, Поштомат №4102: просп. Дмитра Яворницького, 50")
        ));

        // 4. Одеса
        String odesaRef = "db5c88d0-391c-11dd-90d9-001a92567626";
        FALLBACK_CITIES.add(new DeliveryCityResponse(odesaRef, "Одеса", "Одеська область", "м. Одеса (Одеська обл.)"));
        FALLBACK_WAREHOUSES.put(odesaRef, List.of(
                new DeliveryWarehouseResponse("e748c644-4b33-11e4-ab6d-005056801329", "1", "Відділення №1 (до 1100 кг): вул. Базова, 16", "м. Одеса, Відділення №1: вул. Базова, 16"),
                new DeliveryWarehouseResponse("e748c652-4b33-11e4-ab6d-005056801329", "10", "Відділення №10 (до 30 кг): вул. Дерибасівська, 14", "м. Одеса, Відділення №10: вул. Дерибасівська, 14"),
                new DeliveryWarehouseResponse("e748c660-4b33-11e4-ab6d-005056801329", "7011", "Поштомат №7011: вул. Рішельєвська, 28", "м. Одеса, Поштомат №7011: вул. Рішельєвська, 28")
        ));

        // 5. Харків
        String kharkivRef = "db5c88e0-391c-11dd-90d9-001a92567626";
        FALLBACK_CITIES.add(new DeliveryCityResponse(kharkivRef, "Харків", "Харківська область", "м. Харків (Харківська обл.)"));
        FALLBACK_WAREHOUSES.put(kharkivRef, List.of(
                new DeliveryWarehouseResponse("e758c644-4b33-11e4-ab6d-005056801329", "1", "Відділення №1 (до 1100 кг): просп. Гагаріна, 201-Б", "м. Харків, Відділення №1: просп. Гагаріна, 201-Б"),
                new DeliveryWarehouseResponse("e758c652-4b33-11e4-ab6d-005056801329", "6", "Відділення №6 (до 30 кг): вул. Академіка Павлова, 120", "м. Харків, Відділення №6: вул. Академіка Павлова, 120"),
                new DeliveryWarehouseResponse("e758c660-4b33-11e4-ab6d-005056801329", "8004", "Поштомат №8004: вул. Сумська, 39", "м. Харків, Поштомат №8004: вул. Сумська, 39")
        ));

        // 6. Вінниця
        String vinnytsiaRef = "db5c88c0-391c-11dd-90d9-001a92567626";
        FALLBACK_CITIES.add(new DeliveryCityResponse(vinnytsiaRef, "Вінниця", "Вінницька область", "м. Вінниця (Вінницька обл.)"));
        FALLBACK_WAREHOUSES.put(vinnytsiaRef, List.of(
                new DeliveryWarehouseResponse("e768c644-4b33-11e4-ab6d-005056801329", "1", "Відділення №1 (до 1100 кг): вул. Якова Шепеля, 1", "м. Вінниця, Відділення №1: вул. Якова Шепеля, 1"),
                new DeliveryWarehouseResponse("e768c652-4b33-11e4-ab6d-005056801329", "3", "Відділення №3 (до 30 кг): вул. Соборна, 89", "м. Вінниця, Відділення №3: вул. Соборна, 89"),
                new DeliveryWarehouseResponse("e768c660-4b33-11e4-ab6d-005056801329", "9101", "Поштомат №9101: вул. Келецька, 50", "м. Вінниця, Поштомат №9101: вул. Келецька, 50")
        ));

        // 7. Полтава
        String poltavaRef = "db5c88b0-391c-11dd-90d9-001a92567626";
        FALLBACK_CITIES.add(new DeliveryCityResponse(poltavaRef, "Полтава", "Полтавська область", "м. Полтава (Полтавська обл.)"));
        FALLBACK_WAREHOUSES.put(poltavaRef, List.of(
                new DeliveryWarehouseResponse("e778c644-4b33-11e4-ab6d-005056801329", "1", "Відділення №1 (до 1100 кг): вул. Європейська, 145", "м. Полтава, Відділення №1: вул. Європейська, 145"),
                new DeliveryWarehouseResponse("e778c652-4b33-11e4-ab6d-005056801329", "4", "Відділення №4 (до 30 кг): вул. Соборності, 46", "м. Полтава, Відділення №4: вул. Соборності, 46")
        ));

        // 8. Запоріжжя
        String zaporizhzhiaRef = "db5c88a0-391c-11dd-90d9-001a92567626";
        FALLBACK_CITIES.add(new DeliveryCityResponse(zaporizhzhiaRef, "Запоріжжя", "Запорізька область", "м. Запоріжжя (Запорізька обл.)"));
        FALLBACK_WAREHOUSES.put(zaporizhzhiaRef, List.of(
                new DeliveryWarehouseResponse("e788c644-4b33-11e4-ab6d-005056801329", "1", "Відділення №1 (до 1100 кг): вул. Брянська, 8", "м. Запоріжжя, Відділення №1: вул. Брянська, 8"),
                new DeliveryWarehouseResponse("e788c652-4b33-11e4-ab6d-005056801329", "7", "Відділення №7 (до 30 кг): просп. Соборний, 186", "м. Запоріжжя, Відділення №7: просп. Соборний, 186")
        ));

        // 9. Івано-Франківськ
        String ifRef = "db5c8890-391c-11dd-90d9-001a92567626";
        FALLBACK_CITIES.add(new DeliveryCityResponse(ifRef, "Івано-Франківськ", "Івано-Франківська область", "м. Івано-Франківськ (Івано-Франківська обл.)"));
        FALLBACK_WAREHOUSES.put(ifRef, List.of(
                new DeliveryWarehouseResponse("e798c644-4b33-11e4-ab6d-005056801329", "1", "Відділення №1 (до 1100 кг): вул. Максимовича, 8", "м. Івано-Франківськ, Відділення №1: вул. Максимовича, 8"),
                new DeliveryWarehouseResponse("e798c652-4b33-11e4-ab6d-005056801329", "3", "Відділення №3 (до 30 кг): вул. Незалежності, 36", "м. Івано-Франківськ, Відділення №3: вул. Незалежності, 36")
        ));

        // 10. Черкаси
        String cherkasyRef = "db5c8880-391c-11dd-90d9-001a92567626";
        FALLBACK_CITIES.add(new DeliveryCityResponse(cherkasyRef, "Черкаси", "Черкаська область", "м. Черкаси (Черкаська обл.)"));
        FALLBACK_WAREHOUSES.put(cherkasyRef, List.of(
                new DeliveryWarehouseResponse("e7a8c644-4b33-11e4-ab6d-005056801329", "1", "Відділення №1 (до 1100 кг): вул. Хоменка, 15", "м. Черкаси, Відділення №1: вул. Хоменка, 15"),
                new DeliveryWarehouseResponse("e7a8c652-4b33-11e4-ab6d-005056801329", "2", "Відділення №2 (до 30 кг): бул. Шевченка, 150", "м. Черкаси, Відділення №2: бул. Шевченка, 150")
        ));
    }
}

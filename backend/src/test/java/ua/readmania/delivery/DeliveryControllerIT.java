package ua.readmania.delivery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ua.readmania.BaseIntegrationTest;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class DeliveryControllerIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Отримання списку міст без параметрів запиту повертає топ-міста")
    void shouldReturnTopCitiesWhenNoQuery() throws Exception {
        mockMvc.perform(get("/api/delivery/cities")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)))
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(5)))
                .andExpect(jsonPath("$[?(@.name == 'Київ')].ref").exists())
                .andExpect(jsonPath("$[?(@.name == 'Київ')].displayName").value("м. Київ (Київська обл.)"))
                .andExpect(jsonPath("$[?(@.name == 'Львів')].ref").exists());
    }

    @Test
    @DisplayName("Пошук міст за підрядком query повертає відфільтровані міста")
    void shouldFilterCitiesByQuery() throws Exception {
        mockMvc.perform(get("/api/delivery/cities")
                        .param("query", "Київ")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].name", is("Київ")))
                .andExpect(jsonPath("$[0].area", containsString("Київська")))
                .andExpect(jsonPath("$[0].ref", is("8d5a980d-391c-11dd-90d9-001a92567626")));
    }

    @Test
    @DisplayName("Отримання списку відділень для дійсного cityRef повертає відділення та поштомати")
    void shouldReturnWarehousesForCityRef() throws Exception {
        String kyivRef = "8d5a980d-391c-11dd-90d9-001a92567626";

        mockMvc.perform(get("/api/delivery/warehouses")
                        .param("cityRef", kyivRef)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)))
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$[0].ref").isNotEmpty())
                .andExpect(jsonPath("$[0].number").isNotEmpty())
                .andExpect(jsonPath("$[0].label").isNotEmpty())
                .andExpect(jsonPath("$[0].formattedAddress").isNotEmpty());
    }

    @Test
    @DisplayName("Фільтрація відділень за номером чи описом")
    void shouldFilterWarehousesByQuery() throws Exception {
        String kyivRef = "8d5a980d-391c-11dd-90d9-001a92567626";

        mockMvc.perform(get("/api/delivery/warehouses")
                        .param("cityRef", kyivRef)
                        .param("query", "15")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].number", is("15")))
                .andExpect(jsonPath("$[0].label", containsString("№15")));
    }

    @Test
    @DisplayName("Запит відділень без обов'язкового параметра cityRef повертає 400 Bad Request")
    void shouldReturn400WhenCityRefIsMissing() throws Exception {
        mockMvc.perform(get("/api/delivery/warehouses")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}

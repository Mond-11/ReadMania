package ua.readmania.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ua.readmania.delivery.NovaPoshtaDeliveryService;
import ua.readmania.delivery.dto.DeliveryCityResponse;
import ua.readmania.delivery.dto.DeliveryWarehouseResponse;

@RestController
@RequestMapping("/api/delivery")
public class DeliveryController {

    private final NovaPoshtaDeliveryService deliveryService;

    public DeliveryController(NovaPoshtaDeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping("/cities")
    public ResponseEntity<List<DeliveryCityResponse>> getCities(
            @RequestParam(name = "query", required = false, defaultValue = "") String query) {
        List<DeliveryCityResponse> cities = deliveryService.searchCities(query);
        return ResponseEntity.ok(cities);
    }

    @GetMapping("/warehouses")
    public ResponseEntity<List<DeliveryWarehouseResponse>> getWarehouses(
            @RequestParam(name = "cityRef") String cityRef,
            @RequestParam(name = "query", required = false, defaultValue = "") String query) {
        List<DeliveryWarehouseResponse> warehouses = deliveryService.getWarehouses(cityRef, query);
        return ResponseEntity.ok(warehouses);
    }
}

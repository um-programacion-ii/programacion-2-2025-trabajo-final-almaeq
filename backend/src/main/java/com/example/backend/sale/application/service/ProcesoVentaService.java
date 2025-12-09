package com.example.backend.sale.application.service;

import com.example.backend.event.infrastructure.persistence.entity.Event;
import com.example.backend.event.infrastructure.persistence.repository.EventRepository;
import com.example.backend.sale.infrastructure.client.CatedraApiClient;
import com.example.backend.sale.infrastructure.persistence.entity.Sale;
import com.example.backend.sale.infrastructure.persistence.repository.SaleRepository;
import com.example.backend.sale.infrastructure.web.dto.BlockRequestDto;
import com.example.backend.proxy.application.service.ProxyClientService;
import com.example.backend.sale.infrastructure.web.dto.PersonDto;
import com.example.backend.sale.infrastructure.web.dto.SaleRequestDto;
import com.example.backend.seatSold.infrastructure.persistence.entity.SeatSold;
import com.example.backend.seatSold.infrastructure.persistence.repository.SeatSoldRepository;
import com.example.backend.user.infrastructure.persistence.entity.User;
import com.example.backend.user.infrastructure.persistence.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProcesoVentaService {

    private final CatedraApiClient catedraApiClient;
    private final ProxyClientService proxyClientService;
    private final EventRepository eventRepository;
    private final SaleRepository saleRepository;
    private final SeatSoldRepository seatSoldRepository;
    private final UserRepository userRepository;

    public ProcesoVentaService(CatedraApiClient catedraApiClient,
                               ProxyClientService proxyClientService,
                               EventRepository eventRepository,
                               SaleRepository saleRepository,
                               SeatSoldRepository seatSoldRepository,
                               UserRepository userRepository) {
        this.catedraApiClient = catedraApiClient;
        this.proxyClientService = proxyClientService;      // ⬅⬅ NUEVO
        this.eventRepository = eventRepository;
        this.saleRepository = saleRepository;
        this.seatSoldRepository = seatSoldRepository;
        this.userRepository = userRepository;
    }


    // CAMBIAR: De boolean a Map<String, Object>
    public Map<String, Object> bloquearAsientos(BlockRequestDto request) {
        if (request.getEventoId() == null) {
            return Map.of("resultado", false, "descripcion", "Error: eventoId es nulo");
        }

        if (!eventRepository.existsById(request.getEventoId())) {
            return Map.of("resultado", false, "descripcion", "Evento no encontrado localmente");
        }

        return proxyClientService.bloquearAsientos(request);
    }

    @Transactional
    public Map<String, Object> confirmarVenta(SaleRequestDto request, String username) {
        // 1. Obtener Entidades Locales
        Event evento = eventRepository.findById(request.getEventoId())
                .orElseThrow(() -> new RuntimeException("Evento no encontrado"));

        User usuario = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 2. Preparar Payload para Cátedra (Payload 7 del PDF)
        Map<String, Object> payloadCatedra = new HashMap<>();
        payloadCatedra.put("eventoId", evento.getId());
        payloadCatedra.put("fecha", LocalDateTime.now().toString());
        payloadCatedra.put("precioVenta", 1000.0); // Precio hardcodeado o sacado de la entidad Evento

        List<Map<String, Object>> listaAsientosCatedra = new ArrayList<>();

        // Asumimos que la lista de 'asientos' y 'personas' tienen el mismo orden y tamaño
        for (int i = 0; i < request.getAsientos().size(); i++) {
            String asientoStr = request.getAsientos().get(i); // Ej: "F1-C2"
            PersonDto persona = request.getPersonas().get(i);

            Map<String, Object> asientoMap = parsearAsiento(asientoStr);
            asientoMap.put("persona", persona.getNombre() + " " + persona.getApellido());

            listaAsientosCatedra.add(asientoMap);
        }
        payloadCatedra.put("asientos", listaAsientosCatedra);

        // 3. Llamar a la Cátedra
        Map<String, Object> respuestaCatedra = catedraApiClient.realizarVenta(payloadCatedra);

        boolean resultado = (boolean) respuestaCatedra.getOrDefault("resultado", false);

        // 4. Si es exitoso, guardar en Base de Datos Local
        if (resultado) {
            Sale venta = new Sale();
            venta.setEvento(evento);
            venta.setUser(usuario);
            venta.setFechaVenta(LocalDateTime.now());
            venta.setEstado("CONFIRMADA");

            // Guardamos la venta primero para tener ID
            Sale ventaGuardada = saleRepository.save(venta);

            // Guardamos los asientos vendidos
            for (int i = 0; i < request.getAsientos().size(); i++) {
                String ubicacion = request.getAsientos().get(i);
                PersonDto persona = request.getPersonas().get(i);

                SeatSold asientoVendido = new SeatSold();
                asientoVendido.setVenta(ventaGuardada);
                asientoVendido.setUbicacion(ubicacion);
                asientoVendido.setNombrePersona(persona.getNombre());
                asientoVendido.setApellidoPersona(persona.getApellido());

                seatSoldRepository.save(asientoVendido);
            }
        }

        return respuestaCatedra;
    }

    // Helper simple para parsear F1-C1
    private Map<String, Object> parsearAsiento(String asiento) {
        Map<String, Object> map = new HashMap<>();
        // Lógica de parseo: Ajustar según tu formato real
        // Ejemplo simple si viene "1-2" (fila-columna)
        // O si viene F1-C2
        try {
            // Implementa tu lógica de parseo aquí.
            // Por ejemplo, hardcodeado para prueba:
            map.put("fila", 1);
            map.put("columna", 1);
        } catch (Exception e) {
            map.put("fila", 0);
            map.put("columna", 0);
        }
        return map;
    }
}
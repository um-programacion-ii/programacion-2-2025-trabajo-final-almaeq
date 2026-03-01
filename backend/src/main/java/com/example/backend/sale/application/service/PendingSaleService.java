package com.example.backend.sale.application.service;

import com.example.backend.event.infrastructure.persistence.entity.Event;
import com.example.backend.event.infrastructure.persistence.repository.EventRepository;
import com.example.backend.sale.infrastructure.persistence.entity.Sale;
import com.example.backend.seatSold.infrastructure.persistence.entity.SeatSold;
import com.example.backend.sale.infrastructure.persistence.repository.SaleRepository;
import com.example.backend.seatSold.infrastructure.persistence.repository.SeatSoldRepository;
import com.example.backend.sale.infrastructure.web.dto.PersonDto;
import com.example.backend.sale.infrastructure.web.dto.SaleRequestDto;
import com.example.backend.sale.infrastructure.web.dto.SimpleSeatDto;
import com.example.backend.user.infrastructure.persistence.entity.User;
import com.example.backend.user.infrastructure.persistence.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class PendingSaleService {

    private static final String BACKUP_FILE = "pending_sales_backup.json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Repositorios necesarios para el reintento
    private final SaleRepository saleRepository;
    private final SeatSoldRepository seatSoldRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public PendingSaleService(SaleRepository saleRepository, SeatSoldRepository seatSoldRepository,
                              EventRepository eventRepository, UserRepository userRepository) {
        this.saleRepository = saleRepository;
        this.seatSoldRepository = seatSoldRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    // Clase auxiliar para guardar lo necesario en el JSON
    private static class PendingSale {
        public SaleRequestDto request;
        public String username;
        public PendingSale() {}
        public PendingSale(SaleRequestDto request, String username) {
            this.request = request;
            this.username = username;
        }
    }

    // 1. MÉTODO PARA ENCOLAR (Cuando falla MySQL)
    public void backupSale(SaleRequestDto request, String username) {
        System.err.println("!!! ALERTA: Guardando venta en respaldo local por fallo de BD.");
        try {
            List<PendingSale> pendingList = loadPendingSales();
            pendingList.add(new PendingSale(request, username));
            savePendingSales(pendingList);
        } catch (Exception e) {
            System.err.println("ERROR CRÍTICO: No se pudo crear respaldo de venta: " + e.getMessage());
        }
    }

    // 2. TAREA PROGRAMADA (Se ejecuta cada 60 segundos)
    @Scheduled(fixedDelay = 600000)
    public void processPendingSales() {
        List<PendingSale> pendingList = loadPendingSales();
        if (pendingList.isEmpty()) return;

        System.out.println("Reintentando guardar " + pendingList.size() + " ventas pendientes...");

        List<PendingSale> remaining = new ArrayList<>();

        for (PendingSale pending : pendingList) {
            boolean success = trySaveToDb(pending);
            if (!success) {
                remaining.add(pending); // Si falla de nuevo, la mantenemos
            }
        }

        // Actualizamos el archivo solo con las que sigan fallando
        if (remaining.size() != pendingList.size()) {
            savePendingSales(remaining);
            System.out.println("Reintento finalizado. Pendientes restantes: " + remaining.size());
        }
    }

    @Transactional
    protected boolean trySaveToDb(PendingSale pending) {
        try {
            Event evento = eventRepository.findById(pending.request.getEventoId()).orElse(null);
            User usuario = userRepository.findByUsername(pending.username).orElse(null);

            if (evento == null || usuario == null) return false; // Datos corruptos, no reintentar

            Sale venta = new Sale();
            venta.setEvento(evento);
            venta.setUser(usuario);
            venta.setFechaVenta(LocalDateTime.now());
            venta.setEstado("CONFIRMADA_REINTENTO"); // Marcamos que fue recuperada

            Sale ventaGuardada = saleRepository.save(venta);

            for (int i = 0; i < pending.request.getAsientos().size(); i++) {
                SimpleSeatDto asientoDto = pending.request.getAsientos().get(i);
                PersonDto persona = pending.request.getPersonas().get(i);

                SeatSold asientoVendido = new SeatSold();
                asientoVendido.setVenta(ventaGuardada);
                asientoVendido.setUbicacion("F" + asientoDto.getFila() + "-C" + asientoDto.getColumna());
                asientoVendido.setNombrePersona(persona.getNombre());
                asientoVendido.setApellidoPersona(persona.getApellido());
                seatSoldRepository.save(asientoVendido);
            }
            return true; // Éxito
        } catch (Exception e) {
            System.err.println("Fallo reintento DB: " + e.getMessage());
            return false;
        }
    }

    // --- Métodos Helper de Archivos (JSON) ---
    private List<PendingSale> loadPendingSales() {
        File file = new File(BACKUP_FILE);
        if (!file.exists()) return new ArrayList<>();
        try {
            return objectMapper.readValue(file, new TypeReference<List<PendingSale>>() {});
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private void savePendingSales(List<PendingSale> list) {
        try {
            objectMapper.writeValue(new File(BACKUP_FILE), list);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
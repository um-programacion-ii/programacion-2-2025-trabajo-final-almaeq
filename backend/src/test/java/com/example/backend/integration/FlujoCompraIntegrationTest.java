package com.example.backend.integration;

import com.example.backend.event.infrastructure.persistence.entity.Event;
import com.example.backend.event.infrastructure.persistence.repository.EventRepository;
import com.example.backend.proxy.application.service.ProxyClientService;
import com.example.backend.sale.infrastructure.client.CatedraApiClient;
import com.example.backend.sale.infrastructure.web.dto.BlockRequestDto;
import com.example.backend.sale.infrastructure.web.dto.PersonDto;
import com.example.backend.sale.infrastructure.web.dto.SaleRequestDto;
import com.example.backend.sale.infrastructure.web.dto.SimpleSeatDto;
import com.example.backend.user.infrastructure.persistence.entity.User;
import com.example.backend.user.infrastructure.persistence.repository.UserRepository;
import com.example.backend.user.infrastructure.web.dto.LoginRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Imports estáticos para MockMvc y CSRF
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl",
        "security.jwt.secret-key=3cfa76ef14937c1c0ea519f8fc057a80fcd04a7420f8e8bcd0a7567c272e007b",
        "security.jwt.expiration-time=3600000"
})
public class FlujoCompraIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // --- MOCKS ---

    // 1. Mock del Proxy (Solo se usa para BLOQUEAR)
    @MockBean
    private ProxyClientService proxyClientService;

    // 2. Mock de Cátedra (Solo se usa para VENDER)
    @MockBean
    private CatedraApiClient catedraApiClient;

    private Long eventoId;
    private String usuarioTest = "alumno_test";
    private String passwordTest = "123456";

    @BeforeEach
    void setup() {
        // Crear usuario si no existe
        if (userRepository.findByUsername(usuarioTest).isEmpty()) {
            User user = new User();
            user.setUsername(usuarioTest);
            user.setPassword(passwordEncoder.encode(passwordTest));
            user.setEmail("test@alumno.com");
            user.setFirstName("Test");
            user.setLastName("User");
            user.setEnabled(true);
            userRepository.save(user);
        }

        // Crear evento
        Event event = new Event();
        event.setId(1L);
        event.setTitulo("Evento Test Integration");
        event.setDescripcion("Descripcion Test");
        event.setFechaHora(LocalDateTime.now().plusDays(10));
        event.setPrecio(1000.0);
        event.setFilas(10);
        event.setColumnas(10);

        Event savedEvent = eventRepository.save(event);
        this.eventoId = savedEvent.getId();
    }

    @Test
    @DisplayName("Simular flujo completo: Login -> Ver Eventos -> Bloquear (Proxy) -> Comprar (Catedra)")
    void testFlujoCompletoDeCompra() throws Exception {

        // ==========================================
        // 1. LOGIN
        // ==========================================
        LoginRequestDto loginReq = new LoginRequestDto();
        loginReq.setUsername(usuarioTest);
        loginReq.setPassword(passwordTest);

        MvcResult loginResult = mockMvc.perform(post("/api/authenticate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = loginResult.getResponse().getContentAsString();
        Map<String, String> responseMap = objectMapper.readValue(responseJson, Map.class);
        String jwtToken = responseMap.get("token");
        String bearerToken = "Bearer " + jwtToken;

        // ==========================================
        // 2. OBTENER EVENTOS
        // ==========================================
        mockMvc.perform(get("/api/events")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())));

        // ==========================================
        // 3. BLOQUEAR ASIENTOS (Vía Proxy)
        // ==========================================
        Map<String, Object> mockRespuestaBloqueo = new HashMap<>();
        mockRespuestaBloqueo.put("resultado", true);
        mockRespuestaBloqueo.put("descripcion", "Bloqueo exitoso simulado (Proxy)");

        // MOCKEAMOS SOLO EL PROXY PARA EL BLOQUEO
        Mockito.when(proxyClientService.bloquearAsientos(Mockito.any())).thenReturn(mockRespuestaBloqueo);

        BlockRequestDto blockReq = new BlockRequestDto();
        blockReq.setEventoId(eventoId);
        blockReq.setAsientos(List.of(new SimpleSeatDto(1, 1)));

        mockMvc.perform(post("/api/venta/bloquear")
                        .with(csrf())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blockReq)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value(true));

        // ==========================================
        // 4. CONFIRMAR VENTA (Vía Cátedra)
        // ==========================================
        Map<String, Object> mockRespuestaVenta = new HashMap<>();
        mockRespuestaVenta.put("resultado", true);
        mockRespuestaVenta.put("descripcion", "Venta exitosa simulada (Cátedra)");
        mockRespuestaVenta.put("idVenta", 999);

        // MOCKEAMOS SOLO LA CÁTEDRA PARA LA VENTA
        Mockito.when(catedraApiClient.realizarVenta(Mockito.any())).thenReturn(mockRespuestaVenta);

        SaleRequestDto saleReq = new SaleRequestDto();
        saleReq.setEventoId(eventoId);
        saleReq.setAsientos(List.of(new SimpleSeatDto(1, 1)));

        PersonDto persona = new PersonDto();
        persona.setNombre("Juan");
        persona.setApellido("Test");
        saleReq.setPersonas(List.of(persona));

        mockMvc.perform(post("/api/venta/confirmar")
                        .with(csrf())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saleReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value(true));
    }
}
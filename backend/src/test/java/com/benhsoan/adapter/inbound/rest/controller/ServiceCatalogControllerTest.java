package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.ServiceCatalogRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.enums.Permission;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.PermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.command.servicecatalog.CreateServiceCatalogCommand;
import com.benhsoan.port.dto.command.servicecatalog.UpdateServiceCatalogCommand;
import com.benhsoan.port.dto.result.servicecatalog.ServiceCatalogResult;
import com.benhsoan.port.dto.result.servicecatalog.ServicePriceResult;
import com.benhsoan.port.inbound.servicecatalog.CreateServiceCatalogUseCase;
import com.benhsoan.port.inbound.servicecatalog.GetServiceCatalogUseCase;
import com.benhsoan.port.inbound.servicecatalog.GetServicePriceHistoryUseCase;
import com.benhsoan.port.inbound.servicecatalog.SearchServiceCatalogUseCase;
import com.benhsoan.port.inbound.servicecatalog.UpdateServiceCatalogStatusUseCase;
import com.benhsoan.port.inbound.servicecatalog.UpdateServiceCatalogUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = ServiceCatalogController.class)
@Import({
        AopAutoConfiguration.class,
        ServiceCatalogControllerTest.AspectTestConfig.class,
        ServiceCatalogRestMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        PermissionAspect.class,
        PermissionEvaluator.class
})
class ServiceCatalogControllerTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig {
    }

    private static final UUID SERVICE_ID = UUID.fromString("11000000-0000-0000-0000-000000000001");
    private static final UUID PRICE_ID = UUID.fromString("12000000-0000-0000-0000-000000000001");
    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1");
    private static final LocalDate EFFECTIVE_FROM = LocalDate.of(2026, 8, 1);
    private static final Instant CREATED_AT = Instant.parse("2026-08-01T00:00:00Z");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CreateServiceCatalogUseCase createServiceCatalogUseCase;
    @MockitoBean private UpdateServiceCatalogUseCase updateServiceCatalogUseCase;
    @MockitoBean private UpdateServiceCatalogStatusUseCase updateServiceCatalogStatusUseCase;
    @MockitoBean private GetServiceCatalogUseCase getServiceCatalogUseCase;
    @MockitoBean private SearchServiceCatalogUseCase searchServiceCatalogUseCase;
    @MockitoBean private GetServicePriceHistoryUseCase getServicePriceHistoryUseCase;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private ClockPort clockPort;

    @BeforeEach
    void configureRolePermissions() {
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(Role.create(
                "ADMIN",
                "Administrator",
                true,
                Set.of(
                        Permission.SERVICE_CATALOG_READ,
                        Permission.SERVICE_CATALOG_CREATE,
                        Permission.SERVICE_CATALOG_UPDATE,
                        Permission.SERVICE_PRICE_MANAGE
                )
        )));
        when(roleRepository.findByName("MANAGER")).thenReturn(Optional.of(Role.create(
                "MANAGER",
                "Clinic manager",
                true,
                Set.of(
                        Permission.REPORT_VIEW,
                        Permission.SERVICE_CATALOG_READ,
                        Permission.SERVICE_CATALOG_CREATE,
                        Permission.SERVICE_CATALOG_UPDATE,
                        Permission.SERVICE_PRICE_MANAGE
                )
        )));
    }

    @Test
    void searchesWithFrontendCompatibleResponse() throws Exception {
        when(searchServiceCatalogUseCase.search(any())).thenReturn(new PageImpl<>(
                List.of(result(true)),
                PageRequest.of(0, 20),
                1
        ));

        mockMvc.perform(get("/system/services")
                        .param("keyword", "blood")
                        .param("active", "true")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(SERVICE_ID.toString()))
                .andExpect(jsonPath("$.content[0].serviceCode").value("LAB-CBC"))
                .andExpect(jsonPath("$.content[0].name").value("Complete blood count"))
                .andExpect(jsonPath("$.content[0].price").value(95000.00))
                .andExpect(jsonPath("$.content[0].effectiveFrom").value("2026-08-01"))
                .andExpect(jsonPath("$.content[0].active").value(true))
                .andExpect(jsonPath("$.content[0].serviceName").doesNotExist());
    }

    @Test
    void getsServiceAndPriceHistory() throws Exception {
        when(getServiceCatalogUseCase.getById(SERVICE_ID)).thenReturn(result(true));
        when(getServicePriceHistoryUseCase.getHistory(SERVICE_ID)).thenReturn(List.of(
                new ServicePriceResult(
                        PRICE_ID,
                        SERVICE_ID,
                        new BigDecimal("95000.00"),
                        EFFECTIVE_FROM,
                        CREATED_AT,
                        ACTOR_ID
                )
        ));

        mockMvc.perform(get("/system/services/{id}", SERVICE_ID)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Complete blood count"));

        mockMvc.perform(get("/system/services/{id}/prices", SERVICE_ID)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(PRICE_ID.toString()))
                .andExpect(jsonPath("$[0].price").value(95000.00))
                .andExpect(jsonPath("$[0].effectiveFrom").value("2026-08-01"));
    }

    @Test
    void createsServiceAndMapsNameToApplicationCommand() throws Exception {
        when(createServiceCatalogUseCase.create(any())).thenReturn(result(true));

        mockMvc.perform(post("/system/services")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceCode":"LAB-CBC",
                                  "name":"Complete blood count",
                                  "price":95000,
                                  "effectiveFrom":"2026-08-01"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/system/services/" + SERVICE_ID))
                .andExpect(jsonPath("$.name").value("Complete blood count"));

        ArgumentCaptor<CreateServiceCatalogCommand> captor =
                ArgumentCaptor.forClass(CreateServiceCatalogCommand.class);
        verify(createServiceCatalogUseCase).create(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("Complete blood count", captor.getValue().serviceName());
    }

    @Test
    void updatesServiceAndStatus() throws Exception {
        when(updateServiceCatalogUseCase.update(any())).thenReturn(result(true));
        when(updateServiceCatalogStatusUseCase.updateStatus(SERVICE_ID, false)).thenReturn(result(false));

        mockMvc.perform(put("/system/services/{id}", SERVICE_ID)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Complete blood count",
                                  "active":true,
                                  "price":95000,
                                  "effectiveFrom":"2026-08-01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        ArgumentCaptor<UpdateServiceCatalogCommand> captor =
                ArgumentCaptor.forClass(UpdateServiceCatalogCommand.class);
        verify(updateServiceCatalogUseCase).update(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(SERVICE_ID, captor.getValue().serviceCatalogId());

        mockMvc.perform(patch("/system/services/{id}/status", SERVICE_ID)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void allowsManagerToManageServiceCatalogAndPrices() throws Exception {
        when(searchServiceCatalogUseCase.search(any())).thenReturn(Page.empty());
        when(getServiceCatalogUseCase.getById(SERVICE_ID)).thenReturn(result(true));
        when(getServicePriceHistoryUseCase.getHistory(SERVICE_ID)).thenReturn(List.of());
        when(createServiceCatalogUseCase.create(any())).thenReturn(result(true));
        when(updateServiceCatalogUseCase.update(any())).thenReturn(result(true));
        when(updateServiceCatalogStatusUseCase.updateStatus(SERVICE_ID, false)).thenReturn(result(false));

        mockMvc.perform(get("/system/services")
                        .with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/system/services/{id}", SERVICE_ID)
                        .with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/system/services/{id}/prices", SERVICE_ID)
                        .with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/system/services")
                        .with(user("manager").roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestBody()))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/system/services/{id}", SERVICE_ID)
                        .with(user("manager").roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequestBody()))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/system/services/{id}/status", SERVICE_ID)
                        .with(user("manager").roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/system/services"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/system/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestBody()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(
                createServiceCatalogUseCase,
                updateServiceCatalogUseCase,
                updateServiceCatalogStatusUseCase,
                getServiceCatalogUseCase,
                searchServiceCatalogUseCase,
                getServicePriceHistoryUseCase
        );
    }

    private String createRequestBody() {
        return """
                {
                  "serviceCode":"LAB-CBC",
                  "name":"Complete blood count",
                  "price":95000,
                  "effectiveFrom":"2026-08-01"
                }
                """;
    }

    private String updateRequestBody() {
        return """
                {
                  "name":"Complete blood count",
                  "active":true,
                  "price":95000,
                  "effectiveFrom":"2026-08-01"
                }
                """;
    }

    private ServiceCatalogResult result(boolean active) {
        return new ServiceCatalogResult(
                SERVICE_ID,
                "LAB-CBC",
                "Complete blood count",
                active,
                new BigDecimal("95000.00"),
                EFFECTIVE_FROM,
                CREATED_AT,
                CREATED_AT
        );
    }
}

package com.unequipment.platform.modules.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unequipment.platform.modules.content.controller.ContentAdminController;
import com.unequipment.platform.modules.content.entity.Notice;
import com.unequipment.platform.modules.content.service.ContentService;
import com.unequipment.platform.modules.finance.controller.SettlementAdminController;
import com.unequipment.platform.modules.finance.entity.SettlementRecord;
import com.unequipment.platform.modules.finance.service.SettlementAdminService;
import com.unequipment.platform.modules.order.controller.OrderAdminController;
import com.unequipment.platform.modules.order.controller.ReservationController;
import com.unequipment.platform.modules.order.dto.AuditRequest;
import com.unequipment.platform.modules.order.entity.ReservationOrder;
import com.unequipment.platform.modules.order.service.OrderService;
import com.unequipment.platform.modules.system.controller.AuthController;
import com.unequipment.platform.modules.system.dto.LoginRequest;
import com.unequipment.platform.modules.system.entity.SysUser;
import com.unequipment.platform.modules.system.service.AuthService;
import com.unequipment.platform.security.CurrentUserArgumentResolver;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ApiSmokeMockMvcTest {

    @Mock
    private AuthService authService;
    @Mock
    private OrderService orderService;
    @Mock
    private SettlementAdminService settlementAdminService;
    @Mock
    private ContentService contentService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        AuthController authController = new AuthController(authService);
        ReservationController reservationController = new ReservationController(orderService);
        OrderAdminController orderAdminController = new OrderAdminController(orderService);
        SettlementAdminController settlementAdminController = new SettlementAdminController(settlementAdminService);
        ContentAdminController contentAdminController = new ContentAdminController(contentService);

        mockMvc = MockMvcBuilders.standaloneSetup(
                authController,
                reservationController,
                orderAdminController,
                settlementAdminController,
                contentAdminController
            )
            .setCustomArgumentResolvers(new CurrentUserArgumentResolver())
            .build();
    }

    @Test
    void login_shouldReturnSuccess() throws Exception {
        Map<String, Object> loginResult = new HashMap<>();
        loginResult.put("token", "mock-token");
        when(authService.login(any(LoginRequest.class))).thenReturn(loginResult);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.token").value("mock-token"));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void createMachineReservation_shouldReturnSuccess() throws Exception {
        ReservationOrder order = new ReservationOrder();
        order.setId(1001L);
        when(orderService.createMachineOrder(any(SysUser.class), any())).thenReturn(order);

        String body = "{"
            + "\"instrumentId\":1,"
            + "\"reservedStart\":\"2026-03-20T09:00:00\","
            + "\"reservedEnd\":\"2026-03-20T10:00:00\","
            + "\"projectName\":\"联调测试\","
            + "\"purpose\":\"冒烟验证\""
            + "}";

        mockMvc.perform(post("/api/reservations/machine")
                .requestAttr("currentUser", buildUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").value(1001));

        verify(orderService).createMachineOrder(any(SysUser.class), any());
    }

    @Test
    void auditOrder_shouldReturnSuccess() throws Exception {
        ReservationOrder order = new ReservationOrder();
        order.setId(2002L);
        when(orderService.audit(eq(2002L), any(SysUser.class), any(AuditRequest.class))).thenReturn(order);

        mockMvc.perform(post("/api/admin/orders/2002/audit")
                .requestAttr("currentUser", buildAdmin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"APPROVE\",\"comment\":\"通过\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").value(2002));

        verify(orderService).audit(eq(2002L), any(SysUser.class), any(AuditRequest.class));
    }

    @Test
    void settle_shouldReturnSuccess() throws Exception {
        ReservationOrder order = new ReservationOrder();
        order.setId(3003L);
        when(settlementAdminService.settle(any(SysUser.class), eq(3003L))).thenReturn(order);

        mockMvc.perform(post("/api/admin/settlements/3003/settle")
                .requestAttr("currentUser", buildAdmin()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").value(3003));

        verify(settlementAdminService).settle(any(SysUser.class), eq(3003L));
    }

    @Test
    void createNotice_shouldReturnSuccess() throws Exception {
        Notice notice = new Notice();
        notice.setId(4004L);
        notice.setTitle("测试公告");
        notice.setPublishTime(LocalDateTime.of(2026, 3, 16, 15, 0, 0));
        when(contentService.saveNotice(any(), any(Notice.class), any(SysUser.class))).thenReturn(notice);

        String body = "{"
            + "\"title\":\"测试公告\","
            + "\"category\":\"NOTICE\","
            + "\"summary\":\"摘要\","
            + "\"content\":\"正文\","
            + "\"publishStatus\":\"PUBLISHED\""
            + "}";

        mockMvc.perform(post("/api/admin/content/notices")
                .requestAttr("currentUser", buildAdmin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").value(4004))
            .andExpect(jsonPath("$.data.title").value("测试公告"));

        verify(contentService).saveNotice(eq(null), any(Notice.class), any(SysUser.class));
    }

    private SysUser buildUser() {
        SysUser user = new SysUser();
        user.setId(11L);
        user.setRealName("测试用户");
        user.setDepartmentId(101L);
        user.setPrimaryRoleCode("INTERNAL_USER");
        return user;
    }

    private SysUser buildAdmin() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setRealName("管理员");
        user.setDepartmentId(100L);
        user.setPrimaryRoleCode("ADMIN");
        return user;
    }
}


package com.clanmanager.clanmanager.controller;

import com.clanmanager.clanmanager.dto.WatchReportRequest;
import com.clanmanager.clanmanager.entity.WatchLog;
import com.clanmanager.clanmanager.service.WatchReportService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WatchControllerTest {

    @Test
    void validBearerTokenStoresReport() {
        WatchReportService service = mock(WatchReportService.class);
        WatchController controller = new WatchController(service);
        ReflectionTestUtils.setField(controller, "apiKey", "test-secret");
        WatchReportRequest request = new WatchReportRequest("입장가능인원", "3/200", "4/200", LocalDateTime.now());
        WatchLog expected = WatchLog.builder()
                .id(1L)
                .targetId(request.targetId())
                .oldValue(request.oldValue())
                .newValue(request.newValue())
                .changedAt(request.changedAt())
                .build();
        when(service.report(request)).thenReturn(expected);

        WatchLog response = controller.report("Bearer test-secret", request);

        assertThat(response).isSameAs(expected);
        verify(service).report(request);
    }

    @Test
    void missingOrWrongBearerTokenIsRejected() {
        WatchReportService service = mock(WatchReportService.class);
        WatchController controller = new WatchController(service);
        ReflectionTestUtils.setField(controller, "apiKey", "test-secret");
        WatchReportRequest request = new WatchReportRequest("입장가능인원", "3/200", "4/200", LocalDateTime.now());

        assertThatThrownBy(() -> controller.report("Bearer wrong", request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401 UNAUTHORIZED");
        assertThatThrownBy(() -> controller.report(null, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401 UNAUTHORIZED");
    }
}

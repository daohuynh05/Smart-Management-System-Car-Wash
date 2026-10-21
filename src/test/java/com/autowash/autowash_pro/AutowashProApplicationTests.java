package com.autowash.autowash_pro;

import static org.assertj.core.api.Assertions.assertThat;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import com.autowash.autowash_pro.config.DemoDataInitializer;
import com.autowash.autowash_pro.repository.CustomerRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"demo", "test"})
class AutowashProApplicationTests {
    @Autowired Environment environment;
    @Autowired JdbcTemplate jdbc;
    @Autowired CustomerRepository customers;
    @Autowired DemoDataInitializer demo;
    @Autowired PasswordEncoder encoder;
    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void migrationsAndDemoSeedingAreRepeatable() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE success", Integer.class))
                .isGreaterThanOrEqualTo(10);
        long count = customers.count();
        demo.run();
        assertThat(customers.count()).isEqualTo(count);
        var customer = customers.findByPhone("0000000001").orElseThrow();
        var admin = customers.findByPhone("0000000002").orElseThrow();
        assertThat(customer.isAdmin()).isFalse();
        assertThat(admin.isAdmin()).isTrue();
        assertThat(encoder.matches("CustomerDemo!2026", customer.getPassword())).isTrue();
        assertThat(encoder.matches("AdminDemo!2026", admin.getPassword())).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM vehicles WHERE license_plate = 'DEMO-0001'", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void customerAndAdminLoginEnforceRolesOverHttp() throws Exception {
        String customer = login("customer@example.invalid", "CustomerDemo!2026", "CUSTOMER");
        String admin = login("admin@example.invalid", "AdminDemo!2026", "ADMIN");
        assertThat(get("/api/auth/me", customer).statusCode()).isEqualTo(200);
        assertThat(get("/api/auth/me", admin).statusCode()).isEqualTo(200);
        assertThat(get("/api/admin/dashboard/stats", customer).statusCode()).isEqualTo(403);
        assertThat(get("/api/admin/dashboard/stats", admin).statusCode()).isEqualTo(200);
        var bad = post("/api/auth/login", "{\"emailOrPhone\":\"customer@example.invalid\",\"password\":\"wrong\"}");
        assertThat(bad.statusCode()).isBetween(400, 499);
    }

    private String login(String email, String password, String role) throws Exception {
        var response = post("/api/auth/login", "{\"emailOrPhone\":\"" + email + "\",\"password\":\"" + password + "\"}");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"role\":\"" + role + "\"");
        var matcher = Pattern.compile("\"accessToken\":\"([^\"]+)\"").matcher(response.body());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + environment.getProperty("local.server.port") + path);
    }
    private HttpResponse<String> post(String path, String body) throws Exception {
        return http.send(HttpRequest.newBuilder(uri(path)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    private HttpResponse<String> get(String path, String token) throws Exception {
        return http.send(HttpRequest.newBuilder(uri(path)).header("Authorization", "Bearer " + token)
                .GET().build(), HttpResponse.BodyHandlers.ofString());
    }
}

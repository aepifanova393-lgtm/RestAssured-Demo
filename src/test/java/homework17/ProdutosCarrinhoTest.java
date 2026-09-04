package homework17;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)

public class ProdutosCarrinhoTest {

    private static String adminToken;   // токен администратора (JWT c префиксом "Bearer ")
    private static String adminId;

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "https://serverest.dev";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        createAdmin();
    }

    // Регистрируем администратора и логинимся, получаем JWT-токен.
    // ServeRest: пользователь с administrador="true" может создавать/менять/удалять товары.
    private static void createAdmin() {
        String email = "admin_" + System.currentTimeMillis() + "@qa.com";
        Usuario admin = new Usuario("Администратор Тест", email, "secret123", "true");

        adminId = given()
                .contentType(ContentType.JSON)
                .body(admin)
                .when()
                .post("/usuarios")
                .then()
                .statusCode(201)
                .body("_id", not(emptyOrNullString()))
                .extract()
                .path("_id");

        adminToken = given()
                .contentType(ContentType.JSON)
                .body("""
                          {
                            "email": "%s",
                            "password": "secret123"
                          }
                        """.formatted(email))
                .when()
                .post("/login")
                .then()
                .statusCode(200)
                .body("message", equalTo("Login realizado com sucesso"))
                .body("authorization", notNullValue())
                .extract()
                .path("authorization");
    }

    // ServeRest разрешает только ОДНУ активную корзину на пользователя.
    // Чтобы тесты корзины были независимы и переиспользуемы, каждый такой тест
    // создаёт своего свежего администратора (а не переиспользует общий adminToken).
    private static String createFreshAdminToken() {
        String email = "cart_" + System.currentTimeMillis() + "@qa.com";
        given()
                .contentType(ContentType.JSON)
                .body(new Usuario("Корзинный Админ", email, "secret123", "true"))
                .when()
                .post("/usuarios")
                .then()
                .statusCode(201);

        return given()
                .contentType(ContentType.JSON)
                .body("""
                          {
                            "email": "%s",
                            "password": "secret123"
                          }
                        """.formatted(email))
                .when()
                .post("/login")
                .then()
                .statusCode(200)
                .extract()
                .path("authorization");
    }

    @Order(1)
    @Test
    void shouldCreateProductAsAdmin() {
        given()
                .header("Authorization", adminToken)
                .contentType(ContentType.JSON)
                .body("""
                          {
                            "nome": "Лопата тайного покупателя %s",
                            "preco": 1299,
                            "descricao": "Орудие труда шпиона",
                            "quantidade": 3
                          }
                        """.formatted(System.currentTimeMillis()))
                .when()
                .post("/produtos")
                .then()
                .statusCode(201)
                .body("message", equalTo("Cadastro realizado com sucesso"))
                .body("_id", not(emptyOrNullString()));
    }

    @Order(2)
    @Test
    void shouldValidateAdminTokenIsRequiredForWriteOperations() {
        // Создание товара без токена должно завершиться 401 (тело ответа пустое).
        given()
                .contentType(ContentType.JSON)
                .body("""
                          {
                            "nome": "Товар без токена",
                            "preco": 10,
                            "descricao": "не должен создаться",
                            "quantidade": 1
                          }
                        """)
                .when()
                .post("/produtos")
                .then()
                .statusCode(401);
    }

    @Order(3)
    @Test
    void shouldNotAllowNonAdminToCreateProduct() {
        // Обычный (не админ) пользователь не должен создавать товары -> 403.
        String email = "user_" + System.currentTimeMillis() + "@qa.com";
        given()
                .contentType(ContentType.JSON)
                .body(new Usuario("Обычный ПО", email, "secret123", "false"))
                .when()
                .post("/usuarios")
                .then()
                .statusCode(201);

        String userToken = given()
                .contentType(ContentType.JSON)
                .body("""
                          {
                            "email": "%s",
                            "password": "secret123"
                          }
                        """.formatted(email))
                .when()
                .post("/login")
                .then()
                .statusCode(200)
                .extract()
                .path("authorization");

        given()
                .header("Authorization", userToken)
                .contentType(ContentType.JSON)
                .body("""
                          {
                            "nome": "Попытка без прав",
                            "preco": 10,
                            "descricao": "должна быть отклонена",
                            "quantidade": 1
                          }
                        """)
                .when()
                .post("/produtos")
                .then()
                .statusCode(403)
                .body("message", equalTo("Rota exclusiva para administradores"));
    }

    @Order(4)
    @Test
    void shouldUpdateAndDeleteProduct() {
        // 1. Создаём товар админом
        String productId = given()
                .header("Authorization", adminToken)
                .contentType(ContentType.JSON)
                .body("""
                          {
                            "nome": "Товар для обновления %d",
                            "preco": 100,
                            "descricao": "исходный",
                            "quantidade": 5
                          }
                        """.formatted(System.currentTimeMillis()))
                .when()
                .post("/produtos")
                .then()
                .statusCode(201)
                .extract()
                .path("_id");

        // 2. Обновляем товар (PUT)
        given()
                .header("Authorization", adminToken)
                .pathParam("id", productId)
                .contentType(ContentType.JSON)
                .body("""
                          {
                            "nome": "Обновлённый товар",
                            "preco": 150,
                            "descricao": "обновлённый",
                            "quantidade": 7
                          }
                        """)
                .when()
                .put("/produtos/{id}")
                .then()
                .statusCode(200)
                .body("message", equalTo("Registro alterado com sucesso"));

        // 3. Удаляем товар (DELETE)
        given()
                .header("Authorization", adminToken)
                .pathParam("id", productId)
                .when()
                .delete("/produtos/{id}")
                .then()
                .statusCode(200)
                .body("message", equalTo("Registro excluído com sucesso"));

        // 4. Проверяем, что товара больше нет
        given()
                .pathParam("id", productId)
                .when()
                .get("/produtos/{id}")
                .then()
                .statusCode(400)
                .body("message", equalTo("Produto n\u00e3o encontrado"));
    }

    @Order(5)
    @Test
    void shouldCreateCartAndCompletePurchase() {
        // У каждого пользователя может быть только одна корзина, поэтому
        // создаём свежего админа, а не переиспользуем общий adminToken.
        String token = createFreshAdminToken();

        // 1. Создаём товар (уникальное имя, чтобы тест был переиспользуемым)
        String productId = given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("""
                          {
                            "nome": "Товар для корзины %d",
                            "preco": 50,
                            "descricao": "для корзины",
                            "quantidade": 10
                          }
                        """.formatted(System.currentTimeMillis()))
                .when()
                .post("/produtos")
                .then()
                .statusCode(201)
                .extract()
                .path("_id");

        // 2. Добавляем в корзину (POST /carrinhos)
        String cartId = given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "produtos": [
                            { "idProduto": "%s", "quantidade": 2 }
                          ]
                        }
                        """.formatted(productId))
                .when()
                .post("/carrinhos")
                .then()
                .statusCode(201)
                .body("message", equalTo("Cadastro realizado com sucesso"))
                .body("_id", notNullValue())
                .extract()
                .path("_id");
        assertNotNull(cartId);

        // 3. Завершаем покупку (DELETE /carrinhos/concluir-compra)
        given()
                .header("Authorization", token)
                .when()
                .delete("/carrinhos/concluir-compra")
                .then()
                .statusCode(200)
                .body("message", equalTo("Registro excluído com sucesso"));

        // 4. Повторное завершение покупки: корзины уже нет.
        given()
                .header("Authorization", token)
                .when()
                .delete("/carrinhos/concluir-compra")
                .then()
                .statusCode(200)
                .body("message", equalTo("N\u00e3o foi encontrado carrinho para esse usu\u00e1rio"));
    }

    @Order(6)
    @Test
    void shouldCancelCart() {
        // Свежий админ, чтобы гарантированно не было активной корзины.
        String token = createFreshAdminToken();

        // 1. Создаём товар (уникальное имя, чтобы тест был переиспользуемым)
        String productId = given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("""
                          {
                            "nome": "Товар для отмены %d",
                            "preco": 20,
                            "descricao": "для отмены",
                            "quantidade": 4
                          }
                        """.formatted(System.currentTimeMillis()))
                .when()
                .post("/produtos")
                .then()
                .statusCode(201)
                .extract()
                .path("_id");

        // 2. Добавляем в корзину
        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("""
                          {
                            "produtos": [ { "idProduto": "%s", "quantidade": 1 } ]
                          }
                        """.formatted(productId))
                .when()
                .post("/carrinhos")
                .then()
                .statusCode(201);

        // 3. Отменяем корзину (DELETE /carrinhos/cancelar-compra)
        given()
                .header("Authorization", token)
                .when()
                .delete("/carrinhos/cancelar-compra")
                .then()
                .statusCode(200)
                .body("message", equalTo("Registro excluído com sucesso. Estoque dos produtos reabastecido"));
    }
}
package homework17;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.everyItem;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)

public class ServeRestTest {

    private static String userId;
    private static String token;
    private static String email;

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "https://serverest.dev";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Order(1)
    @Test
    public void shouldGetAllUsers() {

        given()
                .when()
                    .get ("/usuarios")
                .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("quantidade", greaterThan(0))
                    .body("usuarios", not(empty()));
    }

    @Order(2)
    @Test
    public void shouldFindUserByEmail() {
        email = given()
                .when()
                    .get ("/usuarios")
                .then()
                    .extract()
                    .path("usuarios[0].email");

                given()
                    .queryParam("email", email)
                .when()
                    .get ("/usuarios")
                .then()
                        .statusCode(200)
                        .body("quantidade", equalTo(1))
                        .body("usuarios[0].email", equalTo(email));
    }

//    @Order(3)
//    @Test
//    public void shouldCreateNewUser() {
//        email = "spy_" + System.currentTimeMillis() + "@qa.com";
//
//        userId = given()
//                .contentType(ContentType.JSON)
//                .body("""
//                {
//                  "nome": "Тайный Покупатель",
//                  "email": "%s",
//                  "password": "secret123",
//                  "administrador": "true"
//                }
//                """.formatted(email))
//                .when()
//                .post("/usuarios")
//                .then()
//                .statusCode(201)
//                .body("message", equalTo("Cadastro realizado com sucesso"))
//                .body("_id", notNullValue())
//                .extract()
//                .path("_id");
//
//    }

    @Order(3)
    @Test
    @DisplayName("★ Создание пользователя через DTO (сериализация)")
    void shouldCreateUserFromDto() {
        email = "spy_" + System.currentTimeMillis() + "@qa.com";
        Usuario newUser = new Usuario("Тайный Покупатель", email, "secret123", "true");

        userId = given()
                .contentType(ContentType.JSON)
                .body(newUser)
                .when()
                .post("/usuarios")
                .then()
                .statusCode(201)
                .body("message", equalTo("Cadastro realizado com sucesso"))
                .body("_id", notNullValue())
                .extract()
                .path("_id");
    }

    @Order(4)
    @Test
    public void shouldUpdateUser() {
        email = "spy_" + System.currentTimeMillis() + "@qa.com";

        given()
                .pathParam("userId", userId)
                .contentType(ContentType.JSON)
                .body("""
                {
                  "nome": "Обновленный Покупатель",
                  "email": "%s",
                  "password": "secret123",
                  "administrador": "false"
                }
                """.formatted(email))
                .when()
                .put("/usuarios/{userId}")
                .then()
                .statusCode(200)
                .body("message", equalTo("Registro alterado com sucesso"));
    }

    @Order(5)
    @Test
    public void shouldLogin() {
        token = given()
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

    @Order(6)
    @Test
    public void shouldDeleteUser() {
        given()
                .pathParam("userId", userId)
                .header("Authorization", token)
                .when()
                .delete("/usuarios/{userId}")
                .then()
                .statusCode(200)
                .body("message", equalTo("Registro excluído com sucesso"));

        given()
                .pathParam("userId", userId)
                .when()
                .get ("/usuarios/{userId}")
                .then()
                .statusCode(400)
                .body("message", equalTo("Usuário não encontrado"));
    }

    @Order(7)
    @Test
    public void shouldGetAllProducts() {
        given()
                .when()
                .get("/produtos")
                .then()
                .statusCode(200)
                .body("quantidade", greaterThan(0))
                .body("produtos.preco", everyItem(greaterThan(0)))
                .body("produtos.nome", everyItem(not(isEmptyOrNullString())))
                .body("produtos.nome", hasItem("Logitech MX Vertical"));
    }
}

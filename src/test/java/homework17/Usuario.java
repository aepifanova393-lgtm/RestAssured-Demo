package homework17;

//Lombok не захотел взлетать, поэтому класс написан простыней

public class Usuario {
    private String nome;
    private String email;
    private String password;
    private String administrador;

    // Пустой конструктор — нужен Jackson'у для десериализации
    public Usuario() {}

    // Конструктор для удобного создания в тесте
    public Usuario(String nome, String email, String password, String administrador) {
        this.nome          = nome;
        this.email         = email;
        this.password      = password;
        this.administrador = administrador;
    }

    // Геттеры и сеттеры для всех полей — их использует Jackson.
    // (Можно сгенерировать в IDE: правый клик → Generate → Getter and Setter)
    public String getNome()          { return nome; }
    public void   setNome(String v)  { this.nome = v; }

    public String getEmail()         { return email; }
    public void   setEmail(String v) { this.email = v; }

    public String getPassword()      { return password; }
    public void   setPassword(String v) { this.password = v; }

    public String getAdministrador()      { return administrador; }
    public void   setAdministrador(String v) { this.administrador = v; }
}

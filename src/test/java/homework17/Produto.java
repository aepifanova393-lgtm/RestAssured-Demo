package homework17;

public class Produto {
    private String nome;
    private int preco;
    private String descricao;
    private int quantidade;

    public Produto() {}

    public Produto(String nome, int preco, String descricao, int quantidade) {
        this.nome = nome;
        this.preco = preco;
        this.descricao = descricao;
        this.quantidade = quantidade;
    }

    public String getNome()         { return nome; }
    public void   setNome(String v) { this.nome = v; }

    public int     getPreco()         { return preco; }
    public void    setPreco(int v)    { this.preco = v; }

    public String  getDescricao()         { return descricao; }
    public void    setDescricao(String v) { this.descricao = v; }

    public int     getQuantidade()         { return quantidade; }
    public void    setQuantidade(int v)    { this.quantidade = v; }
}
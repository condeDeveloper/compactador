# compactador

Compactador de dados escrito do zero em Java 21: Huffman canônico, LZ77 e E/S
em nível de bit. Sem `java.util.zip`.

```java
byte[] comprimido = Compactador.comprimir(dados);
byte[] devolta = Compactador.descomprimir(comprimido);

System.out.println(Compactador.medir(dados));
// LZ77: 10000 -> 47 bytes (99,5% menor, 0,04 bits/byte)
```

## Por que existe

Compressão é um dos poucos lugares em que a teoria da informação encosta em
código de verdade, e os dois algoritmos clássicos atacam redundâncias
diferentes:

- **Huffman** explora bytes com frequências desiguais. Em texto, espaço e
  vogais dominam, e trocá-los por códigos de três bits enquanto `ç` fica com
  doze compensa. Mas ele não enxerga repetição: em `abcabcabc` os três bytes
  são igualmente frequentes e ele não economiza nada.
- **LZ77** explora trechos repetidos, trocando-os por "volte 3 bytes, copie 6".
  É o que faz um HTML cheio de tags iguais desabar. Em compensação, num texto
  sem repetição literal ele não tem o que fazer.

Nenhum dos dois vence sempre — por isso o `Compactador` mede os três caminhos
(incluindo não comprimir) e fica com o menor.

## As partes

### E/S em nível de bit

Um código de Huffman pode ter três bits. Gravar cada um em um byte jogaria fora
exatamente o que se economizou, então os bits se acumulam em um buffer de oito
antes de virar byte. O `alinhar()` no fim não é detalhe: sem ele os últimos
bits ficam presos no acumulador e o arquivo sai truncado.

### Huffman canônico

Duas árvores com os mesmos comprimentos de código comprimem igual — então não
faz sentido transmitir a árvore. Basta combinar uma regra: ordene os símbolos
por comprimento e, dentro do mesmo comprimento, por valor, e vá contando em
binário. O decodificador remonta tudo a partir de **256 comprimentos**, sem
receber um único ponteiro.

Há um teto de 15 bits por código. Sem ele, uma distribuição em forma de
Fibonacci geraria códigos com dezenas de bits, que não caberiam em um `int` —
e há um teste construindo exatamente essa distribuição para provar os dois
lados.

### LZ77

Procurar a maior repetição comparando cada posição com todas as anteriores
custaria o quadrado do tamanho da entrada. A saída é indexar as posições por
uma chave de três bytes numa tabela de hash com encadeamento: para cada ponto,
a tabela já diz onde aquele trio apareceu antes.

Um detalhe que parece bug e é recurso: a cópia pode passar do ponto de partida.
`<1,1000>` repete o byte anterior mil vezes, porque na descompressão cada byte
copiado já está disponível para o seguinte. É assim que mil zeros cabem em
menos de 50 bytes.

## Formato do arquivo

```
C0 DE  método  corpo
```

Dois bytes de assinatura e um de método. É esse byte que permite ter
`ARMAZENADO` como rede de segurança: quando nada comprime — dado aleatório,
arquivo já compactado — a saída fica **três bytes** maior que a entrada, não o
dobro. Tem teste para isso.

## Entropia

```java
Compactador.entropia(dados);   // bits por byte
```

É o piso teórico: nenhum código que trate os bytes um a um consegue ficar
abaixo. Dado aleatório dá 8,0 e não comprime de jeito nenhum; texto em
português fica perto de 4,5. Serve para saber quando parar de tentar.

## Estrutura

```
bits/EscritorDeBits.java      acumula bits e emite bytes
bits/LeitorDeBits.java        o caminho de volta
huffman/Frequencias.java      contagem por byte e entropia de Shannon
huffman/ArvoreDeHuffman.java  fila de prioridade, comprimentos e o teto de 15 bits
huffman/CodigoCanonico.java   atribuição canônica, tabela e decodificação
huffman/Huffman.java          comprimir e descomprimir
lz77/Correspondencia.java     o par distância/comprimento
lz77/JanelaDeslizante.java    índice por hash de três bytes
lz77/Lz77.java                comprimir e descomprimir
Compactador.java              escolhe o método e cuida do cabeçalho
Estatisticas.java             razão, economia e bits por byte
```

## Rodando

```bash
mvn test
```

76 testes, nenhuma dependência de runtime — só JUnit e AssertJ para testar.

## Limites conhecidos

- **Tudo em memória**, `byte[]` na entrada e na saída. Não serve para arquivo
  grande; um fluxo em blocos resolveria, mas mudaria o formato.
- Huffman e LZ77 não são combinados como no deflate. Combiná-los renderia mais,
  ao custo de um formato bem mais complicado — e o objetivo aqui era ver cada
  algoritmo isolado.
- O LZ77 usa busca gulosa, sem *lazy matching*: ele fica com a primeira boa
  repetição em vez de conferir se começar um byte depois renderia mais.
- A tabela de Huffman ocupa 256 bytes fixos, o que pesa em entrada pequena.
- Sem verificação de integridade (CRC) no arquivo.

## Licença

MIT.

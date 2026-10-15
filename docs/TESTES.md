# Validação executada e bloqueios

## Execução real nesta sessão

Data: 2026-10-15 UTC. Ambiente completo em `AUDITORIA.md`.
Compilador: **ECJ 3.45.0.v20260224-0835**, `--release 25 -failOnWarning`.
Runtime: **Temurin 25.0.2+10-LTS**, `-ea -Xms128m -Xmx128m`.
Código Fabric/Minecraft deliberadamente excluído do teste independente, sem stubs
ou APIs fictícias para aparentar compilação real.

`scripts/test-core.sh`: **PASS — 155315 verificações**, não 155315 cenários do jogo.

| Área testada no núcleo | Resultado |
|---|---|
| Janela vazia, amostra zero, capacidade 1 e limite 65536 | passou |
| p50/p95/p99 nearest-rank, min/max/média e >50ms (50ms exatos excluídos) | passou |
| Evicção do mais antigo, wrap repetido, snapshot imutável, clear sem dados antigos | passou |
| Duração negativa rejeitada sem mutação, Long.MAX_VALUE sem overflow de soma | passou |
| Oracle independente: seed 263015, 25000 operações em cada capacidade 1/2/17/128/4096 | passou |
| END órfão, START duplicado, tick inacabado na parada, relógio regressivo | passou |
| Wrap assinado do nanoTime, cooldown em limite de 5s e durante wrap | passou |
| 10000 resets de sessão sem herdar amostras/cooldown | passou, núcleo somente |
| Arquivo ausente não é criado; UTF-8, padrões e limites válidos | passou |
| Booleanos inválidos, overflow/número fracionário/NaN, chave desconhecida, escape malformado | passou |
| Configuração >8192 bytes rejeitada; 8192 aceita; diretório em vez de arquivo | passou |
| Locale alemão não altera decimal do relatório; janela vazia não finge medição | passou |
| Equivalência do ring com referência cronológica sintética | passou no microbenchmark |
| Alocação durante inserção, contador ThreadMXBean | 0.000 bytes/op em ambos os coletores nos 5 ensaios |

Também executados: `bash -n scripts/*.sh`, parsing JSON do metadata, consistência
dos pins e `git diff --check`. Workflow CI adicionado, **não executado nesta sessão**.
Ramo javac padrão dos scripts é destinado ao JDK completo; localmente usamos
explicitamente ECJ. Não declarar o adaptador compilado por causa desses testes.

## Reprodução com JDK 25 completo (preferida)

```sh
java -version
javac -version
scripts/test-core.sh
scripts/benchmark-core.sh results.csv
./gradlew --no-daemon clean check build
```

Scripts requerem Bash ≥4 (Linux; Windows via Bash/WSL). No macOS usar Bash ≥4
ou executar o build/teste `./gradlew check build` com JDK 25. Windows nativo:
`gradlew.bat check build`. O Gradle executa `coreTest` via `check`.
A distribuição Gradle, Maven Fabric/Mojang e dependências precisam estar acessíveis.
Não há flags de JVM universais recomendadas; 128 MiB só para núcleo/benchmark.

## Reprodução do caminho alternativo realmente usado

Este não resolve o build Fabric; só permite compilar/testar o núcleo em sandbox
sem JDK completo. Os downloads vão para `.local/` ignorado, não para o mod:

```sh
python3 -m pip install --target .local/python 'jdk4py==25.0.2.1'
mkdir -p .local/jdt
(cd .local && npm pack @vscjava/java-language-server@0.1.2 --silent)
tar -xzf .local/vscjava-java-language-server-0.1.2.tgz -C .local/jdt
export JAVA_HOME="$PWD/.local/python/jdk4py/java-runtime"
export ECJ_JAR="$PWD/.local/jdt/package/server/plugins/org.eclipse.jdt.core.compiler.batch_3.45.0.v20260224-0835.jar"
echo "c8f5e66787ea6e4e0394ba3051c86a4edc50683ce9850f71739646529a99a2d7  $ECJ_JAR" | sha256sum --check
scripts/test-core.sh
scripts/benchmark-core.sh results.csv
```

O wheel acima é para Linux x86_64 desta sessão; outras plataformas selecionam
outros wheels. `npm pack` obtém o arquivo, sem instalar dependências nem executar
postinstall do language server. Não usar ferramentas de bootstrap em produção.

## Pendências de alta importância / gates de release

- [ ] Build real com Gradle/JDK completo, compilação do adaptador contra API exata,
  resolução de dependências e inspeção do JAR; CI verde não substitui runtime.
- [ ] Inicializar 26.3 dedicado e integrado: módulo ausente/off/on, comando permitido
  somente a admin/console/RCON; comando negado para jogador não op e op nível 2.
- [ ] Com off, confirmar ausência de callbacks por tick/alocações; com on, comparar
  intervalo com profiler, janela e cooldown compartilhado entre 2 operadores.
- [ ] Sair/entrar em mundos 100 vezes e observar retenção do servidor no heap;
  após pausa vazia, nenhum tick falso; data pack reload não duplica listeners.
- [ ] S0–S6/C0–C1 e fixture exata, A/B repetido, JFR/GC/frametime/IO/rede externos.
- [ ] Instalação só servidor, só cliente, ambos e cliente sem mod; perda de pacotes,
  inventários concorrentes, salvamento/reinício/crash e teste prolongado 4h.
- [ ] Mods de conteúdo e otimizações com artefatos 26.3 Fabric reais.

Nenhum teste de inventário/mundo/Minecraft foi executado; não temos evidência de
zero duplicação/perda, ausência de corrupção ou crash no jogo. Ausência de código
que altere esses sistemas reduz o risco, **não substitui o teste solicitado**.
Nenhum defeito conhecido no núcleo após os testes acima; adaptador, requisitos
de otimização e matriz de runtime continuam abertos. Não distribuir como release
pronta, não recomendar instalar em mundo de produção sem backup/validação.

# Auditoria técnica — 2026-10-15 (UTC)

## Estado inicial (antes de implementar)

Commit auditado: `b492f303866c6b4410e32fedb0ab322456809125`.
Somente `README.md` (um título) e `.gitignore`. Nenhum código, build,
configuração, teste, dependência, mixin ou instrução de contribuição/AGENTS.md.
Não existia mod executável para medir uma linha de base do jogo.
Nenhum gargalo de Minecraft foi demonstrado neste ambiente.

## Alvo e versões

| Componente | Selecionado | Evidência / situação |
|---|---|---|
| Minecraft Java | **26.3**, somente | fontes oficiais Fabric; binários Mojang inacessíveis |
| Fabric Loader | **0.20.0** | release oficial publicada em 2026-10-15 |
| Fabric API | **0.163.0+26.3** | release oficial de 2026-10-14 |
| Fabric Loom | **1.18** | release estável oficial, sem SNAPSHOT |
| Gradle | **9.7.1** | wrapper do exemplo oficial consultado |
| Java requerido | **25** | build do exemplo oficial 26.3 |
| Java runtime usado nos testes independentes | **Temurin 25.0.2+10-LTS** | `jdk4py==25.0.2.1`, PyPI; `java -version` confirmado |
| Lado implementado | servidor dedicado e servidor integrado | entrypoint comum, sem importar classes cliente |

Versões selecionadas **não equivalem** a build/runtime Fabric validado.
O exemplo consultado ainda usava Loader 0.19.5 e API 0.162.0+26.3;
os pins acima usam releases estáveis mais recentes verificadas, não `latest`
no build. Não foi introduzido Yarn: o exemplo 26.3 usa nomes oficiais diretamente.

Fontes primárias consultadas por GitHub API:

- https://github.com/FabricMC/fabric/releases/tag/0.163.0%2B26.3
- https://github.com/FabricMC/fabric-loader/releases/tag/0.20.0
- https://github.com/FabricMC/fabric-loom/releases/tag/1.18
- https://github.com/FabricMC/fabric-example-mod/tree/1685ed178a113092f8aa002c4f7d78181643d75b
- https://github.com/FabricMC/fabric/tree/33b369075baa5ad6c969f44787a18bf2c3ea0a5d

APIs verificadas nessa revisão: `ServerTickEvents.START_SERVER_TICK` e
`END_SERVER_TICK`, `ServerLifecycleEvents.SERVER_STARTED/SERVER_STOPPED`,
`CommandRegistrationCallback`, `CommandSourceStack.sendSuccess`,
`source.permissions().hasPermission(Permissions.COMMANDS_ADMIN)` (usado pela
implementação Fabric de permissões). Compilação real contra Minecraft pendente.
Eventos de tick não são chamados durante pausa de servidor vazio.

## Ambiente realmente usado

- Linux x86_64, kernel `6.1.158+`, sandbox KVM; nenhum teste em Windows/macOS.
- CPU exposta: Intel Xeon @ 2.60 GHz, 2 vCPUs (1 núcleo / 2 threads), host compartilhado.
- RAM visível ~3.8 GiB; limite cgroup 3,997,061,120 bytes; sem swap.
- Disco virtual `/dev/root`, 21 GiB; backend/IOPS físicos desconhecidos.
- Nenhuma GPU ou `/dev/dri` exposta. FPS, renderização e driver: não medidos.
- Java/Gradle não estavam instalados. Runtime de testes obtido por PyPI;
  tentativa de Temurin via release GitHub falhou no redirect para
  `release-assets.githubusercontent.com`. O wheel jdk4py é runtime reduzido,
  sem `javac`/`jdk.compiler`. Para compilar o núcleo com **release 25**, usamos
  Eclipse ECJ **3.45.0.v20260224-0835**, extraído (sem executar scripts npm) de
  `@vscjava/java-language-server@0.1.2` via registry.npmjs.org. Não compilamos
  para outra versão Java. JDK completo continua necessário ao build Gradle.
- Rede de saída permite GitHub/PyPI/npm, mas não os hosts oficiais de
  distribuição. `curl` para `meta.fabricmc.net`, `maven.fabricmc.net` e
  `piston-meta.mojang.com` falhou com `SSL_ERROR_SYSCALL`.
- Isso não prova ausência de 26.3: a versão é encontrada nas fontes oficiais.
  Impede resolver artefatos, executar jogo e comprovar compatibilidade aqui.

## Mapa de execução e investigação

| Caminho | Estado inicial / alteração permitida agora | Recurso a medir no jogo |
|---|---|---|
| Inicialização / parada | nenhum; ler config uma vez, criar/liberar estado limitado por sessão | startup, heap após sair do mundo |
| Tick servidor | nenhum; dois eventos somente se diagnóstico habilitado | CPU principal, intervalo START→END, GC |
| Frame / render / meshes / texturas | ausente; não interceptado | CPU/GPU, frametime e latência de input |
| Chunk / geração / luz / persistência | ausente; não interceptado | CPU workers, disco, filas, bytes enviados |
| Entidades / IA / redstone / fluidos / block entities | ausente; não interceptado | perfil CPU e invariantes determinísticas |
| Pacotes / inventários / comandos | nenhum; consulta administrativa sob demanda | RTT separado de tempo de resposta e MSPT |

Não há varreduras, capabilities, sincronizações, contenção ou vazamentos
preexistentes a corrigir. Não atribuímos limitação de CPU/GPU/disco sem perfil.
Não paralelizamos estado de mundo. Não alteramos RNG, tick scheduling,
renderização, distância, salvamento, inventários nem pacotes de simulação.

## Plano priorizado e critérios de reversão

| Prioridade | Problema / evidência | Mudança | Risco | Métrica / reversão |
|---|---|---|---|---|
| P0 | inexistência de build e baseline | estrutura 26.3 exata + protocolo A/B | API real ainda não compilada | bloquear release até build/runtime passarem |
| P1 | medir ticks sem introduzir histórico ilimitado ou trabalho por tick O(n); risco arquitetural, não gargalo provado do jogo | diagnóstico opt-in, ring `long[]`, relatório sob demanda e cooldown | pequeno custo de relógio/eventos; percentis só cobrem janela | microbenchmark comparativo e alocação; manter desativado/remover se custo no jogo relevante |
| P2 | gargalo do jogo ainda desconhecido | JFR + baseline Minecraft e Fabric sem mod, depois config off/on | medição pode perturbar carga | só otimizar hotspot com ganho acima do ruído; reverter regressões |
| P3 | render/chunks/rede/memória | uma alteração localizada após P2, nunca pacote amplo | alto sem evidência | ganho A/B repetido + semântica idêntica; reverter sem ganho |
| P4 | QoL de inventário e persistência | **adiado**, requer servidor/multiplayer real e transações testadas | duplicação/perda de itens | zero perda/duplicação; não distribuir sem teste |

A primeira entrega independente é infraestrutura de diagnóstico eficiente,
**não uma otimização comprovada do Minecraft**. Não satisfaz ainda a exigência
de alto impacto no jogo: essa decisão depende do profiling bloqueado.

## Tentativa real de build

`JAVA_HOME=<runtime-25> ./gradlew --version` executou o wrapper oficial e
falhou em `https://services.gradle.org/distributions/gradle-9.7.1-bin.zip`:
`SSLHandshakeException: Remote host terminated the handshake`. Não houve
resolução Loom/Minecraft, compilação do adaptador, testes Fabric ou JAR.
Além da rede, é preciso um JDK 25 completo (o runtime reduzido não basta).
O SHA-256 do wrapper obtido do exemplo oficial é
`7a9ce74cff467ca1bf60a4fcd9f05185acceda4d0f382434d393e17864262c5d`.

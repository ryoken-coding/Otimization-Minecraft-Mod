# Third-party notices

- Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`):
  Gradle authors, Apache-2.0. Scripts retain their copyright/license headers.
  Retrieved without modifications from FabricMC/fabric-example-mod commit
  `1685ed178a113092f8aa002c4f7d78181643d75b`. Wrapper properties pin Gradle 9.7.1.
- Build layout based on that official Fabric example (CC0-1.0). No example
  gameplay implementation, icon or mixin copied.
- Fabric API and Fabric Loader: Apache-2.0 dependencies, not vendored. API
  signatures/documentation consulted at revisions recorded in docs/AUDITORIA.md.
- Minecraft is proprietary; no Minecraft binary or source is redistributed.
- Tests use Temurin runtime 25.0.2+10 via jdk4py 25.0.2.1 and Eclipse ECJ
  3.45.0.v20260224-0835 (EPL-2.0), from the npm archive
  `@vscjava/java-language-server@0.1.2`. Compiler/runtime are not redistributed.
  All diagnostic implementation/test/benchmark Java sources in this repository
  are new; no optimization mod code was incorporated.

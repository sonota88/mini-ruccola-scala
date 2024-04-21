version: see build.sbt

```
git clone --recursive https://github.com/sonota88/mini-ruccola-scala.git
cd mini-ruccola-scala

./docker.sh build
./test.sh all
```

```
(
  cd src/main/scala/mini_ruccola
  LANG=C wc -l *.scala lib/*.scala
)

  337 CodeGenerator.scala
   67 Lexer.scala
   25 Main.scala
  302 Parser.scala
   92 lib/Json.scala
   40 lib/Node.scala
   62 lib/Token.scala
   16 lib/Utils.scala
  941 total

(
  cd src/main/scala/mini_ruccola
  LANG=C wc -l {Lexer,Parser,CodeGenerator}.scala lib/{Node,Token}.scala
)

   67 Lexer.scala
  302 Parser.scala
  337 CodeGenerator.scala
   40 lib/Node.scala
   62 lib/Token.scala
  808 total
```

package mini_ruccola

import mini_ruccola.lib._

object Main {

  def main(args: Array[String]): Unit = {
    val cmd = args(0)

    cmd match {
      case "test-json" => testJson()
      case "lex"       => Lexer.main()
      case "parse"     => Parser.main()
      case "codegen"   => CodeGenerator.main()
      case _ => throw Utils.panic("unsupported command")
    }
  }

  private def testJson(): Unit = {
    val src = Utils.readStdin()
    val xs: ListNode = Json.parse(src)
    Json.prettyPrint(xs)
  }

}

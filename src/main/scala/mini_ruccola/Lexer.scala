package mini_ruccola

import mini_ruccola.lib._

object Lexer {

  // patterns
  val P_SKIP    = Utils.re("^([ \n])")
  val P_SYM     = Utils.re("^(==|!=|[(){},;=+*])")
  val P_IDENT   = Utils.re("^([a-z_][a-z0-9_]*)")
  val P_INT     = Utils.re("^(-?[0-9]+)")
  val P_COMMENT = Utils.re("^(//.*?)\n")
  val P_STR     = Utils.re("""^"(.*?)"""")

  def main(): Unit = {
    val src = Utils.readStdin()

    var lineNo = 1

    var i = 0
    while (i < src.size) {
      val tail = src.substring(i)

      tail match {
        case P_SKIP(str) => {
          if (str == "\n") { lineNo += 1 }
          i += str.size
        }
        case P_SYM(str) => {
          printToken(lineNo, TokenKind.Sym, str)
          i += str.size
        }
        case P_INT(str) => {
          printToken(lineNo, TokenKind.Int, str)
          i += str.size
        }
        case P_IDENT(str) => {
          val kind = if (isKw(str)) TokenKind.Kw else TokenKind.Ident
          printToken(lineNo, kind, str)
          i += str.size
        }
        case P_STR(str) => {
          printToken(lineNo, TokenKind.Str, str)
          i += str.size + 2
        }
        case P_COMMENT(str) => {
          i += str.size
        }
        case _ => throw Utils.panic(s"unexpected pattern (${i}) (${tail})")
      }
    }
  }

  private def printToken(lineNo: Int, kind: TokenKind, value: String): Unit = {
    val token = Token(lineNo, kind, value)
    Json.print(token.toSeq())
    print("\n")
  }

  val KEYWORDS = Set(
    "func", "set", "var", "call_set", "call", "return", "case", "when", "while",
    "_cmt", "_debug"
  )

  private def isKw(str: String): Boolean = KEYWORDS.contains(str)

}

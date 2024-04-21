package mini_ruccola

import mini_ruccola.lib._

object Parser {

  def main(): Unit = new Parser().main()

}

class Parser {

  var pos: Int = 0
  var tokens: Seq[Token] = Seq()

  def main(): Unit = {
    this.pos = 0

    this.tokens =
      Utils.readStdinLines()
        .map(Json.parse)
        .map(listNode => Token.fromSeq(listNode.value))

    val topStmts = parseTopStmts()

    val ast = _list(_str("top_stmts")).appendedAll(topStmts)

    Json.prettyPrint(ast)
  }

  // --------------------------------

  private def parseArg(): Node = {
    peek() match {
      case Token(_, TokenKind.Int, value) => {
        bump()
        _int(value.toInt)
      }
      case Token(_, TokenKind.Ident, value) => {
        bump()
        _str(value)
      }
      case _ => throw Utils.panic(s"invalid token kind (${peek()})")
    }
  }

  private def parseArgs(): ListNode = {
    var args = ListNode()

    if (peekVal() == ")") {
      return args
    }
    args = args.appended(parseArg())

    while (peekVal() == ",") {
      consume(",")
      args = args.appended(parseArg())
    }

    args
  }

  private def parseExprFactor(): Node = {
    peek() match {
      case Token(_, TokenKind.Int, value) => {
        bump()
        _int(value.toInt)
      }
      case Token(_, TokenKind.Ident, value) => {
        bump()
        _str(value)
      }
      case Token(_, TokenKind.Sym, value) => {
        consume("(")
        val expr = parseExpr()
        consume(")")
        expr
      }
      case _ => throw Utils.panic(s"invalid token kind")
    }
  }

  val BINOP_SET = Set("+", "*", "==", "!=")

  private def isBinOp(t: Token): Boolean = {
    BINOP_SET.contains(t.value)
  }

  private def parseExpr(): Node = {
    var expr = parseExprFactor()

    while (isBinOp(peek())) {
      val op = peekVal(); bump()
      val rhs = parseExprFactor()
      expr = _list(_str(op), expr, rhs)
    }

    expr
  }

  private def parseSet(): ListNode = {
    consume("set")
    val varName = peekVal(); bump()
    consume("=")
    val expr = parseExpr()
    consume(";")

    _list(_str("set"), _str(varName), expr)
  }

  private def parseFuncall(): ListNode = {
    val fnName = peekVal()
    bump()

    consume("(")
    val args = parseArgs()
    consume(")")

    _list(_str(fnName)).appendedAll(args)
  }

  private def parseCall(): ListNode = {
    consume("call")
    val funcall = parseFuncall()
    consume(";")

    _list(_str("call"), funcall)
  }

  private def parseCallSet(): ListNode = {
    consume("call_set")

    val varName = peekVal()
    bump()

    consume("=")
    val funcall = parseFuncall()
    consume(";")

    _list(_str("call_set"), _str(varName), funcall)
  }

  private def parseReturn(): ListNode = {
    consume("return")
    val expr = parseExpr()
    consume(";")

    _list(_str("return"), expr)
  }

  private def parseWhile(): ListNode = {
    consume("while")
    consume("(")
    val cond = parseExpr()
    consume(")")
    consume("{")
    val stmts = parseStmts()
    consume("}")

    _list(_str("while"), cond, stmts)
  }

  private def parseWhen(): ListNode = {
    consume("when")
    consume("(")
    val cond = parseExpr()
    consume(")")
    consume("{")
    val stmts = parseStmts()
    consume("}")

    _list(cond).appendedAll(stmts)
  }

  private def parseCase(): ListNode = {
    consume("case")

    var whenClauses: ListNode = ListNode()

    while (peekVal() == "when") {
      whenClauses = whenClauses.appended(parseWhen())
    }

    _list(_str("case")).appendedAll(whenClauses)
  }

  private def parseVmComment(): ListNode = {
    consume("_cmt")
    consume("(")
    val comment = peekVal(); bump()
    consume(")")
    consume(";")

    _list(_str("_cmt"), _str(comment))
  }

  private def parseStmt(): ListNode = {
    peekVal() match {
      case "set"      => parseSet()
      case "call"     => parseCall()
      case "call_set" => parseCallSet()
      case "return"   => parseReturn()
      case "while"    => parseWhile()
      case "case"     => parseCase()
      case "_cmt"     => parseVmComment()
      case _ => throw Utils.panic(s"unsupported (${peek()})")
    }
  }

  private def parseStmts(): ListNode = {
    var stmts = _list()
    while (peekVal() != "}") {
      val stmt = parseStmt()
      stmts = stmts.appended(stmt)
    }
    stmts
  }

  private def parseVar(): ListNode = {
    consume("var")

    val varName = peekVal()
    bump()

    var stmt = _list(_str("var"), _str(varName))

    if (peekVal() == "=") {
      consume("=")
      val expr = parseExpr()
      stmt = stmt.appended(expr)
    }
    consume(";")

    stmt
  }

  private def parseFuncDef(): ListNode = {
    consume("func")

    val fnName: String =
      peek() match {
        case Token(_, TokenKind.Ident, str) => str
        case _ => throw Utils.panic("invalid token kind")
      }
    bump()

    consume("(")
    val args = parseArgs()
    consume(")")
    consume("{")

    var stmts: ListNode = ListNode()
    while (peekVal() != "}") {
      val stmt =
        peekVal() match {
          case "var" => parseVar()
          case _ => parseStmt()
        }
      stmts = stmts.appended(stmt)
    }

    consume("}")

    _list(_str("func"), _str(fnName), args, stmts)
  }

  private def parseTopStmts(): ListNode = {
    var topStmts = ListNode()

    while (!isEnd()) {
      val topStmt =
        peekVal() match {
          case "func" => parseFuncDef()
          case _ => throw Utils.panic("unexpected token")
        }
      topStmts = topStmts.appended(topStmt)
    }

    topStmts
  }

  // --------------------------------

  private def isEnd(): Boolean = this.pos >= this.tokens.size
  private def peekVal(): String = peek().value
  private def peek(): Token = this.tokens(this.pos)
  private def bump(): Unit = this.pos += 1

  private def consume(expected: String): Unit = {
    val actual = peekVal()
    if (actual == expected) {
      bump()
    } else {
      throw Utils.panic(s"unexpected token / expected (${expected}) / actual (${actual})")
    }
  }

  private def _int(n: Int): IntNode = IntNode(n) 
  private def _str(str: String): StrNode = StrNode(str) 
  private def _list(xs: Node*): ListNode = ListNode(xs)

}

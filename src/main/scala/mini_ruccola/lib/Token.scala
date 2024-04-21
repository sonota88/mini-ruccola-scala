package mini_ruccola.lib

sealed abstract class TokenKind(val str: String)

object TokenKind {

  case object Kw    extends TokenKind("kw"   ) // keyword
  case object Sym   extends TokenKind("sym"  ) // symbol
  case object Ident extends TokenKind("ident") // identifier
  case object Int   extends TokenKind("int"  ) // integer
  case object Str   extends TokenKind("str"  ) // string

  val VALUE_SET = Set(Kw, Sym, Ident, Int, Str)

  def fromStr(kindStr: String): Option[TokenKind] = {
    VALUE_SET.find(value => value.str == kindStr)
  }

}

case class Token(lineNo: Int, kind: TokenKind, value: String) {

  def toSeq(): Seq[Node] = {
    Seq(
      IntNode(this.lineNo),
      StrNode(this.kind.str),
      StrNode(this.value)
    )
  }

}

object Token {

  def fromSeq(xs: Seq[Node]): Token = {
    val lineNo: Int =
      xs(0) match {
        case (node: IntNode) => node.value
        case _ => throw Utils.panic("invalid node type")
      }

    val kind: TokenKind =
      xs(1) match {
        case (node: StrNode) => {
          TokenKind.fromStr(node.value) match {
            case Some(kind) => kind
            case None => throw Utils.panic(s"invalid token kind (${node.value})")
          }
        }
        case _ => throw Utils.panic("invalid node type")
      }

    val value: String =
      xs(2) match {
        case (node: StrNode) => node.value
        case _ => throw Utils.panic("invalid node type")
      }

    new Token(lineNo, kind, value)
  }

}

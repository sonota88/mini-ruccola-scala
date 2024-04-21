package mini_ruccola.lib

object Json {

  def prettyPrint(xs: ListNode): Unit = printList(xs.value, true, 0)
  def print(xs: ListNode): Unit = printList(xs.value, false, 0)
  def print(xs: Seq[Node]): Unit = printList(xs, false, 0)

  private def _print(arg: Any): Unit = System.out.print(arg)

  private def printList(xs: Seq[Node], pretty: Boolean, lv: Int): Unit = {
    _print("[")
    if (pretty) { _print("\n") }

    xs.zipWithIndex.foreach {
      case (x, i) => {
        if (i >= 1) {
          _print(",")
          _print(if (pretty) "\n" else " ")
        }
        printNode(x, pretty, lv + 1)
      }
    }
    if (pretty) { _print("\n") }

    if (pretty) { printIndent(lv) }
    _print("]")
  }

  private def printNode(node: Node, pretty: Boolean, lv: Int): Unit = {
    if (pretty) { printIndent(lv) }
    node match {
      case IntNode(n) => _print(n)
      case StrNode(s) => _print("\"" + s + "\"")
      case ListNode(xs) => printList(xs, pretty, lv)
    }
  }

  private def printIndent(lv: Int): Unit = _print("  " * lv)

  def parse(json: String): ListNode = parseList(json)._1

  // patterns
  val P_BEGIN = Utils.re("^\\[")
  val P_END   = Utils.re("^\\]")
  val P_SKIP  = Utils.re("^([ ,\n])")
  val P_INT   = Utils.re("^(-?[0-9]+)")
  val P_STR   = Utils.re("""^"(.*?)"""")

  private def parseList(tail: String): (ListNode, Int) = {
    var i = 0
    var xs: List[Node] = List()

    if (tail(0) != '[') {
      throw Utils.panic("assertion failed")
    }
    i += 1

    while (i < tail.size) {
      val work = tail.substring(i)

      work match {
        case P_BEGIN() => {
          val (inner, size) = parseList(work)
          xs = inner :: xs
          i += size
        }
        case P_END() => {
          i += 1
          return (ListNode(xs.reverse), i)
        }
        case P_SKIP(str) => {
          i += str.size
        }
        case P_INT(str) => {
          xs = IntNode(str.toInt) :: xs
          i += str.size
        }
        case P_STR(str) => {
          xs = StrNode(str) :: xs
          i += str.size + 2
        }
        case _ => {
          throw Utils.panic(s"unexpected pattern (${work})")
        }
      }
    }

    throw Utils.panic(s"invalid input")
  }

}

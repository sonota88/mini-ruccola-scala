package mini_ruccola.lib

sealed trait Node

final case class IntNode(value: Int) extends Node
final case class StrNode(value: String) extends Node

final case class ListNode(value: Seq[Node] = Seq()) extends Node {

  def strAt(i: Int): StrNode = {
    get(i) match {
      case x: StrNode => x
      case _ => throw Utils.panic("invalid node type")
    }
  }

  def listAt(i: Int): ListNode = {
    get(i) match {
      case x: ListNode => x
      case _ => throw Utils.panic("invalid node type")
    }
  }

  def appended(node: Node): ListNode = {
    ListNode(
      this.value.appended(node)
    )
  }

  def appendedAll(listNode: ListNode): ListNode = {
    ListNode(
      this.value.appendedAll(listNode.value)
    )
  }

  def get(i: Int): Node = this.value(i)
  def size(): Int = this.value.size
  def drop(n: Int): ListNode = ListNode(this.value.drop(1))

}

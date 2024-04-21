package mini_ruccola.lib

import scala.util.matching.Regex

object Utils {

  def readStdin(): String = readStdinLines().mkString("\n")
  def readStdinLines(): Seq[String] = io.Source.stdin.getLines().toSeq
  def re(patternStr: String): Regex = ("(?s)" + patternStr).r.unanchored
  def panic(msg: String): RuntimeException = throw new RuntimeException("PANIC " + msg)

  // for debug
  def puts_e(arg: Any): Unit = System.err.println(arg)
  def puts_kv_e(k: String, v: Any): Unit = puts_e(s"${k} (${v})")

}

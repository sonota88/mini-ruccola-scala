package mini_ruccola

import mini_ruccola.lib._

object CodeGenerator {

  var _labelId = 0

  private def getNextLabelId(): Int = {
    this._labelId += 1
    this._labelId
  }

  def main(): Unit = {
    val src = Utils.readStdin()
    val ast: ListNode = Json.parse(src)

    puts("  call main")
    puts("  exit")

    genTopStmts(ast)
  }

  private def genExprAdd(): Unit = {
    puts("  pop reg_b")
    puts("  pop reg_a")
    puts("  add reg_a reg_b")
  }

  private def genExprMult(): Unit = {
    puts("  pop reg_b")
    puts("  pop reg_a")
    puts("  mul reg_b")
  }

  private def genExprEqCommon(eqType: String, thenVal: Int, elseVal: Int): Unit = {
    val labelId = getNextLabelId()

    val labelEnd = s"end_${eqType}_${labelId}"
    val labelThen = s"then_${labelId}"

    puts("  pop reg_b")
    puts("  pop reg_a")

    puts("  cmp")
    puts(s"  je ${labelThen}")

    puts(s"  mov reg_a ${elseVal}")
    puts(s"  jmp ${labelEnd}")

    puts(s"label ${labelThen}")
    puts(s"  mov reg_a ${thenVal}")

    puts(s"label ${labelEnd}")
  }

  private def genExprEq(): Unit = genExprEqCommon("eq", 1, 0)
  private def genExprNeq(): Unit = genExprEqCommon("neq", 0, 1)

  private def genExprBinary(ctx: Context, binopExpr: ListNode): Unit = {
    val op = binopExpr.strAt(0).value
    val lhs = binopExpr.get(1)
    val rhs = binopExpr.get(2)

    genExpr(ctx, lhs)
    puts("  push reg_a")
    genExpr(ctx, rhs)
    puts("  push reg_a")

    op match {
      case "+"  => genExprAdd()
      case "*"  => genExprMult()
      case "==" => genExprEq()
      case "!=" => genExprNeq()
      case _ => throw Utils.panic(s"unsupported operator (${op})")
    }
  }

  private def genExpr(ctx: Context, expr: Node): Unit = {
    expr match {
      case IntNode(n) => puts(s"  mov reg_a ${n}")
      case StrNode(str) => {
        if (ctx.lvars.contains(str)) {
          val disp = Context.lvarDisp(ctx, str)
          puts(s"  mov reg_a [bp:${disp}]")
        } else if (ctx.args.contains(str)) {
          val disp = Context.argDisp(ctx, str)
          puts(s"  mov reg_a [bp:${disp}]")
        } else {
          throw Utils.panic(s"no such variable or function argument (${str})")
        }
      }
      case binopExpr: ListNode => genExprBinary(ctx, binopExpr)
    }
  }

  private def _genSet(ctx: Context, dest: String, expr: Node): Unit = {
    genExpr(ctx, expr)

    if (ctx.lvars.contains(dest)) {
      val disp = Context.lvarDisp(ctx, dest)
      puts(s"  mov [bp:${disp}] reg_a")
    } else {
      throw Utils.panic(s"no such variable (${dest})")
    }
  }

  private def genSet(ctx: Context, stmt: ListNode): Unit = {
    val dest: String = stmt.strAt(1).value
    val expr = stmt.get(2)

    _genSet(ctx, dest, expr)
  }

  private def _genFuncall(ctx: Context, funcall: ListNode): Unit = {
    val fnName = funcall.strAt(0).value
    val args = funcall.drop(1)

    args.value.reverse.foreach(arg => {
      genExpr(ctx, arg)
      puts("  push reg_a")
    })

    _genVmComment(s"call  ${fnName}")
    puts(s"  call ${fnName}")
    puts(s"  add sp ${args.size()}")
  }

  private def genCall(ctx: Context, stmt: ListNode): Unit = {
    val funcall = stmt.listAt(1)
    _genFuncall(ctx, funcall)
  }

  private def genCallSet(ctx: Context, stmt: ListNode): Unit = {
    val varName = stmt.strAt(1).value
    val funcall = stmt.listAt(2)

    _genFuncall(ctx, funcall)

    val disp = Context.lvarDisp(ctx, varName)
    puts(s"  mov [bp:${disp}] reg_a")
  }

  private def genReturn(ctx: Context, stmt: ListNode): Unit = {
    val expr = stmt.get(1)
    genExpr(ctx, expr)
    asmEpilogue()
    puts("  ret")
  }

  private def genWhile(ctx: Context, stmt: ListNode): Unit = {
    val cond = stmt.get(1)
    val stmts = stmt.listAt(2)

    val labelId = getNextLabelId()

    val labelBegin = s"while_${labelId}"
    val labelEnd = s"end_while_${labelId}"

    puts(s"label ${labelBegin}")

    genExpr(ctx, cond)
    puts("  mov reg_b 0")
    puts("  cmp")

    puts(s"  je ${labelEnd}")

    genStmts(ctx, stmts)

    puts(s"  jmp ${labelBegin}")
    puts(s"label ${labelEnd}")
  }

  private def genCase(ctx: Context, stmt: ListNode): Unit = {
    val labelId = getNextLabelId()

    val whenClauses = stmt.drop(1)

    var whenIdx = -1

    val labelEnd = s"end_case_${labelId}"
    val labelEndWhenHead = s"end_when_${labelId}"

    whenClauses.value
      .foreach(whenClause => {
        whenIdx += 1

        val _whenClause =
          whenClause match {
            case listNode: ListNode => listNode
            case _ => throw Utils.panic("invalid node type")
          }

        val cond = _whenClause.get(0)
        val stmts = _whenClause.drop(1)

        genExpr(ctx, cond)

        puts("  mov reg_b 0")
        puts("  cmp")

        puts(s"  je ${labelEndWhenHead}_${whenIdx}")

        genStmts(ctx, stmts)
        puts(s"  jmp ${labelEnd}")

        puts(s"label ${labelEndWhenHead}_${whenIdx}")
      })

    puts(s"label ${labelEnd}")
  }

  private def _genVmComment(comment: String): Unit = {
    puts("  _cmt " + comment.replace(" ", "~"))
  }

  private def genVmComment(stmt: ListNode): Unit = {
    val comment: String = stmt.strAt(1).value
    _genVmComment(comment)
  }

  private def genStmt(ctx: Context, stmt: ListNode): Unit = {
    val head: String = stmt.strAt(0).value
    head match {
      case "set"      => genSet(ctx, stmt)
      case "call"     => genCall(ctx, stmt)
      case "call_set" => genCallSet(ctx, stmt)
      case "return"   => genReturn(ctx, stmt)
      case "while"    => genWhile(ctx, stmt)
      case "case"     => genCase(ctx, stmt)
      case "_cmt"     => genVmComment(stmt)
      case _ => throw Utils.panic("unsupported statement")
    }
  }

  private def genStmts(ctx: Context, stmts: ListNode): Unit = {
    stmts.value.foreach(stmt =>
      stmt match {
        case xs: ListNode => genStmt(ctx, xs)
        case _ => throw Utils.panic("invalid node type")
      }
    )
  }

  private def genVar(ctx: Context, stmt: ListNode): Unit = {
    puts("  add sp -1")

    stmt.size() match {
      case 2 => () // do nothing
      case 3 => {
        val dest = stmt.strAt(1).value
        val expr = stmt.get(2)
        _genSet(ctx, dest, expr)
      }
      case _ => throw Utils.panic("unsupported")
    }
  }

  private def toArgsSeq(xs: ListNode): Seq[String] = {
    xs.value.map(node =>
      node match {
        case StrNode(s) => s
        case _ => throw Utils.panic("invalid node type")
      }
    )
  }

  private def genFuncDef(fnDef: ListNode): Unit = {
    val fnName: String = fnDef.strAt(1).value
    val args: ListNode = fnDef.listAt(2)
    val stmts = fnDef.listAt(3)

    var ctx = new Context(toArgsSeq(args), Seq())

    puts(s"label ${fnName}")

    asmPrologue()

    stmts.value.foreach(stmt => {
      stmt match {
        case stmt: ListNode => {
          val head: String = stmt.strAt(0).value
          head match {
            case "var" => {
              val lvar: String = stmt.strAt(1).value
              ctx = Context.addLvar(ctx, lvar)
              genVar(ctx, stmt)
            }
            case _ => genStmt(ctx, stmt)
          }
        }
        case _ => throw Utils.panic("invalid node type")
      }
    })

    asmEpilogue()

    puts("  ret")
  }

  private def genTopStmts(topStmts: ListNode): Unit = {
    topStmts.value.drop(1).foreach(topStmt =>
      topStmt match {
        case listNode: ListNode => genFuncDef(listNode)
        case _ => throw Utils.panic("invalid node type")
      }
    )
  }

  // --------------------------------

  private def asmPrologue(): Unit = {
    puts("  push bp")
    puts("  mov bp sp")
  }

  private def asmEpilogue(): Unit = {
    puts("  mov sp bp")
    puts("  pop bp")
  }

  private def puts(arg: String): Unit = println(arg)

}

class Context(val args: Seq[String], val lvars: Seq[String])

object Context {

  def addLvar(ctx: Context, lvar: String): Context = {
    new Context(ctx.args, ctx.lvars :+ lvar)
  }

  def lvarDisp(ctx: Context, lvar: String): Int = -(ctx.lvars.indexOf(lvar) + 1)
  def argDisp(ctx: Context, arg: String): Int = ctx.args.indexOf(arg) + 2

}
